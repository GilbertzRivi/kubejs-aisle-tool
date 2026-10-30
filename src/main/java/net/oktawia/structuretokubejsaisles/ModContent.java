package net.oktawia.structuretokubejsaisles;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.structuretokubejsaisles.hc.CopyToolItem;

/** Everything the mod registers: the two corner components, the Copy Tool item and its creative tab. */
public final class ModContent {
    public static final String MOD_ID = "structuretokubejsaisles";

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);

    /** The first corner (sneak + right click). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> POS1 =
            COMPONENTS.registerComponentType("pos1",
                    b -> b.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    /** The second corner (right click). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> POS2 =
            COMPONENTS.registerComponentType("pos2",
                    b -> b.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    /** The marked multiblock controller. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> CONTROLLER =
            COMPONENTS.registerComponentType("controller",
                    b -> b.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    /** Present (true) while right-clicking a block marks the controller instead of a corner. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CONTROLLER_MODE =
            COMPONENTS.registerComponentType("controller_mode",
                    b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredItem<Item> COPY_TOOL =
            ITEMS.register("copy_tool", () -> new CopyToolItem(new Item.Properties().stacksTo(1)));

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("structruretokubejsaisles_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(COPY_TOOL.get()))
                    .title(Component.translatable("creativetab.structruretokubejsaisles_tab"))
                    .displayItems((params, output) -> output.accept(COPY_TOOL.get()))
                    .build());

    private ModContent() {}
}
