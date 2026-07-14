# nop-app-erp：面向 AI 的声明式全栈架构

> 用途：介绍文档，后续据此拟制 PPT 大纲
> 素材来源：19 份 `*.orm.xml`、32 份架构文档、18 个 skill、187 份计划、实际 Processor/BizModel/xbiz 代码
> 规模基线：154 模块 | 352 自有实体 | 18+1 业务域 | ~2890 测试 | 187 计划 | 22 天全绿（截至 2026-07 基线，见 `docs/testing/known-good-baselines.md`）

---

## 核心命题

本项目的架构回答了一个根本问题：**怎么让 AI 能在 22 天内产出 154 模块、352 实体、~2890 测试而不自相矛盾？**

答案不是"AI 很聪明"，而是**把整个系统的表达方式从"程序员的命令式"改成了"AI 友好的声明式"**。

以下按**设计维度**而非技术分层组织——每个维度解决 AI 开发的一个独立问题，维度之间没有堆叠顺序。

## 范式定位：AI 实习生，不是 AI 代理

在展开具体维度之前，需要明确这个架构的 AI 哲学定位。

当前主流的"AI 友好开发"有几种范式：
- **Cursor/Copilot + 传统 Spring Boot**：让 AI **更快地写更多**代码——提升打字速度，但不改变代码结构
- **低代码平台（Retool/Bubble）**：可视化拖拽——AI 读不了画板，专有锁定
- **AI 自治代理（AutoGPT/vibe coding）**：让 AI **自己决定**做什么——无监督，方向不可控

本项目的定位与此三者本质不同：**AI 当受监督的实习生，不是当自治代理。** 它的"AI 友好"意思是"让 AI 容易做**正确**的事"——通过护栏、技能、保护区、审计门，而非"让 AI 随心所欲"。

这恰恰是它能产出企业级 ERP（352 实体、业财一体）而非玩具应用的原因。vibe coding 产不出需要三轴状态分离和过账引擎的系统。AGENTS.md 中的 5 级自主权（`implement`、`plan-first`、`ask-first`、`research-only`、`blocked`，每个工作项独立打标）和 6 个保护区，就是对"AI 不能自作主张"的明确立法。

以下九个维度，本质上都是在回答："怎么让一个受监督的 AI 实习生，在 22 天内不出错地产出 154 模块？"

## 维度一：数据契约（一个文件承载一切）

**问题**：AI 怎么在一个地方完整理解一个业务域的数据契约？

### 1.1 ORM XML 即真相源

`module-purchase/model/app-erp-purchase.orm.xml` 同时包含：共享类型定义（`<domains>` 块）、数据字典（`<dicts>` 块，每个选项的 code/label/value/i18n-en:label）、实体列定义（`<columns>` 块）、字段到字典的绑定（`ext:dict`）。AI 读一个文件，就知道这个域的全部数据契约。

每个列有 `propId` 顺序编号、`code="UPPER_SNAKE"` 机器标识、`stdSqlType` + `stdDataType`、可选的 `domain` 引用和 `ext:dict` 引用。所有属性在同一文件里，不需要跨文件查。

### 1.2 `domains` 共享类型锚

```
<domain name="amount" stdSqlType="DECIMAL" precision="18" scale="2"/>
```

实体中 `<column name="totalAmount" domain="amount"/>` 自动继承类型。AI 看到 `domain="amount"` 就知道精度，不需要检查 352 个实体的每一条 `<column>`。域共享定义也包含 `code`、`name`、`quantity`、`unitPrice`、`exchangeRate`、`taxRate`、`delVersion`、`version`、`createdBy`、`createTime`、`updatedBy`、`updateTime`、`remark` 等。

### 1.3 数据字典全 VARCHAR + 语义编码

所有 19 域的每个字典：
- `valueType="string"`，不用 int/char/Java enum
- `code == value`（语义合一：`code="DRAFT"`、`value="DRAFT"`）
- 命名空间 `erp-<缩写>/<名>`

验证：采购 9 个字典、销售 8 个字典、财务 30 个字典、主数据 18 个字典——全部 `valueType="string"`，全部 `code == value`。

跨域共享使用 `wf/` 命名空间（如 `wf/approve-status`），这些字典在 Nop 平台的 `nop-wf` 模块中定义，不在业务域的 ORM XML 中。

AI 在 Java 写 `DOC_STATUS_DRAFT`，数据库存 `DRAFT`，API 返回 `DRAFT`，UI 显示"草稿"——同一字符串贯穿全链路，零映射成本。

### 1.4 标准字段模式

**每个实体**末端的分组（propId 依次递增）：

```
delVersion → version → createdBy → createTime → updatedBy → updateTime
```

（部分域中 `remark` 列可能跟在 `updateTime` 之后，非严格最后 6 个。）

**每个单据头**包含这些标准字段（顺序不固定，业务字段穿插其间）：

```
orgId + businessDate + currencyId + exchangeRate + docStatus + approveStatus
+ posted + postedAt + postedBy + approvedBy + approvedAt
```

实际顺序：业务标识（orgId, supplierId/customerId 等）→ 日期（businessDate）→ 财务（currencyId, exchangeRate, 金额）→ 状态（docStatus, approveStatus, 域特定状态）→ posted/postedAt/postedBy → 审批追踪（approvedBy/approvedAt）→ remark → 标准 6 框架字段。

AI 知道 ANY 单据实体一定有这些字段，不用查每个实体的定义。

### 1.5 `tagSet` 语义标签

`tagSet` 声明在 ORM XML 的实体或列上，AI 扫描即可判断特性：

| 标签 | 用在 | 含义 |
|------|------|------|
| `gid` | 所有实体 | 全局标识 |
| `erp.<domain>` | 所有实体 | 域归属（如 `erp.purchase`） |
| `use-approval` | 需审批实体 | 接审批流 |
| `seq-default` | 主键 id | 自增序列 |
| `pub` | `<to-one>` | 公开可导航关联 |
| `pub,cascade-delete,insertable,updatable` | `<to-many>` | 父子级联 |
| `audit,audit-save` | 审计字段 | 变更日志配置 |

### 1.6 跨模块引用契约

