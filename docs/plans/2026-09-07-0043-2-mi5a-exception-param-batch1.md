---
status: active
mission: ai-check-r3
work-item: MI.5a
group: "2026-09-07-0043"
verify: [test]
---

# 2026-09-07-0043-2 MI.5a 异常路径中文参数清剿 1/2（common-service 抽象族 + pur/sal/ast/fin）

## Current Baseline

- 冻结基线（`docs/audits/cjk-baseline.md` §SNAPSHOT，2026-09-06）：CAT-2（异常路径中文参数）——common-service 5 行 / 5 文件、purchase 36 行 / 20 文件、sales 31 行 / 18 文件、assets 41 行 / 22 文件、finance 23 行 / 10 文件，合计 136 行 / 75 文件；文件级逐文件计数以该 SNAPSHOT `files:` 块为准。
- 口径注记：roadmap MI.5a 探针粗口径（pur 32 / sal 30 / ast 29 / fin 20，2026-08-31 throw 粗口径 + `.param` 精确混合）为口径敏感快照；按 roadmap §目的「口径敏感项以 M0.2 脚本 + M0.3 快照冻结为准」，本批红线以冻结 SNAPSHOT 为准（pur 36 / sal 31 / ast 41 / fin 23；2026-09-07 实跑复核一致）。
- 抽象族现状（实仓复核）：`module-common-service` `Abstract{Approve,Cancel,Reject,SubmitForApproval,WithdrawApproval}Processor` 各 1 行——`illegalStatusException(...)` 期望态实参传中文散文（如 `AbstractCancelProcessor` 传 `"非已作废"`）；helper `AbstractProcessor.defaultIllegalStatusException(current, expected...)` 以 `String.join(" / ", expected)` 拼接进 `ARG_EXPECTED_STATUS`。修复 = 期望态实参改传状态码/枚举名/字典值本身（如 `"CANCELLED"`），helper 契约随之收敛为「传码不传散文」；各域同型调用点随语义收敛（含 MI.5b 范围域内调用点——若收敛需要变更 helper 签名/参数类型，记录 Decision）。
- 修复模式（`docs/architecture/i18n-compliance.md` 修复模式对照表 CAT-2 行）：异常路径携带中文散文参数 → 传状态码/枚举名/字典值本身；错误消息语义不变（中文仍由 ErrorCode 模板 + i18n 承载）。
- 行为不变式（roadmap 横切关注点 8）：仅改异常参数实参，禁止借机改异常类型、错误码 key、抛出条件、控制流、事务边界；**不建英文 i18n 承载**（合规基线表裁定，本轮不建 827 条 en 镜像）。
- 域内 CAT-1（MI.2/MI.3 已清）/ CAT-3（MI.6）计数本批不触碰，仅动 CAT-2 行。
- 依赖状态：M0.6 done；本批依赖 MI.4（依赖图 MI.4 → MI.5a），执行顺序居本批三计划之第 2。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行（全 reactor 4006/0/0/1；CJK `--strict` 绿）。
- 剩余差距：5 面 136 行 CAT-2 红线（CAT-2 全量 204 的 MI.5a 半场）；`--strict` 门控下 actual 只降不升。

## Goals

- common-service 抽象族 + purchase/sales/assets/finance CAT-2 归零（136 行 / 75 文件），期望态/异常参数全部改传状态码/枚举名/字典值本身，错误消息语义不变。
- 5 个模块（module-common-service + 4 域 service）`mvn test` 全绿零回归；`node tools/check-hardcoded-cjk.mjs --strict` 保持绿且上述 5 面 CAT2=0（单向收紧合法下降）。

## Non-Goals

- 不改错误码定义（`ErrorCode.define`）、异常类型、抛出条件、业务行为（横切关注点 8）。
- 不动其余 12 域 CAT-2（MI.5b 范围）；CAT-3/4 面（MI.6/7/8）不动。
- 不动 `_init-data` seed、ORM 模型、页面文件；不建英文 i18n 承载。

## Phase 1 — 红线记录 + common-service 抽象族先行（5 文件 5 行）

> 统一类型：Fix-heavy（1 红线 Proof + 1 Fix | Decision + 2 Proof）。
> Skill: none（roadmap MI.5a 行指定）
> Targets: `module-common-service/src/main/java/app/erp/common/service/Abstract*.java`（5 个 Abstract*Processor + helper `AbstractProcessor`）
> Prereqs: plan `2026-09-07-0043-1`（MI.4）完成

