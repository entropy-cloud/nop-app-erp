# 2026-07-26-0300-1 doc-status 共享字典统一（6 域 ext:dict + logistics stale 清理）

> Plan Status: draft
> **Review Hold: pending ORM protection-zone human approval** — 本计划修改 6 域 28 列 ORM `ext:dict` 属性值 + 删除 logistics 内联 ORM `<dict>` 定义，触及 `model/*.orm.xml` ask-first 保护区域（`project-context.md:69`）。inventory 先例（1600-1）经会话内人工批准授权；本计划尚未获得同类授权。在人工批准（路径 a：会话内确认 + 增 §Approval & Execution Status；路径 b：拆分 ORM 变更为独立人工门控后续计划）前保持 `draft`，不提升为 `active`。
> Last Reviewed: 2026-07-26
> Source: `docs/plans/2026-07-24-0930-2-shared-dict-status-enum-unification.md` §Deferred But Adjudicated「ORM ext:dict 引用统一（approve-status/doc-status 共享 dict key）」doc-status 子项（Successor Required: yes；触发条件：ORM 变更授权 + 强制统一 dict key 引用需求）+ `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F2（HIGH）闭包项 #3（🔶 部分 Done，inventory approve-status 子项已闭合，doc-status 7 域合并仍 Deferred）
> Related: `docs/plans/2026-07-24-1600-1-inventory-approve-status-extdict-unification.md`（inventory approve-status 统一先例——同型 ext:dict 迁移 + Java 常量兼容 + per-domain YAML 删除）、`docs/plans/2026-07-24-0930-2-shared-dict-status-enum-unification.md`（F2 字典真相碎裂 parent plan）
> Audit: required

## Current Baseline

F2 审计（HIGH）识别字典与状态枚举真相碎裂。`2026-07-24-0930-2` 完成 approve-status 8 份冗余 per-domain YAML 去重 + D1 `Erp*DocStatus` 接口全域 9 域推广。`2026-07-24-1600-1` 完成 inventory approve-status ORM ext:dict 统一（5 列 `erp-inv/approve-status` → `wf/approve-status`）。**doc-status 7 域合并仍 Deferred**。

实时仓库核实（2026-07-26）：

**6 域 doc-status ORM ext:dict 消费者（28 列，值集合全同 DRAFT/ACTIVE/CANCELLED）**：

| 域 | 列数 | ext:dict 值 |
|----|------|-------------|
| purchase | 8 | `erp-pur/doc-status` |
| sales | 7 | `erp-sal/doc-status` |
| assets | 6 | `erp-ast/doc-status` |
| quality | 4 | `erp-qa/doc-status` |
| maintenance | 2 | `erp-mnt/doc-status` |
| cs | 1 | `erp-cs/doc-status` |

6 份 per-domain `doc-status.dict.yaml`（`erp-{pur,sal,ast,qa,mnt,cs}/doc-status.dict.yaml`）经逐文件 `diff` 核实**字节级完全相同**（DRAFT/ACTIVE/CANCELLED 三值 + zh-CN label 草稿/已生效/已作废）。

**logistics 域 stale 残留**：`module-logistics/model/app-erp-logistics.orm.xml:58` 含内联 `<dict label="单据状态" name="erp-log/doc-status" valueType="string">` 定义 + `module-logistics/erp-log-meta/.../erp-log/doc-status.dict.yaml` 物理文件，但**全域 0 列** `ext:dict="erp-log/doc-status"` 消费者——dict 定义与 YAML 文件均为 stale，可安全删除（镜像 0930-2 approve-status 8 份零消费者 YAML 删除范式）。

**cs time-entry-approve-status（1 处特化保留，不在本计划范围）**：`erp-cs/time-entry-approve-status` 值集合 PENDING/APPROVED/REJECTED ≠ `wf/approve-status` UNSUBMITTED/SUBMITTED/APPROVED/REJECTED——值集合不同，为合法特化，0930-2 §Deferred 正确标「特化保留」。本计划 **Non-Goal** 不统一此项。

**Java 常量层**：6 域 `Erp*DocStatus` 接口（`module-*/erp-*-dao/.../dao/constants/Erp*DocStatus.java`）各自声明 `DOC_STATUS_DRAFT="DRAFT"` / `DOC_STATUS_ACTIVE="ACTIVE"` / `DOC_STATUS_CANCELLED="CANCELLED"` 常量——值全同，接口名按域隔离。这些常量接口在 0930-2 Phase 2 创建用于「服务层字面量→常量引用」收敛（0605-2 已完成全域替换），**不因 dict key 统一而需要变更**（常量持有的是值，不是 dict key；`Erp*Constants extends Erp*DocStatus` 兼容链不引用 dict key）。

**无平台标准 doc-status dict**：`../nop-entropy/` 经 `find` 核实无 `doc-status.dict.yaml`（仅有 `wf/approve-status` 等审批轴字典）。doc-status（DRAFT/ACTIVE/CANCELLED 单据生命周期轴）为应用层语义，需创建项目级共享 dict。

剩余差距：6 域 28 列 ext:dict 引用碎片化 + 6 份字节级重复 per-domain YAML + logistics stale 残留（1 内联 ORM dict + 1 YAML）；F2 闭包项 #3 doc-status 段仍 🔶。

## Goals

- 创建项目级共享 doc-status dict（`erp/doc-status`，值 DRAFT/ACTIVE/CANCELLED），消除 6 份字节级重复 per-domain YAML
- 6 域 28 列 ORM `ext:dict="erp-<domain>/doc-status"` → `ext:dict="erp/doc-status"` 统一引用
- logistics stale 残留清理（内联 ORM `<dict>` 定义 + per-domain YAML 删除）
- F2 闭包项 #3 从 🔶 → ✅（doc-status 子项 RELEASED + cs time-entry-approve-status 重分类为永久裁决特化 Successor: no）

## Non-Goals

- **不统一 cs `time-entry-approve-status`**——值集合不同（PENDING/APPROVED/REJECTED ≠ wf/approve-status），为合法特化
- **不修改 Java `Erp*DocStatus` 常量接口**——常量持有值非 dict key；dict key 统一不影响 Java 常量层
- **不改变 doc-status 值集合**——DRAFT/ACTIVE/CANCELLED 三值不变，仅统一 dict key 引用
- **不触及 finance/manufacturing/inventory 的域特有 status dict**（如 erp-fin/voucher-status、erp-mfg/work-order-status 等）——这些值集合不同，非合并候选
- **不触及 crm `erp-crm/lead-doc-status`**——crm 域使用 `erp-crm/lead-doc-status`（`module-crm/model/app-erp-crm.orm.xml:37,221`），为线索特有状态轴（非通用 doc-status 语义），非合并候选
- **不做 view.xml / page.yaml / xmeta 层引用变更**——`ext:dict` 变更经 codegen 增量传播，手写层无 dict key 直接引用
- **不做 Timesheet 周网格 / Barcode PDA**（frontend-ui-roadmap P3 defer / Non-Goal 项目 2.x）

## Task Route

- Type: `architecture change`（新建跨域共享 dict `erp/doc-status` 作为新共享真相源 + 6 域 ORM `ext:dict` 属性值统一变更 + codegen 增量重新生成；同型先例 1600-1 Task Route 亦标 `architecture change`——ext:dict 跨域统一引入共享 dict 属架构层变更非纯实现）
- Owner Docs: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F2（HIGH 审计发现）、`docs/design/domain-design-guidelines.md` §字典设计（`<dict name="erp-<short>/kebab-name">` 命名规范——共享 dict 为 `erp/doc-status` 跨域引用例外）、`docs/audits/compliance-baseline.md`（R3 基线同步）
- Skill Selection Basis: `nop-backend-dev` 匹配 ORM ext:dict 变更 + codegen 增量重新生成 + Java 常量兼容链（与 1600-1 同型 ext:dict 统一工作经该技能路由）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 回滚策略：单次 `git revert` 可回滚（ORM ext:dict 变更 + YAML 增删在同一提交）

## Execution Plan

### Phase 1 — 共享 dict 选址 + logistics stale 清理

Status: planned
Targets: `app-erp-all/src/main/resources/_vfs/dict/erp/doc-status.dict.yaml`（候选 A）或等价路径、`module-logistics/model/app-erp-logistics.orm.xml`（内联 dict 删除）、`module-logistics/erp-log-meta/src/main/resources/_vfs/dict/erp-log/doc-status.dict.yaml`（删除）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Fix | Proof`
- Prereqs: 无

