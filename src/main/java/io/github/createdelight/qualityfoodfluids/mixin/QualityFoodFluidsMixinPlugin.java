package io.github.createdelight.qualityfoodfluids.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class QualityFoodFluidsMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("BrewinKegBlockEntityMixin")) {
            return isLoaded("brewinandchewin");
        }

        if (mixinClassName.endsWith("CreatedieselBulkFermenterBlockEntityMixin")
                || mixinClassName.endsWith("CreatedieselBulkFermentingRecipeMixin")) {
            return isLoaded("create") && isLoaded("createdieselgenerators");
        }

        if (mixinClassName.endsWith("FarmersRespiteKettleBlockEntityMixin")) {
            return isLoaded("farmersrespite");
        }

        if (isCreateMixin(mixinClassName)) {
            return isLoaded("create");
        }

        return true;
    }

    private static boolean isLoaded(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    private static boolean isCreateMixin(String mixinClassName) {
        return mixinClassName.endsWith("BasinBlockEntityMixin")
                || mixinClassName.endsWith("BasinOperatingBlockEntityMixin")
                || mixinClassName.endsWith("BasinRecipeMixin")
                || mixinClassName.endsWith("BeltDeployerCallbacksMixin")
                || mixinClassName.endsWith("DeployerHandlerMixin")
                || mixinClassName.endsWith("FillingBySpoutMixin")
                || mixinClassName.endsWith("FluidDrainingBehaviourMixin")
                || mixinClassName.endsWith("FluidFillingBehaviourMixin")
                || mixinClassName.endsWith("FluidHelperMixin")
                || mixinClassName.endsWith("GenericItemEmptyingMixin")
                || mixinClassName.endsWith("HosePulleyFluidHandlerMixin")
                || mixinClassName.endsWith("OpenEndedPipeMixin")
                || mixinClassName.endsWith("RecipeApplierMixin")
                || mixinClassName.endsWith("SequencedAssemblyRecipeMixin");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
