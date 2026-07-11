# Flux 引擎集成策略分析：view.xml 兼容扩展 vs. page.yaml 直写 vs. 元编程标记

> 分析日期：2026-07-11
> 状态：调研分析（未决策）
> 涉及项目：nop-entropy、nop-chaos-flux、nop-app-erp

## 1. 背景与问题陈述

### 1.1 当前技术栈

nop-app-erp 的前端通过以下管道生成：

```
orm.xml (实体模型，唯一真相)
  │ [构建期: nop-cli gen / mvn install]
  ▼
*.xmeta (字段元数据: domain, stdDomain, dict, mandatory, updatable, ext:kind 等)
  │ [构建期: view-gen.xlib:GenViewFromMeta]
  ▼
_gen/_Xxx.view.xml (自动生成的 CRUD 视图基线)
  │ [x:extends 继承]
  ▼
Xxx.view.xml (手写定制层)
  │ [运行期: web.xlib:GenPage + control.xlib]
  ▼
main.page.yaml (3 行生成桩 or 手写 AMIS 页面)
  │ [XJsonLoader 解析 x:gen-extends/x:extends/@i18n/@query:]
  ▼
AMIS JSON (发送到前端 amis-core 渲染)
```

**关键事实**（基于对 nop-app-erp 的全面扫描）：

| 指标 | 数量 | 说明 |
|------|------|------|
| .view.xml 文件总数 | 674（337 生成 + 337 手写） | 每实体 1:1 配对 |
| 纯继承桩（≤20 行，零定制内容） | **329 / 337 = 97.6%** | 几乎全部依赖 xmeta 自动推导 |
| 有实质定制的 view.xml | **8（2.4%）** | 最复杂 147 行 |
| page.yaml 文件总数 | 726 | |
| 3 行生成桩 page.yaml | 702（365 main + 337 picker） | 仅调用 `web:GenPage` |
| 手写 page.yaml | 24（10 看板 + 14 报表） | 直接编写 AMIS DSL |
| _delta/ 目录 | 0 | 不使用 Delta 叠加 |

### 1.2 nop-chaos-flux 是什么

nop-chaos-flux（以下简称 Flux）是对 AMIS 的**从零重写**，2026 年 4 月启动。核心技术栈：React 19 + TypeScript 6.0 + Zustand 5 + Vite 8 + TailwindCSS 4 + shadcn/ui（Radix UI）。

**Flux 的核心设计决策**（与本题直接相关的）：

1. **分层职责明确** — 渲染框架只消费纯净 JSON，不处理 i18n/权限/模块化/元数据推导：
   ```
   Nop 平台层（结构变换）
     → i18n 文本替换、权限裁剪、模块分解合并、编译期元编程（XPL）、XML↔JSON 转换
          ↓ 输出纯净 JSON
   Flux 渲染框架层
     → 统一值编译、作用域管理、动作分发、渲染协调
          ↓
   shadcn/ui 组件层（Radix UI）
   ```

2. **宿主契约 RendererEnv** — Flux 通过 `env.loadPage(path)` 获取已处理好的 JSON schema，不直接消费 view.xml 或 xmeta。

3. **迁移策略已明确为双栈** — 在 `nop-chaos-next` 中新增 `pageType: 'flux'`，保留 `pageType: 'amis'`，逐页迁移而非一次性替换。

4. **~55+ 渲染器已实现**（L0 基线），137 个 AMIS 类型已有完整的保留/弃用映射决策。

### 1.3 核心问题

当 Flux 替代 AMIS 时，三种集成路径的权衡：

| 选项 | 描述 | 关键优势 | 关键风险 |
|------|------|----------|----------|
| **A. view.xml 兼容扩展** | 扩展 `web.xlib` 增加 Flux 输出模式 | 保留全部元编程能力 | view.xml 可能是 AMIS 形状的 |
| **B. page.yaml 直写 Flux** | 跳过 view 层，在 page.yaml 中直接编写 Flux JSON | 最简单、最 Flux 原生 | 丢失 domain→control 自动推导 |
| **C. page.yaml 元编程标记** | 在 YAML 中引入 `xui-gen:Field` 等标记 | 混合：YAML 直写 + 编译期推导 | 创造第三种抽象层 |

---

## 2. 深入分析

### 2.1 关键发现：view.xml 本身是渲染器无关的

这是最重要的发现。view.xml 的三层模型：

| view.xml 层 | 描述 | AMIS 对应 | Flux 对应 |
|-------------|------|-----------|-----------|
| `<grids>` | 列表/表格定义 | crud + columns | crud + columns |
| `<forms>` | 表单（add/edit/view/query） | form + controls | form + controls |
| `<pages>` | 页面组合（crud/picker/tabs/wizard） | page/dialog/tabs | page/dialog/tabs |

这些是**通用 UI 模式**，不是 AMIS 专有概念。AMIS 专有的是 `web.xlib` 的输出格式和 `control.xlib` 中的每个标签实现。

view.xml 中与渲染器相关的只有两处：
1. `<controlLib>` 指向的标签库（当前默认 `/nop/web/xlib/control.xlib`）
2. `<gen-control>` 中内嵌的原始 AMIS JSON（仅 3 个文件使用）

**结论**：view.xml 的输入模型是渲染器无关的，只有输出层（`web.xlib` + `control.xlib`）是 AMIS 绑定的。

### 2.2 control.xlib 的推导机制详析

`XuiHelper.getControlTag()` 的查找链（首匹配优先）：

```
1. {mode}-{control}        ← 显式 control 属性
2. {mode}-{domain}         ← 如 edit-roleId
3. {mode}-{baseDomain}     ← 去除长度后缀如 -4k
4. {mode}-{stdDomain}      ← 如 edit-enum, edit-boolFlag
5. {mode}-{relKind}        ← edit-to-one, edit-to-many
6. {mode}-{stdDataType}    ← edit-string, edit-int, edit-decimal
```

每种匹配在 `control.xlib` 中对应一个标签，输出一段 AMIS JSON 片段。例如 `edit-roleId` 生成一个 picker 控件：

```json
{
  "type": "picker",
  "x:extends": "/nop/auth/pages/NopAuthRole/picker.page.yaml",
  "valueField": "roleId",
  "labelField": "roleName"
}
```

**Flux 版本只需**：创建一个 `flux-control.xlib`，其中 `edit-roleId` 输出 Flux 格式的 picker：

```json
{
  "type": "picker",
  "pickerPage": "/nop/auth/pages/NopAuthRole/picker.page.yaml",
  "valueField": "roleId",
  "labelField": "roleName"
}
```

整个推导算法（`XuiHelper.getControlTag`）是渲染器无关的 — 它只负责找到标签名，实际输出由标签库决定。

### 2.3 nop-app-erp 的元编程依赖度量化

