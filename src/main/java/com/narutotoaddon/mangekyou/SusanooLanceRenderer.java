package com.narutotoaddon.mangekyou;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Broca/lanca do Susanoo do Shisui (modelo proprio com animacao de giro e estocada). */
public class SusanooLanceRenderer extends GeoEntityRenderer<SusanooLance> {
    public SusanooLanceRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<SusanooLance>() {
            @Override
            public ResourceLocation getModelResource(SusanooLance e) {
                return new ResourceLocation(MangekyouAddon.MODID, "geo/susanoo_lance.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(SusanooLance e) {
                return new ResourceLocation(MangekyouAddon.MODID, "textures/entity/lance_shisui.png");
            }

            @Override
            public ResourceLocation getAnimationResource(SusanooLance e) {
                return new ResourceLocation(MangekyouAddon.MODID, "animations/susanoo_lance.animation.json");
            }
        });
        this.shadowRadius = 0.0F;
    }

    @Override
    public RenderType getRenderType(SusanooLance e, ResourceLocation tex, MultiBufferSource buf, float pt) {
        return RenderType.entityTranslucentEmissive(tex);
    }

    @Override
    public void preRender(PoseStack ps, SusanooLance e, BakedGeoModel model, MultiBufferSource buf, VertexConsumer vc,
                          boolean isReRender, float pt, int light, int overlay, float r, float g, float b, float a) {
        float s = SusanooGeometry.lanceScale(e.stage());
        this.scaleHeight = s;
        this.scaleWidth = s;
        ps.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        super.preRender(ps, e, model, buf, vc, isReRender, pt, light, overlay, r, g, b, a);
    }
}
