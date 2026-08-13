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

    private static final int SNEAK_DURATION_TICKS = 12; // 0.6s

    private float lookStartYaw;
    private float lookStartPitch;
    private float lookTargetYawOffset;
    private static final int LOOK_TURN_TICKS = 8;
    private static final int LOOK_HOLD_TICKS = 6;
    private static final int LOOK_RETURN_TICKS = 8;
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
                client.player.setSneaking(false);
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
                client.player.setSneaking(true);
                actionTicksRemaining = SNEAK_DURATION_TICKS;
            }
            case JUMP -> {
                client.options.jumpKey.setPressed(true);
                actionTicksRemaining = 2;
            }
            case LOOK -> {
                lookStartYaw = client.player.getYaw();
                lookStartPitch = client.player.getPitch();
                lookTargetYawOffset = (random.nextBoolean() ? 1 : -1) * (15f + random.nextInt(20));
                lookPhase = 0;
                lookPhaseTicksRemaining = LOOK_TURN_TICKS;
            }
        }
    }

    private void runCurrentAction(MinecraftClient client) {
        switch (currentAction) {
            case SNEAK -> {
                if (--actionTicksRemaining <= 0) {
                    client.player.setSneaking(false);
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

    private void runLookAction(MinecraftClient client) {
        lookPhaseTicksRemaining--;
        float progress;
        switch (lookPhase) {
            case 0 -> {
                progress = 1f - (lookPhaseTicksRemaining / (float) LOOK_TURN_TICKS);
                client.player.setYaw(lookStartYaw + lookTargetYawOffset * progress);
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
                progress = 1f - (lookPhaseTicksRemaining / (float) LOOK_RETURN_TICKS);
                client.player.setYaw(lookStartYaw + lookTargetYawOffset * (1f - progress));
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
