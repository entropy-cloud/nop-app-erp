# 2026-08-26-0630-1-ai-check-fix-f2-1-finance-posting-p1 F2.1：finance-过账 P1 余项（fin-001 回写通道 / fin-002 平衡校验 / fin-004 ACTIVE 过滤）

> Plan Status: completed
> Last Reviewed: 2026-08-27
> Source: ai-check F2.1；findings：P1-CK-fin-001/002/004（`docs/audits/check/ck-finance-posting.md`）
> Related: F1.1/F1.4（引擎幂等与 fail-closed 基线）；勘察设计（agent_6d7d7760，2026-08-26，含触发点裁决/域清单/挂点依据）
> Audit: required（过账引擎 plan-first）

## Current Baseline

- **fin-001**：sweep 重试成功仅 markRetried，源单 posted 永久 false（反向有 VoucherReversedListener 对偶，正向无）。裁决：**只在 sweep RetryHelper + 手动 RetryProcessor 两处「调用方不在场」通道派发** posted 事件。引擎 process() 路径不派发的真实依据（审查 R1 修正——原勘察「stale 实例乐观锁冲突」论证与平台共享 session 语义相悖，被既有方向一双写绿测（reverseProcess→PurReversalListener 内层写→外层同 session 再写）证伪）：①`posting.md §反写契约`成文裁决「正常过账成功 → 域调用方置 posted=true」域自治、引擎不持源实体；②硬规则 6 原子性——引擎内派发使 posted=true 随 REQUIRES_NEW 内层事务**提前提交逃逸主事务回滚**（外层后置失败 → posted=true + 未审批回滚的原子性破坏）+ 脏实体跨连接 flush 风险；③F1.1 幂等收敛已覆盖 SYNC 重审悬挂（返回 id→调用方置位）。平台事实：REQUIRES_NEW 只换事务不换 session（线程级注册表继承），审查以 `OrmSessionRegistry`/`OrmTemplateImpl#runInSession` L204-211/`OrmTransactionListener#onBeforeCommit` 源码级核实。
- **fin-002**：postVoucher 无借贷平衡校验；owner doc state-machine.md L40 明列平衡为 DRAFT→POSTED 迁移守卫——挂**过账边**（BizModel postVoucher），不挂保存（草稿补录语义）。
- **fin-004**：AcctSchemaResolver/SchemaPropagator 无 ACTIVE 过滤；字典恰 ACTIVE/INACTIVE 两值（纯过滤问题闭合）；收紧后与 F1.4 守卫形成 fail-closed 闭环。

## Goals

- fin-001：`VoucherPostedEvent` + `IErpFinVoucherPostedListener` + `ErpFinPostedListenerRegistry`（镜像反向对偶）+ 两重试通道派发（listener 失败经 recorder 落工作台且 eventData 透传形成自愈）+ **一期 5 核心域 listener**（purchase/sales/inventory/finance/mfg——前四者扩展现有 ReversalListener 类 dual 接口；反射写回否决：违反反写契约 + billHeadCode 编码不统一）。
- fin-002：postVoucher 平衡断言（Σdebit==Σcredit，复用 ERR_UNBALANCED）+ 头合计重算写回。
- fin-004：两处查询加 `eq("status", STATUS_ACTIVE)` + 常量提取（AcctSchemaResolver）。

## Non-Goals

- 二期 5 域 listener（assets/hr/projects/maintenance/quality——专用解码集中，F2.x 各域簇内收口）；listener 不重放域侧编排（冲抵等残留已知，posted-only 语义与反向 listener 哲学一致）；零行凭证守卫（可选防御，实现时定夺并注记）；previewReverseVoucher 告警。

## Task Route

- Type: implementation-only change
- Owner Docs: `docs/design/finance/posting.md`（§反写契约）、`docs/design/finance/state-machine.md`（L40 平衡守卫）、`docs/design/finance/multiple-accounting-schemas.md`
- Skill Selection Basis: none（勘察含 owner doc 依据）

## Execution Plan

### Phase 1 - fin-002 + fin-004（两个 S 项先行）

Status: completed
Targets: `ErpFinVoucherBizModel#postVoucher`、`AcctSchemaResolver`、`SchemaPropagator`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 独立草案审查通过

- [x] Fix: fin-002 postVoucher 平衡断言 + 头合计重算（不平衡抛 ERR_UNBALANCED 保持 DRAFT；负数红字风格平衡通过）
- [x] Fix: fin-004 两处 ACTIVE 过滤 + `STATUS_ACTIVE` 常量（resolvePrimarySchemaId 与 findActiveSchemasByOrg；statusScore 死代码化简）
- [x] Proof: 新测试——不平衡凭证 postVoucher 拒 + 平衡/红字风格通过 + 头合计重算；resolver 混合 ACTIVE/INACTIVE 只返 ACTIVE、仅 INACTIVE 返 null；`TestErpFinMultiSchemaPosting` 增 INACTIVE 账套排除例；fin 全量绿

