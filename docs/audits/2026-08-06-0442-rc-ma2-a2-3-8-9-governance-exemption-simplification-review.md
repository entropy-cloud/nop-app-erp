# rc-ma2-a2-3-8-9-governance-exemption-simplification-review MA2 治理豁免类方案 B 复查报告（A2.3 + A2.8 + A2.9 + A2.4-A2.7 空集认证）

> Plan Status: completed
> 产出时间：2026-08-06
> 来源 Plan：`docs/plans/2026-08-06-0442-1-rc-ma2-a2-3-8-9-governance-exemption-simplification-review.md`（Work Item A2.3 + A2.8 + A2.9，规则 14 合并）
> Mission：requirement-compliance（MA2 方案 B 关闭项复查）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §4「显式人工批准记录」三判据 / §5 Q4 修复义务 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议 + §MA2↔MA3 协作）
> 路线图：`docs/backlog/requirement-compliance-roadmap.md`（A2.3 mfg 1 项 + A2.8 扩展域 1 项 + A2.9 跨域 1 项 + A2.4/A2.5/A2.6/A2.7 空集认证）
> 复查全集：`docs/audits/rc-existing-inventory.md`（A2.3 1 项 + A2.8 1 项 + A2.9 1 项 + §对账差异登记 #1 + §集成排序）
> Skill：`docs/skills/open-ended-audit-prompt.md`
> 审计性质：**只读审计**——读 plan / owner doc / product-scope / arm-index / git 历史 / checker reporter 裁决证据，**不修改任何代码/ORM/api.xml/真相源**

---

## §导出口径与复查对象

本报告复查对象 = M0.3（`rc-existing-inventory.md`）导出的「治理豁免登记」类方案 B 关闭项 **3 项**（A2.3 mfg 1 项 + A2.8 扩展域 1 项 + A2.9 跨域 1 项），均为 §对账差异登记 #1 列出的「实质等同治理豁免」组（P1-MA2-038 / P1-MA1-029 / P1-MA1-022；同组第 4 项 P1-MA1-016 已在 `2026-08-06-1400-rc-ma2-a2-1-2` finance 报告 done，本报告不重复）。每项逐条应用方法论 §4「显式人工批准记录」三判据 (i)→(ii)→(iii) 核证，区分「有意设计（保留 P2 successor）」vs「静默降级（重开 MR1）」。同时认证 A2.4/A2.5/A2.6/A2.7 空分区（0 项方案 B），为 MA2 全里程碑收尾提供证据。

**Q4 修复义务绑定**（§5）：3 项均为 **P1**（非 P0），无 P0-MA2-018 那样的 Q4 强制重开张力。Q4=(a) 要求 P1 必须实现**除非**经 §4 三判据证明为「有意设计」——即 (i)/(ii)/(iii) 满足其一 → 有意设计（保留 P2 successor）；均不满足 → 静默降级重开 MR1（R1.0 展开为 RC-R1.n）。鉴于 3 项均有 owner doc 豁免登记（(ii) 候选），复查焦点 = §4(i) 独立 plan-audit 通过记录是否成立。

**与 MA3 的两面关系**（§MA2↔MA3 协作）：本报告只复查**方案 B 关闭裁决本身是否正当**（有意设计 vs 静默降级）；successor 触发条件是否回队属 A3.x（P1-MA2-038→A3.2 委外收敛 / P1-MA1-022→A3.2+A3.5 md 迁移 / P1-MA1-029→A3.5 pur/sal Facade），独立 plan，交叉引用不重复。

---

## 1. 方案 B 关闭项清单 + 锚点（段 1，§6 MA2 适配）

| # | Finding ID | 域 | arm-index 关闭方式标签 | owner doc 锚点（§4(ii) 审查对象） | 关闭 plan（§4(i) 审查对象） | successor（→ A3.x） | 分区 |
|---|-----------|----|---------------------|-------------------------------|--------------------------|---------------------|------|
| 1 | `P1-MA2-038` | mfg | `resolved（同域委外写豁免扩展登记于 posting-exemptions.md §MrpReleaseService）` | `architecture/posting-exemptions.md §MrpReleaseService`（含 P1-MA2-038 覆盖性裁决 :33） | `2026-07-29-2225-1-cross-domain-daofor-governance-adjudication.md`（completed） | 收敛条件=委外域提供 purpose-built `createFromMrpLineForSubcontract` I*Biz 时收敛（→ A3.2） | A2.3 |
| 2 | `P1-MA1-029` | contract（→ pur/sal 跨域写） | `resolved（写侧豁免补登于 posting-exemptions.md §ErpCtInvoicePlanBizModel）` | `architecture/posting-exemptions.md §ErpCtInvoicePlanBizModel`（:72-90） | `2026-07-29-2225-1`（completed） | 收敛条件=pur/sal 域提供 purpose-built `createFromInvoicePlan` I*Biz 时收敛（→ A3.5） | A2.8 |
| 3 | `P1-MA1-022` | pur+sal+ast+inv+mnt+prj+qa+drp+aps（**9 域合并**） | `resolved（读侧统一裁决：md 目标域子集=可迁移[successor 已命名] / fin·inv·mfg 目标域子集=永久只读豁免）` | `architecture/data-dependency-matrix.md §9 跨域只读访问裁决`（:854-901） | `2026-07-29-2225-1`（completed） | md 子集=可迁移（触发=master-data I*Biz 补便捷只读方法 → A3.2+A3.5）；fin·inv·mfg·sal·ast 子集=永久只读豁免；Dashboard facade read-only 聚合永久接受（→ A3.2/A3.5） | A2.9 |

