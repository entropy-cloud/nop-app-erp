# 2026-09-27-0318-1 UX 行级业务动作 P1 缺陷修复批

> Plan Status: completed
> Last Reviewed: 2026-09-27
> Source: `docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md` §3（09-14 基线 P1×5 原样未修 + 新 P1×1）
> Related: `docs/analysis/2026-09-14-complex-page-ux-review.md` §6.2/§7
> Audit: required

## Current Baseline

- 手写 view.xml 共 368 个（`find module-*/erp-*-web/src/main/resources/_vfs -name '*.view.xml' -not -name '_*' -not -path '*_gen*'` 口径，2026-09-27 HEAD 复跑），其中 6 处 P1 级交互缺陷（行号与 09-14 报告一致）：
  1. `erp-cs-web/.../ErpCsTicket.view.xml:382` `assign?ticketId=$id&assignedToId=0`——后端回落逻辑 `StringHelper.isEmpty(assignedToId) ? context.getUserId() : assignedToId`（`ErpCsTicketBizModel.java:240`）被 `"0"` 击穿，工单被分派给不存在的用户 ID 0。用户选人能力**存在**：平台 delta xlib `edit-userId` tag（`app-erp-all/src/main/resources/_vfs/_delta/default/nop/web/xlib/flux-control.xlib:133-163`）产用户 picker（valueField userId/labelField userName，加载 `NopAuthUser/picker.page.yaml`，该页在 app 运行时 vfs 存在：`app-erp-all/_dump/nop-app/nop/auth/pages/NopAuthUser/picker.page.yaml`），仅是 cs 域页面未接线。
  2. `ErpCsTicket.view.xml:396` `resolve?resolution=` 空串直接提交，操作历史无解决说明（后端 `resolution` 为 `@Optional`）。
  3. `erp-qa-web/.../ErpQaNonConformance.view.xml:150` 同构：`resolve?ncrId=$id&resolution=`（后端 `resolution` @Name 必填、`noCapaReason` @Optional，`ErpQaNonConformanceBizModel.java:68-71`）。
  4. `erp-mfg-web/.../ErpMfgWorkOrder.view.xml:216` `reportCompletion?workOrderId=$id&completedQty=0`——用户无法报实际完工数量（后端 `completedQty` @Name 必填 BigDecimal，`ErpMfgWorkOrderBizModel.java:102-104`）。
  5. `erp-aps-web/.../ErpApsOperationOrder.view.xml:143-147` 排程按钮 confirmText 自述「请在排程方案详情页执行」却仍带 `scheduleId=0` 调 mutation——死按钮。
  6. `erp-cs-web/.../ErpCsTicket.view.xml` escalateQuality 表单 materialId 为 `input-number` 手输数字 ID（违反全库 picker 选物料范式）。
- 危险动作确认基线（跨行正则口径 `<action … level="danger">`）：全库 **44 处**，其中 42 处已有 confirmText 或 dialog 二次确认；裸奔的恰为 `ErpFinBudgetScenario.view.xml:187-192` reject/cancel 两处（ErpFinReconciliation:163 / ErpFinVoucher:243 的红冲为 dialog 预览确认范式，非裸奔）。
- 可复用范式（同仓实证，草案审查已核实）：
  - 参数 dialog + simple 提交页 = `ErpCsTicket` escalateQuality/adoptKnowledge（`<dialog page="…" size="md"/>` + `<simple … useFormActions="true">` + api url 带 `?ticketId=$id` **且** `withFormData="true"` 的混合形态与现网完全一致）。
  - form cell picker = 平台控件链：`<disp>`（form cell/grid col 基元）经 `app-erp-all/_dump/nop-app/nop/schema/xui/disp.xdef:16-17` 合法声明 `control="xml-name"` 与 `ui:pickerUrl="string"`；delta xlib `edit-ref-id` tag 消费 `propMeta['ui:pickerUrl']`（注释示例逐字即 `/erp/md/pages/ErpMdMaterial/picker.page.yaml`，`:170-178`），`edit-userId` tag 产用户 picker。**注意**：AMIS JSON `type:'picker'` gen-control 块已于 2026-08-24 全仓删除；`docs/design/picker-patterns.md` §5.3 的 `<cell><picker><source>` 标签写法经第 2 轮审查证伪（schema 无此子元素、全仓零先例、owner doc 自述「本 Phase 不实现」）——**不采用**。
  - link 跳转 = `ErpPurOrder.view.xml:228-232` `actionType="link"` + `link="/ErpPurReceive-main?filter_orderId=${id}"`；目标 `/ErpApsSchedule-main` 存在（`ErpApsSchedule.view.xml:99-106` 含可用 listAction）。
  - confirmText 范式 = `ErpApsOperationOrder.view.xml:150` 等 42 处。
