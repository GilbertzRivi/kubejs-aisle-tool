module net.oktawia.structruretokubejsaisles.hc;

// A real `java.util.function.Supplier<ServerPlayer>` implementer, needed to target a specific
// player when sending `ClipboardPacket` (see Copytool.hc's `use_item`, which passes
// `PacketDistributor::PLAYER.with(&PlayerSupplier { ... })`) -- `SimpleChannel`'s networking API
// needs a real supplier, not just a bare player reference. Same shape `defs/Items.hc`'s own
// `CopyToolSupplier` already establishes for `DeferredRegister.register`'s `Supplier<T>` param,
// reusing the same shared `JSupplier` (`extern interface` for `java.util.function.Supplier`)
// declared there rather than a second alias for the same real interface.
//
// Kept in its own file rather than inline in Copytool.hc: a file's *first* declared struct
// becomes that file's real "named binary target" for every `pub fn`/`pub static` in the same
// file (see the README) -- putting this small helper struct in Copytool.hc would have silently
// renamed the class every top-level fn there compiles onto.
struct PlayerSupplier { player: ServerPlayer }
impl JSupplier for PlayerSupplier {
    fn get(&self) -> JObject {
        return self.player as JObject;
    }
}
