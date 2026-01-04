"""
Chatbot Service
RAG-based chatbot implementation for game rules and platform guidance.
Now enhanced with:
- RAG via Chroma (static docs)
- Live user context via Java backend endpoints (profile/friends/achievements)
"""
from __future__ import annotations

import uuid
import hashlib
import base64
import json
from typing import Optional, List, Dict, Tuple

from app.config import settings
from app.modules.chatbot.types import ChatResponse, ConversationMessage, ConversationHistory
from app.modules.chatbot.rag.llm_client import LlmClient, OpenAiLlmClient
from app.modules.chatbot.vector.chroma import ChatVectorDB

from app.modules.chatbot.services.profile_service import ProfileService
from app.modules.chatbot.services.friend_service import FriendService
from app.modules.chatbot.services.achievement_service import AchievementService
from app.modules.chatbot.services.game_service import GameService
from app.modules.chatbot.services.lobby_service import LobbyService


class ChatbotService:
    def __init__(
        self,
        llm_client: Optional[LlmClient] = None,
        vector_db: Optional[ChatVectorDB] = None,
        profile_service: Optional[ProfileService] = None,
        friend_service: Optional[FriendService] = None,
        achievement_service: Optional[AchievementService] = None,
        game_service: Optional[GameService] = None,
        lobby_service: Optional[LobbyService] = None,
    ) -> None:
        self.vector_db = vector_db
        
        # Initialize services (create new instances if not provided)
        self.profile_service = profile_service or ProfileService()
        self.friend_service = friend_service or FriendService()
        self.achievement_service = achievement_service or AchievementService()
        self.game_service = game_service or GameService()
        self.lobby_service = lobby_service or LobbyService()

        self.enabled = settings.CHATBOT_ENABLED
        self.cache: Dict[str, str] = {}
        self.conversations: Dict[str, List[ConversationMessage]] = {}

        self.use_llm: bool = settings.CHATBOT_USE_LLM

        # assign FIRST
        self.llm_client: Optional[LlmClient] = llm_client

        # log safely AFTER assignment
        print(
            "[chatbot] use_llm=", self.use_llm,
            "client=", type(self.llm_client).__name__ if self.llm_client else None,
            "cache_enabled=", settings.CHATBOT_CACHE_ENABLED
        )

        if self.use_llm and self.llm_client is None:
            try:
                self.llm_client = OpenAiLlmClient()
                print("[chatbot] OpenAI LLM client initialized inside ChatbotService.")
            except Exception as e:
                self.llm_client = None
                print(f"[chatbot] LLM enabled but failed to initialize LLM client: {e}. Using placeholder mode.")

    # -----------------------------
    # Basics
    # -----------------------------

    def _is_disabled(self) -> bool:
        return not self.enabled

    def _get_or_create_conversation_id(self, conversation_id: Optional[str]) -> str:
        return conversation_id or str(uuid.uuid4())

    def _extract_bearer_token(self, auth_header: Optional[str]) -> Optional[str]:
        if not auth_header:
            return None
        # Handle both "Bearer <token>" and just "<token>" formats
        if auth_header.startswith("Bearer "):
            token = auth_header[len("Bearer "):].strip()
        else:
            token = auth_header.strip()
        # Return None if token is empty
        return token if token else None

    def _extract_user_id_from_token(self, token: Optional[str]) -> Optional[str]:
        """
        Extract user_id (subject) from JWT token without verification.
        JWT tokens are base64url encoded JSON, so we can decode the payload.
        """
        if not token:
            return None
        
        try:
            # JWT format: header.payload.signature
            parts = token.split('.')
            if len(parts) < 2:
                return None
            
            # Decode the payload (second part)
            payload = parts[1]
            # Add padding if needed (base64url decoding)
            padding = 4 - len(payload) % 4
            if padding != 4:
                payload += '=' * padding
            
            # Decode base64url
            decoded = base64.urlsafe_b64decode(payload)
            data = json.loads(decoded)
            
            # Extract 'sub' claim (subject/user_id)
            user_id = data.get('sub')
            return user_id
        except Exception as e:
            print(f"[chatbot] Failed to extract user_id from token: {e}")
            return None

    def _get_cache_key(self, message: str, user_id: Optional[str], context_kinds: Optional[List[str]] = None) -> str:
        # Include context_kinds in cache key to differentiate cached responses with/without user context
        context_str = ",".join(sorted(context_kinds)) if context_kinds else "no_context"
        base = f"{user_id or 'anon'}|{message.lower().strip()}|{context_str}"
        return hashlib.md5(base.encode()).hexdigest()

    def _is_cached(self, cache_key: str) -> bool:
        if not settings.CHATBOT_CACHE_ENABLED:
            return False
        return cache_key in self.cache

    def _get_cached_response(self, cache_key: str, conversation_id: str) -> ChatResponse:
        return ChatResponse(
            response=self.cache[cache_key],
            conversation_id=conversation_id,
            sources=["cached"],
            cached=True,
        )

    def _store_in_cache(self, cache_key: str, response: str) -> None:
        if not settings.CHATBOT_CACHE_ENABLED:
            return
        self.cache[cache_key] = response

    def _ensure_conversation_exists(self, conversation_id: str) -> None:
        if conversation_id not in self.conversations:
            self.conversations[conversation_id] = []

    def _build_history_context(self, conversation_id: str, max_messages: int = 10) -> str:
        messages = self.conversations.get(conversation_id, [])
        if not messages:
            return ""
        tail = messages[-max_messages:]
        return "\n".join([f"{m.role}: {m.content}" for m in tail])

    def _add_to_conversation(self, conversation_id: str, user_message: str, assistant_response: str) -> None:
        self._ensure_conversation_exists(conversation_id)
        self.conversations[conversation_id].append(ConversationMessage(role="user", content=user_message))
        self.conversations[conversation_id].append(ConversationMessage(role="assistant", content=assistant_response))

    # -----------------------------
    # Main chat entrypoint
    # -----------------------------

    def chat(
        self,
        message: str,
        conversation_id: Optional[str] = None,
        user_id: Optional[str] = None,
        auth_header: Optional[str] = None,
        bypass_cache: bool = False,
    ) -> ChatResponse:
        if self._is_disabled():
            return ChatResponse(
                response="Chatbot is currently disabled.",
                conversation_id=self._get_or_create_conversation_id(conversation_id),
                sources=None,
                cached=False,
            )

        conv_id = self._get_or_create_conversation_id(conversation_id)
        token = self._extract_bearer_token(auth_header)
        
        # Debug logging for token extraction
        if not token:
            print(f"[chatbot] Warning: No token extracted from auth_header. auth_header present: {auth_header is not None}")
        else:
            print(f"[chatbot] Token extracted successfully. Token length: {len(token)}, starts with: {token[:20] if len(token) > 20 else token}...")

        # Extract user_id from token if not provided
        if not user_id and token:
            user_id = self._extract_user_id_from_token(token)
            if user_id:
                print(f"[chatbot] Extracted user_id from token: {user_id}")
            else:
                print(f"[chatbot] Warning: Could not extract user_id from token")

        # Check if we need user context BEFORE checking cache
        # This ensures we fetch fresh data for user-specific queries
        context_kinds: List[str] = []
        if token:
            context_kinds = self._needs_user_context(message)
        
        # Cache key includes context_kinds to differentiate cached responses with/without context
        # This allows us to cache both generic responses and user-specific responses separately
        cache_key = self._get_cache_key(message, user_id, context_kinds)
        
        # Check cache - but bypass cache if query needs fresh user context data
        # This ensures achievement/match history queries always get current data
        if not bypass_cache and not context_kinds and self._is_cached(cache_key):
            return self._get_cached_response(cache_key, conv_id)

        # Fetch user context if needed (for both LLM and placeholder modes)
        user_ctx_text = ""
        user_ctx_sources: List[str] = []
        if context_kinds:
            user_ctx_text, user_ctx_sources = self._fetch_user_context(token, user_id, context_kinds)

        # Decide whether to use LLM or placeholder
        if self.use_llm and self.llm_client is not None:
            response_text, sources = self._generate_llm_response(
                message=message,
                conversation_id=conv_id,
                user_id=user_id,
                token=token,
            )
        else:
            response_text = self._generate_placeholder_response(message, user_ctx_text, context_kinds, user_id)
            sources = ["placeholder_mode"] + user_ctx_sources

        self._store_in_cache(cache_key, response_text)
        self._add_to_conversation(conv_id, message, response_text)

        return ChatResponse(
            response=response_text,
            conversation_id=conv_id,
            sources=sources,
            cached=False,
        )

    # -----------------------------
    # Retrieval (Chroma RAG)
    # -----------------------------

    def _select_docs_for_message(self, message: str) -> Tuple[str, List[str]]:
        top_k = settings.CHATBOT_RAG_TOP_K

        if self.vector_db is None:
            return (
                "This is a Connect Four training platform with AI agents, logging, and analytics. "
                "If the answer is not in the known docs, reply that you are not sure.",
                ["generic_info"],
            )

        hits = self.vector_db.query(query_text=message, top_k=top_k)

        if not hits:
            return (
                "No relevant documents were found in the knowledge base. "
                "If you cannot answer confidently, say you are not sure.",
                ["kb_empty_or_no_match"],
            )

        docs_text_parts: List[str] = []
        sources: List[str] = []

        for h in hits:
            meta_src = h.metadata.get("source") if isinstance(h.metadata, dict) else None
            sources.append(str(meta_src or h.id))
            docs_text_parts.append(h.document)

        return "\n\n---\n\n".join(docs_text_parts), sources

    # -----------------------------
    # Live user context (Java backend)
    # -----------------------------

    def _needs_user_context(self, message: str) -> List[str]:
        m = message.lower()
        needs: List[str] = []

        # Profile-related keywords - more flexible matching
        profile_keywords = [
            "my profile", "my rank", "my stats", "my account", "my exp", "my experience",
            "profile", "rank", "stats", "level", "experience", "xp", "player info"
        ]
        if any(k in m for k in profile_keywords):
            needs.append("profile")

        # Friends-related keywords - catch variations like "how many friends", "friends I have", etc.
        friends_keywords = [
            "my friends", "friends list", "who are my friends", "show my friends",
            "how many friends", "friends i have", "friends do i have", "number of friends",
            "friend count", "my friend", "friends", "friend"
        ]
        if any(k in m for k in friends_keywords):
            needs.append("friends")

        # Friend requests keywords
        friend_request_keywords = [
            "friend requests", "pending requests", "pending friend requests",
            "friend request", "pending friend", "requests", "friend invite"
        ]
        if any(k in m for k in friend_request_keywords):
            needs.append("friend_requests")

        # Achievements-related keywords - catch "how to get", "how do i get", "unlock", etc.
        # Also catch questions about available achievements, Connect Four achievements, etc.
        achievement_keywords = [
            "my achievements", "my badges", "what did i unlock", "what have i unlocked",
            "achievements", "achievement", "badges", "badge", "unlock", "unlocked",
            "how to get", "how do i get", "how can i get", "how to unlock", "how do i unlock",
            "get achievement", "earn achievement", "obtain achievement",
            "what achievements", "which achievements", "show achievements", "list achievements",
            "achievements do i", "achievements have i", "achievements unlocked",
            "achievements available", "available achievements", "what achievements are",
            "connect four achievements", "connect 4 achievements", "achievements in connect",
            "achievements for connect", "achievements can i", "achievements can you"
        ]
        if any(k in m for k in achievement_keywords):
            needs.append("achievements")

        # Match history keywords
        match_history_keywords = [
            "my matches", "my games", "game history", "match history", "my game history",
            "my match history", "past games", "previous games", "recent games",
            "games i played", "matches i played", "how many games", "how many matches",
            "win rate", "win loss", "my wins", "my losses", "game stats", "match stats",
            "recent matches", "last games", "game record", "match record"
        ]
        if any(k in m for k in match_history_keywords):
            needs.append("match_history")

        # Lobby-related keywords
        lobby_keywords = [
            "my lobby", "current lobby", "lobby i'm in", "am i in a lobby", "what lobby",
            "lobby", "lobbies", "join lobby", "create lobby", "lobby status",
            "lobby details", "lobby info", "search lobbies", "find lobby"
        ]
        if any(k in m for k in lobby_keywords):
            needs.append("lobby")

        return needs

    def _fetch_user_context(self, token: Optional[str], user_id: Optional[str], kinds: List[str]) -> Tuple[str, List[str]]:
        """
        Fetch user-related data from Java backend and return:
          - formatted text to append to prompt
          - sources list describing which APIs were used
        """
        if not token:
            return "(No authentication token provided. Cannot fetch user context.)", ["api:no_token"]
        
        parts: List[str] = []
        sources: List[str] = []

        for k in kinds:
            try:
                if k == "profile":
                    data = self.profile_service.get_profile(token)
                    # Extract user_id from profile if not already available
                    if not user_id and isinstance(data, dict):
                        user_id = data.get("id")
                    parts.append(f"User profile (GET /api/platform/players):\n{data}")
                    sources.append("api:/api/platform/players")

                elif k == "friends":
                    friends = self.friend_service.get_friends(token)
                    data = {"friends": friends, "count": len(friends)} if isinstance(friends, list) else friends
                    parts.append(f"User friends (GET /api/platform/players/friends):\n{data}")
                    sources.append("api:/api/platform/players/friends")

                elif k == "friend_requests":
                    requests = self.friend_service.get_pending_friend_requests(token)
                    data = {"requests": requests, "count": len(requests)} if isinstance(requests, list) else requests
                    parts.append(f"Pending friend requests (GET /api/platform/friends?status=PENDING):\n{data}")
                    sources.append("api:/api/platform/friends?status=PENDING")

                elif k == "achievements":
                    # Always fetch all available achievements first (for information about how to get them)
                    # Only fetch if we have a valid token (endpoint requires authentication)
                    if token:
                        try:
                            all_achievements = self.achievement_service.get_all_achievements(token)
                            if all_achievements:
                                parts.append(f"All available achievements in the system (GET /api/platform/achievements):\n{all_achievements}")
                                sources.append("api:/api/platform/achievements")
                        except Exception as e:
                            error_msg = str(e)
                            print(f"[chatbot] Failed to fetch all achievements: {error_msg}")
                            # If it's a 401, the token might be invalid - try to continue with user achievements
                            if "401" not in error_msg:
                                parts.append(f"(Could not fetch all achievements: {error_msg})")
                    else:
                        print("[chatbot] No token provided, cannot fetch all achievements (endpoint requires authentication)")
                    
                    # Also fetch user's unlocked achievements if user_id is available
                    # Try to get user_id from profile if not available
                    if not user_id:
                        try:
                            profile_data = self.profile_service.get_profile(token)
                            if isinstance(profile_data, dict):
                                user_id = profile_data.get("id")
                        except Exception:
                            pass
                    
                    if user_id:
                        try:
                            user_achievements = self.achievement_service.get_achievements(user_id, token)
                            data = {"achievements": user_achievements, "count": len(user_achievements)} if isinstance(user_achievements, list) else user_achievements
                            parts.append(f"User's unlocked achievements (GET /api/platform/achievements/users/{user_id}):\n{data}")
                            sources.append("api:/api/platform/achievements/users/{userId}")
                        except Exception as e:
                            print(f"[chatbot] Failed to fetch user achievements: {e}")
                            parts.append(f"(Could not fetch user's unlocked achievements: {e})")
                    else:
                        parts.append("(User ID not available to fetch user's unlocked achievements)")
                        sources.append("api:achievements_missing_user_id")

                elif k == "match_history":
                    # Try to get user_id from profile if not available
                    if not user_id:
                        try:
                            profile_data = self.profile_service.get_profile(token)
                            if isinstance(profile_data, dict):
                                user_id = profile_data.get("id")
                        except Exception:
                            pass
                    
                    if not user_id:
                        parts.append("User match history requested, but no user_id was provided.")
                        sources.append("api:match_history_missing_user_id")
                    else:
                        data = self.game_service.get_match_history(user_id, token, limit=20)
                        parts.append(f"User match history (GET /api/v1/games/sessions/player/{user_id}/history):\n{data}")
                        sources.append("api:/api/v1/games/sessions/player/{userId}/history")

                elif k == "lobby":
                    lobby_data = self.lobby_service.get_current_lobby(token)
                    if lobby_data:
                        parts.append(f"User current lobby (GET /api/platform/lobbies/current):\n{lobby_data}")
                        sources.append("api:/api/platform/lobbies/current")
                    else:
                        parts.append("User is not currently in any lobby.")
                        sources.append("api:/api/platform/lobbies/current")

            except Exception as e:
                error_msg = str(e)
                print(f"[chatbot] Failed to load {k} from API: {error_msg}")
                parts.append(f"(Failed to load {k} from platform API: {error_msg})")
                sources.append(f"api_failed:{k}")

        return "\n\n".join(parts), sources

    # -----------------------------
    # LLM response generation
    # -----------------------------

    def _generate_llm_response(
        self,
        message: str,
        conversation_id: str,
        user_id: Optional[str],
        token: Optional[str],
    ) -> Tuple[str, List[str]]:
        if self.llm_client is None:
            # Fallback to placeholder with user context
            kinds = self._needs_user_context(message) if token else []
            user_ctx_text, user_ctx_sources = self._fetch_user_context(token, user_id, kinds) if token and kinds else ("", [])
            text = self._generate_placeholder_response(message, user_ctx_text, kinds)
            return text, ["placeholder"] + user_ctx_sources

        history_context = self._build_history_context(conversation_id)
        docs_text, rag_sources = self._select_docs_for_message(message)

        # Live user context if needed (already fetched in chat method, but keeping for backward compatibility)
        user_ctx_text = ""
        user_ctx_sources: List[str] = []
        if token:
            kinds = self._needs_user_context(message)
            if kinds:
                user_ctx_text, user_ctx_sources = self._fetch_user_context(token, user_id, kinds)

        system_prompt = (
            "You are a helpful assistant for the BanditGames Connect Four training platform. "
            "Provide clear, direct, and informative answers. Use plain text only - no markdown formatting, no emojis, no excessive enthusiasm. "
            "Use the provided documents and recent conversation context to give detailed, helpful answers. "
            "If the user asks about their profile/friends/achievements/match history/lobbies, use the provided user-specific information to give accurate, factual responses. "
            "When discussing match history, analyze their win rate, recent performance, game types played, and provide insights about their gaming patterns. "
            "When discussing lobbies, provide information about their current lobby status, available lobbies, or how to join/create lobbies. "
            "Be professional and concise. If the answer is not supported by the documents or user data, state that you don't have that information."
        )

        parts: List[str] = []
        if history_context:
            parts.append("Recent conversation:\n" + history_context)

        parts.append("Relevant documents:\n" + docs_text)

        if user_ctx_text:
            parts.append("User-specific information (from live platform APIs):\n" + user_ctx_text)

        parts.append("User question:\n" + message)
        user_prompt = "\n\n".join(parts)

        text = self.llm_client.chat(system_prompt=system_prompt, user_prompt=user_prompt)
        return text, rag_sources + user_ctx_sources

    # -----------------------------
    # Placeholder responses
    # -----------------------------

    def _generate_placeholder_response(self, message: str, user_ctx_text: str = "", context_kinds: List[str] = None, user_id: Optional[str] = None) -> str:
        message_lower = message.lower()
        context_kinds = context_kinds or []

        # Handle user context queries - check achievements FIRST before generic responses
        if "achievements" in context_kinds:
            if user_ctx_text:
                try:
                    import json
                    # Extract JSON data from the user_ctx_text (format: "User achievements (GET /api/...):\n{json}")
                    if "User achievements" in user_ctx_text and "Failed to load" not in user_ctx_text:
                        # Find the JSON part after the colon
                        json_start = user_ctx_text.find(":\n") + 2
                        if json_start > 1:
                            json_str = user_ctx_text[json_start:].strip()
                            try:
                                data = json.loads(json_str)
                                # Handle both list and dict responses
                                achievements = data if isinstance(data, list) else data.get("achievements", []) if isinstance(data, dict) else []
                                
                                # Handle empty list case
                                if isinstance(achievements, list) and len(achievements) == 0:
                                    return (
                                        "You currently don't have any unlocked achievements. "
                                        "Play games and complete milestones to unlock achievements. "
                                        "Common achievements include: First Win, Win Streak, Game Master, and Perfect Game."
                                    )
                                
                                if achievements:
                                    count = len(achievements) if isinstance(achievements, list) else 1
                                    # Format achievement names if available
                                    achievement_names = []
                                    if isinstance(achievements, list) and len(achievements) > 0:
                                        for ach in achievements:
                                            if isinstance(ach, dict):
                                                name = ach.get("name") or ach.get("achievementName") or ach.get("title") or ach.get("achievement", {}).get("name", "Achievement")
                                                achievement_names.append(f"- {name}")
                                            elif isinstance(ach, str):
                                                achievement_names.append(f"- {ach}")
                                    
                                    response = f"You have {count} unlocked achievement{'s' if count != 1 else ''}."
                                    if achievement_names:
                                        response += "\n\nYour achievements:\n" + "\n".join(achievement_names[:10])  # Limit to 10
                                        if count > 10:
                                            response += f"\n... and {count - 10} more."
                                    else:
                                        response += " Check your profile page to see the full list."
                                    
                                    return response
                            except json.JSONDecodeError as e:
                                # If JSON parsing fails, log and continue
                                print(f"[chatbot] Error parsing achievement JSON: {e}, raw: {json_str[:100]}")
                    
                    # If we have user_ctx_text but couldn't parse it properly (and it's not an error message)
                    if "Failed to load" not in user_ctx_text and "achievements_missing_user_id" not in user_ctx_text:
                        return (
                            "I can see your achievement information. "
                            "For detailed information about each achievement, check your profile page."
                        )
                except Exception as e:
                    # Log error but continue to fallback
                    print(f"[chatbot] Error processing achievement data: {e}, user_ctx_text: {user_ctx_text[:200]}")
            
            # If achievements were requested but we don't have data (API might have failed)
            # This handles the case where context_kinds contains "achievements" but user_ctx_text is empty or has an error
            if not user_ctx_text or "Failed to load" in user_ctx_text or "achievements_missing_user_id" in user_ctx_text:
                if "achievements_missing_user_id" in user_ctx_text:
                    error_msg = "I need your user ID to fetch your achievements. Please make sure you're logged in properly."
                elif "Failed to load" in user_ctx_text:
                    error_msg = "I encountered an error while fetching your achievements. Please check your profile page or try again later."
                else:
                    error_msg = "I couldn't retrieve your achievements at the moment. Please check your profile page or try again later."
                return error_msg

        if "friends" in context_kinds and user_ctx_text:
            try:
                import json
                if "friends" in user_ctx_text.lower():
                    # Extract JSON data
                    json_start = user_ctx_text.find(":\n") + 2
                    if json_start > 1:
                        json_str = user_ctx_text[json_start:].strip()
                        try:
                            data = json.loads(json_str)
                            friends = data if isinstance(data, list) else []
                            count = len(friends) if isinstance(friends, list) else 0
                            if count > 0:
                                friend_word = "friend" if count == 1 else "friends"
                                return (
                                    f"You have {count} {friend_word} on the platform. "
                                    "To see their names and details, check the Friends section of the platform."
                                )
                            else:
                                return (
                                    "You currently don't have any friends. "
                                    "You can add friends by sending friend requests to other players. "
                                    "Go to the Friends section to find and add friends."
                                )
                        except json.JSONDecodeError:
                            pass
                    return (
                        "I can see your friends information. "
                        "For the exact count and details, check the Friends section of the platform."
                    )
            except Exception:
                pass

        if "profile" in context_kinds and user_ctx_text:
            return (
                "I can see your profile information. "
                "For detailed stats and information, check the Profile section of the platform."
            )
        
        # Lobby handling
        if "lobby" in context_kinds:
            if user_ctx_text:
                try:
                    import json
                    if "User current lobby" in user_ctx_text and "Failed to load" not in user_ctx_text:
                        json_start = user_ctx_text.find(":\n") + 2
                        if json_start > 1:
                            json_str = user_ctx_text[json_start:].strip()
                            try:
                                data = json.loads(json_str)
                                lobby_name = data.get("name", "Unnamed Lobby")
                                max_players = data.get("maxPlayers", 0)
                                current_players = len(data.get("playerIds", [])) if isinstance(data.get("playerIds"), list) else 0
                                game_id = data.get("gameId")
                                is_private = data.get("isPrivate", False)
                                
                                response = f"You are currently in a lobby: {lobby_name}. "
                                response += f"Players: {current_players}/{max_players}. "
                                if game_id:
                                    response += f"Game: {game_id}. "
                                if is_private:
                                    response += "This is a private lobby."
                                else:
                                    response += "This is a public lobby."
                                
                                return response
                            except json.JSONDecodeError as e:
                                print(f"[chatbot] Error parsing lobby JSON: {e}, raw: {json_str[:100]}")
                    
                    if "User is not currently in any lobby" in user_ctx_text:
                        return "You are not currently in any lobby. You can create a new lobby or search for available lobbies in the Lobbies section."
                    
                    if "Failed to load" not in user_ctx_text:
                        return "I can see your lobby information. For more details, check the Lobbies section of the platform."
                except Exception as e:
                    print(f"[chatbot] Error processing lobby data: {e}, user_ctx_text: {user_ctx_text[:200]}")
            
            # Error handling for lobby
            if not user_ctx_text or "Failed to load" in user_ctx_text:
                return "I encountered an error while fetching your lobby information. Please try again later or check the Lobbies section."
        
        # Match history handling
        if "match_history" in context_kinds:
            if user_ctx_text:
                try:
                    import json
                    if "User match history" in user_ctx_text and "Failed to load" not in user_ctx_text:
                        json_start = user_ctx_text.find(":\n") + 2
                        if json_start > 1:
                            json_str = user_ctx_text[json_start:].strip()
                            try:
                                data = json.loads(json_str)
                                matches = data.get("matches", []) if isinstance(data, dict) else (data if isinstance(data, list) else [])
                                count = len(matches) if isinstance(matches, list) else 0
                                
                                if count == 0:
                                    return "You don't have any completed matches yet. Play some games to build your match history."
                                
                                # Calculate win rate - check if user_id is in player_ids for each match
                                wins = 0
                                if user_id:
                                    for m in matches:
                                        if isinstance(m, dict):
                                            player_ids = m.get("player_ids", [])
                                            winner_id = m.get("winner_id")
                                            if user_id in player_ids and winner_id == user_id:
                                                wins += 1
                                
                                win_rate = (wins / count * 100) if count > 0 else 0
                                
                                response = f"You have {count} completed match{'es' if count != 1 else ''}."
                                if count > 0:
                                    response += f" Win rate: {wins} wins out of {count} matches ({win_rate:.1f}%)."
                                
                                # Add recent game types if available
                                game_types = {}
                                for m in matches[:10]:  # Check last 10 matches
                                    if isinstance(m, dict):
                                        gt = m.get("game_type", "unknown")
                                        game_types[gt] = game_types.get(gt, 0) + 1
                                
                                if game_types:
                                    types_str = ", ".join([f"{count} {gt}" for gt, count in game_types.items()])
                                    response += f" Recent games: {types_str}."
                                
                                return response
                            except json.JSONDecodeError as e:
                                print(f"[chatbot] Error parsing match history JSON: {e}, raw: {json_str[:100]}")
                    
                    if "Failed to load" not in user_ctx_text:
                        return "I can see your match history information. For detailed statistics, check your profile page."
                except Exception as e:
                    print(f"[chatbot] Error processing match history data: {e}, user_ctx_text: {user_ctx_text[:200]}")
            
            # Error handling for match history
            if not user_ctx_text or "Failed to load" in user_ctx_text or "match_history_missing_user_id" in user_ctx_text:
                if "match_history_missing_user_id" in user_ctx_text:
                    return "I need your user ID to fetch your match history. Please make sure you're logged in properly."
                elif "Failed to load" in user_ctx_text:
                    return "I encountered an error while fetching your match history. Please try again later or check your profile page."
                else:
                    return "I couldn't retrieve your match history at the moment. Please try again later."

        # Game-related queries
        if "connect four" in message_lower or "connect 4" in message_lower or "win" in message_lower:
            if "win" in message_lower and ("connect" in message_lower or "4" in message_lower):
                return (
                    "To win Connect Four, you need to be the first player to form a horizontal, vertical, "
                    "or diagonal line of four of your colored discs. Here are some strategies:\n\n"
                    "1. **Block your opponent**: Always watch for your opponent's potential winning moves and block them.\n"
                    "2. **Create multiple threats**: Try to set up situations where you have multiple ways to win.\n"
                    "3. **Control the center**: The center columns (3, 4) are often more valuable as they offer more winning combinations.\n"
                    "4. **Think ahead**: Plan 2-3 moves ahead to anticipate your opponent's responses.\n"
                    "5. **Watch for traps**: Be careful not to set up your opponent for an easy win."
                )
            return (
                "Connect Four is a two-player connection game. Players take turns dropping colored "
                "discs into a 6x7 grid. The goal is to be the first to form a horizontal, vertical, "
                "or diagonal line of four of your discs. The game ends when a player wins or the board is full (draw)."
            )

        if "rules" in message_lower:
            return (
                "I can help explain the rules of Connect Four and other games on the platform. "
                "What specific game would you like to know about?"
            )

        if "how to play" in message_lower:
            return (
                "To play Connect Four: 1) Choose a column (0-6), 2) Your disc drops to the lowest empty space, "
                "3) Try to get four in a row (horizontal, vertical, or diagonal), 4) Block your opponent from winning."
            )

        # Generic achievement info - only if not already handled by user context
        if ("achievement" in message_lower or "achievements" in message_lower) and "achievements" not in context_kinds:
            return (
                "Achievements are rewards you earn by completing various milestones in the platform. "
                "Common achievements include:\n"
                "- **First Win**: Win your first game\n"
                "- **Win Streak**: Win multiple games in a row\n"
                "- **Game Master**: Play a certain number of games\n"
                "- **Perfect Game**: Win without your opponent making a move\n\n"
                "To see your specific achievements and how to unlock them, ask me: 'What achievements do I have?' "
                "or 'Show me my achievements'. You can also check your achievements in your profile page."
            )

        if "platform" in message_lower or "help" in message_lower:
            return (
                "I can help you with: game rules, platform navigation, achievements, friends, and gameplay tips. "
                "What would you like to know?"
            )

        return (
            "I'm here to help with game rules and platform questions. "
            "You can ask me about Connect Four rules, how to play, achievements, friends, or general platform help. "
            "What would you like to know?"
        )

    def get_conversation_history(self, conversation_id: str) -> ConversationHistory:
        messages = self.conversations.get(conversation_id, [])
        return ConversationHistory(conversation_id=conversation_id, messages=messages)
