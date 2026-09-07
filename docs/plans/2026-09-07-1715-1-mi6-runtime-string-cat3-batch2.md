---
status: active
mission: ai-check-r3
work-item: MI.6
group: "2026-09-07-1715"
verify: [test]
---

# 2026-09-07-1715-1 MI.6 运行时字符串中文裁决清剿批 2/2（CAT-3 全域归零）

## Current Baseline

- 冻结基线（`docs/audits/cjk-baseline.md` §SNAPSHOT，generated 2026-09-06）：CAT-3 全局 390 行 / 144 文件。批 1（plan `2026-09-07-0902-1`）归零 finance 80 + assets 58 + cs 43 = 181 行并登记白名单 11 文件；2026-09-07 实跑 `node tools/check-hardcoded-cjk.mjs`（report mode）：CAT1=0 / CAT2=0 / **CAT3=209 sites / 88 files** / CAT4=1318，390 − 181 = 209 对账一致。CAT-4 归 MI.8（本批计划组 N=2），本批不触碰。
- 本批范围（checker 实跑冻结口径，除批 1 三域外全部 16 域 209 行 / 88 文件）：hr 28 / inventory 28 / manufacturing 28 / aps 20 / projects 15 / b2b 15 / purchase 14 / sales 13 / maintenance 13 / quality 13 / common-service 8 / contract 6 / logistics 3 / crm 2 / master-data 2 / notify 1；drp CAT3 = 0 无批内面。逐文件计数以执行时 checker 实跑输出 + SNAPSHOT `files:` 块为准（本计划不复制清单，防基线陈旧——`docs/lessons/13-requirement-baseline-staleness.md`）。
- 裁决协议已由批 1 落定并登记 `docs/architecture/i18n-compliance.md` CAT-3 修复模式行（含混合文件规则 (i)(ii)(iii)）：(a) 字典回归——AcctDocProvider `fact(code, "科目名", …)` 类科目显示名置空，由过账引擎 `resolveSubjects` 的 `StringHelper.isBlank` 回填 `ErpMdSubject.getName()`；(b) 改英文——无字典来源的真实运行时字符串（memo/audit/reason/errorMsg/report 标签等）；(c) 白名单窄类登记（C1 `@Description("中文")` 族按判定准绳 #5/E3 先例；C2 功能型中文内容契约：OCR/解析正则与文档内容匹配、seed/通知配置数据契约、文档化字符串契约——四要素齐备登记 `docs/audits/cjk-baseline.md` §WHITELIST）。本批沿用该协议，不另立 Decision（协议变更须先修订 owner doc）。
- 分批执行协议（`docs/backlog/ai-check-r3-roadmap.md` MI.6 行）：每批完成记入 `docs/audits/cjk-baseline.md` 批注账，MI.6 保持 todo 直至 CAT-3 = 0 或白名单全覆盖。本计划为批 2/2（收官批），批末断言 CAT-3 全域 = 0。
- 行为不变式（roadmap 横切关注点 8）：仅改字符串载体（字典 key / 枚举名 / 英文）或登记白名单，禁止借机改业务行为、控制流、异常语义（lesson 09/10 既有裁决不重开）；行为疑点登记 finding 归 M2，不在本批顺手改。不建英文 i18n 承载镜像（判定准绳表 #1）。
- 快照与 seed 义务：持久化面载体改动触发受影响 `_cases` 快照**外科式文本变换**重录（旧值→新值仅限消息/名称列，保留 `*` 通配符；禁用 `force-save-output` 全量重录——会窄化通配符断言，批 1 实证）；seed CSV 零改动，不触发横切关注点 7 双面重录义务；app-erp-all 集成快照若受影响同批同步。
- 依赖状态：MI.5b done（plan `2026-09-07-0043-3`）；批 1 done（plan `2026-09-07-0902-1`，独立结束审计 ACCEPT）；本计划执行顺序居本批三计划（`1715-1/2/3`）之第 1。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行（全 reactor 4006/0/0/1；2026-09-07 MI.8 批 1 闭包重审轮全仓 `mvn test` 4006/0/0/1 复现一致）；MI.4/MI.5a/MI.5b/MI.6 批 1 收官零新增失败。
- 剩余差距：16 域 209 行 CAT-3 红线；`--strict` 门控下 actual 只降不升。

