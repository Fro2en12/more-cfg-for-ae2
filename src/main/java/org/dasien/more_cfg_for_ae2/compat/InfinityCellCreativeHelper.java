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
import org.dasien.more_cfg_for_ae2.ModDataComponents;
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
        ItemInfinityCell infinityCell = EAESingletons.INFINITY_WATER_CELL;
        if (infinityCell == null) {
            return List.of();
        }

        // EPP 1.21 移除了 getRecordCell(AEKey)：单元格改为构造器绑定固定 AEKey。
        // 此处以已注册的水单元格为载体，把目标记录写入自定义组件 cell_record，
        // 由 InfinityCellInventoryMixin 在构造时优先读取，恢复动态自定义无限单元格。
        List<ItemStack> cells = new ArrayList<>();
        List<String> invalidEntries = new ArrayList<>();

        for (String entry : uniqueIds(eppConfig.infinityCells)) {
            InfinityCellRegistryTarget target = InfinityCellRegistryTarget.fromEntry(entry);
            String id = InfinityCellRegistryTarget.registryId(entry);
            if (target == InfinityCellRegistryTarget.ITEM) {
                Optional<Item> item = resolveItem(id);
                if (item.isPresent()) {
                    cells.add(recordCell(infinityCell, AEItemKey.of(item.get())));
                } else {
                    invalidEntries.add(entry);
                }
            } else if (target == InfinityCellRegistryTarget.FLUID) {
                Optional<Fluid> fluid = resolveFluid(id);
                if (fluid.isPresent()) {
                    cells.add(recordCell(infinityCell, AEFluidKey.of(fluid.get())));
                } else {
                    invalidEntries.add(entry);
                }
            } else {
                Optional<AEKey> key = resolveCompatKey(target, id);
                if (key.isPresent()) {
                    cells.add(recordCell(infinityCell, key.get()));
                } else {
                    invalidEntries.add(entry);
                }
            }
        }

        logInvalidEntries(invalidEntries);
        return cells;
    }

    /** 创建记录指定 AEKey 的自定义无限单元格（记录写入 cell_record 组件） */
    private static ItemStack recordCell(ItemInfinityCell cell, AEKey key) {
        ItemStack stack = new ItemStack(cell);
        stack.set(ModDataComponents.CELL_RECORD.get(), key);
        return stack;
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
