package com.sihoo.ai_agent_virtual_mobile.character;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CharacterStateControllerTest {
    private FakeCharacterMotionPlayer player;
    private InMemoryPetRepository repository;
    private CharacterStateController controller;

    @Before
    public void setUp() {
        player = new FakeCharacterMotionPlayer();
        repository = new InMemoryPetRepository();
        controller = new CharacterStateController(
                player,
                repository,
                new CharacterTimings(1.0f, 2.0f, 3.0f)
        );
    }

    @Test
    public void modelReadyStartsAppearanceThenIdle() {
        repository.saveOutfit(OutfitType.OUTFIT);
        controller.onModelReady();

        assertEquals(CharacterState.FIRST_VISIT, controller.getCurrentState());
        assertEquals("appearance", player.lastStarted);
        assertEquals(OutfitType.OUTFIT, player.appliedOutfit);
        assertTrue(controller.onUserActivity());
        assertEquals(CharacterState.FIRST_VISIT, controller.getCurrentState());

        player.appearanceFinished = true;
        controller.update(0.016f);

        assertEquals(CharacterState.IDLE, controller.getCurrentState());
        assertFalse(repository.isFirstVisit());
        assertTrue(player.idleEffectsEnabled);
        assertTrue(repository.getLastActivityAt() > 0L);
    }

    @Test
    public void idleBecomesBoredThenSleep() {
        enterIdle();

        advance(1.0f);
        assertEquals(CharacterState.BORED, controller.getCurrentState());
        assertEquals("bored", player.lastStarted);

        player.boredFinished = true;
        advance(1.0f);
        assertEquals(CharacterState.SLEEP_ENTRY, controller.getCurrentState());

        player.sleepEntryFinished = true;
        controller.update(0.016f);
        assertEquals(CharacterState.SLEEP, controller.getCurrentState());
        assertEquals("sleepLoop", player.lastStarted);
    }

    @Test
    public void userActivityWakesFromSleep() {
        enterIdle();
        advance(1.0f);
        player.boredFinished = true;
        advance(1.0f);
        player.sleepEntryFinished = true;
        controller.update(0.016f);
        assertEquals(CharacterState.SLEEP, controller.getCurrentState());

        assertTrue(controller.onUserActivity());
        assertEquals(CharacterState.WAKING, controller.getCurrentState());

        player.wakeFinished = true;
        controller.update(0.016f);
        assertEquals(CharacterState.IDLE, controller.getCurrentState());
    }

    @Test
    public void screenShownRestartsAppearance() {
        enterIdle();
        controller.onScreenHidden();
        controller.onScreenShown();

        assertEquals(CharacterState.FIRST_VISIT, controller.getCurrentState());
        assertEquals("appearance", player.lastStarted);

        controller.onScreenShown();
        assertEquals(CharacterState.FIRST_VISIT, controller.getCurrentState());
    }

    @Test
    public void applyOutfitPersistsAndBodyDoubleTapKeepsIdleAfterFinish() {
        enterIdle();
        assertTrue(controller.applyOutfit(OutfitType.JACKET_OFF));
        assertEquals(OutfitType.JACKET_OFF, repository.getOutfit());
        assertEquals(OutfitType.JACKET_OFF, controller.getCurrentOutfit());

        controller.onBodyDoubleTap(true);
        assertEquals(CharacterState.BODY_DOUBLE_TAP, controller.getCurrentState());
        assertTrue(player.surprisedStarted);
        assertEquals("bodyDoubleTapChest", player.lastStarted);

        player.bodyDoubleTapFinished = true;
        controller.update(0.016f);
        assertEquals(CharacterState.IDLE, controller.getCurrentState());
        assertTrue(player.faceCleared);
        assertEquals(OutfitType.JACKET_OFF, controller.getCurrentOutfit());
    }

    @Test
    public void headPatThenFinishReturnsToIdle() {
        enterIdle();
        controller.onHeadPat(0.1f, 0.2f);
        assertEquals(CharacterState.HEAD_PAT, controller.getCurrentState());

        player.headPatFinished = true;
        controller.update(0.016f);
        assertEquals(CharacterState.IDLE, controller.getCurrentState());
    }

    private void enterIdle() {
        controller.onModelReady();
        player.appearanceFinished = true;
        controller.update(0.016f);
        assertEquals(CharacterState.IDLE, controller.getCurrentState());
    }

    private void advance(float seconds) {
        int steps = Math.max(1, Math.round(seconds / 0.1f));
        for (int i = 0; i < steps; i++) {
            controller.update(0.1f);
        }
    }
}
