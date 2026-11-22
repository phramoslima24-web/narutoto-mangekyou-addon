package com.narutotoaddon.mangekyou;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Escudo dourado octogonal (Espelho de Yata), de pe na frente do Susanoo, com pulso e clarao ao ser atingido. */
public class YataMirrorRenderer extends EntityRenderer<YataMirrorEntity> {
    private static final ResourceLocation TEX = new ResourceLocation(MangekyouAddon.MODID, "textures/entity/yata_mirror.png");

    public YataMirrorRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public ResourceLocation getTextureLocation(YataMirrorEntity e) { return TEX; }

    @Override
    public boolean shouldRender(YataMirrorEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(YataMirrorEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float rad = SusanooGeometry.mirrorRadius(e.stage());
        float open = Math.min(1.0F, (e.tickCount + pt) / 8.0F); // animacao de ativacao
        float pulse = 1.0F + 0.03F * Mth.sin((e.tickCount + pt) * 0.4F) + (e.hitAge() < 4 ? 0.06F : 0.0F);
        float s = rad * 1.9F * open * pulse;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.lerp(pt, e.yRotO, e.getYRot())));
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(TEX));
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float h = s * 0.5F;
        int a = e.hitAge() < 4 ? 255 : 215;
        vtx(vc, m, n, -h, -h, 0, 0, 1, a);
        vtx(vc, m, n, h, -h, 0, 1, 1, a);
        vtx(vc, m, n, h, h, 0, 1, 0, a);
        vtx(vc, m, n, -h, h, 0, 0, 0, a);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    private static void vtx(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, int a) {
        vc.vertex(m, x, y, z).color(255, 255, 255, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
