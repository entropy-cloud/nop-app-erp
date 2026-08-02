# ARM-MA6 数据权限运行验证审计报告（A6.3）

> Audit Status: closed
> 里程碑：MA6（安全与权限层审计）
> 维度：数据权限运行验证（19 模块 data-auth.xml + 角色侧行级过滤声明-落地差距 + A2.18 orgId 读路径复核）
> Skill：`multi-dimensional-audit-prompt.md`（跨多维度挑战——声明-落地差距/替代机制/角色侧行过滤/A2.18 互补/dashboard 直访）
> Owner Docs：`docs/design/roles-and-permissions.md §数据权限` + `docs/architecture/multi-company.md`（A2.18 复核）
> Plan：`docs/plans/2026-07-29-1410-1-ma6-permission-and-data-auth-audit.md`（Phase 3）
> 审计日期：2026-07-29
> 审计锚点：HEAD 0e963531d（M0 锚点）

## 1. 审计对象与范围

按 `multi-dimensional-audit-prompt.md` 对 19 模块做**数据权限运行验证**——`data-auth.xml` 规则落地 + `IDataAuthChecker`/`IQueryTransformer` 缺位 + 角色侧行级过滤（部门/创建人/分配人可见）声明-落地差距 + 复核 A2.18 orgId 读路径隔离结论（P1-MA2-093）。**聚焦角色侧行级过滤维度，与 A2.18 多公司 orgId 维度互补不重复**（plan Non-Goals 明示）。

## 2. 维度裁决

### 维度 1：`data-auth.xml` 全空对 owner doc §数据权限 声明的运行时影响

owner doc `roles-and-permissions.md §数据权限` 声明 4 类行级过滤规则。核实运行时落地：

**核实**：19 模块 `erp-{module}.data-auth.xml` 全部为空：

```xml
<!-- module-finance/erp-fin-service/.../erp-fin.data-auth.xml（全域 19 模块同型）-->
<data-auth x:schema="/nop/schema/data-auth.xdef" xmlns:x="/nop/schema/xdsl.xdef">
    <objs/>
</data-auth>
```

- 19 个 app 级 `app.data-auth.xml` 仅 `<x:gen-extends>` 引用 `auth-gen:GenFromModules`——聚合 19 个空 `<objs/>` 模块 → **空聚合**（运行时 zero 规则）。
- 平台机制：`CrudBizModel.prepareFindPageQuery` 经 `AuthHelper.appendFilter` → `DefaultDataAuthChecker.getFilter` 在无 `<obj>` 规则时**返回 null**（不追加任何过滤）。

**VERDICT: owner doc §数据权限 4 类行级过滤运行时完全不生效**：

| owner doc §数据权限 声明 | 运行时落地 | 裁决 |
|------------------------|-----------|------|
| 「业务员只能看自己创建的单据（可配置为部门可见）」 | data-auth.xml `<objs/>` 空 → `getFilter` 返回 null → **无 createdById 过滤** | **完全不生效** |
| 「财务员可见所有财务相关单据与凭证」 | 同上 → 财务员无特殊作用域（与业务员同视野） | **完全不生效**（实际「全见」是因「无人有过滤」而非「财务员显式全见」） |
| 「质检员只能看分配给自己的质检任务」 | 同上 → **无 assigneeId 过滤** | **完全不生效** |
| 「维护人员只能看分配给自己的维护访问」 | 同上 → **无 assigneeId 过滤** | **完全不生效** |

→ 见 §3 P1-MA6-002（角色侧行级过滤声明-落地差距）。

### 维度 2：零 `IDataAuthChecker`/`IQueryTransformer` 是否有替代机制兜底

核实是否存在 BizModel 内 QueryTransformer 手拼条件 / IContext 取 userId 过滤等替代机制兜底声明能力：

**核实**：
- 自定义 `IDataAuthChecker` / `IQueryTransformer` 实现类：`rg -l 'IDataAuthChecker|IQueryTransformer' module-*/erp-*-service/src/main/java` = **0**（零注册）。
- `IServiceContext`/`IContext`：**均无 `getOrgId()`**（A2.18 已确认）。`getUserId()` 存在但 BizModel 查询路径**未**用它做行级过滤（grep `getCreatedBy`/userId 过滤 in @BizQuery = 0，与 A6.2 维度 1 同源证据）。
- BizModel 查询路径：5 域（fin/pur/sal/inv/mfg）@BizQuery 抽样（A2.18 已做）全部无 createdById/assigneeId/orgId 手拼过滤——**零替代机制兜底**。

**VERDICT: 无替代机制**。owner doc §数据权限 声明的行级过滤能力**既无 data-auth.xml 规则、亦无代码层手拼兜底**——声明能力运行时彻底未落地。

