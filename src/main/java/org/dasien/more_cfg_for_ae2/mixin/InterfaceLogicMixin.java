package org.dasien.more_cfg_for_ae2.mixin;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.util.IConfigManager;
import appeng.core.definitions.AEItems;
import appeng.helpers.InterfaceLogic;
import appeng.helpers.InterfaceLogicHost;
import appeng.helpers.MultiCraftingTracker;
import appeng.util.ConfigInventory;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.world.item.Item;
import org.dasien.more_cfg_for_ae2.compat.ConfigurableInterfaceLogic;
import org.dasien.more_cfg_for_ae2.compat.InterfaceConfigHelper;
import org.dasien.more_cfg_for_ae2.compat.ResizableCraftingTracker;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(value = InterfaceLogic.class, remap = false)
public abstract class InterfaceLogicMixin implements ConfigurableInterfaceLogic {
    @Unique
    private static final Logger MORE_CFG_FOR_AE2_LOGGER = LogUtils.getLogger();

    @Unique
    private static final Field MORE_CFG_FOR_AE2_LOCAL_INV_HANDLER = moreCfgForAe2$findLocalInvHandlerField();

    @Shadow
    @Final
    protected InterfaceLogicHost host;

    @Shadow
    @Final
    @Mutable
    private ConfigInventory config;

    @Shadow
    @Final
    @Mutable
    private ConfigInventory storage;

    @Shadow
    @Final
    @Mutable
    private GenericStack[] plannedWork;

    @Shadow
    @Final
    private MultiCraftingTracker craftingTracker;

    @Shadow
    @Final
    private IManagedGridNode mainNode;

    @Shadow
    @Final
    protected appeng.api.networking.security.IActionSource interfaceRequestSource;

    @Shadow
    @Final
    private IUpgradeInventory upgrades;

    @Shadow
    public abstract IConfigManager getConfigManager();

    @Invoker("readConfig")
    protected abstract void moreCfgForAe2$readConfig();

    @Invoker("updatePlan")
    protected abstract void moreCfgForAe2$updatePlan(int slot);

    @Invoker("acquireFromNetwork")
    protected abstract boolean moreCfgForAe2$acquireFromNetwork(IEnergyService energy, MEStorage networkStorage,
            int slot, AEKey what, long amount);

    @Invoker("handleCrafting")
    protected abstract boolean moreCfgForAe2$handleCrafting(int slot, AEKey what, long amount);

    @Invoker("onConfigRowChanged")
    protected abstract void moreCfgForAe2$onConfigRowChanged();

    @Invoker("onStorageChanged")
    protected abstract void moreCfgForAe2$onStorageChanged();

