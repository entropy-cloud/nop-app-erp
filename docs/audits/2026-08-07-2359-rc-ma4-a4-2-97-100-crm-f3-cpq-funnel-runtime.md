# rc-ma4-a4-2-97-100 crm-F3 CPQ/漏斗推进 运行时确认验证报告（A1.30 SP-1..SP-4）

> 计划：`docs/plans/2026-08-07-2359-1-rc-ma4-a4-2-97-100-crm-f3-cpq-funnel-runtime.md`
> 来源审计：`docs/audits/2026-08-05-1830-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md` §7 静态存疑点 SP-1..SP-4 + §6 新建 P2-RC-036/037/038/039 + reuse P1-MA2-075 resolved R1.24
> 域：crm | 功能切片：crm-F3 CPQ/漏斗推进 | UC 清单：UC-CRM-06/13
> Audit Type: 只读审计（无生产代码/ORM/api.xml/view.xml/真相源变更）
> 技能：`docs/skills/multi-dimensional-audit-prompt.md`

---

## 0. 审计范围与方法

本报告对 A1.30 §7 登记的 4 个静态存疑点（SP-1..SP-4）执行运行时确认，产出各自裁决 + file:line 证据链。审计方法 = 代码可达性追踪（grep census / config-gate 普查 / ORM 精度读 / 枚举常量字面值 grep / XLang 评估路径追踪）+ 复用 A1.30 L4 集成测试（经 IGraphQLEngine 的 GraphQL 路径即运行时证据）。

**多维自检**：需求正确性 / owner-doc 对齐 / 架构边界影响 / 验证充分性 / 回归风险 / 路由技能选择 / 待办策略漂移 七维度逐项给出一句裁决（见 §9）。本报告不触及 view.xml gen-control 契约维度（无 delta view 变更）。CRM 域不直接产生会计凭证，不触及业财保护区域探针。

---

## 1. A4.2.97 — UC-CRM-06 ④ 等值边界运行时触发面（P2-RC-036）

### 1.1 静态判定复核

| 站点 | file:line | 实测 |
|------|-----------|------|
| GraphQL 入口 | `ErpCrmLeadBizModel.java:112-115` `moveStage(@BizMutation)` → 委托 processor | ✅ |
| 编排链 | `ErpCrmLeadMoveStageProcessor.java:18-26`：requireLead→validateMovable→requireStage→**validateStageDirection:23**→doMoveStage:24 | ✅ |
| **方向守卫** | `ErpCrmLeadProcessor.validateStageDirection:91-110` | 见下 |
| 守卫逻辑 | `:93-95` fromStageId==null → return（跳过）；`:99` `if (fromSeq != null && toSeq != null && toSeq < fromSeq)`（**严格小于**）；`:100-104` allowStageBackward()=true → LOG.warn + return；`:105-108` STRICT → throw `ERR_STAGE_BACKWARD_MOVE` | ✅ |
| config-gate | `ErpCrmConfigs.allowStageBackward():17-20` 读 `CONFIG_ALLOW_STAGE_BACKWARD`，默认 `DEFAULT_ALLOW_STAGE_BACKWARD = Boolean.FALSE`（`ErpCrmConstants:36`） | ✅ |

### 1.2 运行时确认

1. **等值边界（toSeq==fromSeq）放行**：`:99` 条件 `toSeq < fromSeq` 对等值（==）为 false → **不进 if 分支** → validateStageDirection 直接返回 → doMoveStage 执行。等值 stage 移动无论 allow-backward 真假均放行。**config-gate `allow-stage-backward` 仅控制 `<`（回退）分支**，与等值无关。

2. **GraphQL `ErpCrmLead__moveStage` 运行时成功**：`TestErpCrmStageDirectionGuard#testEqualSequenceForwardSucceeds:96-105` 经 `IGraphQLEngine.executeRpc(mutation, "ErpCrmLead__moveStage", ...)`（`:110-116` rpc helper）实测两个 sequence=20 的 stage（STAGE_LOW 5101 / STAGE_HIGH 5102）间移动——`moveStage(5004L, STAGE_HIGH).getStatus()==0`（成功）+ `stageId 移到 HIGH`。**此为经 GraphQL 引擎的运行时证据**（非纯静态判定），确认等值 stage 移动经 `ErpCrmLead__moveStage` 放行成功。