### 维度 3：角色侧行级过滤声明-落地差距矩阵

| owner doc 声明的可见规则 | 期望过滤列 | data-auth.xml 规则 | 代码兜底 | 落地状态 |
|------------------------|----------|-------------------|---------|---------|
| 自己创建的单据 | `createdById == currentUser` | 无（`<objs/>`） | 无（@BizQuery 无手拼） | **❌ 未落地** |
| 部门可见 | `deptId IN currentUserDeptTree` | 无 | 无 | **❌ 未落地** |
| 财务全见 | （财务员角色无过滤） | 无 | 无 | **❌ 未落地**（实际全见是因无人有过滤，非角色级配置） |
| 分配给自己（质检/维护） | `assigneeId == currentUser` | 无 | 无 | **❌ 未落地** |

**4 类规则 0 落地**。→ P1-MA6-002。

### 维度 4：复核 A2.18 orgId 读路径隔离结论（P1-MA2-093）+ 角色侧行级过滤互补

A2.18（P1-MA2-093）结论：orgId 多公司数据级行过滤**全仓未落地**（平台仅 tenant 自动过滤，本项目 0 实体启用 useTenant + 19 data-auth.xml 全空 + 0 IDataAuthChecker + 无 getOrgId API + 11 dashboard 直访绕过认证管道 + 单组织种子 176 行全 orgId=2 掩盖泄漏）。

**复核结论**：
- A2.18 结论**维持，无升级/降级**——本审计重 grep 确认 19 data-auth.xml 仍全 `<objs/>` + 0 IDataAuthChecker/IQueryTransformer + IServiceContext/IContext 无 getOrgId（与 A2.18 完全一致）。
- **维度互补不重复**：A2.18 聚焦 **orgId 多公司维度**（owner doc `multi-company.md:29`「所有业务单据按 orgId 隔离查询」）；A6.3 聚焦 **角色侧行级过滤维度**（owner doc `roles-and-permissions.md §数据权限`「业务员只看自己创建」「质检员只看分配给自己」）。两者根因相邻（数据权限管道全空）但**声明来源不同 + 过滤维度不同**（orgId 跨组织 vs createdById/assigneeId 角色内）——P1-MA2-093 与 P1-MA6-002 **不重叠**。

### 维度 5：dashboard BizModel 直访路径是否绕过（空）数据权限管道

A2.18 已识别 11 个 dashboard BizModel 经 `IDaoProvider` 直访 `daoProvider.daoFor(...).findAllByQuery(q)` 绕过仅有的（空）认证管道。

**复核确认**：11 dashboard 直访路径**维持原状**（A2.18 已登记 P1-MA2-093 + P1-MA2-095）。本审计补充视角：因 data-auth.xml 全空，**即使 dashboard 走 CrudBizModel 标准查询管道也不会有行级过滤**——dashboard 直访的额外风险是「绕过未来 data-auth 规则填充后的管道」，而非「绕过当前（空）管道」。当前基线下 dashboard 直访与标准查询的行级过滤效果**等价（均为零）**。不分裂独立 finding（归 P1-MA2-093 修复时一并评估 dashboard 走标准管道）。

### 维度 6：owner-doc 对齐 / 架构边界

- owner doc `roles-and-permissions.md §设计能力基线` 声明「数据权限规则：data-auth.xml 行级过滤——**独立于操作级开关，始终附加到查询条件**」——此声明与运行时（data-auth.xml 全空 → 不附加任何条件）**直接矛盾**。owner doc 把 data-auth.xml 描述为「已沉淀，始终生效」的能力，但实际规则集为空。
- 架构边界：填充 data-auth.xml 规则属配置层变更（不改 ORM/契约），但需 `IUserContext` 暴露 createdById/assigneeId/deptId 上下文——当前 `IContext` 仅有 userId/orgId（orgId 还不在标准 context）。属补能力，MR3 裁决。

### 维度 7：回归风险

填充角色侧行级过滤若全局开启，可能导致现有测试/报表因「 suddenly 过滤」而断（如财务员原本全见，开启后若规则配错变窄视野）。MR3 修复须配灰度（先某角色/某域）+ 负向测试（A 角色查 B 角色数据断言空）。

## 3. 新增 P1 清单

### P1-MA6-002 角色侧行级过滤声明-落地差距（4 类规则 0 落地）

