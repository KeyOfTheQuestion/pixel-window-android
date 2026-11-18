package local.pixel.window;

final class CatMotion {
    enum Stage { ENTERING, RESTING, LEAVING, ALERT, RETREATING, WAITING, OFFSCREEN }

    private static final long FIRST_VISIT = 5_900;
    private static final long PERIOD = 47_000;
    private static final long STEP = 16;
    private static final int REACTION_MS = 144;
    static final long REST_TIME = 4_500;
    private static final float SHELTER = 451;
    private static final float REST = 358;
    private static final float WALK_SPEED = .016f;
    private static final float RETREAT_SPEED = .040f;
    private Stage stage = Stage.OFFSCREEN;
    private Stage afterWait;
    private float x = SHELTER;
    private float distance;
    private long cycle = Long.MIN_VALUE;
    private long simulated;
    private long enteredState;
    private long lastBike;
    private boolean bikeFromLeft;

    void update(long elapsed) {
        if (elapsed < FIRST_VISIT) {
            return;
        }
        long visit = Math.floorDiv(elapsed - FIRST_VISIT, PERIOD);
        if (visit != cycle) {
            cycle = visit;
            simulated = FIRST_VISIT + PERIOD * visit;
            enteredState = simulated;
            stage = Stage.ENTERING;
            x = SHELTER;
            distance = 0;
            lastBike = Long.MIN_VALUE;
        }
        while (simulated + STEP <= elapsed && stage != Stage.OFFSCREEN) {
            simulated += STEP;
            step();
        }
    }

    Stage stage() {
        return stage;
    }

    float position() {
        return x;
    }

    float walked() {
        return distance;
    }

    boolean facingRight() {
        return stage == Stage.LEAVING || stage == Stage.RETREATING
            || ((stage == Stage.ALERT || stage == Stage.WAITING) && afterWait == Stage.LEAVING);
    }

    float gaitStrength() {
        float remaining = stage == Stage.ENTERING ? x - REST : SHELTER - x;
        float envelope = Math.min(1, Math.min(distance, remaining) / 2.3f);
        return envelope * envelope * (3 - 2 * envelope);
    }

    long stateAge() {
        return simulated - enteredState;
    }

    private void step() {
        if ((stage == Stage.ENTERING || stage == Stage.RESTING || stage == Stage.LEAVING) && shouldYield()) {
            bikeFromLeft = StreetTraffic.goingRight(simulated);
            lastBike = StreetTraffic.pass(simulated);
            afterWait = stage == Stage.LEAVING ? Stage.LEAVING : Stage.ENTERING;
            changeTo(Stage.ALERT);
            return;
        }

        switch (stage) {
            case ENTERING:
                moveToward(REST, WALK_SPEED);
                if (x == REST) {
                    changeTo(Stage.RESTING);
                }
                break;
            case RESTING:
                if (stateAge() >= REST_TIME) {
                    changeTo(Stage.LEAVING);
                }
                break;
            case LEAVING:
                moveToward(SHELTER, WALK_SPEED);
                if (x == SHELTER) {
                    changeTo(Stage.OFFSCREEN);
                }
                break;
            case ALERT:
                if (stateAge() >= REACTION_MS) {
                    changeTo(bikeFromLeft ? Stage.RETREATING : Stage.WAITING);
                }
                break;
            case RETREATING:
                moveToward(SHELTER, RETREAT_SPEED);
                if (x == SHELTER) {
                    changeTo(Stage.WAITING);
                }
                break;
            case WAITING:
                if (stateAge() >= 192 && bicycleHasPassed()) {
                    changeTo(afterWait == Stage.LEAVING && x == SHELTER ? Stage.OFFSCREEN : afterWait);
                }
                break;
            case OFFSCREEN:
                break;
        }
    }

    private boolean shouldYield() {
        if (x >= 438 || StreetTraffic.pass(simulated) == lastBike) {
            return false;
        }
        float bike = StreetTraffic.bikeX(simulated);
        boolean fromLeft = StreetTraffic.goingRight(simulated);
        float distanceInFront = fromLeft ? x - bike : bike - x;
        float notice = fromLeft ? 44 + (SHELTER - x) * StreetTraffic.SPEED / RETREAT_SPEED : 57;
        return distanceInFront >= 0 && distanceInFront <= notice;
    }

    private boolean bicycleHasPassed() {
        if (StreetTraffic.pass(simulated) != lastBike) {
            return true;
        }
        float bike = StreetTraffic.bikeX(simulated);
        return Float.isNaN(bike) || (!bikeFromLeft && bike < x - 45);
    }

    private void moveToward(float goal, float speed) {
        float delta = goal - x;
        float step = Math.min(Math.abs(delta), STEP * speed);
        x = Math.abs(delta) <= step ? goal : x + Math.copySign(step, delta);
        distance += step;
    }

    private void changeTo(Stage next) {
        stage = next;
        enteredState = simulated;
        distance = 0;
    }
}
