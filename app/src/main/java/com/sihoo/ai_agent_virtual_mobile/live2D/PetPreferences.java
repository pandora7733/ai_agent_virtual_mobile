package com.sihoo.ai_agent_virtual_mobile.live2D;

import android.content.Context;
import android.content.SharedPreferences;

public class PetPreferences {
    private static final String PREF_NAME = "pet_preferences";
    private static final String KEY_FIRST_VISIT_COMPLETED =
            "first_visit_completed";
    private static final String KEY_OUTFIT = "outfit_type";

    private final SharedPreferences preferences;

    public PetPreferences(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public boolean isFirstVisit() {
        return !preferences.getBoolean(
                KEY_FIRST_VISIT_COMPLETED,
                false
        );
    }

    public void markFirstVisitCompleted() {
        preferences.edit()
                .putBoolean(KEY_FIRST_VISIT_COMPLETED, true)
                .apply();
    }

    public void saveOutfit(LAppMinimumLive2DManager.OutfitType outfitType) {
        if (outfitType == null) {
            return;
        }

        preferences.edit()
                .putString(KEY_OUTFIT, outfitType.name())
                .apply();
    }

    public LAppMinimumLive2DManager.OutfitType getOutfit() {
        String name = preferences.getString(
                KEY_OUTFIT,
                LAppMinimumLive2DManager.OutfitType.DEFAULT.name()
        );

        try {
            return LAppMinimumLive2DManager.OutfitType.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return LAppMinimumLive2DManager.OutfitType.DEFAULT;
        }
    }
}