import { Routes, Route, Navigate } from "react-router-dom";
import { useKeycloak } from '@react-keycloak/web';

import { ApiClient } from './lib/app/api.client'
import { PrivateRoutes } from "./routes/PrivateRoutes"
import { AdminRoutes } from "./routes/AdminRoutes";
import { Landing } from "./pages/landing/Landing";
import { ConnectFour } from "./pages/connectFour/ConnectFour";
import { Platform } from "./pages/app/Platform";
import { GameLibrary } from "./pages/app/GameLibrary"
import { GamePage } from "./pages/game/GamePage"
import { FriendsChat } from "./pages/app/FriendsChat"
import { Friends } from "./pages/app/Friends"
import { Achievements } from "./pages/app/Achievements"
import { Profile } from "./pages/app/Profile"
import { Lobbies } from "./pages/app/Lobbies"
import { LobbyDetail } from "./pages/app/LobbyDetail"
import { Players } from "./pages/app/Players"
import { Analytics } from "./pages/app/Analytics"
import { ChatWebSocketProvider } from "./contexts/ChatWebSocketContext"


export default function App() {
    const { keycloak, initialized } = useKeycloak();

    initialized && ApiClient.setKeycloakInstance(keycloak)

    return (
        <ChatWebSocketProvider>
            <Routes>
                <Route path="/" element={<Landing/>}/>

                <Route element={<PrivateRoutes/>}>
                    <Route path="/app" element={<Platform/>}>
                        <Route index element={<Navigate to="games" replace/>} />
                        <Route path="games" element={<GameLibrary />} />
                        <Route path="lobbies" element={<Lobbies />} />
                        <Route path="lobbies/:lobbyId" element={<LobbyDetail />} />
                        <Route path="players" element={<Players />} />
                        <Route path="chat" element={<FriendsChat />} />
                        <Route path="friends" element={<Friends />} />
                        <Route path="achievements" element={<Achievements />} />
                        <Route path="profile" element={<Profile />} />
                        <Route path="game/:lobbyId" element={<GamePage />} />
                        <Route path="game/ai/:sessionId" element={<GamePage />} />
                        <Route element={<AdminRoutes />}>
                            <Route path="analytics" element={<Analytics />} />
                        </Route>
                    </Route>

                    <Route path="/game/connect4" element={<ConnectFour/>}/>
                </Route>

                <Route path="*" element={<Navigate to="/" replace/>}/>
            </Routes>
        </ChatWebSocketProvider>
    );
}