| 依赖程度 | 来源 | 占比 | 影响 |
|----------|------|------|------|
| **完全依赖 xmeta 推导** | 329 个纯继承桩 view.xml | 97.6% | 控件 100% 来自 xmeta → control.xlib |
| **部分依赖** | 8 个定制 view.xml | 2.4% | 大部分控件仍来自 xmeta，仅自定义表单参数和行动作 |
| **完全手写** | 24 个 page.yaml（看板+报表） | — | 与 view 层无关，直接编写渲染器 JSON |

如果跳过 view 层（选项 B），需要手写 337×~3 forms/grid × ~10 fields = **约 10,000+ 个字段控件的 Flux JSON 定义**。这些目前全部由 xmeta 自动推导。

### 2.4 Flux 的 page.yaml 格式兼容性

当前 page.yaml 的 Nop 平台层指令：

| 指令 | 当前用途 | Flux 兼容性 |
|------|----------|-------------|
| `x:gen-extends` | 编译期 XPL 生成 | **完全兼容** — 这是平台层机制，Flux 设计文档明确支持 |
| `x:extends` | 继承外部文件 | **完全兼容** — 同上 |
| `x:override` | 合并操作符 | **完全兼容** — 同上 |
| `@i18n:` | 国际化 | **完全兼容** — Flux 设计文档 §10.1 明确描述 |
| `@query:` / `@mutation:` | GraphQL URL 约定 | **完全兼容** — Flux CRUD schema 使用相同 REST 约定 |
| `@dict:` | 字典加载 | **完全兼容** — 通过 `env.loadDict()` 实现 |
| `xui:roles` / `xui:permissions` | 权限裁剪 | **完全兼容** — Flux 设计文档 §10.2 明确描述 |

**关键洞察**：page.yaml 的"平台层壳"是渲染器无关的。差异只在于壳下面的 JSON body 是 AMIS 格式还是 Flux 格式。

---

## 3. 三个选项的详细权衡

### 选项 A：扩展 view.xml 增加 Flux 输出模式

**方案**：
- 创建 `flux-control.xlib`（与 `control.xlib` 平行），为每个 domain/stdDomain/stdDataType 提供 Flux 格式的控件标签
- 在 `web.xlib` 中新增 `web:GenFluxPage`（与 `web:GenPage` 平行），将 view.xml 转换为 Flux JSON
- page.yaml 通过 `x:gen-extends` 选择调用哪个生成器

```yaml
# AMIS 版本
x:gen-extends: |
  <web:GenPage view="ErpAstAsset.view.xml" page="main" xpl:lib="/nop/web/xlib/web.xlib" />

# Flux 版本
x:gen-extends: |
  <flux:GenFluxPage view="ErpAstAsset.view.xml" page="main" xpl:lib="/nop/web/xlib/flux-web.xlib" />
```

**优势**：
1. **零修改 337 个 view.xml 文件** — 所有手写视图和生成基线完全复用
2. **保留全部元编程能力** — domain→control 推导、xmeta 集成、view-gen 自动生成全部保持
3. **保留全部 Delta 能力** — bounded-merge、x:extends、x:override 全部在 view 层有效
4. **渐进迁移** — 同一个 view.xml 可同时输出 AMIS 和 Flux，按 page.yaml 的生成器选择切换
5. **与 Flux 设计哲学一致** — Flux 明确说"元编程属于平台层"，view.xml + XPL 正是平台层

**劣势**：
1. **需要编写 flux-control.xlib** — ~60 个标签（对应 control.xlib 的 domain/stdDomain/stdDataType 映射），一次性投入
2. **需要编写 flux-web.xlib** — GenFluxPage + 子标签（GenFluxGrid、GenFluxForm 等），结构参照 web.xlib 但输出 Flux JSON
3. **`<gen-control>` 内嵌的 AMIS JSON 需要迁移** — 但仅影响 3 个文件，且这 3 个文件本就是手写 AMIS，可逐步迁移
4. **view.xml 的 page 类型可能不完全覆盖 Flux 能力** — 例如 Flux 的 `loop`/`recurse`/`reaction` 结构节点在 view.xml 中无对应概念

**工作量估算**：
- flux-control.xlib：~60 个标签 × 平均 15 行 = ~900 行（参照 control.xlib 的 1206 行）
- flux-web.xlib：~600 行（参照 web.xlib 的 929 行，去掉 AMIS 特有逻辑）
- 总计：约 1500 行 XPL 标签库代码

### 选项 B：page.yaml 直写 Flux，跳过 view 层

**方案**：
- CRUD 页面的 page.yaml 不再调用 `web:GenPage`，而是直接编写 Flux JSON schema
- 完全绕过 view.xml + control.xlib 推导

```yaml
# Flux 直写
type: page
body:
  - type: crud
    name: crud-grid
    loadAction:
      action: ajax
      args:
        method: post
        url: "/r/ErpAstAsset__findPage"
    columns:
      - name: id
        label: ID
        type: text
      - name: code
        label: 编码
        type: text
      # ... 每个字段都要手写
    filter:
      type: form
      body:
        # ... 每个查询字段都要手写
    dialog:
      type: form
      body:
        # ... 每个表单字段都要手写
```

**优势**：
1. **最 Flux 原生** — 无任何中间抽象，直接控制 Flux schema 的每个细节
2. **无 AMIS 遗留** — 彻底与 AMIS 模型解耦
3. **Flux 能力全覆盖** — 可以使用 view.xml 无法表达的 Flux 结构（loop、recurse、reaction、fragment）

**劣势**：
1. **丧失 domain→control 自动推导** — 每个字段都要手写控件定义
2. **工作量爆炸** — 337 实体 × 平均 3 个 form/grid × 平均 10 个字段 = ~10,000+ 字段定义需要手写
3. **丧失 view-gen 自动生成** — 从 ORM 模型到视图基线的自动化链路断裂
4. **丧失 view 层 Delta 定制** — bounded-merge 等机制不再可用，只能在 page.yaml 层做 Delta
5. **维护负担** — ORM 模型变更（新增字段/修改 domain）需要同步修改多个手写 page.yaml
6. **与 Flux 自身定位矛盾** — Flux 设计文档明确说元编程在平台层解决，而 view.xml + XPL 正是平台层机制

### 选项 C：page.yaml 引入元编程标记

**方案**：
- 在 page.yaml 的 Flux JSON 中引入 `xui-gen:Field` 等编译期标记
- 标记在加载时被 XPL 展开，从 xmeta 推导出控件定义

```yaml
type: page
body:
  - type: crud
    name: crud-grid
    columns:
      - xui-gen:Field          # 编译期标记：从 xmeta 推导
        name: roleId
        domain: roleId          # 自动生成 Flux picker 控件
      - xui-gen:Field
        name: status
        stdDomain: enum
        dict: assetStatus       # 自动生成 Flux select 控件
    dialog:
      type: form
      body:
        - xui-gen:FormFields    # 编译期标记：从 xmeta 批量推导表单字段
          include: [code, name, categoryId, status]
```

**优势**：
1. **在 YAML 中保留元编程** — 不依赖 view.xml 层
2. **Flux 原生结构** — 生成的结果就是 Flux JSON
3. **粒度灵活** — 可以逐字段标记，也可以批量标记

