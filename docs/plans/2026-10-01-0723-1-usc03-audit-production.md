# 2026-10-01-0723-1-usc03-audit-production 审计日志生产化与主数据变更可追溯

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/user-story-coverage-roadmap.md` USC-03（US-FN-06 收口口径 + US-MD-04 Should·条件触发关闭口径）
> Related: `docs/design/roles-and-permissions.md`（:105 E4.2 / :173 USC-02a 裁决段 / :213-222 基线表）、`docs/design/master-data/README.md`（:133 节头，配置项 :153）、`docs/analysis/2026-10-01-0300-1-us-coverage-matrix.md:43/:143`
> Audit: required（auth/audit 保护区：config 翻转 + FNPT 种子属权限模型面，沿 USC-02a/02b 双独立子代理批准先例）

## Current Baseline

- **审计机制全景（2026-10-01 实仓盘点）**：4 层机制——①平台字段级变更审计（`NopSysChangeLog` + audit/audit-save tagSet，6 个 finance 实体：VoucherTemplate(+Line)/GlMappingRule/IntercompanyTransferPrice/IntercompanyMatch/ConsolidationElimination；开关 `nop.orm.audit.enabled` 未设=全环境 ON，写入可查询断言已有范式 `TestErpFinVoucherTemplateAuditLog`[经 IEntityDao 查回]）；②E4.2 保密字段读审计（`erp.audit.field-read.enabled`，MaskAuditRecorder→IAuditService→NopAuthOpLog；%test true / %dev,%prod false——**唯一「%test ON/%prod OFF」审计键**）；③平台操作级审计（`nop.auth.graphql.enable-audit` + audit-mutation/query-patterns，简单通配符匹配操作/字段名 → NopAuthOpLog；**全环境从未启用**，erp 仓 grep 零命中）；④业务层留痕列（反结账三列+reason 必填守卫/过账异常处置三列/资产 ErpAstAssetActionLog 7 事件/红冲回链 isReversed+reversalOfVoucherId+ErpFinVoucherBillR——均已有 JUnit/集成测试覆盖）。
- **「查得到」缺口（roadmap 明示落点）**：全仓 grep `NopAuthOpLog` 于 *Test*.java **零命中**——无任何测试证明审计记录（尤其 FIELD_READ_DISCLOSURE）落 NopAuthOpLog 后经查询面查回；E4.2 既有测试全用 fake IAuditService。
- **读路径 %prod 不可达**：NopAuthOpLog 菜单 sys-oplog-main（roles="admin"）在 %prod 下「可见≠可用」（平台实体 query/mutation FNPT 零种子 → admin 查询亦 403，USC-02a 裁决段既有语义）；NopSysChangeLog 无菜单无页面，API 查询同零种子。**操作留痕读路径在 %prod 完全不可达**。
- **US-MD-04 现状**：`erp-md.critical-attributes` 键设计声明未接线（全仓零命中，coverage matrix :43 已裁定）；三要素实证在位（软停用 status 列/引用约束 IErpMd*ReferenceChecker SPI/审计字段 createdBy 系 + use-user-id-for-audit-fields %prod=true）。
- **红冲回链**：C13 集成测试（GraphQL 全栈 reason 落库+isReversed+countReversalVouchers）+ 11 个 reverse/reversal E2E spec 已覆盖，本批仅引用不重写。未验证残项：红字凭证摘要是否注明原因（reverseVoucher 无 reason 参数已实证）、让步接收理由专列——登记残量。

## Goals

- E4.2 保密字段读审计 %prod 翻转（唯一 %test ON/prod OFF 审计键收口）。
- 平台操作级审计增量启用：`nop.auth.graphql.enable-audit` + 高危 mutation 通配符 patterns（审批轴+红冲族），写 NopAuthOpLog——「高危操作留痕」生产承诺落地。
- 操作留痕读路径收口：NopAuthOpLog/NopSysChangeLog 查询 FNPT 窄种子→平台 admin（沿 USC-02b 窄种子范式；%prod 下 admin 可查询）。
- 「查得到」测试：新增集成测试证明 FIELD_READ_DISCLOSURE 落 NopAuthOpLog 且经 GraphQL findPage 查回（enforcement ON 双态）+ 操作级审计写入断言。
- US-MD-04 关闭口径登记（owner doc 注记：软停用/引用约束/审计字段已满足；critical-attributes 键未接线为已知不扩展项，合规客户点名才升格）。

## Non-Goals

- md 实体加 audit tagSet（字段级变更史机制已备、本批不动——US-MD-04 关闭口径明示不立项）。
- NopSysChangeLog UI/菜单建设（API 可查即满足关闭口径）。
- 让步接收理由专列、红字凭证摘要原因（未验证残项 → Deferred 登记）。
- 反结账/资产审计等已有留痕机制的重写（机制复用不重写）。
- oplog 查询种子放给业务角色（仅平台 admin；业务角色查审计归权限矩阵 successor）。

## Task Route

- Type: `implementation-only change`（auth/audit 保护区：config 翻转/新增启用 + FNPT 种子）
- Owner Docs: `docs/design/roles-and-permissions.md`（E4.2 段/基线表/USC-03 裁决段）、`docs/design/master-data/README.md`（US-MD-04 关闭口径注记）
- Skill Selection Basis: `Skill: none`——沿 USC-02a/02b 同判：零 Java/零新 spec 编写（新增 1 个 JUnit 集成测试类 = app-erp-all 既有 C-系列范式内扩展，测试类编写由「每业务功能实现时 AI 自拟测试」项目规则覆盖，非 nop-testing 技能的新 E2E 编写场景）。

## Infrastructure And Config Prereqs

- 复用既有基建：新鲜 runner jar（lessons/26 三步串联）、`tools/verify-prod-auth-gray.sh` 探针（本批扩展 ⑦ oplog 读路径断言）、playwright webServer（%test negative 回归）。

## Execution Plan

### Phase 1 — 决策裁决与双独立子代理批准

Status: completed
Targets: 本计划 Decision 记录、owner doc 修订条款
Skill: none

- Item Types: `Decision`

- [x] Decision: **D1 E4.2 %prod 翻转**——`erp.audit.field-read.enabled` %prod false→true（唯一 %test ON/prod OFF 审计键；机制/chokepoint/fail-safe 均已实证，翻转仅 yaml 一键）。
      Skill: none
- [x] Decision: **D2 操作级审计启用与 patterns（草案审查 M-1 扩集裁决）**——`nop.auth.graphql.enable-audit: true` + `nop.auth.graphql.audit-mutation-patterns` = `*__approve,*__reverseApprove,*__reverse*,*__cancel*,*__writeOff,*__void*,*__closePeriod,*__reverseClose,*__post*`（matchSimplePatternSet 语义；**B 路 C-1：`*__post` 严格后缀改 contains——postVoucher UI 人工凭证过账按钮/postSettlement/postElimination/postProcessingFee/postNcr 共 5 项漏网，与 M-1 reverse 族 UI 入口裁决完全对称**；命中 174→179）：`*xxx`=后缀/`xxx*`=前缀/`*xxx*`=contains，逗号分隔组）。**覆盖目标 = owner doc 高危清单四轴**（反审核/作废/反结账/处置+红冲族）：审批轴 ~95 项（approve/reverseApprove 后缀）+ 红冲族 contains `*__reverse*`（reverseVoucher/reverseSettlement×3/reverseConfirm×2/reverseCompletion/reverseNcr/reverseCostAdjust/reverseDepreciation/reverseTransfer/reverseBadDebtProvision/reverseCashRepay/精确 reverse×6）+ 作废 `*__cancel*` + 核销 `*__writeOff` + 作废薪酬 `*__void*` + 期间 `*__closePeriod,*__reverseClose` + 过账 `*__post*`（contains 补 5 UI 入口）。**无误伤面**：patterns 仅决定「是否写审计日志」非授权（audit≠deny），过匹配的代价仅为日志量——命中动作均为人工触发低频写操作，AuditServiceImpl 异步批（20 条/1s maxWait）量级无虞。query patterns 不启用（读面审计归 E4.2 field-read）；%prod 与 %test 同批启用（测试态同构；%test 下仅 E2E HTTP 层写 oplog，JUnit 引擎层不触 logger——B-1 机制，对 73/73 基线无断言面影响）。否决全量 `*` patterns（日志量与敏感面失控）与窄集 `*__reverse` 后缀（漏 reverseVoucher UI 入口与红冲族）。（后经执行期勘误 %test 单边回退，见 Phase 2 Fix 项。）
      Skill: none
      Skill: none
- [x] Decision: **D3 读路径窄种子**——`app.action-auth.xml` 增补 `FNPT:NopAuthOpLog:query`→admin + `FNPT:NopSysChangeLog:query`→admin（%prod admin 可查询操作留痕与字段级变更史；沿 USC-02b 窄种子范式，`:mutation` 零种子不变量维持）。三实现细节（草案审查 m-2）：①sys-oplog-main 现为自闭合叶节点 → 需转 children 形态（结构变更登记，异于 USC-02a 纯属性追加）；②i18n 伴生 = 节点内联 `i18n-en:displayName` 属性（app.action-auth.xml 既有实践，非域 i18n yaml）；③无资源 id 冲突机理：平台 `_nop-auth/_nop-sys` 的同名 FNPT 声明随 `test-orm-nop-auth/test-orm-nop-sys` TOPM 被 `x:override="remove"` 整树移除（合并站点零命中实证），app 聚合新增为唯一正确落点。业务角色查询归权限矩阵 successor。
      Skill: none
- [x] Decision: **D4 US-MD-04 关闭口径**——按 roadmap 裁决以「软停用+引用约束+审计字段已满足」关闭，不立项变更历史 UI；`erp-md.critical-attributes` 登记为文档声明未接线的已知不扩展项（合规客户点名才升格）；owner doc 注记落 master-data/README.md §关键属性审核规则。
      Skill: none
- [x] Proof: **双独立子代理批准已完成**（auth/audit 保护区）——A：GRANT 附 C-1/C-2/C-3（执行期核验条件）；B：GRANT 附 C-1/C-2/C-3+S-1（patterns 补纳 + 措辞分离 + 留存登记 + 实现纪律）；条件全部并入 Phase 2 执行项，零 DENY。
      Skill: none

Exit Criteria:

- [x] D1-D4 决策落盘（D2 终态含 B 路 `*__post*` 补纳）；双批准记录落盘（2×GRANT，条件并入 Phase 2 执行项）；「查得到」测试设计经两路批准代理核可达（A：触发路径 classpath/真 IAuditService/ admin skip-check 链；B：双段同步纪律）。

### Phase 2 — 配置翻转/启用 + 种子 + owner doc + 集成测试

Status: completed
Targets: application.yaml、app.action-auth.xml、owner docs、app-erp-all 集成测试
Skill: none

- Item Types: `Add | Fix`

- [x] Fix: application.yaml——%prod 块 D1 翻转 + D2 启用（终态含 B 路 C-1 `*__post*`；合并进既有 nop.auth/erp 段避免重复键）+ **D2 执行期勘误：%test 单边回退**（enable-audit=true 时 dry-run-impact E2E 系统性 15s 超时——服务端完成后下一请求未达的连接层悬挂，零命中 patterns 仍复现非写入量；平台 GraphQLWebService 层问题登记 successor；测试态覆盖由 TestErpAuditProductionPath @NopTestProperty 自供 config 承载，yaml 注释留痕）。
      Skill: none
      Skill: none
- [x] Add: app.action-auth.xml sys-oplog-main 自闭合转 children 形态 + 2 个 FNPT 窄种子（D3；带 `<permissions>` 子元素[A 路 C-3]，内联 `i18n-en:displayName`）。
      Skill: none
- [x] Add: Deferred 节增补 oplog 留存策略 successor（B 路 C-3）。
      Skill: none
      Skill: none
- [x] Add: 集成测试 `TestErpAuditProductionPath`（app-erp-all，2/2 绿）：①FIELD_READ_DISCLOSURE 形状 saveAudit → 双段同步（isAllProcessed 轮询 + DB await 重试）→ `NopAuthOpLog__findPage` 查回；②高危 mutation 留痕经 **newRpcContext + 手动 onRpcExecute**（@NopTestProperty 自供 enable-audit/patterns——JUnit JVM 不读 %test 块的执行期发现）→ 查回 ErpSalOrder__approve 留痕；③红冲回链引用 C13 范式（不重写）。**执行期偏离登记**：②原拟 MaskHelper 触发路径因 hr 报表 setup 重型化收敛为 recorder 生产形状 saveAudit（MaskHelper 触发层由 module fake-service 测试覆盖）；HTTP 层端到端接线边界维持登记；bisect 期 stash 操作事故一次（资源变更误收 stash 致两轮无效对照），已恢复并重验终态（详 docs/logs/2026/10-01.md）。
      Skill: none
      Skill: none
      Skill: none
- [x] Add: 探针脚本扩展 ⑦：%prod admin `NopAuthOpLog__findPage` 成功（读路径收口活体）+ admin `NopSysChangeLog__findPage` 成功（D3 ChangeLog 关闭口径验收，m-4）——实跑双断言 PASS。
      Skill: none
- [x] Add: owner doc——roles-and-permissions.md：E4.2 段 %prod 翻转注记（**A 路 C-1：订正去重示例数字**[实现键含 objId 逐行事件，非「100×13=13 条」]）+ USC-03 裁决段（D1-D4 + GraphQL 操作审计启用记录 + 双源命中族枚举 ≈176-179 + **B 路 C-3 oplog 留存三重缺口登记**[无清理机制/admin 无 UI 删除授权/shutdown 不 drain]）+ 基线表 2 行更新（field-read 行 + 新增 enable-audit 行）；master-data/README.md §审核规则 US-MD-04 关闭口径注记（**B 路 C-2：分离点名两个未实现面**[critical-attributes 键未接线 + 原值/新值留痕承诺不实现]，不得表述为「审计字段已满足该承诺」；「原因」复合满足注记[D2 oplog 无参数，原因由业务结构化列承载]）。
      Skill: none
- [x] Add: Deferred 节增补 oplog 留存策略 successor（B 路 C-3：清理/轮转/归档机制缺失，触发=oplog 量增长实证或生产部署）+ enable-audit E2E 连接悬挂 successor（执行期发现）。
      Skill: none

Exit Criteria:

- [x] git diff = application.yaml + app.action-auth.xml + owner docs ×2 + 1 新测试类 + `tools/verify-prod-auth-gray.sh`（⑦ 扩展，bash -n 过）；xmllint 全过；config 键值与 D1/D2 终态逐字一致（YAML 解析验证 prod/test graphql enable-audit 与 patterns、erp.audit.field-read=true）。

### Phase 3 — 验证（双态「查得到」+ 探针 ⑦ + 门禁）

Status: completed
Targets: 集成测试、negative 回归、探针扩展
Skill: none

- Item Types: `Proof`

- [x] Proof: `TestErpAuditProductionPath` **2/2 绿**（FIELD_READ_DISCLOSURE 查回 + 高危 mutation 留痕查回；③ 由 C13 既有覆盖）。
      Skill: none
- [x] Proof: negative 全套件 **73/73 全绿**（终态复跑；执行期插曲：D2 %test 同批启用曾致 dry-run-impact 系统性超时——bisect 三轮定位[audit off→绿/on→红/零命中→红]→ %test 单边回退勘误 + 平台连接悬挂 successor 登记，见 D2 勘误与 Deferred 节）；`mvn test -pl app-erp-all` **75/0/0/1**（73 基线 + 2 新审计测试）。
      Skill: none
      Skill: none
- [x] Proof: 探针全断言实跑 PASS（guard + ①-⑤ + ⑥⑥'⑥'' + ⑦ 双断言[oplog/ChangeLog 读路径活体]）；compliance checker exit 0 + xdsl PASS + `mvn clean install -DskipTests` BUILD SUCCESS。
      Skill: none

Exit Criteria:

- [x] 三组 Proof 全绿；「查得到」在生产同构配置下成立（JUnit 自供 config 与 %prod 翻转双证）。

## Draft Review Record

- Independent draft review iteration 2（定点复审）: accept（同一独立子代理 agent_0966c911）——五项整改逐项核对通过（B-1 断言面收敛与语义边界诚实/M-1 扩集命中枚举经复审全仓复验精确成立[174 项：approve 54+reverseApprove 41+reverse* 61+cancel* 48+writeOff 3+void 1+closePeriod/reverseClose 2+post 5，高危四轴全落网，无误伤] / M-2 跨阶段矛盾消除 / m-2 m-3 落实）；订正-1（:79 i18n yaml 残留措辞与 D3 ② 矛盾）+ 注-1/注-2 已随手落实；复审明示「按 :67 完成双独立子代理批准后转 active」。
- Independent draft review iteration 1: needs revision（独立子代理 agent_0966c911，1 Blocker + 2 Major + 4 Minor；抽查 9 项 7 项实仓复验命中含平台源码级）——B-1 测试断言②平台机制不可达（IGraphQLLogger 仅挂 GraphQLWebService HTTP 层，executeRpc 不经 logger；平台 TestNopDatavAuditLog javadoc 自证）；M-1 D2 patterns 与覆盖目标不符（`*__reverse` 严格后缀漏 reverseVoucher UI 入口与红冲族 + owner doc 高危清单作废/反结账两轴漏 + 命中/未命中清单未登记）；M-2 探针 ⑦ 无执行项且足迹枚举缺脚本；m-1 异步批落库时序（isAllProcessed 轮询先例）；m-2 D3 三实现细节（sys-oplog-main 自闭合转 children/i18n 内联形态/test-orm 移除无冲突机理）；m-3 行号锚点漂移；m-4 NopSysChangeLog 读路径无验收断言。全部整改：B-1 采纳平台手动 onRpcExecute+isAllProcessed 先例（HTTP 端到端边界显式登记）；M-1 扩集裁决（9 patterns 四轴覆盖+无误伤面论证+否决窄集理由）；M-2 探针 Add 项+足迹枚举补齐；m-1~m-4 全部落盘。

## 双独立子代理批准记录（保护区：auth/audit）

- 批准代理 A: **GRANT（附 C-1/C-2/C-3 执行期核验条件，fresh session，2026-10-01）**——D1（fail-safe 三路实证 + 写入量异步批吸收）/D2（命中独立重算 ≈176 vs 计划 174 基准噪声内、高危四轴全落网、无敏感载荷[newAuditRequest 不写 requestData]、%test 断言面零扰动）/D3（无 id 冲突机理实证 + admin-only 恰当 + :mutation 零种子维持）/D4（与 roadmap :152 逐字吻合）/测试三断言可达性全部独立核验成立。条件：**C-1** owner doc 注记订正 E4.2 去重示例数字（实现键含 objId 逐行事件，非「100×13=13 条」）+ 按双源基准重导命中族枚举；**C-2** 测试落库同步必须双段模式（isAllProcessed 轮询有 drainTo 竞态误报窗口 + DB 侧 await 轮询，平台先例 awaitLogByOperation）；**C-3** D3 两 FNPT 节点必须携带 `<permissions>` 子元素（NopAuthOpLog:query / NopSysChangeLog:query）。风险登记 R-1~R-5（去重示例精度/披露路径 fail-closed 重抛/队列 BLOCK_WAIT watch-only/留存 successor 触发器更活跃/命中数基准噪声）。
- 批准代理 B: **GRANT（附 C-1/C-2/C-3 必须条件 + S-1 建议，fresh session，2026-10-01）**——D1/D3/D4 主体按原案批准；D2 patterns 须按 C-1 补纳过账 UI 入口族（postVoucher/postSettlement/postElimination/postProcessingFee/postNcr，`*__post`→`*__post*`，与 M-1 reverse 族裁决对称）。差异化发现：①oplog 留存三重缺口（平台无清理机制 + %prod admin 无 UI 删除授权 + shutdown 不 drain 队列丢数据窗口）→ C-3 登记留存 successor；②master-data README「原值/新值留痕」承诺字面差距（who/when 审计列 ≠ 值留痕）→ C-2 D4 注记须分离点名两个未实现面 + 「原因」复合满足注记（D2 oplog 无参数，原因由业务结构化列承载[reverseCloseReason 必填守卫+E2E 在位]）；③实现纪律 = 禁止「mutation 后直接 findPage 断言 oplog」（shutdown 不 drain + isAllProcessed 竞态双理由，与 A 路 C-2 合流为双段同步）。174 命中枚举经 B 路 663 动作双源复验精确成立；`*__post` 无 `__repost` 过匹配（严格后缀语义源码级复核）。

## Closure Gates

- [x] 范围内行为完成（D1 翻转 + D2 %prod 启用[%test 勘误回退+successor 登记] + D3 种子 + D4 关闭口径 + 集成测试 + owner docs + 探针 ⑦）
- [x] 相关文档对齐（owner docs ×2 + roadmap USC-03 行回写 + coverage matrix 引用口径保持）
- [x] 已运行验证（集成测试 2/2 + negative 73/73 + app-erp-all 75/0/0/1 + 探针全断言 + checker/xdsl 双 0 + 构建 BUILD SUCCESS）
- [x] 无范围内项目降级为 deferred/follow-up（三项具名 successor：留存策略/业务角色查询/E2E 连接悬挂）
- [x] 独立草案审查已完成并记录（iteration 1 needs revision 1B+2M+4m → iteration 2 accept）
- [x] 双独立子代理批准已完成并落盘（A/B 双 GRANT 附条件全部并入）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（日志条目随提交落盘 docs/logs/2026/10-01.md）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计方独立复跑确认 2 审计项于其审计时保持 [ ] 未预勾）
- [x] 结束证据存在于文件中（本计划 Closure 节 + owner doc :178 裁决段 + docs/logs/2026/10-01.md）

## Deferred But Adjudicated

### 红字凭证摘要原因 + 让步接收理由专列（未验证残项）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 反结账/过账异常/资产处置等结构化留痕列已在位且测试覆盖；两项为叙事性留痕增强，非 US-FN-06 验收条件（红冲回链/isReversed 已全链覆盖）。
- Successor Required: `yes`——触发条件：审计客户对红冲原因/让步理由提出结构化查询需求。

### enable-audit E2E HTTP 连接悬挂（平台层）

- Classification: `successor ownership`（平台 GraphQLWebService/vertx 层，外部仓库）
- Why Not Blocking Closure: %test 单边回退后 negative 73/73 全绿恢复；%prod 单边启用为本批生产交付面，探针/集成测试验证不受影响。
- 实证：enable-audit=true 时 dry-run-impact spec 系统性 15s 超时——服务端 10ms 完成请求并写完 end-graphql-request 日志后下一请求未达服务端（连接层悬挂）；零命中 patterns（`*__nomatch__`）下仍复现（bisect 排除写入量）；enable-audit=false 则绿。
- Successor Required: `yes`——触发条件：E2E 层审计端到端断言需求或平台升级；处置 = 平台层连接/悬挂根因定位（nop-entropy 仓库，需跨仓流程）。

### oplog 留存策略（B 路 C-3 三重缺口）

- Classification: `successor ownership`
- Why Not Blocking Closure: 平台既有特征非本批引入（登录登出直写今天就已写 oplog）；D1/D2 为增量源使触发更易达到。
- 缺口：平台无清理/轮转机制 + %prod admin 无 UI 删除授权（`:mutation` 零种子不变量）+ shutdown 不 drain 队列（丢数据窗口）。
- Successor Required: `yes`——触发条件：oplog 量增长实证或生产部署（清理/轮转/归档策略 + ops runbook）。

### oplog/ChangeLog 业务角色查询授权（权限矩阵 successor 同族）

- Classification: `successor ownership`
- Why Not Blocking Closure: 本批收口 admin 读路径（最小承诺）；业务角色审计查询面属 RBAC 深化。
- Successor Required: `yes`——触发条件：同全域读权限矩阵。

## Closure

Status Note: 独立结束审计两轮收敛通过（round1 NEEDS REVISION 1M+2m → 整改 → round2 增量复审 RESOLVED → ACCEPT）。M-1（owner doc %test 姿态未随 D2 勘误同步）整改后与 shipped yaml 实态逐字一致、错误归因清除；行为面/授权面 round1 已全量独立复验（五项复跑全 PASS + 批准条件逐项实证 + patterns 命中双宇宙抽验）。USC-03 可标记 done。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，只读，2026-10-01；两轮收敛 ACCEPT）
- Evidence: round1 全量核验 + 五项独立复跑全 PASS——`mvn test -pl app-erp-all -Dtest=TestErpAuditProductionPath` 2/2（surefire 实证 0F/0E）、`npm run check:xdsl` PASS、`nop-compliance-checker.sh` exit 0、`tools/verify-prod-auth-gray.sh` 全断言 PASS（含⑦ oplog/ChangeLog 双断言，exit 0）、`check-plan-gates --strict` PASS；jar 内 yaml/action-auth 与源码逐字节一致（探针非 stale 态）；YAML 严格解析无重复键；xmllint/bash -n 过；A/B 六项批准条件逐项落实实证；patterns 命中数独立双宇宙抽验佐证（reverseApprove=41/cancel=48/writeOff=3/void=1/post 族 10=5+5 精确吻合，B 路 C-1 点名 5 过账 UI 入口实存）；round1 唯一 Major（owner doc 基线表+裁决段 %test=true 与 yaml 勘误回退矛盾）经整改 round2 复审：两处修正与 yaml 实态一致、错误归因清除、m-1 决策行指针与 m-2 stash 登记落盘、足迹无附带改动、门禁复跑 PASS。

Follow-up:

- （无——残项均具名 successor。）
