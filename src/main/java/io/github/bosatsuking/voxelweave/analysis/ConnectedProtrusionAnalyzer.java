package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Pure TIP-origin, bounded six-neighbor tracing. Feature kinds do not control traversal. */
public final class ConnectedProtrusionAnalyzer {
    private ConnectedProtrusionAnalyzer() { }

    public static ConnectedProtrusionAnalysis analyze(
            SchematicSnapshot snapshot, SurfaceAnalysis surface,
            SurfaceFeatureAnalysis features, ProtrusionAnalysisRequest request) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(surface);
        Objects.requireNonNull(features);
        Objects.requireNonNull(request);
        // Validate before handling empty inputs; structural equality is not provenance.
        if (!surface.isFrom(snapshot)) {
            throw new IllegalArgumentException("Surface analysis must belong to the current snapshot");
        }
        if (!features.isFrom(surface)) {
            throw new IllegalArgumentException("Feature analysis must belong to the supplied surface analysis");
        }

        Map<GridPoint, SurfaceCell> cells = new HashMap<>();
        for (SurfaceCell cell : surface.cells()) cells.put(cell.position(), cell);
        Map<GridPoint, SurfaceFeatureDescriptor> descriptors = new HashMap<>();
        for (SurfaceFeatureDescriptor descriptor : features.descriptors()) {
            descriptors.put(descriptor.position(), descriptor);
        }
        Map<GridPoint, SurfaceComponent> components = new HashMap<>();
        for (SurfaceComponent component : surface.components()) components.put(component.root(), component);

        List<ProtrusionEvidence> evidence = new ArrayList<>();
        // SurfaceAnalysis already supplies coordinate order. No new component walk or path-pair search.
        for (SurfaceCell seed : surface.cells()) {
            if (seed.occupiedNeighborCount() != 1 || seed.hasUnknownBoundary()) continue;
            ProtrusionEvidence trace = trace(seed, cells, descriptors,
                    components.get(seed.componentRoot()), request.maxTraceLength());
            if (trace.termination() == ProtrusionTermination.OTHER_TIP_REACHED
                    && trace.path().getLast().position().compareTo(seed.position()) < 0) continue;
            evidence.add(trace);
        }
        return new ConnectedProtrusionAnalysis(snapshot, surface, features, request, evidence);
    }

    private static ProtrusionEvidence trace(
            SurfaceCell seed, Map<GridPoint, SurfaceCell> cells,
            Map<GridPoint, SurfaceFeatureDescriptor> descriptors,
            SurfaceComponent component, int limit) {
        List<SurfaceFeatureDescriptor> path = new ArrayList<>();
        EnumSet<ProtrusionContextIssue> issues = EnumSet.noneOf(ProtrusionContextIssue.class);
        if (!component.complete()) issues.add(ProtrusionContextIssue.INCOMPLETE_COMPONENT);
        GridPoint previous = null;
        SurfaceCell current = seed;

        for (int visited = 1; ; visited++) {
            SurfaceFeatureDescriptor descriptor = Objects.requireNonNull(descriptors.get(current.position()));
            List<VoxelFace> occupiedFaces = new ArrayList<>();
            GridPoint next = null;
            boolean overflow = false;
            boolean missing = false;
            for (VoxelFace face : VoxelFace.values()) {
                Optional<GridPoint> neighbor = face.neighborOf(current.position());
                if (neighbor.isEmpty()) {
                    overflow = true;
                    issues.add(ProtrusionContextIssue.COORDINATE_OVERFLOW);
                } else if (current.unknownFaces().contains(face)) {
                    issues.add(ProtrusionContextIssue.UNKNOWN_NEIGHBOR);
                } else if (!current.exposedFaces().contains(face)) {
                    occupiedFaces.add(face);
                    if (!cells.containsKey(neighbor.get())) {
                        missing = true;
                        issues.add(ProtrusionContextIssue.OCCUPIED_NEIGHBOR_OUTSIDE_TARGET);
                    }
                    if (!neighbor.get().equals(previous)) next = neighbor.get();
                }
            }
            // Overflow takes precedence over unknown topology (which also records overflow upstream).
            if (overflow || current.hasUnknownBoundary()) {
                path.add(descriptor);
                return result(component, path, Optional.empty(), List.of(),
                        overflow ? ProtrusionTermination.COORDINATE_OVERFLOW
                                : ProtrusionTermination.INCOMPLETE_CONTEXT, issues);
            }
            if (current.occupiedNeighborCount() >= 3) {
                GridPoint incoming = previous;
                occupiedFaces.removeIf(face -> face.neighborOf(descriptor.position())
                        .filter(point -> point.equals(incoming)).isPresent());
                return result(component, path, Optional.of(descriptor), occupiedFaces,
                        ProtrusionTermination.JUNCTION_REACHED, issues);
            }
            path.add(descriptor);
            if (missing) {
                return result(component, path, Optional.empty(), List.of(),
                        ProtrusionTermination.INCOMPLETE_CONTEXT, issues);
            }
            if (previous != null && current.occupiedNeighborCount() == 1) {
                return result(component, path, Optional.empty(), List.of(),
                        ProtrusionTermination.OTHER_TIP_REACHED, issues);
            }
            // Natural termination at exactly the budget succeeds; continuation requires another visit.
            if (visited == limit) {
                return result(component, path, Optional.empty(), List.of(),
                        ProtrusionTermination.TRACE_LIMIT_REACHED, issues);
            }
            previous = current.position();
            current = Objects.requireNonNull(cells.get(next));
        }
    }

    private static ProtrusionEvidence result(
            SurfaceComponent component, List<SurfaceFeatureDescriptor> path,
            Optional<SurfaceFeatureDescriptor> attachment, List<VoxelFace> support,
            ProtrusionTermination termination, EnumSet<ProtrusionContextIssue> issues) {
        // A remote incomplete component invalidates completeness without hiding visited overflow.
        if (!issues.isEmpty() && termination != ProtrusionTermination.COORDINATE_OVERFLOW) {
            termination = ProtrusionTermination.INCOMPLETE_CONTEXT;
        }
        return new ProtrusionEvidence(component, path, attachment, support, termination, List.copyOf(issues));
    }
}