**劣势**：
1. **重新发明 view.xml** — `xui-gen:Field` 本质上是 view.xml 的 `<cell>` / `<col>` 的 YAML 版本，创造了第三种抽象
2. **维护三套系统** — AMIS(view.xml)、Flux(view.xml)、Flux(xui-gen)，增加了认知负担
3. **推导逻辑需要重新实现** — `XuiHelper.getControlTag` 的查找链需要在新的标记处理器中重新实现
4. **Delta 定制碎片化** — 一部分定制在 page.yaml（手写部分），一部分在标记推导逻辑（自动部分），调试困难
5. **与 XPL 机制重复** — page.yaml 已有 `x:gen-extends` 可以调用任意 XPL 来实现元编程，`xui-gen:` 标记是它的低配替代

---

## 4. 推荐策略：选项 A（view.xml 兼容扩展）为主，分层渐进

### 4.1 推荐结论

**推荐选项 A（view.xml 兼容扩展）作为 CRUD 页面的核心策略。**

理由总结：

| 判断维度 | 结论 |
|----------|------|
| view.xml 是否 AMIS 绑定？ | **否** — 三层模型（grid/form/page）是渲染器无关的通用 UI 抽象 |
| 元编程推导是否渲染器无关？ | **是** — `XuiHelper.getControlTag` 查找算法只找标签名，输出由标签库决定 |
| Flux 是否支持 x:gen-extends 机制？ | **是** — Flux 设计文档 §10.3 明确描述 |
| 跳过 view 层的代价？ | 10,000+ 字段定义需要手写，丧失 ORM→视图自动化链路 |
| 选项 C 是否创造了不必要的抽象？ | **是** — xui-gen:Field 是 view.xml cell/col 的 YAML 低配重复 |

### 4.2 分层页面策略

针对不同页面类型采用不同策略：

```
┌──────────────────────────────────────────────────────────────────┐
│ 页面类型              │ 策略         │ 理由                        │
├───────────────────────┼─────────────┼─────────────────────────────┤
│ 标准 CRUD (702 页)    │ 选项 A       │ 97.6% 是纯继承桩，必须保留   │
│                       │              │ 元编程推导                   │
│ 定制 CRUD (8 页)      │ 选项 A       │ 定制内容在 view.xml 中，     │
│                       │              │ 只需 flux-control.xlib 输出 │
│                       │              │ Flux 格式                    │
│ 看板 (10 页)          │ 选项 B       │ 100% 手写 AMIS，无 view 层。 │
│                       │              │ 直接重写为 Flux JSON         │
│ 报表 (14 页)          │ 选项 B       │ 同上，nop-report 适配 Flux   │
│ 未来复杂交互页面       │ 选项 B       │ 甘特图/Kanban 等复杂页面，   │
│ (凭证录入/排产等)      │              │ 直接编写 Flux schema，利用   │
│                       │              │ Flux 结构节点能力            │
└───────────────────────┴─────────────┴─────────────────────────────┘
```

### 4.3 技术实现架构

```
                     view.xml (渲染器无关)
                    /            \
           web.xlib:GenPage    flux-web.xlib:GenFluxPage
          (AMIS 输出模式)       (Flux 输出模式)
                /                    \
        control.xlib           flux-control.xlib
     (domain→AMIS control)   (domain→Flux control)
                \                    \
                 ↓                    ↓
           AMIS JSON            Flux JSON
                \                    /
          page.yaml              page.yaml
        (x:gen-extends:       (x:gen-extends:
         web:GenPage)           flux:GenFluxPage)
                \                    /
                 ↓                    ↓
           amis-core              Flux runtime
         (现有前端)            (React 19 渲染器)
```

**关键实现件**：

| 组件 | 路径 | 工作量 | 说明 |
|------|------|--------|------|
| `flux-web.xlib` | `nop-web/.../xlib/flux-web.xlib` | ~600 行 | 参照 web.xlib 结构，输出 Flux JSON |
| `flux-control.xlib` | `nop-web/.../xlib/flux-control.xlib` | ~900 行 | 参照 control.xlib，为每个 domain/stdDomain 输出 Flux 控件 |
| `GenFluxPage` | flux-web.xlib 入口标签 | 参照 GenPage | 调度 page_crud/picker/tabs/wizard |
| `GenFluxGrid` | flux-web.xlib 子标签 | 参照 GenGridImpl | 输出 Flux crud + columns |
| `GenFluxForm` | flux-web.xlib 子标签 | 参照 GenFormBody | 解析 layout DSL → Flux form body |
| `DefaultFluxControl` | flux-web.xlib 标签 | 参照 DefaultControl | 调用 XuiHelper.getControlTag → flux-control.xlib |

### 4.4 渐进迁移路线

参照 Flux 迁移计划的双栈策略，nop-app-erp 的迁移分阶段：

**Phase 0 — 基础设施（nop-entropy + Flux 侧）**
- 实现 flux-web.xlib + flux-control.xlib
- 在 nop-chaos-next 中实现 `pageType: 'flux'` 路由（参照 Flux 迁移计划 Phase 5）
- 搭建 Flux CRUD 冒烟测试页面

**Phase 1 — 单域验证（选 1 个域，如 master-data）**
- 为 master-data 域的实体创建 Flux 版 page.yaml（改 `x:gen-extends` 为 `flux:GenFluxPage`）
- 验证 CRUD 全流程：列表/查询/新建/编辑/删除/选择器
- 验证 domain→control 推导正确性（特别是 relation picker、enum select、dict select）
- 验证权限裁剪和 i18n

**Phase 2 — 批量迁移 CRUD 页面**
- 将 702 个生成桩 page.yaml 的 `x:gen-extends` 从 `web:GenPage` 切换为 `flux:GenFluxPage`
- 由于都是 3 行桩文件，可以用脚本批量替换
- 逐域验证冒烟通过

**Phase 3 — 迁移定制页面和看板**
- 8 个定制 view.xml：检查 gen-control 中的内嵌 AMIS JSON，迁移为 Flux 格式
- 10 个看板 page.yaml：重写为 Flux JSON（利用 Flux 的 data-source + chart 组件）
- 14 个报表 page.yaml：配合 nop-report Flux 适配器迁移

**Phase 4 — 退役 AMIS**
- 所有页面验证通过后，移除 AMIS 运行时依赖
- page.yaml 的 `x:gen-extends` 统一指向 Flux 生成器

### 4.5 关于选项 C 的补充说明

选项 C（`xui-gen:Field` 标记）在**手写 Flux 页面**中有局部价值。当开发者直接编写 Flux JSON 时，如果某个字段需要从 xmeta 推导控件，可以通过 `x:gen-extends` 调用 XPL 来实现单字段推导：

```yaml
# 手写 Flux 页面中嵌入单字段推导
x:gen-extends: |
  <flux:GenField name="roleId" mode="edit" xpl:lib="/nop/web/xlib/flux-control.xlib" />

body:
  - type: form
    body:
      - ${roleId_control}  # 从 x:gen-extends 注入
      - type: input-text
        name: customField   # 手写控件
```

