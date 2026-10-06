# 2026-10-06-1710-1 双员工表边界与跨域写一致性裁决（WI-08）

> Plan Status: completed
> Last Reviewed: 2026-10-06
> Source: `docs/backlog/successor-followup-closure-roadmap.md` WI-08（MA1 S 级 deferred successor：`docs/audits/2026-07-27-1015-arm-ma1-s-tier-orm.md` §4.4 + `docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-s-tier.md` §7「hr 双员工表」watch-only 确认）
> Related: `docs/architecture/data-dependency-matrix.md`、`docs/architecture/module-boundaries.md`、`module-hr/erp-hr-dao/src/main/java/app/erp/hr/biz/IErpHrEmployeeBiz.java`（countReferences javadoc 边界先声）
> Audit: required

## Current Baseline（确认记录，2026-10-06 HEAD 实证；草案审查 iteration 1 修订后数字为实测值）

1. **Schema 差异（实测列计数）**：`erp_hr_employee`（module-hr/model/app-erp-hr.orm.xml:271-346）**39 列 = 33 业务列（propId 1-33）+ 6 标准列**——完整人事档案（姓名三段/证件/紧急联系人/组织职位线/雇佣生命周期日期/薪酬三要素 bankAccountId+socialSecurityNo+taxFileNo/`userAccountId` 登录绑定；logical delete + version）；`erp_md_employee`（module-master-data/model/app-erp-master-data.orm.xml:867-906）**15 列 = 9 业务列（propId 1-9）+ 6 标准列**——业务经办人轻量参照（code/name/orgId/position/phone/email/`partnerId`/status）。同名非标准交集 = **code / email / orgId 三列**（orgId 两表同指 `ErpMdOrganization`；email 同义；code 语义分叉且 EMP-* vs HR-EMP-* 为种子约定非系统约束——UK 仅表内 (code,orgId) 唯一，无格式校验）；`name` 两表不共有（hr 为 firstName/lastName/fullName，md 为 name）。
2. **引用矩阵（orm to-one，`refEntityName` 全量 grep）**：`ErpMdEmployee` ← assets(6)/finance(2)/inventory(1)/logistics(1)/maintenance(3)/manufacturing(1)/master-data(2)/projects(3)/purchase(1)/quality(4) **10 域 24 处**（业务单据经办人/责任人参照：assets handler/staff/responsibleBy、purchase requester、quality inspector/responsiblePerson、manufacturing operator、inventory picker、logistics shipper、finance claimant、projects manager、maintenance leader）；`ErpHrEmployee` ← **仅 hr 域内部 25 处 to-one**（contract/assessment 等人事子实体）。
3. **跨域写路径**：零——master-data 生产代码零 `ErpHrEmployee` 引用；hr 生产代码唯一 `ErpMdEmployee` 提及为 `IErpHrEmployeeBiz.java:54` countReferences javadoc 边界声明（「不同表……不经 master-data 的 Party SPI（避免实体 ID 错配）」），另 src/test 一处注释（`TestErpHrEmployeeReferences.java:27`）；sql-lib/xbiz/xlib 全仓对两表零引用。
4. **数据面（种子 + 运行库裁决）**：种子两表人群**不相交**（hr CSV 2 行：HR-EMP-001 赵明/HR-EMP-002 钱华；md CSV 5 行：EMP-001/002/003 张三/李四/王五 + EMP-017 维护员甲 + EMP-021 质检员甲）；无同一自然人在两表并存。**运行库裁决**：生产部署未启动（roadmap §9 生产 DDL 迁移触发未命中），本地运行库 = dev H2 fresh-DB 种子重建（E2E webServer 每轮 `rm db/erp.mv.db` 重建装载同一种子集），无独立运行库数据面——种子结论即数据面结论。
5. **在案登记**：data-dependency-matrix.md:169 将 `erp_md_employee` 登记为 master-data 被引用表（assets/projects/maintenance 的 employeeId/managerId/staffId——该行引用域列表窄于 orm 实际 10 域引用面，本裁决注记一并修正）；**双表边界契约未成文**（MA1 两处审计仅标注 watch-only 未裁决；「S 级」为域复杂度分级〔S 级域 = finance/manufacturing/hr〕非严重性定级；1015 §5.3 假设的「HR 写员工 vs MD 写伙伴关联员工」跨域写已被本确认点 3 双向实证否定，其指定复核义务 MA2 A2.7a 已执行 verdict pass 无双表一致性问题）。
6. **登录绑定面**：`userAccountId` 仅存在于 hr orm 列+索引/view.xml 展示/i18n/api bean，**零消费逻辑**——全仓无代码把 `ErpMdEmployee`（md 经办人）解析到登录账号；非 hr 域 xbiz 零 employeeId 命中、notify 域零员工引用；hr 域内 `approverId→ErpHrEmployee` 参照（orm.xml:514）同样不做登录解析（userAccountId 零消费）；审计归因实际走平台审计戳 createdBy/updatedBy，与经办人弱指针解耦。

