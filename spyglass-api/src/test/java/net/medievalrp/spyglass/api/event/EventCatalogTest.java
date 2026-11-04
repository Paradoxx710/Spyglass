package net.medievalrp.spyglass.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EventCatalogTest {

    @Test
    void registeredCustomEventResolvesToCustomRecord() {
        // A custom name an integrator registers must resolve so the read path
        // (recordClassOf) decodes it instead of silently dropping the row.
        assertThat(EventCatalog.recordClassOf("unit-test-voice")).isNull();
        EventCatalog.register("unit-test-voice", "spoke");
        assertThat(EventCatalog.recordClassOf("unit-test-voice")).isEqualTo(CustomRecord.class);
        assertThat(EventCatalog.isRegistered("unit-test-voice")).isTrue();
        assertThat(EventCatalog.pastTenseOf("unit-test-voice")).isEqualTo("spoke");
        assertThat(EventCatalog.eventNames()).contains("unit-test-voice");
    }

    @Test
    void registrationIsCaseInsensitiveAndIdempotent() {
        EventCatalog.register("Unit-Test-Caps", "yelled");
        assertThat(EventCatalog.recordClassOf("UNIT-TEST-CAPS")).isEqualTo(CustomRecord.class);
        assertThat(EventCatalog.pastTenseOf("unit-test-caps")).isEqualTo("yelled");
        // Re-register is a no-op overwrite, not a duplicate.
        EventCatalog.register("unit-test-caps", "shouted");
        assertThat(EventCatalog.pastTenseOf("unit-test-caps")).isEqualTo("shouted");
    }

    @Test
    void builtinsTakePrecedenceAndCannotBeRedefined() {
        EventCatalog.register("say", "whispered");
        // 'say' is a built-in -> still ChatRecord, registration ignored.
        assertThat(EventCatalog.recordClassOf("say")).isEqualTo(ChatRecord.class);
        assertThat(EventCatalog.pastTenseOf("say")).isNull();
    }

    @Test
    void killAndMobKillAreStoredAsEntityHitRecord() {
        assertThat(EventCatalog.recordClassOf("kill")).isEqualTo(EntityHitRecord.class);
        assertThat(EventCatalog.recordClassOf("mob-kill")).isEqualTo(EntityHitRecord.class);
        assertThat(EventCatalog.eventNames()).contains("kill", "mob-kill");
    }

    @Test
    void unknownEventResolvesToNull() {
        assertThat(EventCatalog.recordClassOf("not-an-event")).isNull();
        assertThat(EventCatalog.recordClassOf(null)).isNull();
        assertThat(EventCatalog.isRegistered("not-an-event")).isFalse();
    }

    @Test
    void mixedCaseBuiltinResolvesCaseInsensitively() {
        // useSign keeps its historical spelling in config and stored records,
        // but a query typed in any case must still decode as BlockUseRecord.
        assertThat(EventCatalog.recordClassOf("useSign")).isEqualTo(BlockUseRecord.class);
        assertThat(EventCatalog.recordClassOf("usesign")).isEqualTo(BlockUseRecord.class);
        assertThat(EventCatalog.recordClassOf("USESIGN")).isEqualTo(BlockUseRecord.class);
        assertThat(EventCatalog.isRegistered("usesign")).isTrue();
        assertThat(EventCatalog.eventNames()).contains("useSign");
    }

    @Test
    void signEditReusesTheBlockPlaceShapeAndIsRollbackable() {
        // sign-edit carries before/after BlockSnapshots, so it reuses the
        // place record for storage and rollback without a schema change.
        assertThat(EventCatalog.recordClassOf("sign-edit")).isEqualTo(BlockPlaceRecord.class);
        assertThat(EventCatalog.isRegistered("SIGN-EDIT")).isTrue();
        assertThat(EventCatalog.isRollbackable("sign-edit")).isTrue();
        assertThat(EventCatalog.eventNames()).contains("sign-edit");
    }

    @org.junit.jupiter.api.Test
    void slotAccurateTransfersMapToContainerShapesButNeverRollBack() {
        org.assertj.core.api.Assertions.assertThat(EventCatalog.recordClassOf("transfer-withdraw"))
                .isEqualTo(ContainerWithdrawRecord.class);
        org.assertj.core.api.Assertions.assertThat(EventCatalog.recordClassOf("transfer-deposit"))
                .isEqualTo(ContainerDepositRecord.class);
        // The record shape is rollbackable, the event names must not be:
        // an area rollback never replays hopper flow (#226).
        org.assertj.core.api.Assertions.assertThat(EventCatalog.isRollbackable("withdraw")).isTrue();
        org.assertj.core.api.Assertions.assertThat(EventCatalog.isRollbackable("transfer-withdraw")).isFalse();
        org.assertj.core.api.Assertions.assertThat(EventCatalog.isRollbackable("transfer-deposit")).isFalse();
        // The retired lean pair still decodes historical rows.
        org.assertj.core.api.Assertions.assertThat(EventCatalog.recordClassOf("transfer-out"))
                .isEqualTo(ItemDropRecord.class);
        org.assertj.core.api.Assertions.assertThat(EventCatalog.recordClassOf("transfer-in"))
                .isEqualTo(ItemPickupRecord.class);
    }
}
