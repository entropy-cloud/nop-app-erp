# ck-common-app — 共享模块与聚合工程（module-common-service / module-common-test / app-erp-all / app-erp-test-data）实现代码检查报告

> 工作项：C8.2。执行日期：2026-08-26。执行者：域检查子代理（ZCode 会话，read-only 检查阶段）。
> 范围：`module-common-service` 全部 21 个主文件（`AbstractProcessor` + 6 个 per-mutation 编排骨架（Approve/Reject/SubmitForApproval/WithdrawApproval/ReverseApprove/Cancel）、`ErpCommonErrors`、`SoDGuard`、`MaskHelper`/`StringMaskFormat`/`MaskAuditRecorder`、`UniqueConstraintHelper`、`DashboardUtil`、org 隔离三件套（`ErpOrgContext`/`ErpOrgIsolationQueryTransformer`/`ErpOrgIsolationOrmInterceptor` + Constants）、`ErpRoleDataAuthChecker` + Constants、`CommonServiceModule` marker、`_vfs/erp/common/beans/app-service.beans.xml` 4 bean + `dict/erp/doc-status.dict.yaml`）；`module-common-test` 全部 5 个主文件（冻结钟 2 + FaultInjectionStubs + PerfTiming + marker）+ `_cases` 4 autotest.yaml；`app-erp-all` 全部 6 个主 Java 文件（`ErpApplication` + meta 五件套）+ `_vfs` 全资源（`app.action-auth.xml` 聚合 / `app.data-auth.xml` / `_delta/default` 3 文件 / 33 job.yaml + scheduler.yaml / `erp/xlib/control.xlib` / application.yaml + bootstrap.yaml / `nop-vfs-index.txt`）；`app-erp-test-data`（0 Java，纯骨架：load-order.txt + tables/README.md）。跨文件核实：96 个 `_init-data` CSV × 19 域 orm.xml 列集全量比对（脚本）+ FK/orgId/序号地板三向一致性、平台源码实证 6 处（`CrudBizModel` L179/L411-412 `nopGlobalQueryTransformer` 注入与调用点、`ErrorMessageManager.resolveDescription` 缺参渲染字面量、`orm-defaults.beans.xml` L49 `IOrmInterceptor` collect-beans、`CrudBizModel.invokeDefaultPrepareUpdate` 经 `getThisObj().invoke` 允许 xbiz 覆盖、`IOrmInterceptor` 平台钩子签名、`IQueryTransformer` 接口形状）+ git 提交考古 3 处（l10n 菜单引入 069428710 / roles 补全 4ebfd30c0 / 骨架 §16.4 修复 4dab22be3）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.2/B4）。21+5+6 主文件全量通读（无抽样）；骨架子类面（~120 个 `extends Abstract*Processor`）做调用面 grep + 2 处深读抽样（purchase Order 链）；R12a/b/c 共享内核 import 基线按 checker 同口径复测；arm-index 与 27 份已产出 ck-* 报告对候选 finding 逐条 grep 裁决复用/新增。
> 切片边界：骨架子类的业务正确性归各域报告（本报告只对骨架本体与共享面负责）；`erp-*-web` AMIS 页面契约 drift 不深查；`_gen/` 产物不查；测试代码仅用于行为语义交叉验证。

## common 骨架是否提供 CRUD 守卫统一修复挂点（修复阶段输入，任务点名必答）

**明确回答：没有。common 今天不提供任何 CRUD 写路径（`__save`/`update_`）的统一守卫挂点。**

- **证据 A（无 BizModel 基类）**：`module-common-service` 21 文件清单里**没有任何 `extends CrudBizModel` 的类**；`module-common-service` 与 `app-erp-all` 全目录 grep `defaultPrepareUpdate|defaultPrepareSave` 命中 **0**。共享面只有 per-mutation Processor 骨架（`AbstractApproveProcessor.approve` L22-35 等六条命名动作链），它们只治理命名动作路径，**不拦截通用 CRUD 保存路径**——正是 `P1-CK-pur-003` 族（CRUD update 无守卫→状态字段可直改）的旁路面本身。
- **证据 B（守卫现状碎片化）**：全域 34 个文件各自 override `defaultPrepareUpdate`（grep 计数），无共享实现；其余域（b2b/cs/drp 等，各域报告已登记同型站点）零 override。
- **修复阶段的两个可行统一挂点（供 F0.2 方法基线裁决）**：
  1. **BizModel 层新基类**：在 common 新增 `AbstractErpCrudBizModel extends CrudBizModel`，override `defaultPrepareUpdate`/`defaultPrepareSave` 做共享状态字段守卫（平台语义已实证：`CrudBizModel.java` L981 `protected void defaultPrepareUpdate`，且 L1961/1972 `invokeDefaultPrepareSave/Update` 经 `getThisObj().invoke` 调用——意味着**也可不换基类、逐实体在 xbiz XML 里声明 `defaultPrepareUpdate` 覆盖**，无 Java 改动的第二杠杆）。代价：~100+ 域 BizModel 换基类（每处一行）或逐实体 xbiz 声明；属跨域共享行为变更，修复应走 plan-first。
  2. **ORM 层拦截器**：`IOrmInterceptor` collect-beans 钩子（平台 `orm-defaults.beans.xml` L49 实证；本项目 `ErpOrgIsolationOrmInterceptor` 已在用该机制）可在 `preUpdate` 比较状态字段旧/新值拦截。局限：ORM 层无 `IServiceContext`/动作名，无法区分「命名 mutation 合法写」与「CRUD 直写」，需要白名单注册表或接受误伤——**不推荐作为唯一机制**。
