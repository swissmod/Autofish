package com.yourserver.autofish;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class PlayerDetectionLogic {

    private enum DetectState { NONE, WAITING_TO_DISABLE, DISABLED_WAITING_FOR_CLEAR, WAITING_TO_RESUME, RESUMING_LOOK }

    private final AutoFishConfig config;

    private DetectState detectState = DetectState.NONE;
    private int timer = 0;

    private static final double DETECTION_RADIUS = 5.0;
    private static final double HALF_ANGLE_DEGREES = 40.0;
    private static final int WAIT_BEFORE_DISABLE_TICKS = 60;
    private static final int RESUME_DELAY_TICKS = 100;

    private float originalYaw, originalPitch;

    private boolean lookActive = false;
    private int lookTicksElapsed;
    private static final int LOOK_TOTAL_TICKS = 15;
    private float lookAnimStartYaw, lookAnimStartPitch, lookAnimTargetYaw, lookAnimTargetPitch;

    public PlayerDetectionLogic(AutoFishConfig config) {
        this.config = config;
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (lookActive) {
            runLookAnimation(client);
        }

        if (!config.playerDetectionEnabled) {
            detectState = DetectState.NONE;
            timer = 0;
            return;
        }

        switch (detectState) {
            case NONE -> {
                if (!config.enabled) {
                    return;
                }
                if (findPlayerInFront(client) != null) {
                    detectState = DetectState.WAITING_TO_DISABLE;
                    timer = WAIT_BEFORE_DISABLE_TICKS;
                }
            }
            case WAITING_TO_DISABLE -> {
                PlayerEntity intruder = findPlayerInFront(client);
                if (intruder == null) {
                    detectState = DetectState.NONE;
                    return;
                }
                if (--timer <= 0) {
                    startLookAt(client, intruder);
                    config.enabled = false;
                    detectState = DetectState.DISABLED_WAITING_FOR_CLEAR;
                }
            }
            case DISABLED_WAITING_FOR_CLEAR -> {
                if (findPlayerInFront(client) == null) {
                    detectState = DetectState.WAITING_TO_RESUME;
                    timer = RESUME_DELAY_TICKS;
                }
            }
            case WAITING_TO_RESUME -> {
                if (findPlayerInFront(client) != null) {
                    detectState = DetectState.DISABLED_WAITING_FOR_CLEAR;
                    return;
                }
                if (--timer <= 0) {
                    startLookBack(client);
                    detectState = DetectState.RESUMING_LOOK;
                }
            }
            case RESUMING_LOOK -> {
                if (!lookActive) {
                    config.enabled = true;
                    detectState = DetectState.NONE;
                }
            }
        }
    }

    private PlayerEntity findPlayerInFront(MinecraftClient client) {
        Box box = client.player.getBoundingBox().expand(DETECTION_RADIUS);
        double cosHalfAngle = Math.cos(Math.toRadians(HALF_ANGLE_DEGREES));
        Vec3d forward = client.player.getRotationVector();
        Vec3d eyePos = client.player.getEyePos();

        for (PlayerEntity other : client.world.getEntitiesByClass(PlayerEntity.class, box, p -> p != client.player)) {
            Vec3d toOther = other.getEyePos().subtract(eyePos);
            double distance = toOther.length();
            if (distance < 0.01 || distance > DETECTION_RADIUS) {
                continue;
            }
            double dot = forward.normalize().dotProduct(toOther.normalize());
            if (dot >= cosHalfAngle) {
                return other;
            }
        }
        return null;
    }

    private void startLookAt(MinecraftClient client, PlayerEntity target) {
        originalYaw = client.player.getYaw();
        originalPitch = client.player.getPitch();

        Vec3d delta = target.getEyePos().subtract(client.player.getEyePos());
        double horizontalDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0));
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(delta.y, horizontalDist)));

        beginLookAnimation(client.player.getYaw(), client.player.getPitch(), targetYaw, targetPitch);
    }

    private void startLookBack(MinecraftClient client) {
        beginLookAnimation(client.player.getYaw(), client.player.getPitch(), originalYaw, originalPitch);
    }

    private void beginLookAnimation(float startYaw, float startPitch, float targetYaw, float targetPitch) {
        lookAnimStartYaw = startYaw;
        lookAnimStartPitch = startPitch;
        lookAnimTargetYaw = targetYaw;
        lookAnimTargetPitch = targetPitch;
        lookTicksElapsed = 0;
        lookActive = true;
    }

    private float ease(float t) {
        return t * t * (3f - 2f * t);
    }

    private void runLookAnimation(MinecraftClient client) {
        lookTicksElapsed++;
        float progress = ease(lookTicksElapsed / (float) LOOK_TOTAL_TICKS);
        client.player.setYaw(lerpAngle(lookAnimStartYaw, lookAnimTargetYaw, progress));
        client.player.setPitch(lookAnimStartPitch + (lookAnimTargetPitch - lookAnimStartPitch) * progress);
        if (lookTicksElapsed >= LOOK_TOTAL_TICKS) {
            lookActive = false;
        }
    }

    private float lerpAngle(float start, float end, float progress) {
        float delta = MathHelper.wrapDegrees(end - start);
        return start + delta * progress;
    }
}
