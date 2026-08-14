module net.oktawia.structruretokubejsaisles.hc;

// `put`/`get`/`containsKey` are generic (`HashMap<K,V>`, which `LinkedHashMap` doesn't override
// these on), erasing to `Object` at the bytecode level exactly like `Clipboard.hc`'s own header
// comment already warned about for this exact class -- that lesson just hadn't been applied
// here yet. Declaring them with concrete `String` params/return compiled clean and would have
// thrown `NoSuchMethodError` the moment the "copy blocks" action actually ran (verified via
// `javap` against `java.util.HashMap`). Callers cast with `as JObject`/`as String` at each site.
extern class LinkedHashMap = "java.util.LinkedHashMap" {
    fn new() -> Self;
    fn put(&mut self, key: &JObject, value: &JObject) -> JObject;
    fn get(&self, key: &JObject) -> JObject;
    fn containsKey(&self, key: &JObject) -> Bool;
    fn entrySet(&self) -> Set;
}

// `interface` on `Set`/`Iterator`/`Entry`/`JSupplier`-adjacent types below (and `JList`/
// `Registry`/`Component`/`TooltipFlag`/`DefaultedRegistry` in McBindings.hc) is load-bearing, not
// decorative: these are genuine Java `interface`s, not classes, and without this marker their
// instance methods compile to INVOKEVIRTUAL (illegal against an interface-declared method -- the
// JVM verifier requires INVOKEINTERFACE) instead of erroring at compile time or even at `javap`
// time, only failing once the JVM actually *links* the call (`IncompatibleClassChangeError: ...
// must be InterfaceMethodref constant`) -- see ExternClassDecl's own doc in the HotChocolate
// compiler for the full mechanism.
extern class Set = "java.util.Set" interface {
    fn iterator(&self) -> Iterator;
}

extern class Iterator = "java.util.Iterator" interface {
    // `next()` is `Iterator<E>.next()`, generic, erasing to `Object` -- same reasoning as
    // `LinkedHashMap` above. Callers cast the result with `as Entry`.
    fn hasNext(&self) -> Bool;
    fn next(&self) -> JObject;
}

extern class Entry = "java.util.Map$Entry" interface {
    // `Map.Entry<K,V>.getKey()`/`getValue()` are also generic, erasing to `Object` -- same
    // reasoning again. Callers cast the result with `as String`.
    fn getKey(&self) -> JObject;
    fn getValue(&self) -> JObject;
}

// `SimpleChannel`/`NetworkHandler`/`PacketDistributor` below were badly wrong on every axis
// (verified via `javap` and the real hand-written `NetworkHandler.java`):
// - `NetworkHandler.INSTANCE` is a real static *field* of type `SimpleChannel`, not a method
//   returning `NetworkHandler` itself -- `send` isn't even declared on `NetworkHandler` at all,
//   it's `SimpleChannel.send(PacketDistributor$PacketTarget, MSG)` (`MSG` erased to `Object`).
// - `PacketDistributor.PLAYER` is a static *field*, not a method, and `.with(Supplier<T>)`
//   returns a `PacketDistributor$PacketTarget`, not another `PacketDistributor` -- `send` needs
//   an actual target (via `.with(...)`), not the bare distributor.
// `with` needs a real `java.util.function.Supplier` implementer to know *which* player to target
// -- reuses `JSupplier` (declared once, `defs/Items.hc`) the same way `CopyToolSupplier` already
// does there, rather than redeclaring another `extern class Supplier` alias for the same interface.
// `BiConsumer<T,U>.accept`/`Function<T,R>.apply` erase to `(Object,Object)V`/`(Object)Object` no
// matter what `T`/`U`/`R` are instantiated with at any one call site -- same generic-erasure
// reasoning as `JSupplier` above, just for `SimpleChannel.registerMessage`'s remaining three
// functional params (see network/NetworkHandler.hc, the one real caller of `registerMessage`).
extern interface JBiConsumer = "java.util.function.BiConsumer" {
    fn accept(&self, a: JObject, b: JObject);
}
extern interface JFunction = "java.util.function.Function" {
    fn apply(&self, t: JObject) -> JObject;
}
// `Class<MSG>` (the real second param of `registerMessage`) erases to plain `Class`, NOT
// `Object` -- unlike a bare type-variable occurrence, a concrete generic class parameterized BY
// a type variable (`Class<MSG>`, not `MSG` itself) keeps its own raw erasure. `Type.class`
// literal syntax (see the compiler's own `Expr.ClassLit`) produces exactly this type.
extern class JClass = "java.lang.Class" {}