- **结论**：P1-CK-pur-003 族**不可在现有 common 代码一处修复**。修复阶段要么按 34 站点既有碎片模式逐域补齐（现状路径），要么先建上述挂点再批量迁移（推荐先裁决挂点，避免 19 域重复实现同一守卫）。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P2-CK-common-001（D2，跨 D10）MaskAuditRecorder ThreadLocal 去重集无请求级清理——线程池复用线程上跨请求审计抑制，保密字段披露事件静默漏记

- **控制点 A**：`app/erp/common/service/MaskAuditRecorder.java` L54（`private final ThreadLocal<LinkedHashSet<DedupKey>> tlDedup = ThreadLocal.withInitial(LinkedHashSet::new);`）+ L126-133（`recordDisclosure` 内 `DedupKey key = new DedupKey(ctx.getUserId(), entityName, objId, fieldName); LinkedHashSet<DedupKey> seen = tlDedup.get(); ... if (!seen.add(key)) { return; }`）
- **控制点 B**：清理入口 `clearThreadDedup()`（L187-189）**生产代码零调用**（grep 实证：全仓仅定义处 + 同包测试 `threadDedupSize` 辅助；无 filter/interceptor/请求生命周期钩子调用它）。javadoc L32 自述设计意图为「**同一请求线程内**仅记一次」。
- **证据**：Quarkus RESTEasy worker 线程（及 GraphQL 执行线程）跨请求复用是部署常态；dedup 集随线程存活 → 同一 `(userId, entityName, objId, fieldName)` 四元组在**后续任意请求**中的再次披露命中残留 key 直接 return，**不写审计记录**。触发条件：运维开启 `erp.audit.field-read.enabled=true`（该 config 即为此功能的生产开关，`%test`=ON / `%dev`/`%prod`=OFF——默认关闭限制当前暴露面，但功能启用即失真）。次要面：L128-130 `seen.size() >= 500 → seen.clear()` 全清策略在超限后放行全部旧 key（审计洪泛反向问题，javadoc 已自认防泄漏取舍）。
- **问题**：D2 审计闭环——按请求去重的语义实际实现为按线程生命周期去重；保密字段读访问审计（E4.2 合规面）在真实部署下系统性漏记且无任何告警。
- **建议修复方向**：在请求出口清理（注册平台 `IWebServerFilter`/request-end 钩子，或改用 `IServiceContext` attribute 承载 per-request dedup——上下文天然请求级）；或 dedup key 加入请求 id。
- **arm-index 裁决**：新增（grep「MaskAudit/ThreadLocal dedup/field-read」arm-index 与 27 份 ck-* 报告零命中）。

### P3-CK-common-002（D5）聚合菜单含「中国本地化」死菜单——3 个 FLUX 页面 URL 指向不存在的模块页面，admin 角色可见可点

