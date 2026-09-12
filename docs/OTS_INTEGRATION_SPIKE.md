# ObjToSchematic 0.2.0 integration spike

## Status and decision

**Decision: B — conditionally adoptable as a required companion mod.**

A direct read-only boundary from ObjToSchematic (OTS) 0.2.0 into VoxelWeave is technically feasible without pasting to the world or routing through Litematica first. The boundary can preserve VoxelWeave's existing converter-agnostic core by converting OTS state only inside a client integration adapter and returning the existing `SchematicSnapshotCapture` domain type.

This spike does **not** add the adapter, an OTS dependency, write-back, Paste, export, or Step 7B-2.

Before OTS is promoted to a production hard dependency, the proposed boundary must pass a Java 25 / Minecraft 26.1.2 interactive smoke test and an exact-version compile/runtime dependency test. OTS 0.2.0 exposes implementation classes rather than a documented Java integration API, so VoxelWeave should pin the integration to the audited OTS version and fail closed on version mismatch.

## Evidence and limits

The conclusions below distinguish static evidence from runtime behavior.

### Verified statically

The supplied Fabric JAR was inspected read-only:

```text
file: ots-fabric-0.2.0.jar
sha256: d8526aa96b6167a4b952b201c6ef6d5d2531ea63af78c9fa7875604ede52f605
```

Evidence used:

- `fabric.mod.json` metadata and public class signatures;
- integration-facing bytecode for selection, block storage, transforms, revisions, worker completion, apply coordinate mapping, default block-state resolution, and telemetry enablement;
- current VoxelWeave domain/integration contracts;
- official ObjToSchematic / CurseForge distribution pages.

No OTS voxeliser, modifier, rendering algorithm, asset, or UI implementation is copied or proposed for reuse.

### Not runtime-verified in this spike

The supplied OTS JAR requires Java 25 and Minecraft 26.1.2. The available analysis runtime does not provide a Minecraft client session, so the following remain explicit manual-smoke requirements:

- selection changes while VoxelWeave captures;
- moving / rotating / scaling an object through the OTS UI and observing block regeneration/revision behavior;
- negative-coordinate placement in a real world;
- block registry/state conversion against Minecraft 26.1.2;
- OTS + VoxelWeave + Litematica coexistence in the same Fabric instance;
- OTS telemetry opt-out behavior as observed from an actual generated config;
- exact Curse Maven / development-runtime dependency resolution in this repository.

Static facts are not presented as runtime guarantees.

## OTS metadata and dependency facts

The supplied JAR declares:

```text
mod id:        ots
version:       0.2.0
environment:   *
Minecraft:     ~26.1.2
Java:          >=25
Fabric Loader: >=0.19.2
Fabric API:    required
license:       All-Rights-Reserved
```

VoxelWeave is currently client-only and targets Minecraft 26.1.2 / Java 25. A future VoxelWeave dependency would therefore be a client-side requirement from VoxelWeave's point of view, even though OTS itself contains both common/server and client entry points.

CurseForge publishes the 26.1.2 Fabric file separately and exposes a Curse Maven path for project files. The OTS JAR must not be committed to this repository, embedded in VoxelWeave, or redistributed by VoxelWeave. Build/runtime acquisition should use an official distribution path and an exact audited file/version.

The current OTS license page states **All Rights Reserved unless otherwise explicitly stated**. This spike makes no claim that OTS implementation classes are a stable public Java API. Direct integration therefore needs version pinning and compatibility checks rather than assuming source/binary compatibility across OTS updates.

Official references:

- https://www.curseforge.com/minecraft/mc-mods/objtoschematic/files/all
- https://www.curseforge.com/minecraft/mc-mods/objtoschematic/license
- https://objtoschematic.com/wiki

## Selection and lifecycle contract

Static evidence:

- `OtsClientState.selected()` resolves the selected integer ID through the mutable scene-object list.
- `OtsClientState.objects()` returns the backing list, not an immutable snapshot.
- `SceneObject.blocks()` returns the current `BlockMesh` reference.
- `SceneObject.hasValidBlocks()` is true only when a `BlockMesh` exists and its build revision equals the current block revision.
- `SceneObject.isBusy()` reflects an active OTS worker job.
- OTS worker jobs run off-thread, while their result handlers are scheduled back through `Minecraft.execute(...)` before replacing generated scene data.
- `BlockMesh.blocks()` returns its mutable `Long2LongOpenHashMap` directly.

