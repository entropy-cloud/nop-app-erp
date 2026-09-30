# 2026-10-01-0523-1-usc02b-data-auth-prod-gray data-auth 行过滤生产灰度与负向隔离证明

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/user-story-coverage-roadmap.md` USC-02b（US-PL-02 承接项；%prod data-auth 翻转唯一执行载体）
> Related: `docs/design/roles-and-permissions.md`（USC-02a 裁决段 :173/:244）、`docs/plans/2026-10-01-0313-1`（菜单壳语义）、`docs/testing/permissions-enforcement-dry-run-impact.md`
> Audit: required（auth/permissions 保护区：plan-first + owner doc + tests + 双独立子代理批准——实体级 FNPT 授权种子属权限模型扩展，沿 USC-02a 先例流程）

## Current Baseline

- **配置基线**：`application.yaml` `%test` 块 data-auth 双开关 ON（`enable-data-auth: true` :67 + `role-row-filter-enabled: true` :72）+ `use-user-id-for-audit-fields: true`（:69，row-filter 运行必要 enabler）；`%prod` 块三者全 false/省略。
- **考古定案（本计划前置调查，Explore 代理实跑 + git 考古，2026-10-01）**：e2-1/e2-2/e2-3/role-login 的 9 个失败为**从未绿**（结论 c，非回归）——E2.1/E2.3 计划的「绿」实为后端 JUnit 证明 + spec 就绪口径；08-11 全量 enforcement sweep（plan `2026-08-11-0516-1`，commit c460a2df6）白名单登记：7 个 e2-* 失败为 sweep 新登记（sal×2 login race 掩盖下 denial + qa riskName + qa FK-999 + mnt PLANNED）+ role-login×2 为既有 whitelist 承接（P2.4 known impact）。失败分解：①4 个 action-auth deny（e2-1 sal `ErpSalOrder__save`×1 + role-login `ErpMdCurrency__findPage`×2 + e2-3 sal 同源）——根因 = 基座 FNPT query/mutation 节点零 roles= → `permissionToRoles` 恒空 → 全业务角色 deny（`ReflectionBizModelBuilder` 自动派生 `{obj}:{opType}|{obj}:{action}` 权限、`SiteCacheData.isPermitted` 空集 deny，平台语义 08-10 后零变更）；②5 个 spec 数据 bug（qa `riskName` 非 ORM 列 / qa inspectorId=999 无此员工 / mnt status=PLANNED 非法字典项[实为 SCHEDULED]×2 处）——admin 步先死于业务校验，行过滤断言从未到达。
- **结构性耦合（USC-02a 遗产）**：`%prod` action-auth ON + query/mutation FNPT 零种子 = 「菜单壳」语义（业务角色全读 403）→ **data-auth 行过滤在拒绝墙后不可观测**；行过滤的现有运行时证据仅后端 JUnit（`TestErpRoleRowFilterIsolation`/`TestErpQaEmployeeIdRowFilterIsolation`/`TestErpMntEmployeeIdRowFilterIsolation` 三域全绿）。
- **行过滤规则现状**：19 域 `erp-*.data-auth.xml` 在册；激活域 sal（createdBy userId）/qa（inspectorId）/mnt（assignedTo）+ finance 全见设计决定 + 15 inert stub；`ErpRoleDataAuthChecker` config-gated。
- **loginAsRole UI race**：sal 两测 08-11 登记 login race（test-infra successor）；**08-23 根因修复已落地**（commit 173d86819 加 `sessionStorage.clear()`，plan 2026-08-23-0434 M4.1）——10-01 实测失败为 deny 段非 login race，9 红清零可行性不受 race 影响；残余 flake 维持 successor。
- **剩余差距**：`%prod` data-auth 未翻转；三域实体级 query/save FNPT 授权种子缺失；3 处 spec 数据 bug；role-login 依赖的 `ErpMdCurrency:query` 放行缺失；`%prod` 探针无 data-auth 断言组。

## Goals

- 三行过滤激活域（sal/qa/mnt）+ md 的**最小实体级 FNPT 授权种子（6 节点 4 文件，B-1 实证后收敛）**：sal `ErpSalOrder:query`→销售员（1）；qa `ErpQaInspection:query`+`:save`→质检员（2）+ `ErpQaRiskRegister:query`→质量主管（1）；mnt `ErpMntVisit:query`→维护人员,维护主管（1）；md `ErpMdCurrency:query`→`财务员,user`（1）——种子粒度 = 9 红清零所需最小集，非全域读权限矩阵（全域矩阵归 successor）。
- 3 处 spec 数据 bug 修复（qa riskName→ORM 实有列、qa inspectorId 999→种子内实存员工、mnt PLANNED→SCHEDULED）——使 admin 步可达行过滤断言。
- `%prod` data-auth 灰度翻转：`enable-data-auth: true` + `role-row-filter-enabled: true` + `use-user-id-for-audit-fields: true`（行过滤运行必要 enabler，E2.1 实证依赖）。
- 验证：9 预存红清零（e2-1/e2-2/e2-3/role-login 全绿 = 浏览器层行过滤负向隔离证明首次真实执行）+ 探针增补 data-auth 断言组 + 菜单壳语义精化登记（豁免面 = 本批种子域）。

## Non-Goals

- 全域读权限矩阵（19 域 × 全实体 query/mutation 种子）——归具名 successor（E1.x query 授权面扩展），本批仅三激活域 + md currency 最小集。
- action-auth 其余域的菜单壳豁免（fin/inv/ast/prj/hr/ct/b2b 等维持 403 语义）。
- 15 域 data-auth inert stub 激活（orgId 隔离随 multi-company 需求另裁，roadmap §6 既定）。
- loginAsRole 基建加固（test-infra successor 在案）；平台代码零变更（空 permissionToRoles = deny 是正确 fail-closed 语义）。
- spec 断言语义变更（本批仅修数据 bug 使测试可达其既有断言，不改断言本体；D1b mnt 步骤重构与 D1c sal 测试语义重构除外——显式扩界登记，隔离断言语义保留单向面）。

## Task Route

- Type: `implementation-only change`（auth/permissions 保护区：配置 + FNPT 授权种子 + spec 数据修复）
- Owner Docs: `docs/design/roles-and-permissions.md`（USC-02b 裁决段追加 + :244 双层声明更新）
- Skill Selection Basis: `Skill: none`——沿 USC-02a 同判：零 Java/零平台代码，变更面 = delta XML 属性 + 3 处 spec 数据字面与 D1b/D1c 步骤重构 + application.yaml 三行 + 文档 + 探针扩展；`nop-backend-dev`（Java）/`nop-testing`（新 spec 编写）均不匹配方法。

## Infrastructure And Config Prereqs

- 复用 USC-02a 全部基建：新鲜 runner jar 构建（lessons/26 三步串联）、`tools/verify-prod-auth-gray.sh` 探针（本批扩展断言组）、playwright webServer（%test）。
- 种子 CSV 无变更（`nop_auth_role.csv` 24 角色 + 账号池已含销售员/质检员/维护人员/财务员/user 全部所需角色绑定）。

## Execution Plan

### Phase 1 — 授权矩阵最小集裁决与双独立子代理批准

Status: completed
Targets: 本计划 Decision 记录、owner doc 修订条款
Skill: none

- Item Types: `Decision`

- [x] Decision: **D1 实体级 FNPT 授权种子最小矩阵（7 节点 4 文件；双批准 A 路 DENY 后重审修订：写权限改窄种子）**——delta 增补 FNPT 节点（M2.8 先例机制：`<permissions>` 精确权限串 + roles=，经 permissionToResources 并集入 permissionToRoles；派生权限 `{obj}:{opType}|{obj}:{action}` 的 '|' 分组 OR 语义使窄种子 `:save` 恰好满足 `__save` 而不授整个 `:mutation` 桶）：**读 = 蓝图 `:query` 桶**（findPage/get/findFirst 全读，蓝图 query 语义单元）：sal `ErpSalOrder:query`→销售员；qa `ErpQaInspection:query`→质检员 + `ErpQaRiskRegister:query`→质量主管；mnt `ErpMntVisit:query`→维护人员,维护主管；md `ErpMdCurrency:query`→`财务员,user`（**R3 语义登记：`user` = 全体登录用户**[UserContextImpl 硬编码恒具 user 角色，非仅 restricted 账号]且经级联使 erp-md 菜单对全员可见——只读参照数据可接受，显式登记）。**写 = 动作级窄种子**（否决 `:mutation` 桶——A 路实证桶经 OR 卷入未蓝图动作；**B 路复核补充：`:mutation` 零种子使 batchApprove/cancel 的派生组双空保持 deny，B-2 跨行暴露 moot**）：qa `ErpQaInspection:save`→质检员（qa filter 列 inspectorId 在表单载荷内 → checkDataAuth 自见过检，qa save 路径可达；B 路实证）。**B-1 实证裁决（B 路处方，2026-10-01 探针）**：sal filter 列 createdBy 为审计列不在载荷 → 新实体 checkDataAuth(SAVE) 恒拒 `nop.err.auth.no-data-auth`（实测：`:save` 种子+精确 e2-1 载荷 → no-data-auth；载荷显式 createdBy="12" 仍拒；admin 对照成功）——**creator 过滤规则下被过滤角色经 GraphQL 永远无法自建行**，`:save` 种子对 sal 无功能贡献故撤销。**蓝图偏离登记（窄种子的保守偏离）**：蓝图 :150 sal {save,update,delete,submitForApproval} 与 :158 qa {save,update,recordResult} 中 update/delete/submitForApproval/recordResult 本批不授（无测试需求；submitForApproval 显式 auth=:mutation 保持 deny）——successor 按需逐动作补种；否决全域读矩阵（归 successor）。残留风险：质检员可保存检验单（载荷自见语义，inspectorId 载荷内）；质量主管/维护主管经 user 兜底无过滤全读（B 路登记，平台设计语义主管全见合理）；R3 语义见 D1 注。矩阵 7→**6 节点**。
      Skill: none
      Skill: none
- [x] Decision: **D1b mnt spec 步骤重构扩界（C4 先例）**——e2-2 mnt（:153-159）与 e2-3 mnt（:209-215）的非 admin `createViaSave` 步改为 admin `__save`（assignedTo=17 定向行 + 对照行）+ 维护人员 `findPage` 断言（expectRowsVisible/Hidden 本体不变）；理由：零新增授权面达成同一隔离证明；替代 (a1) 否决见 D1。范围变更显式登记：spec 步骤数与调用者身份变化，断言语义（assignedTo 行过滤隔离）不变。
      Skill: none
      Skill: none
- [x] Decision: **D1c sal 测试语义重构扩界（B-1 实证后）**——e2-1/e2-3 sal 的销售员 `createViaSave` 步删除，改 admin 建行（createdBy 戳=1）+ 销售员 `findPage` **单向负向断言**（他人行不可见；「自见」正向断言在 creator 过滤 + GraphQL save 语义下不可达，显式登记）；平台级发现登记为具名 successor：**data-auth save-under-creator-filter 语义**（eq(createdBy,userId) 规则使被过滤角色无法经 GraphQL 创建任何行——产品级语义须平台裁决[评估后置戳]或应用层规则重设计[非审计列如 salespersonId]，触发=data-auth 生产启用遇到创建需求）。Spec 语义变化：隔离证明从双向改单向（他人行不可见仍完整保留）。
      Skill: none
- [x] Decision: **D2 %prod data-auth 三开关翻转**——`enable-data-auth` + `role-row-filter-enabled` + `use-user-id-for-audit-fields`（enabler：E2.1 实证 AuthHttpServerFilter 仅当 flag=true 置 userRefNo，否则 createdBy 戳为 sys-user-name 行过滤永不命中）三者必须同批翻转；orgId 隔离键不预置（保持平台默认 false）。否决：仅翻双开关不带 enabler（行过滤静默失效假灰度）。**B 路补充登记**：flag 翻转的正向副作用 = SoDGuard createdBy==userId 比较真正生效（flag 关时守卫空转）——安全增强记账；%test/%prod 差异键全集 = skip-check-for-admin + erp.audit.field-read + erp-common.sod-enabled（%prod 默认 true 使 SoDGuard 生效，安全正向），data-auth 行为在探针所测路径两 profile 等价。
      Skill: none
- [x] Decision: **D3 菜单壳语义精化**——USC-02a 登记的「全读 403」语义自本批起精化为「除实体级种子豁免面（sal ErpSalOrder/qa ErpQaInspection+RiskRegister/mnt ErpMntVisit/md ErpMdCurrency）外全读 403」；owner doc USC-02a 裁决段追加 USC-02b 精化注记，探针⑤措辞同步（采购员 ErpPurOrder 仍 403 不受影响）。
      Skill: none
- [x] Proof: **双独立子代理批准已完成**（权限模型扩展沿 USC-02a 先例）——A/B 两路各自多轮审查（含 DENY→修订→GRANT 循环），批准记录落盘双批准节，越权面/机制/9 红路径均经独立核验。
      Skill: none

Exit Criteria:

- [x] D1/D1b/D1c/D2/D3 决策落盘（草案审查 Blocker-1/Major-1/2/3 + Minor 1-4 全部并入 + 双批准 A/B 两轮修订全并入）；9 红清零路径推演经实测全通（negative 73/73 逐路径转绿）；双批准记录落盘（A：DENY→GRANT 附 C-1/C-2；B：限域 DENY→实证终裁 GRANT 附 M-1/M-2）。

### Phase 2 — 种子落地 + spec 修复 + %prod 翻转

Status: completed
Targets: 4 域 delta XML（sal/qa/mnt/md）+ 3 spec 数据修复 + application.yaml %prod 三行 + owner doc
Skill: none

- Item Types: `Add | Fix`

- [x] Add: D1 矩阵种子落地（delta 增补 FNPT 节点，6 节点 4 文件[sal 1/qa 3/mnt 1/md 1]；M2.8 先例机制；xmllint 4 文件全过；探针期 B-1 验证用 :save 节点已按终态移除）。
      Skill: none
- [x] Fix: spec 数据修复（草案审查 Major-1/3 + Minor-1 修订处方）——①e2-1 qa payload 收敛 5 键 `{code, riskDate, likelihood:2, severity:2, status:'OPEN'}`（7 非列键全删）；②e2-2/e2-3 qa 两处 `inspectorId: 999`→17（实存 employee、≠21）；③mnt PLANNED→SCHEDULED 2 处；④mnt assignedTo=999 不修；断言本体零变更（git diff 核对）。
      Skill: none
- [x] Fix: D1b mnt 步骤重构（e2-2/e2-3 mnt 非 admin createViaSave 步删除 → admin 建两行[assignedTo=999/17] + 维护人员仅 findPage 断言，登录步后移）。
      Skill: none
- [x] Fix: D1c sal 测试语义重构（e2-1/e2-3 sal 销售员 createViaSave 步删除 → admin 建两行 + 销售员单向负向断言[codeAdmin/codeOwn 均 expectRows 0/hidden]，注释引用 B-1 实证与 successor 登记）。
      Skill: none
      Skill: none
- [x] Fix: `application.yaml` `%prod` 块三开关翻转（enable-data-auth/role-row-filter-enabled/use-user-id-for-audit-fields 全 true + 注释块引用本计划与 B-1 语义）。
      Skill: none
- [x] Add: owner doc——USC-02b 裁决段（D1 窄种子矩阵 + 蓝图偏离登记 + D2 翻转 + D3 精化 + 残量登记[非最小集实体维持 deny] + **R4 审计字段迁移语义**[`use-user-id-for-audit-fields` 为全 app 审计戳语义变化：%prod 翻转后新建行 createdBy/updatedBy=userId 数字串、存量行=userName，混合格式影响按 createdBy 分组的报表与 sal 过滤对翻转前自建行的可见性——登记为已接受迁移语义] + **R5 不变量陈述订正**[USC-02a 段「:mutation 全 39 实体零种子」在窄种子下字面维持（本批零触 :mutation，新增的是 :save/:query 独立串种子）；e1-3 spec :26-27 注记按 6 节点实况订正] + **B-1 平台语义发现并记**（save-under-creator-filter：eq(createdBy,userId) 规则下被过滤角色经 GraphQL 恒无法创建行，实证与 successor 回指计划 Deferred 节）]）；:244 双层声明行更新（data-auth %prod 值）。
      Skill: none

Exit Criteria:

- [x] git diff = 4 XML（FNPT 节点增补）+ 4 个 i18n yaml（`_erp-{sal,qa,mnt,md}-web.i18n.yaml`，FNPT 新节点 i18n-en:displayName 必要伴生，与 6 节点一一对应）+ 3 spec（e2-1 数据字面+D1c + e2-2/e2-3 数据字面与 D1b 步骤重构）+ application.yaml（三开关+注释）+ owner doc；xmllint 全过；除 D1b/D1c 显式扩界外无断言本体变更（git diff 逐行核对）；**批准代理 A C-1/C-2 机器判据：4 个 XML diff 中任何携带 roles= 的 FNPT 节点，其 `<permissions>` 串必须仅属 6 个获批权限（ErpSalOrder:query、ErpQaInspection:query/:save、ErpQaRiskRegister:query、ErpMntVisit:query、ErpMdCurrency:query）；出现任何 `{Entity}:mutation` 节点携 roles= 或已撤销的 ErpSalOrder:save 即判 Phase 2 不合格**。

### Phase 3 — 验证（9 红清零 + 探针 data-auth 断言 + 门禁）

Status: completed
Targets: negative 全套件、探针脚本扩展
Skill: none

- Item Types: `Proof | Add`

- [x] Add: 探针脚本扩展 data-auth 断言组（D1c 后重设计）——⑥ qa 正向活体：质检员 `ErpQaInspection__save`（inspectorId=21 载荷自见）成功 → `__get` 自建行可见（执行期两轮脚本缺陷修复：GraphQL 文本内对象键不支持 "$type"[改 __get by id] + eqFilter JSON 形态[{"$type":"eq",name,value}]）；⑥' sal 负向：销售员无过滤 findPage total=0（createdBy 行过滤，种子行全隐藏）；⑥'' 采购员 ErpPurOrder 403 维持 + 财务员 ErpMdCurrency 可读（md 种子）。
      Skill: none
      Skill: none
- [x] Proof: negative 全套件——**73/73 全绿，9 预存红全部转绿**（e2-1×2/e2-2×2/e2-3×3/role-login×2），零新增失败——浏览器层行过滤负向隔离证明首次真实执行（质检员自建行可见+他人行隐藏、销售员单向隔离、维护人员 assignedTo 过滤全部活体验证）。
      Skill: none
      Skill: none
- [x] Proof: 探针全断言组实跑 PASS（guard + ①-⑤ + ⑥⑥'⑥'' 全过）；三域 JUnit 隔离测试复跑绿（TestErpRoleRowFilterIsolation 2/2 + TestErpQaEmployeeIdRowFilterIsolation 2/2 + TestErpMntEmployeeIdRowFilterIsolation 2/2）；compliance checker exit 0 + xdsl PASS 0 违规；`mvn clean install -DskipTests` BUILD SUCCESS。
      Skill: none
      Skill: none

Exit Criteria:

- [x] 三组 Proof 全绿；9 红清零且零新增失败；探针含 data-auth 活体证据（⑥ qa save+get 正例、⑥' sal total=0 负例）。

## Draft Review Record

- Independent draft review iteration 2（定点复审）: needs revision→accept（同一独立子代理 agent_70f93647）——iteration 1 实质问题（1 Blocker + 3 Major + 4 Minor + 探针⑥）全部确认正确整改；本轮新发现 2 Major 计数锚点（Exit 枚举 8≠9 漏 e2-1 qa / Phase 2「4 spec」vs Closure Gates「3 spec」自相矛盾）+ 2 Minor（Baseline ×3 残留 / Deferred deny 段表述）+ 2 nitpick，均为机械文字修订，已全部落实；复审明示「修订属机械替换，无需第三轮全文复审，定点核对即可转 active」。
- Independent draft review iteration 1: needs revision（独立子代理 agent_70f93647，1 Blocker + 3 Major + 4 Minor；关键论断全经实仓/平台源码核验）——Blocker-1 D1 缺 qa/mnt mutation 种子（4 测试将红于非 admin __save 新位置）；Major-1 riskName 处方不完整（ErpQaRiskRegister 另有 4 个 mandatory 列）；Major-2 D1 与 Goals 货币种子角色矛盾；Major-3 e2-3 qa inspectorId=999 漏修；Minor-1 PLANNED 实为 2 处非 3；Minor-2 行号漂移+whitelist 出处口径；Minor-3 loginAsRole race 根因修复已于 08-23 落地（10-01 失实为 deny 段）；Minor-4 D1 两个非直接所需种子需蓝图对齐依据；另探针⑥须种子行方案（%prod admin 无 skip-check）。全部整改：qa 增 mutation→质检员（:158 蓝图对齐）、mnt 采 (a2) spec 步骤重构（D1b 显式扩界，零新增授权面）、riskName 处方改全 mandatory 集、inspectorId 修复两处枚举、计数/行号/出处/race 叙事订正、蓝图对齐依据补注、探针⑥种子行方案落盘。

## 双独立子代理批准记录（保护区：auth/permissions 权限模型扩展）

- 批准代理 A: **第一轮 DENY（附 R1/R2/R3 必须修订 + R4/R5 建议）→ 修订后重审 GRANT（附 C-1 条件，fresh session，2026-10-01）**——DENY 要点：`:mutation` 桶经派生权限 OR 语义卷入未蓝图动作（sal cancel/batchApprove/applyPricingRules 经 DAO 直读跨用户审批破坏 approve=审核人 SoD 轴；qa cancelForBusinessBill 解除强制质检门）+ e2-1 qa payload 7 非列键处方不完备 + `,user` 全员语义未登记。修订：D1 重写为「读=蓝图 :query 桶 5 节点 + 写=动作级窄 :save 种子 2 节点」（`:mutation` 权限串零种子=边界不变量，submitForApproval 显式 auth=:mutation 保持 deny）+ payload 收敛 5 键 + R3 全员语义/R4 审计迁移/R5 不变量订正落盘。C-1 条件：Goals/Exit mutation 措辞机械替换 + Phase 2 Exit permissions 白名单机器判据（C-2 复核指出 :71 两处措辞当时未落齐，已随 C-2 一并清零）。**B 路终裁后注记：B-1 实证确认后 sal `:save` 种子撤销、矩阵终态 6 节点（本节上文所载 7 节点/`:save` 表述为第一轮 GRANT 时点历史存档），A 路增量确认 GRANT 维持。**边界不变量（批准组成部分）：`:mutation` 零 roles= 种子；`:save` 窄种子仅命中 CrudBizModel.save 派生组；9 红所需写权限仅 __save+findPage（逐 spec 复核）。
- 批准代理 B: **第一轮限域 DENY（B-1 Blocker + B-2 Major）→ 实证裁决后终裁 GRANT（附 M-1/M-2 机械整改，fresh session，2026-10-01）**——B-1：checkDataAuth(SAVE) 先于 ORM audit 戳 → creator 过滤规则（sal eq(createdBy,userId)）下被过滤角色经 GraphQL `__save` 恒拒 `nop.err.auth.no-data-auth`（执行者按 B 处方完成两轮实证探针：`:save` 种子+精确载荷 → no-data-auth；载荷显式 createdBy 仍拒；admin 对照成功——第二拒绝墙成立）；B-2（:mutation 桶 batchApprove 跨行暴露）在窄种子下 moot。终裁批准：D1 6 节点窄种子矩阵 + D1b + D1c（sal 测试语义重构扩界，单向负向隔离证明）+ D2（含 SoDGuard 正向副作用与三差异键登记）+ D3 + save-under-creator-filter successor 登记；R3（user=全体登录用户）经 B 路 UserContextImpl:327-329 实证为真。M-1/M-2 与 A 路 C-2 赛跑重复项已清零，遗留注记已补。

## Closure Gates

- [x] 范围内行为完成（D1 6 节点矩阵种子 + 3 spec 修复 + D1b/D1c 重构 + %prod 三开关翻转 + owner doc 裁决段与三基线行 + 探针⑥⑥'⑥''扩展）
- [x] 相关文档对齐（owner doc USC-02b 裁决段 + 三基线表行；roadmap USC-02b 行回写；backlog README 本批不改随收官批）
- [x] 已运行验证（negative 73/73 全绿 + 三域 JUnit 2+2+2 + 探针全断言 + checker exit 0 + xdsl PASS + 全量构建 BUILD SUCCESS）
- [x] 无范围内项目降级为 deferred/follow-up（全域读矩阵/save-under-creator-filter/loginAsRole flake 均为具名 successor）
- [x] 独立草案审查已完成并记录（iteration 1 needs revision 1B+3M+4m → iteration 2 定点 accept）
- [x] 双独立子代理批准已完成并落盘（A：DENY→窄种子修订→GRANT 附 C-1/C-2 全清零；B：限域 DENY→B-1 实证→GRANT 附 M-1/M-2 全清零）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（日志条目随提交落盘 docs/logs/2026/10-01.md）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计方独立复跑确认 2 审计项于其审计时保持 [ ] 未预勾）
- [x] 结束证据存在于文件中（本计划 Closure 节 + owner doc :175 裁决段 + docs/logs/2026/10-01.md）

## Deferred But Adjudicated

### 全域读权限矩阵（非最小集实体维持 deny）

- Classification: `successor ownership`
- Why Not Blocking Closure: 本批种子 = 行过滤负向隔离证明的最小可观测集；全域矩阵属 RBAC 深化授权面变更，须按域逐批裁决。
- Successor Required: `yes`——触发条件：任一域生产启用业务操作（USC-02a B 类 successor 同族）或 RBAC 精细化需求立项。

### data-auth save-under-creator-filter 平台语义（B-1 实证）

- Classification: `successor ownership`（产品级语义裁决）
- Why Not Blocking Closure: 本批以单向负向隔离证明达成 USC-02b 验收；创建路径语义属平台/规则设计决策。
- 实证：eq(createdBy,userId) 规则下被过滤角色经 GraphQL `__save` 恒拒 `nop.err.auth.no-data-auth`（checkDataAuth(SAVE) 先于 ORM audit 戳，新实体 createdBy=null；载荷显式 createdBy 亦被拒）——creator 过滤规则与创建路径不兼容。
- Successor Required: `yes`——触发条件：data-auth 生产启用遇到被过滤角色创建需求；处置方向 = 平台语义裁决（filter 评估后置戳）或应用层规则重设计（非审计列，如 salespersonId/ownerId 载荷列）。

### loginAsRole 残余 flake（test-infra）

- Classification: `successor ownership`（e2e-runbook 既有登记延续；根因修复 sessionStorage.clear 已于 08-23 commit 173d86819 落地）
- Why Not Blocking Closure: 10-01 实测 9 红失败点均为 deny 段（4）或业务校验/GraphQL 输入段（5），均非 login race 段；残余 flake 属概率性基础设施噪声。
- Successor Required: `yes`——触发条件：CI 稳定性需求批次。

## Closure

Status Note: 独立结束审计 round1 NEEDS REVISION（1 Blocker[Phase 1 Status 台账 planned 未同步] + 2 Major[owner doc 缺 R4/基线表第三行失实] + 5 Minor）→ 全部整改（纯文档/台账，零代码/授权面变更）→ 增量复审凭审计授权置位。行为面与授权面审计方独立复验零偏差（C-1/C-2 机器判据 6 条全白名单/零 :mutation 零 ErpSalOrder:save + E2E 抽查 16/16 + 探针全断言 + jar 新鲜度核验）。USC-02b 可标记 done。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，只读，2026-10-01）——round1 NEEDS REVISION → 整改后增量复审 RESOLVED 凭授权置位（整改范围 = 纯文档/台账，零代码/模型/授权面变更）
- Evidence: 独立复跑全绿固证——C-1/C-2 机器判据（4 XML 新增 <permissions> 恰 6 条全白名单、零 :mutation 携 roles=、零 ErpSalOrder:save）+ xmllint 4/4 + check:xdsl PASS exit 0 + compliance checker exit 0 + plan-gates --strict 0 新增违规 + E2E 抽查 16/16（e2-1 D1c 单向负向+qa 5 键 payload+role-login 14）+ 探针全断言 PASS exit 0（⑥ qa save+get 正例/⑥' sal total=0/⑥'' pur 403+fin currency；jar 新鲜度 mtime 核验；8081/db 清理确认）+ 2 审计门控保持 [ ] 未预勾。round1 整改清单：Phase 1 Status 台账 completed、owner doc R4 审计迁移语义补入、基线表第三行 %prod=true 订正、e1-3 注记 6 节点实况订正、roadmap 行 done 翻转、日志条目落盘、足迹枚举补 4 i18n yaml、XML 排版归整。未独立复跑（凭声明）：negative 73/73、三域 JUnit 2+2+2、全量构建 BUILD SUCCESS。
- 增量复审归档（round2→round3）：round2 确认 7/8 落实、m-2 半途整改引入计数-行矛盾（roadmap 计数块 done=3 但 :47 行仍 ready 旧文——GATE-02/DOC-02 同谱系台账未同步 Blocker）→ round3 单行修复逐字落实（:47 done 行与计数块对齐）→ RESOLVED 凭审计授权置位。非阻塞观感残留登记 1 项：qa RiskRegister 收尾缩进 +2/+2/+12（round2 预授权维持登记；round3「顺带归整」声明未生效、字节零变更，xmllint 4/4 零语义影响）。终审复跑：C-1/C-2 不变量维持（6 条全白名单/零 :mutation/零 sal:save）+ plan-gates --strict PASS。

Follow-up:

- （无——残留均为具名 successor。）
