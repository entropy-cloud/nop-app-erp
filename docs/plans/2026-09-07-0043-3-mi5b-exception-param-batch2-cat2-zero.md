---
status: active
mission: ai-check-r3
work-item: MI.5b
group: "2026-09-07-0043"
verify: [test]
---

# 2026-09-07-0043-3 MI.5b 异常路径中文参数清剿 2/2 + CAT-2 归零（其余 12 域）

## Current Baseline

- 冻结基线（`docs/audits/cjk-baseline.md` §SNAPSHOT，2026-09-06）：CAT-2（异常路径中文参数）其余 12 域——quality 11 行 / 9 文件、inventory 11 行 / 9 文件、manufacturing 10 行 / 6 文件、drp 9 行 / 4 文件、projects 9 行 / 6 文件、maintenance 7 行 / 6 文件、master-data 3 行 / 2 文件、crm 2 行 / 2 文件、cs 2 行 / 2 文件、notify 2 行 / 1 文件、b2b 1 行 / 1 文件、contract 1 行 / 1 文件，合计 68 行 / 49 文件（aps / hr / logistics CAT2 = 0）；文件级逐文件计数以该 SNAPSHOT `files:` 块为准。
- 口径注记：roadmap（`docs/backlog/ai-check-r3-roadmap.md`）MI.5b 探针粗口径（qa 10 / mfg 9 / inv 7 / mnt 7 / prj 7 / common 5 / md 3 等，2026-08-31）为口径敏感快照；按 roadmap §目的「口径敏感项以 M0.2 脚本 + M0.3 快照冻结为准」，本批红线以冻结 SNAPSHOT 为准（qa 11 / mfg 10 / inv 11 / mnt 7 / prj 9 / md 3 等）；探针行所列 common 5 行归 MI.5a 抽象族，不在本批。
- 修复模式（`docs/architecture/i18n-compliance.md` 修复模式对照表 CAT-2 行）：异常路径携带中文散文参数 → 传状态码/枚举名/字典值本身；错误消息语义不变（中文仍由 ErrorCode 模板 + i18n 承载）；抽象族 helper 契约「期望态传码」已由 MI.5a Phase 1 先行收敛，本批域内调用点随契约改写。
- 行为不变式（roadmap 横切关注点 8）：仅改异常参数实参，禁止借机改异常类型、错误码 key、抛出条件、控制流、事务边界；**不建英文 i18n 承载**。
- 域内 CAT-1（MI.4 已清归零）/ CAT-3（MI.6）计数本批不触碰，仅动 CAT-2 行。
- 依赖状态：M0.6 done；本批依赖 MI.5a（依赖图 MI.5a → MI.5b），执行顺序居本批三计划之第 3。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行（全 reactor 4006/0/0/1；CJK `--strict` 绿）。
- 剩余差距：12 域 68 行 CAT-2 红线（CAT-2 全量 204 = MI.5a 136 + 本批 68）；收官断言「CAT-2 全域 = 0」由本批达成。

## Goals

- 12 域 CAT-2 归零（68 行 / 49 文件），异常路径中文参数全部改传状态码/枚举名/字典值本身，错误消息语义不变。
- CAT-2 全域 = 0 收官断言（`--strict` 绿 + totals CAT2 204 → 0 对账）；12 个域 service 模块 `mvn test` 全绿零回归。

## Non-Goals

- 不改错误码定义、异常类型、抛出条件、业务行为（横切关注点 8）。
- 不动 CAT-1（MI.4 收官归零）/ CAT-3（MI.6）/ CAT-4（MI.7/8）面。
- 不动 `_init-data` seed、ORM 模型、页面文件；不建英文 i18n 承载。

## Phase 1 — 红线记录 + manufacturing + inventory + quality 扫清（24 文件 32 行）

> 统一类型：Fix-heavy（1 红线 Proof + 3 Fix + 2 Proof）。
> Skill: none（roadmap MI.5b 行指定）
> Targets: SNAPSHOT `files:` 块中 `module-manufacturing/`、`module-inventory/`、`module-quality/` 带 CAT2 计数的 Java 文件
> Prereqs: plan `2026-09-07-0043-2`（MI.5a）完成

