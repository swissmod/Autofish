package com.yourserver.autofish;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.random.Random;

public final class AntiAfkLogic {

    private enum ActionType { SNEAK, JUMP, LOOK, STRAFE }

    private final AutoFishConfig config;
    private final Random random = Random.create();

    private int ticksUntilNextAction;
    private int actionTicksRemaining = 0;
    private ActionType currentAction = null;

    private static final int SNEAK_DURATION_TICKS = 60; // 3 seconds

    private static final int JUMP_DURATION_TICKS = 40;
    private static final int JUMP_SNEAK_START_TICK = 10;
    private static final int JUMP_SNEAK_DURATION_TICKS = 20; // 1 second
    private int jumpElapsedTicks;
    private boolean jumpSneakActive;

    private float lookStartYaw;
    private float lookStartPitch;
    private float lookTargetYawOffset;
    private float lookTargetPitchOffset;
    private float lookOvershootStrength;
    private int lookTurnTicks;
    private int lookHoldTicks;
    private int lookReturnTicks;
    private int lookPhaseTicksRemaining;
    private int lookPhase;

    private int strafeRandomTicksRemaining;
    private int strafeCycleTicksRemaining;
    private int strafeMicroStepTicks;
    private boolean strafeGoingRight;
    private int strafeNetBias;
    private boolean strafeCorrecting;
    private int strafeCorrectionTicksRemaining;
    private static final int STRAFE_CORRECTION_CAP_TICKS = 40; // safety cap, 2 seconds

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
            if (currentAction != null) {
                client.options.sneakKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                currentAction = null;
            }
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
                jumpElapsedTicks = 0;
                jumpSneakActive = false;
            }
            case LOOK -> {
                lookStartYaw = client.player.getYaw();
                lookStartPitch = client.player.getPitch();

                lookTargetYawOffset = (random.nextBoolean() ? 1 : -1) * (8f + random.nextInt(34));
                lookTargetPitchOffset = (random.nextBoolean() ? 1 : -1) * (2f + random.nextInt(11));

                lookTurnTicks = 7 + random.nextInt(7);
                lookHoldTicks = 4 + random.nextInt(10);
                lookReturnTicks = 10 + random.nextInt(12);

                lookOvershootStrength = random.nextInt(6) == 0
                        ? 0f
                        : 1.0f + random.nextFloat() * 3.0f;

                lookPhase = 0;
                lookPhaseTicksRemaining = lookTurnTicks;
            }
            case STRAFE -> {
                strafeRandomTicksRemaining = 10 + random.nextInt(21);
                strafeGoingRight = random.nextBoolean();
                strafeMicroStepTicks = 2 + random.nextInt(3);
                strafeCycleTicksRemaining = strafeMicroStepTicks;
                strafeNetBias = 0;
                strafeCorrecting = false;
                setStrafeKey(client, strafeGoingRight, true);
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
            case JUMP -> runJumpAction(client);
            case LOOK -> runLookAction(client);
            case STRAFE -> runStrafeAction(client);
        }
    }

    private void runJumpAction(MinecraftClient client) {
        jumpElapsedTicks++;

        boolean shouldSneakNow = jumpElapsedTicks >= JUMP_SNEAK_START_TICK
                && jumpElapsedTicks < JUMP_SNEAK_START_TICK + JUMP_SNEAK_DURATION_TICKS;
        if (shouldSneakNow && !jumpSneakActive) {
            client.options.sneakKey.setPressed(true);
            jumpSneakActive = true;
        } else if (!shouldSneakNow && jumpSneakActive) {
            client.options.sneakKey.setPressed(false);
            jumpSneakActive = false;
        }

        if (--actionTicksRemaining <= 0) {
            client.options.jumpKey.setPressed(false);
            if (jumpSneakActive) {
                client.options.sneakKey.setPressed(false);
                jumpSneakActive = false;
            }
            finishAction();
        }
    }

    private void setStrafeKey(MinecraftClient client, boolean right, boolean pressed) {
        if (right) {
            client.options.rightKey.setPressed(pressed);
        } else {
            client.options.leftKey.setPressed(pressed);
        }
    }

    private void runStrafeAction(MinecraftClient client) {
        if (!strafeCorrecting) {
            strafeNetBias += strafeGoingRight ? 1 : -1;

            if (--strafeCycleTicksRemaining <= 0) {
                setStrafeKey(client, strafeGoingRight, false);
                strafeGoingRight = !strafeGoingRight;
                strafeMicroStepTicks = 2 + random.nextInt(3);
                strafeCycleTicksRemaining = strafeMicroStepTicks;
                setStrafeKey(client, strafeGoingRight, true);
            }

            if (--strafeRandomTicksRemaining <= 0) {
                setStrafeKey(client, strafeGoingRight, false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);

                if (strafeNetBias == 0) {
                    finishStrafe(client);
                    return;
                }
                boolean correctToRight = strafeNetBias < 0;
                strafeCorrectionTicksRemaining = Math.min(Math.abs(strafeNetBias), STRAFE_CORRECTION_CAP_TICKS);
                strafeCorrecting = true;
                setStrafeKey(client, correctToRight, true);
                strafeGoingRight = correctToRight;
            }
            return;
        }

        if (--strafeCorrectionTicksRemaining <= 0) {
            setStrafeKey(client, strafeGoingRight, false);
            finishStrafe(client);
        }
    }

    private void finishStrafe(MinecraftClient client) {
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.player.setVelocity(0.0, client.player.getVelocity().y, 0.0);
        finishAction();
    }

    private float ease(float t) {
        return t * t * (3f - 2f * t);
    }

    private float easeOutBack(float t, float c1) {
        float c3 = c1 + 1f;
        float x = t - 1f;
        return 1f + c3 * x * x * x + c1 * x * x;
    }

    private void runLookAction(MinecraftClient client) {
        lookPhaseTicksRemaining--;
        float rawProgress;
        switch (lookPhase) {
            case 0 -> {
                rawProgress = 1f - (lookPhaseTicksRemaining / (float) lookTurnTicks);
                float eased = ease(rawProgress);
                client.player.setYaw(lookStartYaw + lookTargetYawOffset * eased);
                client.player.setPitch(lookStartPitch + lookTargetPitchOffset * eased);
                if (lookPhaseTicksRemaining <= 0) {
                    lookPhase = 1;
                    lookPhaseTicksRemaining = lookHoldTicks;
                }
            }
            case 1 -> {
                if (lookPhaseTicksRemaining <= 0) {
                    lookPhase = 2;
                    lookPhaseTicksRemaining = lookReturnTicks;
                }
            }
            case 2 -> {
                rawProgress = 1f - (lookPhaseTicksRemaining / (float) lookReturnTicks);
                float eased = easeOutBack(rawProgress, lookOvershootStrength);
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