Exit Criteria:

- [x] 新测试绿 + `mvn test -pl module-finance/erp-fin-service,module-master-data/erp-md-service` 绿

### Phase 2 - fin-001 一期（基础设施 + 5 核心域）

Status: completed
Targets: 新文件 ×3（event/interface/registry）+ beans.xml + RetryHelper/RetryProcessor 派发 + 4 个 ReversalListener 扩展 dual 接口 + 新 FinPostedListener
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] Fix: finance 基础设施（event/interface/registry/错误码/FAILED_STAGE 常量/beans collect-beans/空监听 warn）
- [x] Fix: 两通道派发（doRetry NORMAL 分支 post 返回非 null 后 + 手动 retry；listener 失败经 recorder 落工作台 eventData 透传自愈；REVERSAL 分支不派发）
- [x] Fix: 5 域 listener（pur/sal/inv/mfg 扩展现有类 dual 接口——posted=true+三字段、已 true 跳过防 version 无谓递增、findByCode miss no-op；fin 新建 FinPostedListener 覆盖 EXPENSE_CLAIM/EMPLOYEE_ADVANCE/NOTES_*；inv 增 stock-move 分支与 -PPV strip；mfg 后缀 strip 对齐勘察域清单表）
- [x] Proof: 单元（registry 镜像测试）+ 集成（悬挂模拟：seed posted=false 源单+POSTED 凭证+PENDING 异常→retry→RETRIED+posted=true；失败隔离变体；REVERSAL 重试不触发守卫）+ 域侧 listener 单测（5 域各一：posted 翻转 + 已 posted 不更新）+ fin/pur/sal/inv/mfg 五域全量绿

Exit Criteria:

- [x] 集成悬挂闭环测试绿 + 五域全量绿

  > 验证（2026-08-27）：`mvn test -pl erp-fin-service,erp-pur-service,erp-sal-service,erp-inv-service,erp-mfg-service,erp-md-service` 全绿——fin 514 / pur 340 / sal 312 / inv 242 / mfg 299 / md 160，BUILD SUCCESS。回归中修复两处与本计划无关的快照漂移：TestErpPurSupplierPriceResolver 三例 VALID_FROM/VALID_TO 日期滚动漂移（`*` 通配，34ab1dcb2 先例）；TestErpMfgWorkOrderEndToEnd#testEndToEndIssueReportCompletion 2_record_work_response.json5 updateTime 跨实体毫秒竞态（`*` 通配）。

### Phase 3 - 收口

Status: completed
Targets: 索引/roadmap/日志
Skill: none

- Item Types: `Proof | Decision`
- Prereqs: Phase 2

- [x] Proof: 全 reactor install + compliance 对照

  > 验证（2026-08-27）：全 reactor `mvn clean install -DskipTests` exit 0；`bash docs/audits/nop-compliance-checker.sh` exit 0（零真实漂移）。

- [x] Fix（owner doc 对齐，审查必改 2）：`docs/design/finance/posting.md §反写契约`——反写方向汇总「正常过账成功」行补两通道派发语义 + 镜像方向二新增 VoucherPostedEvent 契约节
- [x] Decision: fin-001/002/004 → fixed；二期 5 域 listener 登记 Deferred（各归 F2.x 域簇）；sal-003 关联面复核注记；`ErpMdAcctSchemaBizModel#findFirstByOrg` 同型 INACTIVE 泄漏面登记 Deferred（审查注记，非本 finding 范围）
- [x] Decision: 手动 retry 的 post 返回 null 分支（RetryProcessor L52-55 视为成功）**不派发**（F1.1 后 null 近乎死分支；null 无凭证存在性证据，与 doRetry event==null 不派发同语义）
- [x] 独立结束审计

  > 2026-08-27 agent_fc038f56 首轮 PASS（on substance）——A 代码契约（infra 镜像/两通道仅非 null 派发/REVERSAL 与引擎 process() 零派发 grep 证实/beans 注册/四域 dual 接口守卫/fin-002/004 挂点）、B 文档回填（posting.md §悬挂补写/索引 fixed 行/sal-003 注记/Deferred 登记/roadmap done）、C 反模式零命中、D 定向复跑 fin 15/15 + inv 4/4 绿；1 minor = 本计划收口簿记（日志/勾选/状态翻转），已随后完成；2 note = mfg PRODUCTION_VARIANCE 无 postedAt/postedBy 列（代码注记在案）/既有无关 open（P2-CK-fin-013）不构成本计划回归。

## Closure

Status Note: fin-001/002/004 全部修复并经独立结束审计通过；代码、测试、owner doc、索引、roadmap、日志六面证据均已在仓库落地（以下逐项核实）。

Closure Audit Evidence:

