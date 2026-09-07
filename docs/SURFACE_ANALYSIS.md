# Surface analysis domain (Step 6A)

VoxelWeave surface analysis is a pure, read-only layer over `SchematicSnapshot` and `OperationTarget`.
It does not smooth, delete, fill, recolor or otherwise mutate schematic data. Its job is to describe
local voxel topology once so later refinement tools can share the same structural evidence.

## Why this layer exists

Converted 3D models often contain several different kinds of geometry at the same time:

- broad intentional surfaces;
- ridges and thin structural details;
- isolated conversion artifacts;
- disconnected islands;
- stair-step noise;
- selection/capture boundaries where the neighboring schematic state is not known.

Applying one generic smoothing rule to all of those structures would create a recognizable procedural
style and can erase creator intent. Step 6A therefore exposes conservative topology facts rather than
performing a shape rewrite.

## Occupancy is a policy

`SurfaceAnalyzer` does not hard-code Minecraft air names or parse Minecraft block-state strings.
Callers provide a pure `BlockOccupancyPolicy`:

```text
BlockStateRef -> occupied / empty
```

This keeps Minecraft/Litematica representation details at the integration boundary and keeps the
analysis layer independently testable.

## Six-neighbor topology

Each occupied analyzed voxel becomes a `SurfaceCell`. The six cardinal faces are evaluated in stable
order:

```text
DOWN, UP, NORTH, SOUTH, WEST, EAST
```

For each face the neighbor is one of three states:

- **occupied** — captured neighbor state exists and the occupancy policy says it contains geometry;
- **exposed** — captured neighbor state exists and the occupancy policy says it is empty;
- **unknown** — the snapshot has no data for that neighbor, or coordinate arithmetic cannot be represented.

Unknown is intentionally different from exposed. Missing snapshot data must never be silently treated
as air, because a block at the edge of a selection may continue into geometry that was not captured.

`SurfaceCell` therefore exposes:

- deterministic exposed-face list;
- deterministic unknown-face list;
- occupied-neighbor count;
- component root;
- conservative predicates for interior, known surface, isolated and weakly-supported cells.

An isolated or weakly-supported classification is only returned when all six neighboring states are
known. Unknown-boundary cells are not automatically treated as cleanup candidates.

## Connected components

Occupied cells inside the operation target are grouped using 6-connectivity. Each component has:

- a deterministic root: the lexicographically smallest occupied coordinate reached for that component;
- size inside the operation target;
- a `complete` flag.

A component is incomplete when its connectivity touches unknown snapshot data or known occupied data
outside the operation target. Only complete components are eligible for `smallIslandCandidates(...)`.
This prevents a selection boundary from making a large structure look like a tiny disconnected island.

## Bounding and overlap semantics

Analysis only emits cells whose coordinates are inside `OperationTarget.worldRegions()`.
The input snapshot is a map keyed by world-space coordinate, so partially overlapping target regions do
not duplicate analyzed cells. Output cells and components are sorted deterministically.

The analyzer may use captured neighbor data outside the target when that data is available, but it never
emits or mutates those outside-target coordinates. This allows a future capture halo to improve boundary
confidence without changing the analysis API.

## Performance

Surface analysis is an explicit operation, not a render-tick task. It scans captured snapshot entries once
to collect occupied target cells, then performs bounded six-neighbor component/topology passes.
Future expensive descriptors should be cached by snapshot/workspace revision rather than recomputed per frame.

## Step 6A scope boundary

Implemented here:

- local six-face topology;
- exposed/interior/unknown distinction;
- conservative isolated/weak-support candidates;
- deterministic 6-connected component sizes and completeness.

Not implemented here:

- curvature or surface-normal estimation;
- edge-strength / feature-preservation scoring;
- spike removal;
- smoothing / relaxation;
- gap filling;
- contour rewriting;
- palette, gradient, pattern or dithering operations.

Those later operations should consume this analysis instead of re-implementing neighbor semantics.
