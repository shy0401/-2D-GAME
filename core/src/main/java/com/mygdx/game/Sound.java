package com.mygdx.game;

import com.badlogic.gdx.Gdx;

/**
 * 짧은 효과음용 래퍼. 필요시 play(volume) 사용.
 * 경로는 assets/ 내부 기준. 예: "sfx/hit.wav"
 */
public class Sound {
    private com.badlogic.gdx.audio.Sound sound;

    public Sound(String internalPath) {
        if (internalPath != null && !internalPath.isEmpty()) {
            sound = Gdx.audio.newSound(Gdx.files.internal(internalPath));
        }
    }

    public long play() {
        return (sound != null) ? sound.play() : -1L;
    }

    public long play(float volume01) {
        return (sound != null) ? sound.play(Math.max(0f, Math.min(1f, volume01))) : -1L;
    }

    public void stop() {
        if (sound != null) sound.stop();
    }

    public void dispose() {
        if (sound != null) {
            sound.dispose();
            sound = null;
        }
    }
}