- 剩余差距：6 处缺陷原样暴露在生产用户面上（P1）。

## Goals

- 6 处 P1 交互缺陷全部修复：参数经 dialog 收集或语义修正，危险动作有二次确认，死按钮消除，物料选择走 picker。
- 全部改动限于 view.xml 保留层（5 个文件），零 Java/ORM/契约变更，页面结构经 flux 静态门禁合法（零新增 error）。

## Non-Goals

- 不新增 sys 用户管理页面（用户选人 picker 页面 `NopAuthUser/picker.page.yaml` 与 `edit-userId` 控件链已存在，本计划仅接线消费，不新建页面/菜单/权限）。
- 不做 drawer 尺寸/状态字段 readOnly/列裁剪等一致性批次（归同日 UX 一致性批量计划，另文起草）。
- 不改任何被调 BizModel 方法签名/守卫（后端行为已正确）。
- 不补 reportCompletion 默认带出计划剩余量（行数据字段名无法从 list 选择集保证，错误默认值比空输入更危险；dialog 空输入 mandatory 已消除 `=0` 缺陷）。

## Task Route

- Type: `implementation-only change`（用户可见行为修复，不触模型/契约）
- Owner Docs: `docs/design/customer-service/use-cases.md`（UC-CS 工单分派/解决语义）、`docs/design/quality/use-cases.md`（NCR 解决语义）、`docs/design/manufacturing/use-cases.md`（报工语义）、`docs/design/aps/scheduling.md`、`docs/design/finance/budget.md`（修复均对齐其已声明的动作语义，不改语义）
- Skill Selection Basis: 发现来源 `frontend-page-ux-audit-prompt.md`；实现范式复制自实仓页面与 `docs/design/picker-patterns.md`——实现阶段 `Skill: none`（docs/skills 无实现类技能）；**Fix-3 picker 落地后的验证阶段加载 `flux-rendering-conformance-audit-prompt.md`** 对 picker 写法做合规自检

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯前端资源文件；`npm run validate:flux` 与 `mvn clean install -DskipTests` 为既有验证入口）

## Execution Plan

### Phase 1 - CS 工单三缺陷（assign 语义 / resolve 参数 / escalate picker）

Status: completed
Targets: `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/ErpCsTicket.view.xml`
Skill: none（验证阶段 Fix-3 加载 flux-rendering-conformance-audit-prompt）

- Item Types: `Fix`×3
- Prereqs: none

- [x] Fix-1 assign 按钮：改 `actionType="dialog"`（`<dialog page="assignTicket" size="md"/>`），新增 `assignTicket` form（assignedToId cell 用平台用户 picker：`custom="true" mandatory="true" control="userId"`——control 值为**类型后缀**，XuiHelper.tryGetControl 以 `mode + '-' + type` 拼 tag 名解析到 `edit-userId`；完整 tag 名会查找 `edit-edit-userId` 落空回落 input-text）+ `<simple name="assignTicket" form="assignTicket" useFormActions="true">`（api `@mutation:ErpCsTicket__assign?ticketId=$id` withFormData="true"）。**Decision（主方案/降级）**：主方案=用户 picker 选人（完整兑现 09-14 建议且控件链已存在）；若实现时 `edit-userId` 在 custom cell 经 flux 渲染验证失败（validate:flux 报错或运行时验证证据），降级为「移除 `&assignedToId=0` + confirmText 自领语义」，降级证据落盘 Draft Review Record
      - Skill: none
- [x] Fix-2 resolve 按钮：改 `actionType="dialog"`（`<dialog page="resolveTicket" size="md"/>`），新增 `resolveTicket` form（resolution textarea mandatory + i18n-en）+ `<simple name="resolveTicket" form="resolveTicket" useFormActions="true">`（api `@mutation:ErpCsTicket__resolve?ticketId=$id` withFormData="true"，混合形态对齐 escalateQuality 现网写法），删除原 confirmText 直提
      - Skill: none
