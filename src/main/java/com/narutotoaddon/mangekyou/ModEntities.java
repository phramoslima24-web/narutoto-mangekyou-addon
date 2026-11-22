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

    private static RegistryObject<EntityType<CharacterSusanoo>> susanoo(String id) {
        return ENTITIES.register(id, () -> EntityType.Builder.<CharacterSusanoo>of(CharacterSusanoo::new, MobCategory.MISC)
                .sized(5.0F, 45.0F).clientTrackingRange(16).updateInterval(1).fireImmune()
                .build(new ResourceLocation(MangekyouAddon.MODID, id).toString()));
    }

    /** Tres Susanoos independentes, um por Mangekyou (modelos e texturas proprios). */
    public static final RegistryObject<EntityType<CharacterSusanoo>> MADARA_SUSANOO = susanoo("madara_susanoo");
    public static final RegistryObject<EntityType<CharacterSusanoo>> ITACHI_SUSANOO = susanoo("itachi_susanoo");
    public static final RegistryObject<EntityType<CharacterSusanoo>> SHISUI_SUSANOO = susanoo("shisui_susanoo");

    public static final RegistryObject<EntityType<SusanooProjectile>> SUSANOO_PROJECTILE = ENTITIES.register("susanoo_projectile",
            () -> EntityType.Builder.<SusanooProjectile>of(SusanooProjectile::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F).clientTrackingRange(16).updateInterval(1).setShouldReceiveVelocityUpdates(true).fireImmune()
                    .build(new ResourceLocation(MangekyouAddon.MODID, "susanoo_projectile").toString()));

    public static final RegistryObject<EntityType<YataMirrorEntity>> YATA_MIRROR = ENTITIES.register("yata_mirror",
            () -> EntityType.Builder.<YataMirrorEntity>of(YataMirrorEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).fireImmune()
                    .build(new ResourceLocation(MangekyouAddon.MODID, "yata_mirror").toString()));

    public static final RegistryObject<EntityType<SusanooLance>> SUSANOO_LANCE = ENTITIES.register("susanoo_lance",
            () -> EntityType.Builder.<SusanooLance>of(SusanooLance::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).fireImmune()
                    .build(new ResourceLocation(MangekyouAddon.MODID, "susanoo_lance").toString()));

    public static void onAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent e) {
        e.put(MADARA_SUSANOO.get(), CharacterSusanoo.createAttributes().build());
        e.put(ITACHI_SUSANOO.get(), CharacterSusanoo.createAttributes().build());
        e.put(SHISUI_SUSANOO.get(), CharacterSusanoo.createAttributes().build());
    }

    private ModEntities() {}
}
