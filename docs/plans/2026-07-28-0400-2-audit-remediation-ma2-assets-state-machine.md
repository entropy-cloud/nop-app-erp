# 2026-07-28-0400-2-audit-remediation-ma2-assets-state-machine MA2 assets 状态机审查（A2.10）

> Plan Status: active
> Mission: audit-remediation
> Work Item: A2.10 assets 状态机审查（A 级单域，18 状态字段）
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.10）
> Related: `docs/plans/2026-07-28-0230-3-audit-remediation-ma2-purchase-state-machine.md`（A2.8 purchase 状态机审查范式——docStatus/approveStatus 双轴业务单据 + reverseApprove→REJECTED 强制规则 + Processor 守卫 + tryPost 吞异常悬挂同型）；`docs/plans/2026-07-27-2211-1-audit-remediation-ma2-inventory-costing-consistency.md`（A2.4 库存核算一致性 done——成本/余额/流水三方对账 + reclose 兜底 + STANDARD 红冲不变量破缺 P1-MA2-024 + 红冲 today() P2-MA2-028）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/assets/state-machine.md`（资产卡片生命周期状态机 DRAFT/IN_SERVICE/IDLE/SCRAPPED/SOLD + 折旧计划条目 PENDING/EXECUTED/REVERSED + 终态不可恢复 + §审查提示清理凭证结转/停提配置/补提路径）；`docs/design/assets/depreciation-and-posting.md`+`split-merge.md`+`cip.md`+`inventory.md`（owner doc）
> Audit: required

## Current Baseline

assets（固定资产）域 A 级状态机审查（单域单工作项，18 状态字段）。assets 是 ERP 资产生命周期的核心域，状态机驱动**两类状态对象**：(1) **资产卡片生命周期状态机**（DRAFT→IN_SERVICE→IDLE/SCRAPPED/SOLD，owner doc §1-10 主覆盖）；(2) **业务单据审批状态机**（Movement/ValueAdjustment/Disposal/Capitalization/Split/Merge 等 docStatus/approveStatus 双轴）。assets 域**拥有全域最高密度的 Processor 链（48 Processor）**（roadmap A4.3 单列专属审计——本审计只做状态机业务正确性，Processor 代码质量归 A4.3），折旧/处置/资本化正确性直接影响财务报表（固定资产原值/累计折旧/清理损益）。

实时仓库已落地的资产状态机实现（待审查，路径 `module-assets/`）：

- **状态字段清单**（ORM `app-erp-assets.orm.xml`，18 状态字段分布于两类状态对象）：
  - **资产卡片生命周期轴**（`ErpAstAsset`）：`status`(erp-ast/asset-status)——owner doc §1 列 5 态（DRAFT/IN_SERVICE/IDLE/SCRAPPED/SOLD），**dict 实际 6 态（多 DISPOSED，由 split-merge 引入）**（P2-MA1-023 已登记 owner doc drift）。
  - **折旧计划条目轴**（`ErpAstDepreciationSchedule`）：`status`(erp-ast/depreciation-schedule-status)——owner doc 列 3 态（PENDING/EXECUTED/REVERSED），**dict 实际 4 态（多 CANCELLED）**（P2-MA1-024 已登记 owner doc drift）。
  - **业务单据双轴**（docStatus erp/doc-status + approveStatus wf/approve-status）：`ErpAstMovement`（资产移动/转移）+ `ErpAstValueAdjustment`（价值调整/重估，+ adjustmentType）+ `ErpAstDisposal`（处置，+ disposalType/reason，`useWorkflow="true"` xwf 审批轴浏览器层限制已 owner doc 标注）+ `ErpAstAssetCapitalization`（资本化入账，+ sourceType）+ `ErpAstSplit`（拆分）+ `ErpAstMerge`（合并）。
  - **CIP 在建工程轴**（`ErpAstCip`）：`status`(erp-ast/cip-status)。
  - **资产盘点轴**（`ErpAstInventory`/`ErpAstInventoryLine`）：`status`(erp-ast/inventory-status) + varianceType + disposition。
  - **资产维修轴**（`ErpAstMaintenance`）：`status`(erp-ast/maintenance-status) + treatment。
- **状态迁移实现**（`module-assets/erp-ast-service/.../service/`）：资产卡片生命周期迁移（资本化 DRAFT→IN_SERVICE / 暂停 IN_SERVICE→IDLE / 恢复 IDLE→IN_SERVICE / 处置 IN_SERVICE|IDLE→SCRAPPED|SOLD）+ 业务单据 Processor（submitForApproval/approve/reject/reverseApprove/cancel）——**需审查 reverseApprove 目标态合规性**（assets 业务单据的 reverseApprove 是否统一→REJECTED，与 finance/purchase 强制规则一致）+ 终态不可恢复约束（SCRAPPED/SOLD 无出边——处置错误经"处置冲销"反向清理凭证而非状态回退）。
- **折旧引擎与 Processor 链**（48 Processor 全域最高密度）：`ErpAstDepreciationScheduleProcessor`（期末批量折旧 + reverseDepreciation 冲销）+ 9 个 posting dispatcher（DEPRECIATION/CAPITALIZATION/DISPOSAL 等 createFacts，tryPost 吞异常 / reverse 硬前置——与 finance P1-MA2-032 + purchase P1-MA2-051 同型）+ `IErpFinAcctDocProvider`（跨工程聚合财务凭证）。**期末批量折旧高影响**（影响所有资产，需财务员权限+确认——owner doc §6 危险操作）。
- **跨域访问**：`IErpFinVoucherBiz`（折旧/处置/资本化过账跨域写会计保护区域）/ `IErpInvStockMoveBiz`（库存转固场景资本化入账出库——owner doc §7）/ `IErpFinBudgetCommitmentBiz`（若有）。daoFor 跨域只读已在 MA1 登记（P1-MA1-022 含 `ErpAstDepreciationScheduleProcessor:290` ErpFinAccountingPeriod + 9 个 posting dispatcher ErpMdSubject）；**finance 反向依赖** `ErpFinAccountingPeriodProcessor.reverseDepreciation:389` 跨域 daoFor ErpAstDepreciationSchedule **只读查询**（P1-MA1-016 已登记——finance→assets 跨域只读 DAO 查询，状态写经 `IErpAstDepreciationScheduleBiz.reverseDepreciation` I*Biz；本审计复核 assets 侧 `reverseDepreciation` 状态迁移 EXECUTED→REVERSED 正确性）。
- **处置审批浏览器层限制**（owner doc §已知限制）：`ErpAstDisposal` `useWorkflow="true"` xwf 审批轴浏览器层不可达（nop-wf sysUser(0) tagSet 覆盖），替代路径 DIRECT 三轴审批浏览器层可达——本审计复核 DIRECT 审批驱动的过账触发状态迁移正确性。
- **测试覆盖**：需审查资产状态机相关测试（折旧/处置/资本化/拆分合并/资产盘点/CIP 资本化等）。

**已登记的直指资产状态机的 finding（本审计须复核其状态机行为）**：

- `P1-MA1-008`（todo MR1，assets）：`ErpAstDepreciationSchedule`/`Movement`/`Revaluation`/`Split`/`Merge`/`Disposal`/`Capitalization`/`Transfer` 共 29 列 propId 缺失。**状态机 scope**：propId 是 ORM 元数据治理，非状态迁移——本审计确认无运行时状态机影响。
- `P1-MA1-016`（todo MR1，finance→assets）：`ErpFinAccountingPeriodProcessor.reverseDepreciation:389` 跨域 daoFor `ErpAstDepreciationSchedule`。**状态机 scope**：finance→assets 跨域 DAO 查询触发 assets 折旧冲销——本审计复核 assets 侧 `reverseDepreciation` 状态迁移（EXECUTED→REVERSED）正确性 + 是否经 assets I*Biz。
- `P1-MA1-022`（todo MR1，9 域合并）：ast `ErpAstDepreciationScheduleProcessor:290` ErpFinAccountingPeriod + 9 个 posting dispatcher ErpMdSubject 只读。**状态机 scope**：跨域只读是 period/subject 查询副作用，不破坏状态机——本审计复核异常路径无悬挂。
- `P2-MA1-023`（todo MR1，assets）：state-machine.md §1 列 5 态 vs dict 6 态（多 DISPOSED）。**状态机 scope**：直接是状态机 owner doc drift——本审计复核 DISPOSED 状态可达性 + 是否死状态。
- `P2-MA1-024`（todo MR1，assets）：折旧计划条目状态 owner doc 列 3 态 vs dict 4 态（多 CANCELLED）。**状态机 scope**：直接是状态机 owner doc drift——本审计复核 CANCELLED 可达性 + 折旧冲销路径。

**但从未做过一次覆盖资产全状态机（资产卡片生命周期 + 折旧计划条目 + 7 业务单据双轴 + CIP + 盘点 + 维修，18 状态字段）、按 `state-machine-business-review-prompt.md` 10 维度的系统性业务审查**。已知未核验控制点（owner doc §审查提示 + 已登记 finding）：

- **状态定义清晰性**：资产卡片 status 5 态 vs dict 6 态（DISPOSED 可达性——split-merge 引入，是否死状态）；折旧计划 3 态 vs dict 4 态（CANCELLED 可达性）；IDLE 折旧停提/恢复配置语义（owner doc §1「可配，默认停提」——配置是否落实）；处置终态 SCRAPPED/SOLD 与 DISPOSED 关系。
- **转换完整性**：资产卡片生命周期迁移完整性（DRAFT→IN_SERVICE→IDLE/SCRAPPED/SOLD 全迁移 + 资本化前置/处置前置）；业务单据 Processor reverseApprove 目标态合规性（→REJECTED 统一）；**终态不可恢复约束**（SCRAPPED/SOLD 无出边——owner doc §3 明示「处置错误经处置冲销反向清理凭证+重新处置，非状态回退」）；拆分/合并特殊迁移（IN_SERVICE→DISPOSED + 新资产 DRAFT）。
- **终端状态与恢复**：SCRAPPED/SOLD 终态不可恢复（处置冲销路径）；DRAFT 可物理删除（未入账）；IN_SERVICE/IDLE 不可删除只能处置；折旧计划 REVERSED 终态（红字凭证）。
- **异常路径**：折旧计提时已结账（拒绝，需反结账或计入当前期间——owner doc §4）；折旧后账面净值低于残值（直线法预计算，其他方法校验截断）；处置时累计折旧与原值不符（拒绝）；资本化凭证生成失败（资产保持 DRAFT+posted=false 异步重试）；资产类别科目映射缺失（折旧/处置凭证报错）；**期间结账后才发现折旧漏提**（反结账补提 vs 当期补提——owner doc §4/§9 场景 D，补提凭证注明归属期间）；并发折旧同一资产（乐观锁）；重复折旧幂等。
- **可达性**：DISPOSED dict 项可达性（split-merge 触发——P2-MA1-023）；折旧计划 CANCELLED 可达性（P2-MA1-024）；从 DRAFT 可达 IN_SERVICE→IDLE/SCRAPPED/SOLD 全态；IDLE↔IN_SERVICE 合法往复；无不可达状态/死锁。
- **角色与权限**：资本化入账/暂停恢复（资产管理员）；报废处置（资产管理员+审批）；出售处置（资产管理员+审批+财务确认）；折旧执行（财务员/系统期末自动）；**期末批量折旧高影响**（财务员权限+确认——owner doc §6 危险操作）。
- **外部依赖**：折旧/处置/资本化凭证生成（IErpFinAcctDocProvider 跨工程聚合→IErpFinVoucherBiz 跨域写会计保护区域）；期末批量折旧（财务域期末结账触发或资产域定时任务）；资本化库存出库（IErpInvStockMoveBiz 库存转固）；**finance 反向 reverseDepreciation**（finance→assets 跨域 daoFor P1-MA1-016）；外部步骤失败是否阻断状态迁移。
- **TODO/任务策略**：DRAFT assigned（资产管理员完善入账）；IDLE monitor（闲置资产待决策恢复/处置——owner doc §8「闲置超期提醒」）；IN_SERVICE 否（正常折旧自动）；SCRAPPED/SOLD 否；是否存在期望有人行动但不产生待办的状态。
- **场景演练**：(a) 设备购置折旧 happy path（DRAFT→IN_SERVICE+资本化凭证→期末折旧凭证→净值=残值停止）；(b) 资产闲置与恢复（IN_SERVICE→IDLE 停提→IDLE→IN_SERVICE 恢复）；(c) 资产报废（IN_SERVICE→SCRAPPED+清理凭证结转原值/累计折旧/清理损失）；(d) 资产出售（IN_SERVICE→SOLD+清理凭证+出售收入/清理损益）；(e) **折旧漏提补提**（反结账补提 vs 当期补提）；(f) **拆分/合并**（IN_SERVICE→DISPOSED+新资产 DRAFT）；(g) reverseApprove 红冲（业务单据→REJECTED+posted=false+凭证 reverse）；(h) **DIRECT 审批驱动的处置过账**（浏览器层可达路径）；(i) 资本化库存转固（IErpInvStockMoveBiz 出库）；(j) 并发折旧同一资产（乐观锁）。
- **与设计文档一致性**：`state-machine.md`/`depreciation-and-posting.md`/`split-merge.md`/`cip.md`/`inventory.md` vs 实现——重点核验：(1) §1 资产 5 态 vs dict 6 态（DISPOSED——P2-MA1-023）；(2) 折旧计划 3 态 vs dict 4 态（CANCELLED——P2-MA1-024）；(3) §3 终态不可恢复约束落实；(4) §4 异常路径（折旧漏提补提）；(5) §6 危险操作权限；(6) 处置 DIRECT 审批 xwf 限制 owner doc 一致性。

剩余差距：需要一次系统性状态机业务审查，发现任何遗漏的 P0（**资产卡片 DISPOSED 状态在非 split-merge 路径不可达** [若死状态——按 finance P1-MA2-031 + mfg P1-MA2-035 同型裁决 P1，不破坏主路径] / **终态不可恢复约束被违反** [SCRAPPED/SOLD 有出边——若破坏审计轨迹，P1/P0] / **折旧漏提补提路径缺失** [若破坏期末正确性——owner doc §4 明示两条路径] / **reverseApprove 目标态不一致** [契约漂移] / **资本化凭证失败资产悬挂 DRAFT+posted=false** [若 tryPost 吞异常无告警——同 finance P1-MA2-032 同型]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **资产卡片生命周期 + 折旧计划条目 + 7 业务单据双轴 + CIP + 盘点 + 维修（18 状态字段）** 做系统性业务审查，产出审计报告。
- 重点核验已识别控制点：(1) 状态定义清晰性（**DISPOSED 6 态 vs owner doc 5 态** / **折旧 CANCELLED 4 态 vs owner doc 3 态** / IDLE 停提配置）；(2) 转换完整性（生命周期迁移 + **reverseApprove 目标态合规** / **终态不可恢复约束** / 拆分合并特殊迁移）；(3) 终端与恢复（SCRAPPED/SOLD 不可恢复 + 处置冲销路径 / DRAFT 可删除 / 折旧 REVERSED）；(4) 异常路径（折旧已结账 / 净值低于残值 / 累计折旧不符 / **资本化凭证失败悬挂** / 科目映射缺失 / **折旧漏提补提** / 并发折旧 / 幂等）；(5) 可达性（DISPOSED/CANCELLED 可达性）；(6) 角色权限（**期末批量折旧高影响**）；(7) 外部依赖（过账跨域会计写 / 资本化库存转固 / finance 反向 reverseDepreciation）；(8) TODO 任务策略（IDLE 闲置超期提醒）；(9) 场景演练（10 个代表性场景）。
- 复核已登记 finding 在资产状态机运行时的行为影响：P1-MA1-008（propId——无状态机影响）/ P1-MA1-016（finance→assets reverseDepreciation 状态迁移复核）/ P1-MA1-022（跨域只读——状态机角度无升级）/ P2-MA1-023（DISPOSED 死状态复核）/ P2-MA1-024（CANCELLED 死状态复核），标注终态。
- scope matrix §状态机正确性 ast 列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.10 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A4.3 assets 折旧引擎与 Processor 链路专属审计 — 48 Processor 代码质量系统性审查归 A4.3；本审计只做状态机业务正确性审查（折旧/处置/资本化状态迁移 + 凭证触发正确性）。
- **不**审计 A2.3 期末结账端到端 — done；本审计只确认资产折旧/处置经 finance I*Biz（IErpFinVoucherBiz）+ finance 反向 reverseDepreciation 的**状态机迁移**正确性（期间结账 GL 正确性归 A2.3 finding）。
- **不**审计 A2.5 finance 期间/预算状态机 — done；本审计只复核资产折旧与会计期间结账的状态机协作（折旧已结账拒绝 + 反结账补提）。
- **不**审计 A4.6 finance+mfg view.xml drift — 资产页面契约漂移归 A4.6/A4.7。
- **不**审计 config-gated Deferred 偏离是否应实现（IDLE 停提配置 / 多级审批链 / 闲置超期提醒 job） — owner doc 已裁定，本审计只确认其在状态机上不引入悬挂。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/assets/state-machine.md`（资产卡片生命周期 + 折旧计划条目 + 终态不可恢复 + §审查提示 — **需复核 DISPOSED/CANCELLED 死状态 + 终态约束 + 补提路径**）；`docs/design/assets/depreciation-and-posting.md`（折旧/处置/资本化凭证 + 期末批量折旧 + reverseDepreciation 冲销）；`docs/design/assets/split-merge.md`（拆分/合并 DISPOSED 状态引入）；`docs/design/assets/cip.md`（在建工程资本化）；`docs/design/assets/inventory.md`（资产盘点差异 → 移动单）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）；`docs/architecture/posting-exemptions.md`（资产过账跨域写豁免登记）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.10 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：资产折旧/处置/资本化过账触及 finance 凭证链（IErpFinVoucherBiz 跨域写会计保护区域）+ 资本化库存转固触及库存写（IErpInvStockMoveBiz）。P0 即时修复若触及 `ErpAst*Processor`/折旧引擎/`Ast*PostingDispatcher`/xbiz 文件，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（会计保护区域）。ORM 字典变更（asset-status/depreciation-schedule-status）属 ask-first。xbiz 文件变更属状态机契约变更——须 owner doc + 人工确认。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 资产状态机系统性业务审查

