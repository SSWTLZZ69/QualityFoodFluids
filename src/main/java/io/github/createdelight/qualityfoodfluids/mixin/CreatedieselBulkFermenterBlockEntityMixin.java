package io.github.createdelight.qualityfoodfluids.mixin;

import com.jesz.createdieselgenerators.content.bulk_fermenter.BulkFermenterBlockEntity;
import com.jesz.createdieselgenerators.content.bulk_fermenter.BulkFermentingRecipe;
import io.github.createdelight.qualityfoodfluids.internal.BasinProcessingTicket;
import io.github.createdelight.qualityfoodfluids.internal.BasinQualityTicketHolder;
import io.github.createdelight.qualityfoodfluids.internal.BulkFermenterQualityContext;
import io.github.createdelight.qualityfoodfluids.internal.BulkFermenterQualityHooks;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BulkFermenterBlockEntity.class, remap = false)
public abstract class CreatedieselBulkFermenterBlockEntityMixin implements BasinQualityTicketHolder, BulkFermenterQualityHooks {
    @Unique
    private static final String QUALITY_FOOD_FLUIDS$TICKET_KEY = "QualityFoodFluidsTicket";

    @Shadow
    BulkFermentingRecipe currentRecipe;

    @Unique
    private BasinProcessingTicket qualityFoodFluids$ticket;

    @Shadow
    protected abstract void onFluidStackChanged();

    @Inject(method = "startProcessing", at = @At("HEAD"))
    private void qualityFoodFluids$ensureBulkFermentingTicket(CallbackInfo callback) {
        BulkFermenterQualityContext.ensureTicket((BulkFermenterBlockEntity) (Object) this, currentRecipe);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void qualityFoodFluids$readTicket(CompoundTag compound, boolean clientPacket, CallbackInfo callback) {
        qualityFoodFluids$ticket = compound.contains(QUALITY_FOOD_FLUIDS$TICKET_KEY)
                ? BasinProcessingTicket.load(compound.getCompound(QUALITY_FOOD_FLUIDS$TICKET_KEY))
                : null;
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void qualityFoodFluids$writeTicket(CompoundTag compound, boolean clientPacket, CallbackInfo callback) {
        if (qualityFoodFluids$ticket != null) {
            compound.put(QUALITY_FOOD_FLUIDS$TICKET_KEY, qualityFoodFluids$ticket.save());
        }
    }

    @Override
    public BasinProcessingTicket qualityFoodFluids$getTicket() {
        return qualityFoodFluids$ticket;
    }

    @Override
    public void qualityFoodFluids$setTicket(BasinProcessingTicket ticket) {
        qualityFoodFluids$ticket = ticket;
    }

    @Override
    public void qualityFoodFluids$onFluidOutputsChanged() {
        onFluidStackChanged();
    }
}