Therefore the production capture boundary must run on the Minecraft client thread and fail closed unless all of these are true at capture start:

1. `OtsClientState.selected()` is present.
2. The selected object is still present under the same ID.
3. `isBusy()` is false.
4. `hasValidBlocks()` is true.
5. `blockifyError()` is null.
6. `blocks()` and its atlas are non-null.

The adapter must never hold the OTS map as the VoxelWeave snapshot. It must deep-copy the bounded coordinate/state data into VoxelWeave-owned immutable values.

### Capture stability check

Capture should record a pre-copy token containing at least:

- selected object ID and object identity;
- `BlockMesh` identity;
- `blockRevision`;
- current `blockRenderOffset` values;
- position / origin / rotation / scale values used as a conservative transform-race guard;
- `isBusy == false` and `hasValidBlocks == true`.

After copying, re-read the same values. Reject the capture if selection changed, the object disappeared, a job started, block validity changed, revision changed, mesh identity changed, or any captured transform/offset value changed.

Revision alone is not sufficient: OTS exposes mutable objects/maps through accessors, and `blockRenderOffset` can change when the object moves without requiring VoxelWeave to reinterpret the existing block keys.

Private metadata such as object name and source path must not be copied into `SchematicSnapshot` or routine logs.

## Coordinate and transform contract

### Packed coordinates

OTS block keys use `VoxelKey` integer packing. `VoxelKey.unpackX/Y/Z(key)` is the supported decoding boundary for the audited version; VoxelWeave should not duplicate the bit layout.

The adapter should treat decoded values as OTS block-grid coordinates and convert them to current world-space coordinates using the same boundary OTS uses when preparing an Apply operation:

```text
offset = round(SceneObject.blockRenderOffset())
worldX = decodedX + offsetX
worldY = decodedY + offsetY
worldZ = decodedZ + offsetZ
```

`SceneObject.blockRenderOffset()` is `position - blockBasePosition`, and `setBlocks(...)` records the current position as that block base. OTS's apply path adds the rounded block-render offset to every decoded block key before transfer.

**Do not apply `origin`, `rotation`, or `scale` a second time.** Those transforms are upstream inputs to voxel/block generation. For a valid `BlockMesh`, the current placement delta represented by `blockRenderOffset()` is the only additional coordinate offset used by the audited OTS apply path.

VoxelWeave should be stricter than OTS integer wraparound:

- require each rounded offset to fit the Minecraft integer coordinate range used by `BlockPos`;
- use checked addition when combining decoded coordinates and offsets;
- reject overflow rather than wrapping;
- convert the checked result to `GridPoint(long, long, long)`.

Negative coordinates are valid after this checked mapping.

### Operation target and halo

`BlockMesh` is sparse. The minimum production boundary should use the transformed non-air block coordinates to derive an occupied world-space AABB. For the first adapter implementation:

- the operation target is the occupied AABB of the selected OTS object;
- the snapshot capture domain is that AABB plus a one-block halo on all six sides;
- halo expansion must be overflow-checked;
- an empty `BlockMesh` is rejected instead of creating a zero-volume target.

A later UI can intersect this object target with an explicit user selection without changing the core snapshot contract.

## Sparse blocks: known air versus unknown

Static evidence supports a specific distinction:

- `BlockMesh` uses a sparse map;
- its map default return value is zero;
- `BlockAtlas` reserves ID `0` for `minecraft:air`;
- `BlockMesh.isBlockAt(x,y,z)` uses map-key presence to distinguish stored blocks;
- the OTS blockifier inserts entries for converted voxels, while the apply path iterates only stored map entries.

For the **bounded capture domain defined above**, a coordinate with no stored block key is therefore represented to VoxelWeave as `minecraft:air`. Coordinates outside that bounded capture domain are absent from `SchematicSnapshot` and remain unknown under VoxelWeave semantics.

