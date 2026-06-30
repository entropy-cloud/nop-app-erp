# CRUD 实施路线图

> 最后更新：2026-06-30
> 本路线图独立于业务逻辑实施路线图。CRUD 操作不依赖业务规则，可独立并行实施。

## 目的

为全部 18 个模块的**标准 CRUD 操作**提供实施状态索引。每个模块在完成 CRUD 后获得：
- 实体 DAO + 标准 `CrudBizModel<T>`（codegen 已生成空壳，需验证可运行）
- 标准列表/编辑页面（view.xml + page.yaml）
- 菜单接入（action-auth.xml）
- CRUD 冒烟测试（创建/读取/更新/删除）

CRUD 完成后，业务逻辑深化在此基础上进行。

## 阶段状态

> `todo` = 未开始 / `planned` = 有执行计划 / `done` = 已完成（codegen + 页面 + 菜单，测试统一在 Phase 4）

### Phase 1 — 核心 5 域（进销存+财务）

| 域 | 实体数 | codegen | 页面 | 菜单 | 状态 |
|---|--------|---------|------|------|------|
| master-data | 22 | ✅ | ✅ | ✅ | `done` |
| purchase | 17 | ✅ | ✅ | ✅ | `done` |
| sales | 13 | ✅ | ✅ | ✅ | `done` |
| inventory | 15 | ✅ | ✅ | ✅ | `done` |
| finance | 17 | ✅ | ✅ | ✅ | `done` |

### Phase 2 — 扩展 5 域

| 域 | 实体数 | codegen | 页面 | 菜单 | 状态 |
|---|--------|---------|------|------|------|
| assets | 10 | ✅ | ✅ | ✅ | `done` |
| manufacturing | 23 | ✅ | ✅ | ✅ | `done` |
| quality | 11 | ✅ | ✅ | ✅ | `done` |
| projects | 13 | ✅ | ✅ | ✅ | `done` |
| maintenance | 12 | ✅ | ✅ | ✅ | `done` |

### Phase 3 — 新增 8 域

| 域 | 实体数 | codegen | 页面 | 菜单 | action-auth | 状态 |
|---|--------|---------|------|------|------------|------|
| crm | 34 | ✅ | ❌ | ❌ | ✅ | `planned` |
| customer-service | 16 | ✅ | ❌ | ❌ | ✅ | `planned` |
| human-resource | 28 | ✅ | ❌ | ❌ | ✅ | `planned` |
| aps | 6 | ✅ | ❌ | ❌ | ✅ | `planned` |
| contract | 15 | ✅ | ❌ | ❌ | ✅ | `planned` |
| drp | 7 | ✅ | ❌ | ❌ | ✅ | `planned` |
| logistics | 7 | ✅ | ❌ | ❌ | ✅ | `planned` |
| b2b | 13 | ✅ | ❌ | ❌ | ✅ | `planned` |

> Phase 3 action-auth 已创建，但页面路径尚不存在。需通过 `mvn compile` 触发 precompile 生成 view.xml/page.yaml。

### Phase 4 — CRUD 冒烟测试

所有 18 域的基本操作验证。框架：`JunitAutoTestCase` + GraphQL mutation/query 快照。

| 测试 | 覆盖 | 方法 | 通过标准 |
|------|------|------|---------|
| 新建实体 | 每域 1 主实体 + 1 头-行对 | GraphQL mutation | 返回成功，非空 ID |
| 查询/筛选 | 列表页加载 + 搜索条件 | GraphQL query | 返回不报错 |
| 编辑保存 | 修改 1-2 关键字段 | mutation → query 验证 | 修改值与输入一致 |
| 逻辑删除 | 验证 delVersion=1 | mutation → query | delVersion=1 |
| 关系导航 | 主子表级联 | 新建头→添加行→查询行 | 外键正确引用 |

执行：`mvn test -pl module-{xx} -am`，按 Phase 1→2→3 顺序推进。

## 验证命令

```bash
mvn clean install -DskipTests
mvn test -pl module-crm -am   # 示例: 测试单模块
```

## 规则

- 本文件只跟踪 CRUD 状态。业务逻辑归 `implementation-roadmap.md`。
- Phase 3 页面需通过 `mvn compile` 生成（precompile/gen-page.xgen 从 xmeta 渲染）。
