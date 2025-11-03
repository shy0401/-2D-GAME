package com.mygdx.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Hud {
    private final Texture bg; // 1x1 텍스처

    public Hud() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1,1,1,1);
        pm.fill();
        bg = new Texture(pm);
        pm.dispose();
    }

    public void drawDialogue(SpriteBatch batch, BitmapFont font, String text, int screenW, int screenH) {
        int pad = 16;
        int boxW = screenW - 40;
        int boxH = 120;
        int x = 20;
        int y = 40;

        // 반투명 검은 박스
        batch.setColor(0, 0, 0, 0.8f);
        batch.draw(bg, x, y, boxW, boxH);
        batch.setColor(Color.WHITE);

        // 텍스트
        font.draw(batch, text, x + pad, y + boxH - pad);
    }

    public void dispose() {
        bg.dispose();
    }
}
