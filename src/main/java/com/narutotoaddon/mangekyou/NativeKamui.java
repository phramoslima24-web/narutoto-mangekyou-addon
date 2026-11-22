package com.narutotoaddon.mangekyou;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Kamui do Obito = o Kamui ORIGINAL do Narutoto (Intangibilidade, Transferencia Total e Parcial, dimensao
 * {@code narutoto:kamui_dimension}). Nada foi duplicado: os jutsus tem as mesmas chaves do mod e quem executa sao os
 * procedimentos dele. O original so aceita o Mangekyou comum (if_sharigan == 2) e o dono "obito"; aqui os mesmos
 * procedimentos sao chamados com esse estado emprestado por um instante quando o jogador esta no Mangekyou Eterno (3)
 * ou no Rinnegan (4), para o Kamui nao se perder na evolucao.
 */
public final class NativeKamui {
    private NativeKamui() {}

    private static String procedure(Ability a) {
        return switch (a) {
            case KAMUI_INTANGIBLE -> "KamuiIntangibleJutsuProcedure";
            case KAMUI_TRANSFER_WHOLE -> "KamuiTransferJutsuProcedure";
            case KAMUI_TRANSFER_PART -> "KamuiTransferJutsu2Procedure";
            default -> null;
        };
    }

    /** Chamado quando a tecla de jutsu e solta com um jutsu nativo selecionado. */
    public static void use(ServerPlayer p, Ability a) {
        String proc = procedure(a);
        if (proc == null) return;
        double stage = Implants.stage(p);
        // Estagio 2 (ou criativo): o original ja trata sozinho a mesma soltura de tecla; nao duplicar.
        if (stage == 2.0D || p.getAbilities().instabuild) return;
        if (stage < 3.0D) return;
        Bridge.withState(p, 2.0D, "obito", () -> Bridge.callProcedureWorld(proc, p));
    }

    /**
     * Intangibilidade solta + botao direito numa entidade = suga para a dimensao Kamui. O original exige dono "obito" e
     * estagio 2; aqui vale tambem no Eterno/Rinnegan e para quem tem o olho do Obito sem ser o dono original.
     */
    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getHand() != p.getUsedItemHand()) return;
        if (!Implants.has(p, Implant.OBITO)) return;
        Ability sel = Ability.byJutsuKey(Bridge.getString(p, "jutsu_choosed_name", ""));
        if (sel != Ability.KAMUI_INTANGIBLE) return;
        double stage = Implants.stage(p);
        String owner = Bridge.getString(p, "mangekyo_owner", "");
        if (stage < 2.0D) return;
        if (stage == 2.0D && owner.equals("obito")) return; // o original ja trata
        Bridge.withState(p, 2.0D, "obito", () -> Bridge.callProcedureWorld("KamuiRightClickProcedure", e.getTarget(), p));
    }
}
