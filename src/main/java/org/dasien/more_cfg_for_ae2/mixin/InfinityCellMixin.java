package org.dasien.more_cfg_for_ae2.mixin;

import appeng.api.stacks.AEKey;
import com.glodblock.github.extendedae.common.items.ItemInfinityCell;
import org.dasien.more_cfg_for_ae2.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemInfinityCell.class, remap = false)
public abstract class InfinityCellMixin {
    @Inject(method = "getAsIntMax", at = @At("HEAD"), cancellable = true)
    private static void moreCfgForAe2$useLongMaxValue(AEKey key, CallbackInfoReturnable<Long> cir) {
        Config config = Config.get();
        if (config instanceof Config.WithExPatternProvider eppConfig && eppConfig.infinityCellUseLongMaxValue) {
            cir.setReturnValue(Long.MAX_VALUE);
        }
    }
}
