# 固定资产域 - 资产维修管理与费用归集（UC-AST-10）

> 资产维修全流程 owner 文档：维修工单（关联资产卡片，可弱关联 maintenance 域 ErpMntVisit）→ 归集维修费用（人工/备件/外协）→ 裁决处置方式（CAPITALIZE 延长寿命/提升效能 vs EXPENSE 日常维修）→ 资本化路径（增加资产原值 + 重算折旧计划 + 资本化凭证）或费用化路径（维修费用凭证 借费用/贷存货或银行）→ 反向红冲纠错。
>
> 与 `use-cases.md` §UC-AST-10 双向引用；机制引用 `depreciation-and-posting.md` §二（资本化）/§四（价值调整科目映射范式）+ `state-machine.md`。跨域边界参照 `../maintenance/`。

## 一、状态机（维修工单头 `ErpAstMaintenance.status`）

四态主链 + 两终态，字典 `erp-ast/maintenance-status`：

```
DRAFT（草稿）
  └─ submit → SUBMITTED（已提交）
              └─ startWork → IN_PROGRESS（维修中）
                              └─ completeWork → COMPLETED（已完工）
                                                └─ decideTreatment + approve + post → POSTED（已过账，终态）
  └─ cancel（仅 DRAFT/SUBMITTED） → CANCELLED（已作废，终态）
```

| 迁移 | 触发动作 | 前置条件 | 结果 |
|------|----------|----------|------|
| DRAFT → SUBMITTED | `submit` | 头存在、资产非终态 | 进入待开工状态 |
| SUBMITTED → IN_PROGRESS | `startWork` | 已提交 | 维修中，可归集费用 |
| IN_PROGRESS → COMPLETED | `completeWork` | 已提交 | 完工，待裁决处置 |
| COMPLETED → POSTED | `decideTreatment` + `approve` + `post` | 已裁决处置、费用已归集、阈值门控通过 | 业财过账（按 treatment 分派 CAPITALIZE/EXPENSE 凭证），posted=true |
| DRAFT/SUBMITTED → CANCELLED | `cancel` | 非终态 | 作废，不可再过账 |
| POSTED → （回退） | `reverse` | 已过账 | 红字凭证 + posted=false（纠错必经 reverse） |

终态：`POSTED`、`CANCELLED`。POSTED 经 `reverse` 红冲纠错。

裁决处置（`decideTreatment`）在 COMPLETED 态进行（不迁移状态），由操作员按维修性质裁决：
- **CAPITALIZE（资本化）**：维修延长资产使用寿命或提升效能，费用资本化（增加资产原值 + 重算折旧计划）。
- **EXPENSE（费用化）**：日常维修/维护性支出，费用化（计入当期维修费用）。

## 二、资本化/费用化裁决规则

```
裁决输入：维修性质（延长寿命/提升效能 → CAPITALIZE；日常维修 → EXPENSE）
          + 资本化金额阈值门控（erp-ast.maintenance-capitalize-threshold，默认 0=不设阈值按 treatment 字段判定）

CAPITALIZE 路径：
  1. 资产卡片.原值 += 资本化金额（capitalizedAmount）
  2. 重算折旧计划：按剩余使用年限重新摊销（原值+增量 − 已计提累计折旧 − 残值）/ 剩余月数
     删除/重建未执行（PENDING）折旧计划条目，残值约束保留
     config-gated erp-ast.maintenance-cap-adjust-depreciation-base（默认 true）
  3. 生成 MAINTENANCE_CAPITALIZATION(480) 凭证：借 固定资产（原值增量）/ 贷 在建工程或银行存款/存货（费用来源）

EXPENSE 路径：
  1. 不影响资产原值/折旧计划
  2. 生成 MAINTENANCE_EXPENSE(470) 凭证：借 维修费用/制造费用 / 贷 存货或银行（按费用来源）
```

阈值门控：当 `maintenance-capitalize-threshold > 0` 且资本化金额 < 阈值时，强制费用化（`ERR_AST_MAINTENANCE_CAPITALIZE_BELOW_THRESHOLD`），避免小额维修资本化增加审计/折旧复杂度。

资本化金额（`capitalizedAmount`）默认等于费用合计（`totalCostAmount`），操作员可在裁决时调整（部分费用化场景：超阈值部分资本化、其余费用化归 successor）。

## 三、折旧计划重算规则（资本化路径）

