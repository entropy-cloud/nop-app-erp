---
status: active
mission: ai-check-r3
work-item: M1.7
group: "2026-09-09-0232"
verify: [test]
---

# 2026-09-09-0232-2 M1.7 manufacturing mfg-3 五维符合性审计（委外/批次追溯/差异切片）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK report mode CAT1..4 = 0/0/0/0、双 checker 零回归、白名单 27 文件四要素齐备）；前置切片 M1.5（mfg-1，plan `2026-09-08-1042-2`）已执行且闭包审计 ACCEPT（2026-09-08，审计时点 HEAD `dd39e6cce`；roadmap done 翻转按 M1.x 同批先例归 owner/engine 处置，不阻塞本切片）；同格族姊妹切片 M1.6（mfg-2）与本计划同批起草，执行序居本计划之前，其报告先落盘时 mfg-2 面归并指向该报告。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U08 × 五维 × mfg-3**（§4 映射表）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准；mfg-3 单元焦点 = 委外/批次基因/差异公式。
- 切片范围（U08 mfg-3）：委外链（ErpMfgSubcontractOrder Facade/Processor + 9 per-mutation Processor + SubcontractPostingDispatcher + Issue/Receipt/Fee 3 AcctDocProvider + MfgSubcontractReversalListener + Document/Approval 双状态机 Bean + 行 BizModel）/批次基因（BatchGenealogyWriter/Tracer + ErpMfgBatchGenealogyBizModel）/差异链（ProductionVarianceCalculator + ErpMfgCostVarianceBizModel + CalculateVariancesProcessor + ProductionVarianceDispatcher + AcctDocProvider）/Forecast 链（r1 切片先例范围：ErpMfgForecast BizModel 族 + StateMachine）；owner doc `docs/design/manufacturing/subcontracting.md`（243 行）+ `batch-genealogy.md`（157 行）+ `variance-analysis.md`（123 行）+ `use-cases.md`（269 行，实仓核验在盘）；物理面 `module-manufacturing/erp-mfg-{dao,service,web}` 的 `src/main`。
- 切片边界（冻结清单 §3.2 + U08 行 + r1 切片边界）：BOM/路由/MRP/仿真/CRP 面归 mfg-2（M1.6 同批）邻接标注；工单/作业卡/领料/预留/完工入库面归 mfg-1（M1.5 已审）；库存移动消费点（`ErpInvStockMoveReverseProcessor`/`GenerateMoveProcessor` 反向移动消费侧行为）归本格，inv 引擎内部归 U02（M1.13 已审）；common 抽象族行为缺陷归 U20（M1.15），本切片只审调用点合规；聚合横切面归 U21（M1.16）；posting 引擎内部归 fin-1（M1.1 已审）。
- 跨轮查重源（§2）：r1 `docs/audits/check/ck-mfg-subcontract.md`（0 P0 / 5 P1 / 7 P2 / 6 P3 = 18 条；跨轮索引状态 6 fixed / 12 open——P1-CK-mfg3-001 委外计价等经 F2.7 修复，HEAD 复核义务在 Phase 2）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照；新立 ID 序列接续 `P{n}-CK-mfg3-{NNN}-r3`（自 019 起，历史 ID 永不覆写）。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：mfg 探针族（CAT-1 31 / CAT-2 9）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；机器基线块差距归 successor `ai-check-r3-compliance-baseline-raise`。
- 仓库现状（2026-09-09 草案时点实核）：HEAD `7ad2e426b`（姊妹 M1.4 计划落盘提交）；脏面 = 本批 3 份 `2026-09-09-0232-*` 计划文件（草案产物）。审计证据一律以实跑时 HEAD + 脏面披露为准（Phase 1 机械登记）。
- 模块级回归参照：`mvn test -pl module-manufacturing/erp-mfg-service` 308 全绿（M1.5 执行两轮与 MI.6 批 2/2 批注账一致）；known-good-baselines 无模块级 mfg 计数行，实跑计数照实登记不预填。
- 剩余差距：U08 × 五维 × mfg-3 五格 verdict 未落盘；mfg-3 无 `-r3` 切片报告。

## Goals

