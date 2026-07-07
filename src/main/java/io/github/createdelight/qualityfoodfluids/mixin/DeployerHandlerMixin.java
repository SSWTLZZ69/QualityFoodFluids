package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.content.kinetics.deployer.DeployerHandler;
import io.github.createdelight.qualityfoodfluids.internal.SequencedAssemblyQualityContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DeployerHandler.class, remap = false)
public abstract class DeployerHandlerMixin {
    @Inject(method = "activate", at = @At("HEAD"))
    private static void qualityFoodFluids$captureDeployingSource(
            DeployerFakePlayer player,
            Vec3 vec,
            BlockPos pos,
            Vec3 movement,
            @Coerce Object mode,
            CallbackInfo callback
    ) {
        ItemStack heldItem = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        SequencedAssemblyQualityContext.captureDeployingSource(heldItem);
    }

    @Inject(method = "activate", at = @At("RETURN"))
    private static void qualityFoodFluids$clearDeployingSource(
            DeployerFakePlayer player,
            Vec3 vec,
            BlockPos pos,
            Vec3 movement,
            @Coerce Object mode,
            CallbackInfo callback
    ) {
        SequencedAssemblyQualityContext.clearDeployingSource();
    }
}
