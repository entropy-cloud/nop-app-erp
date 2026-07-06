# 资产拆分与合并（Asset Split / Merge）

## 目的

资产拆分：一项固定资产拆分为多项（如一台设备拆为多个组件独立核算）。资产合并：多项固定资产合并为一项（如多台设备组装为一条生产线）。拆分/合并涉及**账面价值分摊**和**折旧历史延续**。

## 边界

- 本模块负责：拆分/合并的申请、审批、价值分摊计算、折旧历史拆分、新卡片创建。
- 本模块不负责：折旧计提（按新卡片各自的折旧计划执行）；凭证生成（finance 域）。
- ORM 实体见 `model/app-erp-assets.orm.xml`。**实现注记（计划 0930-2）**：owner doc 早期文字引用
  `ErpAstAssetSplitMerge` / `ErpAstAssetSplitMergeLine` 单头单行聚合体命名，实际建模采用**分离实体**
  `ErpAstSplit`（拆分单头）+ `ErpAstMerge`（合并单头）+ `ErpAstSplitLine`（拆分目标行）+
  `ErpAstMergeLine`（合并源资产行）。分离建模理由：拆分=1源→N目标（头-行），合并=N源→1目标（行-头），
  两流程方向相反，强行共用单聚合体会导致行实体字段冗余（targetAssetId 与 sourceAssetId 二选一）且
  状态机分支复杂化；分离后两实体各自字段集与状态机清晰，业财过账类型也可独立区分（ASSET_SPLIT/ASSET_MERGE）。

## 资产拆分流程

```
拆分申请（DRAFT）
  ├─ 源资产（ErpAstAsset）→ 一个
  ├─ 拆分方式
  │   ├─ 按价值比例（proportional）：按指定比例分配原值/累计折旧/净值
  │   └─ 按固定金额（fixed_amount）：指定各拆分项的固定价值
  └─ 目标资产列表（每条一个 ErpAstAssetSplitMergeLine）
       │
       ▼（审批 → APPROVED）
  执行拆分
       │
       ├─ 源资产状态 → DISPOSED（处置），账面净值归零
       │
       ├─ 创建 N 个新资产卡片（ErpAstAsset）
       │   ├─ 原值 = 源资产原值 × 比例（或指定金额）
       │   ├─ 累计折旧 = 源资产累计折旧 × 比例（或指定金额）
       │   ├─ 净值 = 原值 − 累计折旧
       │   ├─ 折旧方法 = 继承源资产折旧方法（可单独修改）
       │   ├─ 已使用期间数 = 继承源资产剩余折旧期间按比例调整
       │   └─ 投入使用日期 = 源资产原始投入使用日期
       │
       └─ 生成凭证（借：固定资产-新卡片 贷：固定资产-源卡片）
```

## 资产合并流程

```
合并申请（DRAFT）
  ├─ 源资产（ErpAstAsset）→ 多个
  ├─ 合并方式
  │   ├─ 全额合并（full）：全部账面价值相加
  │   └─ 部分合并（partial）：选择部分源资产的价值
  └─ 目标资产定义
       │
       ▼（审批 → APPROVED）
  执行合并
       │
       ├─ 所有源资产状态 → DISPOSED
       │
       ├─ 创建 1 个新资产卡片
       │   ├─ 原值 = 各源资产原值之和
       │   ├─ 累计折旧 = 各源资产累计折旧之和
       │   ├─ 净值 = 原值 − 累计折旧
       │   ├─ 折旧方法 = 资产价值最大项的方法（或手动指定）
       │   ├─ 剩余折旧期间 = 按加权平均剩余期间取整
       │   └─ 投入使用日期 = 最早的源资产投入使用日期
       │
       └─ 生成凭证（借：固定资产-新卡片 贷：固定资产-各源卡片）
```

## 关键业务规则

1. **价值平衡约束**：拆分时，各目标资产的原值之和必须等于源资产原值（允许微量舍入差异，不超过 0.01 元）。
2. **折旧历史延续**：新卡片的"已计提折旧月数"按拆分/合并比例从源资产继承，用于计算剩余折旧年限。
3. **拆分后折旧**：新卡片按各自剩余折旧年限 × 各自账面净值重新计算月折旧额（直线法下）。
4. **合并后折旧**：合并后的月折旧额 = 各源资产月折旧额之和的加权平均（非简单相加）。
5. **不可逆**：拆分/合并执行后不可撤销。如有误，走一般资产处置 + 新建流程。
6. **审批要求**：拆分/合并视为危险操作，需财务主管审批方可执行。

