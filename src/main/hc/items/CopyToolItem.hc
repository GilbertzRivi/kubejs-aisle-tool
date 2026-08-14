module net.oktawia.structruretokubejsaisles.items;

// The full HC-native replacement for items/CopyToolItem.java's thin Java shell -- a real
// `extends Item` subclass. `use`/`useOn` delegate to Copytool's use_item/use_on functions
// (called bare now -- see McBindings.hc's own doc comment on why no `Copytool::` prefix is
// needed anymore); `appendHoverText` is inlined here directly since it needs
// `List<Component>.add(Object)`, a generic erased Java method only reachable via a reference
// cast (`as`, see the README). Every `extern class` this file used to declare on its own
// (Item/ItemStack/Level/Player/UseOnContext/InteractionResult(Holder)/Component/JList/JObject)
// now lives once, shared, in McBindings.hc.

pub struct CopyToolItem extends Item {}

impl CopyToolItem {
    override fn use(&self, level: &Level, player: &Player, hand: InteractionHand) -> InteractionResultHolder {
        return use_item(&level, &player, hand);
    }

    // `context.getPlayer()` is legitimately null for a non-player use context (e.g. a
    // dispenser) -- the one real null check this whole class needs.
    override fn useOn(&self, context: &UseOnContext) -> InteractionResult {
        let player = context.getPlayer();
        if player == null {
            return InteractionResult::PASS;
        }
        return use_on(&context, &player);
    }

    // Known limitation: no per-line animated color styling (the original Java used
    // `Style.EMPTY.withColor(...)`, a static *field* of a type this project has no other need
    // to declare). The tooltip text itself -- the actually load-bearing part -- is unaffected;
    // only the cosmetic coloring is dropped.
    override fn appendHoverText(&self, stack: &ItemStack, level: &Level, tooltip: &mut JList, flag: &TooltipFlag) {
        tooltip.add(Component::literal("Shift Right Click to set first position and right click to set second.") as JObject);
        tooltip.add(Component::literal("Then right click on air to copy to clipboard.") as JObject);
        tooltip.add(Component::literal("Some selections will be so big that the output is pushed to a file. In these cases the filepath will be told to you.") as JObject);
    }
}
