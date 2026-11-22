package com.narutotoaddon.mangekyou;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Modelo/animacao/textura proprios de cada personagem e estagio (assets/narutoto_mangekyou/geo e textures/entity). */
public class SusanooModel extends GeoModel<CharacterSusanoo> {
    @Override
    public ResourceLocation getModelResource(CharacterSusanoo e) { return e.kind().geo(e.getStage()); }

    @Override
    public ResourceLocation getTextureResource(CharacterSusanoo e) { return e.kind().texture(e.getStage()); }

    @Override
    public ResourceLocation getAnimationResource(CharacterSusanoo e) { return e.kind().animation(e.getStage()); }
}
