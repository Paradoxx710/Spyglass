package net.medievalrp.spyglass.plugin.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.medievalrp.spyglass.api.SpyglassApi;
import net.medievalrp.spyglass.plugin.command.render.Feedback;
import net.medievalrp.spyglass.plugin.command.service.HelpService;
import net.medievalrp.spyglass.plugin.command.service.RbqueueService;
import net.medievalrp.spyglass.plugin.command.service.RollbackMode;
import net.medievalrp.spyglass.plugin.command.service.RollbackService;
import net.medievalrp.spyglass.plugin.command.service.SalvageService;
import net.medievalrp.spyglass.plugin.command.service.SearchService;
import net.medievalrp.spyglass.plugin.command.service.SnapshotService;
import net.medievalrp.spyglass.plugin.command.service.StatsService;
import net.medievalrp.spyglass.plugin.command.service.TeleportService;
import net.medievalrp.spyglass.plugin.command.service.ToolService;
import net.medievalrp.spyglass.plugin.command.service.UndoService;
import net.medievalrp.spyglass.plugin.config.SpyglassConfig;
import net.medievalrp.spyglass.plugin.imports.ImportConfig;
import net.medievalrp.spyglass.plugin.imports.ImportPaths;
import net.medievalrp.spyglass.plugin.imports.ImportService;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class SpyglassCommands {

    // /sg is the short backend alias; /sgv on the Velocity proxy stays
    // namespace-distinct so the proxy never shadows the Paper command.
    // /s joins unless commands.s-alias is disabled in config (#279, opt-out;
    // was opt-in under #250) - an operator turns it off when another plugin
    // on the server claims the single-letter root. Every name here comes from
    // commands.aliases.root, so an operator can edit the whole list.
    private final List<String> rootAliases;

    static List<String> rootAliases(boolean sAlias) {
        return sAlias ? List.of("spyglass", "sg", "s") : List.of("spyglass", "sg");
    }

    private final JavaPlugin plugin;
    private final SpyglassApi api;
    private final SpyglassConfig.Commands commands;
    private final HelpService help;
    private final SearchService search;
    private final RollbackService rollback;
    private final UndoService undo;
    private final RbqueueService rbqueue;
    private final PageCache pageCache;
    private final ToolService tool;
    private final TeleportService teleport;
    private final SalvageService salvage;
    private final SnapshotService snapshot;
    private final StatsService stats;
    private final SpyglassSuggestions suggestions;
    private final ImportService imports;
    private final ImportConfig importConfig;
    private final Path importDir;
    private final net.medievalrp.spyglass.plugin.migrate.MigrateService migrate;

    public SpyglassCommands(JavaPlugin plugin,
                        SpyglassApi api,
                        HelpService help,
                        SearchService search,
                        RollbackService rollback,
                        UndoService undo,
                        RbqueueService rbqueue,
                        PageCache pageCache,
                        ToolService tool,
                        TeleportService teleport,
                        SalvageService salvage,
                        SnapshotService snapshot,
                        StatsService stats,
                        SpyglassSuggestions suggestions,
                        ImportService imports,
                        ImportConfig importConfig,
                        Path importDir,
                        net.medievalrp.spyglass.plugin.migrate.MigrateService migrate,
                        SpyglassConfig.Commands commands) {
        this.plugin = plugin;
        this.api = api;
        this.commands = commands != null
                ? commands
                : new SpyglassConfig.Commands(true, SpyglassConfig.DEFAULT_COMMAND_ALIASES);
        this.help = help;
        this.search = search;
        this.rollback = rollback;
        this.undo = undo;
        this.rbqueue = rbqueue;
        this.pageCache = pageCache;
        this.tool = tool;
        this.teleport = teleport;
        this.salvage = salvage;
        this.snapshot = snapshot;
        this.stats = stats;
        this.suggestions = suggestions;
        this.imports = imports;
        this.importConfig = importConfig;
        this.importDir = importDir;
        this.migrate = migrate;
        this.rootAliases = names("root", "spyglass");
    }

    /**
     * Canonical name first, then the configured extras from
     * {@code commands.aliases}. LinkedHashSet keeps registration order stable
     * and drops an alias that repeats the canonical name.
     */
    private List<String> names(String command, String canonical) {
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        names.add(canonical);
        names.addAll(commands.extraAliases(command));
        return List.copyOf(names);
    }

    // v1-compat subcommand aliases. Operators have years of muscle memory
    // around {@code /spyglass l} and {@code /spyglass rb}; v2 originally exposed
    // only the long names. Every extra name now lives in commands.aliases in
    // config.conf, so deleting one there unregisters it while the canonical
    // name (help, search, rollback, ...) keeps working.

    /** Support Discord shown by /sg version - the invite the README publishes. */
    private static final String SUPPORT_INVITE = "discord.gg/7kxDxMYKyW";
    private static final String SUPPORT_URL = "https://" + SUPPORT_INVITE;

    private java.util.function.Consumer<CommandSender> reloadHandler = sender -> {};
    private java.util.function.Supplier<String> updateStatus = () -> "";

    public void setReloadHandler(java.util.function.Consumer<CommandSender> handler) { reloadHandler = handler; }
    public void setUpdateStatus(java.util.function.Supplier<String> status) { updateStatus = status; }

    public CommandManager<CommandSender> register() {
        LegacyPaperCommandManager<CommandSender> manager = LegacyPaperCommandManager.createNative(
                plugin, ExecutionCoordinator.simpleCoordinator());
        for (String root : rootAliases) {
            for (String name : names("reload", "reload")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .permission("spyglass.reload")
                        .handler(ctx -> reloadHandler.accept(ctx.sender())));
            }
            manager.command(manager.commandBuilder(root)
                    .permission("spyglass.use")
                    .handler(ctx -> help.send(ctx.sender())));

            for (String name : names("help", "help")) {
                // Optional page (#249): the full command list overflows chat,
                // so /sg help paginates and /sg help <n> jumps to a page.
                manager.command(manager.commandBuilder(root).literal(name)
                        .optional("page", IntegerParser.integerParser(1))
                        .permission("spyglass.use")
                        .handler(ctx -> help.send(ctx.sender(),
                                ctx.<Integer>optional("page").orElse(1))));
            }

            for (String name : names("events", "events")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .permission("spyglass.use")
                        .handler(ctx -> sendEnabledEvents(ctx.sender())));
            }

            for (String name : names("version", "version")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .permission("spyglass.use")
                        .handler(ctx -> sendVersion(ctx.sender())));
            }

            // #168: /spyglass stats - on-demand ingest analytics snapshot.
            for (String name : names("stats", "stats")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .permission("spyglass.use")
                        .handler(ctx -> stats.execute(ctx.sender())));
            }

            for (String name : names("search", "search")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .required("params", suggestions.paramsParser(), suggestions.paramsProvider())
                        .permission("spyglass.search")
                        .handler(ctx -> search.execute(ctx.sender(), ctx.get("params"))));
            }

            for (String name : names("rollback", "rollback")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .required("params", suggestions.paramsParser(), suggestions.paramsProvider())
                        .permission("spyglass.rollback")
                        .handler(ctx -> rollback.execute(ctx.sender(), ctx.get("params"), RollbackMode.ROLLBACK)));
            }

            for (String name : names("restore", "restore")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .required("params", suggestions.paramsParser(), suggestions.paramsProvider())
                        .permission("spyglass.rollback")
                        .handler(ctx -> rollback.execute(ctx.sender(), ctx.get("params"), RollbackMode.RESTORE)));
            }

            for (String name : names("undo", "undo")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .permission("spyglass.rollback")
                        .handler(ctx -> undo.execute(ctx.sender())));
            }

            for (String name : names("rbqueue", "rbqueue")) {
                // /sg rbqueue (no args = list)
                manager.command(manager.commandBuilder(root).literal(name)
                        .permission("spyglass.rollback")
                        .handler(ctx -> rbqueue.execute(ctx.sender(), "")));
                // /sg rbqueue <subcommand-with-args> — pass through
                manager.command(manager.commandBuilder(root).literal(name)
                        .required("args", StringParser.greedyStringParser())
                        .permission("spyglass.rollback")
                        .handler(ctx -> rbqueue.execute(ctx.sender(), ctx.get("args"))));
            }

            for (String name : names("page", "page")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .required("number", IntegerParser.integerParser(1))
                        .permission("spyglass.use")
                        .handler(ctx -> pageCache.show(ctx.sender(), ctx.get("number"))));
            }

            for (String name : names("tool", "tool")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .permission("spyglass.tool")
                        .handler(ctx -> tool.toggle(ctx.sender())));
            }

            for (String name : names("inventory", "inventory")) {
                // Gated on its own node, not spyglass.rollback: pulling items out
                // of a rollback-destroyed container is a distinct capability from
                // running a rollback, so an operator can grant recovery without
                // granting rollback (and vice versa). See plugin.yml (#199).
                // Open the in-game inventory GUI.
                manager.command(manager.commandBuilder(root).literal(name)
                        .permission("spyglass.salvage")
                        .handler(ctx -> salvage.execute(ctx.sender())));

            }

            for (String name : names("snapshot", "snapshot")) {
                // /sg snapshot <params>: view a player inventory or container as
                // of a past instant (#341). Greedy params (t:/p:/trg:/w:), the
                // house style; whole-span suggestions keep multi-key completion
                // alive (#55). Gated on spyglass.snapshot - viewing without the
                // take grant is legitimate.
                manager.command(manager.commandBuilder(root).literal(name)
                        .required("params", suggestions.snapshotParamsParser(),
                                suggestions.snapshotParamsProvider())
                        .permission("spyglass.snapshot")
                        .handler(ctx -> snapshot.execute(ctx.sender(), ctx.get("params"))));

            }

            // /spyglass tele <world> <x> <y> <z> — wired to search-result click
            // events so staff can jump to the scene of an incident.
            for (String name : names("tele", "tele")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .required("world", StringParser.stringParser())
                        .required("x", StringParser.stringParser())
                        .required("y", StringParser.stringParser())
                        .required("z", StringParser.stringParser())
                        .permission("spyglass.tele")
                        .handler(ctx -> teleport.execute(ctx.sender(),
                                ctx.get("world"), ctx.get("x"), ctx.get("y"), ctx.get("z"))));
            }

            // /spyglass import <file> — SQLite import from plugins/Spyglass/import/
            // /spyglass import mysql <source> — live MySQL import from import.conf
            for (String name : names("import", "import")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .required("file", StringParser.stringParser(), suggestions.importFileProvider())
                        .flag(manager.flagBuilder("confirm").build())
                        .permission("spyglass.import")
                        .handler(ctx -> handleImportFile(ctx.sender(),
                                ctx.get("file"), ctx.flags().isPresent("confirm"))));

                manager.command(manager.commandBuilder(root).literal(name).literal("mysql")
                        .required("source", StringParser.stringParser(), suggestions.importSourceProvider())
                        .flag(manager.flagBuilder("confirm").build())
                        .permission("spyglass.import")
                        .handler(ctx -> handleImportMysql(ctx.sender(),
                                ctx.get("source"), ctx.flags().isPresent("confirm"))));
            }

            // /spyglass migrate <backend> — copy the active backend's records
            // into another configured backend (internal storage migration).
            // Validation (unknown/active/unconfigured target) happens inside
            // MigrateService, which messages the sender for every outcome.
            for (String name : names("migrate", "migrate")) {
                manager.command(manager.commandBuilder(root).literal(name)
                        .required("backend", StringParser.stringParser(),
                                suggestions.migrateBackendProvider())
                        .flag(manager.flagBuilder("confirm").build())
                        .permission("spyglass.migrate")
                        .handler(ctx -> migrate.migrate(ctx.sender(),
                                ctx.get("backend"), ctx.flags().isPresent("confirm"))));
            }
        }
        return manager;
    }

    // /spyglass import <file>: resolve inside the import dir (guards path
    // traversal) then hand off to ImportService, which messages the sender
    // asynchronously for every guard/finish outcome. Only the synchronous
    // "bad file" failure is reported here.
    private void handleImportFile(CommandSender sender, String file, boolean confirm) {
        Path resolved;
        try {
            resolved = ImportPaths.resolveInside(importDir, file);
        } catch (IOException ex) {
            sender.sendMessage(Feedback.error(ex.getMessage()));
            return;
        }
        imports.importSqlite(sender, resolved, confirm);
    }

    // /spyglass import mysql <source>: look the name up in import.conf; an
    // unknown source is reported here, everything else is ImportService's job.
    private void handleImportMysql(CommandSender sender, String source, boolean confirm) {
        Optional<ImportConfig.MysqlSourceSpec> spec = importConfig.source(source);
        if (spec.isEmpty()) {
            String available = importConfig.sources().keySet().stream().sorted()
                    .reduce((a, b) -> a + ", " + b).orElse("(none configured)");
            sender.sendMessage(Feedback.error(
                    "Unknown import source '" + source + "'. Available: " + available));
            return;
        }
        imports.importMysql(sender, source, spec.get(), confirm);
    }

    private void sendEnabledEvents(CommandSender sender) {
        String joined = api.enabledEvents().stream().sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("(none)");
        sender.sendMessage(Feedback.success("Enabled Events: "));
        sender.sendMessage(Feedback.bonus(joined));
    }

    private void sendVersion(CommandSender sender) {
        String status = updateStatus.get();
        if (!status.isBlank()) sender.sendMessage(Feedback.bonus(status));
        for (Component line : versionLines(plugin.getPluginMeta().getVersion(),
                plugin.getPluginMeta().getAuthors(),
                plugin.getServer().getMinecraftVersion())) {
            sender.sendMessage(line);
        }
    }

    // Package-private and pure so the formatting is unit-testable without a
    // live server or command manager.
    static List<Component> versionLines(String version, List<String> authors, String serverVersion) {
        String by = authors.isEmpty() ? "" : "by " + String.join(", ", authors) + ", ";
        return List.of(
                Feedback.success("Spyglass v" + version),
                Feedback.bonus(by + "on Paper " + serverVersion),
                supportLine());
    }

    /**
     * The support Discord, same invite the README publishes. Rendered as the
     * bare host + code rather than the full URL so the line stays short in
     * chat, with the real link on the click event; a console sender cannot
     * click but still reads the address.
     */
    private static Component supportLine() {
        return Component.text()
                .append(Component.text("Support: ", NamedTextColor.GRAY))
                .append(Component.text(SUPPORT_INVITE, NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(SUPPORT_URL))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Click to open the support Discord",
                                        NamedTextColor.GRAY))))
                .asComponent();
    }
}
