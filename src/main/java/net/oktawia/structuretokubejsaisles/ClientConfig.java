package net.oktawia.structuretokubejsaisles;

import net.neoforged.neoforge.common.ModConfigSpec;

/** How the KubeJS text is written. Client side only: the text is built on the client from what the server scanned. */
public final class ClientConfig {
    public enum LetterCase {
        /** a to z first, then A to Z (the original behaviour). */
        LOWERCASE_FIRST,
        /** A to Z first, then a to z. */
        UPPERCASE_FIRST
    }

    public enum AirMode {
        /** Air is a space, which GTM reads as "anything goes". */
        SPACE,
        /** Air gets a letter with Predicates.air(), so the spot must really be empty. */
        AIR_PREDICATE,
        /** Air is an ordinary block with Predicates.blocks("minecraft:air"), as the 1.20.1 tool wrote it. */
        BLOCK
    }

    public enum OutputStyle {
        /** Just the .slice and .where lines. */
        FRAGMENT,
        /** The whole .pattern(definition => FactoryBlockPattern.start()...build()) call. */
        FULL_PATTERN
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<LetterCase> LETTER_CASE;
    public static final ModConfigSpec.EnumValue<AirMode> AIR;
    public static final ModConfigSpec.BooleanValue PARTS_AS_ABILITIES;
    public static final ModConfigSpec.EnumValue<OutputStyle> STYLE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        LETTER_CASE = b.comment("Which letters come first when a block needs a character.")
                .defineEnum("letterCase", LetterCase.LOWERCASE_FIRST);
        AIR = b.comment("How air inside the selection is written.")
                .defineEnum("air", AirMode.SPACE);
        PARTS_AS_ABILITIES = b.comment("Write GTM hatches and buses as Predicates.abilities(PartAbility....) instead of the exact block.")
                .define("gtmPartsAsAbilities", true);
        STYLE = b.comment("Copy only the slice and where lines, or the whole .pattern(...) call.")
                .defineEnum("outputStyle", OutputStyle.FRAGMENT);
        SPEC = b.build();
    }

    private ClientConfig() {}

    // The Hot Chocolate code reads the settings through these (plain values, no Java enums on its side).

    public static boolean uppercaseFirst() {
        return LETTER_CASE.get() == LetterCase.UPPERCASE_FIRST;
    }

    /** 0 = space, 1 = Predicates.air(), 2 = ordinary block. */
    public static int airMode() {
        return AIR.get().ordinal();
    }

    public static boolean partsAsAbilities() {
        return PARTS_AS_ABILITIES.get();
    }

    public static boolean fullPattern() {
        return STYLE.get() == OutputStyle.FULL_PATTERN;
    }
}
