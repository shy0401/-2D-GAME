package com.mygdx.game.entity;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public final class NpcLoader {

    private NpcLoader() {}

    /** Tiled ObjectLayer("NPC")에서 NPC를 읽어와 생성한다. */
    public static Array<NPC> loadFrom(TiledMap map, String layerName, String mapName) {
        Array<NPC> out = new Array<>();
        MapLayer layer = map.getLayers().get(layerName);
        if (layer == null) return out;

        MapProperties p = map.getProperties();
        int mapWpx = (p.get("width", Integer.class)) * (p.get("tilewidth", Integer.class));
        int mapHpx = (p.get("height", Integer.class)) * (p.get("tileheight", Integer.class));

        for (MapObject mo : layer.getObjects()) {
            if (!(mo instanceof RectangleMapObject)) continue;

            Rectangle r = ((RectangleMapObject) mo).getRectangle();

            // Tiled(Y-down) → 게임(Y-up) 변환
            float gx = r.x;
            float gy = mapHpx - r.y - r.height;

            // 속성 파싱(기본값 포함)
            MapProperties mp = mo.getProperties();
            String sprite = getStr(mp, "sprite", "gfx/char_8x12.png");
            int character = getInt(mp, "character", 0);
            int size = getInt(mp, "size", Math.round(r.width)); // 오브젝트 폭을 기본값으로
            float speed = getFloat(mp, "speed", 60f);
            String characterType = getStr(mp, "characterType", "0");

            NPC npc = new NPC(gx, gy, sprite, character, size);
            npc.speed = speed;
            npc.setDialogues(mapName, characterType);

            out.add(npc);
        }
        return out;
    }

    private static String getStr(MapProperties mp, String key, String def) {
        Object v = mp.get(key);
        return v == null ? def : String.valueOf(v);
    }
    private static int getInt(MapProperties mp, String key, int def) {
        Object v = mp.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        try { return v == null ? def : Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }
    private static float getFloat(MapProperties mp, String key, float def) {
        Object v = mp.get(key);
        if (v instanceof Number) return ((Number) v).floatValue();
        try { return v == null ? def : Float.parseFloat(String.valueOf(v)); } catch (Exception e) { return def; }
    }
}
