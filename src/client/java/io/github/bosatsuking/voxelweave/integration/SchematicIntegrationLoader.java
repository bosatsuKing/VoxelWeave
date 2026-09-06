package io.github.bosatsuking.voxelweave.integration;

import io.github.bosatsuking.voxelweave.integration.litematica.LitematicaReadOnlyIntegration;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;

import java.util.Optional;

public final class SchematicIntegrationLoader {
    public static final String SUPPORTED_LITEMATICA_VERSION = "0.27.12";
    public static final String SUPPORTED_MALILIB_VERSION = "0.28.11";

    private SchematicIntegrationLoader() {
    }

    public static ReadOnlySchematicIntegration load(Logger logger) {
        FabricLoader loader = FabricLoader.getInstance();
        Optional<ModContainer> litematica = loader.getModContainer("litematica");
        Optional<ModContainer> malilib = loader.getModContainer("malilib");
        String litematicaVersion = versionOf(litematica);
        String malilibVersion = versionOf(malilib);

        if (litematica.isEmpty()) {
            return unavailable(IntegrationState.LITEMATICA_MISSING, false, litematicaVersion,
                    malilib.isPresent(), malilibVersion);
        }
        if (malilib.isEmpty()) {
            return unavailable(IntegrationState.MALILIB_MISSING, true, litematicaVersion,
                    false, malilibVersion);
        }
        if (!SUPPORTED_LITEMATICA_VERSION.equals(litematicaVersion)) {
            logger.warn("Unsupported Litematica version {}; expected {}",
                    litematicaVersion, SUPPORTED_LITEMATICA_VERSION);
            return unavailable(IntegrationState.LITEMATICA_UNSUPPORTED, true, litematicaVersion,
                    true, malilibVersion);
        }
        if (!SUPPORTED_MALILIB_VERSION.equals(malilibVersion)) {
            logger.warn("Unsupported MaLiLib version {}; expected {}",
                    malilibVersion, SUPPORTED_MALILIB_VERSION);
            return unavailable(IntegrationState.MALILIB_UNSUPPORTED, true, litematicaVersion,
                    true, malilibVersion);
        }

        try {
            ReadOnlySchematicIntegration integration =
                    new LitematicaReadOnlyIntegration(litematicaVersion, malilibVersion);
            return () -> safelyDiagnose(integration, logger, litematicaVersion, malilibVersion);
        } catch (LinkageError error) {
            logger.error("VoxelWeave could not link its read-only Litematica adapter", error);
            return unavailable(IntegrationState.INTEGRATION_ERROR, true, litematicaVersion,
                    true, malilibVersion);
        }
    }

    private static String versionOf(Optional<ModContainer> container) {
        return container.map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("");
    }

    private static ReadOnlyIntegrationDiagnostic safelyDiagnose(
            ReadOnlySchematicIntegration integration,
            Logger logger,
            String litematicaVersion,
            String malilibVersion) {
        try {
            return integration.diagnose();
        } catch (RuntimeException | LinkageError error) {
            logger.error("VoxelWeave read-only Litematica diagnostic failed", error);
            return new ReadOnlyIntegrationDiagnostic(
                    IntegrationState.INTEGRATION_ERROR,
                    true,
                    litematicaVersion,
                    true,
                    malilibVersion,
                    false,
                    false);
        }
    }

    private static ReadOnlySchematicIntegration unavailable(
            IntegrationState state,
            boolean litematicaLoaded,
            String litematicaVersion,
            boolean malilibLoaded,
            String malilibVersion) {
        ReadOnlyIntegrationDiagnostic diagnostic = new ReadOnlyIntegrationDiagnostic(
                state,
                litematicaLoaded,
                litematicaVersion,
                malilibLoaded,
                malilibVersion,
                false,
                false);
        return () -> diagnostic;
    }
}
