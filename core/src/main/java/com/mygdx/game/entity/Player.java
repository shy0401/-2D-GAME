package com.mygdx.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.mygdx.game.gfx.SpriteManagerGdx;

public class Player {
    public float x, y;
    public float width, height;
    public float speed = 140f;
    public int health = 5;

    public final Rectangle bounds;

    private final SpriteManagerGdx sm;
    private Animation<TextureRegion> down, left, right, up;
    private float stateTime = 0f;
    private int facing = 0;              // 0:down,1:left,2:right,3:up
    private boolean moving = false;

    private boolean invincible = false;
    private float iTime = 0f;
    private final float iDuration = 1.0f;

    private boolean disposed = false;

    public Player(float startX, float startY, String spritePath, int character, int drawSize) {
        this.x = startX;
        this.y = startY;
        this.width = drawSize;
        this.height = drawSize;

        this.sm = new SpriteManagerGdx(spritePath, character);

        TextureRegion[] d = { sm.get("down1"),  sm.get("down2"),  sm.get("down3")  };
        TextureRegion[] l = { sm.get("left1"),  sm.get("left2"),  sm.get("left3")  };
        TextureRegion[] r = { sm.get("right1"), sm.get("right2"), sm.get("right3") };
        TextureRegion[] u = { sm.get("up1"),    sm.get("up2"),    sm.get("up3")    };

        down  = new Animation<>(0.15f, d);
        left  = new Animation<>(0.15f, l);
        right = new Animation<>(0.15f, r);
        up    = new Animation<>(0.15f, u);
        down.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        left.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        right.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        up.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);

        this.bounds = new Rectangle(x, y, width, height);
    }

    public Player(float startX, float startY) {
        this(startX, startY, "sprites/Actor1.png", 1, 16);
    }

    public void update(float dt) {
        float dx = 0, dy = 0; moving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            dx -= speed * dt; facing = 1; moving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            dx += speed * dt; facing = 2; moving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            dy += speed * dt; facing = 3; moving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            dy -= speed * dt; facing = 0; moving = true;
        }

        x += dx; y += dy;
        bounds.setPosition(x, y);
        if (moving) stateTime += dt;

        if (invincible) {
            iTime += dt;
            if (iTime >= iDuration) { invincible = false; iTime = 0f; }
        }
    }

    public void onHit(float knockX, float knockY) {
        if (invincible) return;
        health = Math.max(0, health - 1);
        invincible = true; iTime = 0f;

        float len = (float)Math.sqrt(knockX*knockX + knockY*knockY);
        if (len > 1e-4f) {
            float k = 30f;
            x += (knockX/len)*k; y += (knockY/len)*k; bounds.setPosition(x, y);
        }
    }

    public boolean shouldDraw() {
        if (!invincible) return true;
        return ((int)(iTime * 20f) % 2) == 0;
    }

    public void draw(Batch batch) {
        if (!shouldDraw()) return;
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
