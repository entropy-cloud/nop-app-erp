# Nop 平台最佳实践合规审计（2026-09-10 21:17）

> 审计对象：nop-app-erp 全部 18 业务域 + notify 子系统 + common 共享模块（生产代码 `*/src/main/java` + `_vfs` 资源，排除 `_gen/`、`_` 前缀生成物、`target/`、test）
> 审计方法：`Skill: nop-platform-conformance-audit-prompt`（15 维度）+ `nop-compliance-checker.sh` 复跑（19 规则）+ 5 个独立子代理分域定性深扫（每条发现附 file:line，经 grep/read 核实）
> 权威基线：`../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`、`04-reference/safe-api-reference.md`、`02-core-guides/domain-logic-and-ddd.md`、`02-core-guides/service-layer.md`；项目 owner docs `docs/architecture/processor-extension-pattern.md`、`entity-state-machine-bean.md`、`docs/audits/compliance-baseline.md`
> HEAD at audit: `dc31a2555`；checker 漂移锚点：`060ddab0a`（2026-09-09-2100-1 基线裁决点）。审计窗口内有并行 mission（ai-check-r3 M2.8，plan 2026-09-10-1141-3）修改 web 层 action-auth 文件——不触及本审计的 service 层 Java 代码，发现不受影响；该 M2.8 变更自身的 compliance 影响未在本审计范围内，其 closure 须自行复跑 checker。

## 裁决：失败（条件性，需修复后复验）

无「手改生成物 / @Inject private / 裸 RuntimeException / System.currentTimeMillis」类硬违规（这些维度 checker 全绿），治理基线（149+ per-mutation Processor、34+ StateMachine Bean、nop-job 全接线、nop-report xpt 渲染、ErrorCode 体系）显著优于历史状态；但存在 **2 项 blocker、约 66 项 major**，其中五处**未登记跨域写**直接违反「跨域写走 I*Biz Facade」硬规则、3 项 compliance 基线漂移未裁决（已知失败模式 #1 复发）、notify 邮件/短信通道收件人断链。按技能严重性指南（跨模块写反向/绕过 I*Biz = blocker~major），总体判**失败**，修复 P0/P1 后可复验通过。

## 审计维度合规率

| # | 维度 | 状态 | 关键证据 |
|---|------|------|---------|
| 1 | 决策顺序 Model→Delta→Java | ⚠️ 部分 | 业务规则大量硬编码 Java（nop-rule 0 采用，见 `05`），但 ORM/dict 为真相源未被绕过 |
| 2 | 跨实体访问 I*Biz | ❌ 失败 | 5 处未登记跨域 daoFor 写（`04` §1）+ 6 处 ORM to-one 可替代 daoFor(FK)（`04` §3）+ R1b/R1d 裸 dao() 漂移（`01`） |
| 3 | 异常处理 NopException+ErrorCode | ✅ 通过 | 全仓 0 处 `extends RuntimeException`；Errors 类体系完整 |
| 4 | IoC 与事务 | ✅ 通过 | 0 处 `@Inject private`；`@BizMutation+@Transactional` 仅 2 处已登记（R10 同源） |
| 5 | 平台辅助工具 | ⚠️ 部分 | 1 处 `LocalDateTime.now()`（`01` §4）；CoreMetrics/JsonTool/StringHelper 其余全面使用 |
| 6 | 标准服务模式 CrudBizModel | ⚠️ 部分 | BizModel 大面薄 Facade 化；R1b×1/R1d×1 新漂移 + 少量裸 dao() 写（`04` §4） |
| 7 | 跨模块外部实体引用 notGenCode | ✅ 通过 | 本次未发现新违规（历史 MA1 已收口） |
| 8 | 状态机与规则引擎 | ❌ 失败 | ~400 处内联状态比较 + 双源迁移矩阵漂移（`03`）；nop-rule 0 采用（`05` §1） |
| 9 | 审批流与作业 | ✅ 通过 | nop-job 全接线（35 job.yaml + 12 batch.xml，0 自研循环）；不用 wf 为 owner doc 裁决 |
| 10 | 定制能力 Delta | ✅ 通过 | 未发现违规（本次未深扫 delta 层，历史 MA1 已覆盖） |
| 11 | 多租户与本地化 | ✅ 通过 | i18n checker 基线 0 缺陷（2026-07-29 锚点，未复跑——残留风险） |
| 12 | 测试与验证 | ✅ 通过 | 历史基线 3991 tests full-green（2026-09-07 log），本次零代码变更不复跑 |
| 13 | Codegen 产物安全 | ✅ 通过 | 无手改 `_` 前缀文件迹象；R8=0 孤儿 |
| 14 | 聚合完整性 action-auth | ✅ 通过 | 未发现新模块遗漏（19 域已全注册） |
| 15 | Processor per-mutation 纪律 | ⚠️ 部分 | 494 mutation 中 ~12 个多步编排滞留 BizModel（`02`）；God Processor 1 个（873 行） |
| 16 | DDD 实体方法落位 | ❌ 失败 | 369 实体仅 22 个有 is/can 方法（6%）；4 域实体 100% 空壳（`03`） |
| 17 | 跨域通知平台对齐 | ❌ 失败 | notify 自建同步派发 + 收件人断链 blocker（`05` §2） |

通过 9 / 部分 4 / 失败 4（17 维度口径，含技能 15 维度 + 实体方法 + Processor 纪律细分）。

## 发现统计

