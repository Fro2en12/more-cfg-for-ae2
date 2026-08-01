# More CFG for AE2

> **Fork 来源**：本仓库 fork 自 [Dasien-loop/more-cfg-for-ae2](https://github.com/Dasien-loop/more-cfg-for-ae2)，
> 由 [Fro2en12](https://github.com/Fro2en12) 维护的 1.21.1 迁移版。

More CFG for AE2 是一个 NeoForge 1.21.1 小型兼容与配置增强模组，主要用于把 Applied Energistics 2 及部分联动模组里写死或不方便调整的数值暴露到配置文件和配置界面中。

> **迁移说明**：本分支（`port/1.21.1`）已将原 Forge 1.20.1 代码迁移至 NeoForge 21.1.247，
> 构建与运行需要 JDK 21。受上游 API 变化影响：
> - Ex Pattern Provider 1.21（ExtendedAE 2.2.29）移除了 `getRecordCell(AEKey)` 动态记录 API，
>   已通过自定义 `DataComponent`（`more_cfg_for_ae2:cell_record`）+ Mixin 恢复该功能，
>   单元格以已注册水单元格为载体并携带目标 AEKey 记录（支持存档与网络同步）。
> - Applied Botanics（appbot）依赖未发布的 Botania 454-SNAPSHOT，相关兼容需自行提供运行时环境。
> - Ars Energistique（arseng）需 Ars Nouveau 生态，相关兼容需自行提供运行时环境。

当前重点支持 AE2 接口容量、Ex Pattern Provider 无限元件与 ME Requester 请求数量上限。

## 功能

- 可配置普通 AE2 ME 接口的每槽上限与页数。
- 加载 Ex Pattern Provider 时，额外提供扩展 ME 接口与超大 ME 接口的每槽上限和页数配置。
- 加载 Ex Pattern Provider 时，可通过配置添加物品/流体无限元件，并在独立创造模式物品栏中显示。
- 无限元件注册项支持可视化选择器，可以从已注册物品和流体中选择。
- 加载 Just Enough Characters 时，无限元件选择器搜索可复用其拼音搜索能力。
- 可选择让 Ex Pattern Provider 无限元件使用 `Long.MAX_VALUE` 数值，而不是默认的 `Integer.MAX_VALUE` 级别数值。
- 加载 ME Requester 时，可配置单条请求的请求数量上限和并行数量上限。
- ME Requester 数值输入改为弹窗输入，方便查看和输入大数值。
- 联动 mixin 会按模组加载状态启用，未安装对应联动模组时不会强制加载其类。

## 环境

- Minecraft: `1.21.1`
- NeoForge: `21.1.x`
- Java: `21`
- AE2: `19.x`
- Configuration: `3.1.x`

可选联动：

- Ex Pattern Provider
- ME Requester
- Just Enough Characters

## 配置

配置文件位于：

```text
config/more_cfg_for_ae2.yaml
```

基础配置示例：

```yaml
meInterfaceSlotLimit: 2000000
meInterfacePages: 10
```

加载 Ex Pattern Provider 后会出现额外配置：

```yaml
extendedInterfaceSlotLimit: 2000000
extendedInterfacePages: 20
oversizeInterfaceSlotLimit: 2147483647
oversizeInterfacePages: 20
infinityCells:
  - item:minecraft:cobblestone
  - fluid:minecraft:water
infinityCellUseLongMaxValue: false
```

`infinityCells` 使用带类型前缀的注册名：

- `item:<namespace:path>` 表示物品。
- `fluid:<namespace:path>` 表示流体。

加载 ME Requester 后会出现额外配置：

```yaml
meRequesterRequestAmountLimit: 2147483647
meRequesterRequestBatchLimit: 2147483647
```

这些配置是上限，不是默认填充值。服务端会强制钳制请求数量，避免客户端伪造超过配置的数值。

## 许可

本仓库包含的 `LICENSE` 当前为 MIT License。模组依赖项遵循各自项目的许可。
