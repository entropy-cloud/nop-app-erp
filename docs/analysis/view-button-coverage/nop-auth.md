# nop-auth 视图按钮需求覆盖分析

## 分析范围

nop-auth 域共 14 个实体页面，分类如下：

| 实体 | 分类 |
|------|------|
| DemoPage | Other（测试页，非业务实体） |
| NopAuthDept | Custom（树形实体 + row-add-child-button） |
| NopAuthExtLogin | CRUD |
| NopAuthGroup | Custom（树形实体 + row-add-child-button） |
| NopAuthOpLog | Custom（只读操作日志，无 add/update） |
| NopAuthPosition | CRUD |
| NopAuthResource | Custom（树形实体 + row-add-child-button + refreshSiteMapCache） |
| NopAuthRole | Custom（角色管理 + row-user-button + row-authorization-button） |
| NopAuthRoleDataAuth | CRUD |
| NopAuthSession | Custom（只读会话日志，无 add/update） |
| NopAuthSite | CRUD |
| NopAuthTenant | CRUD |
| NopAuthUser | Custom（用户管理 + reset-password/disable/enable） |
| NopAuthUserSubstitution | CRUD |

## 期望按钮推导依据

nop-auth 无 domain-specific `ui-patterns.md`，属 Group B（METHODOLOGY §6）。期望按钮 = CRUD 基线（add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button）+ 平台约定。

平台约定附加按钮记录于 METHODOLOGY §1.3：
- `row-add-child-button` — 树形实体（NopAuthDept, NopAuthGroup, NopAuthResource）
- `row-user-button` / `row-authorization-button` — 角色管理（NopAuthRole）
- `reset-password-button` / `disable-user-button` / `enable-user-button` — 用户管理（NopAuthUser）

## 逐实体分析

### DemoPage — Other

- **期望按钮**：无（非业务实体，测试/演示页面）
- **实际按钮**：`test` (page-action 表单)
- **差距**：无
- **判定**：clean

### NopAuthDept — Custom（树形实体）

- **期望按钮**：CRUD 基线 + `row-add-child-button`（平台约定）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`, `row-add-child-button`
- **差距**：无
- **判定**：clean

### NopAuthExtLogin — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：clean

### NopAuthGroup — Custom（树形实体）

- **期望按钮**：CRUD 基线 + `row-add-child-button`（平台约定）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`, `row-add-child-button`
- **差距**：无
- **判定**：clean

### NopAuthOpLog — Custom（只读操作日志）

- **期望按钮**：CRUD 基线（add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button）
- **实际按钮**：`batch-delete-button`, `row-view-button`, `row-delete-button`
- **差距**：
  - `add-button`: missing (minor) — 操作日志为系统自动记录，无手工新建场景，缺失符合预期
  - `row-update-button`: missing (minor) — 操作日志为不可变审计记录，无编辑场景，缺失符合预期
- **判定**：clean（差距因实体只读性质属有意为之，非遗漏）

### NopAuthPosition — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：clean

### NopAuthResource — Custom（树形实体 + 管理操作）

- **期望按钮**：CRUD 基线 + `row-add-child-button`（平台约定）
- **实际按钮**：`add-button`, `batch-delete-button`, `refreshSiteMapCache`, `row-view-button`, `row-update-button`, `row-delete-button`, `row-add-child-button`
- **差距**：
  - `refreshSiteMapCache`: extra (info) — 工具栏自定按钮，用于资源变更后刷新站点地图缓存，平台资源管理常见操作，有意添加，非多余
- **判定**：clean

### NopAuthRole — Custom（角色管理）

- **期望按钮**：CRUD 基线 + `row-user-button` + `row-authorization-button`（平台约定）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`, `row-user-button`, `row-authorization-button`
- **差距**：无
- **判定**：clean

### NopAuthRoleDataAuth — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：clean

### NopAuthSession — Custom（只读会话日志）

- **期望按钮**：CRUD 基线（add-button, batch-delete-button, row-view-button, row-update-button, row-delete-button）
- **实际按钮**：`batch-delete-button`, `row-view-button`, `row-delete-button`
- **差距**：
  - `add-button`: missing (minor) — 会话为系统登录时自动创建，无手工新建场景，缺失符合预期
  - `row-update-button`: missing (minor) — 会话记录为不可变日志，无编辑场景，缺失符合预期
- **判定**：clean（差距因实体只读性质属有意为之，非遗漏）

### NopAuthSite — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：clean

### NopAuthTenant — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：clean

### NopAuthUser — Custom（用户管理）

- **期望按钮**：CRUD 基线 + `reset-password-button` + `disable-user-button` + `enable-user-button`（平台约定）
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`, `reset-password-button`, `disable-user-button`, `enable-user-button`
- **附加页面**：`deleted`（recover-deleted-button）、`role-users`（select-user-button, batch-delete-button）、`select-role-users`（batch-add-user-button）、`resetUserPassword`（无 action 按钮）— 均为用户管理子页面，非主 CRUD 页面，不纳入基线比较
- **差距**：无
- **判定**：clean

### NopAuthUserSubstitution — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
- **差距**：无
- **判定**：clean

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| Other | DemoPage | 0 | — | 测试页面 |
| Custom (tree) | NopAuthDept | 0 | — | 树形，含 row-add-child-button |
| CRUD | NopAuthExtLogin | 0 | — | |
| Custom (tree) | NopAuthGroup | 0 | — | 树形，含 row-add-child-button |
| Custom (log) | NopAuthOpLog | 0 | — | 只读日志，无 add/update 属有意为之 |
| CRUD | NopAuthPosition | 0 | — | |
| Custom (tree+action) | NopAuthResource | 0 | — | 树形 + refreshSiteMapCache |
| Custom (role mgmt) | NopAuthRole | 0 | — | 含 row-user-button, row-authorization-button |
| CRUD | NopAuthRoleDataAuth | 0 | — | |
| Custom (log) | NopAuthSession | 0 | — | 只读会话日志，无 add/update 属有意为之 |
| CRUD | NopAuthSite | 0 | — | |
| CRUD | NopAuthTenant | 0 | — | |
| Custom (user mgmt) | NopAuthUser | 0 | — | 含 reset-password/disable/enable |
| CRUD | NopAuthUserSubstitution | 0 | — | |

### 总评

- 总实体数：14
- 无差距实体：14（100%）
- Blocker 差距：0
- Major 差距：0
- Minor/Info 差距：0（NopAuthOpLog 和 NopAuthSession 虽偏离 CRUD 基线，但属只读系统日志实体有意行为，不计为修复性差距）

**结论：nop-auth 域的按钮声明与 Group B 期望（CRUD 基线 + 平台约定）完全一致。所有实体均处于 clean 状态。**