| 来源 | blocker | major | minor | 合计 |
|------|---------|-------|-------|------|
| 子代理 A（pur/sal/inv） | 0 | 7 | 12 | 19 |
| 子代理 B（fin/ast） | 0 | 8 | 19 | 27 |
| 子代理 C（mfg/aps/log/drp） | 0 | 14 | 16 | 30 |
| 子代理 D（hr/prj/qa/mnt） | 1 | 14 | 10 | 25 |
| 子代理 E（crm/cs/b2b/ct/md/notify/common/all + 横切） | 1 | 19 | 21 | 41 |
| 主代理量化（checker 漂移 3 + 时钟 1） | 0 | 4 | 0 | 4 |
| **合计** | **2** | **66** | **78** | **146** |

## P0（修复优先级最高）

1. **[blocker] notify 邮件/短信收件人断链** — `NotificationDispatcher.java:177-195` 构造 EmailMessage/SmsMessage 从不设置收件人地址，EMAIL/SMS 通道实际不可达（详见 `05` §2.1）
2. **[blocker] projects→assets 跨域裸写绕过生命周期守卫** — `ErpPrjProjectSettlementProcessor.java:223-232` `daoFor(ErpAstAsset).updateEntity` 直接回写资产状态 + 硬编码 `ASSET_STATUS_DRAFT="DRAFT"`（详见 `04` §1.1）
3. **[major] 3 项 compliance 基线漂移未裁决**（已知失败模式 #1 复发，源 = 今日活跃 mission M2.5 提交 `48b57cd06`）— R1b 0→1、R1d 14→15、R2c 1542→1543（详见 `01`）
4. **[major] 4 处未登记跨域 daoFor 写**：inv CostAdjustmentService→mfg rollup、drp DrpReleaseService→inv/pur、ct InvoicePlan→pur/sal 发票、mfg BatchGenealogyWriter→inv 批次（详见 `04` §1）

## P1（结构性治理，建议独立 roadmap）

- **实体贫血**：~400 处内联状态比较应上提实体方法（`03` 已给各域 top 候选与签名建议）——这是 L-6（plan 2026-07-20-2200-1）登记 follow-up 触发条件已满足的规模化形态
- **双源迁移矩阵**：StateMachine Bean 与 Processor 根类内联并存（ErpPurInvoiceProcessor 同类混用；sales 根 Processor 整块内联而子类已走 Bean）——违反 `entity-state-machine-bean.md` 单一承载契约
- **nop-rule 0 采用**：LeadScoringEngine 自创 DSL、GL 科目映射自研优先级链、坏账账龄矩阵、定价匹配矩阵、供应商评分权重等（`05` §1）
- **Processor 覆盖缺口 ~12 处**（`02`）：ErpCtDocumentBizModel 0 Processor、ErpCtContractBizModel 终止/到期族、ErpHrSurvey 族、ErpLogDeliveryBookingBizModel.book、ErpSalOrderBizModel.applyPricingRules、ErpFinEmployeeAdvanceBizModel cashRepay 族
- **N+1 查询 10+ 处**（`05` §4）：mfg costing/mrp 链 6 处、mnt OEE 列表、qa Spc 采样、pur/sal/inv 看板 getEntityById 循环、cs 看板全量内存聚合
- **`*Service` 命名 13 + `*ServiceImpl` 1**（`06` §1）：违反平台 service-layer.md 与 owner doc 硬规则 4
- **God class / 死代码**：ErpFinReportBizModel 741 行（Java 聚合 + 硬编码现金科目前缀 1001/1002/1012/1031）、ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor 873 行、ErpHrSalarySimulationBizModel ~150 行死代码、ErpCtInvoicePlanBizModel 跨域写死代码副本（`06`）

## 文件清单

| 文件 | 内容 |
|------|------|
| `01-checker-baseline-drift.md` | compliance checker 19 规则复跑结果、3 项漂移站点定位、时钟违规、裁决路径 |
| `02-processor-coverage.md` | per-mutation Processor 覆盖缺口（按域，file:line + 行数 + 步骤概要） |
| `03-entity-method-anemia.md` | 实体方法缺口量化、各域上提候选与签名建议、双源迁移矩阵漂移 |
| `04-cross-domain-and-dao.md` | 未登记跨域写、daoFor(FK) 反模式、裸 dao() 写、吞异常（B1 复发点） |
| `05-platform-capabilities.md` | nop-rule 替代候选、notify 平台对齐、报表/看板聚合下推、N+1 清单 |
| `06-naming-procedural.md` | 命名违规、God class、死代码、重复 helper |
| `findings-index.md` | 全量 146 项发现索引（severity/category/domain/file:line） |

## 与历史审计的关系（避免重复计数）

- **已知且已裁决不重复报告**：R2c daoFor 基线站点（历次 baseline-raise per-site 裁决）、REQUIRES_NEW R10=14 文档化豁免、aps→mfg/inv §9.4 永久只读豁免、mnt OEE 只读豁免、不用 use-approval/wf 裁决、R3=5 瞬态实体登记。
- **新发现（本次首次落盘）**：全部见本目录；其中 L-6 实体方法上提在 plan 2026-07-20-2200-1 已有登记但其 follow-up 触发条件（新增审批流单据 ≥5）早已满足，本次给出全域量化证据（~400 处）。
- **复发模式**：compliance 基线漂移（lesson 07）第 4 次复发（1057-1 → V.2 → 0651-1 → 本次 M2.5）；过账吞异常悬挂（lesson 09/B1）在 EmployeeAdvance cashRepay 出现半状态变体。

## 残留风险与未覆盖面

- i18n checker 未复跑（F15 基线 2026-07-29，此后大量代码变更，覆盖率漂移未知）
- delta 定制层、api.xml 契约层未深扫（历史 MA1/MA3 已覆盖）
- 部分发现标注「待复核」（有 javadoc/plan 登记但未入 compliance-baseline 豁免体系的站点），修复计划需先逐条裁决
- 本审计**不修改任何代码**；修复须走独立计划（ORM 变更属保护区域，需双独立子 agent 批准）
