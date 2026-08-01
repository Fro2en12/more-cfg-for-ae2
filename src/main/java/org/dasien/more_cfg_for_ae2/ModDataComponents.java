package org.dasien.more_cfg_for_ae2;

import appeng.api.stacks.AEKey;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * More CFG for AE2 自定义数据组件。
 * <p>
 * EPP 1.21 的无限单元格（ItemInfinityCell）在构造时通过 Supplier 绑定固定 AEKey，
 * 不再提供 getRecordCell(AEKey) 动态记录 API。为恢复"记录任意物品/流体的自定义
 * 无限单元格"，此处注册一个 DataComponent 存储目标 AEKey：
 * <ul>
 *   <li>单元格 ItemStack 使用已注册的 ItemInfinityCell（水单元格）作为载体；</li>
 *   <li>记录值写入 {@link #CELL_RECORD} 组件（AEKey 自带 Codec / StreamCodec，支持存档与网络同步）；</li>
 *   <li>配合 {@code InfinityCellInventoryMixin} 在构造时优先读取该组件。</li>
 * </ul>
 */
public final class ModDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(More_cfg_for_ae2.MODID);

    /** 无限单元格的目标记录（AEKey：物品/流体/化学物等） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AEKey>> CELL_RECORD =
            DATA_COMPONENTS.register("cell_record", () ->
                    DataComponentType.<AEKey>builder()
                            .persistent(AEKey.CODEC)
                            .networkSynchronized(AEKey.STREAM_CODEC)
                            .build());

    private ModDataComponents() {
    }
}
