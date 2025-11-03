package com.mygdx.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import java.io.File;
import java.nio.file.Paths;

public class DesktopLauncher {

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    public static void main (String[] args) {
        // 안전 tmp / 로케일
        if (isWindows()) {
            File tmp = new File("C:/gdx_tmp");
            if (!tmp.exists()) tmp.mkdirs();
            System.setProperty("java.io.tmpdir", tmp.getAbsolutePath());
        }
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("org.lwjgl.system.allocator", "system");
        System.setProperty("user.language", "ko");
        System.setProperty("user.country", "KR");

        System.out.println("[WD] " + Paths.get("").toAbsolutePath());
        System.out.println("[TMP] " + System.getProperty("java.io.tmpdir"));

        boolean useAngle = Boolean.parseBoolean(System.getProperty("useAngle", "true"));

        // 빠른 에셋 확인(core/assets 기준으로 실행해야 true)
        System.out.println("[ASSET-CHECK] images/book_dark.png=" + new File("images/book_dark.png").exists());

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("The Dream");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);

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

        new Lwjgl3Application(new MainGame(), config);
    }
}
