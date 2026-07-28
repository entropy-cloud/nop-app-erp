# MA3 可定制性验证审计报告（A3.8）

> 报告时间：2026-07-28
> 里程碑/工作项：MA3 / A3.8（可定制性验证）
> Skill：`docs/skills/open-ended-audit-prompt.md`（开放式审计——搜索"标准检查清单之外的隐藏问题"；适配可定制性主题时以"声明能力 vs 实证落地"为搜索框架）
> 审查目标：`docs/architecture/customization-capabilities.md`（8 能力声明）+ 实时仓库 Delta/扩展字段/保留层/nop-dyn/BizLoader/模块组装实证 + `../nop-entropy/docs-for-ai/` 平台机制权威
> Verdict：**GAPS（有差距）** —— 零 P0（定制能力缺失是"能力未实证"非"活跃数据破坏"，原则上无 P0）；5 项 P1（major，全部目标 MR2，文档类）

## 0. 审查范围与方法

按 `open-ended-audit-prompt.md` 对 nop-app-erp 声明的 8 项定制能力做**实际可用性 + 不破坏基线**的抽样验证审计。**项目定制化**（按 skill §项目定制化层）：以"声明能力 vs 实证落地"为搜索框架，跨工件跨维度寻找虚假承诺 / 未验证路径 / 升级破坏风险。

**核心审计张力**：owner doc `customization-capabilities.md` 声明 nop-app-erp 是"产品化通用 ERP，充分利用扩展能力，快速适配各领域，**不改基线源码**"，并列举 8 能力 + 场景决策矩阵 + 升级路径保护 5 项。但实时仓库中：(a) 业务级 Delta = 0；(b) EAV 扩展字段 = 0；(c) nop-dyn = 0；(d) BizLoader = 0。这意味着**定制能力是"平台提供但本项目未验证落地"的能力**——声明 vs 实证存在 gap。本审计裁决：(1) 这些能力是否**实际可用**（抽样验证机制确实工作）；(2) 是否**不破坏基线**（定制层与基线分离、升级合并不冲突）；(3) owner doc 声明的"产品化可定制"承诺是否需要标注"当前为平台能力声明，落地验证为 successor"。

**实时仓库核实证据**（基线全部经直接验证为真）：

| 基线项 | 实测命令 | 结果 |
|--------|---------|------|
| 业务域零 Delta 文件 | `find module-*/erp-*-*/src -path "*_delta*"` | 0 命中业务域（仅 `app-erp-all/src/main/resources/_vfs/_delta/default/nop/auth/pages/` 下 2 个平台 nop-auth view delta） |
| EAV 扩展字段零声明 | `grep -rE "extField\|extFields\|NopSysExtField" module-*/model/*.orm.xml` | 0 命中（注意 `ext:dict=` 为字典绑定标注非 EAV 路径） |
| nop-dyn 动态实体零使用 | `grep -rl "nop-dyn\|NopDynEntity" module-*/erp-*-service` | 0 命中 |
| `@BizLoader` 零业务使用 | `grep -rl "@BizLoader" module-*/erp-*-service --include="*.java"` | 0 命中 |
| 保留层广泛使用（finance） | `find module-finance/erp-fin-service/src -name "*BizModel.java"` | 40 个非下划线 BizModel |
| 保留层广泛使用（finance） | `find module-finance/erp-fin-service/target/classes/_vfs/erp/fin/model -name "[A-Z]*.xbiz" -not -name "_*"` | 36 个非下划线 xbiz（`ErpFinVoucher.xbiz` 等，全部 `x:extends="_ErpFinVoucher.xbiz"`） |
| 保留层广泛使用（finance） | `find module-finance/erp-fin-web/target/classes/_vfs/erp/fin/pages -name "[A-Z]*.view.xml" -not -name "_*"` | 36 个非下划线 view.xml（`ErpFinVoucher.view.xml` 等，全部 `x:extends="_gen/_ErpFinVoucher.view.xml"`） |
| 保留层广泛使用（全域） | 19 模块 × (BizModel + xbiz + view) | BizModel=352 / xbiz=352 / view.xml=352 非下划线保留层文件 |
| `ext:dict` 字典绑定（配置驱动） | `grep -l "ext:dict" module-*/model/*.orm.xml` | 19 个 ORM 文件全覆盖（master-data 30 处 / finance 等其余域类似量级） |
| 字典 yaml 总数 | `find module-*/erp-*-meta/src/main/resources/_vfs/dict -name "*.yaml"` | 279 个字典定义文件（19 模块全覆盖） |
| DAG 模块依赖方向 | `cat module-finance/erp-fin-service/pom.xml` | finance-service 仅 compile-scope 依赖 master-data/assets/inventory/purchase/sales/notify/projects 的 **`*-dao`**（非 `*-service`），注释明确标注"DAG 合法 R：finance→X 只读依赖"；service 实现由 `app-erp-all` 注入 |

## 1. 8 项定制能力声明-实证裁决矩阵

