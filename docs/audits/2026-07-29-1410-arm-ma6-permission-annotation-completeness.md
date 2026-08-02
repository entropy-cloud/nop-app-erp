# ARM-MA6 权限注解完整性审计报告（A6.1）

> Audit Status: closed
> 里程碑：MA6（安全与权限层审计）
> 维度：权限注解完整性 grep（全域 19 模块）
> Skill：`open-ended-audit-prompt.md`（grep 完整性需主动搜索未知缺口）
> Owner Doc：`docs/design/roles-and-permissions.md`（角色体系 + 高危操作 + 角色→权限点映射 + 运行基线）
> Plan：`docs/plans/2026-07-29-1410-1-ma6-permission-and-data-auth-audit.md`（Phase 1）
> 审计日期：2026-07-29
> 审计锚点：HEAD 0e963531d（M0 锚点，与 MA1-MA5 同基线）

## 1. 审计对象与范围

按 `open-ended-audit-prompt.md` 对全域 19 模块做**权限注解完整性 grep 审计**——核实 `@BizMutation`/`@BizQuery`/敏感动作是否携带任何角色/权限绑定 + FNPT 权限点（`{EntityName}:{query,mutation}`）对 owner doc 高危操作表的覆盖关系 + `enable-action-auth=false` 默认关闭对"已定义权限点但不生效"的影响范围 + 与 P1-MA3-046（A3.6 全域敏感动作零运行时权限保护）协同裁决（合并/分裂/升降级）。

**审计对象是权限注解完整性的项目级证据**，不是单个工件。输入包括 owner doc（角色矩阵 + 高危操作表 + FNPT 映射 + 运行基线节）、19 模块 BizModel/Processor Java 源码、38 个 `_erp-*.action-auth.xml` + `app.action-auth.xml`、`application.yaml`、既有审计结论（P1-MA3-046 / P1-MA2-093）。

## 2. grep 基线（2026-07-29 实仓）

| grep 项 | 命令 | 计数 | 裁决 |
|---------|------|------|------|
| `@BizMutation` 注解 | `rg -c '@BizMutation' module-*/erp-*-{service,dao}/src/main/java` | **825** | 全域 mutation 入口 |
| `@BizQuery` 注解 | `rg -c '@BizQuery' module-*/erp-*-{service,dao}/src/main/java` | **313** | 全域 query 入口 |
| `@BizAuth` / `@Auth` / `permissionName` / `@RolesAllowed` / `@PreAuthorize` / `@Secured` | `rg '@BizAuth\|@Auth\b\|permissionName\|@RolesAllowed\|@PreAuthorize\|@Secured'` | **0** | **零角色/权限绑定注解** |
| xbiz `<auth>` 元素 | `rg '<auth>' module-*/erp-*-service/src/main/resources` | **0** | xbiz 层零权限声明 |
| AMIS `permission:` 属性 | （A3.6 已确认 = 0） | **0** | 前端零权限绑定 |
| 自定义 `IDataAuthChecker` / `IQueryTransformer` | `rg -l 'IDataAuthChecker\|IQueryTransformer' module-*/erp-*-service/src/main/java` | **0** | 零自定义数据权限组件 |
| FNPT 权限点总数 | `rg -c 'resourceType="FNPT"' module-*/erp-*-web/src/main/resources` | **706** | codegen 产出三层（TOPM/SUBM/FNPT）齐全 |
| FNPT 动作后缀（去重） | `rg -o 'FNPT:[A-Za-z]+:[a-zA-Z]+' ... \| sed 's/.*://' \| sort -u` | **3 类**：`query` / `mutation` / `inbox` | 仅 query/mutation 两态 + notify 专属 inbox |
| `nop_auth_role_resource` 种子 | `rg -l 'nop_auth_role_resource\|role_resource' --glob '*.{sql,csv,yaml,yml,xml}'` | **0** | 零角色-资源授权种子 |
| `enable-action-auth` / `skip-check-for-admin` in application.yaml | `rg 'enable-action-auth\|skip-check-for-admin' app-erp-all/.../application.yaml` | **未显式覆盖**（用平台默认 `false` / `true`） | 操作级拦截默认关闭 |

**注解形态矩阵**：825 `@BizMutation` + 313 `@BizQuery` = **1138 个动作注解，100% 裸注解**（无任何角色/权限绑定）。与 plan Current Baseline 声明（~825 / ~313）一致——EXECUTE 起始重 grep 取权威值确认无漂移。

