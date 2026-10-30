package net.oktawia.structuretokubejsaisles;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.oktawia.structuretokubejsaisles.hc.hcCompileMerged1;

@Mod(ModContent.MOD_ID)
public class AisleToolMod {

    public AisleToolMod(IEventBus modBus) {
        ModContent.COMPONENTS.register(modBus);
        ModContent.ITEMS.register(modBus);
        ModContent.TABS.register(modBus);
        modBus.addListener(AisleToolMod::registerPayloads);
        modBus.addListener(AisleToolMod::addToIngredients);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(ClipboardPayload.TYPE, ClipboardPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> hcCompileMerged1.set_clipboard(payload.data())));
    }

    private static void addToIngredients(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModContent.COPY_TOOL.get());
        }
    }
}
