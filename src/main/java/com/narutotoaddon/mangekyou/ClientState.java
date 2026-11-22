package com.narutotoaddon.mangekyou;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Estado do lado do cliente: implante e olhos cegos de cada jogador visivel (enviado pelo servidor). */
public final class ClientState {
    private static final Map<UUID, String> IMPLANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> BLIND = new ConcurrentHashMap<>();
    private static final Map<UUID, String> BLIND_OWNER = new ConcurrentHashMap<>();
    private static final Map<UUID, String> OWNED = new ConcurrentHashMap<>();
    public static volatile UUID self;

    private ClientState() {}

    public static void set(UUID player, String implantId, int blindMask, String blindOwner, String owned) {
        if (implantId == null || implantId.isEmpty()) {
            OWNED.remove(player);
            IMPLANTS.remove(player);
            BLIND.remove(player);
            BLIND_OWNER.remove(player);
            return;
        }
        IMPLANTS.put(player, implantId);
        OWNED.put(player, owned == null || owned.isEmpty() ? implantId : owned);
        if (blindMask == 0) {
            BLIND.remove(player);
            BLIND_OWNER.remove(player);
        } else {
            BLIND.put(player, blindMask);
            if (blindOwner == null || blindOwner.isEmpty()) BLIND_OWNER.remove(player);
            else BLIND_OWNER.put(player, blindOwner);
        }
    }

    public static Implant get(UUID player) {
        return player == null ? null : Implant.byId(IMPLANTS.get(player));
    }

    /** Bit 1 = olho esquerdo cego, bit 2 = olho direito cego. */
    public static int blind(UUID player) {
        return player == null ? 0 : BLIND.getOrDefault(player, 0);
    }

    /** Implante que causou a cegueira (o seu Mangekyou define a textura cega); null se nao ha dono. */
    public static Implant blindOwner(UUID player) {
        return player == null ? null : Implant.byId(BLIND_OWNER.get(player));
    }

    public static Implant mine() {
        return get(self);
    }

    /** Todos os olhos possuidos por um jogador (o olho ativo e um deles). */
    public static java.util.List<Implant> owned(UUID player) {
        java.util.List<Implant> out = new java.util.ArrayList<>();
        String csv = player == null ? null : OWNED.get(player);
        if (csv == null) return out;
        for (String id : csv.split(",")) {
            Implant i = Implant.byId(id.trim());
            if (i != null && !out.contains(i)) out.add(i);
        }
        return out;
    }

    public static java.util.List<Implant> myOwned() {
        return owned(self);
    }

    public static int myBlind() {
        return blind(self);
    }
}