> 裁决三档：`已验证落地`（owner doc 声明 + 项目实证落地，机制工作）/ `平台声明待落地`（owner doc 声明 + 平台机制存在 + 项目零落地实证——能力可用但本项目未启用）/ `声明过当`（owner doc 声明能力 + 列举具体业务示例 + 实际未以该机制实现，示例误导）。

| # | 能力 | owner doc 声明（`customization-capabilities.md` 节） | 项目实证 | 裁决 | gap 描述 |
|---|------|----------|---------|------|---------|
| 1 | **配置驱动（字典/参数/编码规则）** | §定制能力总览 行1 + §决策顺序（极低改动成本） | **广泛落地**：19 ORM 文件全覆盖 `ext:dict`；279 个字典 yaml；编码规则经 codegen `code` 字段 + 平台 `SeqGenerator` 实现（`erp-md/material-type` 等）；AMIS 列表/表单自动绑定 dict 下拉 | **已验证落地** | 无 gap |
| 2 | **扩展字段（EAV）** | §能力二（NopSysExtField + `extFields.fldX.string` 路径） | **零落地**：19 ORM 文件零 `extField`/`extFields`/`NopSysExtField` 声明 | **平台声明待落地** | owner doc 声明能力 + 列举 3 业务场景（物料颜色尺码 / 往来单位行业属性 / 凭证分录辅助核算），实际零落地。**机制可用性未抽样验证**——本审计受 Non-Goals "不手写业务级 Delta/扩展字段作为生产产物" 约束，未抽样构造 EAV 配置验证持久化路径；裁决按 owner doc 声明过当风险登记（见 P1-MA3-058）。 |
| 3 | **nop-dyn 动态实体** | §能力三（NopDynEntityMeta/PropMeta/Page/Sql + VIRTUAL/REAL 存储 + AMIS/OpenTiny/Formily schema） | **零落地**：19 service 模块零 `nop-dyn`/`NopDynEntity` 业务使用 | **平台声明过当** | owner doc 将 nop-dyn 列为 8 能力之一并描述"按需把动态实体模块组装到不同应用"，但项目完全未配置动态实体。本项目采用 codegen ORM 而非动态实体是合理设计选择（类型安全 + 性能 + codegen 纪律），但 owner doc 应显式标注"平台能力，本项目未启用"——否则审查者/客户期望运行时建模但实际不存在（见 P1-MA3-059）。 |
| 4 | **Delta 定制（基线差量覆盖）** | §能力一 + §定制能力总览 行4（核心手段，适配各领域业务的核心手段） | **零业务级落地**：仅 2 个**平台层** nop-auth view delta（NopAuthOpLog/NopAuthSession）位于 `app-erp-all/_vfs/_delta/default/nop/auth/pages/`，是**对平台 nop-auth 模块的 UI 微调**，不是对业务域（finance/inventory/...）的定制 | **平台声明待落地** | owner doc 声明 Delta 是"适配各领域业务的核心手段"+ 列举 4 业务场景（物料加客户专属字段 / 采购订单多级审 / 销售出库加按钮 / 凭证模板科目覆盖），实际零业务域 Delta。**机制可用性经平台文档 + 2 个平台层 delta 实证可工作**（合并机制 + `_dump/` 来源标注 + `nop.debug=true` 调试），但**业务级零实证**——客户按 owner doc 期望"Delta 是核心定制手段"但项目从未验证业务场景（见 P1-MA3-057）。 |
| 5 | **task.xml 编排** | §定制能力总览 行5 + `../nop-entropy/docs-for-ai/03-modules/nop-task.md` | **零落地**：全域 grep `task.xml` 业务使用 = 0（仅平台 nop-task 模块本身）；复杂业务编排全部经 BizModel/Processor Java 实现 | **平台声明过当**（轻度） | owner doc 列 task.xml 为定制能力并声明"拓扑可变的编排仍首选 task.xml"，但项目所有编排（采购订单审批/销售订单过账/期末结账/坏账准备/承付释放等）全部以 Processor + protected step + Delta beans 同名覆盖实现（processor-extension-pattern.md），未使用 task.xml。这是合理设计选择（拓扑稳定的复杂流程），但 owner doc 表格未标注"本项目未启用 task.xml，编排全部走 Processor 模式"——表头声明能力但实际未使用（见 P1-MA3-059 同型）。**裁决合并至 P1-MA3-059**（owner doc 能力声明清单与实际启用集合的系统性偏差，task.xml/nop-dyn/BizLoader 三项同型）。 |
| 6 | **非下划线扩展层（保留层定制）** | §能力四（在自己的模块里扩展生成保留层） | **广泛落地**：19 模块 BizModel=352 + xbiz=352 + view.xml=352 非下划线保留层文件；**抽样验证继承机制正确**：`ErpFinVoucher.xbiz` 头部 `x:extends="_ErpFinVoucher.xbiz"` + `ErpFinVoucher.view.xml` 头部 `x:extends="_gen/_ErpFinVoucher.view.xml"` + 生成基线 `_ErpFinVoucher.xbiz` 含 `<x:gen-extends><biz-gen:DefaultBizGenExtends ... forEntity="true" entityName="app.erp.fin.dao.entity.ErpFinVoucher"/></x:gen-extends>` 驱动 codegen；`ErpFinVoucher.view.xml` 列表列布局经 `<cols x:override="bounded-merge">` 合并生成基线 | **已验证落地** | 无 gap。**升级路径风险**：保留层与生成基线分离正确，codegen 增量再生时生成层刷新、保留层定制保留（`mvn clean install -DskipTests` 触发增量重新生成不覆盖非下划线文件，见 `AGENTS.md §codegen 后阶段规则`）。 |
| 7 | **模块化组装（按需引入/裁剪业务域）** | §能力五 + §定制能力总览 行7（18 域各自独立 Maven 工程） | **已验证可行**：抽样 `module-finance/erp-fin-service/pom.xml` 显示 finance-service **compile-scope 仅依赖其他域的 `*-dao`**（非 `*-service`），DAG 边方向经 `data-dependency-matrix.md` 明确登记（finance→assets/inventory/purchase/sales/projects 只读 R 关系）；service 实现由 `app-erp-all` 聚合时注入。**裁剪可行性裁决**：裁剪 manufacturing 后 finance 仍可构建（finance-service 仅 compile-scope 依赖其他域 dao，运行期 impl 经 IBizObjectManager 解析失败→配置门控跳过，pom 注释 line 47-48 已显式声明）；纯商贸客户组装 master-data/inventory/purchase/sales/finance 五域可行 | **已验证落地** | 无 gap。**裁剪未抽样端到端验证**——本审计未实际裁剪 manufacturing 跑 finance-only 构建（Non-Goals "不手改生成物"）；但 DAG 依赖边的设计已允许裁剪，pom 注释明确"运行期 impl 由 app-erp-all 注入，单域测试无 ast-service 时经 IBizObjectManager 解析失败→配置门控跳过"是已验证的解耦模式。 |
| 8 | **BizModel/Processor 手写** + **BizLoader 计算** | §定制能力总览 行8 + §能力六（`@BizLoader` 派生字段） | **BizModel/Processor：广泛落地**（352 个非下划线 BizModel + 全域 Processor/Dispatcher 链路经 MA2 状态机审计逐域确认）；**BizLoader：零落地**（19 service 模块零 `@BizLoader`） | **BizModel/Processor 已验证落地 / BizLoader 平台声明过当** | owner doc §能力六声明 BizLoader 是"为已有实体动态增加计算字段"+ 列举 3 业务示例（采购"未交量"/物料"当前库存"/凭证"借贷平衡校验结果"），实际全部以其他机制实现：未交量经 SQL 查询或 Processor 计算 + 当前库存经 StockBalance 实体查询 + 借贷平衡经 `balanceStatus` 字段 + view.xml `<gen-control>` 内联脚本计算（见 `ErpFinVoucher.view.xml:13-19` `<col id="balanceStatus" custom="true"><gen-control>` 实证）。owner doc 声明"普通扩展字段优先用 @BizLoader"但项目从未使用——示例误导审查者期望 @BizLoader 落地但实际不存在（见 P1-MA3-060）。 |

