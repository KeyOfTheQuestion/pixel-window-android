package local.pixel.window;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

import static local.pixel.window.DoorSwing.BOTTOM;
import static local.pixel.window.DoorSwing.LEFT;
import static local.pixel.window.DoorSwing.RIGHT;
import static local.pixel.window.DoorSwing.TOP;

final class BookshopDoor {
    // Glass edges in the original day and night backdrops, in source pixels.
    private static final int[] DAY_COLUMNS = {959, 982, 986, 1008, 1013, 1036};
    private static final int[] DAY_ROWS = {419, 455, 458, 494, 501, 536};
    private static final int[] NIGHT_COLUMNS = {958, 983, 988, 1010, 1015, 1040};
    private static final int[] NIGHT_ROWS = {419, 457, 461, 496, 502, 537};

    private final Artwork day;
    private final Artwork night;
    private final RectF aperture = new RectF(LEFT, TOP, RIGHT, BOTTOM);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Matrix transform = new Matrix();
    private final float[] source = new float[8];
    private final float[] panel = new float[8];

    BookshopDoor(Bitmap dayBackdrop, Bitmap nightBackdrop, int sceneWidth, int sceneHeight) {
        day = artwork(dayBackdrop, sceneWidth, sceneHeight, DAY_COLUMNS, DAY_ROWS, 0xff573a28);
        night = artwork(nightBackdrop, sceneWidth, sceneHeight, NIGHT_COLUMNS, NIGHT_ROWS, 0xff603923);
    }

    void draw(Canvas canvas, float openness, float shade) {
        DoorSwing.project(openness, panel);
        if (shade < 1) {
            drawArtwork(canvas, day);
        }
        if (shade > 0) {
            if (shade < 1) {
                canvas.saveLayerAlpha(aperture, Math.round(255 * shade));
            }
            drawArtwork(canvas, night);
            if (shade < 1) {
                canvas.restore();
            }
        }
    }

    private void drawArtwork(Canvas canvas, Artwork artwork) {
        canvas.drawBitmap(artwork.room, null, aperture, paint);
        source[2] = source[4] = artwork.panel.getWidth();
        source[5] = source[7] = artwork.panel.getHeight();
        transform.setPolyToPoly(source, 0, panel, 0, 4);
        canvas.drawBitmap(artwork.panel, transform, paint);
    }

    private static Artwork artwork(Bitmap backdrop, int sceneWidth, int sceneHeight,
                                   int[] columns, int[] rows, int floorColor) {
        float scaleX = backdrop.getWidth() / (float) sceneWidth;
        float scaleY = backdrop.getHeight() / (float) sceneHeight;
        int left = Math.round(LEFT * scaleX);
        int top = Math.round(TOP * scaleY);
        int width = Math.round(RIGHT * scaleX) - left;
        int height = Math.round(BOTTOM * scaleY) - top;
        int[] frame = new int[width * height];
        backdrop.getPixels(frame, 0, width, left, top, width, height);
        int[] room = DoorGlazing.separate(frame, width, height, relative(columns, left), relative(rows, top), floorColor);
        return new Artwork(
            Bitmap.createBitmap(room, width, height, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(frame, width, height, Bitmap.Config.ARGB_8888));
    }

    private static int[] relative(int[] edges, int origin) {
        int[] offsets = new int[edges.length];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = edges[i] - origin;
        }
        return offsets;
    }

    private static final class Artwork {
        final Bitmap room;
        final Bitmap panel;

        Artwork(Bitmap room, Bitmap panel) {
            this.room = room;
            this.panel = panel;
            room.prepareToDraw();
            panel.prepareToDraw();
        }
    }
}
