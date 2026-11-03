package com.narutotoaddon.mangekyouizanagi;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Ponte por reflexao para as variaveis do jogador do mod Narutoto (NarutotoModVariables.PlayerVariables).
 * Assim o addon nao precisa do jar do Narutoto para compilar.
 */
public final class Bridge {
    private Bridge() {}

    private static Capability<?> cap;
    private static boolean capTried;
    private static final Map<String, Field> FIELDS = new HashMap<>();

    private static Capability<?> cap() {
        if (!capTried) {
            capTried = true;
            try {
                Class<?> c = Class.forName("net.mcreator.narutoto.network.NarutotoModVariables");
                cap = (Capability<?>) c.getField("PLAYER_VARIABLES_CAPABILITY").get(null);
            } catch (Throwable t) {
                System.err.println("[narutoto_mangekyou_izanagi] Nao achei PLAYER_VARIABLES_CAPABILITY: " + t);
            }
        }
        return cap;
    }

    public static Object vars(Entity e) {
        Capability<?> c = cap();
        if (c == null || e == null) return null;
        LazyOptional<?> lo = e.getCapability(c);
        return lo.resolve().orElse(null);
    }

    private static Field field(Object vars, String name) throws NoSuchFieldException {
        Field f = FIELDS.get(name);
        if (f == null) {
            f = vars.getClass().getField(name);
            FIELDS.put(name, f);
        }
        return f;
    }

    public static double getDouble(Entity e, String name, double def) {
        try {
            Object v = vars(e);
            return v == null ? def : field(v, name).getDouble(v);
        } catch (Throwable t) {
            return def;
        }
    }

    public static String getString(Entity e, String name, String def) {
        try {
            Object v = vars(e);
            Object o = v == null ? null : field(v, name).get(v);
            return o == null ? def : o.toString();
        } catch (Throwable t) {
            return def;
        }
    }

    public static boolean setString(Entity e, String name, String value) {
        try {
            Object v = vars(e);
            if (v == null) return false;
            field(v, name).set(v, value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void sync(Entity e) {
        try {
            Object v = vars(e);
            if (v != null) v.getClass().getMethod("syncPlayerVariables", Entity.class).invoke(v, e);
        } catch (Throwable t) {
            System.err.println("[narutoto_mangekyou_izanagi] sync falhou: " + t);
        }
    }

    /** Chama um procedure estatico do Narutoto do tipo execute(Entity). */
    public static void callProcedure(String simpleName, Entity e) {
        try {
            Class.forName("net.mcreator.narutoto.procedures." + simpleName)
                    .getMethod("execute", Entity.class).invoke(null, e);
        } catch (Throwable t) {
            System.err.println("[narutoto_mangekyou_izanagi] " + simpleName + " falhou: " + t);
        }
    }
}
