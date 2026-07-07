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
            return LoadingModList.get().getModFileById("brewinandchewin") != null;
        }

        if (mixinClassName.endsWith("CreatedieselBulkFermenterBlockEntityMixin")
                || mixinClassName.endsWith("CreatedieselBulkFermentingRecipeMixin")) {
            return LoadingModList.get().getModFileById("createdieselgenerators") != null;
        }

        if (mixinClassName.endsWith("FarmersRespiteKettleBlockEntityMixin")) {
            return LoadingModList.get().getModFileById("farmersrespite") != null;
        }

        return true;
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
