package org.dasien.more_cfg_for_ae2.mixin;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.InterfaceLogicHost;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.SetStockAmountMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.dasien.more_cfg_for_ae2.compat.LongStockAmountMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = SetStockAmountMenu.class, remap = false)
public abstract class SetStockAmountMenuMixin extends AEBaseMenu implements LongStockAmountMenu {
    @Unique
    private static final String MORE_CFG_FOR_AE2_ACTION_SET_STOCK_AMOUNT_LONG =
            "moreCfgForAe2SetStockAmountLong";

    @Shadow
    @Final
    private InterfaceLogicHost host;

    @Shadow
    private AEKey whatToStock;

    @Shadow
    private int slot;

    @GuiSync(3)
    @Unique
    private long moreCfgForAe2$initialAmount = -1L;

    @GuiSync(4)
    @Unique
    private long moreCfgForAe2$maxAmount = -1L;

    protected SetStockAmountMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void moreCfgForAe2$registerLongStockAmountAction(int id, Inventory playerInventory, InterfaceLogicHost host,
            CallbackInfo ci) {
        this.registerClientAction(MORE_CFG_FOR_AE2_ACTION_SET_STOCK_AMOUNT_LONG, Long.class,
                this::moreCfgForAe2$confirm);
    }

    @Inject(method = "setWhatToStock", at = @At("RETURN"))
    private void moreCfgForAe2$syncLongAmounts(int slot, AEKey whatToStock, int initialAmount, CallbackInfo ci) {
        GenericStackInv config = this.host.getConfig();
        GenericStack stack = config.getStack(slot);
        this.moreCfgForAe2$initialAmount = stack != null && Objects.equals(stack.what(), whatToStock)
                ? stack.amount()
                : Math.max(0L, initialAmount);
        this.moreCfgForAe2$maxAmount = config.getMaxAmount(whatToStock);
    }

    @Override
    public long moreCfgForAe2$getInitialAmount() {
        return this.moreCfgForAe2$initialAmount;
    }

    @Override
    public long moreCfgForAe2$getMaxAmount() {
        return this.moreCfgForAe2$maxAmount;
    }

    @Override
    public void moreCfgForAe2$confirm(long amount) {
        if (this.isClientSide()) {
            this.sendClientAction(MORE_CFG_FOR_AE2_ACTION_SET_STOCK_AMOUNT_LONG, amount);
            return;
        }

        GenericStackInv config = this.host.getConfig();
        if (!Objects.equals(config.getKey(this.slot), this.whatToStock)) {
            this.host.returnToMainMenu(this.getPlayer(), (ISubMenu) this);
            return;
        }

        amount = Math.min(amount, config.getMaxAmount(this.whatToStock));
        if (amount <= 0) {
            config.setStack(this.slot, null);
        } else {
            config.setStack(this.slot, new GenericStack(this.whatToStock, amount));
        }
        this.host.returnToMainMenu(this.getPlayer(), (ISubMenu) this);
    }
}