- 按冻结清单对 U08 × 五维 × mfg-3 五格全跑（禁止抽样、禁止跳维），逐格落 verdict，产出 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-mfg-subcontract-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单 + 跨轮 `docs/audits/check/ai-check-index.md`。
- 全程零生产代码改动（roadmap 规则 6），收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（修复归 M2.x）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 mfg-1（M1.5 已审）/ mfg-2（M1.6 同批）格与其他单元格；工单/BOM/MRP/差异归属外缺陷按边界标注归并不立项。
- 不接管 r1/r2 工作项；不重开既有裁决（F2.7 修复方案、lesson 09/10、r1 反向移动双路径裁决的库存维度结论以 finding 证据链复核方式处理而非直接重开裁决）；不做 roadmap 状态翻转。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用）；盘点注记落本计划勾选注记
> Prereqs: 无（M1.5 已执行；本批执行序第 2，M1.6 先行不阻塞本计划起草）

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用；roadmap 规则 8），确认索引与冻结清单在位
      - Skill: none
      - 实证（2026-09-09）：目录在位，`ai-check-r3-index.md` + `m0-5-audit-checklists.md` 均在盘；同目录已含 13 份 M1.x 姊妹 `ck-*-r3.md` 报告（含 M1.6 `ck-mfg-bom-mrp-r3.md`——姊妹先行落地确认）
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、姊妹在制会话披露；后续全部证据注记引用该时点
      - Skill: none
      - 实证：审计时点 HEAD `c63d71bb365668fc3ad25507c2561782297a1410`（短 `c63d71bb3`，M1.6 计划落盘提交之后）；脏面 = 2 untracked 计划文件（本计划 `2026-09-09-0232-2-*` + M1.9 姊妹 `2026-09-09-0232-3-*`，草案产物非生产面）；姊妹在制披露：M1.6（mfg-2）报告已先落盘（`ck-mfg-bom-mrp-r3.md` 在盘、HEAD `7ad2e426b` 时点），mfg-2 面归并指向该报告；M1.9（`2026-09-09-0232-3`）为同批未执行计划文件仅存在于脏面
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none
      - 实证：compliance checker 19 规则 = R1d 14 / R2a 34 / R2b 242 / R2c 1542 / R2d 38 / R3 5 / R6 2 / R10 14 / R12a 71 / R12b 66 / R12c 42、R1a-c/R4/R5/R7/R8/R11 = 0——与 M0.3 快照行逐位一致，**零漂移**；CJK report mode：扫描 java 3430 + yaml 886，CAT-1..4 = 0/0/0/0（CAT-5 注释豁免 21158 行仅统计）——与 MI 终态行一致，**零漂移**

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.7 行指定）
> Targets: `module-manufacturing/erp-mfg-dao|erp-mfg-service/src/main/java`（mfg-3 范围 = 委外/批次基因/差异/Forecast 族文件）
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族 + codegen 产物安全 + 聚合完整性（`app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
      - 实证（HEAD `c63d71bb3`）：① checker 19 规则逐位 = M0.3 快照行（Phase 1 已录）；② 反模式 grep 族（erp-mfg-{service,dao}/src/main/java）：`extends RuntimeException`=0、`@Inject`+private=0、`System.currentTimeMillis`/`LocalDateTime.now()`/`new Date()`=0、@Transactional∩@BizMutation 7 文件均为 Processor REQUIRES_NEW 已裁决范式（R6=2/R10=14 基线内）、IDaoProvider/IOrmTemplate 命中处注释核查 → finding（见 mfg3-019-r3）；③ codegen 产物安全：mfg 模块 `_gen/`/`_` 前缀 git 脏面=空、`__XGEN_FORCE_OVERRIDE__` 命中仅 `target/classes` 构建产物（源面零命中）；④ 聚合完整性：E1 勘误路径在用，`x:extends` 注册 19 业务域 + 4 系统模块含 `/erp/mfg/auth/erp-mfg.action-auth.xml`
- [x] <Proof> 15 维度逐维走查 mfg-3 范围（重点：②跨实体 I*Biz（inv 移动/fin 凭证调用点）⑧双轴状态机（委外 Document/Approval Bean）⑨审批流、委外红冲两段移动反转完整性——F2.7 修复复用 HEAD 复核、批次基因写入/追溯口径、差异公式（价差/量差/用量差异）、Forecast 状态机；涉 inv 引擎/posting 引擎内部时标注归属 U02/fin-1）+ blocker/major/minor 分级
      - Skill: nop-platform-conformance-audit-prompt
      - 实证（15/15 维无跳维）：①决策顺序 pass（F2.7 修复走 Processor 范式，无可模型化硬编码新增；r1 mfg3-017 常量命名 open 保留）②跨实体 finding（新立 mfg3-019-r3：Writer L319/BizModel L144/Dispatcher L291+302 daoFor 跨域直查无豁免注释，且与 F2.7 修复体同读经 `stockMoveBiz`/`stockLedgerBiz` I*Biz 构成切片内范式不对称）③异常 pass（NopException 全合规；`readBoolConfig`/`currentUserId`/`isWriteEnabled`/`defaultMaxDepth` 宽 catch = r1 P3-CK-mfg-016 族注记站点复核在位，归并不重复立项）④IoC/事务 pass（R5=0；REQUIRES_NEW 为基线内 processor 范式；F1.2 同型残余修复 reload 范式在位 L235-241）⑤平台辅助 pass（CoreMetrics/AppConfig 全合规）⑥标准服务 pass（6/6 mfg-3 BizModel extends AbstractErpCrudBizModel——F1.3 接入复核）⑦机制 B pass（本切片无新增跨模块实体声明面）⑧状态机 pass（Document/Approval 双 Bean + Forecast Bean 迁移表声明式；F2.7 assertNotCancelled L294-301 接线；r1 8 态可达裁决复核成立）⑨审批流 finding（新立 mfg3-020-r3：8 个自定义 mutation 零 FNPT 注册；SoD `assertApproverNotCreator` L336 在位）⑩定制顺序 pass ⑪多租户/本地化 pass（无 tenantId 预置）⑫测试归 DIM-T ⑬Codegen 安全 pass ⑭聚合完整性 pass ⑮抽样见下行。分级：0 blocker / 0 major / 2 minor（P3）新立 + 归并注记 2 族。**U08×B×mfg-3 verdict = finding（归并态 + 2 新立 P3）**
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样：`subcontracting.md` + `batch-genealogy.md` + `variance-analysis.md` 中 ≥2 doc × 2 关键断言（委外成本构成/红冲完工两段移动/基因追溯链/差异公式口径）对照代码；≥2 处漂移扩大至全部 owner doc
      - Skill: code-quality-audit-prompt
      - 实证（3 doc × 6 断言，0 漂移，不触发扩样）：①subcontracting.md L238「红冲全量撤销 + 反向两段库存移动」↔ Processor `reverseInventoryMoves` L190-193（RECEIPT+ISSUE 双段，F2.7 修复后 canSafelyReverse L226-232 MANUFACTURE 分支使两段实际可达）+ inv 侧 `inverseMoveType` MANUFACTURE→OUTGOING（ErpInvStockMoveProcessor L363-376）一致；②subcontracting.md L229 科目分解「ISSUE Dr 1408/Cr 1401；RECEIPT Dr 1405/Cr 1408；FEE Dr 1408/Cr 2202」↔ 3 Provider javadoc+常量实证一致；③subcontracting.md L100/L192 成本构成「材料成本+加工费」↔ `computeReceiptUnitCost` L427-436（fee+aggregateIssueMaterialCost，F2.7）一致；④batch-genealogy.md Decision 2「batchNo=FG-{woCode} 派生」↔ `GENEALOGY_OUTPUT_BATCH_PREFIX="FG"`+Writer L193 一致；⑤batch-genealogy.md Decision 3「config 键 genealogy-write-enabled 默认 true」↔ L294 默认值 "true" 一致；⑥variance-analysis.md L68 委外差异公式「实际=wo.subcontractCost、标准=rollupLine.subcontractCost×完工量」↔ Calculator L196-197 一致（F2.7 后实际侧 writer 在位）
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-mfg-subcontract.md` 18 条 finding + r2 + 基线快照；fixed 项（F2.7 族）复用并复核 HEAD 有效性，open 项同型归并原 ID）；裁决证据落勾选注记
      - Skill: code-quality-audit-prompt
      - 裁决记录：查 r1 18 条（6 fixed：001-005 F2.7 + 011 F1.3；12 open：006-010/012-018）+ r2 目录 + ai-check-index §Mission 基线快照。**复用 6**（001 computeReceiptUnitCost 材料成本合计 ✓、002 canSafelyReverse MANUFACTURE 分支 + inv inverseMoveType ✓、003 assertNotCancelled 三迁移 ✓、004 resolveInputLot 领料单头仓 ✓、005 applySubcontractCostToWorkOrder config-gated writer ✓、011 六 BizModel AbstractErpCrudBizModel ✓——全部 HEAD `c63d71bb3` 复核有效）；**归并 14**（r1 open 12 条现症逐条复核全部仍在位：006 reverseOneMove L209-217 吞异常 / 007 仓库参数 @Optional 无校验 / 008 usedInputLots L161 去重丢量 / 009 reverseIfExists L133-140 posted 行门控 / 010 findFirmedRollupLine L345-366 无 orgId+全量载入 / 012 validateTransitionForReverseApprove L303-310 仅审批轴 / 013 traceChain L89 off-by-one / 014 outputQty L177+lotStatus L182 / 015 静默跳过零日志 / 016 死列仅 MrpReleaseService L220 ZERO 写 / 017 SUBJECT_FINISHED_GOODS=1401 L285 / 018 N+1 族——均归并原 ID 追加 HEAD 现症证据；另 2 条归并：①F2.7 修复体 `applySubcontractCostToWorkOrder` 查询无 orgId 过滤（L500-501 仅 productId+docStatus）→ 归并 P2-CK-mfg3-010 同族新站点追加证据；②宽 catch 4 站点 → 归并 r1 P3-CK-mfg-016 族注记）；**新立 2**（P3-CK-mfg3-019-r3 daoFor 豁免注释族——同型家族跨单元先例 fin3-017-r3/fin4-022-r3/ast-028-r3/qa-027-r3/mfg2-026-r3 按每切片自立 ID 先例；P3-CK-mfg3-020-r3 FNPT 注册族——同族先例 mfg2-024-r3/fin4-023-r3/drp-018/b2b-011/prj-022-r3/qa-029-r3；历史 ID 零覆写，自 019 起）