- Auditor / Agent: agent_fc038f56（独立子代理，2026-08-27）首轮 PASS——A 代码契约（infra 镜像/两通道仅非 null 派发/REVERSAL 与引擎 process() 零派发/beans 注册/四域 dual 接口守卫/fin-002/004 挂点）、B 文档回填、C 反模式零命中、D 定向复跑 fin 15/15 + inv 4/4 绿；唯一 minor（收口簿记时序）当日补齐
- 代码落地：`module-finance/erp-fin-service` 主代码含 `VoucherPostedEvent` / `IErpFinVoucherPostedListener` / `ErpFinPostedListenerRegistry` / `FinPostedListener`，派发挂点在 `ErpFinDeferredPostingRetryHelper`（doRetry NORMAL 非 null 分支）与 `ErpFinPostingExceptionRetryProcessor`（手动 retry）；fin-002 平衡断言与 fin-004 ACTIVE 过滤按 Phase 1 落地
- 测试落地：`TestErpFinPostedListenerRegistry`（registry 镜像）、`TestPostedListenerSuspensionRecovery`（悬挂闭环 + REVERSAL 不派发守卫）、`TestFinPostedListenerWriteback`、`TestErpFinVoucherBalanceAndSchemaFilter`；验证（2026-08-27）`mvn test -pl erp-fin-service,erp-pur-service,erp-sal-service,erp-inv-service,erp-mfg-service,erp-md-service` 全绿——fin 514 / pur 340 / sal 312 / inv 242 / mfg 299 / md 160，BUILD SUCCESS；全 reactor `mvn clean install -DskipTests` exit 0；`bash docs/audits/nop-compliance-checker.sh` exit 0
- 文档回填：`docs/design/finance/posting.md` L422 新增「悬挂补写：VoucherPostedEvent 契约」节 + L454 反写方向汇总行补两通道派发语义；`docs/audits/check/ai-check-index.md` fin-001/002/004 → fixed、L607 F2.1 done；`docs/backlog/ai-check-roadmap.md` L117 F2.1 → done；`docs/logs/2026/08-26.md` 已追加 F2.1 条目

Follow-up:

- 二期 5 域 listener 归 F2.5/F2.8/F2.9/F2.12/F2.13 各域簇收口（见 Deferred But Adjudicated）
- `ErpMdAcctSchemaBizModel#findFirstByOrg` 同型 INACTIVE 泄漏面 watch-only（md 域簇遇该控制点收口）

## Draft Review Record

- Independent draft review iteration 1: `needs revision → 修订后通过`（agent `agent_b3fa392b`，2026-08-26）——三项设计与挂点全部源码级复核成立（两通道完备/recorder eventData 自愈闭环可行/dual 接口扩展可行/fin-002 挂点依据 owner doc/fin-004 字典闭合+findFirstByOrg 独立不受化简影响/误伤面干净）；2 必改：R1 fin-001 论证机制错误（乐观锁理论被平台共享 session 语义+既有反向双写绿测证伪——改挂反写契约/硬规则6 原子性/F1.1 三真实依据）、R2 Phase 3 缺 posting.md §反写契约更新；2 注记（手动 retry null 分支定夺=不派发/findFirstByOrg Deferred）采纳。修订后 Plan Status: active。

## Closure Gates

- [x] 范围内行为完成
- [x] 五域+md 全量绿 + 全 reactor install + compliance 零真实漂移

  > fin 514 / pur 340 / sal 312 / inv 242 / mfg 299 / md 160（2026-08-27 单轮六模块 BUILD SUCCESS）；全 reactor install exit 0；compliance checker exit 0。

- [x] 索引回填
- [x] roadmap F2.1 → done
- [x] 独立结束审计
- [x] `docs/logs/2026/08-26.md` 追加

## Deferred But Adjudicated

### ErpMdAcctSchemaBizModel#findFirstByOrg 同型 INACTIVE 泄漏

- Classification: watch-only residual
- Why Not Blocking Closure: 独立 statusScore 内存择优（M-6 显式设计），与 resolver/propagator 不同控制点，非 fin-004 范围
- Successor Required: no（md 域簇 F2.x 遇该控制点收口）

### 二期 5 域 listener（assets/hr/projects/maintenance/quality）

- Classification: out-of-scope improvement（一期范围外）
- Why Not Blocking Closure: 专用解码（assetCode+period/SAL-id 内嵌/后缀 strip）集中在此五域；findByCode miss 容忍使分期安全（部分域未上线=优雅降级现状）
- Successor Required: yes（F2.5/F2.8/F2.9/F2.13/F2.12 各域簇内收口）

### listener 编排重放残留（冲抵等）

- Classification: watch-only residual
- Why Not Blocking Closure: posted-only 语义与反向 listener 既有裁决一致（域编排独立于凭证事件）
- Successor Required: no