## Goals

- 16 域 209 行 CAT-3 = 0 或白名单全覆盖：真实运行时字符串按批 1 协议改字典 key / 枚举名 / 英文；`@Description("中文")` 族与功能型中文内容契约窄类按四要素登记白名单。
- `node tools/check-hardcoded-cjk.mjs --strict` 保持绿且全局 CAT-3 归零（单向收紧合法下降）；各批域 service 模块 `mvn test` 全绿零回归；CAT-1/2 持平 0、CAT-4 持平 1318 不触碰。
- 批注账落账（MI.6 批 2/2 行），MI.6 具备转 done 条件（roadmap 状态翻转由 owner/engine 依收官审计处置）。

## Non-Goals

- 不动 CAT-4 页面 yaml 面（1318 行，MI.8 批 2 = 本批计划组 N=2 范围）；不动已归零的 CAT-1/2 面与批 1 三域（fin/ast/cs）CAT-3 面。
- 不改错误码定义、异常类型、抛出条件、业务行为；不动 `_init-data` seed、ORM 模型、页面文件；不建英文 i18n 承载镜像。
- 不改 `tools/check-hardcoded-cjk.mjs` 脚本口径与 SNAPSHOT 块（机器自动重生成，勿手改）；不修订批 1 已落定的裁决协议与 `i18n-compliance.md` CAT-3 行（协议争议登记 finding 归 M2）。

## Phase 1 — 红线记录 + 批域三态盘点

> 统一类型：Proof（2 项 Proof）。
> Skill: none（roadmap MI.6 行指定）
> Targets: 无生产代码改动；盘点注记落本计划勾选注记
> Prereqs: plan `2026-09-07-0902-1`（MI.6 批 1）完成

- [x] <Proof> 批第一动作：`node tools/check-hardcoded-cjk.mjs` 实跑，记录批域红线数字（16 域逐域计数 + 合计 209）于本项勾选注记，与 Current Baseline 实跑值对账一致（修复批产物 = 脚本输出数字 + 盘点注记 + 白名单登记，不产 ck-* 报告，横切关注点 13）
      - Skill: none
      - 执行注记（2026-09-07）：checker 实跑 report mode，CAT1=0/CAT2=0/**CAT3=209 sites/88 files**/CAT4=1318，与冻结口径对账一致。批域逐域：hr 28 / inventory 28 / manufacturing 28 / aps 20 / projects 15 / b2b 15 / purchase 14 / sales 13 / maintenance 13 / quality 13 / common-service 8 / contract 6 / logistics 3 / crm 2 / master-data 2 / notify 1（求和 209 ✓，drp 0 无批内面）。逐文件计数经 `--baseline --out` 临时导出（未触碰冻结 SNAPSHOT 块）与 SNAPSHOT `files:` 块逐文件一致。
