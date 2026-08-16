# 2026-08-16-1634-1-rc-mr1-r1-57-crm-team-member-allocation RC-R1.57 — crm 团队分配 ROUND_ROBIN/LOAD_BALANCED（P1-RC-036 越界项：ORM 结构变更 + 挑人算法 + config 门控）

> Plan Status: completed

#### 双独立子 agent 批准记录（2026-08-16，实施前落盘）

- **Reviewer A**：ses_ff627bd01ffek61BuHJk5fx1oo（fresh session）——**APPROVE**。检查范围：纯加性实证（grep ErpCrmTeamMember/erp_crm_team_member 全仓零 code/ORM 命中 + Non-Goals 不动 ErpCrmLead/ErpCrmTeam 既有列/UK）；propId 1-10 连续；约定一致（tagSet/useLogicalDelete/审计列/stdDomain=userId 对齐 ErpCrmTeam.teamLeaderId :393）；UK 命名无碰撞；to-one team 同域引用（对齐 orm.xml:25 跨工程零 refEntityName）；治理链（roadmap:37 A 类授权 + ai-autonomy-policy:79 双批准先于写入 + Phase 2 首项排序）。Required corrections: none。Optional：D3 默认 TRUE 行为变化注记保留（territory.md 写作时）；IDX 与 UK 首列部分冗余属文件既有惯例无害。
- **Reviewer B**：ses_ff627a22effeuranOcZ0uUl9OT（fresh session）——**APPROVE**。检查范围：纯加性（git status module-crm/model 干净 + 全仓零命中）；零迁移/回填（Phase 2 仅 Add + codegen）；无 codegen 环（to-one 单向同域，ErpCrmTeam 零改动）；propId 连续 + mandatory 三列；约定逐项比对 ErpCrmTeam/ErpCrmTerritoryAssignmentRule；Non-Goals 对齐；治理排序（批准 checkbox 在 orm.xml Add 前 + 批准记录落盘）。Required corrections: none。Optional：beans.xml 项按 D4 定稿（BizModel 侧传入）实为核验型 no-op；UK 不含 delVersion 与文件惯例一致（逻辑删除重加同成员会 UK 冲突，非阻断，与既有 UK 惯例一致）。

两个独立子 agent 均 APPROVE，无强制修正 → 批准先于 live orm.xml 写入的治理约束满足（ai-autonomy-policy.md:79）。
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.57（P1-RC-036，UC-CRM-11 ROUND_ROBIN/LOAD_BALANCED 降级 MANUAL 修复）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.57 行 + `docs/audits/arm-index.md` P1-RC-036 行（:196）+ 展开器 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（RC-R1.57 = ORM 结构变更 ErpCrmTeamMember）
> Related: `docs/design/crm/use-cases.md`（L1 UC-CRM-11 :239-266）；`docs/design/crm/territory.md`（§业务规则 2 :132-160 + §实现注记 2 :224-228）；`docs/audits/2026-08-05-1030-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`（A1.28 §5.6 :235/:240/:257 + §6 P1-RC-036 :290）；`docs/audits/2026-08-07-2345-rc-ma4-a4-2-83-96-crm-lead-lifecycle-marketing-forecast-runtime.md`（A4.2.87 :62-70）；`docs/plans/2026-07-07-1100-1-territory-assignment-engine.md`（territory.md Deferred 源头 plan）
> Audit: required

## Current Baseline

- **finding P1-RC-036（arm-index:196，UC-CRM-11 #6）**：L1（`use-cases.md:255-258`）逐字「按 assignmentMethod 分配：ROUND_ROBIN → 轮流分给团队内成员 / LOAD_BALANCED → 分给线索最少的成员 / MANUAL → 标记待分配」+「回写 lead.territoryId / lead.teamId / lead.ownerId」——L1 显式三方法并列且 ROUND_ROBIN/LOAD_BALANCED 要求**挑人**。L3 实仓（HEAD 核查）：
  - `TerritoryAssignmentEngine.toResult:73-84` **显式将非 MANUAL 方法降级为 MANUAL**（`:79-82` if 非 MANUAL → setAssignmentMethod(MANUAL) + setDegraded(true)，**ownerId 永不设置**）——ROUND_ROBIN（轮流挑人）+ LOAD_BALANCED（最少线索挑人）**未实现**；
  - `ErpCrmLeadBizModel.assignLead:136-158` + `defaultPrepareSave:211-238`（config-gated `erp-crm.territory.auto-assign-on-create` 默认 TRUE）消费 `AssignmentResult`，ownerId 仅当 `result.getOwnerId() != null` 时回写（:153-155/:233-235）——引擎不挑人时 lead.ownerId 保持 null 待分配；
  - **ErpCrmTeamMember 实体不存在**（全仓 grep `ErpCrmTeamMember|teamMember` 零命中）；`ErpCrmTeam`（`app-erp-crm.orm.xml:385-413`）仅 id/code/name/orgId/teamLeaderId/remark + UK_CRM_TEAM_CODE_ORG + IDX_CRM_TEAM_TEAM_LEADER_ID，**无成员子实体**；
  - `ErpCrmLead`：ownerId（:212 propId 25）/teamId（:213 propId 26）/territoryId（:214 propId 41）+ leadType/docStatus（:191/:221）——挑人计数所需字段齐备；
  - dict `erp-crm/assignment-method`（:109-113）：ROUND_ROBIN/LOAD_BALANCED/MANUAL 三值全持久化（引擎不消费非 MANUAL）；
  - `ErpCrmConstants`（:117-119）：ASSIGNMENT_METHOD_ROUND_ROBIN/LOAD_BALANCED/MANUAL 常量已存在；
  - `TerritoryAssignmentEngine` 为纯函数式 bean（beans.xml `app-service.beans.xml:40-41` 注册，构造器可注入 ConditionMatcher）——成员解析需新注入面或经 BizModel 传入。
