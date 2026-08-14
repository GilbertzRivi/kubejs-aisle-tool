
module net.oktawia.structruretokubejsaisles.hc;

// `JMath` is shared, declared once in McBindings.hc.

pub fn greet(name: String) -> String {
    return "[Hot Chocolate] hello from a real .hc-compiled class, " + name + "!";
}

pub fn bonusCount(base: Int) -> Int {
    return base + 42;
}


fn colorByte(color: Int, place: Int) -> Int {
    return (color / place) % 256;
}

pub fn animatedColor(color1: Int, color2: Int, timeInDuration: Int, duration: Int) -> Int {
    let time = timeInDuration as Float / duration as Float;
    let pi = 3.14159265358979323846;
    let phase = (JMath::sin((time * 2.0f) as Double * pi) as Float) * 0.5f + 0.5f;

    let r1 = colorByte(color1, 65536);
    let r2 = colorByte(color2, 65536);
    let g1 = colorByte(color1, 256);
    let g2 = colorByte(color2, 256);
    let b1 = color1 % 256;
    let b2 = color2 % 256;

    let r = (r1 as Float + ((r2 - r1) as Float) * phase) as Int;
    let g = (g1 as Float + ((g2 - g1) as Float) * phase) as Int;
    let b = (b1 as Float + ((b2 - b1) as Float) * phase) as Int;

    return r * 65536 + g * 256 + b;
}
