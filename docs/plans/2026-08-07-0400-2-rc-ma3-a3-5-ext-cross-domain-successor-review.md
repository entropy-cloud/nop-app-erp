# 2026-08-07-0400-2 rc-ma3-a3-5-ext-cross-domain-successor-review MA3 successor 追踪完整性与回队复查（扩展域+跨域）

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A3.5（扩展域 + 跨域 successor 复查）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A3.5
> Related: `docs/plans/2026-08-02-1530-2-existing-inventory-export.md`（M0.3 done，导出 successor 三源对账清单 扩展域+跨域 分组 + §对账差异登记 #5 实现修复项 successor 残留注记）、`docs/plans/2026-08-07-0300-2-rc-ma3-a3-1-finance-successor-review.md`（A3.1 done，范式参照）、`docs/plans/2026-08-06-0442-2-rc-ma3-a3-2-mfg-inv-pur-successor-review.md`（A3.2 done，范式参照）、`docs/plans/2026-08-06-0442-3-rc-ma3-a3-3-sal-ast-prj-qa-successor-review.md`（A3.3 done，范式参照；#5 maintenance 域 employee-id 投影交叉引用）、`docs/plans/2026-08-07-0400-1-rc-ma3-a3-4-hr-crm-cs-successor-review.md`（A3.4 同批，MA3 域分组连续复查）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份 MA3 复查报告 + arm-index/backlog 登记更新。基线盘点的是 successor 触发条件的现状，**不修改任何代码/真相源**。

- **方法论契约就绪**：`docs/audits/requirement-compliance-methodology.md`（§4 三判据 / §5 Q4 + 保护区域 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议 + §MA2↔MA3 协作）已落盘（M0.1 done）。

- **successor 三源对账全集已导出**（M0.3 done，`docs/audits/rc-existing-inventory.md` §successor 三源对账清单 — 扩展域+跨域分组）：design-level successor 去重并集 = **8 项**，逐项含三源覆盖标记（S1=arm-index 行内 / S2=owner doc 内嵌 / S3=backlog README）、触发条件摘要、已满足?、当前归属、复杂度：

  | # | successor 项 | 域 | 三源覆盖 | 触发条件摘要 | 已满足? | 复杂度 |
  |---|-------------|----|---------|-------------|---------|--------|
  | 1 | contract EXPIRED 自动到期 Job + 续期草稿 | contract | S1 | nop-job 接线时（P1-MA2-071 经 R1.22 实现修复，方案 B 路径未走） | ❌ 未满足 | A |
  | 2 | b2b EDI 出站自动化（TransportManager 接线 + ACK-timeout + 重试 + 升级） | b2b | S1+S2 | MFT transport 真实对接上线时（AS2/SFTP/FTPS） | ❌ 未满足（config-gated OFF + Mock transport） | A |
  | 3 | contract InvoicePlan 跨域写收敛为 I*Biz | contract | S1 | pur/sal 提供 purpose-built Facade 时 | ❌ 未满足 | A |
  | 4 | logistics 部分签收 | logistics | S1 | 承运商支持部分签收回调时 | ❌ 未满足 | C |
  | 5 | 跨公司 orgId 隔离查询/写入（多公司部署） | 跨域 | S1 | 多组织部署启用时 | ❌ 未满足（单组织种子掩盖） | A（跨域） |
  | 6 | 多账套 acctSchemaId 读路径隔离（报表/看板） | 跨域(finance) | S1 | `multi-schema-enabled=true` 启用时 | ❌ 未满足 | S |
  | 7 | 全域敏感动作 action-level RBAC（@BizAuth/FNPT） | 跨域 | S1 | owner doc §运行基线 灰度翻转 | ❌ 未满足（owner doc 显式声明有意默认） | A（跨域） |
  | 8 | OPEN_AUDIT 轮次形式化 | 跨域 | S1 | 形式化 closure-pending 清理循环 | ❌ 未满足 | A（跨域） |

