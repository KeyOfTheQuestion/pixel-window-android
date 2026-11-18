package local.pixel.window;

final class DoorSwing {
    static final float LEFT = 224.1f;
    static final float RIGHT = 247.9f;
    static final float TOP = 95.6f;
    static final float BOTTOM = 148.5f;
    static final float VIEW_DISTANCE = 72;
    private static final float WIDTH = RIGHT - LEFT;
    private static final float CENTER_X = (LEFT + RIGHT) / 2;
    private static final float CENTER_Y = (TOP + BOTTOM) / 2;
    private static final double OPEN_ANGLE = Math.toRadians(76);
    private static final float CROSS_START = .30f;
    private static final float CROSS_END = .70f;

    private DoorSwing() {}

    static float openness(float progress) {
        return ease(Math.min(progress / CROSS_START, (1 - progress) / (1 - CROSS_END)));
    }

    static float crossing(float progress) {
        return ease((progress - CROSS_START) / (CROSS_END - CROSS_START));
    }

    static void project(float openness, float[] corners) {
        double angle = OPEN_ANGLE * openness;
        float scale = perspective(angle);
        float freeX = freeEdgeX(angle, scale);
        corners[0] = LEFT;
        corners[1] = TOP;
        corners[2] = freeX;
        corners[3] = CENTER_Y + (TOP - CENTER_Y) * scale;
        corners[4] = freeX;
        corners[5] = CENTER_Y + (BOTTOM - CENTER_Y) * scale;
        corners[6] = LEFT;
        corners[7] = BOTTOM;
    }

    static float passageCenter() {
        return (freeEdgeX(OPEN_ANGLE, perspective(OPEN_ANGLE)) + RIGHT) / 2;
    }

    private static float perspective(double angle) {
        float depth = WIDTH * (float) Math.sin(angle);
        return VIEW_DISTANCE / (VIEW_DISTANCE + depth);
    }

    private static float freeEdgeX(double angle, float scale) {
        float rotatedX = LEFT + WIDTH * (float) Math.cos(angle);
        return CENTER_X + (rotatedX - CENTER_X) * scale;
    }

    private static float ease(float value) {
        float t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }
}
