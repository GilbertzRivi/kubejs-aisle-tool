module net.oktawia.structruretokubejsaisles.network;

// The HC-native replacement for network/NetworkHandler.java -- previously left in Java (see
// IDEAS.md's now-shipped "Lambdas / closures" and "Java class-literal syntax" entries) because
// `newSimpleChannel`/`registerMessage` together needed three real Java `Supplier`/`Predicate`/
// `BiConsumer`/`Function` lambdas plus a `ClipboardPacket.class` literal, and HC had neither
// feature until this compile. `SimpleChannel`/`JBiConsumer`/`JFunction`/`JClass`/`JSupplier`/
// `ResourceLocation` are all shared, declared once in Copytool.hc/Items.hc/McBindings.hc -- this
// file only declares what's genuinely new: `NetworkRegistry.newSimpleChannel` itself, the
// `Predicate` functional interface its version-check params need, and a binding for the
// still-Java `ClipboardPacketHandler.handle` (its own file stays hand-written Java: the
// oversized-output file-write fallback it needs, `Files.writeString(Path, CharSequence, Charset,
// OpenOption...)`, is varargs -- a real, separate HC gap this compile doesn't close, unlike
// lambdas/class-literals).

extern interface JPredicate = "java.util.function.Predicate" {
    fn test(&self, t: JObject) -> Bool;
}

extern class NetworkRegistry = "net.minecraftforge.network.NetworkRegistry" {
    fn newSimpleChannel(name: ResourceLocation, versionSupplier: &dyn JSupplier, clientAccepted: &dyn JPredicate, serverAccepted: &dyn JPredicate) -> SimpleChannel;
}

// `ClipboardPacketHandler.handle`'s second param is already a real `Supplier` value handed in by
// Forge -- reuses `JSupplierValue` (declared once, Structruretokubejsaisles.hc) rather than a
// second `extern class` alias for the same binary name: `checkMethodCall`'s extern-method
// resolution matches by binary name across every registered `extern class`, not by which alias a
// given call site used, so two aliases for the same real class with different method tables
// silently shadow each other (whichever registers first wins, everywhere) instead of erroring --
// see `JSupplierValue`'s own doc for how this was actually found.
extern class ClipboardPacketHandler = "net.oktawia.structruretokubejsaisles.network.ClipboardPacketHandler" {
    static fn handle(packet: ClipboardPacket, ctx: JSupplierValue);
}

pub static PROTOCOL_VERSION: String = "1";

pub static INSTANCE: SimpleChannel = NetworkRegistry::newSimpleChannel(
    ResourceLocation::new("structuretokubejsaisles", "main"),
    || PROTOCOL_VERSION as JObject,
    |s| PROTOCOL_VERSION.equals(s),
    |s| PROTOCOL_VERSION.equals(s),
);

// Called once from the mod's own bootstrap -- see Structruretokubejsaisles.hc's `@entry`.
// `encode`/`decode` are ClipboardPacket.hc's own `@serializable`-synthesized top-level fns, not
// extern declarations -- ordinary bare-name fn calls, exactly like calling any other fn this
// same compile declares. Registers exactly the one message id (0) the original Java did (its own
// `int id = 0; ...; id++` post-increment was likewise only ever used once).
pub fn registerPackets() {
    INSTANCE.registerMessage(
        0,
        ClipboardPacket.class,
        |packet, buf| encode(&(packet as ClipboardPacket), &mut (buf as FriendlyByteBuf)),
        |buf| decode(&mut (buf as FriendlyByteBuf)) as JObject,
        |packet, ctx| ClipboardPacketHandler::handle(packet as ClipboardPacket, ctx as JSupplierValue),
    );
}
