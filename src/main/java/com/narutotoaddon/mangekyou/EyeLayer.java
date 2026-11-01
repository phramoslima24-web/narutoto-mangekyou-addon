package com.narutotoaddon.mangekyou;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Desenha os olhos do implante por cima do Mangekyou do Narutoto.
 * Usa exatamente a mesma geometria do mod (quadro de 2.1 x 2.1 preso a cabeca), so que mostrando apenas
 * o recorte dos olhos (154x95 da textura 1500x1500) numa textura ampliada 4x.
 */
public class EyeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float HALF = 2.1F;
    // recorte da regiao dos olhos na textura original 1500x1500 do Narutoto
    private static final float U0 = 673F / 1500F, U1 = 827F / 1500F, V0 = 725F / 1500F, V1 = 820F / 1500F;

    public EyeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (player.isInvisible() && (mc.player == null || player.isInvisibleTo(mc.player))) return;
        Implant implant = ClientState.get(player.getUUID());
        if (implant == null) return;
        // o mod so desenha o Mangekyou no estagio 2
        if (Bridge.getDouble(player, "if_sharigan", 0) != 2.0D) return;

        double pos = Bridge.getDouble(player, "sharingan_position", 2);
        float yOff = pos == 0 ? -0.3F : (pos == 1 ? -0.25F : -0.2F);

        ResourceLocation tex = new ResourceLocation(MangekyouAddon.MODID,
                "textures/entity/mangekyou_eyes_" + implant.id + ".png");

        ps.pushPose();
        getParentModel().head.translateAndRotate(ps);
        ps.translate(0.0D, 0.0D, -0.2705D); // 0.0005 na frente do quadro do mod
        ps.translate(0.0D, yOff, 0.0D);

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();

        float xl = -HALF + U0 * 2 * HALF, xr = -HALF + U1 * 2 * HALF;
        float yt = -HALF + V0 * 2 * HALF, yb = -HALF + V1 * 2 * HALF;
        vertex(vc, m, n, xl, yb, 0F, 1F, light);
        vertex(vc, m, n, xr, yb, 1F, 1F, light);
        vertex(vc, m, n, xr, yt, 1F, 0F, light);
        vertex(vc, m, n, xl, yt, 0F, 0F, light);
        ps.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, int light) {
        vc.vertex(m, x, y, 0.0F).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, -1.0F).endVertex();
    }
}
