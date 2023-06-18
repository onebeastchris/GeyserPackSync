package net.onebeastchris.geyserperserverpacks.common.utils;

import java.util.HashMap;
import java.util.UUID;

public class PlayerStorage {

    // UUID is the player, Storage stores details about which packs to apply/which server to send to
    HashMap<UUID, Storage> temp = new HashMap<>();

    void addPlayer(UUID uuid, Storage serverStorage) {
        temp.put(uuid, serverStorage);
    }

    // what else do we need to store? a fallback? whether the packs have been sent? o.ó
    record Storage(String server){
    }
}