- **§对账差异登记 #5（实现修复项 successor 残留）**：多项 finding 经 `resolved (R*.n done)` 实现修复关闭，但其 owner doc/arm-index 注记仍保留 successor 触发条件（如 P1-MA2-071 contract EXPIRED R1.22 / P1-MA2-073 b2b 自动化 / P1-MA2-093/094/095 多公司 R1.29 / P1-MA3-046 RBAC R2.7）。这些**不属 MA2 方案 B 复查**（已实现修复），但其 successor **属 MA3 复查**——本计划须按 M0.3 正确分流纳入上表，区分「已实现修复的 finding」与「仍待触发的 successor」。

  > **计数自检注记**：rc-existing-inventory §集成排序汇总表（:217）标注 A3.5 = 「9 项 successor」，但同文件 §successor 三源对账清单详细表（:172-179）权威列出 **8 行**。本计划以详细表 8 项为复查全集（逐行精确匹配），9-vs-8 差异为源文档汇总表 off-by-one（32 vs 33 总和），不影响完整枚举纪律。

- **复查四项任务**（roadmap MA3 Work Item Details）：逐项核对 ① 触发条件是否已满足（已满足 → 回队 MA1/R1.0）；② 是否该回队（回到审计 / R1.0 修复 / backlog README）；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核（防「已登记但从未触发」）。

- **已知结构性约束**：
  - #1（contract EXPIRED 自动到期）：P1-MA2-071 经 R1.22 已实现修复；须核实 successor（续期草稿 / 到期 Job 增强路径）是否仍有效。
  - #2（b2b EDI 出站自动化）：P1-MA2-073 + `managed-file-transfer.md` Non-Goal；config-gated OFF + Mock transport，真实 MFT 对接未上线；触及外部集成（AS2/SFTP/FTPS），修复实施时属外部集成保护区域。
  - #3（contract InvoicePlan 跨域写收敛）：`posting-exemptions.md §ErpCtInvoicePlanBizModel`；跨域依赖 pur/sal 提供 purpose-built Facade（同 A3.2 #2 委外收敛 createFromMrpLine 范式）。
  - #5（跨公司 orgId 隔离）：P1-MA2-093/094 经 R1.29 已实现修复（orgId 列 + 查询过滤），但多公司**部署侧**仍 successor（单组织种子掩盖，触发条件 = 多组织部署启用时）。须区分「finding 已修复（orgId 隔离已实现）」与「successor 仍有效（多公司部署未验证）」。
  - #6（多账套 acctSchemaId 读路径隔离）：P1-MA2-095 经 R1.29 已实现修复（acctSchemaId 列），但**报表侧**读路径隔离 successor。与 A4.1.23（多账套部署「每账套独立三表」运行时渲染）交叉引用（不同控制点：MA3 successor 触发条件 vs MA4 运行时渲染行为）。
  - #7（全域 action-level RBAC）：P1-MA3-046 经 R2.7 部分修复；owner doc 显式声明有意默认（灰度翻转触发条件）。
  - #8（OPEN_AUDIT 轮次形式化）：P1-MA6-005 R3.5 Deferred option B；跨域 governance successor。

- **与 A3.3 交叉引用**：A3.3 #5（employee-id 行过滤，quality+maintenance 跨域投影）已由 A3.3 合并裁决；maintenance 域投影归 A3.3 结论。本 A3.5 如遇 maintenance successor 项须交叉引用 A3.3 结论（不重复裁决）。

- **Q4 修复义务边界**：successor 触发条件**已满足**者须回队 MR1（R1.0 展开为 RC-R1.n，Q4 强制实现禁方案 B）；触发条件**未满足**者维持 backlog successor 登记（不强制实现，待触发）。
- **finding 路由 vs successor 触发条件路由（防执行者混淆）**：本 A3.x 只裁决 **successor 触发条件**是否回队，不重审方案 B 关闭裁决本身（属 A2.x）。successor 回队与否（A3.x）≠ finding 是否修复（A2.x→MR1），两者各自裁决、交叉引用不冲突。

- **保护区域**：本复查为**只读审计**（读 arm-index/owner doc/backlog README/git log，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。回队项的修复**不在本计划实施**——经 MR1（R1.0 展开）；触及外部集成（如 #2 MFT 对接）/ ORM 的修复行须 ask-first（§5）。

