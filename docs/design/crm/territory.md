# CRM 域 - 销售区域管理（Territory Management）

## 目的

设计销售区域管理体系：定义区域层级结构（region → area → branch → team），支持按地理/行业/客户规模分配线索与商机，设置区域销售配额（quota），提供区域维度的管道报表与预测数据分层。

## 边界

- 本模块负责：区域层级树管理、区域分配规则、销售配额管理、区域维度的管道聚合。
- 区域引擎是 CRM `ErpCrmLead` 与 `ErpCrmForecast` 的维度支撑——线索分配时按区域规则自动填充 `lead.territoryId`，预测按 `territoryId` 聚合。
- 本模块不负责：销售团队管理（`ErpCrmTeam` 已有）；线索主数据（`ErpCrmLead`）；预测计算（`sales-forecast.md`）；实际收入回写（sales 域）。
- 本模块实体**建议命名**，待 ORM 计划落地（`model/app-erp-crm.orm.xml` 是 ask-first 保护区域）。

## 设计依据

> 参考 **Odoo sales teams**（`crm_team` / `crm_team_member`）：团队按区域分层，线索自动按规则分配，团队 leader 可查看团队管道。但 Odoo 无独立区域（territory）实体，区域通过 team 层级隐式表达。
>
> 参考 **Salesforce territories**（`Territory` / `Territory2` / `Territory2Model`）：三层树形结构（`Territory2Model → Territory2 → Territory2`），支持按地理/行业/客户规模自动分配，配额（quota）按 territory 管理，管道报表按 territory 分层展示。Salesforce 有完整 territory 管理架构，包含 assignment rules 和 territory hierarchy。

## 实体清单

> 表前缀 `erp_crm_`、类名 `ErpCrm*`、字典 `erp-crm/*`。

### ErpCrmTerritory（销售区域节点）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/name/orgId | 标准 | — |
| parentId | 父区域（→ErpCrmTerritory，自引用树形结构） | 🟢 Salesforce `Territory2.parentId` |
| territoryType | dict `erp-crm/territory-type`：REGION（大区）/ AREA（地区）/ BRANCH（分公司）/ TEAM（团队） | 🟢 Salesforce `Territory2Model` |
| managerId | 区域负责人（→User） | 🟢 Odoo `crm_team.user_id` |
| description | 区域描述（覆盖范围说明） | — |
| fullPath | 路径冗余（如 "/华东/上海/浦东"，加速树形查询） | — |
| level | 层级深度（0=根，1=region, 2=area, 3=branch, 4=team） | — |
| isActive | 是否启用（停用的区域不再分配新线索） | 🟢 Salesforce `Territory2.isActive` |
| isLeaf | 是否叶子节点（分配规则通常落到叶子层级） | — |
| sortOrder | 同级排序 | — |
| 标准审计字段 | | |

**层级示例**：

```
华北（REGION）
  ├─ 北京（AREA）
  │   ├─ 北京分公司（BRANCH）
  │   ├─ 天津小组（TEAM）
  │   └─ ...
  ├─ 东北（AREA）
  │   └─ ...
  └─ ...

华东（REGION）
  ├─ 上海（AREA）
  │   ├─ 浦东小组（TEAM）
  │   └─ ...
  └─ ...
```

### ErpCrmTerritoryAssignmentRule（区域分配规则）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/orgId | 标准 | — |
| ruleName | 规则名称 | 🟢 Salesforce `AssignmentRule` |
| priority | 规则优先级（数字越小越优先） | — |
| territoryId | 目标区域（→ErpCrmTerritory） | — |
| conditionType | dict `erp-crm/assignment-condition-type`：GEOGRAPHY（按地理）/ INDUSTRY（按行业）/ CUSTOMER_SIZE（按客户规模）/ CUSTOM_FIELD（按自定义字段） | 🟢 Salesforce territory assignment rules |
| conditionValue | JSON 条件值（如 `{"province": ["上海", "浙江", "江苏"]}` 或 `{"industry": ["制造业", "金融"]}`） | — |
| assignmentMethod | dict `erp-crm/assignment-method`：ROUND_ROBIN（轮流分配）/ LOAD_BALANCED（按负载）/ MANUAL（手动） | 🟢 Odoo 线索分配规则 |
| groupId | 目标团队（→ErpCrmTeam，与 territoryId 配合，分配线索到区域内的特定团队） | — |
| isDefault | 是否默认规则（无匹配时使用） | — |
| isActive | 是否启用 | — |
| 标准审计字段 | | |

