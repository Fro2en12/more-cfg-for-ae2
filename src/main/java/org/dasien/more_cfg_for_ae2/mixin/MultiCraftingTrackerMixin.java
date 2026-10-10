package org.dasien.more_cfg_for_ae2.mixin;

import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.helpers.MultiCraftingTracker;
import org.dasien.more_cfg_for_ae2.compat.ResizableCraftingTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;
import java.util.concurrent.Future;

@Mixin(value = MultiCraftingTracker.class, remap = false)
public abstract class MultiCraftingTrackerMixin implements ResizableCraftingTracker {
    @Shadow
    @Final
    @Mutable
    private int size;

    @Shadow
    private Future<ICraftingPlan>[] jobs;

    @Shadow
    private ICraftingLink[] links;

    @Override
    public int moreCfgForAe2$getSlotCount() {
        return this.size;
    }

    @Override
    public boolean moreCfgForAe2$isBusy(int slot) {
        if (slot < 0 || slot >= this.size) {
            return false;
        }
        return (this.links != null && this.links[slot] != null)
                || (this.jobs != null && this.jobs[slot] != null);
    }

    @Override
    public void moreCfgForAe2$resize(int slotCount) {
        if (slotCount <= 0) {
            throw new IllegalArgumentException("slotCount 必须为正数");
        }
        if (slotCount == this.size) {
            return;
        }

        if (this.jobs != null) {
            this.jobs = Arrays.copyOf(this.jobs, slotCount);
        }
        if (this.links != null) {
            this.links = Arrays.copyOf(this.links, slotCount);
        }
        this.size = slotCount;
    }
}
