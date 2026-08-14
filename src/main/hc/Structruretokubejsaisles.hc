module net.oktawia.structruretokubejsaisles;

// The HC-native replacement for the mod's root `@Mod`-annotated bootstrap class (previously
// Structruretokubejsaisles.java) -- the last piece of hand-written Forge glue in this mod, made
// portable by HC's new `@entry("forge.mod", ...)` compiler directive: Forge instantiates the
// `@Mod` class via a real no-arg constructor with an actual imperative body (registering the mod
// event bus, ...), unlike `@SubscribeEvent`'s pure method-scanning, so this needed genuinely new
// codegen (a struct's constructor synthesizing a real extra call) rather than reuse of the
// existing annotation-emission machinery alone. `Aisletool`/`NetworkHandler`/`ItemProperties`/
// `JObject` are shared, declared once elsewhere in this same compile (Aisletool.hc/Copytool.hc/
// McBindings.hc) -- no `extern class` bridging needed for this project's *own* compiled classes.
//
// `CTab`/`Items`'s own `register` functions are called with a *borrowed* `IEventBus` first
// (`&bus`), then Items' own bare HC `register(bus: IEventBus)` fn (Items.hc) takes it *owned* as
// the last use -- same borrow-then-move shape as every other "pass the same value to several
// calls, the last one gets to keep it" case in this project. `CTab`'s own `register_tab` is a
// real native HC fn now too (defs/CTab.hc, ported once HC gained lambda literals), called bare
// like `Items.hc`'s own `register` -- no `extern class CTab` bridging needed for this project's
// *own* compiled classes, same as `NetworkHandler`... except `NetworkHandler` genuinely still
// needs one (see Copytool.hc): its `static INSTANCE: SimpleChannel` field is read from here
// indirectly (`NetworkHandler::registerPackets()` below), and an `extern class` alias is still
// the only way this file's own checker pass learns that class's real shape without a circular
// same-file struct/fn declaration of its own.
//
// The two empty-bodied event handlers from the original Java (`onServerStarting`,
// `ClientModEvents.onClientSetup`) are deliberately NOT ported -- they were real no-ops with zero
// behavior, so there's nothing to port; replicating an empty stub would just be ceremony.

extern class FMLJavaModLoadingContext = "net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext" {
    static fn get() -> FMLJavaModLoadingContext;
    fn getModEventBus(&self) -> IEventBus;
}

extern class FMLCommonSetupEvent = "net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent" {}

// `org.slf4j.Logger` is a real Java interface, not a class -- without the `interface` marker
// its `.info(...)` call compiles to INVOKEVIRTUAL instead of the INVOKEINTERFACE the JVM
// verifier actually requires, throwing `IncompatibleClassChangeError: Found interface
// org.slf4j.Logger, but class was expected` the moment it's linked at runtime (compiles and
// `javap`-verifies clean either way -- same failure mode this project's other `extern class`
// interface markers already document, e.g. Component/DefaultedRegistry in McBindings.hc).
extern class Logger = "org.slf4j.Logger" interface {
    fn info(&self, msg: String);
}
extern class LogUtils = "com.mojang.logging.LogUtils" {
    static fn getLogger() -> Logger;
}

extern class ResourceKey = "net.minecraft.resources.ResourceKey" {}
extern class CreativeModeTabs = "net.minecraft.world.item.CreativeModeTabs" {
    static INGREDIENTS: ResourceKey;
}

// `BuildCreativeModeTabContentsEvent.accept(Supplier<? extends ItemLike>)` -- the overload the
// original Java's `event.accept(Items.COPY_TOOL)` actually resolves to (`RegistryObject<T>
// implements Supplier<T>`, verified via `javap`); the erased param type really is
// `java.util.function.Supplier`, not `java.lang.Object` (the bounded wildcard type argument
// erases away, but `Supplier` itself doesn't). Distinct from `defs/Items.hc`'s own `JSupplier`
// (an `extern interface`, for an HC struct to *implement* Supplier) -- this is the other
// direction, a real `extern class` for *calling into* a value that already is one. The ONE such
// alias for this binary name in the whole compile -- `checkMethodCall`'s extern-method
// resolution matches by binary name across every registered `extern class`, not by which alias
// a given call site actually used, so a second `extern class X = "...Supplier" { ... }`
// declared with a different (even a strict superset) method table anywhere else in this
// project would silently and non-deterministically shadow this one's `get()` depending on file
// processing order -- learned the hard way porting `ClipboardPacketHandler.hc`'s own `ctx.get()`
// call, which is why it reuses this alias (see network/NetworkHandler.hc's own `RawSupplier`
// removal) instead of declaring its own.
extern class JSupplierValue = "java.util.function.Supplier" interface {
    fn get(&self) -> JObject;
}
extern class BuildCreativeModeTabContentsEvent = "net.minecraftforge.event.BuildCreativeModeTabContentsEvent" {
    fn getTabKey(&self) -> ResourceKey;
    fn accept(&self, item: &JSupplierValue);
}

@entry("forge.mod", modid: "structuretokubejsaisles")
pub fn init() {
    let bus = FMLJavaModLoadingContext::get().getModEventBus();
    register_tab(&bus);
    register(bus);
    NetworkHandler::registerPackets();
}

// `@Mod.EventBusSubscriber` here (alongside `@entry` above, which adds its own separate `@Mod`
// annotation at codegen time -- see CodeGen's `generate()`) is what makes Forge actually scan
// this class for the two `@SubscribeEvent`-annotated static fns below; without it they'd compile
// fine and simply never fire. `bus: MOD` since both fire on the mod event bus (matching the
// original Java's `modEventBus.addListener(...)`, not `MinecraftForge.EVENT_BUS`) -- no `value:
// [...]` Dist restriction, matching the original's own lack of one (both fire on every side).
@"net.minecraftforge.fml.common.Mod$EventBusSubscriber"(
    modid: "structuretokubejsaisles",
    bus: enum("net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus", "MOD"),
)
pub struct Structruretokubejsaisles {}

@"net.minecraftforge.eventbus.api.SubscribeEvent"
pub fn on_common_setup(event: &FMLCommonSetupEvent) {
    let greeting = greet("Structure To KubeJs Aisles");
    let bonus = bonusCount(8);
    LogUtils::getLogger().info(greeting);
    LogUtils::getLogger().info("[Hot Chocolate] bonusCount(8) = {bonus}");
}

@"net.minecraftforge.eventbus.api.SubscribeEvent"
pub fn on_add_creative(event: &BuildCreativeModeTabContentsEvent) {
    if event.getTabKey() == CreativeModeTabs::INGREDIENTS {
        event.accept(&(COPY_TOOL as JSupplierValue));
    }
}
