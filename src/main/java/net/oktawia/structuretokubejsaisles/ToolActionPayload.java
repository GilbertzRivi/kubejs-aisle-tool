package net.oktawia.structuretokubejsaisles;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** What the two Copy Tool keys ask the server to do to the tool in the player's main hand. */
public record ToolActionPayload(int action) implements CustomPacketPayload {
    public static final int TOGGLE_MODE = 0;
    public static final int CLEAR = 1;

    public static final Type<ToolActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModContent.MOD_ID, "tool_action"));

    public static final StreamCodec<ByteBuf, ToolActionPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(ToolActionPayload::new, ToolActionPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Applies the action to the tool in {@code player}'s main hand; does nothing when the main hand holds something else. */
    public static void apply(ServerPlayer player, int action) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!stack.is(ModContent.COPY_TOOL.get())) return;
        switch (action) {
            case TOGGLE_MODE -> {
                boolean on = !CopyToolData.controllerMode(stack);
                CopyToolData.setControllerMode(stack, on);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        on ? "Right click a block to mark the controller (sneak to unmark it)."
                                : "Right click a block to set corners again."), true);
            }
            case CLEAR -> {
                CopyToolData.clear(stack, CopyToolData.CORNER_1);
                CopyToolData.clear(stack, CopyToolData.CORNER_2);
                CopyToolData.clear(stack, CopyToolData.CONTROLLER);
                CopyToolData.setControllerMode(stack, false);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Selection cleared."), true);
            }
            default -> { }
        }
    }
}