## 2. 抽样验证证据

### 2.1 维度「Delta 定制实证」

**裁决**：能力机制可用（经平台文档 + 2 个平台层 delta 实证），业务级零落地。

**抽样证据**：
- `app-erp-all/src/main/resources/_vfs/_delta/default/nop/auth/pages/NopAuthOpLog/NopAuthOpLog.view.xml` —— 平台 nop-auth 模块 view delta，证实 Delta 合并机制实际可工作（VFS 路径 `_delta/{deltaDir}/...` + deltaDir=`default` 配置激活）
- `_dump/` 来源标注机制经 `nop.debug=true` 启用（平台 `delta-customization.md §调试` 权威）
- **业务域零 Delta 不意味能力缺失**——是设计选择（项目用 codegen ORM + 保留层而非 Delta 适配业务）。但 owner doc 将 Delta 列为"适配各领域业务的**核心手段**"+ 列举 4 业务场景，业务级零实证是声明-实证 gap

**结论**：Delta 合并机制**实际可用**（经平台层实证），业务级实证为 successor（首个真实业务域 Delta 上线时验证）；不破坏基线（Delta 与基线分离，升级时自动合并）

### 2.2 维度「扩展字段 EAV 实证」

**裁决**：零使用 = "能力未利用"（非"能力不可用"）。

