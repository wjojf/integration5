# BanditGames Frontend

Modern gaming platform frontend built with React, TypeScript, and TanStack Query.

## Features

- User authentication with Keycloak
- Game library (including Connect-4)
- Real-time chat with WebSocket
- Friend management
- Lobby system
- Player profiles

## Tech Stack

- React 18 + TypeScript
- TanStack Query (React Query)
- Tailwind CSS
- Vite
- Keycloak for authentication
- WebSocket (STOMP over SockJS)

## Setup

Install dependencies:
```bash
npm install
```

Create `.env.local`:
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
VITE_KC_URL=http://localhost:8080
VITE_KC_REALM=banditgames
VITE_KC_CLIENT_ID=platform-client
```

## Development

Start dev server:
```bash
npm run dev
```

Access at `http://localhost:5173`

## Build

```bash
npm run build
```

Preview production build:
```bash
npm run preview
```

