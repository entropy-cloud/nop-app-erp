---
status: active
mission: ai-check-r3
work-item: MI.3
group: "2026-09-06-2104"
verify: [test]
---

# 2026-09-06-2104-3 MI.3 LOG 英文化批 2（mfg/inv/cs/b2b/pur/sal/hr）

## Current Baseline

- 冻结基线（`docs/audits/cjk-baseline.md` §SNAPSHOT，2026-09-06）：CAT-1（LOG 语句含中文）——manufacturing 31 行 / 10 文件、inventory 26 行 / 8 文件、cs 22 行 / 11 文件、b2b 22 行 / 7 文件、purchase 22 行 / 8 文件、sales 22 行 / 9 文件、hr 20 行 / 6 文件，合计 165 行 / 59 文件；文件级逐文件计数以该 SNAPSHOT `files:` 块为准。
- 口径注记：roadmap MI.3 行探针数（「约 166 行、cs 23」）为 2026-08-31 口径敏感快照；按 roadmap §目的「口径敏感项以 M0.2 脚本 + M0.3 快照冻结为准」，本批红线以冻结 SNAPSHOT 为准（cs = 22，实仓复核一致）。
- 修复模式（`docs/architecture/i18n-compliance.md` 修复模式对照表 CAT-1 行）：LOG 消息模板改英文，保留 `{}` 占位参数与参数值不变，不走 i18n 机制。
- 行为不变式（roadmap 横切关注点 8 + `docs/lessons/09-posting-exception-swallow-suspension.md`）：仅改消息载体字符串，禁止借机改吞异常行为、控制流、日志级别或参数求值顺序；域内过账/编排族 warn/error 降级消息语义不变。
- 域内 CAT-2/CAT-3 计数（mfg CAT2=10/CAT3=28、inv CAT2=11/CAT3=28、cs CAT2=2/CAT3=43、b2b CAT2=1/CAT3=15、purchase CAT2=36/CAT3=14、sales CAT2=31/CAT3=13、hr CAT2=0/CAT3=28）本批不触碰，归 MI.5a/5b/6。
- 依赖状态：M0.6 done；本批依赖 MI.2（依赖图 MI.2 → MI.3），执行顺序居本批三计划之第 3。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行（全 reactor 4006/0/0/1；CJK `--strict` 绿）。
- 剩余差距：7 域 165 行 CAT-1 红线；`--strict` 门控下 actual 只降不升，本批目标 = 7 域 CAT1 归零。

## Goals

- 7 域 CAT-1 归零（165 行 / 59 文件），LOG 消息模板全部英文化，语义与原中文消息等价。
- 7 个域 service 模块 `mvn test` 全绿零回归；`node tools/check-hardcoded-cjk.mjs --strict` 保持绿且 7 域 CAT1=0（单向收紧合法下降）。

## Non-Goals

- 不改业务行为、控制流、异常语义、日志级别（横切关注点 8）。
- 不动 CAT-2/3/4 面（MI.5a/5b/6/7/8 范围）；logistics/notify/projects/contract/maintenance/crm/drp/quality 域 LOG 归 MI.4。
- 不动 `_init-data` seed、ORM 模型、页面文件。

## Phase 1 — manufacturing + inventory 扫清（18 文件 57 行）

> 统一类型：Fix-heavy（2 Fix + 2 Proof）。
> Skill: none（roadmap MI.3 行指定）
> Targets: SNAPSHOT `files:` 块中 `module-manufacturing/`、`module-inventory/` 带 CAT1 计数的 Java 文件
> Prereqs: plan `2026-09-06-2104-2`（MI.2）完成

- [ ] <Fix> manufacturing 10 文件 LOG 消息模板逐行英文化（保留 `{}` 占位与参数、日志级别、控制流不变）
      - Skill: none
- [ ] <Fix> inventory 8 文件同模式英文化
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言两域 CAT1 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-manufacturing/erp-mfg-service,module-inventory/erp-inv-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] manufacturing / inventory 两域 CAT1 31+26 → 0（脚本断言）
- [ ] erp-mfg-service、erp-inv-service 测试全绿，零新增失败

## Phase 2 — cs + b2b + purchase 扫清（26 文件 66 行）

