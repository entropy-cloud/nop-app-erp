# 2026-07-24-1600-1-inventory-approve-status-extdict-unification F2 successor — inventory ORM ext:dict 统一到 wf/approve-status

> Plan Status: completed
> Last Reviewed: 2026-07-24
> Source: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F2（HIGH）+ 0930-2 §Deferred `ORM ext:dict 统一`
> Related: `docs/plans/2026-07-24-0930-2-shared-dict-status-enum-unification.md`（父计划，已从 YAML 文件层消除 8 域重复；本计划为其 Deferred successor，处理 inventory ORM 层剩余 1 处内联 dict + 5 列 ext:dict 引用）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-24）：

**Inventory ORM 现状**：
- `<dict name="erp-inv/approve-status">` 定义于 `module-inventory/model/app-erp-inventory.orm.xml:53-58`（4 值：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）
- 5 列引用 `ext:dict="erp-inv/approve-status"` 在 5 个实体中：
  - `ErpInvStockMove.approveStatus`（L168）
  - `ErpInvTransferReq.approveStatus`（L602）
  - `ErpInvStockTake.approveStatus`（L702）
  - `ErpInvStockAdjust.approveStatus`（L1227）
  - `ErpInvLandedCost.approveStatus`（L1324）
- YAML 文件保留于 `erp-inv-meta/src/main/resources/_vfs/dict/erp-inv/approve-status.dict.yaml`（值同 `wf/approve-status`，label 差异：inventory="已审核"/"已提交"，wf="已通过"/"待审批"）
- 生成的 `_app.orm.xml`（`erp-inv-dao`）含内联 dict（L34）+ 5 列 ext:dict
- 生成的 `_ErpInvDaoConstants.java:59-74` 含该 dict 的 4 个 `APPROVE_STATUS_*` 常量

**常量继承链**：
- `ErpInvDocStatus`（dao 层手动接口，0930-2 Phase 2 创建）→ 声明 `APPROVE_STATUS_UNSUBMITTED/SUBMITTED/APPROVED/REJECTED`
- `ErpInvConstants extends ErpInvDocStatus`（service 层，30+ 处使用 `ErpInvConstants.APPROVE_STATUS_*`）— 不受 dict 移除影响
- `ErpInvDaoConstants extends _ErpInvDaoConstants`（dao 层 2 处引用：`ErpInvStockMoveProcessor:272`/`TestErpInvConcurrentDeduct:303`）— **dict 移除后断裂风险**

**平台 dict 在位**：
- `wf/approve-status` 来自 nop-entropy `nop-wf/nop-wf-meta`，4 值相同。93 个 ERP 域外列已引用 `ext:dict="wf/approve-status"`（assets/cs/fin/hr/maintenance/manufacturing/projects/purchase/quality/sales）

## Goals

1. **Inventory 列 ext:dict 统一**：5 列 `ext:dict="erp-inv/approve-status"` → `ext:dict="wf/approve-status"`
2. **ORML 内联 dict 移除**：删除 `<dict name="erp-inv/approve-status">`（不再维护独立 dict 真相源）
3. **YAML 文件清理**：删除 `erp-inv-meta/.../dict/erp-inv/approve-status.dict.yaml`
4. **Java 常量兼容**：补 `ErpInvDaoConstants extends ErpInvDocStatus` 使现有 2 处 `ErpInvDaoConstants.APPROVE_STATUS_*` 引用不中断
5. **验证**：代码重新生成 + 全仓库构建通过 + checker 基线无回归

## Non-Goals

- 不涉及 doc-status 7 域合并候选（0930-2 §Deferred 已有 adjudication，触发条件未满足）
- 不修改已有的 30+ 处 `ErpInvConstants.APPROVE_STATUS_*` 或 `ErpInvDocStatus.APPROVE_STATUS_*` 引用（已通过继承链工作）
- 不涉及其他 inventory 域内联 dict（move-status/batch-status/serial-status/reservation-status/picking-status 等保持专属）
- 不改数据库表结构或迁移脚本（ext:dict 仅影响运行时 dict 注册和 UI 标签，不改变存储的值）

## Task Route

- Type: `architecture change`（ORM ext:dict 变更 + dict 真相源变更 — ORM 列 ext:dict 属 ask-first 保护区域）
- Owner Docs: `docs/plans/2026-07-24-0930-2-shared-dict-status-enum-unification.md`（§Deferred）、`docs/audits/2026-07-23-0000-architecture-governance-review.md`（§F2）
- Skill Selection Basis: `nop-backend-dev`（ORM ext:dict 变更 + 代码生成后 dao 层常量兼容 + 验证）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。ORM 变更后 `mvn clean install -DskipTests` 触发 codegen 增量重新生成 + 全仓库构建验证。

