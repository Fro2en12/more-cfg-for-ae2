package org.dasien.more_cfg_for_ae2.compat;

public interface ResizableCraftingTracker {
    int moreCfgForAe2$getSlotCount();

    boolean moreCfgForAe2$isBusy(int slot);

    void moreCfgForAe2$resize(int slotCount);
}