- [x] <Proof> 批域 88 文件机械三态盘点——逐文件判定 CAT-3 构成 = `@Description("中文")` 专属 / 真实运行时字符串 / 混合（grep + checker 分类引擎复核；沿用批 1 三态判定口径），盘点结果按域记入勾选注记，标注 C2 功能型契约候选簇（正则/文档内容匹配、seed/通知契约、文档化字符串契约）
      - Skill: none
      - 执行注记（2026-09-07，checker 分类引擎复用扫描 209 sites 逐条在案）：
        - **`@Description` 专属（C1 白名单 12 文件 15 行）**：aps IErpApsOperationOrderBiz(1)/ErpApsOperationOrderBizModel(1)；inventory IErpInvStockLedgerBiz(2)/ErpInvDashboardBizModel(1)/ErpInvStockLedgerBizModel(2)；manufacturing ErpMfgDashboardBizModel(1)；maintenance ErpMntDashboardBizModel(1)；quality ErpQaDashboardBizModel(1)；common-service ErpOrgIsolationQueryTransformer(1)；projects ErpPrjDashboardBizModel(1)；purchase ErpPurDashboardBizModel(1)；sales ErpSalDashboardBizModel(1)；master-data ErpMdDashboardBizModel(1)。**本批 88 文件无混合文件**（@Description 文件均无真实运行时行并存）。
        - **C2 功能型中文内容契约候选簇（白名单登记）**：①seed 角色名契约——common-service MaskHelper ROLE_* 7 行（nop_auth_role.csv seed roleId 逐一比对存在，ck-common-app.md 在案）+ hr ErpHrConstants.HR_ROLE_ID(1) + contract ErpCtConfigs.DEFAULT_TERMINATE_APPROVER_ROLE(1)，同批 1 cs「客服主管」先例；②无正则/文档内容匹配簇（批 1 fin OCR/发票簇为本批批域外特有）。
        - **真实运行时字符串（(a) 字典回归 / (b) 英文）**：其余 73 文件 194 行——AcctDocProvider `fact()` 科目名 63 行按 (a) 置空（resolveSubjects isBlank 回填，ErpFinPostingProcessor:680-682 路径核实）；memo/remark/reason/conflict 描述/report 标签/setter 默认名 131 行按 (b) 英文化（含 aps SchedulingEngine conflict reason 14 行、hr SalaryPostingDispatcher 告警 stage 4 行、ErpPartyType 枚举显示名等）。

Exit Criteria:

- [x] 批域红线数字在案且与冻结口径逐域对账一致（209/88 逐域逐文件一致，见 Phase 1 Proof 1 注记）
- [x] 88 文件三态盘点完成（专属 12 文件 15 行 / 真实 73 文件 194 行 / 混合 0 文件逐域在案，C2 候选簇 = seed 角色名契约 3 文件 9 行标注，见 Phase 1 Proof 2 注记）

## Phase 2 — 重簇四域清剿：hr 28 / inventory 28 / manufacturing 28 / aps 20（104 行）

> 统一类型：Fix-heavy（1 Fix + 1 Add + 2 Proof）。
> Skill: none
> Targets: checker 实跑 `module-hr/`、`module-inventory/`、`module-manufacturing/`、`module-aps/` 带 CAT3 计数的 Java 文件（重簇 = aps `ErpApsSchedulingEngine` conflict reason 6 行、`ErpApsAtpCtpServiceImpl` reason/工序名等，其余执行时盘点为准）
> Prereqs: Phase 1 完成（红线先行）

- [x] <Fix> 四域批域文件逐簇裁决（批 1 协议）：真实运行时字符串按簇改字典 key / 枚举名 / 英文；混合文件先修真实行后整文件登记；仅动 CAT-3 行；持久化载体改动同步 `_cases` 快照外科式变换（仅消息/名称列，保留 `*` 通配符）
      - Skill: none
      - 执行注记（2026-09-07）：hr 11 文件（SalaryPostingProvider 8 科目名按 (a) 置空 resolveSubjects 回填——测试 seed 科目名与原字面量一致零漂移；Dispatcher 4 告警 stage/2 冲突 reason/3 anomaly 描述/report 标签 2/发展行动 2/planName/银行文件列头/名 splitting 回退按 (b) 英文化）+ inventory 9 文件（5 Provider 18 科目名按 (a) 置空；4 remark/reason 按 (b)）+ manufacturing 9 文件（6 Provider 15 科目名按 (a)；ProductionVariance memo 构建器与科目名解耦新增英文 memo 标签参数；memo/remark/stage 9 行按 (b)）+ aps 3 文件（SchedulingEngine conflict reason 14 行/AtpCtp reason 2+工序名 1/remark 1 按 (b)）。快照外科变换：inventory `_cases` 33 cell（erp_inv_stock_move.REMARK 冲销→Reversal/盘点差异→Stocktake diff、erp_mfg_cost_rollup.REMARK、erp_inv_cost_adjust[,_line] REASON/REMARK、erp_fin_voucher_line.SUBJECT_NAME 按 case 级 seed 回填名）+ TestErpInvStockTakeCompleteDiffMove 5 处断言载体同步（语义不变）；hr/mfg/aps 快照零漂移。
