package net.medievalrp.spyglass.plugin.command;

import java.util.Objects;

/** Carries a catalog message through a {@code ParamParseException} to its command boundary. */
public final class CatalogMessageException extends RuntimeException {

    private final String key;
    private final Object[] placeholders;

    public CatalogMessageException(String key, Object... placeholders) {
        super(key, null, false, false);
        this.key = Objects.requireNonNull(key, "key");
        this.placeholders = placeholders == null ? new Object[0] : placeholders.clone();
    }

    public String key() {
        return key;
    }

    public Object[] placeholders() {
        return placeholders.clone();
    }
}
