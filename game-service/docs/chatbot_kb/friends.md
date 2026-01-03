# Friends & Friend Requests

This document describes the friendship system in the BanditGames platform.

## Send Friend Request

Endpoint:
POST /api/friends/requests

Description:
Sends a friend request from the authenticated user to another user.

Rules:
- You cannot send a friend request to yourself
- Only one request can exist between two users
- The initial status is PENDING

## Get Friends by Status

Endpoint:
GET /api/friends?status={STATUS}

Valid statuses:
- PENDING
- ACCEPTED
- REJECTED
- BLOCKED

This endpoint is used to retrieve:
- incoming friend requests
- accepted friends
- blocked users

## Modify Friend Request

Endpoint:
PATCH /api/friends/requests/{friendshipId}

Actions:
- ACCEPT
- REJECT
- BLOCK

Only the addressee (receiver) of a friend request can modify it.

## Typical chatbot questions answered using this data

- "Do I have any friend requests?"
- "Who are my accepted friends?"
- "How do I block a user?"
- "How do friend requests work?"