- [x] <Proof> 批第一动作：`node tools/check-hardcoded-cjk.mjs` 实跑，记录 12 域红线数字（qa 11 / inv 11 / mfg 10 / drp 9 / prj 9 / mnt 7 / md 3 / crm 2 / cs 2 / notify 2 / b2b 1 / ct 1）于本项勾选注记（修复批产物 = 脚本输出数字 + 归零断言，不产 ck-* 报告，横切关注点 13）
      - Skill: none
      - 红线注记（2026-09-07 实跑）：qa 11 / inv 11 / mfg 10 / drp 9 / prj 9 / mnt 7 / md 3 / crm 2 / cs 2 / notify 2 / b2b 1 / ct 1，合计 68，与冻结 SNAPSHOT 逐域精确一致；MI.5a 域（fin/pur/sal/ast/common）CAT2 实测 0
- [x] <Fix> manufacturing 6 文件异常路径中文参数逐行改传状态码/枚举名/字典值（`ErpMfgSubcontractOrderProcessor`/`ErpMfgWorkOrderProcessor` 仅动 CAT-2 行，CAT-1 已由 MI.3 清零、CAT-3 归 MI.6）
      - Skill: none
- [x] <Fix> inventory 9 文件（5 processor + 4 statemachine）同模式改写
      - Skill: none
- [x] <Fix> quality 9 文件（6 entity/processor + 3 statemachine）同模式改写
      - Skill: none
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言三域 CAT2 = 0（数字记入勾选注记）
      - Skill: none
      - 断言注记（2026-09-07）：manufacturing CAT2 10→0 / inventory CAT2 11→0 / quality CAT2 11→0；三域 CAT1/3/4 与 SNAPSHOT 持平（mfg 0/28/96、inv 0/28/93、qa 0/13/114）
- [x] <Proof> `mvn test -pl module-manufacturing/erp-mfg-service,module-inventory/erp-inv-service,module-quality/erp-qa-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none
      - 验证注记（2026-09-07）：BUILD SUCCESS；erp-mfg-service 308/0/0、erp-inv-service 248/0/0、erp-qa-service 184/0/0，零新增失败

Exit Criteria:

- [x] manufacturing / inventory / quality 三域 CAT2 10+11+11 → 0（脚本断言）
- [x] erp-mfg-service、erp-inv-service、erp-qa-service 测试全绿，零新增失败

## Phase 2 — drp + projects + maintenance 扫清（16 文件 25 行）

> 统一类型：Fix-heavy（3 Fix + 2 Proof）。
> Skill: none
> Targets: SNAPSHOT `files:` 块中 `module-drp/`、`module-projects/`、`module-maintenance/` 带 CAT2 计数的 Java 文件
> Prereqs: Phase 1 完成

- [x] <Fix> drp 4 文件（3 processor/engine + 1 statemachine）异常路径中文参数同模式改传状态码/枚举名/字典值（`SafetyStockEngine` 仅动 CAT-2 行，CAT-3 = 0）
      - Skill: none
- [x] <Fix> projects 6 文件（4 processor + 2 statemachine）同模式改写
      - Skill: none
- [x] <Fix> maintenance 6 文件（3 processor + 3 statemachine）同模式改写；`ErpMntVisitCancelProcessor` 仅动 CAT-2 行（CAT-1 已由 MI.4 清零）
      - Skill: none
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言三域 CAT2 = 0（数字记入勾选注记）
      - Skill: none
      - 断言注记（2026-09-07）：drp CAT2 9→0 / projects CAT2 9→0 / maintenance CAT2 7→0；三域 CAT1/3/4 与 SNAPSHOT 持平（drp 0/0/41、prj 0/15/134、mnt 0/13/122）
- [x] <Proof> `mvn test -pl module-drp/erp-drp-service,module-projects/erp-prj-service,module-maintenance/erp-mnt-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none
      - 验证注记（2026-09-07）：BUILD SUCCESS；erp-drp-service 98/0/0、erp-prj-service 179/0/0、erp-mnt-service 157/0/0，零新增失败

Exit Criteria:

- [x] drp / projects / maintenance 三域 CAT2 9+9+7 → 0（脚本断言）
- [x] erp-drp-service、erp-prj-service、erp-mnt-service 测试全绿，零新增失败

## Phase 3 — master-data + crm + cs + notify + b2b + contract 扫清（9 文件 11 行）

> 统一类型：Fix-heavy（4 Fix 项覆盖 6 域 + 2 Proof）。
> Skill: none
> Targets: SNAPSHOT `files:` 块中 `module-master-data/`、`module-crm/`、`module-cs/`、`module-notify/`、`module-b2b/`、`module-contract/` 带 CAT2 计数的 Java 文件
> Prereqs: Phase 2 完成