3. **config-gate 全生产 application.yaml override 普查**：grep 全部 20 个 application.yaml（app-erp-all + 19 域 erp-<short>-app），**零生产 override** `erp-crm.allow-stage-backward`。唯一引用在测试资源 `src/test/resources/allow-stage-backward-test.yaml`（设 true）。→ **生产默认 = STRICT（false）**。config-gate 为部署启用决策，默认严格对齐 L1 `:122` 单调递增意图（仅等值边界偏移）。

4. **FunnelAggregationEngine sequence 排序实际影响**：等值 sequence 的两个 stage 间移动后，`FunnelAggregationEngine` 按 sequence 排序将二者视为同位（稳定近似排序）。owner doc `state-machine.md:90` 已注记「转化率/dropOffRate 按 sequence 排序为近似值（历史 convLog 仍全量留痕，审计不丢）」。等值边界不破坏 convLog 审计完整性（`writeConvLog:159-168` 全量写 fromStageId/toStageId/changedAt/changedBy）。

### 1.3 裁决

**维持 P2-RC-036 P2（§2 P2① 次要验收标准边界场景弱）。**

- **主路径正确**：前移（`>`）允许 + 回退（`<`）STRICT 默认拒绝 + config-gated allow-backward 放行——全实现。
- **等值边界（`==`）放行**偏离 L1 `use-cases.md:122` 字面 `<=`（等值应拒绝），属次要验收标准边界场景弱。**不破坏活跃数据**（stageId 单调性主路径守卫正确，等值移动不破坏 convLog 审计完整性）/ 不破坏 GL/核心循环/会计正确性（CRM 域不产生凭证）。
- **修复归 MR1 R1.0 展开器**：纯 Processor 代码逻辑修复（`validateStageDirection:99` 将 `toSeq < fromSeq` 改为 `toSeq <= fromSeq`），按 roadmap 预授权类目可自动执行，不触 ask-first。须协同 L4 `testEqualSequenceForwardSucceeds:96-105` 反向断言。

---

## 2. A4.2.98 — UC-CRM-13 ⑩ configSnapshot JSON 截断与 quotation 关联（P2-RC-038）

### 2.1 静态判定复核

| 站点 | file:line | 实测 |
|------|-----------|------|
| 快照生成 | `ErpCrmProductConfiguratorGenerateQuoteProcessor.buildConfigSnapshot:202-216`：`JSON.stringify({selectedFeatures, ruleEvaluation})` | ✅ |
| remark 装载 | `buildQuotationData:246`：`data.put("remark", "CPQ pricingSource=" + pricingSource + "; snapshot=" + truncate(configSnapshot, 500))` | ✅ |
| 截断函数 | `truncate:250-255`：`value.length() <= max ? value : value.substring(0, max)` — 超 500 字符截断为恰好 500 | ✅ |
| **ORM remark 列精度 census** | `module-sales/model/app-erp-sales.orm.xml:130` `<column name="remark" ... precision="1000" ...>`（propId=22，domain=`remark`[precision=1000，orm.xml:42]） | 见下 |

### 2.2 运行时确认

1. **截断行为确认**：configSnapshot 超 500 字符时 `truncate` 截断为前 500 字符，丢失尾部（后序 ruleEvaluation marks + 大型 selectedFeatures）。

2. **ORM 字段精度容量分析**：`quotation.remark` 列 = VARCHAR(1000)。remark 实际内容 = 前缀 `"CPQ pricingSource=" + pricingSource + "; snapshot="`（pricingSource 如 `BUNDLE:RULE` / `PRICE_RULE` / `BASE_PRICE`，前缀约 35-45 字符）+ 截断后 500 字符 = **约 535-545 字符**，远在 1000 列容量内。**无 DB 级溢出/截断风险**——500 字符截断是应用层在 insert 前施加的 cosmetic 切割，非 DB 约束。

3. **关键配置信息丢失评估**：
   - 典型配置（少量特征/规则）：snapshot 通常 < 500 字符，**无丢失**。
   - 大型配置（多特征/多规则）：snapshot > 500 字符 → 丢失尾部 ruleEvaluation marks（`featureCode/mark/featureValue` 三元组列表）。影响面 = **cosmetic**（remark 备注显示不完整），不影响定价/报价单创建/弱指针回写主路径。
   - configSnapshot 同时由 `buildConfigSnapshot:202-216` 返回给调用方供前端消费（UI 配置摘要），remark 仅作**二级审计轨迹**，非唯一记录——截断不导致配置信息彻底丢失。

### 2.3 裁决

**维持 P2-RC-038 P2（§2 P2① 边界弱）。**

