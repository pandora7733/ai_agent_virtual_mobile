package com.sihoo.ai_agent_virtual_mobile.character;

public interface PetRepository {
    boolean isFirstVisit();

    void markFirstVisitCompleted();

    void saveOutfit(OutfitType outfitType);

    OutfitType getOutfit();

    void saveLastActivityAt(long epochMillis);

    long getLastActivityAt();
}