## 配置选项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| 拆分方式 | proportional | proportional / fixed_amount |
| 舍入处理 | DOWN_UP | DOWN_UP（最大项补差）/ ROUND_ALL（全部四舍五入） |
| 合并折旧方法继承 | WEIGHTED | WEIGHTED（按净值加权平均）/ MAX（取最大价值项） |

## 实现注记（计划 0930-2）

> 本节记录 plan `2026-07-07-0930-2-assets-split-merge-value-rebalance` 落地时的实现 Decision、契约遵守声明与 Non-Goal 边界，作为 owner doc 与代码实现对齐的桥梁。

### 实体命名漂移修正

owner doc 早期正文 §边界 与 §流程引用 `ErpAstAssetSplitMerge` / `ErpAstAssetSplitMergeLine` 单聚合体命名，
实际建模（`module-assets/model/app-erp-assets.orm.xml`）采用分离实体：

- `ErpAstSplit` + `ErpAstSplitLine`（拆分：1源→N目标）
- `ErpAstMerge` + `ErpAstMergeLine`（合并：N源→1目标）

分离建模理由见 §边界。本期以分离实体为实现真相源。

### 关键 Decision

1. **资产状态字典 DISPOSED**：新增 `DISPOSED`（与 SCRAPPED/SOLD 并列的终态），语义=「已拆分/合并内部处置，账面净值归零」。
   - 语义边界：`DISPOSED` = 内部结构重组无损（拆分/合并）；`SCRAPPED`/`SOLD` = 对外处置有损益。
   - 替代方案（rejected）：复用 SCRAPPED（语义偏向报废损失，与无损拆分/合并错位）；新增 SPLIT/MERGED 子状态（粒度过细，状态机复杂化）。
2. **业务类型粒度**：双类型 `ASSET_SPLIT(440)` + `ASSET_MERGE(450)`（非单 `ASSET_RESTRUCTURE`），便于 reverse 审计与报表按方向区分。三件套（枚举 + 字典 + 常量）一致同步。
3. **舍入策略**：`erp-ast.split-rounding-mode` DOWN_UP（最大项补差，默认）/ ROUND_ALL（全部四舍五入），config-gated；原值与累计折旧同步补差。
4. **合并折旧方法继承**：`erp-ast.merge-depreciation-inherit` WEIGHTED（按净值加权，默认）/ MAX（取最大价值项），config-gated。
5. **拆分/合并是否强制审批**：`erp-ast.split-merge-require-approval` 默认 true。
6. **auto-execute-at-approve**：approve 末自动触发执行（拆分/合并不可「已审批未执行」，避免悬空审批态）。

### 不可逆契约遵守声明

owner doc §关键业务规则 5「拆分/合并执行后不可撤销」为保护区域契约，本期严格遵守：

- **不实现 reverse 红冲回退**：`IApprovableBiz` 接口的 `reverseApprove` 方法保留（接口契约），但 `ErpAstSplitProcessor` / `ErpAstMergeProcessor` 实现为抛 `ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED` / `ERR_AST_MERGE_REVERSE_NOT_SUPPORTED`。
- **错误更正路径**：经既有 `IErpAstDisposalBiz` SCRAPPED 处置 + 新建拆分/合并流程。
- **触发条件**（reverse 后继工作项启动条件）：当 owner doc §关键业务规则 5 修订为允许红冲回退时（保护区域契约修订）。

### Non-Goal 边界（计划内，附触发条件）

| 范围 | 原因 | 后继触发条件 |
|------|------|------------|
| 跨类别拆分/合并 | 涉及类别变更决策与多科目凭证；本期仅同 categoryId 内（新卡继承源类别） | 跨类别拆分业务需求上线时 |
| 跨币种拆分/合并 | 源与新卡片须同 currencyId；跨币种需先经外币重估 | 跨币种资产结构重组业务上线时 |
| 多次部分拆分/合并 | 一次拆分单=一次完整拆分；多次操作走多个拆分单 | 业务确认需单次部分拆分时 |
| 批量拆分/合并 | 单审批；批量审批属工作流增强面 | 批量资产结构重组业务上线时 |
| 历史折旧额重新分摊 | 本期按 proportion 比例继承累计折旧金额（不重算历史月折旧额） | 精确历史折旧分摊合规要求启动时 |
| 多级审批工作流（nop-wf `.xwf`） | approveStatus 走 DIRECT 三轴审批状态机（0540-3 范式） | 拆分/合并需多级审批工作流时 |
| 拆分/合并报表 | 归 nop-report successor（0504-2 子系统已就绪） | 资产结构重组报表需求启动时 |
