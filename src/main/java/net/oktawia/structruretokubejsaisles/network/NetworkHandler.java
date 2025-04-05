package net.oktawia.structruretokubejsaisles.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.oktawia.structruretokubejsaisles.Structruretokubejsaisles;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Structruretokubejsaisles.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerPackets() {
        int id = 0;
        INSTANCE.registerMessage(
                id++,
                ClipboardPacket.class,
                ClipboardPacket::encode,
                ClipboardPacket::decode,
                ClipboardPacket::handle);
    }
}