- **§4 三判据复核（A1.28 §5.6 已执行）**：territory.md:224-228 Deferred 标注（"触发条件：ErpCrmTeamMember 实体落地"）为 AI 落地补注，git log 无人工批准痕迹；(iii) product-scope 未裁剪 → **非 documented simplification → Q4=(a) 重开 P1 强制实现**。A4.2.87 运行时确认维持 P1（:70）。
- **owner doc 契约（territory.md §业务规则 2 + §分配执行流程 :158-160）**：ROUND_ROBIN →「查 teamId 成员列表，取上次分配的下一位」；LOAD_BALANCED →「查 teamId 成员当前线索数，分给最少的」——挑人算法语义已设计，仅实现缺失。
- **2026-08-12 批量裁决 A 类**（roadmap 头 :31-37）：crm RC-R1.57（新增 ErpCrmTeamMember 实体 或 成员子表）ORM 修改授权已批量批准（对齐 Q3 纯加性类自动执行范围：加列/加 UK/新增实体）；越界回落双独立子 agent 批准（2026-08-15 升级：保护区域 ORM 修改须双独立子 agent 批准，批准记录落盘计划文件）。**product-scope 确认义务裁決**：arm-index:196 状态列标注「先须人工确认 product-scope 是否裁剪」——本计划按 2026-08-12 批量裁决 A 类（用户批准 RC-R1.57 ORM 授权，roadmap:37）+ 2026-08-15 升级（越界项不再暂停等待人工，ai-autonomy-policy.md:83）**显式裁决：product-scope 未裁剪 ROUND_ROBIN/LOAD_BALANCED（product-scope.md 无排除记录）→ Q4=(a) P1 强制实现**，该确认义务由裁决声明闭合，审计不复开。
- **测试基线**：`TestErpCrmTerritoryQuota`（13 @Test，区域配额/rollup，无引擎挑人断言）；`TestSequenceAssignmentEngine`（序列分配，非 territory）；无 TerritoryAssignmentEngine 专属测试类。erp-crm-service **180 tests 全绿**（R1.25 基线）。
- **compliance 基线**：R2c=1420 / R2b=233 / R2d=35（`compliance-baseline.md` §BASELINE 机器可读块）；新实体 + 引擎成员解析可能新增 daoFor 站点 → 结束前须复跑 checker 分类 baseline-raise vs Fix。

## Goals

- **UC-CRM-11 三方法运行时成立（P1-RC-036 核心）**：ROUND_ROBIN 按团队成员轮流挑人 + LOAD_BALANCED 按团队成员当前线索数挑人 + MANUAL 保持待分配语义——`AssignmentResult.ownerId` 在 ROUND_ROBIN/LOAD_BALANCED 下被真实设置并回写 lead.ownerId（L1 字面 :255-258 + territory.md 语义 :158-160）。
- **成员模型落地**：新增 `ErpCrmTeamMember` 实体（teamId + userId 成员行，Q3 纯加性：新实体 + UK），承载团队-成员关系（territory.md:227 successor 触发条件满足）。
- **引擎改造**：`TerritoryAssignmentEngine` 保留纯函数式可测性——成员/计数解析经新注入面（函数式接口或 BizModel 传入）接入，toResult 非 MANUAL 不再无条件降级；无成员/不可解析时保持 MANUAL 降级 + degraded 标记（既有语义不破坏）。
- **config 门控**：`erp-crm.territory.assignment-method-enabled`（或等价键，Phase 1 定稿）控制挑人激活（默认值决策见 Phase 1 D3；关闭时维持既有 MANUAL 降级零回归）。
- **测试**：新增 `TestErpCrmTerritoryAssignment`——①ROUND_ROBIN 轮流（成员 A→B→A）；②LOAD_BALANCED 最少线索（计数差异/平手裁决）；③无成员降级 MANUAL；④config 关闭降级 MANUAL；⑤assignLead 集成回写 ownerId；⑥defaultPrepareSave 自动分配路径；⑦GraphQL 冒烟。既有 180 tests 零回归。
- **零回归**：erp-crm-service 全量测试全绿 + 全仓 `mvn test` + 全量构建 + compliance checker 零漂移（新增 daoFor 站点带 per-site 证据 baseline-raise 或零新增）。
- **owner doc 收敛**：territory.md §实现注记 2 Deferred→已实现 + 实现注记补写；arm-index P1-RC-036 → done (RC-R1.57) + roadmap 行 done + logs 条目。