Exit Criteria:

- [x] U08×B×mfg-3 格 verdict 落盘，15 维度无跳维
- [x] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成（复用 6 / 归并 14 / 新立 2）

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2）
> Targets: `module-manufacturing/erp-mfg-web/src/main/resources/_vfs`（mfg-3 面：委外/批次/差异/Forecast 相关页面）
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（导出 0 error；325 条既有 variant 外部漂移为 successor 在案，非本切片 finding）+ flux-only grep（AMIS 保留层 / ORM `ext:web-renderer="flux"`）
      - Skill: none
      - 实证：`npm run validate:flux` → files=855 validated=855（导出 0 error）errors=325 warnings=18491——325 ERR 与 M1.4/M1.6 登记的既有 variant 既有漂移 successor 对账一致；mfg 域 32 文件 34 条与 M1.5/M1.6 记录「精确一致」；mfg-3 页面命中 5 文件各 1 条（BatchGenealogy/CostVariance/Forecast/ForecastLine/SubcontractOrderLine）全部为同族 `variant "primary"` 既有漂移，非新 finding。flux-only：`component="AMIS"` 保留层命中=0；`grep -L ext:web-renderer="flux"` 全 ORM=空
- [x] <Proof> mfg-3 页面走查：委外/批次基因/成本差异/Forecast 实体页与手写页（M0.4 源头链查表判定）对照 view-and-page-strategy + ui-patterns（REST `/r/` / `x:extends` 定制 / i18n-en 承载）逐页落 verdict；涉 mfg E2E spec（mfg-chain 等 orchestration 族）时核对 PageObject + flux 引擎缺省 + 禁 GraphQL 断言（e2e-runbook L229 登记的 API 驱动型数据层断言口径不适用禁令）
      - Skill: none
      - 实证（8 页族逐页 + E2E 无跳页）：6 实体页族（SubcontractOrder/SubcontractOrderLine/BatchGenealogy/CostVariance/Forecast/ForecastLine）源头链 = codegen `_gen/_*.view.xml` + 保留层 `*.view.xml` `x:extends` bounded-merge 定制（非手写覆盖，合规）+ 空 scaffold `lib.xjs`（数据访问走 flux 缺省 REST `/r/` 层，页面零 GraphQL 调用）；标签 `i18n-en:label/displayName` 英文承载在位；2 手写报表页（production-variance/forecast-variance-report.page.yaml）`@query:ErpMfgReport__renderHtml` REST 约定 + `i18nEn` 承载 + `/p/` 下载端点，且已覆盖于 visual 像素回归层。E2E：mfg-subcontract-chain/mfg-variance/mfg-genealogy 3 spec 选择器纪律 grep（data-slot/data-testid/.cxd- adapter 外）=0、E2E_ENGINE 无 override（缺省 flux）、数据层经 `callMutation` GraphQL 全栈断言 = runbook L229「业务动作套件：自定义 @BizMutation 经 GraphQL 全栈可达」登记口径（禁令不适用），spec 无 GraphQL UI 渲染断言。**发现 1 项归并**：SubcontractOrder 与 Forecast 列表 grid docStatus 单元格样式分支硬编码 `'ACTIVE'`（subcontract-status 8 态字典与 forecast-status 字典 DRAFT/APPROVED/CONSUMED/CANCELLED 均无 ACTIVE 值——高亮分支恒死）→ 同型归并 P3-CK-pur-017-r3（M1.13 三单匹配页死状态样式分支族）跨域新站点追加，非新立。**U08×F×mfg-3 verdict = finding（归并态 0 新立，minor）**