两种模式各司其职：
- **`notGenCode` 外部实体引用**：ORM 可导航。被引用的实体是轻量级存根（保留 FQN `app.erp.md.dao.entity.ErpMdMaterial`，只含 ID/name/基础字段，不含标准审计和 useLogicalDelete），通过 `biz:moduleId="erp/md"` 指向目标域。用于高频多维关联查询。
- **弱指针字符串三元组**：`billType + billCode + billLineCode`，无外键约束，用于跨域反查源单。

约 369 条引用白名单记录在 `data-dependency-matrix.md`。AI 新增跨模块引用时查白名单即可确定机制。

### 1.7 XDef 模式验证——XML 的编译时类型安全

每个 XML 配置文件引用 XDef schema，构建时自动验证结构、类型和约束：

| 文件类型 | Schema 路径 |
|---------|------------|
| ORM 模型 | `/nop/schema/orm/orm.xdef` |
| 视图 | `/nop/schema/xui/xview.xdef` |
| xbiz | `/nop/schema/biz/xbiz.xdef` |
| action-auth | `/nop/schema/action-auth.xdef` |
| data-auth | `/nop/schema/data-auth.xdef` |

Schema 定义每个元素和属性的允许值、默认值、类型及关系。例如 `orm.xdef` 通过 `xdef:ref` 引用 `entity.xdef`（约 227 行）、`dict.xdef` 等子 schema，共同定义 ORM 模型的完整结构——`<domains>`、`<dicts>`、`<entities>` 等子元素的格式、key 属性、必需字段和可选扩展属性。

**对 AI 的意义**：AI 写的 XML 在构建时被验证——属性名拼错、类型错误、缺少必需字段都会被 XDef schema 捕获。这相当于 XML 的"编译时类型检查"，确保 AI 生成的配置始终符合平台预期的格式。Schema 本身也是 Machine-readable 的。

---

## 维度二：传播引擎（单点变更自动扩散）

**问题**：AI 改了一个字段定义，有多少文件需要同步更新？

### 2.1 全链自动生成

AI 在 `*.orm.xml` 加 `ext:dict="erp-pur/doc-status"`，`mvn clean install` 自动传播：

```
*.orm.xml
  ↓ gen-orm → 实体 Java、DaoConstants（含字典常量如 DOC_STATUS_DRAFT="DRAFT"）
  ↓ gen-meta → XMeta（含 dict 引用 + 自动 _label 字段）
  ↓ gen-i18n → i18n YAML（dict.option.label.erp-pur/doc-status.DRAFT = "草稿"）
  ↓ gen-page → AMIS 视图（表格列 + 下拉选择框）
```

AI 只需要理解 ORM XML 一个文件格式。其余所有 Java 实体、i18n、UI 字典选择框自动生成。

### 2.2 生成层 vs 保留层（视觉边界）

| 层 | 前缀 | AI 能否编辑 |
|----|------|-----------|
| 生成层 | `_gen/`、`_` 前缀、`_*DaoConstants.java` | **禁止** |
| 保留层 | 无 `_` 前缀 | 可以 |

AI 看到 `_` 就知道不该碰，看到无前缀就知道是自己的地盘。

### 2.3 两阶段元编程——`_app.orm.xml` 模型聚合

每个域有生成文件 `_app.orm.xml`（dao 模块中），通过 `x:gen-extends` + `orm-gen.xlib` 在构建时从源模型生成最终 ORM 模型：

```
app-erp-purchase.orm.xml（源模型）
            ↓ 构建时：_app.orm.xml 调用 orm-gen:DefaultGenExtends
_app.orm.xml（聚合模型）
  - 继承源模型的 domains/dicts/entities
  - 平台为每个实体自动补充：useLogicalDelete、标准字段属性、
    registerShortName、关联关系标记等
  - x:post-extends 做最终转换
            ↓
app.orm.xml（薄委托文件，x:extends="_app.orm.xml"）
```

19 个域的 `_app.orm.xml` 聚合了各域的完整 ORM 模型。`app.orm.xml` 是薄委托层，仅用于定制扩展。

**对 AI 的意义**：AI 只需要编辑源模型 `app-erp-xxx.orm.xml`，构建时由两阶段元编程自动转换为最终 ORM 模型。`x:gen-extends` 做生成期转换（如根据 `ext:dict` 自动派生字典常量），`x:post-extends` 做最终期调整（如补充平台默认属性）。AI 不需要理解完整的 ORM 模型格式——只需要关注源模型。

### 2.4 可逆性——Delta 是"行为的 Git"

这个架构对"AI 一定会犯错"的正面回答是 Delta 机制。在传统代码里，AI 的错误编辑和正确代码混在同一个文件里，撤销意味着逐行 diff。而在这个架构里，AI 的每一次定制都是一个**可整体移除的差量**：

- 删掉一个 Delta 目录（`_vfs/_delta/{dir}/`），基线立刻无损还原
- 手写 view.xml 中的 `bounded-merge` 整块（`<cols x:override="bounded-merge">`）可以整体移除以恢复生成层默认列
- xbiz 手写层可以整体删除，退回生成层的默认行为

更关键的是 `nop.debug=true` 时 `_dump/` 目录输出的 `<!--LOC:...-->` 来源标注——合并后的每一行都能追溯到它的源文件。

**对 AI 的意义**：可逆性不是"锦上添花"，而是"敢让 AI 动手"的前提。没有可逆性的 AI 友好只是把风险放大；有了 Delta 的可逆性，AI 的工作变成了一系列可放弃的实验。AI 犯错时的污染永远是局部的、可定位的、可回滚的。

---

## 维度三：架构约束（防止 AI 走错路）

**问题**：用什么架构规则确保 AI 写的代码不自相矛盾？

### 3.1 决策优先级

```
Model → Delta → Java
```

能用 ORM XML 表达的不写 Java，能用 `x:extends` 覆盖的不改源文件。AI 决策顺序是硬编码的。

### 3.2 DAG 单向模块依赖

18+1 域独立 Maven 工程，依赖方向：`master-data ← inventory ← purchase/sales ← finance ← 扩展域`。循环依赖在架构上不可能发生。

### 3.3 三轴状态分离

