package com.inkspire.rogue;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.inkspire.rogue.ui.GameView;

public final class MainActivity extends Activity {
    private GameView game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        installCrashLogger();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemBars();
        game = new GameView(this);
        setContentView(game);
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void hideSystemBars() {
        // Keep this deliberately simple: Window.getInsetsController() can be null before the
        // decor is attached on some vendor builds, and an NPE here would crash before first frame.
        Window w = getWindow();
        w.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (game != null) game.onHostPause();
    }

    @Override
    public void onBackPressed() {
        if (game == null || !game.handleBack()) super.onBackPressed();
    }

    private void installCrashLogger() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                java.io.File dir = getExternalFilesDir(null);
                if (dir == null) dir = getFilesDir();
                java.io.File out = new java.io.File(dir, "last-crash.txt");
                java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(out, false));
                pw.println("Ink Spire crash: " + new java.util.Date());
                error.printStackTrace(pw);
                pw.close();
            } catch (Throwable ignored) { }
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }
}
