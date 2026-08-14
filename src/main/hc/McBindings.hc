module net.oktawia.structruretokubejsaisles.hc;

// Shared Minecraft/Forge `extern class` declarations, consolidated out of the individual files
// that used to each hand-redeclare their own (often inconsistent, sometimes incomplete -- see
// HotChocolate's IDEAS.md for the real mistakes this was already causing, e.g. `UseOnContext
// .getPlayer()` declared non-nullable in one file and correctly nullable in another for the
// exact same real JVM method). One declaration per real JVM type, the union of every method/
// field any file in this project actually needs, instead of N slightly-different copies.
//
// Only reachable at all because this project now compiles as one directory-mode `sourceDir`
// (see build.gradle's `hotChocolate { }` block and HotChocolate's README, "Multi-file projects")
// -- every other file in `src/main/hc/` sees these declarations directly, no per-file
// redeclaration and no `extern class Foo = "already.compiled.Foo" { ... }` bridging needed for
// this project's *own* compiled classes either (Copytool/Aisletool/CopyToolItem/Items/RenderUtil
// are all just referenced by their real names directly now).

// --- java.lang / java.util -------------------------------------------------------------------

extern class JMath = "java.lang.Math" {
    fn sin(x: Double) -> Double;
    static fn min(a: Int, b: Int) -> Int;
    static fn max(a: Int, b: Int) -> Int;
}

extern class System = "java.lang.System" {
    fn currentTimeMillis() -> Long;
}

extern class StringBuilder = "java.lang.StringBuilder" {
    fn new() -> Self;
    fn append(&mut self, text: String) -> StringBuilder;
    fn toString(&self) -> String;
}

extern class JObject = "java.lang.Object" {}

// `charAt` is deliberately NOT declared here: `String.charAt(int)` really returns Java `char`
// (JVM descriptor `C`), and HC has no `Char` type to declare that return as -- claiming `-> Int`
// (descriptor `I`) type-checks fine but crashes at runtime with `NoSuchMethodError:
// 'int java.lang.String.charAt(int)'` the moment it's actually called (verified). Use
// `.substring(i, i+1)` for a single character instead -- real `String`, no descriptor mismatch.
extern class JString = "java.lang.String" {
    static fn valueOf(c: Int) -> String;
    fn length(&self) -> Int;
    fn split(&self, regex: String) -> [String];
    // The 2-arg overload, `limit`, is genuinely needed (not just available) in two places
    // Clipboard.hc's decode_clipboard uses it: `-1` on the line-split preserves trailing empty
    // lines the same way the original Java's `data.split("\n", -1)` did, and `2` on the
    // mapping-entry split stops a block-state value containing its own `=` (e.g.
    // `furnace[facing=north]`) from getting incorrectly split into more than 2 parts.
    fn split(&self, regex: String, limit: Int) -> [String];
    fn trim(&self) -> String;
    fn isEmpty(&self) -> Bool;
    fn substring(&self, start: Int, end: Int) -> String;
    // `equals(Object)`, not `equals(String)` -- real value equality, unlike this language's own
    // `==` on two `String`s (reference identity -- see the compiler's own `IF_ACMPEQ` codegen
    // fix). Needed wherever a compared string genuinely might not be the same interned instance
    // (e.g. a protocol-version string that just came back over the network) -- `==` would be
    // silently wrong there, not just less idiomatic.
    fn equals(&self, other: JObject) -> Bool;
}

// --- Minecraft client --------------------------------------------------------------------------

