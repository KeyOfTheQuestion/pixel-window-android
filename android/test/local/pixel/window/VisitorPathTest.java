package local.pixel.window;

public final class VisitorPathTest {
    public static void main(String[] args) {
        VisitorPath visitor = new VisitorPath(0);
        visitor.sample(5_000L);
        check(visitor.stage == VisitorPath.Stage.APPROACH && visitor.x < VisitorPath.DOOR_X, "walk to the shop");
        float first = visitor.x;
        visitor.sample(5_500L);
        check(Math.abs(visitor.x - first - 12f) < .1f, "walk at 24 scene units per second");
        visitor.sample(12_000L);
        check(visitor.stage == VisitorPath.Stage.ENTER && visitor.x == VisitorPath.DOOR_X, "enter through the door");
        visitor.sample(16_000L);
        check(visitor.stage == VisitorPath.Stage.READ, "pause inside to read");
        visitor.sample(22_000L);
        check(visitor.stage == VisitorPath.Stage.EXIT && visitor.x == VisitorPath.DOOR_X, "leave through the same door");
        visitor.sample(27_000L);
        check(visitor.stage == VisitorPath.Stage.DEPART && visitor.x > VisitorPath.DOOR_X, "leave toward the far side");
        visitor.sample(61_000L);
        check(visitor.stage == VisitorPath.Stage.APPROACH && !visitor.right, "alternate walking direction");
        VisitorPath third = new VisitorPath(2);
        third.sample(5_000L);
        check(third.stage == VisitorPath.Stage.OFFSCREEN, "stagger arrivals");
        VisitorPath[] visitors = {visitor, new VisitorPath(1), third};
        for (long elapsed = 0; elapsed < 112_000; elapsed += 25) {
            int atDoor = 0;
            for (VisitorPath route : visitors) {
                route.sample(elapsed);
                if (route.stage == VisitorPath.Stage.ENTER || route.stage == VisitorPath.Stage.EXIT) {
                    atDoor++;
                }
            }
            check(atDoor <= 1, "give each visitor time to use the door");
        }
        System.out.println("Sidewalk, door, bookshop reading and return walks passed");
    }

    private static void check(boolean value, String desc) {
        if (!value) {
            throw new AssertionError(desc);
        }
    }
}
