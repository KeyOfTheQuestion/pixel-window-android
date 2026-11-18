package local.pixel.window;

final class CatGait {
    private static final float STRIDE = 12.2f;
    private static final float SUPPORT = .64f;
    // Far fore, far hind, near fore, near hind.
    private static final float[] PHASES = {.10f, .29f, .60f, .79f};
    private final float[] offsets = new float[4];
    private final float[] lifts = new float[4];
    private float rise;

    void sample(float distance, float strength) {
        float cycle = distance / STRIDE;
        for (int paw = 0; paw < PHASES.length; paw++) {
            float phase = cycle + PHASES[paw];
            phase -= (float) Math.floor(phase);
            float swing = Math.max(0, (phase - SUPPORT) / (1 - SUPPORT));
            float reach = swing * swing * (3 - 2 * swing);
            float arc = (float) Math.sin(Math.PI * swing);
            // During support the paw moves backward relative to the torso at exactly its ground speed.
            offsets[paw] = strength * STRIDE * (phase - SUPPORT / 2 - reach);
            lifts[paw] = strength * (paw % 2 == 0 ? 1.45f : 1.25f) * arc * arc;
        }
        rise = strength * .20f * (float) Math.cos(4 * Math.PI * cycle);
    }

    float offset(int paw) {
        return offsets[paw];
    }

    float lift(int paw) {
        return lifts[paw];
    }

    float rise() {
        return rise;
    }
}