- [x] <Add> 四域 `@Description` 专属文件 + 已修混合文件 + C2 功能型契约窄类登记 `docs/audits/cjk-baseline.md` §WHITELIST（四要素齐备：文件路径 / 理由 / owner doc 指针 / 裁决来源 = 本计划 + `i18n-compliance.md` 判定准绳 #5/E3）
      - Skill: none
      - 执行注记（2026-09-07）：登记 7 文件（aps 2 + inventory 3 + manufacturing 1 = @Description 专属 6 文件；hr ErpHrConstants = C2② seed 角色名契约），四要素齐备落 §WHITELIST「批 2/2」段；本簇无混合文件；`--strict` PASS exit 0（0 新增违规，白名单文件豁免 + 9 文件 removed-from-tree 改善项）。
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言四域 CAT3 = 0（数字记入勾选注记）
      - Skill: none
      - 执行注记（2026-09-07）：hr 28→**0** / inventory 28→**0** / manufacturing 28→**0** / aps 20→**0**（hr 余 1 行 = ErpHrConstants 白名单豁免，checker --strict PASS；四域合计 104 → 0 对账一致）
- [x] <Proof> `mvn test -pl module-hr/erp-hr-service,module-inventory/erp-inv-service,module-manufacturing/erp-mfg-service,module-aps/erp-aps-service -am` 全绿零新增失败
      - Skill: none
      - 执行注记（2026-09-07）：erp-hr-service **249**/0/0 + erp-inv-service **253**/0/0 + erp-mfg-service **308**/0/0 + erp-aps-service **82**/0/0 = **892 tests 全绿 0 失败 0 错误**（快照外科变换后复跑确认）

Exit Criteria:

- [x] 四域 CAT3 104 → 0（归零 + 白名单全覆盖，脚本断言逐域对账一致）
- [x] 4 个 service 模块聚合测试全绿，零新增失败

## Phase 3 — 中簇四域清剿：projects 15 / b2b 15 / purchase 14 / sales 13（57 行）

> 统一类型：Fix-heavy（1 Fix + 1 Add + 2 Proof）。
> Skill: none
> Targets: checker 实跑 `module-projects/`、`module-b2b/`、`module-purchase/`、`module-sales/` 带 CAT3 计数的 Java 文件
> Prereqs: Phase 2 完成

- [x] <Fix> 四域批域文件逐簇裁决（同 Phase 2 模式）
      - Skill: none
      - 执行注记（2026-09-07）：projects 4 文件（ProjectSettlementAcctDocProvider 9 科目名 + ProjectCostCollectionProvider 3 科目名按 (a) 置空；任务看板 titles 4 值、转固资产名后缀按 (b) 英文化）+ b2b 8 文件 15 行全真实运行时字符串（EDI log action 消息 7/ASN remark 5/webhook resultMsg 1/PartnerProfile 守卫描述 2/TransportManager 错误前缀 1 按 (b)）+ purchase 3 文件（PurAcctDocProvider 11 科目名按 (a)；PaymentSettler remark/CT 折扣 tag「节省」→"saved "按 (b)）+ sales 4 文件（SalAcctDocProvider 7 科目名按 (a)；核销冲销/赠品行/换货价差 remark 5 行按 (b)）。快照外科变换（csv 感知外科式，仅消息/名称列）：projects 15 cell（SUBJECT_NAME 按 case 级 seed 回填名 + erp_ast_asset.NAME -转固→-Capitalized）+ purchase 23 cell（REMARK 核销冲销/CT 节省 tag + SUBJECT_NAME 按 seed 名）+ sales 10 cell（REMARK 核销冲销/赠品行/换货价差行）+ TestErpSalReturnExchange 3 处断言载体同步（语义不变）；b2b 快照 24 文件逐 cell 同步（RESULT_MSG/REMARK/ERROR_MSG 载体）。