这不需要新的 `xui-gen:` 语法 — `x:gen-extends` + XPL 已经覆盖了这个需求。

---

## 5. 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| Flux 组件覆盖不完整 | 部分 AMIS 控件在 Flux 中无对应 | Flux 已有 137 个 AMIS 类型的保留/弃用映射；L0 基线 ~55+ 渲染器已覆盖 CRUD 所需 |
| view.xml 的 page 类型无法表达 Flux 新能力 | loop/recurse/reaction 等结构节点无 view.xml 对应 | 这些能力用于手写复杂页面（选项 B），不需要通过 view.xml 表达 |
| `<gen-control>` 内嵌 AMIS JSON 遗留 | 仅 3 个文件，迁移范围可控 | 逐文件迁移为 Flux JSON 或 view.xml 新写法 |
| Flux 迁移计划尚未到达 ERP 层 | nop-chaos-next 集成 Flux 是前置条件 | 等 nop-chaos-next Phase 5（pageType 双栈路由）完成后再启动 ERP 迁移 |
| 双输出（AMIS + Flux）期间的维护成本 | 需要同时维护 control.xlib + flux-control.xlib | domain→control 映射规则不变，只是输出格式不同；标签名和触发条件完全相同 |

---

## 6. 总结

| 选项 | 推荐度 | 适用场景 |
|------|--------|----------|
| **A. view.xml 兼容扩展** | **推荐（CRUD 主力）** | 702 个 CRUD 生成桩 + 8 个定制视图 |
| **B. page.yaml 直写 Flux** | **推荐（复杂页面）** | 24 个看板/报表 + 未来甘特图/Kanban 等复杂交互页面 |
| **C. xui-gen:Field 标记** | **不推荐** | x:gen-extends + XPL 已覆盖此需求，无需新抽象 |

核心判断：**view.xml 是渲染器无关的 UI 抽象，AMIS 绑定只在输出层（web.xlib + control.xlib）。** 为 Flux 创建平行输出层（flux-web.xlib + flux-control.xlib）是成本最低、风险最小、与 Flux 设计哲学最一致的路径。跳过 view 层会导致 97.6% 的页面丧失元编程推导能力，维护成本不可接受。

---

## 7. Action 模型的 Flux 兼容性分析

### 7.1 问题的提出

view.xml 中的 `<action>` 元素携带了大量 AMIS 导向的属性（`actionType`、`<api>`、`<dialog>`、`<drawer>`、`<onEvent>` 等）。Flux 的 action 代数与之差异显著。是重新定义 action 模型使其兼容 Flux，还是保持 view.xml action 不变并在输出层转换？

### 7.2 view.xml action 模型 vs. Flux action 代数

#### view.xml action 的实际用法（nop-app-erp 扫描结果）

| 特性 | view.xml 中使用情况 | 文件数 |
|------|---------------------|--------|
| `actionType="ajax"` | 后端调用（审批/打卡等） | 3 |
| `actionType="drawer"` | 抽屉表单（编辑/面试/调动） | 5 |
| `actionType="submit"` | 表单提交确认 | 3 |
| `actionType="cancel"` | 取消/关闭表单 | 3 |
| `<dialog page="..."/>` | 弹窗页面引用（生成基线中标准模式） | 337（每个 _gen view） |
| `<api url="@query:..."/>` | 列表查询 | 337 |
| `<api url="@mutation:..."/>` | 保存/删除/批量删除 | 337 |
| `<confirmText>` | 操作确认 | ~337（生成）+ 4（定制） |
| `<messages><success>` | 成功提示 | 4（定制） |
| `disabledOn` / `visibleOn` | 条件渲染 | **0**（view.xml 中未使用） |
| `<onEvent>` | AMIS 事件逃逸口 | **0**（全项目未使用） |

**关键发现**：`<onEvent>`（唯一真正 AMIS 绑定的逃逸口）在整个项目中**零使用**。`disabledOn`/`visibleOn` 在 view.xml 中也**零使用**。实际使用的 action 特性集非常小且渲染器无关。

#### 语义映射表

| view.xml action 语义 | AMIS 输出（当前 web.xlib:NormalizeAction） | Flux 输出（flux-web.xlib:NormalizeFluxAction） | 兼容性 |
|---------------------|-------------------------------------------|------------------------------------------------|--------|
| `<api url="@mutation:X__save?id=$id"/>` | `{ actionType:"ajax", api:{ url:... } }` | `{ action:"ajax", args:{ url:"/r/X__save", data:{id:"${id}"} } }` | 语义相同，格式转换 |
| `<dialog page="add"/>` | `{ actionType:"dialog", dialog:{ body:<页面内容> } }` | `{ action:"openDialog", args:{ body:<Flux页面内容> } }` | 语义相同，页面内联方式相同 |
| `<drawer page="edit"/>` | `{ actionType:"drawer", drawer:{ body:<页面内容> } }` | `{ action:"openDrawer", args:{ body:<Flux页面内容> } }` | 语义相同 |
| `<confirmText>` | confirmText 属性 | confirmText 属性（位置从 button 移到 action 内） | **完全相同** |
| `<messages><success>` | messages.success 属性 | messages.success 属性 | **完全相同** |
| `actionType="submit"` | AMIS 表单提交 | `{ action:"submitForm" }` | 语义相同 |
| `actionType="cancel"` | AMIS 关闭弹窗 | `{ action:"closeSurface" }` | 语义相同 |
| `<table><api url="@query:X__findPage"/>` | crud.api | `loadAction:{ action:"ajax", args:{url:"/r/X__findPage"} }` | 语义相同 |
| `<simple><api url="@mutation:X__save"/></simple>` | form.api | `submitAction:{ action:"ajax", args:{url:"/r/X__save"} }` | 语义相同 |
| `actionGroup` | dropdown-button | dropdown-button（Flux 有对应渲染器） | 语义相同 |

### 7.3 结论：view.xml action 模型已经是渲染器无关的

view.xml 的 `<action>` 模型捕获的是**交互意图**，而非渲染器实现细节：

- **`<api url="@mutation:..."/>`** — "调用后端"（`@mutation:` 是 Nop 平台层 URL 约定，不是 AMIS 特有的）
- **`<dialog page="add"/>`** — "打开页面作为弹窗"（page 引用是 view.xml 内部抽象）
- **`<confirmText>`** — "操作前确认"
- **`<messages>`** — "操作后反馈"
- **`actionType="drawer"`** — "以抽屉形式打开"

这些语义在 Flux 中都有直接对应物。**不需要重新定义 action 模型。**

### 7.4 转换层实现：NormalizeFluxAction

与 `NormalizeAction`（AMIS 输出）平行，创建 `NormalizeFluxAction`（Flux 输出），负责以下转换：

#### (a) actionType → action 字符串映射