- [x] <Proof> 批第一动作：`node tools/check-hardcoded-cjk.mjs` 实跑，记录 5 面红线数字（common 5 / pur 36 / sal 31 / ast 41 / fin 23）于本项勾选注记（修复批产物 = 脚本输出数字 + 归零断言，不产 ck-* 报告，横切关注点 13）
      - Skill: none
      - 实测（2026-09-07）：common 5 / pur 36 / sal 31 / ast 41 / fin 23，合计 136，与冻结 SNAPSHOT 逐域精确一致（CAT1 全域 0 与 MI.4 收官态一致）
- [x] <Fix | Decision> 抽象族 5 文件期望态实参改传状态码/枚举名（`"非已作废"` 型散文 → 状态码本身）；`defaultIllegalStatusException` helper 契约收敛为「期望态传码」——保持 `String... expected` 签名不变；若实现裁决发现需变更签名/参数类型（如改枚举），在计划勾选注记记录 Decision（选择、替代方案、残余风险）并同步收敛全部调用点
      - Skill: none
      - 完成：5 处 `illegalStatusException(entity, …, "非已作废")` → `!` 前缀 + 状态码（Approve/Reject/Submit/Withdraw 传 `"!CANCELLED"`，Cancel 传 `"!" + cancelledDocStatus()`）；helper 签名 `String... expected` 未变，`defaultIllegalStatusException` 增契约 javadoc（传码不传散文）
      - Decision（2026-09-07，否定语义参数表示法）：**选择** `!` 前缀 + 字典状态码（如 `"!CANCELLED"`，渲染 `期望=!CANCELLED`）表达「非已作废」型否定语义，与原中文散文语义逐字对映；**替代方案** (a) 裸传禁用码 `"CANCELLED"`——渲染 `期望=CANCELLED` 与守卫语义相反，判语义失真否决；(b) 静态枚举允许态集合——common 抽象族守卫的合法源态随实体/操作变化，无法静态穷举，否决；(c) 改 ErrorCode 模板承载否定语义——违反横切关注点 8（错误码定义不可改），否决；**残余风险** `!` 前缀为本计划裁决的显示约定（非字典值），后续 i18n en 镜像（本轮不建）按字面渲染，MI.5b 其余域同型调用点沿用同一约定
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言 common-service CAT2 = 0（数字记入勾选注记）
      - Skill: none
      - 实测：common-service CAT2 5 → 0（全局 totals CAT2 204 → 199，CAT1/3/4 持平：common CAT3=8 未触碰）
- [x] <Proof> `mvn test -pl module-common-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none
      - 实测：exit 0，module-common-service 23 tests 0F/0E，BUILD SUCCESS 全绿零回归

Exit Criteria:

- [x] common-service CAT2 5 → 0（脚本断言）
- [x] module-common-service 测试全绿，零新增失败

## Phase 2 — purchase + sales 扫清（38 文件 67 行）

> 统一类型：Fix-heavy（2 Fix + 2 Proof）。
> Skill: none
> Targets: SNAPSHOT `files:` 块中 `module-purchase/`、`module-sales/` 带 CAT2 计数的 Java 文件
> Prereqs: Phase 1 完成（抽象族契约先行收敛，域内同型调用点随语义改写）

- [x] <Fix> purchase 20 文件（2 entity + 10 processor + 8 statemachine）异常路径中文参数逐行改传状态码/枚举名/字典值；仅动 CAT-2 行（CAT-3 保持原样归 MI.6）
      - Skill: none
      - 完成：28 处语句改写——8 statemachine `illegal("cancel",…,"非已作废")` → `"!" + ErpPurDocStatus.DOC_STATUS_CANCELLED`；4 Cancel processor 捕获重抛 → `"!CANCELLED"`；Order/Receive/Requisition/Return processor 守卫邻接常量 → `"!" + ErpPurConstants.DOC_STATUS_CANCELLED`；Invoice/Payment processor（isCancelled 型守卫）→ `"!CANCELLED"`；Quotation/Rfq BizModel `ARG_EXPECTED_DOC_STATUS` → `"!" + ErpPurDocStatus.DOC_STATUS_CANCELLED`；8 处 `"UNSUBMITTED 或 REJECTED"` → `"UNSUBMITTED / REJECTED"`（helper join 分隔符约定，码值不变）；statemachine javadoc 引用 owner doc 语句（CAT-5 豁免）未触碰
- [x] <Fix> sales 18 文件（12 processor/entity + 6 statemachine）同模式改写
      - Skill: none
      - 完成：6 statemachine → `"!" + ErpSalDocStatus.DOC_STATUS_CANCELLED`；6 Cancel processor → `"!CANCELLED"`；6 processor 守卫邻接常量 → `"!" + ErpSalConstants.DOC_STATUS_CANCELLED`；6 处 `"UNSUBMITTED 或 REJECTED"` → `"UNSUBMITTED / REJECTED"`；ErpSalReturnProcessor `requirePeriodOpen` `ARG_PERIOD` 空日期分支散文 `"未设置业务日期"` → 传 `null`（period 为实体编码，无码可传时禁止散文，`{period}` 渲染 null 语义不变）
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言两域 CAT2 = 0（数字记入勾选注记）
      - Skill: none
      - 实测：purchase CAT2 36 → 0、sales CAT2 31 → 0（全局 totals CAT2 199 → 132，CAT1/3/4 持平：pur CAT3=14、sal CAT3=13 未触碰）
- [x] <Proof> `mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none
      - 实测：exit 0 BUILD SUCCESS，erp-pur-service 341 tests 0F/0E、erp-sal-service 316 tests 0F/0E，全绿零回归