资本化维修对**既有 IN_SERVICE 资产**增加原值并**重算折旧计划**（区别于新建资本化建卡路径）。重算经 `IErpAstDepreciationScheduleBiz.recalculateForCapitalizationMaintenance(assetId, increment)` 加性方法（非破坏性扩展已 done 接口）：

```
重算公式（直线法，残值约束）：
  新可折旧基数 = （原值 + 增量）− 残值
  剩余月数 = 使用年限 − 已执行月数
  每期折旧 = 新可折旧基数 / 剩余月数（最后一期补差到残值）

执行步骤：
  1. 删除所有 PENDING（未执行）折旧计划条目
  2. 按剩余月数重新生成 PENDING 条目（每期等额，残值约束最后一期调整）
  3. 保留已 EXECUTED/REVERSED 条目不变（历史不追溯）
  4. 资产卡片原值 += 增量，净值同步调整
```

残值约束：重算后每期折旧确保最终净值收敛到残值（不低于残值）。非直线法折旧方法的重算归 successor（本期资本化维修默认直线法资产，DECLINING/UNITS 重算触发条件=非直线法资产资本化维修需求时）。

## 四、科目映射（业财过账）

两个独立业务类型（区别于新建 CAPITALIZATION(80)，资本化维修=既有资产原值增量，独立类型利于审计与科目映射）：

### MAINTENANCE_EXPENSE(470) — 费用化路径

| 借方 | 贷方 | 科目来源 |
|------|------|----------|
| 维修费用（6602）/ 制造费用（5101） | 存货（1403）/ 银行存款（1002）/ 维修中转清算科目（2502） | 借方=维修费用兜底；贷方按费用来源 + maintenanceVisitId 分支 |

贷方分支（**防双重库存扣减规则**，config-gated `erp-ast.maintenance-linked-credit-clearing` 默认 true）：
- `maintenanceVisitId` 非空（已关联维护工单）：贷**维修中转/清算科目**（备件已由 maintenance 域 `ErpMntSparePartUsage.generateMove` 实物出库，价值侧只做科目重分类，防双重扣减）。
- `maintenanceVisitId` 为空（独立资产维修）：贷**存货（备件消耗）/银行存款（外协/人工）**直接。

SPARE_PART 费用行为**操作员手工录入的会计确认**（独立于 ErpMntSparePartUsage 实物出库），保持 assets 域价值确认的独立性与简单性。

### MAINTENANCE_CAPITALIZATION(480) — 资本化路径

| 借方 | 贷方 | 科目来源 |
|------|------|----------|
| 固定资产（1601，原值增量） | 在建工程（1604）/ 银行存款（1002）/ 存货（1403） | 借方=类别 subjectId；贷方按费用来源 |

凭证按维修工单聚合（一张维修单一张凭证，多行分录汇总费用来源）。`billHeadCode` = 维修单 code，作为幂等/红冲键。

## 五、与 maintenance 域边界（互补不闭合）

本计划属 **assets 域价值侧**（资产维修工单的资本化/费用化会计处理），与 maintenance 域实物侧**互补而非闭合**：

| 维度 | assets 域（本计划） | maintenance 域（plan 1018-3） |
|------|---------------------|------------------------------|
| 价值视角 | 价值侧（会计处理） | 实物侧（物理维护） |
| 实体 | `ErpAstMaintenance` + `ErpAstMaintenanceCost` | `ErpMntVisit` + `ErpMntSparePartUsage` |
| 业务类型 | MAINTENANCE_EXPENSE(470) / MAINTENANCE_CAPITALIZATION(480) | MAINTENANCE_ISSUE（1018-3 Deferred，未接线） |
| 结果表面 | 资产原值/折旧/会计凭证 | 设备状态/库存出库 |

两域经 `ErpAstMaintenance.maintenanceVisitId` 弱关联（不跨工程 refEntityName，走 I*Biz）。1018-3 的 MAINTENANCE_ISSUE（maintenance 域实物侧备件消耗/工时费用化凭证）仍 **open**，本计划**不闭合**该 Deferred。命名邻近（MAINTENANCE_EXPENSE vs MAINTENANCE_ISSUE）通过不同 code 段（470/480 vs 待定）+ 本边界注记消歧。未来若两域统一维修单据模型，命名邻近可能需重整（触发条件：维修单据跨域统一时）。

## 六、反向红冲（reverse）

维修单 POSTED 后纠错必经 `reverse`：