```
ajax    → action: "ajax"
dialog  → action: "openDialog"
drawer  → action: "openDrawer"
submit  → action: "submitForm"
cancel  → action: "closeSurface"
url/link→ action: "navigate"
```

#### (b) URL 约定转换

`@mutation:X__save?id=$id` 在平台层解析为：

```json
// AMIS 输出
{ "url": "@mutation:X__save?id=$id" }

// Flux 输出（由 NormalizeFluxAction 在平台层解析）
{ "url": "/r/X__save", "method": "post", "data": { "id": "${id}" } }
```

Flux 使用 `/r/Entity__method` 约定（见 `standard-crud.json`），而 `@query:`/`@mutation:` 是 Nop 平台层 URL 宏。转换在 XPL 中完成，Flux 运行时只看到最终 URL。

#### (c) 页面内联（LoadPage → LoadFluxPage）

view.xml 中 `<dialog page="add"/>` 引用的页面（如 `<simple name="add" form="add">`）需要内联到 `openDialog.args.body` 中。

在 AMIS 路径中，`web.xlib:LoadPage` 将页面内容加载并内联。在 Flux 路径中，`flux-web.xlib:LoadFluxPage` 做同样的事，但生成的是 Flux 格式的 form JSON（由 GenFluxForm 产生）。

#### (d) 后续行为转换

AMIS 的 `reload`/`target` 在 Flux 中变为 `then` 链：

```
// view.xml 隐含语义（由 web.xlib 在代码生成时自动添加）
reload: "crud-grid"

// Flux 输出
then: [{ action: "component:refresh", componentId: "crud-grid" }]
```

#### (e) disabledOn/visibleOn → disabled/visible

虽然 ERP 项目中 view.xml 零使用，但 XDef schema 允许这些属性。转换很简单——去掉 `On` 后缀，值不变：

```
disabledOn="${!canUndo}"  →  disabled: "${!canUndo}"
visibleOn="${isAdmin}"    →  visible: "${isAdmin}"
```

### 7.5 view.xml action 模型无法覆盖的 Flux 能力

Flux action 代数有一些 view.xml 无法表达的高级特性：

| Flux 能力 | 描述 | view.xml 可表达？ | 解决方案 |
|-----------|------|-------------------|----------|
| `then`/`onError`/`onSettled` | 动作链分支 | 否 | 在 page.yaml 中用 `x:gen-extends` 注入 Flux action 链 |
| `parallel` | 并行执行 | 否 | 同上 |
| `when` | 守卫条件 | 否 | 同上 |
| `control:{retry,timeout,debounce}` | 执行控制 | 否 | 同上 |
| `component:setValue` | 跨组件写值 | 否 | 同上 |

但这些高级能力**在标准 CRUD 中不需要**。标准 CRUD 的 action 模式是：

1. **列表查询**：`@query:findPage` → `loadAction: { ajax }`
2. **新增/编辑**：`<dialog>` + `<simple><api save/update>` → `openDialog` + `submitAction: { ajax }`
3. **删除**：`@mutation:delete` + confirm + reload → `ajax` + confirmText + then: refresh
4. **状态流转**：`@mutation:submit/approve/reject` + confirm + message + reload → 同上

ERP 项目中仅有的复杂 action 场景（ErpCsTicket 的 KB 建议采纳、ErpHrRecruitment 的多步表单）已通过 `<gen-control>` 内嵌 AMIS JSON 实现，在 Flux 迁移时重写为 page.yaml 直写即可。

### 7.6 推荐策略

```
┌────────────────────────────────────────────────────────────────────────┐
│ Action 来源           │ 策略             │ 理由                          │
├───────────────────────┼─────────────────┼───────────────────────────────┤
│ 标准 CRUD action      │ view.xml 保持    │ 337 实体的 rowActions/        │
│ (findPage/save/delete │ + NormalizeFlux  │ listActions 完全由 _gen 自动  │
│ /batchDelete)         │ Action 转换      │ 生成，语义与渲染器无关         │
├───────────────────────┼─────────────────┼───────────────────────────────┤
│ 状态机 action         │ view.xml 保持    │ submit/approve/reject 等是    │
│ (submit/approve/      │ + NormalizeFlux  │ 简单 ajax+confirm+message+   │
│ reject/cancel)        │ Action 转换      │ reload 模式，转换直接          │
├───────────────────────┼─────────────────┼───────────────────────────────┤
│ 复杂 action 链        │ page.yaml 直写   │ 需要 then/parallel/when 等    │
│ (多步编排/条件分支)    │ Flux action 代数 │ Flux 高级能力，直接编写        │
├───────────────────────┼─────────────────┼───────────────────────────────┤
│ <gen-control> 内嵌    │ page.yaml 直写   │ 3 个文件中嵌入的 AMIS JSON，   │
│ AMIS JSON             │ Flux JSON        │ 直接重写为 Flux 格式           │
└───────────────────────┴─────────────────┴───────────────────────────────┘
```

**核心判断**：view.xml action 模型不需要重新定义。它捕获的是交互意图（"调用后端"/"打开弹窗"/"确认操作"），而非 AMIS 实现细节。与控件推导一样，action 的 AMES 绑定只在 `web.xlib:NormalizeAction` 的输出格式中。为 Flux 创建平行的 `NormalizeFluxAction` 即可完成转换。

### 7.7 NormalizeFluxAction 的工作量估算

| 组件 | 参照 | 估算工作量 | 说明 |
|------|------|-----------|------|
| `NormalizeFluxAction` 标签 | `web.xlib:NormalizeAction`（~60 行） | ~80 行 | actionType→action 映射 + URL 解析 + dialog/drawer 页面内联 |
| `GenFluxActions` 标签 | `web.xlib:GenActions`（~20 行） | ~25 行 | 遍历 action 列表，调用 NormalizeFluxAction |
| `LoadFluxPage` 标签 | `web.xlib:LoadPage`（~50 行） | ~50 行 | 加载 view.xml 页面定义，生成 Flux body JSON |
| CRUD loadAction 生成 | `grid_crud.xpl` 中的 `<table><api>` 处理 | ~30 行 | 转换为 `loadAction: { ajax }` |
| Form submitAction 生成 | `page_simple.xpl` 中的 `<api>` 处理 | ~20 行 | 转换为 `submitAction: { ajax }` |
| **总计** | | **~205 行** | 在 flux-web.xlib 中实现 |

---

## 8. view.xml 的 AMIS 词汇问题：是否需要 view2.xdef？

### 8.1 问题的本质

前述分析得出"view.xml 是渲染器无关的"结论后，一个更深层的质疑是：view.xml 的**属性名和语法**确实携带了大量 AMIS DNA。`actionType`、`level`、`submitOnChange`、`autoFillHeight`、`persistData`、`wrapWithPanel`、`canAccessSuperData`……这些词汇是 AMIS 的概念。如果 Flux 是长期方向，view 层是否应该用 Flux 原生的词汇重写？

### 8.2 view.xml 全属性分类审计

对 `xview.xdef`、`grid.xdef`、`form.xdef`、`action.xdef` 中所有属性的三分类审计：

