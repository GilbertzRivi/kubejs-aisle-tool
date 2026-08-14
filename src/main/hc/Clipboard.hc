module net.oktawia.structruretokubejsaisles.hc;

// Ported from ClipboardPacket.setClipboard/appendAisle/jsEscape (network/ClipboardPacket.java):
// the pure data-transformation half of "reconstruct a structure-building script from the
// compressed aisle data a CopyToolItem selection sent over the wire." The Minecraft-API side
// effects that used to live directly inside that method (actually setting the system clipboard,
// writing a fallback file when the result is too big, showing a toast) stay in Java -- see
// ClipboardPacket.handle() -- since those are pure Forge/AWT-clipboard glue with no algorithm in
// them, exactly the same "port logic, not glue" split CopyToolItem/CTab already established.

// `JString`/`StringBuilder` are shared, declared once in McBindings.hc.

extern class Integer = "java.lang.Integer" {
    static fn parseInt(s: String) -> Int;
}

extern class NumberFormatException = "java.lang.NumberFormatException" {}

// Not `java.util.LinkedHashMap` -- generic JDK collections erase their type params to `Object`
// at the bytecode level (`V put(K, V)` really has erased descriptor
// `(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;`, not `(String,String)void`), and HC
// has no reference-type cast to get a `String` back out of the `Object` a `.get()`/iterator
// would hand back -- only numeric `as` casts exist today (see the README's Java interop known
// limitation). Same fixed-capacity-array-plus-count pattern `copytool.hc`'s own alphabet table
// already uses successfully: mapping keys here are single characters from the same bounded
// encoding alphabet, so a generous fixed capacity (128) is safe, and insertion order falls out
// for free from array order -- no extern collection, no erasure trap, nothing to get wrong.
fn mapping_find(keys: &[String], count: Int, key: String) -> Int {
    var idx = 0;
    while idx < count {
        if keys[idx] == key {
            return idx;
        }
        idx = idx + 1;
    }
    return -1;
}

// `\`/`"` are the only two characters this format needs escaped (it's building a JS-ish string
// literal for `.where("<key>", Predicates.blocks("<value>"))`). Scans char-by-char via
// `.substring(i, i+1)` rather than `.charAt(i)` -- `String.charAt` really returns Java `char`
// (JVM descriptor `C`), which HC has no type for; declaring it `-> Int` type-checks but crashes
// at runtime with `NoSuchMethodError` the moment it's actually called (verified the hard way).
// `.substring` has no such trap: it's a real `String` in, real `String` out, exact descriptor
// match either way.
pub fn js_escape(s: String) -> String {
    let out = StringBuilder::new();
    var i = 0;
    let len = s.length();
    while i < len {
        let c = s.substring(i, i + 1);
        if c == "\\" {
            out.append("\\\\");
        } else if c == "\"" {
            out.append("\\\"");
        } else {
            out.append(c);
        }
        i = i + 1;
    }
    return out.toString();
}

fn append_aisle(output: &mut StringBuilder, encoded: String, size_x: Int, size_z: Int, level_size: Int, y_index: Int) {
    output.append(".aisle(");
    let level_start = y_index * level_size;
    var z = 0;
    while z < size_z {
        let row_start = level_start + z * size_x;
        let row = encoded.substring(row_start, row_start + size_x);
        if z > 0 {
            output.append(", ");
        }
        output.append("\"");
        output.append(js_escape(row));
        output.append("\"");
        z = z + 1;
    }
    output.append(")\n");
}

// Reconstructs the `.aisle(...).where(...)` KubeJS structure-building script from the
// compressed "SIZE_X SIZE_Y SIZE_Z\n<char>=<blockId>\n...\n<encoded aisle data>" wire format
// CopyToolItem's use_item sends. Returns `data` unchanged on any malformed input (too few
// lines, wrong dimension count, unparseable size, truncated encoded data) -- matching the
// original Java's "just hand back the raw text" fallback, so the caller can always fall back to
// setting the system clipboard to whatever it got.
pub fn decode_clipboard(data: String) -> String {
    let lines = data.split("\n", -1);
    if lines.length < 2 {
        return data;
    }

    let dims = lines[0].trim().split(" ");
    if dims.length != 3 {
        return data;
    }

    var size_x = 0;
    var size_y = 0;
    var size_z = 0;
    try {
        size_x = Integer::parseInt(dims[0]);
        size_y = Integer::parseInt(dims[1]);
        size_z = Integer::parseInt(dims[2]);
    } catch (e: NumberFormatException) {
        return data;
    }

    var mapping_keys = [""; 128];
    var mapping_values = [""; 128];
    var mapping_count = 0;
    var i = 1;
    while i < lines.length && !lines[i].trim().isEmpty() {
        let line = lines[i].trim();
        let parts = line.split("=", 2);
        if parts.length == 2 && !parts[0].isEmpty() {
            let key = parts[0].substring(0, 1);
            // Matches `LinkedHashMap.put`'s real behavior: a repeated key overwrites its value
            // in place rather than adding a second entry (and doesn't move its position).
            let existing = mapping_find(&mapping_keys, mapping_count, key);
            if existing >= 0 {
                mapping_values[existing] = parts[1];
            } else if mapping_count < 128 {
                mapping_keys[mapping_count] = key;
                mapping_values[mapping_count] = parts[1];
                mapping_count = mapping_count + 1;
            }
        }
        i = i + 1;
    }
    while i < lines.length && lines[i].trim().isEmpty() {
        i = i + 1;
    }

    let encoded_builder = StringBuilder::new();
    while i < lines.length {
        encoded_builder.append(lines[i].trim());
        i = i + 1;
    }
    let encoded = encoded_builder.toString();

    let expected_length = size_x * size_y * size_z;
    if encoded.length() < expected_length {
        return data;
    }

    var output = StringBuilder::new();
    let level_size = size_x * size_z;

    // Original Java's `AISLE_TOP_FIRST` was a `private static final boolean` always `true` --
    // never actually toggled anywhere, so there's nothing to preserve by keeping it a variable.
    var y = size_y - 1;
    while y >= 0 {
        append_aisle(&mut output, encoded, size_x, size_z, level_size, y);
        y = y - 1;
    }

    output.append("\n");

    var m = 0;
    while m < mapping_count {
        output.append(".where(\"");
        output.append(js_escape(mapping_keys[m]));
        output.append("\", Predicates.blocks(\"");
        output.append(js_escape(mapping_values[m]));
        output.append("\"))\n");
        m = m + 1;
    }

    return output.toString();
}