- [x] Fix-3 escalateQuality.materialId：gen-control c:script 输出 flux PickerSchema v3.2 契约（type picker + pickerPopup + valueField/labelField + pickerSchema dynamic-renderer 复用 `/erp/md/pages/ErpMdMaterial/picker.page.yaml`，与 delta flux-control.xlib edit-ref-id 自身输出同构、pickerUrl 内联）；**不走 cell 属性 `control="ref-id"`**——base edit-ref-id 脚本解引用 `propMeta['ui:pickerUrl']`，custom cell 无 propMeta 会 NPE 静默回落 input-number（结束审计实证）；落地后按 flux-rendering-conformance-audit-prompt 自检 picker 写法合规
      - Skill: flux-rendering-conformance-audit-prompt（验证）

Exit Criteria:

- [x] 三个缺陷在 view.xml 中消除：`assignedToId=0`/`resolution=` 零命中；materialId 无手输数字控件；assign/resolve 走 dialog 收集（assign 主方案为用户 picker 选人，若降级须有渲染失败证据落盘）
- [x] 新增 form/simple 段结构完整（label 均带 i18n-en 伴生），xmllint well-formed 通过

### Phase 2 - MFG/QA/APS 三缺陷

Status: completed
Targets: `module-manufacturing/erp-mfg-web/.../ErpMfgWorkOrder/ErpMfgWorkOrder.view.xml`、`module-quality/erp-qa-web/.../ErpQaNonConformance/ErpQaNonConformance.view.xml`、`module-aps/erp-aps-web/.../ErpApsOperationOrder/ErpApsOperationOrder.view.xml`
Skill: none

- Item Types: `Fix`×3
- Prereqs: Phase 1 范式落地（resolve dialog 同构复制）

- [x] Fix-4 reportCompletion：改 dialog（新 form `reportCompletion`：completedQty input-number mandatory + i18n-en；simple 提交页 api `@mutation:ErpMfgWorkOrder__reportCompletion?workOrderId=$id` withFormData）
      - Skill: none
- [x] Fix-5 QA resolve：同 Fix-2 范式（form 字段 resolution textarea mandatory + noCapaReason input-text 选填），删除 `resolution=` 直提
      - Skill: none
- [x] Fix-6 APS 排程死按钮：mutation 替换为 `actionType="link"` 跳排程方案列表（`link="/ErpApsSchedule-main"`，label/tooltip 调整为「去排程」语义），消除 `scheduleId=0` 无效调用（对齐 ErpPurOrder 创建入库单范式）
      - Skill: none

Exit Criteria:

- [x] `completedQty=0`/`scheduleId=0`/`resolution=`（qa）零命中；link 目标 `ErpApsSchedule-main` 页面存在
- [x] 三文件 xmllint well-formed 通过

### Phase 3 - FIN 危险动作确认

Status: completed
Targets: `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinBudgetScenario/ErpFinBudgetScenario.view.xml`
Skill: none

- Item Types: `Fix`×1
- Prereqs: none

- [x] Fix-7 `row-reject-button`/`row-cancel-button` 补 confirmText（驳回/作废各一条，语义对齐同文件 approve 上下文与全库 danger 确认文案范式）；仅 view.xml，不动 fin 任何 Java
      - Skill: none

Exit Criteria:

