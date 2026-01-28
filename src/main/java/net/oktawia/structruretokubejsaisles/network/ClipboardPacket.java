package net.oktawia.structruretokubejsaisles.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

public class ClipboardPacket {
    public static final String END_MARKER = "ThisIsTheEnd";

    private static final boolean AISLE_TOP_FIRST = true;

    private final String data;

    private static final StringBuilder packetAccumulator = new StringBuilder();

    public ClipboardPacket(String data) {
        this.data = data;
    }

    public static void encode(ClipboardPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.data);
    }

    public static ClipboardPacket decode(FriendlyByteBuf buf) {
        return new ClipboardPacket(buf.readUtf());
    }

    public static void handle(ClipboardPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> setClipboard(packet.data));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static String jsEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @OnlyIn(Dist.CLIENT)
    private static void setClipboard(String incoming) {
        if (!Objects.equals(incoming, END_MARKER)) {
            packetAccumulator.append(incoming);
            return;
        }

        String data = packetAccumulator.toString();
        packetAccumulator.setLength(0);

        String[] lines = data.split("\n", -1);
        if (lines.length < 2) {
            Minecraft.getInstance().keyboardHandler.setClipboard(data);
            return;
        }

        String[] dims = lines[0].trim().split(" ");
        if (dims.length != 3) {
            Minecraft.getInstance().keyboardHandler.setClipboard(data);
            return;
        }

        int sizeX, sizeY, sizeZ;
        try {
            sizeX = Integer.parseInt(dims[0]);
            sizeY = Integer.parseInt(dims[1]);
            sizeZ = Integer.parseInt(dims[2]);
        } catch (NumberFormatException e) {
            Minecraft.getInstance().keyboardHandler.setClipboard(data);
            return;
        }

        Map<Character, String> mapping = new LinkedHashMap<>();
        int i = 1;
        while (i < lines.length && !lines[i].trim().isEmpty()) {
            String line = lines[i].trim();
            String[] parts = line.split("=", 2);
            if (parts.length == 2 && !parts[0].isEmpty()) {
                mapping.put(parts[0].charAt(0), parts[1]);
            }
            i++;
        }
        while (i < lines.length && lines[i].trim().isEmpty()) i++;

        StringBuilder encodedBuilder = new StringBuilder();
        for (; i < lines.length; i++) {
            encodedBuilder.append(lines[i].trim());
        }
        String encoded = encodedBuilder.toString();

        int expectedLength = sizeX * sizeY * sizeZ;
        if (encoded.length() < expectedLength) {
            Minecraft.getInstance().keyboardHandler.setClipboard(data);
            return;
        }

        StringBuilder output = new StringBuilder();
        int levelSize = sizeX * sizeZ;

        if (AISLE_TOP_FIRST) {
            for (int y = sizeY - 1; y >= 0; y--) {
                appendAisle(output, encoded, sizeX, sizeZ, levelSize, y);
            }
        } else {
            for (int y = 0; y < sizeY; y++) {
                appendAisle(output, encoded, sizeX, sizeZ, levelSize, y);
            }
        }

        output.append("\n");

        for (Map.Entry<Character, String> entry : mapping.entrySet()) {
            output.append(".where(\"")
                    .append(jsEscape(String.valueOf(entry.getKey())))
                    .append("\", Predicates.blocks(\"")
                    .append(jsEscape(entry.getValue()))
                    .append("\"))\n");
        }

        String outStr = output.toString();

        if (outStr.length() > 32767) {
            try {
                Path path = Paths.get("packet.txt");
                Files.writeString(path, outStr, StandardCharsets.UTF_8);
                Minecraft.getInstance().keyboardHandler.setClipboard("Data saved to: " + path.toAbsolutePath());
                Minecraft.getInstance().getToasts().addToast(
                        new SystemToast(
                                SystemToast.SystemToastIds.TUTORIAL_HINT,
                                Component.literal("Copy Tool Debug"),
                                Component.literal("Path copied to clipboard")
                        )
                );
            } catch (Exception e) {
                Minecraft.getInstance().keyboardHandler.setClipboard(outStr.substring(0, 32767));
            }
        } else {
            Minecraft.getInstance().keyboardHandler.setClipboard(outStr);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void appendAisle(StringBuilder output,
                                    String encoded,
                                    int sizeX,
                                    int sizeZ,
                                    int levelSize,
                                    int yIndex) {

        output.append(".aisle(");
        int levelStart = yIndex * levelSize;

        List<String> rows = new ArrayList<>(sizeZ);
        for (int z = 0; z < sizeZ; z++) {
            int rowStart = levelStart + z * sizeX;
            String row = encoded.substring(rowStart, rowStart + sizeX);
            rows.add("\"" + jsEscape(row) + "\"");
        }

        output.append(String.join(", ", rows));
        output.append(")\n");
    }
}
