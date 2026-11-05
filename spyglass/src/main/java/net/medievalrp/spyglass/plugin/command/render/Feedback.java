package net.medievalrp.spyglass.plugin.command.render;

import net.kyori.adventure.text.Component;
import net.medievalrp.spyglass.api.param.ParamParseException;
import net.medievalrp.spyglass.api.util.BlockLocation;
import net.medievalrp.spyglass.plugin.command.CatalogMessageException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.ApiStatus;

/**
 * Prefixed Adventure components for the common sender feedback categories,
 * backed by configurable templates in {@link Messages}.
 */
@ApiStatus.Internal
public final class Feedback {

    private Feedback() {
    }

    public static Component error(CommandSender sender, String message) {
        return Messages.getInstance().error(sender, message);
    }

    public static Component error(String message) {
        return Messages.getInstance().error(message);
    }

    public static Component info(CommandSender sender, String message) {
        return Messages.getInstance().info(sender, message);
    }

    public static Component info(String message) {
        return Messages.getInstance().info(message);
    }

    public static Component warn(CommandSender sender, String message) {
        return Messages.getInstance().warn(sender, message);
    }

    public static Component warn(String message) {
        return Messages.getInstance().warn(message);
    }

    public static Component success(CommandSender sender, String message) {
        return Messages.getInstance().success(sender, message);
    }

    public static Component success(String message) {
        return Messages.getInstance().success(message);
    }

    public static Component bonus(CommandSender sender, String message) {
        return Messages.getInstance().bonus(sender, message);
    }

    public static Component bonus(String message) {
        return Messages.getInstance().bonus(message);
    }

    public static Component querying(CommandSender sender) {
        return Messages.getInstance().querying(sender);
    }

    public static Component querying() {
        return Messages.getInstance().querying();
    }

    public static Component toolOk(CommandSender sender, String message) {
        return Messages.getInstance().toolOk(sender, message);
    }

    public static Component toolOk(String message) {
        return Messages.getInstance().toolOk(message);
    }

    public static Component catalogError(CommandSender sender, String key, Object... placeholders) {
        return Messages.getInstance().errorFeedback(key, sender, placeholders);
    }

    public static Component catalogError(String key, Object... placeholders) {
        return catalogError(null, key, placeholders);
    }

    public static Component parseError(CommandSender sender, ParamParseException exception) {
        if (exception.getCause() instanceof CatalogMessageException message) {
            return catalogError(sender, message.key(), message.placeholders());
        }
        return error(sender, exception.getMessage());
    }

    public static Component catalogInfo(CommandSender sender, String key, Object... placeholders) {
        return Messages.getInstance().feedback(key, sender, placeholders);
    }

    public static Component catalogWarn(CommandSender sender, String key, Object... placeholders) {
        return Messages.getInstance().feedback(key, sender, placeholders);
    }

    public static Component catalogSuccess(CommandSender sender, String key, Object... placeholders) {
        return Messages.getInstance().feedback(key, sender, placeholders);
    }

    public static Component catalogBonus(CommandSender sender, String key, Object... placeholders) {
        return Messages.getInstance().feedback(key, sender, placeholders);
    }

    public static Component catalogBonus(String key, Object... placeholders) {
        return catalogBonus(null, key, placeholders);
    }

    public static Component catalogToolOk(CommandSender sender, String key, Object... placeholders) {
        return Messages.getInstance().feedback(key, sender, placeholders);
    }

    public static Component inspectHeader(CommandSender sender, String target, BlockLocation location,
                                          net.medievalrp.spyglass.api.util.Duration window) {
        return Messages.getInstance().inspectHeader(sender, target, location, window);
    }

    public static Component inspectHeader(String target, BlockLocation location,
                                          net.medievalrp.spyglass.api.util.Duration window) {
        return Messages.getInstance().inspectHeader(target, location, window);
    }
}
