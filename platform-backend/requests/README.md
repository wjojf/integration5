# HTTP Requests Documentation

This folder contains organized HTTP request files for testing all API endpoints in the BanditGames Platform Backend.

## File Structure

```
requests/
├── README.md               # This file
├── auth.http               # Keycloak authentication endpoints
├── players.http            # Player management endpoints
├── lobbies.http            # Lobby management endpoints
├── friends.http            # Friends & friendship requests endpoints
├── chat.http               # Chat & messaging endpoints
├── chess-game.http         # Chess game ACL integration endpoints
└── health.http             # Health check & actuator endpoints
```

## Quick Start

### 1. Prerequisites
- IntelliJ IDEA or VS Code with REST Client extension
- Docker Compose running (Keycloak + Backend services)

### 2. Start Services
```bash
cd deploy
docker-compose up -d
```

### 3. Get Authentication Token
1. Open `auth.http`
2. Run "Get Access Token for Admin User" or "Get Access Token for Regular User"
3. Tokens will be automatically saved to `{{adminToken}}` and `{{userToken}}`

### 4. Test Endpoints
Open any domain file and click "Run" next to the request you want to test.

## Available Domains

### Authentication (`auth.http`)
- Get access tokens (admin & user)
- Refresh tokens
- Get user info
- Logout

**Users:**
- Admin: `admin` / `password` (roles: admin, user)
- User1: `user1` / `password` (role: user)

### Players (`players.http`)
**5 endpoints** - Player profile management
- Get own profile
- Get player by ID
- Update profile (username, bio, address, game preferences)
- Search players (by username, rank)
- Delete profile

### Lobbies (`lobbies.http`)
**7 endpoints** - Game lobby management
- Create lobby (public/private, 2-10 players)
- Join/leave lobby
- Start lobby with game
- Invite friends to private lobby
- Get lobby details
- Search lobbies (by game, username)

### Friends (`friends.http`)
**3 endpoints** - Friend system
- Send friend request
- Get friends by status (PENDING, ACCEPTED, REJECTED, BLOCKED)
- Accept/reject/block requests

### Chat (`chat.http`)
**3 endpoints** - Messaging system
- Send message (1-2000 characters)
- Get conversation history (paginated)
- Get all conversation partners

### Chess Game (`chess-game.http`)
**2 endpoints** - ACL integration (no auth required)
- Register chess game
- Get chess game information

### Health (`health.http`)
**3 endpoints** - Service health monitoring
- Health check
- Liveness probe
- Readiness probe

## Authentication

Most endpoints require Bearer token authentication. The token is automatically set after running authentication requests.

**Secured endpoints:** 24/27
**Open endpoints:** 3 (Chess game ACL)

### Token Usage Example
```http
GET {{baseUrl}}/api/players
Authorization: Bearer {{userToken}}
```

## Endpoint Statistics

| Domain | Endpoints | Auth Required |
|--------|-----------|---------------|
| Players | 5 | Yes |
| Lobbies | 7 | Yes |
| Friends | 3 | Yes |
| Chat | 3 | Yes |
| Chess Game | 2 | No |
| Health | 3 | No |
| **Total** | **27** | **24/27** |

## Environment Variables

Variables are defined at the top of each `.http` file:
- `{{baseUrl}}` - Backend API base URL (default: http://localhost:8081)
- `{{keycloakUrl}}` - Keycloak URL (default: http://localhost:8180)
- `{{realm}}` - Keycloak realm name (default: banditgames)
- `{{clientId}}` - OAuth client ID (default: platform-frontend)

You can modify these values in each file to match your environment.

## Response Variables

Some requests automatically save response data to variables:
- `{{adminToken}}` - Admin user's access token
- `{{userToken}}` - Regular user's access token
- `{{lobbyId}}` - Created lobby ID
- `{{friendshipId}}` - Created friendship request ID
- `{{chessGameId}}` - Registered chess game ID

## Request Body Examples

### Player Update
```json
{
  "username": "newUsername",
  "bio": "My bio",
  "address": "123 Street",
  "gamePreferences": ["uuid1", "uuid2"]
}
```

### Create Lobby
```json
{
  "maxPlayers": 4,
  "isPrivate": false
}
```

### Send Message
```json
{
  "receiverId": "uuid",
  "content": "Hello!"
}
```

## Tips

1. **Run auth requests first** - Always authenticate before testing secured endpoints
2. **Use saved variables** - Many requests save IDs automatically for reuse
3. **Check responses** - Each request includes expected response format in comments
4. **Test different users** - Switch between `{{adminToken}}` and `{{userToken}}` to test permissions
5. **Pagination** - Chat conversations support pagination (page, size parameters)

## Search & Filter Examples

**Players:**
- By username: `?username=pro`
- By rank: `?rank=GOLD` (BRONZE, SILVER, GOLD, PLATINUM, DIAMOND)
- Both: `?username=pro&rank=PLATINUM`

**Lobbies:**
- By game: `?gameId=uuid`
- By username: `?username=user1`
- Both: `?gameId=uuid&username=user1`

**Friends:**
- By status: `?status=ACCEPTED` (PENDING, ACCEPTED, REJECTED, BLOCKED)

**Chat:**
- Pagination: `?page=0&size=20` (max size: 50)

## Troubleshooting

### 401 Unauthorized
- Token expired - Run refresh token request
- Missing token - Run authentication request first

### 403 Forbidden
- Wrong user role - Use admin token for admin endpoints
- Resource not owned - Can only modify your own resources

### 404 Not Found
- Invalid UUID - Check the ID format
- Resource deleted - Verify resource exists

## Additional Resources

- Swagger UI: `http://localhost:8081/swagger-ui.html` (when enabled)
- Keycloak Admin: `http://localhost:8180` (admin/admin)
- API Documentation: OpenAPI 3.1.0 specification
