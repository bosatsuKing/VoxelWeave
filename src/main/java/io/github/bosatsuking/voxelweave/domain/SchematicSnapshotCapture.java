package io.github.bosatsuking.voxelweave.domain;

import java.util.Objects;

/** Immutable capture of the bounded operation target and its world-space schematic blocks. */
public record SchematicSnapshotCapture(OperationTarget target, SchematicSnapshot snapshot) {
    public SchematicSnapshotCapture {
        Objects.requireNonNull(target);
        Objects.requireNonNull(snapshot);
    }
}
