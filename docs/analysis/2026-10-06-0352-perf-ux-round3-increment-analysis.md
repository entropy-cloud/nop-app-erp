# 性能与 UI/UX 深度优化第三轮（增量轮）分析报告

> 日期：2026-10-06
>
> 性质：触发条件驱动的 Deferred 重开轮 + 全新扫描角度增量轮。第二轮收官（0ac76cd13）后生产代码**零增量**（仅 1 个 docs 提交），故本轮不是「新代码回归扫描」，而是：①第二轮 §5 两项 Deferred 的重开触发条件（「批次 1/2 完成后的复扫轮」）已实质满足，本轮定案；②三路全新只读扫描通道（后端性能 / 前端 UI-UX / flux 渲染合规）以与上轮不同的切面（批次触达范围之外的同族面、平台范式缺口、门禁宇宙盲区）覆盖；③执行者对全部关键 finding 实仓源码级复核（含 3 处扫描通道结论修正，见 §4）。
> 扫描通道：3 个独立只读子代理（后端性能 / UI-UX / flux 渲染合规）+ 执行者复核。增量基线：`0ac76cd13`（上轮收官 HEAD，树净）。
> 关联：`2026-09-27-perf-ux-deep-optimization-analysis.md`（第一轮基线与 §6 Deferred）、`2026-10-05-0353-perf-ux-round2-increment-analysis.md`（第二轮 F1-F15 与 §5 Deferred）、`docs/architecture/flux-integration-gotchas.md` G-002、`docs/architecture/flux-page-export-and-validation.md`（门禁基线）。

---

## 1. 结论先行

| 面向 | 核心结论 |
|------|---------|
| 存量面 | **上轮修复零回归**。9 select 修复在位（live 面零残留缺 `type: source` 的 source 块）；stock-take-flow 数据面与实体逐列对齐；barcode 索引四点落地（源模型/生成产物/三方言 deploy SQL 逐字同构）；MaskAuditRecorder traceId 修复本体正确（null 保守多记=D1 裁决）；7 手写页 0 error 0 接线族 warning；`validate:flux` errors=325 精确持平 |
| 本轮核心发现 | **批次 1/2 触达范围之外存在四族同型漏网**（P1）：①16 个被服务 `*.flux.yaml` 孪生页中 17 个「hybrid 形态」按钮（`actionType:`+`action:`+`args:` 平铺、缺 onClick 包裹——改了键名未迁移结构的新误用形态，看板/日历/时间线的刷新过滤按钮全惰性）；②25 个报表页 ×2=50 个 `actionType: download`+`api` 按钮惰性——**flux 内置 `download` 动作存在且其 Form 1（`{api:{...}}`）与页面现有 AMIS `api` 形状直接同构，仓内机械可修**；③3 个看板 `onCardMove` 仍用 `@mutation:` 标量形态（G-002 已实锤陷阱的漏网家族，拖拽改状态全链必败）；④notify 收件箱「全部」tab 数据面双错（误接 `findUnread` + selection 请求实体不存在的 `read` 字段）——已读通知在任何 tab 永不出现 |
| 触发 Deferred 定案 | D2（warning 冻结基线）：per-page 数据齐备（7 页 103 条 unknown-property + 全局直方图），冻结机制可行（app 侧消费现有报告 JSON，勿动保护区验证器）；池漂移 18494→21254 的两新码均**生成层信息族非真缺陷**（§4）。D3（required vs validations）：定案为**flux 字段级校验 prop 推广**——flux 不存在 `validations` 块语法，真实创作面是字段级 prop（`required/min/max/pattern/...`），当前全库仅裸 `required`，真实高风险缺口=数值 min ×2 + 空值门 ×3 |
| 后端 | 1 个新立项级发现：`scanSave` 的 `nextLineNo` 每次扫码全量物化盘点行求 max+1（第二轮自产、barcode 默认开启的扫码热路径，单盘点 O(N)/扫、累计 O(N²)）；顺带实证 GATE-04 无界门禁宇宙盲区（仅扫 Dashboard/Report BizModel，实体 BizModel 结构性不可见） |

**本轮修复批次**（§5）：批次 1 = AMIS 残留形态清零（hybrid 17 + download 50 + 死 prop 视觉族）；批次 2 = 三看板 onCardMove /r/ 直发；批次 3 = notify 收件箱查询面闭环（含后端 findPage）；批次 4 = 向导/作业页交互反馈完整性；批次 5 = 后端小额收紧（scanSave + MaskAuditRecorder 分桶）；批次 6 = 基线门禁化收官（warning 冻结基线脚本 + 死文件 parse 门禁 + 基线文档刷新）。

---

## 2. 触发 Deferred 定案（第二轮 §5 两项）

### D2：7 页改写后 unknown-property warning 归零门禁化 —— **✅ 已落地（plan 2026-10-06-0355-6，2026-10-06）**

