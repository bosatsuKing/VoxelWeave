# Selection domain (Step 2)

`domain` uses immutable integer `GridPoint` coordinates (`long`) and non-empty
`Region` bounds: `[minInclusive, maxExclusive)` on all axes. `Region.between`
normalizes half-open boundary corners; equal boundaries and empty intersections
return `Optional.empty()`. It never converts block-inclusive endpoints.

`Selection` contains complete `SelectionBox` snapshots. `PlacementTarget` contains
enabled subregions in world space. `OperationTarget` retains both and computes
their pairwise intersections. Lists are immutable, sorted lexicographically and
exact duplicates are removed. Gaps in either selection or placement remain gaps.
Partial overlaps remain a union: future editing must deduplicate visited blocks.
Empty selection, missing/disabled placement, or disjoint bounds grant no regions.
No placement names, identifiers, paths or live integration objects are retained.

The client adapter reads the selected placement and current area selection on
demand during the existing diagnostic key action. It includes all complete boxes
in the current selection, not only the currently highlighted box. Incomplete boxes
are ignored, matching Step 1. Litematica 0.27.12 `getSubRegionBoxes(PLACEMENT_ENABLED)`
provides inclusive world-space bounds with placement and subregion rotation/mirror
applied. `WorldBoundsMapping` normalizes endpoints and widens before adding one.
It can represent a block at `Integer.MAX_VALUE` without overflow.

Before upstream integer transforms, a conservative magnitude bound checks that
origin + transformed relative position + transformed extent cannot overflow on
any axis. Zero sizes, missing sizes and unsafe magnitudes fail closed through the
existing readable integration-error diagnostic. This may reject extreme placements
whose signed arithmetic would actually fit; it never accepts wrapped bounds.

The optional dependency/version loader and its error boundary remain in place.
Domain and boundary-helper tests have no Minecraft, Litematica or Fabric imports.
The client adapter is compiled in both build modes; actual rotated/mirrored
placement behavior still needs a dev-client smoke test.

These are geometry snapshots, not write authorization handles: they become stale
when selection or placement changes. A future edit must capture/revalidate its
target and define unique block iteration before writing. Container coordinates,
replacement, preview/commit, history and export are outside Step 2.