- **域**：全域 19 模块（owner doc §数据权限 声明覆盖业务员/财务员/质检员/维护人员等多角色）
- **根因**：owner doc `roles-and-permissions.md §数据权限` 声明 4 类行级过滤规则（自己创建/部门可见/财务全见/分配给自己），但 19 模块 `erp-{module}.data-auth.xml` **全部** `<objs/>` 空规则 + 0 自定义 `IDataAuthChecker`/`IQueryTransformer` + BizModel @BizQuery 零 createdById/assigneeId 手拼兜底——**声明能力运行时彻底未落地**。owner doc §设计能力基线 进一步声明 data-auth「独立于操作级开关，始终附加到查询条件」——与运行时（不附加任何条件）直接矛盾。
- **影响**：业务员可见所有单据（非仅自己创建）；质检员可见所有质检任务（非仅分配给自己）——行级隔离声明失效。单组织种子 + admin skip-check 下不显现；多角色用户部署即触发越权读。
- **P1 非 P0**：(1) owner doc §数据权限 部分规则标「可配置」（软门控）；(2) 单组织种子无活跃数据破坏；(3) 行级过滤是补能力（须 IUserContext 暴露上下文 + 填充规则），与 P1-MA2-093 同型「能力缺失非活跃缺陷」。
- **与 P1-MA2-093 互补不重复**：P1-MA2-093 是 orgId 多公司维度（multi-company.md 声明）；P1-MA6-002 是角色侧 createdById/assigneeId/deptId 维度（roles-and-permissions.md §数据权限 声明）——不同声明来源 + 不同过滤列。
- **修复方式**：MR3 裁决（与 P1-MA2-093 协同）——方案 A（推荐）填充 19 `erp-{module}.data-auth.xml` 的 `<objs>` 规则（按角色 × bizObj 配 `eq("createdById", $userId)` / `eq("assigneeId", $userId)` / dept tree 过滤）+ `IUserContext` 暴露 deptId/createdById 上下文 + 灰度（先某角色/某域）+ 负向隔离测试；方案 B 补全局 `IQueryTransformer`（`@Named("nopGlobalQueryTransformer")`）按 bizObj + 角色自动追加行级过滤；方案 C owner doc §数据权限 标注「行级过滤为 successor，当前基线全角色同视野（仅 HTTP 认证 + orgId 待 P1-MA2-093 修复）」。
- **目标 MR**：MR3。

## 4. 与 A2.18 / P1-MA3-046 协同去重

- **P1-MA6-002 ↔ P1-MA2-093**：**互补不重复**（见维度 4）——P1-MA2-093 orgId 多公司维度 / P1-MA6-002 角色侧 createdById+assigneeId+deptId 维度。MR3 协同修复（同为 data-auth.xml 规则填充 + IUserContext 上下文扩展）。
- **P1-MA6-002 ≠ P1-MA3-046**：P1-MA3-046 是 action-level 权限注解 + FNPT enforcement（谁能调动作）；P1-MA6-002 是 data-level 行级过滤（查询返回哪些行）——不同层级。P1-MA3-046 已涵盖「data-auth.xml 全空」作为其 enforcement 缺失的证据之一，但 P1-MA6-002 聚焦**角色侧行级过滤声明-落地差距**（独立维度）。
- **dashboard 直访 → P1-MA2-093**：11 dashboard 直访维持 A2.18 登记，不分裂。

## 5. Verdict

**passes multi-dimensional audit（⚠️ P1，零 P0）**。数据权限运行验证产出 **1 项新 P1**（P1-MA6-002 角色侧行级过滤 4 类规则 0 落地）+ 复核确认 A2.18（P1-MA2-093 orgId 读路径隔离）结论维持无升级。角色侧行级过滤是与 A2.18 orgId 维度互补的独立数据权限维度，声明-落地差距系统性。

## 6. 剩余未知数（watch-only）

1. **dept tree 过滤依赖组织架构上下文**：owner doc 「部门可见」需 currentUser 的 deptId + 上行/下行部门树——当前 `IContext` 无 deptId，且 hr 域部门树查询路径未与数据权限管道集成。MR3 修复「部门可见」规则时须先确认 dept tree 数据源。
2. **「财务全见」语义歧义**：owner doc 声明「财务员可见所有财务相关单据」——当前运行时因「无人有过滤」事实上的「全见」恰好与财务员期望吻合，但这是**巧合非配置**（其他角色也全见）。MR3 填充规则时须显式为财务员配「无过滤」角色规则，避免「填充他人过滤时误伤财务员全见」。
3. **A2.18 单组织种子掩盖**：176 行业务单据全 orgId=2 + 单角色测试用户——行级过滤缺失在现有测试中不可见。MR3 修复须补第二种子组织 + 多角色用户 + 负向隔离测试（与 P1-MA2-093 修复测试协同）。

## 7. audit 关闭条件

本报告产出 + arm-index §P1 详细清单登记 P1-MA6-002 + arm-index §报告清单登记本报告 + roadmap A6.3 推进 `todo → ready`。