**预置条件类型说明**：

| conditionType | conditionValue 示例 | 匹配逻辑 |
|---------------|---------------------|----------|
| GEOGRAPHY | `{"province": ["上海", "浙江"]}` | 线索 `companyName` 所属省/市匹配 |
| INDUSTRY | `{"industryCode": ["manufacturing", "finance"]}` | 线索 `industry` 匹配（字典编码） |
| CUSTOMER_SIZE | `{"minEmployees": 100, "maxEmployees": null}` | 线索 `companySize` 在范围内 |
| CUSTOM_FIELD | `{"sourceId": "WECHAT_ADS"}` | 线索任意字段值匹配 |

### ErpCrmQuota（销售配额）

按区域 + 期间 + 币种的配额行。一个区域一个期间可有多个配额行（按不同维度拆解）。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/orgId | 标准 | — |
| territoryId | 区域（→ErpCrmTerritory，可空，空表示公司级配额） | 🟢 Salesforce `Quota.territoryId` |
| teamId | 团队（→ErpCrmTeam，可空） | — |
| ownerId | 销售员（→User，可空。空表示区域/团队配额汇总行） | 🟢 Salesforce `Quota.userId` |
| periodType | dict `erp-crm/quota-period-type`：ANNUAL（年度）/ QUARTERLY（季度）/ MONTHLY（月度） | — |
| fiscalYear | 财年（如 2026） | — |
| periodLabel | 期间标签（如 "2026-Q3"、"2026-07"） | — |
| quotaAmount | 配额金额（目标收入） | 🟢 Salesforce `Quota.amount` |
| currencyId | 币种 | — |
| isFinalized | 是否已定稿（定稿后不可修改配额值） | — |
| notes | 配额说明或调整理由 | — |
| 标准审计字段 | | |

### 配额层级汇总

```
公司配额（territoryId=null, teamId=null, ownerId=null）
     ↑  Σ
区域配额（territoryId=华东, teamId=null, ownerId=null）
     ↑  Σ
团队配额（territoryId=华东, teamId=销售1组, ownerId=null）
     ↑  Σ
个人配额（territoryId=华东, teamId=销售1组, ownerId=张三）
```

- 层级配额通过聚合自动得出，但管理员可直接为各层级写入显式配额值（覆盖聚合值）。
- 聚合规则：子节点配额求和（显式值优先，无显式值则向下聚合）。

## 业务规则

1. **区域树**：`ErpCrmTerritory` 自引用树形结构。最大深度 4 层（REGION → AREA → BRANCH → TEAM）。删除节点时禁止删除有子节点的父节点。
2. **线索自动分配**：线索创建时未指定 `ownerId`/`teamId`：

   ```
   线索创建（未指派 owner）
      │
      ├─ 按优先级遍历 ErpCrmTerritoryAssignmentRule（isActive=true）
      │     └─ conditionType 匹配线索 → 分配 territoryId/teamId
      │
      ├─ 无规则匹配 → 使用 isDefault=true 的规则
      │
      └─ 仍无匹配 → 线索 territoryId 留空，标记"未分配"
   ```

   - ROUND_ROBIN：轮流分给团队内成员
   - LOAD_BALANCED：分给当前线索最少的成员
   - 分配后记录 `lead.teamId` / `lead.ownerId` / `lead.territoryId`

3. **配额与预测的关系**：
   - 配额（quota）是目标，预测（forecast）是预计。两套独立数据，但在报表中同屏对比。
   - 区域管道报表：`ErpCrmForecast` 按 `territoryId` 聚合，与 `ErpCrmQuota` 同屏展示（实际 / 预测 / 目标）。
