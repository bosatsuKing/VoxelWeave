package io.github.bosatsuking.voxelweave.integration;

import io.github.bosatsuking.voxelweave.domain.SchematicSnapshotCapture;

import java.util.Optional;

@FunctionalInterface
public interface ReadOnlySchematicIntegration {
    ReadOnlyIntegrationDiagnostic diagnose();

    /** Captures the current bounded schematic target when the integration is ready. */
    default Optional<SchematicSnapshotCapture> captureSnapshot() {
        return Optional.empty();
    }
}