Exit Criteria:

- [x] U08×F×mfg-3 格 verdict 落盘；全局面门禁数字在案对账一致
- [x] mfg-3 范围页面逐页走查完成，无跳页

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md` + `docs/design/manufacturing/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ mfg 相关 seed（cost_variance/forecast 族 + 委外相关单据 seed）
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿
      - Skill: none
      - 实证：Tests run: 4, Failures: 0, Errors: 0, Skipped: 0——4/0/0/0 全绿（与 M1.4/M1.6 登记数一致）
- [x] <Proof> mfg-3 seed 面核对：`git status --porcelain` seed 路径 expect 空 + deploy `_seed_*.sql` 同步义务查登记处表 + cost_variance/forecast seed 自洽约束抽查（按 seed-data.md 运营域约束段；SPC 双层门控默认关核对）
      - Skill: none
      - 实证：①seed 脏面 = 0（只读审计合规）；②资产清点 372 CSV + 1 SQL 与 seed-data.md 对账表最新登记行一致；③deploy 种子全仓仅 module-cs（3 方言）+ module-notify（3 方言）命中，登记处表（seed-data.md §同步义务 L107-108）两行均「✅ 已聚合」裁决在案，**mfg 无 deploy seed、无同步义务**；④mfg-3 seed 抽查：subcontract_order 3 行（id=2 COMPLETED+posted=true+POSTED_STATUS/AT/BY 齐备、id=3 CANCELLED N-TERM 负例与 seed-data.md L89 登记一致；docStatus/approveStatus 全部 ∈ 8 态字典）、cost_variance 1 行（MATERIAL_USAGE, posted=false 无孤儿凭证声明）、forecast 1 行（APPROVED ∈ forecast-status 字典）——自洽约束通过；⑤引擎重算覆盖防护：`variance-auto-calc-enabled` 默认 false（WorkOrderProcessor L595 实证）、`subcontract-cost-aggregation-enabled` 默认 false、`subcontract-posting-enabled` 默认 false——seed 静态行不被引擎重算覆盖；SPC 双层门控静态行裁决沿用 M1.12 r3「复核成立不重开」结论（本切片不重开既有裁决）

