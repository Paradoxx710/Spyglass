package net.medievalrp.spyglass.plugin.command.render;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.medievalrp.spyglass.api.util.BlockLocation;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.ApiStatus;

/**
 * Prefixed Adventure components for the common sender feedback categories,
 * backed by configurable templates in {@link Messages}.
 */
@ApiStatus.Internal
public final class Feedback {

    /** «Spyglass» — green brackets, aqua name, reused across every message. */
    public static final Component PREFIX = Component.text()
            .append(Component.text("«", NamedTextColor.GREEN))
            .append(Component.text("Spyglass", NamedTextColor.AQUA))
            .append(Component.text("»", NamedTextColor.GREEN))
            .asComponent();

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

    public static Component inspectHeader(CommandSender sender, String target, BlockLocation location,
                                          net.medievalrp.spyglass.api.util.Duration window) {
        return Messages.getInstance().inspectHeader(sender, target, location, window);
    }

    public static Component inspectHeader(String target, BlockLocation location,
                                          net.medievalrp.spyglass.api.util.Duration window) {
        return Messages.getInstance().inspectHeader(target, location, window);
    }
}
