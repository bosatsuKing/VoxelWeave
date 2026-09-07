package io.github.bosatsuking.voxelweave.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable world-space schematic snapshot.
 *
 * <p>Captured integrations store every visited coordinate, including air. A missing coordinate therefore
 * means the position was outside the captured data, not that the schematic contains air there.</p>
 */
public record SchematicSnapshot(Map<GridPoint, BlockStateRef> blocks) {
    public SchematicSnapshot {
        Objects.requireNonNull(blocks);
        blocks = Map.copyOf(blocks);
    }

    public BlockStateRef blockAt(GridPoint position) {
        return blocks.get(position);
    }
}
