package io.github.bosatsuking.voxelweave.history;

import io.github.bosatsuking.voxelweave.domain.BlockChange;
import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.ChangeSet;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Pure application of an already-evaluated ChangeSet to an immutable schematic snapshot. */
public final class ChangeSetApplier {
    private ChangeSetApplier() { }

    public static SchematicSnapshot apply(SchematicSnapshot snapshot, ChangeSet changeSet) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(changeSet);
        if (changeSet.isEmpty()) return snapshot;

        Map<GridPoint, BlockStateRef> next = new HashMap<>(snapshot.blocks());
        for (BlockChange change : changeSet.changes()) {
            BlockStateRef actual = next.get(change.position());
            if (!change.before().equals(actual)) {
                throw new IllegalStateException("ChangeSet does not match snapshot at " + change.position()
                        + ": expected " + change.before().value() + " but was "
                        + (actual == null ? "<missing>" : actual.value()));
            }
            next.put(change.position(), change.after());
        }
        return new SchematicSnapshot(next);
    }
}