**三源核查**：3 项的 arm-index 关闭方式标签（arm-index.md :462/:463/:484 实测）+ owner doc 锚点（posting-exemptions.md :8-33/:72-90 + data-dependency-matrix.md :854-901 实测）+ 关闭 plan（`2026-07-29-2225-1`，全部 completed）三源一致。**3 项均由同一治理统一裁决 plan（`2026-07-29-2225-1`）关闭**——该 plan 同时关闭了 A2.2 的 P1-MA1-016（已在 `2026-08-06-1400-rc-ma2-a2-1-2` 复查 done），属同组同 plan 同关闭方式（§对账差异 #1 同组实质等同）。

---

## 2. §4 三判据逐项证据（段 2）

> 判据应用顺序 (i)→(ii)→(iii)；判据三仅当 (i)/(ii) 均不成立时兜底。**代理独立审计通过 = 「审计裁决质量证据」（可区分「静默降级」vs「经审计裁决的简化」），不算人工批准**（方法论 §4）。证据来源 = 关闭 plan 的 `Draft Review Record` / `## Closure`（§4(i)）+ owner doc 显式标注 + git 批准痕迹（§4(ii)）+ product-scope（§4(iii)）。

### 2.1 `P1-MA2-038`（mfg 同域委外写豁免扩展）— 判据 (i) 成立

- **(i) plan 含独立 plan-audit 通过记录**：**成立**。关闭 plan `2026-07-29-2225-1` 含 `Draft Review Record`（2 轮迭代，独立子代理 `ses_051b8f106` iteration 1 needs revision → iteration 2 acceptable-as-is）+ `Closure Audit Evidence`（独立结束审计子代理 `ses_051a2e94`，新会话未执行本 plan，逐项核实产物 + 实仓代码抽查三项事实性核实全部属实，**Verdict: PASS，全部 Closure Gates 可满足**）。该 plan 的 Phase 2 Explore\|Add 项明确裁决「既有 MrpReleaseService 条目原仅覆盖 mfg→pur ErpPurOrder（跨域写）；P1-MA2-038 的 ErpMfgSubcontractOrder 是同域目标（mfg→mfg），属不同写入路径」并扩展既有条目范围声明（非新增独立条目，因同一 MrpReleaseService + 同一 MRP 释放业务场景 + 同一 O-4 豁免根因）。
- **(ii) owner doc 显式 documented simplification 标注且经人工批准**：owner doc 标注存在（`posting-exemptions.md §MrpReleaseService:8-33` 含显式豁免登记：位置 / 写入目标[跨域写 ErpPurOrder + 同域写绕审批 ErpMfgSubcontractOrder] / 触发场景 / 理由 / 风险 / 补偿机制 / 收敛条件 + `:33` P1-MA2-038 覆盖性裁决），但批准来源 = 独立子代理审计（非人工）。git 历史核实（`git log -- docs/architecture/posting-exemptions.md`）：补登提交 `973c12abc`（`canonical <canonical_entropy@163.com>`，2026-07-29，subject 含 R1.5+R1.7 跨域 daoFor 治理统一裁决）——commit author=committer=canonical 即项目操作者账户提交的 AI 生成工作流（AGE），无独立「人工批准豁免」讨论/PR 审批痕迹可追溯（区别于 §4(ii) 要求的「批准来源可追溯（git log / commit message / 讨论文档）」——此处的 commit 是操作者对 AI 子代理产物的合流动作，非「人工批准该豁免决策」的独立痕迹）。依方法论「代理独立审计不算人工批准」+「AI 自写标注不算」，(ii) 单独**不成立**——但 (i) 已成立，无需触发 (ii) 的人工批准判定。
- **(iii) product-scope 范围裁剪登记**：**不成立**。product-scope.md 无委外/MRP 释放/同域写豁免相关范围裁剪条目（`rg -i "委外|subcontract|MRP|同域写|O-4" docs/requirements/product-scope.md` 仅命中项目阶段叙述性提及「manufacturing」域名，无豁免登记）。
- **核证结论**：判据 (i) 成立 → 该简化经独立审计裁决（区别于静默降级）。裁决理由（governed-path eval §3.1 裁决分支 b：I*Biz 强注入破坏单模块测试启动 + MRP 释放场景 config-gated `erp-mfg.subcontract-release-enabled` 默认 false 控制风险暴露面 + 委外单 postedStatus=DRAFT 须再经 SubcontractPostingDispatcher 过账不自动过账）有 owner-doc + 实仓证据支撑。

