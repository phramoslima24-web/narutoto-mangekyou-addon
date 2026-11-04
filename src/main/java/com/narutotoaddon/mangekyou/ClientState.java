package com.narutotoaddon.mangekyou;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Estado do lado do cliente: implante e olhos cegos de cada jogador visivel (enviado pelo servidor). */
public final class ClientState {
    private static final Map<UUID, String> IMPLANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> BLIND = new ConcurrentHashMap<>();
    public static volatile UUID self;

    private ClientState() {}

    public static void set(UUID player, String implantId, int blindMask) {
        if (implantId == null || implantId.isEmpty()) {
            IMPLANTS.remove(player);
            BLIND.remove(player);
            return;
        }
        IMPLANTS.put(player, implantId);
        if (blindMask == 0) BLIND.remove(player);
        else BLIND.put(player, blindMask);
    }

    public static Implant get(UUID player) {
        return player == null ? null : Implant.byId(IMPLANTS.get(player));
    }

    /** Bit 1 = olho esquerdo cego, bit 2 = olho direito cego. */
    public static int blind(UUID player) {
        return player == null ? 0 : BLIND.getOrDefault(player, 0);
    }

    public static Implant mine() {
        return get(self);
    }

    public static int myBlind() {
        return blind(self);
    }
}
