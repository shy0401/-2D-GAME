package com.mygdx.game.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

/** 8×12(여러 캐릭터) 또는 3×4(단일 캐릭터) 시트를 모두 지원 + 안전 폴백 */
public class SpriteManagerGdx implements Disposable {
    private Texture sheet;
    private TextureRegion[][] grid;
    private final Map<String, TextureRegion> map = new HashMap<>();

    // 공용 폴백 (1×1 흰색 -> images/white.png, 없으면 badlogic.jpg)
    private static Texture fallbackTex;
    private static TextureRegion fallbackRegion;

    /** @param filePath 예: "sprites/Actor1.png"
     *  @param character 1..8 (8×12 시트일 때만 사용, 3×4 시트면 무시)
     */
    public SpriteManagerGdx(String filePath, int character) {
        this.sheet = loadTextureStrict(filePath); // 존재/용량 검사
        if (this.sheet == null) {
            installFallback();
            bakeFallback3x4();
            return;
        }

        this.sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        int sheetW = sheet.getWidth();
        int sheetH = sheet.getHeight();

        // 기본 가정은 8×12
        int rows = 8, cols = 12;
        if (sheetW % 12 != 0 || sheetH % 8 != 0) {
            // 3×4로 재시도
            rows = 4; cols = 3;
        }

        int cellW = safeDiv(sheetW, cols);
        int cellH = safeDiv(sheetH, rows);
        if (cellW <= 0 || cellH <= 0) {
            Gdx.app.error("SpriteManagerGdx", "Invalid cell size from sheet: " + sheetW + "x" + sheetH + " rows=" + rows + " cols=" + cols);
            installFallback();
            bakeFallback3x4();
            return;
        }

        this.grid = TextureRegion.split(sheet, cellW, cellH);

        // 프레임 채우기
        boolean ok = true;
        try {
            if (rows == 8 && cols == 12) {
                // 8×12: 캐릭터 블록(3×4)에서 character 선택
                int idx = clamp(character - 1, 0, 7); // 0..7
                int groupsPerRow = cols / 3; // 4
                int gx = idx % groupsPerRow; // 0..3
                int gy = idx / groupsPerRow; // 0..1
                int col0 = gx * 3, row0 = gy * 4;

                put3("down",  row0 + 0, col0);
                put3("left",  row0 + 1, col0);
                put3("right", row0 + 2, col0);
                put3("up",    row0 + 3, col0);
            } else {
                // 3×4: 단일 캐릭터 시트
                put3("down",  0, 0);
                put3("left",  1, 0);
                put3("right", 2, 0);
                put3("up",    3, 0);
            }
        } catch (Throwable t) {
            ok = false;
            Gdx.app.error("SpriteManagerGdx", "Grid index error. Fallback will be used for " + filePath, t);
        }

        if (!ok) {
            installFallback();
            bakeFallback3x4();
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int safeDiv(int a, int b) {
        return (b == 0) ? 0 : (a / b);
    }

    private void put3(String dir, int row, int col0) {
        // 범위 검증
        if (row < 0 || row >= grid.length) throw new ArrayIndexOutOfBoundsException("row=" + row);
        if (col0 < 0 || col0 + 2 >= grid[row].length) throw new ArrayIndexOutOfBoundsException("col0=" + col0);

        map.put(dir + "1", grid[row][col0]);
        map.put(dir + "2", grid[row][col0 + 1]);
        map.put(dir + "3", grid[row][col0 + 2]);
    }

    private static TextureRegion ensureFallbackRegion() {
        if (fallbackRegion != null) return fallbackRegion;

        // 1) images/white.png (권장: 1×1)
        FileHandle white = Gdx.files.internal("images/white.png");
        if (white.exists() && white.length() >= 8) {
            fallbackTex = new Texture(white);
            fallbackTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            fallbackRegion = new TextureRegion(fallbackTex);
            return fallbackRegion;
        }

        // 2) badlogic.jpg (프로젝트에 있을 때만)
        FileHandle badlogic = Gdx.files.internal("badlogic.jpg");
        if (badlogic.exists()) {
            fallbackTex = new Texture(badlogic);
            fallbackTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            fallbackRegion = new TextureRegion(fallbackTex);
            return fallbackRegion;
        }

        return null;
    }

    private void installFallback() {
        TextureRegion fb = ensureFallbackRegion();
        if (fb == null) {
            // 마지막 보루: sheet가 있다면 그 중 1×1 잘라 쓰기 (정말 비상용)
            if (sheet != null) {
                fallbackRegion = new TextureRegion(sheet, 0, 0, 1, 1);
            } else {
                throw new RuntimeException("No fallback texture available (images/white.png or badlogic.jpg missing).");
            }
        }
    }

    /** 폴백으로 3×4 키 세트 채우기 */
    private void bakeFallback3x4() {
        TextureRegion f = (fallbackRegion != null) ? fallbackRegion
            : new TextureRegion(sheet, 0, 0, 1, 1);
        map.clear();
        map.put("down1", f); map.put("down2", f); map.put("down3", f);
        map.put("left1", f); map.put("left2", f); map.put("left3", f);
        map.put("right1", f); map.put("right2", f); map.put("right3", f);
        map.put("up1", f); map.put("up2", f); map.put("up3", f);
    }

    private static Texture loadTextureStrict(String path) {
        FileHandle fh = Gdx.files.internal(path);
        if (!fh.exists()) {
            Gdx.app.error("SpriteManagerGdx", "Texture not found: " + path);
            return null;
        }
        // PNG 최소 헤더(8바이트)도 못 미치면 손상 가능성이 큼
        if (fh.length() < 8) {
            Gdx.app.error("SpriteManagerGdx", "Texture seems corrupted/empty: " + path + " (length=" + fh.length() + ")");
            return null;
        }
        return new Texture(fh);
    }

    public TextureRegion get(String key) { return map.get(key); }

    @Override
    public void dispose() {
        // 폴백 텍스처는 전역 공유 → 여기서 dispose 하지 않음
        if (sheet != null) {
            sheet.dispose();
            sheet = null;
        }
        grid = null;
        map.clear();
    }
}
