package com.mygdx.game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mygdx.game.MainGame;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class Lwjgl3Launcher {

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    private static void ensureTmpDir() {
        if (isWindows()) {
            File tmp = new File("C:/gdx_tmp");
            if (!tmp.exists()) tmp.mkdirs();
        }
    }

    private static void setupSystemProperties() {
        if (isWindows()) {
            ensureTmpDir();
            System.setProperty("java.io.tmpdir", "C:/gdx_tmp");
        }
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.language", "ko");
        System.setProperty("user.country", "KR");
        System.setProperty("org.lwjgl.system.allocator", "system"); // jemalloc 회피
        // Optional debug:
        // System.setProperty("org.lwjgl.util.Debug", "true");
        // System.setProperty("org.lwjgl.util.DebugLoader", "true");

        System.out.println("[WD] " + Paths.get("").toAbsolutePath());
        System.out.println("[TMP] " + System.getProperty("java.io.tmpdir"));
    }

    private static Lwjgl3ApplicationConfiguration createConfig() {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("TheDream");
        config.setWindowedMode(1024, 1024);
        config.setResizable(true);
        config.useVsync(true);

        boolean useAngle = Boolean.parseBoolean(System.getProperty("useAngle", isWindows() ? "true" : "false"));
        try {
            if (useAngle) {
                config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 3, 0);
                System.out.println("[GL] ANGLE (GLES 3.0)");
            } else {
                config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL32, 3, 2);
                System.out.println("[GL] Desktop OpenGL 3.2");
            }
        } catch (Throwable t) {
            System.out.println("[GL] Fallback → ANGLE GLES 2.0 : " + t.getMessage());
            config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 2, 0);
        }
        return config;
    }

    public static void main(String[] args) {
        // 전역 예외 로그
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            ensureTmpDir();
            try (PrintWriter pw = new PrintWriter(isWindows() ? "C:/gdx_tmp/crash.log" : "crash.log", "UTF-8")) {
                e.printStackTrace(pw);
            } catch (Exception ignore) {}
            System.exit(1);
        });

        setupSystemProperties();
        Lwjgl3ApplicationConfiguration config = createConfig();

        // 에셋 경로 빠른 점검 (core/assets 기준)
        File f1 = new File("maps/room.tmx");
        File f2 = new File("images/book_dark.png");
        System.out.println("[ASSET-CHECK] maps/room.tmx=" + f1.exists());
        System.out.println("[ASSET-CHECK] images/book_dark.png=" + f2.exists());

        // 앱 1회 실행
        new Lwjgl3Application(new MainGame(), config);
    }
}
