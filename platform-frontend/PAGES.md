# Application Pages Structure

This document outlines the page structure and routing of the Frontend application.

## Public Pages

| Route | Component | Description |
| :--- | :--- | :--- |
| `/` | `Landing.tsx` | The landing page of the application (Public entry point). |

## Application Pages (Protected)
*Base Route:* `/app`  
*Layout Component:* `Platform` (`src/pages/app/index.tsx`)  
*Guard:* `PrivateRoutes` (Requires Authentication)

| Sub-Route | Component | File Path | Description |
| :--- | :--- | :--- | :--- |
| `/games` | `GameLibrary` | `src/pages/games/GameLibrary.tsx` | Main dashboard showing available games to play. |
| `/lobbies` | `Lobbies` | `src/pages/lobby/Lobbies.tsx` | Browser for active game lobbies. |
| `/players` | `Players` | `src/pages/app/Players.tsx` | Search and view other players. |
| `/chat` | `FriendsChat` | `src/pages/chat/FriendsChat.tsx` | Chat interface for friends and messages. |
| `/achievements` | `Achievements` | `src/pages/achievements/Achievements.tsx` | User achievements and progress. |
| `/profile` | `ProfileEditor` | `src/pages/player/ProfileEditor.tsx` | User profile settings and editor. |
| `/ml-dashboard` | `Analytics` | `src/pages/analytics/Analytics.tsx` | **[NEW]** Machine Learning Analytics Dashboard. |

## Game Pages (Protected)
*Guard:* `PrivateRoutes`

| Route | Component | Description |
| :--- | :--- | :--- |
| `/game/connect4` | `Connect4.tsx` | The actual gameplay screen for Connect 4. |

## Directory Structure
```
src/pages/
├── achievements/
│   └── Achievements.tsx
├── analytics/
│   └── Analytics.tsx
├── app/
│   ├── index.tsx (Platform Layout)
│   └── Players.tsx
├── chat/
│   └── FriendsChat.tsx
├── games/
│   ├── Connect4.tsx
│   └── GameLibrary.tsx
├── lobby/
│   └── Lobbies.tsx
├── player/
│   └── ProfileEditor.tsx
└── Landing.tsx
```
