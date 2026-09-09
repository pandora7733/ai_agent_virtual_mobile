package com.sihoo.ai_agent_virtual_mobile.character;

import android.content.Context;

public final class PetRepositories {
    private static PetRepository instance;

    private PetRepositories() {
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesPetRepository(context);
        }
    }

    public static synchronized PetRepository get() {
        if (instance == null) {
            throw new IllegalStateException("PetRepository is not initialized");
        }
        return instance;
    }

    public static synchronized void resetForTests() {
        instance = null;
    }
}
