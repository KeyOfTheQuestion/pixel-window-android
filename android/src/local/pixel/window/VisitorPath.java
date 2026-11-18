package local.pixel.window;

final class VisitorPath {
    enum Stage { OFFSCREEN, APPROACH, ENTER, READ, EXIT, DEPART }

    static final float DOOR_X = DoorSwing.passageCenter();
    private static final float LEFT_EDGE = -14f;
    private static final float RIGHT_EDGE = 454f;
    private static final float SPEED = .024f;
    private static final long PERIOD = 56_000;
    private static final long ENTER_TIME = 3_600;
    private static final long EXIT_TIME = 3_500;
    private static final long[] DOOR_ARRIVALS = {11_200, 15_500, 29_600};

    final int visitor;
    Stage stage = Stage.OFFSCREEN;
    boolean right;
    float x;
    float progress;
    float distance;

    VisitorPath(int visitor) {
        this.visitor = visitor;
    }

    void sample(long elapsed) {
        right = (Math.floorDiv(elapsed, PERIOD) + visitor) % 2 == 0;
        float start = right ? LEFT_EDGE : RIGHT_EDGE;
        float end = right ? RIGHT_EDGE : LEFT_EDGE;
        long inbound = travelTime(start, DOOR_X);
        long outbound = travelTime(DOOR_X, end);
        long readTime = 5_700 + visitor * 600L;
        long time = Math.floorMod(elapsed, PERIOD) - (DOOR_ARRIVALS[visitor] - inbound);

        if (time < 0) {
            stage = Stage.OFFSCREEN;
        } else if (time < inbound) {
            set(Stage.APPROACH, time, inbound, start, DOOR_X);
        } else if ((time -= inbound) < ENTER_TIME) {
            set(Stage.ENTER, time, ENTER_TIME, DOOR_X, DOOR_X);
        } else if ((time -= ENTER_TIME) < readTime) {
            set(Stage.READ, time, readTime, DOOR_X, DOOR_X);
        } else if ((time -= readTime) < EXIT_TIME) {
            set(Stage.EXIT, time, EXIT_TIME, DOOR_X, DOOR_X);
        } else if ((time -= EXIT_TIME) < outbound) {
            set(Stage.DEPART, time, outbound, DOOR_X, end);
        } else {
            stage = Stage.OFFSCREEN;
        }
    }

    private static long travelTime(float start, float end) {
        return Math.round(Math.abs(end - start) / SPEED);
    }

    private void set(Stage next, long time, long duration, float from, float to) {
        stage = next;
        progress = time / (float) duration;
        x = from + (to - from) * progress;
        distance = Math.abs(x - from);
    }
}
