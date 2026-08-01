package org.dasien.more_cfg_for_ae2.mixin;

import appeng.client.gui.NumberEntryType;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.AEBaseMenu;
import com.almostreliable.merequester.client.widgets.NumberField;
import com.almostreliable.merequester.mixin.accessors.EditBoxMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.dasien.more_cfg_for_ae2.client.MERequesterNumberInputScreen;
import org.dasien.more_cfg_for_ae2.compat.MERequesterConfigHelper;
import org.dasien.more_cfg_for_ae2.compat.MERequesterNumberFieldBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalLong;
import java.util.function.Consumer;

@Mixin(value = NumberField.class, remap = false)
public abstract class MERequesterNumberFieldMixin implements MERequesterNumberFieldBridge {
    @Shadow
    @Final
    private String name;

    @Shadow
    private NumberEntryType type;

    @Unique
    private Consumer<Long> moreCfgForAe2$submitter;

    @Shadow
    abstract OptionalLong getLongValue();

    @Shadow
    abstract void setLongValue(long value);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void moreCfgForAe2$captureSubmitter(int x, int y, String name, ScreenStyle style, Consumer<Long> submitter,
            CallbackInfo ci) {
        this.moreCfgForAe2$submitter = submitter;
        ((EditBox) (Object) this).setMaxLength(32);
    }

    @Inject(method = "setFocused", at = @At("HEAD"), cancellable = true, remap = false)
    private void moreCfgForAe2$openNumberPopup(boolean focused, CallbackInfo ci) {
        if (!focused || !((EditBoxMixin) this).merequester$isEditable()) {
            return;
        }

        Screen parent = Minecraft.getInstance().screen;
        if (parent instanceof MERequesterNumberInputScreen) {
            return;
        }
        if (!(parent instanceof AEBaseScreen<?> baseScreen)) {
            return;
        }

        @SuppressWarnings("unchecked")
        AEBaseScreen<AEBaseMenu> typedParent = (AEBaseScreen<AEBaseMenu>) baseScreen;
        ((AEBaseScreenAccessor) baseScreen).moreCfgForAe2$switchToScreen(
                new MERequesterNumberInputScreen(typedParent, this));
        ci.cancel();
    }

    @Override
    public String moreCfgForAe2$getName() {
        return this.name;
    }

    @Override
    public NumberEntryType moreCfgForAe2$getType() {
        return this.type;
    }

    @Override
    public long moreCfgForAe2$getValue() {
        return this.getLongValue().orElse(0L);
    }

    @Override
    public long moreCfgForAe2$getLimit() {
        return "batch".equals(this.name)
                ? MERequesterConfigHelper.requestBatchLimit()
                : MERequesterConfigHelper.requestAmountLimit();
    }

    @Override
    public void moreCfgForAe2$setValue(long value) {
        this.setLongValue(value);
    }

    @Override
    public void moreCfgForAe2$submit(long value) {
        if (this.moreCfgForAe2$submitter != null) {
            this.moreCfgForAe2$submitter.accept(value);
        }
    }
}
