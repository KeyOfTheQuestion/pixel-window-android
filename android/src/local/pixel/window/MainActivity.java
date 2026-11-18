package local.pixel.window;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

public final class MainActivity extends Activity {
    private StreetView scene;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setDecorFitsSystemWindows(false);
        WindowManager.LayoutParams window = getWindow().getAttributes();
        window.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        getWindow().setAttributes(window);
        scene = new StreetView(this);
        scene.setOnLongClickListener(view -> {
            String[] options = {"跟随北京日出日落", "白天", "夜晚"};
            new AlertDialog.Builder(this)
                .setTitle("窗外光线")
                .setSingleChoiceItems(options, scene.lighting(), (dialog, selection) -> {
                    scene.setLighting(selection);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
            return true;
        });
        setContentView(scene);
        hideBars();
    }

    @Override
    protected void onResume() {
        super.onResume();
        scene.setActive(true);
        hideBars();
    }

    @Override
    protected void onPause() {
        scene.setActive(false);
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) {
            hideBars();
        }
    }

    private void hideBars() {
        WindowInsetsController bars = getWindow().getInsetsController();
        if (bars != null) {
            bars.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            bars.hide(WindowInsets.Type.systemBars());
        }
    }
}
