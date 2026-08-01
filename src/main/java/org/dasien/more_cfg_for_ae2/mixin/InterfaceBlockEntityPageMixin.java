package org.dasien.more_cfg_for_ae2.mixin;

import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.blockentity.misc.InterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.dasien.more_cfg_for_ae2.compat.ConfigurableInterfaceHost;
import org.dasien.more_cfg_for_ae2.compat.InterfaceConfigHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InterfaceBlockEntity.class, remap = false)
public abstract class InterfaceBlockEntityPageMixin extends AENetworkedBlockEntity implements ConfigurableInterfaceHost {
    @Unique
    private int moreCfgForAe2$page;

    protected InterfaceBlockEntityPageMixin(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
    }

    @Override
    public void moreCfgForAe2$setPage(int page) {
        this.moreCfgForAe2$page = InterfaceConfigHelper.clampPage(this, page);
    }

    @Override
    public int moreCfgForAe2$getPage() {
        return InterfaceConfigHelper.clampPage(this, this.moreCfgForAe2$page);
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"), remap = false)
    private void moreCfgForAe2$savePage(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        tag.putInt("more_cfg_for_ae2_page", this.moreCfgForAe2$page);
    }

    @Inject(method = "loadTag", at = @At("RETURN"))
    private void moreCfgForAe2$loadPage(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        this.moreCfgForAe2$setPage(tag.getInt("more_cfg_for_ae2_page"));
    }
}