所有业务单据状态由三个正交字段表达：

| 轴 | 所有权 | 枚举 |
|----|--------|------|
| `docStatus` | 各域自定 | 域定义的状态机 |
| `approveStatus` | 工程统一 | `UNSUBMITTED/SUBMITTED/APPROVED/REJECTED`，使用共享字典 `wf/approve-status`（Nop 平台 nop-wf 模块定义） |
| `posted` | 过账引擎 | `true/false + postedAt + postedBy` |

AI 不需要为每个域设计审批状态机。

### 3.4 跨域数据依赖类型

| 类型 | 语义 | 实现 |
|------|------|------|
| R | 只读主数据 | `notGenCode` + `<to-one>` |
| S | 同步写，同一事务 | `I*Biz` 接口 |
| P | 弱关联反查 | `billType + billCode` 字符串 |

AI 查 `data-dependency-matrix.md` 的 369 条白名单即可决策。

---

## 维度四：实现模式（AI 生成代码的模板）

**问题**：AI 怎么写代码才能保证 19 个域风格一致、不踩坑？

### 4.1 xbiz 单行委托

每个 xbiz 文件的业务动作只有一行：

```xml
<mutation name="submitForApproval">
    <source><c:script>
        return inject('app.erp.pur.service.processor.ErpPurOrderProcessor').submitForApproval(id, svcCtx);
    </c:script></source>
</mutation>
```

`inject(beanId)` 按 ID 查 Spring Bean，`id` 和 `svcCtx` 是 xbiz 上下文隐式变量。没有复杂 XLang 逻辑、没有条件分支、没有循环。纯委托。

实际代码验证：ErpPurOrder.xbiz（4 个动作全部单行委托）、ErpSalOrder.xbiz（同模式）——模式跨域一致。

### 4.2 I*Biz 接口分层（多重继承）

```
ICrudBiz<T>                  —— 标准 CRUD（save/update/delete/get/findPage/findList/batchGet）
IApprovableBiz<T>            —— 标准审批（submitForApproval/approve/reject/withdrawApproval/reverseApprove）
     ↑ 平行父接口 ↓
IErpPurOrderBiz extends ICrudBiz<ErpPurOrder>, IApprovableBiz<ErpPurOrder>
```

`IErpPurOrderBiz` 同时扩展两个父接口（非链式继承），因此同时具有 CRUD + 审批 + 域自定义方法。

注意：`IApprovableBiz` 的方法是 Java `default` 方法（含 `throw UnsupportedOperationException` 默认体，无 `@Name` 注解、无 Javadoc），与 `ICrudBiz` 和域自定义接口（含 `@Name` + Javadoc）不同。AI 应当知晓这一区别。

### 4.3 聚合根接口组织（DDD 契约）

每个域的业务方法按 DDD 聚合根组织：

```
ErpPurOrderBiz       ← 采购订单聚合根：订单相关的全部业务方法
ErpPurRequisitionBiz ← 请购单聚合根：请购单相关的全部业务方法
```

每个聚合根对应一个 `I*Biz` 接口（dao 模块，纯契约）和一个 `*BizModel` 实现（service 模块）。方法命名即语义文档：
- `createFromRequisition`（从另一聚合根创建）
- `cancelWithReason`（带原因的状态变更）

**对 AI 的意义**：AI 知道每个实体的业务方法一定在其对应 `I*Biz` 接口中。新增一个业务方法时，第一个问题就是"它属于哪个聚合根？"——实体名即答案。

### 4.4 ErrorCode 接口模式

```java
ErrorCode ERR_ORDER_NOT_FOUND = ErrorCode.define(
    "erp.err.pur.order-not-found",         // kebab-case，人可读
    "采购订单 {orderCode} 不存在",           // 中文模板，含义自明
    ARG_ORDER_CODE
);
```

`erp.err.<域>.<kebab-case>` 命名——错误码本身就是文档。中文模板含结构化占位符，AI 使用时知道需要哪些参数。

实际验证：ErpPurErrors.java（63-64 行）、ErpFinErrors.java（103-105 行）均使用此模式。

### 4.5 Processor 两层模式

```
Facade（I*Biz / BizModel）：@BizMutation 事务入口 + 参数校验 → 委托
Processor：protected 方法（派生可覆盖）+ IServiceContext 末参
    ↑ SPI 路由
I*Provider（各域实现）：ioc:collect-beans 自动注册
```

事务边界在 `@BizMutation` 层（xbiz mutation 触发时自动开启），Processor 自身**不携带** `@SingleSession` 或 `@Transactional`。`@SingleSession` 仅用于部分专用 Processor（如 ErpFinPostingProcessor 等需要显式 ORM Session 控制的场景）。

实际验证：ErpPurOrderProcessor.java 含 20 个 protected 方法、无 `@Transactional`、所有方法以 `IServiceContext` 末参——类注释明确说明"本类不带 @Transactional"。

反模式检查清单固化在 skill 中：跨域注入具体类（绕过 I*Biz）、Processor 自带 `@Transactional`、`private` 方法（不可覆盖）、缺 `IServiceContext` 参数。

### 4.6 审批配置门控

| 模式 | 配置代价 | 变更方式 |
|------|---------|---------|
| NONE | 不加 `use-approval` 标签 | 默认 |
| DIRECT | 加 `tagSet="use-approval"` | 一行 ORM 标签 |
| WORKFLOW | 再加 `wf:wfName` xmeta 属性 | 一行 xmeta 属性 |

DIRECT → WORKFLOW 升迁：**Processor 代码零改动**。同一套 Processor 在两种模式下都工作。

### 4.7 bounded-merge 视图定制

`view.xml` 使用 `x:override="bounded-merge"` 定制列表列和行操作按钮：**按 `id` 属性匹配子元素，只做一层合并，不递归嵌套**。匹配的覆盖，不匹配的追加。注意：bounded-merge 是 keep-only 过滤——生成层中存在但手写层未列出的列会**被删除**，只有手写层显式列出的列才保留在最终结果中。

实际验证：100+ 个手写 view.xml 使用 `bounded-merge` 用于 `<cols>` 和 `<rowActions>`。表单布局通常用 `replace` 完全重写。