This preserves the current rule:

```text
present coordinate + air state = known empty
missing snapshot coordinate     = unknown / outside captured data
```

The adapter must not create an unbounded "everything else is air" interpretation.

## Block palette and BlockState mapping

Static evidence:

- each stored block value contains an OTS atlas block ID plus neighbour metadata;
- `BlockMesh.getBlockId(value)` extracts the atlas block ID;
- `BlockAtlas.getBlockName(id)` maps an ID to a namespaced Minecraft block name;
- OTS's own Apply path resolves that name through the Minecraft block registry and uses `defaultBlockState()`.

Therefore the compatible VoxelWeave mapping is:

```text
packed OTS value
  -> BlockMesh.getBlockId(...)
  -> BlockAtlas.getBlockName(...)
  -> Minecraft registry lookup
  -> block.defaultBlockState()
  -> BlockStateRef(blockState.toString())
```

This mapping belongs entirely in the client integration adapter. `BlockStateRef` and the pure core stay unaware of OTS and Minecraft types.

Fail closed when:

- the atlas ID has no name;
- the identifier is invalid;
- the named block is not present in the active registry;
- a registry lookup would silently fall back to another block.

### State properties

OTS 0.2.0's audited Apply path uses `defaultBlockState()` from the atlas block name. The inspected `BlockMesh`/`BlockAtlas` boundary does not carry arbitrary Minecraft block-state properties. VoxelWeave must not invent orientation or other properties that OTS did not provide.

This means the direct adapter can preserve OTS 0.2.0 semantics, but it cannot reconstruct state properties that are absent from OTS's block representation.

## Provenance and stale-data contract

The OTS adapter should return the existing domain boundary:

```text
OTS client types
  -> integration/ots adapter
  -> SchematicSnapshotCapture
       |- OperationTarget
       `- SchematicSnapshot
  -> existing workspace / analysis / transformation
```

No OTS class may appear in `domain`, `analysis`, `transform`, or history APIs.

The immutable `SchematicSnapshot` itself remains the source identity used by the existing surface/feature/protrusion provenance chain. OTS revision values are capture-time guards only; they are not added to pure-domain equality or used as a global VoxelWeave revision system.

A stale OTS capture is handled by discarding it and creating a new `SchematicSnapshotCapture`, which naturally invalidates analysis bound to the previous snapshot instance.

## Relationship to the Litematica path

OTS and Litematica should be sibling read adapters, not a pipeline of mandatory recapture steps:

```text
ObjToSchematic selected object
        -> OTS read adapter ---------┐
                                     v
                              SchematicSnapshotCapture
                                     ^
