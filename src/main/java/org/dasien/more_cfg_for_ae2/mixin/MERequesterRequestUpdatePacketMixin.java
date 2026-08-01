package org.dasien.more_cfg_for_ae2.mixin;

import com.almostreliable.merequester.network.RequestUpdatePacket;
import org.dasien.more_cfg_for_ae2.compat.MERequesterConfigHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = RequestUpdatePacket.class, remap = false)
public abstract class MERequesterRequestUpdatePacketMixin {
    @ModifyArg(
            method = "handle(Lcom/almostreliable/merequester/network/RequestUpdatePacket;Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/almostreliable/merequester/requester/abstraction/AbstractRequesterMenu;updateRequesterNumbers(JIJJ)V"),
            index = 2)
    private long moreCfgForAe2$clampPacketAmount(long amount) {
        return MERequesterConfigHelper.clampRequestAmount(amount);
    }

    @ModifyArg(
            method = "handle(Lcom/almostreliable/merequester/network/RequestUpdatePacket;Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/almostreliable/merequester/requester/abstraction/AbstractRequesterMenu;updateRequesterNumbers(JIJJ)V"),
            index = 3)
    private long moreCfgForAe2$clampPacketBatch(long batch) {
        return MERequesterConfigHelper.clampRequestBatch(batch);
    }
}
