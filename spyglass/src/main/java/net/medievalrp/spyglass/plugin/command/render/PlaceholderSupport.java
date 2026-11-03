package net.medievalrp.spyglass.plugin.command.render;

import java.lang.reflect.Method;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class PlaceholderSupport {

    private static final Method SET_PLACEHOLDERS_METHOD;

    static {
        Method method = null;
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            method = papi.getMethod("setPlaceholders", Player.class, String.class);
        } catch (Throwable ignored) {
        }
        SET_PLACEHOLDERS_METHOD = method;
    }

    private PlaceholderSupport() {
    }

    public static boolean isAvailable() {
        return SET_PLACEHOLDERS_METHOD != null;
    }

    public static String apply(CommandSender sender, String text) {
        if (text == null || !text.contains("%") || SET_PLACEHOLDERS_METHOD == null || !(sender instanceof Player player)) {
            return text;
        }
        try {
            return (String) SET_PLACEHOLDERS_METHOD.invoke(null, player, text);
        } catch (Throwable ignored) {
            return text;
        }
    }
}