> **收官注记**：`check:flux-baseline` 门禁落地（五项检查+伴侣 self-test），冻结基线 `tools/flux-baseline.json`。执行期收获：①手写页 unknown-property 103→8（i18nEn 族随 flux dist 重建从报告消失，机制注记待考）；②parse 门禁立即兑现价值——暴露批次 2 的 YAML 引号终止事故（prj/cs kanban 导出失败被 totals-only 门禁掩蔽，已修复）；③7 页多重集冻结以修复后干净态测取。详见 flux-page-export-and-validation.md §7b。

- 触发条件「批次 1/2 完成后的复扫轮」已满足。实测（`_tmp/flux-page-validation-report.json`，2026-10-05 13:43 新鲜度，晚于复扫批）：
  - 7 个手写页全部 **0 error**，warning 共 **103 条且 100% 为 unknown-property**，接线族（actionType/api/onEvent/action/args）**零残留**——上轮修复的「warning 归零」目标已达成，冻结基线即为当前干净态。
  - per-page：pda-stock-take 9 / stock-take-flow 13 / disposal-wizard 17 / period-close-wizard 17 / visit-wizard 21 / lead-conversion 16 / inbox 10。
  - 直方图（(prop,renderer)×次数）：`i18nEn`×72（text 23/button 19/select 8/page 7/input-text 5/form 4/input-number 3/crud 1/button-group 1/textarea 1）、`level`(button)×14、`placeholder`×5、`valueField`(select)×3、`labelField`(select)×3、`wrapWithPanel`(form)×2、`tpl`(text)×1、`required`(button-group)×1、`options`(button-group)×1、`tabs`(tabs)×1。
- 冻结机制（建议，保护区验证器零改动）：app 侧小型门禁脚本消费现有报告 JSON——①7 页 (page,prop,renderer) 多重集冻结，语义用「禁止新增+允许收缩」（G-002:92 已计划上游将 `i18nEn` 白名单化，精确计数会在上游迭代时误报）；②接线族零容忍扩展到**全部被服务手写页**（不止 7 页）；③`totals.errors=325` 钉住；④新码计数器登记。
- 池漂移裁决：18494 → 21254（unknown-property 6349→9049 + conflicting-field-definition 11044 持平 + **新码** `unvalidated-component-target` 1154 + `duplicate-schema-id` 7）。两新码逐样本定案为生成层非真缺陷（§4）；unknown-property +2700 的增量需在批次 6 按报告 per-file 数据归族（预期主体=生成层，手写页活面增量已由本轮 findings 覆盖）。

### D3：required vs validations 全库统一核对 —— **触发，定案改题**

- 实证：flux **不存在 `validations` 块语法**（flux-compiler/flux-core 全库零命中）。真实校验创作面=字段级 prop：`required / minLength / maxLength / minItems / maxItems / pattern / atLeastOneFilled / allOrNone / uniqueBy / atLeastOneOf / equalsField / notEqualsField / requiredWhen / requiredUnless / format(email|url|integer) / async`（`flux-compiler/src/validation-lowering.ts:9-31`），触发模型 `validateOn: change|blur|submit`。**无数值取值校验规则**——input-number 的 `min/max` 是提交钳制（`input-number-renderer.tsx:17-19,129,145`）而非校验。
- 全库手写页（16 页）盘点：仅裸 `required` 在用（合计 ~20 处），高级校验 prop 零使用；`validations` 零命中。
- 真实高风险缺口（归批次 4）：①数值域无 `min`（F-R10）；②异步按钮空值门缺失 ×3（F-R9）。`pattern/minLength` 暂无高风险站点（reason 类字段有后端守卫），不强行铺开。
- 结论：Deferred 项以「字段级校验 prop 按风险站点推广」结案，不做全库机械统一（flux 范式本身字段级、无块语法可「统一」到）。

---

## 3. 全部新发现清单（按严重级；编号 R*=round 3）

### 3.1 P1（确认缺陷）