#### Category A — 渲染器中立的结构语义（view.xml 的核心价值）

这些属性是 view.xml 存在的理由，与任何渲染器无关：

| 属性/元素 | 出现位置 | 语义 |
|-----------|----------|------|
| `bizObjName`、`objMeta`、`controlLib` | xview | 实体绑定与元数据引用 |
| `<grids>`/`<forms>`/`<pages>` | xview | 三层结构容器 |
| `<grid id="list">`/`<form id="add">` | grid/form | 命名的视图定义 |
| `<col id="roleId" sortable/>`/`<cell id="name" mandatory/>` | grid/form | 字段描述符 |
| `domain`、`stdDomain`、`control`、`dict` | disp.xdef | 控件推导（渲染器中立算法） |
| `<layout>` | form | 字段排列 DSL |
| `<crud name="main">`/`<picker>`/`<tabs>`/`<wizard>` | xview pages | 页面组合模式 |
| `<selection>` | grid/form | GraphQL 字段选择集 |
| `<filter>`/`<orderBy>` | grid | 查询元数据 |
| `<rules>` | form | 表单校验规则 |
| `editMode` | grid/form/cell | 控件编辑模式 |
| `x:extends`/`x:override`/`bounded-merge` | xdsl | Delta 定制机制 |
| `xui:role`/`xui:permissions` | action | 权限控制 |

#### Category B — AMIS 命名但语义通用（可机械映射）

| view.xml 属性 | AMIS 来源 | 通用语义 | Flux 对应 |
|---------------|-----------|----------|-----------|
| `actionType="ajax/dialog/drawer"` | AMIS actionType | 动作意图 | `action: "ajax/openDialog/openDrawer"` |
| `level="primary/danger"` | AMIS button level | 按钮视觉级别 | `variant: "primary/destructive"` |
| `<api url="@query:/@mutation:">` | AMIS api | 后端调用 | `args: { url }` |
| `<dialog page="add"/>` | AMIS dialog | 弹窗页面引用 | `action: "openDialog", args: { body }` |
| `<drawer page="edit"/>` | AMIS drawer | 抽屉页面引用 | `action: "openDrawer", args: { body }` |
| `<confirmText>` | AMIS confirmText | 操作前确认 | `confirmText`（名称相同） |
| `<messages><success>` | AMIS messages | 操作后反馈 | `messages: { success }`（名称相同） |
| `disabledOn`/`visibleOn` | AMIS 平行字段 | 条件渲染 | `disabled`/`visible`（去 On 后缀） |
| `icon="fa fa-plus"` | AMIS icon | 图标 | `icon: "plus"`（Flux 用 lucide-react） |
| `actionGroup` | AMIS dropdown-button | 按钮分组 | dropdown-button（Flux 有对应） |

#### Category C — 纯 AMIS 特有，无通用语义

| 属性/元素 | 出现位置 | 语义 | ERP 实际使用 |
|-----------|----------|------|-------------|
| `submitOnChange` | form/cell | AMIS 表单自动提交 | **0 次**（手写层）/ 337 次（_gen 层） |
| `submitOnInit` | form | AMIS 表单初始化提交 | 0 次 |
| `resetAfterSubmit` | form/simple | AMIS 表单重置 | 0 次 |
| `persistData`/`persistDataKeys` | form | AMIS 数据持久化 | 0 次 |
| `wrapWithPanel`/`submitText` | form | AMIS 表单布局 | 0 次 |
| `inheritData`/`canAccessSuperData` | form | AMIS 作用域继承 | 0 次 |
| `promptPageLeave` | form | AMIS 离开提示 | 0 次 |
| `interval`/`checkInterval`/`initCheckInterval` | form/grid | AMIS 轮询 | 0 次 |
| `silentPolling` | form/grid/page | AMIS 加载动画 | 0 次 |
| `initFetch`/`initFetchOn` | form/grid/page | AMIS 自动拉取 | 0 次 |
| `stopAutoRefreshWhen` | form/grid/page | AMIS 自动刷新控制 | 0 次 |
| `<asyncApi>`/`<initAsyncApi>` | form | AMIS 长轮询 | 0 次 |
| `autoFillHeight` | crud/table | AMIS 高度自适应 | **0 次**（手写层）/ 337 次（_gen 层） |
| `loadDataOnce` | table | AMIS 前端分页 | 0 次 |
| `stopAutoRefreshWhenModalIsOpen` | table | AMIS 弹窗暂停刷新 | 0 次 |
| `affixHeader`/`checkOnItemClick` | grid | AMIS 表格行为 | 0 次 |
| `combineNum`/`combineFromIndex` | grid | AMIS 单元格合并 | 0 次 |
| `<prefixRow>`/`<affixRow>` | grid | AMIS 表格外行 | 0 次 |
| `rowClassName`/`rowClassNameExpr` | grid | AMIS 行样式 | 0 次 |
| `rowDrag`/`colDrag` | table | AMIS 拖拽排序 | 0 次 |
| `asideResizor`/`asideMinWidth`/`asideMaxWidth` | page | AMIS 侧边栏布局 | 0 次 |
| `close="boolean-or-string"` | action | AMIS 弹窗关闭行为 | 0 次 |
| `countDown`/`countDownTpl` | action | AMIS 按钮倒计时 | 0 次 |
| `copyFormat`/`content` | action | AMIS 复制行为 | 0 次 |
| `<feedback>` | action | AMIS 操作后反馈弹窗 | 0 次 |
| `<onEvent>` | action | AMIS 原始事件 JSON | **0 次** |
| `onClick` (JS 代码字符串) | action | AMIS 点击处理 | 0 次 |
| `hotKey` | action | AMIS 快捷键 | 0 次 |
| `tabsMode`/`closeable`/`draggable`/`mountOnEnter`/`unmountOnExit` | tabs | AMIS 标签页行为 | 0 次 |

### 8.3 审计结论：AMIS DNA 的真实分布

> **重要修正**（见 §8.3a）：初始分类中 Category C 过于激进。经过 Flux 组件 schema 全量审计，大部分"AMIS 特有"属性实际上是**通用 UI 行为**，Flux 已支持或应支持。修正后的三分类见 §8.3a。

**初始分类：**

```
Category A (渲染器中立): ~40 个属性 — view.xml 存在的核心价值
Category B (AMIS 命名, 通用语义): ~15 个属性 — 可被输出层机械映射
Category C (纯 AMIS 特有): ~50 个属性 — 几乎全部在 ERP 中零使用
```

**手写 view.xml 中 Category C 属性使用量：0。**

Category C 属性的使用仅存在于 codegen 模板生成的 `_gen/` 文件中（`submitOnChange="true"` 和 `autoFillHeight="true"`），共 1011 处，全部来自模板硬编码。

### 8.3a 修正：Flux 组件 schema 全量审计 — 大部分"AMIS 特有"属性是通用 UI 行为