- **主路径正确**：配置快照生成（满足 L1 `:319`「生成配置快照(JSON)」）+ 跨域建报价单 + 弱指针回写全实现且强测。
- **configSnapshot 落 remark 截断 cosmetic**：ORM 列（1000）充足承载应用截断（500）载荷，无 DB 溢出；应用层 500 截断可能丢大型配置尾部（cosmetic），且 snapshot 另有返回路径不唯一依赖 remark。
- **修复归 MR1 R1.0 展开器**：纯 BizModel（配置快照独立字段或扩 remark 长度），按 roadmap 预授权类目可自动执行，不触 ask-first。

---

## 3. A4.2.99 — UC-CRM-13 ⑫ generateQuote 弱指针 relatedBillType 枚举值契约一致

### 3.1 静态判定复核

| 站点 | file:line | 实测 |
|------|-----------|------|
| 枚举常量定义 | `ErpCrmConstants.java:25`：`String RELATED_BILL_TYPE_SALES_QUOTATION = "SALES_QUOTATION"`（注释：「转化结果弱指针单据类型（自由字符串，sales/master-data 侧无字典约束）」） | ✅ |
| 弱指针回写 | `ErpCrmProductConfiguratorGenerateQuoteProcessor.generateQuote:127-128`：`lead.setRelatedBillType(ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION)` + `lead.setRelatedBillCode(quotation.getCode())` | ✅ |
| **A1.28 同型回写交叉** | `ErpCrmConversionConvertToQuotationProcessor.java:27`：`facade.markLeadConverted(lead, ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION, ...)`（UC-CRM-03 转化路径） | ✅ 同一常量 |
| L1 契约字面值 | `docs/design/crm/use-cases.md:64`：`回写 lead.relatedBillType = 'SALES_QUOTATION'` | ✅ 字面值一致 |
| quotation.code 构造 | `buildQuotationData:225`：`code = "CPQ-" + configurator.getId() + "-" + CoreMetrics.currentTimeMillis()` | 见下 |

### 3.2 运行时确认

1. **枚举值契约一致**：常量 `"SALES_QUOTATION"` = L1 `use-cases.md:64` 字面值 `'SALES_QUOTATION'`，逐字一致。

2. **与 A1.28 UC-CRM-03 转化路径同型回写交叉确认**：`ErpCrmConversionConvertToQuotationProcessor:27`（UC-CRM-03 商机→报价单转化）使用**完全相同**的常量 `ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION`。两条路径（CPQ generateQuote / convertToQuotation）共享同一枚举常量单一真相源 → 契约一致。

3. **与 sales 域 quotation.code 命名空间无冲突**：
   - CPQ 路径 quotation.code = `"CPQ-" + cfgId + "-" + millis`（含毫秒时间戳），与 sales 域常规报价单 code 命名空间区分。
   - sales ErpSalQuotation 唯一键 = `UK_SAL_QUOTATION_CODE_ORG` on `(code, orgId)`（orm.xml:145）。CPQ code 含 cfgId + 毫秒时间戳，冲突概率实际为零。

4. **弱指针语义**：relatedBillType 为「自由字符串，sales/master-data 侧无字典约束」（ErpCrmConstants:25 注释）——crm 侧自管枚举值，sales 侧无回链字典校验，无跨域契约硬约束。

5. **L4 强断言**：`TestErpCrmCpqGenerateQuote#testGenerateQuoteViaBundlePricing:102-104` 断言 `relatedBillType == RELATED_BILL_TYPE_SALES_QUOTATION` + `relatedBillCode == quotation.getCode()`（强）；`TestErpCrmLeadConversion:127-128` 对 UC-CRM-03 路径断言同一常量（交叉证实）。

### 3.3 裁决

**主路径闭合（枚举值契约一致）。无需登记 watch-only。**

- 枚举值 `"SALES_QUOTATION"` 与 L1 字面值一致 + 与 A1.28 UC-CRM-03 转化路径共享同一常量单一真相源 + quotation.code 命名空间无冲突 + 弱指针为自由字符串无跨域硬约束 + L4 双路径强断言。契约闭合，无 cosmetic 风险需登记。

---

## 4. A4.2.100 — UC-CRM-13 ② conditionExpression XLang 评估失败模式

### 4.1 静态判定复核

