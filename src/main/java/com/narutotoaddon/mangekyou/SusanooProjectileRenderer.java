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

/** Magatama / joia: sprite brilhante que gira de frente para a camera; agulhas: duas faces cruzadas alinhadas ao voo. */
public class SusanooProjectileRenderer extends EntityRenderer<SusanooProjectile> {
    public SusanooProjectileRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public ResourceLocation getTextureLocation(SusanooProjectile e) {
        String k = e.kind().id;
        return switch (e.variant()) {
            case SusanooProjectile.JEWEL -> tex("projectile/" + k + "_jewel.png");
            case SusanooProjectile.NEEDLE -> tex(e.isFire() ? "projectile/needle_fire.png" : "projectile/needle_shisui.png");
            default -> tex("projectile/" + k + "_magatama.png");
        };
    }

    private static ResourceLocation tex(String p) {
        return new ResourceLocation(MangekyouAddon.MODID, "textures/" + p);
    }

    @Override
    public boolean shouldRender(SusanooProjectile e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(SusanooProjectile e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(e)));
        float size = e.size();
        if (e.variant() == SusanooProjectile.NEEDLE) {
            ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, e.yRotO, e.getYRot()) - 90.0F));
            ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, e.xRotO, e.getXRot())));
            quad(ps, vc, size * 2.0F, size * 0.5F);
            ps.mulPose(Axis.XP.rotationDegrees(90.0F));
            quad(ps, vc, size * 2.0F, size * 0.5F);
        } else {
            ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
            float spin = (e.tickCount + pt) * (e.variant() == SusanooProjectile.MAGATAMA ? 22.0F : 34.0F);
            ps.mulPose(Axis.ZP.rotationDegrees(spin));
            quad(ps, vc, size, size);
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    private static void quad(PoseStack ps, VertexConsumer vc, float w, float h) {
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float hw = w * 0.5F, hh = h * 0.5F;
        v(vc, m, n, -hw, -hh, 0, 0, 1);
        v(vc, m, n, hw, -hh, 0, 1, 1);
        v(vc, m, n, hw, hh, 0, 1, 0);
        v(vc, m, n, -hw, hh, 0, 0, 0);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float vv) {
        vc.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
