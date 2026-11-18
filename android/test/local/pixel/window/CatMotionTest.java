package local.pixel.window;

public final class CatMotionTest {
    public static void main(String[] args) {
        CatMotion cat = new CatMotion();
        cat.update(6_800);
        check(cat.stage() == CatMotion.Stage.ENTERING, "cat should walk into view");
        float before = cat.position();
        boolean retreated = false;
        boolean waited = false;
        for (long now = 7_000; now <= 9_480; now += 16) {
            cat.update(now);
            retreated |= cat.stage() == CatMotion.Stage.RETREATING && cat.position() > before;
            waited |= cat.stage() == CatMotion.Stage.WAITING;
        }
        check(retreated, "cat must move away from the approaching bicycle");
        check(waited, "cat should give the bike time to pass");
        CatMotion waiting = new CatMotion();
        waiting.update(8_800);
        check(waiting.stage() == CatMotion.Stage.WAITING, "the cat should be sheltered while the rider is still passing");
        cat.update(10_500);
        check(cat.stage() == CatMotion.Stage.ENTERING, "cat should resume walking once the bike clears");

        float start = cat.position();
        cat.update(11_000);
        float traveled = start - cat.position();
        check(traveled >= 7.5f && traveled <= 8.5f, "the cat should stroll at around 16 scene units per second");
        check(StreetTraffic.bikeX(5_000) < StreetTraffic.bikeX(5_500), "first pass goes right");
        check(StreetTraffic.bikeX(33_000) > StreetTraffic.bikeX(33_500), "next pass returns left");
        check(Float.isNaN(StreetTraffic.bikeX(12_000)), "leave a quiet interval between rides");
        System.out.println("Alternating riders, bicycle avoidance and quiet-street walking passed");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
