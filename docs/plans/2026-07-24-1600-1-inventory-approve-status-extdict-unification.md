# 2026-07-24-1600-1-inventory-approve-status-extdict-unification F2 successor — inventory ORM ext:dict 统一到 wf/approve-status

> Plan Status: draft
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

Status: planned
Targets: `module-inventory/model/app-erp-inventory.orm.xml`（1 dict + 5 column ext:dict）、`erp-inv-meta/.../dict/erp-inv/approve-status.dict.yaml`、`ErpInvDaoConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: 无

- [ ] Fix: 删除 module-inventory ORM 中 `<dict name="erp-inv/approve-status">` 元素（L53-58），消除内联 dict 真相源
      - Skill: `nop-backend-dev`
- [ ] Fix: 修改 5 列 `ext:dict="erp-inv/approve-status"` → `ext:dict="wf/approve-status"`
      - L168（ErpInvStockMove.approveStatus）
      - L602（ErpInvTransferReq.approveStatus）
      - L702（ErpInvStockTake.approveStatus）
      - L1227（ErpInvStockAdjust.approveStatus）
      - L1324（ErpInvLandedCost.approveStatus）
      - Skill: `nop-backend-dev`
- [ ] Fix: 删除 `erp-inv-meta/src/main/resources/_vfs/dict/erp-inv/approve-status.dict.yaml`（23 行，codegen 不再生成）
      - Skill: `nop-backend-dev`
- [ ] Fix: `ErpInvDaoConstants extends _ErpInvDaoConstants` → `ErpInvDaoConstants extends _ErpInvDaoConstants, ErpInvDocStatus`（确保 dict 常量移除后 `ErpInvDaoConstants.APPROVE_STATUS_*` 仍可解析）
      - Skill: `nop-backend-dev`
      - 受影响的 2 处 Java 引用（`ErpInvStockMoveProcessor:272`/`TestErpInvConcurrentDeduct:303`）经继承链自动兼容，无需改动

Exit Criteria:

> Phase 1 完成 ORM 变更 + 文件清理 + Java 兼容层

- [ ] ORM 无内联 dict `erp-inv/approve-status`（grep `<dict name="erp-inv/approve-status">`=0）
- [ ] 5 列 ext:dict 已指向 `wf/approve-status`（grep `ext:dict="erp-inv/approve-status"`=0）
- [ ] YAML 文件已删除（`find erp-inv-meta -name approve-status.dict.yaml`=0）
- [ ] `ErpInvDaoConstants` 已 extends `ErpInvDocStatus`（grep `extends _ErpInvDaoConstants, ErpInvDocStatus`=1）

### Phase 2 — Codegen 重新生成 + 全仓库验证

Status: planned
Targets: 全 154 模块 reactor
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 1 完成

- [ ] Proof: `mvn clean install -DskipTests` — 触发 codegen 增量重新生成。验证：
      - `_app.orm.xml` 中内联 dict 已移除（`erp-inv/approve-status` dict 消失）+ 5 列 ext:dict 已更新为 `wf/approve-status`
      - `_ErpInvDaoConstants.java` 中 `APPROVE_STATUS_*` 常量已消失（4 行减少）
      - 全 154 模块 BUILD SUCCESS
      - Skill: `nop-backend-dev`
- [ ] Proof: `mvn test -pl module-inventory` — 库存域测试全绿（含 `TestErpInvConcurrentDeduct:303` 常量引用通过继承链解析）
      - Skill: `nop-backend-dev`
- [ ] Proof: `bash docs/audits/nop-compliance-checker.sh` — 全 16 规则 ≤ 基线无回归（R3=19 不变：`new Erp*()` 构造无变化；R2c 下降：`_ErpInvDaoConstants` 4 常量移除减少 hand-maintained 分布密度；R2b/R2d 不变）
      - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 验证全仓库通过，无回归

- [ ] `mvn clean install -DskipTests` BUILD SUCCESS（154 模块）
- [ ] `mvn test -pl module-inventory` Tests run: ≥114（基线），Failures: 0
- [ ] checker 全 16 规则 ≤ 基线

## Approval & Execution Status

- ORM 保护区域变更：**人工批准**（2026-07-24，会话内确认）
- 执行状态：**deferred** — 计划已拟制、ORM 变更已批准，但暂不执行（包括跳过独立草案审查和结束审计）。触发执行条件：待后续切片纳入 sprint 时激活。

## Draft Review Record

- [独立草案审查跳过 — 按人工指示暂不执行]

## Closure Gates

- [ ] 范围内行为完成（ORM ext:dict 5 列统一 + 内联 dict 移除 + YAML 清理 + Java 兼容）
- [ ] 相关文档对齐（0930-2 §Deferred 项标注 RELEASED 或合并关闭）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-inventory` 全绿 + checker ≤ 基线
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### doc-status 7 域合并候选

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: doc-status 合并需统一 ORM ext:dict 到共享 dict key（属 ask-first 保护区域）。0930-2 已裁决为 Deferred successor，本计划仅覆盖 inventory approve-status 统一。
- Successor Required: `yes`（触发条件：ORM ext:dict 统一授权 + 有统一 doc-status 字典键需求）

## Closure

Status Note: 待执行后填写

Closure Audit Evidence:

- Auditor / Agent: 待执行
- Evidence: 待执行

Follow-up:

- 无