- [ ] `Decision`：共享 doc-status dict 文件选址裁决。候选：
      - (a) `app-erp-all/src/main/resources/_vfs/dict/erp/doc-status.dict.yaml`——聚合 app 模块，runtime VFS 合并可达；注意 `control.xlib` 先例位于 `_vfs/erp/xlib/`（不同 VFS 子树），dict 候选位于 `_vfs/dict/erp/`——先例证明 app-erp-all resources 在 runtime 可达，但单模块测试 classpath 可达性须 Phase 1 Proof 实测确认（非先例直接覆盖）
      - (b) 新建 `module-common-meta` 模块承载共享 dict——最干净但增 Maven 模块构建复杂度
      - (c) 复用 `module-common-service` 的 resources 目录——非 meta 模块语义，但 `_vfs` 合并不限模块类型
      - 推荐裁决倾向实测验证后确定：先验证 (a) 单模块测试可达性（`mvn test -pl module-purchase/erp-pur-service -am` 是否能解析 `_vfs/dict/erp/doc-status.dict.yaml`）；若可达选 (a)，若不可达评估 (c) 或 (b)
      - 考虑的替代方案：保持 per-domain（零变更），但 F2 HIGH 审计发现要求收口
      - 残留风险：(a) 若 app-erp-all 不在单模块测试 classpath，dict 解析失败导致 view 渲染异常——Phase 1 Proof 须实测确认
      - Skill: `nop-backend-dev`
