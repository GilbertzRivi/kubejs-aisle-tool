package net.oktawia.structuretokubejsaisles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/**
 * Reads and writes what is stored on a Copy Tool stack: corner 1, corner 2, the controller (corner number 3), and whether
 * right-clicking a block currently marks corners or the controller.
 */
public final class CopyToolData {
    public static final int CORNER_1 = 1;
    public static final int CORNER_2 = 2;
    public static final int CONTROLLER = 3;

    private CopyToolData() {}

    private static DataComponentType<BlockPos> type(int which) {
        return switch (which) {
            case CORNER_1 -> ModContent.POS1.get();
            case CORNER_2 -> ModContent.POS2.get();
            default -> ModContent.CONTROLLER.get();
        };
    }

    /** The stored position, or null when it has not been set. */
    public static BlockPos get(ItemStack stack, int which) {
        return stack.get(type(which));
    }

    public static void set(ItemStack stack, int which, BlockPos pos) {
        stack.set(type(which), pos);
    }

    public static void clear(ItemStack stack, int which) {
        stack.remove(type(which));
    }

    /** True while right-clicking a block marks the controller instead of a corner. */
    public static boolean controllerMode(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModContent.CONTROLLER_MODE.get()));
    }

    public static void setControllerMode(ItemStack stack, boolean on) {
        if (on) {
            stack.set(ModContent.CONTROLLER_MODE.get(), Boolean.TRUE);
        } else {
            stack.remove(ModContent.CONTROLLER_MODE.get());
        }
    }
}
