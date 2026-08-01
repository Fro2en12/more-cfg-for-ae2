package org.dasien.more_cfg_for_ae2.mixin;

import dev.toma.configuration.client.screen.ArrayConfigScreen;
import dev.toma.configuration.client.widget.ConfigEntryWidget;
import dev.toma.configuration.client.widget.EditBoxWidget;
import dev.toma.configuration.config.value.AbstractArrayValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.dasien.more_cfg_for_ae2.client.InfinityCellRegistrySelectionScreen;
import org.dasien.more_cfg_for_ae2.client.RegistryTargetIconButton;
import org.dasien.more_cfg_for_ae2.compat.InfinityCellRegistryTarget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArrayConfigScreen.class, remap = false)
public abstract class ArrayConfigScreenMixin {
    @Shadow
    @Final
    public AbstractArrayValue<?> array;

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void moreCfgForAe2$addRegistryPickers(CallbackInfo ci) {
        if (!InfinityCellRegistryTarget.CONFIG_KEY.equals(this.array.getId())) {
            return;
        }

        Object value = this.array.get();
        if (!(value instanceof String[] ids)) {
            return;
        }

        int visibleIndex = 0;
        for (GuiEventListener listener : ((ArrayConfigScreen<?, ?>) (Object) this).children()) {
            if (!(listener instanceof ConfigEntryWidget entryWidget)) {
                continue;
            }
            EditBoxWidget editBox = findEditBox(entryWidget);
            if (editBox == null) {
                continue;
            }

            int arrayIndex = indexFromLabel(entryWidget.getComponentName().getString());
            if (arrayIndex < 0) {
                arrayIndex = ((AbstractConfigScreenAccessor) this).moreCfgForAe2$getIndex() + visibleIndex;
            }
            if (arrayIndex < 0 || arrayIndex >= ids.length) {
                continue;
            }

            int capturedIndex = arrayIndex;
            String currentId = ids[capturedIndex];
            InfinityCellRegistryTarget target = InfinityCellRegistryTarget.fromEntry(currentId);
            if (target == null) {
                target = InfinityCellRegistryTarget.ITEM;
            }
            InfinityCellRegistryTarget capturedTarget = target;
            RegistryTargetIconButton button = new RegistryTargetIconButton(editBox.getX() - 22, editBox.getY(), target,
                    currentId, pressed -> Minecraft.getInstance().setScreen(new InfinityCellRegistrySelectionScreen(
                            (ArrayConfigScreen<?, ?>) (Object) this, capturedTarget,
                            selectedId -> moreCfgForAe2$setArrayValue(capturedIndex, selectedId))));
            entryWidget.addRenderableWidget(button);
            visibleIndex++;
        }
    }

    private void moreCfgForAe2$setArrayValue(int index, String selectedId) {
        Object value = this.array.get();
        if (!(value instanceof String[] ids) || index < 0 || index >= ids.length) {
            return;
        }

        String[] updated = new String[ids.length];
        System.arraycopy(ids, 0, updated, 0, ids.length);
        updated[index] = selectedId;
        ((AbstractArrayValue<String>) this.array).setValue(updated);
        ArrayConfigScreen<?, ?> screen = (ArrayConfigScreen<?, ?>) (Object) this;
        screen.init(Minecraft.getInstance(), screen.width, screen.height);
    }

    private static EditBoxWidget findEditBox(ConfigEntryWidget entryWidget) {
        for (GuiEventListener child : entryWidget.children()) {
            if (child instanceof EditBoxWidget editBox) {
                return editBox;
            }
        }
        return null;
    }

    private static int indexFromLabel(String label) {
        int colon = label.indexOf(':');
        if (colon <= 0) {
            return -1;
        }
        try {
            return Integer.parseInt(label.substring(0, colon).trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
