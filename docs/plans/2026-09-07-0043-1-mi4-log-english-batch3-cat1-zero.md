---
status: active
mission: ai-check-r3
work-item: MI.4
group: "2026-09-07-0043"
verify: [test]
---

# 2026-09-07-0043-1 MI.4 LOG 英文化批 3 + CAT-1 归零（log/notify/prj/ct/mnt/crm/drp/qa + aps 补遗）

## Current Baseline

- 冻结基线（`docs/audits/cjk-baseline.md` §SNAPSHOT，2026-09-06）：CAT-1（LOG 语句含中文）——logistics 19 行 / 4 文件、notify 15 行 / 5 文件、projects 14 行 / 6 文件、contract 12 行 / 5 文件、maintenance 10 行 / 4 文件、crm 3 行 / 3 文件、drp 2 行 / 1 文件、quality 1 行 / 1 文件（roadmap MI.4 探针清单 8 域合计 76 行），另 aps 1 行 / 1 文件（`ErpApsSchedulingProcessor`）；合计 77 行 / 30 文件。
- 对账注记：roadmap MI.4 探针行未列 aps（2026-08-31 探针快照 aps LOG=0，冻结 SNAPSHOT 实测 1）；按 roadmap §目的「口径敏感项以 M0.2 脚本 + M0.3 快照冻结为准」，本批以冻结 SNAPSHOT 为准并纳入 aps。CAT-1 全量 335 = MI.2 93（fin 54 + ast 39）+ MI.3 165（7 域）+ 本批 77；收官断言「CAT-1 全域 = 0」必须含 aps 补遗，否则不成立。
- 修复模式（`docs/architecture/i18n-compliance.md` 修复模式对照表 CAT-1 行）：LOG 消息模板改英文，保留 `{}` 占位参数与参数值不变，不走 i18n 机制。
- 行为不变式（roadmap 横切关注点 8 + `docs/lessons/09-posting-exception-swallow-suspension.md`）：仅改消息载体字符串，禁止借机改吞异常行为、控制流、日志级别或参数求值顺序；notify 派发族（`NotificationDispatcher`）、logistics gateway 族（`GatewayDispatcher`）、projects/maintenance 过账 dispatcher 族 warn/error 降级消息语义不变。
- 域内 CAT-2/CAT-3 计数（mnt CAT2=7/CAT3=13、drp CAT2=9/CAT3=0、qa CAT2=11/CAT3=13、prj CAT2=9/CAT3=15、notify CAT2=2/CAT3=1、ct CAT2=1/CAT3=6、crm CAT2=2/CAT3=2、log CAT2=0/CAT3=3、aps CAT2=0/CAT3=20）本批不触碰，归 MI.5b（CAT-2）/ MI.6（CAT-3）。
- 依赖状态：M0.6 done；本批依赖 MI.3（依赖图 MI.3 → MI.4），执行顺序居本批三计划之第 1。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行（全 reactor 4006/0/0/1；CJK `--strict` 绿）。
- 剩余差距：9 域 77 行 CAT-1 红线；`--strict` 门控下 actual 只降不升，本批目标 = 全域 CAT1 归零（MI 里程碑 CAT-1 收官批）。

## Goals

- 9 域（logistics/notify/projects/contract/maintenance/crm/drp/quality + aps 补遗）CAT-1 归零（77 行 / 30 文件），LOG 消息模板全部英文化，语义与原中文消息等价。
- CAT-1 全域 = 0 收官断言（`--strict` 绿 + totals CAT1 335 → 0 对账）；9 个域 service 模块 `mvn test` 全绿零回归。

## Non-Goals

- 不改业务行为、控制流、异常语义、日志级别（横切关注点 8）。
- 不动 CAT-2/3/4 面（MI.5a/5b/6/7/8 范围）。
- 不动 `_init-data` seed、ORM 模型、页面文件。

## Phase 1 — 红线记录 + logistics + notify 扫清（9 文件 34 行）

> 统一类型：Fix-heavy（1 红线 Proof + 2 Fix + 2 Proof）。
> Skill: none（roadmap MI.4 行指定）
> Targets: SNAPSHOT `files:` 块中 `module-logistics/`、`module-notify/` 带 CAT1 计数的 Java 文件
> Prereqs: plan `2026-09-06-2104-3`（MI.3）完成

- [ ] <Proof> 批第一动作：`node tools/check-hardcoded-cjk.mjs` 实跑，记录 9 域红线数字（log 19 / notify 15 / prj 14 / ct 12 / mnt 10 / crm 3 / drp 2 / qa 1 / aps 1）于本项勾选注记（修复批产物 = 脚本输出数字 + 归零断言，不产 ck-* 报告，横切关注点 13）
      - Skill: none
- [ ] <Fix> logistics 4 文件 LOG 消息模板逐行英文化（保留 `{}` 占位与参数、日志级别、控制流不变）；`GatewayDispatcher` 网关族 warn/error 降级消息语义不变
      - Skill: none
- [ ] <Fix> notify 5 文件同模式英文化；`NotificationDispatcher` 派发族 warn/error 降级消息语义不变；`NotificationRecipientResolver` 仅动 CAT-1 行（CAT2:2 保持原样归 MI.5b）
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言两域 CAT1 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-logistics/erp-log-service,module-notify/erp-notify-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] logistics / notify 两域 CAT1 19+15 → 0（脚本断言）
- [ ] erp-log-service、erp-notify-service 测试全绿，零新增失败

## Phase 2 — projects + contract 扫清（11 文件 26 行）

