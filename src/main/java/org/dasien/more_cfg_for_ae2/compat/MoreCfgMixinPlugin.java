package org.dasien.more_cfg_for_ae2.compat;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class MoreCfgMixinPlugin implements IMixinConfigPlugin {
    private static final Set<String> EX_PATTERN_PROVIDER_MIXINS = Set.of(
            "org.dasien.more_cfg_for_ae2.mixin.ContainerExInterfaceMixin",
            "org.dasien.more_cfg_for_ae2.mixin.CUpdatePageMixin",
            "org.dasien.more_cfg_for_ae2.mixin.GuiExInterfaceMixin",
            "org.dasien.more_cfg_for_ae2.mixin.InfinityCellMixin",
            "org.dasien.more_cfg_for_ae2.mixin.InfinityCellInventoryMixin",
            "org.dasien.more_cfg_for_ae2.mixin.PartOversizeConfigInventoryMixin",
            "org.dasien.more_cfg_for_ae2.mixin.PartOversizeInterfaceMixin",
            "org.dasien.more_cfg_for_ae2.mixin.TileOversizeConfigInventoryMixin",
            "org.dasien.more_cfg_for_ae2.mixin.TileOversizeInterfaceMixin");

    private static final Set<String> ME_REQUESTER_MIXINS = Set.of(
            "org.dasien.more_cfg_for_ae2.mixin.MERequesterRequestMixin",
            "org.dasien.more_cfg_for_ae2.mixin.MERequesterRequestUpdatePacketMixin",
            "org.dasien.more_cfg_for_ae2.mixin.MERequesterNumberFieldMixin",
            "org.dasien.more_cfg_for_ae2.mixin.MERequesterScreenWidgetInitMixin",
            "org.dasien.more_cfg_for_ae2.mixin.VerticalButtonBarMixin");

    private boolean exPatternProviderLoaded;
    private boolean meRequesterLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        this.exPatternProviderLoaded = LoadingModList.get().getModFileById(LoadedMods.EX_PATTERN_PROVIDER) != null;
        this.meRequesterLoaded = LoadingModList.get().getModFileById(LoadedMods.ME_REQUESTER) != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (EX_PATTERN_PROVIDER_MIXINS.contains(mixinClassName)) {
            return this.exPatternProviderLoaded;
        }
        if (ME_REQUESTER_MIXINS.contains(mixinClassName)) {
            return this.meRequesterLoaded;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
