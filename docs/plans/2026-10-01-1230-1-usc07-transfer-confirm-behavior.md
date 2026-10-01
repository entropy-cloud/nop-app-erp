# 2026-10-01-1230-1-usc07-transfer-confirm-behavior 核心主干 E2E 缺口补齐（调拨确认行为）

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/user-story-coverage-roadmap.md` USC-07（M4 核心主干验证深化；USC-01 矩阵路由 US-IV-02 缺口）
> Related: `docs/analysis/2026-10-01-0300-1-us-coverage-matrix.md`（§四分流表 #7）、`docs/design/inventory/state-machine.md`（调拨状态机 :178）、`docs/design/inventory/cross-domain.md`（在途规则 :81 + 运行时边界登记）、`tests/e2e/business-actions/inventory-stock-move.action.spec.ts`
> Audit: required（E2E 新增；零生产代码预期——运行时行为边界若需生产变更按缺陷流程另立）

> **执行期事故留痕（round2 审计 B-1 登记）**：2026-10-01 13:14 M-1 整改时 python 脚本变量误用（roadmap 计数块编辑写到了计划路径），本计划文件曾被 roadmap 全文覆盖。经审计方处方指引，从审计记录+执行者会话重写计划本体（非 git——文件未提交过）；重写版 = round-1 审计核验过的正文 + 全部累积修订（B1/M1(a)/m1-m5/iteration 2 accept）+ M-2 勘误注记 + 本留痕。测试/矩阵/日志/roadmap 四面经审计方核验未被事故波及。

## Current Baseline

- **USC-01 矩阵裁定（US-IV-02）**：🔶——设计（调拨状态机+在途规则）与实现（实体含 inTransitWarehouseId + confirm 动作）在位，但「在途可见」无行为级断言（e2e 仅子表 CRUD 写入）→ USC-07 承接。
- **运行时行为边界（2026-10-01 实读，plan 起草前核验；round1 审计独立复核为真）**：`ErpInvTransferOrderBizModel.confirm` → `ErpInvTransferOrderConfirmProcessor`（74 行）：①状态机守卫 DRAFT→CONFIRMED；②config 条件分派跨法人内部交易凭证钩子（fromWarehouseId/toWarehouseId/businessDate 三者非空时；config 默认关但 e2e webServer 全局开 `-Derp-fin.intercompany-posting-enabled=true`——种子女仓同法人 skip 分支，分派不抛错即达）。**不生成任何 StockMove、不写 inTransit 数量、无收发两段 mutation**——`inTransitWarehouseId` 实体字段仅存在于生成实体/API Bean，服务层零消费（grep 实证）。设计文档「在途库存规则」（cross-domain.md :81）为**设计层概念，运行时未实现**。注：StateMachine javadoc「失败吞掉」已被 P2-CK-fin4-008 rethrow 反转（代码为准）。
- **运行时可断言行为（本批真实交付面）**：①confirm 状态翻转 DRAFT→CONFIRMED（浏览器层未断言过——USC-01 缺口本体）；②intercompany posting 分派：三条件齐备时触发（同法人 skip 不抛错即达）——内部交易凭证链已由 fin-domain JUnit（TestErpFinIntercompanyTransfer）覆盖，本批断言分派不抛错即达。
- **建单最小集（ORM :601-609 实证 + inventory.write.spec.ts :148 先例）**：code/orgId/businessDate/fromWarehouseId/toWarehouseId/**docStatus（DRAFT）**/**approveStatus（UNSUBMITTED）**[后两者 mandatory 无默认值——B1 修正] + lines 子表 + inTransitWarehouseId（可选，本批断言字段）。
- **剩余差距**：confirm 行为浏览器层断言缺失；「在途数量运行时追踪未实现」这一设计-运行时差距未显式登记（矩阵称「在途可见无行为级断言」但真实边界是行为不存在——需勘误回写）。

## Goals

- E2E `tests/e2e/business-actions/transfer-confirm.action.spec.ts`（M1(a) 裁决：迁 business-actions 层——confirm 状态翻转+守卫+字段持久化为业务断言，roadmap 归位规则明文「业务断言→business-actions/orchestration」；D5(b) 窄屏网格 residual 整体消失）：①GraphQL 建调拨单（最小集见 Current Baseline）→ `ErpInvTransferOrder__confirm` → docStatus=CONFIRMED 翻转断言（verifyState 范式）→ 非法守卫（CONFIRMED 再 confirm 抛领域码 message token）。②inTransitWarehouseId 赋值场景下 confirm 后字段保持（实体字段持久化断言——不声称在途数量行为）。③cleanup（删行+删头；CONFIRMED 同法人确认无下游产物，安全）。
- 「在途数量运行时追踪未实现」设计-运行时差距勘误回写：USC-01 矩阵 US-IV-02 行 + inventory/cross-domain.md 登记条（运行时仅状态翻转+内部交易钩子；在途数量追踪为设计层概念未实现，successor 触发=多仓在途可见需求立项）。
- 桌面回归不破坏。

## Non-Goals

- 在途数量运行时追踪实现（需后端 generateMove 中间态/收发两段 mutation 设计——产品+架构裁决，具名 successor）。
- 跨法人内部交易凭证数值断言（fin-domain TestErpFinIntercompanyTransfer 已覆盖，引用不重写）。
- 拣货/发货两段 mutation 建设（运行时不存在；归属=Deferred「在途数量运行时追踪」同族 successor——多仓在途可见需求立项时联合裁决，非 USC-05 范围）。

## Task Route

- Type: `verification or audit work`（E2E 新增 + 文档勘误；零生产代码预期）
- Owner Docs: `docs/analysis/2026-10-01-0300-1-us-coverage-matrix.md`（US-IV-02 行勘误）、`docs/design/inventory/cross-domain.md`（在途运行时边界登记）
- Skill Selection Basis: Phase 1 `nop-testing`（E2E 编写）；Phase 2 `none`（文档勘误）。

## Infrastructure And Config Prereqs

- 复用 business-actions 既有范式（GraphQL 建单/mutation/verifyState）；无新基建。

## Execution Plan

### Phase 1 — E2E 行为断言编写与实测

Status: completed
Targets: `tests/e2e/business-actions/transfer-confirm.action.spec.ts`
Skill: nop-testing

- Item Types: `Add | Proof`

- [x] Add: E2E——①GraphQL 建调拨单（最小集沿 inventory.write.spec.ts :148 先例：code/orgId/fromWarehouseId/toWarehouseId/businessDate/docStatus='DRAFT'/approveStatus='UNSUBMITTED'/lines 子表 + inTransitWarehouseId[断言字段]）→ `ErpInvTransferOrder__confirm` → **docStatus**=CONFIRMED 翻转断言（verifyState 范式）→ 非法守卫（CONFIRMED 再 confirm → message token「不允许执行该操作」[领域码 erp.err.inv.illegal-status-transition；extensions 实测无 nop-error-code——执行期实测]）。②inTransitWarehouseId 赋值场景下 confirm 后字段保持（实体字段持久化断言——不声称在途数量行为）。③cleanup：删行+删头（CONFIRMED 同法人确认无下游产物，安全）。**执行期实测勘误**：①状态断言字段为 `docStatus`（非 status——首跑「对象上没有定义字段[status]」错误实证）；②守卫断言用 message token（extensions 实测无 nop-error-code）；③调拨状态字段=docStatus（design state-machine.md :178）。
      Skill: nop-testing
- [x] Proof: negative 全套件 73/73 零新增失败。
      Skill: nop-testing

Exit Criteria:

- [x] E2E 全绿；confirm 行为断言落地（docStatus 翻转+守卫+字段持久化；2/2）。

### Phase 2 — 勘误回写与登记

Status: completed
Targets: 矩阵 US-IV-02 行、inventory/cross-domain.md
Skill: none

- Item Types: `Add`

- [x] Add: 矩阵 US-IV-02 行**追加带日期出处勘误注记**（保留原判文字，遵矩阵 §七「一次性产物」章程）：「[USC-07 勘误 2026-10-01] 原判『在途可见无行为级断言』低估边界——在途数量运行时追踪未实现（confirm 仅状态翻转+内部交易钩子）；confirm 行为断言已补（USC-07），缺口面从测试面转为实现面」+ 判定维持 🔶。
      Skill: none
- [x] Add: inventory/cross-domain.md 在途库存规则小节登记运行时边界（设计概念 vs 运行时实现分界 + successor 触发=多仓在途可见需求立项）。
      Skill: none

Exit Criteria:

- [x] 勘误落盘；USC-01 缺口分流闭环（#7 唯一 A 类承接项已补——测试面已补，实现面具名 successor；USC-07 清单随之闭合）。

## Draft Review Record

- Independent draft review iteration 2（定点清扫）: accept（同一独立子代理 agent_96659f40，明示「B1/M1/M2 均为文本级修订，整改后可直接进入 iteration 2 复审；本轮复审即 iteration 2——B1/M1/M2 修复后由执行者回填本行 accept，无需重做行为核验」）。B1 mandatory 字段补齐/M1(a) 迁 business-actions/M2 消解/m1-m5 全部落盘。
- Independent draft review iteration 1: needs revision（独立子代理 agent_96659f40，1B+2M+5m；运行时行为边界三断言全部实核为真）——B1 建单最小集漏 docStatus/approveStatus 两个 ORM mandatory 无默认值字段；M1 落位 mobile/ 层与 roadmap 归位规则冲突+Goals 未消解 OR；M2 mobile 层无页面可达断言；m1 分派条件漏 businessDate+种子女仓钉死；m2 Non-Goal successor 错挂 USC-05；m3 矩阵勘误改注记方式；m4 缺 cleanup；m5 Closure 注明 USC-07 清单闭合。全部整改落盘（M1(a) 迁 business-actions 层）。

## Closure Gates

- [x] 范围内行为完成（E2E 2/2 + 勘误回写 ×2）
- [x] 相关文档对齐（矩阵 US-IV-02 勘误注记 + cross-domain.md 运行时边界登记 + roadmap USC-07 行回写）
- [x] 已运行验证（transfer-confirm 2/2 + negative 73/73 + mobile 5/5）
- [x] 无范围内项目降级为 deferred/follow-up（在途数量追踪具名 successor）
- [x] 独立草案审查已完成并记录（iteration 1 needs revision 1B+2M+5m → iteration 2 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（日志条目已落盘 docs/logs/2026/10-01.md）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计方独立复跑确认 2 审计项于其审计时保持 [ ] 未预勾）
- [x] 结束证据存在于文件中（本计划 Closure 节 + 矩阵 US-IV-02 勘误注记 + cross-domain.md 运行时边界登记 + docs/logs/2026/10-01.md :9 事故留痕）

## Deferred But Adjudicated

### 在途数量运行时追踪（设计-运行时差距）

- Classification: `successor ownership`
- Why Not Blocking Closure: 需后端行为设计（generateMove 中间态/收发两段 mutation/在途数量账），超出 E2E 补齐批边界；确认状态翻转+内部交易钩子为运行时既有全部行为，已断言。
- Successor Required: `yes`——触发条件：多仓在途可见需求立项（product+architecture 联合裁决）。

## Closure

Status Note: 独立结束审计四轮收敛通过（round1 NEEDS REVISION 2M+1m → round2 增量复审引出 B-1[计划本体被 roadmap 全文覆盖——执行期脚本变量误用事故]→ 从审计记录+会话重写恢复 → round3 正文逐节比对一致+M-2 注记落盘 → round4 行级确认 RESOLVED）。行为面独立复跑全绿（transfer-confirm 2/2 + mobile 5/5 + plan-gates --strict PASS）。USC-07 可标记 done。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session ×4 轮增量复审；round1 NEEDS REVISION 2M+1m → round2 B-1 计划本体覆盖事故 → round3/round4 增量复审收敛）
- Evidence: round1 独立复跑 transfer-confirm.action.spec.ts 2/2 + mobile 5/5 + check-plan-gates --strict PASS（0 new violations/82 baseline）；round2 B-1 实证（计划路径与 roadmap diff -q IDENTICAL，mtime 13:14:35）；round3 正文逐节比对（round-1 核验版+累积修订一致）+ M-2 注记三点落盘（领域码 erp.err.inv.illegal-status-transition=ErpInvErrors.java:54）+ 门控未预勾 + plan-gates --strict 复跑 PASS；round4 日志事故行 :9 行级确认（唯一命中、载体正确、内容与审计记录逐点对应）。事故谱系与恢复路径见 docs/logs/2026/10-01.md :9。

Follow-up:

- （无——残项具名 successor。）
