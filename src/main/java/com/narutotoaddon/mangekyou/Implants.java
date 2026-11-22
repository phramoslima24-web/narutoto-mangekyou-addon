package com.narutotoaddon.mangekyou;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
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

/**
 * Olhos implantados no jogador (acumulativos) + aprendizado dos jutsus. Tudo fica em dados persistentes do jogador
 * ({@code getPersistentData}), copiados na morte ({@link #onClone}) e enviados ao cliente ({@link #sync}).
 * <ul>
 *   <li>{@code mkaddon_implants}: lista (ids separados por virgula) de TODOS os olhos que o jogador possui;</li>
 *   <li>{@code mkaddon_implant}: olho ATIVO (aparencia e Susanoo); sempre um dos olhos da lista;</li>
 *   <li>{@code mkaddon_drawn} / {@code mkaddon_awakened}: sorteio do primeiro despertar (feito uma unica vez).</li>
 * </ul>
 */
public final class Implants {
    private Implants() {}

    /** Olho ativo (aparencia/Susanoo). Nome mantido para saves antigos (so existia um olho). */
    private static final String TAG = "mkaddon_implant";
    private static final String TAG_LIST = "mkaddon_implants";
    private static final String TAG_DRAWN = "mkaddon_drawn";
    private static final String TAG_AWAKENED = "mkaddon_awakened";
    private static final String TAG_BAR_V = "mkaddon_bar_v";
    private static final String TAG_STAGE = "mkaddon_last_stage";
    /** Versao da migracao da barra de jutsus (2.8.0: todos os jutsus dos olhos vao para a barra do Narutoto). */
    private static final int BAR_VERSION = 28;
    public static final int BAR_SLOTS = 20;

    /** Todos os olhos que o jogador possui (na ordem em que foram obtidos). */
    public static List<Implant> owned(Entity e) {
        CompoundTag d = e.getPersistentData();
        List<Implant> out = new ArrayList<>();
        for (String id : d.getString(TAG_LIST).split(",")) {
            Implant i = Implant.byId(id.trim());
            if (i != null && !out.contains(i)) out.add(i);
        }
        Implant legacy = Implant.byId(d.getString(TAG)); // saves antigos: so existia o olho ativo
        if (legacy != null && !out.contains(legacy)) out.add(legacy);
        return out;
    }

    /** Olho ativo (define o Susanoo e os olhos desenhados); null se nao tem nenhum. */
    public static Implant get(Entity e) {
        Implant a = Implant.byId(e.getPersistentData().getString(TAG));
        if (a != null) return a;
        List<Implant> all = owned(e);
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }

    public static boolean has(Entity e, Implant i) {
        return owned(e).contains(i);
    }

    /** Alguma das habilidades de algum olho possuido (sem duplicados). */
    public static boolean allows(Entity e, Ability a) {
        for (Implant i : owned(e)) if (i.allows(a)) return true;
        return false;
    }

    /** Todas as habilidades dos olhos possuidos, sem duplicados, na ordem em que os olhos foram obtidos. */
    public static List<Ability> abilities(Entity e) {
        Set<Ability> set = new LinkedHashSet<>();
        for (Implant i : owned(e)) for (Ability a : i.abilities) set.add(a);
        return new ArrayList<>(set);
    }

    public static String ownedCsv(Entity e) {
        StringBuilder sb = new StringBuilder();
        for (Implant i : owned(e)) {
            if (sb.length() > 0) sb.append(',');
            sb.append(i.id);
        }
        return sb.toString();
    }

    /** Grava o olho na lista (sem duplicar) e o torna o olho ativo. Retorna true se era novo. */
    private static boolean addOwned(Entity e, Implant i) {
        List<Implant> all = owned(e);
        boolean isNew = !all.contains(i);
        if (isNew) all.add(i);
        StringBuilder sb = new StringBuilder();
        for (Implant x : all) {
            if (sb.length() > 0) sb.append(',');
            sb.append(x.id);
        }
        e.getPersistentData().putString(TAG_LIST, sb.toString());
        e.getPersistentData().putString(TAG, i.id);
        return isNew;
    }

