package com.banditgames.platform.friends.domain.exception;

public class FriendshipOperationException extends RuntimeException {
    
    public FriendshipOperationException(String message) {
        super(message);
    }
    
    public FriendshipOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