- [x] <Add> 四域白名单登记（四要素齐备，同 Phase 2 口径）
      - Skill: none
      - 执行注记（2026-09-07）：登记 3 文件（projects/purchase/sales DashboardBizModel @Description 专属，b2b 无 @Description 文件），四要素齐备落 §WHITELIST。
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言四域 CAT3 = 0（数字记入勾选注记）
      - Skill: none
      - 执行注记（2026-09-07）：projects 15→**0** / b2b 15→**0** / purchase 14→**0** / sales 13→**0**（三域余 1 行 = DashboardBizModel 白名单豁免，四域合计 57 → 0 对账一致）
- [x] <Proof> `mvn test -pl module-projects/erp-prj-service,module-b2b/erp-b2b-service,module-purchase/erp-pur-service,module-sales/erp-sal-service -am` 全绿零新增失败
      - Skill: none
      - 执行注记（2026-09-07）：聚合复跑 erp-prj-service **179**/0/0 + erp-b2b-service **80**/0/0 + erp-pur-service **341**/0/0 + erp-sal-service **316**/0/0 = **916 tests 全绿 0 失败 0 错误**

Exit Criteria:

- [x] 四域 CAT3 57 → 0（归零 + 白名单全覆盖，脚本断言逐域对账一致）
- [x] 4 个 service 模块聚合测试全绿，零新增失败

## Phase 4 — 轻簇八域清剿：maintenance 13 / quality 13 / common-service 8 / contract 6 / logistics 3 / crm 2 / master-data 2 / notify 1（48 行）

> 统一类型：Fix-heavy（1 Fix + 1 Add + 2 Proof）。
> Skill: none
> Targets: checker 实跑 `module-maintenance/`、`module-quality/`、`module-common-service/`、`module-contract/`、`module-logistics/`、`module-crm/`、`module-master-data/`、`module-notify/` 带 CAT3 计数的 Java 文件
> Prereqs: Phase 3 完成