| # | 发现 | 位置 | 证据 | 修复方向 |
|---|------|------|------|---------|
| F-R1 | notify 收件箱「全部」tab 数据面双错：data-source 误接 `ErpSysNotification__findUnread`（与未读 tab 完全同源——已读通知在全部 tab 永不出现，tab 语义失效）+ selection 请求实体不存在的 `read` 字段（已读态维护在 `ErpSysNotificationRead` 关联表，`erp_sys_notification` 无此列）→ GraphQL `undefined-field`；`read` 映射列恒不渲染。**草案审查修正（owner doc 对齐）**：`docs/design/notify/inbox-patterns.md` §2 已有权威裁决——全部 tab = 客户端拼接 findUnread+findRead（明文「避免新加 findInbox 方法」）；页面从未实现该设计，本缺陷本质是 code-vs-owner-doc 漂移，修复应优先向 owner doc 收敛（客户端拼接）而非新端点 | `module-notify/erp-notify-web/.../ErpSysNotification/inbox.page.yaml:256,:260` × `ErpSysNotificationBizModel.java:75-108`（仅 findUnread/findRead/countUnread）× `module-notify/model/app-erp-notify.orm.xml:90-110` × `docs/design/notify/inbox-patterns.md` §2 | 执行者复核实证 + 草案审查 agent_18b3414c owner doc 引证；导出产物同型。F14（stock-take-flow 数据面错位）同族新站点 | 批次 3：按 owner doc 裁决实现客户端拼接（read 派生机制 Decision），筛选接线 Decision（@Optional 白名单参数 vs 移除筛选 UI）；新端点仅作拼接机制不可行时的 fallback（须 owner doc 同步更新） |
| F-R2 | 3 个看板 `onCardMove` 用 `@mutation:` 标量参数 URL——批次 1 已运行时实锤的 G-002/F13 陷阱（壳层 `nopRpcResolver.ts:274-277` 仅适配 `__save` 类 data 参数操作，标量参数报 `unknown-operation-arg`）的**漏网家族**：拖拽改状态（prj startTask/completeTask/blockTask/unblockTask、cs assign/start/resolve/close/reopen/cancel 6 分支、crm moveStage）全链必败。附带：prj kanban `data` 内混入 `i18nEn` 杂键（会被当作 mutation 参数发出，且中文 literals 的 i18nEn 放置层级错误） | `module-projects/erp-prj-web/.../ErpPrjTask/kanban.flux.yaml:54-62`、`module-cs/erp-cs-web/.../ErpCsTicket/kanban.flux.yaml:83-93`、`module-crm/erp-crm-web/.../ErpCrmLead/opportunity-kanban.flux.yaml:53-58` | 执行者复核实证（三文件源码逐一）；`@mutation:`+标量 data 形态与批次 1 探针实锤形态同构；后端签名全标量（`ErpPrjTaskBizModel.java:114,140`、`ErpCsTicketBizModel.java:232-300`、`ErpCrmLeadBizModel.moveStage`）。E2E 从不拖拽故零暴露（F1 同机制）。cs 侧 `cs-026-r3` 注释显示路由逻辑此前已被修订过（6 分支覆盖+落空拒拖），仅传输形态漏改 | 批次 2：三处 url 改 `/r/Erp*__method` 直发（G-002 规则）+ 移除 prj data 内 i18nEn 杂键；运行时探针定案（拖拽事件或同构 fetch 探针：负对照=`@mutation:` 形态失败复验、正对照=`/r/` 形态成功）；cs resolve 分支 `resolution` 传递裁决（§4 修正：非必填，可不传） |
| F-R3 | 16 个被服务 `*.flux.yaml` 孪生页中 **17 个「hybrid 形态」惰性按钮**：`actionType: ajax` + `action: ajax` + `args:` + `target:` 全部平铺在按钮级、缺 `onClick` 包裹——对照批次 1 已修复页（onClick ActionSchema）与迁移表（09-amis-migration.md:52），这是**改了键名未迁移结构的半迁移新形态**（批次 1 迁移记录 gotchas:125 只覆盖孪生 start/complete/cancel 与 4 镜像，不含刷新/过滤按钮）。按钮全部惰性：看板/日历/时间线/甘特/看板的「刷新」与「按项目过滤」点击无任何效果 | 16 文件 17 处（执行者 grep 复核计数一致）：aps/schedule-gantt、b2b/edi-detail、b2b/asn-flow、ct/version-diff×2、crm/calendar、crm/timeline、crm/opportunity-kanban、cs/kanban、cs/ErpCsTicketAction/timeline、drp/net-requirement、hr/team-vacation-calendar、hr/org-chart、log/shipment-tracking、mfg/bom-tree、prj/kanban、pur/three-way-match（各 `*.flux.yaml`） | 验证器报告 per-file wiring 族 warning 交叉验证（每按钮 3 条 actionType/action/args unknown-property）；button renderer 契约同 F1（仅消费 onClick） | 批次 1：按批次 1 同法机械改写 `onClick: {action:'ajax', args:{...}}`（target → then:refreshSource 链裁决）；探针正负对照（负对照=hybrid 不发请求，修复后正对照复验）；G-002 增补具名形态「hybrid actionType+action+args」 |
| F-R4 | 25 个 `report/*.page.yaml`（均无 flux 孪生，为被服务活页）× 各 2 个下载按钮 = **50 个 `actionType: download`+`api` 惰性按钮**——flux 模式下全部种子报表的 XLSX/PDF 下载不可用 | 执行者复核实证（lead-conversion-funnel.page.yaml 等；同族分布 ast×2/crm×3/cs×1/fin×5/hr×2/inv×1/md×2/mfg×3/mnt×2/prj×2/qa×2） | **扫描通道「外部仓能力缺口」结论已被执行者复核推翻**：flux `BUILT_IN_ACTION_REGISTRY` 内置 `download` 动作（`flux-core/src/constants.ts:54`），运行时 `executeRuntimeDownloadAction` Form 1（`{api:{...ExecutableApiRequest}}`，responseType blob 强制，`runtime-action-helpers.ts:328-368`）与页面现有 AMIS `api:{url,method,data}` 形状**直接同构**，且有运行时测试（`download-action.test.ts`） | 批次 1：仓内机械改写 `onClick: {action:'download', args:{api:{url,method:post,responseType:blob,data:{...}}}}`（50 处同构）；运行时证明=至少 1 页真实下载断言（blob 响应落盘）+ 其余机械同构 + flux 门禁零新增 |

