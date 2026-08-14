module net.oktawia.structruretokubejsaisles.client;

// HC-native replacement for client/CopyToolHudOverlay.java. `@Mod$EventBusSubscriber`/
// `@SubscribeEvent` (see the README's "Java annotations" section) are real classfile
// annotations here, discovered by Forge's mod-file bytecode scanner exactly the same way it
// would find them on a javac-compiled class -- no change needed to Structruretokubejsaisles.java,
// which never called EVENT_BUS.register() for this class to begin with (that's the whole point
// of the `@Mod.EventBusSubscriber` pattern over manual registration).
//
// `Minecraft`/`LocalPlayer`/`ItemStack`/`Item`/`BlockPos`/`JMath` are shared, declared once in
// McBindings.hc; `get_pos_from_stack` is also shared there (was a byte-for-byte duplicate of
// CopyToolSelectionRender.hc's own copy before). `has_pos`/`get_pos_raw`/`CopyToolItem` all
// resolve directly too, no `extern class` bridging needed -- see McBindings.hc's own doc comment.
//
// Both fns below are plain top-level fns, not `impl CopyToolHudOverlay { }` methods -- deliberately:
// annotations only attach to a top-level `struct`/`fn` (see the README), and both need to land as
// real static methods directly on `CopyToolHudOverlay`'s own class for Forge to find them, which
// is exactly what the "named binary target for module-level pub fn" feature already does since
// `CopyToolHudOverlay` is this module's first declared struct.

// `RenderGuiOverlayEvent.getOverlay()`/`VanillaGuiOverlay.type()` both actually return
// `NamedGuiOverlay` (a record wrapping a `ResourceLocation id` + the real `IGuiOverlay`), NOT
// `IGuiOverlay` directly -- verified against the real mapped Forge jar via `javap` after this
// was declared wrong here and shipped a `NoSuchMethodError` at class-load time (the JVM matches
// methods by exact descriptor; a wrong declared return type here compiles fine and only fails
// once Forge's event bus actually invokes it). `==`/`!=` between two `NamedGuiOverlay` values
// compares by reference, not `NamedGuiOverlay`'s own record-generated structural `equals()` --
// fine here since both sides come from the same enum-singleton pipeline (matches the idiomatic
// Java `event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()` this file ports).
extern class NamedGuiOverlay = "net.minecraftforge.client.gui.overlay.NamedGuiOverlay" {}
// A real Java enum -- `HOTBAR` is a static field (the enum constant), not a method.
extern class VanillaGuiOverlay = "net.minecraftforge.client.gui.overlay.VanillaGuiOverlay" {
    static HOTBAR: VanillaGuiOverlay;
    fn type(&self) -> NamedGuiOverlay;
}
extern class RenderGuiOverlayEventPost = "net.minecraftforge.client.event.RenderGuiOverlayEvent$Post" {
    fn getOverlay(&self) -> NamedGuiOverlay;
    fn getGuiGraphics(&self) -> GuiGraphics;
}

@"net.minecraftforge.fml.common.Mod$EventBusSubscriber"(
    modid: "structuretokubejsaisles",
    value: [enum("net.minecraftforge.api.distmarker.Dist", "CLIENT")],
)
pub struct CopyToolHudOverlay {}

@"net.minecraftforge.eventbus.api.SubscribeEvent"
pub fn on_render_overlay(event: &RenderGuiOverlayEventPost) {
    if event.getOverlay() != VanillaGuiOverlay::HOTBAR.type() {
        return;
    }

    let mc = Minecraft::getInstance();
    // Narrowing (`== null`/`!= null` re-typing) only recognizes a plain local identifier, not a
    // field-access expression like `mc.player` directly -- bind it to a local first, same
    // pattern CopyToolItem.hc's `useOn` already established.
    let player = mc.player;
    if player == null {
        return;
    }

    let stack = player.getMainHandItem();
    if !(stack.getItem() is CopyToolItem) {
        return;
    }

    let p1 = get_pos_from_stack(&stack, "copytool_pos1");
    let p2 = get_pos_from_stack(&stack, "copytool_pos2");

    let screen_w = mc.getWindow().getGuiScaledWidth();
    let screen_h = mc.getWindow().getGuiScaledHeight();

    let x = screen_w / 2 - 90;
    let y = screen_h - 60;

    var gg = event.getGuiGraphics();

    if p1 == null && p2 == null {
        let line1 = "CopyTool: set corners";
        let line2 = "(Sneak + RightClick = Corner 1)";

        let center_x = screen_w / 2;
        let x1 = center_x - mc.font.width(line1) / 2;

        gg.drawString(mc.font, line1, x1, y, 0xFFFFFF, true);
        gg.drawString(mc.font, line2, center_x - mc.font.width(line2) / 2, y + 10, 0xAAAAAA, true);
        return;
    }

    // No if/else-as-expression yet (see IDEAS.md) -- statement form with a `var` instead.
    var c1 = "Corner1: -";
    if p1 != null {
        c1 = "Corner1: {p1.getX()} {p1.getY()} {p1.getZ()}";
    }
    var c2 = "Corner2: -";
    if p2 != null {
        c2 = "Corner2: {p2.getX()} {p2.getY()} {p2.getZ()}";
    }

    gg.drawString(mc.font, c1, x, y, 0xFFFFFF, true);
    gg.drawString(mc.font, c2, x, y + 10, 0xFFFFFF, true);

    // Nested, not `p1 != null && p2 != null` -- narrowing only recognizes a single `ident !=
    // null` condition per `if` (see the README), not a compound `&&`; nesting narrows both.
    if p1 != null {
        if p2 != null {
            let min = BlockPos::new(
                JMath::min(p1.getX(), p2.getX()),
                JMath::min(p1.getY(), p2.getY()),
                JMath::min(p1.getZ(), p2.getZ()),
            );
            let max = BlockPos::new(
                JMath::max(p1.getX(), p2.getX()),
                JMath::max(p1.getY(), p2.getY()),
                JMath::max(p1.getZ(), p2.getZ()),
            );

            let size_x = max.getX() - min.getX() + 1;
            let size_y = max.getY() - min.getY() + 1;
            let size_z = max.getZ() - min.getZ() + 1;

            let blocks = (size_x as Long) * (size_y as Long) * (size_z as Long);

            gg.drawString(mc.font, "Size: {size_x}x{size_y}x{size_z}   Blocks: {blocks}", x, y + 22, 0xFFFFFF, true);
        }
    }
}
