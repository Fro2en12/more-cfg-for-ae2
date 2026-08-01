package org.dasien.more_cfg_for_ae2.compat;

import appeng.api.stacks.AEKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Applied Mekanistics 兼容（Mekanism 10.x / 1.21）。
 * <p>
 * Mekanism 1.21 将气体/灌注/颜料/泥浆统一为单一 Chemical 注册表
 * （MekanismAPI.CHEMICAL_REGISTRY），不再按类型分注册表。
 * 此处统一列出全部化学物；target 参数保留仅为兼容旧调用。
 */
public final class AppliedMekanisticsInfinityCellCompat {
    private AppliedMekanisticsInfinityCellCompat() {
    }

    public static Optional<AEKey> resolve(InfinityCellRegistryTarget target, String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return Optional.empty();
        }
        return createStack(location).map(MekanismKey::of);
    }

    public static List<InfinityCellRegistryEntry> entries(InfinityCellRegistryTarget target) {
        List<InfinityCellRegistryEntry> entries = new ArrayList<>();
        MekanismAPI.CHEMICAL_REGISTRY.forEach(chemical -> addEntry(entries, target, chemical));
        return entries;
    }

    public static Component name(InfinityCellRegistryTarget target, String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return Component.literal(id);
        }
        return chemical(location).map(Chemical::getTextComponent).orElse(Component.literal(id));
    }

    public static Optional<ResourceLocation> icon(InfinityCellRegistryTarget target, String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? Optional.empty() : chemical(location).map(Chemical::getIcon);
    }

    public static int tint(InfinityCellRegistryTarget target, String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? 0xFFFFFFFF : chemical(location).map(Chemical::getTint).orElse(0xFFFFFFFF);
    }

    private static Optional<ChemicalStack> createStack(ResourceLocation id) {
        return chemical(id).map(chemical -> new ChemicalStack(chemical, 1));
    }

    private static Optional<Chemical> chemical(ResourceLocation id) {
        Chemical chemical = MekanismAPI.CHEMICAL_REGISTRY.get(id);
        return chemical != null && !chemical.isEmptyType() ? Optional.of(chemical) : Optional.empty();
    }

    private static void addEntry(List<InfinityCellRegistryEntry> entries, InfinityCellRegistryTarget target,
                                 Chemical chemical) {
        if (chemical.isEmptyType()) {
            return;
        }
        ResourceLocation id = MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical);
        if (id == null) {
            return;
        }
        entries.add(new InfinityCellRegistryEntry(target, id.toString(), chemical.getTextComponent()));
    }
}
