package org.dasien.more_cfg_for_ae2.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.dasien.more_cfg_for_ae2.compat.AppFluxInfinityCellCompat;
import org.dasien.more_cfg_for_ae2.compat.AppliedBotanicsInfinityCellCompat;
import org.dasien.more_cfg_for_ae2.compat.AppliedMekanisticsInfinityCellCompat;
import org.dasien.more_cfg_for_ae2.compat.ArsEnergistiqueInfinityCellCompat;
import org.dasien.more_cfg_for_ae2.compat.InfinityCellRegistryTarget;
import org.dasien.more_cfg_for_ae2.compat.LoadedMods;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class RegistryEntryDisplayHelper {
    private static final Set<String> BROKEN_ITEM_RENDERERS = new HashSet<>();

    private RegistryEntryDisplayHelper() {
    }

    public static Component name(InfinityCellRegistryTarget target, String id) {
        if (target == InfinityCellRegistryTarget.ITEM) {
            return itemForId(id).getDescription();
        }
        if (target == InfinityCellRegistryTarget.FLUID) {
            Fluid fluid = fluidForId(id);
            if (fluid == Fluids.EMPTY) {
                return Component.translatable("block.minecraft.barrier");
            }
            return new FluidStack(fluid, 1).getDisplayName();
        }
        return compatName(target, id);
    }

    public static void renderIcon(GuiGraphics graphics, InfinityCellRegistryTarget target, String id, int x, int y) {
        if (target == InfinityCellRegistryTarget.ITEM) {
            renderItemIcon(graphics, id, x, y);
        } else if (target == InfinityCellRegistryTarget.FLUID) {
            renderFluidIcon(graphics, id, x, y);
        } else {
            renderCompatIcon(graphics, target, id, x, y);
        }
    }

    public static Item itemForId(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return Items.BARRIER;
        }
        Item item = BuiltInRegistries.ITEM.get(location);
        return item != null && item != Items.AIR ? item : Items.BARRIER;
    }

    public static Fluid fluidForId(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return Fluids.EMPTY;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(location);
        return fluid != null ? fluid : Fluids.EMPTY;
    }

    public static void renderSlot(GuiGraphics graphics, int x, int y, boolean hovered) {
        graphics.fill(x, y, x + 20, y + 20, hovered ? 0xFFB8B8B8 : 0xFF6F6F6F);
        graphics.fill(x + 1, y + 1, x + 19, y + 19, 0xFF1D1D1D);
        graphics.fill(x + 1, y + 1, x + 19, y + 2, hovered ? 0xFFEEEEEE : 0xFF9A9A9A);
        graphics.fill(x + 1, y + 1, x + 2, y + 19, hovered ? 0xFFEEEEEE : 0xFF9A9A9A);
        graphics.fill(x + 18, y + 1, x + 19, y + 19, 0xFF2D2D2D);
        graphics.fill(x + 1, y + 18, x + 19, y + 19, 0xFF2D2D2D);
    }

    private static void renderItemIcon(GuiGraphics graphics, String id, int x, int y) {
        renderItemFallbackSafe(graphics, id, new ItemStack(itemForId(id)), x, y);
    }

    private static void renderFluidIcon(GuiGraphics graphics, String id, int x, int y) {
        Fluid fluid = fluidForId(id);
        if (fluid == Fluids.EMPTY) {
            renderBarrier(graphics, x, y);
            return;
        }

        FluidStack stack = new FluidStack(fluid, 1);
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation texture = extensions.getStillTexture(stack);
        if (texture == null) {
            renderBarrier(graphics, x, y);
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(texture);
        int tint = extensions.getTintColor(stack);
        float alpha = ((tint >> 24) & 0xFF) / 255.0F;
        if (alpha <= 0.0F) {
            alpha = 1.0F;
        }
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        RenderSystem.setShaderColor(red, green, blue, alpha);
        graphics.blit(x, y, 0, 16, 16, sprite);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static Component compatName(InfinityCellRegistryTarget target, String id) {
        if (target == InfinityCellRegistryTarget.FE && LoadedMods.isAppliedFluxLoaded()) {
            return AppFluxInfinityCellCompat.name(id);
        }
        if (target == InfinityCellRegistryTarget.MANA && LoadedMods.isAppliedBotanicsLoaded()) {
            return AppliedBotanicsInfinityCellCompat.name(id);
        }
        if (isMekanismChemical(target) && LoadedMods.isAppliedMekanisticsLoaded()) {
            return AppliedMekanisticsInfinityCellCompat.name(target, id);
        }
        if (target == InfinityCellRegistryTarget.SOURCE && LoadedMods.isArsEnergistiqueLoaded()) {
            return ArsEnergistiqueInfinityCellCompat.name(id);
        }
        return Component.literal(id);
    }

    private static void renderCompatIcon(GuiGraphics graphics, InfinityCellRegistryTarget target, String id, int x,
                                         int y) {
        Optional<ResourceLocation> texture = Optional.empty();
        int tint = 0xFFFFFFFF;
        if (target == InfinityCellRegistryTarget.FE && LoadedMods.isAppliedFluxLoaded()) {
            texture = AppFluxInfinityCellCompat.icon(id);
        } else if (target == InfinityCellRegistryTarget.MANA && LoadedMods.isAppliedBotanicsLoaded()) {
            renderItemFallbackSafe(graphics, AppliedBotanicsInfinityCellCompat.icon(), x, y);
            return;
        } else if (isMekanismChemical(target) && LoadedMods.isAppliedMekanisticsLoaded()) {
            texture = AppliedMekanisticsInfinityCellCompat.icon(target, id);
            tint = AppliedMekanisticsInfinityCellCompat.tint(target, id);
        } else if (target == InfinityCellRegistryTarget.SOURCE && LoadedMods.isArsEnergistiqueLoaded()) {
            renderItemFallbackSafe(graphics, ArsEnergistiqueInfinityCellCompat.creativeCell(), x, y);
            return;
        }

        if (texture.isPresent()) {
            renderTextureIcon(graphics, texture.get(), tint, x, y);
            return;
        }

        renderBarrier(graphics, x, y);
    }

    private static void renderTextureIcon(GuiGraphics graphics, ResourceLocation texture, int tint, int x, int y) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(texture);
        float alpha = ((tint >> 24) & 0xFF) / 255.0F;
        if (alpha <= 0.0F) {
            alpha = 1.0F;
        }
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        RenderSystem.setShaderColor(red, green, blue, alpha);
        graphics.blit(x, y, 0, 16, 16, sprite);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderBarrier(GuiGraphics graphics, int x, int y) {
        renderItemFallbackSafe(graphics, "minecraft:barrier", new ItemStack(Items.BARRIER), x, y);
    }

    private static void renderItemFallbackSafe(GuiGraphics graphics, ItemStack stack, int x, int y) {
        renderItemFallbackSafe(graphics, BuiltInItemId.of(stack), stack, x, y);
    }

    private static void renderItemFallbackSafe(GuiGraphics graphics, String rendererKey, ItemStack stack, int x, int y) {
        if (BROKEN_ITEM_RENDERERS.contains(rendererKey)) {
            renderSimpleFallback(graphics, x, y);
            return;
        }
        boolean rendered = renderItemSafely(graphics, stack, x, y);
        if (!rendered) {
            BROKEN_ITEM_RENDERERS.add(rendererKey);
            renderSimpleFallback(graphics, x, y);
        }
    }

    private static boolean renderItemSafely(GuiGraphics graphics, ItemStack stack, int x, int y) {
        try {
            graphics.flush();
            graphics.pose().pushPose();
            graphics.renderItem(stack, x, y);
            graphics.pose().popPose();
            graphics.flush();
            restoreGuiRenderState(graphics);
            return true;
        } catch (RuntimeException ignored) {
            restoreGuiRenderState(graphics);
            return false;
        }
    }

    private static void restoreGuiRenderState(GuiGraphics graphics) {
        while (!graphics.pose().clear()) {
            graphics.pose().popPose();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
    }

    private static void renderSimpleFallback(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 2, y + 2, x + 14, y + 14, 0xFF7A2A2A);
        graphics.fill(x + 4, y + 4, x + 12, y + 12, 0xFF2A1515);
        graphics.fill(x + 7, y + 5, x + 9, y + 11, 0xFFE0E0E0);
        graphics.fill(x + 7, y + 12, x + 9, y + 14, 0xFFE0E0E0);
    }

    private static final class BuiltInItemId {
        private BuiltInItemId() {
        }

        static String of(ItemStack stack) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id != null ? id.toString() : "minecraft:barrier";
        }
    }

    private static boolean isMekanismChemical(InfinityCellRegistryTarget target) {
        return target == InfinityCellRegistryTarget.GAS
                || target == InfinityCellRegistryTarget.INFUSE_TYPE
                || target == InfinityCellRegistryTarget.PIGMENT
                || target == InfinityCellRegistryTarget.SLURRY;
    }
}
