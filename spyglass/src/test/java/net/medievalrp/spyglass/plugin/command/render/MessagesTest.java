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
    }

    @Test
    void defaultWandHasNameAndNoLore() {
        Messages messages = new Messages(Map.of());

        assertThat(plain(messages.toolName())).isEqualTo("Spyglass Wand");
        assertThat(messages.toolLore()).isEmpty();
    }
}
