package com.banditgames.platform.lobby.domain.exception;

public class LobbyOperationException extends RuntimeException {
    
    public LobbyOperationException(String message) {
        super(message);
    }
    
    public LobbyOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