- **剩余差距**：A3.5 复查报告缺失 = MR1（R1.0）该域 successor 回队决策证据缺口来源 + **MA3 里程碑最后一个待完成行**（A3.1-A3.4 done 后 A3.5 是 MA3 收官）。本计划产出 A3.5 复查报告并登记回队决策，解除其在 MR1 链路的该域证据缺口 + 完成 MA3 里程碑。

## Goals

- 产出 MA3 复查报告 `docs/audits/<执行时间戳>-rc-ma3-a3-5-ext-cross-domain-successor-review.md`，含方法论 §6 **9 段全部内容**（MA3 适配：段 1=successor 三源对账清单 扩展域+跨域 分组；段 2=逐项四任务核证[触发条件已满足?/是否回队/补登记/README 覆盖]；段 3/4=既有行为证据；段 5=复查结论「回队 MR1 / 维持 backlog successor / 补登记」；段 6=arm-index 衔接；段 7=静态存疑点；段 8=过程纪律自检；段 9=与既有审计差异增量）。
- 对 8 项 successor **逐项**完成四任务核证（完整枚举，禁止抽样）：每项给出触发条件状态（已满足/未满足 + 证据，grep 实仓代码/config/ORM 字段）+ 回队决策（回队 MR1 / 维持 backlog / 补登记）+ README 既有行覆盖复核结果。
- 核实 §对账差异登记 #5（实现修复项 successor 残留：#1 contract EXPIRED R1.22 / #5 跨公司 R1.29 / #6 多账套 R1.29 / #7 RBAC R2.7 等）纳入完整性，区分「已实现修复的 finding」与「仍待触发的 successor」，避免误将已修复 finding 重新纳入 MR1。
- 对回队项登记双向可追溯（successor ↔ finding ID ↔ MR1 R1.0 预留展开行）。
- 报告产出即更新 `docs/audits/arm-index.md`（successor 回队注记）。

## Non-Goals

- **不实施修复**（修复属 MR1 R1.0 展开的 RC-R1.n；本计划是审计）。
- **不复查方案 B 关闭裁决本身**（属 A2.x；本计划只复查 successor 触发条件；与 A2.x 的两面关系按 §MA2↔MA3 协作交叉引用）。
- **不修改真相源**（product-scope / 各域 owner doc / backlog README 的既有 successor 登记；§9 冻结——回队决策记入报告 + arm-index，不直改 backlog README；README 覆盖差异记入报告交后续 backlog 维护）。
- **不复查其他域 successor**（A3.1-A3.4 各自独立 plan；A3.3 maintenance 域 employee-id 投影结论交叉引用不重复裁决）。
- **不重跑既有 arm 审计**（§去重协议：既有 `2026-07-2*-arm-ma2-*` / `arm-ma3-*` 扩展域+跨域已证实行为直接引用）。

## Task Route

- Type: `verification or audit work`（successor 触发条件完整性 + 回队复查；非实现变更）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§4/§5/§6/§7/§8/§9 + §MA2↔MA3 协作 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A3.5 工作项 + Work Item Details MA3）+ `docs/audits/rc-existing-inventory.md`（successor 三源对账清单 扩展域+跨域 分组 + §对账差异登记 #5）+ `docs/audits/arm-index.md`（successor 行内声明）+ 扩展域+跨域 owner docs（`contract/`[EXPIRED Job + InvoicePlan posting-exemptions]+`b2b/managed-file-transfer.md`[EDI Non-Goal]+`logistics/`[部分签收 Deferred]+`architecture/data-dependency-matrix.md §9`[跨域只读]+`finance/posting.md`[多账套]+各域 `architecture/governance`[RBAC/OPEN_AUDIT]，successor 内嵌段落 S2）+ `docs/backlog/README.md`（S3 既有追踪行）。
- Skill Selection Basis: `Skill: docs/skills/open-ended-audit-prompt.md`（roadmap MA3 全部 A3.x 指定）。该技能适合「触发条件是否已满足 + 是否该回队 + 实现修复项 successor 残留核实 + 跨域 governance successor 触发条件多依赖部署 config」的开放式逐项裁决。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 复查以读 arm-index/owner doc/backlog README/git log + grep 实仓代码（验证 Job/scheduler 回调/transport 接线/orgId 过滤/RBAC 注解/多账套 config 是否存在）为主（纯分析）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；本审计无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 8 项 successor 四任务逐项核证

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma3-a3-5-ext-cross-domain-successor-review.md`（新建，先填段 1-5）
Skill: `docs/skills/open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.3 done（方法论契约 + successor 三源对账导出就绪）

