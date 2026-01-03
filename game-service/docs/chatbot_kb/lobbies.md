# Game Lobbies

This document describes the lobby system used to organize multiplayer games.

## Create Lobby

Endpoint:
POST /api/lobbies

Description:
Creates a new lobby.
The authenticated user becomes the host and automatically joins.

## Join Lobby

Endpoint:
POST /api/lobbies/{lobbyId}/join

Description:
Join an existing lobby.
Private lobbies require an invitation.

## Leave Lobby

Endpoint:
POST /api/lobbies/{lobbyId}/leave

Description:
Leaves a lobby.
If the host leaves, the lobby is cancelled.

## Invite to Lobby

Endpoint:
POST /api/lobbies/{lobbyId}/invite

Description:
Invites a friend to a private lobby.
Only the host can invite players.

## Search Lobbies

Endpoint:
GET /api/lobbies/search

Filters:
- gameId
- host username (partial match)

Returns only public lobbies that are in WAITING state.

## Typical chatbot questions

- "How do I create a lobby?"
- "How do I invite a friend to a lobby?"
- "How can I find public lobbies?"
- "Why can't I join a lobby?"