// `registerMessage`'s real return type is `IndexedMessageCodec$MessageHandler<MSG>` (erased to
// the raw `MessageHandler`), NOT `void` -- verified via `javap` against the real mapped Forge
// jar after declaring it `void` shipped a real `NoSuchMethodError` (the JVM matches a method by
// its *entire* descriptor, return type included, same failure mode as any other wrong-signature
// `extern class` declaration in this project). The original Java discards the return value as a
// bare statement anyway; this alias exists purely to get the descriptor right, never called into.
extern class MessageHandler = "net.minecraftforge.network.simple.IndexedMessageCodec$MessageHandler" {}

extern class SimpleChannel = "net.minecraftforge.network.simple.SimpleChannel" {
    fn send(&self, target: PacketDistributorTarget, packet: &JObject);
    fn registerMessage(&self, id: Int, msgType: JClass, encoder: &dyn JBiConsumer, decoder: &dyn JFunction, handler: &dyn JBiConsumer) -> MessageHandler;
}
extern class NetworkHandler = "net.oktawia.structruretokubejsaisles.network.NetworkHandler" {
    static INSTANCE: SimpleChannel;
    // Called once from the mod's own bootstrap -- see Structruretokubejsaisles.hc's `@entry`.
    static fn registerPackets();
}
extern class PacketDistributorTarget = "net.minecraftforge.network.PacketDistributor$PacketTarget" {}
extern class PacketDistributor = "net.minecraftforge.network.PacketDistributor" {
    static PLAYER: PacketDistributor;
    fn with(&self, supplier: &dyn JSupplier) -> PacketDistributorTarget;
}

// `PlayerSupplier` itself lives in its own file (network/PlayerSupplier.hc) -- declaring it here
// would make it this file's *first* struct, stealing the "named binary target" role from
// `Copytool` itself (every `pub fn` in this file would silently land on a class named
// `PlayerSupplier` instead) purely as a side effect of where one small helper struct happens to
// be declared. See the README's "Named binary target for module-level pub fn/pub static".

// `ClipboardPacket` itself is a native HC struct now (see network/ClipboardPacket.hc) -- no
// `extern class` bridging needed for it, same as every other struct this project already
// compiles as part of this same directory-mode build.



fn get_pos(stack: &ItemStack, key: String) -> Option<BlockPos> {
    let tag = stack.getTag();
    if tag.contains(key) {
        let res: Option<BlockPos> = Option::Some { value: BlockPos::of(tag.getLong(key)) };
        return res;
    }
    let res: Option<BlockPos> = None;
    return res;
}

pub fn has_pos(stack: &ItemStack, key: String) -> Bool {
    let tag = stack.getTag();
    return tag.contains(key);
}

pub fn get_pos_raw(stack: &ItemStack, key: String) -> BlockPos {
    let tag = stack.getTag();
    return BlockPos::of(tag.getLong(key));
}

pub fn write_pos(stack: &mut ItemStack, key: String, pos: BlockPos) {
    let tag = stack.getOrCreateTag();
    tag.putLong(key, pos.asLong());
}

pub fn clear_pos(stack: &mut ItemStack, key: String) {
    let tag = stack.getTag();
    tag.remove(key);
}

pub fn get_animated_color(color1: Int, color2: Int, duration: Int) -> Int {
    let time = (System::currentTimeMillis() % (duration as Long)) as Int;
    return animatedColor(color1, color2, time, duration);
}