Exit Criteria:

- [x] purchase / sales 两域 CAT2 36+31 → 0（脚本断言）
- [x] erp-pur-service、erp-sal-service 测试全绿，零新增失败

## Phase 3 — assets + finance 扫清（32 文件 64 行）

> 统一类型：Fix-heavy（2 Fix + 2 Proof）。
> Skill: none
> Targets: SNAPSHOT `files:` 块中 `module-assets/`、`module-finance/` 带 CAT2 计数的 Java 文件
> Prereqs: Phase 2 完成

- [x] <Fix> assets 22 文件（8 processor + 14 statemachine）异常路径中文参数同模式改传状态码/枚举名/字典值
      - Skill: none
      - 完成：`"非已作废"` → `"!" + ErpAstConstants.DOC_STATUS_CANCELLED`（isCancelled/终态守卫 9 处）、`"非已生效"` → `"!" + ErpAstConstants.DOC_STATUS_ACTIVE`（ACTIVE 守卫 4 处）、`"非已过账"` → `"!POSTED"`（posted 标志守卫 3 处，无相邻常量按字典码字面）、`"UNSUBMITTED 或 REJECTED"` 5 处与常量拼接 `" 或 "` 12 处 → `" / "`（Code 列表 helper join 约定）、`ErpAstInventoryProcessor` `expected + "（操作 " + action + "）"` → `expected + " (action=" + action + ")"`；矩阵测试 `TestErpAstValueAdjustmentDocumentStateMachineMatrix` 2 处 `ARG_EXPECTED_STATUS` 断言随语义收敛更新为 `!ACTIVE`/`!CANCELLED`（断言参数值属修复面；javadoc 注释未触碰）
