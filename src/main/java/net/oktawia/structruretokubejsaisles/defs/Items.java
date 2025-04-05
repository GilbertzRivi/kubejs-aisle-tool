package net.oktawia.structruretokubejsaisles.defs;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.structruretokubejsaisles.Structruretokubejsaisles;
import net.oktawia.structruretokubejsaisles.items.CopyToolItem;

public class Items {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Structruretokubejsaisles.MODID);

    public static final RegistryObject<Item> COPY_TOOL = ITEMS.register("copy_tool",
            () -> new CopyToolItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

