package net.oktawia.structuretokubejsaisles.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.oktawia.structuretokubejsaisles.ControllerFrame;
import net.oktawia.structuretokubejsaisles.CopyToolData;
import net.oktawia.structuretokubejsaisles.ModContent;
import net.oktawia.structuretokubejsaisles.ToolActionPayload;
import net.oktawia.structuretokubejsaisles.hc.hcCompileMerged1;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scans real blocks in a game-test world and checks the KubeJS text, and drives the item the way a player would. */
@GameTestHolder(ModContent.MOD_ID)
@PrefixGameTestTemplate(false)
public class CopyToolGameTests {

    private static final Pattern WHERE = Pattern.compile("\\.where\\(\"(.)\", Predicates\\.blocks\\(\"([^\"]+)\"\\)\\)");
    private static final Pattern SLICE = Pattern.compile("\\.slice\\((.*)\\)");

    /** The server's text split into its three parts: header, the letter table, and the cell characters. */
    private record Scan(String header, Map<Character, String> table, String cells) {
        static Scan of(String data) {
            String[] parts = data.split("\n\n", 2);
            String[] lines = parts[0].split("\n");
            Map<Character, String> table = new HashMap<>();
            for (int i = 1; i < lines.length; i++) table.put(lines[i].charAt(0), lines[i].substring(2));
            return new Scan(lines[0], table, parts[1]);
        }

        char cell(int index) {
            return cells.charAt(index);
        }

        String blockAt(int index) {
            return table.get(cell(index));
        }
    }

    private static int[] noController() {
        return ControllerFrame.defaultFrame();
    }

    /** A 3 (x) by 2 (y) by 4 (z) box of stone with glass in the (max x, min y, max z) corner and a sponge at (min x, max y, min z). */
    private static void buildBox(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 4; z++) {
            helper.setBlock(new BlockPos(x, 1 + y, z), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.GLASS);
        helper.setBlock(new BlockPos(0, 2, 0), Blocks.SPONGE);
    }

