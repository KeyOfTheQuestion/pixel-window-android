package local.pixel.window;

import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

final class PixelActors {
    private static final int INK = 0xff342a28;
    private static final int TEAL = 0xff3f6660;
    private static final int TEAL_LIGHT = 0xff64877b;
    private static final int SKIN = 0xffe7a879;
    private final Paint paint = new Paint();
    private ColorMatrixColorFilter bikeTint;
    private int tintStep = -1;

    void drawBike(Canvas canvas, int x, int floor, boolean right, long rideTime, float shade) {
        int tint = Math.round(255 * shade);
        if (tint != tintStep) {
            tintStep = tint;
            bikeTint = tint == 0 ? null : SceneColors.tint(tint / 255f, .84f, .79f, .88f);
        }
        paint.setColorFilter(bikeTint);
        canvas.save();
        canvas.translate(x, floor);
        if (!right) {
            canvas.scale(-1, 1);
        }
        box(canvas, -21, 0, 20, 2, 0x55332b3d);
        float rotation = rideTime / 97f;
        wheel(canvas, -13, -6, rotation);
        wheel(canvas, 13, -6, rotation);
        line(canvas, -13, -6, -4, -14, 0xffb69a6a, 2);
        line(canvas, -13, -6, -2, -6, 0xffb69a6a, 2);
        line(canvas, -2, -6, 6, -15, 0xffe0ba7b, 2);
        line(canvas, -4, -14, 6, -15, 0xffe0ba7b, 2);
        line(canvas, -4, -14, -2, -6, 0xffe0ba7b, 2);
        line(canvas, 6, -15, 13, -6, 0xffb69a6a, 2);
        line(canvas, 6, -15, 4, -20, INK, 2);
        box(canvas, 3, -21, 9, -19, INK);
        box(canvas, -8, -16, -1, -14, INK);
        box(canvas, 16, -11, 19, -9, 0xffd9a469);

        float pedal = rideTime / 115f;
        for (int side = 0; side < 2; side++) {
            float phase = pedal + side * (float) Math.PI;
            int pedalX = -2 + Math.round((float) Math.cos(phase) * 4);
            int pedalY = -7 + Math.round((float) Math.sin(phase) * 4);
            int knee = Math.round((pedalX + 3) * 0.65f);
            int pants = side == 0 ? 0xff40444d : 0xff55555a;
            line(canvas, -5, -17, knee, -12, pants, 4);
            line(canvas, knee, -12, pedalX, pedalY, pants, 3);
            box(canvas, pedalX - 1, pedalY, pedalX + 4, pedalY + 2, INK);
        }

        int bounce = ((rideTime / 170) % 4 == 1) ? 1 : 0;
        box(canvas, -9, -23 + bounce, -1, -17, TEAL);
        box(canvas, -7, -25 + bounce, 1, -19, TEAL);
        box(canvas, -6, -24 + bounce, -4, -17, TEAL_LIGHT);
        line(canvas, -2, -23 + bounce, 1, -20, TEAL_LIGHT, 3);
        line(canvas, 1, -20, 7, -20, SKIN, 2);
        box(canvas, -2, -29 + bounce, 3, -24 + bounce, SKIN);
        box(canvas, 2, -27 + bounce, 5, -25 + bounce, SKIN);
        box(canvas, -3, -30 + bounce, 3, -28 + bounce, 0xff603a35);
        box(canvas, -1, -31 + bounce, 3, -30 + bounce, 0xff966152);
        box(canvas, 3, -29 + bounce, 5, -28 + bounce, 0xff603a35);
        box(canvas, 2, -27 + bounce, 3, -26 + bounce, INK);
        box(canvas, -5, -24 + bounce, 1, -22 + bounce, 0xffc96d51);
        int scarfTail = (int) (rideTime / 160 % 2);
        box(canvas, -12, -24 + scarfTail, -6, -22 + scarfTail, 0xffc96d51);
        box(canvas, -15, -23 + scarfTail, -11, -22 + scarfTail, 0xffe69e68);
        canvas.restore();
        paint.setColorFilter(null);
    }

    private void wheel(Canvas canvas, int x, int y, float rotation) {
        dot(canvas, x, y, 6, INK);
        dot(canvas, x, y, 4, 0xffbfb095);
        dot(canvas, x, y, 3, 0xff5e5858);
        int dx = Math.round((float) Math.cos(rotation) * 4);
        int dy = Math.round((float) Math.sin(rotation) * 4);
        line(canvas, x - dx, y - dy, x + dx, y + dy, 0xffc6b59c, 1);
        line(canvas, x - dy, y + dx, x + dy, y - dx, 0xffb6aa95, 1);
        box(canvas, x - 1, y - 1, x + 1, y + 1, 0xffddbe85);
    }

    void drawStillLife(Canvas canvas, int x, int floor, float shade) {
        int wood = SceneColors.mix(0xffaf7258, 0xff705146, shade);
        int green = SceneColors.mix(0xff4e7360, 0xff41605a, shade);
        int light = SceneColors.mix(0xff8b9965, 0xff688074, shade);
        box(canvas, x - 10, floor + 1, x + 31, floor + 3, 0x5531242c);
        box(canvas, x - 4, floor - 12, x + 8, floor - 10, wood);
        box(canvas, x - 3, floor - 10, x + 7, floor - 1, wood);
        box(canvas, x - 2, floor - 9, x + 1, floor - 2, SceneColors.mix(0xffd19a71, 0xff826052, shade));
        line(canvas, x + 2, floor - 12, x + 3, floor - 24, green, 2);
        box(canvas, x - 4, floor - 20, x + 2, floor - 17, green);
        box(canvas, x - 7, floor - 22, x - 2, floor - 19, light);
        box(canvas, x + 3, floor - 26, x + 6, floor - 20, light);
        box(canvas, x + 6, floor - 23, x + 10, floor - 20, green);
        box(canvas, x + 1, floor - 30, x + 4, floor - 25, green);
        box(canvas, x + 19, floor - 5, x + 39, floor, SceneColors.mix(0xffa35a54, 0xff62566e, shade));
        box(canvas, x + 17, floor - 9, x + 35, floor - 5, SceneColors.mix(0xff678575, 0xff697366, shade));
        box(canvas, x + 19, floor - 8, x + 33, floor - 6, SceneColors.mix(0xffd5b599, 0xffa58a73, shade));
    }

    private void box(Canvas canvas, int left, int top, int right, int bottom, int color) {
        paint.setColor(color);
        canvas.drawRect(left, top, right, bottom, paint);
    }

    private void dot(Canvas canvas, int x, int y, float radius, int color) {
        paint.setColor(color);
        canvas.drawCircle(x, y, radius, paint);
    }

    private void line(Canvas canvas, int startX, int startY, int endX, int endY, int color, float width) {
        paint.setColor(color);
        paint.setStrokeWidth(width);
        canvas.drawLine(startX, startY, endX, endY, paint);
    }
}
