package io.github.createdelight.qualityfoodfluids.internal;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public record BasinProcessingTicket(ResourceLocation recipeId, List<String> sourceSignature, Quality quality, List<FluidStack> outputFluids) {
    private static final String RECIPE_KEY = "Recipe";
    private static final String SOURCES_KEY = "Sources";
    private static final String QUALITY_KEY = "Quality";
    private static final String OUTPUTS_KEY = "Outputs";

    public BasinProcessingTicket(ResourceLocation recipeId, List<String> sourceSignature, Quality quality, List<FluidStack> outputFluids) {
        this.recipeId = recipeId;
        this.sourceSignature = List.copyOf(sourceSignature);
        this.quality = quality == null ? Quality.NONE : quality;
        this.outputFluids = copyFluids(outputFluids);
    }

    public boolean matches(ResourceLocation recipeId, List<String> sourceSignature) {
        return this.recipeId.equals(recipeId) && this.sourceSignature.equals(sourceSignature);
    }

    public List<FluidStack> copyOutputs() {
        return copyFluids(outputFluids);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(RECIPE_KEY, recipeId.toString());

        ListTag sources = new ListTag();
        for (String source : sourceSignature) {
            sources.add(StringTag.valueOf(source));
        }
        tag.put(SOURCES_KEY, sources);
        tag.putInt(QUALITY_KEY, quality.level());

        ListTag outputs = new ListTag();
        for (FluidStack stack : outputFluids) {
            outputs.add(stack.writeToNBT(new CompoundTag()));
        }
        tag.put(OUTPUTS_KEY, outputs);
        return tag;
    }

    public static BasinProcessingTicket load(CompoundTag tag) {
        if (!tag.contains(RECIPE_KEY, Tag.TAG_STRING)) {
            return null;
        }

        ResourceLocation recipeId = ResourceLocation.tryParse(tag.getString(RECIPE_KEY));
        if (recipeId == null) {
            return null;
        }

        List<String> sources = new ArrayList<>();
        ListTag sourceTags = tag.getList(SOURCES_KEY, Tag.TAG_STRING);
        for (int i = 0; i < sourceTags.size(); i++) {
            sources.add(sourceTags.getString(i));
        }

        List<FluidStack> outputs = new ArrayList<>();
        ListTag outputTags = tag.getList(OUTPUTS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < outputTags.size(); i++) {
            FluidStack stack = FluidStack.loadFluidStackFromNBT(outputTags.getCompound(i));
            if (!stack.isEmpty()) {
                outputs.add(stack);
            }
        }

        Quality quality = tag.contains(QUALITY_KEY, Tag.TAG_INT)
                ? Quality.get(tag.getInt(QUALITY_KEY))
                : firstFluidQuality(outputs);

        if (outputs.isEmpty() && quality == Quality.NONE && !tag.contains(QUALITY_KEY, Tag.TAG_INT)) {
            return null;
        }

        return new BasinProcessingTicket(recipeId, sources, quality, outputs);
    }

    private static Quality firstFluidQuality(List<FluidStack> outputs) {
        for (FluidStack stack : outputs) {
            Quality quality = QualityFoodFluidsApi.getQuality(stack);

            if (quality.level() > 0) {
                return quality;
            }
        }

        return Quality.NONE;
    }

    private static List<FluidStack> copyFluids(List<FluidStack> fluids) {
        List<FluidStack> copies = new ArrayList<>();

        for (FluidStack stack : fluids) {
            copies.add(stack.copy());
        }

        return copies;
    }
}
