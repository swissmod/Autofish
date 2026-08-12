package com.yourserver.autofish;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;

public final class AutoFishLogic {

    private enum State {
        IDLE, WAITING_FOR_BITE, REACTING, WAITING_TO_RECAST
    }

    private final AutoFishConfig config;
    private final Random random = Random.create();

    private State state = State.IDLE;
    private int tickTimer = 0;

    private double lastBobberY = Double.NaN;

    private static final int BOBBER_SPAWN_GRACE_TICKS = 40; // 2 seconds
    private int graceTicksRemaining = 0;

    private static final int MAX_WAIT_TICKS = 1200; // 60 seconds
    private int waitTicks = 0;

    private static final double SETTLE_VELOCITY_EPSILON = 0.03;
    private static final int REQUIRED_SETTLED_TICKS = 10; // 0.5s of calm floating
    private boolean settled = false;
    private int settledTicks = 0;

    public AutoFishLogic(AutoFishConfig config) {
        this.config = config;
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (!config.enabled) {
            state = State.IDLE;
            return;
        }
        if (client.player == null || client.world == null || client.interactionManager == null) {
            return;
        }
        if (client.currentScreen != null) {
            return;
        }
        if (!isHoldingFishingRod(client)) {
            state = State.IDLE;
            return;
        }
        if (config.pauseOnFullInventory && client.player.getInventory().getEmptySlot() == -1) {
            return;
        }

        switch (state) {
            case IDLE -> {
                castLine(client);
                state = State.WAITING_FOR_BITE;
                lastBobberY = Double.NaN;
                graceTicksRemaining = BOBBER_SPAWN_GRACE_TICKS;
                waitTicks = 0;
                settled = false;
                settledTicks = 0;
            }
            case WAITING_FOR_BITE -> checkForBite(client);
            case REACTING -> {
                if (--tickTimer <= 0) {
                    reelIn(client);
                    tickTimer = millisToTicks(randomBetween(config.minDelayMs, config.maxDelayMs));
                    state = State.WAITING_TO_RECAST;
                }
            }
            case WAITING_TO_RECAST -> {
                if (--tickTimer <= 0) {
                    state = State.IDLE;
                }
            }
        }
    }

    private boolean isHoldingFishingRod(MinecraftClient client) {
        ItemStack main = client.player.getMainHandStack();
        ItemStack off = client.player.getOffHandStack();
        return main.getItem() instanceof FishingRodItem || off.getItem() instanceof FishingRodItem;
    }

    private void castLine(MinecraftClient client) {
        rightClick(client);
    }

    private void reelIn(MinecraftClient client) {
        rightClick(client);
    }

    private void rightClick(MinecraftClient client) {
        Hand hand = client.player.getMainHandStack().getItem() instanceof FishingRodItem
                ? Hand.MAIN_HAND : Hand.OFF_HAND;
        client.interactionManager.interactItem(client.player, hand);
        client.player.swingHand(hand);
    }

    private void checkForBite(MinecraftClient client) {
        FishingBobberEntity bobber = findOwnBobber(client);
        if (bobber == null) {
            if (graceTicksRemaining > 0) {
                graceTicksRemaining--;
                return;
            }
            state = State.IDLE;
            return;
        }
        graceTicksRemaining = 0;

        if (++waitTicks > MAX_WAIT_TICKS) {
            state = State.IDLE;
            return;
        }

        double y = bobber.getVelocity().y;

        if (!settled) {
            if (Math.abs(y) < SETTLE_VELOCITY_EPSILON) {
                settledTicks++;
                if (settledTicks >= REQUIRED_SETTLED_TICKS) {
                    settled = true;
                }
            } else {
                settledTicks = 0;
            }
            lastBobberY = y;
            return;
        }

        if (!Double.isNaN(lastBobberY)) {
            double drop = lastBobberY - y;
            if (drop > config.biteSensitivity) {
                int reactionMs = config.randomizeReactionTime
                        ? randomBetween(config.minReactionMs, config.maxReactionMs)
                        : 0;
                tickTimer = Math.max(0, millisToTicks(reactionMs));
                state = State.REACTING;
            }
        }
        lastBobberY = y;
    }

    private FishingBobberEntity findOwnBobber(MinecraftClient client) {
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof FishingBobberEntity bobber && bobber.getOwner() == client.player) {
                return bobber;
            }
        }
        return null;
    }

    private int randomBetween(int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) return minInclusive;
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    private int millisToTicks(int millis) {
        return Math.max(0, Math.round(millis / 50.0f));
    }
}