| 站点 | file:line | 实测 |
|------|-----------|------|
| 评估入口 | `ProductConfigRuleEngine.evaluate:55-62`：expr 非空时 `match = evalCondition(expr, features)` 优先于 source 单行匹配 | ✅ |
| **XLang 评估** | `evalCondition:85-98` | 见下 |
| scope 设置 | `:87-88`：`XLang.newEvalScope()` + `scope.setLocalValue(null, "selectedFeatures", features)` | ✅ |
| 编译 | `:89-92`：`XLang.newCompileTool().allowUnregisteredScopeVar(true).compileFullExpr(SourceLocation.fromClass(...), expr)` | ✅ |
| 调用 | `:93`：`action.invoke(scope)` | ✅ |
| 返回 | `:94`：`return Boolean.TRUE.equals(result)`（仅 Boolean.TRUE 为 true；null/false/非 boolean → false） | ✅ |
| **失败模式** | `:95-97`：`catch (NopException e) { throw e.param("conditionExpression", expr); }` — 编译/评估失败重抛含 conditionExpression param | ✅ |

### 4.2 运行时确认

1. **复杂表达式评估行为**：XLang `compileFullExpr` + `allowUnregisteredScopeVar(true)` 编译完整布尔表达式。`selectedFeatures` 作为 scope 值（Map）注入，属性访问 `selectedFeatures.CPU_TYPE` 解析为 map key 查找。类 Javadoc（`:35`）显式文档化复杂示例 `selectedFeatures.CPU_TYPE == 'INTEL_XEON' && selectedFeatures.MEMORY == '64GB'`，`&&` 为标准逻辑与。复杂多特征表达式经 XLang 语义**评估行为符合预期**。

2. **失败模式确认**：`compileFullExpr` 对语法非法表达式抛 NopException，`evalCondition:95-97` 捕获并重抛 `.param("conditionExpression", expr)` ——表达式文本作为 param 附加，诊断完整，**无静默吞没**。

3. **L4 测试覆盖差距**：`TestProductConfigRuleEngine`：
   - `testConditionExpressionPriority:86-98`：简单单特征表达式 `selectedFeatures.CPU_TYPE == 'INTEL_XEON'` — 断言优先于 source 单行匹配（强）
   - `testConditionExpressionFalse:101-110`：简单单特征表达式 `selectedFeatures.MEMORY == '128GB'` 不匹配 — 断言不命中（强）
   - **差距**：**无测试覆盖复杂复合表达式**（含 `&&`，如 `selectedFeatures.CPU_TYPE == 'INTEL_XEON' && selectedFeatures.MEMORY == '64GB'`）。现有两测试仅覆盖单特征比较；复杂表达式路径仅经 Javadoc 示例文档化，**无 L4 断言**。

### 4.3 裁决

**主路径行为正确（简单表达式强测覆盖）；复杂表达式边界登记 watch-only。**

- **简单表达式**（单特征比较）经 XLang 评估行为正确 + 强测覆盖（2 @Test 强）。
- **复杂复合表达式**（`&&` 多特征）经 XLang 语义支持（Javadoc 文档化）但**无 L4 直接断言** → 登记 watch-only（复杂表达式边界）。失败模式（非法表达式 → NopException + conditionExpression param）确认正确，无静默吞没。
- watch-only 非强制修复；如需补强 = 纯测试补充（增 `&&` 复合表达式 @Test），按 roadmap 预授权类目可自动执行，不触 ask-first。

---

## 5. 与既有 finding 衔接（复用 or 新增裁决）

> 本报告为运行时确认，对 A1.30 §6 已建 finding 维持分级，不新建、不撤销、不重开。

| Finding ID | 本报告裁决 | 衔接依据 |
|-----------|-----------|---------|
| `P2-RC-036`（UC-CRM-06 ④ 等值边界） | **维持 P2 watch-only** | §1 运行时确认：等值（==）经 GraphQL 放行，config-gate 仅控 `<`；生产默认 STRICT；修复归 MR1 纯 Processor 预授权 |
| `P2-RC-037`（UC-CRM-13 ⑥ 前端 wizard successor） | **维持 P2 successor** | 本报告不触及前端 wizard（runtime confirm 范围外），维持 A1.30 裁决不变 |
| `P2-RC-038`（UC-CRM-13 ⑩ createFromConfig→save 方法名漂移 + configSnapshot 截断） | **维持 P2 watch-only** | §2 运行时确认：ORM remark 列（1000）充足，应用截断（500）cosmetic，snapshot 另有返回路径；createFromConfig 方法名漂移沿用 A1.30 裁决；修复归 MR1 纯 BizModel 预授权 |
| `P2-RC-039`（UC-CRM-13 ⑨ configSnapshot 落库断言弱） | **维持 P2 watch-only** | §2 确认 configSnapshot 落 remark 路径，断言强度沿用 A1.30 裁决；修复 = 纯测试补充预授权 |
| `P1-MA2-075`（stageId 单向递增守卫） | **维持 resolved R1.24** | §1 复核 `validateStageDirection:91-110` STRICT 默认 + config-gated 落地，genuinely resolved 无回退 |

