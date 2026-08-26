# 2026-08-26-0530-1-ai-check-fix-f1-4-acct-schema-fail-closed F1.4：账套解析空静默成功 fail-closed

> Plan Status: completed
> Last Reviewed: 2026-08-26
> Source: ai-check F1.4；finding：P1-CK-fin-005（`docs/audits/check/ck-finance-posting.md`）
> Related: F1.1（同引擎改动先例）；ast/sal/pur/prj/mfg 六 dispatcher 传导面（同型受影响注记）
> Audit: required（会计/财务过账引擎 = plan-first）

## Current Baseline

- P1-CK-fin-005（已实证）：`AcctSchemaResolver` 可返回 null（org 无 ACTIVE 账套）→ 各 dispatcher `resolveAcctSchemaId` 传 null → `SchemaPropagator.resolveTargetSchemas(orgId, null)` 单/多账套两分支均返回**空列表** → `process()` for 循环零执行 → 返回 null——**零凭证、零异常记录、零告警的第三态**（F1.1 后 dispatcher 侧可见 posted=false，但引擎无失败信号、期末门控三扫描面全落空）。
- F1.1 后语义：幂等命中返回既有 id；本缺陷路径（fresh post + null schema）仍返回 null 且无异常——与真失败不可区分。
- 剩余差距：引擎入口无账套 fail-closed 守卫。

## Goals

- 引擎 `process()` 入口：`event.getAcctSchemaId() == null` 时抛新错误码 `erp.err.fin.posting.no-active-schema`（中文描述 + orgId/businessType/billHeadCode 参数）——一处中央修复覆盖全域 8+ dispatcher 传导面（ast-006 站点注记同步收口）。

## Non-Goals

- 不改 AcctSchemaResolver 本体（其 null 语义被多消费方依赖——引擎守卫是收敛点）；不改 SchemaPropagator 多账套逻辑；不补期末门控扫描面（守卫后该场景不再产生悬挂态）。

## Task Route

- Type: implementation-only change
- Owner Docs: `docs/design/finance/multiple-accounting-schemas.md`、`docs/architecture/processor-extension-pattern.md`
- Skill Selection Basis: none（根因已实证，直入修复）

## Infrastructure And Config Prereqs

No infra prereqs.

## Execution Plan

### Phase 1 - 引擎守卫 + 测试

Status: completed
Targets: `ErpFinPostingProcessor#process`、`ErpFinPostingErrors`（或 ErpFinErrors）
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 独立草案审查通过

- [x] Fix（R1 修订——守卫入 try 块）：process() try 块第一条语句（幂等短路之后）：`event.getAcctSchemaId() == null` 抛 NopException——经既有 catch → recordPostFailure → ErpFinPostingException PENDING → sweep 重试链（MAX_RETRY→MANUAL）+ G2 告警，达成 fin-005 可观测性目标（try 前抛会绕过记录链，审计 R1 指出按原字面实施将挫败修复目标）。`resolveTargetSchemas` L138 原地不动（null 输入零 DAO 短路）。错误码 `erp.err.fin.posting.no-active-schema`，文案「组织未配置账套，无法过账」（S3：resolver 仅组织零账套行返回 null；与 open 的 fin-004 ACTIVE 过滤联动注记）
- [ ] Decision（R2 两条不吞异常传播链，显式接受为 fail-closed 改进）：①qa NCR dispatchScrap 无内部 catch——守卫抛错使 NCR 上账/处置 mutation 整体失败（今日：静默零凭证 + NCR resolve 假成功，留下断账）——接受，理由：org 无账套属配置错误，fail-closed 优于静默断账；②prj 保留金返还路径 catch 包装重抛 ERR_RETENTION_RETURN_POSTING_FAILED——结算 mutation 失败——同理由接受。全域其余 dispatcher 吞异常站点行为不变（多一条 warn + 异常工作台记录）
- [x] Proof: fin 新单测——null schema 的 PostingEvent 直调 process 断言抛错且错误码/参数正确；既有 fin 497 全绿（确认无合法 null-schema 过账路径——若有测试依赖将暴露并裁决）

Exit Criteria:

- [ ] 新守卫测试绿 + fin 全量绿

## Draft Review Record

- Independent draft review iteration 1: `needs revision → 修订后通过`（agent `agent_37e68934`，2026-08-26）——误伤面全域核实成立（48 文件 8+ dispatcher 逐域查：全域无合法 null-schema 过账；hr javadoc 自证该路径为登记过的潜伏缺陷）；幂等排序意图正确但守卫位置描述与实际代码顺序矛盾（R1：try 前抛绕过 catch→recordPostFailure→PENDING→sweep 记录链，按原字面实施将挫败 fin-005 可观测性目标——修订为 try 块首语句）；R2 两条不吞异常传播链（qa NCR/prj 保留金）显式裁决为接受的 fail-closed 改进；S1-S4（ast-006 撞号措辞/closer 服务覆盖面诚实性/错误文案/测试计数口径）全部采纳。修订后 Plan Status: active。

## Closure Gates

- [x] 范围内行为完成
- [x] `mvn test -pl module-finance/erp-fin-service` **498/498 全绿**（含新增 testNullSchemaFailsClosed；两既有审批测试快照重录——守卫使无种子环境的首个失败成因更准确：无账套先于无期间，异常工作台记录链不变）+ 全 reactor install
- [x] compliance 零漂移（无新 daoFor）
- [ ] 索引回填：P1-CK-fin-005 → fixed；ast-006 传导注记同步
- [x] roadmap F1.4 → done
- [x] 独立结束审计
- [x] `docs/logs/2026/08-26.md` 追加

## Deferred But Adjudicated

### resolver 本体 null 语义统一

- Classification: watch-only residual
- Why Not Blocking Closure: resolver null 被 4 个 closer 服务 + 2 个 VoucherBuilder（硬编码 "1" 回退）+ 预算/成本消费方依赖，引擎守卫已收敛最危险路径（凭证零落）
- Successor Required: no

### fin 内部 closer 服务同病控制点（S2）

- Classification: watch-only residual
- Why Not Blocking Closure: AnnualClose/ProfitLossClosing/ExchangeRevaluation/BadDebtProvision 四服务自行循环 resolveTargetSchemas 零迭代静默跳过（不进 process()，本修复不覆盖）；期末门控 preCheck 已扫该族悬挂面
- Successor Required: no（F2.x finance 域簇内遇该报告控制点再收口）

## Closure

Status Note: 第一批（F1.1-F1.4）最后一片——引擎 fail-closed + 异常工作台记录链 + fin 498/498 + 全 reactor install + compliance 零漂移。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `agent_a77244d0`（fresh session）
- Evidence: **首轮 PASS**（7 项全过：守卫位置/错误码/三测试独立重跑 24 绿/fin 498 与 reactor install 与 R2c=1506 三项独立复现/R2 两传播链事实抽查成立/索引 501 吻合/Deferred 完整/anti-hollow 含快照归属甄别）；4 收尾项（ast 报告注记/勾选对齐/日志补 install 留痕）当日处理
