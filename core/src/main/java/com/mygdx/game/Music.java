package com.mygdx.game;

import com.badlogic.gdx.Gdx;

/**
 * LibGDX com.badlogic.gdx.audio.Music 래퍼.
 * - stop(): 재생만 중지(자원 유지)
 * - play(): 재생 시작/재개
 * - isPlaying(): 재생 중 여부
 * - dispose()/close(): 자원 해제(더 이상 사용 불가)
 * 경로는 core/assets 기준 내부 경로. 예: "assets/music/intro.mp3"
 */
public class Music {
    private com.badlogic.gdx.audio.Music music;
    private boolean disposed = false;

    public Music(String internalPath, boolean loop) {
        music = Gdx.audio.newMusic(Gdx.files.internal(internalPath));
        music.setLooping(loop);
    }

    /** == play() */
    public void start() {
        if (music != null && !disposed) music.play();
    }

    /** 재생 */
    public void play() {
        if (music != null && !disposed) music.play();
    }

    /** 일시 정지 */
    public void pause() {
        if (music != null && !disposed) music.pause();
    }

    /** 재개 == play() */
    public void resume() {
        if (music != null && !disposed) music.play();
    }

    /** 재생 중지(자원 유지) */
    public void stop() {
        if (music != null && !disposed) music.stop();
    }

    /** 자원 해제(중지+dispose). 이후 이 인스턴스는 재사용 불가 */
    public void close() {
        dispose();
    }

    /** 현재 재생 중인지 */
    public boolean isPlaying() {
        return music != null && !disposed && music.isPlaying();
    }

    /** 생존 여부(자원 해제되지 않았는지). 기존 Thread isAlive 대체용 */
    public boolean isAlive() {
        return music != null && !disposed;
    }

    /** 음량 0~1 */
    public void setVolume(float volume01) {
        if (music != null && !disposed) {
            float v = Math.max(0f, Math.min(1f, volume01));
            music.setVolume(v);
        }
    }

    /** 루프 여부 변경(필요 시) */
    public void setLooping(boolean loop) {
        if (music != null && !disposed) music.setLooping(loop);
    }

    /** 자원 해제 */
    public void dispose() {
        if (music != null && !disposed) {
            try { music.stop(); } catch (Exception ignored) {}
            music.dispose();
            disposed = true;
            music = null;
        }
    }
}