pub fn use_item(level: &Level, player: &Player, hand: InteractionHand) -> InteractionResultHolder {
    var stack = player.getItemInHand(hand);

    if !level.isClientSide() {
        let pos1_opt: Option<BlockPos> = get_pos(&stack, "copytool_pos1");
        let pos2_opt: Option<BlockPos> = get_pos(&stack, "copytool_pos2");

        match pos1_opt {
            Option::Some { value: pos1 } => {
                match pos2_opt {
                    Option::Some { value: pos2 } => {
                        var min_x = 0;
                        if pos1.getX() < pos2.getX() { min_x = pos1.getX(); } else { min_x = pos2.getX(); }
                        var min_y = 0;
                        if pos1.getY() < pos2.getY() { min_y = pos1.getY(); } else { min_y = pos2.getY(); }
                        var min_z = 0;
                        if pos1.getZ() < pos2.getZ() { min_z = pos1.getZ(); } else { min_z = pos2.getZ(); }

                        var max_x = 0;
                        if pos1.getX() > pos2.getX() { max_x = pos1.getX(); } else { max_x = pos2.getX(); }
                        var max_y = 0;
                        if pos1.getY() > pos2.getY() { max_y = pos1.getY(); } else { max_y = pos2.getY(); }
                        var max_z = 0;
                        if pos1.getZ() > pos2.getZ() { max_z = pos1.getZ(); } else { max_z = pos2.getZ(); }

                        let size_x = max_x - min_x + 1;
                        let size_y = max_y - min_y + 1;
                        let size_z = max_z - min_z + 1;


                        let alphabet = [
                            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
                            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
                            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                            "!", "#", "$", "%", "&", "'", "(", ")", "*", "+", ",", "-", ".", "/", ":", ";", "<", ">", "?", "@", "[", "]", "^", "_", "`", "\{", "|", "\}", "~"
                        ];
                        let alphabetLength = 91;

                        let mapping = LinkedHashMap::new();
                        var next_index = 0;
                        let data_builder = StringBuilder::new();

                        for z in min_z..max_z + 1 {
                            for y in min_y..max_y + 1 {
                                for x_inv in 0..size_x {
                                    let x = max_x - x_inv;
                                    let current = BlockPos::new(x, y, z);
                                    let state = level.getBlockState(current);
                                    let block = state.getBlock();
                                    let id = BuiltInRegistries::BLOCK.getKey(&(block as JObject)).toString();

                                    var ch = "";
                                    if mapping.containsKey(&(id as JObject)) {
                                        ch = mapping.get(&(id as JObject)) as String;
                                    } else {
                                        if next_index >= alphabetLength {
                                            player.displayClientMessage(Component::literal("Too many unique blocks. Expand ALPHABET.") as Component, true);
                                            return InteractionResultHolder::fail(&stack as JObject);
                                        }
                                        ch = alphabet[next_index];
                                        mapping.put(&(id as JObject), &(ch as JObject));
                                        next_index = next_index + 1;
                                    }
                                    data_builder.append(ch);
                                }
                            }
                        }

                        let mapping_builder = StringBuilder::new();
                        let entries = mapping.entrySet();
                        let it = entries.iterator();
                        while it.hasNext() {
                            let entry = it.next() as Entry;
                            mapping_builder.append(entry.getValue() as String);
                            mapping_builder.append("=");
                            mapping_builder.append(entry.getKey() as String);
                            mapping_builder.append("\n");
                        }

                        let final_data_builder = StringBuilder::new();
                        final_data_builder.append(JString::valueOf(size_x));
                        final_data_builder.append(" ");
                        final_data_builder.append(JString::valueOf(size_z));
                        final_data_builder.append(" ");
                        final_data_builder.append(JString::valueOf(size_y));
                        final_data_builder.append("\n");
                        final_data_builder.append(mapping_builder.toString());
                        final_data_builder.append("\n");
                        final_data_builder.append(data_builder.toString());

                        let final_data = final_data_builder.toString();

                        let target = PacketDistributor::PLAYER.with(&PlayerSupplier { player: &player as ServerPlayer });
                        NetworkHandler::INSTANCE.send(target, &(ClipboardPacket { data: final_data } as JObject));

                        player.displayClientMessage(Component::literal("Copied blocks.") as Component, true);
                        clear_pos(&mut stack, "copytool_pos1");
                        clear_pos(&mut stack, "copytool_pos2");
                    }
                    Option::None {} => {
                        player.displayClientMessage(Component::literal("Set both corners first.") as Component, true);
                    }
                }
            }
            Option::None {} => {
                player.displayClientMessage(Component::literal("Set both corners first.") as Component, true);
            }
        }
    }

    let res = InteractionResultHolder::success(&stack as JObject);
    return res;
}

pub fn use_on(context: &UseOnContext, player: &Player) -> InteractionResult {
    var stack = context.getItemInHand();
    let pos = context.getClickedPos();

    if context.isSecondaryUseActive() {
        write_pos(&mut stack, "copytool_pos1", pos);
    } else {
        write_pos(&mut stack, "copytool_pos2", pos);
    }

    let level = context.getLevel();
    if !level.isClientSide() {
        var msg = "";
        if context.isSecondaryUseActive() { msg = "Corner 1 set!"; } else { msg = "Corner 2 set!"; }
        player.displayClientMessage(Component::literal(msg) as Component, true);
    }

    return InteractionResult::SUCCESS;
}