## Non-Goals

- **不实现 LOAD_BALANCED 的"最少线索"实时重算调度**（仅分配时点计数查询，不做周期性再平衡——L1 未要求）。
- **不实现成员维护 UI 页面**（ErpCrmTeamMember 走标准 CRUD 生成，无 AMIS 定制）。
- **不改 ErpCrmLead/ErpCrmTeam 既有列与 UK**（仅新增 ErpCrmTeamMember 实体）。
- **不重写 ConditionMatcher/匹配遍历逻辑**（引擎匹配部分保持，仅改造 toResult/挑人段）。
- **不实现跨组织成员继承/团队成员角色**（成员=userId 直挂 teamId，无角色/层级语义）。
- **不改真相源契约段落**（use-cases L1 不动；territory.md 契约段不动，仅补实现注记 + Deferred 更新）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：ORM 结构变更[Q3 纯加性新实体，2026-08-12 A 类已授权] + 引擎挑人算法 + config 门控；Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/crm/use-cases.md`（L1 UC-CRM-11）+ `docs/design/crm/territory.md`（§业务规则 2/§分配执行流程/§实现注记 2）
- Skill Selection Basis: ORM 模型变更（加实体）→ 增量重生成 `mvn clean install -DskipTests`；BizModel/引擎编排（`nop-backend-dev`：IBiz 注入 + 跨实体访问 + config 门控范式）；测试（`nop-testing`：JunitBaseTestCase + GraphQL 引擎冒烟 + `_cases/` 快照）。

## Infrastructure And Config Prereqs

- 新 config 键（Phase 1 D3 定稿）：`erp-crm.territory.assignment-method-enabled`（默认值待 D3 裁决：倾向 TRUE 对齐 auto-assign-on-create 默认 TRUE 先例；关闭=既有 MANUAL 降级零回归）。
- ORM 变更（新增 ErpCrmTeamMember 实体）→ 须 `mvn clean install -DskipTests` 增量重新生成（**不要重跑 nop-cli gen**），生成产物核对含 propId 分配。
- 双独立子 agent 批准 checkbox（2026-08-15 升级：ORM 保护区域修改须两个独立子 agent 分别检查批准，批准记录落盘本计划 Phase 2）。

## Execution Plan

### Phase 1 - 成员模型/挑人语义/引擎形态/config 裁决（Decision）

Status: completed
Targets: `docs/design/crm/territory.md`（§实现注记 2 更新）；orm.xml 新实体段**以计划内文本草案形态产出**（不落盘 live orm.xml——ORM 保护区域写入须待 Phase 2 双独立子 agent 批准后实施，对齐 ai-autonomy-policy.md:79）
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: none

- [x] `Decision` D1 成员实体设计：`ErpCrmTeamMember`（tableName `erp_crm_team_member`，tagSet=gid,erp.crm，useLogicalDelete=true 对齐 ErpCrmTeam）——列集 {id, teamId FK BIGINT, userId stdDomain=userId VARCHAR 36, remark} + 审计列 + UK（UK_CRM_TEAM_MEMBER_TEAM_USER: teamId+userId）+ to-one team；propId 连续分配（对齐平台 propId 连续校验）；不设 orgId 冗余列（经 team 解析）。替代方案（ErpCrmTeam 内嵌成员子表 JSON）否决理由记录。**草案文本落本计划，不写 live orm.xml**。
      - Skill: `nop-backend-dev`
- [x] `Decision` D2 挑人算法语义（对齐 territory.md :158-160 字面）：
      - ROUND_ROBIN：查 teamId 成员列表（按 id 升序）→ 查该 team 上次分配记录（lead.teamId=teamId 且 ownerId 非空，按 createTime desc limit 1）→ 取上次 owner 在成员列表中的下一位（循环）；无历史记录 → 取第一位成员；
      - LOAD_BALANCED：查 teamId 成员当前线索数（count lead.teamId=teamId 且 ownerId=member，docStatus 非 CONVERTED/LOST/CANCELLED 的活跃线索——计数口径 Phase 1 定稿）→ 取最少者（平手按成员 id 升序首个）；
      - 成员列表为空 / 查询失败 → 保持 MANUAL 降级 + degraded=true（既有语义）。
      - Skill: `nop-backend-dev`
- [x] `Decision` D3 config 门控：键名 `erp-crm.territory.assignment-method-enabled`（ErpCrmConstants.CONFIG_* + ErpCrmConfigs reader）；默认值裁决（TRUE 对齐 auto-assign-on-create 先例 vs FALSE 保守）——记录理由与残留风险（默认 FALSE 时 ROUND_ROBIN/LOAD_BALANCED 不激活，L1 三方法字面仅在部署启用后成立，须 owner doc 注记 config-gate 语义）。
      - Skill: `nop-backend-dev`
- [x] `Decision` D4 引擎注入形态：选项 A（推荐）——`TerritoryAssignmentEngine` 新增函数式接口（如 `TeamMemberResolver`：`List<String> resolveTeamMemberUserIds(Long teamId, IServiceContext ctx)` + `String resolveLastAssignedOwner(Long teamId, ...)` + `Map<String,Integer> countActiveLeadsByOwner(Long teamId, ...)`）经构造器注入，BizModel 提供 daoProvider 实现，引擎保持纯函数式可测；选项 B——BizModel 预解析成员列表/计数传入 assign()。记录理由 + 测试可测性分析。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] D1-D4 各记录选择 + 替代方案 + 残留风险（写入计划或 territory.md 注记）
- [x] orm.xml 新实体段**草案文本**（D1 定稿形态，本计划内）well-formed（`xmllint --noout` 通过）——live orm.xml 未改动

#### Phase 1 裁决记录（2026-08-16 执行）

**D1 成员实体设计（选项 A 通过）**：`ErpCrmTeamMember` 独立实体（tableName `erp_crm_team_member`，tagSet=gid,erp.crm，useLogicalDelete=true 对齐 ErpCrmTeam）——列集 {id(seq-default), teamId BIGINT mandatory, userId stdDomain=userId VARCHAR36 mandatory, remark} + 审计 6 列（delVersion/version/createdBy/createTime/updatedBy/updateTime），propId 1-10 连续分配（对齐平台 propId 连续校验）；UK `UK_CRM_TEAM_MEMBER_TEAM_USER`(teamId,userId)；to-one team（ErpCrmTeam）；indexes：IDX_CRM_TEAM_MEMBER_TEAM_ID + IDX_CRM_TEAM_MEMBER_USER_ID。**不设 orgId 冗余列**（成员归属经 team 解析，避免与 ErpCrmTeam.orgId 双写漂移）。**替代方案否决**：ErpCrmTeam 内嵌成员子表 JSON——JSON 列无法建 UK/无法被 dao 查询/无审计行级语义，违背「成员=userId 直挂 teamId 可查询可维护」；ErpCrmTeam 增 to-many members 反向关系——L1 无成员列表读契约需求，引擎经 resolver 直查，避免生成 to-many 无消费方死代码。**残留风险**：成员维护走标准 CRUD 生成（Non-Goal，无 AMIS 定制页面）；团队成员变更与在途分配无一致性事务（分配时点读成员快照，成员被删/改后下次分配自然生效）。**草案文本**（xmllint --noout 通过，well-formed，namespace 行为与 live orm.xml 一致）：

```xml
        <!-- 团队成员 -->
        <entity ext:web-renderer="flux" className="app.erp.crm.dao.entity.ErpCrmTeamMember"
                displayName="团队成员" ext:icon="user-plus" i18n-en:displayName="Team Member" name="app.erp.crm.dao.entity.ErpCrmTeamMember"
                tableName="erp_crm_team_member" tagSet="gid,erp.crm" registerShortName="true" useLogicalDelete="true" deleteFlagProp="delVersion" deleteVersionProp="delVersion" versionProp="version" createTimeProp="createTime" createrProp="createdBy" updateTimeProp="updateTime" updaterProp="updatedBy">
            <columns>
                <column name="id" displayName="ID" stdSqlType="BIGINT" primary="true" mandatory="true" code="ID" propId="1" tagSet="seq-default" stdDataType="long" i18n-en:displayName='ID' />
                <column name="teamId" displayName="销售团队" stdSqlType="BIGINT" mandatory="true" code="TEAM_ID" propId="2" stdDataType="long" i18n-en:displayName='Team' />
                <column name="userId" stdDomain="userId" displayName="成员用户" stdSqlType="VARCHAR" precision="36" mandatory="true" code="USER_ID" propId="3" stdDataType="string" i18n-en:displayName='User' />
                <column name="remark" displayName="备注" domain="remark" code="REMARK" propId="4" stdSqlType="VARCHAR" stdDataType="string" i18n-en:displayName='Remark' />
                <column name="delVersion" code="DEL_VERSION" displayName="逻辑删除版本" propId="5" domain="delVersion" stdSqlType="BIGINT" stdDataType="long" mandatory="true" defaultValue="0" i18n-en:displayName='Del Flag'  ui:show="R"/>
                <column name="version" code="VERSION" displayName="数据版本" propId="6" domain="version" stdSqlType="INTEGER" stdDataType="int" mandatory="true" defaultValue="0" i18n-en:displayName='Version' />
                <column name="createdBy" code="CREATED_BY" displayName="创建人" propId="7" domain="createdBy" stdSqlType="VARCHAR" precision="50" mandatory="true" stdDataType="string" i18n-en:displayName='Created By' />
                <column name="createTime" code="CREATE_TIME" displayName="创建时间" propId="8" domain="createTime" stdSqlType="TIMESTAMP" stdDataType="timestamp" mandatory="true" i18n-en:displayName='Create Time' />
                <column name="updatedBy" code="UPDATED_BY" displayName="修改人" propId="9" domain="updatedBy" stdSqlType="VARCHAR" precision="50" mandatory="true" stdDataType="string" i18n-en:displayName='Updated By' />
                <column name="updateTime" code="UPDATE_TIME" displayName="修改时间" propId="10" domain="updateTime" stdSqlType="TIMESTAMP" stdDataType="timestamp" mandatory="true" i18n-en:displayName='Update Time' />
            </columns>
            <relations>
                <to-one name="team" refEntityName="app.erp.crm.dao.entity.ErpCrmTeam" tagSet="pub" i18n-en:displayName='Team'><join><on leftProp="teamId" rightProp="id"/></join></to-one>
            </relations>
            <unique-keys>
                <unique-key name="UK_CRM_TEAM_MEMBER_TEAM_USER" columns="teamId,userId"/>
            </unique-keys>
            <indexes>
                <index name="IDX_CRM_TEAM_MEMBER_TEAM_ID" unique="false">
                    <column name="teamId"/>
                </index>
                <index name="IDX_CRM_TEAM_MEMBER_USER_ID" unique="false">
                    <column name="userId"/>
                </index>
            </indexes>
        </entity>
