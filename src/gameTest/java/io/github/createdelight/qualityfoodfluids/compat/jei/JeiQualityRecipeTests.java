package io.github.createdelight.qualityfoodfluids.compat.jei;

import de.cadentem.quality_food.core.Quality;
import de.cadentem.quality_food.util.QualityUtils;
import io.github.createdelight.qualityfoodfluids.QualityFoodFluids;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.library.ingredients.subtypes.SubtypeManager;
import mezz.jei.library.load.registration.SubtypeRegistration;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

@GameTestHolder(QualityFoodFluids.MODID)
@PrefixGameTestTemplate(false)
public final class JeiQualityRecipeTests {
    private static final Quality[] GRADES = {Quality.NONE, Quality.IRON, Quality.GOLD, Quality.DIAMOND};

    @GameTest(template = "empty")
    public static void registeredQualityVariantsShareRecipeIds(GameTestHelper helper) {
        SubtypeRegistration registration = new SubtypeRegistration();
        QualityFoodFluidsJeiPlugin plugin = new QualityFoodFluidsJeiPlugin();
        plugin.registerItemSubtypes(registration);
        plugin.registerFluidSubtypes(registration, null);
        SubtypeManager manager = new SubtypeManager(registration.getInterpreters());
        assertContexts(manager);
        check(!manager.getSubtypeInfo(ForgeTypes.FLUID_STACK, fluid(Quality.NONE, 5), UidContext.Recipe)
                        .equals(manager.getSubtypeInfo(ForgeTypes.FLUID_STACK, fluid(Quality.DIAMOND, 6), UidContext.Recipe)),
                "QFF recipe lookup discarded native aging");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeInterpretersKeepAgingAndIgnoreRecipeQuality(GameTestHelper helper) {
        SubtypeRegistration registration = new SubtypeRegistration();
        // Native interpreters can inspect every NBT field in Recipe context.
        // Ingredient context deliberately ignores quality to exercise QFF's
        // subtype-manager hook independently of QFF's own interpreters.
        registration.registerSubtypeInterpreter(ForgeTypes.FLUID_STACK, testFluid(), (stack, context) ->
                context == UidContext.Recipe ? String.valueOf(stack.getTag())
                        : "aging=" + stack.getOrCreateTag().getInt("native_aging"));
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, bucket(Quality.NONE).getItem(), (stack, context) ->
                context == UidContext.Recipe ? stack.save(new CompoundTag()).toString()
                        : "aging=" + stack.getOrCreateTag().getInt("native_aging"));
        SubtypeManager manager = new SubtypeManager(registration.getInterpreters());
        assertContexts(manager);
        ItemStack aged = bucket(Quality.DIAMOND);
        aged.getOrCreateTag().putInt("native_aging", 6);
        check(!manager.getSubtypeInfo(VanillaTypes.ITEM_STACK, bucket(Quality.NONE), UidContext.Recipe)
                        .equals(manager.getSubtypeInfo(VanillaTypes.ITEM_STACK, aged, UidContext.Recipe)),
                "native item interpreter lost aging");
        check(!manager.getSubtypeInfo(ForgeTypes.FLUID_STACK, fluid(Quality.NONE, 5), UidContext.Recipe)
                        .equals(manager.getSubtypeInfo(ForgeTypes.FLUID_STACK, fluid(Quality.DIAMOND, 6), UidContext.Recipe)),
                "native fluid interpreter lost aging");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lookupCopiesStripStoredQualityWithoutChangingContainers(GameTestHelper helper) {
        for (String fixture : new String[]{"bucket_test_storage", "bucket_test_cap_storage"}) {
            ItemStack storage = new ItemStack(ForgeRegistries.ITEMS.getValue(id(fixture)));
            storage.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(AssertionError::new)
                    .fill(fluid(Quality.DIAMOND, 5), FluidAction.EXECUTE);
            storage.getOrCreateTag().putString("native_metadata", "keep");
            storage.getOrCreateTag().put(QualityUtils.QUALITY_TAG,
                    bucket(Quality.DIAMOND).getTag().get(QualityUtils.QUALITY_TAG).copy());
            CompoundTag upgrade = new CompoundTag();
            upgrade.put("contents", fluid(Quality.IRON, 7).writeToNBT(new CompoundTag()));
            storage.getOrCreateTag().put("upgrade", upgrade);
            CompoundTag before = storage.save(new CompoundTag()).copy();

            ItemStack lookup = (ItemStack) JeiQualityComparison.withoutQuality(storage);
            check(!lookup.getTag().contains(QualityUtils.QUALITY_TAG), "lookup retained outer QF tag");
            FluidStack contained = FluidUtil.getFluidContained(lookup).orElseThrow(AssertionError::new);
            check(!QualityFoodFluidsApi.hasQuality(contained), "lookup retained stored fluid quality");
            check(contained.getTag().getInt("native_aging") == 5, "lookup lost contained aging");
            FluidStack nested = FluidStack.loadFluidStackFromNBT(lookup.getTag().getCompound("upgrade").getCompound("contents"));
            check(!QualityFoodFluidsApi.hasQuality(nested) && nested.getTag().getInt("native_aging") == 7,
                    "lookup lost nested native data or retained quality");
            check("keep".equals(lookup.getTag().getString("native_metadata")), "lookup lost item data");
            check(before.equals(storage.save(new CompoundTag())), "recipe lookup mutated the real container");
            check(QualityFoodFluidsApi.getQuality(FluidUtil.getFluidContained(storage).orElseThrow()) == Quality.DIAMOND,
                    "recipe lookup changed stored fluid quality");
        }
        helper.succeed();
    }

    private static void assertContexts(SubtypeManager manager) {
        Set<String> fluidIngredients = new HashSet<>();
        Set<String> itemIngredients = new HashSet<>();
        Set<String> fluidRecipes = new HashSet<>();
        Set<String> itemRecipes = new HashSet<>();
        for (Quality grade : GRADES) {
            FluidStack fluid = fluid(grade, 5);
            FluidStack fluidBefore = fluid.copy();
            ItemStack item = bucket(grade);
            CompoundTag itemBefore = item.save(new CompoundTag()).copy();
            fluidIngredients.add(manager.getSubtypeInfo(ForgeTypes.FLUID_STACK, fluid, UidContext.Ingredient));
            itemIngredients.add(manager.getSubtypeInfo(VanillaTypes.ITEM_STACK, item, UidContext.Ingredient));
            fluidRecipes.add(manager.getSubtypeInfo(ForgeTypes.FLUID_STACK, fluid, UidContext.Recipe));
            itemRecipes.add(manager.getSubtypeInfo(VanillaTypes.ITEM_STACK, item, UidContext.Recipe));
            check(fluidBefore.isFluidStackIdentical(fluid), "JEI mutated original fluid");
            check(itemBefore.equals(item.save(new CompoundTag())), "JEI mutated original item");
        }
        check(fluidIngredients.size() == 4 && itemIngredients.size() == 4, "JEI collapsed quality ingredient variants");
        check(fluidRecipes.size() == 1 && itemRecipes.size() == 1, "quality variants cannot match the same recipe");
    }

    private static ItemStack bucket(Quality grade) {
        ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id("bucket_test_bucket")));
        stack.getOrCreateTag().putInt("native_aging", 5);
        return QualityFoodFluidsApi.applyItemQuality(stack, grade);
    }

    private static FluidStack fluid(Quality grade, int aging) {
        FluidStack stack = new FluidStack(testFluid(), 1000);
        stack.getOrCreateTag().putInt("native_aging", aging);
        return QualityFoodFluidsApi.withQuality(stack, grade);
    }

    private static Fluid testFluid() {
        return ForgeRegistries.FLUIDS.getValue(id("bucket_test_fluid"));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(QualityFoodFluids.MODID, path);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
