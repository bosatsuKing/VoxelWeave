package io.github.bosatsuking.voxelweave.domain;

/** Integer world-space coordinate. Long also represents an exclusive int-max + 1 boundary. */
public record GridPoint(long x, long y, long z) implements Comparable<GridPoint> {
    @Override
    public int compareTo(GridPoint other) {
        int result = Long.compare(x, other.x);
        if (result == 0) result = Long.compare(y, other.y);
        if (result == 0) result = Long.compare(z, other.z);
        return result;
    }
}
