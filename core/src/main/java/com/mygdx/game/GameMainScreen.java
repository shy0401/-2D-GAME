package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameMainScreen implements Screen {
    private final MainGame game;
    private Stage stage;
    private Texture bgTex;
    private Skin skin;
    private com.badlogic.gdx.audio.Music menuMusic;
    private Viewport viewport;

    private BitmapFont uiFont;
    private BitmapFont uiFontLarge;
    private BitmapFont titleFont;

    public GameMainScreen(MainGame game, BitmapFont uiFont, BitmapFont uiFontLarge) {
        this.game = game;
        this.uiFont = uiFont;
        this.uiFontLarge = (uiFontLarge != null) ? uiFontLarge : uiFont;
        this.titleFont = this.uiFontLarge; // 타이틀은 크게
    }

    public GameMainScreen(MainGame game) { this(game, Fonts.ui, Fonts.uiLarge); }

    @Override
    public void show() {
        viewport = new FitViewport(1024, 1024);
        stage = new Stage(viewport, game.batch);
        Gdx.input.setInputProcessor(stage);

        bgTex = loadTex("images/background1.jpg");

        // 간소 스킨
        skin = makeMinimalSkin(uiFont);

        // 타이틀
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = titleFont;
        titleStyle.fontColor = Color.WHITE;

        Label title = new Label("The Dream", titleStyle);
        title.setAlignment(Align.center);

        // 버튼
        TextButton startBtn    = new TextButton("게임시작", skin);
        TextButton settingsBtn = new TextButton("설정", skin);
        TextButton exitBtn     = new TextButton("종료", skin);

        startBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (menuMusic != null) menuMusic.stop();
                Gdx.input.setInputProcessor(null);
                Gdx.app.postRunnable(() ->
                    game.setScreen(new PlayScreen(game, uiFont, uiFontLarge))
                );
            }
        });
        settingsBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { }
        });
        exitBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        // 레이아웃
        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(10);
        root.add(title).padBottom(30).row();
        root.add(startBtn).width(260).height(72).row();
        root.add(settingsBtn).width(260).height(60).row();
        root.add(exitBtn).width(260).height(60);
        stage.addActor(root);

        // BGM
        menuMusic = safeMusic("music/intro.ogg");
        if (menuMusic == null) menuMusic = safeMusic("music/intro.mp3");
        if (menuMusic != null) {
            menuMusic.setLooping(true);
            menuMusic.setVolume(0.85f);
            menuMusic.play();
        }
    }

    private com.badlogic.gdx.audio.Music safeMusic(String path) {
        try {
            FileHandle h = Gdx.files.internal(path);
            if (!h.exists()) return null;
            return Gdx.audio.newMusic(h);
        } catch (Throwable t) {
            Gdx.app.error("MUSIC", "load fail: " + path, t);
            return null;
        }
    }

    private Texture loadTex(String path) {
        FileHandle h = Gdx.files.internal(path);
        if (h.exists()) return new Texture(h);
        String base = path.replaceFirst("\\.[^.]+$", "");
        for (String ext : new String[]{".png",".jpg",".jpeg"}) {
            FileHandle c = Gdx.files.internal(base + ext);
            if (c.exists()) return new Texture(c);
        }
        return null;
    }

    private Skin makeMinimalSkin(BitmapFont font) {
        Skin s = new Skin();
        Pixmap pmUp = new Pixmap(4,4, Pixmap.Format.RGBA8888);
        pmUp.setColor(0,0,0,0.75f); pmUp.fill();
        Texture upTex = new Texture(pmUp); pmUp.dispose();
        s.add("btnUp", upTex);

        Pixmap pmDown = new Pixmap(4,4, Pixmap.Format.RGBA8888);
        pmDown.setColor(0.15f,0.15f,0.15f,0.9f); pmDown.fill();
        Texture downTex = new Texture(pmDown); pmDown.dispose();
        s.add("btnDown", downTex);

        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.up   = new TextureRegionDrawable(new TextureRegion(upTex));
        tbs.down = new TextureRegionDrawable(new TextureRegion(downTex));
        tbs.font = font;
        tbs.fontColor = Color.YELLOW;
        s.add("default", tbs);

        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = font;
        ls.fontColor = Color.WHITE;
        s.add("default", ls);

        return s;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.begin();
        if (bgTex != null) game.batch.draw(bgTex, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }

    @Override
    public void dispose() {
        if (menuMusic != null) { menuMusic.dispose(); menuMusic = null; }
        if (stage != null)     { stage.dispose(); stage = null; }
        if (bgTex != null)     { bgTex.dispose();  bgTex = null; }
        if (skin != null)      { skin.dispose();   skin = null; }
    }
}
