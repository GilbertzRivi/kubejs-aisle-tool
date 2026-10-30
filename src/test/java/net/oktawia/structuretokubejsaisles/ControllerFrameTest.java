package net.oktawia.structuretokubejsaisles;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Hand-worked from GTM 8's own definitions: the default pattern builder walks slices BACK, strings UP and chars RIGHT, and for a
 * controller with front F and up U, BACK is F's opposite and RIGHT is GTUtil.cross(F, U). The south-facing row is also what the 1.20.1
 * tool always wrote (first slice at the highest z, first char at the highest x).
 */
class ControllerFrameTest {

    private static int[] vectors(Direction slice, Direction row, Direction ch) {
        return new int[] {
                slice.getStepX(), slice.getStepY(), slice.getStepZ(),
                row.getStepX(), row.getStepY(), row.getStepZ(),
                ch.getStepX(), ch.getStepY(), ch.getStepZ()
        };
    }

    @Test
    void facingSouthIsTheOldToolsLayout() {
        assertArrayEquals(vectors(Direction.NORTH, Direction.UP, Direction.WEST), ControllerFrame.frameOf(Direction.SOUTH, Direction.UP));
        assertArrayEquals(ControllerFrame.frameOf(Direction.SOUTH, Direction.UP), ControllerFrame.defaultFrame());
    }

    @Test
    void theOtherHorizontalFacings() {
        assertArrayEquals(vectors(Direction.SOUTH, Direction.UP, Direction.EAST), ControllerFrame.frameOf(Direction.NORTH, Direction.UP));
        assertArrayEquals(vectors(Direction.WEST, Direction.UP, Direction.SOUTH), ControllerFrame.frameOf(Direction.EAST, Direction.UP));
        assertArrayEquals(vectors(Direction.EAST, Direction.UP, Direction.NORTH), ControllerFrame.frameOf(Direction.WEST, Direction.UP));
    }

    @Test
    void aControllerFacingUpUsesTheGivenUpwardsDirection() {
        assertArrayEquals(vectors(Direction.DOWN, Direction.NORTH, Direction.WEST), ControllerFrame.frameOf(Direction.UP, Direction.NORTH));
    }

    @Test
    void crossMatchesGtmsGtUtil() {
        // cross(a, b) as GTUtil.cross computes it: the right-hand rule on the step vectors
        assertArrayEquals(new int[] {1, 0, 0}, step(ControllerFrame.cross(Direction.NORTH, Direction.UP)));
        assertArrayEquals(new int[] {0, 0, 1}, step(ControllerFrame.cross(Direction.EAST, Direction.UP)));
    }

    private static int[] step(Direction d) {
        return new int[] {d.getStepX(), d.getStepY(), d.getStepZ()};
    }
}