- [x] `Proof` 对 8 项 successor 逐项核证任务①②③：① 触发条件是否已满足（grep 实仓代码/config/ORM 验证，如 #1 contract EXPIRED 查 R1.22 修复后自动到期 Job 是否落地 + 续期草稿 successor 是否仍有触发条件、#2 b2b EDI 查 TransportManager 接线 + ACK-timeout + 重试 + 升级是否存在 + Mock transport config-gated 状态、#3 contract InvoicePlan 查 pur/sal 是否提供 purpose-built Facade、#4 logistics 部分签查承运商回调是否支持、#5 跨公司 orgId 查 R1.29 修复后 orgId 隔离是否落地 + 多公司部署 config 是否启用、#6 多账套查 acctSchemaId 读路径隔离 + multi-schema-enabled config、#7 全域 RBAC 查 @BizAuth/FNPT 注解覆盖率 + R2.7 部分修复范围、#8 OPEN_AUDIT 查轮次形式化是否存在）；② 是否该回队（已满足→回队 MR1 R1.0；未满足→维持 backlog）；③ 无触发条件的补登记。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Proof` 核实 §对账差异登记 #5（实现修复项 successor 残留）：区分 #1 contract EXPIRED（P1-MA2-071 R1.22）/ #5 跨公司（P1-MA2-093/094 R1.29）/ #6 多账套（P1-MA2-095 R1.29）/ #7 RBAC（P1-MA3-046 R2.7）的「finding 已修复」与「successor 仍有效」，避免误将已修复 finding 重新纳入 MR1。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Proof` 任务④ `docs/backlog/README.md` 既有行覆盖与正确性复核：grep backlog README contract/b2b/logistics/跨域 successor 行，逐项核实覆盖（防「已登记但从未触发」）+ 正确性（触发条件描述与实仓一致）。差异记入报告（不回写 README）。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Decision` 对 8 项逐项给出复查结论（`回队 MR1` / `维持 backlog successor` / `补登记`），列明触发条件状态证据 + 三源覆盖。#2 外部集成保护区域 + #5/#6/#7 实现修复项 successor 残留 + #6 与 A4.1.23 交叉引用 + maintenance 域投影与 A3.3 交叉引用须在结论中显式标注。
      - Skill: `docs/skills/open-ended-audit-prompt.md`

Exit Criteria:

- [x] 报告段 1（8 项三源对账清单）+ 段 2（逐项四任务核证）已落盘，每项含触发条件状态 + 回队决策 + README 覆盖复核（非悬空「待查」）
- [x] §对账差异 #5（实现修复项 successor 残留 #1/#5/#6/#7）已核实；#6 与 A4.1.23 交叉引用已标注

