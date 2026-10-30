package net.oktawia.structuretokubejsaisles;

import net.oktawia.structuretokubejsaisles.hc.hcCompileMerged1;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The text-to-KubeJS conversion, which needs no Minecraft at all. The server's text is "v2 <slices> <rows> <chars>", a table of
 * "letter=block id" lines, a blank line, then one character per block (slice by slice, row by row) with "=" for the controller.
 */
class ClipboardFormatTest {

    private static String decode(String data, boolean upperFirst, int air, boolean parts, boolean full) {
        // Hot Chocolate's Bool is an int on the Java side
        return hcCompileMerged1.decode_clipboard_with(data, upperFirst ? 1 : 0, air, parts ? 1 : 0, full ? 1 : 0);
    }

    private static final String TWO_SLICES = "v2 2 1 2\na=minecraft:stone\nb=gtceu:casing\n\nabba";

    @Test
    void usesSliceNotAisle() {
        String out = hcCompileMerged1.decode_clipboard(TWO_SLICES);
        assertTrue(out.contains(".slice("), out);
        assertFalse(out.contains(".aisle("), "GTM 8 renamed aisle to slice: " + out);
    }

    @Test
    void writesSlicesInOrderAndOneWherePerLetter() {
        assertEquals(
                ".slice(\"ab\")\n"
                        + ".slice(\"ba\")\n"
                        + "\n"
                        + ".where(\"a\", Predicates.blocks(\"minecraft:stone\"))\n"
                        + ".where(\"b\", Predicates.blocks(\"gtceu:casing\"))\n",
                hcCompileMerged1.decode_clipboard(TWO_SLICES));
    }

    @Test
    void rowsAreRowsOfASlice() {
        // one slice of two rows of three: "abc" and "cba"
        String data = "v2 1 2 3\na=x:one\nb=x:two\nc=x:three\n\nabccba";
        assertTrue(hcCompileMerged1.decode_clipboard(data).startsWith(".slice(\"abc\", \"cba\")\n"));
    }

    @Test
    void lowercaseFirstByDefaultThenUppercase() {
        String out = decode(manyBlocks(28), false, 2, false, false);
        assertTrue(out.contains(".where(\"a\", Predicates.blocks(\"x:b0\"))"), out);
        assertTrue(out.contains(".where(\"z\", Predicates.blocks(\"x:b25\"))"), out);
        assertTrue(out.contains(".where(\"A\", Predicates.blocks(\"x:b26\"))"), "after z comes A: " + out);
        assertTrue(out.contains(".where(\"B\", Predicates.blocks(\"x:b27\"))"), out);
    }

    @Test
    void uppercaseFirstOptionGoesTheOtherWay() {
        String out = decode(manyBlocks(28), true, 2, false, false);
        assertTrue(out.contains(".where(\"A\", Predicates.blocks(\"x:b0\"))"), out);
        assertTrue(out.contains(".where(\"Z\", Predicates.blocks(\"x:b25\"))"), out);
        assertTrue(out.contains(".where(\"a\", Predicates.blocks(\"x:b26\"))"), "after Z comes a: " + out);
        assertTrue(out.contains(".where(\"b\", Predicates.blocks(\"x:b27\"))"), out);
    }

    /** n distinct blocks "x:b0" .. "x:b(n-1)", one per cell, in a single row; the server letters them from its own alphabet. */
    private static String manyBlocks(int n) {
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder table = new StringBuilder();
        StringBuilder cells = new StringBuilder();
        for (int i = 0; i < n; i++) {
            table.append(alphabet.charAt(i)).append("=x:b").append(i).append('\n');
            cells.append(alphabet.charAt(i));
        }
        return "v2 1 1 " + n + "\n" + table + "\n" + cells;
    }

    @Test
    void theControllerIsKAndGetsItsOwnWhereLine() {
        String data = "v2 1 1 3\na=x:one\nb=x:two\n\na=b";
        String out = decode(data, false, 2, false, false);
        assertEquals(
                ".slice(\"aKb\")\n"
                        + "\n"
                        + ".where(\"K\", Predicates.controller(Predicates.blocks(definition.get())))\n"
                        + ".where(\"a\", Predicates.blocks(\"x:one\"))\n"
                        + ".where(\"b\", Predicates.blocks(\"x:two\"))\n",
                out);
    }

    @Test
    void ordinaryBlocksNeverUseTheControllersLetter() {
        // uppercase first: the 11th distinct block would be K, which belongs to the controller
        String out = decode(manyBlocks(12).replace("\n\n", "\n\n=") .replace("v2 1 1 12", "v2 1 1 13"), true, 2, false, false);
        assertTrue(out.contains(".where(\"J\", Predicates.blocks(\"x:b9\"))"), out);
        assertTrue(out.contains(".where(\"L\", Predicates.blocks(\"x:b10\"))"), "K is skipped: " + out);
        assertEquals(1, out.split("\\.where\\(\"K\"", -1).length - 1, out);
    }

