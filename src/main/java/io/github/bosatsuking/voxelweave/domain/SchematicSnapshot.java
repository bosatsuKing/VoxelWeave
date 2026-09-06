package io.github.bosatsuking.voxelweave.domain;

import java.util.Map;
import java.util.Objects;

/** Immutable sparse world-space snapshot. Missing coordinates are outside the captured schematic data. */
public record SchematicSnapshot(Map<GridPoint, BlockStateRef> blocks) {
    public SchematicSnapshot {
        Objects.requireNonNull(blocks);
        blocks = Map.copyOf(blocks);
    }

    public BlockStateRef blockAt(GridPoint position) {
        return blocks.get(position);
    }
}