### Phase 2 - 报告定稿 / arm-index / 回队登记

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma3-a3-5-ext-cross-domain-successor-review.md`（补段 6-9，报告定稿）；`docs/audits/arm-index.md`（successor 回队注记）
Skill: `docs/skills/open-ended-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（8 项结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：successor 项均源自既有 arm finding，本复查原则上**复用既有 finding ID**追加 RC 注记；仅当发现新 successor（owner doc 内嵌但 arm-index 无行）才新建/补登记，须 grep 后裁决。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Add` 报告段 6 与 arm-index 衔接段：列明每项的复用/补登记裁决 + 双向可追溯（successor ↔ finding ID ↔ MR1 R1.0 预留展开行）。
      - Skill: none
- [x] `Add` 报告段 7 静态存疑点清单（供 MA4 A4.2 展开）：登记复查中需运行时确认的点（无则注明「无」）。
      - Skill: none
- [x] `Proof` 报告段 8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**。
      - Skill: none
- [x] `Add` 报告段 9 与既有审计差异增量声明：声明复用既有 arm 审计（successor 声明源自 arm-index + 扩展域+跨域状态机/drift 报告）+ 本复查只补的「触发条件是否已满足 + 回队决策」差异 + A3.3 maintenance 域交叉引用。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：回队项在既有 finding/successor 行追加「RC MA3 复查（A3.5）：触发条件已满足→回队 MR1」注记；补登记项（若有）入对应分区。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查段 1-9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告段 6-9 已落盘，9 段齐全；successor 复用/补登记裁决均有 arm-index grep 依据
- [x] 回队项已写入 `arm-index.md`；静态存疑点清单已登记（供 MA4 展开）
- [x] 段 8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 `ses_02ba95dd4ffe00BY30xDYflQok`，fresh session，未起草本计划）。8 项 successor 逐行对 rc-existing-inventory §扩展域+跨域详细表（:172-179）核证全匹配（三源覆盖/触发条件/已满足/复杂度/finding ID）；M0.3 done 核实；方法论 §4/§5/§6/§7/§8/§9 + §去重协议核实；A3.1/A3.2/A3.3 completed + A3.4 draft（同批）核实；§对账差异 #5（P1-MA2-071/073/093/094/095/MA3-046）正确引用；A4.1.23 交叉引用正确（不同控制点）；Rule 1/2/4/7/8/14 + anti-slack 全 PASS；Closure Gates 正确移除 build/test 门控。无 BLOCKER / 无 MAJOR。3 non-blocking MINOR 已采纳修订：8-vs-9 计数差异已显式标注（计数自检注记段）。§MA2↔MA3 协作 citation 精度（cosmetic，概念在 baseline 正确描述）。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 8 项逐项四任务核证 + §对账差异 #5 核实 + successor arm-index 衔接 + 段 8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A3.5 报告 9 段齐全 + 8 项逐项四任务结论 + 回队项登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §4/§5/§6/§7 + §MA2↔MA3 协作 + §去重协议一致；与 rc-existing-inventory 扩展域+跨域 successor 分组 + §对账差异登记 #5 一致；与 A3.3 maintenance 交叉引用一致
- [x] 已运行验证：报告 9 段完整性自检 + 段 8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 回队项的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。回队项的修复按方法论 §10 经 MR1（R1.0 展开为 RC-R1.n）实施；触及外部集成（如 #2 MFT transport AS2/SFTP/FTPS）/ ORM 的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本复查闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告回队项交叉引用展开修复行）

## Closure

Status Note: A3.5 扩展域+跨域 successor 复查闭环——8 项逐项四任务核证完成（0 回队 MR1 / 8 维持 backlog / 0 补登记），MA3 里程碑（A3.1-A3.5）收官，独立结束审计 PASS。

Closure Audit Evidence:

- Auditor / Agent: independent closure audit subagent (fresh session, cold-context — did NOT execute this plan)
- Evidence: PASS。五点一致性核对全部一致——(1) 顶部 `> Plan Status: completed` ↔ (2) Phase 1/Phase 2 Status 均 `completed` 且全部 `[x]` ↔ (3) Phase 1/2 Exit Criteria 全部 `[x]` ↔ (4) Closure Gates 全部 `[x]`（含独立结束审计门控）↔ (5) `docs/logs/2026/08-07.md` A3.5 条目非空（lines 3-19，含 grep 证据 + §对账差异 #5 区分 + MA2↔MA3 两面纪律 + 验证记录）。

  Anti-hollow 实仓抽查（4 项独立 grep，全部证实执行者证据真实）：
  - #1 contract：`rg "ErpCtContractExpiryJob|@CronProvider" module-contract/` **零命中**（自动到期 Job 确实缺失）；`@BizMutation` 手工路径确实存在 → 报告 §2.1 证据真实。
  - #4 logistics：`rg "TRACKING_EVENT_PARTIAL|partialSignedQty|receivedQuantity" module-logistics/` **零命中**；`ErpLogConstants.java:37-39` 仅 PICKED_UP/IN_TRANSIT/DELIVERED 三常量；`GatewayDispatcher.advanceTracking:162-188` 仅处理 DELIVERED → 报告 §2.4 证据真实。
  - #5 跨域 orgId：`ErpOrgContext.java` + `ErpOrgIsolationOrmInterceptor.java:20` + `ErpOrgIsolationQueryTransformer.java:34` 全部存在于 `module-common-service`；`rg "useTenant" --glob '*.orm.xml'` **零命中** → 报告 §2.5 证据真实。
  - #2 b2b：`TransportManager.java:36` 存在并在 `app-service.beans.xml:49-50` 注册，但 `rg "transportManager\.send" module-b2b/erp-b2b-service/src/main` **零生产调用命中**（wired-but-uncalled）→ 报告 §2.2 证据真实。

  §对账差异 #5 纪律核证（C）：报告 §2.1/§2.5/§2.6/§2.7 严格区分 finding-fixed vs successor-still-valid——#5/#6 finding resolved-via-implementation（机制已 R1.29 落地）successor 部署侧维持、#7 finding done R2.7 partial（声明+beans config-gated OFF）successor 灰度翻转维持、#1 finding resolved-via-deferral（§4 三判据复核归 A1.x→MR1 通道）successor 维持；均明确「不误将已修复 finding 重新纳入 MR1」，无重纳违规。

  交叉引用核证（D）：#6↔A4.1.23（不同控制点：MA3 successor 触发条件 vs MA4 运行时渲染行为）在 §2.6/§5.3/§9 多处标注；A3.3 maintenance employee-id 投影交叉引用（不重复裁决）在 §1 footer/§5.3/§9 标注。

  arm-index 核证（E）：`rg "RC MA3 复查（A3.5）|RC MA3 复查 2026-08-07" docs/audits/arm-index.md` 确认 8 项 successor 全部注记——9 个 finding 行（`P1-MA2-071` :514 + `P1-MA1-029` :463 + `P1-MA2-073` :516 + `P1-MA2-079` :522 + `P1-MA2-093` :536 + `P1-MA2-094` :537 + `P1-MA2-095` :538 + `P1-MA3-046` :581 + `P1-MA6-005` :601，其中 #5 跨公司映射 093+094 两个 finding 行，故 8 项 successor = 9 finding 行）均追加注记。

  roadmap+log 核证（F）：`docs/backlog/requirement-compliance-roadmap.md:118` A3.5 = `done ✅`；`docs/logs/2026/08-07.md` A3.5 条目存在且非空。

  §8 checker 表核证（G）：报告 §8 actual-vs-baseline 表（R1a/R1b/R1c/R1d/R2a/R2b/R2c/R2d = 0/0/0/14/34/229/1382/34）与 `docs/audits/compliance-baseline.md` §BASELINE machine-readable 块（lines 296-316）**逐行精确一致**（注：初版 §基线表 lines 17-24 为历史值，已被 machine-readable 块取代为权威）。独立复跑 `bash docs/audits/nop-compliance-checker.sh` exit 0、R2d 实测 34 处与报告一致。报告含「不以 checker 脚本退出码作为门控通过依据」声明 + 独立性声明 + 与 arm-index 交叉去重声明。

  §9 真相源冻结核证（H）：`git status --short` 仅显示 5 项变更——`M docs/audits/arm-index.md`（仅注记追加）+ `M docs/backlog/requirement-compliance-roadmap.md`（仅 A3.5 状态）+ `M docs/logs/2026/08-07.md`（仅日志新增）+ `M docs/plans/.../本计划.md`（仅闭环填写）+ `?? docs/audits/2026-08-07-0430-...md`（新报告）。**零**修改 product-scope / 各域 owner doc 需求契约段落 / arm-index 已关闭 finding 关闭事实 / backlog README——真相源冻结守约。

  参考本次审计派发：独立结束审计派发（INDEPENDENT CLOSURE AUDITOR, fresh session）。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
