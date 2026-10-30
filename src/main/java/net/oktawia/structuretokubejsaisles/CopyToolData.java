package net.oktawia.structuretokubejsaisles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/** Reads and writes the two selection corners stored on a Copy Tool stack (corner 1 and corner 2). */
public final class CopyToolData {
    private CopyToolData() {}

    private static DataComponentType<BlockPos> type(int corner) {
        return corner == 1 ? ModContent.POS1.get() : ModContent.POS2.get();
    }

    /** The stored corner, or null when it has not been set. */
    public static BlockPos get(ItemStack stack, int corner) {
        return stack.get(type(corner));
    }

    public static void set(ItemStack stack, int corner, BlockPos pos) {
        stack.set(type(corner), pos);
    }

    public static void clear(ItemStack stack, int corner) {
        stack.remove(type(corner));
    }
}