> 统一类型：Fix-heavy（2 Fix + 2 Proof）。
> Skill: none
> Targets: SNAPSHOT `files:` 块中 `module-projects/`、`module-contract/` 带 CAT1 计数的 Java 文件
> Prereqs: Phase 1 完成

- [ ] <Fix> projects 6 文件 LOG 消息模板逐行英文化；`ProjectSettlementPostingDispatcher`/`TimesheetPostingDispatcher` 过账族 warn/error 降级消息语义不变
      - Skill: none
- [ ] <Fix> contract 5 文件同模式英文化；`ErpCtContractBizModel`/`ErpCtDocumentBizModel` 仅动 CAT-1 行（CAT3 保持原样归 MI.6）
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言两域 CAT1 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-projects/erp-prj-service,module-contract/erp-ct-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] projects / contract 两域 CAT1 14+12 → 0（脚本断言）
- [ ] erp-prj-service、erp-ct-service 测试全绿，零新增失败

## Phase 3 — maintenance + crm + drp + quality + aps 扫清（10 文件 17 行）

> 统一类型：Fix-heavy（5 Fix + 2 Proof）。
> Skill: none
> Targets: SNAPSHOT `files:` 块中 `module-maintenance/`、`module-crm/`、`module-drp/`、`module-quality/`、`module-aps/` 带 CAT1 计数的 Java 文件
> Prereqs: Phase 2 完成

- [ ] <Fix> maintenance 4 文件 LOG 消息模板逐行英文化；`MaintenanceIssuePostingDispatcher`/`MaintenanceLaborPostingDispatcher` 过账族降级消息语义不变；`ErpMntVisitCancelProcessor` 仅动 CAT-1 行（CAT2:1 归 MI.5b）
      - Skill: none
- [ ] <Fix> crm 3 文件同模式英文化；`ErpCrmLeadProcessor` 仅动 CAT-1 行（CAT2:1 归 MI.5b）
      - Skill: none
- [ ] <Fix> drp 1 文件（`ErpDrpCrossDockStagingTimeoutJob`）+ quality 1 文件（`NcrPostingDispatcher`）+ aps 补遗 1 文件（`ErpApsSchedulingProcessor`）同模式英文化
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言五域 CAT1 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-maintenance/erp-mnt-service,module-crm/erp-crm-service,module-drp/erp-drp-service,module-quality/erp-qa-service,module-aps/erp-aps-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] maintenance / crm / drp / quality / aps 五域 CAT1 10+3+2+1+1 → 0（脚本断言）
- [ ] 五个域 service 模块测试全绿，零新增失败

## Phase 4 — CAT-1 全域收官证明

> 统一类型：Proof（2 项 Proof）。
> Skill: none
> Targets: 无新增代码文件；仅断言
> Prereqs: Phase 1~3 完成

- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs --strict` exit 0：全域 CAT1 = 0 落账（totals 335 → 0 对账：MI.2 93 + MI.3 165 + 本批 77，对照 Phase 1 红线注记），CAT2/3/4 计数不高于快照
      - Skill: none
- [ ] <Proof> 9 个域 service 模块（erp-log/erp-notify/erp-prj/erp-ct/erp-mnt/erp-crm/erp-drp/erp-qa/erp-aps -service）聚合 `mvn test`（含依赖模块 `-am`）全绿，零新增失败
      - Skill: none

Exit Criteria:

- [ ] `--strict` 全绿，全域 CAT1 = 0 与红线注记对账一致
- [ ] 9 模块测试全绿，零新增失败

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-07-0043-1-mi4-log-english-batch3-cat1-zero-1-6a0c0600 to 2026-09-05-123532-mission-driver
- 2026-09-07：iteration 1，共识 accept #review-2026-09-05-123532-mission-driver-2026-09-07-0043-1-mi4-log-english-batch3-cat1-zero-1-6a0c0600（审查中补 Closure Gates 缺失：空 `## Verification` 节替换为可勾选结束门控，含 compliance checker 零漂移复跑项——LOG 字符串属生产代码变更；基线数字经实仓对账与 SNAPSHOT 精确一致）

## Closure Gates

> 仅在所有执行项与各 Phase 退出标准勾选 `[x]` 后关闭。完整仓库验证在此处运行一次（阶段退出仅做域级脚本断言与模块测试）。

- [ ] 范围内行为完成：9 域 CAT-1 归零（77 行 / 30 文件，含 aps 补遗），LOG 消息模板全部英文且语义与原中文消息等价
- [ ] `node tools/check-hardcoded-cjk.mjs --strict` exit 0：全域 CAT1 = 0 落账（totals 335 → 0 对账：MI.2 93 + MI.3 165 + 本批 77，对照 Phase 1 红线注记），CAT2/3/4 计数不高于 SNAPSHOT 快照（单向收紧合法下降）
- [ ] 9 个域 service 模块（erp-log/erp-notify/erp-prj/erp-ct/erp-mnt/erp-crm/erp-drp/erp-qa/erp-aps -service）聚合 `mvn test`（含 `-am`）全绿，零新增失败
- [ ] 生产代码变更零基线漂移：复跑 `bash docs/audits/nop-compliance-checker.sh` 确认 actual 不高于 baseline（LOG 字符串变更不应触发漂移；若漂移则闭包前按 `docs/lessons/` 既定路径开独立基线裁决）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录（见 Draft Review Record）
- [ ] 文本一致性已验证：frontmatter status、各 Phase 状态、退出标准、门控与 `docs/logs/` 条目一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中（Closure 节 + `docs/logs/` 登记）

## Closure
