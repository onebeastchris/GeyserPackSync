package net.onebeastchris.geyserperserverpacks.common.utils;

import java.util.HashMap;

public interface PlayerStorage {
    final HashMap<String, serverStorage> temp = new HashMap<>();

    public default void addPlayer(String xuid, serverStorage serverStorage) {
        temp.put(xuid, serverStorage);
    }

    public default void removePlayer(String xuid) {
        temp.remove(xuid);
    }

    interface serverStorage {
        String targetServer();

        boolean initialJoin();
    }
}