### 2.2 `P1-MA1-029`（contract→pur/sal 跨域写半治理）— 判据 (i) 成立

- **(i) plan 含独立 plan-audit 通过记录**：**成立**。同 plan `2026-07-29-2225-1`（见 2.1）的 Phase 2 Add 项明确补登 P1-MA1-029（`ErpCtInvoicePlanBizModel` contract→pur/sal 跨域写），格式对齐既有 3 条豁免条目（位置/触发场景/理由/风险/补偿机制/收敛条件）。Draft Review Record + Closure Audit Evidence 同 2.1（同一 plan 同一会话）。
- **(ii) owner doc 显式标注且经人工批准**：owner doc 标注存在（`posting-exemptions.md §ErpCtInvoicePlanBizModel:72-90` 含显式豁免登记：位置 `ErpCtInvoicePlanBizModel:127,147,159,164,182,184,196` / 写入目标 ErpPurInvoice+ErpPurInvoiceLine+ErpSalInvoice+ErpSalInvoiceLine / 触发 `triggerInvoice`/`triggerDuePlans` / config-gated `erp-ct.invoiceplan-auto-trigger` 默认 true / 理由（governed-path branch b + javadoc :41-45）/ 风险（DRAFT/UNSUBMITTED 草稿不自动过账）/ 补偿（合同 ACTIVE 守卫 + @BizMutation 事务原子 + 权限校验）/ 收敛条件[待 pur/sal 提供 createFromInvoicePlan I*Biz]），批准来源 = 独立子代理审计（同 2.1 git trace，canonical 合流 AI 产物，非独立人工批准痕迹）。(ii) 单独**不成立**但 (i) 已成立。
- **(iii) product-scope 范围裁剪登记**：**不成立**。product-scope.md 无合同开票计划/跨域发票草稿生成相关范围裁剪条目（`rg -i "InvoicePlan|发票计划|开票计划|跨域写|governed" docs/requirements/product-scope.md` 零命中）。
- **核证结论**：判据 (i) 成立 → 经审计裁决的简化。裁决理由（governed-path eval §3.1 裁决分支 b：硬注入跨域发票 BizModel 会将完整服务依赖链级联进合同域破坏隔离单元测试 + 发票草稿为纯实体构造不经 submit/approve 业务管道 + A2.1 P2P 运行时复核[2026-07-27] 确认生成 unposted UNSUBMITTED DRAFT 经 purchase 正常审批+过账管道 + 生成行无 receiveLineId 三单匹配跳过对齐 owner doc「回链可选」+ 无 APPROVED 脏发票产生业务正确性不受影响）有 owner-doc + 实仓证据支撑。

### 2.3 `P1-MA1-022`（9 域跨域只读 daoFor 读侧统一裁决）— 判据 (i) 成立（归类核实）