> 统一类型：Fix-heavy（2 Fix + 2 Proof）。
> Skill: none
> Targets: SNAPSHOT `files:` 块中 `module-cs/`、`module-b2b/`、`module-purchase/` 带 CAT1 计数的 Java 文件
> Prereqs: Phase 1 完成

- [ ] <Fix> cs 11 文件 + b2b 7 文件 LOG 消息模板逐行英文化（同批 1 模式）
      - Skill: none
- [ ] <Fix> purchase 8 文件同模式英文化；purchase 过账/核销族 warn/error 降级消息语义不变
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言三域 CAT1 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-cs/erp-cs-service,module-b2b/erp-b2b-service,module-purchase/erp-pur-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] cs / b2b / purchase 三域 CAT1 22+22+22 → 0（脚本断言）
- [ ] erp-cs-service、erp-b2b-service、erp-pur-service 测试全绿，零新增失败

## Phase 3 — sales + hr 扫清（15 文件 42 行）

> 统一类型：Fix-heavy（2 Fix + 2 Proof）。
> Skill: none
> Targets: SNAPSHOT `files:` 块中 `module-sales/`、`module-hr/` 带 CAT1 计数的 Java 文件
> Prereqs: Phase 2 完成

- [ ] <Fix> sales 9 文件 LOG 消息模板逐行英文化（同批 1 模式）
      - Skill: none
- [ ] <Fix> hr 6 文件同模式英文化
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言两域 CAT1 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-sales/erp-sal-service,module-hr/erp-hr-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] sales / hr 两域 CAT1 22+20 → 0（脚本断言）
- [ ] erp-sal-service、erp-hr-service 测试全绿，零新增失败

## Phase 4 — 批级归零证明

> 统一类型：Proof（2 项 Proof）。
> Skill: none
> Targets: 无新增代码文件；仅断言
> Prereqs: Phase 1~3 完成

- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs --strict` exit 0：7 域 CAT1 全部 = 0 落账，其余域与 CAT2/3/4 计数不高于快照（本批累计 165 行归零对照 Phase 1 红线注记）
      - Skill: none
- [ ] <Proof> 7 个域 service 模块（erp-mfg/erp-inv/erp-cs/erp-b2b/erp-pur/erp-sal/erp-hr -service）聚合 `mvn test`（含依赖模块 `-am`）全绿，零新增失败
      - Skill: none

Exit Criteria:

- [ ] `--strict` 全绿，7 域 CAT1 = 0 与红线注记对账一致
- [ ] 7 模块测试全绿，零新增失败

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-06-2104-3-mi3-log-english-batch2-1-53cbab9a to 2026-09-05-123532-mission-driver
- 2026-09-07：iteration 1，共识 accept #review-2026-09-05-123532-mission-driver-2026-09-06-2104-3-mi3-log-english-batch2-1-53cbab9a（审查中补 Closure Gates 缺失：空 `## Verification` 节替换为可勾选结束门控，含 compliance checker 零漂移复跑项——LOG 字符串属生产代码变更）

## Closure Gates

> 仅在所有执行项与各 Phase 退出标准勾选 `[x]` 后关闭。完整仓库验证在此处运行一次（阶段退出仅做域级脚本断言与模块测试）。

- [ ] 范围内行为完成：7 域 CAT-1 归零（165 行 / 59 文件），LOG 消息模板全部英文且语义与原中文消息等价
- [ ] `node tools/check-hardcoded-cjk.mjs --strict` exit 0：7 域 CAT1 = 0 落账，其余域与 CAT2/3/4 计数不高于 SNAPSHOT 快照（单向收紧合法下降）
- [ ] 7 个域 service 模块（erp-mfg/erp-inv/erp-cs/erp-b2b/erp-pur/erp-sal/erp-hr -service）聚合 `mvn test`（含 `-am`）全绿，零新增失败
- [ ] 生产代码变更零基线漂移：复跑 `bash docs/audits/nop-compliance-checker.sh` 确认 actual 不高于 baseline（LOG 字符串变更不应触发漂移；若漂移则闭包前按 `docs/lessons/` 既定路径开独立基线裁决）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录（见 Draft Review Record）
- [ ] 文本一致性已验证：frontmatter status、各 Phase 状态、退出标准、门控与 `docs/logs/` 条目一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中（Closure 节 + `docs/logs/` 登记）

## Closure
