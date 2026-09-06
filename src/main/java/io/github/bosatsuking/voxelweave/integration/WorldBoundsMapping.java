package io.github.bosatsuking.voxelweave.integration;

import io.github.bosatsuking.voxelweave.domain.GridPoint;
import io.github.bosatsuking.voxelweave.domain.Region;

/** Pure adapter boundary helpers; inclusive coordinates never enter the domain API. */
public final class WorldBoundsMapping {
    private WorldBoundsMapping() { }

    public static Region fromInclusive(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new Region(new GridPoint(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)),
                new GridPoint((long) Math.max(x1, x2) + 1, (long) Math.max(y1, y2) + 1,
                        (long) Math.max(z1, z2) + 1));
    }

    /** Conservative proof that any axis permutation/sign flip and both additions fit int.
     * Rejects some extreme but representable placements instead of accepting wrapped bounds.
     */
    public static void requireSafeTransform(int ox, int oy, int oz, int px, int py, int pz,
                                            int sx, int sy, int sz) {
        if (sx == 0 || sy == 0 || sz == 0) {
            throw new IllegalArgumentException("Placement subregion has zero size");
        }
        long origin = maxMagnitude(ox, oy, oz);
        long offset = maxMagnitude(px, py, pz);
        long extent = maxMagnitude(sx, sy, sz) - 1;
        if (origin + offset + extent > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Placement transform exceeds safe integer bounds");
        }
    }

    private static long maxMagnitude(int x, int y, int z) {
        return Math.max(Math.abs((long) x), Math.max(Math.abs((long) y), Math.abs((long) z)));
    }
}
