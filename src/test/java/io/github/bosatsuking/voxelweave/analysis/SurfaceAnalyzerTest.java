package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import io.github.bosatsuking.voxelweave.domain.PlacementTarget;
import io.github.bosatsuking.voxelweave.domain.Region;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;
import io.github.bosatsuking.voxelweave.domain.Selection;
import io.github.bosatsuking.voxelweave.domain.SelectionBox;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurfaceAnalyzerTest {
    private static final BlockStateRef AIR = new BlockStateRef("air");
    private static final BlockStateRef STONE = new BlockStateRef("stone");
    private static final BlockOccupancyPolicy NON_AIR = state -> !state.equals(AIR);

    @Test
    void classifiesInteriorAndKnownExposedFaces() {
        Region targetRegion = region(0, 0, 0, 3, 3, 3);
        Map<GridPoint, BlockStateRef> blocks = airHalo(targetRegion);
        for (long x = 0; x < 3; x++) {
            for (long y = 0; y < 3; y++) {
                for (long z = 0; z < 3; z++) blocks.put(p(x, y, z), STONE);
            }
        }

        SurfaceAnalysis analysis = SurfaceAnalyzer.analyze(
                new SchematicSnapshot(blocks), target(targetRegion), NON_AIR);

        SurfaceCell center = analysis.cellAt(p(1, 1, 1)).orElseThrow();
        assertTrue(center.isInterior());
        assertFalse(center.isSurface());
        assertEquals(6, center.occupiedNeighborCount());

        SurfaceCell corner = analysis.cellAt(p(0, 0, 0)).orElseThrow();
        assertEquals(List.of(VoxelFace.DOWN, VoxelFace.NORTH, VoxelFace.WEST), corner.exposedFaces());
        assertTrue(corner.isSurface());
        assertFalse(corner.hasUnknownBoundary());

        SurfaceComponent component = analysis.components().getFirst();
        assertEquals(27, component.size());
        assertTrue(component.complete());
    }

    @Test
    void missingNeighborDataRemainsUnknownInsteadOfAir() {
        Region targetRegion = region(0, 0, 0, 1, 1, 1);
        SurfaceAnalysis analysis = SurfaceAnalyzer.analyze(
                new SchematicSnapshot(Map.of(p(0, 0, 0), STONE)), target(targetRegion), NON_AIR);

        SurfaceCell cell = analysis.cellAt(p(0, 0, 0)).orElseThrow();
        assertTrue(cell.exposedFaces().isEmpty());
        assertEquals(List.of(VoxelFace.values()), cell.unknownFaces());
        assertTrue(cell.hasUnknownBoundary());
        assertFalse(cell.isSurface());
        assertFalse(cell.isIsolated());
        assertFalse(analysis.components().getFirst().complete());
    }

    @Test
    void identifiesSafeIsolatedAndWeakSupportCandidateWhenNeighborhoodIsKnown() {
        Region targetRegion = region(0, 0, 0, 1, 1, 1);
        Map<GridPoint, BlockStateRef> blocks = airHalo(targetRegion);
        blocks.put(p(0, 0, 0), STONE);

        SurfaceAnalysis analysis = SurfaceAnalyzer.analyze(
                new SchematicSnapshot(blocks), target(targetRegion), NON_AIR);

        SurfaceCell cell = analysis.cellAt(p(0, 0, 0)).orElseThrow();
        assertTrue(cell.isIsolated());
        assertTrue(cell.isWeaklySupported());
        assertEquals(6, cell.knownEmptyNeighborCount());
        assertEquals(1, analysis.smallIslandCandidates(1).size());
    }

    @Test
    void computesDeterministicDisconnectedComponentSizes() {
        Region targetRegion = region(0, 0, 0, 5, 1, 1);
        Map<GridPoint, BlockStateRef> blocks = airHalo(targetRegion);
        blocks.put(p(0, 0, 0), STONE);
        blocks.put(p(1, 0, 0), STONE);
        blocks.put(p(4, 0, 0), STONE);

        SurfaceAnalysis analysis = SurfaceAnalyzer.analyze(
                new SchematicSnapshot(blocks), target(targetRegion), NON_AIR);

        assertEquals(List.of(p(0, 0, 0), p(4, 0, 0)),
                analysis.components().stream().map(SurfaceComponent::root).toList());
        assertEquals(List.of(2, 1), analysis.components().stream().map(SurfaceComponent::size).toList());
        assertTrue(analysis.components().stream().allMatch(SurfaceComponent::complete));
        assertEquals(List.of(p(4, 0, 0)),
                analysis.smallIslandCandidates(1).stream().map(SurfaceComponent::root).toList());
    }

    @Test
    void componentIsIncompleteWhenOccupiedGeometryContinuesOutsideTarget() {
        Region targetRegion = region(0, 0, 0, 1, 1, 1);
        Map<GridPoint, BlockStateRef> blocks = airHalo(targetRegion);
        blocks.put(p(0, 0, 0), STONE);
        blocks.put(p(1, 0, 0), STONE);

        SurfaceAnalysis analysis = SurfaceAnalyzer.analyze(
                new SchematicSnapshot(blocks), target(targetRegion), NON_AIR);

        SurfaceCell cell = analysis.cellAt(p(0, 0, 0)).orElseThrow();
        assertEquals(1, cell.occupiedNeighborCount());
        assertFalse(analysis.components().getFirst().complete());
        assertTrue(analysis.smallIslandCandidates(1).isEmpty());
    }

    @Test
    void overlappingTargetRegionsDoNotDuplicateCellsAndOrderingIsStable() {
        Selection selection = new Selection(List.of(
                new SelectionBox(region(0, 0, 0, 3, 1, 1)),
                new SelectionBox(region(1, 0, 0, 4, 1, 1))));
        PlacementTarget placement = new PlacementTarget(List.of(region(0, 0, 0, 4, 1, 1)));
        OperationTarget target = new OperationTarget(selection, Optional.of(placement));
        Map<GridPoint, BlockStateRef> blocks = airHalo(region(0, 0, 0, 4, 1, 1));
        blocks.put(p(3, 0, 0), STONE);
        blocks.put(p(1, 0, 0), STONE);
        blocks.put(p(2, 0, 0), STONE);
        blocks.put(p(0, 0, 0), STONE);

        SurfaceAnalysis analysis = SurfaceAnalyzer.analyze(new SchematicSnapshot(blocks), target, NON_AIR);

        assertEquals(List.of(p(0, 0, 0), p(1, 0, 0), p(2, 0, 0), p(3, 0, 0)),
                analysis.cells().stream().map(SurfaceCell::position).toList());
        assertEquals(4, analysis.cells().size());
    }

    @Test
    void occupancySemanticsAreProvidedByPolicyNotHardCodedIntoAnalyzer() {
        BlockStateRef customEmpty = new BlockStateRef("custom:transparent_marker");
        BlockOccupancyPolicy policy = state -> state.equals(STONE);
        Region targetRegion = region(0, 0, 0, 1, 1, 1);
        Map<GridPoint, BlockStateRef> blocks = airHalo(targetRegion);
        blocks.replaceAll((point, state) -> customEmpty);
        blocks.put(p(0, 0, 0), STONE);

        SurfaceCell cell = SurfaceAnalyzer.analyze(
                new SchematicSnapshot(blocks), target(targetRegion), policy)
                .cellAt(p(0, 0, 0)).orElseThrow();

        assertTrue(cell.isIsolated());
        assertEquals(6, cell.exposedFaces().size());
    }

    @Test
    void analysisDoesNotMutateInputSnapshot() {
        Region targetRegion = region(0, 0, 0, 1, 1, 1);
        Map<GridPoint, BlockStateRef> source = new HashMap<>(airHalo(targetRegion));
        source.put(p(0, 0, 0), STONE);
        SchematicSnapshot snapshot = new SchematicSnapshot(source);
        Map<GridPoint, BlockStateRef> before = snapshot.blocks();

        SurfaceAnalyzer.analyze(snapshot, target(targetRegion), NON_AIR);

        assertEquals(before, snapshot.blocks());
        assertEquals(STONE, snapshot.blockAt(p(0, 0, 0)));
    }

    private static Map<GridPoint, BlockStateRef> airHalo(Region target) {
        Map<GridPoint, BlockStateRef> blocks = new HashMap<>();
        for (long x = target.minInclusive().x() - 1; x <= target.maxExclusive().x(); x++) {
            for (long y = target.minInclusive().y() - 1; y <= target.maxExclusive().y(); y++) {
                for (long z = target.minInclusive().z() - 1; z <= target.maxExclusive().z(); z++) {
                    blocks.put(p(x, y, z), AIR);
                }
            }
        }
        return blocks;
    }

    private static OperationTarget target(Region region) {
        return new OperationTarget(
                new Selection(List.of(new SelectionBox(region))),
                Optional.of(new PlacementTarget(List.of(region))));
    }

    private static Region region(long minX, long minY, long minZ, long maxX, long maxY, long maxZ) {
        return new Region(p(minX, minY, minZ), p(maxX, maxY, maxZ));
    }

    private static GridPoint p(long x, long y, long z) {
        return new GridPoint(x, y, z);
    }
}
