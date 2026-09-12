package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.GridPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Observed terminal geometry, with path and attachment roles kept separate.
 * Length is a cell count, not arbitrary protrusion depth or neck width.
 */
public record ProtrusionEvidence(
        SurfaceComponent component,
        List<SurfaceFeatureDescriptor> path,
        Optional<SurfaceFeatureDescriptor> attachment,
        List<VoxelFace> attachmentSupportFaces,
        ProtrusionTermination termination,
        List<ProtrusionContextIssue> contextIssues) {

    public ProtrusionEvidence {
        Objects.requireNonNull(component);
        path = List.copyOf(path);
        if (path.isEmpty()) throw new IllegalArgumentException("A trace must include its seed");
        Objects.requireNonNull(attachment);
        attachmentSupportFaces = List.copyOf(attachmentSupportFaces);
        Objects.requireNonNull(termination);
        contextIssues = List.copyOf(contextIssues);
    }

    public GridPoint tipPosition() {
        return path.getFirst().position();
    }

    /** Ordered observed target coordinates; the attachment is excluded. */
    public List<GridPoint> pathPositions() {
        return path.stream().map(SurfaceFeatureDescriptor::position).toList();
    }

    public int observedPathLength() {
        return path.size();
    }

    /** Unavailable for partial or uncertain traces, even if a local junction was observed. */
    public OptionalInt exactPathLength() {
        return component.complete() && contextIssues.isEmpty()
                && (termination == ProtrusionTermination.JUNCTION_REACHED
                    || termination == ProtrusionTermination.OTHER_TIP_REACHED)
                ? OptionalInt.of(path.size()) : OptionalInt.empty();
    }

    /** Face from the final path cell toward the attachment, if observed. */
    public Optional<VoxelFace> attachmentDirection() {
        return attachment.map(value -> direction(path.getLast().position(), value.position()));
    }

    /** Observed steps within the path, followed by the attachment step when present. */
    public List<VoxelFace> stepDirections() {
        ArrayList<VoxelFace> steps = new ArrayList<>();
        for (int i = 1; i < path.size(); i++) {
            steps.add(direction(path.get(i - 1).position(), path.get(i).position()));
        }
        attachmentDirection().ifPresent(steps::add);
        return List.copyOf(steps);
    }

    /** Direction changes in the observed steps; zero does not establish an unobserved continuation. */
    public int directionChangeCount() {
        List<VoxelFace> steps = stepDirections();
        int changes = 0;
        for (int i = 1; i < steps.size(); i++) {
            if (steps.get(i) != steps.get(i - 1)) changes++;
        }
        return changes;
    }

    private static VoxelFace direction(GridPoint from, GridPoint to) {
        for (VoxelFace face : VoxelFace.values()) {
            if (face.neighborOf(from).filter(to::equals).isPresent()) return face;
        }
        throw new IllegalArgumentException("Trace steps must be face connected");
    }
}
