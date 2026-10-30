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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.oktawia.structuretokubejsaisles.CopyToolData;
import net.oktawia.structuretokubejsaisles.ModContent;
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

    /** letter -> block id, read back from the generated .where lines. */
    private static Map<Character, String> letters(String kubejs) {
        Map<Character, String> map = new HashMap<>();
        Matcher m = WHERE.matcher(kubejs);
        while (m.find()) map.put(m.group(1).charAt(0), m.group(2));
        return map;
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void scansAStructureIntoSlices(GameTestHelper helper) {
        // A 3 (x) by 2 (y) by 4 (z) box: stone everywhere, glass in the (max x, min y, max z) corner, a sponge at (min x, max y, min z)
        for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 4; z++) {
            helper.setBlock(new BlockPos(x, 1 + y, z), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.GLASS);
        helper.setBlock(new BlockPos(0, 2, 0), Blocks.SPONGE);

        BlockPos low = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos high = helper.absolutePos(new BlockPos(2, 2, 3));
        String data = hcCompileMerged1.scan_region(helper.getLevel(), low, high);
        helper.assertTrue(data.startsWith("3 4 2\n"), "header is size_x size_z size_y but was: " + data.split("\n")[0]);

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

        Map<Character, String> map = letters(kubejs);
        helper.assertTrue(map.size() == 3, "three distinct blocks but " + map);

        // slices run from the highest z down; rows within a slice run from the lowest y up; the first char is the highest x
        String[] firstRows = first.split(", ");
        char firstCell = firstRows[0].charAt(1);
        helper.assertTrue("minecraft:glass".equals(map.get(firstCell)), "glass at (max x, min y, max z) is the first cell, got " + map.get(firstCell));
        String[] lastRows = last.split(", ");
        char sponge = lastRows[1].charAt(3); // second row (y = max), last char (x = min)
        helper.assertTrue("minecraft:sponge".equals(map.get(sponge)), "sponge at (min x, max y, min z) is the last cell, got " + map.get(sponge));
        helper.succeed();
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 200)
    public static void tooManyDifferentBlocksIsRefused(GameTestHelper helper) {
        int placed = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (placed >= 100) break;
            helper.setBlock(new BlockPos(placed % 10, 1, placed / 10), block);
            // a different block state class can refuse placement (e.g. needs support), so count what really stuck
            if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(placed % 10, 1, placed / 10))).getBlock() == block) placed++;
        }
        BlockPos low = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos high = helper.absolutePos(new BlockPos(9, 1, 9));
        helper.assertTrue(hcCompileMerged1.scan_region(helper.getLevel(), low, high).isEmpty(),
                "more than 91 distinct blocks is refused with an empty result");
        helper.succeed();
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
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(a), Direction.UP, a, false)));
        helper.assertTrue(a.equals(CopyToolData.get(stack, 1)), "sneak + click sets corner 1");

        player.setShiftKeyDown(false);
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(b), Direction.UP, b, false)));
        helper.assertTrue(b.equals(CopyToolData.get(stack, 2)), "a plain click sets corner 2");
        helper.assertTrue(a.equals(CopyToolData.get(stack, 1)), "corner 1 is unchanged by the second click");
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

    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void cornersSurviveSerialization(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModContent.COPY_TOOL.get());
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        CopyToolData.set(stack, 2, pos);
        ItemStack copy = stack.copy();
        helper.assertTrue(pos.equals(CopyToolData.get(copy, 2)), "a copied stack keeps its corner");
        CopyToolData.clear(copy, 2);
        helper.assertTrue(CopyToolData.get(copy, 2) == null, "clear removes it");
        helper.assertTrue(pos.equals(CopyToolData.get(stack, 2)), "and the original is untouched");
        helper.succeed();
    }
}
