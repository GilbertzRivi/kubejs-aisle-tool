package net.oktawia.structuretokubejsaisles;

import net.oktawia.structuretokubejsaisles.hc.hcCompileMerged1;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The text-to-KubeJS conversion, which needs no Minecraft at all. */
class ClipboardFormatTest {

    // Header is "size_x size_z size_y"; then the letter table; a blank line; then one letter per block (z, then y, then x from the far side).
    private static final String TWO_BY_ONE_BY_TWO = """
            2 2 1
            a=minecraft:stone
            b=gtceu:casing

            abba""";

    @Test
    void usesSliceNotAisle() {
        String out = hcCompileMerged1.decode_clipboard(TWO_BY_ONE_BY_TWO);
        assertTrue(out.contains(".slice("), out);
        assertFalse(out.contains(".aisle("), "GTM 8 renamed aisle to slice: " + out);
    }

    @Test
    void writesOneSlicePerLayerAndOneWherePerLetter() {
        String out = hcCompileMerged1.decode_clipboard(TWO_BY_ONE_BY_TWO);
        assertEquals(
                ".slice(\"ba\")\n"
                        + ".slice(\"ab\")\n"
                        + "\n"
                        + ".where(\"a\", Predicates.blocks(\"minecraft:stone\"))\n"
                        + ".where(\"b\", Predicates.blocks(\"gtceu:casing\"))\n",
                out);
    }

    @Test
    void layersComeOutTopFirst() {
        // three layers of z with one row each: the data lists them in order and the last layer is printed first
        String data = "1 3 1\na=x:one\nb=x:two\nc=x:three\n\nabc";
        String out = hcCompileMerged1.decode_clipboard(data);
        assertTrue(out.startsWith(".slice(\"c\")\n.slice(\"b\")\n.slice(\"a\")\n"), out);
    }

    @Test
    void escapesQuotesAndBackslashesInLetters() {
        String data = "1 1 1\n\"=x:quote\n\n\"";
        String out = hcCompileMerged1.decode_clipboard(data);
        assertTrue(out.contains(".slice(\"\\\"\")"), out);
        assertTrue(out.contains(".where(\"\\\"\", Predicates.blocks(\"x:quote\"))"), out);
    }

    @Test
    void returnsTextThatIsNotAStructureUnchanged() {
        assertSame("just some words", hcCompileMerged1.decode_clipboard("just some words"));
        String notNumbers = "a b c\nx=y\n\nzzz";
        assertEquals(notNumbers, hcCompileMerged1.decode_clipboard(notNumbers));
        String tooShort = "2 2 2\na=x:y\n\naaa";
        assertEquals(tooShort, hcCompileMerged1.decode_clipboard(tooShort));
    }

    @Test
    void jsEscape() {
        assertEquals("a\\\"b\\\\c", hcCompileMerged1.js_escape("a\"b\\c"));
    }
}
