package com.yourserver.autofish;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;

/**
 * Drives the actual cast -> wait for bite -> reel -> random delay -> recast loop.
 * Runs on the client tick event, so it only ever acts like a very fast, very
 * consistent player clicking their own mouse - it doesn't touch the network
 * protocol or send anything a real right-click wouldn't send.
 */
public final class AutoFishLogic {

    private enum State {
        IDLE,               // rod not out, waiting to cast
        WAITING_FOR_BITE,   // rod is out, watching the bobber
        REACTING,           // bite detected, waiting a short randomized reaction time
        WAITING_TO_RECAST   // just reeled in, waiting a randomized delay before casting again
    }

    private final AutoFishConfig config;
    private final Random random = Random.create();

    private State state = State.IDLE;
    private int tickTimer = 0;

    private double lastBobberY = Double.NaN;

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
            // Don't do anything while any GUI (including ours) is open.
            return;
        }
        if (!isHoldingFishingRod(client)) {
            state = State.IDLE;
            return;
        }
        if (config.pauseOnFullInventory && client.player.getInventory().getEmptySlot() == -1) {
            return; // inventory full, pause quietly instead of spamming casts
        }

        switch (state) {
            case IDLE -> {
                castLine(client);
                state = State.WAITING_FOR_BITE;
                lastBobberY = Double.NaN;
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

    /**
     * Looks for the player's fishing bobber entity and watches its vertical
     * velocity for the sharp downward jerk that happens when a fish bites.
     * This mirrors the same visual cue a human player watches for - it does
     * not read any hidden/server-only information.
     */
    private void checkForBite(MinecraftClient client) {
        FishingBobberEntity bobber = findOwnBobber(client);
        if (bobber == null) {
            // Bobber is gone (line broke, got reeled by something else, etc).
            state = State.IDLE;
            return;
        }

        double y = bobber.getVelocity().y;
        if (!Double.isNaN(lastBobberY)) {
            double drop = lastBobberY - y;
            // Threshold tuned against vanilla bite behavior; adjust if your
            // server has custom fishing mechanics.
            if (drop > 0.25 && y < -0.05) {
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
        // Minecraft runs at a fixed 20 ticks/sec -> 50ms per tick.
        // We still add randomness at the millisecond level (per your request)
        // and just round to the nearest tick, since the tick loop is the
        // finest resolution client logic actually runs at.
        return Math.max(0, Math.round(millis / 50.0f));
    }
}