- **(i) plan 含独立 plan-audit 通过记录**：**成立**。同 plan `2026-07-29-2225-1` 的 Phase 1 Decision 项明确读侧 daoFor 分类裁决（md 目标域=可迁移[successor 已命名] / fin·inv·mfg·sal·ast 目标域=永久只读豁免[受 nop-entropy lazy/SPI 解耦阻塞]），落地于 `data-dependency-matrix.md §9.1-9.4`。Draft Review Record + Closure Audit Evidence 同 2.1。**特别核实**：该 plan 的 Draft Review iteration 1 独立子代理 `ses_051b8f106` 明确修正了 3 项事实性错误（P1-MA4-012 方向 / P1-MA2-038 mfg→pur 应为 mfg→mfg 同域 / P1-MA4-003 md/ast 应为 md），iteration 2 验证全部修复——独立审计发挥了实质纠错作用（非橡皮图章）。
- **(ii) owner doc 显式标注且经人工批准**：owner doc 标注存在（`data-dependency-matrix.md §9 跨域只读访问裁决:854-901` 含完整裁决：§9.1 裁决原则 + md-service classpath 实测校正[15 域 compile-scope / fin+logistics test-scope] + §9.2 八项 finding 逐项分类表 + §9.3 汇总[可迁移 ~65+ + 可迁移前置 scope 提升 ~26 + 永久只读豁免 ~20] + §9.4 选择记录与残留风险），批准来源 = 独立子代理审计（同 2.1 git trace）。(ii) 单独**不成立**但 (i) 已成立。
- **(iii) product-scope 范围裁剪登记**：**不成立**。product-scope.md 无跨域只读 daoFor/I*Biz 解耦/平台 lazy-SPI 相关范围裁剪条目（`rg -i "跨域|daoFor|只读豁免|governed|lazy" docs/requirements/product-scope.md` 仅命中项目阶段叙述性「跨工程实体引用 → master-data via notGenCode」即 ORM 导航机制 B，与 daoFor 读侧治理豁免不同语义）。
- **归类核实**（M0.3 §对账差异登记 #1 要求 + 本计划 Baseline）：arm-index 关闭标签字面为 `resolved（读侧统一裁决...永久只读豁免）`，非 `方案 B / documented simplification / Deferred` 三标签之一。**本复查裁决归类恰当**：该关闭**无生产代码逻辑变更以修复 finding 本身**——9 域的全部 daoFor 只读调用站点全部保留（pur/sal/ast/inv/mnt/prj/qa/drp/aps 的 posting dispatcher + cost resolver + report facade 均未迁移），关闭方式 = 在 architecture owner doc 登记「读侧统一裁决」+ 命名 successor 条件（md=可迁移 / fin·inv·mfg=永久豁免），与方法论 §4(ii)「owner doc 显式 documented simplification 标注」实质同构。属 MA2 复查范围，归类 KEEP 成立（与 `2026-08-06-1400-rc-ma2-a2-1-2` 对 P1-MA1-016 同组同 plan 的归类核实结论一致）。
- **核证结论**：判据 (i) 成立 → 经审计裁决的简化（读侧统一裁决 + 分类 successor 命名，受平台 SPI 阻塞的子集永久豁免 + md 子集可迁移 successor 已命名）。

---

## 3. 实现证据（段 3，复用既有 arm 审计，§去重协议）

> 本复查为方案 B 关闭项复查（需求契约视角），不重做 doc↔code 文本一致性。实现证据复用既有 arm MA1/MA2/MA4 报告已证实的代码路径，仅列锚点供 §4 三判据核证溯源。

| Finding ID | 代码锚点（复用 arm MA1/MA2/MA4 已证实） | 既有证实报告 |
|-----------|--------------------------------------|-------------|
| P1-MA2-038 | `MrpReleaseService.releaseToSubcontractOrder:185-216`（mfg 同域写）经 `daoProvider.daoFor(ErpMfgSubcontractOrder).saveEntity` 直接持久化委外单为 APPROVED 终态（`:199-201` setDocStatus(APPROVED)+setApproveStatus(APPROVED)）+ 委外单行（`:205-214`）+ javadoc:202「O-4 架构豁免」+ config-gated `erp-mfg.subcontract-release-enabled` 默认 false | `2026-07-2*-arm-ma2-mfg-state-machine`（mfg 状态机行为，委外单 APPROVED 绕审批 O-4）；`A4.2b ma4-mfg-mrp-quality-code-quality`（MrpReleaseService 行为已证实） |
| P1-MA1-029 | `ErpCtInvoicePlanBizModel.java:159,196`（contract→pur/sal 跨域写）经 `daoProvider().daoFor(ErpPurInvoiceLine/ErpSalInvoiceLine.class).saveEntity(...)` 跨域持久化目标域发票行实体（绕 IErpPurInvoiceBiz/IErpSalInvoiceBiz 审批管道）+ javadoc:41-45 bypass rationale（避免服务依赖级联）+ 生成 unposted UNSUBMITTED DRAFT | `2026-07-23-0000-architecture-governance-review`（首审 F1 已识别 ErpCtInvoicePlanBizModel 为 bypass rationale 文件）；`2026-07-27-1949-arm-ma2-procure-to-pay-e2e`（A2.1 P2P 运行时复核：生成草稿经 purchase 正常审批+过账管道，无 APPROVED 脏发票，业务正确性不受影响） |
| P1-MA1-022 | 9 域 posting dispatcher + cost resolver + report facade 的 `daoFor(ErpMd*/ErpFin*/ErpInv*/ErpMfg*)` 只读调用（详见 arm-index :462 枚举 pur/sal/ast/inv/mnt/prj/qa/drp/aps 各站点，A1.12 4 域 + A1.13 扩展 5 域）+ A4.1a-A4.5 代码质量审计复核的 fin/mfg/ast/pur/sal/inv 投影站点（P1-MA4-003/006/008/012/015/022） | `2026-07-27-1227-arm-ma1-cross-module-dag:257`；`A4.1a-A4.5`（代码质量报告，read-only 无活跃数据破坏）；`2026-07-28-1510-arm-ma3-design-doc-baseline`（doc↔code drift 视角） |