- [x] <Fix> finance 10 文件（6 processor/service + 4 statemachine）同模式改写；`CloseVoucherWriter`/`ErpFinApDocumentPipelineProcessor`/`ErpFinVoucherTemplateRenderTemplateProcessor` 仅动 CAT-2 行
      - Skill: none
      - 完成：`CloseVoucherWriter` 内联 `ErrorCode.define` 从 throw 语句上提为 `private static final` 常量（key/模板/参数零变更，define 语句本身合规面不变）；`ErpFinApDocumentPipelineProcessor` 5 处 `fail(doc, step, 中文散文)` 实参 + 1 处 `ARG_STEP` 散文 → 英文原因码（`no classify engine registered` / `purchase invoice service unavailable…` / `file read failed: …` / `file not found (fileId=…)` / `MANUAL_REVIEW_PARTNER_REQUIRED`）——散文仅流入 `persistFailure` 审计 errorMsg 字段（DB 运维面，对齐 CAT-1「运维面英文」裁定），异常 `{step}` 参数与模板渲染语义不变；`ErpFinVoucherTemplateRenderTemplateProcessor` 5 处 `invalid(中文)` + 1 处 `ARG_REASON` 散文 → 原因码（`EMPTY_OR_UNEXPECTED_END`/`UNBALANCED_PAREN`/`NON_WHITELIST_CHAR:c`/`ILLEGAL_NUMBER:n`/`UNPARSED_CHAR:c`/`VAR_VALUE_NOT_NUMERIC:name:val`，模板「…非法：{reason}」承载语义）；EmployeeAdvance/ExpenseClaim processor 或→/ + `"!CANCELLED"`；Notes 族 statemachine `"非终态"` → `"!" + String.join(" / ", terminalStatuses())`、`"RECEIVED 或 DISCOUNTED"` → `"RECEIVED / DISCOUNTED"`；`ErpFinNotesReceivableProcessor` 期望态槽位散文 → `DISCOUNT_DATE/DISCOUNT_BANK/DISCOUNT_RATE_REQUIRED`；矩阵测试 `TestErpFinNotesPayable/ReceivableStateMachineMatrix` 3 处断言随语义收敛更新 + 2 文件 javadoc 契约行同步
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言两域 CAT2 = 0（数字记入勾选注记）
      - Skill: none
      - 实测：assets CAT2 41 → 0、finance CAT2 23 → 0（全局 totals CAT2 132 → 68，CAT1/3/4 持平：ast CAT3=58、fin CAT3=80 未触碰）
