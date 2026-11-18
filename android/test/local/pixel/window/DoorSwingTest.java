package local.pixel.window;

public final class DoorSwingTest {
    public static void main(String[] args) {
        float[] panel = new float[8];
        float width = DoorSwing.RIGHT - DoorSwing.LEFT;
        float height = DoorSwing.BOTTOM - DoorSwing.TOP;
        float center = (DoorSwing.LEFT + DoorSwing.RIGHT) / 2;
        DoorSwing.project(0, panel);
        check(panel[2] == DoorSwing.RIGHT && panel[3] == DoorSwing.TOP && panel[5] == DoorSwing.BOTTOM,
            "the closed panel fills the doorway");

        float previousEdge = panel[2];
        for (int frame = 1; frame <= 60; frame++) {
            DoorSwing.project(frame / 60f, panel);
            check(panel[0] == DoorSwing.LEFT && panel[6] == DoorSwing.LEFT
                && panel[1] == DoorSwing.TOP && panel[7] == DoorSwing.BOTTOM, "keep the hinge fixed");
            check(panel[2] == panel[4] && panel[2] < previousEdge, "the free edge swings into the doorway");
            check(panel[3] > panel[1] && panel[5] < panel[7], "the far edge recedes as a single plane");

            float scale = (panel[5] - panel[3]) / height;
            float depth = DoorSwing.VIEW_DISTANCE * (1 / scale - 1);
            float horizontal = center + (panel[2] - center) / scale - DoorSwing.LEFT;
            check(Math.abs(Math.hypot(horizontal, depth) - width) < .01, "the free edge traces a constant-radius arc");
            previousEdge = panel[2];
        }
        check(panel[2] - DoorSwing.LEFT > 6 && DoorSwing.RIGHT - panel[2] > 14,
            "the entire door and a usable gap remain visible");
        check(Math.abs(DoorSwing.passageCenter() - (panel[2] + DoorSwing.RIGHT) / 2) < .01f,
            "the visitor path passes through the open gap");

        check(DoorSwing.openness(0) == 0 && DoorSwing.openness(1) == 0, "door closes");
        check(Math.abs(DoorSwing.openness(.1f) - DoorSwing.openness(.9f)) < .001f,
            "door returns along the same swing");
        check(DoorSwing.openness(.03f) < .1f && DoorSwing.openness(.97f) < .1f, "door eases in and out");
        check(DoorSwing.openness(.5f) == 1 && DoorSwing.crossing(.5f) > 0, "door stays open for visitors");
        check(DoorSwing.crossing(.2f) == 0 && DoorSwing.crossing(.8f) == 1,
            "visitor waits for the opening and clears it before closing");
        System.out.println("Rigid single-panel door, circular swing and passage timing passed");
    }

    private static void check(boolean value, String description) {
        if (!value) {
            throw new AssertionError(description);
        }
    }
}
