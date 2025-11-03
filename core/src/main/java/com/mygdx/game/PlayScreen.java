// PlayScreen.java
// (전체 최신본, SFX 픽스 포함)
package com.mygdx.game;

import com.badlogic.gdx.audio.Sound; // ★ 추가
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.entity.NPC;
import com.mygdx.game.entity.Player;
import com.mygdx.game.entity.Projectile;
import com.mygdx.game.entity.Zombie;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectMap;

public class PlayScreen implements Screen {

    private static final float DOOR_NEAR_MARGIN = 24f;
    private final MainGame game;

    // ===== Camera / Map =====
    private OrthographicCamera camera;
    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;
    private TiledMapTileLayer collisionLayer;
    private int mapWidthPixels, mapHeightPixels;

    // ===== 화면/타일 규격 =====
    private static final int ORIGINAL_TILE = 16;
    private static final int SCALE = 4;
    private static final int TILE = ORIGINAL_TILE * SCALE;      // 64
    private static final int SCREEN_W = 1024 - 64;              // 960
    private static final int SCREEN_H = 1024 - 64;              // 960

    // ===== Entities =====
    private Player player;
    private final Array<Zombie> zombies = new Array<>();
    private final Array<Projectile> projectiles = new Array<>();
    private final ObjectMap<String, Float> variantSpeedCache = new ObjectMap<>();

    private NPC npc;

    // === 단일 좀비 시트(8마리 × 각 3×4 프레임) 설정 ===
    private static final String ZOMBIE_SHEET = "sprites/zombie.png";
    private static final int ZOMBIE_CHAR_COUNT = 8; // 4 across × 2 down

    private int randomZombieCharacter() {
        return MathUtils.random(0, ZOMBIE_CHAR_COUNT - 1);
    }

    private float getVariantSpeedFactor(String variantKey, int level) {
        String key = level + "|" + variantKey;
        Float cached = variantSpeedCache.get(key);
        if (cached != null) return cached;

        // 결정적 해시 → 0..1 실수
        int hash = (variantKey.hashCode() * 73856093) ^ (level * 19349663);
        float r = ((hash & 0x7fffffff) / (float)0x7fffffff); // 0..1

        // 속도 분산 범위 설정 (원하는 대로 조절: 0.85~1.35)
        float factor = 0.85f + r * 0.50f;

        variantSpeedCache.put(key, factor);
        return factor;
    }

    // ===== Textures =====
    private Texture keyTexture, doorClosedTexture;
    private Texture white1x1; // 페이드/패널/총알 공용(진짜 1x1 화이트 픽셀)
    private Texture victoryTexture;
    // 탄환 그릴 때의 픽셀 크기(정사각형 기준). 필요하면 값만 조절하면 됨.
    private float projSizeDefault = 18f; // 책 선택 전(arrow0.png) 기본 크기
    private float projSizeFire    = 28f;
    private float projSizeIce     = 26f;
    private float projSizeDark    = 26f;
    private float projSizeLight   = 26f;

    // 탄환 기본 텍스처(기존 arrow0.png를 패널과 분리)
    private Texture bulletTex;
    private Texture projDefaultTex, projFireTex, projIceTex, projDarkTex, projLightTex;
    private Animation<TextureRegion> animDefault, animFire, animIce, animDark, animLight;
    private TextureRegion whiteRegion;
    private float projFrameDuration = 0.06f;

    // 책별 탄환 시트 & 애니메이션
    private Animation<TextureRegion> fireAnim, iceAnim, darkAnim, lightAnim;
    private float projAnimTime = 0f;
    // === 회전 보정용: 스프라이트가 기본적으로 바라보는 각도(0도일 때 화면 방향) ===
    private static final float ZERO_ARROW_UP = 90f;  // 기본 화살은 '위'를 봄
    private static final float ZERO_SHEET_RIGHT = 0f; // 시트는 '오른쪽'을 봄

    // 기본 탄환 회전 렌더용 Region
    private TextureRegion bulletRegion;
    private final ObjectMap<Projectile, Float>  projAngles    = new ObjectMap<>();
    private final ObjectMap<Projectile, Integer> projTheme    = new ObjectMap<>();
    // --- Books Textures (sprites/book_*.png) ---
    private Texture bookDarkTex, bookIceTex, bookFireTex, bookLightTex;

    // ===== SFX =====
    private Sound sfxShotFire, sfxShotIce, sfxShotDark, sfxShotLight;
    private Sound sfxHit, sfxKill, sfxRicochet;
    private Sound sfxPickupKey, sfxDoorUnlock, sfxDoorOpen;
    private Sound sfxUiOpen, sfxUiCancel;
    private Sound sfxSelectFire, sfxSelectIce, sfxSelectDark, sfxSelectLight;
    private Sound sfxLevelUp, sfxWaveStart, sfxVictory;
    private Sound sfxDialogOpen, sfxDialogNext, sfxTransition;

    // ===== Shooting cooldown =====
    private float shotCooldownTimer = 0f; // 현재 남은 쿨다운 시간

    // ===== 중앙 배너 =====
    private String bannerLine1 = "";
    private String bannerLine2 = "";
    private float bannerTimer = 0f;
    private static final float BANNER_FADE = 0.25f;
    private static final float BANNER_DURATION = 3.5f;
    private float bannerTotal = 0f; // ★ 추가: 배너 총 지속시간 저장

    // ==== Blink 타이머 ====
    private float timeAccum = 0f;

    // ===== Objects =====
    private Rectangle doorRect, keyRect, spawnAreaRect;
    private boolean doorOpen, keyCollected;

    // --- Book 구조체 ---
    private static class Book {
        Rectangle bounds;
        TextureRegion region;
        String type; // Fire / Ice / Dark / Light
    }
    private final Array<Book> books = new Array<>();
    private static final float BOOK_NEAR_MARGIN = 28f;

    // ===== NPC 관련 =====
    private boolean npcFoundInTmx;
    private float npcX, npcY;
    private static final float NPC_INTERACT_RANGE = 56f;
    private float npcHomeX, npcHomeY;
    private Rectangle npcWanderBounds;
    private float npcTargetX, npcTargetY;
    private float npcWanderCooldown = 0f;
    private float npcMoveSpeed = 40f;
    private boolean npcTalking = false;
    private String npcSpritePath = "sprites/Actor3.png";
    private int npcCharacterIdx = 2;
    private int npcDrawSize = TILE;
    private float npcSpeedTmx = 40f;
    private boolean introTargetPlanned = false;
    private float introTargetX, introTargetY;
    private static final float INTRO_TALK_GAP = 12f;

    // --- 시작 위치 오버라이드 ---
    private boolean overrideSpawn = false;
    private float overrideSpawnX = 0f, overrideSpawnY = 0f;
    private boolean overrideNpcSpawn = false;
    private float overrideNpcSpawnX = 0f, overrideNpcSpawnY = 0f;

    private String resolveSpritePath(String raw, String fallback) {
        String p = (raw == null || raw.isEmpty()) ? fallback : raw;
        if (!p.contains("/")) p = "sprites/" + p;
        if (!Gdx.files.internal(p).exists()) {
            Gdx.app.error("ASSET", "Missing sprite: " + p + " (fallback " + fallback + ")");
            return fallback;
        }
        return p;
    }

    private boolean roomIntroNpcGone = false;
    private String npcCharacterTypeProp = "2";

    // 인트로: 격자 이동
    private boolean introRunning = false;
    private boolean npcGridMoving = false;
    private float npcStepTargetX, npcStepTargetY;
    private float npcStepSpeed = 240f;
    private boolean skipNPCInRoom = false;

    // Dialogue (타자 효과)
    private String[] dialogueLines;
    private int currentLineIndex;
    private String currentLineText;
    private String displayedText;
    private float charTimer;
    private float charInterval = 0.03f;
    private boolean lineComplete;

    public int getSelectedBookType() { return selectedBookType; }
    public void setSelectedBookType(int selectedBookType) { this.selectedBookType = selectedBookType; }

    // ===== Game state =====
    private enum GameState { INTRO, PLAYING, CLEARED, GAMEOVER }
    private GameState state;

    // ===== Level / Wave =====
    private int score;
    private int currentLevel;
    private final int finalLevel = 5;
    private int[] levelScoreThresholds;
    private float levelUpTimer;
    private boolean nextWavePending;

    private int wave = 0;
    private boolean skillChosen = false; // 책 확정 선택 여부
    private int selectedBookType = 0;    // 1:Fire 2:Ice 3:Dark 4:Light

    // === Final Mode (책 선택 후 메인으로 복귀한 마지막 라운드) ===
    private boolean finalMode = false;

    // === Book 설명 다이얼로그 ===
    private boolean bookDialogOpen = false;
    private String  bookDialogType = null;     // "Fire","Ice","Dark","Light"
    private String  bookDialogText = "";

    // === 전환/맵 ===
    private boolean transitioning = false, fadeOut = false, fadeIn = false;
    private float fadeAlpha = 0f;
    private String currentMapPath;
    private int targetMapIndex = 0;  // 0: room, 1: main, 2: school
    private float targetPlayerX, targetPlayerY;

    // ===== HUD =====
    private BitmapFont uiFont, uiFontLarge;
    private GlyphLayout layout;
    private OrthographicCamera hudCamera;
    private Hud hud;
    private boolean paused = false;
    private Stage pauseStage;
    private Skin pauseSkin;
    private Viewport pauseViewport;

    // ===== Viewports & Zoom =====
    private Viewport worldViewport;
    private Viewport hudViewport;
    private static final float DEFAULT_ZOOM = 0.85f; // 플레이어 기준 화면 확대(작게 줄수록 더 확대)


    // ===== Toast =====
    private String toastText = "";
    private float toastTimer = 0f;

    // ===== Audio =====
    private Music bgm;
    private Music footstepLoop;

    // ===== 기타 =====
    private float restartHoldTime = 0f;
    private boolean fontsOwned = false;
    private SpriteBatch mapBatch;

    // === 1x1 화이트 텍스처 생성/보장 ===
    private Texture makeWhiteTexture() {
        Pixmap pm = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }
    private void ensureWhiteTexture() {
        if (white1x1 == null) white1x1 = makeWhiteTexture();
    }

    // 현재 선택된 책에 맞춰 그릴 크기(가로) 결정
    private float getProjectileDrawW() {
        switch (selectedBookType) {
            case 1: return projSizeFire;
            case 2: return projSizeIce;
            case 3: return projSizeDark;
            case 4: return projSizeLight;
            default: return projSizeDefault; // 책 미선택(arrow0.png)
        }
    }

    // 발사 시점 방향/테마/애니메이션 시간 누적 (탄환별 스냅샷)
    private final ObjectMap<Projectile, Float>  projAnimTimes = new ObjectMap<>();

    // 애니메이션 시트가 가로로 길다면 비율을 맞춰 그리기(없으면 정사각)
    private float getProjectileAspect() {
        TextureRegion fr = getProjectileFrame(); // 책별 애니메이션 프레임(없을 수 있음)
        if (fr != null && fr.getRegionHeight() > 0) {
            return (float) fr.getRegionWidth() / fr.getRegionHeight();
        }
        return 1f; // 기본은 정사각
    }

    private float getProjectileDrawH() {
        float w = getProjectileDrawW();
        return w / getProjectileAspect();
    }

    // ====== 인트로 기본 대사 ======
    private final String[] introLines = new String[] {
        "NPC1 : 안녕?",
        "NPC1 : 정신이 들었어?",
        "나 : 안녕..? 넌 누구야?",
        "NPC1 : 나는 엘리라고 해!",
        "NPC1 : 기억...안나는구나?",
        "NPC1 : 하긴.. 충격이 컸겠다!"
    };