- **控制点**：`app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml` L38-65（`<resource id="erp-l10n-cn" displayName="中国本地化" ... roles="admin">` 下 3 个 FLUX 子页 `url="/erp/l10n-cn/pages/ErpL10nCnVatInvoice/main.page.yaml"` / `ErpL10nCnGoldenTaxLog` / `ErpL10nCnTaxRate`）
- **证据**：全仓 grep `ErpL10nCn|l10n-cn`（*.page.yaml/*.view.xml/*.java）——**页面文件零存在**，`module-l10n-cn` 工程不存在（引入提交 069428710 自述「设计阶段，独立 module-l10n-cn 工程」；4ebfd30c0 又为该 TOPM 补了 `roles="admin"` 使其对平台 admin 可见）。菜单 XML 内注释虽标「设计阶段」，但菜单条目本身是激活种子（`nop.auth.site-map.static-config-path` 指向本文件）：admin 登录后看到「中国本地化」顶级菜单，点击 3 个页面均落不存在页面。`nop-vfs-index.txt` 亦无 l10n 条目（资源确实不在 classpath）。
- **问题**：D5 聚合菜单完整性——设计占位菜单以激活状态进入运行时 UI（用户可见坏入口）。
- **建议修复方向**：注释掉整棵 `erp-l10n-cn` 资源块（或加独立 feature gate），待 module-l10n-cn 落地时恢复；同步在 `docs/design/l10n/cn-golden-tax.md` 注记菜单状态。
- **arm-index 裁决**：新增（arm-index l10n 命中仅 P2-MA3-043 docs 索引维度，ck-* 各报告零命中）。

### P3-CK-common-003（D5）action-auth 聚合头注释声称合并 job 菜单但未聚合——nop-job 管理 UI 在 app 中整体缺席，注释与 x:extends 清单失同步

- **控制点**：`app.action-auth.xml` L2（注释「合并全部 18 业务域 + 跨域通知派发子系统(notify) + 系统模块(**nop-auth/sys/wf/report/job**)的菜单」）对照 L5-27 x:extends 清单（`/nop/auth/...`、`/nop/sys/...`、`/nop/wf/...`、`/nop/report/...` 共 4 个系统模块——**无 `/nop/job/auth/nop-job.action-auth.xml`**）
- **证据**：`app-erp-all/pom.xml` 只有 `nop-job-local`（service 层，方案 A 接入提交 069428710 自述根因：nop-job-service 强依赖 RPC），**无 `nop-job-web` 依赖**——job 管理页面不在 classpath，扩展其 action-auth 反而会资源缺失。后果：(a) 33 个 job.yaml 调度无任何管理/监控 UI（只能 DB/API 直操作）；(b) 注释漂移误导后来者认为 job 菜单已聚合。
- **问题**：D5——注释声明与聚合实现失同步 + job 运维面缺口（低危，job 全部 enabled=false 默认关）。
- **建议修复方向**：修正注释去掉 "job"；若需 job 管理 UI，引入 `nop-job-web` 依赖 + extends 其 action-auth（功能增强，修复阶段可裁决 not-a-problem + 注释修正）。
- **arm-index 裁决**：新增（arm-index「nop-job 菜单」零命中；ck-* 报告的 job 类 finding 均为域内键漂移，非聚合层）。

### P3-CK-common-004（D10）defaultNotFoundException 只绑 bizObjId 不绑 bizObjName——67 站点错误消息渲染字面量 `{bizObjName}` 占位符

- **控制点**：`app/erp/common/service/AbstractProcessor.java#defaultNotFoundException` L87-90（`new NopException(ErpCommonErrors.ERR_ENTITY_NOT_FOUND).param(ErpCommonErrors.ARG_BIZ_OBJ_ID, id);`——模板 `"实体不存在：{bizObjName}#{bizObjId}"` 的 `ARG_BIZ_OBJ_NAME`（ErpCommonErrors L12）从未赋值）
- **证据**：平台源码实证 `ErrorMessageManager.resolveDescription`（nop-entropy `nop-kernel/nop-core/.../ErrorMessageManager.java` L144-156）：`if (!params.containsKey(name)) return "{" + name + "}"`——缺参渲染为**字面量花括号**。消费面 grep：`defaultNotFoundException(id)` 共 **67 处**（assets 30 / finance 20 / inventory 7 / quality 5 / projects 4 / crm 1），这些域的「实体不存在」报错全部呈现为 `实体不存在：{bizObjName}#123`——实体类型信息丢失 + 模板泄漏到客户端。
- **问题**：D10 错误信息质量（无数据损坏；排错面退化）。
- **建议修复方向**：`defaultNotFoundException` 增 `bizObjName` 参数（子类可从 `bizObjName` 字段/实体类名传入），或模板去掉 `{bizObjName}` 段。
- **arm-index 裁决**：新增（arm-index「bizObjName/ERR_ENTITY_NOT_FOUND」零命中）。

### P3-CK-common-005（D6）DashboardUtil.buildGroupByQuery 生产零调用 + `additionalFilters` 参数被静默忽略——死代码携带无过滤全表聚合陷阱

- **控制点**：`app/erp/common/service/DashboardUtil.java#buildGroupByQuery` L50-70（方法体只构造 group/agg 两个字段并 `return query`——形参 `List<QueryBean> additionalFilters` **从未被读取**；grep 实证全仓零调用，仅定义处）
- **证据**：若未来某看板按签名传入过滤条件（如 orgId/日期窗），查询将**无任何 filter 直接全表 GROUP BY**——参数被吞而非报错。当前 9 个消费文件只用 `nz`/`toBigDecimal`/`monthKey`/`safeDivide`，`buildGroupByQuery` 是 plan 2026-07-24-2200-1 Phase 4 遗留未接线产物。
- **问题**：D6 共享工具死代码 + 潜在静默数据面放大（当前无行为影响）。
- **建议修复方向**：删除该方法（或实现 additionalFilters 合并并补测试）。
- **arm-index 裁决**：新增（arm-index「buildGroupByQuery/DashboardUtil」零命中）。

### P3-CK-common-006（D1）DashboardUtil.safeDivide 使用废弃 `BigDecimal.ROUND_HALF_UP` 整型常量

- **控制点**：`DashboardUtil.java#safeDivide` L47（`return numerator.divide(denominator, 4, BigDecimal.ROUND_HALF_UP);`）
- **证据**：Java 9 起废弃（应 `RoundingMode.HALF_UP`）；功能正确（三参重载带 scale=4，**不构成** ck-inventory 登记的 `divide(divisor, int)` 无 scale ArithmeticException 家族——此处无异常风险，纯风格/可维护性）。同文件 `toBigDecimal` L27-32 对非数值字符串 `new BigDecimal(v.toString())` 抛未包装 NFE（Map 值异常时看板 500），一并注记。
- **问题**：D1 平台/语言反模式（无行为缺陷）。
- **建议修复方向**：改 `RoundingMode.HALF_UP`；`toBigDecimal` 包 try/catch 归零或抛 NopException。
- **arm-index 裁决**：新增（arm-index「ROUND_HALF_UP」零命中；ck-inventory 的同常量 finding 是无 scale 重载的正确性缺陷，形态不同）。

### P3-CK-common-007（D10）StringMaskFormat 短值边界近全泄漏——len=6 证件号揭示 5/6 字符、len=8 手机号揭示 7/8 字符

- **控制点**：`app/erp/common/service/StringMaskFormat.java#ID_CARD` L16-19（`if (value.length() <= 5) return FULL.mask(value); return value.charAt(0) + "******" + value.substring(value.length() - 4);`）+ `#MOBILE` L28-32（`<= 7` 阈值 + 首3/末4）
- **证据**：ID_CARD 输入长度 6 时输出 = char(0) + `******` + chars(2..5)——**6 字符中 5 个明文**（仅 char(1) 被遮）；MOBILE 长度 8 时 7/8 明文。标准长度（证件 18 / 手机 11）不受影响；异常短值（测试数据、历史脏数据、非 CN 格式）近乎全泄漏，与枚举 javadoc「避免短值泄漏」意图相悖（阈值只保证首尾段不重叠，未保证遮蔽占比）。
- **问题**：D10 边界——脱敏格式对边界长度输入失效（低危：需字段值本身异常短）。
- **建议修复方向**：阈值改为「确保遮蔽中段 ≥ 明文段」（如 ID_CARD `len < 9` 全打码、MOBILE `len < 11` 全打码），或按比例遮蔽。
- **arm-index 裁决**：新增（arm-index「mask 短值/边界泄漏」零命中）。

### P3-CK-common-008（D3）9 个文件残留「共享骨架 reverseApprove→SUBMITTED 为已确认 live 缺陷」陈旧 javadoc——骨架已于 2026-08-14 修复为 REJECTED，注释反向误导修复阶段

- **控制点 A**：`module-purchase/.../statemachine/ErpPurOrderApprovalStateMachine.java` L29 与 `module-sales/.../statemachine/ErpSalOrderApprovalStateMachine.java` L29（「共享骨架 `AbstractReverseApproveProcessor.doReverseApprove`→SUBMITTED 为已确认 live 缺陷……移交显式 successor」）+ `ErpPurOrderReverseApproveProcessor.java` L21 / `ErpSalOrderReverseApproveProcessor.java` L21（「骨架→SUBMITTED 对 Order 为经覆写绕过的死路径」）——同型文本另见 pur `ErpPurReceiveApprovalStateMachine`/`ErpPurRequisitionApprovalStateMachine`/`ErpPurRequisitionReverseApproveProcessor`、sal `ErpSalDeliveryApprovalStateMachine`/`ErpSalQuotationReverseApproveProcessor`，共 **9 文件**
- **控制点 B（事实）**：`module-common-service/.../AbstractReverseApproveProcessor.java` L38-42/L64-66——`doReverseApprove` 写 `rejectedStatus()`，默认 `return "REJECTED"`，**已合规 `domain-design-guidelines.md §16.4`**；grep 实证无任何子类 override `rejectedStatus()` 返回 SUBMITTED。git 实证：commit `4dab22be3`（2026-08-14，「反审核共享骨架 §16.4 合规化修复」）已落地，但 9 处 javadoc 未随修更新。
- **证据**：影响面在修复阶段：按注释检索会得出「骨架仍有 live 缺陷 + Order/Requisition 靠覆写绕过」的错误结论（实际骨架与覆写现已同为 REJECTED，覆写变为冗余但无害）；审计/复审者会被误导。
- **问题**：D3 状态机契约的代码内文档漂移（代码本身正确——见「验证为正确」节）。
- **建议修复方向**：9 处 javadoc 更新为「骨架已合规 §16.4（commit 4dab22be3）」；可顺带裁决是否回收各子类冗余覆写。
- **arm-index 裁决**：新增（ck-purchase/ck-sales 均未登记此注释漂移；arm-index「reverseApprove SUBMITTED javadoc」零命中）。

### P3-CK-common-009（D10）UniqueConstraintHelper 把 ERR_SQL_DATA_INTEGRITY_VIOLATION 一并判为唯一约束冲突——非 UK 完整性违例可被误译为「重复记录」友好错误

- **控制点**：`app/erp/common/service/UniqueConstraintHelper.java#isUniqueConstraintViolation` L22-35（cause 链上任一 `JdbcException` 的 errorCode ∈ {`ERR_SQL_DUPLICATE_KEY`, **`ERR_SQL_DATA_INTEGRITY_VIOLATION`**} 即返回 true）
- **证据**：`ERR_SQL_DATA_INTEGRITY_VIOLATION` 是宽桶（NOT NULL 违例/FK 违例/长度溢出等多数驱动归此）——javadoc 自认「少数驱动归类至此」的取舍。调用方（R1.28 范式：并发首插 flush 后按此判定翻译为域「已存在」错误码）会把真实的数据完整性缺陷**粉饰为幂等重复**，掩盖根因（如某列 NOT NULL 缺值被报「单据已存在」）。
- **问题**：D10 错误定性（触发面窄：仅并发插入路径的 catch 翻译分支）。
- **建议修复方向**：收紧为仅 `ERR_SQL_DUPLICATE_KEY`，或在判定真分支二次核对 SQLState（23000 vs 23502/23503）区分 UK 与其他违例。
- **arm-index 裁决**：新增（arm-index 该 helper 关联的 R1.28/P1-MA2-088 finding 均为幂等语义维度，未覆盖误匹配面）。

### P3-CK-common-010（D1）ErpCommonErrors 错误码命名空间 `nop.err.erp.common.*` 偏离项目 `erp.err.<short>` 约定

- **控制点**：`ErpCommonErrors.java` L18/L24（`"nop.err.erp.common.entity-not-found"` / `"nop.err.erp.common.illegal-status-transition"`）对照域范式 `ErpPurErrors` L49（`"erp.err.pur.illegal-status-transition"`）与 `ErpModuleMetaErrors` L19（`"erp.err.module-meta.dependency-missing"`——app 层另一处遵守约定）
- **证据**：`docs/design/domain-design-guidelines.md §7.1` + skills README 命名约定节规定 ErrorCode 前缀 `erp.err.<short>`；`nop.err.*` 是平台自身命名空间——common 两个码侵入平台前缀段（i18n 键映射/监控按前缀分组时会与平台码混组）。无运行时行为影响。
- **问题**：D1 命名约定漂移。
- **建议修复方向**：改 `erp.err.common.*`（错误码字符串变更属对外契约面，修复阶段评估兼容性后统一裁决；若判定 not-a-problem 需在 guidelines 显式豁免 common 前缀）。
- **arm-index 裁决**：新增（arm-index 错误码相关 finding 均为「未扩展 NopException/中文描述」维度，无前缀漂移条目）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / 裁决 | 本检查对象受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-pur-003` 族（CRUD update 无守卫全域族） | common **无** BizModel 基类/统一挂点（见专节）；骨架只治命名动作路径 | **不可一处修复**——修复阶段先裁决挂点（新基类或 xbiz 声明式覆盖）再批量迁移 |
| cron 键漂移家族（P3-CK-crm-014/mfg-013/inv-021/sal-023/qa-019/hr-012/cs-014/mnt 等） | application.yaml **不含任何 cron 键**（全部经 job.yaml `@cfg:nop.job.<name>.cron-expr\|<default>` 默认值）；33 个 job.yaml 外层双键模式（enabled\|false + cron-expr\|默认）全域一致 | **yaml 侧无未登记站点**；域内层 `erp-<short>.*-cron` 内层门控键漂移归各域 finding |
| `enable-action-auth: false`（%dev/%prod）安全姿态 | application.yaml L53/L79-80 显式 false | **复用** P1-MA3-046（done R2.7，三层 OFF 显式 deliberate + 灰度 successor 待人工批准），不重复登记 |
| `nop.debug: true` / `site-map.support-debug: true` / JWT enc-key 硬编码 fallback（全 profile 生效的根级配置） | application.yaml L3/L12/L6 | **复用** `permissions-enforcement-roadmap.md` L259 prod-flip successor 检查表已登记项（含 2026-07-01 平台合规审计「bootstrap 期可接受，生产前外置化」裁决），不重复登记 |
| orgId 隔离族（写侧 writer 缺口：P2-CK-fin2-007/mfg-007/hr-003④/b2b-003） | common 读侧 `ErpOrgIsolationQueryTransformer` + 写侧 `ErpOrgIsolationOrmInterceptor` 本身正确（见验证节） | **修复时序注记**：各域单据 orgId=null 未回填前开启 `erp.multi-company.org-isolation-enabled` 会使读侧 `eq(orgId,currentOrgId)` 滤空全部历史行——orgId 写侧修复必须先于隔离开关灰度 |
| `ErpModuleMetaBizModel` 4 个 `@BizQuery` 无 `@Auth` 注解 | 归全域裸注解基线（P1-MA3-046 范畴；诊断类查询低敏） | 复用不展开 |
| 骨架 `AbstractCancelProcessor.validateTransitionForCancel` 仅拒「已作废」 | 单守卫宽松是骨架设计（域动态守卫归 `validateCanCancel` 钩子，各域已按需覆盖） | 设计内行为，非缺陷 |
| `StringMaskFormat` 家族先例 | b2b/hr/md 各报告的 masking finding 均为「角色覆盖面/字段遗漏」维度 | 与 007（格式边界）不同维度，无重叠 |

## 验证为正确（显式排除，防误报）

- **R12a/b/c 共享内核 import 基线精确复测无漂移（任务点名）**：按 checker 同口径（import 语句级、排除 test/所属域/`_gen`/target）实测 **70/66/41 == 基线 70/66/41**——`ErpFinBusinessType`/`PostingEvent`/`AcctSchemaResolver` 共享面无新增消费方、无新缺口（prompt 所指 P1-CK-fin-005 为 AcctSchemaResolver null 语义维度，已在 finance 报告登记，共享面未再见同型）。
- **共享骨架模板方法正确（任务点名 validateNotCancelled 骨架）**：六条骨架（Approve/Reject/SubmitForApproval/WithdrawApproval/ReverseApprove/Cancel）编排骨架均为 `requireEntity → validateNotCancelled → validateTransition → validateBusinessRules → beforeStateChange → doXxx → afterStateChange → dao().updateEntity()`，与 `processor-extension-pattern.md` 契约一致（事务钉 Facade、`IServiceContext` 末参透传、`protected` 步骤可 Delta 覆盖、异常构造委托子类保域错误码）；approve/reject 幂等短路（已审批/已驳回直接返回）在守卫之前，无副作用；`@Inject` 字段全部非 private；`now()` 走 `CoreMetrics.currentTimestamp()`。~120 个域子类消费该骨架（assets/pur/sal/fin/inv/mfg/prj/qa/crm/drp）。
- **`AbstractReverseApproveProcessor` §16.4 已合规**：`doReverseApprove` → `rejectedStatus()` 默认 `"REJECTED"` + 清空 approvedBy/approvedAt（commit 4dab22be3 修复在位）——9 处陈旧 javadoc（finding 008）不改变代码正确性。
- **SoDGuard fail-safe 语义正确**：config 默认 true（生产强 SoD）；null-user 放行是 wf 回调路径既有行为（javadoc 登记）；`erp-common.sod-enabled` 仅 %test 显式 false（单账号 E2E 范式），%prod 继承默认 true ✓。
- **MaskHelper fail-closed 语义正确（任务点名）**：无 `IUserContext` → `findAuthorizedRole` 返 null → 数值返 null/字符串打码；7 个角色字面（HR 专员/薪酬审批人/合同审批人/合同专员/采购员/管理员/财务员）与 `nop_auth_role.csv` 种子**逐一比对全部存在**；E4.2 审计重载仅 authorized-clear-text 分支委托记录。
- **状态机契约 ERR_ILLEGAL_STATUS_TRANSITION 抽样一致（任务点名，hr/md 之外再抽 2 处）**：`ErpPurOrderApprovalStateMachine` 与 `ErpDrpPlanStateMachine` 的 `illegal(action, currentStatus, expectedStatus)` 均正确绑定 common 码三参数（currentStatus/expectedStatus/action），Bean 严格无状态（零注入）；Processor 侧映射范式在位（`ErpPurOrderReverseApproveProcessor#validateTransitionForReverseApprove` catch Bean 异常 → 域码 `ERR_ORDER_ILLEGAL_STATUS_TRANSITION` + orderCode/current/expected 参数）——符合 `entity-state-machine-bean.md` §7「Bean 报告非法边、Processor 保留领域语义」契约。注：契约 §7 提议的 `action`/`fromStatus` 元数据以 `ARG_ACTION`（各 Bean 自声明同值常量）+ `ARG_CURRENT_STATUS` 形态落地，语义等价（`ARG_ACTION` 未上提 ErpCommonErrors 属次要重复，不单列 finding）。
- **org 隔离基础设施平台接线实证**：`ErpOrgIsolationQueryTransformer` 注册 `nopGlobalQueryTransformer`（平台 `CrudBizModel.java` L179 `@Named("nopGlobalQueryTransformer")` 注入 + L411-412 在查询准备末端调用 `transform`——实证钩子存在且本类同 id 非 default 覆盖平台 `EmptyQueryTransformer`）；`ErpOrgIsolationOrmInterceptor` 经平台 `orm-defaults.beans.xml` L49 `<ioc:collect-beans by-type="io.nop.orm.IOrmInterceptor"/>` 自动收集（钩子实证）；二者 config-gated 默认 off、`ErpOrgContext` 非法/空白 orgId 静默 null（宽容归一 javadoc 登记）；`ErpOrgIsolationQueryTransformer` 实体含 orgId 列探测带缓存 + 无 orgId 实体透明跳过。
- **`ErpRoleDataAuthChecker` 双层灰度正确**：`getFilter→null`/`isPermitted→true`（关闸零回归）+ delegate 懒初始化 double-checked locking + `@PreDestroy` 传递 destroy；`app.data-auth.xml` 聚合（sal/qa/mnt 三域）**恰好覆盖全部有规则的域**（全 19 域 data-auth.xml 逐一核大小：sal 4491B/6 规则、qa 3395B/3、mnt 2873B/2，其余全 147B 空 `<objs/>`）——无域遗漏、无失同步。
- **action-auth 聚合完整性（任务点名 18+1 域）**：x:extends 的 19 个 erp-* 路径逐一验证文件存在（md/pur/sal/inv/fin/ast/prj/mfg/qa/mnt/crm/cs/hr/aps/ct/drp/log/b2b/notify）+ 4 系统模块；4 个 `test-orm-nop-*` 测试菜单根正确 `x:override="remove"`；`erp-sys` 系统管理 TOPM 内聚平台页面。唯二缺口即 finding 002/003（l10n 死菜单、job 注释漂移）。
- **delta 覆盖符合「不修补 nop core」原则（任务点名）**：`_vfs/_delta/default` 3 文件全部 `x:extends="super"` 差量合并（对照 bug `2026-08-22-ioc-delta-missing-extends-super.md` 教训）：两个 nop-auth view delta 仅 bounded-merge 追加 row-view-button；`flux-control.xlib` delta 只重写 5 个 picker tag、其余 70 tag 从 super 继承（跨仓库 nop-entropy/nop-chaos-flux 零改动，契约错位经应用层 delta 消化）。
- **application.yaml 非 cron 配置键全消费（任务点名普查）**：erp.* 键 3 个——`erp.data-auth.role-row-filter-enabled`（→ErpRoleDataAuthConstants ✓）、`erp.audit.field-read.enabled`（→MaskAuditRecorder ✓）、`erp-common.sod-enabled`（→SoDGuard ✓）；`erp.multi-company.org-isolation-enabled` 未入 yaml = 默认 false（刻意）；`render-mode: flux` 符合 E2E 强制渲染模式；`schema-introspection.enabled: false`/`validate-page-model: true` 安全/质量姿态在位；`%prod` `skip-check-for-admin` 省略继承平台 false（注释显式 DR-1e 裁决）。反向（代码消费而 yaml 未定义的键）归各域报告已登记家族，yaml 侧无孤儿键。
- **job 配置面一致（D4）**：33 个 job.yaml 全含 cronExpr 触发器 + `@cfg:nop.job.<name>.enabled|false` 统一双键模式 + enabled 全默认 false（一致的安全默认）；4 个抽查 invoker bean（erpCsQualityEscalationRetryJob/erpCtDocRetentionJob/erpMntDueVisitJob/erpLogTrackingPollJob）在各自域 beans.xml 全部注册；`nopBatchTaskRunner` 路径指向各域 batch.xml（各域报告已核）。
- **_init-data 种子四向一致性全过（任务点名）**：① 96 CSV 表头列 **全量** 映射 ORM `code` 列（排除 notGenCode 外部实体引用后零缺列——列增删后无种子漂移）；② FK 抽查 9 类引用列（CURRENCY/WAREHOUSE/MATERIAL/PARTNER/SUBJECT/ACCT_SCHEMA/UOM/PERIOD/CATEGORY_ID）跨全部 CSV **零悬空引用**；③ orgId 一致性——全部种子 ORG_ID ∈ {1,2}（`erp_md_organization` 定义全集），无未定义组织；④ 序号地板——最大种子数值 ID 7209（erp_sys_notification_template）< `zz-sequence-advance.sql` NEXT_VALUE=100000（MERGE 时序注释与平台 lazyInit 机制自洽，plan 2026-07-09-0814-1）。
- **nop_auth 种子安全姿态**：`nop_auth_user.csv` 密码为 bcrypt 哈希（非明文）；21 用户 × 21 角色经 `nop_auth_user_role.csv` 全配对（MaskHelper 角色面依赖完整）。
- **app-erp-all meta 五件套正确**：`ModuleMetaReader` 诊断容错（meta 缺失 debug 跳过 / parse-fail warn 不中断）；`checkDependencyIntegrity` 只做存在性+精确版本匹配（SemVer successor 已声明）；`DependencyIntegrityResult` 纯数据；`ErpModuleMetaErrors` 2 码均有参数绑定（对照 finding 010，该文件前缀合规）；`ErpApplication` Quarkus 生命周期标准（start→QuarkusIntegration+NopApplication、stop→CoreInitialization.destroy）。
- **module-common-test 基建正确（D6 抽查）**：`ThreadLocalFrozenClock` 仅冻结日期保留单调钟（不破坏 ContextProvider）；`ensureRegistered` 无条件重挂是对平台 `NopJunitExtension.afterAll` 重置时钟的对抗修正（javadoc 引平台源码行号实证）；`AbstractFrozenClockExtension` beforeAll/afterAll 配对；`FaultInjectionStubs` Proxy 桩 primitive-safe 默认返回 + 无 Mockito 对齐；`PerfTiming` warmup/median/p95/varianceRatio 实现正确（p95 线性插值、median 偶数取均值下取整）。
- **app-erp-test-data 骨架占位是显式声明**：`tables/README.md` 自述「本目录当前为骨架占位，不含具体 CSV」——无漂移可判（D7 声明性 N/A）。
- **D1 机械扫描近零**：module-common-service + app-erp-all 主代码 `@Inject private`=0、`System.currentTimeMillis/new Date`=0、`extends RuntimeException`=0、字符串 `==` 字典比较=0、`printStackTrace`=0、`LocalDateTime.now()`=0；唯一 `catch (Exception)` 在 `AbstractProcessor.currentUserId`（L74-81，取用户上下文防御性吞咽返 null，下游 SoD 对 null 放行是登记过的 wf 回调语义——可接受）。

## arm-index 复用 or 新增裁决（汇总）

- **新增 10 条**（arm-index 相关符号 grep 零命中）：`MaskAudit dedup 跨请求`/`l10n 死菜单`/`job 菜单注释漂移`/`bizObjName 缺参`/`buildGroupByQuery 死代码`/`ROUND_HALF_UP 废弃常量`/`mask 短值边界`/`reverseApprove 陈旧 javadoc×9`/`UniqueConstraintHelper 宽匹配`/`nop.err.erp.common 前缀`。
- **复用（不重复登记）**：P1-MA3-046（enable-action-auth 三层 OFF 姿态）、permissions-enforcement-roadmap L259（nop.debug/support-debug/JWT fallback prod-flip successor 检查表）、`ErpModuleMetaBizModel` 裸 `@BizQuery`（MA3-046 全域基线）。
- **同型核查不成立/不适用**：cron 键漂移家族（yaml 侧无键、job.yaml 模式统一）；P1-CK-fin-003（common 无过账面）；orgId 隔离族（common 基础设施正确，见注记表）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 0 | — |
| P2 | 1 | P2-CK-common-001 |
| P3 | 9 | P3-CK-common-002..010 |

按主维度：D2×1（001）、D5×2（002、003）、D10×3（004、007、009）、D6×1（005）、D1×2（006、010）、D3×1（008）。（D4/D7/D8/D9 本对象无新增 finding——job 接线/种子一致性/共享内核基线全部验证通过，见「验证为正确」节。）

同型/复用裁决：arm-index 复用不登记 3 项（P1-MA3-046、roadmap L259 检查表、裸注解基线）+ 修复在位验证 4 项（R12a/b/c=70/66/41、骨架 §16.4（4dab22be3）、org 隔离双钩子平台接线、data-auth 三域聚合）+ 同型核查不适用 3 项（cron 家族 yaml 侧、fin-003、orgId 写侧族归各域）。

## 剩余风险（查了什么/没查什么）

- **已查**：module-common-service 21 文件 + module-common-test 5 文件 + app-erp-all 6 Java 文件全量通读；app-erp-all `_vfs` 全资源清单核对（19 域 action-auth 文件存在性、3 delta 文件差量语义、33 job.yaml 键模式与 4 bean 注册、application.yaml 键消费普查）；96 种子 CSV × 19 orm.xml 列集脚本全量比对 + 9 类 FK + orgId + 序号地板；平台源码实证 6 处（transformer 钩子、interceptor collect-beans、错误描述缺参渲染、xbiz 可覆盖 defaultPrepareUpdate、IOrmInterceptor 签名、IQueryTransformer 形状）；R12a/b/c checker 同口径复测；骨架 9 文件 git 史（4dab22be3/069428710/4ebfd30c0 考古）；arm-index + 27 份 ck-* 报告对 10 个候选 finding 逐一裁决。
- **未深查**：~120 个骨架子类的逐个模板方法正确性（归各域报告，本报告只验骨架本体 + pur Order 链深读 + 全域 grep 面）；`erp-*-web` AMIS/flux 页面与后端契约 drift（归 C8.2 之外各域 web 抽查范畴）；种子 CSV 的**业务数值语义**（凭证借贷平衡/金额符号等只做结构一致性，语义校验归各域报告与修复阶段测试）；`nop-vfs-index.txt` 与 classpath 资源全量 diff（仅抽查 flux-control/app-all/_init-data 三类条目在位）；H2 单文件库作为 %prod 数据源的部署姿态（已裁决参考应用可接受）；`MaskAuditRecorder` 的平台 `IAuditService` 批处理失败语义（saveAudit 异常上抛为读路径失败——fail-closed 可接受，未实测）。
- **最不确定、建议主 agent 复核**：
  1. **P2-CK-common-001**——「跨请求审计抑制」的定性依赖「Quarkus worker 线程跨请求复用 + 无请求级清理钩子」（grep 零调用实证，但未运行时复现）。修复阶段建议集成测试：同一授权用户两次独立请求读同一保密字段，断言两条 FIELD_READ_DISCLOSURE（当前实现预期只记第一条）。若平台某处存在未被我发现的请求出口清理（如 IAuditService 侧），则降级 not-a-problem。
  2. **P3-CK-common-002**——l10n 死菜单是否「有意的 marketing 占位」需 owner 裁决：若设计侧明确要求菜单先行，则处置为 not-a-problem + owner doc 注记（菜单仍会在 UI 报错，建议至少加 gate）。
  3. **P3-CK-common-009**——`ERR_SQL_DATA_INTEGRITY_VIOLATION` 是否在 H2/目标驱动上确会承载非 UK 违例，需修复阶段用真实违例（如 NOT NULL 列缺值插入）实测翻译路径再定性。
  4. **P3-CK-common-010**——错误码前缀变更是对外契约面（客户端可能按码字符串分支）；修复阶段若判定不改码，须在 domain-design-guidelines §7.1 显式豁免 `nop.err.erp.common.*` 并说明与平台命名空间的边界。
