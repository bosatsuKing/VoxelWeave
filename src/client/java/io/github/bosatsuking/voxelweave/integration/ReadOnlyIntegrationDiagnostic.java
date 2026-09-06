package io.github.bosatsuking.voxelweave.integration;

import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import java.util.Objects;

public record ReadOnlyIntegrationDiagnostic(
        IntegrationState state,
        boolean litematicaLoaded,
        String litematicaVersion,
        boolean malilibLoaded,
        String malilibVersion,
        boolean selectedPlacementPresent,
        boolean currentSelectionPresent,
        OperationTarget operationTarget) {

    public ReadOnlyIntegrationDiagnostic {
        Objects.requireNonNull(operationTarget);
    }

    public ReadOnlyIntegrationDiagnostic(IntegrationState state, boolean litematicaLoaded,
            String litematicaVersion, boolean malilibLoaded, String malilibVersion,
            boolean selectedPlacementPresent, boolean currentSelectionPresent) {
        this(state, litematicaLoaded, litematicaVersion, malilibLoaded, malilibVersion,
                selectedPlacementPresent, currentSelectionPresent, OperationTarget.empty());
    }

    public boolean operationTargetReady() {
        return this.state == IntegrationState.READY && !operationTarget.worldRegions().isEmpty();
    }
}