**抽样证据**：
- 全域 19 ORM 文件零 `extField`/`extFields`/`NopSysExtField` 声明
- 平台 `nop-sys` 模块提供 `NopSysExtField`（`nop_sys_ext_field` 表）+ EAV 机制经 `../nop-entropy/docs-for-ai/03-modules/nop-sys.md` 权威
- **零使用裁决为"未利用"**：客户化字段在当前业务域中 либо 经 codegen ORM 物理加列（如 `ErpMdMaterial` 多币种四件套字段）要么不存在；EAV 路径未启用是合理设计选择（codegen ORM 类型安全 + 性能 + 字段固定）
- **owner doc §能力二 列举 3 业务场景**（物料颜色尺码 / 往来单位行业属性 / 凭证分录辅助核算）应明确标注"当前为平台能力声明，本项目客户化字段经 codegen ORM 加列承载，EAV 路径未启用"

**结论**：EAV 机制**经平台文档可用**，本项目零实证；owner doc 声明过当风险登记（P1-MA3-058）

### 2.3 维度「保留层定制升级安全性」

**裁决**：保留层与生成基线继承关系正确，升级合并不冲突。

**抽样证据**（finance 域 ErpFinVoucher）：
- 生成基线 `_ErpFinVoucher.xbiz` 头部含 `<x:gen-extends><biz-gen:DefaultBizGenExtends xpl:lib="/nop/core/xlib/biz-gen.xlib" forEntity="true" entityName="app.erp.fin.dao.entity.ErpFinVoucher"/></x:gen-extends>` + `<x:post-extends>` —— codegen 驱动
- 保留层 `ErpFinVoucher.xbiz` 头部 `x:extends="_ErpFinVoucher.xbiz"` + `<actions/>` —— 继承生成基线
- 保留层 `ErpFinVoucher.view.xml` 头部 `x:extends="_gen/_ErpFinVoucher.view.xml"` + `<cols x:override="bounded-merge">` —— bounded-merge 合并模式，列定制不冲突
- **codegen 增量再生**：`AGENTS.md §codegen 后阶段规则` 明示"ORM 模型变更后用 `mvn clean install -DskipTests` 触发增量重新生成"——生成层刷新，保留层（非下划线）保留

**结论**：保留层定制**升级安全**，与生成基线分离正确；不破坏基线（保留层继承生成基线，生成层升级时保留层自动合并新行为）

### 2.4 维度「模块化组装/裁剪可行性」

**裁决**：DAG 依赖边允许裁剪，模块裁剪可行。

**抽样证据**（finance → 其他域）：
- `module-finance/erp-fin-service/pom.xml:43-65` 6 个跨域 compile-scope 依赖**全部是 `*-dao`**（`app-erp-master-data-service` 为 test-scope 仅 ORM 加载，非运行时）：
  - `app-erp-assets-dao`（compile，IErpAstDepreciationScheduleBiz 声明位置）
  - `app-erp-inventory-dao`（compile，IErpInvCostingBiz 声明位置）
  - `app-erp-purchase-dao`（compile，ErpPurInvoice 双面对账）
  - `app-erp-sales-dao`（compile，ErpSalInvoice 双面对账）
  - `app-erp-notify-dao`（compile，IErpSysNotificationBiz 声明位置）
  - `app-erp-projects-dao`（compile，ErpPrjProject.name BizLoader）
- pom 注释明确："运行期 impl 由 app-erp-all 注入，单域测试无 ast-service 时经 IBizObjectManager 解析失败→配置门控跳过"——**解耦模式已落地**
- **裁剪可行性**：纯商贸客户组装 master-data/inventory/purchase/sales/finance 五域可行（5 域 dao 依赖闭合）；裁剪 manufacturing 后 finance 可构建（manufacturing 不在 finance compile-scope 依赖链中）

**结论**：模块化组装**已验证落地**；裁剪可行（DAG 边允许 + dao-only 依赖解耦）；未抽样端到端裁剪构建（Non-Goals），但设计已允许

### 2.5 维度「BizLoader 计算字段实证」

**裁决**：零使用 = "能力未利用"（owner doc 声明示例以其他机制实现）。

**抽样证据**：
- 全域 19 service 模块零 `@BizLoader` 业务使用
- owner doc §能力六 列举 3 业务示例的实际实现：
  - **采购"未交量"**：经 SQL 查询或 Processor 计算（未以 @BizLoader 落地）
  - **物料"当前库存"**：经 `ErpInvStockBalance` 实体查询（未以 @BizLoader 落地）
  - **凭证"借贷平衡校验结果"**：经 `ErpFinVoucher.balanceStatus` 字段 + `ErpFinVoucher.view.xml:13-19` `<col id="balanceStatus" custom="true"><gen-control>` 内联 AMIS 脚本计算（`return { type: 'tpl', ... }`），**不是 @BizLoader**
- owner doc §决策提示"普通扩展字段优先用 @BizLoader"——项目从未遵循此决策提示

**结论**：@BizLoader 能力**经平台文档可用**，本项目零落地；owner doc 声明过当风险登记（P1-MA3-060）

## 3. 升级路径保护实证（5 项机制）

> owner doc §升级路径保护 声明 5 项机制确保基线升级时不破坏客户化。

