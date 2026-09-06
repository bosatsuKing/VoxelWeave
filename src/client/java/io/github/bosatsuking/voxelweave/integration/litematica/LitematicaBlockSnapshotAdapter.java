package io.github.bosatsuking.voxelweave.integration.litematica;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import io.github.bosatsuking.voxelweave.domain.Region;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only bridge from Litematica's rendered schematic world into an immutable VoxelWeave snapshot.
 *
 * <p>The adapter only captures coordinates inside the already validated world-space operation target.
 * It rejects coordinates where another placement, or multiple subregions of the selected placement,
 * overlap. This prevents silently reading a composite schematic-world state that cannot be attributed
 * unambiguously to the selected placement.</p>
 */
final class LitematicaBlockSnapshotAdapter {
    private LitematicaBlockSnapshotAdapter() { }

    static SchematicSnapshot snapshot(SchematicPlacement selectedPlacement, OperationTarget target) {
        Objects.requireNonNull(selectedPlacement);
        Objects.requireNonNull(target);

        var schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) {
            throw new IllegalStateException("Litematica schematic world is unavailable");
        }

        var placementManager = DataManager.getSchematicPlacementManager();
        Map<Long, List<PlacementPart>> partsByChunk = new HashMap<>();
        Set<GridPoint> visited = new HashSet<>();
        Map<GridPoint, BlockStateRef> blocks = new HashMap<>();

        for (Region region : target.worldRegions()) {
            for (long x = region.minInclusive().x(); x < region.maxExclusive().x(); x++) {
                for (long y = region.minInclusive().y(); y < region.maxExclusive().y(); y++) {
                    for (long z = region.minInclusive().z(); z < region.maxExclusive().z(); z++) {
                        GridPoint point = new GridPoint(x, y, z);
                        if (!visited.add(point)) continue;

                        BlockPos worldPos = new BlockPos(
                                Math.toIntExact(x), Math.toIntExact(y), Math.toIntExact(z));
                        List<PlacementPart> parts = partsByChunk.computeIfAbsent(
                                chunkKey(x, z), ignored -> placementManager.getAllPlacementsTouchingChunk(worldPos));
                        requireUnambiguousSelectedPlacement(selectedPlacement, worldPos, parts);

                        // Read the state exactly as Litematica presents it in world space. This means
                        // placement/subregion rotation and mirror are already reflected in directional states.
                        blocks.put(point, new BlockStateRef(schematicWorld.getBlockState(worldPos).toString()));
                    }
                }
            }
        }

        return new SchematicSnapshot(blocks);
    }

    private static void requireUnambiguousSelectedPlacement(
            SchematicPlacement selectedPlacement,
            BlockPos worldPos,
            List<PlacementPart> parts) {
        int selectedParts = 0;
        boolean foreignPlacementPresent = false;

        for (PlacementPart part : parts) {
            if (!part.getBox().containsPos(worldPos)) continue;
            if (part.getPlacement() == selectedPlacement) {
                selectedParts++;
            } else {
                foreignPlacementPresent = true;
            }
        }

        if (selectedParts != 1) {
            throw new IllegalStateException(
                    "Selected Litematica placement is ambiguous at " + worldPos + " (matching parts: " + selectedParts + ")");
        }
        if (foreignPlacementPresent) {
            throw new IllegalStateException(
                    "Another Litematica placement overlaps the selected capture target at " + worldPos);
        }
    }

    private static long chunkKey(long x, long z) {
        long chunkX = x >> 4;
        long chunkZ = z >> 4;
        return (chunkX << 32) ^ (chunkZ & 0xFFFF_FFFFL);
    }
}