```

**D2 挑人算法语义（定稿，对齐 territory.md :158-160 字面）**：
- ROUND_ROBIN：`resolveTeamMemberUserIds(teamId)` 返回成员 userId 列表（按成员行 id 升序）→ `resolveLastAssignedOwner(teamId)` 查上次分配记录（lead.teamId=teamId 且 ownerId 非空，按 createTime desc limit 1）→ 上次 owner 在成员列表中定位 index，取 (index+1) mod size 的下一位；无历史记录或上次 owner 已不在列表 → 取第一位成员。
- LOAD_BALANCED：成员列表 → `countActiveLeadsByOwner(teamId)` 返回 Map<userId, count>（count lead.teamId=teamId 且 ownerId=member 且 docStatus 非 CONVERTED/LOST/CANCELLED 的活跃线索——计数口径定稿：**活跃 = docStatus NOT IN (CONVERTED, LOST, CANCELLED)**，对齐 lead 生命周期「终态不占负载」语义）→ 取 count 最少者（成员列表 id 升序序中首个最少的；无 count 记录成员按 0 计）。
- 成员列表为空 / resolver 不可用 / 查询异常 → 保持 MANUAL 降级 + degraded=true（既有语义零变化）。
- **替代方案否决**：ROUND_ROBIN 按 team 维度维护 lastAssignedOwner 独立计数器——需新增载体字段/实体，L1 无此契约；从 lead 历史记录推导（createTime desc）零 ORM 变更且确定性可测。

**D3 config 门控（定稿 TRUE）**：键名 `erp-crm.territory.assignment-method-enabled`（`ErpCrmConstants.CONFIG_TERRITORY_ASSIGNMENT_METHOD_ENABLED` + `ErpCrmConfigs.isAssignmentMethodEnabled()`）。**默认值裁决 = TRUE**：对齐 `erp-crm.territory.auto-assign-on-create` 默认 TRUE 先例（两者同为 territory 自动分配能力族，新实体落地后默认激活挑人符合 L1 三方法字面）；关闭（FALSE）= 既有 MANUAL 降级零回归（Phase 3 测试 ⑥ 覆盖）。**残留风险**：默认 TRUE 使既有 MANUAL 语义部署在成员数据存在时行为变化（有成员→挑人回写 ownerId，无成员→仍 MANUAL 降级）；既有 180 tests 中无成员数据的场景全部保持 MANUAL 降级（零回归由测试⑤/⑥ + 既有测试复跑保证）；owner doc 注记 config-gate 语义（关闭时 L1 三方法字面仅在部署启用后成立）。

**D4 引擎注入形态（选项 A 通过）**：`TerritoryAssignmentEngine` 新增函数式接口 `TeamMemberResolver`（`List<String> resolveTeamMemberUserIds(Long teamId, IServiceContext ctx)` + `String resolveLastAssignedOwner(Long teamId, IServiceContext ctx)` + `Map<String,Integer> countActiveLeadsByOwner(Long teamId, IServiceContext ctx)`）。**接线形态定稿 = BizModel 调用时传入 resolver**（非构造器注入 bean）：`assign(lead, rules, defaultRule, resolver)` 新重载 + 既有 `assign(lead, rules, defaultRule)` 委托 resolver=null（零变化）；beans.xml Engine bean 保持无参（零 IoC 变更面，无孤儿注册风险）；BizModel 内建 resolver lambda（daoProvider 实现）。**理由**：引擎纯函数式可测（Phase 3 测试直接 `new TerritoryAssignmentEngine()` + 假 resolver 断言挑人结果，无需容器）；config 门控在 BizModel 侧（config 关闭时传 null resolver → 引擎按 MANUAL 降级，门控逻辑零进引擎）；选项 B（BizModel 预解析传入）否决——引擎失挑人语义自含性，测试需在 BizModel 层做集成而非引擎单测，且两调用点（assignLead/defaultPrepareSave）重复解析逻辑。

### Phase 2 - ORM 实体落地 + 引擎/BizModel 实现 + config 门控（Add|Fix）

Status: completed
Targets: `module-crm/model/app-erp-crm.orm.xml`（ErpCrmTeamMember 实体段，**Phase 1 草案文本定稿后落盘**）；`module-crm/erp-crm-service/.../support/TerritoryAssignmentEngine.java`；`module-crm/erp-crm-service/.../entity/ErpCrmLeadBizModel.java`；`ErpCrmConstants.java`/`ErpCrmConfigs.java`；生成产物（`mvn clean install -DskipTests` 增量重生成）
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1

- [x] **双独立子 agent 批准 checkbox（ORM 保护区域，2026-08-15 升级）**：两个独立子 agent（fresh session）分别检查批准 Phase 1 草案实体设计（纯加性新实体 + UK，无既有语义变更/无删除/无迁移），批准记录落盘本计划（ses id + 结论）——**批准先于任何 live orm.xml 写入**（ai-autonomy-policy.md:79「两个批准均通过后才可实施」）
      - Skill: `nop-backend-dev`
- [x] `Add` orm.xml 新增 ErpCrmTeamMember 实体段（Phase 1 草案定稿 + 双 agent 批准后落盘）+ `mvn clean install -DskipTests` 增量重生成，生成产物核对（dao 实体 + DAO + IBiz + XMeta + DDL 三方言）
      - Skill: `nop-backend-dev`
- [x] `Add` TerritoryAssignmentEngine 注入面（D4 选项 A：TeamMemberResolver 接口）+ toResult 改造：ROUND_ROBIN/LOAD_BALANCED 经 resolver 挑人回写 ownerId，不可解析/无成员 → MANUAL 降级（degraded=true 保留）；MANUAL 路径零变化
      - Skill: `nop-backend-dev`
- [x] `Add` ErpCrmLeadBizModel 实现 TeamMemberResolver（daoProvider 查 ErpCrmTeamMember + ErpCrmLead 计数/上次分配）+ assignLead/defaultPrepareSave 传 resolver + config 门控（D3 定稿键，ErpCrmConstants.CONFIG_* + ErpCrmConfigs reader）
      - Skill: `nop-backend-dev`
- [x] `Add` beans.xml 更新（Engine bean 构造器参数注入 resolver 或保持 BizModel 侧传入——按 D4 定稿形态）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] orm.xml 实体 + 生成产物一致（生成类含 setUserId/getTeamId 等 + DDL 含新表）；propId 无冲突；live orm.xml 写入晚于双 agent 批准
- [x] 双独立子 agent 批准记录落盘（两个 APPROVE 结论 + 检查范围）
- [x] 引擎/BizModel 编译通过（`mvn compile -pl module-crm/erp-crm-service -am`）+ grep 证实 toResult 非 MANUAL 不再无条件降级（引擎 bean 无孤儿注册）

### Phase 3 - 测试 + 文档回填（Proof）

Status: completed
Targets: `module-crm/erp-crm-service/src/test/java/app/erp/crm/service/TestErpCrmTerritoryAssignment.java`（新增）；`docs/design/crm/territory.md`（§实现注记 2）；`docs/audits/arm-index.md`（P1-RC-036 行）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.57 行）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 2

- [x] `Proof` 新增 `TestErpCrmTerritoryAssignment`：①ROUND_ROBIN 轮流（seed 成员 A/B + 上次分配 owner=A → 返回 B）；②ROUND_ROBIN 无历史 → 第一位；③LOAD_BALANCED 最少线索（A=3/B=1 → B）；④LOAD_BALANCED 平手 → id 升序首个；⑤无成员 → MANUAL + degraded；⑥config 关闭 → MANUAL 降级；⑦assignLead 集成（GraphQL 调 assignLead → lead.ownerId 回写断言）；⑧defaultPrepareSave 自动分配路径；⑨`_cases/` 快照录制（对齐既有测试范式）
      - Skill: `nop-testing`
- [x] `Proof` 零回归验证：`mvn test -pl module-crm/erp-crm-service` 全绿（180 基线 + 新增）+ 快照重录核验 + 全仓 `mvn test` + `mvn clean install -DskipTests` 全量构建 BUILD SUCCESS
      - Skill: `nop-testing`
- [x] `Proof` compliance checker 复跑：`bash docs/audits/nop-compliance-checker.sh`——新增 daoFor 站点（如 BizModel resolver 内 daoFor(ErpCrmTeamMember.class)/daoFor(ErpCrmLead.class)）分类：同域 BizModel/engine 站点 per-site 证据登记 baseline-raise（对齐 R1.48/R1.51 先例）或零漂移
      - Skill: none
- [x] 文档回填：territory.md §实现注记 2 Deferred→已实现 + D1-D4 裁决注记 + config 门控语义；arm-index P1-RC-036 → done (RC-R1.57)；roadmap RC-R1.57 行 done ✅；`docs/logs/2026/08-16.md` 日志条目
      - Skill: none

Exit Criteria:

- [x] TestErpCrmTerritoryAssignment ①-⑨ 全绿（指定成功/失败模式：挑人结果断言逐项 + 快照落盘）
- [x] erp-crm-service 全量测试全绿（180 基线零回归，失败模式=任何既有测试翻红）
- [x] compliance checker actual ≤ baseline（或 baseline-raise 带 per-site 证据落 compliance-baseline.md）

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_ff63aeaaeffe7u6o8Gycg3KeyU`) — 1 BLOCKER + 1 MAJOR + 5 MINOR。B1 已修订：Phase 1 orm.xml 写入改为**计划内文本草案**（不落盘 live orm.xml）+ 双独立子 agent 批准 checkbox 移至 Phase 2 **首项**（批准先于任何 live orm.xml 写入，对齐 ai-autonomy-policy.md:79）；M1 已修订：全仓 `mvn test` 补入 Phase 3 验证项 + Closure Gates；m1 已修订：assignLead :136-158/ownerId :153-155/:233-235/dict :109-113 行号订正；m2 已修订：Phase 2 Exit Criteria R8 引用移除（引擎非 Processor，R8 不适用）；m3 已修订：baseline 增 product-scope 确认义务显式裁决声明（2026-08-12 A 类 + 2026-08-15 升级 → Q4=(a) 强制实现）；m4 已修订：Deferred「成员维护 UI」重复条目移除（保留 Non-Goals）；m5 已修订：Phase 1/2 orm.xml 拆分经 B1 修订显式化。
- Independent draft review iteration 2: `acceptable as-is` (`ses_ff6320d23ffetiCLIJLRP2qx2D`) — 迭代 1 全部 7 项修复逐项核实 FIXED（B1 Phase 1 文本草案 + Phase 2 批准先于写入 :63/:69/:84/:95/:97/:108；M1 全仓 mvn test :123/:144；m1 行号 :136-158/:153-155/:233-235/:109-113；m2 R8 移除 :110；m3 product-scope 裁决 :23；m4 Deferred 去重 :151-157；m5 拆分显式化）；独立基线 5 项全 PASS（toResult :73-84 降级 / ErpCrmTeamMember 零命中 / Lead 三列 propId / R2c=1420 / 180 tests）；格式合规 PASS，无反松弛词。3 项非阻塞 MINOR（Phase 2 批准 checkbox 与 Phase 3 文档回填项无逐项类型标签[阶段级已声明]、roadmap 头行号 :34→:31 漂移、Deferred 平手策略未命名重开触发条件[optimization candidate 可接受]）。草案审查收敛 → `Plan Status: draft → active`。

