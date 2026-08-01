package org.dasien.more_cfg_for_ae2.compat;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.glodblock.github.extendedae.common.EAESingletons;
import com.glodblock.github.extendedae.common.items.ItemInfinityCell;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.dasien.more_cfg_for_ae2.Config;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class InfinityCellCreativeHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    private InfinityCellCreativeHelper() {
    }

    public static void addConfiguredCells(CreativeModeTab.Output output) {
        for (ItemStack stack : configuredCells()) {
            output.accept(stack);
        }
    }

    public static ItemStack createIcon() {
        List<ItemStack> cells = configuredCells();
        if (!cells.isEmpty()) {
            return cells.get(0);
        }

        ItemInfinityCell infinityCell = EAESingletons.INFINITY_WATER_CELL;
        return infinityCell != null ? new ItemStack(infinityCell) : ItemStack.EMPTY;
    }

    private static List<ItemStack> configuredCells() {
        Config config = Config.get();
        if (!(config instanceof Config.WithExPatternProvider eppConfig)) {
            return List.of();
        }
        // EPP 1.21：无限单元格（水/圆石）通过构造器绑定固定 AEKey，无法再按配置动态创建
        // 自定义记录单元格。此处返回 EPP 内置无限单元格作为可见占位，避免创造标签页为空。
        ItemInfinityCell infinityCell = EAESingletons.INFINITY_WATER_CELL;
        if (infinityCell == null) {
            return List.of();
        }

        return List.of(new ItemStack(infinityCell));
    }

    private static Set<String> uniqueIds(String[] configuredIds) {
        Set<String> ids = new LinkedHashSet<>();
        if (configuredIds == null) {
            return ids;
        }
        for (String configuredId : configuredIds) {
            if (configuredId != null && !configuredId.isBlank()) {
                ids.add(configuredId.trim());
            }
        }
        return ids;
    }

    private static Optional<Item> resolveItem(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return Optional.empty();
        }

        Item item = BuiltInRegistries.ITEM.get(location);
        return item != null && item != Items.AIR ? Optional.of(item) : Optional.empty();
    }

    private static Optional<Fluid> resolveFluid(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return Optional.empty();
        }

        Fluid fluid = BuiltInRegistries.FLUID.get(location);
        return fluid != null && fluid != Fluids.EMPTY ? Optional.of(fluid) : Optional.empty();
    }

    private static Optional<AEKey> resolveCompatKey(InfinityCellRegistryTarget target, String id) {
        if (target == null) {
            return Optional.empty();
        }
        return switch (target) {
            case FE -> LoadedMods.isAppliedFluxLoaded() ? AppFluxInfinityCellCompat.resolve(id) : Optional.empty();
            case MANA -> LoadedMods.isAppliedBotanicsLoaded() ? AppliedBotanicsInfinityCellCompat.resolve(id)
                    : Optional.empty();
            case GAS, INFUSE_TYPE, PIGMENT, SLURRY -> LoadedMods.isAppliedMekanisticsLoaded()
                    ? AppliedMekanisticsInfinityCellCompat.resolve(target, id) : Optional.empty();
            case SOURCE -> LoadedMods.isArsEnergistiqueLoaded() ? ArsEnergistiqueInfinityCellCompat.resolve(id)
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    private static void logInvalidEntries(List<String> invalidEntries) {
        if (!invalidEntries.isEmpty()) {
            LOGGER.warn("Skipping invalid Ex Pattern Provider infinity cell entries: {}", invalidEntries);
        }
    }
}
