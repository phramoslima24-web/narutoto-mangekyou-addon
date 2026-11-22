package com.narutotoaddon.mangekyou;

import java.util.ArrayList;
import java.util.List;

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

    /** Lista (CSV) de TODOS os olhos implantados. Nunca e apagada por um novo implante. */
    private static final String TAG_EYES = "mkaddon_eyes";
    /** Marca que o primeiro despertar (sorteio) ja aconteceu. Nunca volta a false. */
    private static final String TAG_AWAKENED = "mkaddon_awakened";

    /** Olho ATIVO: define a aparencia, o Susanoo do personagem e a cegueira. */
    public static Implant get(Entity e) {
        return Implant.byId(e.getPersistentData().getString(TAG));
    }

    /** Todos os olhos implantados (migra saves antigos que so tinham o olho ativo). */
    public static List<Implant> eyes(Entity e) {
        var d = e.getPersistentData();
        List<Implant> list = EyeSet.parse(d.getString(TAG_EYES));
        Implant act = get(e);
        if (act != null && EyeSet.add(list, act)) d.putString(TAG_EYES, EyeSet.write(list)); // save antigo
        return list;
    }

    public static boolean has(Entity e, Implant i) {
        return eyes(e).contains(i);
    }

    /** O jogador tem algum olho implantado que concede este jutsu? (habilidades de todos os olhos valem juntas) */
    public static boolean owns(Entity e, Ability a) {
        return EyeSet.allows(eyes(e), a);
    }

    public static boolean awakened(Entity e) {
        return e.getPersistentData().getBoolean(TAG_AWAKENED) || !eyes(e).isEmpty();
    }

    /** Jutsus de todos os olhos, sem duplicados. */
    public static List<Ability> abilities(Entity e) {
        return EyeSet.abilities(eyes(e));
    }

    private static void store(ServerPlayer p, List<Implant> eyes, Implant active) {
        var d = p.getPersistentData();
        d.putString(TAG_EYES, EyeSet.write(eyes));
        d.putString(TAG, active.id);
        d.putBoolean(TAG_AWAKENED, true);
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
                new SyncImplantPacket(p.getUUID(), i == null ? "" : i.id, i == null ? 0 : blindMask(p),
                        i == null ? "" : blindOwnerId(p)));
    }

    private static void sound(ServerPlayer p, String modSound, SoundEvent fallback, float pitch) {
        SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("narutoto", modSound));
        p.level().playSound(null, p.blockPosition(), s != null ? s : fallback, SoundSource.PLAYERS, 1.0F, pitch);
    }

    // ---------------------------------------------------------------- Cegueira
    private static final String TAG_BLIND_MASK = "mkaddon_blind_mask";
    /** Id do implante (Madara, Itachi ou Shisui) cujo Izanagi/Izanami causou a cegueira atual. */
    private static final String TAG_BLIND_OWNER = "mkaddon_blind_owner";

    /** Olhos cegos: bit 1 = esquerdo, bit 2 = direito. */
    public static int blindMask(Entity e) {
        return e.getPersistentData().getInt(TAG_BLIND_MASK) & 3;
    }

    /**
     * Quem "possui" a cegueira atual: so o Mangekyou desse personagem devolve a visao. Em saves antigos (sem o
     * dono gravado) vale o implante atual do jogador, ou o Itachi (unico que curava antes).
     */
    public static Implant blindOwner(Entity e) {
        if (blindMask(e) == 0) return null;
        Implant o = Implant.byId(e.getPersistentData().getString(TAG_BLIND_OWNER));
        if (o != null && o.canBlind()) return o;
        Implant cur = get(e);
        return cur != null && cur.canBlind() ? cur : Implant.ITACHI;
    }

    /** Id do dono da cegueira ("" se nao esta cego). Usado na sincronizacao com o cliente. */
    public static String blindOwnerId(Entity e) {
        Implant o = blindOwner(e);
        return o == null ? "" : o.id;
    }

    /** Cega o olho usado (Izanagi / Izanami). So o Mangekyou do MESMO personagem devolve a visao. */
    public static void blind(ServerPlayer p, int eyeBit) {
        int mask = blindMask(p) | (eyeBit & 3);
        if (blindMask(p) == 0) {
            // primeira cegueira: grava de qual Mangekyou ela veio (enquanto cego nao da para trocar de implante)
            Implant cur = get(p);
            if (cur == null || !cur.canBlind()) {
                cur = null;
                for (Implant i : eyes(p)) if (i.canBlind()) { cur = i; break; }
            }
            if (cur != null) p.getPersistentData().putString(TAG_BLIND_OWNER, cur.id);
            else p.getPersistentData().remove(TAG_BLIND_OWNER);
        }
        p.getPersistentData().putInt(TAG_BLIND_MASK, mask);
        Implant owner = blindOwner(p);
        Component who = Component.translatable(owner.nameKey());
        msg(p, (eyeBit & 1) != 0 ? "blinded_left" : "blinded_right", who);
        if (mask == 3) msg(p, "both_blind_warn");
        sync(p);
    }

    private static void clearBlind(ServerPlayer p) {
        p.getPersistentData().remove(TAG_BLIND_MASK);
        p.getPersistentData().remove(TAG_BLIND_OWNER);
        p.removeEffect(MobEffects.BLINDNESS);
    }

    /**
     * Implanta o olho. Se o jogador esta cego, so e aceito o Mangekyou do personagem que causou a cegueira
     * (Madara cura Madara, Itachi cura Itachi, Shisui cura Shisui): ele devolve a visao dos dois olhos.
     * Retorna false se foi recusado (o item nao deve ser consumido).
     */
    public static boolean apply(ServerPlayer p, Implant implant) {
        List<Implant> eyes = new ArrayList<>(eyes(p));
        if (blindMask(p) != 0) {
            Implant owner = blindOwner(p);
            if (implant != owner) {
                msg(p, "blind_wrong_eye", Component.translatable(owner.nameKey()));
                return false;
            }
            clearBlind(p);
            EyeSet.add(eyes, implant); // cura: o olho entra na lista se ainda nao estava
            store(p, eyes, implant);
            sync(p);
            sound(p, "inject", SoundEvents.PLAYER_LEVELUP, 1.0F);
            msg(p, "sight_restored");
            grantBar(p);
            return true;
        }
        if (!EyeSet.add(eyes, implant)) {
            msg(p, "eye_already_owned", Component.translatable(implant.nameKey())); // sem duplicar; item nao e consumido
            return false;
        }
        store(p, eyes, implant); // o olho novo vira o ativo; os anteriores e seus jutsus continuam
        sync(p);
        sound(p, "inject", SoundEvents.PLAYER_LEVELUP, 1.0F);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".implant_done"));
        grantBar(p);
        return true;
    }

    /** Primeiro despertar do Mangekyou: sorteia Sasuke, Madara, Itachi ou Shisui (uma unica vez, salvo para sempre). */
    public static void awakenLottery(ServerPlayer p) {
        if (awakened(p)) return;
        Implant drawn = EyeSet.draw(new java.util.Random(p.getRandom().nextLong()));
        List<Implant> eyes = new ArrayList<>();
        EyeSet.add(eyes, drawn);
        store(p, eyes, drawn);
        sync(p);
        sound(p, "inject", SoundEvents.PLAYER_LEVELUP, 1.0F);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".awakened_random",
                Component.translatable(drawn.nameKey())));
        grantBar(p);
    }

    /** Troca o olho ativo (aparencia/Susanoo) entre os ja implantados. Nao mexe em jutsus. */
    public static void cycleActive(ServerPlayer p) {
        List<Implant> eyes = eyes(p);
        if (eyes.size() < 2) {
            msg(p, "single_eye");
            return;
        }
        Implant cur = get(p);
        Implant next = eyes.get((Math.max(0, eyes.indexOf(cur)) + 1) % eyes.size());
        store(p, eyes, next);
        sync(p);
        p.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".active_eye",
                Component.translatable(next.nameKey())), true);
    }

    // ------------------------------------------------- Barra original do Narutoto
    private static final int BAR_SLOTS = 20; // jutsu_key1..jutsu_key20 (PlayerVariables do Narutoto 0.4.1)

    /**
     * Coloca os jutsus de TODOS os olhos na barra original do Narutoto: grava o nome no learned_jutsu e no
     * primeiro slot livre (jutsu_keyN). So adiciona o que ainda nao foi aprendido; nunca remove nada.
     * Se a barra estiver cheia, o jutsu fica aprendido e pode ser posto num slot pelo menu de dojutsu.
     */
    public static void grantBar(ServerPlayer p) {
        if (!mangekyouActive(p)) return;
        String learned = Bridge.getString(p, "learned_jutsu", null);
        if (learned == null) return; // variaveis do Narutoto indisponiveis
        List<Ability> missing = new ArrayList<>();
        for (Ability a : abilities(p)) if (!a.isLearnedIn(learned)) missing.add(a);
        if (missing.isEmpty()) return;
        boolean changed = false;
        for (Ability a : missing) {
            String name = a.jutsuName();
            if (!Bridge.setString(p, "learned_jutsu", learned + name)) break;
            learned = learned + name;
            for (int n = 1; n <= BAR_SLOTS; n++) {
                String f = "jutsu_key" + n;
                if (Bridge.getString(p, f, "x").isEmpty() && Bridge.setString(p, f, name)) break;
            }
            changed = true;
        }
        if (!changed) return;
        Bridge.sync(p);
        if (p.getServer() != null) {
            p.getServer().execute(() -> {
                Bridge.callProcedure("JutsuSwichNameDisplayProcedure", p);
                Bridge.sync(p);
            });
        }
        sound(p, "learn_jutsu", SoundEvents.PLAYER_LEVELUP, 1.2F);
    }

    private static final String TAG_SEL = "mkaddon_sel";

    /** Jutsu escolhido pela tecla propria "trocar". */
    public static Ability ownSelected(ServerPlayer p) {
        List<Ability> all = abilities(p);
        if (all.isEmpty()) return null;
        return all.get(Math.floorMod(p.getPersistentData().getInt(TAG_SEL), all.size()));
    }

    public static void cycle(ServerPlayer p) {
        if (p.isShiftKeyDown()) { // Shift + tecla "trocar": troca o olho ativo
            cycleActive(p);
            return;
        }
        List<Ability> all = abilities(p);
        if (all.isEmpty()) {
            msg(p, "no_implant");
            return;
        }
        int next = (p.getPersistentData().getInt(TAG_SEL) + 1) % all.size();
        p.getPersistentData().putInt(TAG_SEL, next);
        p.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".own_selected",
                Component.translatable(all.get(next).jutsuKey())), true);
    }

    /** Tecla propria "usar": jutsu do addon selecionado no menu do Narutoto, senao o escolhido por "trocar". */
    public static void useOwn(ServerPlayer p) {
        if (eyes(p).isEmpty()) {
            msg(p, "no_implant");
            return;
        }
        Ability a = Ability.byJutsuKey(Bridge.getString(p, "jutsu_choosed_name", ""));
        if (a == null || !owns(p, a)) a = ownSelected(p);
        AbilityHandler.use(p, a);
    }

    /** Botao do menu de dojutsu: aprende o jutsu e coloca no slot escolhido. */
    public static void learn(ServerPlayer p, String abilityId) {
        Ability a = Ability.byId(abilityId);
        if (a == null || !owns(p, a)) {
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
        if (a.isLearnedIn(learned)) {
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
        if (st >= 2 && !awakened(p)) awakenLottery(p); // 1o despertar: sorteio unico
        Implant imp = get(p);
        if (st >= 2 && !eyes(p).isEmpty() && p.tickCount % 40 == 0) grantBar(p); // jutsus de todos os olhos na barra
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
                Net.CH.send(PacketDistributor.PLAYER.with(() -> viewer), new SyncImplantPacket(target.getUUID(), i.id, blindMask(target), blindOwnerId(target)));
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
        if (!from.getString(TAG_EYES).isEmpty()) to.putString(TAG_EYES, from.getString(TAG_EYES));
        if (from.getBoolean(TAG_AWAKENED)) to.putBoolean(TAG_AWAKENED, true);
        if (from.contains(TAG_SEL)) to.putInt(TAG_SEL, from.getInt(TAG_SEL));
        // a cegueira sobrevive a morte: so o Mangekyou do mesmo personagem devolve a visao
        if (from.getInt(TAG_BLIND_MASK) != 0) {
            to.putInt(TAG_BLIND_MASK, from.getInt(TAG_BLIND_MASK));
            String owner = from.getString(TAG_BLIND_OWNER);
            if (!owner.isEmpty()) to.putString(TAG_BLIND_OWNER, owner);
        }
    }
}
