package com.mygdx.game.entity;

import com.badlogic.gdx.math.Rectangle;

public class Projectile {
    public float x, y;
    public float width, height;
    public float vx, vy;   // velocity components (pixels per second)
    public float speed;
    public Rectangle bounds;

    public Projectile(float x, float y, float vx, float vy, float speed) {
        this.x = x;
        this.y = y;
        this.width = 8;
        this.height = 8;
        this.vx = vx;
        this.vy = vy;
        this.speed = speed;
        this.bounds = new Rectangle(x, y, width, height);
    }

    /** Update projectile position. Returns false if the projectile should be removed (e.g., out of bounds). */
    public boolean update(float dt, int mapWidthPixels, int mapHeightPixels) {
        // Move projectile
        x += vx * speed * dt;
        y += vy * speed * dt;
        bounds.setPosition(x, y);
        // Check if out of map bounds -> remove it
        if (x < 0 || x > mapWidthPixels || y < 0 || y > mapHeightPixels) {
            return false;  // off screen
        }
        return true;
    }
}