```
reverse：
  1. 红字凭证（IErpFinVoucherBiz.reverse，billHeadCode 幂等/红冲键）
  2. CAPITALIZE 路径：回退资产原值（原值 − 资本化金额）+ 回退折旧计划重算（删除重算生成的 PENDING 条目，恢复原计划）
  3. EXPENSE 路径：仅凭证回退（不影响资产原值/折旧）
  4. 维修单 posted=false，reversed=true
```

reverse 幂等：同一维修单二次 reverse 拒绝（`reversed=true` 拦截）。

## 七、配置项

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `erp-ast.maintenance-require-approval` | true | 维修过账强制审批（approve 后才 post） |
| `erp-ast.maintenance-capitalize-threshold` | 0 | 资本化金额阈值，低于则强制费用化（0=不设阈值按 treatment 字段判定） |
| `erp-ast.maintenance-cap-adjust-depreciation-base` | true | 资本化是否重算折旧基数（镜像 0540-3 重估调整范式） |
| `erp-ast.maintenance-linked-credit-clearing` | true | 关联维护工单时贷中转科目防双重扣减（false=仍贷存货，需人工对账） |

## 八、关键业务规则

1. **一工单一资产**：本期一维修工单关联一个资产卡片（多资产联合维修归 successor）。
2. **资产终态拦截**：已 SCRAPPED/SOLD/DISPOSED 资产不可创建维修工单（`ERR_AST_MAINTENANCE_ASSET_TERMINAL`）。
3. **费用归集前置**：post 前须有费用行（`ERR_AST_MAINTENANCE_NO_COST`）；裁决处置前置（`ERR_AST_MAINTENANCE_TREATMENT_NOT_DECIDED`）。
4. **状态机强约束**：非法迁移抛 `ERR_AST_MAINTENANCE_ILLEGAL_STATUS_TRANSITION`。
5. **资本化与费用化互斥**：一维修工单过账后 treatment 不可变更（纠错走 reverse + 新建）。

## 九、实现注记（计划 2026-07-07-0842-2）

- **头-行建模 Decision**：选 `ErpAstMaintenance`（头）+ `ErpAstMaintenanceCost`（费用行），非单表（费用归集需多行多类型，头-行是仓内范式）。
- **独立业务类型 Decision**：选 `MAINTENANCE_EXPENSE(470)` + `MAINTENANCE_CAPITALIZATION(480)` 两个独立类型，非复用 `CAPITALIZATION(80)`（资本化维修=既有资产原值增量，与新建资本化语义不同，独立类型利于审计与科目映射）。
- **SPARE_PART 费用来源边界 Decision（B2）**：SPARE_PART 费用行为操作员手工录入的会计确认（独立于 ErpMntSparePartUsage）。防双重扣减：`maintenanceVisitId` 非空时贷中转科目，为空时贷存货/银行。
- **折旧重算落位 Decision（S1）**：在 `IErpAstDepreciationScheduleBiz` 加性新增 `recalculateForCapitalizationMaintenance` 方法（非破坏性扩展已 done 接口），重算逻辑是稳定领域事实、跨场景可复用、可独立单测。
- **编排方式 Decision**：Processor 模式（`ErpAstMaintenanceProcessor`，protected step 方法），镜像 `ErpAstAssetCapitalizationProcessor`，拓扑稳定不引入 task.xml。
- **跨实体访问**：注入 `IErpAstAssetBiz`（资产校验）+ `IErpAstDepreciationScheduleBiz`（折旧重算）；maintenance 域 `IErpMntVisitBiz` 仅 R 只读弱关联（assets→maintenance 单向，不跨工程 refEntityName）。

## 十、Non-Goals（本期不做，附触发条件）

- **预测性维护 / IoT 触发维修工单**：本期手工创建维修工单满足 UC-AST-10 价值管理；IoT 触发属实物采集能力面（触发条件：IoT 设备数据源落地 + 预测性维护需求时）。
- **维修计划自动生成**：归 maintenance 域 1018-3 已 done 的维护计划；本计划只管维修工单价值侧。
- **多资产联合维修工单**：本期一工单一资产（触发条件：多资产联合维修业务需求时）。
- **维修质检全流程**：归 quality 域。
- **维修工单 AMIS 深度定制页**：codegen 标准页满足本期（触发条件：维修 UX 优化需求时）。
- **非直线法资产资本化维修折旧重算**：本期资本化维修默认直线法资产重算；DECLINING/UNITS 重算归 successor（触发条件：非直线法资产资本化维修需求时）。
