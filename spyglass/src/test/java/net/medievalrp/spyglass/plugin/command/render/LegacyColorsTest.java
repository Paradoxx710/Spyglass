package net.medievalrp.spyglass.plugin.command.render;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LegacyColorsTest {

    @Test
    void translatesColorsFormattingAndReset() {
        assertThat(LegacyColors.translate("&6Gold &lBold &rReset"))
                .isEqualTo("<reset><gold>Gold <bold>Bold <reset>Reset");
    }

    @Test
    void colorAfterFormattingResetsThatFormattingLikeTheLegacyClient() {
        assertThat(LegacyColors.translate("&l&6Gold"))
                .isEqualTo("<bold><reset><gold>Gold");
    }

    @Test
    void translatesHashHex() {
        assertThat(LegacyColors.translate("&#ff5555Hi"))
                .isEqualTo("<reset><#ff5555>Hi");
    }

    @Test
    void translatesLegacyXHex() {
        assertThat(LegacyColors.translate("&x&f&f&5&5&5&5Hi"))
                .isEqualTo("<reset><#ff5555>Hi");
    }

    @Test
    void leavesUnknownAndIncompleteCodesAlone() {
        assertThat(LegacyColors.translate("R&Z")).isEqualTo("R&Z");
        assertThat(LegacyColors.translate("&# not hex")).isEqualTo("&# not hex");
        assertThat(LegacyColors.translate("&")).isEqualTo("&");
        assertThat(LegacyColors.translate(null)).isNull();
    }

    @Test
    void doubleAmpersandIsALiteralAmpersand() {
        assertThat(LegacyColors.translate("&&6")).isEqualTo("&6");
    }
}