extern class Minecraft = "net.minecraft.client.Minecraft" {
    static fn getInstance() -> Minecraft;
    fn getWindow(&self) -> Window;
    fn renderBuffers(&self) -> RenderBuffers;
    fn getToasts(&self) -> ToastComponent;
    player: LocalPlayer?;
    level: ClientLevel?;
    font: Font;
    gameRenderer: GameRenderer;
    keyboardHandler: KeyboardHandler;
}
extern class KeyboardHandler = "net.minecraft.client.KeyboardHandler" {
    fn setClipboard(&self, text: String);
}
extern class ToastComponent = "net.minecraft.client.gui.components.toasts.ToastComponent" {
    fn addToast(&self, toast: SystemToast);
}
extern class SystemToastIds = "net.minecraft.client.gui.components.toasts.SystemToast$SystemToastIds" {
    static TUTORIAL_HINT: SystemToastIds;
}
extern class SystemToast = "net.minecraft.client.gui.components.toasts.SystemToast" {
    fn new(id: SystemToastIds, title: Component, msg: Component) -> Self;
}
extern class LocalPlayer = "net.minecraft.client.player.LocalPlayer" {
    fn getMainHandItem(&self) -> ItemStack;
}
extern class ClientLevel = "net.minecraft.client.multiplayer.ClientLevel" {}
extern class Window = "com.mojang.blaze3d.platform.Window" {
    fn getGuiScaledWidth(&self) -> Int;
    fn getGuiScaledHeight(&self) -> Int;
}
extern class Font = "net.minecraft.client.gui.Font" {
    fn width(&self, s: String) -> Int;
}
extern class GuiGraphics = "net.minecraft.client.gui.GuiGraphics" {
    // Really returns `int` (the rightmost x position after the drawn text), not `void` --
    // declared `-> Int` here to match the real method's descriptor exactly (verified via `javap`
    // against the real mapped Forge jar); a wrong return type here compiles fine and only fails
    // with `NoSuchMethodError` once this is actually called (same failure mode as any other
    // wrong `extern class` signature -- see the README's Java interop section). Every call site
    // in this project discards the return value, same as if it were declared `void`.
    fn drawString(&mut self, font: Font, s: String, x: Int, y: Int, color: Int, dropShadow: Bool) -> Int;
}

// --- Minecraft world/item/entity ----------------------------------------------------------------

extern class ItemProperties = "net.minecraft.world.item.Item$Properties" {
    fn new() -> Self;
}
extern class Item = "net.minecraft.world.item.Item" {
    fn new(props: ItemProperties) -> Self;
    fn use(&self, level: Level, player: Player, hand: InteractionHand) -> InteractionResultHolder;
    fn useOn(&self, context: UseOnContext) -> InteractionResult;
    fn appendHoverText(&self, stack: ItemStack, level: Level, tooltip: JList, flag: TooltipFlag);
}
// `Item implements ItemLike`, but HC tracks no subtyping between two different `extern class`
// aliases even when the real classes have one (same limitation `MutableComponent`/`Component`
// already documents) -- callers cast explicitly with `as ItemLike`.
extern class ItemLike = "net.minecraft.world.level.ItemLike" interface {}
extern class ItemStack = "net.minecraft.world.item.ItemStack" {
    fn new(item: ItemLike) -> Self;
    fn getItem(&self) -> Item;
    fn getTag(&self) -> CompoundTag;
    fn getOrCreateTag(&mut self) -> CompoundTag;
}
extern class TooltipFlag = "net.minecraft.world.item.TooltipFlag" interface {}
extern class JList = "java.util.List" interface {
    fn add(&mut self, item: JObject) -> Bool;
}

extern class Level = "net.minecraft.world.level.Level" {
    fn isClientSide(&self) -> Bool;
    fn getBlockState(&self, pos: BlockPos) -> BlockState;
}
extern class Player = "net.minecraft.world.entity.player.Player" {
    fn getItemInHand(&self, hand: InteractionHand) -> ItemStack;
    fn displayClientMessage(&self, msg: Component, actionBar: Bool);
}
extern class ServerPlayer = "net.minecraft.server.level.ServerPlayer" {}
extern class InteractionHand = "net.minecraft.world.InteractionHand" {}

