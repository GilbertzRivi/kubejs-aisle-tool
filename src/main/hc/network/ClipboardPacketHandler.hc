module net.oktawia.structruretokubejsaisles.network;

// The HC-native replacement for network/ClipboardPacketHandler.java -- the last hand-written
// Java file in this mod, previously blocked on `Files.writeString(Path, CharSequence, Charset,
// OpenOption...)`'s real varargs param: HC has no varargs *syntax*, but at the bytecode level a
// varargs param is just an ordinary array param (javac itself just passes an empty array when a
// call site gives zero variadic arguments) -- so declaring it as a real `[OpenOption]` array
// param and passing `[StandardOpenOption::WRITE as OpenOption; 0]` (a real, if never-actually-
// read, zero-length array -- see the compiler's own `[value; count]` form) closes the gap with
// no new compiler feature needed at all. `enqueueWork`'s `Runnable` callback is the other half
// of why this stayed Java -- both close now that lambda literals exist.

extern interface JRunnable = "java.lang.Runnable" {
    fn run(&self);
}
extern class NetworkEventContext = "net.minecraftforge.network.NetworkEvent$Context" {
    fn enqueueWork(&self, task: &dyn JRunnable);
    fn setPacketHandled(&mut self, handled: Bool);
}

extern class Path = "java.nio.file.Path" {
    // `-> Self` only resolves for a `new` constructor (its declared return type is never even
    // read -- see checkStaticCall's ctor branch); an ordinary method needs the real alias name.
    fn toAbsolutePath(&self) -> Path;
    fn toString(&self) -> String;
}
extern class Paths = "java.nio.file.Paths" {
    static fn get(first: String) -> Path;
}
extern class Charset = "java.nio.charset.Charset" {}
extern class StandardCharsets = "java.nio.charset.StandardCharsets" {
    static UTF_8: Charset;
}
extern class JCharSequence = "java.lang.CharSequence" interface {}
// A real `OpenOption` implementer, just so `[StandardOpenOption::WRITE as OpenOption; 0]` below
// has an actual value to declare (never read -- the array is zero-length) without needing a
// `null`/undeclarable "empty value" of its own. `WRITE` has no special meaning here over any
// other constant; it's simply never used.
extern class OpenOption = "java.nio.file.OpenOption" interface {}
extern class StandardOpenOption = "java.nio.file.StandardOpenOption" {
    static WRITE: StandardOpenOption;
}
extern class Files = "java.nio.file.Files" {
    // `path` borrowed, not owned -- the call site reads it again afterward
    // (`path.toAbsolutePath()`) and reference-ness is purely a compile-time move-checking device
    // here anyway (codegen treats an object reference identically either way).
    fn writeString(path: &Path, content: JCharSequence, cs: Charset, opts: [OpenOption]) -> Path;
}

extern class JException = "java.lang.Exception" {}

// Called from `NetworkHandler.hc`'s `registerPackets` (a small forwarding lambda -- see its own
// `extern class ClipboardPacketHandler` declaration, unchanged: this file's real binary
// name/signature stayed exactly the same, so that extern binding still resolves correctly now
// that the real thing behind it is HC instead of Java).
pub fn handle(packet: ClipboardPacket, ctx: JSupplierValue) {
    let context = ctx.get() as NetworkEventContext;
    context.enqueueWork(|| set_clipboard(packet.data));
    context.setPacketHandled(true);
}

// `@OnlyIn(Dist.CLIENT)` on the original Java is enforced by construction here instead: nothing
// else in this compile ever calls `set_clipboard` from anything but the client-only `enqueueWork`
// callback above, and Forge's own `runtimedistcleaner` service strips unreachable client-only
// code paths the same way regardless of which language compiled them.
fn set_clipboard(data: String) {
    let out_str = decode_clipboard(data);
    if out_str.length() > 32767 {
        try {
            let path = Paths::get("packet.txt");
            Files::writeString(&path, out_str as JCharSequence, StandardCharsets::UTF_8, [StandardOpenOption::WRITE as OpenOption; 0]);
            Minecraft::getInstance().keyboardHandler.setClipboard("Data saved to: " + path.toAbsolutePath().toString());
            Minecraft::getInstance().getToasts().addToast(SystemToast::new(
                SystemToastIds::TUTORIAL_HINT,
                Component::literal("Copy Tool Debug") as Component,
                Component::literal("Path copied to clipboard") as Component,
            ));
        } catch (e: JException) {
            Minecraft::getInstance().keyboardHandler.setClipboard(out_str.substring(0, 32767));
        }
    } else {
        Minecraft::getInstance().keyboardHandler.setClipboard(out_str);
    }
}