## Closure Gates

- [x] 范围内行为完成（三方法挑人 + 成员模型 + config 门控 + 回写链路）
- [x] 相关文档对齐（territory.md/arm-index/roadmap/logs）
- [x] 已运行验证（`mvn test -pl module-crm/erp-crm-service` + 全仓 `mvn test` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### LOAD_BALANCED 平手策略（id 升序首个）

- Classification: `optimization candidate`
- Why Not Blocking Closure: L1 未规定平手裁决策略，id 升序首个为确定性实现；更复杂策略（如随机/轮换平手）无契约支撑
- Successor Required: `no`

## Closure

Status Note: 执行已完成（Phase 1-3 全勾选 + 验证全绿 + 文档回填完整），独立结束审计通过，Plan Status 置 completed。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，本会话——执行者未自我审计）
- Evidence: 独立复核 live 仓库（非仅信计划文字）：
  - **ORM**：`module-crm/model/app-erp-crm.orm.xml:416-445` ErpCrmTeamMember 实体段置于 ErpCrmTeam（:385-413）之后——tableName `erp_crm_team_member`、propId 1-10 连续、列集 {id, teamId FK, userId stdDomain=userId, remark} + 审计 6 列、UK `UK_CRM_TEAM_MEMBER_TEAM_USER`(teamId,userId)（:435）、to-one team（:432）——与 D1 草案逐字一致。
  - **生成产物**：`erp-crm-dao/.../entity/ErpCrmTeamMember.java` + `_gen/_ErpCrmTeamMember.java`（getTeamId:464/setTeamId:472/getUserId:483/setUserId:491/getTeam:635 齐备）；DDL 三方言 `module-crm/deploy/sql/{mysql,oracle,postgresql}/_create_erp-crm.sql` 均含 `CREATE TABLE erp_crm_team_member`（:337）。
  - **引擎**：`TerritoryAssignmentEngine.java` 嵌套接口 `TeamMemberResolver`（:174-185）+ `assign(lead, rules, defaultRule, TeamMemberResolver, IServiceContext)` 重载（:58，既有 3 参委托 null resolver :50）；`toResult`（:91-131）非 MANUAL 经 resolver 挑人——ROUND_ROBIN→`pickRoundRobin`（:119/:136-144，无历史首位）、LOAD_BALANCED→`pickLoadBalanced`（:121/:149-162，最少者平手 id 升序首个）；仅 resolver null/无 team/成员空/查询异常时 `degradeToManual`（:164-168）——**非 MANUAL 非无条件降级**（grep 证实）。
  - **BizModel**：`ErpCrmLeadBizModel.java` `buildTeamMemberResolver()`（:312，config 关闭返回 null :313-314）、`teamMemberDao():377`/`leadDao():381` daoFor helper；`assignLead`（:150）与 `defaultPrepareSave`（:234）均传 resolver。
  - **Config**：`ErpCrmConstants.java:104` `CONFIG_TERRITORY_ASSIGNMENT_METHOD_ENABLED = "erp-crm.territory.assignment-method-enabled"`；`ErpCrmConfigs.java:83-84` `isAssignmentMethodEnabled()` 默认 `Boolean.TRUE`。
  - **测试**：`TestErpCrmTerritoryAssignment.java` 8 个 @Test（RR 轮流/无历史/LB 最少/LB 平手/降级/config 关闭/assignLead 集成/defaultPrepareSave）与 Phase 3 ①-⑧ 逐项对应；`_cases/.../TestErpCrmTerritoryAssignment/` 下 8 组 autotest.yaml + output（response.json5 + tables，含 erp_crm_team_member.csv）。
  - **文档回填**：`territory.md:224` §实现注记 2 标注「已实现，plan 2026-08-16-1634-1」+ D1-D4/config 门控语义；`arm-index.md:196` P1-RC-036 状态列「done (RC-R1.57)（2026-08-16 修复落地…」；`requirement-compliance-roadmap.md:449` RC-R1.57 行「done ✅」；`docs/logs/2026/08-16.md:3` RC-R1.57 条目置顶；`compliance-baseline.md:416-425` R2b/R2c baseline-raise 注记（plan 2026-08-16-1634-1）+ §BASELINE 机器可读块 R2b: 235 / R2c: 1422（:437-438）。
  - **验证复跑（本审计独立执行）**：`mvn test -pl module-crm/erp-crm-service` → **Tests run: 188, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS**（180 基线 + 8 新增零回归）；`bash docs/audits/nop-compliance-checker.sh` → **R2b=235 / R2c=1422**，全 19 规则 actual ≤ updated baseline（R1a=0/R1b=0/R1c=0/R1d=14/R2a=34/R2b=235/R2c=1422/R2d=35/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=9/R11=0/R12a=69/R12b=66/R12c=40），与日志/计划记录的 earlier 全量 `mvn test`（0 failures 0 errors）+ `mvn clean install -DskipTests` BUILD SUCCESS 证据一致。
  - **治理/一致性**：双独立子 agent 批准记录（Reviewer A/B 均 APPROVE 无强制修正）先于 live orm.xml 写入（Phase 2 首项 checkbox）；独立草案审查 2 轮收敛（iter-2 acceptable as-is）；Closure Gates 8/8 勾选；无范围内项目降级为 deferred/follow-up（Deferred But Adjudicated 仅 LOAD_BALANCED 平手策略 optimization candidate，successor no，已裁定）。

Follow-up:

- 无阻塞项。非阻塞：LOAD_BALANCED 平手策略（id 升序首个）为已裁定 optimization candidate（L1 未规定，无需 successor）；成员维护 UI 页面为 Non-Goal（标准 CRUD 生成）。
