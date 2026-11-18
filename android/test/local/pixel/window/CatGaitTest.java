package local.pixel.window;

public final class CatGaitTest {
    public static void main(String[] args) {
        CatGait gait = new CatGait();
        float[] min = new float[4];
        float[] max = new float[4];
        float[] lift = new float[4];
        float[] lastX = new float[4];
        float[] lastY = new float[4];
        for (int sample = 0; sample <= 244; sample++) {
            float distance = sample * .1f;
            gait.sample(distance, 1);
            int airborne = 0;
            for (int paw = 0; paw < 4; paw++) {
                float x = gait.offset(paw);
                float y = gait.lift(paw);
                airborne += y > .01f ? 1 : 0;
                min[paw] = Math.min(min[paw], x);
                max[paw] = Math.max(max[paw], x);
                lift[paw] = Math.max(lift[paw], y);
                if (sample > 0) {
                    check(Math.abs(lastX[paw] - x) < .45f, "paw must move continuously");
                    if (lastY[paw] == 0 && y == 0) {
                        check(Math.abs(x - lastX[paw] - .1f) < .002f, "grounded paw must not slide");
                    }
                }
                lastX[paw] = x;
                lastY[paw] = y;
            }
            check(airborne <= 2, "at least two paws should support the cat at walking speed");
            check(gait.lift(0) == 0 || gait.lift(2) == 0, "forepaws must alternate");
            check(gait.lift(1) == 0 || gait.lift(3) == 0, "hind paws must alternate");
        }
        for (int paw = 0; paw < 4; paw++) {
            check(min[paw] < -3 && max[paw] > 3 && lift[paw] > 1, "each paw must take a full step");
        }
        gait.sample(6, 0);
        for (int paw = 0; paw < 4; paw++) {
            check(gait.offset(paw) == 0 && gait.lift(paw) == 0, "standing puts all paws on the sill");
        }
        System.out.println("Four alternating paws, stable footfalls and continuous gait passed");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
