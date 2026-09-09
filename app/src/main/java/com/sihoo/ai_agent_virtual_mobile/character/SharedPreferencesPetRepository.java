package com.sihoo.ai_agent_virtual_mobile.character;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesPetRepository implements PetRepository {
    private static final String PREF_NAME = "pet_preferences";
    private static final String KEY_FIRST_VISIT_COMPLETED =
            "first_visit_completed";
    private static final String KEY_OUTFIT = "outfit_type";
    private static final String KEY_LAST_ACTIVITY_AT = "last_activity_at";

    private final SharedPreferences preferences;

    public SharedPreferencesPetRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    @Override
    public boolean isFirstVisit() {
        return !preferences.getBoolean(KEY_FIRST_VISIT_COMPLETED, false);
    }

    @Override
    public void markFirstVisitCompleted() {
        preferences.edit()
                .putBoolean(KEY_FIRST_VISIT_COMPLETED, true)
                .apply();
    }

    @Override
    public void saveOutfit(OutfitType outfitType) {
        if (outfitType == null) {
            return;
        }

        preferences.edit()
                .putString(KEY_OUTFIT, outfitType.name())
                .apply();
    }

    @Override
    public OutfitType getOutfit() {
        String name = preferences.getString(
                KEY_OUTFIT,
                OutfitType.DEFAULT.name()
        );

        try {
            return OutfitType.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return OutfitType.DEFAULT;
        }
    }

    @Override
    public void saveLastActivityAt(long epochMillis) {
        preferences.edit()
                .putLong(KEY_LAST_ACTIVITY_AT, epochMillis)
                .apply();
    }

    @Override
    public long getLastActivityAt() {
        return preferences.getLong(KEY_LAST_ACTIVITY_AT, 0L);
    }
}
