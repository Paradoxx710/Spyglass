package net.medievalrp.spyglass.plugin.command.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessagesTest {

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    @AfterEach
    void resetMessages() {
        Messages.setInstance(null);
    }

    @Test
    void defaultsProvideExpectedHouseStyle() {
        Messages messages = Messages.getInstance();
        Component error = messages.error("database unreachable");
        assertThat(plain(error)).isEqualTo("«Spyglass» (Error) database unreachable");

        Component info = messages.info("hello");
        assertThat(plain(info)).isEqualTo("«Spyglass» hello");
        assertThat(plain(messages.warn("careful"))).isEqualTo("«Spyglass» careful");
        assertThat(plain(messages.success("done"))).isEqualTo("«Spyglass» done");
        assertThat(plain(messages.bonus("queued"))).isEqualTo("«Spyglass» queued");
        assertThat(plain(messages.toolOk("ready"))).isEqualTo("«Spyglass» ready");
        assertThat(plain(messages.querying())).isEqualTo("«Spyglass» Querying records...");
    }

    @Test
    void customTemplateOverridesDefault() {
        Messages messages = new Messages(Map.of(
                "feedback.error", "<red>[CustomError] %message%</red>"
        ));

        Component error = messages.error("boom");
        assertThat(plain(error)).isEqualTo("[CustomError] boom");
    }

    @Test
    void feedbackCatalogTranslatesBodiesAndPreservesRuntimePlaceholders() {
        Messages messages = new Messages(Map.of(
                "catalog.teleport.world-not-found",
                "${prefix} <red>(Error)</red> Mundo desconocido: %world%"
        ));

        assertThat(plain(messages.feedback("catalog.teleport.world-not-found",
                null, "%world%", "survival")))
                .isEqualTo("«Spyglass» (Error) Mundo desconocido: survival");
    }

    @Test
    void catalogTemplatesLoadFromMessagesConfig(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("messages.conf"), """
                catalog {
                  teleport {
                    world-not-found = "${prefix} <red>(Error)</red> Mundo desconocido: %world%"
                  }
                }
                """);

        Messages loaded = Messages.load(tempDir, null);

        assertThat(plain(loaded.feedback("catalog.teleport.world-not-found",
                null, "%world%", "survival")))
                .isEqualTo("«Spyglass» (Error) Mundo desconocido: survival");
    }

    @Test
    void shippedMessagesConfigLoadsCatalogDefaults(@TempDir Path tempDir) {
        Messages loaded = Messages.load(tempDir, null);

        assertThat(plain(loaded.feedback("catalog.teleport.world-not-found",
                null, "%world%", "survival")))
                .isEqualTo("«Spyglass» (Error) World not found: survival");
        assertThat(loaded.rawList("help.commands"))
                .containsExactly("help", "search", "page", "rollback", "restore", "undo", "tool",
                        "events", "rbqueue", "inventory", "stats", "import", "migrate", "version", "reload");
        assertThat(plain(loaded.get("help.commands.search", null)))
                .isEqualTo("/spyglass search <Lookup Params>: Search Data Records based on the parameters provided.");
        assertThat(plain(loaded.format("rbqueue.empty", (org.bukkit.command.CommandSender) null)))
                .contains("No rollbacks running");
        assertThat(plain(loaded.format("snapshot.chat.expired",
                (org.bukkit.command.CommandSender) null)))
                .contains("snapshot has expired");
        assertThat(plain(loaded.format("updates.available",
                (org.bukkit.command.CommandSender) null, "%version%", "2.0")))
                .contains("Spyglass 2.0");
        assertThat(loaded.raw("commands.page-header")).contains("%results%");
        assertThat(loaded.raw("commands.tool-no-room"))
                .isEqualTo("Make room in your inventory for the Spyglass wand.");
    }

    @Test
    void catalogTemplateOwnsPrefixAndPlaceholderSpacing() {
        Messages messages = new Messages(Map.of(
                "prefix", "[SG]",
                "catalog.example", "${prefix} Value: %value%"
        ));

        assertThat(plain(messages.feedback("catalog.example", null, "%value%", "ready")))
                .isEqualTo("[SG] Value: ready");
    }

    @Test
    void catalogErrorsUseTheConfigurableSharedErrorTemplate() {
        Messages.setInstance(new Messages(Map.of(
                "prefix", "[Custom]",
                "feedback.error", "${prefix} <yellow>ERR</yellow> %message%",
                "catalog.custom", "${prefix} <red>(Error)</red> Failed for %target%"
        )));

        assertThat(plain(Feedback.catalogError((org.bukkit.command.CommandSender) null, "catalog.custom",
                "%target%", "chest")))
                .isEqualTo("[Custom] ERR Failed for chest");
    }

    @Test
    void reloadConfirmationUsesItsConfiguredPrefixAndMiniMessageColors() {
        Messages messages = new Messages(Map.of(
                "prefix", "<#123456>[Custom]</#123456>",
                "commands.config-reloaded",
                "${prefix} <#abcdef>Reloaded</#abcdef>"
        ));

        Component confirmation = messages.format("commands.config-reloaded", (org.bukkit.command.CommandSender) null);
        String serialized = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(confirmation);

        assertThat(plain(confirmation)).isEqualTo("[Custom] Reloaded");
        assertThat(serialized).contains("#123456").contains("#ABCDEF");
    }

    @Test
    void legacyMessagesConfigGainsPrefixesAndSpacingWithoutDuplicatingConfiguredPrefix(
            @TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("messages.conf"), """
                config-version = 1
                prefix = "[Legacy]"
                feedback {
                  info = "${prefix}%message%"
                  bonus = "%message%"
                }
                catalog {
                  teleport {
                    world-not-found = "World not found: %world%"
                  }
                }
                """);

        Messages loaded = Messages.load(tempDir, null);

        assertThat(plain(loaded.info("Ready"))).isEqualTo("[Legacy] Ready");
        assertThat(plain(loaded.bonus("Queued"))).isEqualTo("[Legacy] Queued");
        assertThat(plain(loaded.feedback("catalog.teleport.world-not-found",
                null, "%world%", "survival")))
                .isEqualTo("[Legacy] (Error) World not found: survival");
    }

    @Test
    void componentPlaceholdersStayLiteralAndSupportMultipleLines() {
        Messages messages = new Messages(Map.of(
                "custom", "<green>%player%</green>\n%detail%"
        ));

        Component formatted = messages.formatComponents("custom", Map.of(
                "%player%", Component.text("<red>Alice</red>"),
                "%detail%", Component.text("line one\nline two")
        ));

        assertThat(plain(formatted))
                .isEqualTo("<red>Alice</red>\nline one\nline two");
    }

    @Test
    void placeholdersAreEscapedAgainstTagInjection() {
        Messages messages = new Messages(Map.of(
                "feedback.info", "<gray>User said: </gray>%message%"
        ));

        Component component = messages.info("<red>I am red</red>");
        // Escaped tags appear literally in plain text, not parsed as formatting
        assertThat(plain(component)).isEqualTo("User said: <red>I am red</red>");
    }

    @Test
    void loadFromHoconFileReadsCustomConfig(@TempDir Path tempDir) throws Exception {
        String hocon = """
            prefix = "[SG]"
            feedback {
              error = "${prefix} ERROR: %message%"
            }
            commands {
              no-results = "Nada encontrado."
            }
            """;
        Files.writeString(tempDir.resolve("messages.conf"), hocon);

        Messages loaded = Messages.load(tempDir, null);
        assertThat(plain(loaded.error("test"))).isEqualTo("[SG] ERROR: test");
        assertThat(loaded.raw("commands.no-results")).isEqualTo("Nada encontrado.");
    }

    @Test
    void loadFromHoconReadsMultilineAndEventSpecificResultTemplates(@TempDir Path tempDir)
            throws Exception {
        String hocon = """
            results {
              line = "%player% - %action%"
              hover {
                default = \"\"\"Source: %source%
            Event: %event%\"\"\"
                events {
                  command = \"\"\"%source% ran:
            %command-line%\"\"\"
                }
              }
            }
            """;
        Files.writeString(tempDir.resolve("messages.conf"), hocon);

        Messages loaded = Messages.load(tempDir, null);

        assertThat(loaded.raw("results.line")).isEqualTo("%player% - %action%");
        assertThat(loaded.raw("results.hover.default")).isEqualTo("Source: %source%\nEvent: %event%");
        assertThat(loaded.hasTemplate("results.hover.events.command")).isTrue();
        assertThat(loaded.raw("results.hover.events.command"))
                .isEqualTo("%source% ran:\n%command-line%");
    }

    @Test
    void missingKeyFallsBackToDefault() {
        Messages messages = new Messages(Map.of("custom.key", "value"));
        // Unset key falls back to default
        assertThat(messages.raw("commands.no-results")).isEqualTo("No results.");
    }

    @Test
    void legacyColorCodesRenderAlongsideMiniMessage() {
        Messages messages = new Messages(Map.of(
                "feedback.info", "&6<bold>Gold</bold> &cRed"
        ));

        assertThat(plain(messages.info(""))).isEqualTo("Gold Red");
    }

    @Test
    void legacyCodesInPlaceholderValuesStayLiteral() {
        Messages messages = new Messages(Map.of(
                "feedback.info", "%message%"
        ));

        // Translation runs on the template, not on player/plugin input.
        assertThat(plain(messages.info("&cnot red"))).isEqualTo("&cnot red");
    }

    @Test
    void literalAmpersandEscapeIsPreserved() {
        assertThat(LegacyColors.translate("100&& off"))
                .isEqualTo("100& off");
    }

    @Test
    void listValuesAreFlattenedAndRenderedAsWandLore(@TempDir Path tempDir) throws Exception {
        String hocon = """
            tool {
              name = "&6Custom Wand"
              lore = ["line one", "&cline two"]
            }
            """;
        Files.writeString(tempDir.resolve("messages.conf"), hocon);

        Messages loaded = Messages.load(tempDir, null);
        assertThat(plain(loaded.toolName())).isEqualTo("Custom Wand");
        assertThat(loaded.toolLore()).extracting(MessagesTest::plain)
                .containsExactly("line one", "line two");
        assertThat(loaded.toolLore())
                .allSatisfy(line -> assertThat(line.decoration(
                        net.kyori.adventure.text.format.TextDecoration.ITALIC))
                        .isEqualTo(net.kyori.adventure.text.format.TextDecoration.State.FALSE));
    }

    @Test
    void defaultWandHasNameAndNoLore() {
        Messages messages = new Messages(Map.of());

        assertThat(plain(messages.toolName())).isEqualTo("Spyglass Wand");
        assertThat(messages.toolName().decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC))
                .isEqualTo(net.kyori.adventure.text.format.TextDecoration.State.FALSE);
        assertThat(messages.toolLore()).isEmpty();
    }
}
