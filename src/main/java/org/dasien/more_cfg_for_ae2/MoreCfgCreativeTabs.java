package org.dasien.more_cfg_for_ae2;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.dasien.more_cfg_for_ae2.compat.InfinityCellCreativeHelper;
import org.dasien.more_cfg_for_ae2.compat.LoadedMods;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MoreCfgCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            More_cfg_for_ae2.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INFINITY_CELLS = TABS.register("infinity_cells",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.more_cfg_for_ae2.infinity_cells"))
                    .icon(MoreCfgCreativeTabs::infinityCellsIcon)
                    .displayItems((parameters, output) -> {
                        if (LoadedMods.isExPatternProviderLoaded()) {
                            InfinityCellCreativeHelper.addConfiguredCells(output);
                        }
                    })
                    .build());

    private MoreCfgCreativeTabs() {
    }

    private static ItemStack infinityCellsIcon() {
        if (LoadedMods.isExPatternProviderLoaded()) {
            ItemStack icon = InfinityCellCreativeHelper.createIcon();
            if (!icon.isEmpty()) {
                return icon;
            }
        }
        return new ItemStack(Items.BARRIER);
    }
}