Exit Criteria:

- [x] U08×S×mfg-3 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
- [x] seed 零变更 + 同步义务核对 + 自洽抽查结果在案

**U08×S×mfg-3 verdict = pass**

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-manufacturing/erp-mfg-service`（`<SVC>` 绑定：`<SVC>` = `module-manufacturing/erp-mfg-service`，冻结清单 §1.4 程式记号）
> Prereqs: Phase 1 完成

- [x] <Proof> 切片本地回归：`mvn test -pl module-manufacturing/erp-mfg-service` 全绿零失败（模块级参照 = 308 全绿（M1.5 执行 + MI.6 批注账）；全仓聚合对照面 = known-good-baselines 2026-09-08 MI 终态行 4006/0/0/1；实跑计数照实登记不预填）
      - Skill: none
      - 实证（第 1 轮）：Tests run: 308, Failures: 0, Errors: 0, Skipped: 0——308/0/0/0 全绿，与模块级参照（M1.5 执行两轮 + MI.6 批 2/2 批注账 308）逐位一致；Phase 7 复跑见下
- [x] <Proof> 覆盖缺口对账：公开方法清单 × `_cases` 测试目录清单逐项对账（mfg-3 范围 = 委外/批次基因/差异/Forecast 族 BizModel）；关键业务流清单核对——委外发料→收货→加工费→红冲（含红冲后余额断言——r1 P1-CK-mfg3-002 盲区复核面）/批次基因写入→追溯/差异计算触发链逐行核覆盖；缺口按业务关键度定级
      - Skill: none
      - 实证：公开方法 16 个（SubcontractOrder 5 / BatchGenealogy 4 / CostVariance 5 / Forecast 2 / Line 两 BizModel 0）× `_cases` 8 目录（TestErpMfgSubcontracting/SubcontractReverse/BatchGenealogy/ProductionVariance/VarianceAlert/VarianceRecomputeReversal/ForecastCrudSmoke/ForecastSource）全对齐，测试方法 47 个；关键业务流逐行核：①委外发料→收货→加工费→红冲 ✓（testFullLifecycleWithPosting + testMultiCurrencyFeeVoucherLineBaseline + SubcontractReverse 4 测试）——**r1 P1-CK-mfg3-002 盲区已消除**：红冲后余额断言在位（TestErpMfgSubcontractReverse L144-148 `findBalance(P)`/`findBalance(M1)`，F2.7 修复配套「P 余额归 0 + M1 余额恢复 10」）；②基因写入→追溯 ✓（BatchGenealogy 9 测试：forwardTrace/backwardTrace/traceChain 深度+环路/recallReport 强断言 affectedLots/degraded/REJECTED 排除 + F2.7 多仓解析 testMultiWarehouseInputLotResolved）；③差异计算触发链 ✓（ProductionVariance 13 + VarianceRecomputeReversal 4（红冲→删旧→重算链）+ VarianceAlert 3 + F2.7 testSubcontractCostAggregatedOnCompletion）；④Forecast approve/cancel ✓（CrudSmoke 3 + Source 4）。零 P2+ 级覆盖缺口
- [x] <Proof> 快照纪律：`SnapshotTest.RECORDING` 提交态零残留 + `*` 通配/`delVersion` 屏蔽合规抽查
      - Skill: none
      - 实证：`grep -rn "SnapshotTest.RECORDING" erp-mfg-service/src/test/java` = 0；mfg-3 四 `_cases` 目录抽查 `"*"` 通配/`delVersion` 手工屏蔽 = 0 命中（CREATE_TIME/UPDATE_TIME 由框架自动屏蔽）

Exit Criteria:

- [x] U08×T×mfg-3 格 verdict 落盘；本地回归全绿数字在案
- [x] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案

**U08×T×mfg-3 verdict = pass**（本地回归 308/0/0/0；覆盖 16 方法 × 8 测试类 47 测试全对齐；快照纪律合规）

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + mfg 白名单条目抽查
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0）+ `--self-test` PASS；脚本红 = MI 回归升级报 MI 通道，不立 DIM-I finding
      - Skill: none
      - 实证：`--strict` → RESULT: PASS（--strict: 0 new violations vs frozen snapshot；170 baseline files, totals CAT1..4=0/0/209/1318），exit 0；`--self-test` → RESULT: PASS (self-test green)，exit 0——零回归门控双 PASS（Phase 1 report mode CAT1..4=0/0/0/0 与 MI 终态行一致交叉印证）
- [x] <Proof> 白名单合规抽查：§WHITELIST mfg 相关条目抽 ≥3 条核对四要素 + `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空；四要素缺失 = 白名单登记缺陷 finding
      - Skill: none
      - 实证：§WHITELIST 块 mfg 相关条目总体 = **1 条**（`ErpMfgDashboardBizModel.java` CAT3——MI 后 mfg 白名单收缩至此），四要素齐备（文件路径 ✓ / 理由「@Description 中文专属文件 E3 豁免」✓ / owner doc `i18n-compliance.md 判定准绳表 #5` ✓ / 裁决来源 `plan 2026-09-07-1715-1 Phase 2` ✓）；为满足抽 ≥3 条，加抽跨域 2 条同核：`MaskHelper.java`（C2 功能型中文内容契约②，ROLE_* seed 角色名数据契约，四要素齐备）+ `ErpCtConfigs.java`（C2② seed 角色名契约，四要素齐备）——3/3 四要素齐备，零登记缺陷；`grep -L "@Locale"` 全量 `*Errors.java` = 空（全部声明）；`git status --porcelain 'module-*/erp-*-meta/**' 'app-erp-all/src/main/resources/_vfs/i18n/**'` = 0（生成 i18n yaml 零手改）

