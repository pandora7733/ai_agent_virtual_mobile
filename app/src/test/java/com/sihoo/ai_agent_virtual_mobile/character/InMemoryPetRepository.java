package com.sihoo.ai_agent_virtual_mobile.character;

final class InMemoryPetRepository implements PetRepository {
    private boolean firstVisit = true;
    private OutfitType outfit = OutfitType.DEFAULT;
    private long lastActivityAt = 0L;

    @Override
    public boolean isFirstVisit() {
        return firstVisit;
    }

    @Override
    public void markFirstVisitCompleted() {
        firstVisit = false;
    }

    @Override
    public void saveOutfit(OutfitType outfitType) {
        if (outfitType != null) {
            outfit = outfitType;
        }
    }

    @Override
    public OutfitType getOutfit() {
        return outfit;
    }

    @Override
    public void saveLastActivityAt(long epochMillis) {
        lastActivityAt = epochMillis;
    }

    @Override
    public long getLastActivityAt() {
        return lastActivityAt;
    }
}
