package com.narutotoaddon.mangekyou;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MangekyouAddon.MODID);

    public static final RegistryObject<EntityType<IzanagiBody>> IZANAGI_BODY = ENTITIES.register("izanagi_body",
            () -> EntityType.Builder.<IzanagiBody>of(IzanagiBody::new, MobCategory.MISC)
                    .sized(0.8F, 0.35F).clientTrackingRange(8).updateInterval(3).fireImmune()
                    .build(new ResourceLocation(MangekyouAddon.MODID, "izanagi_body").toString()));

    private ModEntities() {}
}
