package io.github.bosatsuking.voxelweave.integration;

public record ReadOnlyIntegrationDiagnostic(
        IntegrationState state,
        boolean litematicaLoaded,
        String litematicaVersion,
        boolean malilibLoaded,
        String malilibVersion,
        boolean selectedPlacementPresent,
        boolean currentSelectionPresent) {

    public boolean operationTargetReady() {
        return this.state == IntegrationState.READY;
    }
}