### 3.2 P2（一致性/正确性问题）

| # | 发现 | 位置 | 修复方向 |
|---|------|------|---------|
| F-R5 | **messages 范式未随批次 2 推广**：6 个 mutation 按钮静默成功（无 `messages.success/failed` toast）——disposal-wizard 确认提交（且无 then 链：不刷新 assetsData、不重置 step1Form，可重复提交；assetsData 查询无 status 过滤，已处置资产仍在下拉）、lead-conversion moveStage 确认（无 then：stages/leads 不刷新、「当前阶段」陈旧）、visit-wizard 保存结果（无 then：零反馈、visitData 不刷新）、period-close closePeriod/finalizePeriod/reverseClose（**草案审查修正**：三按钮已有 `then: refreshSource periodData` 链 main.flux.yaml:127-129/:177-179/:212-214，缺口仅为 messages ×3）（runtime `runtime-action-helpers.ts:162-171` 实证 success toast 仅作者声明时弹出） | `disposal-wizard/main.page.yaml:123-141`（执行者复核实证）、`lead-conversion/main.page.yaml:96-110`、`visit-wizard/main.flux.yaml:166-178`、`period-close-wizard/main.flux.yaml:114-129,:164-179,:198-214` | 批次 4：按 pda 页范式补 `messages.success/failed`；disposal/lead-conversion/visit 另补 `then`（refreshSource/setValues 重置）；disposal 查询补 status 白名单 |
| F-R6 | stock-take-flow 三按钮依赖手输不可见数字主键 `takeId`（input-number required）：表格不显示 id、无行接线，用户对着「单号 STKxxx」列表无从得知输入什么——F4 同族新站点（F4 仅修 pda 页） | `stock-take-flow/main.page.yaml:69-76` × 列定义 :35-58（无 id 列） | 批次 4：Decision 裁决形态——①select 绑定 `ErpInvStockTake__findPage`（F14 修复后数据面可用，code→id 映射，零后端改动，与 period-close 期间 select 同范式）；②后端 startTake/completeTake/cancelTake 增 takeCode @Optional 形态（pda scanSave :66-91 先例）。倾向① |
| F-R7 | notify 收件箱筛选表单整体死接：notificationType/channel/sentAtFrom/sentAtTo/status 五个筛选字段不驱动任何查询（四个 data-source 均裸 `data:{userId:null}`）。**草案审查修正**：「刷新」按钮并非缺口——已是四源 refreshSource 链（:107-121），本项缺口仅为筛选字段未接线 | `inbox.page.yaml:51-105` vs 四个 data-source（:9-16,:131-139,:202-210,:252-260） | 批次 3 随 F-R1 处置：筛选接入查询 args 或移除筛选 UI（Decision）；刷新按钮保持不变 |
| F-R8 | period-close `main.page.yaml` 是**非法 YAML**（单引号串内 `\'` 转义，PyYAML PARSE FAIL line 191）——flux 模式下该死文件从不被解析故门禁全绿；一旦切回 AMIS 模式或删除 flux 孪生，该页路由即崩。死文件族零门禁覆盖（visit-wizard 死孪生的表达式语法错 `${visitType \|\| \|\| "—"}`、kanban.page.yaml 全库唯一 danger-无-confirm 命中等同族） | `period-close-wizard/main.page.yaml:191,194-195`（执行者 PARSE FAIL 复现实证） | 批次 6：修复非法 YAML（`\'`→`''`）+ gate 脚本增**全量 page/flux yaml parse 检查**（死文件也纳门禁）止血该族；死孪生归宿政策（删除 vs 豁免标记）登记 Deferred（需 owner doc 裁决） |
| F-R9 | 异步按钮空值门缺失 ×3 + 在途无禁用：①pda 保存按钮无空值门（空字段也弹 confirm 后打服务器）；②lead-conversion 确认按钮无 `${!toStageId}` 门；③period-close 反结账按钮丢了死文件里的 `disabled:"${!reverseCloseReason}"` 门（后端 `ERR_REVERSE_CLOSE_REASON_REQUIRED` 兜底但用户只得到通用失败 toast）。在途面：flux button `loading` 仅来自显式 prop（`button.tsx:71,210`），无 onClick 异步自动置忙，全库手写页 `loading:` 零命中——closePeriod（AR→AP→INV→AST→GL 重编排）在途可再点 | `pda-stock-take/main.page.yaml:53-83`、`lead-conversion/main.page.yaml:96-110`、`period-close-wizard/main.flux.yaml:198-214`（执行者复核：then 链在位、disabled 门确失） | 批次 4：补三处 disabled 空值门表达式；在途 busy 全按钮推广登记 Deferred（trigger=flux auto-busy 能力或实测反馈），仅对 closePeriod 加 scope 状态位示范裁决（Decision） |
| F-R10 | 数值域无 `min` 钳制（前端）×2 + 后端符号校验缺口 ×1：pda `actualQuantity` 无 `min:0`（负数直达后端由 BizModel 抛错，多付一次往返）；disposal `disposalAmount` 无 `min:0` 且**后端对负数零校验**（`ErpAstDisposalProcessor.java:121-122` 直接参与损益，负处置金额静默放大清理损失） | `pda-stock-take/main.page.yaml:48-52`、`disposal-wizard/main.page.yaml:62-68` | 批次 4：前端两处 `min:0`；后端符号守卫归批次 5 Decision（须先核对 ast owner doc 是否已声明金额语义，防真空立法） |
| F-R11 | `scanSave` 的 `nextLineNo` 每次扫码全量物化盘点行求 max+1：`findAllByExample(仅 takeId)` 全列物化 + 内存 for 求最大——单盘点 O(N) 行/次、整盘点累计 O(N²)，位于 barcode 默认开启的扫码热路径（第二轮自产） | `ErpInvStockTakeLineBizModel.java:152-162`（执行者复核实证），调用点 :139 | 批次 5：单查询化——OQL `select max(lineNo)` 或 `orderBy(lineNo desc)+limit 1` 点查（现有 `IDX_INV_STOCK_TAKE_LINE_TAKE_ID` 承载；注意 A9 登记的「纯维度投影仍注入主键维度」平台坑——即便注入仍单行返回，收益保持）。附带裁决：`ErpInvStockTakeLine` 无 (takeId,lineNo) UK（并发双扫静默重复行号）——orm 保护区变更，Decision 倾向 watch-only 登记（单用户 PDA 时序、UK 需存量数据去重前置） |
| F-R12 | flux 门禁基线文档漂移：warning 池 18494→21254、新码 ×2（`unvalidated-component-target` 1154 = 生成层 querySubmit/queryReset 组件定向「family 正确但无 target-binding 元数据不可编译期定型」信息族；`duplicate-schema-id` 7 = 生成层重复 id）、files 856 vs 文档 855 | `_tmp/flux-page-validation-report.json` totals + 逐样本（执行者复核）× `flux-page-export-and-validation.md:244-304,:273` | 批次 6：基线文档刷新（新码裁决登记归生成层外部 Deferred 车道）+ gate 脚本钉住 totals |

