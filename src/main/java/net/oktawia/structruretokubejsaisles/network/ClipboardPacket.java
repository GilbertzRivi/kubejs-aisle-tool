package net.oktawia.structruretokubejsaisles.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

public class ClipboardPacket {
    private String data;
    private static StringBuilder packetAccumulator = new StringBuilder();
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
        ctx.get().enqueueWork(() -> {
            setClipboard(packet.data);
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void setClipboard(String data) {
        if (!Objects.equals(data, "ThisIsTheEnd")){
            packetAccumulator.append(data);
            return;
        }

        String completePacket = packetAccumulator.toString();
        packetAccumulator.delete(0, completePacket.length());
        data = completePacket;

        String[] lines = data.split("\n");
        if (lines.length < 2) {
            Minecraft.getInstance().keyboardHandler.setClipboard(data);
            return;
        }
        String[] dims = lines[0].split(" ");
        if (dims.length != 3) {
            Minecraft.getInstance().keyboardHandler.setClipboard(data);
            return;
        }
        int sizeX = Integer.parseInt(dims[0]);
        int sizeY = Integer.parseInt(dims[1]);
        int sizeZ = Integer.parseInt(dims[2]);

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

        while (i < lines.length && lines[i].trim().isEmpty()) {
            i++;
        }

        StringBuilder encodedBuilder = new StringBuilder();
        for (; i < lines.length; i++) {
            encodedBuilder.append(lines[i].trim());
        }
        String encoded = encodedBuilder.toString();

        int expectedLength = sizeX * sizeZ * sizeY;
        if (encoded.length() < expectedLength) {
            Minecraft.getInstance().keyboardHandler.setClipboard(data);
            return;
        }

        StringBuilder output = new StringBuilder();
        int levelSize = sizeX * sizeZ;
        for (int y = 0; y < sizeY; y++) {
            output.append(".aisle(");
            int levelStart = y * levelSize;
            List<String> rows = new ArrayList<>();
            for (int z = 0; z < sizeZ; z++) {
                int rowStart = levelStart + z * sizeX;
                String row = encoded.substring(rowStart, rowStart + sizeX);
                rows.add("\"" + row + "\"");
            }
            output.append(String.join(", ", rows));
            output.append(")\n");
        }
        output.append("\n");
        for (Map.Entry<Character, String> entry : mapping.entrySet()) {
            output.append(".where(\"")
                    .append(entry.getKey())
                    .append("\", Predicates.blocks(\"")
                    .append(entry.getValue())
                    .append("\"))\n");
        }
        if (output.toString().length() > 32767) {
            try {
                Path path = Paths.get("packet.txt");
                Files.writeString(path, output);
                Minecraft.getInstance().keyboardHandler.setClipboard("Dane zapisane do: " + path.toAbsolutePath());
            } catch (Exception ignored) {
            }
        } else {
            Minecraft.getInstance().keyboardHandler.setClipboard(output.toString());
        }
    }
}