---

## 4. 运行时行为证据（段 4，复用既有 arm MA1/MA2/MA3/MA4，§去重协议）

> 本 mission MA2 = 方案 B 关闭项复查（需求契约视角），与 audit-remediation MA1（架构治理/跨模块 DAG）/ MA2（状态机/链路行为）/ MA3（doc↔code drift）/ MA4（代码质量）维度不重叠（方法论 §去重协议 §MA2(本)↔MA3(audit-remediation) 边界）。既有 arm 报告已证实的运行时行为直接引用：

- **P1-MA2-038**：mfg 同域委外写经 MrpReleaseService.releaseToSubcontractOrder 创建 ErpMfgSubcontractOrder 为 APPROVED 终态（绕 O-4 审批），但 postedStatus=DRAFT 须再经 manufacturing 域 SubcontractPostingDispatcher 过账（不自动过账），加工费=0 须采购员补录，config-gated 默认 false 控制风险暴露面——经 mfg 状态机 + A4.2b 报告复核确认 read-only/同域写无跨模块数据破坏。
- **P1-MA1-029**：contract→pur/sal 跨域写生成 unposted UNSUBMITTED DRAFT 发票草稿，后续经 purchase/sales 正常审批+过账管道；生成行无 receiveLineId 致三单匹配跳过（对齐 owner doc「回链可选」）；无 APPROVED 脏发票产生——经 P2P E2E 报告（`2026-07-27-1949`）证实业务正确性不受影响，维持治理层 finding 不升级。
- **P1-MA1-022**：9 域跨域只读 daoFor 返回的是托管实体，读语义与 I*Biz 等价（差异仅在「绕过目标域业务规则封装」，读侧业务规则通常为空或已由调用方保证）；MA2 状态机运行时复核 + A4.1a-A4.5 代码质量审计均确认「仅治理缺陷，不破坏运行时正确性」，read-only 无跨域数据破坏；arm-index :462 已含 A1.48 drp 复核注记（UC-DRP-02/04 跨域聚合 DrpDemandAggregator/DrpReleaseService daoFor 复用本 finding 作行为证据）。

---

## 5. 符合性结论（段 5，§6 MA2 适配：复查结论 + 是否重开 MR1 + A2.4-A2.7 空集认证）

> 复查结论二分：`有意设计（保留 P2 successor）`（§4 三判据满足其一）/ `静默降级（重开 MR1）`（三判据均不满足）。3 项均为 P1，无 P0 Q4 强制重开张力。

### 5.1 逐项复查结论 + 三源对照

| Finding ID | §4 (i) plan-audit 通过 | §4 (ii) owner doc 标注+人工批准 | §4 (iii) product-scope 裁剪 | 命中判据 | 复查结论 | 重开路由 |
|-----------|----------------------|-------------------------------|---------------------------|---------|---------|---------|
| `P1-MA2-038` | ✅（plan 2225-1，2 轮 + closure PASS[ses_051a2e94]） | ⚠️（标注有，人工批准=子代理审计非人工；git canonical 合流非独立批准痕迹） | ❌ | **(i)** | **有意设计（保留 P2 successor）** | 不重开 |
| `P1-MA1-029` | ✅（plan 2225-1，2 轮 + closure PASS） | ⚠️（同上） | ❌ | **(i)** | **有意设计（保留 P2 successor）** | 不重开 |
| `P1-MA1-022` | ✅（plan 2225-1，2 轮含 iteration 1 三项事实性纠错 + closure PASS） | ⚠️（豁免裁决登记于 §9，人工批准=子代理审计） | ❌ | **(i)** | **有意设计（保留 P2 successor）** | 不重开（归类恰当，§对账差异 #1 核实通过） |

**三源对照声明**（每项 arm-index 关闭标签 vs owner doc 标注 vs product-scope）：3 项 owner doc 标注与 arm-index 关闭标签语义一致（M0.3 导出已校验）；product-scope 全部无范围裁剪登记（§4(iii) 全不成立，(i) 已分别裁决）。**§对账差异登记 #1 归类核实**：3 项归类 KEEP 成立（详见 §2.3 归类核实段 + 2.1/2.2 暗含），关闭均无生产代码逻辑变更以修复 finding 本身（P1-MA2-038 MrpReleaseService O-4 写保留 / P1-MA1-029 ErpCtInvoicePlanBizModel 跨域写保留 / P1-MA1-022 9 域 daoFor 只读调用保留），关闭方式 = 登记 governance 豁免 + 文档化，与方法论 §4(ii) 实质同构。