    @ModifyVariable(
            method = "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/InterfaceLogicHost;Lnet/minecraft/world/item/Item;I)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true)
    private static int moreCfgForAe2$useConfiguredSlotCount(int originalSlotCount, IManagedGridNode mainNode,
            InterfaceLogicHost host, Item item) {
        return InterfaceConfigHelper.slotCountForHost(host, originalSlotCount);
    }

    @Inject(
            method = "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/InterfaceLogicHost;Lnet/minecraft/world/item/Item;I)V",
            at = @At("RETURN"))
    private void moreCfgForAe2$applyConfiguredSlotLimit(IManagedGridNode mainNode, InterfaceLogicHost host, Item item,
            int slots, CallbackInfo ci) {
        InterfaceConfigHelper.applySlotLimit((InterfaceLogic) (Object) this, this.host);
    }

    @Inject(method = "updateStorage", at = @At("HEAD"), cancellable = true)
    private void moreCfgForAe2$updateStorageWithLongAmounts(CallbackInfoReturnable<Boolean> cir) {
        boolean changed = false;
        for (int slot = 0; slot < this.plannedWork.length; slot++) {
            GenericStack work = this.plannedWork[slot];
            if (work != null) {
                changed = this.moreCfgForAe2$usePlan(slot, work.what(), work.amount()) || changed;
            }
        }
        cir.setReturnValue(changed);
    }

    @Override
    public void moreCfgForAe2$ensureConfiguredSlotCount() {
        int configuredSlotCount = InterfaceConfigHelper.slotCountForHost(this.host, this.config.size());
        ResizableCraftingTracker tracker = (ResizableCraftingTracker) this.craftingTracker;
        if (configuredSlotCount == this.config.size()
                && configuredSlotCount == this.storage.size()
                && configuredSlotCount == this.plannedWork.length
                && configuredSlotCount == tracker.moreCfgForAe2$getSlotCount()) {
            InterfaceConfigHelper.applySlotLimit((InterfaceLogic) (Object) this, this.host);
            return;
        }

        int currentSlotCount = Math.max(
                Math.max(this.config.size(), this.storage.size()),
                Math.max(this.plannedWork.length, tracker.moreCfgForAe2$getSlotCount()));
        if (configuredSlotCount < currentSlotCount
                && !this.moreCfgForAe2$canShrinkTo(configuredSlotCount, tracker)) {
            MORE_CFG_FOR_AE2_LOGGER.warn(
                    "拒绝将 AE2 接口从 {} 个槽位缩减为 {} 个：被移除槽位仍有配置、库存或活动合成任务",
                    currentSlotCount, configuredSlotCount);
            InterfaceConfigHelper.applySlotLimit((InterfaceLogic) (Object) this, this.host);
            return;
        }

        ConfigInventory oldConfig = this.config;
        ConfigInventory oldStorage = this.storage;

        ConfigInventory newConfig = ConfigInventory.configStacks(configuredSlotCount)
                .changeListener(this::moreCfgForAe2$onConfigRowChanged)
                .allowOverstacking(true)
                .build();
        ConfigInventory newStorage = ConfigInventory.storage(configuredSlotCount)
                .changeListener(this::moreCfgForAe2$onStorageChanged)
                .build();

        moreCfgForAe2$copyStacks(oldConfig, newConfig);
        moreCfgForAe2$copyStacks(oldStorage, newStorage);

        if (!this.moreCfgForAe2$resetLocalInventoryHandler()) {
            MORE_CFG_FOR_AE2_LOGGER.error(
                    "无法重置 AE2 接口的 localInvHandler，库存维持 {} 个槽位",
                    currentSlotCount);
            return;
        }

        tracker.moreCfgForAe2$resize(configuredSlotCount);

        this.config = newConfig;
        this.storage = newStorage;
        this.plannedWork = new GenericStack[configuredSlotCount];

        InterfaceConfigHelper.applySlotLimit((InterfaceLogic) (Object) this, this.host);
        this.moreCfgForAe2$readConfig();
        this.host.saveChanges();
    }

    @Unique
    private boolean moreCfgForAe2$canShrinkTo(int slotCount, ResizableCraftingTracker tracker) {
        for (int slot = slotCount; slot < this.config.size(); slot++) {
            if (this.config.getStack(slot) != null) {
                return false;
            }
        }
        for (int slot = slotCount; slot < this.storage.size(); slot++) {
            if (this.storage.getStack(slot) != null) {
                return false;
            }
        }

        for (int slot = slotCount; slot < tracker.moreCfgForAe2$getSlotCount(); slot++) {
            if (tracker.moreCfgForAe2$isBusy(slot)) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private boolean moreCfgForAe2$resetLocalInventoryHandler() {
        if (MORE_CFG_FOR_AE2_LOCAL_INV_HANDLER == null) {
            return false;
        }
        try {
            MORE_CFG_FOR_AE2_LOCAL_INV_HANDLER.set(this, null);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private static void moreCfgForAe2$copyStacks(ConfigInventory source, ConfigInventory target) {
        int copiedSlots = Math.min(source.size(), target.size());
        target.beginBatch();
        for (int slot = 0; slot < copiedSlots; slot++) {
            target.setStack(slot, source.getStack(slot));
        }
        target.endBatchSuppressed();
    }

    @Unique
    private boolean moreCfgForAe2$usePlan(int slot, AEKey what, long amount) {
        boolean changed = this.moreCfgForAe2$tryUsePlan(slot, what, amount);
        if (changed) {
            this.moreCfgForAe2$updatePlan(slot);
        }
        return changed;
    }

    @Unique
    private boolean moreCfgForAe2$tryUsePlan(int slot, AEKey what, long amount) {
        IGrid grid = this.mainNode.getGrid();
        if (grid == null) {
            return false;
        }

        MEStorage networkStorage = grid.getStorageService().getInventory();
        IEnergyService energy = grid.getEnergyService();

        if (amount < 0) {
            long toReturn = -amount;
            GenericStack stored = this.storage.getStack(slot);
            if (!what.matches(stored) || stored.amount() < toReturn) {
                return true;
            }

            long inserted = StorageHelper.poweredInsert(energy, networkStorage, what, toReturn,
                    this.interfaceRequestSource);
            if (inserted > 0) {
                this.storage.extract(slot, what, inserted, Actionable.MODULATE);
                return true;
            }
            return false;
        }

        if (((ResizableCraftingTracker) this.craftingTracker).moreCfgForAe2$isBusy(slot)) {
            return this.moreCfgForAe2$handleCrafting(slot, what, amount);
        }

        if (amount <= 0) {
            return false;
        }

        if (this.storage.insert(slot, what, amount, Actionable.SIMULATE) != amount) {
            return true;
        }
        if (this.moreCfgForAe2$acquireFromNetwork(energy, networkStorage, slot, what, amount)) {
            return true;
        }

        if (this.storage.getStack(slot) == null && this.upgrades.isInstalled(AEItems.FUZZY_CARD)) {
            FuzzyMode fuzzyMode = this.getConfigManager().getSetting(Settings.FUZZY_MODE);
            var fuzzyMatches = grid.getStorageService().getCachedInventory().findFuzzy(what, fuzzyMode);
            for (Object2LongMap.Entry<AEKey> fuzzyMatch : fuzzyMatches) {
                AEKey fuzzyKey = fuzzyMatch.getKey();
                long insertable = this.storage.insert(slot, fuzzyKey, amount, Actionable.SIMULATE);
                if (this.moreCfgForAe2$acquireFromNetwork(energy, networkStorage, slot, fuzzyKey, insertable)) {
                    return true;
                }
            }
        }

        return this.moreCfgForAe2$handleCrafting(slot, what, amount);
    }

    @Unique
    private static Field moreCfgForAe2$findLocalInvHandlerField() {
        try {
            Field field = InterfaceLogic.class.getDeclaredField("localInvHandler");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
