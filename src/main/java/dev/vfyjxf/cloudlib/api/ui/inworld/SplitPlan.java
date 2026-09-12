package dev.vfyjxf.cloudlib.api.ui.inworld;

/**
 * Pure split arithmetic for world-drag item distribution — shared by the
 * client (drop preview) and the server (authoritative transfer), so the math
 * must stay allocation-light, deterministic and free of Minecraft state.
 */
public final class SplitPlan {

    /**
     * Evenly splits {@code count} items across {@code targets} destinations.
     * Each destination gets {@code count/targets}; the first {@code count%targets}
     * get one extra. Sum of the result always equals {@code count}.
     */
    public static int[] evenly(int count, int targets) {
        int[] shares = new int[Math.max(0, targets)];
        if (targets <= 0 || count <= 0) return shares;
        int base = count / targets;
        int rem = count % targets;
        for (int i = 0; i < targets; i++) {
            shares[i] = base + (i < rem ? 1 : 0);
        }
        return shares;
    }

    /**
     * One item per destination while supply lasts — the first
     * {@code min(count, targets)} entries get 1, the rest 0.
     */
    public static int[] oneEach(int count, int targets) {
        int[] shares = new int[Math.max(0, targets)];
        if (targets <= 0 || count <= 0) return shares;
        int give = Math.min(count, targets);
        for (int i = 0; i < give; i++) shares[i] = 1;
        return shares;
    }

    /** Number of destinations that would receive a non-zero share. */
    public static int used(int[] shares) {
        int n = 0;
        for (int s : shares) if (s > 0) n++;
        return n;
    }

    private SplitPlan() {}
}
