package local.pixel.window;

final class StreetTraffic {
    private static final long BIKE_FIRST = 2_400;
    private static final long BIKE_PERIOD = 28_000;
    private static final long BIKE_DURATION = 6_600;
    private static final float ROAD_START = -29;
    private static final float ROAD_END = 469;
    static final float SPEED = (ROAD_END - ROAD_START) / BIKE_DURATION;

    private StreetTraffic() {}

    static long pass(long elapsed) {
        return Math.floorDiv(elapsed - BIKE_FIRST, BIKE_PERIOD);
    }

    static long rideTime(long elapsed) {
        return SceneTime.phase(elapsed, BIKE_FIRST, BIKE_PERIOD);
    }

    static boolean goingRight(long elapsed) {
        return pass(elapsed) % 2 == 0;
    }

    static float bikeX(long elapsed) {
        long ride = rideTime(elapsed);
        if (elapsed < BIKE_FIRST || ride >= BIKE_DURATION) {
            return Float.NaN;
        }
        float t = ride / (float) BIKE_DURATION;
        return ROAD_START + (ROAD_END - ROAD_START) * (goingRight(elapsed) ? t : 1 - t);
    }
}