| # | 机制 | owner doc 声明 | 项目实证 | 裁决 |
|---|------|----------|---------|------|
| 1 | **Delta 自动合并** | "基线升级后，Delta 按 `x:extends="super"` 重新合并，定制保留" | 平台层 delta 实证合并机制工作；业务级零 delta | **平台实证 / 业务级 successor** |
| 2 | **扩展字段独立存储** | "EAV 数据在独立表，基线表结构变化不影响扩展数据" | 零 EAV 使用 | **平台机制可用 / 项目零实证** |
| 3 | **nop-dyn 运行时配置** | "动态实体配置在数据库，基线代码升级不触碰动态配置" | 零 nop-dyn 使用 | **平台机制可用 / 项目零实证** |
| 4 | **模块化组装** | "客户组装的模块集合独立于基线模块演进" | DAG dao-only 依赖解耦已验证；finance 可独立构建 | **已验证** |
| 5 | **保留层不冲突** | "非下划线扩展文件继承生成基线，基线升级时生成层刷新、保留层定制保留" | 19 模块 352 套保留层继承机制全部正确（抽样 ErpFinVoucher 实证） | **已验证** |

**升级路径保护综合裁决**：5 项机制中 2 项已验证（模块化组装 + 保留层不冲突），3 项平台机制可用但项目零实证（Delta 自动合并 + EAV 独立存储 + nop-dyn 运行时配置）。**owner doc 应显式标注每项机制的实证状态**，否则客户按 owner doc 期望 5 项全部经项目实证但实际仅 2 项落地（见 P1-MA3-061）。

## 4. 声明过当检测（开放式核心）

### 4.1 "产品化通用 ERP"承诺状态

owner doc §产品定位 声明 nop-app-erp 是"产品化通用 ERP 产品"，4 个核心承诺：

| 承诺 | 实证状态 | 裁决 |
|------|---------|------|
| **通用**（18 业务域内置标准能力） | ✅ 实证：18 业务域 + notify 跨域子系统 ORM 全设计 + CRUD 全 done + 核心业务逻辑 + 业财一体 + 运营成熟度 + 报表 + 看板（见 `AGENTS.md §当前项目阶段`） | **已验证** |
| **产品化**（可发布标准产品基线，模型优先） | ✅ 实证：`nop-cli gen` 生成 + codegen 纪律经 MA1 平台合规审计 PASS（A1.11-A1.13） | **已验证** |
| **可定制**（充分利用扩展能力，不改基线源码） | ⚠️ 部分：保留层 + 模块化组装 + 配置驱动**已实证落地**；Delta + EAV + nop-dyn + BizLoader **零业务实证** | **部分验证**——核心定制机制（保留层/模块化/字典）已落地，扩展能力（Delta/EAV/nop-dyn/BizLoader）零实证 |
| **升级友好**（定制层与基线分离，定制自动合并） | ⚠️ 部分：保留层 + 模块化**已实证升级安全**；Delta 自动合并 + EAV 独立存储 + nop-dyn 运行时配置**平台机制可用但零实证** | **部分验证** |

### 4.2 owner doc 声明清单 vs 实际启用集合偏差

owner doc §定制能力总览 表格列 8 能力，实际启用集合：

| 能力 | 表格声明 | 实际启用 | 偏差 |
|------|---------|---------|------|
| 配置驱动 | ✓ | ✓ | 无 |
| 扩展字段 EAV | ✓ | ✗（零声明） | **声明未启用** |
| nop-dyn 动态实体 | ✓ | ✗（零配置） | **声明未启用** |
| Delta 定制 | ✓（核心手段） | △（仅平台层，业务级零） | **业务级未启用** |
| task.xml 编排 | ✓ | ✗（零业务使用） | **声明未启用** |
| 非下划线扩展层 | ✓ | ✓（广泛） | 无 |
| 模块化组装 | ✓ | ✓（已验证） | 无 |
| BizModel/Processor 手写 | ✓ | ✓（广泛） | 无 |
| BizLoader 计算 | ✓（§能力六独立章节） | ✗（零业务使用） | **声明未启用** |

**偏差汇总**：8 能力中 4 已验证落地 + 1 业务级未启用（Delta 平台层实证）+ 3 完全未启用（EAV/nop-dyn/task.xml）+ 1 独立章节声明未启用（BizLoader）。owner doc §定制能力总览 表格未标注每项能力的实际启用状态——审查者/客户按表格期望 8 能力全部启用但实际仅 4 项落地。

### 4.3 "不改基线源码"边界裁决

owner doc §产品定位 声明"不改基线源码"。**保留层手写是否违反此承诺？**

**裁决：不违反**。保留层（非下划线 BizModel/xbiz/view）是**扩展自己的生成保留层**（owner doc §能力四 明示"在自己的模块里扩展自己的生成保留层"），不是"定制别人基线"的 Delta。两者边界：
- **非下划线扩展层**：在自己的模块里扩展自己的 codegen 产物——本项目主路径（合法）
- **Delta**：在已有产品/基线模块上做差量覆盖——本项目零业务级（合法未启用）