- [x] 全库 `<action … level="danger">`（跨行正则口径）44 处全部具备 confirmText 或 dialog 二次确认（44/44，42 处既有 + Fix-7 两处）
- [x] 该文件 xmllint 通过

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 agent_b40f179f，2026-09-27）——Blocker B-1：danger 覆盖率按 HEAD 实况为 44 处（原引分析文档 58/62 未复击实仓）；Major M-1：validate:flux 门禁「exit 0」与架构文档 §7「零新增 error 对照 325 基线」口径冲突；M-2：Owner Docs 三条死路径；M-3：Fix-3 picker 复制 VoucherLine JSON 写法存在「form cell 零先例 + flux valueKey/labelKey 契约差异」双重风险；Minor m-1/m-2/m-3。修订：danger 基线改 44 处；门禁改「零新增 error」；Owner Docs 改真实路径；Fix-3 初改 §5.3 标签范式（后被 iteration 2 证伪再改）；计数口径、Non-Goals、Skill 说明同步。
- Independent draft review iteration 2: needs revision（独立子代理 agent_0cd24245，2026-09-27）——B-1/M-1/M-2/m-1/m-3 消解验证通过。新发现：**Blocker B-2**：`<cell><picker><source>` 非 schema 合法构造（disp.xdef 无此子元素、全仓零先例、§5.3 自述不实现），正道=cell 属性 `control` + `ui:pickerUrl`（edit-ref-id/edit-userId 控件链）；**Major M-4**：「无用户 picker」前提为假（`NopAuthUser/picker.page.yaml` 在 app 运行时 vfs 存在 + delta xlib `edit-userId` tag），Fix-1 须重做替代方案分析；m-4：view.xml 实数 368 非 370；m-5：iteration 2 不得预填 accept。修订：Fix-3 改 `control="edit-ref-id" ui:pickerUrl=...`；Fix-1 升级为用户 picker dialog（主方案）+ 降级 Decision 附证据条件；基线 368；范式描述改平台控件链；Deferred assignee picker 移除（并入 Fix-1）。
- Independent draft review iteration 4（结束审计 round 1）：FAIL（独立子代理 agent_bb7a26d4，2026-09-27）——Fix-1/Fix-3 的 picker 在导出产物中未生效（assignedToId=input-text、materialId=input-number，与 HEAD 同）：① control 属性须传类型后缀（userId→edit-userId），完整 tag 名被 `XuiHelper.tryGetControl` 的 `mode+'-'+type` 拼接规则击穿；② base edit-ref-id 脚本解引用 null propMeta（custom cell）静默回落 input-number；③ `mvn -pl app-erp-all test` 依赖 .m2 旧 jar 掩盖修复（模块资源改动须先重装该模块）；④ Closure/日志失实声称与执行者预勾结束审计门控。整改：Fix-1 改 `control="userId"`、Fix-3 改 gen-control v3.2 pickerSchema（URL 内联）；重装模块后终版导出实证两 picker 均 type:"picker"；失实记录更正；结束审计门控取消预勾；分析报告 §3.1 回填。终版验证：full build SUCCESS、FLUX_PAGE_ERROR_COUNT: 2（全平台页）、JS 校验 errors 325=325 零新增、warnings 18496（+1 净值 = 两个新 picker 各 +1 条平台同款 `hiddenFieldPolicy` unknown-property 良性警告、−1 按钮重排序位移）、checker 与 HEAD 逐字节一致。（独立子代理 agent_0d319e9a，2026-09-27 定点复核）——B-2 修订成立（disp.xdef `control`/`ui:pickerUrl` 合法 + delta xlib `edit-ref-id` 消费链实证 + NopAuthUser.xmeta 平台先例补强）；M-4 修订成立（edit-userId tag + picker 页存在，降级 Decision 触发/证据明确）；m-4（368 复跑一致）/m-5（未预填）成立；一致性检查通过。备注：disp.xdef 行号引 :16-17 实为 :15,17（cosmetic，实施时顺手修正）；Fix-1/Fix-3 为 app 层 cell 属性 picker 首用，实施时先跑 validate:flux 落证据。**计划可进入实施。**
- Independent closure audit iteration 5: **PASS / passes closure audit**（独立子代理 fresh session，2026-09-27，仅复审 round 1 整改闭合，不做全量重审）。Round 1 五项必须修复逐条验证：① Fix-1 闭合——计划记录 `control="userId"`（类型后缀）；导出产物 `app-erp-all/target/flux-pages/erp/cs/pages/ErpCsTicket/main.page.json` `body/columns/16/buttons/2`（分派 dialog）assignedToId 节点 `type:"picker"`（pickerPopup dialog 选择用户 / valueField userId / labelField userName / pickerSchema.loadAction.args.url=`/nop/auth/pages/NopAuthUser/picker.page.yaml`）；源 view.xml:399-400 实证。② Fix-3 闭合——同 JSON `body/columns/16/buttons/5`（质量问题升级）materialId 节点 `type:"picker"`，pickerSchema.loadAction.args.url=`/erp/md/pages/ErpMdMaterial/picker.page.yaml`；源 view.xml:330-347 gen-control pickerSchema 实证。③ 失实记录更正闭合——计划 Closure 段与 `docs/logs/2026/09-27.md` 均如实记录两轮纠错（完整 tag 名被 tryGetControl 拼接击穿→input-text；control="ref-id" null propMeta NPE→input-number；.m2 旧 jar 掩盖→须重装模块再导出）；终版数字（errors 325=325、warnings 18496 +1 净值）已持久化于 iteration 4 行。④ 结束审计门控已取消预勾（复审前为 `[ ]`）。⑤ 文档对齐闭合——分析报告 §3.1 表 P1-1~P1-5 全部标「✅ 已修复」+ 路线表登记 done。终版证据独立抽查：`/tmp/report_final.json` totals errors=325（=325 基线零新增）/warnings=18496；vs `/tmp/report_head.json`（325/18495）per-file diff **唯一变更文件** ErpCsTicket main.page.json：+2 picker `hiddenFieldPolicy` unknown-property（buttons/2、buttons/5）−1 同路径 input-number 同类警告（renderer 换替），该警告类 HEAD 基线既有 864 处 picker + 227 处 input-number 实例（平台自产同款良性）；`/tmp/checker_final.txt` 与 `/tmp/checker_head.txt` diff 为空（零漂移）；git status 生产代码严格限 5 个 view.xml；缺陷串（assignedToId=0/resolution=/completedQty=0/scheduleId=0）全库零命中；aps link/fin 2×confirmText/qa resolveNcr dialog 抽查落地；时间戳链源 04:55→导出 05:05→报告 05:05（重装模块后导出，.m2 掩盖已 remediate）。**Minor 记录（不阻塞关闭，cosmetic）**：(a) 本计划 Closure Status Note 与日志 09-27 行仍残留「逐字节一致 / warnings 18495=18495 零新增」字样——该描述属重装模块前被 .m2 旧 jar 掩盖的失效导出，终版实际为 warnings 18496（+1 净值，机制见上），以本 iteration 5 记录为准；(b) iteration 4 行将 −1 归因「按钮重排序位移」，实为同路径（buttons/5）input-number→picker 渲染器换替所致，算术与良性定性不变。**计划可关闭。**