    // --- dispose 가드 ---
    private boolean disposed = false;

    // ====== 생성자 ======
    public PlayScreen(MainGame game, BitmapFont uiFont, BitmapFont uiFontLarge) {
        this.game = game;
        this.uiFont = uiFont;
        this.uiFontLarge = (uiFontLarge != null) ? uiFontLarge : uiFont;
        this.fontsOwned = false;

        // ★ 검은 패널용 화이트 1x1 텍스처를 항상 직접 생성
        white1x1 = makeWhiteTexture();

        // 기본 탄환 텍스처 로드 실패 시에도 그릴 수 있도록 fallback
        bulletTex  = safeLoadTexture("images/arrow0.png");
        if (bulletTex == null) bulletTex = white1x1;

        if (bulletTex != null) bulletRegion = new TextureRegion(bulletTex);

        currentMapPath = "maps/room.tmx";

        // ★ 카메라 & 뷰포트
        camera = new OrthographicCamera();
        worldViewport = new FitViewport(SCREEN_W, SCREEN_H, camera);
        camera.zoom = DEFAULT_ZOOM;  // ★ 플레이어 기준 확대

        loadMap(currentMapPath);
        loadProjectileSheets();
        // HUD 카메라/뷰포트
        hudCamera = new OrthographicCamera();
        hudViewport = new FitViewport(SCREEN_W, SCREEN_H, hudCamera);
        layout = new GlyphLayout();

        score = 0;
        currentLevel = 1;
        levelScoreThresholds = new int[]{5, 10, 15, 20, 9999};
        levelUpTimer = 0f;
        nextWavePending = false;

        victoryTexture = safeLoadTexture("images/victory.png");

        state = GameState.PLAYING;
        startRoomIntro();
        startStageBgm("music/horizons.mp3", 0.9f);
        hud = new Hud();
        initPauseUI();
        loadSfx(); // ★ SFX 로드
    }

