package io.github.bosatsuking.voxelweave.transform;

import io.github.bosatsuking.voxelweave.domain.BlockChange;
import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.ChangeSet;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import io.github.bosatsuking.voxelweave.domain.Region;
import io.github.bosatsuking.voxelweave.domain.ReplacementRequest;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure bounded block replacement. Produces a previewable ChangeSet and never mutates the snapshot. */
public final class BlockReplacementEngine {
    private BlockReplacementEngine() { }

    public static ChangeSet replace(SchematicSnapshot schematic, OperationTarget target,
            ReplacementRequest request) {
        Objects.requireNonNull(schematic);
        Objects.requireNonNull(target);
        Objects.requireNonNull(request);
        if (request.isNoOp()) return ChangeSet.empty();

        List<Region> regions = target.worldRegions();
        if (regions.isEmpty()) return ChangeSet.empty();

        Set<GridPoint> visited = new HashSet<>();
        List<BlockChange> changes = new ArrayList<>();
        for (Region region : regions) {
            for (long x = region.minInclusive().x(); x < region.maxExclusive().x(); x++) {
                for (long y = region.minInclusive().y(); y < region.maxExclusive().y(); y++) {
                    for (long z = region.minInclusive().z(); z < region.maxExclusive().z(); z++) {
                        GridPoint position = new GridPoint(x, y, z);
                        if (!visited.add(position)) continue;
                        BlockStateRef before = schematic.blockAt(position);
                        if (request.source().equals(before)) {
                            changes.add(new BlockChange(position, before, request.target()));
                        }
                    }
                }
            }
        }
        return ChangeSet.of(changes);
    }
}
