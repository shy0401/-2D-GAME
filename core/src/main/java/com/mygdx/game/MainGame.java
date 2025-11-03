package com.mygdx.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MainGame extends Game {
    public SpriteBatch batch;

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_INFO);
        // 한글 폰트(증분) 준비
        Fonts.init();
        batch = new SpriteBatch();
        // 메인 메뉴 시작 (폰트 주입)
        setScreen(new GameMainScreen(this, Fonts.ui, Fonts.uiLarge));
    }

    /** 이전 스크린 안전 해제 후 전환 */
    public void switchScreen(Screen next) {
        Screen prev = getScreen();
        setScreen(next);
        if (prev != null) {
            try { prev.dispose(); } catch (Throwable ignored) {}
        }
    }

    @Override public void render() { super.render(); }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            try { getScreen().dispose(); } catch (Throwable ignored) {}
        }
        if (batch != null) batch.dispose();
        Fonts.dispose();  // 앱 종료 시점에만 generator/폰트 동시 해제
    }
}
