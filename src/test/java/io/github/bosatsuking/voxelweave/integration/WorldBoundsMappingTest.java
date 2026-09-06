package io.github.bosatsuking.voxelweave.integration;

import io.github.bosatsuking.voxelweave.domain.GridPoint;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldBoundsMappingTest {
    @Test void inclusiveSingleBlockAndReversedCorners() {
        var block = WorldBoundsMapping.fromInclusive(-1, 2, 3, -1, 2, 3);
        assertEquals(new GridPoint(-1, 2, 3), block.minInclusive());
        assertEquals(new GridPoint(0, 3, 4), block.maxExclusive());
        assertEquals(WorldBoundsMapping.fromInclusive(-2, 8, 3, 5, -1, 9),
                WorldBoundsMapping.fromInclusive(5, -1, 9, -2, 8, 3));
    }

    @Test void intMaxInclusiveBoundaryWidensBeforeAddition() {
        var bounds = WorldBoundsMapping.fromInclusive(Integer.MIN_VALUE, 0, Integer.MAX_VALUE,
                Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        assertEquals(new GridPoint(Integer.MIN_VALUE, 0, Integer.MAX_VALUE), bounds.minInclusive());
        assertEquals(new GridPoint(2147483648L, 1, 2147483648L), bounds.maxExclusive());
    }

    @Test void safeTransformAllowsNegativeSizesAndBoundary() {
        assertDoesNotThrow(() -> WorldBoundsMapping.requireSafeTransform(-10, 2, 3, 4, -5, 6, -7, 8, -9));
        assertDoesNotThrow(() -> WorldBoundsMapping.requireSafeTransform(Integer.MAX_VALUE, 0, 0, 0, 0, 0, 1, 1, 1));
    }

    @Test void unsafeTransformsFailBeforeUpstreamIntArithmetic() {
        assertThrows(IllegalArgumentException.class,
                () -> WorldBoundsMapping.requireSafeTransform(Integer.MAX_VALUE, 0, 0, 1, 0, 0, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> WorldBoundsMapping.requireSafeTransform(0, 0, 0, Integer.MIN_VALUE, 0, 0, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> WorldBoundsMapping.requireSafeTransform(1, 0, 0, 0, 0, 0, Integer.MIN_VALUE, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> WorldBoundsMapping.requireSafeTransform(0, 0, 0, 0, 0, 0, 0, 1, 1));
    }
}
