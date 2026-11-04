package net.medievalrp.spyglass.plugin.listener.modern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.medievalrp.spyglass.api.event.BlockPlaceRecord;
import net.medievalrp.spyglass.api.event.EventRecord;
import net.medievalrp.spyglass.api.util.Duration;
import net.medievalrp.spyglass.plugin.listener.RecordingSupport;
import net.medievalrp.spyglass.plugin.pipeline.Recorder;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.junit.jupiter.api.Test;

class SignEditListenerTest {

    private final CapturingRecorder recorder = new CapturingRecorder();
    private final RecordingSupport support = new RecordingSupport(Duration.parse("4w"), "test");
    // Strong ref so Location's weak World reference can't be collected mid-test.
    private final World world = mock(World.class);
    private final SignEditListener listener = new SignEditListener(recorder, support, Runnable::run);

    @Test
    void ignoresCancelledEdits() throws Exception {
        Method handler = SignEditListener.class.getMethod("onSignChange", SignChangeEvent.class);
        EventHandler annotation = handler.getAnnotation(EventHandler.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.ignoreCancelled()).isTrue();
    }

    @Test
    void recordsFrontEditWithBeforeAndAfterSnapshots() {
        editSign(Side.FRONT,
                List.of("old 1", "old 2", "old 3", "old 4"),
                List.of("back 1", "back 2", "back 3", "back 4"),
                List.of(Component.text("new 1"), Component.text("new 2"),
                        Component.text("new 3"), Component.text("new 4")));

        assertThat(recorder.records).hasSize(1);
        BlockPlaceRecord record = (BlockPlaceRecord) recorder.records.get(0);
        assertThat(record.event()).isEqualTo("sign-edit");
        assertThat(record.target()).isEqualTo("OAK_SIGN");
        assertThat(record.location().x()).isEqualTo(10);
        assertThat(record.location().y()).isEqualTo(64);
        assertThat(record.location().z()).isEqualTo(20);

        assertThat(record.originalBlock().signFront())
                .containsExactly("old 1", "old 2", "old 3", "old 4");
        assertThat(record.newBlock().signFront())
                .containsExactly("new 1", "new 2", "new 3", "new 4");
        // The untouched side survives both snapshots.
        assertThat(record.originalBlock().signBack())
                .containsExactly("back 1", "back 2", "back 3", "back 4");
        assertThat(record.newBlock().signBack())
                .containsExactly("back 1", "back 2", "back 3", "back 4");
    }

    @Test
    void recordsBackEditWithoutTouchingTheFront() {
        editSign(Side.BACK,
                List.of("front 1", "front 2", "front 3", "front 4"),
                List.of("old 1", "old 2", "old 3", "old 4"),
                List.of(Component.text("new 1"), Component.text("new 2"),
                        Component.text("new 3"), Component.text("new 4")));

        BlockPlaceRecord record = (BlockPlaceRecord) recorder.records.get(0);
        assertThat(record.originalBlock().signFront())
                .containsExactly("front 1", "front 2", "front 3", "front 4");
        assertThat(record.newBlock().signFront())
                .containsExactly("front 1", "front 2", "front 3", "front 4");
        assertThat(record.originalBlock().signBack())
                .containsExactly("old 1", "old 2", "old 3", "old 4");
        assertThat(record.newBlock().signBack())
                .containsExactly("new 1", "new 2", "new 3", "new 4");
    }

    @Test
    void padsMissingAndNullLinesToFour() {
        editSign(Side.FRONT,
                List.of("old 1", "old 2", "old 3", "old 4"),
                List.of("back 1", "back 2", "back 3", "back 4"),
                Arrays.asList(Component.text("new 1"), null));

        BlockPlaceRecord record = (BlockPlaceRecord) recorder.records.get(0);
        assertThat(record.newBlock().signFront())
                .containsExactly("new 1", "", "", "");
    }

    private void editSign(Side side, List<String> oldFront, List<String> oldBack,
                          List<Component> newLines) {
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getName()).thenReturn("world");

        BlockData data = mock(BlockData.class);
        when(data.getAsString()).thenReturn("minecraft:oak_sign[rotation=0,waterlogged=false]");

        SignSide frontSide = mock(SignSide.class);
        when(frontSide.lines()).thenReturn(components(oldFront));
        SignSide backSide = mock(SignSide.class);
        when(backSide.lines()).thenReturn(components(oldBack));

        Sign sign = mock(Sign.class);
        when(sign.getType()).thenReturn(Material.OAK_SIGN);
        when(sign.getBlockData()).thenReturn(data);
        when(sign.getSide(Side.FRONT)).thenReturn(frontSide);
        when(sign.getSide(Side.BACK)).thenReturn(backSide);

        Block block = mock(Block.class);
        when(block.getState(false)).thenReturn(sign);
        when(block.getWorld()).thenReturn(world);
        when(block.getX()).thenReturn(10);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(20);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Alice");

        SignChangeEvent event = mock(SignChangeEvent.class);
        when(event.getBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);
        when(event.getSide()).thenReturn(side);
        when(event.lines()).thenReturn(newLines);

        listener.onSignChange(event);
    }

    private static List<Component> components(List<String> lines) {
        return lines.stream().<Component>map(Component::text).toList();
    }

    private static final class CapturingRecorder implements Recorder {
        private final List<EventRecord> records = new java.util.ArrayList<>();

        @Override
        public void record(EventRecord record) {
            records.add(record);
        }

        @Override
        public void recordAll(List<EventRecord> records) {
            this.records.addAll(records);
        }

        @Override
        public boolean flush(Duration timeout) {
            return true;
        }

        @Override
        public net.medievalrp.spyglass.plugin.pipeline.AsyncRecorder.ShutdownReport shutdown(Duration timeout) {
            return null;
        }
    }
}
