package io.github.createdelight.qualityfoodfluids;

import com.mojang.logging.LogUtils;
import io.github.createdelight.qualityfoodfluids.network.QualityFoodFluidsNetwork;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(QualityFoodFluids.MODID)
public final class QualityFoodFluids {
    public static final String MODID = "quality_food_fluids";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QualityFoodFluids() {
        QualityFoodFluidsNetwork.register();
        LOGGER.info("Quality Food Fluids loaded");
    }
}
