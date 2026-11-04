package com.narutotoaddon.mangekyou;

import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Desenha o corpo do Izanagi: o modelo do jogador (com a skin do dono) deitado de costas. */
public class IzanagiBodyRenderer extends EntityRenderer<IzanagiBody> {
    private final PlayerModel<LivingEntity> model;

    public IzanagiBodyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
    }

    @Override
    public ResourceLocation getTextureLocation(IzanagiBody body) {
        UUID id = body.getOwner().orElse(null);
        if (id == null) return DefaultPlayerSkin.getDefaultSkin();
        var conn = Minecraft.getInstance().getConnection();
        PlayerInfo info = conn == null ? null : conn.getPlayerInfo(id);
        return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
    }

    @Override
    public void render(IzanagiBody body, float yaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        ps.translate(0.0D, 0.2D, -0.9D);            // encosta as costas no chao e centraliza
        ps.mulPose(Axis.XP.rotationDegrees(90.0F)); // deita de costas, rosto para cima
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.translate(0.0D, -1.501D, 0.0D);
        model.young = false;
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(body))),
                light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        ps.popPose();
        super.render(body, yaw, partialTick, ps, buffers, light);
    }
}
