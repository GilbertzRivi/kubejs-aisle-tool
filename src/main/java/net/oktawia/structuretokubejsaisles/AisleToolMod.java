package net.oktawia.structuretokubejsaisles;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.oktawia.structuretokubejsaisles.hc.hcCompileMerged1;

@Mod(ModContent.MOD_ID)
public class AisleToolMod {

    public AisleToolMod(IEventBus modBus, ModContainer container) {
        ModContent.COMPONENTS.register(modBus);
        ModContent.ITEMS.register(modBus);
        ModContent.TABS.register(modBus);
        modBus.addListener(AisleToolMod::registerPayloads);
        modBus.addListener(AisleToolMod::addToIngredients);

        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(ClipboardPayload.TYPE, ClipboardPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> hcCompileMerged1.set_clipboard(payload.data())));
        registrar.playToServer(ToolActionPayload.TYPE, ToolActionPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        ToolActionPayload.apply(player, payload.action());
                    }
                }));
    }

    private static void addToIngredients(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModContent.COPY_TOOL.get());
        }
    }
}
