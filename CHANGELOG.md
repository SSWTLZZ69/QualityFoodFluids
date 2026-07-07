# Changelog

## 0.1.2 - 2026-07-08

- Fixed Jade compatibility so Quality Food Fluids no longer reads Jade's internal fluid-storage payload and does not suppress the normal fluid container display.
- Fixed duplicate JEI subtype registrations when other plugins already handle the same item or fluid.
- Fixed Brewin' And Chewin' keg and Farmer's Respite kettle item-pouring paths so quality from pourable containers is copied into stored fluids.
- Applied keg and kettle produced-fluid quality at the output `FluidStack` creation point.
- Fixed Brewin' And Chewin' keg and Farmer's Respite kettle recipe checks so QFF quality NBT does not prevent matching.

## 0.1.1 - 2026-07-08

- Fixed optional dependency version ranges for mods that do not use semantic versions.
- Kept Create as an optional dependency.

## 0.1.0 - 2026-07-07

Initial release for Minecraft 1.20.1 Forge.

- Added fluid quality storage on `FluidStack` NBT.
- Added placed source-fluid quality storage and bucket pickup/placement transfer.
- Added optional Create integration for drains, spouts, basins, tanks, hose pulleys, pipes, deployers, and sequenced assembly.
- Added quality-aware item and fluid outputs for Create basin recipes.
- Added locked processing tickets to avoid output-blocking quality rerolls.
- Added Jade fluid quality display support.
- Added JEI information for quality-capable fluids.
- Added optional Brewin' And Chewin' keg support.
- Added optional Farmer's Respite kettle support.
- Added optional Create Diesel Generators bulk fermenter support.
