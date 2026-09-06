package io.github.bosatsuking.voxelweave.domain;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class SelectionDomainTest {
    private static Region cube(long min, long max) {
        return new Region(new GridPoint(min, min, min), new GridPoint(max, max, max));
    }

    @Test void normalRegionUsesExclusiveMaximum() {
        Region region = cube(1, 5);
        assertTrue(region.contains(new GridPoint(1, 2, 4)));
        assertFalse(region.contains(new GridPoint(5, 2, 4)));
        assertFalse(region.contains(new GridPoint(2, 5, 4)));
        assertFalse(region.contains(new GridPoint(2, 4, 5)));
    }

    @Test void reversedBoundaryEndpointsNormalizePerAxis() {
        assertEquals(Optional.of(new Region(new GridPoint(-2, 1, -4), new GridPoint(8, 9, 3))),
                Region.between(new GridPoint(8, 1, 3), new GridPoint(-2, 9, -4)));
    }

    @Test void singleBlock() {
        assertEquals(Optional.of(cube(2, 3)), cube(2, 3).intersection(cube(0, 10)));
    }

    @Test void partialIntersection() {
        assertEquals(Optional.of(cube(3, 5)), cube(0, 5).intersection(cube(3, 8)));
    }

    @Test void fullContainmentIsSymmetric() {
        assertEquals(Optional.of(cube(2, 4)), cube(0, 8).intersection(cube(2, 4)));
        assertEquals(cube(0, 8).intersection(cube(2, 4)), cube(2, 4).intersection(cube(0, 8)));
    }

    @Test void noIntersection() {
        assertTrue(cube(0, 2).intersection(cube(3, 5)).isEmpty());
    }

    @Test void touchingFaceEdgeAndCornerHaveNoBlocks() {
        Region region = cube(0, 2);
        assertTrue(region.intersection(new Region(new GridPoint(2, 0, 0), new GridPoint(3, 2, 2))).isEmpty());
        assertTrue(region.intersection(new Region(new GridPoint(2, 2, 0), new GridPoint(3, 3, 2))).isEmpty());
        assertTrue(region.intersection(cube(2, 3)).isEmpty());
    }

    @Test void negativeCoordinates() {
        assertEquals(Optional.of(cube(-5, -2)), cube(-10, -2).intersection(cube(-5, 1)));
    }

    @Test void multipleSelectionsAndPlacementGapsArePreserved() {
        Selection selection = new Selection(List.of(new SelectionBox(cube(0, 3)), new SelectionBox(cube(8, 12))));
        OperationTarget target = new OperationTarget(selection, Optional.of(new PlacementTarget(List.of(cube(1, 10)))));
        assertEquals(List.of(cube(1, 3), cube(8, 10)), target.worldRegions());
        assertFalse(target.worldRegions().stream().anyMatch(r -> r.contains(new GridPoint(5, 5, 5))));
        OperationTarget splitPlacement = new OperationTarget(new Selection(List.of(new SelectionBox(cube(0, 20)))),
                Optional.of(new PlacementTarget(List.of(cube(1, 3), cube(8, 10)))));
        assertEquals(target.worldRegions(), splitPlacement.worldRegions());
    }

    @Test void emptySelectionAndMissingPlacementGrantNothing() {
        assertTrue(new OperationTarget(new Selection(List.of()), Optional.of(new PlacementTarget(List.of(cube(0, 5)))))
                .worldRegions().isEmpty());
        assertTrue(new OperationTarget(new Selection(List.of(new SelectionBox(cube(0, 5)))), Optional.empty())
                .worldRegions().isEmpty());
        assertTrue(OperationTarget.empty().worldRegions().isEmpty());
    }

    @Test void emptyPlacementAndDisjointSelectionGrantNothing() {
        Selection selection = new Selection(List.of(new SelectionBox(cube(0, 1))));
        assertTrue(new OperationTarget(selection, Optional.of(new PlacementTarget(List.of()))).worldRegions().isEmpty());
        assertTrue(new OperationTarget(selection, Optional.of(new PlacementTarget(List.of(cube(2, 3)))))
                .worldRegions().isEmpty());
    }

    @Test void snapshotsAreImmutableAndDeterministic() {
        var input = new ArrayList<>(List.of(cube(8, 10), cube(1, 3), cube(1, 3)));
        PlacementTarget placement = new PlacementTarget(input);
        input.clear();
        assertEquals(List.of(cube(1, 3), cube(8, 10)), placement.worldRegions());
        assertThrows(UnsupportedOperationException.class, () -> placement.worldRegions().clear());
        var boxes = new ArrayList<>(List.of(new SelectionBox(cube(8, 10)), new SelectionBox(cube(1, 3))));
        Selection selection = new Selection(boxes);
        boxes.clear();
        assertEquals(new Selection(List.of(new SelectionBox(cube(1, 3)), new SelectionBox(cube(8, 10)))), selection);
        assertThrows(UnsupportedOperationException.class, () -> selection.boxes().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> new OperationTarget(selection, Optional.of(placement)).worldRegions().clear());
    }

    @Test void overlappingBoxesRemainAUnionWithoutFillingOutside() {
        OperationTarget target = new OperationTarget(new Selection(List.of(new SelectionBox(cube(0, 4)),
                new SelectionBox(cube(2, 6)))), Optional.of(new PlacementTarget(List.of(cube(0, 10)))));
        for (int x = -1; x <= 7; x++) for (int y = -1; y <= 7; y++) for (int z = -1; z <= 7; z++) {
            GridPoint point = new GridPoint(x, y, z);
            assertEquals(cube(0, 4).contains(point) || cube(2, 6).contains(point),
                    target.worldRegions().stream().anyMatch(region -> region.contains(point)));
        }
    }

    @Test void extremeBoundariesRequireNoOverflowingArithmetic() {
        Region all = cube(Long.MIN_VALUE, Long.MAX_VALUE);
        assertEquals(Optional.of(cube(-1, 1)), all.intersection(cube(-1, 1)));
        assertTrue(all.contains(new GridPoint(Long.MIN_VALUE, 0, 0)));
    }

    @Test void invalidAndZeroExtentBoundsCannotBecomeRegions() {
        assertTrue(Region.between(new GridPoint(0, 0, 0), new GridPoint(0, 2, 3)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> cube(2, 2));
        assertThrows(IllegalArgumentException.class, () -> cube(3, 2));
        assertThrows(NullPointerException.class, () -> new SelectionBox(null));
    }
}
