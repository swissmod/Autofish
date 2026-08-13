package com.yourserver.autofish;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.random.Random;

public final class AntiAfkLogic {

    private enum ActionType { SNEAK, JUMP, LOOK }

    private final AutoFishConfig config;
    private final Random random = Random.create();

    private int ticksUntilNextAction;
    private int actionTicksRemaining = 0;
    private ActionType currentAction = null;

    private static final int SNEAK_DURATION_TICKS = 60; // 3 seconds
    private static final int JUMP_DURATION_TICKS = 40;  // 2 seconds, holds the key so it bunny-hops

    private float lookStartYaw;
    private float lookStartPitch;
    private float lookTargetYawOffset;
    private float lookTargetPitchOffset;
    private static final int LOOK_TURN_TICKS = 10;
    private static final int LOOK_HOLD_TICKS = 8;
    private static final int LOOK_RETURN_TICKS = 10;
    private int lookPhaseTicksRemaining;
    private int lookPhase;

    private static final int MIN_INTERVAL_MS = 5000;
    private static final int MAX_INTERVAL_MS = 10000;

    public AntiAfkLogic(AutoFishConfig config) {
        this.config = config;
        scheduleNextAction();
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void scheduleNextAction() {
        int ms = MIN_INTERVAL_MS + random.nextInt(MAX_INTERVAL_MS - MIN_INTERVAL_MS + 1);
        ticksUntilNextAction = Math.max(1, Math.round(ms / 50.0f));
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        if (!config.enabled || !config.antiAfkEnabled || client.currentScreen != null) {
            if (currentAction == ActionType.SNEAK) {
                client.options.sneakKey.setPressed(false);
            }
            if (currentAction == ActionType.JUMP) {
                client.options.jumpKey.setPressed(false);
            }
            currentAction = null;
            return;
        }

        if (currentAction != null) {
            runCurrentAction(client);
            return;
        }

        if (--ticksUntilNextAction <= 0) {
            startRandomAction(client);
        }
    }

    private void startRandomAction(MinecraftClient client) {
        ActionType[] options = ActionType.values();
        currentAction = options[random.nextInt(options.length)];

        switch (currentAction) {
            case SNEAK -> {
                client.options.sneakKey.setPressed(true);
                actionTicksRemaining = SNEAK_DURATION_TICKS;
            }
            case JUMP -> {
                client.options.jumpKey.setPressed(true);
                actionTicksRemaining = JUMP_DURATION_TICKS;
            }
            case LOOK -> {
                lookStartYaw = client.player.getYaw();
                lookStartPitch = client.player.getPitch();
                lookTargetYawOffset = (random.nextBoolean() ? 1 : -1) * (12f + random.nextInt(26));
                lookTargetPitchOffset = (random.nextBoolean() ? 1 : -1) * random.nextInt(6);
                lookPhase = 0;
                lookPhaseTicksRemaining = LOOK_TURN_TICKS;
            }
        }
    }

    private void runCurrentAction(MinecraftClient client) {
        switch (currentAction) {
            case SNEAK -> {
                if (--actionTicksRemaining <= 0) {
                    client.options.sneakKey.setPressed(false);
                    finishAction();
                }
            }
            case JUMP -> {
                if (--actionTicksRemaining <= 0) {
                    client.options.jumpKey.setPressed(false);
                    finishAction();
                }
            }
            case LOOK -> runLookAction(client);
        }
    }

    private float ease(float t) {
        return t * t * (3f - 2f * t);
    }

    private void runLookAction(MinecraftClient client) {
        lookPhaseTicksRemaining--;
        float rawProgress;
        float eased;
        switch (lookPhase) {
            case 0 -> {
                rawProgress = 1f - (lookPhaseTicksRemaining / (float) LOOK_TURN_TICKS);
                eased = ease(rawProgress);
                client.player.setYaw(lookStartYaw + lookTargetYawOffset * eased);
                client.player.setPitch(lookStartPitch + lookTargetPitchOffset * eased);
                if (lookPhaseTicksRemaining <= 0) {
                    lookPhase = 1;
                    lookPhaseTicksRemaining = LOOK_HOLD_TICKS;
                }
            }
            case 1 -> {
                if (lookPhaseTicksRemaining <= 0) {
                    lookPhase = 2;
                    lookPhaseTicksRemaining = LOOK_RETURN_TICKS;
                }
            }
            case 2 -> {
                rawProgress = 1f - (lookPhaseTicksRemaining / (float) LOOK_RETURN_TICKS);
                eased = ease(rawProgress);
                client.player.setYaw(lookStartYaw + lookTargetYawOffset * (1f - eased));
                client.player.setPitch(lookStartPitch + lookTargetPitchOffset * (1f - eased));
                if (lookPhaseTicksRemaining <= 0) {
                    client.player.setYaw(lookStartYaw);
                    client.player.setPitch(lookStartPitch);
                    finishAction();
                }
            }
        }
    }

    private void finishAction() {
        currentAction = null;
        scheduleNextAction();
    }
}
