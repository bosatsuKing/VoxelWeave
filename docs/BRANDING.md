# VoxelWeave Branding Direction

**Status:** Design direction only. No final icon, logo, or binary asset is approved or shipped by this document.

## Purpose

The VoxelWeave icon should communicate a Minecraft-adjacent block tool whose identity comes from refinement and weaving. It should remain useful if the project later supports more input adapters; it must not look like an OBJ/GLB converter or imply that one companion mod owns the workflow.

Brand associations:

- voxel and block geometry;
- weave, interconnection, and layers;
- careful refinement rather than destructive replacement;
- analysis before editing;
- reversible, bounded work.

The icon should feel precise and constructive. It should avoid generic “magic cleanup” imagery because VoxelWeave preserves creator intent and exposes uncertainty rather than promising automatic perfection.

## Concept comparison

| Concept | Composition | Strength | Main risk | Decision |
|---|---|---|---|---|
| **A. Weaved Voxel Cube** | One voxel cube crossed by two interwoven contour bands | Directly combines the product name with Minecraft-like block geometry; remains valid across input sources | Too many crossings could become noise at 16 px | **Recommended** |
| **B. Refined Block Face** | A coarse stepped left edge transitioning to a clean right contour | Communicates before/after refinement quickly | Can imply generic smoothing or a converter result | Reserve as an explanatory graphic |
| **C. Grid and Surface Trace** | A compact wireframe grid with one highlighted analyzed contour | Signals analysis and geometry to technical users | Fine grid lines can disappear at small sizes | Reserve for documentation or larger marks |

## Recommended direction: Weaved Voxel Cube

Use a single three-quarter-view cube with two broad bands crossing its front/top surfaces. The cube supplies the block silhouette; the bands supply the weave. A slightly stepped outer edge may suggest refinement, but the icon should not rely on a detailed before/after scene.

```text
      ______
     /  ╳  /|
    /_____/ |
    | ╲ ╱ | |
    |  ╳  | /
    |_/ \_|/
```

The sketch describes composition only. It is not a pixel-accurate logo.

### Shape requirements

- Use one dominant cube silhouette and no more than two bands.
- Make the crossing readable through overlap order or a small gap, not thin outlines.
- Keep major forms asymmetric enough that the mark has a recognizable orientation.
- Preserve a clear silhouette when rendered as a single color.
- Avoid loose strands outside the cube at the smallest sizes.
- Use block-aligned or 45-degree edges; do not depend on fine curves.

### Color direction

Recommended working palette:

| Role | Color | Hex | Use |
|---|---|---|---|
| Base | Dark slate | `#263640` | Cube body / silhouette |
| Primary accent | Bright cyan | `#39C6D4` | First weave band / analysis signal |
| Secondary accent | Light stone | `#D7D2C8` | Second band / refined surface |
| Deep shadow | Near black-blue | `#111B22` | Separation at overlaps |

The palette is a direction, not a locked production value. Validate it against light and dark UI backgrounds. Cyan should remain an accent rather than filling the whole cube. A one-color version must still show the cube-and-crossing structure through shape and negative space.

## Small-size requirements

The primary acceptance size is the launcher/Mod Menu range, not a large repository banner.

- At **16×16**, retain the outer cube and one unmistakable crossing; minor surface steps may disappear.
- At **32×32**, both bands and their overlap order should be legible.
- At **64×64**, limited face shading or one refinement step may be added without changing the silhouette.
- Keep important strokes at least two pixels wide in the 32×32 master study.
- Test on light, dark, and mid-value backgrounds without relying on a surrounding badge.
- Inspect nearest-neighbor output; avoid anti-aliased detail that becomes gray noise when downscaled.
- Check grayscale and monochrome versions before approval.

## Visuals to avoid

- A pickaxe, hammer, wand, or sparkle as the only motif; these do not distinguish VoxelWeave.
- Dense wireframes, many tiny blocks, text, initials, or gradients that collapse below 64 px.
- A photorealistic 3D render that does not read as a mod icon.
- Imagery that makes VoxelWeave look like a dedicated OBJ/GLB importer.
- Visual language, silhouettes, colors, logos, or official art closely associated with Axiom, Litematica, ObjToSchematic, Minecraft, or another project.
- A “rough becomes perfectly smooth” promise that conflicts with feature preservation and user-controlled policy.

## Relationship to documentation visuals

README diagrams should remain plain Mermaid diagrams and tables. They explain system relationships and current/planned status; they are not part of the logo. The icon should not embed an architecture diagram, workflow arrows, companion-mod marks, or status badges.

Concept B can later support a larger before/after illustration, and Concept C can support architecture or analysis documentation. Those secondary graphics should use the same slate/cyan/stone palette without becoming alternate logos.

## Future production checklist

A later icon-production task should:

1. Create original artwork from this direction without using another mod's assets.
2. Compare at least three silhouette thumbnails before adding surface detail.
3. Review 16, 32, and 64 px raster previews on light and dark backgrounds.
4. Check monochrome recognition and common color-vision deficiencies.
5. Export the exact sizes/formats required by Fabric metadata and distribution pages.
6. Add source artwork and licensing/provenance information only after the final design is approved.
7. Update `fabric.mod.json` in the same reviewed change that adds the final asset.

Final icon production, animation, multi-resolution export, metadata wiring, and external image generation are outside this documentation change.
