package net.medievalrp.spyglass.plugin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

/**
 * End-to-end boot-path proof for the removed-knob migrations (#307, #312):
 * a real pre-removal config.conf on disk, loaded through the real
 * {@link SpyglassConfig#load}, must enable the plugin with every operator
 * setting intact - not reset, not disabled - while the dead keys are
 * dropped, the version is stamped, and the original file is backed up.
 */
class SpyglassConfigLoadMigrationTest {

    private static JavaPlugin pluginIn(Path dataFolder) {
        return pluginIn(dataFolder, Logger.getLogger("test"));
    }

    private static JavaPlugin pluginIn(Path dataFolder, Logger logger) {
        // Mockito 5's inline mock maker stubs JavaPlugin's final accessors.
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(plugin.getLogger()).thenReturn(logger);
        // Fresh stream per call: load() reads the bundled template twice
        // (migration, then event auto-merge).
        when(plugin.getResource("config.conf")).thenAnswer(inv ->
                SpyglassConfigLoadMigrationTest.class.getResourceAsStream("/config.conf"));
        return plugin;
    }

    @Test
    void preVersioningConfigWithBothRemovedKnobsBootsWithItsSettings(@TempDir Path dataFolder)
            throws Exception {
        // A v1-era file (no config-version key): both later-removed knobs
        // set to their non-default values, plus settings the operator chose.
        Path file = dataFolder.resolve("config.conf");
        Files.writeString(file, "database { backend = \"sqlite\" }\n"
                + "storage {\n"
                + "  durability = \"wal-batched\"\n"
                + "  rolled-audit = \"receipts\"\n"
                + "  retention = \"8w\"\n"
                + "  queue-max = 250000\n"
                + "}\n"
                + "limits { max-radius = 250 }\n"
                + "server { name = \"lobby\" }\n");

        SpyglassConfig config = SpyglassConfig.load(pluginIn(dataFolder));

        // The operator's settings survived the migration.
        assertThat(config.database().backend()).isEqualTo(SpyglassConfig.Backend.SQLITE);
        assertThat(config.storage().retention().seconds())
                .isEqualTo(java.time.Duration.ofDays(7 * 8).toSeconds());
        assertThat(config.storage().queueMax()).isEqualTo(250_000);
        assertThat(config.limits().maxRadius()).isEqualTo(250);
        assertThat(config.server().name()).isEqualTo("lobby");

        // The rewritten file is stamped current with the dead keys dropped.
        ConfigurationNode written = HoconConfigurationLoader.builder().path(file).build().load();
        assertThat(written.node("config-version").getInt())
                .isEqualTo(SpyglassConfig.CONFIG_VERSION);
        assertThat(written.node("storage", "durability").virtual()).isTrue();
        assertThat(written.node("storage", "rolled-audit").virtual()).isTrue();

        // The pre-migration original is recoverable next to it.
        try (Stream<Path> siblings = Files.list(dataFolder)) {
            assertThat(siblings.filter(p -> p.getFileName().toString()
                    .startsWith("config.conf.v1.bak"))).hasSize(1);
        }
    }

    @Test
    void alreadyCurrentConfigLoadsWithoutRewriteOrBackup(@TempDir Path dataFolder)
            throws Exception {
        Path file = dataFolder.resolve("config.conf");
        Files.writeString(file, "config-version = " + SpyglassConfig.CONFIG_VERSION + "\n"
                + "database { backend = \"sqlite\" }\n"
                + "storage { retention = \"12w\" }\n");

        SpyglassConfig config = SpyglassConfig.load(pluginIn(dataFolder));

        assertThat(config.storage().retention().seconds())
                .isEqualTo(java.time.Duration.ofDays(7 * 12).toSeconds());
        try (Stream<Path> siblings = Files.list(dataFolder)) {
            assertThat(siblings.filter(p -> p.getFileName().toString().endsWith(".bak")))
                    .isEmpty();
        }
    }

    @Test
    void invalidTimezoneFallsBackToDefaultAndWarns(@TempDir Path dataFolder) throws Exception {
        Path file = dataFolder.resolve("config.conf");
        Files.writeString(file, "config-version = " + SpyglassConfig.CONFIG_VERSION + "\n"
                + "timezone = \"Not/A_Real_Zone\"\n");
        Logger logger = Logger.getLogger("spyglass-config-timezone-test-" + System.nanoTime());
        java.util.List<String> warnings = new java.util.ArrayList<>();
        java.util.logging.Handler handler = new java.util.logging.Handler() {
            @Override
            public void publish(java.util.logging.LogRecord record) {
                warnings.add(record.getMessage());
            }

            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.addHandler(handler);

        SpyglassConfig config = SpyglassConfig.load(pluginIn(dataFolder, logger));

        assertThat(config.timezone()).isEqualTo(java.time.ZoneId.of(SpyglassConfig.DEFAULT_TIMEZONE));
        assertThat(warnings).anySatisfy(message -> {
            assertThat(message).contains("invalid timezone 'Not/A_Real_Zone'");
            assertThat(message).contains(SpyglassConfig.DEFAULT_TIMEZONE);
        });
        logger.removeHandler(handler);
    }

    @Test
    void v3ConfigGainsTheAliasSectionAndKeepsSAliasOptOut(@TempDir Path dataFolder)
            throws Exception {
        Path file = dataFolder.resolve("config.conf");
        Files.writeString(file, "config-version = 3\n"
                + "database { backend = \"sqlite\" }\n"
                + "commands { s-alias = false }\n");

        SpyglassConfig config = SpyglassConfig.load(pluginIn(dataFolder));

        // The old opt-out still removes /s even though the migrated template
        // lists it in commands.aliases.root.
        assertThat(config.commands().sAlias()).isFalse();
        assertThat(config.commands().extraAliases("root"))
                .contains("sg")
                .doesNotContain("s");

        ConfigurationNode written = HoconConfigurationLoader.builder().path(file).build().load();
        assertThat(written.node("config-version").getInt())
                .isEqualTo(SpyglassConfig.CONFIG_VERSION);
        assertThat(written.node("commands", "aliases", "root").getList(String.class))
                .contains("sg", "s");
    }
}