// `success`/`fail` are really `static <T> InteractionResultHolder<T> success(T)`/`fail(T)` --
// generic, so `T` erases to `Object` at the real bytecode level (verified via `javap`: the real
// descriptor is `(Ljava/lang/Object;)Lnet/minecraft/world/InteractionResultHolder;`, not
// `(Lnet/minecraft/world/item/ItemStack;)...`) -- declaring the param as plain `ItemStack` here
// shipped a real `NoSuchMethodError` the first time either was actually called. `&JObject`, not
// owned `JObject`: Java has no ownership transfer here (it just reads the reference), and
// declaring it borrowed changes nothing about the actual bytecode -- it just lets a caller pass
// `&stack` from inside a branch that also returns early without poisoning `stack` as moved for
// the rest of the function, the way an owned param would. Call sites cast the real `ItemStack`
// value to `JObject` first (`&stack as JObject`) -- the same reference-cast bridge `List<Component>
// .add(Object)` already needs (see `CopyToolItem.hc`'s `appendHoverText`) for the same reason:
// this is exactly what a generic Java API's erased `Object` parameter always needs from HC.
extern class InteractionResultHolder = "net.minecraft.world.InteractionResultHolder" {
    fn success(obj: &JObject) -> InteractionResultHolder;
    fn fail(obj: &JObject) -> InteractionResultHolder;
}
// `SUCCESS`/`PASS` are real static *fields* (enum constants), not methods -- declaring them as
// `fn SUCCESS() -> InteractionResult;` compiled clean and threw `NoSuchMethodError` the moment
// either was actually called (verified via `javap` against the real mapped Forge jar).
extern class InteractionResult = "net.minecraft.world.InteractionResult" {
    static SUCCESS: InteractionResult;
    static PASS: InteractionResult;
}
// `getPlayer()` really can return null (a non-player use context, e.g. a dispenser) -- declared
// nullable here consistently, unlike an earlier duplicate declaration elsewhere in this project
// that had it wrong (non-nullable) simply because that file's own code never happened to hit the
// null case.
extern class UseOnContext = "net.minecraft.world.item.context.UseOnContext" {
    fn getPlayer(&self) -> Player?;
    fn getItemInHand(&self) -> ItemStack;
    fn getClickedPos(&self) -> BlockPos;
    fn isSecondaryUseActive(&self) -> Bool;
    fn getLevel(&self) -> Level;
}

extern class BlockPos = "net.minecraft.core.BlockPos" {
    fn new(x: Int, y: Int, z: Int) -> Self;
    static fn of(packedPos: Long) -> BlockPos;
    fn asLong(&self) -> Long;
    fn getX(&self) -> Int;
    fn getY(&self) -> Int;
    fn getZ(&self) -> Int;
    fn offset(&self, dx: Int, dy: Int, dz: Int) -> BlockPos;
}
extern class BlockState = "net.minecraft.world.level.block.state.BlockState" {
    fn getBlock(&self) -> Block;
}
extern class Block = "net.minecraft.world.level.block.Block" {}
// `BLOCK` is a real static field of type `DefaultedRegistry<Block>` (erased `DefaultedRegistry`),
// not `Registry` (which `DefaultedRegistry` merely extends) and not a method -- `GETSTATIC`
// needs the exact declared field type, and `static fn BLOCK() -> Registry;` compiled clean and
// threw `NoSuchFieldError`/`NoSuchMethodError` the moment it was actually called (verified via
// `javap`). `DefaultedRegistry` is itself a real interface (extends `Registry`, also an
// interface) -- needs its own `getKey` declared here (HC tracks no subtyping between two
// different `extern class` aliases, so `Registry`'s method isn't inherited even though the real
// interfaces are). `getKey(T)` is generic, erasing to `Object`, same as `Map.Entry`/`Iterator`
// elsewhere in this project -- callers cast the `Block` argument with `as JObject`.
extern class BuiltInRegistries = "net.minecraft.core.registries.BuiltInRegistries" {
    static BLOCK: DefaultedRegistry;
}
extern class DefaultedRegistry = "net.minecraft.core.DefaultedRegistry" interface {
    fn getKey(&self, value: &JObject) -> ResourceLocation;
}
extern class ResourceLocation = "net.minecraft.resources.ResourceLocation" {
    fn new(namespace: String, path: String) -> Self;
    fn toString(&self) -> String;
}

