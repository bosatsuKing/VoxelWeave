package io.github.bosatsuking.voxelweave.integration.litematica;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.util.PositionUtils;
import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshotCapture;
import io.github.bosatsuking.voxelweave.integration.IntegrationState;
import io.github.bosatsuking.voxelweave.integration.ReadOnlyIntegrationDiagnostic;
import io.github.bosatsuking.voxelweave.integration.ReadOnlySchematicIntegration;

import java.util.Optional;

public final class LitematicaReadOnlyIntegration implements ReadOnlySchematicIntegration {
    private final String litematicaVersion;
    private final String malilibVersion;

    public LitematicaReadOnlyIntegration(String litematicaVersion, String malilibVersion) {
        this.litematicaVersion = litematicaVersion;
        this.malilibVersion = malilibVersion;
    }

    @Override
    public ReadOnlyIntegrationDiagnostic diagnose() {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager()
                .getSelectedSchematicPlacement();
        AreaSelection selection = DataManager.getSelectionManager().getCurrentSelection();
        boolean placementPresent = placement != null;
        boolean selectionPresent = selection != null;
        OperationTarget target = OperationTarget.empty();

        IntegrationState state;
        if (!placementPresent) {
            state = IntegrationState.PLACEMENT_MISSING;
        } else if (!placement.isEnabled()) {
            state = IntegrationState.PLACEMENT_DISABLED;
        } else if (!selectionPresent) {
            state = IntegrationState.SELECTION_MISSING;
        } else if (PositionUtils.getValidBoxes(selection).isEmpty()) {
            state = IntegrationState.SELECTION_INVALID;
        } else {
            target = LitematicaTargetMapping.snapshot(placement, selection);
            state = target.worldRegions().isEmpty() ? IntegrationState.TARGET_EMPTY : IntegrationState.READY;
        }

        return new ReadOnlyIntegrationDiagnostic(
                state,
                true,
                this.litematicaVersion,
                true,
                this.malilibVersion,
                placementPresent,
                selectionPresent,
                target);
    }

    @Override
    public Optional<SchematicSnapshotCapture> captureSnapshot() {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager()
                .getSelectedSchematicPlacement();
        AreaSelection selection = DataManager.getSelectionManager().getCurrentSelection();

        if (placement == null || !placement.isEnabled() || selection == null
                || PositionUtils.getValidBoxes(selection).isEmpty()) {
            return Optional.empty();
        }

        OperationTarget target = LitematicaTargetMapping.snapshot(placement, selection);
        if (target.worldRegions().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SchematicSnapshotCapture(
                target,
                LitematicaBlockSnapshotAdapter.snapshot(placement, target)));
    }
}
