package com.banditgames.platform.friends.domain.exception;

public class FriendshipNotFoundException extends RuntimeException {
    
    public FriendshipNotFoundException(String message) {
        super(message);
    }
    
    public FriendshipNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

