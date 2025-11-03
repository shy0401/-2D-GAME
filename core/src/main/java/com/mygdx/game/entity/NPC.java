package com.mygdx.game.entity;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.mygdx.game.gfx.SpriteManagerGdx;

/** Player/Zombie와 동일한 8x12(3x4 캐릭터) 시트를 쓰는 NPC */
public class NPC {
    public float x, y;
    public float width, height;         // 화면에 그릴 크기(px)
    public float speed = 60f;           // 걷기 속도 (인트로에선 덮어씀)
    public final Rectangle bounds;

    private final SpriteManagerGdx sm;
    private final Animation<TextureRegion> down, left, right, up;
    private float stateTime = 0f;
    private int facing = 0;             // 0:down,1:left,2:right,3:up
    private boolean moving = false;

    private final String[][] dialogues = new String[10][]; // set 단위 저장
    private int dialogueSet = 0;
    private int dialogueIndex = 0;

    private boolean disposed = false;

    public NPC(float x, float y, String spritePath, int character, int drawSize) {
        this.x = x; this.y = y;
        this.width = drawSize; this.height = drawSize;

        sm = new SpriteManagerGdx(spritePath, character);

        TextureRegion[] d = { sm.get("down1"),  sm.get("down2"),  sm.get("down3")  };
        TextureRegion[] l = { sm.get("left1"),  sm.get("left2"),  sm.get("left3")  };
        TextureRegion[] r = { sm.get("right1"), sm.get("right2"), sm.get("right3") };
        TextureRegion[] u = { sm.get("up1"),    sm.get("up2"),    sm.get("up3")    };

        Animation.PlayMode loop = Animation.PlayMode.LOOP_PINGPONG;
        down  = new Animation<>(0.18f, d); down.setPlayMode(loop);
        left  = new Animation<>(0.18f, l); left.setPlayMode(loop);
        right = new Animation<>(0.18f, r); right.setPlayMode(loop);
        up    = new Animation<>(0.18f, u); up.setPlayMode(loop);

        bounds = new Rectangle(x, y, width, height);
    }

    public void setDialogues(String mapName, String characterType) {
        for (int i = 0; i < dialogues.length; i++) dialogues[i] = null;

        switch (mapName) {
            case "room": {
                if ("0".equals(characterType)) {
                    dialogues[0] = new String[]{
                        "헉..헉..!", "꿈이었구나...", "(여태까지 꾼 꿈중 가장 현실감이 느껴졌다.)",
                        "(뭔가 기분이 불쾌하고 찝찝하다.)", "(내일 엘리에게 말해봐야겠다.)"
                    };
                }
                break;
            }
            case "main": {
                if ("2".equals(characterType)) {
                    dialogues[0] = new String[]{
                        "엘리: 여기가 정원이구나!",
                        "힘들었을텐데 학교 소개시켜줘서\n정말 고마워!",
                        "나: 어..아니야.", "엘리: 아니긴 뭐가 아니야.",
                        "이런 좋은 공간은 너 아니었으면\n몰랐을거야.", "너랑 친구돼서 정말 다행이다."
                    };
                }
                break;
            }
            case "school": {
                switch (characterType) {
                    case "0":
                        dialogues[0] = new String[]{
                            "담임선생: 자 얘들아 오늘 새로운\n전학생이 왔다.", "잘 적응할 수 있도록 너희들이\n잘 도와주거라.",
                            "엘리: 안녕 난 엘리라고 해.\n잘 부탁해.", "짝짝짝짝",
                            "담임선생: 엘리는 저기 빈자리에 앉으렴.", "((주인공의 옆자리에 앉으며))",
                            "엘리: 안녕.", "나: 어.. 안녕.", "엘리: 앞으로 잘 지내보자!", "나: 그래.."
                        };
                        break;
                    case "1":
                        dialogues[0] = new String[]{
                            "철학선생: 오늘은 철학자들의\n종교적 관점에 대해서 알아보고 의견을\n나눠보도록 하겠습니다.",
                            "먼저 철학자 아리스토텔레스는...",
                            "나: (나는 신따위는 없다고 생각한다.)",
                            "(만약 신이 존재한다면 무책임한 방관자일 뿐이고\n그런 신이라면 없다고 여기는 게 나을 것이다.)",
                            "(저 하늘에 어떤 존재가 있다면\n그것은 타락한 것들 뿐일 것이다.)",
                            "(저주받은 나의 인생처럼 말이다.)",
                            "(내가 원하는 것은 그리 큰 것이 아니다.)",
                            "(그저 평화롭고 화목한 가정에서\n따뜻한 밥을 먹고 친구들과 즐겁게\n학교생활을 하고 싶을 뿐이다.)",
                            "(그 사소한 것들마저 이뤄지지 않는\n인생이 저주받은 것이 아니면 뭐란 말인가)",
                            "철학선생: 이제 각자의 생각을 발표해봅시다."
                        };
                        break;
                    case "2":
                        dialogues[0] = new String[]{
                            "엘리: 저기, 내가 학교를 잘 몰라서\n그러는데 소개 좀 시켜줄래?", "나: 알겠어."
                        };
                        break;
                    case "3":
                        dialogues[0] = new String[]{
                            "체육선생: 자, 이제 알려줬으니까\n두 명씩 짝지어서 주고받아보거라"
                        };
                        break;
                }
                break;
            }
            case "night": {
                if ("2".equals(characterType)) {
                    dialogues[0] = new String[]{
                        "나: 엄마, 이제 들어가셔야죠.", "엄마: ...", "나: (언제까지 이렇게 살아야 하는걸까...)"
                    };
                }
                if (dialogues[0] == null) dialogues[0] = new String[]{"나: 엄마, 이제 들어가셔야죠."};
                break;
            }
            default:
                dialogues[0] = new String[]{"..."};
        }
        dialogueSet = 0;
        dialogueIndex = 0;
    }

    public void beginDialogue(int set) { dialogueSet = Math.max(0, Math.min(set, dialogues.length - 1)); dialogueIndex = 0; }
    public String nextLine() { String[] arr = dialogues[dialogueSet]; if (arr == null || dialogueIndex >= arr.length) return null; return arr[dialogueIndex++]; }
    public String[] getLines(int set) { return dialogues[set]; }

    public void moveToward(float tx, float ty, float dt) {
        float dx = tx - x, dy = ty - y;
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        moving = len > 1e-3f;
        if (!moving) return;

        dx /= len; dy /= len;
        if (Math.abs(dx) > Math.abs(dy)) facing = (dx < 0) ? 1 : 2; else facing = (dy < 0) ? 0 : 3;

        x += dx * speed * dt;
        y += dy * speed * dt;
        bounds.setPosition(x, y);
        stateTime += dt;
    }

    public void idle(float dt, int face) { moving = false; facing = face; }
    public void draw(Batch batch) {
        TextureRegion frame;
        switch (facing) {
            case 1:  frame = left.getKeyFrame(moving ? stateTime : 0f); break;
            case 2:  frame = right.getKeyFrame(moving ? stateTime : 0f); break;
            case 3:  frame = up.getKeyFrame(moving ? stateTime : 0f); break;
            default: frame = down.getKeyFrame(moving ? stateTime : 0f); break;
        }
        batch.draw(frame, x, y, width, height);
    }
    public void dispose() {
        if (disposed) return;
        disposed = true;
        sm.dispose();
    }
}
