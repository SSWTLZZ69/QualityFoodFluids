# Quality Food Fluids 0.1.4

Quality Food Fluids is a Forge 1.20.1 addon for Quality Food. It lets datapack-selected fluids carry Quality Food quality through containers, placed source blocks, and optional processing-machine integrations.

## 0.1.4 Changes

- Ordinary drinkable items can now receive Quality Food quality automatically without a material-whitelist entry.
- Potion items remain excluded from automatic quality eligibility.
- Buckets and fluid containers become quality-capable automatically when they contain a fluid supported by Quality Food Fluids.
- The existing Quality Food blacklist remains authoritative.
- Fixes the bundled milk fluid tag when running without a registered milk fluid.

## Highlights

- Fluids can store Quality Food quality on `FluidStack` NBT.
- Placeable source fluids can keep quality in the world.
- Create drains, spouts, basins, tanks, hose pulleys, pipes, deployers, and sequenced assembly preserve or use quality when Create is installed.
- Item and fluid inputs can both affect qualified item and fluid outputs.
- Machine runs lock their quality result when processing starts, preventing blocked-output reroll exploits.
- Jade and JEI integration are included when those mods are installed.
- Optional compat is included for Brewin' And Chewin', Farmer's Respite, and Create Diesel Generators bulk fermenting.
- Common food and drink fluids are included by default and can still be extended or overridden with datapack tags.

## Required Dependencies

- Forge 47+
- Quality Food 2.3.0+

## Optional Integrations

- Create 6.0.8+
- Jade
- JEI
- Brewin' And Chewin'
- Farmer's Respite
- Create Diesel Generators

## Pack Configuration

Extend the `quality_food_fluids:quality_fluids` and `quality_food_fluids:world_quality_fluids` tags to support additional pack fluids. Use `quality_food_fluids:clear_quality_fluids` to force specific fluids to discard carried quality.
