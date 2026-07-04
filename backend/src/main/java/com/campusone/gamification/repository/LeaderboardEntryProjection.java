package com.campusone.gamification.repository;

import java.util.UUID;

public interface LeaderboardEntryProjection {

    Long getRank();

    UUID getUserId();

    String getFullName();

    Long getTotalXpForPeriod();

    Integer getAllTimeXp();

    Integer getLevel();
}
