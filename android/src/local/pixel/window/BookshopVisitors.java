package local.pixel.window;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

final class BookshopVisitors {
    private static final int CELL = 362;
    private static final int[] WALK = {0, 1, 2, 1};
    private static final float[] BOOK_X = {285.6f, 346.6f, 266.4f};
    private static final float[] GLASS_LEFT = {275.4f, 334.4f, 259.7f};
    private static final float[] GLASS_RIGHT = {295.3f, 358.9f, 273.1f};
    private static final float[] PANE_TOPS = {94.1f, 104.9f, 118.0f};
    private static final float[] PANE_BOTTOMS = {103.8f, 116.6f, 130.4f};
    private static final float THRESHOLD = 150f;
    private static final ColorMatrixColorFilter LAMP_LIGHT = new ColorMatrixColorFilter(new float[]{
        .95f, 0, 0, 0, 13,
        0, .88f, 0, 0, 8,
        0, 0, .73f, 0, 2,
        0, 0, 0, 1, 0
    });

    private final Bitmap atlas;
    private final BookshopDoor door;
    private final Rect[][] frames = new Rect[3][4];
    private final Path[] panes = new Path[3];
    private final VisitorPath[] routes = {new VisitorPath(0), new VisitorPath(1), new VisitorPath(2)};
    private final Paint sprite = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint detail = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF destination = new RectF();
    private ColorMatrixColorFilter streetTint;
    private int tintStep = -1;

    BookshopVisitors(Resources resources, BookshopDoor door) {
        this.door = door;
        atlas = BitmapFactory.decodeResource(resources, R.drawable.bookshop_readers);
        atlas.prepareToDraw();
        for (int row = 0; row < frames.length; row++) {
            for (int col = 0; col < frames[row].length; col++) {
                int x = col * CELL;
                int y = row * CELL;
                frames[row][col] = new Rect(x + 70, y + 5, x + 279, y + 351);
            }
            Path glass = new Path();
            for (int i = 0; i < PANE_TOPS.length; i++) {
                glass.addRect(GLASS_LEFT[row], PANE_TOPS[i], GLASS_RIGHT[row], PANE_BOTTOMS[i], Path.Direction.CW);
            }
            panes[row] = glass;
        }
    }

    boolean draw(Canvas canvas, long elapsed, float shade) {
        int tint = Math.round(255 * shade);
        if (tint != tintStep) {
            tintStep = tint;
            streetTint = SceneColors.tint(tint / 255f, .79f, .67f, .72f);
        }
        float open = 0;
        boolean moving = false;
        for (VisitorPath route : routes) {
            route.sample(elapsed);
            if (route.stage == VisitorPath.Stage.ENTER || route.stage == VisitorPath.Stage.EXIT) {
                open = Math.max(open, DoorSwing.openness(route.progress));
            }
        }
        if (open > 0) {
            door.draw(canvas, open, shade);
        }
        for (VisitorPath route : routes) {
            if (route.stage == VisitorPath.Stage.READ) {
                reading(canvas, route, elapsed);
            } else if (route.stage != VisitorPath.Stage.OFFSCREEN) {
                pedestrian(canvas, route);
                moving = true;
            }
        }
        return moving;
    }

    private void reading(Canvas canvas, VisitorPath route, long elapsed) {
        int person = route.visitor;
        float visibility = .88f * smooth(route.progress / .10f) * smooth((1 - route.progress) / .11f);
        float x = BOOK_X[person];
        float floor = 138.2f;
        float lean = (float) Math.sin(elapsed / 1_450.0 + person) * 1.2f;
        canvas.save();
        canvas.clipPath(panes[person]);
        canvas.rotate(lean, x, 120);
        sprite.setColorFilter(LAMP_LIGHT);
        pose(canvas, person, 3, x, floor, 32.0f, false, visibility);
        canvas.restore();
    }

    private void pedestrian(Canvas canvas, VisitorPath route) {
        int person = route.visitor;
        float walkway = 159 + person * .85f;
        float floor = walkway;
        float height = 34.0f;
        float opacity = 1;
        float distance = route.distance;
        boolean standing = false;
        if (route.stage == VisitorPath.Stage.ENTER) {
            float depth = DoorSwing.crossing(route.progress);
            floor = mix(walkway, THRESHOLD, depth);
            height -= 4 * depth;
            opacity = smooth((1 - depth) / .7f);
            distance = 15.6f * depth;
            standing = depth == 0;
        } else if (route.stage == VisitorPath.Stage.EXIT) {
            float depth = DoorSwing.crossing(route.progress);
            floor = mix(THRESHOLD, walkway, depth);
            height = 30 + 4 * depth;
            opacity = smooth(depth / .7f);
            distance = 15.6f * depth;
            standing = depth == 1;
        }
        int index = (int) (distance / 5.2f) % WALK.length;
        float step = standing ? 0 : (float) Math.sin(distance * Math.PI / 10.4);
        floor -= .46f * Math.abs(step);
        detail.setColor(0xff2c2226);
        detail.setAlpha(Math.round(opacity * 93));
        canvas.drawOval(route.x - 7, floor - .7f, route.x + 7, floor + 1.35f, detail);
        sprite.setColorFilter(streetTint);
        pose(canvas, person, standing ? 1 : WALK[index], route.x, floor, height, route.right, opacity);
    }

    private void pose(Canvas canvas, int person, int frame, float x, float floor, float height,
                      boolean right, float opacity) {
        if (opacity <= 0) {
            return;
        }
        Rect area = frames[person][frame];
        float half = height * area.width() / area.height() / 2;
        destination.set(x - half, floor - height, x + half, floor);
        sprite.setAlpha(Math.round(255 * opacity));
        canvas.save();
        if (right) {
            canvas.scale(-1, 1, x, floor);
        }
        canvas.drawBitmap(atlas, area, destination, sprite);
        canvas.restore();
    }

    private static float smooth(float value) {
        float t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }

    private static float mix(float from, float to, float progress) {
        return from + (to - from) * progress;
    }
}
