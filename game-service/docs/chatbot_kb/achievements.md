# Achievements System

This document describes how achievements work on the BanditGames platform.

## All Achievements

Endpoint:
GET /api/achievements

Description:
Returns all achievements available on the platform.
Can optionally be filtered by gameId.

Each achievement includes:
- id
- gameId
- name
- description
- category
- rarity (COMMON, RARE, etc.)

## Achievements Per Game

Endpoint:
GET /api/achievements/games/{gameId}

Description:
Returns all achievements available for a specific game.

## User Achievements

Endpoint:
GET /api/achievements/users/{userId}

Optional query parameter:
- gameId

Description:
Returns all achievements unlocked by a specific user.
Includes unlock timestamps.

This endpoint is relevant for chatbot questions like:
- "What achievements have I unlocked?"
- "Show my achievements"
- "What badges do I have?"
- "What achievements did I earn in this game?"