**x:override 策略体系**：Nop 提供八种合并策略（`XDefOverride` 枚举定义），适用于不同的定制场景：

| 策略 | 行为 | 适用场景 |
|------|------|---------|
| `merge`（默认） | 递归合并，按 `id` 匹配子元素 | 配置文件的通用增量定制 |
| `bounded-merge` | 一层深度合并，按 `id` 匹配，不递归嵌套 | 列表列/行操作的定制 |
| `merge-replace` | 递归合并，但匹配的子元素整体替换而非合并 | 需要完全覆盖子元素的场景 |
| `merge-super` | 合并时优先保留父层值 | 回退到生成层默认值 |
| `replace` | 完全替换父节点内容 | 表单布局完整重写 |
| `remove` | 删除匹配 `id` 的元素 | 从生成层移除不需要的菜单/页面 |
| `append` | 无条件追加子元素到末尾 | 在列表末尾添加新操作 |
| `prepend` | 无条件前插子元素到头部 | 在列表头部添加操作 |

所有域的手写 action-auth.xml 使用 `remove` 移除生成层的测试菜单；手写 view.xml 使用 `replace` 重写表单布局、`bounded-merge` 定制列表。AI 按需选择策略，不需要自己实现合并逻辑。

### 4.8 CRUD 技术子空间（纯配置驱动）

CRUD 在架构中被视为一个独立的技术子空间——它不包含业务逻辑，完全由配置驱动：

- ORM XML 中定义实体 → codegen 自动生成全套 ORM + XMeta + i18n
- `x:gen-extends` + `biz-gen.xlib` 在 xbiz 层绑定 GraphQL 操作到实体
- 框架 `CrudBizModel` 已提供完整的 Java 实现：`save`、`update`、`delete`、`get`、`findPage`、`findList`、`findFirst`、`batchGet`、`findCount`、`batchDelete`、`batchModify`、`saveOrUpdate` 等（详见 `ICrudBiz` 接口）
- AI 永远不需要手写 CRUD——BizModel 继承 `CrudBizModel` 即可，这些方法是框架的 Java 实现而非 xbiz 生成的存根

**对 AI 的意义**：CRUD 是"不需要 AI 思考"的部分。AI 只需要关注非 CRUD 的业务方法（提交审批、从请购单创建订单等）。框架自动提供的 CRUD 承担了绝大多数（多数实体超过半数）的数据操作。

### 4.9 xbiz 优先于 Java（配置覆盖代码的定制链）

定制链优先级（从高到低）：

```
手写 xbiz（最高，无需编译部署）
    ↑
Java BizModel（可覆盖生成层行为，需编译）
    ↑
生成 _*.xbiz（最低，默认 CRUD + approval-support 继承）
```

AI 从轻到重选择定制路径：改 xmeta（最轻）→ 改 ORM XML（次轻）→ Java BizModel 覆写 → 手写 xbiz（最重但不需要编译）。

### 4.10 xbiz 元编程与横向特性注入

xbiz 继承链支持通过元编程注入横向行为。实际机制如下：

```
_ErpPurOrder.xbiz（生成层）
  ├─ x:extends="/nop/wf/base/approval-support.xbiz"  ← 审批方法注入
  └─ x:gen-extends + biz-gen.xlib                      ← CRUD 桩生成
        ↓
ErpPurOrder.xbiz（手写层，x:extends="_ErpPurOrder.xbiz"）
```

`approval-support.xbiz` 定义 5 个标准 action（不是文档此前声称的 4 个）：
`submitForApproval`、`withdrawApproval`、`approve`、`reject`、`reverseApprove`

这 5 个 action 不是单行委托——它们包含完整的 XLang 逻辑（状态校验、条件 WF 启动、时间戳设置）。`withdrawApproval` 是文档此前遗漏的 action。

**元编程的真正位置**在 `x:gen-extends` + `biz-gen.xlib`，而非 ORM XML 标签。`biz-gen.xlib` 中的 `GenAbstractCrudMethods` 模板根据 XMeta 配置生成 CRUD 抽象桩。

**对 AI 的意义**：AI 在 xbiz 继承链中声明 `x:extends` 即可注入整套横向特性（审批、日志、通知等）。不需要手写这些模板式的业务方法。特性以可继承的 xbiz 基文件形式存在，AI"搭积木"即可。

---

## 维度五：隐式运行时（声明即运行行为）

**问题**：AI 写字段名和类名就够了，剩下的谁管？

### 5.1 视图字段与 xmeta 映射

手写 view.xml 中列出的字段名映射到 xmeta 定义：

```xml
<col id="orgName" label="业务组织"/>     ← 手写视图：引用 xmeta 中定义的属性
<col id="supplierName" label="供应商"/>
```

生成的 `_gen/_ErpPurOrder.view.xml` 包含原始 ID 列（`<col id="orgId" ui:number="true"/>`），手写视图通过 bounded-merge 用名称后置的列（`orgName`、`supplierName`）覆盖它们，显式赋予 `label`。

框架从 xmeta 的 `<to-one displayName="orgName">` 配置推断 GraphQL 查询展开，在 RPC 响应中自动包含 `{value, label}` 结构，前端据此显示名称而非 ID。

**对 AI 的意义**：AI 写 view.xml 时，在 xmeta 中定义过 displayName 的关联字段可以映射为名称显示列。但手写视图仍需要显式列举这些列——框架在运行时处理 RPC 展开，不在 view.xml 层面做自动推断。

### 5.2 关联 label 的 RPC 链路

`view.xml` 的 `orgName` 在运行时触发完整链路：

```
view.xml 列出 orgName 列
  → xmeta 声明 <to-one displayName="orgName" name="org">
  → GraphQL 查询自动展开 org { value, label: orgName }
  → 后端 ORM 自动 join 关联表取组织名称
  → 前端显示组织名称
```

AI 不需要写 GraphQL 展开查询、不需要处理关联加载。但 view.xml 需要显式列出名称列，框架只负责运行时数据获取。

### 5.3 启动时全页面结构验证