- [x] <Proof> `mvn test -pl module-assets/erp-ast-service,module-finance/erp-fin-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none
      - 实测：exit 0 BUILD SUCCESS，erp-ast-service 337 tests 0F/0E、erp-fin-service 525 tests 0F/0E，全绿零回归（首轮 3 处矩阵断言失败已随语义收敛修正后复跑全绿）

Exit Criteria:

- [x] assets / finance 两域 CAT2 41+23 → 0（脚本断言）
- [x] erp-ast-service、erp-fin-service 测试全绿，零新增失败

## Phase 4 — 批级归零证明

> 统一类型：Proof（2 项 Proof）。
> Skill: none
> Targets: 无新增代码文件；仅断言
> Prereqs: Phase 1~3 完成

- [x] <Proof> `node tools/check-hardcoded-cjk.mjs --strict` exit 0：5 面 CAT2 = 0 落账（对照 Phase 1 红线注记，本批累计 136 行归零），其余域 CAT2 与 CAT1/3/4 计数不高于快照
      - Skill: none
      - 实测：`--strict` PASS exit 0（0 new violations vs frozen snapshot，453 baseline files）；common-service/purchase/sales/assets/finance 五面 CAT2 全部 = 0，全局 totals CAT2 204 → 68（-136 = 本批归零，对账 5+36+31+41+23 = 136 与 Phase 1 红线注记精确一致）；CAT1 全域 0 / CAT3 390 / CAT4 1700 与快照持平（IMPROVEMENTS 190 条 file-CAT 下降均为合法单向收紧）
- [x] <Proof> 5 个模块（module-common-service + erp-pur/erp-sal/erp-ast/erp-fin -service）聚合 `mvn test`（含依赖模块 `-am`）全绿，零新增失败
      - Skill: none
      - 实测：exit 0 BUILD SUCCESS，common 23 / pur 341 / sal 316 / ast 337 / fin 525，合计 1542 tests 0F/0E/0S，零新增失败

Exit Criteria:

- [x] `--strict` 全绿，5 面 CAT2 = 0 与红线注记对账一致
- [x] 5 模块测试全绿，零新增失败

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-07-0043-2-mi5a-exception-param-batch1-1-63a49956 to opencode/glm-5.3-flash
- 2026-09-07：iteration 1，共识 approved #review-2026-09-05-123532-mission-driver-2026-09-07-0043-2-mi5a-exception-param-batch1-1-63a49956

## Closure Gates

> 仅在所有执行项目和各阶段退出标准全部勾选 `[x]` 后关闭。完整仓库验证在此处运行一次（不在阶段退出标准重复）。

（ledger 格式：本节为门控证据注记，非计数域——计数域仅 Phase 节 + Closure Findings 节，完成态由 Phase 复选框 + Verification pass 线 + Closure 审计回执派生）

- gate 1 范围内行为完成：5 面 CAT2 归零，`--strict` 断言与 Phase 1 红线注记对账一致（累计 136 行 / 75 文件）——common 5 + pur 36 + sal 31 + ast 41 + fin 23 = 136 全部归零（`--strict` PASS exit 0，全局 totals CAT2 204 → 68，-136 与红线注记逐域精确一致；75 文件 CAT2 全清；改动面 = 75 生产文件 + helper javadoc + 3 测试文件语义收敛 + 3 docs，git status 实测无越域文件）
- gate 2 若 Phase 1 Decision 落定 helper 签名/参数类型变更：同步核对 `docs/architecture/i18n-compliance.md` CAT-2 修复模式行与 helper 新契约表述一致——helper `String... expected` 签名未变更（条件不触发）；否定语义 `!` 前缀约定已按 Decision 登记进 `docs/architecture/i18n-compliance.md` CAT-2 修复模式行（含 plan 指针），与 helper javadoc 契约表述一致，MI.5b 直接消费
- gate 3 已运行验证：`node tools/check-hardcoded-cjk.mjs --strict` exit 0 且 5 面 CAT2=0；5 模块聚合 `mvn test`（含 `-am`）全绿零新增失败；`bash docs/audits/nop-compliance-checker.sh` 复跑无 actual > baseline 漂移（生产代码变更的结束审计硬要求）——`--strict` PASS exit 0（0 new violations，453 baseline files）；5 模块聚合 BUILD SUCCESS（common 23 / pur 341 / sal 316 / ast 337 / fin 525 = 1542/0/0/0）；全 reactor `mvn clean install -DskipTests` BUILD SUCCESS + 全 reactor `mvn test` BUILD SUCCESS（surefire 聚合 3991/0/0/1，1 skip 为既有预期跳过）；checker exit 0，R2b 242>240 / R2c 1542>1537 / R12a 71>70 经对照 MI.4 收官 HEAD worktree 复核结论为**本批外预存漂移**（本批 diff 0 条 daoFor/共享内核 import 变更，已实测），沿用 MI.4 既有 successor 登记（R2b/R2c/R12a 独立基线裁决），本批零新增漂移
- gate 4 无范围内项目降级为 deferred/follow-up——范围内 5 面 136 行全部完成；域内 CAT-3 面（ast 58 / fin 80 等）与其余域 CAT-2（68 行）按计划边界保持原样（归 MI.5b/MI.6 既有范围），非本批范围降级
- gate 5 独立草案审查已完成并记录（见 Draft Review Record）——iteration 1 共识 approved #review-2026-09-05-123532-mission-driver-2026-09-07-0043-2-mi5a-exception-param-batch1-1-63a49956
- gate 6 文本一致性已验证：frontmatter `status`、各 Phase `Status`/`Exit Criteria`、Closure Gates 与 `docs/logs/` 条目一致——frontmatter `status: active` 未动（ledger 完成态派生，不落 `completed`）；4 Phase 全部执行项 + 退出标准 16 项 `[x]`，各 Phase 无残留 `Status: planned` 行；`docs/logs/2026/09-07.md` MI.5a 条目、roadmap MI.5a `done` 行与本计划 Verification 数字一致
- gate 7 结束审计由独立子代理（新会话）执行；执行者未自我审计——CLOSURE_AUDIT 步骤（mission-driver run 2026-09-05-123532-mission-driver，独立审计会话 opencode/glm-5.3-flash，非执行者上下文）已实跑复核，回执落 Closure 节
- gate 8 结束证据存在于文件中——Closure 节已记录审计者、独立实跑证据与 dispatch/accepted 回执对

## Verification

- pass test 2026-09-07-mi5a exit=0 Phase 级：module-common-service 23/0/0/0、erp-pur-service 341/0/0/0、erp-sal-service 316/0/0/0、erp-ast-service 337/0/0/0、erp-fin-service 525/0/0/0（各 Phase `mvn test -pl … -am` BUILD SUCCESS，零新增失败）。
- pass test 2026-09-07-mi5a exit=0 5 模块聚合 `mvn test -pl module-common-service,module-purchase/erp-pur-service,module-sales/erp-sal-service,module-assets/erp-ast-service,module-finance/erp-fin-service -am` BUILD SUCCESS（1542/0/0/0）。
- pass build 2026-09-07-mi5a exit=0 全 reactor `mvn clean install -DskipTests` BUILD SUCCESS。
- pass test 2026-09-07-mi5a exit=0 全 reactor `mvn test` BUILD SUCCESS（surefire 聚合 3991/0/0/1，1 skip 为既有预期跳过，零失败零错误零新增失败）。
- pass cjk-strict 2026-09-07-mi5a exit=0 `node tools/check-hardcoded-cjk.mjs --strict`：PASS exit 0（0 new violations vs frozen snapshot，453 baseline files；5 面 75 文件 136 行 CAT2 全部归零，全局 totals CAT2 204 → 68 对账成立 = 5+36+31+41+23 对照 Phase 1 红线注记；CAT1 全域 0、CAT3 390、CAT4 1700 与快照持平不高于，IMPROVEMENTS 190 条 file-CAT 为合法单向收紧下降）。
- pass compliance 2026-09-07-mi5a exit=0 `bash docs/audits/nop-compliance-checker.sh`：exit 0；R1a-d/R2a/R2d/R3-R8/R10/R11/R12b/R12c 与 baseline 持平；R2b 242>240、R2c 1542>1537、R12a 71>70 为**本批外预存漂移**（MI.4 收官时已 HEAD worktree 复核为预存并有 successor 登记；本批 diff 实测 0 条 daoFor/共享内核 import 变更，actual 逐规则与 MI.4 收官态一致）——归既有 successor 独立基线裁决，本批零新增漂移。
- pass test ai-check-r3-verify-2026-09-07-0704 exit=0 mission-driver verify run：全 reactor `mvn clean install -DskipTests` BUILD SUCCESS（2:09 min）+ 全 reactor `mvn test` BUILD SUCCESS（16:55 min，模块 Results 行汇总 4006/0/0/1，1 skip 既有预期，零新增失败，与 `ai-check-r3-m0` 基线行及 MI.4 收官态精确一致）。

## Closure

Status Note: 5 面（common-service 抽象族 + pur/sal/ast/fin）CAT-2 异常路径中文参数 136 行 / 75 文件全部归零，`--strict` 单向收紧 PASS exit 0，5 模块聚合 `mvn test` 全绿零回归；4 Phase 全部执行项与退出标准已勾选，Closure Gates 证据注记在案，独立结束审计通过（回执如下），计划可关闭。

Closure Audit Evidence:

- Auditor / Agent: opencode/glm-5.3-flash（独立闭包审计子代理会话，mission-driver run 2026-09-05-123532-mission-driver，非执行者上下文；单模型降级如实登记于 models= 后缀）
- Evidence: 独立实跑复核（2026-09-07）——`node tools/check-hardcoded-cjk.mjs --strict` PASS exit 0（5 面 CAT2=0：common 0 / pur 0 / sal 0 / ast 0 / fin 0；CAT3 common 8 / pur 14 / sal 13 / ast 58 / fin 80 与快照持平）；源码抽查 `AbstractCancelProcessor` 传 `"!" + cancelledDocStatus()`、`AbstractProcessor` helper 契约 javadoc、`CloseVoucherWriter` 静态 ErrorCode 常量上提、ast 矩阵测试断言 `"!" + ErpAstConstants.*` 与计划注记一致；全仓 grep `"非已作废"` 0 命中；`docs/logs/2026/09-07.md` MI.5a 条目与 `docs/architecture/i18n-compliance.md` CAT-2 行（含 `!` 前缀约定 + plan 指针）同步在案
- dispatch audit #audit-2026-09-05-123532-mission-driver-2026-09-07-0043-2-mi5a-exception-param-batch1-1-28bb385e to opencode/glm-5.3-flash models={exec:opencode/glm-5.3-flash,aud:opencode/glm-5.3-flash}
- accepted #audit-2026-09-05-123532-mission-driver-2026-09-07-0043-2-mi5a-exception-param-batch1-1-28bb385e：审计结论 approved——5 面 136 行 / 75 文件 CAT-2 归零与 Phase 1 红线注记逐域对账一致，行为不变式（错误码 key/模板、异常类型、抛出条件、控制流零变更）抽查无违例，Verification 数字与实时仓库一致，文档同步无漂移，准予关闭。