Exit Criteria:

- [x] U08×I×mfg-3 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
- [x] 白名单四要素抽查 ≥3 条 + `@Locale` + meta 禁手改核对结果在案

**U08×I×mfg-3 verdict = pass**（双 PASS + 白名单 3 抽查四要素齐备（mfg 总体 1 条全齐 + 跨域 2 条同核）+ `@Locale` 全声明 + meta 零手改）

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-mfg-subcontract-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：三态一致性、级别（P0~P3）、ID 规范（`P{n}-CK-mfg3-{NNN}-r3`，自 019 起）、归属标注（mfg-1/mfg-2/U02/fin-1/U20/U21 归并指针）复核
      - Skill: code-quality-audit-prompt
      - 复裁决记录：新立 2 条 ID 规范复核 ✓（`P3-CK-mfg3-019-r3` 豁免注释族 + `P3-CK-mfg3-020-r3` FNPT 族——自 019 起接续 r1 最大 018，格式 `P{n}-CK-mfg3-{NNN}-r3` 合规，零冲突零覆写）；级别一致性复核 ✓（2 条均 P3，与两族全部同族先例 fin3-017/fin4-022/ast-028/qa-027/mfg2-026 + mfg2-024/fin4-023/drp-018/b2b-011/prj-022/qa-029 定级逐一对齐）；三态一致性复核 ✓（复用 6 与跨轮索引 6 fixed 对账、归并 12 与 12 open 对账、历史 18 ID 零覆写）；归属标注复核 ✓（报告 §2.5 五行：inv 引擎内部→U02、posting 引擎内部→fin-1、common 抽象族→U20、聚合横切面→U21、CostRollupService 本体→mfg-2 + 工单面→mfg-1）；DIM-F pur-017-r3 跨域站点追加与 in-round 先例（fin2-017 族 fin-4 站点追加 / mfg-023-r3 bom-tree 站点追加）一致