    private static BlockPos low(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(0, 1, 0));
    }

    private static BlockPos high(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(2, 2, 3));
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void scansLikeTheOldToolWhenNoControllerIsMarked(GameTestHelper helper) {
        buildBox(helper);
        String data = hcCompileMerged1.scan_region(helper.getLevel(), low(helper), high(helper), noController(), 0, low(helper));
        Scan scan = Scan.of(data);
        helper.assertTrue("v2 4 2 3".equals(scan.header()), "slices (z) rows (y) chars (x) but was: " + scan.header());
        helper.assertTrue(scan.cells().length() == 24, "one character per block");
        // first slice = highest z, first row = lowest y, first char = highest x: that corner holds the glass
        helper.assertTrue("minecraft:glass".equals(scan.blockAt(0)), "glass at (max x, min y, max z) comes first, got " + scan.blockAt(0));
        // last slice = lowest z, last row = highest y, last char = lowest x: the sponge
        helper.assertTrue("minecraft:sponge".equals(scan.blockAt(23)), "sponge at (min x, max y, min z) comes last, got " + scan.blockAt(23));
        helper.succeed();
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void decodesTheScanIntoSlices(GameTestHelper helper) {
        buildBox(helper);
        String data = hcCompileMerged1.scan_region(helper.getLevel(), low(helper), high(helper), noController(), 0, low(helper));
        String kubejs = hcCompileMerged1.decode_clipboard(data);
        helper.assertTrue(!kubejs.contains(".aisle("), "no GTM 1.20 aisle call");

        Matcher slices = SLICE.matcher(kubejs);
        int count = 0;
        String first = null;
        String last = null;
        while (slices.find()) {
            if (first == null) first = slices.group(1);
            last = slices.group(1);
            count++;
        }
        helper.assertTrue(count == 4, "one slice per z (4) but found " + count);

        Map<Character, String> letters = new HashMap<>();
        Matcher m = WHERE.matcher(kubejs);
        while (m.find()) letters.put(m.group(1).charAt(0), m.group(2));
        helper.assertTrue(letters.size() == 3, "three distinct blocks but " + letters);
        helper.assertTrue("minecraft:glass".equals(letters.get(first.split(", ")[0].charAt(1))), "glass is the first cell");
        helper.assertTrue("minecraft:sponge".equals(letters.get(last.split(", ")[1].charAt(3))), "sponge is the last cell");
        helper.succeed();
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void controllerTurnsTheScanToFaceIt(GameTestHelper helper) {
        // glass at (2, 1, 3) and the controller at (1, 2, 1) of the box, for each of the four horizontal facings.
        // Expected cell numbers worked out by hand from GTM 8: slices walk BACK (away from the front), rows UP, chars RIGHT.
        BlockPos glass = helper.absolutePos(new BlockPos(2, 1, 3));
        BlockPos controllerRel = new BlockPos(1, 2, 1);
        BlockPos controller = helper.absolutePos(controllerRel);

        record Case(Direction facing, String header, int glassCell, int controllerCell) {}
        Case[] cases = {
                // south: slices run north (z from 3 down), chars run west (x from 2 down) -- the old tool's layout
                new Case(Direction.SOUTH, "v2 4 2 3", 0, ((3 - 1) * 2 + 1) * 3 + (2 - 1)),
                // north: slices run south (z from 0 up), chars run east (x from 0 up)
                new Case(Direction.NORTH, "v2 4 2 3", ((3 - 0) * 2 + 0) * 3 + 2, ((1 - 0) * 2 + 1) * 3 + 1),
                // east: slices run west (x from 2 down), chars run south (z from 0 up)
                new Case(Direction.EAST, "v2 3 2 4", 3, ((2 - 1) * 2 + 1) * 4 + 1),
                // west: slices run east (x from 0 up), chars run north (z from 3 down)
                new Case(Direction.WEST, "v2 3 2 4", (2 * 2 + 0) * 4 + 0, ((1 - 0) * 2 + 1) * 4 + (3 - 1)),
        };

        for (Case c : cases) {
            buildBox(helper);
            helper.setBlock(controllerRel, Blocks.FURNACE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, c.facing()));
            int[] frame = ControllerFrame.frame(helper.getLevel(), controller);
            String data = hcCompileMerged1.scan_region(helper.getLevel(), low(helper), high(helper), frame, 1, controller);
            Scan scan = Scan.of(data);
            helper.assertTrue(c.header().equals(scan.header()), c.facing() + ": header " + c.header() + " but was " + scan.header());
            helper.assertTrue("minecraft:glass".equals(scan.blockAt(c.glassCell())), c.facing() + ": glass should be cell " + c.glassCell());
            helper.assertTrue(scan.cell(c.controllerCell()) == '=', c.facing() + ": controller should be cell " + c.controllerCell()
                    + " of " + scan.cells());
            helper.assertTrue(scan.cells().indexOf('=') == scan.cells().lastIndexOf('='), c.facing() + ": exactly one controller cell");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 200)
    public static void tooManyDifferentBlocksIsRefused(GameTestHelper helper) {
        int placed = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (placed >= 100) break;
            BlockPos at = new BlockPos(placed % 10, 1, placed / 10);
            helper.setBlock(at, block);
            // some blocks refuse to stay put (they need support), so count only what really stuck
            if (helper.getLevel().getBlockState(helper.absolutePos(at)).getBlock() == block) placed++;
        }
        BlockPos low = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos high = helper.absolutePos(new BlockPos(9, 1, 9));
        helper.assertTrue(hcCompileMerged1.scan_region(helper.getLevel(), low, high, noController(), 0, low).isEmpty(),
                "more than 91 distinct blocks is refused with an empty result");
        helper.succeed();
    }

    private static UseOnContext click(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos pos) {
        return new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 200)
    public static void clickingSetsTheCornersOnTheItem(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(ModContent.COPY_TOOL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        BlockPos a = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos b = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.assertTrue(CopyToolData.get(stack, 1) == null && CopyToolData.get(stack, 2) == null, "no corners yet");

        player.setShiftKeyDown(true);
        stack.useOn(click(helper, player, stack, a));
        helper.assertTrue(a.equals(CopyToolData.get(stack, 1)), "sneak + click sets corner 1");

        player.setShiftKeyDown(false);
        stack.useOn(click(helper, player, stack, b));
        helper.assertTrue(b.equals(CopyToolData.get(stack, 2)), "a plain click sets corner 2");
        helper.assertTrue(a.equals(CopyToolData.get(stack, 1)), "corner 1 is unchanged by the second click");
        helper.succeed();
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 200)
    public static void controllerModeMarksAndUnmarksTheController(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(ModContent.COPY_TOOL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        helper.assertTrue(!CopyToolData.controllerMode(stack), "starts in corner mode");
        ToolActionPayload.apply(player, ToolActionPayload.TOGGLE_MODE);
        helper.assertTrue(CopyToolData.controllerMode(stack), "the mode key switches to controller mode");

        stack.useOn(click(helper, player, stack, pos));
        helper.assertTrue(pos.equals(CopyToolData.get(stack, 3)), "a click marks the controller");
        helper.assertTrue(CopyToolData.get(stack, 1) == null && CopyToolData.get(stack, 2) == null, "and sets no corner");

        player.setShiftKeyDown(true);
        stack.useOn(click(helper, player, stack, pos));
        helper.assertTrue(CopyToolData.get(stack, 3) == null, "sneak + click unmarks it");

        ToolActionPayload.apply(player, ToolActionPayload.TOGGLE_MODE);
        helper.assertTrue(!CopyToolData.controllerMode(stack), "the mode key switches back");
        helper.succeed();
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 200)
    public static void theClearKeyForgetsEverything(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(ModContent.COPY_TOOL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        CopyToolData.set(stack, 1, pos);
        CopyToolData.set(stack, 2, pos);
        CopyToolData.set(stack, 3, pos);
        CopyToolData.setControllerMode(stack, true);

        ToolActionPayload.apply(player, ToolActionPayload.CLEAR);
        helper.assertTrue(CopyToolData.get(stack, 1) == null && CopyToolData.get(stack, 2) == null && CopyToolData.get(stack, 3) == null,
                "corners and controller are gone");
        helper.assertTrue(!CopyToolData.controllerMode(stack), "and the tool is back in corner mode");

        // pressing the keys while holding something else does nothing to it
        ItemStack other = new ItemStack(net.minecraft.world.item.Items.STICK);
        player.setItemInHand(InteractionHand.MAIN_HAND, other);
        ToolActionPayload.apply(player, ToolActionPayload.TOGGLE_MODE);
        helper.assertTrue(other.isEmpty() == false, "the other item is untouched");
        helper.succeed();
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 200)
    public static void usingWithoutBothCornersDoesNotCopy(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(ModContent.COPY_TOOL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        CopyToolData.set(stack, 1, helper.absolutePos(new BlockPos(1, 1, 1)));

        stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(CopyToolData.get(stack, 1) != null, "the lone corner is kept (nothing was copied)");
        helper.succeed();
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 200)
    public static void aControllerOutsideTheSelectionIsRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(ModContent.COPY_TOOL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        CopyToolData.set(stack, 1, helper.absolutePos(new BlockPos(0, 1, 0)));
        CopyToolData.set(stack, 2, helper.absolutePos(new BlockPos(2, 2, 2)));
        CopyToolData.set(stack, 3, helper.absolutePos(new BlockPos(5, 1, 5)));

        stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(CopyToolData.get(stack, 1) != null && CopyToolData.get(stack, 3) != null,
                "nothing was copied, so the marks are kept");
        helper.succeed();
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void marksSurviveCopyingTheStack(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModContent.COPY_TOOL.get());
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        CopyToolData.set(stack, 2, pos);
        CopyToolData.set(stack, 3, pos);
        CopyToolData.setControllerMode(stack, true);
        ItemStack copy = stack.copy();
        helper.assertTrue(pos.equals(CopyToolData.get(copy, 2)) && pos.equals(CopyToolData.get(copy, 3)), "a copied stack keeps its marks");
        helper.assertTrue(CopyToolData.controllerMode(copy), "and its mode");
        CopyToolData.clear(copy, 2);
        helper.assertTrue(CopyToolData.get(copy, 2) == null, "clear removes it");
        helper.assertTrue(pos.equals(CopyToolData.get(stack, 2)), "and the original is untouched");
        helper.succeed();
    }
}