### 3.3 P3（体验优化）

| # | 发现 | 位置 | 修复方向 |
|---|------|------|---------|
| F-R13 | select `labelField`/`valueField` 为 flux 不存在的死 prop（富标签意图未实现，实际只显 code）：disposal 资产 select（"code · name (净值)" 意图）、lead-conversion ×2 | `disposal-wizard/main.page.yaml:38-43`、`lead-conversion/main.page.yaml:43-47,:89-90` | 批次 1（死 prop 清零批）：富标签并入 options 表达式的 `label:` 映射（`.map(a=>({label: a.code+' · '+a.name,...}))`），删死 prop |
| F-R14 | 按钮 `level:` 死 prop 视觉族：flux ButtonSchema 用 `variant`（`default/primary/destructive/outline/secondary/ghost/link`），`level` 非 button prop——手写页 16 文件的 `level: primary/danger` 从未生效，**危险按钮无红色样式、主按钮无强调样式**（confirmText 等行为不受影响） | 16 文件（执行者 grep 复核；7 手写页内 level(button)×14 + 孪生/报表页活面） | 批次 1：活面 `level:X` → `variant:`（primary→primary、danger→destructive、其余映射 Decision 登记）；死文件不动 |
| F-R15 | 小额体验面：①period-close 期间 select source 块缺 `query:{limit}`（visit-wizard 有 limit 200，同族两形）；②stock-take-flow/disposal 作业列表静默 200 上限无分页/截断指示；③visit-wizard 备件表 totalAmount 裸 DECIMAL 直出（**批次 4 复核修正：假阳性**——totalAmount 仅存在于 selection 未被任何文本/列渲染，无改写点；正例：disposal 预览已用 `ROUND(x,2)`） | `period-close-wizard/main.flux.yaml:28-34`、`stock-take-flow/main.page.yaml:25`、`disposal-wizard/main.page.yaml:20`、`visit-wizard/main.flux.yaml:78,:127` | 批次 4：limit 形状补齐 + footerToolbar 分页/截断提示（ROUND 项=假阳性无改写点，见③注记） |
| F-R16 | 日期/时间戳原串直出（系统性平台范式缺口）：flux 表格无 date 列类型/格式化路径，inbox sentAt 三处 TIMESTAMP 原串 + visitDate 文本拼接；生成页同形态（app 级基线非手写页个例） | `inbox.page.yaml:159-162,:230-233,:295-298`、`visit-wizard/main.flux.yaml:97`、`_gen/_ErpSysNotification.view.xml:61` | **Deferred 登记**（trigger：flux 表格列格式化范式落地或逐域 UX 迭代）；属平台能力缺口非本仓单页可修 |
| F-R17 | **批次 1 执行期新发现（2026-10-06）**：view.xml delta 层对话框按钮 AMIS 惰性接线流入 flux 导出——批次 1 清零手写页/报表页后，门禁残余 28 条 wiring warning（actionType/api/onEvent 族）全部位于 `ErpFinVoucher`/`ErpFinVoucherTemplate`/`ErpFinVoucherBillR`/`ErpMdMaterial` 等 **view.xml delta 页面**（`x:extends="_gen/..."` 的 app 仓维护 override，gen-control onEvent + 对话框按钮 actionType+api）。修正 round-3 §4 早前「gen-control 层不在 flux 导出范围」的判断——delta 内容确实进导出。按 F6 教训：惰性机制虽已实锤（同 renderer），但**对话框运行时可达性未探针**（这些 dialog 入口在 flux 模式是否被触发未知），修复前须先运行时可达性证据 | `module-finance/erp-fin-web/.../ErpFinVoucher*/`、`module-master-data/erp-md-web/.../ErpMdMaterial/ErpMdMaterial.view.xml`（x:extends delta） | **Deferred 登记**（trigger：相关对话框 flux 运行时可达性探针证实惰性面暴露后按 G-002 规则迁移；或逐域 UX 迭代触及时处置）；门禁侧由批次 6 的接线族零容忍规则豁免生成页宇宙（仅手写页清单纳入） |
| F-R18 | **✅ 已修复（plan 2026-10-06-0555-1，双批准，nop-chaos-flux b18117e97）**：flux `refreshSource` 跨 fragment scope 解析失败——`refreshDataSource` 对传入 scope 仅做 exact-bucket 查找（无父链遍历、nameIndex 亦不可达），按钮（form 内）刷新页面级数据源报 `Source not found`（fluxDebug notify error 实证）。flux-guide 01/05 官方示例的规范形态运行期不可达，波及本批 17 按钮与第二轮 then:refreshSource 跨 scope 站点。修复=scope 链最近优先遍历（同名遮蔽保持）+ nameIndex 兜底 + isolate 边界截断（C2）；guide data-source.md 限制声明同步改写（C1）；+4 单测；E2E 探针转绿验收 | `nop-chaos-flux/packages/flux-runtime/src/async-data/source-registry.ts` × `action-adapter.ts:511-527` | ✅ 修复落地，gotchas G-003 登记 |
| F-R20 | **批次 3 执行期新发现（2026-10-06）**：收件箱页在 auth-ON E2E 中从未 UI 可达（预存环境缺陷家族）——①唯一 role-scoped 菜单（notify-inbox TOPM/SUBM roles="user"）对 E2E 账号 nop 不可见（种子缺 `1,user` 角色绑定；且 runner jar 打包种子与工作区种子存在时点差，重打包后才生效）；②客户端 permission guard（nop-chaos-next packages/core usePermissionGuard）按字面角色集合拒绝，与服务端 admin bypass 语义不一致；③既有 notify-inbox spec 的 markRead/markAllRead 两用例预存失败（后端所有权守卫「当前用户[1]无权操作接收人[nop]」与 spec 期望漂移——该 spec GraphQL 驱动从不触 UI，路由 '/ErpSysNotification-inbox' 页面是否渲染从未被断言）。诊断链：错误路由排查 → SiteMap API 比对（服务端正确）→ 菜单 dump（438 按钮无通知中心）→ TOPM 字段 diff（roles 字段差异）→ 权限守卫源码定位 | `app-erp-all/src/main/resources/_vfs/_init-data/nop_auth_user_role.csv`、`nop-chaos-next/packages/core/src/hooks/usePermissionGuard.ts`、`tests/e2e/business-actions/notify-inbox.action.spec.ts` | **独立批次 successor**（批次 6 后）：①E2E 账号角色绑定生效路径定案（种子重打包已做，getLoginUserInfo roleInfos 返回面待查）+ ②客户端守卫语义裁决（与服务端 admin bypass 对齐[nop-chaos-next 双批准]或 E2E 专用账号体系）+ ③markRead/markAllRead spec 期望更新 + ④skip 守卫用例转绿验收（plan 0355-3 的 all-tab 用例已在树） |
| F-R19 | **批次 1 Phase 3 附带发现（2026-10-06，登记后精确化）**：报表 `/p/{Entity}__download` 直连层存在预存失败家族——首轮直连 8 用例中 crm funnel/mfg crp-load/cs ticket-sla ×2 失败（响应为 JSON 错误信封 `{"bizFatal"...}` 族，含 `nop.err.auth.no-permission` 形态[stale-token 探针佐证错误通道]），fin/md 通过；**后续 UI 层复跑 crm funnel/cs ticket-sla ×2 转绿**（负载波动/时序因素），mfg crp-load 直连+UI 双挂持续（确定性后端失败）、md partner-list 直连过而 UI 持续挂（页面特定，待诊断）。均为预存后端/稳定性问题（直连层绕开全部前端，与本 mission 前端改动无关），涉及 auth/permissions 保护区（plan-first + roles-and-permissions owner doc 对齐） | `/p/ErpCrmReport__download`、`/p/ErpMfgReport__download`、`/p/ErpCsReport__download`、md partner-list UI 层（E2E `reports.download.spec.ts`/`reports.amis-download.spec.ts` 证据） | **登记 + 独立批次**（批次 2-6 之后：根因探针[auth 资源表 diff + 渲染日志]→auth 对齐 roles-and-permissions.md 镜像规则→渲染稳定性定案→E2E 断言）；不阻塞批次 1（下载断言以 6 绿用例为准） |