- [ ] `Add`：按 Phase 1 Decision 选址创建 `doc-status.dict.yaml`（内容 = 6 份 per-domain 字节级相同的 DRAFT/ACTIVE/CANCELLED 三值 + zh-CN label 草稿/已生效/已作废 + `__XGEN_FORCE_OVERRIDE__` 头）
      - Skill: `nop-backend-dev`
- [ ] `Fix`：logistics stale 残留清理——删除 `module-logistics/model/app-erp-logistics.orm.xml:58` 内联 `<dict label="单据状态" name="erp-log/doc-status" ...>` 定义（0 列消费者）+ 删除 `module-logistics/erp-log-meta/.../erp-log/doc-status.dict.yaml` 物理文件
      - Skill: `nop-backend-dev`
- [ ] `Proof`：共享 dict 选址可达性验证——`mvn clean install -DskipTests`（codegen 增量重新生成 + 154 模块 BUILD SUCCESS）+ 抽样 `mvn test -pl module-purchase/erp-pur-service -am`（单模块测试 dict 解析可达）
      - Skill: none

Exit Criteria:

- [ ] 共享 dict 文件存在且 well-formed YAML（DRAFT/ACTIVE/CANCELLED 三值）
- [ ] logistics stale 内联 ORM dict + YAML 已删除（`grep "erp-log/doc-status" module-logistics/` 返回 0）
- [ ] 共享 dict 在单模块测试路径可达（`mvn test -pl module-purchase/erp-pur-service -am` 0 failures）

### Phase 2 — 6 域 ORM ext:dict 统一 + per-domain YAML 删除

Status: planned
Targets: 6 域 ORM 源模型（`module-{purchase,sales,assets,quality,maintenance,cs}/model/app-erp-*.orm.xml`，28 列 `ext:dict` 属性值变更）、6 份 per-domain `doc-status.dict.yaml` 删除
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Item Types Note: Phase 2 is Fix-heavy（ext:dict 属性值统一 + 冗余文件删除）
- Prereqs: Phase 1 共享 dict 选址确认 + 可达性验证通过

- [ ] `Fix`：逐域修改 ORM 源模型 `ext:dict` 属性值 `erp-<domain>/doc-status` → `erp/doc-status`（6 域 28 列）：
      - purchase 8 列（`module-purchase/model/app-erp-purchase.orm.xml`）
      - sales 7 列（`module-sales/model/app-erp-sales.orm.xml`）
      - assets 6 列（`module-assets/model/app-erp-assets.orm.xml`）
      - quality 4 列（`module-quality/model/app-erp-quality.orm.xml`）
      - maintenance 2 列（`module-maintenance/model/app-erp-maintenance.orm.xml`）
      - cs 1 列（`module-cs/model/app-erp-cs.orm.xml`）
      - Skill: `nop-backend-dev`
