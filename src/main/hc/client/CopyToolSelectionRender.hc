module net.oktawia.structruretokubejsaisles.client;

// HC-native replacement for client/CopyToolSelectionRender.java. Same shape as
// CopyToolHudOverlay.hc (own doc comment has the fuller explanation of the annotation/
// named-holder mechanics) -- world-space selection-box rendering instead of the HUD overlay.
// `Minecraft`/`LocalPlayer`/`ClientLevel`/`ItemStack`/`Item`/`BlockPos`/`JMath` and
// `get_pos_from_stack` are all shared, declared once in McBindings.hc. `draw_selection_box`
// resolves directly too -- it's a real top-level fn in this same compile (RenderUtil.hc), no
// `extern class RenderUtil` bridging needed.

extern class PoseStack = "com.mojang.blaze3d.vertex.PoseStack" {}
extern class RenderLevelStageEvent = "net.minecraftforge.client.event.RenderLevelStageEvent" {
    fn getStage(&self) -> RenderLevelStageEventStage;
    fn getPoseStack(&self) -> PoseStack;
}
extern class RenderLevelStageEventStage = "net.minecraftforge.client.event.RenderLevelStageEvent$Stage" {
    static AFTER_TRANSLUCENT_BLOCKS: RenderLevelStageEventStage;
}

@"net.minecraftforge.fml.common.Mod$EventBusSubscriber"(
    modid: "structuretokubejsaisles",
    value: [enum("net.minecraftforge.api.distmarker.Dist", "CLIENT")],
)
pub struct CopyToolSelectionRender {}

@"net.minecraftforge.eventbus.api.SubscribeEvent"
pub fn on_render_world(event: &RenderLevelStageEvent) {
    if event.getStage() != RenderLevelStageEventStage::AFTER_TRANSLUCENT_BLOCKS {
        return;
    }

    let mc = Minecraft::getInstance();
    let player = mc.player;
    if player == null {
        return;
    }
    let level = mc.level;
    if level == null {
        return;
    }

    let stack = player.getMainHandItem();
    if !(stack.getItem() is CopyToolItem) {
        return;
    }

    let p1 = get_pos_from_stack(&stack, "copytool_pos1");
    let p2 = get_pos_from_stack(&stack, "copytool_pos2");
    if p1 == null && p2 == null {
        return;
    }

    let pose = event.getPoseStack();

    // Explicit branches, not a `(p1 != null) ? p1 : p2`-style single-var fallback -- HC's move
    // checker flags a variable as moved on its very first (even non-consuming-looking) read in
    // some positions, so "maybe reassign, then re-check the original" patterns fight the checker
    // unnecessarily; three plain branches sidestep that entirely.
    // `&pose`/`&p1`/`&p2`/`&min`/`&max` explicitly everywhere below, even where a bare identifier
    // would type-check -- a static call's arguments are always consumed unless borrowed (see
    // get_pos_from_stack's own comment above), and every one of these is either used twice in the
    // same call (`p1` as both the plain arg and `.offset(...)`'s receiver) or reused across a
    // later sibling branch after an earlier `if` (with no `else`) already "conservatively" moved
    // it in its own then-branch -- HC's move checker merges branch state as moved-on-either-side,
    // not reachability-aware, so a value used inside a branch that returns still reads as moved
    // to whatever follows that whole `if`.
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
            draw_selection_box(&pose, &min, &max.offset(1, 1, 1));
            return;
        }
        draw_selection_box(&pose, &p1, &p1.offset(1, 1, 1));
        return;
    }
    if p2 != null {
        draw_selection_box(&pose, &p2, &p2.offset(1, 1, 1));
    }
}