---

## 4. 复核后修正与不修裁决（扫描通道结论的执行者定案）

- **假阳性修正 ①**：UI/UX 通道称「cs 看板拖到 RESOLVED 即使 URL 修好也必被后端 resolution 必填守卫拒绝」——**证伪**：`ErpCsTicketBizModel.resolve` 的 `resolution` 为 `@Optional`（:266-268），processor 对 null 安全（`ErpCsTicketResolveProcessor.java:73-78` 仅非空时写 remark）。拖拽 resolve 传 ticketId 单参即合法。F-R2 修复不含「补 resolution 交互」义务。
- **假阳性修正 ②**：flux 通道称 50 个 download 按钮修复须走「外部仓能力缺口 lane」——**推翻**：flux 内置 `download` 动作存在（constants.ts:54）且 Form 1 与现有 api 形状同构，仓内机械可修（F-R4）。
- **新码定案（非真缺陷）**：`unvalidated-component-target` 1154 条逐样本为生成层 querySubmit/queryReset 选择器信息族（message 自证「selector family 正确」）；`duplicate-schema-id` 7 条为生成层重复 id（crud-grid 等），登记生成层外部 Deferred 车道（与 11044/6349 族同道），不立 app 仓修复项。
- **不修（登记）**：
  - `ErpInvStockLedgerBizModel.checkStockBalanceConsistency` 余额侧 `findAll()`（:110）——对账语义需全集，截断=错误方向（mnt watch-only 教训）；大头侧流水已是 DB 级 GROUP BY 正面范式。watch-only。
  - GATE-04 无界门禁宇宙扩展（实体 BizModel 站点）——F-R11 即盲区实证，但宇宙扩展=新台账全量裁决面（大量合法小表读），独立立项代价>本轮收益。Deferred（trigger：实体层无界台账立项）。
  - MaskAuditRecorder 500-cap 请求内 clear-all → >500 distinct cell 请求中段整集清空、已记录 cell 同请求重记（E4.2 在大请求内漂移；方向=过记保守，非性能风险）——随批次 5 以 traceId 分桶换代消解（分桶后键生命周期=单请求，容量上限自然消去）。
  - legacy 死孪生 AMIS 残留（kanban.page.yaml danger-无-confirm 唯一命中、visit-wizard 死孪生语法错等）——live 面危险确认 44/44 结论仍成立；死文件族归宿归 F-R8 批次 6 parse 门禁 + Deferred 政策项。
  - view.xml gen-control 层 AMIS 风格（~40 处 onEvent.setValue + dialog 按钮 ajax+api）——不在 flux 导出范围、无惰性实证，登记独立 lane 豁免标记候选，不与本轮混计。

