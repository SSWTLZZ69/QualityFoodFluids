package io.github.createdelight.qualityfoodfluids.gametest;

import de.cadentem.quality_food.core.Quality;
import de.cadentem.quality_food.util.QualityUtils;
import de.cadentem.quality_food.util.Utils;
import io.github.createdelight.qualityfoodfluids.QualityFoodFluids;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsCreateRules;
import io.github.createdelight.qualityfoodfluids.util.FluidQualityNames;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@GameTestHolder(QualityFoodFluids.MODID)
@PrefixGameTestTemplate(false)
public final class ForgeBucketQualityTests {
    private static final Quality[] GRADES = {Quality.NONE, Quality.IRON, Quality.GOLD, Quality.DIAMOND};

    @GameTest(template = "empty")
    public static void drainPreservesEveryGrade(GameTestHelper helper) {
        for (Quality grade : GRADES) {
            ItemStack bucket = bucket(grade);
            ItemStack before = bucket.copy();
            IFluidHandlerItem handler = handler(bucket);
            assertFluid(FluidUtil.getFluidContained(bucket).orElse(FluidStack.EMPTY), grade);
            assertFluid(handler.getFluidInTank(0), grade);
            assertFluid(handler.drain(1000, FluidAction.SIMULATE), grade);
            assertFluid(handler.drain(fluid(grade), FluidAction.SIMULATE), grade);
            check(ItemStack.matches(before, handler.getContainer()), "SIMULATE drain changed the container");

            FluidStack exposed = handler.getFluidInTank(0);
            exposed.getOrCreateTag().putBoolean("external_mutation", true);
            check(!handler.getFluidInTank(0).getOrCreateTag().contains("external_mutation"),
                    "getFluid exposed shared fluid data");
            check(ItemStack.matches(before, bucket), "getFluid changed the bucket's NBT");

            assertFluid(handler.drain(1000, FluidAction.EXECUTE), grade);
            check(handler.getContainer().is(Items.BUCKET), "drain did not produce an empty bucket");
            check(handler.drain(1000, FluidAction.EXECUTE).isEmpty(), "empty bucket drained twice");
            assertFluid(handler(bucket(grade)).drain(fluid(grade), FluidAction.EXECUTE), grade);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fillPreservesEveryGradeAndNativeNbt(GameTestHelper helper) {
        for (Quality grade : GRADES) {
            FluidStack input = fluid(grade);
            input.getOrCreateTag().putInt("input_metadata", 42);
            FluidStack before = input.copy();
            ItemStack template = Fixtures.template(false);
            ItemStack templateBefore = template.copy();

            ItemStack direct = FluidUtil.getFilledBucket(input);
            assertBucket(direct, grade);
            check(direct != template, "getFilledBucket returned the shared provider template");
            check(direct.getTag().getInt("native_bucket_metadata") == 23, "native bucket NBT was lost");
            check(ItemStack.matches(templateBefore, template), "getFilledBucket modified the provider template");

            ItemStack empty = new ItemStack(Items.BUCKET);
            empty.getOrCreateTag().putBoolean("empty_bucket_metadata", true);
            ItemStack emptyBefore = empty.copy();
            IFluidHandlerItem handler = handler(empty);
            check(handler.fill(input, FluidAction.SIMULATE) == 1000, "SIMULATE fill accepted wrong amount");
            check(ItemStack.matches(emptyBefore, handler.getContainer()), "SIMULATE fill changed the container");
            check(before.isFluidStackIdentical(input), "fill changed its input fluid");
            check(handler.fill(input, FluidAction.EXECUTE) == 1000, "fill accepted wrong amount");
            assertBucket(handler.getContainer(), grade);
            check(handler.getContainer().getTag().getInt("native_bucket_metadata") == 23,
                    "capability fill lost provider NBT");
            check(before.isFluidStackIdentical(input), "EXECUTE fill changed its input fluid");
            check(ItemStack.matches(templateBefore, template), "capability fill modified the provider template");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tankRoundTripIncludesLastBucket(GameTestHelper helper) {
        for (Quality grade : GRADES) {
            FluidTank tank = new FluidTank(2000);
            for (int index = 0; index < 2; index++) {
                FluidActionResult emptied = FluidUtil.tryEmptyContainer(bucket(grade), tank, 1000, null, true);
                check(emptied.isSuccess() && emptied.getResult().is(Items.BUCKET), "bucket did not empty into tank");
            }
            check(tank.getFluidAmount() == 2000, "tank received the wrong amount");
            check(QualityFoodFluidsApi.getQuality(tank.getFluid()) == grade, "tank lost quality");

            for (int index = 0; index < 2; index++) {
                FluidActionResult filled = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), tank, 1000, null, true);
                check(filled.isSuccess(), "tank could not fill a bucket, including its last bucket");
                assertBucket(filled.getResult(), grade);
                check(tank.getFluidAmount() == (1 - index) * 1000, "tank consumed the wrong amount");
            }
            check(tank.isEmpty(), "tank was not empty after its last bucket");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void simulatedTransfersAreReadOnly(GameTestHelper helper) {
        for (Quality grade : GRADES) {
            FluidTank tank = new FluidTank(2000);
            ItemStack full = bucket(grade);
            ItemStack fullBefore = full.copy();
            check(FluidUtil.tryEmptyContainer(full, tank, 1000, null, false).isSuccess(), "simulated transfer failed");
            check(tank.isEmpty() && ItemStack.matches(full, fullBefore), "simulated empty changed tank or bucket");

            tank.fill(fluid(grade), FluidAction.EXECUTE);
            FluidStack tankBefore = tank.getFluid().copy();
            ItemStack empty = new ItemStack(Items.BUCKET);
            ItemStack emptyBefore = empty.copy();
            FluidActionResult result = FluidUtil.tryFillContainer(empty, tank, 1000, null, false);
            check(result.isSuccess(), "simulated tank-to-bucket transfer failed");
            assertBucket(result.getResult(), grade);
            check(tankBefore.isFluidStackIdentical(tank.getFluid()), "simulated fill drained the tank");
            check(ItemStack.matches(empty, emptyBefore), "simulated fill changed its input bucket");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sequentialGradesAndNativeAcceptance(GameTestHelper helper) {
        FluidTank tank = new FluidTank(1000);
        for (Quality grade : new Quality[]{Quality.DIAMOND, Quality.IRON, Quality.NONE}) {
            check(FluidUtil.tryEmptyContainer(bucket(grade), tank, 1000, null, true).isSuccess(), "sequential empty failed");
            FluidActionResult result = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), tank, 1000, null, true);
            check(result.isSuccess(), "sequential fill failed");
            assertBucket(result.getResult(), grade);
            check(tank.isEmpty(), "sequential transfer left fluid behind");
        }

        tank = new FluidTank(2000);
        tank.fill(fluid(Quality.DIAMOND), FluidAction.EXECUTE);
        FluidStack before = tank.getFluid().copy();
        check(tank.fill(fluid(Quality.IRON), FluidAction.EXECUTE) == 0, "native tank accepted different quality NBT");
        check(before.isFluidStackIdentical(tank.getFluid()), "rejected mixing changed the tank");
        IFluidHandlerItem full = handler(bucket(Quality.DIAMOND));
        ItemStack bucketBefore = full.getContainer().copy();
        check(full.drain(fluid(Quality.IRON), FluidAction.EXECUTE).isEmpty(), "bucket ignored native drain matching");
        check(ItemStack.matches(bucketBefore, full.getContainer()), "rejected drain changed the bucket");
        check(handler(new ItemStack(Items.BUCKET)).fill(new FluidStack(Fixtures.FLUID, 999), FluidAction.EXECUTE) == 0,
                "bucket accepted less than its native capacity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unsupportedAndExcludedFluidsKeepNativeBehavior(GameTestHelper helper) {
        FluidStack excluded = new FluidStack(Fixtures.EXCLUDED_FLUID, 1000);
        check(!QualityFoodFluidsApi.canCarryQuality(excluded), "test exclusion tag was not loaded");
        ItemStack template = Fixtures.template(true);
        ItemStack before = template.copy();
        check(FluidUtil.getFilledBucket(excluded) == template, "unsupported fluid result was copied or rewritten");
        check(ItemStack.matches(before, template), "unsupported fluid's native bucket changed");
        check(!QualityFoodFluidsApi.hasQuality(FluidUtil.getFluidContained(template).orElse(FluidStack.EMPTY)),
                "excluded fluid gained quality from its bucket");
        check(FluidUtil.getFilledBucket(FluidStack.EMPTY).isEmpty(), "empty fluid produced a bucket");
        check(FluidUtil.getFluidContained(new ItemStack(Items.BUCKET)).orElse(FluidStack.EMPTY).isEmpty(),
                "empty bucket gained fluid");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void reusableStorageKeepsFluidQualityWithoutItemQuality(GameTestHelper helper) {
        for (Quality grade : GRADES) {
            ItemStack storage = new ItemStack(Fixtures.STORAGE);
            storage.getOrCreateTag().putString("native_metadata", "keep");
            IFluidHandlerItem storageHandler = handler(storage);
            check(storageHandler.fill(fluid(grade), FluidAction.EXECUTE) == 1000, "storage fill failed");
            check(!QualityFoodFluidsApi.isAutomaticallyQualityCapableItem(storage), "fluid capability enabled item quality");
            check(!Utils.isValidItem(storage), "QF accepted storage as a quality item");
            check(!FluidQualityNames.shouldPrefixItemName(storage), "storage gained a quality name");
            ItemStack result = QualityFoodFluidsApi.copyFluidQualityToItem(fluid(grade), storage);
            assertNoItemQuality(result);
            assertFluid(FluidUtil.getFluidContained(result).orElse(FluidStack.EMPTY), grade);
            check("keep".equals(result.getTag().getString("native_metadata")), "native storage data was lost");

            FluidTank tank = new FluidTank(5000);
            check(FluidUtil.tryEmptyContainer(result, tank, 1000, null, true).isSuccess(), "storage-to-tank transfer failed");
            assertFluid(tank.getFluid(), grade);
            FluidActionResult refilled = FluidUtil.tryFillContainer(new ItemStack(Fixtures.STORAGE), tank, 1000, null, true);
            check(refilled.isSuccess(), "tank-to-storage transfer failed");
            assertNoItemQuality(refilled.getResult());
            assertFluid(handler(refilled.getResult()).drain(1000, FluidAction.EXECUTE), grade);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void staleStorageItemQualityCannotChangeStoredFluid(GameTestHelper helper) {
        for (Quality grade : new Quality[]{Quality.NONE, Quality.IRON}) {
            ItemStack storage = new ItemStack(Fixtures.STORAGE);
            handler(storage).fill(fluid(grade), FluidAction.EXECUTE);
            storage.getOrCreateTag().put(QualityUtils.QUALITY_TAG,
                    bucket(Quality.DIAMOND).getTag().get(QualityUtils.QUALITY_TAG).copy());
            ItemStack before = storage.copy();
            assertFluid(FluidUtil.getFluidContained(storage).orElse(FluidStack.EMPTY), grade);
            assertFluid(QualityFoodFluidsApi.copyContainerQualityToFluid(handler(storage), fluid(grade)), grade);
            check(QualityFoodFluidsApi.getContainedFluidQuality(storage) == grade, "stale item tag contaminated storage quality");
            ItemStack cleaned = QualityFoodFluidsApi.applyItemQuality(storage, Quality.DIAMOND);
            assertNoItemQuality(cleaned);
            assertFluid(FluidUtil.getFluidContained(cleaned).orElse(FluidStack.EMPTY), grade);
            ItemStack filled = QualityFoodFluidsCreateRules.applyFillingQuality(fluid(Quality.DIAMOND), storage);
            assertNoItemQuality(filled);
            assertFluid(FluidUtil.getFluidContained(filled).orElse(FluidStack.EMPTY), grade);
            ItemStack output = QualityFoodFluidsCreateRules.applyItemOutputQuality(Quality.DIAMOND, storage);
            assertNoItemQuality(output);
            assertFluid(FluidUtil.getFluidContained(output).orElse(FluidStack.EMPTY), grade);
            check(ItemStack.matches(before, storage), "quality cleanup mutated its input");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void normalTankDoesNotInheritAnotherTanksQuality(GameTestHelper helper) {
        IFluidHandlerItem multiTank = new FluidHandlerItemStack(new ItemStack(Fixtures.STORAGE), 5000) {
            @Override
            public int getTanks() {
                return 2;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return fluid(tank == 0 ? Quality.DIAMOND : Quality.NONE);
            }
        };
        assertFluid(QualityFoodFluidsApi.copyContainerQualityToFluid(multiTank, multiTank.getFluidInTank(1)), Quality.NONE);
        assertFluid(QualityFoodFluidsApi.copyContainerQualityToFluid(multiTank, multiTank.getFluidInTank(0)), Quality.DIAMOND);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void copperBucketKeepsQualityOnlyInItsFluid(GameTestHelper helper) {
        if (!ModList.get().isLoaded("fluidlogistics")) {
            helper.succeed();
            return;
        }

        Item copperBucket = ForgeRegistries.ITEMS.getValue(new ResourceLocation("fluidlogistics", "copper_bucket"));
        check(copperBucket != null && copperBucket != Items.AIR, "copper bucket was not registered");
        for (Quality grade : GRADES) {
            FluidTank source = new FluidTank(5000);
            source.fill(fluid(grade), FluidAction.EXECUTE);
            FluidActionResult filled = FluidUtil.tryFillContainer(new ItemStack(copperBucket), source, 1000, null, true);
            check(filled.isSuccess(), "real copper bucket could not be filled");
            ItemStack copper = filled.getResult();
            check(handler(copper).getTankCapacity(0) == 5000, "unexpected copper bucket handler");
            check(!Utils.isValidItem(copper), "real copper bucket was accepted as a QF item");
            assertNoItemQuality(copper);
            assertFluid(FluidUtil.getFluidContained(copper).orElseThrow(), grade);
            copper = QualityFoodFluidsCreateRules.applyFillingQuality(fluid(grade), copper);
            assertNoItemQuality(copper);
            assertFluid(FluidUtil.getFluidContained(copper).orElseThrow(), grade);
            FluidTank destination = new FluidTank(5000);
            FluidActionResult emptied = FluidUtil.tryEmptyContainer(copper, destination, 1000, null, true);
            check(emptied.isSuccess() && emptied.getResult().is(copperBucket), "copper bucket transfer failed");
            assertNoItemQuality(emptied.getResult());
            assertFluid(destination.getFluid(), grade);
        }
        helper.succeed();
    }

    private static void assertNoItemQuality(ItemStack stack) {
        check(!stack.hasTag() || !stack.getTag().contains(QualityUtils.QUALITY_TAG), "storage retained outer QF quality");
    }

    private static FluidStack fluid(Quality grade) {
        return QualityFoodFluidsApi.withQuality(new FluidStack(Fixtures.FLUID, 1000), grade);
    }

    private static ItemStack bucket(Quality grade) {
        return QualityFoodFluidsApi.applyItemQuality(new ItemStack(Fixtures.BUCKET), grade);
    }

    private static IFluidHandlerItem handler(ItemStack bucket) {
        return bucket.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElseThrow();
    }

    private static void assertFluid(FluidStack fluid, Quality grade) {
        check(!fluid.isEmpty() && fluid.getFluid() == Fixtures.FLUID && fluid.getAmount() == 1000,
                "wrong fluid or amount");
        check(QualityFoodFluidsApi.getQuality(fluid) == grade, "fluid quality differs from " + grade);
    }

    private static void assertBucket(ItemStack bucket, Quality grade) {
        check(bucket.is(Fixtures.BUCKET) && bucket.getCount() == 1, "wrong bucket or count");
        check(QualityUtils.getQuality(bucket) == grade,
                "bucket quality differs from " + grade + ": " + QualityUtils.getQuality(bucket));
        if (grade == Quality.NONE) {
            check(!bucket.hasTag() || !bucket.getTag().contains(QualityUtils.QUALITY_TAG), "normal bucket retained old quality NBT");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // Registered only in the gameTest source set; no fixture IDs or tags enter the shipped mod.
    @Mod.EventBusSubscriber(modid = QualityFoodFluids.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Fixtures {
        private static Fluid FLUID;
        private static Fluid EXCLUDED_FLUID;
        private static Item BUCKET;
        private static Item EXCLUDED_BUCKET;
        private static Item STORAGE;
        private static Item CAP_STORAGE;
        private static FluidType TYPE;
        private static FluidType EXCLUDED_TYPE;
        private static ItemStack bucketTemplate;
        private static ItemStack excludedBucketTemplate;

        @SubscribeEvent
        public static void register(RegisterEvent event) {
            event.register(ForgeRegistries.Keys.FLUID_TYPES, id("bucket_test_type"), () -> {
                TYPE = type(false);
                return TYPE;
            });
            event.register(ForgeRegistries.Keys.FLUID_TYPES, id("bucket_test_excluded_type"), () -> {
                EXCLUDED_TYPE = type(true);
                return EXCLUDED_TYPE;
            });
            event.register(Registries.FLUID, id("bucket_test_fluid"), () -> {
                FLUID = testFluid(false);
                return FLUID;
            });
            event.register(Registries.FLUID, id("bucket_test_excluded_fluid"), () -> {
                EXCLUDED_FLUID = testFluid(true);
                return EXCLUDED_FLUID;
            });
            event.register(Registries.ITEM, id("bucket_test_bucket"), () -> {
                BUCKET = new BucketItem(() -> FLUID, new Item.Properties().stacksTo(1));
                return BUCKET;
            });
            event.register(Registries.ITEM, id("bucket_test_excluded_bucket"), () -> {
                EXCLUDED_BUCKET = new BucketItem(() -> EXCLUDED_FLUID, new Item.Properties().stacksTo(1));
                return EXCLUDED_BUCKET;
            });
            event.register(Registries.ITEM, id("bucket_test_storage"), () -> {
                STORAGE = new Item(new Item.Properties().stacksTo(1)) {
                    @Override
                    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
                        // Same Forge handler and capacity as Fluid Logistics' copper bucket.
                        return new FluidHandlerItemStack(stack, 5000);
                    }
                };
                return STORAGE;
            });
            event.register(Registries.ITEM, id("bucket_test_cap_storage"), () -> {
                CAP_STORAGE = new Item(new Item.Properties().stacksTo(1)) {
                    @Override
                    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
                        return new SerializedStorage(stack);
                    }
                };
                return CAP_STORAGE;
            });
        }

        private static FluidType type(boolean excluded) {
            return new FluidType(FluidType.Properties.create()) {
                @Override
                public ItemStack getBucket(FluidStack fluid) {
                    return template(excluded);
                }
            };
        }

        private static Fluid testFluid(boolean excluded) {
            return new WaterFluid.Source() {
                @Override
                public FluidType getFluidType() {
                    return excluded ? EXCLUDED_TYPE : TYPE;
                }

                @Override
                public Item getBucket() {
                    return excluded ? EXCLUDED_BUCKET : BUCKET;
                }
            };
        }

        private static ItemStack template(boolean excluded) {
            if (excluded) {
                if (excludedBucketTemplate == null) {
                    excludedBucketTemplate = createTemplate(EXCLUDED_BUCKET);
                }
                return excludedBucketTemplate;
            }
            if (bucketTemplate == null) {
                bucketTemplate = createTemplate(BUCKET);
            }
            return bucketTemplate;
        }

        private static ItemStack createTemplate(Item item) {
            ItemStack result = QualityFoodFluidsApi.applyItemQuality(new ItemStack(item), Quality.DIAMOND);
            result.getOrCreateTag().putInt("native_bucket_metadata", 23);
            return result;
        }

        private static ResourceLocation id(String path) {
            return new ResourceLocation(QualityFoodFluids.MODID, path);
        }
    }

    private static final class SerializedStorage extends FluidTank
            implements IFluidHandlerItem, ICapabilitySerializable<CompoundTag> {
        private final ItemStack container;
        private final LazyOptional<IFluidHandlerItem> capability = LazyOptional.of(() -> this);

        private SerializedStorage(ItemStack container) {
            super(5000);
            this.container = container;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == ForgeCapabilities.FLUID_HANDLER_ITEM ? this.capability.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return writeToNBT(new CompoundTag());
        }

        @Override
        public void deserializeNBT(CompoundTag data) {
            readFromNBT(data);
        }
    }
}
