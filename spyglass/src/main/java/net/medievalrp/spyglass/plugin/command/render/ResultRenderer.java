package net.medievalrp.spyglass.plugin.command.render;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.medievalrp.spyglass.api.SpyglassApi;
import net.medievalrp.spyglass.api.event.BlockBreakRecord;
import net.medievalrp.spyglass.api.event.BlockPlaceRecord;
import net.medievalrp.spyglass.api.event.BlockSnapshot;
import net.medievalrp.spyglass.api.event.ChatRecord;
import net.medievalrp.spyglass.api.event.CommandRecord;
import net.medievalrp.spyglass.api.event.BlockUseRecord;
import net.medievalrp.spyglass.api.event.ContainerDepositRecord;
import net.medievalrp.spyglass.api.event.ContainerInteractRecord;
import net.medievalrp.spyglass.api.event.ContainerWithdrawRecord;
import net.medievalrp.spyglass.api.event.CustomRecord;
import net.medievalrp.spyglass.api.event.EntityDeathRecord;
import net.medievalrp.spyglass.api.event.EntityHitRecord;
import net.medievalrp.spyglass.api.event.EntityMountRecord;
import net.medievalrp.spyglass.api.event.EntityNameRecord;
import net.medievalrp.spyglass.api.event.EventRecord;
import net.medievalrp.spyglass.api.event.ItemDropRecord;
import net.medievalrp.spyglass.api.event.ItemPickupRecord;
import net.medievalrp.spyglass.api.event.JoinRecord;
import net.medievalrp.spyglass.api.event.Origin;
import net.medievalrp.spyglass.api.event.QuitRecord;
import net.medievalrp.spyglass.api.event.RollbackOpRecord;
import net.medievalrp.spyglass.api.event.Source;
import net.medievalrp.spyglass.api.event.StoredItem;
import net.medievalrp.spyglass.api.event.TeleportRecord;
import net.medievalrp.spyglass.api.extension.DisplayRenderer;
import net.medievalrp.spyglass.api.query.QueryResult;
import net.medievalrp.spyglass.api.util.BlockLocation;
import net.medievalrp.spyglass.plugin.config.SpyglassConfig;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class ResultRenderer {

    private final SpyglassApi api;
    public void setConfig(SpyglassConfig config) { this.config = config; }

    private volatile SpyglassConfig config;

    public ResultRenderer(SpyglassApi api, SpyglassConfig config) {
        this.api = api;
        this.config = config;
    }

    /** Stand-in shown where a join IP would render when the viewer
     *  lacks {@code spyglass.search.ip} (#48). */
    public static final String IP_HIDDEN = "(ip hidden)";

    public Component renderAggregation(QueryResult.RecordAggregation aggregation,
                                       java.util.EnumSet<net.medievalrp.spyglass.api.query.Flag> flags,
                                       boolean showIp) {
        return renderAggregation(aggregation, flags, showIp, false);
    }

    public Component renderAggregation(QueryResult.RecordAggregation aggregation,
                                       java.util.EnumSet<net.medievalrp.spyglass.api.query.Flag> flags,
                                       boolean showIp, boolean rolledBack) {
        EventRecord sample = aggregation.sample();
        long count = aggregation.count();
        java.util.EnumSet<net.medievalrp.spyglass.api.query.Flag> safe = flags == null
                ? java.util.EnumSet.noneOf(net.medievalrp.spyglass.api.query.Flag.class)
                : flags;
        // Aggregations span multiple locations; `-ex` location-append is
        // skipped because any one sample.location() would be misleading.
        // The flags ride through so the drill-down click command can
        // carry the parent search's scope (notably -g) forward.
        return line(sample, count, true, false, safe, showIp, rolledBack);
    }

    public Component renderSingle(EventRecord record, java.util.EnumSet<net.medievalrp.spyglass.api.query.Flag> flags,
                                  boolean showIp) {
        return renderSingle(record, flags, showIp, false);
    }

    public Component renderSingle(EventRecord record, java.util.EnumSet<net.medievalrp.spyglass.api.query.Flag> flags,
                                  boolean showIp, boolean rolledBack) {
        java.util.EnumSet<net.medievalrp.spyglass.api.query.Flag> safe = flags == null
                ? java.util.EnumSet.noneOf(net.medievalrp.spyglass.api.query.Flag.class)
                : flags;
        boolean extended = safe.contains(net.medievalrp.spyglass.api.query.Flag.EXTENDED);
        return line(record, 1, false, extended, safe, showIp, rolledBack);
    }

    private Component line(EventRecord record, long count, boolean grouped, boolean extended,
                           java.util.EnumSet<net.medievalrp.spyglass.api.query.Flag> flags,
                           boolean showIp, boolean rolledBack) {
        String tag = originTag(record.origin());
        Component renderedLeadingTags = Component.empty();
        for (Component leadingTag : leadingTags(record)) {
            renderedLeadingTags = renderedLeadingTags.append(leadingTag);
        }
        int qty = quantityOf(record);
        // Target span: skipped entirely when the record has nothing
        // useful to put there (e.g. QuitRecord), otherwise rendered as
        // " TARGET" with a single leading space so the line stays
        // tidy regardless of whether quantity/target are present.
        String targetText = targetOf(record, showIp);
        Component renderedTarget = Component.empty();
        if (!targetText.isEmpty()) {
            Component defaultTarget = Component.text(targetText);
            java.util.Optional<DisplayRenderer> custom = api == null
                    ? java.util.Optional.empty()
                    : api.displayRenderer(record.event());
            Component target = custom
                    .map(renderer -> {
                        try {
                            return renderer.renderTarget(record, defaultTarget, flags);
                        } catch (RuntimeException ex) {
                            return defaultTarget;
                        }
                    })
                    .orElse(defaultTarget);
            renderedTarget = target;
        }

        Map<String, Component> placeholders = new LinkedHashMap<>();
        placeholders.put("%date%", grouped
                ? Component.text("(" + shortDate(record.occurred()) + ")")
                : Component.empty());
        placeholders.put("%origin-tag%", tag.isEmpty()
                ? Component.empty()
                : Component.text(tag));
        placeholders.put("%leading-tags%", renderedLeadingTags);
        placeholders.put("%player%", Component.text(displaySourceName(record)));
        placeholders.put("%source%", Component.text(displaySourceName(record)));
        placeholders.put("%action%", Component.text(verb(record)));
        placeholders.put("%amount%", qty > 0
                ? Component.text(String.valueOf(qty))
                : Component.empty());
        placeholders.put("%target%", renderedTarget);
        String signText = signText(record);
        placeholders.put("%sign-text%", textOrEmpty(signText));
        placeholders.put("%count%", grouped
                ? Component.text("x" + count)
                : Component.empty());
        placeholders.put("%time%", Component.text(timeAgo(record.occurred())));
        Messages messages = Messages.getInstance();
        String eventLineKey = "results.event-lines." + record.event();
        String lineKey = messages.hasTemplate(eventLineKey) ? eventLineKey : "results.line";
        String lineTemplate = messages.raw(lineKey);
        for (String optional : List.of(
                "%date%", "%origin-tag%", "%leading-tags%", "%amount%", "%target%", "%count%")) {
            if (placeholders.get(optional).equals(Component.empty())) {
                lineTemplate = removeSeparatorAfter(lineTemplate, optional);
            }
        }
        Component rendered = messages.formatComponentTemplate(lineTemplate, placeholders)
                .hoverEvent(HoverEvent.showText(hover(record, count, showIp)));
        if (grouped) {
            // Grouped result: click drills down into the per-player,
            // per-event stream. Matches v1's buildDetailCommand shape.
            // Carry -g forward when the parent search was global: the
            // drill-down command has no location predicate, so without
            // -g the re-parse re-applies the default radius around the
            // operator's *current* position and finds nothing — the
            // grouped rows were gathered globally, not near the operator.
            String drill = "/spyglass search "
                    + "a:" + record.event()
                    + " p:" + record.sourceName()
                    + " -ng";
            if (flags.contains(net.medievalrp.spyglass.api.query.Flag.GLOBAL)) {
                drill += " -g";
            }
            rendered = rendered.clickEvent(ClickEvent.runCommand(drill));
        } else if (record.location() != null) {
            // Single record: click teleports the operator to the scene.
            rendered = rendered.clickEvent(teleportClick(record.location()));
        }
        if (extended && record.location() != null) {
            // v1's `-ex`: a second inline gray line showing coordinates,
            // also clickable for teleport (redundant with the main line
            // click but consistent with v1's DataHelper.buildLocation).
            rendered = rendered.append(Component.newline())
                    .append(Component.text(
                                    " - (x: " + record.location().x()
                                            + " y: " + record.location().y()
                                            + " z: " + record.location().z()
                                            + " world: " + record.location().worldName() + ")",
                                    NamedTextColor.GRAY)
                            .clickEvent(teleportClick(record.location()))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("Click to teleport", NamedTextColor.GRAY))));
        }
        // A reverted record renders muted so "already rolled back" reads at a
        // glance, CoreProtect-style (#319): default gray + struck through +
        // italic on the content, while the leading "= " marker keeps its
        // normal styling.
        return rolledBack ? mutedRolledBack(rendered) : rendered;
    }

    /**
     * Mute a finished row: leave the leading "= " marker (the first child)
     * alone, and gray + strike-through + italicize everything after it. The
     * click and hover ride in the root/child styles and survive the recolor;
     * the hover tooltip is not a child, so it keeps its own colors.
     */
    private static Component mutedRolledBack(Component row) {
        List<Component> children = row.children();
        if (children.isEmpty()) {
            return row;
        }
        List<Component> muted = new ArrayList<>(children.size());
        muted.add(children.get(0)); // "= " marker stays normal - not struck
        for (int index = 1; index < children.size(); index++) {
            muted.add(muteDeep(children.get(index)));
        }
        return row.children(muted);
    }

    /**
     * Default gray + struck through + italic, applied to every node so it
     * overrides the per-span green/aqua/white. No bold.
     */
    private static Component muteDeep(Component component) {
        Component restyled = component.color(NamedTextColor.GRAY)
                .decorate(TextDecoration.STRIKETHROUGH)
                .decorate(TextDecoration.ITALIC);
        if (component.children().isEmpty()) {
            return restyled;
        }
        List<Component> children = new ArrayList<>(component.children().size());
        for (Component child : component.children()) {
            children.add(muteDeep(child));
        }
        return restyled.children(children);
    }

    /**
     * Leading tags contributed by a registered {@link DisplayRenderer} for
     * this record's event. Mirrors the {@code renderTarget} contract: a
     * {@code null} return or a thrown {@link RuntimeException} yields no tags.
     */
    private List<Component> leadingTags(EventRecord record) {
        if (api == null) {
            return List.of();
        }
        return api.displayRenderer(record.event())
                .map(renderer -> {
                    try {
                        List<Component> tags = renderer.leadingTags(record);
                        return tags == null ? List.<Component>of() : tags;
                    } catch (RuntimeException ex) {
                        return List.<Component>of();
                    }
                })
                .orElse(List.of());
    }

    private static ClickEvent teleportClick(BlockLocation loc) {
        return ClickEvent.runCommand("/spyglass tele "
                + loc.worldId() + " " + loc.x() + " " + loc.y() + " " + loc.z());
    }

    private String shortDate(Instant occurred) {
        return occurred.atZone(config.timezone())
                .format(DateTimeFormatter.ofPattern("d/M/yyyy"));
    }

    public static Component pageHeader(int page, int totalPages, int totalResults) {
        // Each arrow only renders when there's a page to go to. On page 1
        // the left disappears entirely; on the last page the right does.
        // Single-page results show no arrows at all.
        net.kyori.adventure.text.TextComponent.Builder builder = Component.text()
                .append(Messages.getInstance().format("commands.page-header",
                        "%page%", String.valueOf(page),
                        "%total%", String.valueOf(totalPages),
                        "%results%", String.valueOf(totalResults)));
        if (page > 1) {
            builder.append(Component.text(" ", NamedTextColor.WHITE))
                    .append(navButton("←", page - 1));
        }
        if (page < totalPages) {
            builder.append(Component.text(" ", NamedTextColor.WHITE))
                    .append(navButton("→", page + 1));
        }
        return builder.asComponent();
    }

    private static String removeSeparatorAfter(String template, String placeholder) {
        int cursor = template.indexOf(placeholder);
        if (cursor < 0) {
            return template;
        }
        cursor += placeholder.length();
        while (cursor < template.length() && template.charAt(cursor) == '<') {
            int tagEnd = template.indexOf('>', cursor);
            if (tagEnd < 0) {
                return template;
            }
            cursor = tagEnd + 1;
        }
        if (cursor < template.length() && template.charAt(cursor) == ' ') {
            return template.substring(0, cursor) + template.substring(cursor + 1);
        }
        return template;
    }

    private static Component navButton(String arrow, int targetPage) {
        Component label = Component.text()
                .append(Component.text("[", NamedTextColor.RED))
                .append(Component.text(arrow, NamedTextColor.RED))
                .append(Component.text("]", NamedTextColor.RED))
                .asComponent();
        return label
                .clickEvent(ClickEvent.runCommand("/spyglass page " + targetPage))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Page " + targetPage, NamedTextColor.RED)));
    }

    private Component hover(EventRecord record, long count, boolean showIp) {
        List<Component> details = new ArrayList<>();
        String recipients = "";
        String commandLine = "";
        String ip = "";
        String damage = "";
        String weapon = "";
        String cause = "";
        String via = "";
        if (record instanceof ChatRecord chat && !chat.recipients().isEmpty()) {
            recipients = recipientNames(chat.recipients());
            details.add(kv("Recipients", recipients));
        }
        if (record instanceof CommandRecord command) {
            commandLine = command.commandLine();
            details.add(kv("Line", commandLine));
        }
        if (record instanceof JoinRecord join && join.address() != null && !join.address().isBlank()) {
            ip = showIp ? join.address() : IP_HIDDEN;
            details.add(kv("IP", ip));
        }
        if (record instanceof EntityHitRecord hit) {
            damage = String.format(Locale.ROOT, "%.1f", hit.damage());
            details.add(kv("Damage", damage));
            if (hit.projectile()) {
                weapon = hit.projectileType() == null
                        ? "projectile" : upperOrEmpty(hit.projectileType());
                details.add(kv("Weapon", weapon));
            }
        }
        if (record instanceof EntityDeathRecord death && death.damageCause() != null
                && !death.damageCause().isBlank()) {
            cause = death.damageCause();
            details.add(kv("Cause", cause));
        }
        if (record instanceof TeleportRecord tp && tp.cause() != null && !tp.cause().isBlank()) {
            via = tp.cause();
            details.add(kv("Via", via));
        }
        StringBuilder extensionValues = new StringBuilder();
        record.extensions().forEach((key, value) -> {
            details.add(kv(capitalizeKey(key), value));
            if (extensionValues.length() > 0) {
                extensionValues.append('\n');
            }
            extensionValues.append(capitalizeKey(key)).append(": ").append(value);
        });
        StoredItem item = subjectItem(record);
        String itemName = item == null || item.name() == null ? "" : item.name();
        String itemEnchants = item == null ? "" : String.join(", ", item.enchants());
        String itemTags = itemTags(item);
        String itemLore = item == null ? "" : String.join("\n", visibleLore(item.lore()));
        appendItemDetail(details, record);
        if (api != null) {
            api.displayRenderer(record.event()).ifPresent(renderer -> {
                try {
                    List<Component> extraLines = renderer.hoverLines(record);
                    if (extraLines != null) {
                        details.addAll(extraLines);
                    }
                } catch (RuntimeException ignored) {
                }
            });
        }

        int amount = quantityOf(record);
        String timestamp = fullTimestamp(record.occurred());
        Map<String, Component> placeholders = new LinkedHashMap<>();
        placeholders.put("%source%", textOrEmpty(record.sourceName()));
        placeholders.put("%player%", textOrEmpty(record.sourceName()));
        placeholders.put("%event%", textOrEmpty(record.event()));
        placeholders.put("%action%", textOrEmpty(verb(record)));
        placeholders.put("%count%", textOrEmpty(count > 1 ? String.valueOf(count) : ""));
        placeholders.put("%target%", textOrEmpty(targetOf(record, showIp)));
        placeholders.put("%sign-text%", textOrEmpty(signText(record)));
        placeholders.put("%when%", textOrEmpty(timestamp));
        placeholders.put("%time%", textOrEmpty(timestamp));
        placeholders.put("%time-ago%", textOrEmpty(timeAgo(record.occurred())));
        placeholders.put("%date%", textOrEmpty(shortDate(record.occurred())));
        placeholders.put("%origin%", textOrEmpty(originText(record.origin())));
        placeholders.put("%origin-tag%", textOrEmpty(originTag(record.origin())));
        placeholders.put("%location%", textOrEmpty(locationText(record.location())));
        placeholders.put("%server%", textOrEmpty(record.server()));
        placeholders.put("%amount%", textOrEmpty(amount > 0 ? String.valueOf(amount) : ""));
        placeholders.put("%recipients%", textOrEmpty(recipients));
        placeholders.put("%command-line%", textOrEmpty(commandLine));
        placeholders.put("%ip%", textOrEmpty(ip));
        placeholders.put("%damage%", textOrEmpty(damage));
        placeholders.put("%weapon%", textOrEmpty(weapon));
        placeholders.put("%cause%", textOrEmpty(cause));
        placeholders.put("%via%", textOrEmpty(via));
        placeholders.put("%extensions%", textOrEmpty(extensionValues.toString()));
        placeholders.put("%item-name%", textOrEmpty(itemName));
        placeholders.put("%item-enchants%", textOrEmpty(itemEnchants));
        placeholders.put("%item-tags%", textOrEmpty(itemTags));
        placeholders.put("%item-lore%", textOrEmpty(itemLore));
        placeholders.put("%count-line%", count > 1
                ? Component.newline().append(kv("Count", String.valueOf(count)))
                : Component.empty());
        placeholders.put("%server-line%", record.server() != null && !record.server().isBlank()
                ? Component.newline().append(kv("Server", record.server()))
                : Component.empty());
        placeholders.put("%details%", joinedLines(details));

        Messages messages = Messages.getInstance();
        String eventKey = "results.hover.events." + record.event();
        String templateKey = messages.hasTemplate(eventKey) ? eventKey : "results.hover.default";
        return messages.formatComponents(templateKey, placeholders);
    }

    private static Component textOrEmpty(String value) {
        return value == null ? Component.empty() : Component.text(value);
    }

    private static String signText(EventRecord record) {
        if (!(record instanceof BlockPlaceRecord place) || !"sign-edit".equals(record.event())) {
            return "";
        }
        BlockSnapshot before = place.originalBlock();
        BlockSnapshot after = place.newBlock();
        if (before == null || after == null) {
            return "";
        }
        List<String> editedLines;
        if (!before.signFront().equals(after.signFront())) {
            editedLines = after.signFront();
        } else if (!before.signBack().equals(after.signBack())) {
            editedLines = after.signBack();
        } else {
            return "\"\"";
        }
        return editedLines.stream()
                .filter(line -> line != null && !line.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private static Component joinedLines(List<Component> lines) {
        Component joined = Component.empty();
        for (Component line : lines) {
            joined = joined.append(Component.newline()).append(line);
        }
        return joined;
    }

    private static String itemTags(StoredItem item) {
        if (item == null || item.tags() == null || item.tags().isBlank()) {
            return "";
        }
        String tags = item.tags();
        return tags.length() > MAX_TAGS_HOVER
                ? tags.substring(0, MAX_TAGS_HOVER) + "…"
                : tags;
    }

    private static List<String> visibleLore(List<String> lore) {
        if (lore.size() <= MAX_LORE_LINES) {
            return lore;
        }
        List<String> visible = new ArrayList<>(lore.subList(0, MAX_LORE_LINES));
        visible.add("… (+" + (lore.size() - MAX_LORE_LINES) + " more)");
        return visible;
    }

    private String verb(EventRecord record) {
        return config.pastTense(record.event());
    }

    /** Hover lore lines are capped so a heavily-flavored RP item can't
     *  produce a screen-filling tooltip. */
    private static final int MAX_LORE_LINES = 12;

    /** Custom-data ({@code tags}) can run to kilobytes; the hover shows a
     *  preview and trusts {@code itags:} for the full search. */
    private static final int MAX_TAGS_HOVER = 128;

    /**
     * The item a record is "about", for hover detail: the deposited item
     * for a deposit, the withdrawn item for a withdraw, the dropped /
     * picked-up item otherwise. {@code null} for records carrying no item.
     */
    private static StoredItem subjectItem(EventRecord record) {
        return switch (record) {
            case ContainerDepositRecord deposit ->
                    deposit.afterItem() != null ? deposit.afterItem() : deposit.beforeItem();
            case ContainerWithdrawRecord withdraw ->
                    withdraw.beforeItem() != null ? withdraw.beforeItem() : withdraw.afterItem();
            case ItemDropRecord drop -> drop.item();
            case ItemPickupRecord pickup -> pickup.item();
            default -> null;
        };
    }

    /**
     * Append "Item Name" / "Enchants" / "Tags" / "Lore" hover lines for the
     * record's subject item when it carries custom metadata. Plain-text
     * projections only (colors were stripped at record time); a vanilla item
     * with no name, lore, enchants, or custom data adds nothing.
     */
    private static void appendItemDetail(List<Component> lines, EventRecord record) {
        StoredItem item = subjectItem(record);
        if (item == null) {
            return;
        }
        boolean hasName = item.name() != null && !item.name().isBlank();
        boolean hasLore = !item.lore().isEmpty();
        boolean hasEnchants = !item.enchants().isEmpty();
        String tags = itemTags(item);
        boolean hasTags = !tags.isEmpty();
        if (!hasName && !hasLore && !hasEnchants && !hasTags) {
            return;
        }
        if (hasName) {
            lines.add(kv("Item Name", item.name()));
        }
        if (hasEnchants) {
            lines.add(kv("Enchants", String.join(", ", item.enchants())));
        }
        if (hasTags) {
            // The full custom_data is searchable via itags:; the hover only
            // previews it so a kilobyte NBT blob can't blow up the tooltip.
            lines.add(kv("Tags", tags));
        }
        if (hasLore) {
            lines.add(Component.text("Lore:", NamedTextColor.GRAY));
            List<String> lore = visibleLore(item.lore());
            for (String line : lore) {
                lines.add(Component.text("  " + line, NamedTextColor.GRAY));
            }
        }
    }

    /**
     * Source-name display rule: keep player names verbatim (case
     * matters — "Itdontmatta" is the actual identity), but uppercase
     * everything else (entity types, env descriptions like "fire" /
     * "lava", console, command_block) so they read as identifiers
     * matching the uppercase target style ("FIRE broke SHORT_GRASS"
     * rather than "fire broke SHORT_GRASS").
     */
    private static String displaySourceName(EventRecord record) {
        Source source = record.source();
        String name = record.sourceName();
        if (source == null || name == null) {
            return name;
        }
        if (Source.PLAYER.equals(source.kind())) {
            return name;
        }
        return name.toUpperCase(Locale.ROOT);
    }

    /** Defensive uppercase for entity-type / target tokens that are
     *  conventionally uppercase but might come back lowercase from
     *  legacy data. */
    private static String upperOrEmpty(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    /** "ITEM IN CONTAINER" — appends the container type to the
     *  deposit / withdraw target so the line reads like
     *  "itdontmatta deposited ACTIVATOR_RAIL IN HOPPER". Falls
     *  back to just the item if container type is unknown. */
    private static String withContainer(String item, String containerType) {
        if (containerType == null || containerType.isBlank()) {
            return item;
        }
        return item + " IN " + containerType;
    }

    private static String targetOf(EventRecord record, boolean showIp) {
        return switch (record) {
            case BlockBreakRecord breakRec -> breakRec.target();
            case BlockPlaceRecord placeRec -> placeRec.target();
            case ContainerDepositRecord deposit -> withContainer(deposit.target(), deposit.containerType());
            case ContainerWithdrawRecord withdraw -> withContainer(withdraw.target(), withdraw.containerType());
            case ContainerInteractRecord interact -> interact.target();
            case BlockUseRecord use -> use.target();
            // One row per rollback/restore/undo operation (#22); the
            // mode rides in target. Renders "<operator> ran ROLLBACK".
            case RollbackOpRecord op -> upperOrEmpty(op.mode());
            case ChatRecord chat -> chatText(chat);
            // Generic integrator event: render "target: message" (like chat),
            // so a voice transcript reads "spoke voice to 3 players: <text>".
            case CustomRecord custom -> customText(custom);
            case CommandRecord command -> "/" + command.target();
            // Join's "target" in the data model is the player's own
            // name, which duplicates the source — show the IP instead
            // so the line reads "<player> joined <ip>". Quit has no
            // companion field so the target span gets suppressed
            // entirely (the line() builder skips empty targets).
            case JoinRecord join -> join.address() == null || join.address().isEmpty()
                    ? ""
                    : (showIp ? join.address() : IP_HIDDEN);
            case QuitRecord quit -> "";
            case ItemDropRecord drop -> drop.target();
            case ItemPickupRecord pickup -> pickup.target();
            case TeleportRecord tp -> tp.target() + " via " + tp.cause();
            // Entity events: bukkit EntityType names are conventionally
            // uppercase (ZOMBIE, SNIFFER, etc.). Display lowercase /
            // mixed-case incoming values uppercased for consistency.
            // Damage / damage-cause / projectile detail moves to the
            // hover so the inline form matches "<src> hit TARGET TIME"
            // — not "<src> hit TARGET for 1.0 TIME".
            case net.medievalrp.spyglass.api.event.EntityLifecycleRecord decoration -> upperOrEmpty(decoration.target());
            case EntityDeathRecord death -> upperOrEmpty(death.target());
            case EntityHitRecord hit -> upperOrEmpty(hit.target());
            case EntityMountRecord mount ->
                    (mount.dismount() ? "dismounted " : "mounted ") + upperOrEmpty(mount.target());
            case EntityNameRecord named -> {
                String arrow = named.oldName() == null
                        ? "→ " + named.newName()
                        : "'" + named.oldName() + "' → '" + named.newName() + "'";
                yield upperOrEmpty(named.target()) + " " + arrow;
            }
        };
    }

    private static int quantityOf(EventRecord record) {
        return switch (record) {
            case ContainerDepositRecord deposit -> deposit.amount();
            case ContainerWithdrawRecord withdraw -> withdraw.amount();
            case ItemDropRecord drop -> drop.amount();
            case ItemPickupRecord pickup -> pickup.amount();
            default -> 0;
        };
    }

    /**
     * Inline text for a chat record: the message is the content, so always
     * show it. When {@code target} is a distinct value (a channel/scope a hook
     * like WhisperNet parked there, not the message), prefix it — "#OOC: hi" —
     * so the log stays readable even with no {@link DisplayRenderer} registered.
     * Vanilla chat stores target == message (its aggregation key), so this just
     * yields the message.
     */
    /**
     * Inline text for a custom integrator event: the freeform message is the
     * content; when {@code target} is a distinct summary it prefixes it
     * ("voice to 3 players: hello there"). Mirrors {@link #chatText}.
     */
    private static String customText(CustomRecord custom) {
        String message = custom.message() == null ? "" : custom.message();
        String target = custom.target();
        if (target != null && !target.isEmpty() && !target.equals(message)) {
            return message.isEmpty() ? target : target + ": " + message;
        }
        return message;
    }

    private static String chatText(ChatRecord chat) {
        String message = chat.message() == null ? "" : chat.message();
        String channel = chat.target();
        if (channel != null && !channel.isEmpty() && !channel.equals(message)) {
            return channel + ": " + message;
        }
        return message;
    }

    /** Title-cases an extension-field key for its hover label ("channel" -> "Channel"). */
    private static String capitalizeKey(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        return Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }

    private static String originTag(Origin origin) {
        if (origin == null || origin.kind() == null) {
            return "";
        }
        return switch (origin.kind().toLowerCase(java.util.Locale.ROOT)) {
            case Origin.WORLDEDIT -> "[WE]";
            case Origin.FAWE -> "[FAWE]";
            case Origin.PLUGIN -> "[" + (origin.detail() == null ? "PL" : origin.detail()) + "]";
            default -> "";
        };
    }

    private static Component kv(String key, String value) {
        return Component.text()
                .append(Component.text(key + ": ", NamedTextColor.GRAY))
                .append(Component.text(value, NamedTextColor.WHITE))
                .asComponent();
    }

    private static String locationText(BlockLocation location) {
        if (location == null) {
            return "(unknown)";
        }
        return location.worldName() + " " + location.x() + "," + location.y() + "," + location.z();
    }

    private static String originText(Origin origin) {
        if (origin == null || origin.kind() == null) {
            return "unknown";
        }
        String detail = origin.detail();
        return detail == null || detail.isEmpty() ? origin.kind() : origin.kind() + ":" + detail;
    }

    /**
     * Resolve recipient UUIDs to names via Bukkit's offline-player cache. If a
     * name isn't known (player never joined on this server) we fall back to a
     * short UUID so the hover never says blank.
     */
    private static String recipientNames(java.util.List<java.util.UUID> recipients) {
        if (recipients.size() > 8) {
            return recipients.size() + " players";
        }
        StringBuilder out = new StringBuilder();
        for (java.util.UUID id : recipients) {
            if (out.length() > 0) {
                out.append(", ");
            }
            String name = org.bukkit.Bukkit.getOfflinePlayer(id).getName();
            out.append(name != null ? name : id.toString().substring(0, 8));
        }
        return out.toString();
    }

    private static String timeAgo(Instant occurred) {
        Messages messages = Messages.getInstance();
        long seconds = Math.max(0L, Instant.now().getEpochSecond() - occurred.getEpochSecond());
        if (seconds < 5) {
            return messages.raw("time.just-now");
        }
        if (seconds < 60) {
            return messages.raw("time.seconds").replace("%s", String.valueOf(seconds));
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return messages.raw("time.minutes").replace("%s", String.valueOf(minutes));
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return messages.raw("time.hours").replace("%s", String.valueOf(hours));
        }
        long days = hours / 24;
        if (days < 7) {
            return messages.raw("time.days").replace("%s", String.valueOf(days));
        }
        long weeks = days / 7;
        return messages.raw("time.weeks").replace("%s", String.valueOf(weeks));
    }

    private String fullTimestamp(Instant instant) {
        return instant.atZone(config.timezone())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