---

## 5. 修复批次规划

> 每批 = 一个独立 plan（独立草案审查 → 执行 → 验证 → 独立结束审计 → 独立 git 提交），遵循 `00-plan-authoring-and-execution-guide.md`。执行顺序 1→2→3→4→5→6；批次 6 最后（warning 冻结基线必须在批次 1-4 消灭接线族 warning 之后测取，门禁诞生即绿）。

| 批次 | Plan 主题 | 覆盖发现 | 改动面 | 保护区/审批 |
|------|----------|---------|--------|------------|
| 1 | AMIS 残留形态清零（hybrid 按钮 + download 按钮 + 死 prop 视觉族） | F-R3 + F-R4 + F-R13 + F-R14 | 16 flux.yaml（17 按钮 onClick 化）+ 25 report 页（50 按钮 download 化）+ labelField/valueField 富标签化 3 处 + level→variant 活面盘点改写；G-002 增补 hybrid/download 形态 | 无（本仓页面文件）；探针正负对照 + ≥1 真实下载断言 |
| 2 | 三看板 onCardMove /r/ 直发修复 | F-R2 | 3 个 kanban.flux.yaml + prj i18nEn 杂键移除；gotchas 增补 onCardMove 族登记 | 无；运行时探针定案（负对照 @mutation 失败复验 / 正对照 /r 成功） |
| 3 | notify 收件箱查询面闭环 | F-R1 + F-R7 | `ErpSysNotificationBizModel` 增 findPage 式查询（read 投影 Decision）+ inbox.page.yaml 全部 tab 改接 + 筛选接线 + E2E | 无（notify 非保护区）；后端查询须 limit+白名单（无界门禁对照） |
| 4 | 向导/作业页交互反馈完整性 | F-R5 + F-R6 + F-R9 + F-R10（前端半）+ F-R15 | messages/then ×6 按钮（4 页）+ 空值门 ×3 + min:0 ×2 + select limit + ROUND + 200-cap 指示 + stock-take-flow takeId 形态 Decision | 无（页面面）；disposal 查询 status 白名单属查询参数非契约变更 |
| 5 | 后端小额收紧（scanSave 热路径 + MaskAudit 分桶） | F-R11 + F-R10（后端半 Decision）+ MaskAudit 500-cap | `ErpInvStockTakeLineBizModel.nextLineNo` 单查询化 + `MaskAuditRecorder` traceId 分桶换代 + JUnit 扩展 + StockTakeLine UK 裁决 + disposalAmount 符号守卫 Decision | 无 orm 变更（UK 若立须 orm 保护区双批准——Decision 倾向 watch-only）；ast 符号守卫须 owner doc 依据，无依据则 watch-only |
| 6 | flux 基线门禁化收官 | F-R8 + F-R12 + D2 落地 | app 侧 gate 脚本（7 页 warning 多重集冻结[修复后测取] + 全被服务手写页接线族零容忍 + totals.errors=325 钉住 + 新码计数器 + 全量 yaml parse 检查）+ period-close 死文件 YAML 修复 + `flux-page-export-and-validation.md` 基线刷新 + 本报告收官注记 | 无（app 仓工具与文档）；保护区验证器（nop-chaos-flux）零改动 |

