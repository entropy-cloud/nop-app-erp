# 固定资产域 - 资产盘点（UC-AST-09）

> 资产盘点全流程 owner 文档：盘点单（范围=部门/类别/地点）→ 录入实盘 → 差异复核 → 差异处置（盘盈建新卡 / 盘亏触发处置或调查）→ 业财过账 → 反向红冲纠错。
>
> 与 `use-cases.md` §UC-AST-09 双向引用；机制引用 `depreciation-and-posting.md` §四（价值调整科目映射范式）+ `state-machine.md`。

## 一、状态机（盘点单头 `ErpAstInventory.status`）

四态主链 + 两终态，字典 `erp-ast/inventory-status`：

```
DRAFT（草稿）
  └─ submitForCount → COUNTING（盘点中）
                       └─ reconcile（冻结差异） → RECONCILING（差异复核）
                                                   └─ processVariance + post → POSTED（已过账，终态）
  └─ cancel（仅 DRAFT/COUNTING） → CANCELLED（已作废，终态）
```

| 迁移 | 触发动作 | 前置条件 | 结果 |
|------|----------|----------|------|
| DRAFT → COUNTING | `submitForCount` | 头存在、范围非空、已展开账面行 | 行进入可录入实盘状态 |
| COUNTING → RECONCILING | `reconcile` | 所有账面行已录入实盘数量 | 计算差异（盘盈/盘亏/一致），冻结行不可再改 |
| RECONCILING → POSTED | `post`（config-gated 先 `approve`） | 所有差异行已处置（建卡/触发处置/标记调查） | 业财过账生成 ASSET_INVENTORY_ADJUSTMENT 凭证，posted=true |
| DRAFT/COUNTING → CANCELLED | `cancel` | 非终态 | 作废，不可再过账 |
| POSTED → （回退） | `reverse` | 已过账 | 红字凭证 + posted=false（遵守既有不可逆契约范式，纠错必经 reverse） |

终态：`POSTED`、`CANCELLED`。POSTED 经 `reverse` 回到 RECONCILING（非终态回退，仅纠错路径）。

差异处置在 RECONCILING 态进行（不迁移状态），由 `processVariance` 按行的 `varianceType` 分支处理：

- **MATCHED（一致）**：`disposition=NONE`，无需处置。
- **SURPLUS（盘盈）**：`disposition=NEW_CARD` → 建新资产卡片（IN_SERVICE，原值=评估价值）；`disposition=INVESTIGATE` → 仅标记，不建卡。
- **SHORTAGE（盘亏）**：`disposition=DISPOSAL` → 将账面资产置为 SCRAPPED（终态）；`disposition=INVESTIGATE` → 仅标记调查，保留资产账面。

## 二、差异引擎

```
差异 = 实盘数量 − 账面数量
  差异 > 0 → SURPLUS（盘盈）
  差异 < 0 → SHORTAGE（盘亏）
  差异 = 0 → MATCHED（一致）

差异金额（盘亏）= 账面净值（原值 − 累计折旧）
差异金额（盘盈）= 评估价值（用户在行上录入，作为新卡原值/入账金额）
```

资产卡片在本域表示单个资产单位（无数量字段），故 `bookQuantity` 取 0（盘盈新行）或 1（账面行），`actualQuantity` 取 0（盘亏，未找到）或 1（找到）。

## 三、科目映射（业财过账 ASSET_INVENTORY_ADJUSTMENT）

业务类型 `ErpFinBusinessType.ASSET_INVENTORY_ADJUSTMENT(460)`，字典 `erp-fin/business-type` 同步。镜像 `ValueAdjustmentAcctDocProvider` 范式（按差异类型分支科目分解），科目来源资产类别科目映射 + 全局兜底：

| 差异类型 | 借方 | 贷方 | 科目来源 |
|----------|------|------|----------|
| SURPLUS（盘盈） | 固定资产（1601） | 营业外收入（6301） | 借方=类别 subjectId；贷方=营业外收入兜底 |
| SHORTAGE（盘亏） | 营业外支出（6711） | 固定资产（1601） | 借方=营业外支出兜底；贷方=类别 subjectId |

凭证按盘点单聚合（一张盘点单一张凭证，多行分录汇总盘盈/盘亏总额）。`billHeadCode` = 盘点单 code，作为幂等/红冲键。

