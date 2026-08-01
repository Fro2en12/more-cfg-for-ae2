package org.dasien.more_cfg_for_ae2.mixin;

import com.almostreliable.merequester.requester.Request;
import org.dasien.more_cfg_for_ae2.compat.MERequesterConfigHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = Request.class, remap = false)
public abstract class MERequesterRequestMixin {
    @ModifyVariable(method = "updateAmount", at = @At("HEAD"), argsOnly = true)
    private long moreCfgForAe2$clampRequestAmount(long amount) {
        return MERequesterConfigHelper.clampRequestAmount(amount);
    }

    @ModifyVariable(method = "updateBatch", at = @At("HEAD"), argsOnly = true)
    private long moreCfgForAe2$clampRequestBatch(long batch) {
        return MERequesterConfigHelper.clampRequestBatch(batch);
    }
}
