module net.oktawia.structruretokubejsaisles.defs;

// The HC-native replacement for defs/Items.java's registry-glue shell. Compiles to a real
// `net.oktawia.structruretokubejsaisles.defs.Items` class -- the module's *first* declared
// struct (`Items` itself, zero fields, just a namespace to hang statics/fns off of) is what its
// top-level `pub static`/`pub fn` land on (see HotChocolate's README, "Named binary target for
// module-level pub fn/pub static" era -- IDEAS.md's entry for this got resolved into that
// feature), so Java code's existing `import ...defs.Items; Items.COPY_TOOL`/`Items.register(bus)`
// keeps working completely unchanged.
//
// `DeferredRegister.register(String, Supplier<T>)` is a real Java functional-interface param --
// no lambdas in this language yet, so `CopyToolSupplier` (below) is a real struct implementing
// the real `java.util.function.Supplier` via `extern interface`, exactly the same shape
// `examples/extern_interface.hc`'s `Runnable` example uses.

pub struct Items {}

// `ItemProperties`/`JObject` are shared, declared once in McBindings.hc. `CopyToolItem` needs no
// extern declaration at all anymore -- it's a real struct in this same compile (items/
// CopyToolItem.hc), and `CopyToolItem::new(...)` already resolves via the same auto-derived
// `extends Item` constructor mechanism a hand-written extern declaration used to just redeclare.

extern class IForgeRegistry = "net.minecraftforge.registries.IForgeRegistry" interface {}

// `ForgeRegistries.ITEMS` is a real `public static final` field, not a method -- read via
// `Alias::FIELD` (see the README's "extern class static fields" section).
extern class ForgeRegistries = "net.minecraftforge.registries.ForgeRegistries" {
    static ITEMS: IForgeRegistry;
}

extern class IEventBus = "net.minecraftforge.eventbus.api.IEventBus" interface {}

extern interface JSupplier = "java.util.function.Supplier" {
    fn get(&self) -> JObject;
}

extern class RegistryObject = "net.minecraftforge.registries.RegistryObject" {
    fn get(&self) -> JObject;
}

extern class DeferredRegister = "net.minecraftforge.registries.DeferredRegister" {
    fn create(reg: IForgeRegistry, modid: String) -> DeferredRegister;
    fn register(&mut self, name: String, sup: &dyn JSupplier) -> RegistryObject;
    fn register(&mut self, bus: IEventBus);
}

struct CopyToolSupplier {}
impl JSupplier for CopyToolSupplier {
    fn get(&self) -> JObject {
        return CopyToolItem::new(ItemProperties::new()) as JObject;
    }
}

pub static ITEMS: DeferredRegister = DeferredRegister::create(ForgeRegistries::ITEMS, "structuretokubejsaisles");
pub static COPY_TOOL: RegistryObject = ITEMS.register("copy_tool", &CopyToolSupplier {});

pub fn register(bus: IEventBus) {
    ITEMS.register(bus);
}
