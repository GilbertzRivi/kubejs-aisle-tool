package net.oktawia.structuretokubejsaisles;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/** The Java end of sending the scanned structure to the player who copied it (called from the Hot Chocolate code). */
public final class ClipboardSender {
    private ClipboardSender() {}

    public static void send(Player player, String data) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ClipboardPayload(data));
        }
    }
}