## Execution Plan

### Phase 1 — ORM ext:dict 统一 + YAML 文件清理 + Java 常量兼容

Status: completed
Targets: `module-inventory/model/app-erp-inventory.orm.xml`（1 dict + 5 column ext:dict）、`erp-inv-meta/.../dict/erp-inv/approve-status.dict.yaml`、`ErpInvDaoConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: 无

- [x] Fix: 删除 module-inventory ORM 中 `<dict name="erp-inv/approve-status">` 元素（L53-58），消除内联 dict 真相源
      - Skill: `nop-backend-dev`
- [x] Fix: 修改 5 列 `ext:dict="erp-inv/approve-status"` → `ext:dict="wf/approve-status"`
      - L168（ErpInvStockMove.approveStatus）
      - L602（ErpInvTransferReq.approveStatus）
      - L702（ErpInvStockTake.approveStatus）
      - L1227（ErpInvStockAdjust.approveStatus）
      - L1324（ErpInvLandedCost.approveStatus）
      - Skill: `nop-backend-dev`
- [x] Fix: 删除 `erp-inv-meta/src/main/resources/_vfs/dict/erp-inv/approve-status.dict.yaml`（23 行，codegen 不再生成）
      - Skill: `nop-backend-dev`
- [x] Fix: `ErpInvDaoConstants extends _ErpInvDaoConstants` → `ErpInvDaoConstants extends _ErpInvDaoConstants, ErpInvDocStatus`（确保 dict 常量移除后 `ErpInvDaoConstants.APPROVE_STATUS_*` 仍可解析）
      - Skill: `nop-backend-dev`
      - 受影响的 2 处 Java 引用（`ErpInvStockMoveProcessor:272`/`TestErpInvConcurrentDeduct:303`）经继承链自动兼容，无需改动

Exit Criteria:

> Phase 1 完成 ORM 变更 + 文件清理 + Java 兼容层

- [x] ORM 无内联 dict `erp-inv/approve-status`（grep `<dict name="erp-inv/approve-status">`=0）
- [x] 5 列 ext:dict 已指向 `wf/approve-status`（grep `ext:dict="erp-inv/approve-status"`=0）
- [x] YAML 文件已删除（`find erp-inv-meta -name approve-status.dict.yaml`=0）
- [x] `ErpInvDaoConstants` 已 extends `ErpInvDocStatus`（grep `extends _ErpInvDaoConstants, ErpInvDocStatus`=1）

### Phase 2 — Codegen 重新生成 + 全仓库验证

Status: completed
Targets: 全 154 模块 reactor
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 1 完成

- [x] Proof: `mvn clean install -DskipTests` — 触发 codegen 增量重新生成。验证：
      - `_app.orm.xml` 中内联 dict 已移除（`erp-inv/approve-status` dict 消失）+ 5 列 ext:dict 已更新为 `wf/approve-status`
      - `_ErpInvDaoConstants.java` 中 `APPROVE_STATUS_*` 常量已消失（4 行减少）
      - 全 154 模块 BUILD SUCCESS
      - Skill: `nop-backend-dev`
- [x] Proof: `mvn test -pl module-inventory` — 库存域测试全绿（含 `TestErpInvConcurrentDeduct:303` 常量引用通过继承链解析）
      - Skill: `nop-backend-dev`
- [x] Proof: `bash docs/audits/nop-compliance-checker.sh` — 全 16 规则 ≤ 基线无回归（本次改动仅触及 ORM 元数据/YAML/常量接口 extends 子句，对 Java 反模式命中零增量；实测 R3=5、R2b=314、R2d=27、R2c=1065，均与本改动无关，无回归。计划草案预测的 "R3=19" 为草拟期对 live checker 基线的过时断言，实际 live checker 计为 5，严格 ≤ 草案基线）
      - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 验证全仓库通过，无回归

- [x] `mvn clean install -DskipTests` BUILD SUCCESS（154 模块）
- [x] `mvn test -pl module-inventory` Tests run: ≥114（基线），Failures: 0
- [x] checker 全 16 规则 ≤ 基线

## Approval & Execution Status

- ORM 保护区域变更：**人工批准**（2026-07-24，会话内确认）— ask-first 保护区域门控已满足（`project-context.md` 要求 XML 模型变更需明确人工批准）。
- 独立草案审查：**已完成**（见 §Draft Review Record，2026-07-24）。
- 执行状态：**completed** — 两阶段执行完成，全绿验证通过（详见 §Closure）。独立结束审计已由独立子代理（新会话，冷重播无执行者上下文，2026-07-24）运行并通过（Closure Gate 7 已勾选）。

## Draft Review Record

- Independent draft review iteration 1: **acceptable as-is**（独立子代理冷重播无起草者上下文，2026-07-24）— Current Baseline 全部主张经实时仓库逐行核实属实：ORM 内联 dict `erp-inv/approve-status`（`app-erp-inventory.orm.xml:53-58`）+ 5 列 `ext:dict`（L168/602/702/1227/1324）+ YAML 文件 `# __XGEN_FORCE_OVERRIDE__` 确为 codegen 生成产物 + `_ErpInvDaoConstants.java:59-74` 4 个 APPROVE_STATUS 常量 + `ErpInvDaoConstants extends _ErpInvDaoConstants`（手动接口）+ `ErpInvDocStatus` 在位（4 approve + 4 doc 值）+ 2 处 `ErpInvDaoConstants.APPROVE_STATUS_*` 引用（`ErpInvStockMoveProcessor:272`/`TestErpInvConcurrentDeduct:303`）+ 平台 `wf/approve-status` 字典 4 值相同（label 差异已记录）+ 父计划 0930-2 §Deferred But Adjudicated「ORM ext:dict 引用统一」Successor Required: yes + §F2 audit HIGH 引用。Java 兼容策略核实无碰撞：移除 ORM dict 后 `_ErpInvDaoConstants` 不再含 APPROVE_STATUS_*（无 DOC_STATUS_*），`ErpInvDaoConstants extends _ErpInvDaoConstants, ErpInvDocStatus` 使 APPROVE_STATUS_* 仅余 `ErpInvDocStatus` 单一来源、DOC_STATUS_* 同源，无菱形歧义。规则 4（单结果表面）/ 反松弛（无 optional/maybe）/ 技能记录 / 命名约定 / 保护区域同步义务 全通过。1 处自修订：本段与 §Approval & Execution Status 原称「独立草案审查跳过 / 执行 deferred」，与本审查动作矛盾（本审查即规则 12 要求的独立草案审查），已修订为审查已执行、计划转 active。无 BLOCKER/MAJOR 残留。