## 四、差异处置与既有链的复用

- **盘盈建新卡**：注入 `IErpAstAssetCapitalizationBiz`。为避免与盘点差异凭证双重过账（资本化链自带 CAPITALIZATION 凭证），本实现采用"建卡不入账"——直接创建 `ErpAstAsset`（IN_SERVICE，原值=评估价值），在行上记录 `newAssetId`。资本化链的完整过账复用为后继优化（触发条件：盘盈要求生成独立资本化凭证时）。`IErpAstAssetCapitalizationBiz` 仍注入以保留复用入口与类型安全。
- **盘亏触发处置**：注入 `IErpAstDisposalBiz`。同理为避免双重过账（处置链自带 DISPOSAL 凭证），本实现直接将账面资产状态置为 `SCRAPPED`（终态）。`IErpAstDisposalBiz` 注入保留复用入口。完整处置链（独立 DISPOSAL 凭证 + 清理损益）为后继优化。
- **配置门控**：`erp-ast.inventory-require-approval`（盘点过账是否强制先 approve，默认 true）；`erp-ast.inventory-negative-shortage-blocks`（盘亏是否阻塞 post 待调查，默认 false=允许盘亏直接触发处置）。

## 五、反向红冲

`reverse(id)` 仅对 POSTED 盘点单可用：
1. 经 `AssetPostingExecutor.reverse(code, ASSET_INVENTORY_ADJUSTMENT)` 生成红字凭证，原正常凭证标记 `isReversed=true`。
2. 盘点单 `posted=false`，状态回退 RECONCILING（允许修订后重新 post）。
3. 盘盈新建资产 / 盘亏 SCRAPPED 资产的状态变更不在 reverse 范围内（资产状态变更不可逆，需经资产域冲销流程独立处理——与 capitalization/disposal 既有契约一致）。

## 六、配置项

| 配置键 | 默认 | 含义 |
|--------|------|------|
| `erp-ast.inventory-require-approval` | true | 盘点 post 前是否强制 approve 复核 |
| `erp-ast.inventory-negative-shortage-blocks` | false | 盘亏行是否阻塞 post（true=所有盘亏须先 INVESTIGATE 才允许 post） |

## 七、关键业务规则

1. **范围展开**：`createInventory` 按部门/类别/地点范围查询 IN_SERVICE/IDLE 资产，生成账面行（bookQuantity=1, bookValue=账面净值）。空范围拒绝（`ERR_AST_INVENTORY_RANGE_EMPTY`）。
2. **行防重**：一资产在一盘点单内只能一行（UK 校验 `ERR_AST_INVENTORY_LINE_ASSET_DUPLICATE`）。
3. **差异冻结**：reconcile 后行不可再改实盘数量；差异处置在 RECONCILING 态进行。
4. **过账门控**：post 前所有 SURPLUS/SHORTAGE 行须有 disposition；config-gated approve 先决。
5. **终态不可逆**：CANCELLED 不可恢复；POSTED 纠错走 reverse，不走 cancel。

## 八、实现偏离补注（2026-07-07，计划 0842-1 落地）

- **盘盈/盘亏处置链复用收窄**：§四所述，为避免与 ASSET_INVENTORY_ADJUSTMENT 凭证双重过账，盘盈建卡 / 盘亏 SCRAPPED 走直接 dao 操作，资本化/处置链的独立凭证暂不触发。`IErpAstAssetCapitalizationBiz` / `IErpAstDisposalBiz` 仍注入以保留后继完整复用入口。触发条件：盘盈/盘亏要求生成独立 CAPITALIZATION / DISPOSAL 凭证时。
- **盘点 AMIS 工作台深度定制**：codegen 标准列表/编辑页满足本期功能验证；深度定制属前端面（见计划 Deferred But Adjudicated）。
- **周期性自动盘点 / 扫码盘点**：本期 Non-Goal（见计划 Deferred But Adjudicated）。

## 参考机制文档

- `state-machine.md` — 资产卡片状态（盘亏 SCRAPPED 终态、盘盈 IN_SERVICE 新卡）
- `depreciation-and-posting.md` §四 — 价值调整科目映射范式（本文件镜像）
- `../finance/posting.md` — 业财过账引擎