Status: planned
Targets: `module-assets/erp-ast-service/.../service/processor/ErpAst*Processor.java`（资产卡片生命周期迁移 + 业务单据 Processor submitForApproval/approve/reject/reverseApprove/cancel + 守卫 + doApprove/doReverseApprove + IErpFinVoucherBiz）；`ErpAstDepreciationScheduleProcessor`（期末批量折旧 + reverseDepreciation 冲销 EXECUTED→REVERSED + daoFor ErpFinAccountingPeriod:290）；9 个 posting dispatcher（DEPRECIATION/CAPITALIZATION/DISPOSAL 等 createFacts + tryPost 吞异常/reverse 硬前置）；折旧引擎（净值/残值校验/直线法预计算/其他方法截断）；`IErpFinAcctDocProvider`（跨工程聚合）；拆分/合并 BizModel（IN_SERVICE→DISPOSED + 新资产 DRAFT）；CIP 资本化 BizModel；资产盘点 BizModel（差异→移动单）
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-008 propId + P1-MA1-016 finance→assets + P1-MA1-022 跨域只读 + P2-MA1-023/024 owner doc drift 已登记，本审计复核状态机角度）；A2.3 done（期末结账，折旧/期间协作）；A2.4 done（库存核算，红冲 today()/STANDARD 不变量同型范式 P1-MA2-024/P2-MA2-028）；A2.5a done（finance 凭证 reverseApprove 红冲闭环 + tryPost 吞误同型）

