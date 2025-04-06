package net.oktawia.structruretokubejsaisles.items;

import com.google.common.base.Splitter;
import net.minecraft.core.BlockPos;
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
    private BlockPos pos1 = null;
    private BlockPos pos2 = null;

    public CopyToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && pos1 != null && pos2 != null) {
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
            char nextChar = 'A';

            StringBuilder dataBuilder = new StringBuilder();
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        BlockPos current = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(current);
                        String id = state.getBlock().toString().substring(state.getBlock().toString().indexOf('{') + 1, state.getBlock().toString().lastIndexOf('}'));;
                        if (!mapping.containsKey(id)) {
                            mapping.put(id, nextChar);
                            nextChar++;
                        }
                        dataBuilder.append(mapping.get(id));
                    }
                }
            }

            StringBuilder mappingBuilder = new StringBuilder();
            for (Map.Entry<String, Character> entry : mapping.entrySet()) {
                mappingBuilder.append(entry.getValue())
                        .append("=")
                        .append(entry.getKey())
                        .append("\n");
            }

            String finalData = sizeX + " " + sizeY + " " + sizeZ + "\n" +
                    mappingBuilder.toString() + "\n" +
                    dataBuilder.toString();

            if (player instanceof ServerPlayer serverPlayer) {
                Iterable<String> chunks = Splitter.fixedLength(32767).split(finalData);
                int i = 0;
                for(String chunk: chunks){
                    i = i+1;
                    NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new ClipboardPacket(chunk));
                    player.displayClientMessage(Component.literal("sent packet " + i + " / " + finalData.length()/32767), true);
                }
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new ClipboardPacket("ThisIsTheEnd"));
            }
            player.displayClientMessage(Component.literal("Copied " + (sizeX * sizeY * sizeZ) + " blocks."), true);

            pos1 = null;
            pos2 = null;
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player != null) {
            if (context.isSecondaryUseActive()) {
                pos1 = pos;
                player.displayClientMessage(Component.literal("Corner 1 set!"), true);
            } else {
                pos2 = pos;
                player.displayClientMessage(Component.literal("Corner 2 set!"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}