对 Flux 全部渲染器 schema（FormSchema、CrudSchema、TableSchema、TabsSchema、ButtonSchema、PageSchema、DialogSchema、DrawerSchema、WizardSchema）的属性级审计表明，原 Category C 中**绝大多数属性是通用 UI 行为**，Flux 已支持或应支持。

#### 已被 Flux 支持的"AMIS 特有"属性（26 项）

这些属性不是 AMIS 发明——它们是任何 CRUD 框架都需要的通用 UI 行为，Flux 已覆盖：

| view.xml 属性 | Flux 对应属性 | Flux 位置 |
|---------------|--------------|-----------|
| `submitOnChange` | `submitOnChange: boolean`（防抖 300ms） | FormSchema |
| `preventEnterSubmit` | `preventEnterSubmit: boolean` | FormSchema |
| `scrollToFirstError` | `scrollToFirstError: boolean` | FormSchema |
| `autoLoad`/`initFetch` | `autoLoad: boolean` + `loadAction` | FormSchema |
| `loadDataOnce` | `clientMode.loadDataOnce` + `loadAllData` | CrudSchema |
| `filterTogglable` | `filterTogglable: boolean \| config` | CrudSchema |
| `mode` (table/cards) | `listMode: 'table'\|'cards'\|'list'` | CrudSchema |
| `interval`/`silentPolling` | `polling: { enabled, stopWhen }` + `data-source.interval` | CrudSchema |
| `stopAutoRefreshWhen` | `polling.stopWhen` / `data-source.stopWhen` | CrudSchema |
| `affixHeader` | `affixHeader: boolean` | TableSchema |
| `combineNum` | `combineNum: number` | TableSchema |
| `rowDrag` | `draggable: boolean` | TableSchema |
| `col fixed="left/right"` | `column.fixed: 'left'\|'right'` | TableColumnSchema |
| `col sortable` | `column.sortable: boolean` | TableColumnSchema |
| `selectable`/`multiple` | `selection: { type: 'checkbox'\|'radio' }` | CrudSchema/TableSchema |
| `mountOnEnter` | `mountOnEnter` (per-tab) | TabsItemSchema |
| `unmountOnExit` | `unmountOnExit` (per-tab) | TabsItemSchema |
| `tabsMode` | `tabsMode`（10 种模式） | TabsSchema |
| `tooltip` | `tooltip: string` | ButtonSchema |
| `disabledTip` | `disabledTip: string` | ButtonSchema |
| `block` | `block: boolean` | ButtonSchema |
| `active` | `active: boolean \| string` | ButtonSchema |
| `closeOnEsc` | `closeOnEsc: boolean` | DialogSchema/DrawerSchema |
| `closeOnOutside` | `closeOnOutsideClick`/`closeOnOutside` | DialogSchema/DrawerSchema |
| `showCloseButton` | `showCloseButton: boolean` | DialogSchema/DrawerSchema |
| `dialog size` | `size: 'xs'\|'sm'\|'md'\|'lg'\|'xl'\|'full'` | DialogSchema/DrawerSchema |

**结论**：这些属性在 view.xml 中保留是完全正确的——它们是通用 UI 行为，Flux 也需要。

#### Flux 尚不支持但应该引入的通用 UI 行为（12 项）

这些是**真正有价值的功能缺口**，建议向 Flux 提 feature request：

| 属性 | 语义 | 优先级 | ERP 场景 |
|------|------|--------|----------|
| `autoFillHeight` | 表格自适应视口高度 | **高** | 全部 CRUD 页面都需要（337 个 _gen view 使用） |
| `rowClassName`/`rowClassNameExpr` | 条件行样式（危险行高亮等） | **高** | 超期工单红色、库存预警黄色、审批拒绝行灰色 |
| `checkOnItemClick` | 点击行切换选中 | 中 | 批量选择场景 |
| `promptPageLeave` | 未保存数据离开提示 | **高** | 凭证录入、复杂表单 |
| `resetAfterSubmit` | 提交后重置表单 | 中 | 连续录入场景 |
| `asideResizor`/`asideMinWidth` | 可调整宽度的侧边栏 | 中 | 宽筛选条件面板 |
| `alwaysShowPagination` | 始终显示分页栏 | 低 | UX 偏好 |
| `hotKey` | 键盘快捷键 | 中 | 无障碍 + 高效操作 |
| `countDown`/`countDownTpl` | 按钮倒计时 | 低 | SMS 验证码场景 |
| `tooltipPlacement` | tooltip 位置控制 | 低 | 密集按钮区域 |
| `persistData` | 表单数据跨页面持久化 | 中 | 多步录入流程 |
| `stopAutoRefreshWhenModalIsOpen` | 弹窗打开时暂停轮询 | 中 | 看板自动刷新 + 编辑弹窗 |

其中 `autoFillHeight`、`rowClassName`、`promptPageLeave` 三个为**高优先级**——ERP 几乎每个 CRUD 页面都需要前两个，凭证录入等复杂表单需要第三个。

#### Flux 设计上故意不支持的 AMIS 概念（7 项）

这些属性反映了 AMIS 的设计哲学差异，Flux 有不同的处理方式，**不需要在 Flux 中支持**：

| 属性 | AMIS 方式 | Flux 替代方式 | 理由 |
|------|-----------|-------------|------|
| `wrapWithPanel` | Form 自带 panel chrome | Form 有显式 actions region | Flux 更灵活 |
| `submitText` | Form 内置提交按钮文本 | actions 区域手写按钮 | Flux 更灵活 |
| `inheritData`/`canAccessSuperData` | Form 隐式继承父 scope | `scopePolicy: 'form'` 硬隔离 | Flux 显式更安全 |
| `<onEvent>` | AMIS 原始事件 JSON | 统一事件字段（onClick/onChange 等） | Flux 统一值语义 |
| `onClick` (JS 代码) | 内联 JS | action 代数 | Flux 不允许 new Function |
| `<feedback>` | 操作后弹窗反馈 | `then: [{ action: "openDialog" }]` | Flux action 链更统一 |
| `<asyncApi>`/`<initAsyncApi>` | AMIS 长轮询 | `data-source.interval` + `stopWhen` | Flux 有更清晰的轮询模型 |

#### 修正后的三分类

```
Category A (渲染器中立结构): ~40 个属性 — view.xml 核心，AMIS/Flux 通用
Category B (AMIS 命名, 通用语义): ~15 个属性 — 可被输出层机械映射
Category C-1 (通用 UI 行为, Flux 已支持): 26 个属性 — Flux 有同名或类似属性
Category C-2 (通用 UI 行为, Flux 应补充): 12 个属性 — 建议向 Flux 提 feature
Category C-3 (AMIS 哲学差异, Flux 故意不支持): 7 个属性 — Flux 有替代方案
```

**修正结论**：view.xml 中 ~93% 的属性（A + B + C-1 + C-2）是**通用 UI 词汇**，不是 AMIS DNA。只有 ~7%（C-3）是真正的 AMIS 哲学差异，且 Flux 有替代方案。view.xml 的属性词汇设计比初始评估要**健康得多**。

