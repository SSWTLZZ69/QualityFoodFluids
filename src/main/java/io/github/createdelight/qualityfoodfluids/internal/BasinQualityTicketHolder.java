package io.github.createdelight.qualityfoodfluids.internal;

public interface BasinQualityTicketHolder {
    BasinProcessingTicket qualityFoodFluids$getTicket();

    void qualityFoodFluids$setTicket(BasinProcessingTicket ticket);

    default void qualityFoodFluids$clearTicket() {
        qualityFoodFluids$setTicket(null);
    }
}
