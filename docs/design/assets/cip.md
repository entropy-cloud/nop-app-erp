# 在建工程转固（CIP — Construction In Progress）

## 目的

在建工程（CIP）核算自建或委托建造的固定资产在达到预定可使用状态前的全部支出。CIP 是"资产形成中"的中间会计状态，完工后转固定资产并开始计提折旧。

## 边界

- 本模块负责：CIP 资产卡片、建设成本归集、利息资本化、完工转固、部分转固。
- 本模块不负责：资产类别/折旧方法配置（assets/README.md）；科目表（master-data 域）。
- ORM 实体见 `model/app-erp-assets.orm.xml`（ErpAstCip、ErpAstCipCostItem、ErpAstCipProgressBilling 等）。

## 流程

```
CIP 资产创建（DRAFT）
  ├─ 资产名称、预计转固日期、预算金额
  └─ 资产类别（CIP 类别 → 转固后类别）
       │
       ▼
建设阶段（IN_CONSTRUCTION）
  │
  ├─ 成本归集
  │   ├─ 工程采购 → 采购单据标注 projectId + cipAssetId
  │   ├─ 人工成本 → 工时单归集（关联项目）
  │   ├─ 外部服务 → 服务采购发票归集
  │   └─ 利息资本化 → 专项借款利息按资本化率计入 CIP 成本
  │
  ├─ 进度付款
  │   └─ ErpAstCipProgressBilling：工程进度款，按合同里程碑付款
  │        └─ 进度款本身不转固，只作为已付工程款记录
  │
  └─ 成本归集至 ErpAstCipCostItem（每笔归集一行）
       │
       ▼
完工转固（TRANSFERRED）
  ├─ 汇总全部 ErpAstCipCostItem → 转固总成本
  ├─ 创建固定资产卡片（ErpAstAsset）
  │   ├─ 原值 = 转固总成本
  │   ├─ 折旧方法 = CIP 资产类别配置的默认方法
  │   ├─ 投入使用日期 = 转固日期
  │   └─ 预计净残值 = 按资产类别比例计算
  ├─ 生成转固凭证（借：固定资产 贷：在建工程）
  └─ 可部分转固（一栋楼部分完工先行转固）
```

## 部分转固

大型建设工程（如整条生产线）分阶段完工时，已完工部分可先行转固：

1. 选择部分 ErpAstCipCostItem → 按选择项汇总转固成本。
2. 剩余未转固部分继续留在 CIP 卡片中（状态 IN_CONSTRUCTION）。
3. 全部转固后 CIP 卡片状态 → TRANSFERRED。

## 利息资本化规则

| 规则 | 说明 |
|------|------|
| 资本化期间 | CIP 建设期间，转固后停止 |
| 资本化率 | 专项借款利率；一般借款用加权平均利率 |
| 计算方式 | 累计支出加权平均数 × 资本化率 |
| 上限 | 不超过当期实际借款利息总额 |

## 关键业务规则

1. **CIP 与项目的关系**：一个 CIP 资产关联一个项目（`ErpPrjProject`）。项目下的工程采购/服务采购自动归集到 CIP 成本。非工程类支出（如管理费用）不归入 CIP。
2. **CIP 与采购的关系**：采购单据行可标注 `cipAssetId`，审核过账时自动写入 ErpAstCipCostItem。
3. **转固时点**：达到预定可使用状态（非以竣工决算为准）。已投入使用但决算未完成的，按暂估价值转固，决算后调整。
4. **转固后折旧**：转固次月开始计提折旧。

## 配置选项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| 利息资本化启用 | false | false 时不计算利息资本化（自动计提引擎落地前默认关闭） |
| 转固触发方式 | MANUAL | MANUAL（手动转固）/ AUTO（自动检测完工） |

## 实现注记（计划 0930-1）

- **状态字典 Decision**：CIP 三态状态机使用独立字典 `erp-ast/cip-status`（DRAFT/IN_CONSTRUCTION/TRANSFERRED），不复用 `erp-ast/asset-status`（IN_SERVICE/IDLE/SCRAPPED/SOLD 语义错位）。
- **终态命名 Decision**：使用 `TRANSFERRED`（已完工转固）而非 `COMPLETED`，语义精准——CIP 资产已转出为固定资产，区别于"完工但未转固"。
- **利息资本化 config 默认值 Decision**：默认 `false`（非设计文档的 `true`）。自动计提引擎落地前关闭，避免业务方在无计算引擎时误用。触发条件：专项借款利息管理需求启动。
- **转固路径 Decision**：CIP 转固复用既有 `IErpAstAssetCapitalizationBiz` 审批链（sourceType=CIP(20)），由 Capitalization 单审批通过后建卡 + 出 CAPITALIZATION(80) 凭证。CIP 不重复实现建卡/过账逻辑（DRY）。

### Non-Goals（本期未实现，后继承接）

- **采购单据自动归集**：跨域 hook（purchase→assets），本期仅提供显式 `addCostItem` 入口。触发条件：采购单据行 `cipAssetId` 字段落地时。
- **人工工时自动归集**：工时单关联项目（projects）再分摊到 CIP 属跨域多步编排。触发条件：项目工时分摊到 CIP 业务上线时。
- **利息资本化自动计算引擎**：资本化期间 + 资本化率 + 累计支出加权平均数 + 上限，属独立金融服务面。触发条件：专项借款利息管理需求启动时。
- **CIP 与项目（ErpPrjProject）强关联**：本期 CIP 不强引用 projectId（可选弱引用列已加）。触发条件：项目结算转固与 CIP 双通道业务上线时。
- **AUTO 完工检测**：本期仅 MANUAL，自动检测完工规则归后继。触发条件：自动完工检测规则上线时。
- **部分红冲（部分 reverseTransfer）**：本期仅支持全部红冲。触发条件：部分红冲业务需求上线时。
