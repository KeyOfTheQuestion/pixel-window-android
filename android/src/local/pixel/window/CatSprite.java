package local.pixel.window;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

final class CatSprite {
    private static final int ART_WIDTH = 314;
    private static final int ART_HEIGHT = 234;
    private static final float WIDTH = 25;
    private static final float SCALE = WIDTH / ART_WIDTH;
    private static final Rect STANDING = new Rect(1112, 54, 1426, 288);
    private static final Rect SLEEP = new Rect(737, 843, 1223, 1095);
    private static final float[] ROOT_X = {101, 207, 127, 246};
    private static final float[] ROOT_Y = {160, 153, 154, 152};
    private static final float[] TOE_X = {99, 216, 120, 247};
    private static final int[] FUR = {0xffb67757, 0xffb67757, 0xffdf9960, 0xffe0a06a};

    private final Bitmap body;
    private final Bitmap resting;
    private final CatGait gait = new CatGait();
    private final Paint sprite = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fur = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bodyMask = new Path();
    private final Path leg = new Path();
    private final RectF bodyBox = new RectF();
    private final RectF sleepBox = new RectF();
    private int tintStep = -1;

    CatSprite(Resources resources) {
        body = BitmapFactory.decodeResource(resources, R.drawable.cat_walk);
        resting = BitmapFactory.decodeResource(resources, R.drawable.cat_sprites);
        body.prepareToDraw();
        resting.prepareToDraw();
        fur.setStrokeJoin(Paint.Join.ROUND);
        fur.setStrokeCap(Paint.Cap.ROUND);
        bodyMask.moveTo(0, 0);
        bodyMask.lineTo(ART_WIDTH, 0);
        bodyMask.lineTo(ART_WIDTH, 170);
        bodyMask.lineTo(263, 170);
        bodyMask.quadTo(234, 185, 216, 160);
        bodyMask.quadTo(181, 174, 148, 170);
        bodyMask.quadTo(126, 185, 105, 175);
        bodyMask.lineTo(89, 166);
        bodyMask.lineTo(0, 166);
        bodyMask.close();
    }

    void draw(Canvas canvas, CatMotion motion, float sill, float shade) {
        int tint = Math.round(shade * 255);
        if (tint != tintStep) {
            tintStep = tint;
            var filter = SceneColors.tint(tint / 255f, .87f, .77f, .74f);
            sprite.setColorFilter(filter);
            fur.setColorFilter(filter);
        }
        float x = motion.position();
        long age = motion.stateAge();
        CatMotion.Stage stage = motion.stage();
        shadow.setColor(0x4e241a1e);
        canvas.drawOval(x - 12.5f, sill - 1.05f, x + 12.5f, sill + .9f, shadow);

        if (stage == CatMotion.Stage.RESTING) {
            float curl = smooth(Math.min(age / 300f, (CatMotion.REST_TIME - age) / 420f));
            gait.sample(0, 0);
            drawCat(canvas, x, sill, false, 0, 1 - curl);
            if (curl > 0) {
                float breathing = 1 + .01f * (float) Math.sin(age / 650.0);
                float height = WIDTH * SLEEP.height() / SLEEP.width() * breathing;
                sleepBox.set(x - WIDTH / 2, sill - height, x + WIDTH / 2, sill);
                sprite.setAlpha(Math.round(255 * curl));
                canvas.drawBitmap(resting, SLEEP, sleepBox, sprite);
            }
        } else if (stage == CatMotion.Stage.ALERT || stage == CatMotion.Stage.WAITING) {
            float crouch = stage == CatMotion.Stage.WAITING ? 1 : smooth(age / 144f);
            gait.sample(0, 0);
            drawCat(canvas, x, sill, motion.facingRight(), 16 * crouch, 1);
        } else {
            float settle = stage == CatMotion.Stage.RETREATING ? 16 * (1 - smooth(age / 180f)) : 0;
            gait.sample(motion.walked(), motion.gaitStrength());
            drawCat(canvas, x, sill, motion.facingRight(), settle, 1);
        }
    }

    private void drawCat(Canvas canvas, float x, float sill, boolean right, float crouch, float opacity) {
        if (opacity <= 0) {
            return;
        }
        int alpha = Math.round(255 * opacity);
        float bodyY = 6 + crouch - gait.rise() / SCALE;
        canvas.save();
        canvas.translate(x, sill);
        canvas.scale(right ? -SCALE : SCALE, SCALE);
        canvas.translate(-ART_WIDTH / 2f, -ART_HEIGHT);
        for (int paw = 0; paw < ROOT_X.length; paw++) {
            drawLeg(canvas, paw, bodyY, alpha);
        }
        canvas.translate(0, bodyY);
        canvas.clipPath(bodyMask);
        bodyBox.set(0, 0, ART_WIDTH, ART_HEIGHT);
        sprite.setAlpha(alpha);
        canvas.drawBitmap(body, STANDING, bodyBox, sprite);
        canvas.restore();
    }

    private void drawLeg(Canvas canvas, int paw, float bodyY, int alpha) {
        boolean far = paw < 2;
        boolean hind = paw % 2 == 1;
        float rootX = ROOT_X[paw];
        float rootY = ROOT_Y[paw] + bodyY;
        float toeX = TOE_X[paw] + gait.offset(paw) / SCALE;
        float lift = gait.lift(paw) / SCALE;
        float ground = (far ? 230 : ART_HEIGHT) - lift;
        float jointX = .40f * rootX + .60f * toeX + (hind ? 13 + .36f * lift : 2 + .16f * lift);
        float jointY = .49f * rootY + .51f * ground - .16f * lift;
        float thigh = hind ? 16 : 13;

        leg.rewind();
        leg.moveTo(rootX - thigh, rootY - 10);
        leg.quadTo(rootX, rootY - 20, rootX + thigh, rootY - 6);
        leg.quadTo(jointX + 14, jointY, toeX + 11, ground - 11);
        leg.quadTo(toeX + 13, ground, toeX + 2, ground);
        leg.quadTo(toeX - 16, ground + 1, toeX - 14, ground - 7);
        leg.quadTo(toeX - 13, ground - 11, toeX - 8, ground - 13);
        leg.quadTo(jointX - 12, jointY + 2, rootX - thigh, rootY - 10);
        leg.close();

        fur.setColor(FUR[paw]);
        fur.setAlpha(alpha);
        canvas.drawPath(leg, fur);
        canvas.save();
        canvas.clipPath(leg);
        fur.setColor(far ? 0xff96583d : 0xffb67043);
        fur.setAlpha(alpha);
        fur.setStrokeWidth(far ? 5 : 7);
        canvas.drawLine(jointX - 13, jointY - 4, jointX + 13, jointY + 2, fur);
        fur.setColor(far ? 0xffc89474 : 0xfff4c9a0);
        fur.setAlpha(alpha);
        canvas.drawOval(toeX - 16, ground - 14, toeX + 13, ground + 1, fur);
        canvas.restore();
        fur.setStyle(Paint.Style.STROKE);
        fur.setStrokeWidth(3);
        fur.setColor(far ? 0xff5e382d : 0xff79482c);
        fur.setAlpha(alpha);
        canvas.drawPath(leg, fur);
        fur.setStyle(Paint.Style.FILL);
    }

    private static float smooth(float value) {
        float t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }
}
