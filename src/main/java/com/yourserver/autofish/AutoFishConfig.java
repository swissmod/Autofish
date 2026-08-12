package com.yourserver.autofish;

/**
 * Holds all user-adjustable settings for AutoFish.
 * Kept as simple public fields since this is a tiny, single-purpose config.
 */
public class AutoFishConfig {

    public boolean enabled = false;

    // Random delay window (in milliseconds) before re-casting after a catch.
    public int minDelayMs = 100;
    public int maxDelayMs = 200;

    // Extra: randomized "reaction time" before reeling in after a bite is
    // detected, so every player doesn't reel in on the exact same tick.
    public boolean randomizeReactionTime = true;
    public int minReactionMs = 50;
    public int maxReactionMs = 300;

    // Extra: pause autofishing automatically if your inventory fills up
    // with junk items, so you don't idle forever with a full inventory.
    public boolean pauseOnFullInventory = true;

    public void clampValues() {
        minDelayMs = Math.max(0, minDelayMs);
        maxDelayMs = Math.max(minDelayMs, maxDelayMs);
        minReactionMs = Math.max(0, minReactionMs);
        maxReactionMs = Math.max(minReactionMs, maxReactionMs);
    }
}
