
package net.oktawia.structruretokubejsaisles.items;

import com.google.common.base.Splitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.structruretokubejsaisles.network.ClipboardPacket;
import net.oktawia.structruretokubejsaisles.network.NetworkHandler;

import java.util.LinkedHashMap;
import java.util.Map;

public class CopyToolItem extends Item {
    private static final String NBT_POS1 = "copytool_pos1";
    private static final String NBT_POS2 = "copytool_pos2";

    // Big palette: a-z A-Z 0-9 and lots of symbols.
    // Excludes: '=' (mapping delimiter), '"' and '\' (JS escaping hazards), whitespace/newlines.
    private static final char[] ALPHABET =
            ("abcdefghijklmnopqrstuvwxyz" +
             "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
             "0123456789" +
             "!#$%&'()*+,-./:;<>?@[]^_`{|}~")
            .toCharArray();

    public CopyToolItem(Properties properties) {
        super(properties);
    }

    private static BlockPos readPos(ItemStack stack, String key) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(key)) return null;
        return BlockPos.of(tag.getLong(key));
    }

    private static void writePos(ItemStack stack, String key, BlockPos pos) {
        stack.getOrCreateTag().putLong(key, pos.asLong());
    }

    private static void clearPos(ItemStack stack, String key) {
        var tag = stack.getTag();
        if (tag != null) tag.remove(key);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            BlockPos pos1 = readPos(stack, NBT_POS1);
            BlockPos pos2 = readPos(stack, NBT_POS2);

            if (pos1 != null && pos2 != null) {
                BlockPos min = new BlockPos(
                        Math.min(pos1.getX(), pos2.getX()),
                        Math.min(pos1.getY(), pos2.getY()),
                        Math.min(pos1.getZ(), pos2.getZ())
                );
                BlockPos max = new BlockPos(
                        Math.max(pos1.getX(), pos2.getX()),
                        Math.max(pos1.getY(), pos2.getY()),
                        Math.max(pos1.getZ(), pos2.getZ())
                );

                int sizeX = max.getX() - min.getX() + 1;
                int sizeY = max.getY() - min.getY() + 1;
                int sizeZ = max.getZ() - min.getZ() + 1;

                Map<String, Character> mapping = new LinkedHashMap<>();
                int nextIndex = 0;
                
                StringBuilder dataBuilder = new StringBuilder(sizeX * sizeY * sizeZ);
                for (int y = max.getY(); y >= min.getY(); y--) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        for (int x = min.getX(); x <= max.getX(); x++) {
                            BlockPos current = new BlockPos(x, y, z);
                            BlockState state = level.getBlockState(current);
                            String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

                            Character ch = mapping.get(id);
                            if (ch == null) {
                                if (nextIndex >= ALPHABET.length) {
                                    player.displayClientMessage(Component.literal(
                                            "Too many unique blocks (" + (nextIndex + 1) + "). Expand ALPHABET."
                                    ), true);
                                    return InteractionResultHolder.fail(stack);
                                }
                                ch = ALPHABET[nextIndex++];
                                mapping.put(id, ch);
                            }
                            dataBuilder.append(ch);
                        }
                    }
                }

                StringBuilder mappingBuilder = new StringBuilder();
                for (Map.Entry<String, Character> entry : mapping.entrySet()) {
                    // NOTE: '=' cannot appear as a symbol because it is the delimiter.
                    mappingBuilder.append(entry.getValue())
                            .append("=")
                            .append(entry.getKey())
                            .append("\n");
                }

                String finalData = sizeX + " " + sizeY + " " + sizeZ + "\n" +
                        mappingBuilder + "\n" +
                        dataBuilder;

                if (player instanceof ServerPlayer serverPlayer) {
                    Iterable<String> chunks = Splitter.fixedLength(32767).split(finalData);

                    int sent = 0;
                    int totalChunks = (finalData.length() + 32767 - 1) / 32767;

                    for (String chunk : chunks) {
                        sent++;
                        NetworkHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> serverPlayer),
                                new ClipboardPacket(chunk)
                        );
                        player.displayClientMessage(
                                Component.literal("Sent packet " + sent + " / " + totalChunks),
                                true
                        );
                    }

                    NetworkHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new ClipboardPacket(ClipboardPacket.END_MARKER)
                    );
                }

                player.displayClientMessage(Component.literal("Copied " + (sizeX * sizeY * sizeZ) + " blocks."), true);

                clearPos(stack, NBT_POS1);
                clearPos(stack, NBT_POS2);
            } else {
                player.displayClientMessage(Component.literal("Set both corners first."), true);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player != null) {
            ItemStack stack = context.getItemInHand();
            if (context.isSecondaryUseActive()) {
                writePos(stack, NBT_POS1, pos);
                player.displayClientMessage(Component.literal("Corner 1 set!"), true);
            } else {
                writePos(stack, NBT_POS2, pos);
                player.displayClientMessage(Component.literal("Corner 2 set!"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
