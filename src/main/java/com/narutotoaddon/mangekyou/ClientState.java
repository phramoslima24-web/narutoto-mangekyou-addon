package com.narutotoaddon.mangekyou;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Estado do lado do cliente: implante de cada jogador visivel (enviado pelo servidor). */
public final class ClientState {
    private static final Map<UUID, String> IMPLANTS = new ConcurrentHashMap<>();
    public static volatile UUID self;

    private ClientState() {}

    public static void set(UUID player, String implantId) {
        if (implantId == null || implantId.isEmpty()) IMPLANTS.remove(player);
        else IMPLANTS.put(player, implantId);
    }

    public static Implant get(UUID player) {
        return player == null ? null : Implant.byId(IMPLANTS.get(player));
    }

    public static Implant mine() {
        return get(self);
    }
}
