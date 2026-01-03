package com.banditgames.platform.achievements.adapter.messaging.messages;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class AchievementAcquiredMessage {
    private UUID gameId;
    private UUID playerId;
    private String playerName;
    private String achievementType; //FIRST_BLOOD
    private String achievementDescription;
    private String messageType; //ACHIEVEMENT_AQUIRED
    private Date timestamp;
}
