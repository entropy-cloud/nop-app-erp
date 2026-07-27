# 审计-修复报告索引（arm-index）

> 本轮审计-修复全部报告的统一入口。每份报告产出后同步更新。
> 启动时间：2026-07-27
> 来源：`docs/skills/audit-remediation-roadmap-authoring-prompt.md` 步骤 6.1
> 范围文档：`docs/audits/audit-remediation-scope-and-dimension-matrix.md`
> **审计-修复回归起点锚**：`docs/audits/compliance-baseline.md §M0 锚点注记`（HEAD=0e963531d，2026-07-27 实测落锚；MA/MR/MV 验证里程碑对比此锚点）

## 报告清单

> 状态列：`todo` = 未开始 | `active` = 审计中 | `done` = 报告产出 | N/A = 不适用

| 报告 | 里程碑 | 维度 | 域/功能模块 | 状态 |
|------|--------|------|-----------|------|
| `2026-07-27-1015-arm-ma1-s-tier-orm.md` | MA1 | ORM 模型规范 | finance / manufacturing / hr（A1.1/A1.2/A1.3） | done |
| `2026-07-27-1015-arm-ma1-a-tier-orm.md` | MA1 | ORM 模型规范 | purchase+sales / assets+inventory / crm+quality+projects（A1.4/A1.5/A1.6） | done |
| `2026-07-27-1015-arm-ma1-bc-tier-orm.md` | MA1 | ORM 模型规范 | master-data / cs+contract+b2b+maintenance+drp / aps+logistics+notify（A1.7/A1.8/A1.9） | done |
| `2026-07-27-1227-arm-ma1-cross-module-dag.md` | MA1 | 跨模块依赖/DAG | 全 19 域跨域（A1.10） | done |
| `2026-07-27-1227-arm-ma1-platform-conformance-s-tier.md` | MA1 | Nop 平台合规 | finance / manufacturing / hr（A1.11） | done |

## P0 发现追踪（即时通道）

> P0 不进入批量修复里程碑。发现即就地修复或异步注入 plan。

| Finding ID | 报告 | 描述 | 修复路径 | 修复 plan | 修复状态 |
|-----------|------|------|---------|----------|---------|
| _（MA1 ORM 审计全域 0 P0，无即时通道触发）_ | | | | | |

## P1 发现汇总（待 MR 批量修复）

> MA1 ORM 审计全域共 51 项 P1（S 级 1 + A 级 42 + B+C 级 9 - 重复计 1：maintenance propId 在 A/BC 报告中分类归属一致）+ MA1 跨模块依赖审计 3 项 P1（文档 2 + 代码 1）+ MA1 平台合规审计 S 级 1 项 P1（finance enum↔dict 漂移），统一登记如下。目标 MR1（依赖 MA1+MA2 done，由 R1.0 展开机制转化为具体修复工作项行）。

### P1 类型分布

| 根因 | 数量 | 涉及域 | 严重性 | MR1 修复方式 |
|---|---|---|---|---|
| **propId 缺失**（D3/D4 多币种四件套补字段未重编号） | 40 | mfg(7) + assets(29) + projects(5) + maintenance(5) + quality(1) - maintenance 重复计 1 = 46 → 实体不重复统计 46 列 | major | codegen 增量再生（`mvn clean install -DskipTests`）自动按文档顺序补全 propId；或 MR1 手动 renumber |
| **crm DECIMAL↔Double 类型偏离** | 7 | crm（ForecastAccuracy/PriceRule/LeadFunnel/FunnelStageMetrics 共 7 列） | major | 手动改 `stdDataType="double"` → `"string"` 或 `"decimal"`（推荐 decimal——这些字段参与比率计算） |
| **drp 命名异常**（ErpInvDrp*） | 4 实体 | drp（SafetyStockCalc/CrossDock/DockAppointment/LeadTimeRecord） | major | MR1 裁决：重命名为 `ErpDrp*`/`erp_drp_*`（推荐）或登记 §19.2 例外 |
| **owner doc §5.6.2 数值偏差** | 1 | docs（全域） | major | MR1 用脚本核验值（625 to-one / 111 external）更新 owner doc §5.6.2 + 每域明细表 |
| **finance IDaoProvider 跨域 DAO 查询** | 1 | finance（ErpFinAccountingPeriodProcessor.reverseDepreciation） | major | MR1 改为 `IErpAstDepreciationScheduleBiz.findList()` 等价 I*Biz 只读方法 |
| **owner doc §3.2/§4.4 finance 纯读规则不完整** | 1 | docs（finance） | major | MR1 补注期末结账 command 编排例外（ORM 层纯读 vs I*Biz 层 command 编排分层） |

