package io.github.bosatsuking.voxelweave.integration.litematica;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import io.github.bosatsuking.voxelweave.domain.OperationTarget;
import io.github.bosatsuking.voxelweave.domain.PlacementTarget;
import io.github.bosatsuking.voxelweave.domain.Region;
import io.github.bosatsuking.voxelweave.domain.Selection;
import io.github.bosatsuking.voxelweave.domain.SelectionBox;
import io.github.bosatsuking.voxelweave.integration.WorldBoundsMapping;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

/** Read on the client thread, on demand; never retains live integration objects. */
final class LitematicaTargetMapping {
    private LitematicaTargetMapping() { }

    static OperationTarget snapshot(SchematicPlacement placement, AreaSelection selection) {
        Selection selected = new Selection(selection == null ? List.of()
                : PositionUtils.getValidBoxes(selection).stream()
                        .map(LitematicaTargetMapping::bounds).map(SelectionBox::new).toList());
        if (placement == null || !placement.isEnabled()) {
            return new OperationTarget(selected, Optional.empty());
        }

        // Validate before invoking upstream int arithmetic (including disabled regions' metadata,
        // so the upstream helper cannot emit names for missing sizes).
        BlockPos origin = placement.getOrigin();
        for (SubRegionPlacement subregion : placement.getAllSubRegionsPlacements()) {
            BlockPos size = placement.getSchematic().getAreaSize(subregion.getName());
            if (size == null) throw new IllegalArgumentException("Placement subregion size is unavailable");
            if (!subregion.isEnabled()) continue;
            BlockPos pos = subregion.getPos();
            WorldBoundsMapping.requireSafeTransform(origin.getX(), origin.getY(), origin.getZ(),
                    pos.getX(), pos.getY(), pos.getZ(), size.getX(), size.getY(), size.getZ());
        }

        // Litematica 0.27.12 supplies inclusive world boxes with placement AND subregion
        // rotation/mirror applied. Do not substitute an enclosing box or origin subtraction.
        List<Region> regions = placement.getSubRegionBoxes(
                        SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED).values().stream()
                .map(LitematicaTargetMapping::bounds).toList();
        return new OperationTarget(selected, Optional.of(new PlacementTarget(regions)));
    }

    private static Region bounds(Box box) {
        BlockPos first = box.getPos1();
        BlockPos second = box.getPos2();
        if (first == null || second == null) {
            throw new IllegalArgumentException("World box requires both corners");
        }
        return WorldBoundsMapping.fromInclusive(first.getX(), first.getY(), first.getZ(),
                second.getX(), second.getY(), second.getZ());
    }
}
