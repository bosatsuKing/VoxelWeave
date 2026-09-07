package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.GridPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure higher-order feature classification over Step 6A surface topology. */
public final class SurfaceFeatureAnalyzer {
    private SurfaceFeatureAnalyzer() { }

    public static SurfaceFeatureAnalysis analyze(SurfaceAnalysis surfaceAnalysis) {
        Objects.requireNonNull(surfaceAnalysis);
        if (surfaceAnalysis.cells().isEmpty()) return SurfaceFeatureAnalysis.empty();

        Map<GridPoint, SurfaceCell> cellsByPosition = new HashMap<>(surfaceAnalysis.cells().size());
        for (SurfaceCell cell : surfaceAnalysis.cells()) cellsByPosition.put(cell.position(), cell);

        List<SurfaceFeatureDescriptor> descriptors = new ArrayList<>(surfaceAnalysis.cells().size());
        for (SurfaceCell cell : surfaceAnalysis.cells()) {
            SurfaceComponent component = surfaceAnalysis.componentAt(cell.componentRoot())
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing component for surface cell " + cell.position()));
            int exposedAxisCount = exposedAxisCount(cell.exposedFaces());
            int oppositePairCount = oppositeExposurePairCount(cell.exposedFaces());
            int[] exposureVector = exposureVector(cell.exposedFaces());
            int surfaceNeighborCount = surfaceNeighborCount(cellsByPosition, cell.position());

            descriptors.add(new SurfaceFeatureDescriptor(
                    cell.position(),
                    classify(cell, exposedAxisCount, oppositePairCount),
                    exposureVector[0],
                    exposureVector[1],
                    exposureVector[2],
                    exposedAxisCount,
                    oppositePairCount,
                    surfaceNeighborCount,
                    component.complete()));
        }

        return new SurfaceFeatureAnalysis(descriptors);
    }

    private static SurfaceFeatureKind classify(
            SurfaceCell cell,
            int exposedAxisCount,
            int oppositePairCount) {
        if (cell.hasUnknownBoundary()) return SurfaceFeatureKind.UNKNOWN_BOUNDARY;
        if (cell.isInterior()) return SurfaceFeatureKind.INTERIOR;
        if (cell.isIsolated()) return SurfaceFeatureKind.ISOLATED;
        if (cell.occupiedNeighborCount() == 1) return SurfaceFeatureKind.TIP;
        if (oppositePairCount > 0) return SurfaceFeatureKind.THIN_FEATURE;
        return switch (exposedAxisCount) {
            case 3 -> SurfaceFeatureKind.CORNER;
            case 2 -> SurfaceFeatureKind.EDGE;
            case 1 -> SurfaceFeatureKind.FACE;
            case 0 -> SurfaceFeatureKind.INTERIOR;
            default -> throw new IllegalStateException("Unexpected exposed-axis count: " + exposedAxisCount);
        };
    }

    private static int exposedAxisCount(List<VoxelFace> exposedFaces) {
        boolean x = exposedFaces.contains(VoxelFace.WEST) || exposedFaces.contains(VoxelFace.EAST);
        boolean y = exposedFaces.contains(VoxelFace.DOWN) || exposedFaces.contains(VoxelFace.UP);
        boolean z = exposedFaces.contains(VoxelFace.NORTH) || exposedFaces.contains(VoxelFace.SOUTH);
        return (x ? 1 : 0) + (y ? 1 : 0) + (z ? 1 : 0);
    }

    private static int oppositeExposurePairCount(List<VoxelFace> exposedFaces) {
        int pairs = 0;
        if (exposedFaces.contains(VoxelFace.WEST) && exposedFaces.contains(VoxelFace.EAST)) pairs++;
        if (exposedFaces.contains(VoxelFace.DOWN) && exposedFaces.contains(VoxelFace.UP)) pairs++;
        if (exposedFaces.contains(VoxelFace.NORTH) && exposedFaces.contains(VoxelFace.SOUTH)) pairs++;
        return pairs;
    }

    private static int[] exposureVector(List<VoxelFace> exposedFaces) {
        int x = 0;
        int y = 0;
        int z = 0;
        for (VoxelFace face : exposedFaces) {
            switch (face) {
                case DOWN -> y--;
                case UP -> y++;
                case NORTH -> z--;
                case SOUTH -> z++;
                case WEST -> x--;
                case EAST -> x++;
            }
        }
        return new int[] { x, y, z };
    }

    private static int surfaceNeighborCount(Map<GridPoint, SurfaceCell> cellsByPosition, GridPoint position) {
        int count = 0;
        for (VoxelFace face : VoxelFace.values()) {
            var neighbor = face.neighborOf(position);
            if (neighbor.isEmpty()) continue;
            SurfaceCell neighborCell = cellsByPosition.get(neighbor.get());
            if (neighborCell != null && neighborCell.isSurface()) count++;
        }
        return count;
    }
}
