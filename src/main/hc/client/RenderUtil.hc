module net.oktawia.structruretokubejsaisles.client;

// HC-native replacement for client/RenderUtil.java. Pure rendering-math glue -- no annotations,
// no nullability, nothing that needed the newer extern features except instance field access
// (`Minecraft.gameRenderer`) and reference casts (BlockPos coordinates -> Double for AABB).
// `PoseStack`/`BlockPos`/`Minecraft` are shared, declared once in McBindings.hc.

extern class VertexConsumer = "com.mojang.blaze3d.vertex.VertexConsumer" interface {}

extern class Vec3 = "net.minecraft.world.phys.Vec3" {
    x: Double;
    y: Double;
    z: Double;
}

extern class Camera = "net.minecraft.client.Camera" {
    fn getPosition(&self) -> Vec3;
}

extern class GameRenderer = "net.minecraft.client.renderer.GameRenderer" {
    fn getMainCamera(&self) -> Camera;
}

extern class AABB = "net.minecraft.world.phys.AABB" {
    fn new(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double) -> Self;
    fn move(&self, dx: Double, dy: Double, dz: Double) -> AABB;
}

extern class RenderType = "net.minecraft.client.renderer.RenderType" {
    static fn lines() -> RenderType;
}

extern class BufferSource = "net.minecraft.client.renderer.MultiBufferSource$BufferSource" {
    fn getBuffer(&self, type: RenderType) -> VertexConsumer;
}

extern class RenderBuffers = "net.minecraft.client.renderer.RenderBuffers" {
    fn bufferSource(&self) -> BufferSource;
}

extern class LevelRenderer = "net.minecraft.client.renderer.LevelRenderer" {
    static fn renderLineBox(pose: PoseStack, buffer: VertexConsumer, box: AABB, r: Float, g: Float, b: Float, a: Float);
}

pub struct RenderUtil {}

// Plain top-level fn, not an `impl RenderUtil { }` method -- no precedent/support for a
// self-less method inside `impl`, and this doesn't need one: `RenderUtil` being this module's
// first (only) struct already puts this fn on `RenderUtil`'s own class via the named-holder
// merge (same pattern `Items.hc`'s `register(bus)` uses), so other files just call
// `draw_selection_box(...)` bare -- no `RenderUtil::` prefix needed, same as any other top-level
// fn shared across this directory-mode compile.
pub fn draw_selection_box(pose: &PoseStack, min: &BlockPos, max_exclusive: &BlockPos) {
    let mc = Minecraft::getInstance();
    let cam = mc.gameRenderer.getMainCamera().getPosition();

    let box = AABB::new(
        min.getX() as Double, min.getY() as Double, min.getZ() as Double,
        max_exclusive.getX() as Double, max_exclusive.getY() as Double, max_exclusive.getZ() as Double
    ).move(-cam.x, -cam.y, -cam.z);

    let buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType::lines());

    LevelRenderer::renderLineBox(pose, buffer, box, 0.0f, 1.0f, 0.0f, 1.0f);
}
