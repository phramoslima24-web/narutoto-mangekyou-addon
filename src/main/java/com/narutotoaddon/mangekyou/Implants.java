package com.narutotoaddon.mangekyou;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

/** Implante guardado no jogador + aprendizado dos jutsus no menu de dojutsu do Narutoto. */
public final class Implants {
    private Implants() {}

    private static final String TAG = "mkaddon_implant";
    private static final String TAG_STAGE = "mkaddon_last_stage";

    public static Implant get(Entity e) {
        return Implant.byId(e.getPersistentData().getString(TAG));
    }

    /** Estagio do dojutsu no Narutoto: 1 = Sharingan, 2 = Mangekyou... (-1 se nao deu para ler). */
    public static double stage(Entity e) {
        return Bridge.getDouble(e, "if_sharigan", -1);
    }

    public static boolean mangekyouActive(Entity e) {
        double s = stage(e);
        return s < 0 || s >= 2;
    }

    static void msg(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + "." + key, args));
    }

    public static void sync(ServerPlayer p) {
        Implant i = get(p);
        Net.CH.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p),
                new SyncImplantPacket(p.getUUID(), i == null ? "" : i.id, i == null ? 0 : blindMask(p)));
    }

    private static void sound(ServerPlayer p, String modSound, SoundEvent fallback, float pitch) {
        SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("narutoto", modSound));
        p.level().playSound(null, p.blockPosition(), s != null ? s : fallback, SoundSource.PLAYERS, 1.0F, pitch);
    }

    // ---------------------------------------------------------------- Cegueira
    private static final String TAG_BLIND_MASK = "mkaddon_blind_mask";

    /** Olhos cegos: bit 1 = esquerdo, bit 2 = direito. */
    public static int blindMask(Entity e) {
        return e.getPersistentData().getInt(TAG_BLIND_MASK) & 3;
    }

    /** Cega o olho usado (Izanagi / Izanami). So o Mangekyou do Itachi devolve a visao. */
    public static void blind(ServerPlayer p, int eyeBit) {
        int mask = blindMask(p) | (eyeBit & 3);
        p.getPersistentData().putInt(TAG_BLIND_MASK, mask);
        msg(p, (eyeBit & 1) != 0 ? "blinded_left" : "blinded_right");
        if (mask == 3) msg(p, "both_blind_warn");
        sync(p);
    }

    /**
     * Implanta o olho. Se o jogador esta cego, so o Mangekyou do ITACHI e aceito: ele devolve a visao dos
     * dois olhos. Retorna false se foi recusado (o item nao deve ser consumido).
     */
    public static boolean apply(ServerPlayer p, Implant implant) {
        if (blindMask(p) != 0) {
            if (implant != Implant.ITACHI) {
                msg(p, "blind_wrong_eye");
                return false;
            }
            p.getPersistentData().remove(TAG_BLIND_MASK);
            p.removeEffect(MobEffects.BLINDNESS);
            if (get(p) == null) p.getPersistentData().putString(TAG, implant.id);
            sync(p);
            sound(p, "inject", SoundEvents.PLAYER_LEVELUP, 1.0F);
            msg(p, "sight_restored");
            return true;
        }
        p.getPersistentData().putString(TAG, implant.id);
        sync(p);
        sound(p, "inject", SoundEvents.PLAYER_LEVELUP, 1.0F);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".implant_done"));
        return true;
    }

    private static final String TAG_SEL = "mkaddon_sel";

    /** Jutsu escolhido pela tecla propria "trocar". */
    public static Ability ownSelected(ServerPlayer p) {
        Implant i = get(p);
        if (i == null) return null;
        return i.abilities[Math.floorMod(p.getPersistentData().getInt(TAG_SEL), i.abilities.length)];
    }

    public static void cycle(ServerPlayer p) {
        Implant i = get(p);
        if (i == null) {
            msg(p, "no_implant");
            return;
        }
        int next = (p.getPersistentData().getInt(TAG_SEL) + 1) % i.abilities.length;
        p.getPersistentData().putInt(TAG_SEL, next);
        p.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".own_selected",
                Component.translatable(i.abilities[next].jutsuKey())), true);
    }

    /** Tecla propria "usar": jutsu do addon selecionado no menu do Narutoto, senao o escolhido por "trocar". */
    public static void useOwn(ServerPlayer p) {
        Implant i = get(p);
        if (i == null) {
            msg(p, "no_implant");
            return;
        }
        Ability a = Ability.byJutsuKey(Bridge.getString(p, "jutsu_choosed_name", ""));
        if (a == null || !i.allows(a)) a = ownSelected(p);
        AbilityHandler.use(p, a);
    }

    /** Botao do menu de dojutsu: aprende o jutsu e coloca no slot escolhido. */
    public static void learn(ServerPlayer p, String abilityId) {
        Implant imp = get(p);
        Ability a = Ability.byId(abilityId);
        if (imp == null || a == null || !imp.allows(a)) {
            msg(p, "wrong_implant");
            return;
        }
        if (!mangekyouActive(p)) {
            msg(p, "need_mangekyou");
            return;
        }
        String key = a.jutsuKey();
        String name = a.jutsuName(); // o Narutoto grava o nome traduzido
        String learned = Bridge.getString(p, "learned_jutsu", "");
        if (learned.contains(name) || learned.contains(key)) {
            msg(p, "already_learned");
            return;
        }
        if (!Bridge.setString(p, "learned_jutsu", learned + name) || !Bridge.setString(p, "jutsu_key", name)) {
            msg(p, "bridge_error");
            return;
        }
        Bridge.callProcedure("Bind2Procedure", p);
        Bridge.sync(p);
        if (p.getServer() != null) {
            p.getServer().execute(() -> {
                Bridge.callProcedure("JutsuSwichNameDisplayProcedure", p);
                Bridge.sync(p);
            });
        }
        sound(p, "learn_jutsu", SoundEvents.PLAYER_LEVELUP, 1.2F);
        msg(p, "learned", Component.translatable(key));
    }

    /** Avisa quando o Mangekyou e ativado com um implante. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side.isClient()) return;
        if (!(e.player instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        double st = stage(p);
        double last = p.getPersistentData().getDouble(TAG_STAGE);
        Implant imp = get(p);
        if (imp != null && st >= 2 && last < 2) {
            p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".awakened",
                    Component.translatable(imp.nameKey())));
        }
        p.getPersistentData().putDouble(TAG_STAGE, st);
        // cego dos dois olhos: tela toda preta (cegueira escondida) e criaturas em volta brilham em branco
        if (blindMask(p) == 3) {
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, false));
            for (LivingEntity le : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24.0D),
                    x -> x != p && x.isAlive())) {
                le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false, false));
            }
        }
        // custo de chakra do jutsu do addon selecionado (o Narutoto so conhece os custos dos jutsus dele)
        Ability sel = Ability.byJutsuKey(Bridge.getString(p, "jutsu_choosed_name", ""));
        if (sel != null && Bridge.getDouble(p, "chakra_cost", -1) != sel.chakraCost) {
            Bridge.setDouble(p, "chakra_cost", sel.chakraCost);
            Bridge.sync(p);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof ServerPlayer target && e.getEntity() instanceof ServerPlayer viewer) {
            Implant i = get(target);
            if (i != null) {
                Net.CH.send(PacketDistributor.PLAYER.with(() -> viewer), new SyncImplantPacket(target.getUUID(), i.id, blindMask(target)));
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        var from = e.getOriginal().getPersistentData();
        var to = e.getEntity().getPersistentData();
        String old = from.getString(TAG);
        if (!old.isEmpty()) to.putString(TAG, old);
        // a cegueira sobrevive a morte: so o Mangekyou do Itachi devolve a visao
        if (from.getInt(TAG_BLIND_MASK) != 0) to.putInt(TAG_BLIND_MASK, from.getInt(TAG_BLIND_MASK));
    }
}