## Goals

- 裁决分支：**b（并存有据）**——登记双表边界契约为稳定架构事实（非缺陷）；产出裁决记录（`docs/analysis/`）+ `data-dependency-matrix.md` 对齐 + `module-boundaries.md`「跨工程实体关系规则」节增补（即 roadmap 交付语「owner scope matrix 增补」的实际落点）。
- 命名未来集成触发条件（如「HR 入职自动建业务经办人参照」需求立项时，走独立集成计划经 ID 映射，非本裁决范围）。

## Non-Goals

- 不做合并/主从迁移（分支 a）——无一致性缺陷驱动，ORM 保护区宽爆炸半径迁移无收益。
- 不改任何 ORM 模型、不建两表间外键/ID 映射（防止伪关联）。
- 不重写 hr/md 域既有 owner doc 的员工语义（仅在矩阵/边界文档补登记行）。

## Task Route

- Type: `architecture change`（边界契约文档化，非代码行为变更）
- Owner Docs: `docs/architecture/data-dependency-matrix.md`、`docs/architecture/module-boundaries.md`
- Skill Selection Basis: `Skill: cross-module-dependency-audit-prompt` 的引用矩阵方法论已在确认阶段应用（orm refEntityName 全量 grep + DAG 单向性核对）；本计划交付为裁决文档，无进一步技能匹配，`Skill: none`。

## Infrastructure And Config Prereqs

- 无（纯文档裁决）。

## Execution Plan

### Phase 1 — 裁决记录 + 双文档对齐

Status: completed
Targets: `docs/analysis/2026-10-06-dual-employee-table-adjudication.md`（裁决记录）、`docs/architecture/data-dependency-matrix.md`、`docs/architecture/module-boundaries.md`
Skill: none

- Item Types: `Decision`、`Add`

- [x] （Decision）裁决分支 b 的选择理由与替代方案记录：分支 a（合并/主从）被拒依据——零跨域写、零数据并存、两表 ID 空间独立且服务不同有界上下文（HR 人力全生命周期 vs 业务经办人参照），合并无缺陷可修且触发 ORM 保护区宽迁移；残留风险三项成文——①「HR 入职自动建业务经办人参照」类集成需求（触发 ID 映射集成计划）；②**md 经办人与登录账号零映射**（userAccountId 零消费逻辑；审计归因仅走 createdBy/updatedBy）——「按经办人做权限判定/审计归因」类需求 = 同触发；③两表 code 命名空间分叉为种子约定非约束——「报表/看板跨两表 join 假设」会得静默零行，边界注记显式禁跨表 join。
      - Skill: none
- [x] （Add）`data-dependency-matrix.md`：§被引用主数据表行（:169 附近）补 `erp_md_employee` vs `erp_hr_employee` 边界注记行（两表职责/实测列数/引用面〔含 :169 行域列表窄化修正至 10 域实况〕/零互写/零外键/ID 空间独立/code 命名空间约定性/禁跨表 join）；§565 hr 行员工引用语义澄清（hr 域内 `employeeId` 指 `ErpHrEmployee` 非主数据经办人）。
      - Skill: none
- [x] （Add）`module-boundaries.md`：「跨工程实体关系规则」节（参照共享内核登记先例——analysis 裁决文档 + owner doc 登记行的同构形状）增补双表边界契约：职责分野 + 跨域写禁令（任何域不得经弱指针直写对方员工表）+ 经办人↔登录零映射事实 + 触发条件指针（指向裁决记录）。
      - Skill: none
- [x] （Add）roadmap 回写：`successor-followup-closure-roadmap.md` §2 WI-08 todo→done（§10 规则 2 完成定义）+ §9「双员工表（已入 WI-08）」行注记已裁决（指针到裁决记录）。
      - Skill: none
- [x] （Proof）验证：裁决记录中的确认矩阵各点与实仓一致——引用矩阵 grep 复跑（md←10 域逐域 6/2/1/1/3/1/2/3/1/4=24；hr=25，与裁决记录一致）；两 orm 列清单/交集复验（Baseline 点 1 实测值）；`git diff --stat` 两 orm 零变更（输出「ORM-ZERO-CHANGE-OK」空 diff）；文档互链三处可达（裁决记录↔矩阵↔边界契约）。

Exit Criteria:

