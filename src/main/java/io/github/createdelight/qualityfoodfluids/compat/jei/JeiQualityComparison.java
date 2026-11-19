package io.github.createdelight.qualityfoodfluids.compat.jei;

import de.cadentem.quality_food.util.QualityUtils;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;

/** Removes only quality metadata from temporary JEI recipe-lookup inputs. */
public final class JeiQualityComparison {
    private JeiQualityComparison() {
    }

    public static Object withoutQuality(Object ingredient) {
        if (ingredient instanceof FluidStack fluid) {
            return QualityFoodFluidsApi.clearQuality(fluid);
        }

        if (!(ingredient instanceof ItemStack item) || item.isEmpty()) {
            return ingredient;
        }

        // Include ForgeCaps: reusable containers may store their fluid there
        // rather than in the ordinary item tag. Never mutate the real stack.
        // ItemStack.save may attach live item/capability tag references to its
        // result. Deep-copy the complete serialization before removing fields.
        CompoundTag data = item.save(new CompoundTag()).copy();
        CompoundTag itemTag = data.getCompound("tag");
        boolean changed = itemTag.contains(QualityUtils.QUALITY_TAG);
        itemTag.remove(QualityUtils.QUALITY_TAG);

        if (changed && itemTag.isEmpty()) {
            data.remove("tag");
        }

        changed |= removeFluidQuality(data);
        return changed ? ItemStack.of(data) : ingredient;
    }

    private static boolean removeFluidQuality(Tag tag) {
        boolean changed = false;

        if (tag instanceof CompoundTag compound) {
            changed = compound.contains(QualityFoodFluidsApi.FLUID_QUALITY_TAG);
            compound.remove(QualityFoodFluidsApi.FLUID_QUALITY_TAG);

            for (String key : new ArrayList<>(compound.getAllKeys())) {
                Tag child = compound.get(key);

                if (removeFluidQuality(child)) {
                    changed = true;

                    if (child instanceof CompoundTag childCompound && childCompound.isEmpty()) {
                        compound.remove(key);
                    }
                }
            }
        } else if (tag instanceof ListTag list) {
            for (Tag child : list) {
                changed |= removeFluidQuality(child);
            }
        }

        return changed;
    }
}
