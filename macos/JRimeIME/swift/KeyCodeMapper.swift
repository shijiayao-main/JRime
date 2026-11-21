import AppKit

/// Maps NSEvent keyDowns onto librime key reprs + modifier masks.
/// Letters/punctuation ride on `charactersIgnoringModifiers` (the engine's
/// `RimeKeyEvent.parse` accepts raw chars); hardware-keycode specials use the
/// X11 keysyms librime expects.
enum KeyCodeMapper {
    struct Mapped {
        let repr: String
        let mask: Int
    }

    // librime modifier masks
    static let shift = 1 << 0
    static let lock = 1 << 1
    static let control = 1 << 2
    static let alt = 1 << 3

    // kVK hardware keycodes -> X11 keysym reprs
    private static let specials: [UInt16: String] = [
        36: "Return", 51: "BackSpace", 53: "Escape", 49: "space", 48: "Tab",
        123: "Left", 124: "Right", 125: "Down", 126: "Up",
        115: "Home", 119: "End", 116: "Prior", 121: "Next",
        117: "Delete",
    ]

    /// The modifier keys the engine wants as key events: a Shift tap is the
    /// schema's Chinese/English toggle (ascii_composer), CapsLock commits the
    /// raw code (commit_code). Other modifiers pass through untouched.
    static func mapModifier(_ event: NSEvent) -> String? {
        // flagsChanged fires for both press and release; librime distinguishes
        // the pair through the Release modifier, and ascii_composer only
        // toggles after a complete tap.
        let pressed = event.modifierFlags.contains(.shift)
        switch event.keyCode {
        case 56, 60: return pressed ? "Shift_L" : "Release+Shift_L"
        case 57:
            let caps = event.modifierFlags.contains(.capsLock)
            return caps ? "Caps_Lock" : "Release+Caps_Lock"
        default: return nil
        }
    }

    static func map(_ event: NSEvent) -> Mapped? {
        let flags = event.modifierFlags
        if flags.contains(.command) { return nil } // system shortcuts pass through

        var mask = 0
        if flags.contains(.control) { mask |= control }
        if flags.contains(.option) { mask |= alt }

        var repr: String?
        if let special = specials[UInt16(event.keyCode)] {
            repr = special
        } else if let chars = event.charactersIgnoringModifiers, !chars.isEmpty {
            // lowercased unshifted char: the composition alphabet is lowercase
            repr = String(chars.lowercased().first!)
        }
        guard let repr = repr else { return nil }
        if flags.contains(.shift) { mask |= shift }
        return Mapped(repr: repr, mask: mask)
    }
}
