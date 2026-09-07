package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.BlockStateRef;
import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import io.github.bosatsuking.voxelweave.domain.Region;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** Pure bounded local-topology analysis over immutable schematic data. */
public final class SurfaceAnalyzer {
    private SurfaceAnalyzer() { }

    public static SurfaceAnalysis analyze(
            SchematicSnapshot snapshot,
            OperationTarget target,
            BlockOccupancyPolicy occupancy) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(target);
        Objects.requireNonNull(occupancy);

        List<Region> regions = target.worldRegions();
        if (regions.isEmpty()) return SurfaceAnalysis.empty();

        TreeMap<GridPoint, BlockStateRef> occupiedInsideTarget = new TreeMap<>();
        for (Map.Entry<GridPoint, BlockStateRef> entry : snapshot.blocks().entrySet()) {
            if (insideTarget(regions, entry.getKey()) && occupancy.isOccupied(entry.getValue())) {
                occupiedInsideTarget.put(entry.getKey(), entry.getValue());
            }
        }
        if (occupiedInsideTarget.isEmpty()) return SurfaceAnalysis.empty();

        ComponentResult componentResult = buildComponents(
                snapshot, regions, occupiedInsideTarget, occupancy);
        List<SurfaceCell> cells = new ArrayList<>(occupiedInsideTarget.size());

        for (Map.Entry<GridPoint, BlockStateRef> entry : occupiedInsideTarget.entrySet()) {
            GridPoint position = entry.getKey();
            List<VoxelFace> exposedFaces = new ArrayList<>();
            List<VoxelFace> unknownFaces = new ArrayList<>();
            int occupiedNeighborCount = 0;

            for (VoxelFace face : VoxelFace.values()) {
                Optional<GridPoint> neighbor = face.neighborOf(position);
                if (neighbor.isEmpty()) {
                    unknownFaces.add(face);
                    continue;
                }

                BlockStateRef neighborState = snapshot.blockAt(neighbor.get());
                if (neighborState == null) {
                    unknownFaces.add(face);
                } else if (occupancy.isOccupied(neighborState)) {
                    occupiedNeighborCount++;
                } else {
                    exposedFaces.add(face);
                }
            }

            cells.add(new SurfaceCell(
                    position,
                    entry.getValue(),
                    exposedFaces,
                    unknownFaces,
                    occupiedNeighborCount,
                    componentResult.rootByPosition().get(position)));
        }

        return new SurfaceAnalysis(cells, componentResult.components());
    }

    private static ComponentResult buildComponents(
            SchematicSnapshot snapshot,
            List<Region> regions,
            TreeMap<GridPoint, BlockStateRef> occupiedInsideTarget,
            BlockOccupancyPolicy occupancy) {
        Set<GridPoint> visited = new HashSet<>();
        Map<GridPoint, GridPoint> rootByPosition = new HashMap<>();
        List<SurfaceComponent> components = new ArrayList<>();

        for (GridPoint root : occupiedInsideTarget.keySet()) {
            if (!visited.add(root)) continue;

            ArrayDeque<GridPoint> queue = new ArrayDeque<>();
            List<GridPoint> members = new ArrayList<>();
            boolean complete = true;
            queue.add(root);

            while (!queue.isEmpty()) {
                GridPoint current = queue.removeFirst();
                members.add(current);

                for (VoxelFace face : VoxelFace.values()) {
                    Optional<GridPoint> neighbor = face.neighborOf(current);
                    if (neighbor.isEmpty()) {
                        complete = false;
                        continue;
                    }

                    GridPoint neighborPoint = neighbor.get();
                    BlockStateRef neighborState = snapshot.blockAt(neighborPoint);
                    if (neighborState == null) {
                        complete = false;
                        continue;
                    }
                    if (!occupancy.isOccupied(neighborState)) continue;

                    if (!insideTarget(regions, neighborPoint)) {
                        complete = false;
                        continue;
                    }
                    if (occupiedInsideTarget.containsKey(neighborPoint) && visited.add(neighborPoint)) {
                        queue.addLast(neighborPoint);
                    }
                }
            }

            for (GridPoint member : members) rootByPosition.put(member, root);
            components.add(new SurfaceComponent(root, members.size(), complete));
        }

        return new ComponentResult(Map.copyOf(rootByPosition), List.copyOf(components));
    }

    private static boolean insideTarget(List<Region> regions, GridPoint point) {
        for (Region region : regions) {
            if (region.contains(point)) return true;
        }
        return false;
    }

    private record ComponentResult(
            Map<GridPoint, GridPoint> rootByPosition,
            List<SurfaceComponent> components) { }
}
