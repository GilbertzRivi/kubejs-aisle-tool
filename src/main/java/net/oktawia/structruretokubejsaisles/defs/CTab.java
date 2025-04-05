package net.oktawia.structruretokubejsaisles.defs;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.structruretokubejsaisles.Structruretokubejsaisles;

public class CTab {
    public static final DeferredRegister<CreativeModeTab> CTAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Structruretokubejsaisles.MODID);

    public static final RegistryObject<CreativeModeTab> MOD_TAB = CTAB.register("tutorial_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(Items.COPY_TOOL.get()))
                    .title(Component.translatable("creativetab.tutorial_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(Items.COPY_TOOL.get());
                    })
                    .build());


    public static void register(IEventBus eventBus) {
        CTAB.register(eventBus);
    }
}