`PageProvider` 启动时调用 `validateAllPages()`：扫描所有 `.page.yaml` 文件，确保 YAML/JSON 结构正确、页面文件可编译加载。验证确保页面文件的结构完整性，但不做字段级 xmeta 引用检查。

**对 AI 的意义**：AI 在 view.xml 中写错 YAML 语法或页面结构 → **启动即报错**，不是运行时才暴露。但字段名引用错误不会在启动时被此机制捕获。

### 5.4 后端无显式事务 / 无 Session 管理

```java
@BizMutation          ← AI 标记这是写操作（框架自动开启事务）
public ErpPurOrder submitForApproval(@Name("id") String id, IServiceContext ctx) {
    ErpPurOrder entity = dao().getEntityById(id);
    entity.setDocStatus(DOC_STATUS_ACTIVE);
    dao().updateEntity(entity);
    return entity;
}

@BizQuery             ← AI 标记这是读操作（框架自动管理 Session）
public List<ErpPurOrder> findPending() {
    return dao().findAll(...);
}
```

不需要关心：
- `@Transactional`（`@BizMutation` 自带事务边界）
- `@SingleSession`（框架按 GraphQL 请求自动管理 Session。仅限部分专用 Processor 需要显式 `@SingleSession`）
- 事务传播（全部 REQUIRED，显式 `REQUIRES_NEW` 需在 plan 中记录理由）
- 懒加载异常（同一 Session 内的关联访问框架自动处理）

**对 AI 的意义**：AI 写后端方法只需要区分"这个是写操作（Mutation）还是读操作（Query）"。

### 5.5 GraphQL+NopORM 自动对象关联

AI 在前端或 API 层展开关联字段时，不需要写 JOIN 或嵌套查询代码：

```graphql
query {
    ErpPurOrder__get(id: 123) {
        code
        supplier { name, code }           ← 自动 join erp_md_partner
        lines { material { name, spec } } ← 自动多表 join
        createTime
    }
}
```

NopORM 从 GraphQL 请求的嵌套结构自动推导 ORM 关联路径，生成最优 SQL。N+1 问题由平台 ORM 层的批处理机制自动处理。AI 不需要写 JPQL、Criteria API 或手写 mapper。

### 5.6 ORM 层横切特性（声明式配置）

逻辑删除、乐观锁、审计字段在实体级声明式配置：

```xml
<entity name="ErpPurOrder" tableName="erp_pur_order"
        useLogicalDelete="true"           ← 逻辑删除：delete 转 UPDATE
        deleteFlagProp="delVersion"
        deleteVersionProp="delVersion"
        versionProp="version"             ← 乐观锁
        createTimeProp="createTime"
        createrProp="createdBy"
        updateTimeProp="updateTime"
        updaterProp="updatedBy">
```

框架在运行时自动：
- 逻辑删除：`delete` 操作自动转 `UPDATE ... SET delVersion=1`，查询自动排除已软删行
- 乐观锁：`version` 字段自动参与 `UPDATE ... SET version=version+1 WHERE version=?`
- 审计字段：新增/修改时自动填充创建人/创建时间/修改人/修改时间

**对 AI 的意义**：AI 在 ORM XML 中加实体级属性，框架自动处理全部横切逻辑。不需要在 BizModel 中写软删过滤、版本检查和审计字段填充。

（注：本项目 ORM XML 中未启用多租户配置。Nop 平台内置多租户能力，在实体级声明 `useTenant="true"` 即可启用——平台自动添加 `nopTenantId` 列、在查询中追加租户过滤、新增时自动填充当前租户。）

### 5.7 编码规则（codeRule）— 内置支持

业务单据编码（如采购订单号 `PO-202607-0001`）由内置的编码规则引擎统一管理，在 XMeta 的 `<prop>` 上声明式配置：

```xml
<!-- 在 XMeta 中配置，不是 ORM <column> -->
<prop name="code" biz:codeRule="ErpPurOrder@code"/>
```

`biz:codeRule` 的值是规则名称引用（如 `ErpPurOrder@code`），实际编码模式（如 `PO-{@year}{@month}-{@seq:4}`）配置在数据库 `nop_sys_code_rule` 表的 `codePattern` 字段中。

规则引擎内置变量（全小写，`{@...}` 分隔符）：
- 日期段：`{@year}`、`{@month}`、`{@dayOfMonth}`、`{@hour}`、`{@minute}`、`{@second}`
- 自增序列：`{@seq:4}`（4 位流水号）
- 随机数：`{@randNumber}`
- 属性引用：`{@prop:fieldName}`（引用实体属性值）

自定义变量可通过 `addVariable()` 注册。

AI 不需要在 BizModel 中写 `synchronized` 代码块、不需要查重号段、不需要写 `SELECT MAX(code)` + 1 的自增逻辑。编码规则由框架在实体保存前自动解析和填充。

### 5.8 autoExpr 与 orm-interceptor（自动触发机制）

框架提供两种声明式的自动触发机制，不需要在 Java 中编写拦截器代码。

**autoExpr** — 在 XMeta 中声明自动表达式（不在 ORM 中），在实体状态变化时自动计算：

```xml
<!-- 在 XMeta <prop> 中配置，不是 ORM <column> -->
<prop name="totalAmount">
    <autoExpr when="quantity,unitPrice">
        quantity * unitPrice
    </autoExpr>
</prop>
```

标签名是 `<autoExpr>`（camelCase，非 `auto-expr`），`when` 属性指定触发字段。支持场景：默认值填充、派生字段计算、条件表达式。

**orm-interceptor** — 独立的 `.orm-interceptor.xml` 文件，用 kebab-case 标签声明 XLang 脚本作为拦截动作（不是嵌在 `<entity>` 内的 Java class 引用）：

```xml
<!-- ErpPurOrder.orm-interceptor.xml -->
<interceptor x:schema="/nop/schema/orm/orm-interceptor.xdef">
    <pre-save id="fillCode" order="100">
        <source>
            entity.code = 'PO-' + StringHelper.generateUUID()
        </source>
    </pre-save>
    <post-load id="fillDisplay" order="200">
        <source>
            entity.orgName = inject('app.erp.md.biz.IErpMdOrganizationBiz')
                .get(entity.orgId, true, svcCtx)?.name
        </source>
    </post-load>
</interceptor>
```