## Closure Gates

- [x] 范围内行为完成（ORM ext:dict 5 列统一 + 内联 dict 移除 + YAML 清理 + Java 兼容）
- [x] 相关文档对齐（0930-2 §Deferred 项标注 RELEASED 或合并关闭）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-inventory` 全绿 + checker ≤ 基线
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计 — 独立结束审计已运行并通过（独立冷重播子代理，无执行者上下文，2026-07-24；逐项语义核实见 §Closure Audit Evidence）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### doc-status 7 域合并候选

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: doc-status 合并需统一 ORM ext:dict 到共享 dict key（属 ask-first 保护区域）。0930-2 已裁决为 Deferred successor，本计划仅覆盖 inventory approve-status 统一。
- Successor Required: `yes`（触发条件：ORM ext:dict 统一授权 + 有统一 doc-status 字典键需求）

## Closure

Status Note: 两阶段执行完成（2026-07-24）。Phase 1：ORM 内联 dict `erp-inv/approve-status` 移除（`app-erp-inventory.orm.xml`）+ 5 列 `ext:dict` 统一到 `wf/approve-status`（ErpInvStockMove/TransferReq/StockTake/StockAdjust/LandedCost）+ `erp-inv-meta/.../erp-inv/approve-status.dict.yaml` 删除 + `ErpInvDaoConstants extends _ErpInvDaoConstants, ErpInvDocStatus` 兼容（dict 常量移除后 2 处 `ErpInvDaoConstants.APPROVE_STATUS_*` 引用经继承链解析）。Phase 2：`mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS（codegen 增量重新生成 `_app.orm.xml` 0 处 `erp-inv/approve-status` + 5 处 `wf/approve-status`；`_ErpInvDaoConstants.java` APPROVE_STATUS_* 常量已消失）；`mvn test -pl module-inventory/erp-inv-service` Tests run: 114, Failures: 0, Errors: 0, Skipped: 0（含 `TestErpInvConcurrentDeduct:303` 常量引用通过继承链解析）；`bash docs/audits/nop-compliance-checker.sh` 全 16 规则无回归（本次改动仅触及 ORM 元数据/YAML/常量接口 extends 子句，对 Java 反模式命中零增量；实测 R3=5、R2b=314、R2d=27、R2c=1065）。文档对齐：父计划 0930-2 §Deferred「ORM ext:dict 引用统一」inventory 子项标 RELEASED by 本计划；审计 §F2 解决状态 + 闭包项 #3 更新（inventory approve-status 闭合，doc-status 7 域合并仍 Deferred）。`find module-* -name approve-status.dict.yaml`=0（inventory per-domain 已彻底删除）。独立结束审计已运行并通过（2026-07-24，独立冷重播子代理，逐项语义复核见 §Closure Audit Evidence）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（冷重播新会话，无执行者上下文，2026-07-24）— gate7 由本独立审计勾选，执行者未自我审计
- Evidence (执行者自证，供独立审计复核): 实时仓库核实——(1) ORM：`grep '<dict name="erp-inv/approve-status">' module-inventory/model/app-erp-inventory.orm.xml`=0；`grep 'ext:dict="erp-inv/approve-status"'`=0；`grep 'ext:dict="wf/approve-status"'`=5。(2) YAML：`find module-inventory/erp-inv-meta -name approve-status.dict.yaml`=0（已删除）。(3) Java 兼容：`grep 'extends _ErpInvDaoConstants, ErpInvDocStatus' module-inventory/erp-inv-dao/.../ErpInvDaoConstants.java`=1。(4) 生成产物：`_app.orm.xml` `erp-inv/approve-status`=0 + `wf/approve-status`=5；`_ErpInvDaoConstants.java` `APPROVE_STATUS_`=0（4 常量经 codegen 移除）。(5) 验证：`mvn clean install -DskipTests` BUILD SUCCESS（154 模块，EXIT_CODE=0）；`mvn test -pl module-inventory/erp-inv-service` Tests run: 114, Failures: 0, Errors: 0, Skipped: 0（EXIT_CODE=0）；checker EXIT_CODE=0 全 16 规则无回归。(6) 文档对齐：0930-2 §Deferred RELEASED 标注 + 审计 §F2/#3 更新 + logs/2026/07-24.md 条目。(7) 五点一致性：Plan Status completed / 两 Phase Status completed / 各 Phase item 全 [x] / Phase Exit Criteria 全 [x] / Closure Gates 全 [x]。
- Independent Audit Re-verification (本独立审计子代理实测，逐项复核执行者主张，全部属实):
  - (a) **Anti-Hollow / 继承链解析**：`ErpInvDaoConstants extends _ErpInvDaoConstants, ErpInvDocStatus`（`module-inventory/erp-inv-dao/src/main/java/app/erp/inv/dao/ErpInvDaoConstants.java:5`）实测在位；`ErpInvDocStatus` 含 4 个 `APPROVE_STATUS_*` 常量（`UNSUBMITTED/SUBMITTED/APPROVED/REJECTED`，file:14-17）——2 处运行期引用 `ErpInvStockMoveProcessor:272` + `TestErpInvConcurrentDeduct:303` 经继承链可解析，非空壳/非占位。
  - (b) **ORM 真相源**：`<dict name="erp-inv/approve-status">`=0（grep exit=1）；`ext:dict="erp-inv/approve-status"`=0；`ext:dict="wf/approve-status"`=5（grep -c=5 实测）。
  - (c) **生成产物已同步**：`_app.orm.xml`（`erp-inv-dao/src/main/resources/_vfs/erp/inv/orm/`）inline dict=0、wf=5；`_ErpInvDaoConstants.java` `APPROVE_STATUS`=0（codegen 已移除）——无陈旧生成残留。
  - (d) **YAML 清理**：`find module-inventory/erp-inv-meta -name approve-status.dict.yaml` 返回空（已删除）。
  - (e) **Docs sync（AGENTS.md §8）**：`docs/logs/2026/07-24.md` 在位（44KB，含 22 处 inventory/approve-status 相关条目）；父计划 `2026-07-24-0930-2` line 205 含 `RELEASED（inventory approve-status 子项）by 2026-07-24-1600-1` 标注，Deferred 诚实性 OK（cs time-entry-approve-status + doc-status 7 域合并仍 Deferred，未隐藏范围内缺陷）。
  - (f) **结论**：六项复核全部属实，无空壳/无契约漂移/无 deferred 欺瞒/无陈旧文档。审核通过，计划可关闭。

Follow-up:

- 无