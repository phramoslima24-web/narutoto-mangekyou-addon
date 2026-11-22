package com.narutotoaddon.mangekyou;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Ponte entre o Susanoo ORIGINAL do Narutoto e os Susanoos do addon.
 * O jogador continua usando a habilidade original; quando o mod original invoca a entidade do Sasuke, este gerenciador
 * olha o implante (Madara/Itachi/Shisui) e troca a entidade pela versao correspondente, no mesmo estagio.
 * Sasuke (ou sem implante) nao e tocado.
 */
public final class SusanooManager {
    private SusanooManager() {}

    private static final Map<String, Integer> ORIGINAL_STAGE = Map.of("sasuke_rib_entity", 1, "sasuke_skeleton_entity", 2,
            "sasuke_complete_entity", 3, "sasuke_perfect", 4);
    private static final Set<String> SHADOW_CLONES = Set.of("clone_new", "sage_clone");
    private static final String[] FIRE_WORDS = {"fireball", "goryuka", "housenka", "daiendan", "goka_mekkyaku", "gamayu",
            "haijin", "katon", "flame"};
    public static final String CLONE_FLAG = "mkaddon_clone_susanoo";
    public static final String HIDDEN = "mkaddon_hidden";
    public static final String FIRE_TS = "mkaddon_fire_ts";
    public static final int MAX_CLONE_SUSANOOS = 12;

    private static final Map<UUID, Integer> MAT = new HashMap<>();

    // ------------------------------------------------------------- consultas
    public static CharacterSusanoo find(Player p, SusanooKind k) {
        if (p == null) return null;
        for (CharacterSusanoo s : mine(p, false)) {
            if (k == null || s.kind() == k) return s;
        }
        return null;
    }

    /** Susanoos vivos do jogador (clone=false: so o principal; true: so os dos clones). */
    public static List<CharacterSusanoo> mine(Player p, boolean clones) {
        return p.level().getEntitiesOfClass(CharacterSusanoo.class, p.getBoundingBox().inflate(96.0D, 96.0D, 96.0D),
                s -> s.isAlive() && !s.isRemoved() && s.getFade() == 0 && s.isClone() == clones
                        && p.getUUID().equals(s.getOwnerUUID()));
    }

