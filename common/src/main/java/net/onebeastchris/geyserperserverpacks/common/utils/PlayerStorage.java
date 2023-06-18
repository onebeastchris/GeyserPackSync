package net.onebeastchris.geyserperserverpacks.common.utils;

import java.util.HashMap;

public class PlayerStorage {
    HashMap<String, Storage> temp = new HashMap<>();

    void addPlayer(String xuid, Storage serverStorage) {
        temp.put(xuid, serverStorage);
    }

    interface Storage {
        String targetServer();

        boolean initialJoin();
    }
}
