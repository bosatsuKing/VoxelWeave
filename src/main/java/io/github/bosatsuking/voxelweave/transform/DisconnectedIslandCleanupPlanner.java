package io.github.bosatsuking.voxelweave.transform;

import io.github.bosatsuking.voxelweave.analysis.SurfaceAnalysis;
import io.github.bosatsuking.voxelweave.analysis.SurfaceCell;
import io.github.bosatsuking.voxelweave.analysis.SurfaceComponent;
import io.github.bosatsuking.voxelweave.analysis.SurfaceFeatureAnalysis;
import io.github.bosatsuking.voxelweave.analysis.SurfaceFeatureDescriptor;
import io.github.bosatsuking.voxelweave.domain.BlockChange;
import io.github.bosatsuking.voxelweave.domain.ChangeSet;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Plans bounded island cleanup only. Does not traverse neighbors or apply any changes. */
public final class DisconnectedIslandCleanupPlanner {
    private DisconnectedIslandCleanupPlanner() { }

    public static ChangeSet plan(SchematicSnapshot snapshot, SurfaceAnalysis surface,
                                 SurfaceFeatureAnalysis features, IslandCleanupRequest request) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(surface);
        Objects.requireNonNull(features);
        Objects.requireNonNull(request);
        // Check before even an empty/no-op result. Source identity includes all captured context,
        // not only occupied cells, and prevents mixing features from a different target/analysis.
        if (!surface.isFrom(snapshot)) {
            throw new IllegalArgumentException("Surface analysis must be generated from the current snapshot");
        }
        if (!features.isFrom(surface)) {
            throw new IllegalArgumentException("Feature analysis must be generated from the supplied surface analysis");
        }

        Set<GridPoint> eligibleRoots = new HashSet<>();
        for (SurfaceComponent component : surface.smallIslandCandidates(request.maxComponentSize())) {
            eligibleRoots.add(component.root());
        }

        // Complete the preservation pass before generating changes: one protected cell vetoes
        // the entire component, including cells appearing earlier in deterministic order.
        for (SurfaceCell cell : surface.cells()) {
            if (!eligibleRoots.contains(cell.componentRoot())) continue;
            SurfaceFeatureDescriptor descriptor = features.descriptorAt(cell.position()).orElseThrow(
                    () -> new IllegalArgumentException("Missing feature evidence for analyzed cell"));
            if (cell.hasUnknownBoundary() || !descriptor.hasCompleteContext()
                    || request.protectedFeatureKinds().contains(descriptor.kind())) {
                eligibleRoots.remove(cell.componentRoot());
            }
        }

        ArrayList<BlockChange> changes = new ArrayList<>();
        for (SurfaceCell cell : surface.cells()) {
            if (eligibleRoots.contains(cell.componentRoot()) && !cell.state().equals(request.replacementState())) {
                changes.add(new BlockChange(cell.position(), cell.state(), request.replacementState()));
            }
        }
        return ChangeSet.of(changes);
    }
}
