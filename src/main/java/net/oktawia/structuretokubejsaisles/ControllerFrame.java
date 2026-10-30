package net.oktawia.structuretokubejsaisles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Which way a GTM 8 pattern reads the world, given the controller's facing.
 *
 * The builder's default start() is (slice = BACK, string = UP, char = RIGHT), and relative to a controller with front F and
 * upwards direction U those are: BACK = opposite of F, UP = U, RIGHT = cross(F, U) (GTUtil.cross). A pattern cell
 * (slice s, string r, char c) is therefore the block s steps along BACK, r steps along UP and c steps along RIGHT from the pattern's
 * start. With no controller marked the tool assumes a controller facing south, which is the layout the 1.20.1 tool always wrote.
 */
public final class ControllerFrame {
    private ControllerFrame() {}

    /** The default frame: a controller facing south with up as up. */
    public static int[] defaultFrame() {
        return frameOf(Direction.SOUTH, Direction.UP);
    }

    /** The frame of the controller block at {@code controller}: 9 ints (slice direction, string direction, char direction). */
    public static int[] frame(Level level, BlockPos controller) {
        BlockState state = level.getBlockState(controller);
        Direction front = front(state);
        Direction up = up(state, front);
        return frameOf(front, up);
    }

    /** The frame for a known front and upwards direction. */
    public static int[] frameOf(Direction front, Direction up) {
        Direction slice = front.getOpposite();
        Direction cross = cross(front, up);
        return new int[] {
                slice.getStepX(), slice.getStepY(), slice.getStepZ(),
                up.getStepX(), up.getStepY(), up.getStepZ(),
                cross.getStepX(), cross.getStepY(), cross.getStepZ()
        };
    }

    /** GTUtil.cross from GTM 8, reproduced so the tool needs no GTM classes. */
    public static Direction cross(Direction a, Direction b) {
        return Direction.getNearest(
                a.getStepY() * b.getStepZ() - a.getStepZ() * b.getStepY(),
                a.getStepZ() * b.getStepX() - a.getStepX() * b.getStepZ(),
                a.getStepX() * b.getStepY() - a.getStepY() * b.getStepX());
    }

    /** The block's facing: the property named "facing", else any direction property, else south. */
    public static Direction front(BlockState state) {
        Direction named = directionNamed(state, "facing");
        if (named != null) return named;
        for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty direction) return state.getValue(direction);
        }
        return Direction.SOUTH;
    }

    /** The block's upwards direction: GTM's "upwards_facing" when present, else up (north for a controller that faces up or down). */
    public static Direction up(BlockState state, Direction front) {
        Direction named = directionNamed(state, "upwards_facing");
        if (named != null && named.getAxis() != front.getAxis()) return named;
        if (front.getAxis() == Direction.Axis.Y) return Direction.NORTH;
        return Direction.UP;
    }

    private static Direction directionNamed(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name) && property instanceof DirectionProperty direction) {
                return state.getValue(direction);
            }
        }
        return null;
    }
}