## 6. Deferred But Adjudicated（本轮新增/更新）

| 项 | 分类 | 不做原因 | 重开触发条件 |
|----|------|---------|-------------|
| 日期/时间戳/金额列格式化平台范式（F-R16） | out-of-scope improvement | flux 表格无 date 列类型，app 仓逐页手拼 ROUND/dateText 不成范式；生成页同形态属平台基线 | flux 表格列格式化能力落地；或逐域 UX 迭代时按页处置 |
| GATE-04 无界门禁宇宙扩展至实体 BizModel | optimization candidate | 新台账=全量裁决面（大量合法小表读），独立立项代价大；F-R11 已单点修复 | 实体层无界站点台账立项（第四轮性能轮） |
| 在途 busy/loading 按钮全推广（P2-5 残留半） | watch-only residual | flux 无 auto-busy，scope 状态位逐按钮加为高 churn 低收益 | flux runtime auto-busy 能力落地；或重编排按钮实测误触反馈 |
| 死孪生文件族归宿政策（删除 vs 豁免标记） | optimization candidate | 涉及测试夹具引用与审计口径，须 owner doc/ui-patterns 裁决 | ui-patterns 或 view-and-page-strategy 增补死文件条款 |
| 编辑态误关丢失保护 | out-of-scope improvement | flux 全包零 beforeunload/关闭守卫机制（外部仓缺口，app 无从使用） | nop-chaos-flux 关闭守卫机制落地 |
| `duplicate-schema-id`×7 / `unvalidated-component-target`×1154 | out-of-scope improvement | 生成层信息族，app 仓不可达（与 11044/6349 族同车道） | nop-entropy/nop-chaos-flux 模板迭代时 |
| StockTakeLine (takeId,lineNo) UK | watch-only residual | 单用户 PDA 时序并发面窄；UK 需存量数据去重前置 + orm 保护区双批准 | 并发盘点场景立项；或 md 域 G1 类批量索引轮顺带 |
| mfg 之外 roles 镜像全量复查、pda 节流、A4/A5/G2/G3/B6/C1/I2 等 | 维持既有登记 | 上轮 §5/§6 逐条核对未触发（本轮无新增证据） | 沿用原表触发条件 |

## 7. 验证与基线义务（适用全部批次）

- 全项目构建：`mvn clean install -DskipTests`（批次 3/5 触后端）
- 单测：`mvn test -pl <module> -am`（触域模块）；批次 5 含 red-green
- flux 门禁：`npm run validate:flux`（对照 errors=325 零新增；批次 1-4 后 wiring 族 warning 应净下降，批次 6 冻结）
- 无界查询台账：`npm run check:unbounded-queries`（批次 3/5）
- 合规检查器：`bash docs/audits/nop-compliance-checker.sh`（lesson 07 义务，actual>baseline 逐站点分类）
- CJK checker --strict（触运行时字符串批次；i18nEn 杂键移除注意 CAT4 义务平衡）
- E2E：批次 2/3 含运行时探针/点击断言（`docs/testing/e2e-runbook.md` 强制 flux 渲染模式）
- 每批完成更新 `docs/logs/2026/10-06.md`；每批独立 git 提交