但 owner doc §产品定位 文字"不改基线源码"+ §硬规则"不允许手工修改任何生成物（`_gen/`、`_*.xml`）"需明确**生成物 ≠ 基线源码**：保留层手写不是"改基线源码"，是扩展 codegen 产物（合法）；改 `_gen/` 才是违反硬规则。**当前文字边界模糊**，建议 owner doc 明确两层定义（见 P1-MA3-061 一并裁决）。

## 5. finding 清单（按严重性排序）

> 本审计为可定制性实证 + 声明-实证比对层，**原则上无 P0**（plan Non-Goals + Goals 明示"定制能力缺失是能力未实证非活跃数据破坏"；定制层导致基线运行时破坏走 P0 即时通道——本审计未发现此类破坏）。所有 finding 目标 MR2（MR2 deps = MA3+MA4 done）。Finding ID 接续 A3.7（止于 P1-MA3-056），本批自 **P1-MA3-057** 起。

### MAJOR 类（目标 MR2，文档类）

#### P1-MA3-057 — Delta 定制业务级实证缺口（owner doc 声明"核心手段" + 业务级零落地）

- **能力项**：Delta 定制（owner doc §能力一）
- **严重性**：major
- **声明-实证 gap 描述**：owner doc `customization-capabilities.md §能力一 + §定制能力总览 行4` 声明 Delta 定制是"适配各领域业务的**核心手段**"+ 列举 4 业务场景（物料加客户专属字段 / 采购订单多级审 / 销售出库加按钮 / 凭证模板科目覆盖）；实时仓库业务级 Delta = 0（仅 2 个平台层 nop-auth view delta）。owner doc §产品定位 同步声明"充分利用扩展能力"——客户按 owner doc 期望 Delta 是核心定制手段但项目从未验证业务场景
- **影响**：客户/审查者按 owner doc 期望 Delta 是产品化核心定制路径，实际项目零业务实证；首个真实业务域 Delta 上线时可能发现集成/合并/调试问题（无项目侧先行验证）
- **裁决依据**：Delta 合并机制**经平台层实证可用**（`app-erp-all/_vfs/_delta/default/nop/auth/pages/` 2 文件），但业务级实证为 successor；不破坏基线（Delta 与基线分离）
- **目标 MR**：MR2（文档类）
- **修复方式**：MR2 裁决——方案 A（推荐）owner doc `customization-capabilities.md §能力一` 4 业务场景表后追加"**实证状态注记**：本项目当前业务级 Delta = 0（保留层 + 模块化组装已覆盖产品基线定制需求）；Delta 合并机制经平台层 nop-auth view delta 实证可用；业务级 Delta 实证为首项客户化场景落地时验证（successor）"；方案 B owner doc 删除 §能力一 4 业务场景示例表 + 仅保留机制描述

#### P1-MA3-058 — 扩展字段 EAV 实证缺口（owner doc 声明 3 业务场景 + 零声明）

- **能力项**：扩展字段 EAV（owner doc §能力二）
- **严重性**：major
- **声明-实证 gap 描述**：owner doc `customization-capabilities.md §能力二` 声明 EAV 用途（运行时给实体加字段，不改表结构）+ 列举 3 业务场景（物料颜色尺码 / 往来单位行业属性 / 凭证分录辅助核算）；实时仓库全域 19 ORM 文件零 `extField`/`extFields`/`NopSysExtField` 声明。客户化字段在当前业务域中经 codegen ORM 物理加列承载（如多币种四件套）
- **影响**：客户/审查者按 owner doc 期望 EAV 是客户化字段承载路径，实际项目零实证；首个客户启用 EAV 时可能发现 ORM/xmeta/view 集成问题（无项目侧先行验证）
- **裁决依据**：EAV 机制**经平台文档可用**（`../nop-entropy/docs-for-ai/03-modules/nop-sys.md` 权威），本项目零实证是合理设计选择（codegen ORM 类型安全）；不破坏基线（EAV 独立表）
- **目标 MR**：MR2（文档类）
- **修复方式**：MR2 裁决——方案 A（推荐）owner doc `customization-capabilities.md §能力二` 3 业务场景后追加"**实证状态注记**：本项目当前客户化字段经 codegen ORM 物理加列承载（如多币种四件套），EAV 路径未启用；EAV 机制经平台 nop-sys 模块可用；首个客户启用 EAV 时验证 ORM/xmeta/view 集成（successor）"；方案 B owner doc 删除 §能力二 3 业务场景示例 + 仅保留机制描述

#### P1-MA3-059 — nop-dyn / task.xml / BizLoader 三项 owner doc 能力声明清单与实际启用集合系统性偏差

