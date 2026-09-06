package io.github.bosatsuking.voxelweave.integration;

@FunctionalInterface
public interface ReadOnlySchematicIntegration {
    ReadOnlyIntegrationDiagnostic diagnose();
}
