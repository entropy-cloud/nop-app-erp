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
> Status: planned
> Skill: none（roadmap MI.5a 行指定）
> Targets: `module-common-service/src/main/java/app/erp/common/service/Abstract*.java`（5 个 Abstract*Processor + helper `AbstractProcessor`）
> Prereqs: plan `2026-09-07-0043-1`（MI.4）完成

- [ ] <Proof> 批第一动作：`node tools/check-hardcoded-cjk.mjs` 实跑，记录 5 面红线数字（common 5 / pur 36 / sal 31 / ast 41 / fin 23）于本项勾选注记（修复批产物 = 脚本输出数字 + 归零断言，不产 ck-* 报告，横切关注点 13）
      - Skill: none
- [ ] <Fix | Decision> 抽象族 5 文件期望态实参改传状态码/枚举名（`"非已作废"` 型散文 → 状态码本身）；`defaultIllegalStatusException` helper 契约收敛为「期望态传码」——保持 `String... expected` 签名不变；若实现裁决发现需变更签名/参数类型（如改枚举），在计划勾选注记记录 Decision（选择、替代方案、残余风险）并同步收敛全部调用点
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言 common-service CAT2 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-common-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] common-service CAT2 5 → 0（脚本断言）
- [ ] module-common-service 测试全绿，零新增失败

## Phase 2 — purchase + sales 扫清（38 文件 67 行）

> 统一类型：Fix-heavy（2 Fix + 2 Proof）。
> Status: planned
> Skill: none
> Targets: SNAPSHOT `files:` 块中 `module-purchase/`、`module-sales/` 带 CAT2 计数的 Java 文件
> Prereqs: Phase 1 完成（抽象族契约先行收敛，域内同型调用点随语义改写）

- [ ] <Fix> purchase 20 文件（2 entity + 10 processor + 8 statemachine）异常路径中文参数逐行改传状态码/枚举名/字典值；仅动 CAT-2 行（CAT-3 保持原样归 MI.6）
      - Skill: none
- [ ] <Fix> sales 18 文件（12 processor/entity + 6 statemachine）同模式改写
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言两域 CAT2 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] purchase / sales 两域 CAT2 36+31 → 0（脚本断言）
- [ ] erp-pur-service、erp-sal-service 测试全绿，零新增失败

## Phase 3 — assets + finance 扫清（32 文件 64 行）

> 统一类型：Fix-heavy（2 Fix + 2 Proof）。
> Status: planned
> Skill: none
> Targets: SNAPSHOT `files:` 块中 `module-assets/`、`module-finance/` 带 CAT2 计数的 Java 文件
> Prereqs: Phase 2 完成

- [ ] <Fix> assets 22 文件（8 processor + 14 statemachine）异常路径中文参数同模式改传状态码/枚举名/字典值
      - Skill: none
- [ ] <Fix> finance 10 文件（6 processor/service + 4 statemachine）同模式改写；`CloseVoucherWriter`/`ErpFinApDocumentPipelineProcessor`/`ErpFinVoucherTemplateRenderTemplateProcessor` 仅动 CAT-2 行
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言两域 CAT2 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-assets/erp-ast-service,module-finance/erp-fin-service`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] assets / finance 两域 CAT2 41+23 → 0（脚本断言）
- [ ] erp-ast-service、erp-fin-service 测试全绿，零新增失败

## Phase 4 — 批级归零证明

> 统一类型：Proof（2 项 Proof）。
> Status: planned
> Skill: none
> Targets: 无新增代码文件；仅断言
> Prereqs: Phase 1~3 完成

- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs --strict` exit 0：5 面 CAT2 = 0 落账（对照 Phase 1 红线注记，本批累计 136 行归零），其余域 CAT2 与 CAT1/3/4 计数不高于快照
      - Skill: none
- [ ] <Proof> 5 个模块（module-common-service + erp-pur/erp-sal/erp-ast/erp-fin -service）聚合 `mvn test`（含依赖模块 `-am`）全绿，零新增失败
      - Skill: none

Exit Criteria:

- [ ] `--strict` 全绿，5 面 CAT2 = 0 与红线注记对账一致
- [ ] 5 模块测试全绿，零新增失败

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-07-0043-2-mi5a-exception-param-batch1-1-63a49956 to opencode/glm-5.3-flash
- 2026-09-07：iteration 1，共识 approved #review-2026-09-05-123532-mission-driver-2026-09-07-0043-2-mi5a-exception-param-batch1-1-63a49956

## Closure Gates

> 仅在所有执行项目和各阶段退出标准全部勾选 `[x]` 后关闭。完整仓库验证在此处运行一次（不在阶段退出标准重复）。

- [ ] 范围内行为完成：5 面 CAT2 归零，`--strict` 断言与 Phase 1 红线注记对账一致（累计 136 行 / 75 文件）
- [ ] 若 Phase 1 Decision 落定 helper 签名/参数类型变更：同步核对 `docs/architecture/i18n-compliance.md` CAT-2 修复模式行与 helper 新契约表述一致
- [ ] 已运行验证：`node tools/check-hardcoded-cjk.mjs --strict` exit 0 且 5 面 CAT2=0；5 模块聚合 `mvn test`（含 `-am`）全绿零新增失败；`bash docs/audits/nop-compliance-checker.sh` 复跑无 actual > baseline 漂移（生产代码变更的结束审计硬要求）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录（见 Draft Review Record）
- [ ] 文本一致性已验证：frontmatter `status`、各 Phase `Status`/`Exit Criteria`、Closure Gates 与 `docs/logs/` 条目一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中（Closure 节记录审计者与证据）

## Closure

Status Note: <closure 时填写>

Closure Audit Evidence:

- Auditor / Agent: <closure 时由独立审计子代理填写>
- Evidence: <closure 时由独立审计子代理填写>