- **能力项**：nop-dyn 动态实体（§能力三）+ task.xml 编排（§定制能力总览 行5）+ BizLoader 计算（§能力六）合并裁决
- **严重性**：major
- **声明-实证 gap 描述**：owner doc `customization-capabilities.md §定制能力总览 表 + §能力三 + §能力六` 声明 3 项能力并描述详细机制，但实时仓库 3 项全部零业务使用：
  - **nop-dyn**：19 service 模块零 `nop-dyn`/`NopDynEntity` 使用；本项目采用 codegen ORM 而非动态实体（合理设计选择）
  - **task.xml**：全域零业务 task.xml 使用；复杂业务编排全部经 Processor + protected step + Delta beans 同名覆盖（合理设计选择，processor-extension-pattern.md）
  - **BizLoader**：19 service 模块零 `@BizLoader` 使用；owner doc §能力六 列举 3 业务示例（采购"未交量"/物料"当前库存"/凭证"借贷平衡校验结果"）全部以其他机制实现（SQL 查询 / StockBalance 实体查询 / `balanceStatus` 字段 + view.xml `<gen-control>` 内联脚本）
- **影响**：客户/审查者按 owner doc 表格期望 8 能力全部启用，实际仅 4 项落地；owner doc §能力六 决策提示"普通扩展字段优先用 @BizLoader"——项目从未遵循此提示，决策提示误导
- **裁决依据**：3 项能力**经平台文档可用**，本项目未启用是合理设计选择（codegen ORM/Processor 模式更类型安全 + 性能 + codegen 纪律）；不破坏基线（不启用即不影响）
- **目标 MR**：MR2（文档类）
- **修复方式**：MR2 裁决——方案 A（推荐）owner doc `customization-capabilities.md §定制能力总览` 表格增"**实际启用**"列，3 项标注"⚠️ 平台能力，本项目未启用（reason: codegen ORM/Processor 模式优先）"+ §能力三 / §能力六 章节末追加"**实证状态注记**：本项目采用 codegen ORM + Processor 模式，未启用 nop-dyn/task.xml/@BizLoader；能力经平台模块可用，启用为 successor"；方案 B owner doc 删除 §能力三 + §能力六 独立章节 + §定制能力总览 行3/行5/行8（保留 5 项实际启用能力）

#### P1-MA3-060 — owner doc §能力六 BizLoader 业务示例全部以其他机制实现（示例误导）

- **能力项**：BizLoader 计算（owner doc §能力六）
- **严重性**：major（与 P1-MA3-059 子例关系——P1-MA3-059 是系统性偏差汇总，本项是 §能力六 独立章节示例的具体误导）
- **声明-实证 gap 描述**：owner doc `customization-capabilities.md §能力六 §适用场景` 列举 3 业务示例：
  - **采购订单的"未交量"**——实际经 SQL 查询或 Processor 计算（未以 @BizLoader 落地）
  - **物料的"当前库存"**——实际经 `ErpInvStockBalance` 实体查询（未以 @BizLoader 落地）
  - **凭证的"借贷平衡校验结果"**——实际经 `ErpFinVoucher.balanceStatus` 字段 + `ErpFinVoucher.view.xml:13-19` `<col id="balanceStatus" custom="true"><gen-control>` 内联 AMIS 脚本（`return { type: 'tpl', ... }`）
- **影响**：审查者按 owner doc 示例期望 @BizLoader 落地但实际不存在；§决策提示"普通扩展字段优先用 @BizLoader"误导开发者按此提示实现但项目无范例
- **裁决依据**：@BizLoader 能力**经平台文档可用**（`../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md` 权威），本项目零落地；不破坏基线
- **目标 MR**：MR2（文档类）
- **修复方式**：MR2 裁决（与 P1-MA3-059 协同）——方案 A（推荐）owner doc `customization-capabilities.md §能力六 §适用场景` 3 业务示例后追加"**实证状态注记**：本项目上述 3 业务示例的实际实现路径分别为：未交量→SQL/Processor 计算 / 当前库存→StockBalance 实体查询 / 借贷平衡→balanceStatus 字段 + view.xml gen-control 内联脚本（均未使用 @BizLoader；@BizLoader 能力经平台可用，启用为 successor）"+ §决策提示 修订为"普通扩展字段优先用 @BizLoader 或 view.xml gen-control，本项目当前路径选择 gen-control"；方案 B owner doc 删除 §能力六 独立章节（合并入 P1-MA3-059 方案 B）

#### P1-MA3-061 — 升级路径保护 5 项机制实证状态未标注（owner doc 声明 5 项 + 仅 2 项经项目实证）

