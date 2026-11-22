package com.narutotoaddon.mangekyou;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
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
                System.err.println("[narutoto_mangekyou] Nao achei PLAYER_VARIABLES_CAPABILITY: " + t);
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

    public static boolean setDouble(Entity e, String name, double value) {
        try {
            Object v = vars(e);
            if (v == null) return false;
            field(v, name).setDouble(v, value);
            return true;
        } catch (Throwable t) {
            return false;
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
            System.err.println("[narutoto_mangekyou] sync falhou: " + t);
        }
    }

    public static boolean getBool(Entity e, String name, boolean def) {
        try {
            Object v = vars(e);
            return v == null ? def : field(v, name).getBoolean(v);
        } catch (Throwable t) {
            return def;
        }
    }

    public static boolean setBool(Entity e, String name, boolean value) {
        try {
            Object v = vars(e);
            if (v == null) return false;
            field(v, name).setBoolean(v, value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Chama um procedure estatico do Narutoto do tipo execute(LevelAccessor, Entity). */
    public static void callProcedureWorld(String simpleName, Entity e) {
        try {
            Class.forName("net.mcreator.narutoto.procedures." + simpleName)
                    .getMethod("execute", LevelAccessor.class, Entity.class).invoke(null, e.level(), e);
        } catch (Throwable t) {
            System.err.println("[narutoto_mangekyou] " + simpleName + " falhou: " + t);
        }
    }

    /** Chama um procedure estatico do Narutoto do tipo execute(LevelAccessor, Entity alvo, Entity origem). */
    public static void callProcedureWorld(String simpleName, Entity target, Entity source) {
        try {
            Class.forName("net.mcreator.narutoto.procedures." + simpleName)
                    .getMethod("execute", LevelAccessor.class, Entity.class, Entity.class)
                    .invoke(null, source.level(), target, source);
        } catch (Throwable t) {
            System.err.println("[narutoto_mangekyou] " + simpleName + " falhou: " + t);
        }
    }

    /**
     * Roda um trecho do Narutoto com o estagio do dojutsu (e, se informado, o dono do Mangekyou) trocados so por esse
     * instante. Serve para o Kamui ORIGINAL (que so aceita o Mangekyou comum, estagio 2) funcionar tambem no
     * Mangekyou Eterno e no Rinnegan. Os valores originais sao sempre restaurados e sincronizados.
     */
    public static void withState(Entity e, double stage, String owner, Runnable r) {
        double oldStage = getDouble(e, "if_sharigan", stage);
        String oldOwner = getString(e, "mangekyo_owner", "");
        setDouble(e, "if_sharigan", stage);
        if (owner != null) setString(e, "mangekyo_owner", owner);
        try {
            r.run();
        } finally {
            setDouble(e, "if_sharigan", oldStage);
            setString(e, "mangekyo_owner", oldOwner);
            sync(e);
        }
    }

    /** Chama um procedure estatico do Narutoto do tipo execute(Entity). */
    public static void callProcedure(String simpleName, Entity e) {
        try {
            Class.forName("net.mcreator.narutoto.procedures." + simpleName)
                    .getMethod("execute", Entity.class).invoke(null, e);
        } catch (Throwable t) {
            System.err.println("[narutoto_mangekyou] " + simpleName + " falhou: " + t);
        }
    }
}