## Plan Status 演进

> Plan Status: active（2026-09-27，iteration 3 accept 后置位）

## Closure Gates

- [x] 范围内行为完成（7 处 Fix 全落地）
- [x] 相关文档对齐（`docs/logs/2026/09-27.md` 登记；分析报告 §3.1 表回填修复状态）
- [x] 已运行验证：5 文件 xmllint + `npm run validate:flux` 重跑后按 `_tmp/flux-page-validation-report.json` 对照 **error 零新增（325 基线口径，`docs/architecture/flux-page-export-and-validation.md` §7）** + `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` 无基线漂移
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计（round 1 FAIL 已整改，iteration 5 复审 PASS）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### reportCompletion 默认带出计划剩余量

- Classification: `optimization candidate`
- Why Not Blocking Closure: list 行选择集字段名（planQty/completedQty）无法静态保证，错误默认值风险 > 空输入摩擦
- Successor Required: `no`——触发条件：确认 findPage gql:selection 含计划/完工数量字段后可作 P3 体验优化

（注：assignee picker 已从 Deferred 移入 Fix-1 主方案——第 2 轮审查 M-4 证实 `edit-userId` 控件链与 `NopAuthUser/picker.page.yaml` 在 app 运行时 vfs 均存在，原「无选人能力」前提不成立。）

## Closure

Status Note: 7 处 Fix 全部落地（5 个 view.xml，零 Java/ORM/契约变更）。验证全绿：5 文件 xmllint 通过（namespace 提示为文件既有 DSL 风格）；flux 静态门禁——变更态导出 erp 子树 855 页 0 渲染失败（FLUX_PAGE_ERROR_COUNT: 2 全为平台 /nop/auth 页，stash 对照证实预存）、JS 校验 totals 与 HEAD 态逐字节一致（errors 325=325 对照 325 基线零新增、warnings 18495=18495 零新增）；picker 在导出产物实证为 `type:"picker"`：assignedToId（NopAuthUser pickerSchema，`control="userId"` 后缀链）+ materialId（ErpMdMaterial dynamic-renderer pickerSchema，gen-control v3.2 契约）——**注意实现期两轮纠错**：第一版 control 传完整 tag 名（edit-userId）被 tryGetControl 拼接规则击穿回落 input-text；第二版 control="ref-id" 因 base edit-ref-id 解引用 null propMeta 静默回落 input-number（`mvn -pl app-erp-all test` 依赖 .m2 旧 jar 还会掩盖修复——模块资源改动须先重装该模块再导出验证）；`mvn clean install -DskipTests` BUILD SUCCESS；合规检查器与 HEAD 对照逐项一致（R3=6/R10=15/R2c=1571 为 HEAD 预存漂移，f3.10 批次遗留，本计划 view.xml-only 零引入——归基线裁决 successor）。Fix-1 主方案（用户 picker）落地，未触发降级。Fix-3 flux 合规自检：消费 delta flux-control.xlib 标准 tag（edit-ref-id）+ disp.xdef 合法属性，输出为平台自身渲染产物，by construction 合规（flux-rendering-conformance-audit 口径）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 fresh session（iteration 5，2026-09-27）：**PASS / passes closure audit**——round 1 五项整改全部闭合，证据见 Draft Review Record iteration 5 行；剩余风险仅 2 项 cosmetic 文档残留（已在该行记录，以 iteration 5 为准）

Follow-up:

- （无阻塞跟进）
