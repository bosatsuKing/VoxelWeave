package io.github.bosatsuking.voxelweave;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.bosatsuking.voxelweave.integration.IntegrationState;
import io.github.bosatsuking.voxelweave.integration.ReadOnlyIntegrationDiagnostic;
import io.github.bosatsuking.voxelweave.integration.ReadOnlySchematicIntegration;
import io.github.bosatsuking.voxelweave.integration.SchematicIntegrationLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoxelWeaveClient implements ClientModInitializer {
    public static final String MOD_ID = "voxelweave";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "general"));

    private ReadOnlySchematicIntegration schematicIntegration;
    private KeyMapping diagnosticKey;

    @Override
    public void onInitializeClient() {
        this.schematicIntegration = SchematicIntegrationLoader.load(LOGGER);
        this.diagnosticKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.voxelweave.diagnose_integration",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_V,
                KEY_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (this.diagnosticKey.consumeClick()) {
                if (client.player != null) {
                    showDiagnostic(this.safeDiagnose(), client.player::sendSystemMessage);
                }
            }
        });

        LOGGER.info("VoxelWeave initialized in read-only integration mode");
    }

    private ReadOnlyIntegrationDiagnostic safeDiagnose() {
        try {
            return this.schematicIntegration.diagnose();
        } catch (RuntimeException | LinkageError error) {
            LOGGER.error("VoxelWeave read-only integration diagnostic failed", error);
            return new ReadOnlyIntegrationDiagnostic(
                    IntegrationState.INTEGRATION_ERROR,
                    false,
                    "",
                    false,
                    "",
                    false,
                    false);
        }
    }

    private static void showDiagnostic(
            ReadOnlyIntegrationDiagnostic diagnostic,
            java.util.function.Consumer<Component> messageConsumer) {
        messageConsumer.accept(Component.translatable("message.voxelweave.diagnostic.header"));
        messageConsumer.accept(Component.translatable(
                "message.voxelweave.diagnostic.litematica",
                yesNo(diagnostic.litematicaLoaded()),
                displayVersion(diagnostic.litematicaVersion())));
        messageConsumer.accept(Component.translatable(
                "message.voxelweave.diagnostic.malilib",
                yesNo(diagnostic.malilibLoaded()),
                displayVersion(diagnostic.malilibVersion())));
        messageConsumer.accept(Component.translatable(
                "message.voxelweave.diagnostic.placement",
                yesNo(diagnostic.selectedPlacementPresent())));
        messageConsumer.accept(Component.translatable(
                "message.voxelweave.diagnostic.selection",
                yesNo(diagnostic.currentSelectionPresent())));
        messageConsumer.accept(Component.translatable(diagnostic.state().translationKey()));
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(value
                ? "message.voxelweave.diagnostic.yes"
                : "message.voxelweave.diagnostic.no");
    }

    private static Component displayVersion(String version) {
        return version.isBlank()
                ? Component.translatable("message.voxelweave.diagnostic.version_unavailable")
                : Component.literal(version);
    }
}
