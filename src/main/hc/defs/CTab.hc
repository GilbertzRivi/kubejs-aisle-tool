module net.oktawia.structruretokubejsaisles.defs;

// The HC-native replacement for defs/CTab.java's creative-mode-tab registration -- previously
// left in Java (see IDEAS.md's now-shipped "Lambdas / closures" entry) purely because every one
// of its four registration calls needed a Supplier/DisplayItemsGenerator lambda, and HC had no
// lambda syntax at all until this compile. `ForgeRegistries`/`IEventBus`/`JSupplier`/
// `DeferredRegister`/`RegistryObject`/`ItemLike`/`ItemStack`/`Component`/`MutableComponent`/
// `COPY_TOOL` are all shared, declared once in Items.hc/McBindings.hc -- this file only declares
// what's genuinely new here: `Registries.CREATIVE_MODE_TAB`'s own `DeferredRegister.create`
// overload (see `DeferredRegisterByKey`'s own doc below for why that needs its own alias name)
// and the `CreativeModeTab` builder chain.

// A second, independent alias for the exact same real `net.minecraftforge.registries.
// DeferredRegister` class Items.hc's own `DeferredRegister` alias already names -- needed
// because the real `DeferredRegister.create` is genuinely overloaded (`(IForgeRegistry<T>,
// String)` and `(ResourceKey<? extends Registry<T>>, String)`, two real, distinct methods), and
// HC's overload resolution is arg-*count*-only (see the compiler's own `pickCandidate` doc):
// declaring both `create` shapes on ONE alias would make the checker always pick whichever was
// declared first, silently wrong for every 2-arg call to the other. Two differently-named
// aliases pointing at the same binary class sidesteps this cleanly -- nothing requires an alias
// name to be unique to one real class, only unique to itself.
extern class DeferredRegisterByKey = "net.minecraftforge.registries.DeferredRegister" {
    // `-> Self` only actually resolves for a `new` constructor (its declared return type is
    // never even read -- see `checkStaticCall`'s ctor branch, which derives it straight from
    // the extern class's own binary name instead); an ordinary static method like this one needs
    // the real alias name spelled out.
    fn create(reg: ResourceKey, modid: String) -> DeferredRegisterByKey;
    fn register(&mut self, name: String, sup: &dyn JSupplier) -> RegistryObject;
    fn register(&mut self, bus: IEventBus);
}

extern class Registries = "net.minecraft.core.registries.Registries" {
    static CREATIVE_MODE_TAB: ResourceKey;
}

// `CreativeModeTab$ItemDisplayParameters` is never actually read inside `displayItems`'s lambda
// below (the original Java ignored it too) -- an empty marker is all its param position needs.
extern class ItemDisplayParameters = "net.minecraft.world.item.CreativeModeTab$ItemDisplayParameters" {}
// `CreativeModeTab$Output` is a real Java interface, not a class -- same `interface` marker
// requirement as every other extern-interface case in this project (see Structruretokubejsaisles
// .hc's identical `Logger` fix's own doc for the exact failure mode this avoids:
// `IncompatibleClassChangeError: Found interface ..., but class was expected`).
extern class CreativeModeTabOutput = "net.minecraft.world.item.CreativeModeTab$Output" interface {
    fn accept(&self, item: ItemLike);
}
extern interface JDisplayItemsGenerator = "net.minecraft.world.item.CreativeModeTab$DisplayItemsGenerator" {
    fn accept(&self, params: ItemDisplayParameters, output: CreativeModeTabOutput);
}

extern class CreativeModeTabBuilder = "net.minecraft.world.item.CreativeModeTab$Builder" {
    // Same "'-> Self' only resolves for 'new'" limitation as `DeferredRegisterByKey::create`
    // above -- these are ordinary (fluent-chaining) instance methods, not constructors.
    fn icon(&mut self, sup: &dyn JSupplier) -> CreativeModeTabBuilder;
    fn title(&mut self, c: Component) -> CreativeModeTabBuilder;
    fn displayItems(&mut self, gen: &dyn JDisplayItemsGenerator) -> CreativeModeTabBuilder;
    fn build(&mut self) -> CreativeModeTab;
}
extern class CreativeModeTab = "net.minecraft.world.item.CreativeModeTab" {
    static fn builder() -> CreativeModeTabBuilder;
}

// `DeferredRegisterByKey::create` only reads an *extern* static field (`Registries::
// CREATIVE_MODE_TAB`, a real, already-compiled Java class) -- safe as a `pub static` regardless
// of file processing order. `COPY_TOOL` (below) is a different story: a `pub static` initializer
// can only see statics from files the compiler has *already* processed (directory-mode compiles
// files in alphabetical-path order -- "CTab.hc" sorts before "Items.hc", where `COPY_TOOL` is
// actually declared), so reading it from a `pub static` initializer here would be a forward
// reference the checker rejects. An ordinary fn body has no such restriction (every fn is type-
// checked in a later pass, after every file's statics are already registered) -- see
// `register_tab` below, which is why the tab-registration call lives there instead of a second
// `pub static` the way the original Java's `STRUCTURETOKUBEJSAISLES_TAB` field did.
pub static CREATIVE_MODE_TABS: DeferredRegisterByKey = DeferredRegisterByKey::create(Registries::CREATIVE_MODE_TAB, "structuretokubejsaisles");

// Not named bare `register` -- top-level `pub fn`s share ONE flat global namespace across every
// file in the compile (no imports, no per-class scoping the way Java's own `CTab.register` vs
// `Items.register` had), and `Items.hc` already declares its own `pub fn register(bus)`. Takes
// `bus` *borrowed* (see Structruretokubejsaisles.hc's own `init()`, which calls this first, then
// passes the same `bus` on to `Items.hc`'s `register(bus)` *owned*, as its last use).
//
// Registers the tab itself (name + Supplier) BEFORE subscribing `CREATIVE_MODE_TABS` to the
// event bus -- matches the original Java's own real ordering (there, the name+Supplier
// registration ran eagerly at class-load, via a `static final` field initializer, strictly
// before Forge ever got the chance to call `register(IEventBus)`; `DeferredRegister.register(
// IEventBus)` subscribes whatever's already in its internal list at the moment it's called, so
// calling it first here would register an empty list).
//
// Three lambdas, one nested inside the outer Supplier's own body alongside a sibling one
// (`icon`'s and `displayItems`'s) -- none of them capture anything (both inner lambdas only
// ever read `COPY_TOOL`, a `static`, which is never treated as a capture -- see
// Checker.checkLambda's own doc), so this is the simple case, not the transitive-capture one.
pub fn register_tab(bus: &IEventBus) {
    CREATIVE_MODE_TABS.register(
        "structruretokubejsaisles_tab",
        || CreativeModeTab::builder()
            .icon(|| ItemStack::new(COPY_TOOL.get() as Item as ItemLike) as JObject)
            .title(Component::translatable("creativetab.structruretokubejsaisles_tab") as Component)
            .displayItems(|params, output| output.accept(COPY_TOOL.get() as Item as ItemLike))
            .build() as JObject,
    );
    CREATIVE_MODE_TABS.register(bus);
}