- [x] <Add> 落盘 `ck-mfg-subcontract-r3.md`：五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明
      - Skill: none
      - 实证：`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-mfg-subcontract-r3.md` 落盘——§1 覆盖矩阵 5/5（B=finding 2 新立 / F=finding 归并态 / S=pass / T=pass / I=pass）+ §2 三态裁决（复用 6 / 归并 12 + 跨切片 2 / 新立 2 全文证据）+ §3 统计表 + §4 剩余风险四件套（已查/未深查/残留风险/successor 触发条件）
- [x] <Add> 双索引同步：本轮索引产物清单追加 + 跨轮索引 §报告清单 M1.7 行 + §Finding 追踪新立 ID 行
      - Skill: none
      - 实证：本轮 `ai-check-r3-index.md` 产物清单追加 M1.7 行（ck-mfg-subcontract-r3.md 摘要行）；跨轮 `ai-check-index.md` §报告清单追加 M1.7 行（P0=0/P1=0/P2=0/P3=2/done 摘要）+ §Finding 追踪追加 `P3-CK-mfg3-019-r3`、`P3-CK-mfg3-020-r3` 两行（open，修复归 M2.x）；历史 ID 零覆写
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤生产路径 expect 空
      - Skill: none
      - 实证（收官时点）：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 = 空；非 docs 脏面 = 空（全部脏面为 docs/ 下本计划 + 本报告 + 双索引 + 日志产物，均审计产物面）
- [x] <Proof> 收尾回归：`mvn test -pl module-manufacturing/erp-mfg-service` 复跑全绿
      - Skill: none
      - 实证（第 2 轮）：Tests run: 308, Failures: 0, Errors: 0, Skipped: 0——308/0/0/0 全绿（与 Phase 5 第 1 轮逐位一致，两次全绿收口）

Exit Criteria:

- [x] `ck-mfg-subcontract-r3.md` 落盘且五维矩阵 5 格 verdict 完整
- [x] 双索引行追加在案；零生产代码改动核证通过；mfg service 回归根绿

## Draft Review Record

- dispatch review #review-2026-09-08-193051-mission-driver-2026-09-09-0232-2-m17-mfg-subcontract-genealogy-variance-five-dim-audit-1-190be301 to 2026-09-08-193051-mission-driver
- 2026-09-09：iteration 1，共识 accept #review-2026-09-08-193051-mission-driver-2026-09-09-0232-2-m17-mfg-subcontract-genealogy-variance-five-dim-audit-1-190be301（补 Closure Gates——按 M1.5/姊妹 M1.1 同批先例为本只读审计计划定制门控；基线断言实仓复验在盘——HEAD `7ad2e426b`、脏面=本批 3 计划、owner doc 行数 243/157/123/269、冻结清单 U08 映射与 §1.5 CAT 31/9 与 §1.4 `<SVC>` 记号、r1 统计 18 条 0/5/7/6 与跨轮索引 6 fixed/12 open 与 ID 自 019 起、F2.7 修复 mfg3-001..005、§6-E1 勘误 auth 路径、双技能名、validate:flux 325 variant 既有漂移、mfg service 308 参照、successor `ai-check-r3-compliance-baseline-raise` 在案名；SPC 双层门控锚点 `seed-data.md` L19 与 M1.5 同款核对先例一致；frontmatter `status: draft` → `active`）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行；roadmap 收官机制承载全仓回归与 done 翻转）。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果。