### 5.2 统计

- **重开 MR1**：0 项（3 项均 §4(i) 成立 → 有意设计）
- **有意设计（保留 P2 successor）**：3 项（P1-MA2-038 / P1-MA1-029 / P1-MA1-022）
- **本审计新发现 P0**：0 项（无 MR0 即时通道触发）

### 5.3 A2.4 / A2.5 / A2.6 / A2.7 空集认证（M0.3 §导出口径自检 + §集成排序为证）

引用 M0.3 `rc-existing-inventory.md §集成排序`（:196-207）+ §导出口径自检（:90-113）：

| A2.x 行 | 域 | 方案 B 项数 | 认证依据 |
|---------|----|-----------|---------|
| A2.4 | hr | **0** | 全部已关闭 P1 finding 经导出口径筛选均为实现修复项（`resolved (R1.x done)` / `fixed` / `方案 A 实现`），0 项方案 B 关闭项 |
| A2.5 | purchase + sales | **0** | 同上（P1-MA2-001 GRNI 冲回归 A2.1 finance 会计保护区域；082/083 承付族跨域项 0.3 按主域归 A2.1/A2.9，非本行方案 B） |
| A2.6 | assets + inventory | **0** | 同上（P1-MA2-024/085 系实现修复、089 系并发实现修复，0.3 复核排除） |
| A2.7 | projects + quality | **0** | 同上（P1-MA2-064~070 经 0.3 复核关闭方式标签均为实现修复关闭，非方案 B） |

**空集认证结论**：A2.4/A2.5/A2.6/A2.7 四行 MA2 复查范围 = **空**（0 项方案 B），可直接标 done（roadmap MA2 详情已授权空分区直接 done，单独开 4 空分区计划属过度拆分）。各域 successor 项（如 hr 离职族 / ast IDLE / qa 联动等）归 MA3 A3.x 复查（见 rc-existing-inventory §successor 三源对账清单），不属本 MA2 范围。

---

## 6. 与 arm-index 衔接（段 6，§7「复用 or 新增」裁决）

> §7 规则：本复查的 3 项均为既有 arm finding，原则上**复用既有 ID 追加 RC 注记**；仅当发现新根因/新控制点/新维度才新建 `P*-RC-xxx`。

### 6.1 逐项「复用 or 新增」裁决

| Finding ID | arm-index grep 结果（同域同控制点） | 裁决 | 操作 |
|-----------|--------------------------------------|------|------|
| `P1-MA2-038` | 既有 arm finding（mfg 同域委外写 O-4 豁免），无新根因（同一 MrpReleaseService.releaseToSubcontractOrder 写路径 + 同一 O-4 豁免根因），MA2 复查确认方案 B 关闭裁决正当 | **复用** | 既有行（arm-index :484）追加「RC MA2 复查：有意设计，§4(i) 成立，保留 P2 successor（→A3.2 委外收敛）」注记 |
| `P1-MA1-029` | 既有 arm finding（contract→pur/sal 跨域写半治理），MA2 复查确认方案 B 关闭裁决正当 | **复用** | 既有行（arm-index :463）追加「RC MA2 复查：有意设计，§4(i) 成立，保留 P2 successor（→A3.5 pur/sal Facade）」注记 |
| `P1-MA1-022` | 既有 arm finding（9 域跨域只读 daoFor 读侧统一裁决），MA2 复查确认归类恰当 + 方案 B 关闭裁决正当；arm-index :462 已含 A1.48 drp RC 视角复核注记（UC-DRP-02/04 复用本 finding 作行为证据） | **复用** | 既有行（arm-index :462）追加「RC MA2 复查：有意设计，§4(i) 成立，归类恰当（§对账差异 #1 KEEP），保留 P2 successor（→A3.2 md 迁移 + A3.5 md 跨域 successor）」注记 |

**裁决依据**：3 项均为既有 arm finding 的同一根因/同一控制点（MrpReleaseService O-4 同域写 / ErpCtInvoicePlanBizModel 跨域写 / 9 域跨域只读 daoFor），MA2 复查仅做「方案 B 关闭裁决正当性」裁决（需求契约视角），无新根因/新控制点/新维度——全部复用既有 ID，**不新建 P*-RC-xxx**（禁止未经比对直接新建）。

### 6.2 双向可追溯

