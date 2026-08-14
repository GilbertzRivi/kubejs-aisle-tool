module net.oktawia.structruretokubejsaisles.hc;

// The wire-format half of the old hand-written network/ClipboardPacket.java: `encode`/`decode`
// used to be written out by hand (`buf.writeUtf(packet.data)` / `new ClipboardPacket(buf
// .readUtf())`) for this exact one-field shape -- `@serializable` generates the same pair from
// the field list below instead. The Minecraft-API side (the real `handle()` network callback,
// clipboard/toast/file-fallback side effects, `END_MARKER`) stays hand-written Java -- see
// ClipboardPacketHandler.java -- same "port the data shape, not the glue" split every other file
// ported into this project has already established (Clipboard.hc's own header comment explains
// the same cut for `decode_clipboard`).
@serializable
pub struct ClipboardPacket {
    data: String,
}