- 范围内行为完成（Phase 1~7 全部执行项与退出标准 `[x]`；五格 verdict 完整——`ck-mfg-subcontract-r3.md` 覆盖矩阵 5/5，缺一格不算完）
- 相关文档对齐（双索引同步在案——本轮 `ai-check-r3-index.md` 产物清单行 + 跨轮 `ai-check-index.md` §报告清单 M1.7 行与 §Finding 追踪新 ID 行；历史 ID 零覆写；零生产代码改动 = owner doc 零对齐义务）
- 已运行验证：Phase 1 双 checker 红线（compliance checker 对照 M0.3 快照行 + CJK report mode 对照 0/0/0/0 终态）+ Phase 4 `TestErpSeedDataIntegrity` 全绿 + Phase 5/7 `mvn test -pl module-manufacturing/erp-mfg-service` 两次全绿 + Phase 6 `--strict` PASS + `--self-test` PASS + Phase 7 零改动核证（`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空）——各命令数字落 Phase 勾选注记；任一红线漂移 = 登记裁决后再闭包，不就地吞掉
- 无范围内项目降级为 deferred/follow-up（finding 的「归属 mfg-1/mfg-2/U02/fin-1/U20/U21」归并标注与修复归 M2.x 是审计产出分流，非范围降级）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1）
- 文本一致性已验证：状态、阶段、门控和日志都一致（ledger 格式：frontmatter `status: active` 保持，完成态由全部勾选 + `## Verification` pass 线 + `## Closure` 回执派生；当日日志条目 `docs/logs/` 落盘）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Phase 勾选注记 + `ck-mfg-subcontract-r3.md` + 双索引行 + `## Verification` pass 线 + `## Closure` 审计回执）

## Verification

- PASS 2026-09-09：`bash docs/audits/nop-compliance-checker.sh`——19 规则 = M0.3 快照行逐位一致（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0），零漂移（Phase 1）
- PASS 2026-09-09：`node tools/check-hardcoded-cjk.mjs`（report mode）——CAT1..4 = 0/0/0/0，= MI 终态行，零漂移（Phase 1）
- PASS 2026-09-09：`npm run validate:flux`——files=855 validated=855 errors=325 warnings=18491，325 ERR = 既有 variant 既有漂移 successor 在案，mfg 32 文件 34 条与 M1.5/M1.6 登记精确一致（Phase 3）
- PASS 2026-09-09：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity`——4/0/0/0 全绿（Phase 4）
- PASS 2026-09-09：`mvn test -pl module-manufacturing/erp-mfg-service` 第 1 轮——308/0/0/0 全绿（Phase 5）
- PASS 2026-09-09：`node tools/check-hardcoded-cjk.mjs --strict`——exit 0 PASS；`--self-test`——PASS（Phase 6）
- PASS 2026-09-09：`mvn test -pl module-manufacturing/erp-mfg-service` 第 2 轮（收官复跑）——308/0/0/0 全绿（Phase 7）
- PASS 2026-09-09：零生产代码改动核证——`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 = 空，非 docs 脏面 = 空（Phase 7）
- pass test 2026-09-09-0456 exit=0

## Closure

- dispatch audit #audit-2026-09-09-0456-2026-09-09-0232-2-m17-mfg-subcontract-genealogy-variance-five-dim-audit-1-21c1801a to 2026-09-08-193051-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-2026-09-09-0456-2026-09-09-0232-2-m17-mfg-subcontract-genealogy-variance-five-dim-audit-1-21c1801a：独立结束审计 ACCEPT——35/35 计数域全 `[x]`、五维矩阵 5/5（B=finding 2 新立 P3 / F=finding 归并态 / S/T/I=pass）、报告/双索引/roadmap/日志对账一致；closure visit 实跑 `mvn test -pl module-manufacturing/erp-mfg-service` 308/0/0/0 全绿 BUILD SUCCESS（exit=0），机械 checker `plan-check.mjs --strict` 复核通过