共 8 个拦截点：

| 拦截点 | 触发时机 | 典型用途 |
|--------|---------|---------|
| `pre-save` | 保存前 | 填充编码、校验业务状态 |
| `post-save` | 保存后 | 级联通知 |
| `pre-update` | 更新前 | 更新冗余字段 |
| `post-update` | 更新后 | 记录变更 |
| `pre-delete` | 删除前 | 级联检查 |
| `post-delete` | 删除后 | 清理关联数据 |
| `post-load` | 加载后 | 填充派生字段 |
| `post-reset` | 实体重置后 | 恢复默认值 |

**对 AI 的意义**：AI 不需要在 BizModel 中写 `@PostLoad`、`@PrePersist` JPA 生命周期方法。autoExpr 在 XMeta 中声明字段自动计算；orm-interceptor 在独立 XML 中声明生命周期拦截——两者都是声明式配置，行为可预测、可审计。

### 5.9 xlib 抽象库——字段/实体级别的声明式横切

xlib（XLang Library）是 Nop 平台的声明式抽象单元，是对字段级、实体级局部逻辑进行横切抽象的核心机制。它包括两类：

**control.xlib** — 根据字段的 domain / type / meta 配置，自动决定 UI 控件类型：

```
domain="amount"      → control.xlib 解析为金额输入框（带千分位、小数精度）
domain="code"        → 解析为文本输入框（带编码校验）
domain="email"       → 解析为邮箱输入框（带格式校验）
ext:dict="erp-pur/doc-status" → 自动解析为下拉选择框（选项来自字典）
```

AI 在 view.xml 中只需声明字段名和 domain，不需要指定 `type="input"` 或 `type="select"`——control.xlib 根据字段的声明式配置自动推导 UI 控件类型。

**meta-prop.xlib** — 在 XMeta 层面提供属性级别的语义抽象，自动推导字段属性：

```
field 映射到 to-one 关联 → meta-prop.xlib 自动生成 displayName 配置
field 映射到 dict 字段    → 自动生成 _label 字段和下拉选项
field 映射到日期字段      → 自动设置 datePattern 和控件类型
```

**自定义 xlib 扩展**：任何域都可以创建自己的 xlib，引入新的声明式抽象。例如：
- `wf.xlib`：对审批相关属性提供语义标记
- `audit.xlib`：对审计追踪属性提供声明式配置
- `domain-specific.xlib`：对特定域的横切逻辑进行封装

xlib 可以叠加组合：一个字段同时被 `control.xlib`（决定 UI 控件）、`meta-prop.xlib`（推导 XMeta 属性）、`wf.xlib`（标记审批行为）处理，互不冲突。

**对 AI 的意义**：AI 不需要为每个字段的 UI 控件类型、属性推导、横切行为编写代码。这些由 xlib 层组合式处理——`domain=data` → control.xlib 给出文本输入框；`ext:dict=xxx` → 自动变为下拉选择框。如果要引入新的横切逻辑，创建一个 xlib 即可，不需要改 Java 代码。

---

## 维度六：整体性信息（单文件全貌）

**问题**：AI 怎么在不读完 18 个域的情况下理解整体的业务组织？

### 6.1 action-auth.xml——菜单结构与业务能力清单

每个域有手写 `erp-<domain>.action-auth.xml` 定义菜单结构和业务能力清单，手写文件继承生成文件：

```xml
<auth x:extends="_erp-pur.action-auth.xml"
      x:schema="/nop/schema/action-auth.xdef" ...>
    <site id="main">
        <resource id="erp-pur" resourceType="TOPM" displayName="采购管理"
                  icon="shopping-cart" orderNo="200"
                  routePath="/erp-pur" component="layouts/default/index">
            <resource id="erp-pur-order" resourceType="SUBM" displayName="采购订单"
                      icon="shopping-cart" orderNo="210">
                <resource id="ErpPurOrder-main" resourceType="SUBM" displayName="采购订单"
                          icon="shopping-cart" component="AMIS" orderNo="211"
                          url="/erp/pur/pages/ErpPurOrder/main.page.yaml"
                          app:useCases="UC-PUR-01"/>
            </resource>
            <resource id="erp-pur-receive" resourceType="SUBM" displayName="采购收货"
                      icon="import" orderNo="220">
                <resource id="ErpPurReceive-main" resourceType="SUBM" displayName="收货单"
                          icon="import" component="AMIS" orderNo="221"
                          url="/erp/pur/pages/ErpPurReceive/main.page.yaml"
                          app:useCases="UC-PUR-02"/>
            </resource>
        </resource>
    </site>
</auth>
```

层级结构：`site#main → TOPM（域根菜单）→ SUBM（功能分组，不带 url）→ SUBM（页面叶子节点，含 url + component="AMIS"）`。

这个文件同时承担：
- **菜单结构**：四级嵌套定义 UI 组织
- **业务能力清单**：该域所有可访问的功能页面
- **需求追溯**：`app:useCases` 属性以用例代码（如 `UC-PUR-01`）关联到业务设计文档
- 生成层（`_erp-pur.action-auth.xml`）自动为每个页面添加 `FNPT:Entity:query`/`mutation` 权限节点

**对 AI 的意义**：AI 读一个 `action-auth.xml` 就知道一个域的全部业务能力边界——有哪些功能、菜单怎么组织、入口 URL 是什么。不需要翻多份文档。

### 6.2 数据权限独立配置

数据权限独立配置在 `*-data-auth.xml` 中，与业务代码分离。文件结构如下：

```xml
<data-auth x:schema="/nop/schema/data-auth.xdef" ...>
    <objs/>
</data-auth>
```

项目各域的 `data-auth.xml` 均已就位，但过滤规则尚未实施（`<objs/>` 为空）。权限控制目前主要通过操作权限（action-auth.xml 的 `FNPT:*` 权限节点）而非数据行级过滤实现。

**对 AI 的意义**：数据权限与业务代码分离的机制已就位。AI 写 BizModel 方法时不需要嵌入权限逻辑。当需要实施行级数据权限时，在 `data-auth.xml` 中添加过滤配置即可，业务代码零改动。