    @Test
    void airCanBeASpaceAnAirPredicateOrABlock() {
        String data = "v2 1 1 3\na=minecraft:stone\nb=minecraft:air\n\nabb".replace("abb", "aba");
        String space = decode(data, false, 0, false, false);
        assertTrue(space.startsWith(".slice(\"a a\")\n"), space);
        assertFalse(space.contains("air"), "air as a space needs no where: " + space);

        String predicate = decode(data, false, 1, false, false);
        assertTrue(predicate.startsWith(".slice(\"aba\")\n") || predicate.startsWith(".slice(\"aba\")"), predicate);
        assertTrue(predicate.contains(".where(\"b\", Predicates.air())"), predicate);

        String block = decode(data, false, 2, false, false);
        assertTrue(block.contains(".where(\"b\", Predicates.blocks(\"minecraft:air\"))"), block);
    }

    @Test
    void gtmPartsBecomeAbilitiesThatShareALetter() {
        String data = "v2 1 1 4\na=gtceu:casing\nb=gtceu:lv_input_bus\nc=gtceu:hv_input_bus\nd=gtceu:lv_energy_input_hatch\n\nabcd";
        String out = decode(data, false, 2, true, false);
        assertTrue(out.startsWith(".slice(\"abbc\")\n"), "the two input buses share a letter: " + out);
        assertTrue(out.contains(".where(\"b\", Predicates.abilities(PartAbility.IMPORT_ITEMS)) // gtceu:lv_input_bus"), out);
        assertTrue(out.contains(".where(\"c\", Predicates.abilities(PartAbility.INPUT_ENERGY)) // gtceu:lv_energy_input_hatch"), out);

        String plain = decode(data, false, 2, false, false);
        assertTrue(plain.startsWith(".slice(\"abcd\")\n"), "with the option off every block keeps its own letter: " + plain);
        assertTrue(plain.contains("Predicates.blocks(\"gtceu:lv_input_bus\")"), plain);
    }

    @Test
    void partAbilityNames() {
        assertEquals("IMPORT_ITEMS", hcCompileMerged1.part_abilities("gtceu:lv_input_bus"));
        assertEquals("EXPORT_ITEMS", hcCompileMerged1.part_abilities("gtceu:iv_output_bus"));
        assertEquals("IMPORT_FLUIDS", hcCompileMerged1.part_abilities("gtceu:mv_input_hatch"));
        assertEquals("EXPORT_FLUIDS", hcCompileMerged1.part_abilities("gtceu:mv_output_hatch"));
        assertEquals("INPUT_ENERGY", hcCompileMerged1.part_abilities("gtceu:ev_energy_input_hatch_4a"));
        assertEquals("OUTPUT_ENERGY", hcCompileMerged1.part_abilities("gtceu:ev_energy_output_hatch"));
        assertEquals("MAINTENANCE", hcCompileMerged1.part_abilities("gtceu:maintenance_hatch"));
        assertEquals("IMPORT_ITEMS, IMPORT_FLUIDS", hcCompileMerged1.part_abilities("gtceu:lv_dual_input_hatch"));
        assertEquals("IMPORT_FLUIDS_4X", hcCompileMerged1.part_abilities("gtceu:iv_quadruple_input_hatch"));
        assertEquals("STEAM_IMPORT_ITEMS", hcCompileMerged1.part_abilities("gtceu:steam_input_bus"));
        assertEquals("", hcCompileMerged1.part_abilities("gtceu:solid_machine_casing"));
        assertEquals("", hcCompileMerged1.part_abilities("minecraft:input_bus"));
    }

    @Test
    void fullPatternWrapsEverythingInThePatternCall() {
        String data = "v2 1 1 3\na=x:one\nb=x:two\n\na=b";
        String out = decode(data, false, 2, false, true);
        assertTrue(out.startsWith(".pattern(definition => FactoryBlockPattern.start()\n    .slice(\"aKb\")\n"), out);
        assertTrue(out.contains("\n    .where(\"K\", Predicates.controller(Predicates.blocks(definition.get())))\n"), out);
        assertTrue(out.endsWith("    .build())\n"), out);
        assertFalse(out.contains("no controller was marked"), out);

        String noController = decode(TWO_SLICES, false, 2, false, true);
        assertTrue(noController.contains("// no controller was marked"), noController);
    }

    @Test
    void escapesQuotesAndBackslashes() {
        assertEquals("a\\\"b\\\\c", hcCompileMerged1.js_escape("a\"b\\c"));
    }

    @Test
    void returnsTextThatIsNotAStructureUnchanged() {
        assertSame("just some words", hcCompileMerged1.decode_clipboard("just some words"));
        String oldFormat = "2 2 1\na=x:y\n\naaaa";
        assertEquals(oldFormat, hcCompileMerged1.decode_clipboard(oldFormat));
        String notNumbers = "v2 a b c\nx=y\n\nzzz";
        assertEquals(notNumbers, hcCompileMerged1.decode_clipboard(notNumbers));
        String tooShort = "v2 2 2 2\na=x:y\n\naaa";
        assertEquals(tooShort, hcCompileMerged1.decode_clipboard(tooShort));
    }
}
