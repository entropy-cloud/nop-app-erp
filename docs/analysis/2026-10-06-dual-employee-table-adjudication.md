# 双员工表（erp_hr_employee vs erp_md_employee）边界与跨域写一致性裁决

> 日期：2026-10-06
> 裁决：**分支 b——并存有据**（两表为不同有界上下文的合法并存，非缺陷；不合并、不建主从、不建互指）
> 载体：plan `docs/plans/2026-10-06-1710-1-dual-employee-table-adjudication.md`（WI-08，草案审查 2 轮收敛）
> 来源登记：MA1 S 级域（finance/manufacturing/hr）ORM 审计 deferred successor（`docs/audits/2026-07-27-1015-arm-ma1-s-tier-orm.md` §4.4/§5.3 + `docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-s-tier.md` §7）

## 1. 确认矩阵（2026-10-06 HEAD 实证，六点）

1. **Schema**：`erp_hr_employee` 39 列（33 业务 + 6 标准）——完整人事档案（姓名三段/证件/紧急联系人/组织职位线/雇佣生命周期/薪酬三要素/`userAccountId` 登录绑定；logical delete + version）；`erp_md_employee` 15 列（9 业务 + 6 标准）——业务经办人轻量参照（code/name/orgId/position/phone/email/partnerId/status）。同名非标准交集 = code/email/orgId（orgId 两表同指 `ErpMdOrganization`；email 同义；code 语义分叉且 EMP-* vs HR-EMP-* 为种子约定非系统约束——UK 仅表内 (code,orgId) 唯一）；`name` 不共有（hr 为 firstName/lastName/fullName）。
2. **引用矩阵（orm to-one 全量）**：`ErpMdEmployee` ← **10 域 24 处**（assets 6/finance 2/inventory 1/logistics 1/maintenance 3/manufacturing 1/master-data 2/projects 3/purchase 1/quality 4；业务单据经办人/责任人语义）；`ErpHrEmployee` ← **仅 hr 域内部 25 处**（contract/assessment 等人事子实体）。
3. **跨域写**：零——master-data 生产代码零 `ErpHrEmployee` 引用；hr 生产代码唯一 `ErpMdEmployee` 提及为 `IErpHrEmployeeBiz.java:54` javadoc 边界声明（「不经 master-data 的 Party SPI（避免实体 ID 错配）」），另 src/test 一处注释；sql-lib/xbiz/xlib 全仓零引用。
4. **数据面**：种子两表人群不相交（hr 2 行 HR-EMP-001/002；md 5 行 EMP-001/002/003/017/021）；无同一自然人并存。运行库 = dev H2 fresh-DB 种子重建（E2E webServer 每轮删库重建；生产部署未启动，roadmap §9 生产 DDL 迁移触发未命中）——种子结论即数据面结论。
5. **在案登记勘误**：data-dependency-matrix.md:169 原登记 `erp_md_employee` 引用域仅 assets/projects/maintenance，**窄于 orm 实际 10 域 24 处**（本次对齐修正）；MA1 1015 §5.3 假设的「HR 写员工 vs MD 写伙伴关联员工」跨域写被确认点 3 双向实证否定；其指定复核义务 MA2 A2.7a 已执行 verdict pass 无双表一致性问题。「S 级」为域复杂度分级（finance/manufacturing/hr）非严重性定级。
6. **登录绑定面**：`userAccountId` 零消费逻辑——全仓无代码把 md 经办人解析到登录账号；非 hr 域 xbiz 零 employeeId 命中；hr 域内 `approverId→ErpHrEmployee`（orm.xml:514）同样不做登录解析；审计归因走平台审计戳 createdBy/updatedBy，与经办人弱指针解耦。

## 2. 裁决理由

- **选择分支 b**：两表服务不同有界上下文——HR 人力资本全生命周期档案（hr 域私有消费）vs 跨域业务经办人参照（master-data 公共主数据）；零跨域写、零数据并存、零一致性依赖路径，**无缺陷可修**；边界语义已由 hr 侧 javadoc 先声（ID 错配规避）。
- **被拒备选（分支 a 合并/主从）**：合并无缺陷驱动却触发 ORM 保护区宽爆炸半径迁移（10 域 24 处 to-one 引用面 + HR 子实体 25 处重接线 + 种子/快照全量重录）；主从化引入两表同步一致性义务——恰为本次确认证实**不存在**的问题。
- **原 S 级登记的实证了结**：MA1 时代仅「标注不裁决」的 watch-only，本次 HEAD 六点确认后升格为有据裁决；1015 §5.3 的跨域写假设经实仓证伪，登记语义由「deferred successor（watch-only）」转为「裁决完结（并存有据）」。

## 3. 残留风险与重开触发条件

| # | 风险 | 触发条件（重开 = 独立集成计划） |
|---|---|---|
| 1 | 两表无关联——「HR 入职自动建业务经办人参照」类集成不存在 | 产品立项 HR↔经办人参照自动同步需求时（需 ID 映射，ORM 保护区） |
| 2 | **md 经办人与登录账号零映射**——「按经办人做权限判定/审计归因」类需求无法实现（现状审计归因走 createdBy/updatedBy，不受影响） | 产品立项按经办人授权/归因需求时 |
| 3 | code 命名空间分叉为约定非约束——报表/看板跨两表 join 会得静默零行 | 任何跨表 join 需求出现时先回到本裁决核对 ID 空间独立性 |

## 4. 对齐落点

- `docs/architecture/data-dependency-matrix.md`——被引用主数据表行边界注记 + :169 窄化修正 + §565 hr 行语义澄清。
- `docs/architecture/module-boundaries.md`——「跨工程实体关系规则」节双表边界契约（职责分野/跨域写禁令/零映射事实/触发条件指针）。
- `module-hr/erp-hr-dao/src/main/java/app/erp/hr/biz/IErpHrEmployeeBiz.java` countReferences javadoc——边界先声（既有，不改）。
