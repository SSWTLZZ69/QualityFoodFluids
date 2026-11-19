package io.github.createdelight.qualityfoodfluids.compat.jei;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CgtJeiFluidEntries {
    private CgtJeiFluidEntries() {
    }

    public static Collection<FluidStack> baseEntries(Collection<FluidStack> variants) {
        Map<Fluid, FluidStack> entries = new LinkedHashMap<>();
        for (FluidStack stack : variants) {
            if (stack.isEmpty()) {
                continue;
            }
            Fluid fluid = stack.getFluid();
            Fluid source = fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
            // These are JEI list representatives, never the actual recipe or
            // stored stacks. The ingredient list only needs the ordinary fluid.
            entries.computeIfAbsent(source, key -> new FluidStack(key, 1000));
        }
        return new ArrayList<>(entries.values());
    }
}
