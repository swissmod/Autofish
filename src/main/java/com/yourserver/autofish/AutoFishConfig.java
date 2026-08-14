package com.yourserver.autofish;

public class AutoFishConfig {

    public boolean enabled = false;

    public int minDelayMs = 100;
    public int maxDelayMs = 200;

    public boolean randomizeReactionTime = true;
    public int minReactionMs = 50;
    public int maxReactionMs = 300;

    public boolean pauseOnFullInventory = true;

    public double biteSensitivity = 0.04;
    public boolean antiAfkEnabled = true;
    public boolean playerDetectionEnabled = true;

    // Safety net for server-side teleport bugs: if you move further than
    // this many blocks from where autofishing started, it sends /hub and
    // turns itself off, instead of continuing to fish from a random spot.
    public double teleportResetDistance = 25.0;

    public void clampValues() {
        minDelayMs = Math.max(0, minDelayMs);
        maxDelayMs = Math.max(minDelayMs, maxDelayMs);
        minReactionMs = Math.max(0, minReactionMs);
        maxReactionMs = Math.max(minReactionMs, maxReactionMs);
        if (biteSensitivity < 0.001) biteSensitivity = 0.001;
        if (teleportResetDistance < 1.0) teleportResetDistance = 1.0;
    }
}