- [ ] 维度「状态定义」：审查资产卡片 status 5 态 vs dict 6 态（**DISPOSED 可达性**——P2-MA1-023）；折旧计划 3 态 vs dict 4 态（**CANCELLED 可达性**——P2-MA1-024）；IDLE 折旧停提/恢复配置落实；处置终态 SCRAPPED/SOLD 与 DISPOSED 关系；CIP/盘点/维修状态轴清晰性。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「转换完整性」：资产卡片生命周期迁移完整性（DRAFT→IN_SERVICE→IDLE/SCRAPPED/SOLD + 资本化/处置前置）；业务单据 Processor **reverseApprove 目标态合规性**（→REJECTED 统一——与 finance/purchase 一致）；**终态不可恢复约束**（SCRAPPED/SOLD 无出边——owner doc §3）；拆分/合并特殊迁移（IN_SERVICE→DISPOSED+新资产 DRAFT）。是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「终端状态和恢复」：SCRAPPED/SOLD 终态不可恢复（处置冲销反向清理凭证路径）；DRAFT 可物理删除（未入账）；IN_SERVICE/IDLE 不可删除只能处置；折旧计划 REVERSED 终态（红字凭证）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「异常路径」：核验全覆盖——折旧计提时已结账（拒绝/反结账/当期）；折旧后净值低于残值（直线法预计算/其他校验截断）；处置时累计折旧与原值不符（拒绝）；**资本化凭证生成失败**（资产保持 DRAFT+posted=false 异步重试——悬挂风险）；科目映射缺失（报错）；**期间结账后折旧漏提**（反结账补提 vs 当期补提——owner doc §4/§9 场景 D）；并发折旧同一资产（乐观锁）；重复折旧幂等。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「可达性」：**DISPOSED dict 项可达性**（split-merge 触发——P2-MA1-023）；**折旧计划 CANCELLED 可达性**（P2-MA1-024）；从 DRAFT 可达全态；IDLE↔IN_SERVICE 合法往复；无不可达状态/死锁/无限循环。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「角色和权限」：每个迁移绑定执行角色——资本化/暂停/恢复（资产管理员）；报废处置（+审批）；出售处置（+审批+财务确认）；折旧执行（财务员/系统）；**期末批量折旧高影响**（财务员权限+确认——owner doc §6 危险操作）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「外部依赖」：折旧/处置/资本化凭证生成（IErpFinAcctDocProvider 聚合→IErpFinVoucherBiz 跨域写会计保护区域）；期末批量折旧（财务域期末结账触发/资产域定时任务）；资本化库存转固（IErpInvStockMoveBiz 出库）；**finance 反向 reverseDepreciation**（finance→assets 跨域 daoFor P1-MA1-016——状态迁移复核）；外部步骤失败是否阻断状态迁移。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「TODO/任务策略」：DRAFT assigned（资产管理员完善入账）；IDLE monitor（**闲置超期提醒**——owner doc §8）；IN_SERVICE 否（正常折旧自动）；SCRAPPED/SOLD 否；是否存在期望有人行动但不产生待办的状态。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) 设备购置折旧 happy path；(b) 资产闲置与恢复；(c) 资产报废（清理凭证结转）；(d) 资产出售（清理凭证+损益）；(e) **折旧漏提补提**（反结账 vs 当期）；(f) **拆分/合并**（IN_SERVICE→DISPOSED+新资产）；(g) **reverseApprove 红冲**（业务单据→REJECTED+posted=false+凭证 reverse）；(h) **DIRECT 审批驱动的处置过账**（浏览器层可达——xwf 限制替代路径）；(i) 资本化库存转固；(j) 并发折旧同一资产（乐观锁）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「与设计文档一致性」：每个状态/转换在 `state-machine.md`/`depreciation-and-posting.md`/`split-merge.md`/`cip.md`/`inventory.md` 是否有匹配——重点核验：(1) §1 资产 5 态 vs dict 6 态（DISPOSED）；(2) 折旧计划 3 态 vs dict 4 态（CANCELLED）；(3) §3 终态不可恢复约束落实；(4) §4 异常路径（折旧漏提补提）；(5) §6 危险操作权限；(6) 处置 DIRECT 审批 xwf 限制 owner doc 一致性。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 复核已登记 finding 资产状态机角度：P1-MA1-008（propId 无影响）/ P1-MA1-016（finance→assets reverseDepreciation 状态迁移复核）/ P1-MA1-022（跨域只读无升级）/ P2-MA1-023（DISPOSED 死状态复核）/ P2-MA1-024（CANCELLED 死状态复核），标注终态。
      - Skill: none
