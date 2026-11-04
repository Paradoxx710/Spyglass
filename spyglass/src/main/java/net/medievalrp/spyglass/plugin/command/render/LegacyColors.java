package net.medievalrp.spyglass.plugin.command.render;

/**
 * Translates the traditional ampersand formatting codes into MiniMessage tags
 * so operators can keep using {@code &6}, {@code &l}, {@code &#ff5555}, ...
 * alongside MiniMessage. MiniMessage remains the renderer; this is only a
 * pre-pass over configured message templates, never over player input.
 *
 * <p>Colour codes reset active formatting first, matching the legacy client
 * parser ({@code &6&l} is bold gold while {@code &l&6} is just gold). Use
 * {@code &&} for a literal ampersand.
 */
final class LegacyColors {

    private static final String[] COLORS = {
            "black", "dark_blue", "dark_green", "dark_aqua",
            "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua",
            "red", "light_purple", "yellow", "white"
    };

    private LegacyColors() {
    }

    static String translate(String input) {
        if (input == null || input.indexOf('&') < 0) {
            return input;
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c != '&' || i + 1 >= input.length()) {
                out.append(c);
                i++;
                continue;
            }
            char code = Character.toLowerCase(input.charAt(i + 1));
            if (code == '&') {
                out.append('&');
                i += 2;
                continue;
            }
            // &#RRGGBB
            if (code == '#') {
                if (i + 8 <= input.length() && isHex(input, i + 2, 6)) {
                    out.append("<reset><#").append(input, i + 2, i + 8).append('>');
                    i += 8;
                    continue;
                }
                out.append('&');
                i++;
                continue;
            }
            // &x&R&R&G&G&B&B
            if (code == 'x') {
                if (i + 14 <= input.length()) {
                    StringBuilder hex = new StringBuilder(6);
                    boolean valid = true;
                    for (int part = 0; part < 6; part++) {
                        int amp = i + 2 + part * 2;
                        if (input.charAt(amp) != '&' || !isHexChar(input.charAt(amp + 1))) {
                            valid = false;
                            break;
                        }
                        hex.append(input.charAt(amp + 1));
                    }
                    if (valid) {
                        out.append("<reset><#").append(hex).append('>');
                        i += 14;
                        continue;
                    }
                }
                out.append('&');
                i++;
                continue;
            }
            int color = colorIndex(code);
            if (color >= 0) {
                out.append("<reset><").append(COLORS[color]).append('>');
                i += 2;
                continue;
            }
            String style = style(code);
            if (style != null) {
                out.append('<').append(style).append('>');
                i += 2;
                continue;
            }
            out.append('&');
            i++;
        }
        return out.toString();
    }

    private static int colorIndex(char code) {
        if (code >= '0' && code <= '9') {
            return code - '0';
        }
        if (code >= 'a' && code <= 'f') {
            return 10 + (code - 'a');
        }
        return -1;
    }

    private static String style(char code) {
        return switch (code) {
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underlined";
            case 'o' -> "italic";
            case 'r' -> "reset";
            default -> null;
        };
    }

    private static boolean isHex(String input, int start, int count) {
        for (int i = start; i < start + count; i++) {
            if (!isHexChar(input.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexChar(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f');
    }
}
