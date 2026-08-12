package com.yourserver.autofish;

public class AutoFishConfig {

    public boolean enabled = false;

    public int minDelayMs = 100;
    public int maxDelayMs = 200;

    public boolean randomizeReactionTime = true;
    public int minReactionMs = 50;
    public int maxReactionMs = 300;

    public boolean pauseOnFullInventory = true;

    // How big a sudden downward jerk in the bobber's velocity counts as a
    // bite. Lower = more sensitive (catches subtler bites, but more prone
    // to false triggers). Real observed bite values were roughly 0.06-0.16,
    // so the default sits comfortably below that whole range.
    public double biteSensitivity = 0.04;

    public void clampValues() {
        minDelayMs = Math.max(0, minDelayMs);
        maxDelayMs = Math.max(minDelayMs, maxDelayMs);
        minReactionMs = Math.max(0, minReactionMs);
        maxReactionMs = Math.max(minReactionMs, maxReactionMs);
        if (biteSensitivity < 0.001) biteSensitivity = 0.001;
    }
}
