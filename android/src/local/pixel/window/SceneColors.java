package local.pixel.window;

import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;

final class SceneColors {
    private SceneColors() {}

    static int mix(int day, int night, float shade) {
        return Color.rgb(
            channel(Color.red(day), Color.red(night), shade),
            channel(Color.green(day), Color.green(night), shade),
            channel(Color.blue(day), Color.blue(night), shade));
    }

    static ColorMatrixColorFilter tint(float shade, float red, float green, float blue) {
        return new ColorMatrixColorFilter(new float[] {
            1 + shade * (red - 1), 0, 0, 0, 0,
            0, 1 + shade * (green - 1), 0, 0, 0,
            0, 0, 1 + shade * (blue - 1), 0, 0,
            0, 0, 0, 1, 0
        });
    }

    private static int channel(int day, int night, float shade) {
        return Math.round(day + (night - day) * shade);
    }
}