## 3. FNPT 权限点对高危操作表的覆盖关系

owner doc `roles-and-permissions.md §高危操作权限` 列出 10 类高危操作（反审核/作废/反结账/资产处置/工单关闭/强制齐套开工/让步接收/负库存放行/红冲凭证/批量折旧）。核实这些高危动作在 FNPT 权限点模型中的归属：

**关键发现——FNPT 动作后缀仅 3 类（query/mutation/inbox），高危操作全部坍缩进泛化 `{EntityName}:mutation` 单点**：

| owner doc 高危操作 | BizModel/Processor 方法 | FNPT 权限点归属 | 细粒度缺口 |
|-------------------|------------------------|----------------|-----------|
| 反审核已审核单据 | `reverseApprove`（全域 Processor） | `{Entity}:mutation`（泛化） | **无独立 `:reverseApprove` FNPT** |
| 作废已审核单据 | `cancel`（全域 Processor） | `{Entity}:mutation`（泛化） | **无独立 `:cancel` FNPT** |
| 反结账会计期间 | `ErpFinAccountingPeriodProcessor.reverseClose` | `ErpFinAccountingPeriod:mutation` | **无独立 `:reverseClose` FNPT** |
| 资产报废/出售处置 | `ErpAstDisposalProcessor.approve` | `ErpAstDisposal:mutation` | **无独立 `:dispose` FNPT** |
| 工单关闭（部分完工） | `ErpMfgWorkOrderProcessor.close` | `ErpMfgWorkOrder:mutation` | **无独立 `:close` FNPT** |
| 强制部分齐套开工 | `ErpMfgWorkOrderProcessor.start` | `ErpMfgWorkOrder:mutation` | **无独立 `:forceStart` FNPT** |
| 让步接收（降级） | `ErpQaNcr*Processor` | `ErpQaNcr:mutation` | **无独立 `:concession` FNPT** |
| 红冲已过账凭证 | `ErpFinVoucherBizModel.reverseVoucher` | `ErpFinVoucher:mutation` | **无独立 `:reverseVoucher` FNPT** |
| 期末批量折旧 | `ErpAstDepreciationScheduleProcessor.executeBatchDepreciation` | `ErpAstDepreciationSchedule:mutation` | **无独立 `:batchDepreciation` FNPT** |

**裁决**：706 个 FNPT 点全部是 codegen 按 `{EntityName}:{query,mutation}` 双点模式机械产出（notify 例外加 inbox），**无任何高危操作拥有独立细粒度 FNPT**。owner doc `roles-and-permissions.md §角色→权限点映射` 明确声明「FNPT 权限点模式：每实体约 2 个（query/mutation），冒号后列出的动作（如 save/update/submitForApproval）是 BizModel 业务方法名，均归入 mutation 权限点，**不是**独立的 FNPT 权限点 ID」——即当前 FNPT 模型**设计上**就是粗粒度（query/mutation 二分），高危操作与普通 CRUD 共享同一 `mutation` 点。

**影响**：即使灰度启用 `enable-action-auth=true`，按当前 FNPT 模型只能做到「实体级 mutation 开关」，无法对高危操作（如反结账/红冲/处置）单独授权或灰度。owner doc §运行基线「灰度启用步骤 4」建议「先对高危操作开启，再逐步铺开」——**当前 FNPT 模型不支持此灰度策略**（高危操作无独立 FNPT 点可单独开启）。

## 4. `enable-action-auth=false` 默认关闭的影响范围评估

owner doc `roles-and-permissions.md §运行基线` 显式声明此为**有意默认**（灰度推进）。评估（plan Non-Goal：仅评估影响范围，不做启用/灰度决策）：

- **已定义但不生效**：706 FNPT 点 + 38 action-auth.xml 文件链已就绪，但 `enable-action-auth=false` 致运行时不拦截——任何认证用户可执行任意 mutation/query。**仅 HTTP 登录屏障**（平台层认证默认开启）。
- **保护性默认方向**：`false` 是「错误方向偏保守」（关闭拦截比误拦截安全）——与 P1-MA3-046 降级 P1 非 P0 的理由一致。
- **残留风险**：单组织种子 + 零角色-resource 种子下无活跃数据破坏；多组织部署一旦开启且未先填充角色-resource 种子，FNPT 点仍不生效（因无授权数据）。**风险面 = 已定义权限点的"虚存在"**（文件在但不生效），非"错误拦截"。

