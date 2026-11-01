package com.narutotoaddon.mangekyou;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

    private static void msg(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + "." + key, args), true);
    }

    public static void sync(ServerPlayer p) {
        Implant i = get(p);
        Net.CH.send(PacketDistributor.PLAYER.with(() -> p), new SyncImplantPacket(i == null ? "" : i.id));
    }

    private static void sound(ServerPlayer p, String modSound, SoundEvent fallback, float pitch) {
        SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("narutoto", modSound));
        p.level().playSound(null, p.blockPosition(), s != null ? s : fallback, SoundSource.PLAYERS, 1.0F, pitch);
    }

    public static void apply(ServerPlayer p, Implant implant) {
        p.getPersistentData().putString(TAG, implant.id);
        sync(p);
        sound(p, "inject", SoundEvents.PLAYER_LEVELUP, 1.0F);
        // mensagem no chat
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".implant_done"));
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
        String learned = Bridge.getString(p, "learned_jutsu", "");
        if (learned.contains(key)) {
            msg(p, "already_learned");
            return;
        }
        if (!Bridge.setString(p, "learned_jutsu", learned + key) || !Bridge.setString(p, "jutsu_key", key)) {
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
        String old = e.getOriginal().getPersistentData().getString(TAG);
        if (!old.isEmpty()) e.getEntity().getPersistentData().putString(TAG, old);
    }
}
