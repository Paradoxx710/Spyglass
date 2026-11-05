package net.medievalrp.spyglass.plugin.command.render;

import static org.assertj.core.api.Assertions.assertThat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.medievalrp.spyglass.api.param.ParamParseException;
import net.medievalrp.spyglass.plugin.command.CatalogMessageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Guards the configurable command feedback templates. */
class FeedbackTest {

    @AfterEach
    void resetMessages() {
        Messages.setInstance(null);
    }

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    @Test
    void errorPrefixesAndLabelsTheMessage() {
        assertThat(plain(Feedback.error("boom"))).isEqualTo("«Spyglass» (Error) boom");
    }

    @Test
    void infoWarnSuccessCarryPrefixAndMessage() {
        assertThat(plain(Feedback.info("hi"))).contains("«Spyglass»").contains("hi");
        assertThat(plain(Feedback.warn("careful"))).contains("«Spyglass»").contains("careful");
        assertThat(plain(Feedback.success("done"))).contains("«Spyglass»").contains("done");
    }

    @Test
    void everyCategoryFramesWithASpaceAfterThePrefix() {
        assertThat(plain(Feedback.info("m"))).isEqualTo("«Spyglass» m");
        assertThat(plain(Feedback.warn("m"))).isEqualTo("«Spyglass» m");
        assertThat(plain(Feedback.error("m"))).isEqualTo("«Spyglass» (Error) m");
        assertThat(plain(Feedback.success("m"))).isEqualTo("«Spyglass» m");
        assertThat(plain(Feedback.bonus("m"))).isEqualTo("«Spyglass» m");
        assertThat(plain(Feedback.toolOk("m"))).isEqualTo("«Spyglass» m");
    }

    @Test
    void queryingMessageUsesTheConfiguredPrefixAndSpacing() {
        assertThat(plain(Feedback.querying())).isEqualTo("«Spyglass» Querying records...");
    }

    @Test
    void parseErrorsRenderTheirCatalogMessageWithoutDuplicatingTheErrorFrame() {
        Messages.setInstance(new Messages(java.util.Map.of(
                "prefix", "[Custom]",
                "feedback.error", "${prefix} <yellow>ERR</yellow> %message%",
                "catalog.search.no-permission",
                "${prefix} <red>(Error)</red> Custom denial for %action%"
        )));
        ParamParseException exception = new ParamParseException("Fallback",
                new CatalogMessageException("catalog.search.no-permission", "%action%", "break"));

        assertThat(plain(Feedback.parseError(null, exception)))
                .isEqualTo("[Custom] ERR Custom denial for break");
    }
}
