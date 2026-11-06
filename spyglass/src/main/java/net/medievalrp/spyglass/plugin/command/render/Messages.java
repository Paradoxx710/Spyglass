package net.medievalrp.spyglass.plugin.command.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            Map.entry("feedback.error", "${prefix} <red>(Error)</red> %message%"),
            Map.entry("feedback.info", "${prefix} %message%"),
            Map.entry("feedback.warn", "${prefix} %message%"),
            Map.entry("feedback.success", "${prefix} %message%"),
            Map.entry("feedback.bonus", "${prefix} %message%"),
            Map.entry("feedback.querying", "${prefix} <dark_aqua>Querying records...</dark_aqua>"),
            Map.entry("feedback.tool-ok", "${prefix} %message%"),
            Map.entry("feedback.inspect-header", "${prefix} <green>--- </green>%target%<white> at </white>%location%<gray> (last %window%)</gray><green> ---</green>"),
            Map.entry("commands.no-results", "No results."),
            Map.entry("commands.config-reloaded", "${prefix} Spyglass configuration reloaded. Event toggles, retention, redaction, wand and time-zone settings are active."),
            Map.entry("commands.config-reload-failed", "${prefix} Spyglass could not reload config.conf; check the console."),
            Map.entry("commands.config-restart-required", "${prefix} Restart required for %changes%; no live settings were changed."),
            Map.entry("commands.tool-activated", "Added the Spyglass data tool to your inventory."),
            Map.entry("commands.tool-deactivated", "Deactivated the Spyglass Data Tool"),
            Map.entry("commands.tool-no-room", "Make room in your inventory for the Spyglass wand."),
            Map.entry("help.header", "<aqua> -======= Spyglass (%page%/%pages%) =======-</aqua>"),
            Map.entry("help.subtitle", "<dark_gray><italic>For Powerful Searching</italic></dark_gray>"),
            Map.entry("help.entry", "<aqua>/spyglass </aqua><green>%name% %usage%</green><aqua>: </aqua><gray>%description%</gray>"),
            Map.entry("help.nav", "<red>[%arrow%]</red>"),
            Map.entry("help.nav-hover", "<red>Page %page%</red>"),
            Map.entry("help.nav-separator", " "),
            Map.entry("help.commands.help", "<aqua>/spyglass </aqua><green>help <Page #></green><aqua>: </aqua><gray>Show this help, one page at a time.</gray>"),
            Map.entry("help.commands.search", "<aqua>/spyglass </aqua><green>search <Lookup Params></green><aqua>: </aqua><gray>Search Data Records based on the parameters provided.</gray>"),
            Map.entry("help.commands.page", "<aqua>/spyglass </aqua><green>page <Page #></green><aqua>: </aqua><gray>Moves you to the specified page of results (cached 15 min).</gray>"),
            Map.entry("help.commands.rollback", "<aqua>/spyglass </aqua><green>rollback <Lookup Params></green><aqua>: </aqua><gray>Rollback a set of changes based on the parameters provided.</gray>"),
            Map.entry("help.commands.restore", "<aqua>/spyglass </aqua><green>restore <Lookup Params></green><aqua>: </aqua><gray>Restore a set of changes based on the parameters provided.</gray>"),
            Map.entry("help.commands.undo", "<aqua>/spyglass </aqua><green>undo</green><aqua>: </aqua><gray>Undo your most recent rollback or restore.</gray>"),
            Map.entry("help.commands.tool", "<aqua>/spyglass </aqua><green>tool</green><aqua>: </aqua><gray>Toggle the inspection wand.</gray>"),
            Map.entry("help.commands.events", "<aqua>/spyglass </aqua><green>events</green><aqua>: </aqua><gray>List every event type currently being recorded.</gray>"),
            Map.entry("help.commands.rbqueue", "<aqua>/spyglass </aqua><green>rbqueue</green><aqua>: </aqua><gray>List, cancel, or resume queued rollback jobs.</gray>"),
            Map.entry("help.commands.inventory", "<aqua>/spyglass </aqua><green>inventory</green><aqua>: </aqua><gray>Open the rollback salvage inventory GUI (in-game only).</gray>"),
            Map.entry("help.commands.stats", "<aqua>/spyglass </aqua><green>stats</green><aqua>: </aqua><gray>Show ingest analytics (when enabled in config).</gray>"),
            Map.entry("help.commands.import", "<aqua>/spyglass </aqua><green>import <file | mysql <source>></green><aqua>: </aqua><gray>Import a CoreProtect database: a SQLite file from plugins/Spyglass/import/, or a live MySQL source from import.conf.</gray>"),
            Map.entry("help.commands.migrate", "<aqua>/spyglass </aqua><green>migrate <backend></green><aqua>: </aqua><gray>Copy all records into another configured storage backend.</gray>"),
            Map.entry("help.commands.version", "<aqua>/spyglass </aqua><green>version</green><aqua>: </aqua><gray>Show version and compatible update status.</gray>"),
            Map.entry("help.commands.reload", "<aqua>/spyglass </aqua><green>reload</green><aqua>: </aqua><gray>Reload live configuration settings (requires spyglass.reload).</gray>"),
            Map.entry("help.entries.help.usage", "<Page #>"),
            Map.entry("help.entries.help.description", "Show this help, one page at a time."),
            Map.entry("help.entries.search.usage", "<Lookup Params>"),
            Map.entry("help.entries.search.description", "Search Data Records based on the parameters provided."),
            Map.entry("help.entries.page.usage", "<Page #>"),
            Map.entry("help.entries.page.description", "Moves you to the specified page of results (cached 15 min)."),
            Map.entry("help.entries.rollback.usage", "<Lookup Params>"),
            Map.entry("help.entries.rollback.description", "Rollback a set of changes based on the parameters provided."),
            Map.entry("help.entries.restore.usage", "<Lookup Params>"),
            Map.entry("help.entries.restore.description", "Restore a set of changes based on the parameters provided."),
            Map.entry("help.entries.undo.usage", ""),
            Map.entry("help.entries.undo.description", "Undo your most recent rollback or restore."),
            Map.entry("help.entries.tool.usage", ""),
            Map.entry("help.entries.tool.description", "Toggle the inspection wand."),
            Map.entry("help.entries.events.usage", ""),
            Map.entry("help.entries.events.description", "List every event type currently being recorded."),
            Map.entry("help.entries.rbqueue.usage", ""),
            Map.entry("help.entries.rbqueue.description", "List, cancel, or resume queued rollback jobs."),
            Map.entry("help.entries.inventory.usage", ""),
            Map.entry("help.entries.inventory.description", "Open the rollback salvage inventory GUI (in-game only)."),
            Map.entry("help.entries.stats.usage", ""),
            Map.entry("help.entries.stats.description", "Show ingest analytics (when enabled in config)."),
            Map.entry("help.entries.import.usage", "<file | mysql <source>>"),
            Map.entry("help.entries.import.description", "Import a CoreProtect database: a SQLite file from plugins/Spyglass/import/, or a live MySQL source from import.conf."),
            Map.entry("help.entries.migrate.usage", "<backend>"),
            Map.entry("help.entries.migrate.description", "Copy all records into another configured storage backend."),
            Map.entry("help.entries.version.usage", ""),
            Map.entry("help.entries.version.description", "Show version and compatible update status."),
            Map.entry("help.entries.reload.usage", ""),
            Map.entry("help.entries.reload.description", "Reload live configuration settings (requires spyglass.reload)."),
            Map.entry("rbqueue.usage", "${prefix} <red>(Error)</red> Usage: /sg rbqueue [list | stop | cancel <id> | resume <id>]"),
            Map.entry("rbqueue.empty", "${prefix} No rollbacks running, queued, recent, or resumable."),
            Map.entry("rbqueue.resumable-header", "<red>» Resumable (interrupted by previous shutdown):</red>"),
            Map.entry("rbqueue.resumable-entry", "<red>  %id% (%mode%) by %operator% - query: %query% - started %started%</red>"),
            Map.entry("rbqueue.resumable-help", "<gray>  Use /sg rbqueue resume <id> to re-run, /sg rbqueue cancel <id> to discard.</gray>"),
            Map.entry("rbqueue.running-header", "<aqua>» Running: %id% (%mode%) by %operator% — %applied% applied, %skipped% skipped, %elapsed%</aqua>"),
            Map.entry("rbqueue.query", "<gray>    query: %query%</gray>"),
            Map.entry("rbqueue.queued-header", "<yellow>» Queued (%count%):</yellow>"),
            Map.entry("rbqueue.pending-entry", "<gray>  %position%. %id% (%mode%) by %operator% — query: %query%</gray>"),
            Map.entry("rbqueue.recent-header", "<dark_gray>» Recent:</dark_gray>"),
            Map.entry("rbqueue.recent-entry", "<dark_gray>  %id% %state% (%mode%) by %operator% — %applied% applied, %elapsed%</dark_gray>"),
            Map.entry("rbqueue.cancel-stopping", "<yellow>Cancel signal sent to %id%. Current chunk will finish, then the rollback stops.</yellow>"),
            Map.entry("rbqueue.discarded-resume", "<yellow>Discarded resume entry %id%.</yellow>"),
            Map.entry("rbqueue.removed-pending", "<yellow>Removed %id% from queue.</yellow>"),
            Map.entry("rbqueue.cancel-inflight", "<yellow>Cancel signal sent to %id% (in flight).</yellow>"),
            Map.entry("rbqueue.resuming", "<green>Resuming %id% — re-running query: %query%</green>"),
            Map.entry("rbqueue.no-resumable", "${prefix} <red>(Error)</red> No resumable job matching %id%. Use /sg rbqueue list to see resumables."),
            Map.entry("query.default-radius-hint", "<dark_gray>(no range; defaulting to %radius% blocks - add r:N or -g for global)</dark_gray>"),
            Map.entry("snapshot.chat.inventory-full", "${prefix} <yellow>That whole stack won't fit in your inventory - nothing taken.</yellow>"),
            Map.entry("snapshot.chat.no-permission", "${prefix} <red>You don't have permission to take from snapshots.</red>"),
            Map.entry("snapshot.chat.slot-empty", "${prefix} <red>Nothing to take in that slot.</red>"),
            Map.entry("snapshot.chat.expired", "${prefix} <red>That snapshot has expired - re-run /sg snapshot.</red>"),
            Map.entry("updates.status-pending", "${prefix} <gray>Update check pending.</gray>"),
            Map.entry("updates.status-disabled", "${prefix} <gray>Update checks disabled.</gray>"),
            Map.entry("updates.status-available", "${prefix} <yellow>Spyglass %version% is available for Minecraft %minecraft%: %url%</yellow>"),
            Map.entry("updates.status-none-compatible", "${prefix} <gray>No compatible published release found for Minecraft %minecraft%.</gray>"),
            Map.entry("updates.status-no-newer", "${prefix} <gray>No newer compatible release found for Minecraft %minecraft%.</gray>"),
            Map.entry("updates.status-unavailable", "${prefix} <gray>Update check unavailable; see GitHub releases or try again later.</gray>"),
            Map.entry("updates.available", "<yellow>Spyglass %version% is available for this Minecraft version. Click to view the release.</yellow>"),
            Map.entry("catalog.search.no-results", "No results."),
            Map.entry("catalog.search.no-active-results", "No active search results."),
            Map.entry("catalog.search.query-failed", "Query failed: %error%"),
            Map.entry("catalog.search.no-permission", "You do not have permission to query the '%action%' action."),
            Map.entry("catalog.rollback.no-permission", "You do not have permission to rollback the '%action%' action."),
            Map.entry("catalog.rollback.no-results", "No results."),
            Map.entry("catalog.rollback.queued", "Rollback queued at position %position% (id %id%). View / cancel with /sg rbqueue."),
            Map.entry("catalog.rollback.no-in-flight", "No rollback in flight."),
            Map.entry("catalog.rollback.unknown-job", "Unknown job id: %id%. Use /sg rbqueue list to see ids."),
            Map.entry("catalog.rollback.no-matching-job", "No matching pending or in-flight job."),
            Map.entry("catalog.undo.no-actions", "You have no valid actions to undo"),
            Map.entry("catalog.undo.player-only", "You must be a player to use this command"),
            Map.entry("catalog.undo.lookup-failed", "Undo lookup failed: %error%"),
            Map.entry("catalog.undo.failed", "Undo failed: %error%"),
            Map.entry("catalog.undo.salvage-warning", "Salvage entries for the undone rollback could not be withdrawn; /sg inventory may still list them."),
            Map.entry("catalog.import.running", "An import is already running; try again once it finishes."),
            Map.entry("catalog.import.failed", "Import failed: %error%"),
            Map.entry("catalog.import.complete", "Import complete for %name%: read=%read% written=%written% skipped=%skipped%"),
            Map.entry("catalog.import.reimport-mongo", "This source was already imported and re-importing into MongoDB is not supported; switch backends or restore from a MongoDB backup."),
            Map.entry("catalog.import.confirm-reimport", "This source was already imported on %time% by %operator% (read=%read%, written=%written%, skipped=%skipped%). Re-run with --confirm to import again."),
            Map.entry("catalog.migrate.running", "A migration is already running; try again once it finishes."),
            Map.entry("catalog.migrate.failed", "Migration failed: %error%"),
            Map.entry("catalog.migrate.unknown-backend", "Unknown backend '%backend%' (expected sqlite, mongo, clickhouse, or mariadb)."),
            Map.entry("catalog.migrate.active-backend", "%backend% is already the active backend; nothing to migrate."),
            Map.entry("catalog.migrate.import-running", "A CoreProtect import is running; wait for it before migrating."),
            Map.entry("catalog.teleport.world-not-found", "World not found: %world%"),
            Map.entry("catalog.teleport.invalid-coordinates", "Invalid teleport coordinates."),
            Map.entry("catalog.teleport.out-of-range", "Teleport coordinates out of range."),
            Map.entry("catalog.snapshot.unknown-player", "Unknown player: %player%"),
            Map.entry("catalog.snapshot.lookup-failed", "Snapshot lookup failed - see the console."),
            Map.entry("catalog.snapshot.no-snapshot", "No snapshot for %player% at or before %time%."),
            Map.entry("catalog.snapshot.reconstruction-failed", "Snapshot reconstruction failed - see the console."),
            Map.entry("catalog.snapshot.unknown-world", "Unknown world: %world%"),
            Map.entry("catalog.snapshot.not-container", "Not a container: %material%"),
            Map.entry("catalog.snapshot.not-container-history", "Not a container, and no container history at %target%."),
            Map.entry("catalog.snapshot.outside-world-border", "trg: is outside the world border."),
            Map.entry("catalog.snapshot.container-never-existed", "No container has ever existed at %location% - that chunk has never been generated."),
            Map.entry("catalog.snapshot.container-needs-target", "Container snapshot needs a player (to look at a block), or trg:x,y,z."),
            Map.entry("catalog.snapshot.no-block-in-reach", "No block in reach - look at a container, or use trg:x,y,z."),
            Map.entry("catalog.snapshot.console-needs-world", "trg: needs w: when run from the console."),
            Map.entry("catalog.snapshot.player-snapshots-disabled", "Player inventory snapshots are turned off."),
            Map.entry("catalog.snapshot.enable-player-snapshots", "Set snapshot.players.enabled = true in config.conf and restart to start recording them."),
            Map.entry("catalog.snapshot.container-snapshots-no-setting", "Container snapshots need no setting: look at a container, or use trg:x,y,z."),
            Map.entry("catalog.salvage.player-only", "Open rollback salvage in-game as a player."),
            Map.entry("catalog.salvage.disabled", "Container salvage is not enabled on this backend."),
            Map.entry("catalog.salvage.unavailable", "The rollback salvage GUI is unavailable. Contact an administrator."),
            Map.entry("catalog.salvage.open-failed", "Could not open the rollback salvage GUI. Contact an administrator."),
            Map.entry("catalog.snapshot.player-only", "Open snapshots in-game as a player."),
            Map.entry("catalog.snapshot.gui-unavailable", "The snapshot GUI is unavailable. Contact an administrator."),
            Map.entry("catalog.snapshot.open-failed", "Could not open the snapshot GUI. Contact an administrator."),
            Map.entry("catalog.tool.no-room", "Make room in your inventory for the Spyglass wand."),
            Map.entry("catalog.tool.added", "Added the Spyglass data tool to your inventory."),
            Map.entry("catalog.tool.activated", "<green>Activated the Spyglass Data Tool </green><gray>(%material%)</gray>"),
            Map.entry("catalog.tool.deactivated", "Deactivated the Spyglass Data Tool"),
            Map.entry("catalog.salvage.no-inventories", "No salvaged inventories."),
            Map.entry("catalog.salvage.inventory-full", "Your inventory is full."),
            Map.entry("commands.action-no-permission", "You do not have permission to query the '%action%' action."),
            Map.entry("commands.action-rollback-no-permission", "You do not have permission to rollback the '%action%' action."),
            Map.entry("commands.rollback-queued", "Rollback queued at position %position% (id %id%). View / cancel with /sg rbqueue."),
            Map.entry("commands.page-header", "<green>«</green><aqua>Spyglass</aqua><green>»</green> <gray>(page %page%/%total% - %results% results)</gray>"),
            Map.entry("results.ip-hidden", "(ip hidden)"),
            Map.entry("results.line", "<gray>= </gray><gray>%date%</gray> <aqua>%origin-tag%</aqua> %leading-tags% <green>%player%</green> <white>%action%</white> <green>%amount%</green> <aqua>%target%</aqua> <green>%count%</green> <white>%time%</white>"),
            Map.entry("results.event-lines.sign-edit",
                    "<gray>= </gray><gray>%date%</gray> <aqua>%origin-tag%</aqua> %leading-tags% <green>%player%</green> <white>%action%</white> <aqua>%target%</aqua> with %sign-text% <white>%time%</white>"),
            Map.entry("results.hover.default",
                    "<gray>Source: </gray>%source%\n"
                    + "<gray>Event: </gray>%event%\n"
                    + "<gray>Count: </gray>%count%\n"
                    + "<gray>Target: </gray>%target%\n"
                    + "<gray>When: </gray>%when%\n"
                    + "<gray>Origin: </gray>%origin%\n"
                    + "<gray>Location: </gray>%location%"
                    + "\n<gray>Server: </gray>%server%%details%"),
            Map.entry("tool.name", "<gold><bold>Spyglass Wand</bold></gold>"),
            Map.entry("time.just-now", "just now"),
            Map.entry("time.seconds", "%ss"),
            Map.entry("time.minutes", "%sm"),
            Map.entry("time.hours", "%sh"),
            Map.entry("time.days", "%sd"),
            Map.entry("time.weeks", "%sw")
    );

    private static volatile Messages instance = new Messages(DEFAULTS);
    private static final java.util.List<String> DEFAULT_HELP_COMMANDS = java.util.List.of(
            "help", "search", "page", "rollback", "restore", "undo", "tool", "events",
            "rbqueue", "inventory", "stats", "import", "migrate", "version", "reload");

    private final Map<String, String> templates;
    private final Map<String, java.util.List<String>> listTemplates;
    private final Map<String, java.util.List<String>> mapTemplates;

    public Messages(Map<String, String> templates) {
        this(templates, Collections.emptyMap(), Collections.emptyMap());
    }

    private Messages(Map<String, String> templates, Map<String, java.util.List<String>> listTemplates,
                     Map<String, java.util.List<String>> mapTemplates) {
        Map<String, String> merged = new HashMap<>(DEFAULTS);
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            if (entry.getKey().startsWith("catalog.")) {
                merged.put(entry.getKey(), defaultCatalogTemplate(entry.getKey(), entry.getValue()));
            }
        }
        if (templates != null) {
            merged.putAll(templates);
        }
        String prefix = merged.get("prefix");
        merged.replaceAll((key, value) -> value.replace("${prefix}", prefix));
        this.templates = Collections.unmodifiableMap(merged);

        Map<String, java.util.List<String>> lists = new HashMap<>();
        if (listTemplates != null) {
            listTemplates.forEach((key, value) -> {
                if (value != null) {
                    lists.put(key, value.stream()
                            .map(line -> line == null ? "" : line.replace("${prefix}", prefix))
                            .toList());
                }
            });
        }
        this.listTemplates = Collections.unmodifiableMap(lists);

        Map<String, java.util.List<String>> maps = new HashMap<>();
        if (mapTemplates != null) {
            mapTemplates.forEach((key, value) -> {
                if (value != null) {
                    maps.put(key, java.util.List.copyOf(value));
                }
            });
        }
        this.mapTemplates = Collections.unmodifiableMap(maps);

    }

    private static String defaultCatalogTemplate(String key, String body) {
        String prefix = "${prefix} ";
        if (key.equals("catalog.search.no-active-results")
                || key.equals("catalog.rollback.queued")
                || key.equals("catalog.undo.salvage-warning")
                || key.equals("catalog.import.complete")
                || key.equals("catalog.snapshot.enable-player-snapshots")
                || key.equals("catalog.snapshot.container-snapshots-no-setting")
                || key.equals("catalog.tool.added")
                || key.equals("catalog.tool.activated")
                || key.equals("catalog.tool.deactivated")) {
            return prefix + body;
        }
        return prefix + "<red>(Error)</red> " + body;
    }

    private static void migrateLegacyTemplates(Map<String, String> templates) {
        String version = templates.getOrDefault("config-version", "1");
        if (!version.equals("1")) {
            return;
        }

        String prefix = templates.getOrDefault("prefix", DEFAULTS.get("prefix"));
        for (Map.Entry<String, String> entry : new ArrayList<>(templates.entrySet())) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith("feedback.")) {
                String prefixToken = prefixToken(value, prefix);
                if (prefixToken != null) {
                    String remainder = value.substring(prefixToken.length());
                    if (remainder.startsWith("%message%")) {
                        templates.put(key, prefixToken + " " + remainder);
                    }
                } else {
                    templates.put(key, "${prefix} " + value);
                }
            } else if (key.startsWith("catalog.") && prefixToken(value, prefix) == null) {
                templates.put(key, defaultCatalogTemplate(key, value));
            } else if ((key.equals("commands.config-reloaded")
                    || key.equals("commands.config-reload-failed")
                    || key.equals("commands.config-restart-required"))
                    && prefixToken(value, prefix) == null) {
                templates.put(key, "${prefix} " + value);
            }
        }
    }

    private static String prefixToken(String value, String prefix) {
        if (value.startsWith("${prefix}")) {
            return "${prefix}";
        }
        return value.startsWith(prefix) ? prefix : null;
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
            Map<String, java.util.List<String>> lists = new HashMap<>();
            Map<String, java.util.List<String>> maps = new HashMap<>();
            flatten("", root, map, lists, maps);
            migrateLegacyTemplates(map);
            Messages loaded = new Messages(map, lists, maps);
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

    private static void flatten(String prefix, ConfigurationNode node,
                                Map<String, String> map,
                                Map<String, java.util.List<String>> lists,
                                Map<String, java.util.List<String>> maps) {
        if (node.isList()) {
            java.util.List<String> values = new java.util.ArrayList<>();
            for (ConfigurationNode child : node.childrenList()) {
                String value = child.getString();
                if (value != null) {
                    values.add(value);
                }
            }
            lists.put(prefix, java.util.List.copyOf(values));
        } else if (node.isMap()) {
            java.util.List<String> keys = new java.util.ArrayList<>();
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
                String key = String.valueOf(entry.getKey());
                keys.add(key);
                String nextPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                flatten(nextPrefix, entry.getValue(), map, lists, maps);
            }
            maps.put(prefix, java.util.List.copyOf(keys));
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

    /**
     * A configured list value (for example {@code tool.lore}). A scalar at the
     * same key is treated as a one-line list; an absent key is empty.
     */
    public java.util.List<String> rawList(String key) {
        java.util.List<String> list = listTemplates.get(key);
        if (list != null) {
            return list;
        }
        java.util.List<String> mapKeys = mapTemplates.get(key);
        if (mapKeys != null) {
            return mapKeys;
        }
        if (key.equals("help.commands")) {
            return DEFAULT_HELP_COMMANDS;
        }
        String scalar = templates.get(key);
        return scalar != null ? java.util.List.of(scalar) : java.util.List.of();
    }

    public boolean hasListTemplate(String key) {
        return listTemplates.containsKey(key);
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
        // Translate legacy codes before substituting placeholders, so a player
        // or plugin-supplied value containing "&c" stays literal text instead
        // of becoming formatting.
        String template = LegacyColors.translate(raw(key));
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
        return deserialize(template);
    }

    public Component formatComponents(String key, Map<String, Component> placeholders) {
        return formatComponentTemplate(raw(key), placeholders);
    }

    public Component formatComponentTemplate(String template, Map<String, Component> placeholders) {
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

    public Component feedback(String catalogKey, CommandSender sender, Object... pairs) {
        return format(catalogKey, sender, pairs);
    }

    public Component errorFeedback(String catalogKey, CommandSender sender, Object... pairs) {
        String catalogTemplate = raw(catalogKey);
        String errorMarker = "<red>(Error)</red> ";
        int markerIndex = catalogTemplate.indexOf(errorMarker);
        if (markerIndex < 0) {
            return format(catalogKey, sender, pairs);
        }

        String body = formatText(catalogTemplate.substring(markerIndex + errorMarker.length()),
                sender, toMap(pairs));
        String marker = "SPYGLASSERROR" + java.util.UUID.randomUUID().toString().replace("-", "") + "X";
        String errorTemplate = formatText(raw("feedback.error").replace("%message%", marker),
                sender, Collections.emptyMap());
        Component formatted = deserialize(errorTemplate);
        return formatted.replaceText(TextReplacementConfig.builder()
                .matchLiteral(marker)
                .replacement(deserialize(body))
                .build());
    }

    private static String formatText(String template, CommandSender sender,
                                     Map<String, String> placeholders) {
        template = LegacyColors.translate(template);
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue() : "";
                template = template.replace(entry.getKey(), MINI_MESSAGE.escapeTags(value));
            }
        }
        return sender == null ? template : PlaceholderSupport.apply(sender, template);
    }

    private static Component deserialize(String template) {
        try {
            return MINI_MESSAGE.deserialize(template);
        } catch (Exception ex) {
            return Component.text(template);
        }
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

    /** Display name for the inspector wand ({@code tool.name}). */
    public Component toolName() {
        return format("tool.name", (CommandSender) null, Collections.emptyMap())
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    /** Lore lines for the inspector wand ({@code tool.lore}); empty = no lore. */
    public java.util.List<Component> toolLore() {
        return rawList("tool.lore").stream()
                .map(line -> deserialize(LegacyColors.translate(line))
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                .toList();
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