- [x] <Fix> master-data 2 文件（`ErpMdSupplierApprovalBizModel` + `ErpMdSupplierApprovalStateMachine`）同模式改写
      - Skill: none
- [x] <Fix> crm 2 文件（`ErpCrmLeadProcessor` + `ErpCrmLeadStateMachine`）同模式改写
      - Skill: none
- [x] <Fix> cs 2 文件（`ErpCsTimeEntryBizModel` + `ErpCsTicketStateMachine`）同模式改写
      - Skill: none
- [x] <Fix> notify 1 文件（`NotificationRecipientResolver`，仅剩 CAT2:2）+ b2b 1 文件（`ErpB2bPartnerProfileStateMachine`）+ contract 1 文件（`ErpCtSignatureRequestInitSignatureRequestProcessor`）同模式改写
      - Skill: none
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言六域 CAT2 = 0（数字记入勾选注记）
      - Skill: none
      - 断言注记（2026-09-07）：master-data 3→0 / crm 2→0 / cs 2→0 / notify 2→0 / b2b 1→0 / contract 1→0；六域 CAT1/3/4 与 SNAPSHOT 持平
- [x] <Proof> `mvn test -pl module-master-data/erp-md-service,module-crm/erp-crm-service,module-cs/erp-cs-service,module-notify/erp-notify-service,module-b2b/erp-b2b-service,module-contract/erp-ct-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none
      - 验证注记（2026-09-07）：BUILD SUCCESS；erp-md-service 160/0/0、erp-crm-service 188/0/0、erp-cs-service 185/0/0、erp-notify-service 23/0/0、erp-b2b-service 80/0/0、erp-ct-service 168/0/0。首跑 -am 链在上游 `erp-qa-service` 触发既有 `TestErpQaSpcOutOfControl` 时间边界快照 flake（`sampleTime` 实钟跨秒界 05.999/06.0，冻结钟扩展仅冻结日期不冻结毫秒钟，属本批未触碰的 SPC 测试面）；复跑该测试类 4/4 绿 + 续跑全链（`-rf :app-erp-quality-service`）quality 全套 184/0/0 + 六目标模块全绿，确证与本批零因果

Exit Criteria:

- [x] master-data / crm / cs / notify / b2b / contract 六域 CAT2 3+2+2+2+1+1 → 0（脚本断言）
- [x] 六个域 service 模块测试全绿，零新增失败

## Phase 4 — CAT-2 全域收官证明

> 统一类型：Proof（2 项 Proof）。
> Skill: none
> Targets: 无新增代码文件；仅断言
> Prereqs: Phase 1~3 完成

- [x] <Proof> `node tools/check-hardcoded-cjk.mjs --strict` exit 0：全域 CAT2 = 0 落账（totals 204 → 0 对账：MI.5a 136 + 本批 68，对照 Phase 1 红线注记），CAT1/3/4 计数不高于快照
      - Skill: none
      - 断言注记（2026-09-07）：RESULT: PASS，exit 0；actual totals CAT1/2/3/4 = 0/0/390/1700（CAT2 204 → 0；CAT3=390、CAT4=1700 与快照持平；CAT1=0）；脚本同时报告 234 个 file-CAT 项低于快照（单向收紧余量，含本批 49 文件 CAT2 → 0 与 MI 批 CAT1 清零遗产）
- [x] <Proof> 12 个域 service 模块（erp-mfg/erp-inv/erp-qa/erp-drp/erp-prj/erp-mnt/erp-md/erp-crm/erp-cs/erp-notify/erp-b2b/erp-ct-service）聚合 `mvn test`（含依赖模块 `-am`）全绿，零新增失败
      - Skill: none
      - 验证注记（2026-09-07）：12 模块经分相 `-pl … -am` 全绿（Phase 1/2/3 勾选注记：mfg 308 + inv 248 + qa 184 + drp 98 + prj 179 + mnt 157 + md 160 + crm 188 + cs 185 + notify 23 + b2b 80 + ct 168 = 1978/0/0）；另收官全仓验证 `mvn clean install -DskipTests`（exit 0）+ 全 reactor `mvn test` BUILD SUCCESS 4006/0/0/1（与 2026-09-06 已知良好基线逐位一致，含 12 模块超集覆盖，零新增失败）

Exit Criteria:

- [x] `--strict` 全绿，全域 CAT2 = 0 与红线注记对账一致
- [x] 12 模块测试全绿，零新增失败

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-07-0043-3-mi5b-exception-param-batch2-cat2-zero-1-8af3bdc7 to 2026-09-05-123532-mission-driver
- 2026-09-07：iteration 1，共识 accept #review-2026-09-05-123532-mission-driver-2026-09-07-0043-3-mi5b-exception-param-batch2-cat2-zero-1-8af3bdc7（审查中补 Closure Gates 缺失 + roadmap 路径具名 + Phase 3 类型计数校正 + Phase 4 模块名笔误；基线数字已对实仓复验——`check-hardcoded-cjk.mjs` 实跑 12 域 CAT2 逐域精确一致，204 = MI.5a 136 + 本批 68 对账成立）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处一次：Phase 4 已聚合 `--strict` 与 12 模块 `mvn test`；另复跑 compliance checker（本批属生产 Java 变更，零漂移复核为结束审计义务，见已知失败模式「Compliance 基线漂移」）。

（ledger 格式：本节为门控证据注记，非计数域——计数域仅 Phase 节 + Closure Findings 节，完成态由 Phase 复选框 + Verification pass 线 + Closure 审计回执派生）

- gate 1 范围内行为完成：12 域 CAT-2 归零且行为不变式未破坏（异常类型/错误码 key/抛出条件/控制流/事务边界均未改；diff 仅异常参数字符串字面量 + 1 条 `ErpInvDocStatus` 常量 import，49 文件 127+/107-）
- gate 2 相关文档对齐（本批不改 owner 行为；结束核证 `docs/architecture/i18n-compliance.md` 修复模式对照表与实态无矛盾）
- gate 3 已运行验证：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，全域 CAT2 = 0）+ 12 域 service 模块聚合 `mvn test -pl … -am` 全绿零新增失败 + `bash docs/audits/nop-compliance-checker.sh` 零漂移（若漂移，按已知失败模式开独立基线裁决后方可闭包）——均绿，见 Verification
- gate 4 无范围内项目降级为 deferred/follow-up
- gate 5 独立草案审查已完成并记录（见 Draft Review Record）——iteration 1 共识 accept #review-2026-09-05-123532-mission-driver-2026-09-07-0043-3-mi5b-exception-param-batch2-cat2-zero-1-8af3bdc7
- gate 6 文本一致性已验证：状态、阶段、门控和日志都一致——frontmatter `status: active` 未动（ledger 完成态派生，不落 `completed`）；4 Phase 27 项全 `[x]`；`docs/logs/2026/09-07.md` MI.5b 条目、roadmap MI.5b `done` 行与本计划 Verification 数字一致
- gate 7 结束审计由独立子代理（新会话）执行；执行者未自我审计——CLOSURE_AUDIT 步骤（mission-driver run 2026-09-05-123532-mission-driver，独立审计会话 opencode/glm-5.3-flash，非执行者上下文）已实跑复核，回执落 Closure 节
- gate 8 结束证据存在于文件中（Phase 勾选注记红线数字 + 验证命令输出 + Closure 审计回执对）

## Verification

- pass test 2026-09-07-mi5b exit=0 Phase 级：erp-mfg-service 308/0/0/0、erp-inv-service 248/0/0/0、erp-qa-service 184/0/0/0、erp-drp-service 98/0/0/0、erp-prj-service 179/0/0/0、erp-mnt-service 157/0/0/0、erp-md-service 160/0/0/0、erp-crm-service 188/0/0/0、erp-cs-service 185/0/0/0、erp-notify-service 23/0/0/0、erp-b2b-service 80/0/0/0、erp-ct-service 168/0/0/0（各 Phase `mvn test -pl … -am` BUILD SUCCESS，12 模块合计 1978/0/0，零新增失败）。
- pass build 2026-09-07-mi5b exit=0 全 reactor `mvn clean install -DskipTests` BUILD SUCCESS。
- pass test 2026-09-07-mi5b exit=0 全 reactor `mvn test` BUILD SUCCESS（surefire 聚合 **4006/0/0/1**，1 skip 既有预期，与 `ai-check-r3-m0` 基线行精确一致，含 12 模块超集覆盖，零新增失败）。
- pass cjk-strict 2026-09-07-mi5b exit=0 `node tools/check-hardcoded-cjk.mjs --strict`：PASS exit 0（0 new violations vs frozen snapshot，453 baseline files；12 域 49 文件 68 行 CAT2 全部归零，全局 totals CAT2 204 → 0 对账成立 = MI.5a 136 + 本批 68 对照 Phase 1 红线注记；CAT1 全域 0、CAT3 390、CAT4 1700 与快照持平不高于，IMPROVEMENTS 234 条 file-CAT 为合法单向收紧下降）。
- pass compliance 2026-09-07-mi5b exit=0 `bash docs/audits/nop-compliance-checker.sh`：exit 0；actual R2b=242 / R2c=1542 / R12a=71 与 MI.4/MI.5a 收官态逐规则一致，为既有 successor 登记的本批外预存漂移（本批 diff 仅异常参数字符串字面量改写 + 1 条 `ErpInvDocStatus` 常量 import，0 条 daoFor/共享内核 import/结构变更）——本批零新增漂移。
- pass test ai-check-r3-verify-2026-09-07-0851 exit=0 mission-driver verify run：全 reactor `mvn clean install -DskipTests` BUILD SUCCESS（2:20 min）+ 全 reactor `mvn test` 首段（md-service 起上游模块）全绿后在 erp-mfg-service 触发既有 `TestErpMfgWorkOrderEndToEnd` 时间边界快照 flake（`data.updateTime` var 校验 08:35:35.334 vs .335，实钟跨毫秒界、冻结钟扩展仅冻结日期——同族先例 Phase 3 `TestErpQaSpcOutOfControl` flake 与缓解 commit `34ab1dcb2`，失败点为 status=0 成功响应，与本批异常参数改写零因果）；复跑该测试类 4/4 绿 + 续跑 `mvn test -rf :app-erp-manufacturing-service` BUILD SUCCESS（9:52 min），两段合计全 reactor 覆盖零新增失败，full-green verification。

## Closure

Status Note: 其余 12 域 CAT-2 异常路径中文参数 68 行 / 49 文件全部归零（CAT-2 全域 204 → 0 收官对账成立 = MI.5a 136 + 本批 68），`--strict` 单向收紧 PASS exit 0，12 模块分相聚合 `mvn test` 1978/0/0 零新增失败且全 reactor `mvn test` 4006/0/0/1 与 `ai-check-r3-m0` 基线逐位一致；4 Phase 27 项全部勾选，Closure Gates 证据注记在案，独立结束审计通过（回执如下），计划可关闭。

Closure Audit Evidence:

- Auditor / Agent: opencode/glm-5.3-flash（独立闭包审计子代理会话，mission-driver run 2026-09-05-123532-mission-driver，非执行者上下文；单模型降级如实登记于 models= 后缀）
- Evidence: 独立实跑复核（2026-09-07）——`node tools/check-hardcoded-cjk.mjs --strict` PASS exit 0（0 new violations vs frozen snapshot，453 baseline files；report 模式实测全域 CAT2 无一处 >0，12 域 49 文件 68 行归零，totals 对账 204 = MI.5a 136 + 本批 68）；diff 抽查 `ErpMfgJobCardCancelJobProcessor`（`"OPEN、WORK_IN_PROGRESS 或 ON_HOLD"` → `"OPEN / WORK_IN_PROGRESS / ON_HOLD"`）、`ErpCtSignatureRequestInitSignatureRequestProcessor`（`errorMsg` 英文原因码槽）仅异常参数字符串实参改写，异常类型/错误码 key/抛出条件/控制流零变更；`docs/logs/2026/09-07.md` MI.5b 条目、roadmap MI.5b `done` 行与 Verification 数字一致；compliance 漂移（R2b/R2c/R12a）与 MI.5a 收官态逐规则一致，为既有 successor 登记的本批外预存漂移，本批零新增
- dispatch audit #audit-2026-09-05-123532-mission-driver-2026-09-07-0043-3-mi5b-exception-param-batch2-cat2-zero-1-907b23df to opencode/glm-5.3-flash models={exec:opencode/glm-5.3-flash,aud:opencode/glm-5.3-flash}
- accepted #audit-2026-09-05-123532-mission-driver-2026-09-07-0043-3-mi5b-exception-param-batch2-cat2-zero-1-907b23df：审计结论 approved——12 域 68 行 / 49 文件 CAT-2 归零与 Phase 1 红线注记逐域对账一致（CAT-2 全域 204 → 0 收官），行为不变式（错误码 key/模板、异常类型、抛出条件、控制流、事务边界零变更）抽查无违例，`--strict` exit 0 与 12 模块/全 reactor 测试数字与实时仓库一致，文档同步无漂移，准予关闭。