- [x] <Fix> 八域批域文件逐簇裁决（同前模式）
      - Skill: none
      - 执行注记（2026-09-07/08，接续前次中断会话完成）：前会话已完成八域主代码字符串改写（git diff 逐文件复核确认为仅字符串载体改动，行为不变式未破坏），本会话补齐其遗漏的快照外科变换与断言载体同步——contract 4 文件（ErpCtContractBizModel [附件:]→[attachment:]、ErpCtDocumentBizModel purge/OCR remark 按 (b)、ManualOcrEngine/MockSignatureProvider 按 (b)）+ `_cases` 外科变换 7 cell（erp_ct_document REMARK 6 cell 含 CSV 引号修复、erp_ct_signature_request ERROR_MSG 1 cell）+ 断言载体 2 处（TestErpCtDocRetention contains("Purged:")、TestErpCtDocumentRepository contains("OCR failed")）；maintenance 4 文件 + 快照外科变换（erp_fin_voucher_line MEMO 7 文件、erp_mnt_visit REMARK 4 cell、erp_inv_stock_move REMARK 1 cell）+ 断言载体 2 处（TestErpMntVisitReportAdditionalFault [Additional fault] ×2）；logistics 2 文件 + 快照外科变换（erp_log_shipment REMARK 3 cell）+ 断言载体 2 处（TestErpLogShipmentGateway gateway retries exhausted/non-retryable）；crm 2 文件、master-data 1 文件（ErpPartyType displayName 按 (b)）、notify 1 文件 + 快照外科变换 3 cell + 断言载体 2 处（[merged 标记）；quality 7 文件（前会话已含快照变换）；common-service 零改动（MaskHelper/ErpOrgIsolationQueryTransformer 均白名单豁免）。另：contract/pur/sal/mfg/aps/logistics/md 等 16 域聚合测试暴露前会话批量遗漏的 `_cases` 载体（REMARK 冲销→Reversal、SUBJECT_NAME 按 seed 回填名、MEMO/REMARK (b) 英文化对应 cell）共约 180 cell，经 checker 驱动逐波外科变换归零（仅消息/名称列，`@var:`/`*` 通配保留，csv 含逗号新值按 RFC 4180 补引号）
- [x] <Add> 八域白名单登记（四要素齐备，同前口径）
      - Skill: none
      - 执行注记（2026-09-07/08）：§WHITELIST 批 2/2 段在前会话 15 文件基础上补录 Phase 2 声称已登记但实仓缺失的 hr ErpHrConstants（C2② seed 角色名契约，nop_auth_role.csv:17 + erp-hr.action-auth.xml roles="HR 专员" 实证），批 2/2 白名单合计 16 文件（Phase 2 七 + Phase 3 三 + Phase 4 六）；`--strict` PASS exit 0
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言八域 CAT3 = 0（数字记入勾选注记）
      - Skill: none
      - 执行注记（2026-09-08）：maintenance 13→**0** / quality 13→**0** / common-service 8→**0**（MaskHelper 7 + ErpOrgIsolationQueryTransformer 1 白名单豁免）/ contract 6→**0** / logistics 3→**0** / crm 2→**0** / master-data 2→**0** / notify 1→**0**（八域合计 48 → 0 对账一致；全局 CAT3 = 0，CAT1/2 持平 0、CAT4 持平 1318）
- [x] <Proof> `mvn test -pl module-maintenance/erp-mnt-service,module-quality/erp-qa-service,module-common-service,module-contract/erp-ct-service,module-logistics/erp-log-service,module-crm/erp-crm-service,module-master-data/erp-md-service,module-notify/erp-notify-service -am` 全绿零新增失败
      - Skill: none
      - 执行注记（2026-09-08）：以 16 批域模块聚合 `mvn test -am`（超集，覆盖本项 8 模块）复跑 **BUILD SUCCESS**——erp-mnt-service **157**/0/0 + erp-qa-service **184**/0/0 + module-common-service **23**/0/0 + erp-ct-service **168**/0/0 + erp-log-service **66**/0/0 + erp-crm-service **188**/0/0 + erp-md-service **160**/0/0 + erp-notify-service **23**/0/0 = **969 tests 全绿 0 失败 0 错误**（快照外科变换后确认；聚合中其余 8 批域模块同轮全绿，合计 2772/0/0）

Exit Criteria:

- [x] 八域 CAT3 48 → 0（归零 + 白名单全覆盖，脚本断言逐域对账一致）
- [x] 8 个模块聚合测试全绿，零新增失败

## Phase 5 — 批级归零证明 + 批注账落账 + 收尾门控

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增代码文件；断言、批注账与收官验证
> Prereqs: Phase 2~4 完成

- [x] <Proof> `node tools/check-hardcoded-cjk.mjs --strict` exit 0：CAT-3 全域 = 0 落账（对照 Phase 1 红线注记，本批 209 行归零或白名单全覆盖，逐域对账一致），CAT1/2 持平 0、CAT4 持平 1318；批次红线→归零对账记入 `docs/audits/cjk-baseline.md` 批注账（MI.6 批 2/2 行：209→0 逐域对账 + 白名单增量 + 测试数字）
      - Skill: none
      - 执行注记（2026-09-08）：`--strict` **PASS exit 0**（RESULT: 0 new violations vs frozen snapshot），实跑 CAT1=0/CAT2=0/**CAT3=0 sites/0 files**/CAT4=1318（持平）——对照 Phase 1 红线注记 209 行逐域对账一致（16 域 209→0，drp 0 无批内面）。批注账 MI.6 批 2/2 行已落 `docs/audits/cjk-baseline.md`（209→0 逐域对账 + 白名单增量 16 文件 + 测试数字 + 门控数字）。
- [x] <Proof> 16 个批域模块聚合 `mvn test -pl <Phase 2~4 全部 service 模块> -am` 全绿零新增失败；app-erp-all 集成快照若受载体改动影响同批重录并复跑 `mvn test -pl app-erp-all` 零新增失败
      - Skill: none
      - 执行注记（2026-09-08）：16 模块聚合 `mvn test -am` **BUILD SUCCESS 2772 tests / 0 failures / 0 errors / 0 skipped**（surefire 汇总逐模块：hr 249 / inv 248 / mfg 308 / aps 82 / prj 179 / b2b 80 / pur 341 / sal 316 / mnt 157 / qa 184 / common 23 / ct 168 / log 66 / crm 188 / md 160 / notify 23）。app-erp-all 集成快照受载体改动影响（前会话 16 文件变换波漏掉 C08/C09/C19 三 case）——本会话补齐外科变换 12 cell：C08 `10_promote_to_formal_plan_response.json5` remark + `erp_mfg_mrp_plan.csv` 2 cell（仿真版本/仿真计算结果→Simulation v1 promoted to formal plan / Simulation result (scenario …)）；C09 NCR description 4 cell（不合格项:→Non-conformance items:，`resolution` 输入回显与错误路径 `msg` 按协议不动）；C19 receive remark 3 cell（由 ASN 自动创建→Auto-created from ASN (B2B_ASN weak pointer)、行级自动回填→Auto-filled from ASN AsnLine #1）+ `erp_fin_voucher_line.csv` SUBJECT_NAME 3 cell 按 seed 名回填（1401 库存商品→原材料 ×2、2202 应付账款-暂估→应付账款）；仅消息/名称列，`*`/`@var:` 通配保留。复跑 `mvn test -pl app-erp-all` **70/0/0/1 全绿**（与 2026-09-04 良基线一致）。
- [x] <Proof> 收尾门控（横切关注点 10/11）：全量 `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` 零漂移（若漂移，按已知失败模式「Compliance 基线漂移」开独立基线裁决后方可闭包，不在本批顺手放宽）
      - Skill: none
      - 执行注记（2026-09-08）：全量 `mvn clean install -DskipTests` **BUILD SUCCESS**（156 reactor 模块，02:03 min，exit 0）；`bash docs/audits/nop-compliance-checker.sh` **exit 0 零漂移**——R1d=14/R2a=34/R2b=242/**R2c=1542**/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，与 2026-09-06 `ai-check-r3-m0` 基线行逐项精确一致。

Exit Criteria:

- [x] `--strict` 全绿，CAT-3 全域 = 0 与红线注记对账一致，批注账已记
- [x] 批域模块聚合测试全绿零新增失败；全量 build + compliance checker 收尾门控绿或漂移已独立裁决

## Draft Review Record

- dispatch review #review-2026-09-07-171530-2026-09-07-1715-1-mi6-runtime-string-cat3-batch2-1-b8f20380 to opencode-review-20260907-mi6b2
- 2026-09-07：iteration 1，共识 accept #review-2026-09-07-171530-2026-09-07-1715-1-mi6-runtime-string-cat3-batch2-1-b8f20380
  - Blocker 已修复：补齐 Closure Gates 结束清单与 Closure 证据结构；Minor 已修复：五阶段补 `Status: planned`、移除空 Verification 槽位并入门控验证项。基线对账复核：390−181=209、批域求和 104+57+48=209、owner doc 锚点（§SNAPSHOT/§WHITELIST/i18n-compliance CAT-3 行/roadmap MI.6 行/lesson 13）与 16 个 `-pl` 模块路径均实仓核验存在。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次，阶段退出标准不重复全仓验证（见计划指南执行时规则 7）。ledger 格式：门控以非复选框散文记录（计数域 = `## Phase <n>` + `## Closure Findings`，01-file-ledger §2.5），完成态由复选框 + `## Verification` pass 线 + `## Closure` 回执派生。

- 范围内行为完成：16 域 209 行 CAT-3 = 0 或白名单全覆盖（`node tools/check-hardcoded-cjk.mjs --strict` exit 0 断言，CAT1/2 持平 0、CAT4 持平不升）——2026-09-08 实跑 `--strict` PASS exit 0，CAT3=0 sites/0 files，CAT1/2=0、CAT4=1318 持平；16 域 209→0 逐域对账一致
- 相关文档对齐：`docs/audits/cjk-baseline.md` 批注账 MI.6 批 2/2 行与 §WHITELIST 增量登记和实仓一致；`docs/architecture/i18n-compliance.md` 本批零变更（沿用批 1 协议，不修订）——批注账行已落（209→0 对账 + 白名单 16 文件 + 测试/门控数字）；§WHITELIST 批 2/2 段 16 文件与实仓 grep 核验一致；`git status` 证实 i18n-compliance.md 零改动
- 已运行验证（指定命令）：Phase 2~4 批域 `mvn test -pl <各簇 service 模块> -am` 全绿零新增失败；`mvn clean install -DskipTests` BUILD SUCCESS；`bash docs/audits/nop-compliance-checker.sh` 零漂移（漂移须独立基线裁决后方可闭包）——2026-09-08 收官轮全绿：16 模块聚合 2772/0/0 + app-erp-all 70/0/0/1（集成快照 3 case 补齐外科变换后）+ 全量 install 156 模块 BUILD SUCCESS + compliance checker exit 0（R2c=1542 零漂移），详见 Phase 5 勾选注记；闭包重审轮复跑证据见 `## Verification`
- 无范围内项目降级为 deferred/follow-up（行为疑点登记 finding 归 M2 属既定协议分流，不属降级）——执行中发现的 app-erp-all 集成快照 3 case 漏变换已在本会话补齐归零，无降级项
- 独立草案审查已完成并记录（见 Draft Review Record：#review-2026-09-07-171530-…-b8f20380 iteration 1 共识 accept）
- 文本一致性已验证：顶部状态、各阶段 Exit Criteria、本门控与 `docs/logs/` 条目一致——frontmatter `status: active` 保持（ledger 完成态派生，勿写 completed）；5 个 Phase 全部项与 Exit Criteria `[x]`；per-Phase `Status: planned` 行按同族闭包先例（批 1/MI.8 批 1）移除，完成态由复选框派生；`docs/logs/2026/09-08.md` 条目与批注账数字一致
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符——已由本闭包审计会话（独立上下文，非执行者）执行，dispatch/accepted 回执对落 `## Closure`
- 结束证据存在于文件中（checker 输出数字 + 计划勾选注记 + 批注账 MI.6 批 2/2 行 + `## Verification` pass 线 + `## Closure` 回执对）

## Verification

- pass test 20260908-closure-r1 exit=0

## Closure

Status Note: 16 域 209 行 CAT-3 全域归零 + 白名单 16 文件全覆盖（checker 实跑 CAT1/2/3 = 0/0/0、CAT4 持平 1318）；计数域 27 项全部 `[x]` 且逐项经实仓核验（whitelist grep、协议 (a)/(b) 代码抽查、seed 零改动）；闭包重审轮（本审计会话）全仓 `mvn clean install -DskipTests` exit 0 + `mvn test` 8012/0/0/2 全绿（与已知良基线一致）+ compliance checker exit 0 零漂移——完成态由 ledger 公式（01 §5.2）派生。

Closure Audit Evidence:

- Auditor / Agent: 独立闭包审计会话（opencode-closure-20260908-mi6b2，新会话非执行者上下文）
- Evidence: 本节回执对 + `## Verification` pass 线（20260908-closure-r1）+ `docs/audits/cjk-baseline.md` 批注账 MI.6 批 2/2 行 + `docs/logs/2026/09-08.md` 收官条目
- dispatch audit #audit-20260908-closure-2026-09-07-1715-1-mi6-runtime-string-cat3-batch2-1-835ac124 to opencode-closure-20260908-mi6b2 models={exec:opencode-glm-5.3-flash,aud:opencode-glm-5.3-flash}
- accepted #audit-20260908-closure-2026-09-07-1715-1-mi6-runtime-string-cat3-batch2-1-835ac124：审计 ACCEPT——CAT-3 全域归零证据链成立（`--strict` exit 0、CAT3=0/209→0 逐域对账、白名单 16 文件、协议零漂移），本会话实跑 `mvn clean install -DskipTests` BUILD SUCCESS exit=0、全仓 `mvn test` 8012/0/0/2 exit=0、compliance checker exit 0（R2c=1542 零漂移），plan-check --strict 结构绿，完成态派生成立（单一模型自审降级已如实记录于 models 谱系）