- **有意设计项 ↔ A3.x successor 复查**（两面关系，方法论 §MA2↔MA3 协作）：
  - `P1-MA2-038` → A3.2（mfg+inv+pur successor 复查）：successor「委外单收敛为 createFromMrpLineForSubcontract I*Biz」触发条件复查属 A3.2，独立 plan；本 A2.3 只复查关闭裁决正当性。
  - `P1-MA1-029` → A3.5（扩展域+跨域 successor 复查）：successor「contract InvoicePlan 跨域写收敛为 createFromInvoicePlan I*Biz」触发条件复查属 A3.5，独立 plan；本 A2.8 只复查关闭裁决正当性。
  - `P1-MA1-022` → A3.2（md 目标域子集=可迁移 successor）+ A3.5（md 跨域 successor 触发=master-data I*Biz 补便捷只读方法）：successor 触发条件复查属 A3.2/A3.5，独立 plan；本 A2.9 只复查关闭裁决正当性 + 读侧豁免分类（永久豁免 vs 可迁移 successor）正当性。
- **无重开项**：3 项均 §4(i) 成立 → 有意设计，无 MR1 R1.0 预留展开行（区别于 `2026-08-06-1400-rc-ma2-a2-1-2` 的 P0-MA2-018 重开路由）。
- **arm-index 回填**：§6.1 注记已写入 `arm-index.md`（既有 3 行追加 RC 注记，非新分区）。

---

## 7. 静态存疑点清单（段 7，供 MA4 A4.1/A4.2 展开）

> L5 无法静态定论、需运行时确认的点。本复查为方案 B 关闭项复查（读 plan/owner doc/product-scope/git），以下为复查中静态无法定论、建议 MA4 运行时确认的点：

1. **P1-MA1-029 config-gated `erp-ct.invoiceplan-auto-trigger` 默认 true 的实际触发面**：owner doc 标注该 config 默认 true（`triggerDuePlans` 批量触发受此控制），区别于 P1-MA2-038（`erp-mfg.subcontract-release-enabled` 默认 false）+ P1-MA1-022（无 config gate，纯只读）。默认 true 意味着开票计划到期即自动跨域生成 pur/sal 发票草稿——虽然生成的是 DRAFT/UNSUBMITTED 不自动过账（业务正确性不受影响，§4 P2P 复核已证实），但「批量触发」在高合同量场景下的实际发票草稿生成频率 + 是否对下游 pur/sal 审批队列造成显著负载需运行时确认。**静态已确认无活跃数据破坏**（生成草稿须经正常审批管道），此存疑点为容量/负载观察项，非正确性项——交接 A3.5 successor 复查时附带评估，MA4 无需运行时负向测试。

> 其余 2 项（P1-MA2-038 / P1-MA1-022）的运行时行为已由既有 arm MA1/MA2/MA4 报告充分证实（§4 read-only / config-gated false / 同域写 postedStatus=DRAFT 不自动过账，无活跃数据破坏），无新增静态存疑点。

---

## 8. 过程纪律自检（段 8，§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（actual 见下表，退出码 = 0）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0，不反映 actual vs baseline），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以 checker 脚本退出码作为门控通过依据**。**本审计无生产代码变更（纯审计报告 + arm-index 文档注记），checker 无回归风险**——actual 计数与本审计行为正交（未触及任何生产代码），任何 actual vs baseline 差异均非本审计引入。

  | 规则 | 基线（compliance-baseline.md） | actual（本次实测） | 漂移 | 归因 |
  |------|-------------------------------|-------------------|------|------|
  | R1d | 14 | 14 | 0 | — |
  | R2a | 34 | 34 | 0 | — |
  | R2b | 240 | 229 | -11 | 非本审计引入（本审计零代码变更；漂移源自既有生产代码演化——R2b 计数下降表明既有 daoFor 站点经重构减少，与本审计正交） |
  | R2c | 1380 | 1382 | +2 | 同上（与 `2026-08-06-1400-rc-ma2-a2-1-2` §8 表一致，漂移在本审计执行前已存在） |
  | R2d | 32 | 34 | +2 | 同上 |

  > 本审计仅产出本报告 + `arm-index.md` 注记（纯文档），未触及 `module-*/` 任何生产代码。actual 与基线的差异在本审计执行前已存在（与 finance MA2 报告实测一致），非本审计行为导致，故无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计（见来源 plan Closure Gates）。
- [x] **与 arm-index 交叉去重声明**：本报告全部 3 项 finding 已按 §7 规则 grep arm-index 同域同控制点后给出「复用」裁决（§6.1），无未经比对直接新建的 `P*-RC-xxx` finding。

---

## 9. 与既有审计差异增量声明（段 9，§去重协议）

本报告与既有 arm 审计（`docs/audits/2026-07-2*-arm-ma1-*` / `arm-ma2-*` / `arm-ma3-*` / `arm-ma4-*`）+ 先行 RC MA2 报告（`2026-08-06-1400-rc-ma2-a2-1-2`）的差异增量：