- [ ] `Fix`：删除 6 份字节级重复的 per-domain `doc-status.dict.yaml`：
      - `module-{purchase,sales,assets,quality,maintenance,cs}/erp-*-meta/src/main/resources/_vfs/dict/erp-*/doc-status.dict.yaml`
      - Skill: none
- [ ] `Proof`：`mvn clean install -DskipTests` 触发 codegen 增量重新生成，验证：
      - 6 域 `_app.orm.xml` 中 `ext:dict` 全部为 `erp/doc-status`（`grep "erp-\\*/doc-status" module-*/model/_app.orm.xml` 仅匹配 `erp/doc-status`）
      - 6 域 `_Erp*DaoConstants.java` 无变化（常量持有值非 dict key）
      - 154 模块 BUILD SUCCESS
      - Skill: none

Exit Criteria:

- [ ] 6 域 ORM 28 列 `ext:dict` 统一为 `erp/doc-status`（`grep 'ext:dict="erp-[a-z]*/doc-status"' module-*/model/*.orm.xml` 仅匹配 `erp/doc-status`）
- [ ] 6 份 per-domain doc-status.dict.yaml 已删除（`find module-* -name "doc-status.dict.yaml" -path "*/src/*"` 返回 0）
- [ ] `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS

### Phase 3 — 回归验证 + 文档对齐 + 基线同步

Status: planned
Targets: 受影响域 `mvn test`、`docs/audits/2026-07-23-0000-architecture-governance-review.md`（F2 闭包项 #3 更新）、`docs/audits/compliance-baseline.md`（R3 基线同步）、`docs/plans/2026-07-24-0930-2-shared-dict-status-enum-unification.md`（Deferred RELEASED 标注）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 2 完成

- [ ] `Proof`：受影响 6 域 + logistics 单模块测试全绿（`mvn test -pl module-{purchase,sales,assets,quality,maintenance,cs}/erp-*-service` 0 failures）
      - Skill: none
- [ ] `Proof`：合规检查器零回归（`bash docs/audits/nop-compliance-checker.sh` 全 16 规则 actual ≤ baseline）
      - Skill: none
- [ ] `Add`：F2 闭包项 #3 更新——`docs/audits/2026-07-23-0000-architecture-governance-review.md` 闭包项 #3 标 ✅：doc-status 子项标 RELEASED by 本计划 + cs `time-entry-approve-status` 重分类为「永久裁决特化（Successor: no）」（值集合 PENDING/APPROVED/REJECTED ≠ wf/approve-status，合法特化非冗余）；`docs/plans/2026-07-24-0930-2` §Deferred But Adjudicated「ORM ext:dict 引用统一」doc-status 子项标 RELEASED + cs 子项标「永久裁决特化」
      - Skill: none
- [ ] `Add`：`docs/audits/compliance-baseline.md` R3 同步注记（若 checker 实测 R3 计数变化——预期不变：ext:dict 变更不触发 `new Erp*()` 构造）
      - Skill: none

Exit Criteria:

- [ ] 6 域 + logistics 单模块测试 0 failures
- [ ] 合规检查器全 16 规则 ≤ baseline
- [ ] F2 闭包项 #3 标 ✅（doc-status RELEASED + cs time-entry-approve-status 重分类为永久裁决特化）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（`ses_06557f33effeS6sDBLr7nVWonK`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-26）— 1 Blocker / 1 Major / 3 Minor。全部 load-bearing 事实主张经实时仓库逐项核实**精确匹配**（6 域 28 列 ext:dict ✓ / 6 份 per-domain YAML 字节级相同 md5=7f607bfc... ✓ / logistics 0 列消费者 ✓ / cs time-entry-approve-status 值集合不同 ✓ / 无平台 doc-status dict ✓ / 6 Erp*DocStatus 接口在位 ✓）。**Blocker**：ORM 保护区域授权未满足——计划修改 6 域 28 列 ext:dict 触及 ask-first 保护区域，inventory 先例（1600-1）经会话内人工批准，本计划尚未获得同类授权；**已修订**：增 `Review Hold: pending ORM protection-zone human approval` 标记，明示在人工批准前保持 `draft` 不提升为 `active`（解阻路径 a/b 见 front matter）。**Major**：F2 闭包项 #3 → ✅ 声称过强——cs time-entry-approve-status 为 #3 bundled 子项且仍 Deferred；**已修订**：Phase 3 增 cs time-entry-approve-status 重分类步骤（永久裁决特化 Successor: no）+ Goals/Exit Criteria 软化措辞。**Minor**：(1) Task Route Type `implementation-only` → `architecture change` 对齐 1600-1 同型先例 ✓；(2) Non-Goals 增 crm/lead-doc-status 排除 ✓；(3) Phase 1 Decision 候选 (a) 先例措辞修正（control.xlib 位于不同 VFS 子树，先例仅证 runtime 可达非单模块测试可达）✓。
- Independent draft review iteration 2: `held as draft (Blocker unresolvable at review time)`（mission-driver review，2026-07-26）— 0 新 Blocker / 0 新 Major / 1 Minor。重核 load-bearing 事实精确匹配：6 域 ext:dict 消费者列数 8/7/6/4/2/1=28 ✓（逐域 grep）；logistics `erp-log/doc-status` 0 列消费者 ✓（匹配项均为内联 dict 定义 + 生成态 _app.orm.xml + i18n label，非 ext:dict 消费）；无平台 `doc-status.dict.yaml` ✓。格式合规（全部必需段 + 字段名 + Phase 结构有效）、范围清晰（单一结果面、stale 清理合理 bundle、cs 特化正确 Non-Goal）、结束证据占位就位。**Blocker 确认**：ORM `model/*.orm.xml` ask-first 保护区域（`project-context.md:69`）需人工批准，AI 审查者无权授予——解阻依赖路径 a（会话内人工确认 + 增 §Approval & Execution Status）或路径 b（拆分 ORM 变更为独立人工门控后续计划）。计划已正确置 `Review Hold` 且保持 `draft`，不提升为 `active`。**Minor（保留给结束审计）**：Phase 2 Exit Criteria 含全仓 `mvn clean install`（按指南执行时规则 7 属 Closure Gates），但因生成态 `_app.orm.xml` 须先经 codegen 才能供 Phase 3 grep，本地化合理性成立——结束审计确认即可。
- Independent draft review iteration 3: `held as draft (Blocker unresolvable at review time)`（mission-driver re-review，2026-07-26）— 0 新 Blocker / 0 新 Major / 0 新 Minor。实时仓库复核全部 load-bearing 事实精确匹配：6 域 ext:dict 消费者 `rg -c 'ext:dict="erp-[a-z]+/doc-status"'` 逐域输出 8/7/6/4/2/1=28 ✓；6 份 per-domain `doc-status.dict.yaml` + 1 logistics stale YAML 物理存在 ✓；logistics `app-erp-logistics.orm.xml:58` 内联 `<dict name="erp-log/doc-status">` 在位 + 0 ext:dict 消费者 ✓；`nop-entropy/` 无 `doc-status.dict.yaml` ✓；crm `lead-doc-status.dict.yaml` 正确排除（线索特有轴，非通用 doc-status）。格式合规（必需段齐全 / 字段名正确 / Phase 结构有效 / Item Types 标注合规 / Skill 显式记录）、范围单一结果面无 scope creep、Exit Criteria 可测、Closure Gates 与 Closure Audit Evidence 占位就位。**Blocker 确认（同 iteration 2）**：ORM `model/*.orm.xml` ask-first 保护区域需人工批准，AI 审查者无权授予——计划已正确置 `Review Hold` 保持 `draft`，不提升为 `active`。**新发现 Minor（保留给结束审计）**：logistics 域 `_erp-log.i18n.yaml:210,237` 含 `erp-log/doc-status` i18n label 条目，Phase 1 stale 清理仅列内联 ORM dict + YAML 删除，未含 i18n label 清理——残留 i18n 条目对已删 dict 无害（仅未用 label），但镜像 0930-2 先例的完整 stale 闭环应在执行时一并清理；不阻塞计划授权（属执行细节，结束审计确认）。
- Independent draft review iteration 4: `held as draft (Blocker unresolvable at review time)`（mission-driver review，2026-07-26）— 0 新 Blocker / 0 新 Major / 0 新 Minor。逐项重核 load-bearing 事实精确匹配：ext:dict 消费者逐域 `rg -c` 输出 purchase 8 / sales 7 / assets 6 / quality 4 / maintenance 2 / cs 1 = 28 ✓；7 份 `doc-status.dict.yaml`（6 per-domain + 1 logistics stale）物理存在 ✓；logistics `orm.xml:58` 内联 `<dict name="erp-log/doc-status">` 在位 + 0 ext:dict 消费者（rg 匹配项为内联 dict 定义 + 生成态 `_app.orm.xml` + i18n label）✓；`../nop-entropy/` 无 `doc-status.dict.yaml` ✓；crm `lead-doc-status`（`orm.xml:37,221`）正确排除 ✓。格式合规（全部必需段 + 字段名 + Phase 结构有效）、范围单一结果面（doc-status dict 统一 + logistics stale 同语义清理合理 bundle，无 scope creep）、Exit Criteria 可测、Closure Gates/Closure Audit Evidence 占位就位。**Blocker 确认（同 iteration 2/3）**：ORM `model/*.orm.xml` 为 ask-first 保护区域（`project-context.md:69`），修改 6 域 28 列 ext:dict + 删 logistics 内联 dict 须人工批准——AI 审查者无权授予。解阻依赖路径 a（会话内人工确认 + 增 §Approval & Execution Status）或路径 b（拆分 ORM 变更为独立人工门控后续计划）。计划已正确置 `Review Hold` 且保持 `draft`，不提升为 `active`。iteration 3 Minor（logistics i18n label 残留）确认在位，保留给结束审计。
- Independent draft review iteration 5: `held as draft (Blocker unresolvable at review time)`（mission-driver review，2026-07-26）— 0 新 Blocker / 0 新 Major / 0 新 Minor。实时仓库复核全部 load-bearing 事实精确匹配：6 域源模型 `model/*.orm.xml` ext:dict 消费者逐域输出 purchase 8 / sales 7 / assets 6 / quality 4 / maintenance 2 / cs 1 = 28 ✓（全仓 rg 总计 56 含 codegen 镜像 `_app.orm.xml`，源模型仅 28）；logistics `erp-log/doc-status` ext:dict 消费者 0 ✓（仅内联 `<dict>` 定义 + YAML 文件残留）；7 份 `doc-status.dict.yaml` 物理存在（6 per-domain + 1 logistics stale）✓；`../nop-entropy/` 无 `doc-status.dict.yaml` ✓。格式合规（必需段齐全 / 字段名正确 / Phase 结构有效 / Item Types 标注合规 / Skill 显式记录）、范围单一结果面无 scope creep、Exit Criteria 可测（grep/find/mvn 命令明确）、Closure Gates 与 Closure Audit Evidence 占位就位。**Blocker 确认（同 iteration 2/3/4）**：ORM `model/*.orm.xml` ask-first 保护区域（`project-context.md:69`）需人工批准，AI 审查者无权授予——解阻依赖路径 a/b。计划已正确置 `Review Hold` 且保持 `draft`，不提升为 `active`。iteration 3 Minor（logistics i18n label 残留）保留给结束审计。

## Closure Gates

> 本计划触及 ORM `ext:dict` 属性值（ask-first 保护区域）+ dict YAML 增删。无新实体/列/关系/字典值。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 受影响域 `mvn test` + checker 复跑。

- [ ] 范围内行为完成（6 域 28 列 ext:dict 统一 + 6 份 per-domain YAML 删除 + logistics stale 清理 + 共享 dict 创建）
- [ ] 相关文档对齐（F2 闭包项 #3 + 0930-2 Deferred RELEASED + compliance-baseline R3 同步）
- [ ] 已运行验证：`mvn clean install -DskipTests` + 受影响域 `mvn test` + checker 复跑
- [ ] 无范围内项目降级为 deferred/follow-up（cs time-entry-approve-status 为 Non-Goal 显式排除，非范围内降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### cs `time-entry-approve-status` 统一

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 值集合不同（PENDING/APPROVED/REJECTED ≠ wf/approve-status UNSUBMITTED/SUBMITTED/APPROVED/REJECTED），为合法特化。统一需改值集合语义或新建标准字典，属不同结果面。
- Successor Required: `no`（触发条件：cs time-entry 审批流程对齐平台 wf/approve-status 四态语义时）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理填写>
- Evidence: <待填写>

Follow-up:

- cs time-entry-approve-status 统一（触发条件见上 Deferred But Adjudicated 段，非阻塞）
