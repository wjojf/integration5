# Player Profile (Players API)

This document describes how player profiles work on the BanditGames platform.

## Authenticated Player Profile

Endpoint:
GET /api/players

Description:
Returns the full profile of the currently authenticated player.
This includes private fields such as email and address.

Returned data includes:
- playerId (UUID)
- username
- bio
- gamePreferences (list of game IDs)
- email
- address
- rank (BRONZE, SILVER, GOLD, PLATINUM, DIAMOND)
- exp (experience points)

This endpoint is typically used when a player asks questions about:
- "my profile"
- "my rank"
- "my stats"
- "my experience"
- "my account details"

## Public Player Profile

Endpoint:
GET /api/players/{playerId}

Description:
Returns the public profile of another player.
Private fields such as email and address are excluded.

Returned data includes:
- playerId
- username
- bio
- gamePreferences
- rank
- exp

## Player Friends (Profiles)

Endpoint:
GET /api/players/friends

Description:
Returns profile information for all friends of the authenticated player.

This endpoint is relevant for questions like:
- "who are my friends"
- "show my friends"
- "what rank are my friends"