    // 가로로 frameCount장이 붙은 시트를 Animation으로 만드는 유틸
    private Animation<TextureRegion> makeAnim(Texture sheet, int frameCount, float fps) {
        if (sheet == null || frameCount <= 0) return null;
        int fw = sheet.getWidth() / frameCount;
        int fh = sheet.getHeight();
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = new TextureRegion(sheet, i * fw, 0, fw, fh);
        }
        return new Animation<>(1f / fps, frames);
    }

    private void loadProjectileSheets() {
        // 파일 경로는 필요한 대로 바꿔도 됨. (6프레임 가로 시트 권장)
        // 예: sprites/proj_fire.png  (가로 6컷)
        projFireTex  = safeLoadTexture("sprites/proj_fire.png");
        projIceTex   = safeLoadTexture("sprites/proj_ice.png");
        projDarkTex  = safeLoadTexture("sprites/proj_dark.png");
        projLightTex = safeLoadTexture("sprites/proj_light.png");

        // 초당 12프레임 정도의 부드러운 애니메이션
        fireAnim  = makeAnim(projFireTex,  6, 12f);
        iceAnim   = makeAnim(projIceTex,   6, 12f);
        darkAnim  = makeAnim(projDarkTex,  6, 12f);
        lightAnim = makeAnim(projLightTex, 6, 12f);
    }

    // 현재 선택된 책(스킬)에 따른 탄환 프레임 반환
    private TextureRegion getProjectileFrame() {
        if (selectedBookType == 1 && fireAnim  != null) return fireAnim.getKeyFrame(projAnimTime, true);
        if (selectedBookType == 2 && iceAnim   != null) return iceAnim.getKeyFrame(projAnimTime, true);
        if (selectedBookType == 3 && darkAnim  != null) return darkAnim.getKeyFrame(projAnimTime, true);
        if (selectedBookType == 4 && lightAnim != null) return lightAnim.getKeyFrame(projAnimTime, true);
        return null; // null이면 기본 탄환을 그림
    }

    // ---------- UI helpers ----------
    private void initPauseUI() {
        pauseViewport = new FitViewport(SCREEN_W, SCREEN_H);
        pauseStage = new Stage(pauseViewport, game.batch);
        pauseSkin  = makeMinimalSkin(uiFontLarge != null ? uiFontLarge : uiFont);

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = (uiFontLarge != null ? uiFontLarge : uiFont);
        titleStyle.fontColor = Color.WHITE;

        Label pausedLabel = new Label("PAUSED", titleStyle);

        TextButton resumeBtn = new TextButton("Resume", pauseSkin);
        TextButton optionBtn = new TextButton("Option", pauseSkin);
        TextButton exitBtn   = new TextButton("Exit",   pauseSkin);

        resumeBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { togglePause(false); }

            private void togglePause() { togglePause(!paused); }
            private void togglePause(boolean on) {
                paused = on;
                if (paused) Gdx.input.setInputProcessor(pauseStage);
                else        Gdx.input.setInputProcessor(null);
            }
        });
        optionBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { showOptionsDialog(); }
        });
        exitBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameMainScreen(game));
            }
        });

        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(10);
        root.add(pausedLabel).padBottom(20).row();
        root.add(resumeBtn).width(280).height(64).row();
        root.add(optionBtn).width(280).height(56).row();
        root.add(exitBtn).width(280).height(56);
        pauseStage.addActor(root);
    }

    private void showOptionsDialog() {
        Dialog d = new Dialog("Option", pauseSkin);
        d.text("옵션 준비중\n(예: 볼륨/텍스트 속도 등)");
        d.button("OK");
        d.setModal(true);
        d.setMovable(false);
        d.show(pauseStage);
    }

    private Skin makeMinimalSkin(BitmapFont font) {
        Skin s = new Skin();

        Pixmap upPm = new Pixmap(4,4, Pixmap.Format.RGBA8888);
        upPm.setColor(0,0,0,0.80f); upPm.fill();
        Texture up = new Texture(upPm);
        s.add("up", up);

        Pixmap downPm = new Pixmap(4,4, Pixmap.Format.RGBA8888);
        downPm.setColor(0.15f,0.15f,0.15f,0.90f); downPm.fill();
        Texture down = new Texture(downPm);
        s.add("down", down);

        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.up   = new TextureRegionDrawable(new TextureRegion(up));
        tbs.down = new TextureRegionDrawable(new TextureRegion(down));
        tbs.font = font;
        tbs.fontColor = Color.YELLOW;
        s.add("default", tbs);

        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = font; ls.fontColor = Color.WHITE;
        s.add("default", ls);

        Window.WindowStyle ws = new Window.WindowStyle();
        ws.titleFont = font; ws.titleFontColor = Color.WHITE;
        s.add("default", ws);

        return s;
    }

    // ===== 시작 위치 설정 =====
    public void setPlayerStartPosition(float worldX, float worldY) {
        overrideSpawn = true;
        overrideSpawnX = worldX;
        overrideSpawnY = worldY;
        if (player != null) {
            player.x = worldX; player.y = worldY;
            player.bounds.setPosition(worldX, worldY);
        }
    }
    public void setPlayerStartPositionTiles(int tileX, int tileY) {
        setPlayerStartPosition(tileX * TILE, tileY * TILE);
    }
    public void setStartPositionTiles(int tileX, int tileY) {
        setPlayerStartPositionTiles(tileX, tileY);
    }
    public void setStartPosition(float worldX, float worldY) {
        setPlayerStartPosition(worldX, worldY);
    }

    public void setNpcStartPosition(float worldX, float worldY) {
        overrideNpcSpawn = true;
        overrideNpcSpawnX = worldX;
        overrideNpcSpawnY = worldY;

        if (npc != null) {
            npc.x = worldX; npc.y = worldY;
            npc.bounds.setPosition(npc.x, npc.y);
            npcHomeX = worldX; npcHomeY = worldY;
            if (npcWanderBounds != null) {
                npcWanderBounds.set(npcHomeX - 3*TILE, npcHomeY - 3*TILE, 7*TILE, 7*TILE);
            }
        }
    }
    public void setNpcStartPositionTiles(int tileX, int tileY) {
        setNpcStartPosition(tileX * TILE, tileY * TILE);
    }

    // ---------- Map Load ----------
    private void loadMap(String tmxPath) {
        // === NPC TMX 값 초기화 ===
        npcFoundInTmx   = false;
        npcX = npcY     = 0f;
        npcSpritePath   = "sprites/Actor2.png";
        npcCharacterIdx = 2;
        npcDrawSize     = TILE;
        npcSpeedTmx     = 40f;
        npcCharacterTypeProp = "2";

        // === 이전 리소스 정리 ===
        safeDispose(mapRenderer);  mapRenderer = null;
        safeDispose(tiledMap);     tiledMap    = null;
        if (mapBatch == null) mapBatch = new SpriteBatch();

        // === TMX 로드 ===
        if (!Gdx.files.internal(tmxPath).exists()) {
            Gdx.app.error("TMX", "TMX not found: " + tmxPath);
        }
        try {
            TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
            params.generateMipMaps = false;
            tiledMap = new TmxMapLoader().load(tmxPath, params);
        } catch (Exception e) {
            Gdx.app.error("TMX", "Failed to load map: " + tmxPath, e);
            throw e;
        }

        // 렌더러 재생성 (공용 batch 사용)
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1.0f, mapBatch);

        // 카메라 준비
        /*if (camera == null) camera = new OrthographicCamera();
        camera.setToOrtho(false, SCREEN_W, SCREEN_H);*/

        // 맵 크기 정보
        int mapWidthTiles   = tiledMap.getProperties().get("width", Integer.class);
        int mapHeightTiles  = tiledMap.getProperties().get("height", Integer.class);
        int tilePixelWidth  = tiledMap.getProperties().get("tilewidth", Integer.class);
        int tilePixelHeight = tiledMap.getProperties().get("tileheight", Integer.class);
        mapWidthPixels  = mapWidthTiles  * tilePixelWidth;
        mapHeightPixels = mapHeightTiles * tilePixelHeight;

        // 카메라 뷰포트 동기화
        syncWorldViewportToMap();

        // ✅ 충돌 레이어 탐색
        collisionLayer = findCollisionLayer(tiledMap);

        // 스프라이트 텍스처 로드
        if (keyTexture == null)        keyTexture = safeLoadTexture("sprites/key.png");
        if (doorClosedTexture == null) doorClosedTexture = safeLoadTexture("sprites/door_openTop.png");

        // 책 텍스처(최초 1회 로드)
        if (bookDarkTex == null)  bookDarkTex  = safeLoadTexture("sprites/book_dark.png");
        if (bookIceTex == null)   bookIceTex   = safeLoadTexture("sprites/book_ice.png");
        if (bookFireTex == null)  bookFireTex  = safeLoadTexture("sprites/book_fire.png");
        if (bookLightTex == null) bookLightTex = safeLoadTexture("sprites/book_light.png");

        // 오브젝트 초기화
        doorRect = null; keyRect = null; spawnAreaRect = null;
        npc = null; dialogueLines = null;
        books.clear();

        // === 오브젝트 레이어 파싱 ===
        String playerSpritePath = "sprites/Actor1.png";

        float startX = 0f, startY = 0f;
        MapLayer objectLayer = tiledMap.getLayers().get("Objects");
        if (objectLayer != null) {
            for (MapObject obj : objectLayer.getObjects()) {
                String name = obj.getName();

                if ("Player".equals(name)) {
                    MapProperties mp = obj.getProperties();
                    Float px = mp.get("x", Float.class);
                    Float py = mp.get("y", Float.class);
                    float oh = mp.containsKey("height") ? mp.get("height", Float.class) : TILE;
                    if (px != null) startX = px;
                    if (py != null) startY = mapHeightPixels - py - oh;
                    if (mp.containsKey("sprite")) {
                        playerSpritePath = resolveSpritePath(String.valueOf(mp.get("sprite")), "sprites/Actor1.png");
                    }
                    continue;
                }

                if ("Key".equals(name) && obj instanceof RectangleMapObject) {
                    keyRect = ((RectangleMapObject) obj).getRectangle(); continue;
                }
                if ("Door".equals(name) && obj instanceof RectangleMapObject) {
                    doorRect = ((RectangleMapObject) obj).getRectangle(); continue;
                }
                if ("SpawnArea".equals(name) && obj instanceof RectangleMapObject) {
                    spawnAreaRect = ((RectangleMapObject) obj).getRectangle(); continue;
                }

                // ====== 책: 사각 오브젝트 이름으로 배치 ======
                if (obj instanceof RectangleMapObject) {
                    Rectangle r = ((RectangleMapObject) obj).getRectangle();
                    if ("book_dark".equals(name)) {
                        Book b = new Book();
                        b.type = "Dark";
                        b.region = (bookDarkTex != null) ? new TextureRegion(bookDarkTex) : null;
                        b.bounds = new Rectangle(r.x, r.y, r.width > 0 ? r.width : TILE, r.height > 0 ? r.height : TILE);
                        books.add(b); continue;
                    }
                    if ("book_ice".equals(name)) {
                        Book b = new Book();
                        b.type = "Ice";
                        b.region = (bookIceTex != null) ? new TextureRegion(bookIceTex) : null;
                        b.bounds = new Rectangle(r.x, r.y, r.width > 0 ? r.width : TILE, r.height > 0 ? r.height : TILE);
                        books.add(b); continue;
                    }
                    if ("book_fire".equals(name)) {
                        Book b = new Book();
                        b.type = "Fire";
                        b.region = (bookFireTex != null) ? new TextureRegion(bookFireTex) : null;
                        b.bounds = new Rectangle(r.x, r.y, r.width > 0 ? r.width : TILE, r.height > 0 ? r.height : TILE);
                        books.add(b); continue;
                    }
                    if ("book_light".equals(name)) {
                        Book b = new Book();
                        b.type = "Light";
                        b.region = (bookLightTex != null) ? new TextureRegion(bookLightTex) : null;
                        b.bounds = new Rectangle(r.x, r.y, r.width > 0 ? r.width : TILE, r.height > 0 ? r.height : TILE);
                        books.add(b); continue;
                    }
                }

                // (호환) 타일 오브젝트의 bookType 속성
                if (obj instanceof TiledMapTileMapObject) {
                    TiledMapTileMapObject to = (TiledMapTileMapObject) obj;
                    String bookType = null;
                    Object v = obj.getProperties().get("bookType");
                    if (v != null) bookType = String.valueOf(v);
                    if (bookType == null && to.getTile() != null) {
                        Object tv = to.getTile().getProperties().get("bookType");
                        if (tv != null) bookType = String.valueOf(tv);
                    }
                    if (bookType == null) continue;

                    TextureRegion region = (to.getTile() != null) ? to.getTile().getTextureRegion() : null;
                    if (region == null) continue;

                    Book b = new Book();
                    b.region = region;
                    float x = to.getX(), y = to.getY();
                    b.bounds = new Rectangle(x, y, TILE, TILE);
                    b.type = bookType;
                    books.add(b);
                    continue;
                }

                if ("NPC".equals(name)) {
                    MapProperties mp = obj.getProperties();
                    float ox = mp.get("x", Float.class);
                    float oy = mp.get("y", Float.class);
                    float ow = mp.containsKey("width")  ? mp.get("width",  Float.class) : 0f;
                    float oh = mp.containsKey("height") ? mp.get("height", Float.class) : 0f;

                    npcX = ox;
                    npcY = mapHeightPixels - oy - oh;

                    if (mp.containsKey("sprite"))
                        npcSpritePath = resolveSpritePath(String.valueOf(mp.get("sprite")), "sprites/Actor2.png");
                    else
                        npcSpritePath = resolveSpritePath(npcSpritePath, "sprites/Actor2.png");

                    if (mp.containsKey("character"))    npcCharacterIdx     = Integer.parseInt(String.valueOf(mp.get("character")));
                    if (mp.containsKey("size"))         npcDrawSize         = Integer.parseInt(String.valueOf(mp.get("size")));
                    if (mp.containsKey("speed"))        npcSpeedTmx         = Float.parseFloat(String.valueOf(mp.get("speed")));
                    if (mp.containsKey("characterType"))npcCharacterTypeProp= String.valueOf(mp.get("characterType"));

                    if (ow > 0f) npcDrawSize = Math.round(ow);

                    npcFoundInTmx = true;
                    continue;
                }
            }
        }

        // === 플레이어 생성/이동 ===
        float spawnX = startX, spawnY = startY;
        if (overrideSpawn) { spawnX = overrideSpawnX; spawnY = overrideSpawnY; }
        if (player == null) {
            player = new Player(spawnX, spawnY, "sprites/Actor1.png", 1, TILE);
        } else {
            player.x = spawnX; player.y = spawnY;
            player.bounds.setPosition(player.x, player.y);
        }

        // === 맵별 초기화 ===
        if ("maps/room.tmx".equals(currentMapPath)) setupRoomScene();
        else setupGenericScene();

        centerCameraOnMap();
    }

    // ✅ 충돌 레이어 탐색
    private TiledMapTileLayer findCollisionLayer(TiledMap map) {
        for (MapLayer l : map.getLayers()) {
            if (l instanceof TiledMapTileLayer) {
                Object v = l.getProperties().get("blocked");
                boolean blocked = (v instanceof Boolean) ? (Boolean) v
                    : (v != null && Boolean.parseBoolean(v.toString()));
                if (blocked) return (TiledMapTileLayer) l;
            }
        }
        for (String name : new String[]{"벽", "Collision", "CollisionLayer"}) {
            MapLayer l = map.getLayers().get(name);
            if (l instanceof TiledMapTileLayer) return (TiledMapTileLayer) l;
        }
        for (MapLayer l : map.getLayers()) {
            if (l instanceof TiledMapTileLayer) return (TiledMapTileLayer) l;
        }
        return null;
    }

    private void showBanner(String line1, String line2, float seconds) {
        bannerLine1 = (line1 != null) ? line1 : "";
        bannerLine2 = (line2 != null) ? line2 : "";
        bannerTotal = Math.max(0.01f, seconds); // ★ 전달받은 지속시간 저장
        bannerTimer = bannerTotal;
    }

    // 안전한 Texture 로더
    private Texture safeLoadTexture(String p) {
        if (p == null) return null;
        p = p.replace("\\", "/");
        if (!Gdx.files.internal(p).exists()) {
            Gdx.app.error("ASSET", "Missing: " + p);
            return null;
        }
        return new Texture(Gdx.files.internal(p));
    }
    // 안전한 Sound 로더
    private Sound safeLoadSound(String p) {
        try {
            if (p == null) return null;
            String norm = p.replace("\\", "/");
            if (!Gdx.files.internal(norm).exists()) {
                Gdx.app.error("ASSET", "Missing SFX: " + norm);
                return null;
            }
            return Gdx.audio.newSound(Gdx.files.internal(norm));
        } catch (Throwable t) {
            Gdx.app.error("SFX", "Failed to load: " + p, t);
            return null;
        }
    }

    // SFX 일괄 로드
    private void loadSfx() {
        // 경로는 프로젝트 구조에 맞게 교체 가능
        sfxShotFire   = safeLoadSound("sfx/shot_fire.mp3");
        sfxShotIce    = safeLoadSound("sfx/shot_ice.mp3");
        sfxShotDark   = safeLoadSound("sfx/shot_dark.mp3");
        sfxShotLight  = safeLoadSound("sfx/shot_light.mp3");

        sfxHit        = safeLoadSound("sfx/hit.mp3");
        sfxKill       = safeLoadSound("sfx/kill.mp3");
        sfxRicochet   = safeLoadSound("sfx/ricochet.mp3");

        sfxPickupKey  = safeLoadSound("sfx/pickup_key.mp3");
        sfxDoorUnlock = safeLoadSound("sfx/door_unlock.mp3");
        sfxDoorOpen   = safeLoadSound("sfx/door_open.mp3");

        sfxUiOpen     = safeLoadSound("sfx/ui_open.mp3");
        sfxUiCancel   = safeLoadSound("sfx/ui_cancel.mp3");

        sfxSelectFire = safeLoadSound("sfx/select_fire.mp3");
        sfxSelectIce  = safeLoadSound("sfx/select_ice.mp3");
        sfxSelectDark = safeLoadSound("sfx/select_dark.mp3");
        sfxSelectLight= safeLoadSound("sfx/select_light.mp3");

        sfxLevelUp    = safeLoadSound("sfx/level_up.mp3");
        sfxWaveStart  = safeLoadSound("sfx/wave_start.mp3");
        sfxVictory    = safeLoadSound("sfx/victory.mp3");

        sfxDialogOpen = safeLoadSound("sfx/dialog_open.mp3");
        sfxDialogNext = safeLoadSound("sfx/dialog_next.mp3");
        sfxTransition = safeLoadSound("sfx/transition.mp3");
    }

    // SFX 플레이(널/볼륨 방어)
    private void playSfx(Sound s, float volume) {
        if (s != null) s.play(MathUtils.clamp(volume, 0f, 1f));
    }

    // 현재 선택된 책 기준 샷 쿨다운(초)
    private float getShotCooldown() {
        switch (selectedBookType) {
            case 1: return 0.5f; // Fire
            case 2: return 0.3f; // Ice
            case 3: return 0.1f; // Dark
            case 4: return 0.003f; // Light (제약 없음)
            default: return 0.25f; // 책 미선택 기본값
        }
    }

    private void setupRoomScene() {
        clearZombies();
        doorOpen = false; keyCollected = false;

        if (!skipNPCInRoom) {
            if (npcFoundInTmx) {
                npc = new NPC(npcX, npcY, npcSpritePath, npcCharacterIdx, npcDrawSize);
                npc.speed = npcSpeedTmx;
                String npcCharacterTypeProp = "0";
                npc.setDialogues("room", npcCharacterTypeProp);
            } else {
                float cx = mapWidthPixels * 0.5f - TILE * 0.5f;
                float cy = mapHeightPixels * 0.5f - TILE * 0.5f;
                npc = new NPC(cx, cy, "sprites/Actor2.png", 2, TILE);
                npc.setDialogues("room", "2");
            }

            if (overrideNpcSpawn) {
                npc.x = overrideNpcSpawnX; npc.y = overrideNpcSpawnY;
                npc.bounds.setPosition(npc.x, npc.y);
            }

            npc.x = Math.round(npc.x / TILE) * TILE;
            npc.y = Math.round(npc.y / TILE) * TILE;
            npc.bounds.setPosition(npc.x, npc.y);

            npcHomeX = npc.x; npcHomeY = npc.y;
            npcWanderBounds = new Rectangle(npcHomeX - 3*TILE, npcHomeY - 3*TILE, 7*TILE, 7*TILE);
        }

        introRunning = true;
        state = GameState.INTRO;
        npcTalking = false;
        currentLineIndex = 0;
        currentLineText  = introLines[0];
        displayedText    = "";
        charTimer = 0f; lineComplete = false;
        planNextIntroStep();
    }

    private void setupGenericScene() {
        if (npc == null && (npcX != 0 || npcY != 0)) {
            npc = new NPC(npcX, npcY, "sprites/Actor2.png", 2, TILE);
            npc.setDialogues(extractMapId(currentMapPath), "2");
        }

        if (npc != null && overrideNpcSpawn) {
            npc.x = overrideNpcSpawnX; npc.y = overrideNpcSpawnY;
            npc.bounds.setPosition(npc.x, npc.y);
        }
        npcHomeX = (npc != null ? npc.x : 0);
        npcHomeY = (npc != null ? npc.y : 0);
        npcWanderBounds = (npc != null) ? new Rectangle(npcHomeX - 3*TILE, npcHomeY - 3*TILE, 7*TILE,7*TILE) : null;

        clearZombies();

        // 메인 맵에서만 웨이브 시작. (학교는 절대 스폰 X)
        if ("maps/main.tmx".equals(currentMapPath)) {
            if (wave == 0) wave = 1;
            spawnWave(wave);
        }
        doorOpen = false;
    }

    private void centerCameraOnMap() {
        float visibleW = camera.viewportWidth  * camera.zoom;
        float visibleH = camera.viewportHeight * camera.zoom;
        float cx = (mapWidthPixels  <= visibleW) ? mapWidthPixels  * 0.5f : MathUtils.clamp(player.x + player.width*0.5f, visibleW*0.5f, mapWidthPixels  - visibleW*0.5f);
        float cy = (mapHeightPixels <= visibleH) ? mapHeightPixels * 0.5f : MathUtils.clamp(player.y + player.height*0.5f, visibleH*0.5f, mapHeightPixels - visibleH*0.5f);
        camera.position.set(cx, cy, 0f);
        camera.update();
    }


    private String extractMapId(String path) {
        int slash = path.lastIndexOf('/');
        int dot   = path.lastIndexOf('.');
        String name = (slash >= 0) ? path.substring(slash + 1) : path;
        return (dot > 0) ? name.substring(0, dot) : name;
    }

    // ---------- Intro ----------
    private void planNextIntroStep() {
        if (npc == null || player == null) { npcGridMoving = false; npcTalking = true; return; }

        float px = player.x;
        float py = player.y;

        float tx = px;
        float tyAbove = py + player.height + INTRO_TALK_GAP;
        float tyBelow = py - npc.height - INTRO_TALK_GAP;

        boolean okAbove = canStandAt(tx, tyAbove);
        boolean okBelow = canStandAt(tx, tyBelow);

        if (!okAbove && !okBelow) {
            float txL = Math.max(0, tx - TILE);
            float txR = Math.min(mapWidthPixels - npc.width, tx + TILE);
            if (canStandAt(txL, tyAbove) || canStandAt(txL, tyBelow)) { tx = txL; okAbove = canStandAt(tx, tyAbove); okBelow = canStandAt(tx, tyBelow); }
            else if (canStandAt(txR, tyAbove) || canStandAt(txR, tyBelow)) { tx = txR; okAbove = canStandAt(tx, tyAbove); okBelow = canStandAt(tx, tyBelow); }
        }

        introTargetX = tx;
        introTargetY = okAbove ? tyAbove : (okBelow ? tyBelow : npc.y);

        introTargetPlanned = true;
        npcGridMoving = true;
    }

    private boolean canStandAt(float x, float y) {
        if (x < 0 || y < 0) return false;
        if (x + npc.width  > mapWidthPixels)  return false;
        if (y + npc.height > mapHeightPixels) return false;
        if (isTileBlocked(x, y)) return false;
        if (isTileBlocked(x + npc.width - 1, y)) return false;
        if (isTileBlocked(x, y + npc.height - 1)) return false;
        if (isTileBlocked(x + npc.width - 1, y + npc.height - 1)) return false;
        return true;
    }

    private void startRoomIntro() {
        if (!"maps/room.tmx".equals(currentMapPath)) return;

        if (roomIntroNpcGone) {
            if (npc != null) { npc.dispose(); npc = null; }
            skipNPCInRoom = true;
            state = GameState.PLAYING;
            introRunning = false;
            npcTalking = false;
            return;
        }

        state = GameState.INTRO;
        introRunning = true;
        npcTalking = false;
        currentLineIndex = 0;
        currentLineText  = introLines[0];
        displayedText = "";
        charTimer = 0f; lineComplete = false;

        introTargetPlanned = false;
        planNextIntroStep();
    }

    private void startNpcDialogue() {
        String[] lines = introLines;
        if (npc != null) {
            try {
                String[] fromNpc = npc.getLines(0);
                if (fromNpc != null && fromNpc.length > 0) lines = fromNpc;
            } catch (Exception ignored) {}
        }
        dialogueLines = lines;
        currentLineIndex = 0;
        currentLineText  = dialogueLines[0];
        displayedText    = "";
        charTimer = 0f; lineComplete = false;

        state = GameState.INTRO;
        npcTalking = true;
        playSfx(sfxDialogOpen, 0.5f); // ★ 추가
    }

    // ---------- Render ----------
    private void syncWorldViewportToMap() {
        float viewW = Math.min(SCREEN_W, mapWidthPixels);
        float viewH = Math.min(SCREEN_H, mapHeightPixels);
        camera.setToOrtho(false, viewW, viewH);
        camera.update();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        ensureWhiteTexture(); // ★ 패널/오버레이 보장

        update(delta);
        if (game.getScreen() != this) return;

        // ===== World pass =====
        camera.update();
        mapRenderer.setView(camera);
        mapRenderer.render();

        if (mapRenderer.getBatch() != null) mapRenderer.getBatch().setColor(Color.WHITE);

        final SpriteBatch batch = game.batch;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE); // ★ 월드 패스 시작 시 알파/색 리셋

        if (paused) {
            drawPanel(batch, 0, 0, SCREEN_W, SCREEN_H, 0.45f);
            batch.end();
            pauseStage.act(Math.min(delta, 1 / 30f));
            pauseStage.draw();
            return;
        }

        // 키 그리기(상태 변경은 update에서만)
        if (!keyCollected && keyRect != null) {
            if (keyTexture != null) {
                batch.draw(keyTexture, keyRect.x, keyRect.y, keyRect.width, keyRect.height);
            }
        }

        // 문(닫힘 + 깜빡임)
        if (!doorOpen && doorRect != null && doorClosedTexture != null) {
            drawDoor(batch);
        }

        // 책(학교 맵에서 노출)
        if (isSchool() && !skillChosen) {
            for (Book b : books) {
                if (b.region == null) continue;
                batch.draw(b.region, b.bounds.x, b.bounds.y, b.bounds.width, b.bounds.height);
            }
        }

        if (npc != null) npc.draw(batch);
        player.draw(batch);
        for (Zombie z : zombies) z.draw(batch);
        for (Projectile p : projectiles) {
            TextureRegion frame = getProjectileFrame();

            // 탄환 중심 기준 회전
            float ox = p.width * 0.5f;
            float oy = p.height * 0.5f;

            // 발사 각도 불러오기 (기본값 0)
            float shotAngle = projAngles.get(p, 0f);

            // 테마(책)별 스프라이트 '기본 바라보는 각도' 차이 보정
            int theme = projTheme.get(p, selectedBookType);

            if (frame != null) {
                float zero = (theme == 1 /* Fire */) ? ZERO_SHEET_RIGHT : ZERO_SHEET_RIGHT;
                float rot = shotAngle - zero;

                batch.draw(frame,
                    p.x, p.y,
                    ox, oy,
                    p.width, p.height,
                    1f, 1f,
                    rot
                );
            } else {
                // 기본 화살(arrow0.png)은 '위'를 봄 → ZERO_ARROW_UP 보정
                if (bulletRegion != null) {
                    float rot = shotAngle - ZERO_ARROW_UP;
                    batch.draw(bulletRegion,
                        p.x, p.y,
                        ox, oy,
                        p.width, p.height,
                        1f, 1f,
                        rot
                    );
                }
            }
        }

        // NPC 버블
        if (state == GameState.PLAYING && npc != null && isNearNPC()) {
            final String bubble = "엘리.. 불쌍해..";
            layout.setText(uiFont, bubble);
            float bx = npc.x + (npc.width - layout.width) * 0.5f;
            float by = npc.y + npc.height + 18f;
            uiFont.draw(batch, bubble, bx, by);
        }

        batch.end();

        // ===== HUD pass =====
        hudViewport.apply();                   // ★ HUD도 동일 비율 유지
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        // 중앙 배너
        renderBannerHUD(batch);

        // 문 근접 HUD 안내
        if (doorRect != null && !doorOpen && "maps/room.tmx".equals(currentMapPath)) {
            if (isNearRect(doorRect, player.bounds, DOOR_NEAR_MARGIN)) {
                final String msg = keyCollected ? "[E] 버튼을 눌러 잠금을 해제하세요" : "열쇠가 필요합니다";
                layout.setText(uiFont, msg);
                float x = (SCREEN_W - layout.width) * 0.5f;
                float y = 120f;
                uiFont.setColor(0,0,0,1f); uiFont.draw(batch, msg, x + 2, y - 2);
                uiFont.setColor(1,1,1,1f); uiFont.draw(batch, msg, x, y);
                uiFont.setColor(Color.WHITE);
            }
        }

        // 책 근접 HUD 힌트
        if (isSchool() && !skillChosen && !bookDialogOpen && nearAnyBook(player.bounds, BOOK_NEAR_MARGIN)) {
            String msg = "설명을 들으려면 E버튼을 누르시오.";
            layout.setText(uiFont, msg);
            float x = (SCREEN_W - layout.width) * 0.5f, y = 120f;
            uiFont.setColor(0,0,0,1f); uiFont.draw(batch, msg, x+2, y-2);
            uiFont.setColor(1,1,1,1f); uiFont.draw(batch, msg, x,   y);
            uiFont.setColor(Color.WHITE);
        }

        // ===== 책 설명 다이얼로그 =====
        if (bookDialogOpen) {
            // ★ 전체 화면 딤 오버레이 (대화창 강조)
            drawPanel(batch, 0, 0, SCREEN_W, SCREEN_H, 0.35f);

            // 다이얼로그 가로폭
            final float dialogW = 640f;

            // 여백/레이아웃 상수
            final float sidePad  = 24f;  // 좌우 텍스트 패딩
            final float titleGap = 60f;  // 타이틀 아래 여백
            final float optGap   = 40f;  // 하단 옵션 영역 높이

            // 줄바꿈 폭(텍스트가 차지할 실제 폭)
            float wrapW = dialogW - sidePad * 2f;

            // 타이틀/본문 레이아웃 먼저 계산(본문은 wrap 적용)
            String title = "책 설명 (" + bookDialogType + ")";
            GlyphLayout titleLayout = new GlyphLayout(uiFontLarge, title);
            GlyphLayout textLayout  = new GlyphLayout(uiFont, bookDialogText, Color.WHITE, wrapW, Align.left, true);

            // 텍스트 양에 따라 다이얼로그 높이를 가변으로
            float dialogH = Math.max(220f, titleLayout.height + titleGap + textLayout.height + optGap + 28f);

            // 다이얼로그 위치(화면 중앙)
            float x = (SCREEN_W - dialogW) * 0.5f;
            float y = (SCREEN_H - dialogH) * 0.5f;

            // 바깥 패널(반투명)
            drawPanel(batch, x, y, dialogW, dialogH, 0.75f);

            // 타이틀
            uiFontLarge.draw(batch, title, x + (dialogW - titleLayout.width) * 0.5f, y + dialogH - 24f);

            // ===== 텍스트 배경 박스(반투명 검은 박스) =====
            float textTopY = y + dialogH - titleGap;      // 텍스트 시작 Y(상단)
            float textBoxH = textLayout.height + 12f;     // 위아래 6px 패딩
            float textBoxX = x + sidePad - 6f;
            float textBoxY = textTopY - textBoxH;
            float textBoxW = wrapW + 12f;

            // 반투명 검정 박스 그리기
            drawPanel(batch, textBoxX, textBoxY, textBoxW, textBoxH, 0.6f);

            // ===== 줄바꿈 적용하여 텍스트 그리기(겹침 방지) =====
            uiFont.draw(batch, bookDialogText, x + sidePad, textTopY, wrapW, Align.left, true);

            // 하단 옵션
            String opt = "[Y] 예   /   [N] 아니오";
            layout.setText(uiFont, opt);
            uiFont.draw(batch, opt, x + (dialogW - layout.width) * 0.5f, y + 28f);
        }

        // 토스트
        if (toastTimer > 0f && toastText != null && !toastText.isEmpty()) {
            layout.setText(uiFont, toastText);
            uiFont.draw(batch, toastText, (SCREEN_W - layout.width) * 0.5f, 80f);
        }

        // 상단 HUD
        String scoreText = "Score: " + score + "    Level: " + currentLevel;
        uiFont.draw(batch, scoreText, 10, SCREEN_H - 20);

        if (state != GameState.CLEARED) {
            int levelsLeft = finalLevel - currentLevel;
            String leftText = (levelsLeft > 0) ? ("Levels left until clear: " + levelsLeft) : "Final Level!";
            uiFont.draw(batch, leftText, 10, SCREEN_H - 40);
        }

        String healthText = "Health: " + player.health;
        layout.setText(uiFont, healthText);
        uiFont.draw(batch, healthText, SCREEN_W - layout.width - 10, SCREEN_H - 20);

        // 인트로/클리어
        if (state == GameState.INTRO) {
            String txt = (displayedText != null) ? displayedText : "";
            hud.drawDialogue(batch, uiFont, txt, SCREEN_W, SCREEN_H);
            uiFont.draw(batch, "SPACE/ENTER", SCREEN_W - 160f, 56f);
        } else if (state == GameState.CLEARED) {
            if (victoryTexture != null) {
                float vx = (SCREEN_W - victoryTexture.getWidth()) * 0.5f;
                float vy = (SCREEN_H - victoryTexture.getHeight()) * 0.5f;
                batch.draw(victoryTexture, vx, vy);
            } else {
                String clear = "VICTORY!";
                layout.setText(uiFontLarge, clear);
                uiFontLarge.draw(batch, clear, (SCREEN_W - layout.width) * 0.5f, 360f);
            }

            String tip = "Hold R for 3 seconds to restart";
            layout.setText(uiFont, tip);
            uiFont.draw(batch, tip, (SCREEN_W - layout.width) * 0.5f, 330f);
        }

        batch.end();
    }

    // 넉백 세기(픽셀). 필요하면 스킬별로 getKnockbackStrength()에서 조정
    private float getKnockbackStrength() {
        switch (selectedBookType) {
            case 1: return 60f; // Fire
            case 2: return 30f; // Ice
            case 3: return 32f; // Dark
            case 4: return 30f; // Light
            default: return 32f; // 기본
        }
    }

    // 사각형이 타일에 막히는지(모서리 4점) 검사
    private boolean rectBlocked(float x, float y, float w, float h) {
        return isTileBlocked(x, y)
            || isTileBlocked(x + w - 1, y)
            || isTileBlocked(x, y + h - 1)
            || isTileBlocked(x + w - 1, y + h - 1);
    }

    // 해당 위치로 좀비를 옮겼을 때, 플레이어/다른 좀비/NPC와 겹치는지 검사
    private boolean overlapsAnyEntityExcept(Zombie self, float nx, float ny) {
        Rectangle r = new Rectangle(nx, ny, self.width, self.height);

        if (player != null && r.overlaps(player.bounds)) return true;
        if (npc != null && r.overlaps(npc.bounds)) return true;

        for (int i = 0; i < zombies.size; i++) {
            Zombie z2 = zombies.get(i);
            if (z2 == self) continue;
            if (r.overlaps(z2.bounds)) return true;
        }
        return false;
    }

    /**
     * 좀비 넉백: dirX,dirY 방향으로 strength 픽셀만큼 ‘조각내서’ 안전하게 밀기
     */
    private void applyKnockback(Zombie z, float dirX, float dirY, float strength) {
        float len = (float)Math.sqrt(dirX*dirX + dirY*dirY);
        if (len < 1e-4f) return;

        float nx = dirX / len, ny = dirY / len;
        float totalX = nx * strength;
        float totalY = ny * strength;

        final int STEPS = 8;
        float stepX = totalX / STEPS;
        float stepY = totalY / STEPS;

        for (int s = 0; s < STEPS; s++) {
            float tryX = MathUtils.clamp(z.x + stepX, 0, mapWidthPixels  - z.width);
            if (!rectBlocked(tryX, z.y, z.width, z.height)
                && !overlapsAnyEntityExcept(z, tryX, z.y)) {
                z.x = tryX;
                z.bounds.setPosition(z.x, z.y);
            }
            float tryY = MathUtils.clamp(z.y + stepY, 0, mapHeightPixels - z.height);
            if (!rectBlocked(z.x, tryY, z.width, z.height)
                && !overlapsAnyEntityExcept(z, z.x, tryY)) {
                z.y = tryY;
                z.bounds.setPosition(z.x, z.y);
            }
        }
    }

    private void renderBannerHUD(SpriteBatch batch) {
        if (bannerTimer <= 0f) return;

        float total = (bannerTotal > 0f) ? bannerTotal : BANNER_DURATION; // ★ 전달된 총시간 사용
        float elapsed = Math.max(0f, total - bannerTimer);
        float alpha;
        if (elapsed < BANNER_FADE) alpha = elapsed / BANNER_FADE;            // 페이드 인
        else if (bannerTimer < BANNER_FADE) alpha = bannerTimer / BANNER_FADE; // 페이드 아웃
        else alpha = 1f;

        Color old = batch.getColor();
        batch.setColor(old.r, old.g, old.b, alpha);

        drawPanel(batch, 0, SCREEN_H * 0.5f - 80, SCREEN_W, 160, 0.45f);

        String l1 = (bannerLine1 != null) ? bannerLine1 : "";
        layout.setText(uiFontLarge, l1);
        float x1 = (SCREEN_W - layout.width) * 0.5f;
        float y1 = SCREEN_H * 0.5f + layout.height * 0.5f + 16f;
        uiFontLarge.setColor(0,0,0,alpha); uiFontLarge.draw(batch, l1, x1+2, y1-2);
        uiFontLarge.setColor(1,1,1,alpha); uiFontLarge.draw(batch, l1, x1,   y1);

        String l2 = (bannerLine2 != null) ? bannerLine2 : "";
        layout.setText(uiFont, l2);
        float x2 = (SCREEN_W - layout.width) * 0.5f;
        float y2 = y1 - 42f;
        uiFont.setColor(0,0,0,alpha); uiFont.draw(batch, l2, x2+2, y2-2);
        uiFont.setColor(1,1,1,alpha); uiFont.draw(batch, l2, x2,   y2);

        // ★ 폰트/배치 색 원복
        uiFontLarge.setColor(Color.WHITE);
        uiFont.setColor(Color.WHITE);
        batch.setColor(old);
    }

    private void drawDoor(SpriteBatch batch) {
        if (doorRect == null || doorOpen) return;

        float alpha = keyCollected
            ? (0.4f + 0.6f * (0.5f + 0.5f * MathUtils.sin(timeAccum * 6f)))
            : 1f;

        Color old = batch.getColor();
        batch.setColor(old.r, old.g, old.b, alpha);

        if (doorClosedTexture != null) {
            batch.draw(doorClosedTexture, doorRect.x, doorRect.y, doorRect.width, doorRect.height);
        }

        batch.setColor(old); // 복구 필수
    }

    private void drawPanel(SpriteBatch batch, float x, float y, float w, float h, float alpha) {
        ensureWhiteTexture();
        Color old = batch.getColor();
        batch.setColor(0f,0f,0f, alpha);
        if (white1x1 != null) batch.draw(white1x1, x, y, w, h);
        batch.setColor(old);
    }

    // ---------- Update ----------
    private void update(float dt) {
        timeAccum += dt;
        if (bannerTimer > 0f) {
            bannerTimer -= dt;
            if (bannerTimer < 0f) bannerTimer = 0f;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
            if (paused) return;
        }

        if (paused) {
            if (pauseStage != null) pauseStage.act(dt);
            return;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.R)) {
            restartHoldTime += dt;
            if (restartHoldTime >= 3f) {
                game.setScreen(new PlayScreen(game, uiFont, uiFontLarge));
                dispose();
                restartHoldTime = 0f;
                return;
            }
        } else restartHoldTime = 0f;

        if (toastTimer > 0f) {
            toastTimer -= dt;
            if (toastTimer < 0f) { toastTimer = 0f; toastText = ""; }
        }

        switch (state) {
            case INTRO:   updateIntro(dt); break;
            case PLAYING: updatePlaying(dt); break;
            case GAMEOVER:
            case CLEARED: break;
        }

        if (transitioning) {
            if (fadeOut) {
                fadeAlpha += 0.05f;
                if (fadeAlpha >= 1f) {
                    fadeAlpha = 1f; fadeOut = false;
                    if (targetMapIndex == 0) switchMap("maps/room.tmx", targetPlayerX, targetPlayerY);
                    else if (targetMapIndex == 1) switchMap("maps/main.tmx", targetPlayerX, targetPlayerY);
                    else switchMap("maps/school.tmx", targetPlayerX, targetPlayerY);
                    fadeIn = true;
                }
            } else if (fadeIn) {
                fadeAlpha -= 0.05f;
                if (fadeAlpha <= 0f) {
                    fadeAlpha = 0f; fadeIn = false; transitioning = false;
                }
            }
        }
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            ensurePauseUI();
            Gdx.input.setInputProcessor(pauseStage);
        } else {
            Gdx.input.setInputProcessor(null);
        }
    }

    private void ensurePauseUI() {
        if (pauseStage != null) return;

        pauseViewport = new FitViewport(SCREEN_W, SCREEN_H);
        pauseStage = new Stage(pauseViewport);

        pauseSkin = new Skin();
        Texture tex = (white1x1 != null) ? white1x1 : new Texture(new Pixmap(1,1, Pixmap.Format.RGBA8888));
        pauseSkin.add("white", new TextureRegion(tex));

        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = uiFont; ls.fontColor = Color.WHITE;
        pauseSkin.add("default", ls);

        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.up   = new TextureRegionDrawable(pauseSkin.getRegion("white"));
        tbs.down = new TextureRegionDrawable(pauseSkin.getRegion("white"));
        tbs.over = new TextureRegionDrawable(pauseSkin.getRegion("white"));
        tbs.font = uiFont; tbs.fontColor = Color.YELLOW;
        ((TextureRegionDrawable)tbs.up  ).getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        ((TextureRegionDrawable)tbs.down).getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pauseSkin.add("default", tbs);

        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(10);

        Label title = new Label("PAUSED", ls);
        root.add(title).padBottom(20).row();

        TextButton resumeBtn = new TextButton("Resume", pauseSkin);
        TextButton optionBtn = new TextButton("Option", pauseSkin);
        TextButton exitBtn   = new TextButton("Exit",   pauseSkin);

        resumeBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                paused = false; Gdx.input.setInputProcessor(null);
            }
        });
        optionBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                toast("옵션: 준비중");
            }
        });
        exitBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        root.add(resumeBtn).width(260).height(64).row();
        root.add(optionBtn).width(260).height(56).row();
        root.add(exitBtn).width(260).height(56);

        pauseStage.addActor(root);
    }

    // ---------- Intro ----------
    private void updateIntro(float dt) {
        updateCamera();

        if (npc != null && !npcTalking) {
            if (!introTargetPlanned) planNextIntroStep();

            final float EPS = 1.4f;

            if (Math.abs(npc.x - introTargetX) > EPS) {
                float dir = Math.signum(introTargetX - npc.x);
                float step = Math.min(Math.abs(introTargetX - npc.x), npcStepSpeed * dt);

                float oldX = npc.x;
                npc.x = MathUtils.clamp(npc.x + dir * step, 0, mapWidthPixels - npc.width);
                npc.bounds.setPosition(npc.x, npc.y);

                if (!canStandAt(npc.x, npc.y)) {
                    npc.x = oldX; npc.bounds.setPosition(npc.x, npc.y);
                } else {
                    boolean rollback = false;
                    if (player != null && npc.bounds.overlaps(player.bounds)) rollback = true;
                    if (!rollback) for (int i = 0; i < zombies.size; i++)
                        if (npc.bounds.overlaps(zombies.get(i).bounds)) { rollback = true; break; }

                    if (rollback) { npc.x = oldX; npc.bounds.setPosition(npc.x, npc.y); }
                    else { npc.idle(dt, (dir < 0) ? 1 : 2); }
                }
                return;
            }

            if (Math.abs(npc.y - introTargetY) > EPS) {
                float dir = Math.signum(introTargetY - npc.y);
                float step = Math.min(Math.abs(introTargetY - npc.y), npcStepSpeed * dt);

                float oldY = npc.y;
                npc.y = MathUtils.clamp(npc.y + dir * step, 0, mapHeightPixels - npc.height);
                npc.bounds.setPosition(npc.x, npc.y);

                if (!canStandAt(npc.x, npc.y)) {
                    npc.y = oldY; npc.bounds.setPosition(npc.x, npc.y);
                } else {
                    boolean rollback = false;
                    if (player != null && npc.bounds.overlaps(player.bounds)) rollback = true;
                    if (!rollback) for (int i = 0; i < zombies.size; i++)
                        if (npc.bounds.overlaps(zombies.get(i).bounds)) { rollback = true; break; }

                    if (rollback) { npc.y = oldY; npc.bounds.setPosition(npc.x, npc.y); }
                    else { npc.idle(dt, (dir < 0) ? 0 : 3); }
                }
                return;
            }

            npcGridMoving = false;
            npcTalking = true;
            lineComplete = false;
            charTimer = 0f;
            displayedText = "";
            return;
        }

        if (npcTalking) {
            // ★ 항상 현재 사용 중인 대사 배열을 기준으로 처리
            String[] lines = (dialogueLines != null && dialogueLines.length > 0) ? dialogueLines : introLines;

            if (!lineComplete) {
                charTimer += dt;
                if (charTimer >= charInterval && displayedText.length() < currentLineText.length()) {
                    displayedText = currentLineText.substring(0, displayedText.length() + 1);
                    charTimer = 0f;
                }
                if (displayedText.length() == currentLineText.length()) lineComplete = true;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                if (!lineComplete) {
                    displayedText = currentLineText; lineComplete = true;
                } else {
                    if (currentLineIndex < lines.length - 1) {
                        currentLineIndex++;
                        currentLineText = lines[currentLineIndex]; // ★ lines 사용 (dialogueLines 우선)
                        displayedText = ""; lineComplete = false; charTimer = 0f;
                        playSfx(sfxDialogNext, 0.4f); // ★ 다음 대사 SFX
                    } else {
                        skipNPCInRoom = false;
                        state = GameState.PLAYING;
                        introRunning = false; npcTalking = false;
                    }
                }
            }
        }
    }

    // ---------- PLAY ----------
    private void updatePlaying(float dt) {
        float oldX = player.x, oldY = player.y;
        player.update(dt);
        handlePlayerCollision(player, oldX, oldY);

        // ★ 키 픽업 판정은 update에서만 수행 + SFX/배너 추가
        if (!keyCollected && keyRect != null && player.bounds.overlaps(keyRect)) {
            keyCollected = true;
            playSfx(sfxPickupKey, 0.8f); // ★ 픽업 사운드
            toast("열쇠를 획득함");
            showBanner("열쇠를 획득함", "문을 열어서 광장으로 가시오.", 3.5f);
        }

        if ("maps/room.tmx".equals(currentMapPath)) {
            float moved = Math.abs(player.x - oldX) + Math.abs(player.y - oldY);
            if (moved > 0.5f) {
                if (footstepLoop == null || !footstepLoopPlaying()) {
                    if (footstepLoop != null) { footstepLoop.dispose(); }
                    footstepLoop = new Music("music/longwalking.mp3", true);
                    footstepLoop.setVolume(0.6f);
                    footstepLoop.play();
                }
            } else {
                if (footstepLoop != null && footstepLoop.isPlaying()) footstepLoop.stop();
            }
        } else {
            if (footstepLoop != null && footstepLoop.isPlaying()) footstepLoop.stop();
        }

        updateCamera();
        if (shotCooldownTimer > 0f) shotCooldownTimer -= dt;

        // === Book Dialog 입력 처리 ===
        if (isSchool() && !skillChosen) {
            if (bookDialogOpen) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.Y) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    // 확정 선택
                    applyBookChoice(bookDialogType);
                    bookDialogOpen = false;
                    // 배너 안내 후 메인으로 전이 + Final Mode 시작
                    showBanner("~~책을 선택하셨습니다.", "이제 마지막 라운드입니다. 마지막까지 화이팅!", 3.5f);
                    finalMode = true;
                    // 메인맵에서 시작 좌표(안전한 중앙 근처)
                    startTransition(1, TILE * 8, TILE * 8);
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.N) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    bookDialogOpen = false;
                }
            } else {
                String overlapType = overlappingBookType(player.bounds);
                if (overlapType != null && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    openBookDialog(overlapType);
                }
            }
        }

        handleNpcInteractionDuringPlay(dt);
        handleShooting();
        updateProjectiles(dt);
        updateZombies(dt);
        checkHitsAndScoring(dt);
        updateNpcWander(dt);

        // 문 상호작용
        if (doorRect != null) {
            boolean nearDoor = isNearRect(doorRect, player.bounds, 24f);

            if ("maps/room.tmx".equals(currentMapPath)) {
                if (nearDoor && !doorOpen && keyCollected && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    doorOpen = true;
                    playSfx(sfxDoorUnlock, 0.7f); // ★ 잠금 해제 SFX
                    toast("문이 열렸다");
                } else if (nearDoor && doorOpen && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    startTransition(1, mapWidthPixels * 0.5f, mapHeightPixels * 0.5f);
                }
            } else if ("maps/main.tmx".equals(currentMapPath)) {
                if (nearDoor && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    if (player.x < mapWidthPixels * 0.5f) startTransition(0, TILE*5, TILE*5);
                    else startTransition(2, TILE*8, TILE*12);
                }
            } else if (isSchool()) {
                // 학교의 문은 필요 시 추가
            }
        }
    }

    private boolean footstepLoopPlaying() { return (footstepLoop != null && footstepLoop.isPlaying()); }

    private void applyBookChoice(String type) {
        if (type == null) return;
        String low = type.toLowerCase();
        if (low.equals("fire"))      chooseSkill(1, "Fire");
        else if (low.equals("ice"))  chooseSkill(2, "Ice");
        else if (low.equals("dark")) chooseSkill(3, "Dark");
        else if (low.equals("light"))chooseSkill(4, "Light");
    }

    private void openBookDialog(String type) {
        bookDialogOpen = true;
        bookDialogType = type;
        bookDialogText = getBookDescription(type);
        playSfx(sfxUiOpen, 0.6f); // ★ 추가
    }

    // 예쁘고 일관된 문단을 만들어주는 헬퍼
    private String prettyBookDesc(String title, String[] bullets) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ").append(title).append(" ]\n");
        sb.append("--------------------------------\n");
        sb.append("효과\n");
        for (String b : bullets) sb.append(" - ").append(b).append("\n");
        sb.append("\n이 책을 선택하시겠습니까?");
        return sb.toString();
    }

    private String getBookDescription(String type) {
        String t = (type == null) ? "" : type.toLowerCase();
        if (t.equals("fire")) {
            return prettyBookDesc(
                "불의 서  |  FIRE",
                new String[]{
                    "투사체에 화염의 기운이 깃듭니다(연출).",
                    "공격 빈도와 피해가 소폭 증가합니다.",
                    "초반 라운드 빠른 처리에 유리합니다."
                }
            );
        }
        if (t.equals("ice")) {
            return prettyBookDesc(
                "얼음의 서  |  ICE",
                new String[]{
                    "적에게 일시적 둔화를 부여합니다(연출).",
                    "생존력이 소폭 향상됩니다(받는 피해 경감 느낌).",
                    "카이팅/컨트롤 플레이에 유리합니다."
                }
            );
        }
        if (t.equals("dark")) {
            return prettyBookDesc(
                "어둠의 서  |  DARK",
                new String[]{
                    "치명타 확률이 상승합니다.",
                    "체력이 낮을수록 추가 피해를 줍니다.",
                    "하이리스크-하이리턴 성향에 적합합니다."
                }
            );
        }
        if (t.equals("light")) {
            return prettyBookDesc(
                "빛의 서  |  LIGHT",
                new String[]{
                    "이동 속도와 회복력이 소폭 증가합니다.",
                    "시야가 넓어지는 연출이 적용됩니다.",
                    "맵 탐색/도주/포지셔닝에 유리합니다."
                }
            );
        }
        return "이 책을 선택하시겠습니까?";
    }

    private void chooseSkill(int type, String name) {
        skillChosen = true; selectedBookType = type;
        toast("스킬 선택: " + name);
    }

    // ---------- NPC 상호작용 ----------
    private void resolveAxisByDeltaForPlayer(Player p, float oldX, float oldY, Rectangle other) {
        if (!p.bounds.overlaps(other)) return;
        float dx = p.x - oldX, dy = p.y - oldY;
        if (Math.abs(dx) >= Math.abs(dy)) p.x = oldX; else p.y = oldY;
        p.bounds.setPosition(p.x, p.y);
    }
    private void resolveAxisByDeltaForZombie(Zombie z, float oldX, float oldY, Rectangle other) {
        if (!z.bounds.overlaps(other)) return;
        float dx = z.x - oldX, dy = z.y - oldY;
        if (Math.abs(dx) >= Math.abs(dy)) z.x = oldX; else z.y = oldY;
        z.bounds.setPosition(z.x, z.y);
    }
    private void resolveAxisByDeltaForNPC(NPC n, float oldX, float oldY, Rectangle other) {
        if (!n.bounds.overlaps(other)) return;
        float dx = n.x - oldX, dy = n.y - oldY;
        if (Math.abs(dx) >= Math.abs(dy)) n.x = oldX; else n.y = oldY;
        n.bounds.setPosition(n.x, n.y);
    }

    private boolean isNearNPC() {
        if (npc == null) return false;
        float pcx = player.x + player.width * 0.5f;
        float pcy = player.y + player.height * 0.5f;
        float ncx = npc.x + npc.width * 0.5f;
        float ncy = npc.y + npc.height * 0.5f;

        float r1 = 0.5f * Math.max(player.width, player.height);
        float r2 = 0.5f * Math.max(npc.width, npc.height);
        float range = r1 + r2 + NPC_INTERACT_RANGE;

        float dx = ncx - pcx, dy = ncy - pcy;
        return (dx*dx + dy*dy) <= range * range;
    }

    private void handleNpcInteractionDuringPlay(float dt) {
        if (npc == null) return;
        if (isNearNPC() && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            float ndx = player.x - npc.x, ndy = player.y - npc.y;
            if (Math.abs(ndx) > Math.abs(ndy)) npc.idle(dt, (ndx < 0) ? 1 : 2);
            else                               npc.idle(dt, (ndy < 0) ? 0 : 3);
            if (state != GameState.INTRO) startNpcDialogue();
        }
    }

    // ---------- 전투 ----------
    private void handleShooting() {
        int dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) { dx = 1; dy = 0; }
        else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) { dx = -1; dy = 0; }
        else if (Gdx.input.isKeyPressed(Input.Keys.UP)) { dx = 0; dy = 1; }
        else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) { dx = 0; dy = -1; }

        if (dx == 0 && dy == 0) return;

        float need = getShotCooldown();
        float minGuard = (need <= 0f) ? 0.02f : need;

        if (shotCooldownTimer <= 0f) {
            shoot(dx, dy);
            shotCooldownTimer = minGuard;
        }
    }

    private void shoot(int dx, int dy) {
        float w = getProjectileDrawW();
        float h = getProjectileDrawH();

        Projectile p = new Projectile(
            centerX(player) - w * 0.5f,
            centerY(player) - h * 0.5f,
            dx, dy, 300f
        );

        p.width  = w;
        p.height = h;

        float angleDeg = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        projAngles.put(p, angleDeg);
        projTheme.put(p, selectedBookType);

        projectiles.add(p);

        // ★ 책 타입별 샷 SFX
        switch (selectedBookType) {
            case 1: playSfx(sfxShotFire,  0.8f); break;
            case 2: playSfx(sfxShotIce,   0.8f); break;
            case 3: playSfx(sfxShotDark,  0.8f); break;
            case 4: playSfx(sfxShotLight, 0.7f); break;
            default: playSfx(sfxShotLight, 0.5f); break; // 기본
        }
    }

    private float centerX(Player p) { return p.x + p.width * 0.5f; }
    private float centerY(Player p) { return p.y + p.height* 0.5f; }

    private void updateProjectiles(float dt) {
        projAnimTime += dt;
        for (int i = 0; i < projectiles.size; i++) {
            Projectile p = projectiles.get(i);
            boolean alive = p.update(dt, mapWidthPixels, mapHeightPixels);
            if (!alive) { projectiles.removeIndex(i); i--; continue; }
            if (isTileBlocked(p.x + p.width/2f, p.y + p.height/2f)) {
                playSfx(sfxRicochet, 0.5f);
                projectiles.removeIndex(i); i--;
            }
        }
    }

    private void updateZombies(float dt) {
        for (int i = 0; i < zombies.size; i++) {
            Zombie z = zombies.get(i);
            float oldZX = z.x, oldZY = z.y;
            z.update(dt, player.x, player.y);
            handleZombieCollision(z, oldZX, oldZY);
        }
    }

    private void checkHitsAndScoring(float dt) {
        // 총탄 vs 좀비
        for (int i = 0; i < projectiles.size; i++) {
            Projectile p = projectiles.get(i);
            for (int j = 0; j < zombies.size; j++) {
                Zombie z = zombies.get(j);
                if (z.bounds.overlaps(p.bounds)) {
                    playSfx(sfxHit, 0.6f);

                    projectiles.removeIndex(i); i--;

                    boolean killed = z.takeDamage(1);

                    if (!killed) {
                        float zx = z.x + z.width  * 0.5f;
                        float zy = z.y + z.height * 0.5f;
                        float px = p.x + p.width  * 0.5f;
                        float py = p.y + p.height * 0.5f;

                        float dirX = zx - px;
                        float dirY = zy - py;
                        applyKnockback(z, dirX, dirY, getKnockbackStrength());
                    }

                    if (killed) {
                        playSfx(sfxKill, 0.7f);

                        z.dispose(); zombies.removeIndex(j); j--;
                        score += 1;

                        if (currentLevel < finalLevel && score >= levelScoreThresholds[currentLevel - 1]) {
                            currentLevel++;
                            playSfx(sfxLevelUp, 0.75f);

                            if (currentLevel > finalLevel) {
                                state = GameState.CLEARED; clearZombies();
                                playSfx(sfxVictory, 0.9f);
                                return;
                            }
                            clearZombies(); projectiles.clear();
                            levelUpTimer = 2.0f; nextWavePending = true;
                        }
                    }
                    break;
                }
            }
        }

        if (nextWavePending) {
            levelUpTimer -= dt;
            if (levelUpTimer <= 0) {
                nextWavePending = false; levelUpTimer = 0;
                if (!isSchool()) spawnZombiesForLevel();
            }
        } else if (levelUpTimer > 0) {
            levelUpTimer -= dt; if (levelUpTimer < 0) levelUpTimer = 0;
        }

        if ("maps/main.tmx".equals(currentMapPath) && !transitioning && state != GameState.CLEARED) {
            if (!finalMode) {
                if (zombies.size == 0) {
                    if (wave >= 3) {
                        startTransition(2, TILE*5, TILE*5);
                    } else {
                        wave++;
                        spawnWave(wave);
                        toast("레벨 " + wave);
                    }
                }
            } else {
                if (score >= 40) {
                    state = GameState.CLEARED;
                    if (bgm != null) bgm.stop();
                    playSfx(sfxVictory, 0.9f);
                } else if (zombies.size == 0) {
                    wave++;
                    spawnWave(wave);
                    toast("라운드 " + wave);
                }
            }
        }
    }

    // ---------- NPC 배회 ----------
    private void pickNextNpcTarget(boolean immediate) {
        npcWanderCooldown = immediate ? 0f : MathUtils.random(0.4f, 1.1f);
        if (npc == null) return;

        final int MAX_STEP_TILES = 3;
        final int MAX_TRY = 16;
        for (int t = 0; t < MAX_TRY; t++) {
            int dxTiles = MathUtils.random(-MAX_STEP_TILES, MAX_STEP_TILES);
            int dyTiles = MathUtils.random(-MAX_STEP_TILES, MAX_STEP_TILES);
            if (Math.abs(dxTiles) + Math.abs(dyTiles) == 0) continue;
            if (Math.abs(dxTiles) > 0 && Math.abs(dyTiles) > 0) {
                if (MathUtils.randomBoolean()) dyTiles = 0; else dxTiles = 0;
            }

            float tx = npc.x + dxTiles * TILE;
            float ty = npc.y + dyTiles * TILE;
            if (npcWanderBounds.contains(tx, ty)
                && tx >= 0 && ty >= 0
                && tx + npc.width <= mapWidthPixels
                && ty + npc.height <= mapHeightPixels
                && !isTileBlocked(tx, ty)
                && !isTileBlocked(tx + npc.width - 1, ty)
                && !isTileBlocked(tx, ty + npc.height - 1)
                && !isTileBlocked(tx + npc.width - 1, ty + npc.height - 1)) {
                npcTargetX = tx; npcTargetY = ty; return;
            }
        }
        npcTargetX = npcHomeX; npcTargetY = npcHomeY;
    }

    private void updateNpcWander(float dt) {
        if (npc == null || npcTalking || state != GameState.PLAYING) return;

        if (npcWanderCooldown > 0f) { npcWanderCooldown -= dt; return; }

        float dx = npcTargetX - npc.x, dy = npcTargetY - npc.y;
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len < 2f) { pickNextNpcTarget(false); return; }

        float vx = (dx / len) * npcMoveSpeed;
        float vy = (dy / len) * npcMoveSpeed;

        float oldX = npc.x, oldY = npc.y;

        npc.x = MathUtils.clamp(npc.x + vx * dt, 0, mapWidthPixels  - npc.width);
        npc.y = MathUtils.clamp(npc.y + vy * dt, 0, mapHeightPixels - npc.height);
        npc.bounds.setPosition(npc.x, npc.y);

        if (isTileBlocked(npc.x, npc.y)
            || isTileBlocked(npc.x + npc.width-1, npc.y)
            || isTileBlocked(npc.x, npc.y + npc.height-1)
            || isTileBlocked(npc.x + npc.width-1, npc.y + npc.height-1)) {
            npc.x = oldX; npc.y = oldY;
            npc.bounds.setPosition(npc.x, npc.y);
            pickNextNpcTarget(false);
            return;
        }

        boolean rollback = false;
        if (player != null && npc.bounds.overlaps(player.bounds)) rollback = true;
        if (!rollback) for (int i = 0; i < zombies.size; i++)
            if (npc.bounds.overlaps(zombies.get(i).bounds)) { rollback = true; break; }
        if (rollback) {
            npc.x = oldX; npc.y = oldY;
            npc.bounds.setPosition(npc.x, npc.y);
            pickNextNpcTarget(false);
            return;
        }

        if (Math.abs(dx) > Math.abs(dy)) npc.idle(dt, (dx < 0) ? 1 : 2);
        else                             npc.idle(dt, (dy < 0) ? 0 : 3);
    }

    // ---------- 웨이브 ----------
    private boolean isSchool() { return "maps/school.tmx".equals(currentMapPath); }

    public void spawnWave(int level) {
        if (isSchool()) return;
        playSfx(sfxWaveStart, 0.7f);
        clearZombies();

        int numMonsters;
        float baseSpeed;
        double mulPow;

        if (finalMode) {
            numMonsters = 6 + level * 6;
            baseSpeed   = 6f;
            mulPow      = Math.pow(1.35, Math.max(0, level - 1));
        } else {
            if (level <= 3) numMonsters = level * 3;
            else            numMonsters = 9 + (level - 3) * 5;
            baseSpeed = 5f;
            mulPow    = Math.pow(1.25, Math.max(0, level - 1));
        }

        float mul = (float) mulPow;

        for (int i = 0; i < numMonsters; i++) {
            float zx, zy; int attempts = 0;
            do {
                if (spawnAreaRect != null) {
                    zx = MathUtils.random(spawnAreaRect.x, spawnAreaRect.x + spawnAreaRect.width  - TILE);
                    zy = MathUtils.random(spawnAreaRect.y, spawnAreaRect.y + spawnAreaRect.height - TILE);
                } else {
                    zx = MathUtils.random(0, mapWidthPixels - TILE);
                    zy = MathUtils.random(0, mapHeightPixels - TILE);
                }
                attempts++;
            } while ((isTileBlocked(zx,zy) || distance(zx,zy, player.x,player.y) < 100) && attempts < 100);

            int charIdx = randomZombieCharacter();
            String spriteResolved = resolveSpritePath(ZOMBIE_SHEET, "sprites/zombie.png");
            String variantKey = spriteResolved + "#" + charIdx;
            float variantFactor = getVariantSpeedFactor(variantKey, level);

            float baseSpeedVariant = baseSpeed * variantFactor;

            zombies.add(new Zombie(zx, zy, spriteResolved, charIdx, TILE, baseSpeedVariant, mul, 5));
        }
    }

    private void spawnZombiesForLevel() { spawnWave(Math.max(1, wave)); }

    // ---------- 전환 ----------
    public void startTransition(int mapIndex, float playerX, float playerY) {
        playSfx(sfxTransition, 0.6f); // ★ 추가
        targetMapIndex = mapIndex;
        targetPlayerX = playerX; targetPlayerY = playerY;
        transitioning = true; fadeOut = true; fadeIn = false; fadeAlpha = 0f;
        if (footstepLoop != null && footstepLoop.isPlaying()) footstepLoop.stop();
    }

    private void switchMap(String tmx, float px, float py) {
        currentMapPath = tmx;
        loadMap(currentMapPath);
        projectiles.clear();
        player.x = px; player.y = py;
        player.bounds.setPosition(player.x, player.y);
        state = GameState.PLAYING;

        if ("maps/main.tmx".equals(tmx)) {
            if (finalMode) { wave = Math.max(1, wave); spawnWave(wave); }
            startStageBgm("music/cyberpunk.mp3", 0.6f);
        } else if ("maps/room.tmx".equals(tmx)) {
            startStageBgm("music/horizons.mp3", 0.6f);
        } else if (isSchool()) {
            startStageBgm("music/Ending.mp3", 0.6f);
        }
    }

    private void startStageBgm(String path, float vol) {
        if (bgm != null) { bgm.stop(); bgm.dispose(); }
        bgm = new Music(path, true);
        bgm.setVolume(vol);
        bgm.play();
    }

    // ---------- Collision / Utility ----------
    private boolean isTileBlocked(float wx, float wy) {
        if (collisionLayer == null) return false;

        int tx = (int)(wx / collisionLayer.getTileWidth());
        int ty = (int)(wy / collisionLayer.getTileHeight());
        if (tx < 0 || tx >= collisionLayer.getWidth() || ty < 0 || ty >= collisionLayer.getHeight()) return false;

        TiledMapTileLayer.Cell cell = collisionLayer.getCell(tx, ty);

        Object v = collisionLayer.getProperties().get("blocked");
        boolean layerBlocked = (v instanceof Boolean) ? (Boolean) v
            : (v != null && Boolean.parseBoolean(v.toString()));
        if (layerBlocked) return cell != null && cell.getTile() != null;

        return cell != null && cell.getTile() != null
            && cell.getTile().getProperties().containsKey("blocked");
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return (float)Math.sqrt(dx*dx + dy*dy);
    }

    private void handlePlayerCollision(Player p, float oldX, float oldY) {
        if (p.x < 0) p.x = 0; if (p.y < 0) p.y = 0;
        if (p.x + p.width > mapWidthPixels)  p.x = mapWidthPixels  - p.width;
        if (p.y + p.height > mapHeightPixels) p.y = mapHeightPixels - p.height;
        p.bounds.setPosition(p.x, p.y);

        if (isTileBlocked(p.x, p.y)
            || isTileBlocked(p.x + p.width - 1, p.y)
            || isTileBlocked(p.x, p.y + p.height - 1)
            || isTileBlocked(p.x + p.width - 1, p.y + p.height - 1)) {
            p.x = oldX; p.y = oldY;
            p.bounds.setPosition(p.x, p.y);
        }

        if (npc != null && p.bounds.overlaps(npc.bounds)) {
            resolveAxisByDeltaForPlayer(p, oldX, oldY, npc.bounds);
        }

        for (int i = 0; i < zombies.size; i++) {
            Zombie z = zombies.get(i);
            if (p.bounds.overlaps(z.bounds)) {
                resolveAxisByDeltaForPlayer(p, oldX, oldY, z.bounds);
                break;
            }
        }
    }

    private void handleZombieCollision(Zombie z, float oldX, float oldY) {
        if (z.x < 0) z.x = 0; if (z.y < 0) z.y = 0;
        if (z.x + z.width > mapWidthPixels)  z.x = mapWidthPixels  - z.width;
        if (z.y + z.height > mapHeightPixels) z.y = mapHeightPixels - z.height;
        z.bounds.setPosition(z.x, z.y);

        if (isTileBlocked(z.x, z.y)
            || isTileBlocked(z.x + z.width - 1, z.y)
            || isTileBlocked(z.x, z.y + z.height - 1)
            || isTileBlocked(z.x + z.width - 1, z.y + z.height - 1)) {
            z.x = oldX; z.y = oldY; z.bounds.setPosition(z.x, z.y);
        }

        if (z.bounds.overlaps(player.bounds)) {
            player.onHit(player.x - z.x, player.y - z.y);
            if (player.health <= 0) state = GameState.GAMEOVER;
            resolveAxisByDeltaForZombie(z, oldX, oldY, player.bounds);
        }

        if (npc != null && z.bounds.overlaps(npc.bounds)) {
            resolveAxisByDeltaForZombie(z, oldX, oldY, npc.bounds);
        }

        for (int i = 0; i < zombies.size; i++) {
            Zombie o = zombies.get(i);
            if (o == z) continue;
            if (z.bounds.overlaps(o.bounds)) {
                resolveAxisByDeltaForZombie(z, oldX, oldY, o.bounds);
                break;
            }
        }
    }

    private void updateCamera() {
        float x = player.x + player.width * 0.5f;
        float y = player.y + player.height * 0.5f;

        // 현재 가시 범위(zoom 반영)
        float visibleW = camera.viewportWidth  * camera.zoom;
        float visibleH = camera.viewportHeight * camera.zoom;

        float hw = visibleW * 0.5f;
        float hh = visibleH * 0.5f;

        // 맵이 화면보다 작을 때 중앙 정렬
        if (mapWidthPixels <= visibleW)  x = mapWidthPixels * 0.5f;
        else                              x = MathUtils.clamp(x, hw, mapWidthPixels  - hw);

        if (mapHeightPixels <= visibleH) y = mapHeightPixels * 0.5f;
        else                              y = MathUtils.clamp(y, hh, mapHeightPixels - hh);

        camera.position.set(x, y, 0f);
        camera.update();
    }


    private boolean isNearRect(Rectangle a, Rectangle b, float margin) {
        if (a == null || b == null) return false;
        float ax = a.x - margin, ay = a.y - margin;
        float aw = a.width + margin * 2f, ah = a.height + margin * 2f;
        Rectangle ex = new Rectangle(ax, ay, aw, ah);
        return b.overlaps(ex);
    }

    private boolean nearAnyBook(Rectangle pb, float margin) {
        Rectangle ex = new Rectangle();
        for (Book b : books) {
            if (b == null || b.bounds == null) continue;
            ex.set(b.bounds.x - margin, b.bounds.y - margin, b.bounds.width + margin*2f, b.bounds.height + margin*2f);
            if (pb.overlaps(ex)) return true;
        }
        return false;
    }

    private String overlappingBookType(Rectangle pb) {
        for (Book b : books) {
            if (b == null || b.bounds == null) continue;
            if (pb.overlaps(b.bounds)) return b.type;
        }
        return null;
    }

    private void clearZombies() { for (Zombie z: zombies) z.dispose(); zombies.clear(); }

    private void toast(String msg) { toastText = msg; toastTimer = 1.5f; }

    // ---------- Screen ----------
    @Override public void show() {}

    @Override
    public void resize(int width, int height) {
        if (worldViewport != null) worldViewport.update(width, height, true); // centerCameraOnMap() 효과 포함
        if (hudViewport != null)   hudViewport.update(width, height, true);
        if (pauseViewport != null) pauseViewport.update(width, height, true);
        camera.update();
        hudCamera.update();
    }


    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        if (footstepLoop != null && footstepLoop.isPlaying()) footstepLoop.stop();
        if (bgm != null && bgm.isPlaying()) bgm.stop();
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;

        safeDispose(mapRenderer); mapRenderer = null;
        safeDispose(tiledMap);    tiledMap = null;
        if (mapBatch != null) { mapBatch.dispose(); mapBatch = null; }

        safeDispose(keyTexture);         keyTexture = null;
        safeDispose(doorClosedTexture);  doorClosedTexture = null;
        safeDispose(white1x1);           white1x1 = null;
        safeDispose(victoryTexture);     victoryTexture = null;

        // 책 텍스처 해제
        safeDispose(bookDarkTex);  bookDarkTex  = null;
        safeDispose(bookIceTex);   bookIceTex   = null;
        safeDispose(bookFireTex);  bookFireTex  = null;
        safeDispose(bookLightTex); bookLightTex = null;
        safeDispose(bulletTex);   bulletTex   = null;

        safeDispose(projFireTex);  projFireTex  = null;
        safeDispose(projIceTex);   projIceTex   = null;
        safeDispose(projDarkTex);  projDarkTex  = null;
        safeDispose(projLightTex); projLightTex = null;

        // ----- SFX dispose -----
        try { if (sfxShotFire   != null) sfxShotFire.dispose(); } catch (Throwable ignored) {}
        try { if (sfxShotIce    != null) sfxShotIce.dispose(); } catch (Throwable ignored) {}
        try { if (sfxShotDark   != null) sfxShotDark.dispose(); } catch (Throwable ignored) {}
        try { if (sfxShotLight  != null) sfxShotLight.dispose(); } catch (Throwable ignored) {}

        try { if (sfxHit        != null) sfxHit.dispose(); } catch (Throwable ignored) {}
        try { if (sfxKill       != null) sfxKill.dispose(); } catch (Throwable ignored) {}
        try { if (sfxRicochet   != null) sfxRicochet.dispose(); } catch (Throwable ignored) {}

        try { if (sfxPickupKey  != null) sfxPickupKey.dispose(); } catch (Throwable ignored) {}
        try { if (sfxDoorUnlock != null) sfxDoorUnlock.dispose(); } catch (Throwable ignored) {}
        try { if (sfxDoorOpen   != null) sfxDoorOpen.dispose(); } catch (Throwable ignored) {}

        try { if (sfxUiOpen     != null) sfxUiOpen.dispose(); } catch (Throwable ignored) {}
        try { if (sfxUiCancel   != null) sfxUiCancel.dispose(); } catch (Throwable ignored) {}

        try { if (sfxSelectFire != null) sfxSelectFire.dispose(); } catch (Throwable ignored) {}
        try { if (sfxSelectIce  != null) sfxSelectIce.dispose(); } catch (Throwable ignored) {}
        try { if (sfxSelectDark != null) sfxSelectDark.dispose(); } catch (Throwable ignored) {}
        try { if (sfxSelectLight!= null) sfxSelectLight.dispose(); } catch (Throwable ignored) {}

        try { if (sfxLevelUp    != null) sfxLevelUp.dispose(); } catch (Throwable ignored) {}
        try { if (sfxWaveStart  != null) sfxWaveStart.dispose(); } catch (Throwable ignored) {}
        try { if (sfxVictory    != null) sfxVictory.dispose(); } catch (Throwable ignored) {}

        try { if (sfxDialogOpen != null) sfxDialogOpen.dispose(); } catch (Throwable ignored) {}
        try { if (sfxDialogNext != null) sfxDialogNext.dispose(); } catch (Throwable ignored) {}
        try { if (sfxTransition != null) sfxTransition.dispose(); } catch (Throwable ignored) {}

        // Animation은 Texture를 소유하지 않으므로 dispose 불필요
        fireAnim = iceAnim = darkAnim = lightAnim = null;
        if (hud != null) { hud.dispose(); }
        if (pauseStage != null) { pauseStage.dispose(); pauseStage = null; }
        if (pauseSkin != null)  { pauseSkin.dispose();  pauseSkin  = null; }

        if (npc != null) { npc.dispose(); npc = null; }
        if (bgm != null)         { bgm.dispose(); bgm = null; }
        if (footstepLoop != null){ footstepLoop.dispose(); footstepLoop = null; }
    }

    private static void safeDispose(Disposable d) {
        try { if (d != null) d.dispose(); } catch (Throwable ignored) {}
    }
}
