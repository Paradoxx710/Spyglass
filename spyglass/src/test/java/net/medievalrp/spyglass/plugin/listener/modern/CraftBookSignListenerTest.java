package net.medievalrp.spyglass.plugin.listener.modern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.medievalrp.spyglass.api.event.BlockUseRecord;
import net.medievalrp.spyglass.api.event.EventRecord;
import net.medievalrp.spyglass.api.util.Duration;
import net.medievalrp.spyglass.plugin.listener.RecordingSupport;
import net.medievalrp.spyglass.plugin.pipeline.Recorder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

class CraftBookSignListenerTest {

    private final CapturingRecorder recorder = new CapturingRecorder();
    private final RecordingSupport support = new RecordingSupport(Duration.parse("4w"), "test");
    // Strong ref so Location's weak World reference can't be collected mid-test.
    private final World world = mock(World.class);
    private final CraftBookSignListener listener = new CraftBookSignListener(recorder, support);

    @Test
    void handlesCancelledInteractionsBecauseCraftBookCancelsThem() throws Exception {
        // CraftBook toggles a door/gate/bridge and then cancels the sign's
        // PlayerInteractEvent. With ignoreCancelled = true the successful use
        // was never recorded.
        Method handler = CraftBookSignListener.class.getMethod("onSignInteract", PlayerInteractEvent.class);
        EventHandler annotation = handler.getAnnotation(EventHandler.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.ignoreCancelled()).isFalse();
    }

    @Test
    void recordsBracketedCraftBookSignInteractionAsUseSign() {
        playerClicksSign("[Lift Up]");

        assertThat(recorder.records).hasSize(1);
        BlockUseRecord record = (BlockUseRecord) recorder.records.get(0);
        assertThat(record.event()).isEqualTo("useSign");
        assertThat(record.target()).isEqualTo("OAK_WALL_SIGN [Lift Up]");
        assertThat(record.location().x()).isEqualTo(10);
        assertThat(record.location().y()).isEqualTo(64);
        assertThat(record.location().z()).isEqualTo(20);
    }

    @Test
    void ignoresOrdinarySignReads() {
        playerClicksSign("Welcome");

        assertThat(recorder.records).isEmpty();
    }

    private void playerClicksSign(String secondLine) {
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getName()).thenReturn("world");

        SignSide side = mock(SignSide.class);
        when(side.lines()).thenReturn(List.of(
                Component.text("label"),
                Component.text(secondLine),
                Component.empty(),
                Component.empty()));
        Sign sign = mock(Sign.class);
        when(sign.getSide(Side.FRONT)).thenReturn(side);

        Block block = mock(Block.class);
        when(block.getState(false)).thenReturn(sign);
        when(block.getType()).thenReturn(Material.OAK_WALL_SIGN);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Alice");

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getClickedBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);

        listener.onSignInteract(event);
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
