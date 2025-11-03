package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public final class Fonts {
    // 증분 폰트가 살아있는 동안 generator도 반드시 살아 있어야 함
    private static FreeTypeFontGenerator GEN;
    public static BitmapFont ui;       // 일반 UI
    public static BitmapFont uiLarge;  // 큰 UI/타이틀

    private Fonts() {}

    public static void init() {
        if (ui != null && uiLarge != null) return;

        // TTF 선택 (프로젝트 assets/fonts/ 에 실제 존재해야 함)
        FileHandle ttf = null;
        for (String path : new String[]{
            "fonts/NotoSansKR-Regular.ttf",
            "fonts/NanumGothic.ttf"
        }) {
            FileHandle fh = Gdx.files.internal(path);
            if (fh.exists()) { ttf = fh; break; }
        }
        if (ttf == null) {
            // 최후 폴백: BitmapFont (한글은 네모로 나옴)
            ui = new BitmapFont();
            ui.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            uiLarge = new BitmapFont();
            uiLarge.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Gdx.app.error("FONTS", "Korean TTF not found. Using default bitmap fonts.");
            return;
        }

        // ✅ 커스텀 PixmapPacker 사용 금지: 내부 packer를 사용해 충돌원인 제거
        GEN = new FreeTypeFontGenerator(ttf);

        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 22;
        p.incremental = true;                // 핵심: 필요한 글리프만 런타임 생성
        p.kerning = true;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        // p.characters = null; // (기본) 전체 허용

        ui = GEN.generateFont(p);

        FreeTypeFontGenerator.FreeTypeFontParameter p2 = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p2.size = 34;
        p2.incremental = true;
        p2.kerning = true;
        p2.minFilter = Texture.TextureFilter.Linear;
        p2.magFilter = Texture.TextureFilter.Linear;

        uiLarge = GEN.generateFont(p2);

        Gdx.app.log("FONTS", "Loaded: " + ttf.path());
    }

    public static void dispose() {
        // 순서: 폰트 → 제너레이터
        try { if (ui != null) ui.dispose(); } catch (Throwable ignored) {}
        try { if (uiLarge != null) uiLarge.dispose(); } catch (Throwable ignored) {}
        ui = null; uiLarge = null;

        try { if (GEN != null) GEN.dispose(); } catch (Throwable ignored) {}
        GEN = null;
    }
}
