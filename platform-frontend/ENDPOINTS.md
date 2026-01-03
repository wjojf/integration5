# Platform Frontend Endpoint List

This document lists all the API endpoints used in the frontend application, categorized by feature.

## Player Service
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/platform/players` | Get current player profile |
| `PATCH` | `/api/platform/players` | Update player profile |
| `POST` | `/api/platform/players/exp` | Increase player experience points |
| `GET` | `/api/platform/players/search` | Search for players by criteria |

## Friends Service
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/platform/friends` | Get all friendships |
| `GET` | `/api/platform/friends?status=PENDING` | Get pending friend requests |
| `GET` | `/api/platform/friends?status=ACCEPTED` | Get accepted friends |
| `POST` | `/api/platform/friends/requests` | Send friend request |
| `PATCH` | `/api/platform/friends/{friendshipId}` | Modify friend request (accept, reject) |
| `DELETE` | `/api/platform/friends/{friendshipId}` | Remove friend or friend request |

## Lobby Service
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/platform/lobbies/search` | Get all lobbies with optional filters |
| `GET` | `/api/platform/lobbies/current` | Get current player's lobby |
| `GET` | `/api/platform/lobbies/{lobbyId}` | Get lobby by ID |
| `POST` | `/api/platform/lobbies` | Create a new lobby |
| `POST` | `/api/platform/lobbies/{lobbyId}/join` | Join an existing lobby |
| `DELETE` | `/api/platform/lobbies/{lobbyId}/leave` | Leave a lobby |
| `POST` | `/api/platform/lobbies/{lobbyId}/start` | Start a lobby game |
| `PATCH` | `/api/platform/lobbies/{lobbyId}/ready` | Mark player as ready in lobby |
| `PATCH` | `/api/platform/lobbies/{lobbyId}/invite` | Invite player to lobby |
| `DELETE` | `/api/platform/lobbies/{lobbyId}` | Cancel a lobby |

## Chat Service
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/platform/chat/conversations` | Get all conversations for current user |
| `GET` | `/api/platform/chat/conversations/{userId}` | Get conversation messages with a specific friend |
| `GET` | `/api/platform/chat/messages/unread/count` | Get unread message count |
| `POST` | `/api/platform/chat/messages` | Send a message |
| `PATCH` | `/api/platform/chat/messages/{messageId}/read` | Mark message as read |

## ML Service (Planned/Mock)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/ml/predict/policy` | Get next move prediction (Policy Imitation) |
| `POST` | `/ml/predict/win-probability` | Get win probability prediction |
| `GET` | `/ml/metrics` | Get model performance metrics |
