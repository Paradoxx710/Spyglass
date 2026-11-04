package net.medievalrp.spyglass.plugin.command.param;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.medievalrp.spyglass.api.param.ParamParseException;
import net.medievalrp.spyglass.api.param.QueryParamHandler;
import net.medievalrp.spyglass.api.query.QueryPredicate;
import org.bukkit.command.CommandSender;

public final class EventParam implements QueryParamHandler {

    // A rollback never persists per-block receipts; searches synthesize
    // rolled-<event> entries from the rollback-op record (#22, RolledSynthesis).
    // Those carry the rolled-* event names, so a bare a:break would filter out
    // the rollback that BROKE (removed) a block - the operator sees the original
    // but not its reversal (#330). Expanding a:break to also match rolled-break
    // (and place/deposit/withdraw likewise) surfaces both. The In(event, [...])
    // this produces flows through the synthesis gate and post-filter unchanged.
    private static final Map<String, String> ROLLED_COUNTERPART = Map.of(
            "break", "rolled-break",
            "place", "rolled-place",
            "deposit", "rolled-deposit",
            "withdraw", "rolled-withdraw");

    private final Set<String> enabledEvents;
    private final java.util.function.Function<String, String> permissionResolver;

    public EventParam(Set<String> enabledEvents) {
        this(enabledEvents, name -> "spyglass.search");
    }

    public EventParam(Set<String> enabledEvents, java.util.function.Function<String, String> permissionResolver) {
        // Live reference, NOT a copy: SpyglassApi#registerEvent adds custom
        // event names to this same set at runtime so a:<name> parses for them.
        this.enabledEvents = enabledEvents;
        this.permissionResolver = permissionResolver != null ? permissionResolver : name -> "spyglass.search";
    }

    @Override
    public List<String> aliases() {
        return List.of("a", "action", "event");
    }

    @Override
    public QueryPredicate parse(String alias, String value, ParamContext context) throws ParamParseException {
        if (value == null || value.isBlank()) {
            throw new ParamParseException("Event parameter requires a value.");
        }
        // `!`-prefixed names are excludes, same syntax as c: (#30).
        List<String> includes = new java.util.ArrayList<>();
        List<String> excludes = new java.util.ArrayList<>();
        for (String raw : value.split(",")) {
            String token = raw.trim();
            if (token.isEmpty() || token.equals("!")) {
                continue;
            }
            boolean negated = token.startsWith("!");
            String requested = negated ? token.substring(1).trim() : token;
            if (requested.isEmpty()) {
                continue;
            }
            // Enabled names are config keys and keep their canonical spelling
            // ("useSign"), so resolve the operator's input against them rather
            // than lower-casing the query and hoping every key is lowercase.
            String name = resolveEnabled(requested);
            if (name == null) {
                throw new ParamParseException("Unknown or disabled event: "
                        + requested.toLowerCase(java.util.Locale.ROOT));
            }
            CommandSender sender = context != null ? context.sender() : null;
            String requiredPermission = permissionResolver.apply(name);
            if (sender != null && requiredPermission != null && !sender.hasPermission(requiredPermission)) {
                String template = net.medievalrp.spyglass.plugin.command.render.Messages.getInstance()
                        .raw("commands.action-no-permission");
                throw new ParamParseException(template.replace("%action%", name));
            }
            addWithRolledCounterpart(negated ? excludes : includes, name);
        }
        if (includes.isEmpty() && excludes.isEmpty()) {
            throw new ParamParseException("Event parameter requires at least one name.");
        }
        List<QueryPredicate> clauses = new java.util.ArrayList<>(2);
        if (!includes.isEmpty()) {
            clauses.add(membership(includes));
        }
        if (!excludes.isEmpty()) {
            clauses.add(new QueryPredicate.Not(membership(excludes)));
        }
        return clauses.size() == 1 ? clauses.getFirst() : new QueryPredicate.And(clauses);
    }

    /**
     * Add {@code name} to the bucket, plus its rollback counterpart when that
     * rolled event is enabled (#330). Dedupes so {@code a:break,rolled-break}
     * or a base/rolled pair typed together stays a clean membership. Gating on
     * {@link #enabledEvents} means an operator who disabled the rolled audit
     * keeps the old bare-name behavior.
     */
    private void addWithRolledCounterpart(List<String> bucket, String name) {
        if (!bucket.contains(name)) {
            bucket.add(name);
        }
        String rolled = ROLLED_COUNTERPART.get(name.toLowerCase(java.util.Locale.ROOT));
        if (rolled != null) {
            String resolved = resolveEnabled(rolled);
            if (resolved != null && !bucket.contains(resolved)) {
                bucket.add(resolved);
            }
        }
    }

    /**
     * Find the enabled event whose name matches {@code requested}, preferring
     * an exact match and otherwise comparing case-insensitively. Returns the
     * canonical configured spelling so permissions and stored predicates use
     * the same key as config.conf.
     */
    private String resolveEnabled(String requested) {
        if (enabledEvents.contains(requested)) {
            return requested;
        }
        for (String enabled : enabledEvents) {
            if (enabled.equalsIgnoreCase(requested)) {
                return enabled;
            }
        }
        return null;
    }

    private static QueryPredicate membership(List<String> names) {
        if (names.size() == 1) {
            return new QueryPredicate.Eq("event", names.getFirst());
        }
        return new QueryPredicate.In("event", names);
    }

    @Override
    public List<String> suggestions(CommandSender sender, String input) {
        String lowerInput = input.toLowerCase(java.util.Locale.ROOT);
        return enabledEvents.stream()
                .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(lowerInput))
                .filter(name -> {
                    if (sender == null) {
                        return true;
                    }
                    String requiredPermission = permissionResolver.apply(name);
                    return requiredPermission == null || sender.hasPermission(requiredPermission);
                })
                .sorted()
                .toList();
    }
}
