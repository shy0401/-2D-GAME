package com.mygdx.game.entity;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.mygdx.game.gfx.SpriteManagerGdx;

public class Zombie {
    public float x, y;
    public float width, height;
    public float speed;
    public int health;
    public final Rectangle bounds;

    private final SpriteManagerGdx sm;
    private final Animation<TextureRegion> down, left, right, up;
    private float stateTime = 0f;
    private int facing = 0;
    private boolean moving = false;
    private boolean disposed = false;

    public Zombie(float x, float y, String spritePath, int character, int drawSize,
                  float baseSpeed, float speedMul, int hp) {
        this.x = x;
        this.y = y;
        this.width = drawSize;
        this.height = drawSize;
        this.speed = baseSpeed * speedMul;
        this.health = hp;

        this.sm = new SpriteManagerGdx(spritePath, character);

        TextureRegion[] d = { sm.get("down1"),  sm.get("down2"),  sm.get("down3")  };
        TextureRegion[] l = { sm.get("left1"),  sm.get("left2"),  sm.get("left3")  };
        TextureRegion[] r = { sm.get("right1"), sm.get("right2"), sm.get("right3") };
        TextureRegion[] u = { sm.get("up1"),    sm.get("up2"),    sm.get("up3")    };

        Animation.PlayMode loop = Animation.PlayMode.LOOP_PINGPONG;
        down  = new Animation<>(0.18f, d); down.setPlayMode(loop);
        left  = new Animation<>(0.18f, l); left.setPlayMode(loop);
        right = new Animation<>(0.18f, r); right.setPlayMode(loop);
        up    = new Animation<>(0.18f, u); up.setPlayMode(loop);

        this.bounds = new Rectangle(x, y, width, height);
    }

    public Zombie(float x, float y, float baseSpeed, float speedMultiplier) {
        this(x, y, "sprites/zombie.png", 1, 32, baseSpeed, speedMultiplier, 5);
    }

    public void update(float dt, float targetX, float targetY) {
        float vx = targetX - x;
        float vy = targetY - y;
        float dist = (float) Math.sqrt(vx * vx + vy * vy);

        moving = dist > 1e-3f;
        if (moving) {
            vx /= dist; vy /= dist;
            if (Math.abs(vx) > Math.abs(vy)) {
                facing = (vx < 0) ? 1 : 2;
            } else {
                facing = (vy < 0) ? 0 : 3;
            }

            float step = speed * dt;
            x += vx * step;
            y += vy * step;
            bounds.setPosition(x, y);

            stateTime += dt;
        }
    }

    public boolean takeDamage(int damage) {
        health -= damage;
        return health <= 0;
    }

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
