package net.oktawia.structuretokubejsaisles;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The server's answer to "copy": the scanned structure as text, which the client decodes into KubeJS and puts on the
 * clipboard. A clientbound custom payload may not exceed 1 MiB, so the text is capped just below that.
 */
public record ClipboardPayload(String data) implements CustomPacketPayload {
    public static final int MAX_CHARS = 1_000_000;

    public static final Type<ClipboardPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModContent.MOD_ID, "clipboard"));

    public static final StreamCodec<ByteBuf, ClipboardPayload> CODEC =
            ByteBufCodecs.stringUtf8(MAX_CHARS).map(ClipboardPayload::new, ClipboardPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
