package org.dasien.more_cfg_for_ae2.mixin;

import appeng.api.stacks.AEKey;
import com.glodblock.github.extendedae.common.inventory.InfinityCellInventory;
import net.minecraft.world.item.ItemStack;
import org.dasien.more_cfg_for_ae2.ModDataComponents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 恢复"自定义无限单元格"：EPP 1.21 的 {@link InfinityCellInventory} 在构造时
 * 通过 ItemInfinityCell.getRecord() 取固定记录（水/圆石）。此 Mixin 允许优先
 * 读取 ItemStack 上的 more_cfg_for_ae2:cell_record 组件，从而让同一个已注册
 * 物品（水单元格）携带任意目标 AEKey 记录。
 */
@Mixin(value = InfinityCellInventory.class, remap = false)
public abstract class InfinityCellInventoryMixin {

    @Shadow
    @Final
    @Mutable
    private AEKey record;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void moreCfgForAe2$useCustomRecord(ItemStack stack, CallbackInfo ci) {
        AEKey custom = stack.getOrDefault(ModDataComponents.CELL_RECORD.get(), null);
        if (custom != null) {
            this.record = custom;
        }
    }
}