- **复用既有证据**（不重复验证）：
  - `2026-07-23-0000-architecture-governance-review`（首审 F1 已识别 P1-MA1-029 ErpCtInvoicePlanBizModel 为 bypass rationale 文件）；
  - `2026-07-27-1949-arm-ma2-procure-to-pay-e2e`（P1-MA1-029 跨域写生成 DRAFT 草稿业务正确性不受影响已证实）；
  - `2026-07-27-1227-arm-ma1-cross-module-dag:257`（P1-MA1-022 9 域跨域只读 DAG 行为已证实）；
  - `2026-07-2*-arm-ma2-mfg-state-machine`（P1-MA2-038 mfg 委外单 O-4 状态机行为已证实）；
  - `A4.1a-A4.5 ma4-*-code-quality`（P1-MA1-022 各域投影 P1-MA4-003/006/008/012/015/022 read-only 无活跃数据破坏已证实）；
  - `2026-07-28-1510-arm-ma3-design-doc-baseline`（doc↔code drift 视角，与本文档需求契约视角正交）；
  - `2026-08-06-1400-rc-ma2-a2-1-2`（先行 RC MA2 finance 报告：P1-MA1-016 同 plan 2225-1 同关闭方式同 §对账差异 #1 组已复查 done，本报告对剩余 3 项同组项沿用相同 §4 三判据 + 归类核实方法）。

- **本复查只补的差异增量**：**需求契约 vs 治理豁免类方案 B 关闭裁决正当性**——从 §4 三判据（i）plan-audit 通过 / (ii) owner doc 显式标注+人工批准 / (iii) product-scope 范围裁剪 出发，逐项核证 arm-index「治理豁免登记」类（§对账差异 #1 实质等同组：标签字面非三标签之一但实质等同 governance 豁免 + 文档化，无生产代码逻辑变更）关闭项的关闭裁决是否「有意设计（经审计裁决）」vs「静默降级」+ §对账差异 #1 归类核实（豁免登记是否构成完整治理闭环=实现修复，或=方案 B 文档化简化）。这是既有 arm 审计（架构治理 / 状态机行为 / doc↔code / 代码质量维度）+ 先行 RC MA2 finance 报告（已 done 的 A2.1/A2.2）未覆盖的「A2.3 mfg + A2.8 扩展域 + A2.9 跨域」治理豁免类关闭裁决正当性维度（方法论 §去重协议 §MA2(本)↔MA3(audit-remediation) 边界）。

- **不重复**：不重做架构治理审查（首审已收口）、不重做 doc↔code 文本一致性（audit-remediation MA3 已收口）、不重做状态机/链路行为（arm MA2 已收口）、不重做代码质量（arm MA4 已收口）、不裁决 successor 是否回队（属 MA3 A3.2/A3.5，独立 plan）、不复查 finance 域（A2.1/A2.2 done，P1-MA1-016 同组已在 `2026-08-06-1400-rc-ma2-a2-1-2` done）。

---

## 结论

治理豁免类方案 B 关闭项 MA2 复查（A2.3 + A2.8 + A2.9）+ A2.4-A2.7 空集认证完成。

- **重开 MR1**：0 项（3 项均 §4(i) 成立 → 有意设计）。
- **有意设计（保留 P2 successor）**：3 项（`P1-MA2-038` mfg 同域委外写豁免 / `P1-MA1-029` contract→pur/sal 跨域写半治理 / `P1-MA1-022` 9 域跨域只读读侧统一裁决，均经独立 plan-audit 通过记录 → 经审计裁决的简化）。
- **§对账差异登记 #1 归类核实**：3 项归类 KEEP 全部成立（关闭均无生产代码逻辑变更以修复 finding 本身，关闭方式 = 登记 governance 豁免 + 文档化，与方法论 §4(ii) 实质同构）。
- **A2.4 / A2.5 / A2.6 / A2.7 空集认证**：4 行均 0 项方案 B（全部为实现修复关闭），可直接标 done。
- **arm-index 衔接**：3 项全部复用既有 ID 追加 RC 注记（无新 `P*-RC-xxx`）；无重开项故无 MR1 R1.0 预留展开行；successor 两面交叉引用 A3.2/A3.5。
- **MA2 收尾**：本报告落地后 MA2 全部 A2.x 行 done（A2.1+A2.2 done 于 `2026-08-06-1400-rc-ma2-a2-1-2`；A2.3+A2.8+A2.9 done 于本报告；A2.4-A2.7 空集 done 于本报告 §5.3）。
- **本审计无生产代码变更**（纯报告 + arm-index 文档注记），§9 真相源冻结条款遵守（未修改 product-scope / owner doc 需求契约段落 / arm-index 已关闭 finding 的关闭事实）。
