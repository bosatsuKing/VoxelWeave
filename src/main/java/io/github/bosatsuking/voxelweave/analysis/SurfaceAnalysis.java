package io.github.bosatsuking.voxelweave.analysis;

import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.SchematicSnapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable deterministic result of one bounded surface-analysis pass. */
public final class SurfaceAnalysis {
    private final List<SurfaceCell> cells;
    private final List<SurfaceComponent> components;
    private final SchematicSnapshot sourceSnapshot;

    /** Unbound evidence for inspection/classification; not eligible for cleanup planning. */
    public SurfaceAnalysis(List<SurfaceCell> cells, List<SurfaceComponent> components) {
        this(cells, components, null);
    }

    private SurfaceAnalysis(List<SurfaceCell> cells, List<SurfaceComponent> components,
                            SchematicSnapshot sourceSnapshot) {
        Objects.requireNonNull(cells);
        Objects.requireNonNull(components);

        cells = cells.stream().sorted().toList();
        components = components.stream().sorted().toList();

        Set<GridPoint> positions = new HashSet<>();
        for (SurfaceCell cell : cells) {
            if (!positions.add(cell.position())) {
                throw new IllegalArgumentException("Duplicate surface cell position: " + cell.position());
            }
        }

        Set<GridPoint> componentRoots = new HashSet<>();
        for (SurfaceComponent component : components) {
            if (!componentRoots.add(component.root())) {
                throw new IllegalArgumentException("Duplicate surface component root: " + component.root());
            }
        }
        for (SurfaceCell cell : cells) {
            if (!componentRoots.contains(cell.componentRoot())) {
                throw new IllegalArgumentException("Surface cell references missing component: " + cell.componentRoot());
            }
        }
        this.cells = cells;
        this.components = components;
        this.sourceSnapshot = sourceSnapshot;
    }

    static SurfaceAnalysis fromSnapshot(SchematicSnapshot snapshot, List<SurfaceCell> cells,
                                        List<SurfaceComponent> components) {
        return new SurfaceAnalysis(cells, components, Objects.requireNonNull(snapshot));
    }

    /** Identity check: immutable snapshots cannot change beneath their analysis, including halo data. */
    public boolean isFrom(SchematicSnapshot snapshot) {
        return sourceSnapshot != null && sourceSnapshot == snapshot;
    }

    public List<SurfaceCell> cells() {
        return cells;
    }

    public List<SurfaceComponent> components() {
        return components;
    }

    /** Evidence equality does not grant source compatibility; use isFrom for that. */
    @Override
    public boolean equals(Object other) {
        return other instanceof SurfaceAnalysis analysis
                && cells.equals(analysis.cells) && components.equals(analysis.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cells, components);
    }

    public static SurfaceAnalysis empty() {
        return new SurfaceAnalysis(List.of(), List.of());
    }

    public Optional<SurfaceCell> cellAt(GridPoint position) {
        Objects.requireNonNull(position);
        int low = 0;
        int high = cells.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            SurfaceCell cell = cells.get(mid);
            int comparison = cell.position().compareTo(position);
            if (comparison < 0) low = mid + 1;
            else if (comparison > 0) high = mid - 1;
            else return Optional.of(cell);
        }
        return Optional.empty();
    }

    public Optional<SurfaceComponent> componentAt(GridPoint root) {
        Objects.requireNonNull(root);
        int low = 0;
        int high = components.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            SurfaceComponent component = components.get(mid);
            int comparison = component.root().compareTo(root);
            if (comparison < 0) low = mid + 1;
            else if (comparison > 0) high = mid - 1;
            else return Optional.of(component);
        }
        return Optional.empty();
    }

    public List<SurfaceComponent> smallIslandCandidates(int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be positive");
        return components.stream().filter(component -> component.isSmallIslandCandidate(maxSize)).toList();
    }
}