### 6.3 操作权限：动作定义即权限点

`action-auth.xml` 的生成层自动为每个页面创建 `FNPT:Entity:query`/`mutation`/`action:xxx` 权限点。AI 在 view.xml 中引用 `action="create"`，框架自动检查权限。不需要在 Java 或前端写额外权限检查代码。

---

## 维度七：发现与导航（AI 怎么找到正确的地方）

**问题**：AI 怎么知道某个文件在哪里、某个 Bean 叫什么名字？

### 7.1 VFS 可预测路径

```
_vfs/erp/pur/model/ErpPurOrder/_ErpPurOrder.xbiz  ← 生成
_vfs/erp/pur/model/ErpPurOrder/ErpPurOrder.xbiz   ← 手写
```

模式：`_vfs/<moduleId>/model/<EntityName>/<File>`。AI 知道任何实体 `ErpPurXxx` 的 xbiz 一定在那个路径下，不需要搜索。（物理路径：`<module>/<service>/src/main/resources/_vfs/...`）

路径中 `_` 前缀 = 生成，无前缀 = 手写，视觉区分零成本。`_module` 空文件标记模块边界。每个实体目录下都是成对文件（生成 + 手写）。

### 7.2 显式 Bean 注册

所有自定义 Bean 在 `_vfs/<moduleId>/beans/app-service.beans.xml` 中显式声明。没有 `@ComponentScan`、没有自动类路径扫描。

bean id = 全限定类名（`app.erp.pur.service.processor.ErpPurOrderProcessor`），与 `inject()` 和 Java import 一致。AI 不需要记两套名字。SPI 用 `ioc:collect-beans` 自动收集。

### 7.3 docs 路由表

`docs/index.md` 包含约 30 行的"如果你需要...→ 首先阅读 → 然后阅读"路由表。默认路径：`context → backlog → input → requirements → design/architecture → task routing → plans → audits → logs → bugs`。

---

## 维度八：AI 治理（控制 AI 行为的规则体系）

**问题**：怎么保证 AI 不偏离方向、不自作主张、不失忆？

### 8.1 AGENTS.md 规则体系

| 类别 | 数量 | 用途 |
|------|------|------|
| 决策架构规则 | 22+ | 定义 AI 如何做决定 |
| 操作规则 | 15 | 定义日常行为模式 |
| Nop 平台规则 | 8 | 平台编码约束 |
| 规划触发条件 | 7 | 什么情况下必须写计划 |
| 强制技能加载 | 18 skill | 编写代码前必须加载匹配技能 |

### 8.2 13 阶段默认工作流

当任务触发"非平凡"条件时，AI 必须遵循：材料收集 → 需求澄清 → 综合 → 设计拆分 → 技能选择 → 计划起草 → 独立审计 → 实现 → 验证 → 结束审计 → 日志 → Bug 笔记 → 回顾。**不允许从想法直接跳到代码。**

### 8.3 5 级自主权 + 6 个保护区

每个工作项独立打标为 5 个自主级别之一（非逐级流转的管道，默认 `implement`）：

| 级别 | 含义 |
|------|------|
| `implement` | 直接实施 |
| `plan-first` | 可写计划，审计 + 批准后实施 |
| `ask-first` | 必须问人工（ORM XML、API XML、数据删除） |
| `research-only` | 只能调研 |
| `blocked` | 不能继续 |

保护区附加"必需证据"条件（如 ORM 修改需 design doc + plan audit）。AI 改每类文件前知道要准备什么。

### 8.4 [MISSION_DRIVER] 授权令牌

人工显式指令绕过保护区约束，针对性授权记录在计划中，可追溯。

### 8.5 计划-审计双门控

```
计划草案 → 独立草案审计（全新子代理会话） → 执行 → 独立结束审计 → completed
```

### 8.6 真相源层次

```
实时代码/模型 > 所有者文档 > 需求 > 原始输入
```

### 8.7 合规检查器（工具化反模式检测）

`docs/audits/nop-compliance-checker.sh`——10 条反模式规则固化为可重复运行的脚本：

| 规则 | 查什么 | 严重性 |
|------|--------|--------|
| R1 | `dao().saveEntity()` 在 BizModel | 🔴 |
| R2 | `daoFor()` 跨域 | 🔴 |
| R3 | `new Erp*()` 而非 `newEntity()` | 🟡 |
| R4 | `extends RuntimeException` | 🟢 |
| R5 | `@Inject private` | 🟡 |
| R6 | `@Transactional` + `@BizMutation` | 🟢 |
| R7 | `System.currentTimeMillis()` | 🟢 |
| R8 | Processor 无 xbiz 接线 | 🔴 |
| R9 | `doReverseApprove` 跨 Processor 不一致 | 🟡 |
| R10 | `REQUIRES_NEW` 无文档化理由 | 🟡 |

AI 在结束审计前运行即可自检。

### 8.8 强制技能加载 + 反模式自检

18 个 skill，每个包含路由表、代码模式、反模式自检清单。AI 编写代码前必须扫描并加载匹配的技能。实现后使用技能提供的自检机制验证无违规。

### 8.9 内部信息循环

```
Backlog → Plan → 执行 → Log → 发现问题 → Bug/Audit → 整改 → Skill/Lesson → 新 Plan
```

失忆由日志（`docs/logs/` 按日期排列）、skill（可复用经验固化）、lesson（Bug 提取的持久经验）共同缓解。

---

## 维度九：质量保障（AI 怎么验证自己没写错）

**问题**：AI 写了代码，怎么证明它是对的？

### 9.1 快照录制/回放（基于 JunitAutoTestCase）

| 模式 | 做什么 |
|------|--------|
| RECORDING | 执行测试，实际输出录为快照文件，人工审查 |
| CHECKING（默认） | 执行测试，与已有快照比较，不一致则失败 |

三层验证叠加：
1. 显式 assertEquals 断言关键业务字段
2. `output()` 录制完整 GraphQL 响应
3. 框架自动录制数据库状态到 `output/tables/*.csv`

AI 录一次正确结果，之后自动回归保护。基类 `JunitAutoTestCase` 自动注入 `IGraphQLEngine`、管理 `@var` 变量机制、处理录制/回放模式切换。

