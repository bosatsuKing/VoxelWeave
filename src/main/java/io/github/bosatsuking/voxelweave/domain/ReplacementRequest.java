package io.github.bosatsuking.voxelweave.domain;

import java.util.Objects;

public record ReplacementRequest(BlockStateRef source, BlockStateRef target) {
    public ReplacementRequest {
        Objects.requireNonNull(source);
        Objects.requireNonNull(target);
    }

    public boolean isNoOp() {
        return source.equals(target);
    }
}
