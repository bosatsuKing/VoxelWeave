package io.github.bosatsuking.voxelweave.integration;

public enum IntegrationState {
    READY("message.voxelweave.diagnostic.ready"),
    LITEMATICA_MISSING("message.voxelweave.diagnostic.litematica_missing"),
    MALILIB_MISSING("message.voxelweave.diagnostic.malilib_missing"),
    LITEMATICA_UNSUPPORTED("message.voxelweave.diagnostic.litematica_unsupported"),
    MALILIB_UNSUPPORTED("message.voxelweave.diagnostic.malilib_unsupported"),
    PLACEMENT_MISSING("message.voxelweave.diagnostic.placement_missing"),
    PLACEMENT_DISABLED("message.voxelweave.diagnostic.placement_disabled"),
    SELECTION_MISSING("message.voxelweave.diagnostic.selection_missing"),
    SELECTION_INVALID("message.voxelweave.diagnostic.selection_invalid"),
    TARGET_EMPTY("message.voxelweave.diagnostic.target_empty"),
    INTEGRATION_ERROR("message.voxelweave.diagnostic.integration_error");

    private final String translationKey;

    IntegrationState(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }
}