### 8.4 不需要 view2.xdef 的理由

#### 理由 1：view.xml 的结构模型本身已经是渲染器中立的

三层模型 grid/form/page → crud/picker/tabs/wizard 是通用 UI 分解，AMIS 和 Flux 都有结构相同的组件（crud、form、page、dialog、drawer、tabs）。view.xml 的结构骨架与渲染器无关。

#### 理由 2：Category B 词汇差异是表面问题

`actionType` vs `action`、`level` vs `variant`、`<api>` vs `args`——这些是同一概念的命名差异。属性名的选择不影响结构模型的表达力。输出层做字符串映射的成本是 O(1)。

创建 view2.xdef 把 `actionType` 改名为 `action` 的收益是词汇美观，代价是维护两套 schema、两套 codegen 管线、两套 delta 定制文档、337 个文件迁移。**不划算。**

#### 理由 3：Category C 是死代码，不需要新 schema 来清除

50 个 AMIS 特有属性中，ERP 项目手写层零使用。它们的存在是因为 view.xdef 作为 AMIS 抽象层的历史积累。清除它们不需要新 schema——只需从 XDef 中删除或标记弃用。

#### 理由 4：codegen 模板是 AMIS 词汇的唯一活跃来源

`submitOnChange="true"` 和 `autoFillHeight="true"` 出现在 337 个 _gen 文件中，但全部来自 `view-gen.xlib` 模板硬编码。更新模板即可让新生成的视图不再包含这些属性。不需要新 schema。

#### 理由 5：view2.xdef 会制造生态碎片化

两套 view schema 意味着：
- 两套 XDef schema 维护
- 两套 view-gen codegen 管线
- 两套 delta 定制文档
- 工具链（IDE 插件、校验器、文档生成器）需要支持两种格式
- 开发者需要学习两种格式
- 迁移期间同实体的 AMIS 视图和 Flux 视图如何共存？

### 8.5 推荐：三阶段原地进化，不创建 view2.xdef

> **修正后的依据**（基于 §8.3a 审计）：view.xml 中 ~93% 的属性是通用 UI 词汇，不是 AMIS DNA。`submitOnChange` 等"看起来 AMIS 特有"的属性实际上是 Flux 也需要的通用 UI 行为，Flux 已支持大部分。

```
Phase 1 — Flux 集成（当前）
━━━━━━━━━━━━━━━━━━━━━━━━━━━
• view.xdef 不变
• flux-web.xlib 读取 Category A+B+C-1，忽略 C-3
• 对 C-2（Flux 尚不支持的通用行为），暂时降级处理或用 Flux 替代方案
• AMIS 侧 web.xlib 照常工作
• 零文件迁移

Phase 2 — Flux 补功能 + Codegen 清理（Flux 稳定后）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• 向 Flux 提 feature request 补齐 C-2 高优先级项：
  - autoFillHeight（表格自适应高度）
  - rowClassName / rowClassNameExpr（条件行样式）
  - promptPageLeave（未保存离开提示）
  - resetAfterSubmit（提交后重置）
  - checkOnItemClick（点击行选中）
• 更新 view-gen.xlib 模板，将 AMIS 特有属性（C-3）从 _gen 模板移除
  - 这些属性改由 web.xlib 在 AMIS 输出时自动注入
  - _gen view 只保留通用 UI 行为（A+B+C-1+C-2）

Phase 3 — Schema 清理（AMIS 退役后）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• 从 view.xdef 中移除 C-3 属性定义
• view.xdef 此时只保留通用 UI 词汇（A+B+C-1+C-2）
• web.xlib 和 AMIS 运行时可退役
```

### 8.6 Category B 词汇的长期处理

对于 Category B（AMIS 命名但语义通用的属性），有两种策略：

**策略 1（推荐）：保留现有名称，在文档中明确语义**

`actionType="dialog"` 在 view.xml 中的语义是"打开弹窗"。这个名字虽然是 AMIS 起源，但本身是一个合理的描述性命名。Flux 输出层将其映射为 `action: "openDialog"` 是机械转换。开发者不需要记住两套名字——他们只需要知道 view.xml 中写 `actionType`，Flux 输出中变成 `action`。

**策略 2（可选，长期）：逐步引入中性别名**

在 view.xdef 中允许同时使用 `actionType` 和 `open`（新增的中性名称），两者等价：

```xml
<!-- 现有写法（保持兼容） -->
<action actionType="dialog">
    <dialog page="add"/>
</action>

<!-- 新增的中性写法（可选） -->
<action open="dialog">
    <dialog page="add"/>
</action>
```

但这增加了复杂性而非减少。**不推荐。**

### 8.7 结论

view.xml 的词汇经过 Flux 组件 schema 全量审计后，~93% 被确认为**通用 UI 行为**而非 AMIS DNA。`submitOnChange`、`affixHeader`、`combineNum`、`tabsMode`、`tooltip` 等"看起来 AMIS 特有"的属性，Flux 实际上已支持或应支持。

真正需要 Flux 补齐的高优先级功能缺口是：`autoFillHeight`（表格自适应高度）、`rowClassName`（条件行样式）、`promptPageLeave`（未保存离开提示）——这些是 ERP 全部 CRUD 页面都需要的通用行为。

只有 7 个属性（`wrapWithPanel`、`submitText`、`inheritData`/`canAccessSuperData`、`<onEvent>`、`onClick` JS、`<feedback>`、`<asyncApi>`）是 AMIS 哲学差异，Flux 有更好的替代方案。

**view.xml 的属性设计比预期健康得多，不需要 view2.xdef。正确的做法是：保留通用词汇、向 Flux 补功能缺口、在输出层处理差异。**

---

## 参考

- nop-entropy 前端管道核心文件：
  - `nop-entropy/nop-kernel/nop-xdefs/src/main/resources/_vfs/nop/schema/xui/xview.xdef` — view.xml schema
  - `nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/web.xlib` — view→page 转换库（929 行）
  - `nop-entropy/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/control.xlib` — domain→control 映射库（1206 行）
  - `nop-entropy/nop-frontend-support/nop-ui/src/main/java/io/nop/xui/utils/XuiHelper.java` — 控件推导算法
- Flux 设计文档：
  - `nop-chaos-flux/docs/articles/flux-design-introduction.md` §10 分层职责、§12 宿主契约
  - `nop-chaos-flux/docs/migration/nop-chaos-next-react-19-flux-migration-plan.md` §5 双栈路由策略
  - `nop-chaos-flux/docs/components/amis-baseline-matrix.md` — 137 个 AMIS 类型映射
  - `nop-chaos-flux/apps/playground/src/complex-pages/page-schemas/standard-crud.json` — Flux CRUD 生产级示例
- nop-app-erp 前端实践：
  - `docs/architecture/view-and-page-strategy.md` — 页面策略与代码生成/手写边界
  - `docs/architecture/customization-capabilities.md` — 定制机制（非下划线扩展层 vs Delta）
