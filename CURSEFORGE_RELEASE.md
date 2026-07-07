# Quality Food Fluids 0.1.0

Quality Food Fluids is a Forge 1.20.1 addon for Quality Food and Create. It lets datapack-selected fluids carry Quality Food quality through containers, placed source blocks, Create logistics, and several processing machines.

## Highlights

- Fluids can store Quality Food quality on `FluidStack` NBT.
- Placeable source fluids can keep quality in the world.
- Create drains, spouts, basins, tanks, hose pulleys, pipes, deployers, and sequenced assembly preserve or use quality.
- Item and fluid inputs can both affect qualified item and fluid outputs.
- Machine runs lock their quality result when processing starts, preventing blocked-output reroll exploits.
- Jade and JEI integration are included when those mods are installed.
- Optional compat is included for Brewin' And Chewin', Farmer's Respite, and Create Diesel Generators bulk fermenting.

## Required Dependencies

- Forge 47+
- Quality Food 2.3.0+
- Create 6.0.8+

## Optional Integrations

- Jade
- JEI
- Brewin' And Chewin'
- Farmer's Respite
- Create Diesel Generators

## Pack Configuration

Add fluids to the `quality_food_fluids:quality_fluids` and `quality_food_fluids:world_quality_fluids` tags to decide which fluids in your pack can carry quality.
