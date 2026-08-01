package org.dasien.more_cfg_for_ae2.mixin;

import appeng.api.stacks.AEKey;
import com.glodblock.github.extendedae.common.items.ItemInfinityCell;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dasien.more_cfg_for_ae2.Config;
import org.dasien.more_cfg_for_ae2.ModDataComponents;
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

    /** 自定义记录单元格显示记录目标名称，而非固定的"水单元格" */
    @Inject(method = "getName(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/chat/Component;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void moreCfgForAe2$customRecordName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        AEKey custom = stack.getOrDefault(ModDataComponents.CELL_RECORD.get(), null);
        if (custom != null) {
            cir.setReturnValue(Component.translatable("item.more_cfg_for_ae2.infinity_cell")
                    .append(" (").append(custom.getDisplayName()).append(")"));
        }
    }
}