### P1 详细清单

| Finding ID | 报告 | 域 | 描述 | 目标 MR | 修复状态 |
|-----------|------|---|------|--------|---------|
| `P1-MA1-001` | ma1-s-tier-orm | mfg | `ErpMfgWorkOrder`/`ErpMfgMaterialIssue` 多币种四件套 7 列 propId 缺失 | MR1 | todo |
| `P1-MA1-008` | ma1-a-tier-orm | assets | `ErpAstDepreciationSchedule`/`ErpAstMovement`/`ErpAstRevaluation`/`ErpAstSplit`/`ErpAstMerge`/`ErpAstDisposal`/`ErpAstCapitalization`/`ErpAstTransfer` 共 29 列 propId 缺失 | MR1 | todo |
| `P1-MA1-009` | ma1-a-tier-orm | crm | `ErpCrmForecastAccuracy.{commitAccuracy, upsideAccuracy}`/`ErpCrmPriceRule.discountPercent`/`ErpCrmLeadFunnel.avgSalesCycleDays`/`ErpCrmFunnelStageMetrics.{conversionRate, dropOffRate, avgDaysInStage}` 共 7 列 stdSqlType=DECIMAL vs stdDataType=double（浮点精度损失） | MR1 | todo |
| `P1-MA1-010` | ma1-a-tier-orm | projects | `ErpPrjCostCollection.{exchangeRate, amountSource, amountFunctional}`/`ErpPrjBilling.{amountSource, amountFunctional}` 共 5 列 propId 缺失 | MR1 | todo |
| `P1-MA1-011` | ma1-a-tier-orm + ma1-bc-tier-orm | maintenance | `ErpMntVisit.{orgId, businessDate, posted, postedAt, postedBy}` 共 5 列 propId 缺失 | MR1 | todo |
| `P1-MA1-012` | ma1-a-tier-orm | quality | `ErpQaInspection.businessDate` propId 缺失 | MR1 | todo |
| `P1-MA1-013` | ma1-bc-tier-orm | maintenance | （同 P1-MA1-011，BC 报告同步登记） | MR1 | todo |
| `P1-MA1-014` | ma1-bc-tier-orm | drp | `ErpInvDrpSafetyStockCalc`/`ErpInvDrpCrossDock`/`ErpInvDrpDockAppointment`/`ErpInvDrpLeadTimeRecord` 4 实体 className=`ErpInvDrp*` + tableName=`erp_inv_drp_*` 不符合 §19.1 命名规范 | MR1 | todo |
| `P1-MA1-015` | ma1-cross-module-dag | docs（全域） | owner doc `data-dependency-matrix.md §5.6.2` 自述数值偏低：声称 ~369 to-one / ~68 external，实测 **625 to-one + 0 to-many / 111 external**（+69% / +63%）。owner doc 文字已声明"待 codegen 后跑脚本精确统一"，本审计即该脚本。需用机器核验值更新 §5.6.2 数值与每域明细表 | MR1 | todo |
| `P1-MA1-016` | ma1-cross-module-dag | finance | `ErpFinAccountingPeriodProcessor.reverseDepreciation`（line ~389）使用 `daoProvider.daoFor(ErpAstDepreciationSchedule.class).findAllByQuery(q)` 直接跨域 DAO 查询 assets 实体，违反 `AGENTS.md "跨实体访问"` + `data-dependency-matrix.md §5.3` 规则（跨域查询必须经 I*Biz 接口）。应改为 `IErpAstDepreciationScheduleBiz.findList()` 或等价 I*Biz 只读方法 | MR1 | todo |
| `P1-MA1-017` | ma1-cross-module-dag | docs（finance） | owner doc `data-dependency-matrix.md §3.2/§4.4` "finance 对业务域纯读不回写"规则不完整：未覆盖期末结账期间的跨域 command/request 编排（finance 调 `IErpAstDepreciationScheduleBiz.executeBatchDepreciation/reverseDepreciation` + `IErpInvCostingBiz.reclosePeriodCosts`）。需补注："纯读"指 ORM 层无反向 to-one；期间结账的 command 编排在 I*Biz 层合法（业务域自管实体的写） | MR1 | todo |
| `P1-MA1-018` | ma1-platform-conformance-s-tier | finance | `ErpFinBusinessType` enum 名 ↔ dict `erp-fin/business-type` value 漂移 4 项：`MANUFACTURING_COST_CLOSE(100)`↔`PRODUCTION_COST` / `PROJECT_COST_COLLECTION(110)`↔`PROJECT_COST` / `PERIOD_CLOSE(120)`↔`PERIOD_CLOSING` / `EXCHANGE_GAIN_LOSS(130)`↔`FX_REVALUATION`。代码以 `enum.name()` 持久化（如 `ExchangeRevaluationService:152,213`、`ProfitLossClosingService:152`），UI dict 下拉值与 DB 存储值不符，筛选查询漏命中。owner doc `posting.md §业务类型映射` 明示「常量 code 与字典数值逐一一致」。修复方案 A（推荐 enum 重命名对齐 dict）或方案 B（dict 改值+数据迁移） | MR1 | todo |

