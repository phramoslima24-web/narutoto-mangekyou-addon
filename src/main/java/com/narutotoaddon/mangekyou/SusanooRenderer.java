package com.narutotoaddon.mangekyou;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class SusanooRenderer extends GeoEntityRenderer<CharacterSusanoo> {
    public SusanooRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SusanooModel());
        this.shadowRadius = 0.0F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(CharacterSusanoo e, ResourceLocation tex, MultiBufferSource buf, float pt) {
        return RenderType.entityTranslucent(this.getTextureLocation(e));
    }

    @Override
    public void render(CharacterSusanoo e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        boolean mine = me != null && e.getOwnerUUID() != null && e.getOwnerUUID().equals(me.getUUID());
        boolean firstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON && mc.getCameraEntity() == me;
        if (mine && firstPerson) return; // a camera fica dentro do Susanoo, como no original
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public void preRender(PoseStack ps, CharacterSusanoo e, BakedGeoModel model, MultiBufferSource buf,
                          VertexConsumer vc, boolean isReRender, float pt, int light, int overlay,
                          float r, float g, float b, float a) {
        float scale = SusanooGeometry.modelScale(e.getStage());
        // invocacao: cresce em ~12 ticks; desaparecimento: encolhe e some
        float grow = Math.min(1.0F, (e.tickCount + pt) / 12.0F);
        float fade = 1.0F - e.getFade() / (float) CharacterSusanoo.FADE_MAX;
        float f = Math.max(0.05F, 0.35F + 0.65F * grow) * Math.max(0.05F, fade);
        this.scaleHeight = scale * f;
        this.scaleWidth = scale * f;
        super.preRender(ps, e, model, buf, vc, isReRender, pt, light, overlay, r, g, b, a);
    }

    @Override
    protected float getDeathMaxRotation(CharacterSusanoo e) { return 0.0F; }
}