4. **配额期间**：年度配额可自动按季度或月度均分（或管理员手动调整各季度值）。
5. **定稿锁定**：`isFinalized=true` 后配额不可修改（冲销需先解冻）。
6. **层次隔离**：一个线索只有一个 `territoryId`（分配落到叶子节点）。上级区域的管道 = 子区域管道聚合（`sales-forecast.md` §规则 3 层级聚合）。
7. **区域停用**：`isActive=false` 的 `ErpCrmTerritory` 不再参与分配规则匹配，已有线索保持历史 territoryId 不变。

### 分配执行流程

```
线索创建 / 重新分配（管理员手动触发）
    │
    ├─ 从线索提取分配字段（province / industry / companySize / sourceId）
    │
    ├─ 按 priority 排序规则列表
    │
    ├─ 遍历规则，找到首个 conditionValue 匹配的规则
    │
    ├─ 按 assignmentMethod 分配：
    │     ROUND_ROBIN → 查 teamId 成员列表，取上次分配的下一位
    │     LOAD_BALANCED → 查 teamId 成员当前线索数，分给最少的
    │     MANUAL → 标记待分配，推送给管理员
    │
    └─ 回写 lead.territoryId / lead.teamId / lead.ownerId
```

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-crm.territory.auto-assign-on-create` | true | 线索创建时是否自动分配区域 |
| `erp-crm.territory.default-team-id` | — | 无匹配规则时的默认团队 |
| `erp-crm.territory.max-depth` | 4 | 区域树最大层级 |

## 状态机关联

区域管理不引入独立状态机。线索的 `territoryId` 分配发生在创建时刻（或管理员手动重新分配），不影响线索的 `docStatus` 迁移。

## 反模式警示

- ⛔ **将区域层级硬编码在团队名/部门结构中**——`ErpCrmTerritory` 是独立树结构，与 HR 组织架构分离（销售区域 ≠ 公司组织部门）。
- ⛔ **区域分配规则硬编码 Java**——conditionType + conditionValue 通过配置表管理，新增分配规则零改代码。
- ⛔ **配额与预测用同一张表**——配额（目标）与预测（预计）语义不同，独立实体避免混淆。
- ⛔ **线索分配时使用同步循环**——分配规则匹配可能涉及多条规则遍历，若数量大时改为异步批量分配。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| CRM（ErpCrmLead） | 分配后写 lead.territoryId/teamId/ownerId |
| CRM（ErpCrmForecast） | 预测按 territoryId 层级聚合 |
| CRM（ErpCrmTeam） | 区域关联团队（分配规则落 team） |
| master-data（ErpMdPartner） | 客户地理/行业字段作为分配规则匹配源 |

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| 区域层级树（territory hierarchy） | 🟢 | Salesforce `Territory2` 和 `Territory2Model`（4 层 ≤10k 节点）；Odoo `crm_team` 单层 |
| 区域分配规则（按地理/行业/客户规模） | 🟢 | Salesforce territory assignment rules（条件映射 + 优先级） |
| 轮询/负载分配（ROUND_ROBIN / LOAD_BALANCED） | 🟢 | Odoo `crm.lead` 分配规则（`assign_method` = round_robin / evenly） |
| 配额（quota）按区域管理 | 🟢 | Salesforce `Quota`（territoryId + userId + 期间 + 金额） |
| 配额层级汇总 | 🟢 | Salesforce 配额层级汇总模型 |
| 区域管道报表 | 🟢 | Salesforce territory forecast 管线 |
| 默认规则 + 优先级 | 🟡 | Odoo `crm_team.assignment_rules` JSON 字段 |
| 自定义字段分配 | ⚪ | 本项目设计意图，进阶配置项 |

## 参考

- `sales-forecast.md`（预测按 territory 聚合）
- `README.md` §ErpCrmTeam（销售团队实体）
- `README.md` §ErpCrmLead（区域分配回写字段）
- `state-machine.md` §Lead（分配不影响状态机）
- `use-cases.md` §UC-CRM-01（线索创建与分配衔接）
- `docs/analysis/erp-survey/` — Salesforce/Odoo territory 机制分析
