# Fluid quality and JEI regression

Run from the repository root with Java 17 available to Gradle:

```powershell
.\gradlew.bat runGameTestServer
```

The `gameTest` source set is loaded only by the Forge GameTest server. Its fluids,
buckets, tags, and empty structure are excluded from the published mod JAR.
Tests use real Forge capabilities, JEI 15.20.0.134, and the common QFF Mixins.
The development runtime also supplies Create and optional storage integrations;
the tests do not require an integration pack or its scripts. These development
dependencies remain optional for the shipped mod.

The tests cover:

- Normal, iron, gold, and diamond bucket reads through `FluidUtil.getFluidContained`,
  `getFluidInTank`, and both capability drain overloads.
- Both direct `FluidUtil.getFilledBucket` and capability fill, including a fluid
  provider returning a shared bucket template with existing native NBT and stale
  diamond quality. Normal input must remove only the old QF quality field.
- Two full buckets into a `FluidTank` and back, including the final bucket.
- Simulated fills, drains, and `FluidUtil` transfers without changing inputs,
  containers, templates, or tanks.
- Diamond → iron → normal transfers through the same tank, native rejection of
  different quality NBT, and the native minimum bucket capacity.
- Excluded fluids, empty fluid stacks, and empty buckets retaining native behavior.
- Reusable storage with the same Forge handler/capacity as the copper bucket:
  fluid quality survives both transfer directions, but QF item quality is rejected.
- Stale outer QF tags are cleared on processing without changing stored fluids or
  native data; normal fluid never inherits another tank's grade.
- Real JEI subtype registration and native interpreters: four distinct Ingredient
  IDs, one Recipe ID across quality grades, with native aging preserved.
- Recipe-lookup copies remove quality from item tags, nested fluid NBT, and
  serialized ForgeCaps without changing the actual item or its stored fluid.
- When Fluid Logistics is loaded, its real copper bucket is filled and emptied
  through Forge for all four grades, without receiving an outer QF item tag.

World/Create review: world bucket placement and pickup use separate world-quality
hooks. Create Spout may reapply the same grade after Forge bucket fill; this is
deterministic and the normal-grade Forge path clears stale item quality first.
The new hooks do not change acceptance, amount, or simulation decisions.

## Integration-pack checks

These checks require the actual pack and are not replaced by the fixture tests:

1. Add support tags for the pack's six juice fluids and their buckets in
   the pack, without adding their IDs to QFF.
2. For each juice, prepare normal, iron, gold, and diamond buckets (24 cases).
3. Empty each into its native keg, inspect the stored fluid's QFF quality, refill
   a bucket, and inspect its QF quality. Both must equal the original grade.
4. Repeat diamond → iron → normal on the same keg after draining each batch
   completely. Include its last bucket, and check the pack's existing mixing
   rules when the keg is not empty.
5. Repeat supported-fluid world placement/pickup and Create Spout/emptying with
   the four grades. Verify no grade changes and no stale quality after normal.

Production uses the existing QFF fluid support/exclusion tags and quality APIs.
The declared QFF container/source tags currently have no transfer-side readers;
this regression does not introduce a new whitelist policy or quality NBT format.