// `Component.literal(String)` really returns `MutableComponent` (a real class implementing
// `Component`, not `Component` itself) -- verified via `javap` after this declaration's wrong
// return type shipped a real `NoSuchMethodError` (the JVM matches a static method by its exact
// descriptor, return type included; there's no covariant-return matching at the bytecode level
// the way `MutableComponent` javac-source-level widening to `Component` would suggest). Callers
// that need a `Component` (not a `MutableComponent`) cast explicitly with `as Component` -- HC
// tracks no subtyping between two different `extern class` aliases even when the real classes
// have one, so this is a real, required cast, not decoration.
extern class MutableComponent = "net.minecraft.network.chat.MutableComponent" {}

extern class Component = "net.minecraft.network.chat.Component" interface {
    static fn literal(text: String) -> MutableComponent;
    static fn translatable(key: String) -> MutableComponent;
}

// --- Networking ------------------------------------------------------------------------------

// Only the two methods `@serializable`'s generated encode/decode actually call (see
// network/ClipboardPacket.hc) -- not a full binding of every `FriendlyByteBuf` read/write method.
// `writeUtf` really returns `FriendlyByteBuf` itself (fluent chaining), not `void` -- verified
// via `javap` against the real mapped Forge jar after two other wrong-return-type declarations
// elsewhere in this file shipped real `NoSuchMethodError`s (see CopyToolHudOverlay.hc's history)
// made this worth double-checking here too. `@serializable`'s generated `encode` calls this as a
// bare statement and discards the return value either way, so declaring the real type changes
// nothing observable -- it's purely about matching the real descriptor. NOTE for anyone adding
// more fields later: `writeInt`/`writeLong`/`writeFloat`/`writeDouble`/`writeBoolean` are NOT
// like `writeUtf` here -- they return `io.netty.buffer.ByteBuf` (inherited from `ByteBuf`, a
// completely different type from `FriendlyByteBuf`), which would need its own `extern class`
// declared as their return type if any of them get added to this binding.
extern class FriendlyByteBuf = "net.minecraft.network.FriendlyByteBuf" {
    fn writeUtf(self, v: String) -> FriendlyByteBuf;
    fn readUtf(self) -> String;
}

extern class CompoundTag = "net.minecraft.nbt.CompoundTag" {
    fn contains(&self, key: String) -> Bool;
    fn getInt(&self, key: String) -> Int;
    fn getLong(&self, key: String) -> Long;
    fn putInt(&mut self, key: String, value: Int);
    fn putLong(&mut self, key: String, value: Long);
    fn remove(&mut self, key: String);
}

// --- Position-tag helpers on an ItemStack (CopyToolItem's own persisted corner selections) ----
// Shared between CopyToolHudOverlay.hc and CopyToolSelectionRender.hc -- was two byte-for-byte
// identical private copies before this file existed, a real duplicate-name collision waiting to
// happen the moment both compiled together (which directory mode now does).

// `pub` is load-bearing here, not decorative: called from CopyToolHudOverlay.hc/
// CopyToolSelectionRender.hc, both in the `client` module -- a different package from this
// file's own `net.oktawia.structruretokubejsaisles.hc`, so a non-`pub` (package-private) fn here
// would be a real cross-package access violation, not just a visibility nicety.
pub fn get_pos_from_stack(stack: &ItemStack, key: String) -> BlockPos? {
    if !has_pos(&stack, key) {
        return null;
    }
    return get_pos_raw(&stack, key);
}
