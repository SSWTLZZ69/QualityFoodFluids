package io.github.createdelight.qualityfoodfluids.mixin;

import io.github.createdelight.qualityfoodfluids.util.FluidQualityNames;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import snownee.jade.addon.universal.FluidStorageProvider;
import snownee.jade.api.ui.IDisplayHelper;

@Mixin(value = FluidStorageProvider.class, remap = false)
public abstract class JadeFluidStorageProviderMixin {
    @Dynamic("Jade's synthetic lambda is present in bytecode but hidden from javac's element model.")
    @Redirect(
            method = "lambda$append$0(ZLsnownee/jade/api/Accessor;Lsnownee/jade/api/ui/IElementHelper;Lsnownee/jade/api/ITooltip;Lsnownee/jade/api/view/ClientViewGroup;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lsnownee/jade/api/ui/IDisplayHelper;stripColor(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            require = 2
    )
    private static MutableComponent qualityFoodFluids$preservePrefixColor(IDisplayHelper helper, Component name) {
        if (name.getContents() instanceof TranslatableContents contents
                && FluidQualityNames.WITH_QUALITY.equals(contents.getKey())
                && contents.getArgs().length == 2
                && contents.getArgs()[0] instanceof Component prefix
                && contents.getArgs()[1] instanceof Component originalName) {
            // Jade strips all name colors in both display modes. Keep the QFF
            // prefix's explicit color while applying Jade's rules to the base name.
            return Component.translatable(FluidQualityNames.WITH_QUALITY,
                    prefix.copy(), helper.stripColor(originalName));
        }

        return helper.stripColor(name);
    }
}
