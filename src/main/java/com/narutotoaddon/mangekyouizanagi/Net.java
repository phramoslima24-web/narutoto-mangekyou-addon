package com.narutotoaddon.mangekyouizanagi;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private static final String VERSION = "2";

    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MangekyouAddon.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public static void register() {
        int id = 0;
        CH.registerMessage(id++, SyncImplantPacket.class, SyncImplantPacket::encode, SyncImplantPacket::decode, SyncImplantPacket::handle);
        CH.registerMessage(id++, LearnJutsuPacket.class, LearnJutsuPacket::encode, LearnJutsuPacket::decode, LearnJutsuPacket::handle);
        CH.registerMessage(id++, UseJutsuPacket.class, UseJutsuPacket::encode, UseJutsuPacket::decode, UseJutsuPacket::handle);
    }
}