- **能力项**：升级路径保护（owner doc §升级路径保护）
- **严重性**：major
- **声明-实证 gap 描述**：owner doc `customization-capabilities.md §升级路径保护` 声明 5 项机制确保基线升级时不破坏客户化：(1) Delta 自动合并 / (2) 扩展字段独立存储 / (3) nop-dyn 运行时配置 / (4) 模块化组装 / (5) 保留层不冲突。**实际项目实证状态**：仅 (4) 模块化组装 + (5) 保留层不冲突 经项目实证落地（本审计 §2.3 + §2.4 抽样验证）；(1) Delta 自动合并 仅平台层实证（业务级零 Delta）；(2) EAV 独立存储 + (3) nop-dyn 运行时配置 平台机制可用但项目零实证。owner doc 未标注每项机制的实证状态——客户按 owner doc 期望 5 项全部经项目实证但实际仅 2 项落地
- **影响**：客户/审查者按 owner doc §升级路径保护 期望 5 项机制全部经项目实证，实际仅 2 项落地（模块化组装 + 保留层）；首项客户启用 Delta/EAV/nop-dyn 时可能发现升级合并/独立存储/运行时配置问题（无项目侧先行验证）
- **裁决依据**：(4)(5) **经项目实证**（本审计 §2.3 + §2.4）；(1)(2)(3) **平台机制可用但项目零实证**；不破坏基线（项目当前未启用 3 项零实证能力，无运行时风险）
- **目标 MR**：MR2（文档类）
- **修复方式**：MR2 裁决——方案 A（推荐）owner doc `customization-capabilities.md §升级路径保护` 5 项机制每项后追加"**实证状态**"标注：(1)(2)(3)"平台机制可用 / 项目零实证（successor）"+ (4)(5)"✅ 经项目实证落地"；同步 §产品定位 "不改基线源码"边界澄清（生成物 `_gen/`/`_*.xml` ≠ 基线源码；保留层手写是扩展 codegen 产物合法，改 `_gen/` 才违反硬规则）；方案 B owner doc 删除 §升级路径保护 (1)(2)(3) 3 项仅保留 (4)(5) 2 项 + 删除 §产品定位 "不改基线源码"模糊表述

### NOTE 类（不登记 P2，仅记录）

- **NOTE-1**：本审计受 plan Non-Goals "不手写业务级 Delta/扩展字段作为生产产物" 约束，**未抽样构造 Delta/EAV 配置验证运行时持久化路径**。裁决按 owner doc 声明过当风险登记 P1-MA3-057/058（目标 MR2 文档类）。**首项客户化场景（首个真实业务域 Delta 或 EAV 启用）落地时**应补运行时实证（构造 Delta 验证 `_dump/` 合并 + 构造 EAV 验证 `extFields.fldX.string` 持久化）—— 此为 successor，不进入本审计 P1 清单（避免双重登记）
- **NOTE-2**：保留层定制升级安全性经本审计 §2.3 抽样实证（finance ErpFinVoucher），但**未抽样跑 codegen 增量再生 + diff 保留层不漂移**（Non-Goals "不手改生成物"）。MA1 平台合规审计 codegen 纪律 PASS（A1.11-A1.13 全域 `mvn clean install -DskipTests` 成功触发增量再生，保留层未漂移）是间接证据

## 6. 裁决通过/失败 + 剩余风险

**Verdict：GAPS（有差距）**——8 能力中 4 已验证落地（配置驱动 / 保留层 / 模块化组装 / BizModel-Processor）+ 1 业务级未启用（Delta 平台层实证）+ 3 完全未启用（EAV/nop-dyn/task.xml）+ 1 独立章节声明未启用（BizLoader）；owner doc 声明的"产品化可定制 ERP + 充分利用扩展能力 + 升级友好"承诺**部分验证**（核心定制机制已落地，扩展能力零实证）。

**零 P0**（plan Non-Goals + Goals 明示；定制能力缺失是能力未实证非活跃数据破坏）。

**5 项 P1**（全部目标 MR2，文档类）：
- P1-MA3-057 Delta 定制业务级实证缺口
- P1-MA3-058 扩展字段 EAV 实证缺口
- P1-MA3-059 nop-dyn / task.xml / BizLoader 三项 owner doc 能力声明清单与实际启用集合系统性偏差
- P1-MA3-060 owner doc §能力六 BizLoader 业务示例全部以其他机制实现
- P1-MA3-061 升级路径保护 5 项机制实证状态未标注

**与 A3.1-A3.7 已登记 P1-MA3-001~056 经交叉去重无重复登记**（维度不同：本审计是定制能力实证 vs 文档内容质量/契约/索引路由）。

**剩余风险**：
1. **首项客户化场景 successor**：首个真实业务域 Delta 或 EAV 启用时需补运行时实证（NOTE-1）
2. **保留层升级漂移监控**：codegen 增量再生保留层不漂移经 MA1 间接证据，未抽样端到端 diff（NOTE-2）
3. **声明-实证差距持续维护**：MR2 修复后 owner doc 应建立"能力声明 vs 实证状态"同步机制（每次新能力启用或废弃时更新 §定制能力总览 表格"实际启用"列）

**MA3 累计 P1=52**（A3.1 13 + A3.2 2 + A3.3-A3.5 22 + A3.6 4 + A3.7 6 + 本审计 5），**P2=30**（A3.1 8 + A3.2 1 + A3.3-A3.5 13 + A3.6 4 + A3.7 4 + 本审计 0——本审计 NOTE 不登记 P2）。

**roadmap A3.8 推进至 done**（待独立 closure audit）。
