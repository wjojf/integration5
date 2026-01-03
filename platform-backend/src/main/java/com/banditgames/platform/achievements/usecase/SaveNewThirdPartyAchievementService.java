package com.banditgames.platform.achievements.usecase;

import com.banditgames.platform.achievements.domain.Achievement;
import com.banditgames.platform.achievements.port.in.SaveNewThirdPartyAchievementUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaveNewThirdPartyAchievementService implements SaveNewThirdPartyAchievementUseCase {

//    private final SaveAchievementPort saveAchievementPort;

    @Override
    @Transactional
    public Achievement SaveNewThirdPartyAchievement(SaveNewThirdPartyAchievementRecord record) {
//        saveAchievementPort.save();
        return null;
    }
}
