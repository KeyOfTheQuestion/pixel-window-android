package local.pixel.window;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;
import java.time.Instant;
import java.time.LocalDate;

final class StreetView extends View {
    private static final int WIDTH = 440;
    private static final int HEIGHT = 200;
    private static final int SILL = 187;
    private static final int[][] NIGHT_STARS = {{145, 7}, {152, 11}, {172, 23}, {183, 14}};
    private static final int[] DAY_LEAVES = {0xffefaa62, 0xffcf743d, 0xffeab776};
    private static final int[] NIGHT_LEAVES = {0xff746159, 0xff8e6858, 0xff856e65};
    private final Bitmap day;
    private final Bitmap night;
    private final Bitmap pixels = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
    private final Canvas street = new Canvas(pixels);
    private final Paint paint = new Paint();
    private final Paint sceneryPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF display = new RectF();
    private final PixelActors actors = new PixelActors();
    private final BookshopVisitors visitors;
    private final CatSprite catSprite;
    private final CatMotion catMotion = new CatMotion();
    private final long opened = SystemClock.uptimeMillis();
    private boolean active;
    private LocalDate solarDate;
    private SceneTime.SolarDay solarDay;
    private float clockShade;
    private long checkedAt = Long.MIN_VALUE / 2;
    private int mode;

    StreetView(Context context) {
        super(context);
        day = BitmapFactory.decodeResource(getResources(), R.drawable.street_day);
        night = BitmapFactory.decodeResource(getResources(), R.drawable.street_night);
        day.prepareToDraw();
        night.prepareToDraw();
        catSprite = new CatSprite(getResources());
        visitors = new BookshopVisitors(getResources(), new BookshopDoor(day, night, WIDTH, HEIGHT));
        setContentDescription("像素窗景。街道上的行人和骑车人、茶书店里读书的人、窗台上的猫。长按可切换昼夜。");
    }

    int lighting() {
        return mode;
    }

    void setLighting(int selection) {
        mode = selection;
        if (mode == 0) {
            checkedAt = Long.MIN_VALUE / 2;
        }
        invalidate();
    }

    void setActive(boolean value) {
        active = value;
        if (value) {
            checkedAt = Long.MIN_VALUE / 2;
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float scale = Math.min(width / (float) WIDTH, height / (float) HEIGHT);
        float left = (width - WIDTH * scale) / 2f;
        float top = (height - HEIGHT * scale) / 2f;
        display.set(left, top, width - left, height - top);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        if (mode == 0 && now - checkedAt >= 2_000) {
            var clock = SceneTime.beijing(Instant.now());
            LocalDate date = clock.toLocalDate();
            if (!date.equals(solarDate)) {
                solarDate = date;
                solarDay = SceneTime.solarDay(date);
            }
            clockShade = solarDay.nightMix(clock.toLocalTime().toSecondOfDay() / 60.0);
            checkedAt = now;
        }
        float shade = mode == 0 ? clockShade : mode == 2 ? 1 : 0;
        long elapsed = now - opened;
        street.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        air(elapsed, shade);
        float bike = StreetTraffic.bikeX(elapsed);
        boolean bikeVisible = !Float.isNaN(bike);
        if (bikeVisible) {
            actors.drawBike(street, Math.round(bike), 182, StreetTraffic.goingRight(elapsed),
                StreetTraffic.rideTime(elapsed), shade);
        }
        frame(shade);
        canvas.drawColor(SceneColors.mix(0xff251805, 0xff17121d, shade));
        if (shade < 1) {
            sceneryPaint.setAlpha(255);
            canvas.drawBitmap(day, null, display, sceneryPaint);
        }
        if (shade > 0) {
            sceneryPaint.setAlpha(Math.round(255 * shade));
            canvas.drawBitmap(night, null, display, sceneryPaint);
        }

        canvas.save();
        mapToWindow(canvas);
        boolean visitorsMoving = visitors.draw(canvas, elapsed, shade);
        canvas.restore();

        paint.setAlpha(255);
        canvas.drawBitmap(pixels, null, display, paint);

        catMotion.update(elapsed);
        boolean catVisiting = catMotion.stage() != CatMotion.Stage.OFFSCREEN;
        if (catVisiting) {
            canvas.save();
            mapToWindow(canvas);
            catSprite.draw(canvas, catMotion, SILL + 1, shade);
            canvas.restore();
        }
        if (active && getWindowVisibility() == VISIBLE) {
            if (catVisiting || visitorsMoving || bikeVisible) {
                postInvalidateOnAnimation();
            } else {
                postInvalidateDelayed(32);
            }
        }
    }

    private void mapToWindow(Canvas canvas) {
        canvas.translate(display.left, display.top);
        canvas.scale(display.width() / WIDTH, display.height() / HEIGHT);
        canvas.clipRect(4, 5, WIDTH - 4, HEIGHT);
    }

    private void air(long elapsed, float shade) {
        for (int i = 0; i < 7; i++) {
            long pass = SceneTime.phase(elapsed, 735L * i, 4_250 + i * 165L);
            if (pass > 3_350) {
                continue;
            }
            float t = pass / 3_350f;
            int x = 8 + i * 18 + Math.round(73 * t);
            int y = 32 + i * 13 + Math.round(45 * t + 6 * (float) Math.sin(t * 9 + i));
            int color = SceneColors.mix(DAY_LEAVES[i % 3], NIGHT_LEAVES[i % 3], shade);
            rect(x, y, x + 2, y + 1, color);
            rect(x + 1, y + 1, x + 3, y + 2, color);
        }
        if (shade > 0) {
            for (int index = 0; index < NIGHT_STARS.length; index++) {
                int x = NIGHT_STARS[index][0];
                int y = NIGHT_STARS[index][1];
                int alpha = Math.round(shade * (85 + (float) (60 * (1 + Math.sin(elapsed / 980.0 + index)))));
                rect(x, y, x + 1, y + 1, (alpha << 24) | 0x00decbdc);
            }
        }
    }

    private void frame(float shade) {
        rect(0, SILL, WIDTH, HEIGHT, SceneColors.mix(0xff4e3028, 0xff281b24, shade));
        rect(0, SILL + 1, WIDTH, SILL + 3, SceneColors.mix(0xff966144, 0xff633e37, shade));
        rect(0, 193, WIDTH, 194, SceneColors.mix(0xff73503d, 0xff432a2d, shade));
        rect(0, 0, 4, HEIGHT, 0xff2b1c20);
        rect(WIDTH - 4, 0, WIDTH, HEIGHT, 0xff2b1c20);
        rect(0, 0, WIDTH, 4, 0xff2b1c20);
        rect(4, 4, WIDTH - 4, 5, SceneColors.mix(0xff815844, 0xff3f2b2d, shade));
        int edge = SceneColors.mix(0xff8d6049, 0xff4d3230, shade);
        rect(4, 5, 5, SILL, edge);
        rect(WIDTH - 5, 5, WIDTH - 4, SILL, edge);
        actors.drawStillLife(street, 29, SILL + 1, shade);
    }

    private void rect(int x, int y, int right, int bottom, int color) {
        paint.setColor(color);
        street.drawRect(x, y, right, bottom, paint);
    }
}
