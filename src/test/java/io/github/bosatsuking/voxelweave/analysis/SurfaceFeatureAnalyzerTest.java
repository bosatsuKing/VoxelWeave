package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurfaceFeatureAnalyzerTest {
    private static final BlockStateRef STONE = new BlockStateRef("stone");
    private static final GridPoint ROOT = p(0, 0, 0);

    @Test
    void unknownBoundaryTakesPrecedenceOverConfidentGeometryClasses() {
        SurfaceCell cell = cell(
                ROOT,
                List.of(VoxelFace.UP),
                List.of(VoxelFace.EAST),
                4,
                ROOT);

        SurfaceFeatureDescriptor descriptor = analyze(cell, new SurfaceComponent(ROOT, 1, false));

        assertEquals(SurfaceFeatureKind.UNKNOWN_BOUNDARY, descriptor.kind());
        assertFalse(descriptor.hasCompleteContext());
    }

    @Test
    void classifiesInterior() {
        SurfaceFeatureDescriptor descriptor = analyze(
                cell(ROOT, List.of(), List.of(), 6, ROOT),
                new SurfaceComponent(ROOT, 1, true));

        assertEquals(SurfaceFeatureKind.INTERIOR, descriptor.kind());
        assertEquals(0, descriptor.exposedAxisCount());
        assertTrue(descriptor.hasCompleteContext());
    }

    @Test
    void classifiesIsolatedVoxel() {
        SurfaceFeatureDescriptor descriptor = analyze(
                cell(ROOT, List.of(VoxelFace.values()), List.of(), 0, ROOT),
                new SurfaceComponent(ROOT, 1, true));

        assertEquals(SurfaceFeatureKind.ISOLATED, descriptor.kind());
        assertEquals(3, descriptor.exposedAxisCount());
        assertEquals(3, descriptor.oppositeExposurePairCount());
        assertFalse(descriptor.hasDirectionalExposure());
    }

    @Test
    void classifiesTipBeforeThinFeature() {
        SurfaceFeatureDescriptor descriptor = analyze(
                cell(ROOT,
                        List.of(VoxelFace.UP, VoxelFace.NORTH, VoxelFace.SOUTH, VoxelFace.WEST, VoxelFace.EAST),
                        List.of(),
                        1,
                        ROOT),
                new SurfaceComponent(ROOT, 1, true));

        assertEquals(SurfaceFeatureKind.TIP, descriptor.kind());
        assertEquals(2, descriptor.oppositeExposurePairCount());
        assertEquals(0, descriptor.exposureVectorX());
        assertEquals(1, descriptor.exposureVectorY());
        assertEquals(0, descriptor.exposureVectorZ());
    }

    @Test
    void classifiesOneVoxelThinFeatureFromOppositeExposure() {
        SurfaceFeatureDescriptor descriptor = analyze(
                cell(ROOT, List.of(VoxelFace.WEST, VoxelFace.EAST), List.of(), 4, ROOT),
                new SurfaceComponent(ROOT, 1, true));

        assertEquals(SurfaceFeatureKind.THIN_FEATURE, descriptor.kind());
        assertEquals(1, descriptor.exposedAxisCount());
        assertEquals(1, descriptor.oppositeExposurePairCount());
        assertFalse(descriptor.hasDirectionalExposure());
    }

    @Test
    void classifiesCornerEdgeAndFaceFromIndependentExposedAxes() {
        SurfaceCell corner = cell(p(0, 0, 0),
                List.of(VoxelFace.UP, VoxelFace.EAST, VoxelFace.SOUTH), List.of(), 3, ROOT);
        SurfaceCell edge = cell(p(1, 0, 0),
                List.of(VoxelFace.UP, VoxelFace.EAST), List.of(), 4, ROOT);
        SurfaceCell face = cell(p(2, 0, 0),
                List.of(VoxelFace.UP), List.of(), 5, ROOT);
        SurfaceComponent component = new SurfaceComponent(ROOT, 3, true);

        SurfaceFeatureAnalysis analysis = SurfaceFeatureAnalyzer.analyze(
                new SurfaceAnalysis(List.of(face, corner, edge), List.of(component)));

        SurfaceFeatureDescriptor cornerDescriptor = analysis.descriptorAt(p(0, 0, 0)).orElseThrow();
        assertEquals(SurfaceFeatureKind.CORNER, cornerDescriptor.kind());
        assertEquals(3, cornerDescriptor.exposedAxisCount());
        assertEquals(1, cornerDescriptor.exposureVectorX());
        assertEquals(1, cornerDescriptor.exposureVectorY());
        assertEquals(1, cornerDescriptor.exposureVectorZ());

        SurfaceFeatureDescriptor edgeDescriptor = analysis.descriptorAt(p(1, 0, 0)).orElseThrow();
        assertEquals(SurfaceFeatureKind.EDGE, edgeDescriptor.kind());
        assertEquals(2, edgeDescriptor.exposedAxisCount());
        assertEquals(1, edgeDescriptor.exposureVectorX());
        assertEquals(1, edgeDescriptor.exposureVectorY());
        assertEquals(0, edgeDescriptor.exposureVectorZ());

        SurfaceFeatureDescriptor faceDescriptor = analysis.descriptorAt(p(2, 0, 0)).orElseThrow();
        assertEquals(SurfaceFeatureKind.FACE, faceDescriptor.kind());
        assertEquals(1, faceDescriptor.exposedAxisCount());
        assertEquals(0, faceDescriptor.exposureVectorX());
        assertEquals(1, faceDescriptor.exposureVectorY());
        assertEquals(0, faceDescriptor.exposureVectorZ());
    }

    @Test
    void countsAdjacentSurfaceNeighborsWithoutInventingMissingCells() {
        GridPoint firstPos = p(0, 0, 0);
        GridPoint secondPos = p(1, 0, 0);
        SurfaceCell first = cell(firstPos,
                List.of(VoxelFace.UP, VoxelFace.WEST), List.of(), 4, ROOT);
        SurfaceCell second = cell(secondPos,
                List.of(VoxelFace.UP, VoxelFace.EAST), List.of(), 4, ROOT);
        SurfaceAnalysis topology = new SurfaceAnalysis(
                List.of(second, first),
                List.of(new SurfaceComponent(ROOT, 2, true)));

        SurfaceFeatureAnalysis analysis = SurfaceFeatureAnalyzer.analyze(topology);

        assertEquals(1, analysis.descriptorAt(firstPos).orElseThrow().surfaceNeighborCount());
        assertEquals(1, analysis.descriptorAt(secondPos).orElseThrow().surfaceNeighborCount());
    }

    @Test
    void propagatesComponentCompletenessIntoContextConfidence() {
        SurfaceFeatureDescriptor descriptor = analyze(
                cell(ROOT, List.of(VoxelFace.UP), List.of(), 5, ROOT),
                new SurfaceComponent(ROOT, 1, false));

        assertEquals(SurfaceFeatureKind.FACE, descriptor.kind());
        assertFalse(descriptor.componentComplete());
        assertFalse(descriptor.hasCompleteContext());
    }

    @Test
    void outputOrderingAndKindFilteringAreDeterministic() {
        GridPoint p0 = p(0, 0, 0);
        GridPoint p1 = p(1, 0, 0);
        GridPoint p2 = p(2, 0, 0);
        SurfaceComponent component = new SurfaceComponent(p0, 3, true);
        SurfaceAnalysis topology = new SurfaceAnalysis(List.of(
                cell(p2, List.of(VoxelFace.UP), List.of(), 5, p0),
                cell(p0, List.of(VoxelFace.UP, VoxelFace.EAST), List.of(), 4, p0),
                cell(p1, List.of(VoxelFace.UP), List.of(), 5, p0)),
                List.of(component));

        SurfaceFeatureAnalysis analysis = SurfaceFeatureAnalyzer.analyze(topology);

        assertEquals(List.of(p0, p1, p2),
                analysis.descriptors().stream().map(SurfaceFeatureDescriptor::position).toList());
        assertEquals(List.of(p1, p2),
                analysis.ofKind(SurfaceFeatureKind.FACE).stream()
                        .map(SurfaceFeatureDescriptor::position).toList());
    }

    private static SurfaceFeatureDescriptor analyze(SurfaceCell cell, SurfaceComponent component) {
        return SurfaceFeatureAnalyzer.analyze(new SurfaceAnalysis(List.of(cell), List.of(component)))
                .descriptorAt(cell.position()).orElseThrow();
    }

    private static SurfaceCell cell(
            GridPoint position,
            List<VoxelFace> exposed,
            List<VoxelFace> unknown,
            int occupiedNeighbors,
            GridPoint componentRoot) {
        return new SurfaceCell(position, STONE, exposed, unknown, occupiedNeighbors, componentRoot);
    }

    private static GridPoint p(long x, long y, long z) {
        return new GridPoint(x, y, z);
    }
}
