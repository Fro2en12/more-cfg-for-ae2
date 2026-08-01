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
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.world.item.Item;
import org.dasien.more_cfg_for_ae2.compat.ConfigurableInterfaceLogic;
import org.dasien.more_cfg_for_ae2.compat.InterfaceConfigHelper;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Mixin(value = InterfaceLogic.class, remap = false)
public abstract class InterfaceLogicMixin implements ConfigurableInterfaceLogic {
    @Unique
    private static final Method MORE_CFG_FOR_AE2_IS_BUSY = moreCfgForAe2$findIsBusyMethod();

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
    @Mutable
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
        if (configuredSlotCount == this.config.size() && configuredSlotCount == this.storage.size()) {
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

        this.config = newConfig;
        this.storage = newStorage;
        this.plannedWork = new GenericStack[configuredSlotCount];
        this.craftingTracker = new MultiCraftingTracker((InterfaceLogic) (Object) this, configuredSlotCount);

        InterfaceConfigHelper.applySlotLimit((InterfaceLogic) (Object) this, this.host);
        this.moreCfgForAe2$readConfig();
        this.host.saveChanges();
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

        if (moreCfgForAe2$isBusy(this.craftingTracker, slot)) {
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
    private static Method moreCfgForAe2$findIsBusyMethod() {
        try {
            Method method = MultiCraftingTracker.class.getDeclaredMethod("isBusy", int.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Unique
    private static boolean moreCfgForAe2$isBusy(MultiCraftingTracker tracker, int slot) {
        if (MORE_CFG_FOR_AE2_IS_BUSY == null) {
            return false;
        }
        try {
            return (Boolean) MORE_CFG_FOR_AE2_IS_BUSY.invoke(tracker, slot);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }
}