**新增 watch-only（本报告登记）**：
- `A4.2.100-W1`（UC-CRM-13 ② 复杂 conditionExpression `&&` 表达式无 L4 断言 watch-only）——非 arm-index finding（无活跃数据破坏风险，纯测试覆盖边界），仅作本报告运行时观察注记，供后续 MR1 纯测试补强参考。

**结论**：无新 arm-index finding 新建（A4.2.99 主路径闭合 / A4.2.100 watch-only 为测试覆盖观察非 finding）。全部既有 finding 维持分级。

---

## 6. 多维度裁决汇总

| 维度 | 裁决 |
|------|------|
| 需求正确性 | 四项运行时确认均对齐 L1 use-case 验收标准；A4.2.97 等值边界偏离 L1 `<=` 字面（维持 P2）/ A4.2.98 截断 cosmetic（维持 P2）/ A4.2.99 枚举值逐字一致（闭合）/ A4.2.100 简单表达式行为正确（复杂边界 watch-only） |
| owner-doc 对齐 | state-machine.md §stageId 迁移规则 + cpq.md §实现注记 与实现一致（L2 推定向实现妥协，以 L1 为准） |
| 架构边界影响 | 零新跨模块依赖/API 契约变更/保护区域触碰（只读审计）；CPQ 跨域经 IErpSalQuotationBiz Facade 弱指针交接，零污染 |
| 验证充分性 | A4.2.97 经 GraphQL 集成测试运行时证实 / A4.2.98 ORM 精度+截断容量分析 / A4.2.99 双路径交叉+L4 强断言 / A4.2.100 简单强测+复杂边界 gap 已注记 |
| 回归风险 | 只读审计零生产代码变更，checker actual=baseline，无回归风险 |
| 路由技能选择 | verification/audit 任务路由正确；multi-dimensional-audit 技能匹配 |
| 待办策略漂移 | 无范围扩大/未完成项关闭/阻塞降级；4 项裁决均明确（维持 P2 / 闭合 / watch-only） |

---

## 7. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告为只读审计（零生产代码/ORM/api.xml/view.xml/真相源变更），checker actual = baseline（无代码变更故 actual 不变）。区分门控退出码 vs reporter 退出码——真正门控在 CI workflow，本报告不以 checker 脚本退出码作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告 closure audit 由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部裁决（P2-RC-036/037/038/039 维持 + P1-MA2-075 复用 resolved）已 grep arm-index 同域同控制点后给出维持裁决（§5），无未经比对直接新建的 finding。A4.2.100 watch-only 为测试覆盖观察非 arm-index finding。

---

## 8. 整体裁决

**Verdict: pass（零 P0、零 P1、零新 arm-index finding、4 项存疑点运行时确认完毕）。**

- **A4.2.97（等值边界，P2-RC-036）**：维持 P2。等值（==）经 GraphQL `ErpCrmLead__moveStage` 运行时放行（集成测试证实），config-gate 仅控 `<` 回退分支，生产默认 STRICT。修复归 MR1 纯 Processor 预授权。
- **A4.2.98（configSnapshot 截断，P2-RC-038）**：维持 P2。ORM remark 列（1000）充足承载应用截断（500）载荷，无 DB 溢出；大型配置尾部丢失 cosmetic，snapshot 另有返回路径。修复归 MR1 纯 BizModel 预授权。
- **A4.2.99（弱指针枚举值契约）**：主路径闭合。枚举值 `"SALES_QUOTATION"` 与 L1 字面一致 + 与 A1.28 UC-CRM-03 共享同一常量 + quotation.code 命名空间无冲突。无需登记。
- **A4.2.100（conditionExpression XLang 评估）**：主路径行为正确（简单表达式强测）；复杂 `&&` 表达式边界登记 watch-only（无 L4 断言）；失败模式（NopException + conditionExpression param）正确无静默吞没。

**CRM 域不直接产生会计凭证，四项均不触及业财保护区域探针。零活跃数据破坏。**
