# Quality Food Fluids

Quality Food Fluids is a Forge 1.20.1 addon for Quality Food. It lets fluids carry Quality Food quality, then keeps that quality through containers, world fluid blocks, and optional food-processing machine integrations.

## Features

- Stores quality on `FluidStack` NBT and on placed source fluids through per-dimension saved data.
- Transfers quality between buckets/fluid containers and fluids when filling or emptying.
- Supports Create drains, spouts, basins, tanks, hose pulleys, open-ended pipes, deployers, and sequenced assembly when Create is installed.
- Lets qualified item and fluid inputs both affect qualified item and fluid outputs.
- Locks processing quality when a machine run starts, so blocked outputs cannot be used to reroll quality.
- Shows quality information in Jade and JEI when those mods are installed.
- Adds optional compatibility for Brewin' And Chewin', Farmer's Respite, and Create Diesel Generators bulk fermenting.

## Datapack Tags

This mod does not decide which fluids in your pack should carry food quality. Add fluids to:

- `#quality_food_fluids:quality_fluids` for fluids that can store quality on a `FluidStack`.
- `#quality_food_fluids:world_quality_fluids` for placeable fluids that should keep quality in the world.
- `#quality_food_fluids:clear_quality_fluids` for fluids that should explicitly drop any carried quality.

## Dependencies

Required:

- Minecraft 1.20.1
- Forge 47+
- Quality Food 2.3.0+

Optional:

- Create 6.0.8+
- Jade
- JEI
- Brewin' And Chewin'
- Farmer's Respite
- Create Diesel Generators

## License

MIT License.