> 去重说明：P1-MA1-011 与 P1-MA1-013 是同一组 finding（maintenance propId），在 A 与 BC 报告中均出现，因 maintenance 既属 A 级合并（A1.5 assets+inventory）边缘又属 B 级合并（A1.8 cs+contract+b2b+maintenance+drp）——本次审计按 roadmap 工作项边界在两份报告中各登记一次。MR1 实际修复只处理一次。

## P2 发现汇总（watch-only / 待 MR 顺手收敛）

> P2 不阻塞 MR 批次。仅记录给 MR1/MR2 顺手收敛或永久接受时引用。

| Finding ID | 报告 | 域 | 描述 | 处置 |
|-----------|------|---|------|------|
| `P2-MA1-019` | ma1-platform-conformance-s-tier | finance | `ErpFinBusinessType.fromCode:86` 抛 `IllegalArgumentException`（应 `NopException`，skill 异常处理规则）+ 同文件 `ErpFinVoucherTemplateBizModel:95` 用 `LocalDate.now()`（应 `CoreMetrics.currentDate()`，R7 同性质残留）。programmer-error 路径，非 GraphQL 面向，严重性 P2 | watch-only，MR1 顺手收敛或永久接受 |
| `P2-MA1-020` | ma1-platform-conformance-s-tier | hr | 残留 orphan dict `erp-hr/salary-approval-status`（6 态 PENDING/REVIEWED/APPROVED_FINANCE/APPROVED_MANAGER/PAID/VOID）在 `app-erp-hr.orm.xml:77-83` + i18n。owner doc `payroll.md §审批状态标准化` 明示已废弃（拆为 `wf/approve-status` 4 态），实际 column 已正确引用 wf/approve-status；orphan dict 仅在 i18n + javadoc 残留引用，无运行时影响 | watch-only，MR1 顺手清理（删除 orphan dict 定义 + i18n + javadoc 引用） |

## 跨维度发现（待 MR4 裁决）

| Finding ID | 涉及维度 | 冲突描述 | 裁决状态 |
|-----------|---------|---------|---------|
| _（MR1-MR3 执行后按需填充）_ | | | |

## 归档纪律

1. **报告产出即更新本索引**——审计 plan 的 EXECUTE 阶段最后一项必须是"更新 `arm-index.md`"
2. **修复完成即回填索引**——P0/P1 修复 plan 完成后，在对应行的"修复状态"列回填 `done`
3. **Finding ID 规范**：`P<级别>-<里程碑>-<序号>`（如 `P0-MA1-001`），序号在该里程碑内连续，产出时分配不可变
4. **报告命名**：`docs/audits/YYYY-MM-DD-HHmm-arm-<milestone>-<domain-cluster>-<dimension>.md`
5. **MV 验证里程碑校验**：所有 P0/P1 的修复状态均为 `done` 或显式 deferred
