package com.sihoo.ai_agent_virtual_mobile.character;

public final class PetSession {
    private static CharacterStateController controller;

    private PetSession() {
    }

    public static void bind(CharacterStateController stateController) {
        controller = stateController;
    }

    public static void unbind() {
        controller = null;
    }

    public static boolean applyOutfit(OutfitType outfitType) {
        if (controller == null) {
            return false;
        }
        return controller.applyOutfit(outfitType);
    }

    public static CharacterState getCurrentState() {
        if (controller == null) {
            return CharacterState.LOADING;
        }
        return controller.getCurrentState();
    }

    public static OutfitType getCurrentOutfit() {
        if (controller == null) {
            return OutfitType.DEFAULT;
        }
        return controller.getCurrentOutfit();
    }
}