    /** Escolhe qual dos olhos possuidos fica ativo (aparencia e Susanoo). */
    public static void setActive(ServerPlayer p, String id) {
        Implant i = Implant.byId(id);
        if (i == null || !has(p, i)) return;
        p.getPersistentData().putString(TAG, i.id);
        sync(p);
        p.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".active_eye",
                Component.translatable(i.nameKey())), true);
    }

    /** Resultado do sorteio do primeiro despertar (null se ainda nao houve). */
    public static Implant awakened(Entity e) {
        return Implant.byId(e.getPersistentData().getString(TAG_AWAKENED));
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
        Net.CH.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), packetFor(p, i));
    }

    static SyncImplantPacket packetFor(ServerPlayer p, Implant active) {
        return new SyncImplantPacket(p.getUUID(), active == null ? "" : active.id,
                active == null ? 0 : blindMask(p), active == null ? "" : blindOwnerId(p), ownedCsv(p));
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
            if (cur != null && cur.canBlind()) p.getPersistentData().putString(TAG_BLIND_OWNER, cur.id);
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
     * Implanta o olho (ACUMULATIVO: os olhos e jutsus anteriores nunca sao apagados). Se o jogador esta cego, so e
     * aceito o Mangekyou do personagem que causou a cegueira (Madara cura Madara, Itachi cura Itachi, Shisui cura
     * Shisui): ele devolve a visao dos dois olhos. Olho que o jogador ja possui e recusado (sem cegueira).
     * Retorna false se foi recusado (o item nao deve ser consumido).
     */
    public static boolean apply(ServerPlayer p, Implant implant) {
        if (blindMask(p) != 0) {
            Implant owner = blindOwner(p);
            if (implant != owner) {
                msg(p, "blind_wrong_eye", Component.translatable(owner.nameKey()));
                return false;
            }
            clearBlind(p);
            boolean isNew = addOwned(p, implant);
            sync(p);
            sound(p, "inject", SoundEvents.PLAYER_LEVELUP, 1.0F);
            msg(p, "sight_restored");
            if (isNew) msg(p, "jutsus_available");
            return true;
        }
        if (has(p, implant)) {
            msg(p, "already_owned", Component.translatable(implant.nameKey()));
            return false;
        }
        addOwned(p, implant);
        sync(p);
        sound(p, "inject", SoundEvents.PLAYER_LEVELUP, 1.0F);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".implant_done"));
        msg(p, "jutsus_available");
        return true;
    }

    private static final String TAG_SEL = "mkaddon_sel";

    /** Jutsu escolhido pela tecla propria "trocar" (entre os jutsus de TODOS os olhos possuidos). */
    public static Ability ownSelected(ServerPlayer p) {
        List<Ability> all = abilities(p);
        if (all.isEmpty()) return null;
        return all.get(Math.floorMod(p.getPersistentData().getInt(TAG_SEL), all.size()));
    }

    public static void cycle(ServerPlayer p) {
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
        if (owned(p).isEmpty()) {
            msg(p, "no_implant");
            return;
        }
        Ability a = Ability.byJutsuKey(Bridge.getString(p, "jutsu_choosed_name", ""));
        if (a == null || !allows(p, a)) a = ownSelected(p);
        if (a != null) AbilityHandler.use(p, a);
    }

    // ------------------------------------------------- Barra de jutsus do Narutoto
    // O Narutoto tem 20 slots: o NOME TRADUZIDO do jutsu fica em getPersistentData()["jutsu<N>"] e e copiado para a
    // variavel jutsu_key<N> (Bind2Procedure); jutsu_choosed e o slot selecionado e jutsu_choosed_name o nome dele.
    // learned_jutsu guarda as chaves ("jutsu.xxx") dos jutsus aprendidos.

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty() || s.trim().equals("\"\"");
    }

    /** Slot (1..20) da barra do Narutoto que ja contem o jutsu, ou 0. */
    public static int barSlotOf(Entity e, Ability a) {
        for (int n = 1; n <= BAR_SLOTS; n++) {
            String v = Bridge.getString(e, "jutsu_key" + n, "");
            if (!blank(v) && a.isLearnedIn(v)) return n;
        }
        return 0;
    }

    private static int freeSlot(Entity e) {
        for (int n = 1; n <= BAR_SLOTS; n++) {
            if (blank(Bridge.getString(e, "jutsu_key" + n, "")) && blank(e.getPersistentData().getString("jutsu" + n))) {
                return n;
            }
        }
        return 0;
    }

    private static void putInSlot(ServerPlayer p, int slot, Ability a) {
        String name = a.jutsuName();
        p.getPersistentData().putString("jutsu" + slot, name);
        Bridge.setString(p, "jutsu_key" + slot, name);
    }

    private static void markLearned(ServerPlayer p, Ability a) {
        String learned = Bridge.getString(p, "learned_jutsu", "");
        if (!a.isLearnedIn(learned)) Bridge.setString(p, "learned_jutsu", learned + a.jutsuKey());
    }

    private static void refreshBar(ServerPlayer p) {
        Bridge.callProcedure("JutsuSwichNameDisplayProcedure", p);
        Bridge.sync(p);
        if (p.getServer() != null) {
            p.getServer().execute(() -> {
                Bridge.callProcedure("JutsuSwichNameDisplayProcedure", p);
                Bridge.sync(p);
            });
        }
    }

    /**
     * Coloca os jutsus na PROPRIA barra do Narutoto, em slots livres (nunca sobrescreve um slot ocupado e nao
     * duplica quem ja esta na barra). Retorna quantos foram adicionados.
     */
    public static int addToBar(ServerPlayer p, Collection<Ability> list) {
        int added = 0;
        boolean full = false;
        for (Ability a : list) {
            if (barSlotOf(p, a) > 0) {
                markLearned(p, a);
                continue;
            }
            int slot = freeSlot(p);
            if (slot == 0) {
                full = true;
                break;
            }
            putInSlot(p, slot, a);
            markLearned(p, a);
            added++;
        }
        if (added > 0) {
            Bridge.sync(p);
            refreshBar(p);
            sound(p, "learn_jutsu", SoundEvents.PLAYER_LEVELUP, 1.2F);
        }
        if (full) msg(p, "bar_full");
        return added;
    }

    /** Botao do menu de dojutsu: coloca o jutsu no slot ESCOLHIDO da barra do Narutoto (como os jutsus do mod). */
    public static void learn(ServerPlayer p, String abilityId) {
        Ability a = Ability.byId(abilityId);
        if (a == null || !allows(p, a)) {
            msg(p, "wrong_implant");
            return;
        }
        if (!mangekyouActive(p)) {
            msg(p, "need_mangekyou");
            return;
        }
        // Como no jogo: o jutsu vai para o slot SELECIONADO na barra (jutsu_choosed); um jutsu so fica em um lugar
        int slot = (int) Math.round(Bridge.getDouble(p, "jutsu_choosed", 0));
        if (slot < 1 || slot > BAR_SLOTS) slot = freeSlot(p);
        if (slot < 1) {
            msg(p, "bar_full");
            return;
        }
        int old = barSlotOf(p, a);
        if (old == slot) {
            msg(p, "already_learned");
            return;
        }
        if (old > 0) {
            p.getPersistentData().putString("jutsu" + old, "");
            Bridge.setString(p, "jutsu_key" + old, "");
        }
        putInSlot(p, slot, a);
        markLearned(p, a);
        Bridge.sync(p);
        refreshBar(p);
        sound(p, "learn_jutsu", SoundEvents.PLAYER_LEVELUP, 1.2F);
        msg(p, "learned", Component.translatable(a.jutsuKey()));
    }

    // ------------------------------------------------------- Despertar aleatorio
    private static boolean hasAdvancement(ServerPlayer p, String id) {
        if (p.getServer() == null) return false;
        var adv = p.getServer().getAdvancements().getAdvancement(new ResourceLocation(id));
        return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
    }

    /**
     * Primeiro despertar do Mangekyou (conquista do Narutoto ou estagio >= 2): sorteia UMA vez entre Obito, Sasuke,
     * Madara, Itachi e Shisui. O resultado fica salvo para sempre (nao sorteia de novo ao morrer, sair ou reiniciar).
     * Saves antigos que ja tinham implantes nao sofrem sorteio.
     */
    private static void checkAwakening(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (d.getInt(TAG_DRAWN) != 0) return;
        if (!owned(p).isEmpty()) {
            d.putInt(TAG_DRAWN, 1);
            return;
        }
        if (!(hasAdvancement(p, "narutoto:learn_mangekyo_sasuke") || stage(p) >= 2)) return;
        Implant pick = Implant.LOTTERY[p.getRandom().nextInt(Implant.LOTTERY.length)];
        d.putInt(TAG_DRAWN, 1);
        d.putString(TAG_AWAKENED, pick.id);
        addOwned(p, pick);
        // Todos seguem o caminho do Sasuke do Narutoto (Mangekyou -> Eterno -> Rinnegan): o dono "sasuke" e o unico que
        // o original deixa evoluir. O Kamui do Obito usa o dono "obito" so durante a chamada do procedimento original.
        Bridge.setString(p, "mangekyo_owner", "sasuke");
        Bridge.sync(p);
        sync(p);
        sound(p, "sharingan_open", SoundEvents.PLAYER_LEVELUP, 0.8F);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".awakening_draw",
                Component.translatable(pick.nameKey())));
        msg(p, "jutsus_available");
    }

    /** Sorteio do despertar, aviso do Mangekyou ativado e cegueira total. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side.isClient()) return;
        if (!(e.player instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        checkAwakening(p);
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
        // (os jutsus ORIGINAIS do Narutoto, como o Kamui, ja tem o custo calculado por ele)
        if (sel != null && !sel.isNative() && Bridge.getDouble(p, "chakra_cost", -1) != sel.chakraCost) {
            Bridge.setDouble(p, "chakra_cost", sel.chakraCost);
            Bridge.sync(p);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof ServerPlayer target && e.getEntity() instanceof ServerPlayer viewer) {
            Implant i = get(target);
            if (i != null) {
                Net.CH.send(PacketDistributor.PLAYER.with(() -> viewer), packetFor(target, i));
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

    /** Dados do addon que sobrevivem a morte (a lista de olhos, o sorteio, a cegueira, o modo do Obito...). */
    private static final String[] KEEP_ON_DEATH = {TAG, TAG_LIST, TAG_AWAKENED, "mkaddon_blind_owner", "mkaddon_sel",
            ObitoHandler.TAG_MODE};
    private static final String[] KEEP_INT_ON_DEATH = {TAG_DRAWN, TAG_BAR_V, TAG_BLIND_MASK};

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        CompoundTag from = e.getOriginal().getPersistentData();
        CompoundTag to = e.getEntity().getPersistentData();
        for (String k : KEEP_ON_DEATH) {
            if (!from.contains(k)) continue;
            if (k.equals(ObitoHandler.TAG_MODE)) to.putInt(k, from.getInt(k));
            else if (k.equals("mkaddon_sel")) to.putInt(k, from.getInt(k));
            else if (!from.getString(k).isEmpty()) to.putString(k, from.getString(k));
        }
        for (String k : KEEP_INT_ON_DEATH) {
            if (from.contains(k) && from.getInt(k) != 0) to.putInt(k, from.getInt(k));
        }
    }
}
