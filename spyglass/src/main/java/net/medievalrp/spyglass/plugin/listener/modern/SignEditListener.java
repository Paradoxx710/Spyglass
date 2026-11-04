package net.medievalrp.spyglass.plugin.listener.modern;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.medievalrp.spyglass.api.capture.BlockSnapshots;
import net.medievalrp.spyglass.api.event.BlockPlaceRecord;
import net.medievalrp.spyglass.api.event.BlockSnapshot;
import net.medievalrp.spyglass.api.event.RecordContext;
import net.medievalrp.spyglass.api.util.BlockLocation;
import net.medievalrp.spyglass.plugin.listener.RecordingListener;
import net.medievalrp.spyglass.plugin.listener.RecordingSupport;
import net.medievalrp.spyglass.plugin.pipeline.Recorder;
import net.medievalrp.spyglass.plugin.util.BlockLocations;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Records player edits to a sign's text as {@code sign-edit}. Reuses the
 * {@link BlockPlaceRecord} shape so both storage backends already persist it
 * and rollback can restore the previous lines: the record carries a full
 * before/after {@link BlockSnapshot} pair, with the edited side replaced on
 * the after-state.
 *
 * <p>Unlike {@code useSign} (CraftBook machine signs), this fires for every
 * ordinary sign edit, including the first text written when a sign is placed.
 * It does not fire when another plugin cancels the edit.
 */
@ApiStatus.Internal
public final class SignEditListener implements RecordingListener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final int SIGN_LINES = 4;

    private final Recorder recorder;
    private final RecordingSupport support;
    private final Executor serializer;

    public SignEditListener(Recorder recorder, RecordingSupport support, Executor serializer) {
        this.recorder = recorder;
        this.support = support;
        this.serializer = serializer;
    }

    @Override
    public Set<String> events() {
        return Set.of("sign-edit");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Block block = event.getBlock();
        // The block still holds the old text while the event runs; capture it
        // before the server applies event.lines(). getState(false) is the live
        // tile-entity wrapper, which is safe: captureRaw copies the lines out
        // immediately.
        BlockState state = block.getState(false);
        if (!(state instanceof Sign)) {
            return;
        }
        BlockSnapshots.RawCapture before = BlockSnapshots.captureRaw(state);
        List<String> edited = plainLines(event.lines());

        // Only the side the player edited changes. The other side and all
        // non-sign payload (there is none on a sign) carry over untouched.
        List<String> front = event.getSide() == Side.FRONT ? edited : before.signFront();
        List<String> back = event.getSide() == Side.BACK ? edited : before.signBack();
        BlockSnapshots.RawCapture after = new BlockSnapshots.RawCapture(
                before.type(),
                before.blockData(),
                before.containerContents(),
                front,
                back,
                before.bannerPatterns(),
                before.jukeboxRecord(),
                before.potSherds());

        BlockLocation location = BlockLocations.fromBlock(block);
        RecordContext ctx = support.playerContext(event.getPlayer(), location);
        String target = before.type().name();

        // Same deferred strategy as BlockPlaceListener: getAsString() on the
        // immutable BlockData copy runs off the tick.
        serializer.execute(() -> {
            BlockSnapshot originalBlock = BlockSnapshots.finishCapture(before);
            BlockSnapshot newBlock = BlockSnapshots.finishCapture(after);
            recorder.record(BlockPlaceRecord.of(ctx, "sign-edit", target, originalBlock, newBlock));
        });
    }

    /**
     * Serializes the new side to plain text, always producing four entries.
     * The snapshots store plain text (rollback writes it back with
     * {@code Component.text}), so formatting is intentionally not kept.
     */
    private static List<String> plainLines(List<Component> components) {
        List<String> lines = new ArrayList<>(SIGN_LINES);
        for (int index = 0; index < SIGN_LINES; index++) {
            Component line = index < components.size() ? components.get(index) : null;
            lines.add(line == null
                    ? ""
                    : RecordingSupport.safeText(PLAIN.serialize(line)));
        }
        return List.copyOf(lines);
    }
}