Existing .litematic -> Litematica ---┘
```

The current `ReadOnlySchematicIntegration` / `SchematicSnapshotCapture` boundary already supports this architecture. OTS types stay inside `src/client/.../integration/ots`, while the current Litematica adapter remains available for existing `.litematic` workflows.

The OTS path should not Paste to the world, mutate the OTS object, or require a temporary Litematica schematic merely to obtain a snapshot.

## Telemetry / privacy adoption condition

The supplied JAR metadata explicitly says it collects feature-usage and error-diagnostic analytics and can be opted out with:

```json
{
  "telemetryEnabled": false
}
```

in `config/ots-client.json`.

Static config/telemetry inspection additionally shows:

- the default telemetry value is enabled;
- disabled telemetry returns before scheduling or emitting telemetry work;
- enabled telemetry includes common properties for OTS mod version, Minecraft version, OS name and a per-session UUID;
- telemetry batches include a persisted anonymous ID;
- OTS error telemetry sanitizes job/error messages before emission in the inspected worker path.

VoxelWeave must not silently edit OTS configuration. If OTS becomes required, VoxelWeave documentation should disclose that the companion mod has its own telemetry behavior and point users to OTS's own opt-out setting.

## Dependency strategy

Because the inspected Java classes are not documented as a stable external Java API, the first implementation should be deliberately narrow:

1. Pin OTS to the exact audited 0.2.0 Fabric artifact for Minecraft 26.1.2.
2. Resolve it through an official distribution path (for example Curse Maven / CurseForge), not a checked-in JAR.
3. Compile the OTS adapter only in the client integration source set.
4. Do not `include` or shade OTS into the VoxelWeave JAR.
5. Add runtime/manual smoke coverage before changing `fabric.mod.json` from the current state to a required `ots` dependency.
6. Re-audit the boundary before supporting a newer OTS version; do not use a broad `>=0.2.0` compatibility range for implementation-class integration.

For the planned Minecraft 26.2 VoxelWeave line, repeat the compatibility check against the matching 26.2 OTS artifact instead of assuming the 26.1.2 binary contract is identical.

## Verification matrix for the next implementation

| Case | Expected adapter result | Current evidence |
|---|---|---|
| no selected OTS object | no capture / readable diagnostic | static contract defined; runtime unverified |
| selected object is busy | reject capture | `isBusy()` static behavior verified; runtime unverified |
| selected object has invalid blocks | reject capture | `hasValidBlocks()` revision check statically verified |
| selection changes during copy | reject after post-copy identity check | design contract; runtime unverified |
| block revision changes during copy | reject after post-copy revision check | revision API statically verified |
| BlockMesh instance changes during copy | reject | design contract |
| object moves during copy | reject if offset/transform token changed | static offset behavior verified; runtime unverified |
| negative transformed coordinate | preserve negative world coordinate | mapping defined; runtime unverified |
| coordinate addition overflows | reject, never wrap | VoxelWeave safety contract |
| object rotates/scales then blocks regenerate | consume regenerated BlockMesh; do not reapply transform | upstream/static generation path supports design; runtime smoke required |
| atlas ID is unknown | reject | static atlas API verified |
| valid block name | resolve active registry default state | matches audited OTS Apply path; MC runtime verification required |
| block with state properties | preserve only OTS-provided/default state; invent nothing | audited OTS Apply path uses default state |
| absent sparse entry inside bounded capture | emit known `minecraft:air` | sparse/default-air static contract verified |
| coordinate outside capture+halo | omit from snapshot -> unknown | VoxelWeave domain contract |
| telemetry disabled in OTS config | VoxelWeave does not override it | static OTS config path verified; runtime observation required |

## Acceptance decision

### B — conditionally adoptable

There is no architectural blocker to a direct OTS -> `SchematicSnapshotCapture` adapter. The current VoxelWeave domain boundary is already suitable, and OTS 0.2.0 exposes enough state to reproduce the block placement coordinates and default block identities used by its own Apply preparation without invoking Paste.

The remaining conditions are integration risk, not a reason to merge OTS types into the core:

- runtime smoke the static coordinate/revision assumptions on Java 25 / Minecraft 26.1.2;
- verify exact 0.2.0 compile/runtime acquisition in Gradle/CI;
- pin the audited implementation-class contract rather than assuming API stability;
- keep OTS separately installed and never redistribute its JAR;
- document OTS's telemetry-on-by-default behavior and opt-out;
- keep Litematica as a sibling capture path.

## Next minimum implementation issue

Suggested follow-up title:

> **Integration: add read-only ObjToSchematic 0.2.0 snapshot adapter**

Minimum scope:

- add an `integration/ots` client adapter only;
- add exact-version compile/runtime development wiring without bundling OTS;
- capture the selected, idle, valid OTS `BlockMesh` into a deep-copied `SchematicSnapshotCapture`;
- map coordinates with decoded key + checked rounded `blockRenderOffset`;
- derive occupied AABB + one-block known-air halo;
- resolve atlas names to Minecraft default block states and then `BlockStateRef`;
- pre/post validate selection identity, block-mesh identity, block revision, busy/valid state and transform/offset token;
- add deterministic adapter-level tests around extracted mapping helpers where possible;
- perform a Java 25 / Minecraft 26.1.2 manual smoke covering move, rotation/scale regeneration, negative coordinates, selection change, busy state, sparse air and an unsupported atlas entry;
- keep production hard dependency promotion as a separate decision after that smoke passes.

Out of scope for that follow-up: OTS mutation, Paste, world write, Litematica write-back, export, Step 7B-2, or converter-specific analysis heuristics.