**评估结论**：`false` 默认对当前基线（单组织 + admin skip-check）无残留活跃风险；风险仅在「多组织部署 + 未先配角色-resource 即开启 action-auth」时显现为「FNPT 点存在但仍全放行」。属 P1-MA3-046 修复时一并处置（填充角色-resource 种子 + 灰度启用），**非独立新 finding**。

## 5. 与 P1-MA3-046 协同裁决

P1-MA3-046（A3.6）已登记「全域敏感动作零运行时权限保护」——`@BizAuth`=0 + xbiz `<auth>`=0 + `enable-action-auth=false` + 19 data-auth.xml 全空 + 零角色-resource 种子。本审计（A6.1）是其**深度核实 + FNPT 粒度纵深**：

| 裁决项 | 结论 | 理由 |
|--------|------|------|
| A6.1 注解完整性基线 | **确认 P1-MA3-046 基线**（825 mutation + 313 query = 1138 裸注解，零绑定） | A3.6 已汇总（380 BizModel + 198 Processor + 704 xbiz），A6.1 重 grep 取权威值（825/313）确认无漂移 |
| FNPT 粒度缺口（高危操作坍缩进泛化 mutation） | **合并入 P1-MA3-046 修复范围**（非独立新 P1） | P1-MA3-046 推荐修复方案 A 已明示「补 per-action FNPT 至 `_erp-*.action-auth.xml`」——本审计将其细化为「高危操作需独立 FNPT 点（如 `:reverseClose`/`:reverseVoucher`/`:dispose`）以支持 owner doc §灰度启用步骤 4 的「先高危后铺开」策略」。属同一修复面的粒度细化，不分裂为独立 finding |
| 升降级 | **维持 P1 不升 P0** | 同 P1-MA3-046 降级理由——owner doc 显式声明有意默认 + 单组织种子无活跃数据破坏 + 平台 HTTP 认证默认开启 + `enable-action-auth=false` 保护性默认 |

**协同裁决结论**：**合并为单一 MR 项**（P1-MA3-046 吸纳 A6.1 的 FNPT 粒度细化要求）。A6.1 不产生独立新 P1——本审计的价值是（a）以权威 grep 值确认 P1-MA3-046 基线无漂移；（b）细化 FNPT 粒度缺口（高危操作无独立 FNPT 点致 owner doc §灰度启用步骤 4 不可行），为 MR2 P1-MA3-046 修复提供精确的 per-action FNPT 建模清单。

## 6. Verdict

**passes open-ended audit（确认型审计，零新 P1）**。全域权限注解完整性经 grep 系统性核实：1138 动作注解 100% 裸注解（零角色/权限绑定）+ 零 xbiz `<auth>` + 零角色-resource 种子 + `enable-action-auth=false`——与 P1-MA3-046 基线完全一致，无漂移。本审计唯一纵深发现（FNPT 粒度缺口——高危操作坍缩进泛化 `{Entity}:mutation`）合并入 P1-MA3-046 修复范围，不分裂为独立 finding。

## 7. 剩余未知数（watch-only）

1. **FNPT `inbox` 后缀仅 notify 域**：`ErpSysNotification:inbox-{query,mutation}` 是通知收件箱专属，非通用动作后缀。当前不影响高危操作授权，但 MR2 per-action FNPT 建模时须保留此特例。
2. **`nop.auth.skip-check-for-admin=true`**：管理员跳过权限检查——即使灰度启用 action-auth，管理员仍全放行。owner doc §运行基线已声明，属设计选择非缺陷。多组织部署时管理员范围界定须人工决策（successor）。

## 8. 与 MA1-MA5 + A2.18 + A3.6 交叉去重

- **A6.1 ↔ P1-MA3-046**：合并（见 §5）。A6.1 是 P1-MA3-046 的 grep 深度核实 + FNPT 粒度细化，不重复登记。
- **A6.1 ≠ P1-MA2-093/094**：后者是 orgId 多公司数据级行过滤（data-level row filter by orgId），本审计是 action-level 权限注解 + FNPT 模型（不同层级）。
- **A6.1 ≠ P1-MA3-008/010/012**：后者是**文档层**角色定义/审计描述问题，本审计是**代码层**动作权限注解 + FNPT 粒度（已在 A3.6 去重说明中确立）。

## 9. audit 关闭条件

本报告产出 + arm-index §报告清单登记本报告 + arm-index §P1 详细清单 P1-MA3-046 行补注「A6.1 深度核实 + FNPT 粒度细化合并」+ roadmap A6.1 推进 `todo → ready`。
