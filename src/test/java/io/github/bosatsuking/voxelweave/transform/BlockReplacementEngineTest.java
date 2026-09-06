package io.github.bosatsuking.voxelweave.transform;

import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.ChangeSet;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import io.github.bosatsuking.voxelweave.domain.PlacementTarget;
import io.github.bosatsuking.voxelweave.domain.Region;
import io.github.bosatsuking.voxelweave.domain.ReplacementRequest;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;
import io.github.bosatsuking.voxelweave.domain.Selection;
import io.github.bosatsuking.voxelweave.domain.SelectionBox;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockReplacementEngineTest {
    private static final BlockStateRef A = new BlockStateRef("minecraft:stone");
    private static final BlockStateRef B = new BlockStateRef("minecraft:quartz_block");
    private static final BlockStateRef C = new BlockStateRef("minecraft:dirt");

    @Test
    void replacesOnlyMatchingBlocksInsideIntersection() {
        SchematicSnapshot snapshot = new SchematicSnapshot(Map.of(
                p(0, 0, 0), A,
                p(1, 0, 0), C,
                p(2, 0, 0), A));
        OperationTarget target = target(region(0, 0, 0, 2, 1, 1), region(0, 0, 0, 3, 1, 1));

        ChangeSet result = BlockReplacementEngine.replace(snapshot, target, new ReplacementRequest(A, B));

        assertEquals(List.of(p(0, 0, 0)), result.changes().stream().map(change -> change.position()).toList());
        assertEquals(A, snapshot.blockAt(p(0, 0, 0)));
    }

    @Test
    void ignoresBlocksOutsideSelectionAndPlacementIntersection() {
        SchematicSnapshot snapshot = new SchematicSnapshot(Map.of(
                p(0, 0, 0), A,
                p(1, 0, 0), A,
                p(2, 0, 0), A));
        OperationTarget target = target(region(0, 0, 0, 2, 1, 1), region(1, 0, 0, 3, 1, 1));

        ChangeSet result = BlockReplacementEngine.replace(snapshot, target, new ReplacementRequest(A, B));

        assertEquals(List.of(p(1, 0, 0)), result.changes().stream().map(change -> change.position()).toList());
    }

    @Test
    void deduplicatesCoordinatesAcrossOverlappingRegions() {
        Selection selection = new Selection(List.of(
                new SelectionBox(region(0, 0, 0, 3, 1, 1)),
                new SelectionBox(region(1, 0, 0, 4, 1, 1))));
        PlacementTarget placement = new PlacementTarget(List.of(region(0, 0, 0, 4, 1, 1)));
        OperationTarget target = new OperationTarget(selection, Optional.of(placement));
        SchematicSnapshot snapshot = new SchematicSnapshot(Map.of(
                p(0, 0, 0), A, p(1, 0, 0), A, p(2, 0, 0), A, p(3, 0, 0), A));

        ChangeSet result = BlockReplacementEngine.replace(snapshot, target, new ReplacementRequest(A, B));

        assertEquals(List.of(p(0, 0, 0), p(1, 0, 0), p(2, 0, 0), p(3, 0, 0)),
                result.changes().stream().map(change -> change.position()).toList());
    }

    @Test
    void returnsEmptyWhenNoSourceBlockMatches() {
        ChangeSet result = BlockReplacementEngine.replace(
                new SchematicSnapshot(Map.of(p(0, 0, 0), C)),
                target(region(0, 0, 0, 1, 1, 1), region(0, 0, 0, 1, 1, 1)),
                new ReplacementRequest(A, B));
        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyForNoOpRequest() {
        ChangeSet result = BlockReplacementEngine.replace(
                new SchematicSnapshot(Map.of(p(0, 0, 0), A)),
                target(region(0, 0, 0, 1, 1, 1), region(0, 0, 0, 1, 1, 1)),
                new ReplacementRequest(A, A));
        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyForEmptyOperationTarget() {
        ChangeSet result = BlockReplacementEngine.replace(
                new SchematicSnapshot(Map.of(p(0, 0, 0), A)), OperationTarget.empty(), new ReplacementRequest(A, B));
        assertTrue(result.isEmpty());
    }

    @Test
    void changeOrderingIsDeterministic() {
        SchematicSnapshot snapshot = new SchematicSnapshot(Map.of(
                p(2, 0, 0), A,
                p(0, 0, 0), A,
                p(1, 0, 0), A));
        OperationTarget target = target(region(0, 0, 0, 3, 1, 1), region(0, 0, 0, 3, 1, 1));

        ChangeSet result = BlockReplacementEngine.replace(snapshot, target, new ReplacementRequest(A, B));

        assertEquals(List.of(p(0, 0, 0), p(1, 0, 0), p(2, 0, 0)),
                result.changes().stream().map(change -> change.position()).toList());
    }

    private static OperationTarget target(Region selectionRegion, Region placementRegion) {
        return new OperationTarget(new Selection(List.of(new SelectionBox(selectionRegion))),
                Optional.of(new PlacementTarget(List.of(placementRegion))));
    }

    private static Region region(long minX, long minY, long minZ, long maxX, long maxY, long maxZ) {
        return new Region(p(minX, minY, minZ), p(maxX, maxY, maxZ));
    }

    private static GridPoint p(long x, long y, long z) {
        return new GridPoint(x, y, z);
    }
}
