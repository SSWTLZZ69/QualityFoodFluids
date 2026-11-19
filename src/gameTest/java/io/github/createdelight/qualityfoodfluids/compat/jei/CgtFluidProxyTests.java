package io.github.createdelight.qualityfoodfluids.compat.jei;

import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
import io.github.createdelight.qualityfoodfluids.QualityFoodFluids;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(QualityFoodFluids.MODID)
@PrefixGameTestTemplate(false)
public final class CgtFluidProxyTests {
    @GameTest(template = "empty")
    public static void lateDrinkFluidsHavePersistentProxiesWithAllNbt(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("creategearsandtavern")) {
            helper.succeed();
            return;
        }
        var hasProxy = Class.forName("com.yision.fluidlogistics.content.logistics.fluidPackage.FluidProxyRegistry")
                .getMethod("hasProxy", FluidStack.class);
        check(!(boolean) hasProxy.invoke(null, FluidStack.EMPTY), "empty fluid has a drag target");
        int drinks = 0;
        for (ResourceLocation id : ForgeRegistries.FLUIDS.getKeys()) {
            if (!id.getNamespace().equals("creategearsandtavern") || id.getPath().startsWith("flowing_")) {
                continue;
            }
            drinks++;
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
            for (int quality = 0; quality <= 3; quality++) {
                FluidStack input = new FluidStack(fluid, 1000);
                CompoundTag variant = new CompoundTag();
                variant.putString("drink_id", "kaleidoscope_tavern:" + id.getPath());
                variant.putInt("brew_level", 6);
                input.getOrCreateTag().put("DrinkVariant", variant);
                input.getOrCreateTag().putString("signature_payload", "keep effects and color");
                if (quality > 0) {
                    CompoundTag grade = new CompoundTag();
                    grade.putInt(QualityFoodFluidsApi.QUALITY_KEY, quality);
                    grade.putInt(QualityFoodFluidsApi.VERSION_KEY, QualityFoodFluidsApi.CURRENT_VERSION);
                    input.getOrCreateTag().put(QualityFoodFluidsApi.FLUID_QUALITY_TAG, grade);
                }
                CompoundTag before = input.getTag().copy();
                check((boolean) hasProxy.invoke(null, input), "missing late drink proxy for " + id);
                ItemStack proxy = PackageResourceTypes.createFluidKey(input);
                check(!proxy.isEmpty(), "empty proxy for " + id);
                ResourceLocation expectedId = new ResourceLocation("fluidlogistics",
                        "fluid_proxy/" + id.getNamespace() + "/" + id.getPath());
                check(expectedId.equals(ForgeRegistries.ITEMS.getKey(proxy.getItem())), "unstable proxy ID for " + id);
                ItemStack saved = ItemStack.of(proxy.save(new CompoundTag()));
                FluidStack restored = PackageResourceTypes.getFluid(saved);
                check(restored.getFluid() == fluid && before.equals(restored.getTag()),
                        "proxy persistence changed drink, grade, or cocktail data for " + id);
                check(before.equals(input.getTag()), "proxy construction mutated the real fluid");

                Fluid flowing = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(id.getNamespace(), "flowing_" + id.getPath()));
                if (flowing != null) {
                    FluidStack flow = new FluidStack(flowing, 1000, before.copy());
                    ItemStack flowProxy = PackageResourceTypes.createFluidKey(flow);
                    check(flowProxy.getItem() == proxy.getItem(), "source and flowing drinks use different proxies");
                    check(before.equals(PackageResourceTypes.getFluid(flowProxy).getTag()), "flowing proxy lost NBT");
                }
            }
        }
        check(drinks > 0, "CGT loaded without any discoverable drink fluids");
        System.out.println("CGT proxy regression verified " + drinks + " drinks at all four QFF grades");
        helper.succeed();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