    private static int originalStage(Entity e) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        if (id == null || !id.getNamespace().equals("narutoto")) return 0;
        return ORIGINAL_STAGE.getOrDefault(id.getPath(), 0);
    }

    private static boolean isShadowClone(Entity e) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return id != null && id.getNamespace().equals("narutoto") && SHADOW_CLONES.contains(id.getPath());
    }

    // --------------------------------------------------------------- criacao
    public static CharacterSusanoo spawn(ServerPlayer p, SusanooKind k, int stage, Vec3 pos, float yaw, boolean clone,
                                         UUID anchor) {
        ServerLevel lv = p.serverLevel();
        EntityType<CharacterSusanoo> type = switch (k) {
            case ITACHI -> ModEntities.ITACHI_SUSANOO.get();
            case SHISUI -> ModEntities.SHISUI_SUSANOO.get();
            default -> ModEntities.MADARA_SUSANOO.get();
        };
        CharacterSusanoo s = type.create(lv);
        if (s == null) return null;
        s.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        s.init(p, stage, clone, anchor);
        lv.addFreshEntity(s);
        float h = SusanooGeometry.chestHeight(stage, clone) * 1.4F;
        lv.sendParticles(k.dust(2.0F), pos.x, pos.y + h * 0.4D, pos.z, 50, 1.5D, h * 0.3D, 1.5D, 0.06D);
        lv.sendParticles(k.glowDust(1.5F), pos.x, pos.y + h * 0.4D, pos.z, 20, 1.5D, h * 0.3D, 1.5D, 0.1D);
        return s;
    }

    /**
     * O Susanoo original do Sasuke NAO e removido (assim a montaria, a espada, o arco, os punhos e todos os poderes
     * dele continuam funcionando). Ele fica invisivel e o Susanoo do Mangekyou o acompanha como corpo visual
     * e das habilidades exclusivas.
     */
    private static void attach(ServerPlayer p, SusanooKind k, Mob original, int stage) {
        UUID oid = original.getUUID();
        boolean has = false;
        for (CharacterSusanoo s : mine(p, false)) {
            if (oid.equals(s.getAnchorUuid()) && s.kind() == k && s.getStage() == stage) { has = true; break; }
        }
        if (!has) {
            for (CharacterSusanoo old : mine(p, false)) old.discard();
            spawn(p, k, stage, original.position(), original.getYRot(), false, oid);
        }
        if (!original.isInvisible()) original.setInvisible(true);
        original.getPersistentData().putBoolean(HIDDEN, true);
    }

    // ---------------------------------------------------------------- eventos
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side.isClient()) return;
        if (!(e.player instanceof ServerPlayer p)) return;
        SusanooKind k = SusanooKind.of(p);
        if (k == null) {
            MAT.remove(p.getUUID());
            if (p.tickCount % 20 == 0) {
                for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16.0D, 50.0D, 16.0D),
                        x -> originalStage(x) > 0 && x.getPersistentData().getBoolean(HIDDEN))) {
                    m.setInvisible(false);
                    m.getPersistentData().remove(HIDDEN);
                }
            }
            return;
        }
        if (p.tickCount % 2 != 0) return;

        // 1) o mod original invocou a entidade do Sasuke? troca pela do Mangekyou equipado
        AABB box = p.getBoundingBox().inflate(16.0D, 50.0D, 16.0D);
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, box,
                x -> originalStage(x) > 0 && x instanceof TamableAnimal t && t.isOwnedBy(p))) {
            attach(p, k, m, originalStage(m));
        }

        // 2) Susanoo "ativo" no mod mas sem entidade (troca de implante, relogar, trocar de dimensao): invoca o certo
        double st = Bridge.getDouble(p, "if_susanoo", 0.0D);
        CharacterSusanoo cur = find(p, k);
        if (st >= 1.0D && st <= 4.0D && Implants.mangekyouActive(p)) {
            if (cur == null || cur.getStage() < (int) st) { // nunca rebaixa/recria um Susanoo ja maior ou montado
                int n = MAT.merge(p.getUUID(), 1, Integer::sum);
                if (n >= 25) {
                    MAT.remove(p.getUUID());
                    boolean stillOriginal = !p.level().getEntitiesOfClass(Mob.class, box,
                            x -> originalStage(x) > 0 && x instanceof TamableAnimal t && t.isOwnedBy(p)).isEmpty();
                    if (!stillOriginal) {
                        for (CharacterSusanoo old : mine(p, false)) old.discard();
                        CharacterSusanoo s = spawn(p, k, (int) st, p.position(), p.getYRot(), false, null);
                        if (s != null && (int) st == 4 && p.getVehicle() == null) p.startRiding(s, true);
                    }
                }
            } else {
                MAT.remove(p.getUUID());
            }
        } else {
            MAT.remove(p.getUUID());
        }

        // 3) Madara: Susanoos nos clones
        if (k == SusanooKind.MADARA && p.getPersistentData().getBoolean(CLONE_FLAG) && p.tickCount % 10 == 0) {
            syncClones(p);
        }
    }

    /** Garante um Susanoo por clone de sombra do jogador (respeitando o limite do mod) e limpa quando o principal some. */
    public static void syncClones(ServerPlayer p) {
        CharacterSusanoo main = find(p, SusanooKind.MADARA);
        if (main == null) {
            clearClones(p);
            return;
        }
        List<CharacterSusanoo> existing = mine(p, true);
        Set<UUID> have = new HashSet<>();
        for (CharacterSusanoo c : existing) if (c.getAnchorUuid() != null) have.add(c.getAnchorUuid());
        int modMax = (int) Bridge.getDouble(p, "attribute_chakra", 0.0D) + 5; // limite de clones do Narutoto
        int cap = Math.min(MAX_CLONE_SUSANOOS, Math.max(1, modMax));
        List<Mob> clones = p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(128.0D),
                c -> isShadowClone(c) && c.isAlive() && c instanceof TamableAnimal t && t.isOwnedBy(p));
        for (Mob c : clones) {
            if (have.size() >= cap) break;
            if (have.contains(c.getUUID())) continue;
            if (spawn(p, SusanooKind.MADARA, 3, c.position(), c.getYRot(), true, c.getUUID()) != null) have.add(c.getUUID());
        }
    }

    public static void clearClones(ServerPlayer p) {
        p.getPersistentData().putBoolean(CLONE_FLAG, false);
        for (CharacterSusanoo c : mine(p, true)) c.vanish();
    }

    /** Detecta um jutsu de fogo (Katon) lancado perto de um Shisui: liga o combo do Tsukumo por alguns segundos. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        Entity en = e.getEntity();
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(en.getType());
        if (id == null || !id.getNamespace().equals("narutoto")) return;
        boolean fire = false;
        for (String w : FIRE_WORDS) if (id.getPath().contains(w)) { fire = true; break; }
        if (!fire) return;
        Player near = e.getLevel().getNearestPlayer(en.getX(), en.getY(), en.getZ(), 16.0D, false);
        if (near != null && SusanooKind.of(near) == SusanooKind.SHISUI) {
            near.getPersistentData().putLong(FIRE_TS, e.getLevel().getGameTime());
        }
    }

    public static boolean fireComboActive(Player p) {
        long ts = p.getPersistentData().getLong(FIRE_TS);
        return ts > 0 && p.level().getGameTime() - ts < 100; // ate 5 s depois do jutsu de fogo
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        MAT.remove(e.getEntity().getUUID());
        YataMirrorEntity m = YataMirrorEntity.ACTIVE.remove(e.getEntity().getUUID());
        if (m != null) m.discard();
    }
}