- [x] 裁决记录入册（含分支理由/替代方案/三项残留风险/触发条件）——docs/analysis/2026-10-06-dual-employee-table-adjudication.md
- [x] 两架构文档对齐落笔且与实仓矩阵一致（含 :169 窄化修正）
- [x] roadmap §2/§9 回写完成（todo 5/done 3 + §9 注记已裁决）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立草案审查子代理，fresh session，2026-10-06）——3 Major（F1 Baseline 列数 34/12 不实〔实测 39/15〕且「零交集」不实〔实为 code/email/orgId 三列同名非标准交集，name 不共有〕/F2 roadmap「种子+运行库」确认义务被静默收窄〔须显式裁决运行库=dev H2 种子重建〕/F3 边界契约漏「经办人↔登录身份零映射」登记）+ 5 Minor（F4 测试注释一处措辞/F5 md 种子 5 行漏枚举/F6 module-boundaries 实际落点节名〔跨工程实体关系规则节，共享内核先例同构〕/F7 roadmap 簿记回写步骤缺失/F8 零变更佐证应为 git diff 非 xmllint）。裁决挑战三项复核：经办人↔登录断链现状无运行时故障路径但契约必须显式登记；code 分叉无现存混淆载体（全仓零双表 join 站点）；「S 级」为域复杂度分级非严重性定级、1015 §5.3 跨域写假设已被实仓证伪——分支 b 有据非轻纵。**全部 8 项已修订落盘**（Baseline 六点重写/Decision 三残留风险/落点节指名/roadmap 回写项/git diff 佐证），送 iteration 2 复审。
- Independent draft review iteration 2: `accept`（同审查者增量复审，2026-10-06）——F1-F8 全部实质修复；R1（Closure Gates xmllint→git diff）/R2（24/25 处计数更正，含审查者 iteration 1 误计自我更正）随翻 active 落盘；R3（零映射主语限定 + hr approverId 注）/R4（roadmap 项 Item Type 标签）顺带处理。声明：可转 active。

## Closure Gates

- [x] 范围内行为完成（裁决记录 + 两文档对齐 + roadmap 回写）
- [x] 相关文档对齐（即本计划交付本体）
- [x] 已运行验证（矩阵 grep 复跑 + 两 orm 列清单/交集复验 + `git diff --stat` 零变更佐证；无代码变更故无测试门控）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（2 轮：iteration 1 needs revision 8 项 → 修订 → iteration 2 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（结束审计复核通过）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- 起草时无 deferred 项；分支 a（合并/主从）为**被拒备选**而非 deferred—— reopening 触发条件登记于裁决记录（产品立项「HR↔经办人参照自动同步」需求时）。

## Closure

Status Note: 2026-10-06 结束审计 ACCEPT，Plan Status: active → completed。Phase 1 五项 + Exit Criteria 3 项实证勾选维持；Closure Gates 8 项随本审计全部勾选（第 7 项由本独立审计满足）。裁决分支 b（并存有据）入册：docs/analysis/2026-10-06-dual-employee-table-adjudication.md + data-dependency-matrix 边界注记/:169 修正/§565 澄清 + module-boundaries「跨工程实体关系规则」节契约 + roadmap §5 WI-08 done / §9 注记。残留风险三条与重开触发条件以裁决记录 §3 为准（非 deferred、非缺陷）。日志段已随翻转补记。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，与执行者无共享上下文），successor-followup-closure-roadmap WI-08 派发，2026-10-06。方法：docs/skills/closure-audit-prompt.md。
- Evidence: ①交付物 1-4 逐项在位（analysis 裁决记录六点矩阵/三分支理由/三残留风险；矩阵边界注记行+:169 窄化修正+:565 hr 澄清；module-boundaries 跨工程实体关系规则节四条契约；roadmap WI-08 done + todo 5/done 3 + §9 注记）；②事实抽验全过——引用矩阵独立 grep 复跑：ErpMdEmployee 源模型 10 域 24 处（6/2/1/1/3/1/2/3/1/4）与 ErpHrEmployee 仅 hr 25 处精确吻合；两 orm 列实测 39=33+6 / 15=9+6、propId 分段与交集 code/email/orgId 吻合；`git diff --stat` 两 orm 零变更；锚点抽验 IErpHrEmployeeBiz.java:54 javadoc、orm.xml:514 approverId、md CSV 5 行/hr CSV 2 行人群不相交全中；③跨域写双向零引用 + xbiz/sql-lib/xlib 全仓零引用 + userAccountId 零消费逻辑独立复核成立；④roadmap §6 WI-08 验收四要素（确认/红灯/交付/验收）逐项对照通过，分支 b 红灯义务不适用、以边界契约文档为验收载体成立。Minor（非阻塞）：日志段补记随手项。

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得在此）