### 9.2 每测试 CSV 数据隔离

`_cases/<pkg>/<TestClass>/<method>/input/tables/*.csv`——每个测试方法独立一份 CSV 数据。输出 CSV 由 RECORDING 模式自动录制，与 input/ 形成"输入 → 预期输出"的黄金对。

目录位置：`<module>/<service>/_cases/`（模块根目录下，非 `src/test/resources/`）。

在实践中，本项目的大多数端到端测试使用 Java helper 方法（如 `newOrder("PO-POST-001")`）构造数据，而 `input/tables/` 下的 CSV 文件常常仅有表头行——主要定义表结构用于输出录制验证，而非提供测试输入。纯 CSV 驱动的测试在此仓库中较少，多数测试采用 Java 编程式数据设置 + CSV 录制输出验证的组合模式。

### 9.3 部署种子数据（`_init-data/`）

`app-erp-all/src/main/resources/_vfs/_init-data/` 包含约 91 个 CSV 文件 + 1 个 SQL 文件，覆盖所有域的业务基础数据：

```
_init-data/
├── erp_md_organization.csv       ← 组织架构
├── erp_md_partner.csv            ← 往来单位
├── erp_md_material.csv           ← 物料主数据
├── erp_md_currency.csv           ← 币种
├── erp_md_subject.csv            ← 会计科目
├── erp_pur_order.csv             ← 采购订单示例
├── erp_sal_order.csv             ← 销售订单示例
├── erp_fin_voucher.csv           ← 凭证示例
├── ...
└── zz-sequence-advance.sql       ← 序列初始化
```

种子数据与测试数据分离：`_init-data/` 是部署时加载的基线数据，`_cases/input/tables/` 是测试方法专用的输入数据。AI 不需要在 Java 中写 `@BeforeEach` 数据初始化——所需的数据基线在 CSV 中声明即可。

**对 AI 的意义**：部署种子数据与测试数据使用**相同的 CSV 格式**。AI 只需写 CSV 行即可添加部署数据或测试数据，不需要写 SQL INSERT、不需要写 Java 构造代码、不需要区分"这是部署脚本还是测试数据"。

### 9.4 审计体系 8 阶段演进

单代理 → 3 路并行 → 4 路对抗性（语义保真）→ 工具化合规检查器 → 实现率审计 → 综合审计+自省 → 竞争兑现核实。

---

## 失败模式：声明式在哪里泄漏

以上九个维度讲述了架构如何保护 AI。诚实地讲清楚"架构在哪里**停止**保护你"同样重要——这才是 AI 不踩坑的完整地图。

这个架构的所有失败模式归结为一种：**声明式泄漏**——当你被迫从"声明"掉到"命令"时，保护就消失了。三类典型泄漏：

| 泄漏类型 | 发生条件 | 实际案例 |
|---------|---------|---------|
| **生成器没接住的声明** | 平台有抽象但 codegen 模板没用它 | 12 个域 60+ 处 `LocalDateTime.now()` 直接泄漏（应用 `CoreMetrics.currentTimeMillis()`），导致测试时间不可控（已于 2026-07-08 全部修复，列此为例说明 codegen 模板曾有的泄漏） |
| **I*Biz 没有合身方法时的逃生舱** | 跨域操作需要 `I*Biz` 没有提供的方法，AI 被迫用 `IDaoProvider` 直接写目标域表 | MRP→采购、返利→贷项发票两个场景，绕过了审批管道和数据校验。每个豁免在 `posting-exemptions.md` 中显式记录，标注收敛条件 |
| **声明了但不执行** | 某些配置标签在 schema 层能解析、运行时却是 dead 的 | xbiz `<observes>` 标签在当前 nop-entropy 版本仅 schema 解析、运行时未触发 |

**对 AI 的意义**：AI 需要知道架构的保护区**不等于**平台的全部能力。遇到 `IDaoProvider` 直接跨域写入时，必须在 plan 中记录豁免理由和收敛条件。遇到"声明了但运行时不生效"的标签，查 `docs/bugs/` 和 `docs/lessons/` 确认实际行为。本项目已知的失败模式已固化在 `docs/skills/README.md` 的"已知失败模式"8 条和 `docs/lessons/` 中，AI 实现前应查阅。

---

## 总结：九个维度的正交设计

```
数据契约  —— 在一个文件里完整理解数据
传播引擎  —— 改一处自动更新全部
架构约束  —— 防止走错路的护栏
实现模式  —— 写每类代码的标准模板
隐式运行时 —— 写字段名和类名就够了，框架管剩下的
整体性信息 —— 从一个文件知道一个域的全部业务能力
发现导航  —— 知道文件/Bean/文档在哪
AI 治理   —— 规定 AI 能做什么/不能做什么
质量保障  —— 证明 AI 写对了
```

**这九个维度解决九个独立的问题，没有层序先后。** 它们共同的效果是：**每个维度都在减少 AI 需要做的决策数量**——不是靠"给 AI 写了更好的 prompt"，而是靠把系统的表达方式从命令式改成了声明式。

| 维度 | 减少的 AI 决策 |
|------|--------------|
| 数据契约 | 不需要在 Java/DB/文档间对账，不需要维护 int↔string 映射 |
| 传播引擎 | 不需要手写 getter/setter/i18n/UI 字典 |
| 架构约束 | 不需要考虑循环依赖、不需要为每个域设计审批状态机 |
| 实现模式 | 学会一个域的写法就学会所有域，不可能踩的坑已经写在反模式清单里 |
| 隐式运行时 | 不需要写事务/Session/JOIN/N+1 处理代码 |
| 整体性信息 | 不需要翻遍 18 个域来理解业务能力，一份 action-auth.xml 就是业务地图 |
| 发现导航 | 不需要搜索文件/Bean/文档，路径和名字都是可预测的 |
| AI 治理 | 不需要猜测"我能不能改这个"、"该读哪个文档"、"做完之后还要做什么" |
| 质量保障 | 不需要手工写期望值，录一次自动回归；写测试数据 = 写 CSV |

*本文件是 `docs/ppts/` 下的介绍文档，后续据此拟制 PPT 大纲。*