- [ ] 产出审计报告 `docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`（含：资产卡片生命周期状态图 + 折旧计划条目状态 + 7 业务单据双轴迁移矩阵、各维度通过/失败裁决、控制点 PASS/FAIL、DISPOSED/CANCELLED 死状态裁决、MA1 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [ ] 资产卡片生命周期状态图 + 折旧计划条目状态 + 7 业务单据双轴迁移矩阵产出，每个状态/转换有通过/失败裁决与证据
- [ ] 已识别控制点（状态定义[含 DISPOSED/CANCELLED 死状态] / 转换完整性[含 reverseApprove 合规 + 终态不可恢复约束] / 终端与恢复 / 异常路径[含资本化悬挂 + 折旧漏提补提] / 可达性 / 角色权限[含期末批量折旧高影响] / 外部依赖 / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [ ] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: planned
Targets: 资产状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 ast 列
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] P0 finding 即时处理：每个 P0（**终态不可恢复约束被违反** [SCRAPPED/SOLD 有出边——若破坏审计轨迹] / **折旧漏提补提路径缺失** [若破坏期末正确性] / **资本化凭证失败资产悬挂 DRAFT+posted=false 无告警** [若 tryPost 吞异常无闭环——同 finance P1-MA2-032 同型] / **reverseApprove 目标态不一致** [契约漂移]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [ ] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。本审计对已登记 finding 只复核状态机运行时影响不重复登记根因；新 P1（如 DISPOSED/CANCELLED 死状态 [若确认不可达，按 finance P1-MA2-031 + mfg P1-MA2-035 同型裁决] / 终态约束缺口 / 折旧漏提补提缺口 / 资本化悬挂 / reverseApprove 不一致）按新 finding ID 登记。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §状态机正确性 ast 列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [ ] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [ ] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [ ] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_059ca59d8ffeHEnOQmXcxqNkU1`，独立 general 子代理，fresh-context）—— 1 处 trivial BLOCKER：Current Baseline 跨域访问段将 P1-MA1-016 标注为"finance→assets 跨域写"，与实仓不符（`ErpFinAccountingPeriodProcessor.reverseDepreciation:389` 的 daoFor 是只读 `findAllByQuery` 查询，状态写经 `IErpAstDepreciationScheduleBiz.reverseDepreciation` I*Biz；arm-index:74 分类为跨域只读 DAO 查询违规）。本计划 finding-recheck 项自身已正确称"跨域 DAO 查询"——内部不一致。
- Independent draft review iteration 2: **accept**（同会话复核）—— BLOCKER 已修正：跨域访问段改为"finance→assets 跨域只读 DAO 查询（状态写经 `IErpAstDepreciationScheduleBiz.reverseDepreciation`）"，与 finding-recheck 项一致。审查者确认无需再次实质复核（"no re-review iteration needed for substance, only the read/write label"）。其余核实要点：owner doc 全存在 ✓；finding ID（P1-MA1-008/016/022 + P2-MA1-023/024）描述匹配 ✓；11 个 ErpAst 实体在 ORM 存在 ✓；状态字段数 18 与 roadmap 一致 ✓；反松弛无禁词 ✓。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。资产折旧/处置/资本化过账触及会计保护区域，P0 即时修复须额外人工确认。xbiz 契约变更须人工确认。

- [ ] 范围内行为完成（A2.10 资产状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix、state-machine/depreciation-and-posting/split-merge/cip/inventory owner doc 结论已反映）
- [ ] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test -pl module-assets/erp-ast-service -am` 作回归基线确认；若有 P0 即时修复，该修复模块测试全绿
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A4.3 assets 折旧引擎与 Processor 链路专属审计

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A4.3（48 Processor 全域最高密度）。本审计做资产状态机**业务正确性**审查；折旧引擎/Processor 代码质量（异常处理/N+1/索引/辅助方法）系统性审查归 A4.3。
- Successor Required: `yes`——A4.3 执行时复核。

### A2.3 期末结账端到端 GL 正确性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.3 done（期末结账链路组件齐备已确认）。本审计做资产状态机**迁移正确性**审查；期末结账 GL 正确性归 A2.3 finding（P1-MA2-017~022 待 MR1）。
- Successor Required: `no`——A2.3 已 done，finding 待 MR1。

### A2.17 并发与乐观锁（并发折旧同一资产）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（期末批量折旧并发），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### config-gated Deferred 偏离本身（IDLE 停提配置 / 多级审批链 / 闲置超期提醒 job）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 config-gated/Deferred。本审计只确认其在状态机上不引入悬挂。
- Successor Required: `yes`——各 successor 触发条件满足时（如闲置超期提醒 job 上线 / 多级审批链上线）。

## Closure

Status Note: 待执行 + 独立 closure audit。

Closure Audit Evidence:

- 待执行完成后由独立子代理填充。

Follow-up:

- 待执行后填充（仅非阻塞跟进项目；已确认的缺陷不得出现在此处）。
