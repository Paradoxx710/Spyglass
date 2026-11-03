package net.medievalrp.spyglass.plugin.command.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.medievalrp.spyglass.api.util.BlockLocation;
import net.medievalrp.spyglass.api.util.Duration;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

@ApiStatus.Internal
public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("prefix", "<green>«</green><aqua>Spyglass</aqua><green>»</green>"),
            Map.entry("feedback.error", "<green>«</green><aqua>Spyglass</aqua><green>»</green><red> (Error) </red><gray>%message%</gray>"),
            Map.entry("feedback.info", "<green>«</green><aqua>Spyglass</aqua><green>»</green> <gray>%message%</gray>"),
            Map.entry("feedback.warn", "<green>«</green><aqua>Spyglass</aqua><green>»</green> <yellow>%message%</yellow>"),
            Map.entry("feedback.success", "<green>«</green><aqua>Spyglass</aqua><green>»</green> <green>%message%</green>"),
            Map.entry("feedback.bonus", "<gray>%message%</gray>"),
            Map.entry("feedback.querying", "<dark_aqua>Querying records...</dark_aqua>"),
            Map.entry("feedback.tool-ok", "<green>%message%</green>"),
            Map.entry("feedback.inspect-header", "<green>«</green><aqua>Spyglass</aqua><green>»</green><green> --- </green><aqua>%target%</aqua><white> at </white><green>%location%</green><gray> (last %window%)</gray><green> ---</green>"),
            Map.entry("commands.no-results", "No results."),
            Map.entry("commands.action-no-permission", "You do not have permission to query the '%action%' action."),
            Map.entry("commands.action-rollback-no-permission", "You do not have permission to rollback the '%action%' action."),
            Map.entry("commands.rollback-queued", "Rollback queued at position %position% (id %id%). View / cancel with /sg rbqueue."),
            Map.entry("commands.page-header", "<green>--- </green><aqua>Spyglass Search</aqua><gray> (Page %page%/%total%)</gray><green> ---</green>"),
            Map.entry("results.ip-hidden", "(ip hidden)"),
            Map.entry("results.line", "<gray>= </gray>%date%%origin-tag%%leading-tags%<green>%player%</green> <white>%action%</white>%amount%%target%%count%%time%"),
            Map.entry("results.event-lines.sign-edit",
                    "<gray>= </gray>%date%%origin-tag%%leading-tags%<green>%player%</green> <white>%action%</white>%target% with <white>%sign-text%</white>%time%"),
            Map.entry("results.hover.default",
                    "<gray>Source: </gray><white>%source%</white>\n"
                    + "<gray>Event: </gray><white>%event%</white>%count-line%\n"
                    + "<gray>Target: </gray><white>%target%</white>\n"
                    + "<gray>When: </gray><white>%when%</white>\n"
                    + "<gray>Origin: </gray><white>%origin%</white>\n"
                    + "<gray>Location: </gray><white>%location%</white>"
                    + "%server-line%%details%")
    );

    private static volatile Messages instance = new Messages(DEFAULTS);

    private final Map<String, String> templates;

    public Messages(Map<String, String> templates) {
        Map<String, String> merged = new HashMap<>(DEFAULTS);
        if (templates != null) {
            merged.putAll(templates);
        }
        String prefix = merged.get("prefix");
        merged.replaceAll((key, value) -> value.replace("${prefix}", prefix));
        this.templates = Collections.unmodifiableMap(merged);
    }

    public static Messages getInstance() {
        return instance;
    }

    public static void setInstance(Messages messages) {
        instance = messages != null ? messages : new Messages(DEFAULTS);
    }

    public static Messages load(Path dataFolder, Logger logger) {
        Path file = dataFolder.resolve("messages.conf");
        if (!Files.exists(file)) {
            try {
                Files.createDirectories(dataFolder);
                try (InputStream in = Messages.class.getResourceAsStream("/messages.conf")) {
                    if (in != null) {
                        Files.copy(in, file);
                    }
                }
            } catch (IOException ex) {
                if (logger != null) {
                    logger.warning("Could not extract default messages.conf: " + ex.getMessage());
                }
            }
        }

        if (!Files.exists(file)) {
            setInstance(new Messages(DEFAULTS));
            return instance;
        }

        try {
            HoconConfigurationLoader loader = HoconConfigurationLoader.builder().path(file).build();
            ConfigurationNode root = loader.load();
            Map<String, String> map = new HashMap<>();
            flatten("", root, map);
            Messages loaded = new Messages(map);
            setInstance(loaded);
            return loaded;
        } catch (Exception ex) {
            if (logger != null) {
                logger.warning("Could not load messages.conf; using defaults. Error: " + ex.getMessage());
            }
            setInstance(new Messages(DEFAULTS));
            return instance;
        }
    }

    private static void flatten(String prefix, ConfigurationNode node, Map<String, String> map) {
        if (node.isMap()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
                String key = String.valueOf(entry.getKey());
                String nextPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                flatten(nextPrefix, entry.getValue(), map);
            }
        } else {
            String val = node.getString();
            if (val != null) {
                map.put(prefix, val);
            }
        }
    }

    public String raw(String key) {
        return templates.getOrDefault(key, DEFAULTS.getOrDefault(key, key));
    }

    public boolean hasTemplate(String key) {
        return templates.containsKey(key);
    }

    public Component get(String key) {
        return format(key, (CommandSender) null, Collections.emptyMap());
    }

    public Component get(String key, CommandSender sender) {
        return format(key, sender, Collections.emptyMap());
    }

    public Component format(String key, Object... keyValPairs) {
        return format(key, (CommandSender) null, toMap(keyValPairs));
    }

    public Component format(String key, CommandSender sender, Object... keyValPairs) {
        return format(key, sender, toMap(keyValPairs));
    }

    public Component format(String key, CommandSender sender, Map<String, String> placeholders) {
        String template = raw(key);
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue() != null ? entry.getValue() : "";
                String escaped = MINI_MESSAGE.escapeTags(v);
                template = template.replace(k, escaped);
            }
        }
        if (sender != null) {
            template = PlaceholderSupport.apply(sender, template);
        }
        try {
            return MINI_MESSAGE.deserialize(template);
        } catch (Exception ex) {
            return Component.text(template);
        }
    }

    public Component formatComponents(String key, Map<String, Component> placeholders) {
        String template = raw(key);
        Map<String, String> markers = new HashMap<>();
        if (placeholders != null) {
            int index = 0;
            for (Map.Entry<String, Component> entry : placeholders.entrySet()) {
                String marker = "SPYGLASSPLACEHOLDER" + java.util.UUID.randomUUID()
                        .toString().replace("-", "") + index++ + "X";
                markers.put(marker, entry.getKey());
                template = template.replace(entry.getKey(), marker);
            }
        }

        Component formatted;
        try {
            formatted = MINI_MESSAGE.deserialize(template);
        } catch (Exception ex) {
            formatted = Component.text(template);
        }
        for (Map.Entry<String, String> marker : markers.entrySet()) {
            Component replacement = placeholders.get(marker.getValue());
            if (replacement != null) {
                formatted = formatted.replaceText(TextReplacementConfig.builder()
                        .matchLiteral(marker.getKey())
                        .replacement(replacement)
                        .build());
            }
        }
        return formatted;
    }

    private static Map<String, String> toMap(Object... pairs) {
        if (pairs == null || pairs.length < 2) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>(pairs.length / 2);
        for (int i = 0; i < pairs.length - 1; i += 2) {
            String k = String.valueOf(pairs[i]);
            String v = pairs[i + 1] != null ? String.valueOf(pairs[i + 1]) : "";
            map.put(k, v);
        }
        return map;
    }

    public Component prefix() {
        return get("prefix");
    }

    public Component error(CommandSender sender, String message) {
        return format("feedback.error", sender, "%message%", message);
    }

    public Component error(String message) {
        return error(null, message);
    }

    public Component info(CommandSender sender, String message) {
        return format("feedback.info", sender, "%message%", message);
    }

    public Component info(String message) {
        return info(null, message);
    }

    public Component warn(CommandSender sender, String message) {
        return format("feedback.warn", sender, "%message%", message);
    }

    public Component warn(String message) {
        return warn(null, message);
    }

    public Component success(CommandSender sender, String message) {
        return format("feedback.success", sender, "%message%", message);
    }

    public Component success(String message) {
        return success(null, message);
    }

    public Component bonus(CommandSender sender, String message) {
        return format("feedback.bonus", sender, "%message%", message);
    }

    public Component bonus(String message) {
        return bonus(null, message);
    }

    public Component querying(CommandSender sender) {
        return get("feedback.querying", sender);
    }

    public Component querying() {
        return querying(null);
    }

    public Component toolOk(CommandSender sender, String message) {
        return format("feedback.tool-ok", sender, "%message%", message);
    }

    public Component toolOk(String message) {
        return toolOk(null, message);
    }

    public Component inspectHeader(CommandSender sender, String target, BlockLocation location, Duration window) {
        String locStr = location.x() + " " + location.y() + " " + location.z();
        return format("feedback.inspect-header", sender,
                "%target%", target,
                "%location%", locStr,
                "%window%", window.compact());
    }

    public Component inspectHeader(String target, BlockLocation location, Duration window) {
        return inspectHeader(null, target, location, window);
    }
}
