package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import io.github.createdelight.qualityfoodfluids.internal.SequencedAssemblyQualityContext;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BeltDeployerCallbacks.class, remap = false)
public abstract class BeltDeployerCallbacksMixin {
    @Inject(method = "activate", at = @At("HEAD"))
    private static void qualityFoodFluids$captureDeployingSource(
            TransportedItemStack transported,
            TransportedItemStackHandlerBehaviour handler,
            DeployerBlockEntity deployer,
            Recipe<? extends Container> recipe,
            CallbackInfo callback
    ) {
        DeployerFakePlayer player = deployer.getPlayer();
        ItemStack heldItem = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        SequencedAssemblyQualityContext.captureDeployingSource(heldItem);
    }

    @Inject(method = "activate", at = @At("RETURN"))
    private static void qualityFoodFluids$clearDeployingSource(
            TransportedItemStack transported,
            TransportedItemStackHandlerBehaviour handler,
            DeployerBlockEntity deployer,
            Recipe<? extends Container> recipe,
            CallbackInfo callback
    ) {
        SequencedAssemblyQualityContext.clearDeployingSource();
    }
}
