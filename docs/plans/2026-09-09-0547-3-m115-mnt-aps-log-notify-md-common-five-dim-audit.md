---
status: active
mission: ai-check-r3
work-item: M1.15
group: "2026-09-09-0547"
verify: [test]
---

# 2026-09-09-0547-3 M1.15 maintenance + aps + logistics + notify + master-data + common 五维符合性审计（C 级域 + 归属格合并全格）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK checker report mode CAT1..4 = 0/0/0/0、`--strict`/`--self-test` PASS、双 checker 零回归、白名单 27 文件四要素齐备）。M1.15 为 roadmap 文档序「deps 已满足且无既有计划」工作项（同批 M1.11 为 N=1、M1.14 为 N=2，三者相互独立无解除阻塞关系，按 roadmap 文档序排列；M1.16 依赖本项完成，不在本批范围）。
- 同 mission 先例：12 切片已执行并独立闭包审计 ACCEPT（多域合并先例 M1.12 prj+qa 双报告 / M1.13 pur+sal+inv 三报告，执行目录下 15 份姊妹 `ck-*-r3.md` 在盘）——本计划沿用其通过的七阶段执行形态，六单元分报告落盘。同批姊妹 M1.14（plan `2026-09-09-0547-2`）与 M1.11（plan `2026-09-09-0547-1`）为本批 active 同侪计划（起草时未执行、无报告在盘），非已执行先例，不作为本计划的查重/证据源。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U10 + U15 + U16 + U11 + U01 + U20 各 × 五维（全格）**（§4 映射表第 15 行，6 单元 × 5 维 = 30 格）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准，执行期只允许在查重裁决列追加证据，不改判定标准。
- 切片范围（六单元全格，物理面各 `module-<domain>/erp-<short>-{dao,service}` 的 `src/main` + DIM-F web 面）：
  - **maintenance（U10）**：设备/维保计划/visit/downtime 族；owner doc `docs/design/maintenance/`（equipment-integration + state-machine/README/use-cases）。
  - **aps（U15）**：排产引擎/约束求解/auto-dispatch/alternative-routing 族；owner doc `docs/design/aps/`（scheduling/constraint-based-planning/auto-dispatch/alternative-routing + state-machine/README/use-cases）。
  - **logistics（U16）**：承运商/运费/发运族；owner doc `docs/design/logistics/`（carrier-integration/delivery-window + state-machine/README/use-cases）。
  - **notify（U11，子系统本体归属格）**：通知派发子系统（模板驱动派发/收件箱管线/nop-message 跨域事件）；owner doc `docs/design/notify/`（inbox-patterns + README/seed-data/use-cases）。
  - **master-data（U01）**：跨域被引用面（partner/material/uom/exchange-rate 被 18 域引用）：机制 B notGenCode、汇率 MUTEX 钩子、价格链 helper、SKU 多单位、统一伙伴身份；owner doc `docs/design/master-data/`（exchange-rate-management/sku-multi-unit/unified-party-identity/cross-border-trade/data-migration + README/use-cases）。
  - **common-service（U20，抽象族归属格）**：`AbstractProcessor.illegal*` helper 期望态参数、`AbstractErpCrudBizModel`/`AbstractErpImmutableCrudBizModel` 状态锁基类行为、共享 config/错误码基建（基类逐方法走查）+ common-test 基座；查重锚 r1 `ck-common-app.md`（C8.2）。
- 共享代码唯一归属（冻结清单 §3.2，本计划为两处归属格）：**common 抽象族唯一归属 = 本计划 U20 格**（各域切片只审调用点，基类行为缺陷在本格立项）；**notify 派发子系统本体唯一归属 = 本计划 U11 格**（各域通知发送调用在各域切片记录消费点）；posting 引擎内部归 fin-1（M1.1 已收官，mnt/logistics 运费过账 provider 为消费侧）；聚合横切面归 U21（M1.16）。
- 跨轮查重源（§2，逐单元）：r1 `docs/audits/check/ai-check-index.md` §报告清单 + §Finding 追踪——`ck-maintenance.md`（C5.3：0 P0 / 3 P1 / 6 P2 / 10 P3）、`ck-aps.md`（C8.1：0/2/6/3）、`ck-logistics.md`（C8.1：0/2/7/2）、`ck-notify.md`（C8.1：0/2/4/2）、`ck-master-data.md`（C1.1：0/0/5/9）、`ck-common-app.md`（C8.2：0/0/1/9）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照（已裁决偏离不重复报告）。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：六单元探针族（mnt CAT-1 10/CAT-2 7、aps CAT-1 = 0、log CAT-1 19、notify CAT-1 15、md CAT-3 3、common CAT-2 5/CAT-3 族）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级裁决报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。六单元白名单条目：mnt 1（`ErpMntDashboardBizModel.java`）+ aps 2（`IErpApsOperationOrderBiz.java` + `ErpApsOperationOrderBizModel.java`）+ md 1（`ErpMdDashboardBizModel.java`）+ common 2（`ErpOrgIsolationQueryTransformer.java` + `MaskHelper.java`）= **6 条抽 ≥3 条核对**；log/notify = 0 条（记录条目数 0）。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；六单元 service 模块回归对照计数 mnt 157 / aps 82 / log 66 / notify 23 / md 160 / common 23（`cjk-baseline.md` §批注账 MI.6 批 2 行锚点；执行期以 known-good-baselines 最新行为权威，同批姊妹计划测试增量允许并披露）；`npm run validate:flux` step [1/3] 导出 0 error（999 页 / erp 855），整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移（successor 在案，非本切片 finding）。
- 仓库现状（2026-09-09 起草时实核）：HEAD `2c1c1ef25`，工作树零脏面。审计证据以实跑时点 HEAD + 脏面披露为准（MI.9 收官审计先例：脏树实跑 + 披露）。
- 剩余差距：6 单元 × 五维 = 30 格 verdict 未落盘；执行目录尚无 mnt/aps/log/notify/md/common 切片 `ck-*.md` 报告（common 归属格收口后，各已收官切片标注「归 U20/U11」的共享代码 finding 才有唯一裁决落点）。

## Goals

- 按冻结清单对 U10/U15/U16/U11/U01/U20 各 × 五维全格全跑（禁止抽样、禁止跳维；U20 DIM-F/S 为显式 `n-a` 带理由），逐格落 verdict（`pass` / `finding` / `n-a` 带理由），产出六份报告 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-maintenance-r3.md` + `ck-aps-r3.md` + `ck-logistics-r3.md` + `ck-notify-r3.md` + `ck-master-data-r3.md` + `ck-common-r3.md`（各含五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明，roadmap 横切关注点 13）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样每单元 ≥1 doc × ≥2 断言（六单元合计 ≥6 doc × ≥12 断言；任一单元 ≥2 处漂移扩大至该单元全部 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单（6 行）+ 跨轮 `docs/audits/check/ai-check-index.md`（§报告清单 6 行 + §Finding 追踪新 ID 行）。
- 全程零生产代码改动（roadmap 规则 6）：只产 finding 与索引，收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（M1 只读审计；修复归 M2.x，强制先写失败测试）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖六单元之外任何单元格（crm/cs/ct/b2b/drp 归 M1.14 同批 N=2；app-erp-all 横切归 M1.16——本计划不触碰聚合/菜单/集成快照面）；不重复已收官 12 切片的格。
- 不接管 r1/r2 工作项（横切关注点 5）；不重开既有裁决（lesson 09/10、compliance baseline、CJK 白名单）；不做 roadmap 状态翻转（done 转换由结束审计机制处置）。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用，不新建目录）；盘点注记落本计划勾选注记
> Prereqs: 无

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（本轮唯一规范执行目录，幂等复用；roadmap 规则 8），确认 `ai-check-r3-index.md` 与 `m0-5-audit-checklists.md` 及既有姊妹切片报告在位——**实核 2026-09-09**：目录在位（幂等复用零新建），`ai-check-r3-index.md` + `m0-5-audit-checklists.md` + 15 份姊妹 `ck-*-r3.md`（M1.1/M1.2~M1.4/M1.5~M1.9/M1.10/M1.11/M1.12×2/M1.13×3/M1.14×5）全部在盘
      - Skill: none
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、同批姊妹计划（`2026-09-09-0547-1/2`）执行状态披露（MI.9 收官审计脏树实跑先例）；后续全部证据注记引用该时点——**实核 2026-09-09（审计时点 T0）**：HEAD `e331c55b2fc42196cdd008ca2bb45232c9046843`（计划基线 `2c1c1ef25` 后两笔推进均为姊妹审计产物提交：`5eb2c4dbe` = 0547-1 M1.11 落盘 + 闭包审计 ACCEPT、`e331c55b2` = 0547-2 M1.14 五域落盘 + 闭包审计 ACCEPT，生产代码零变化）；脏面 = 1 条 untracked 计划文件（本计划自身），tracked 零修改；姊妹 0547-1（M1.11）/ 0547-2（M1.14）均已执行完毕并独立闭包审计 ACCEPT（roadmap M1.11/M1.14 行「执行完成待收官翻转」注记在案），非本计划查重/证据障碍
      - Skill: none
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）——**实核 2026-09-09**：checker 19 规则全表 = R1a-c 0/0/0 + R1d 14 + R2a 34 + R2b 242 + R2c 1542 + R2d 38 + R3 5 + R4 0 + R5 0 + R6 2 + R7 0 + R8 0 + R10 14 + R11 0 + R12a 71 + R12b 66 + R12c 42 = **M0.3 快照行逐值一致零漂移**（exit 0）；CJK report mode **CAT-1..4 = 0/0/0/0**（3430 java + 886 yaml 扫描）= MI 终态行一致零漂移（exit 0）
      - Skill: none

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样，六单元逐单元）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.15 行指定，承 M1.14 行「同上」）
> Targets: `module-maintenance/erp-mnt-*`、`module-aps/erp-aps-*`、`module-logistics/erp-log-*`、`module-notify/erp-notify-*`、`module-master-data/erp-md-*` 各 `{dao,service}/src/main/java` + `module-common-service/*` + `module-common-test/*`；走查 verdict 落勾选注记
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑（六单元逐单元；U01 增量：`grep -c 'notGenCode' module-master-data/model/app-erp-master-data.orm.xml`）：compliance checker 逐规则对照 + 反模式 grep 族（`extends RuntimeException`/`@Inject private`/`System.currentTimeMillis`/`@Transactional`×`@BizMutation` 对照 R6 基线/`IDaoProvider|IOrmTemplate|@SqlLibMapper` 命中处注释理由）+ codegen 产物安全（`__XGEN_FORCE_OVERRIDE__`、`_gen` 脏面）+ 聚合完整性（按 §6 勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
      - **实核 2026-09-09**：checker 19 规则全表 = M0.3 快照行逐值一致零漂移 exit 0（R1d 14/R2a 34/R2b 242/R2c 1542/R2d 38/R3 5/R6 2/R10 14/R12a 71/R12b 66/R12c 42 余 0）；反模式族六单元全零（RuntimeException=0/@Inject private=0/System.currentTimeMillis=0/@Transactional∩BizMutation=0/LocalDate.now=0，逐单元 grep 实跑六报告 §1 B 行）；IDaoProvider 逐单元命中核注释：mnt 13 文件（5 有 E3 注释/9 无专项注记归 common-014-r3）、aps 9 文件全有豁免（规范最优域）、log 5 文件（4 有/1 无=log-014-r3 新立）、notify 5 文件（RecipientResolver 有理由/4 自身实体模式内用法）、md 10 文件（5 有/3 无归 common-014-r3）、common 3+1 文件（Transformer/RoleDataAuthChecker 有理由/AbstractProcessor:31 批发 handle 零注释归 common-014-r3）；`@SqlLibMapper` 全域 0；XGEN 命中全为 meta dict.yaml 校验点（mnt 12/aps 12/log 12/notify 5/md 21）+ 各模块 git status porcelain 空；E1 勘误路径聚合器在位含六单元 x:extends 注册（notify :23/md/mnt/aps/log 实证）；U01 增量 `notGenCode` = 0（md 为被引用侧，机制 B 方向正确）
- [x] <Proof> 15 维度逐维走查六单元范围（程序式确定性走查落 verdict；单元专属焦点按 §3.3 各行：mnt 设备状态机/维保计划/预警查询、aps 前向/后向排产引擎/约束求解/dispatch、log 承运商调度/运费过账消费侧/发运状态机、notify 派发子系统本体（模板驱动/收件箱管线/nop-message 跨域事件）、md 跨域被引用面（机制 B notGenCode/汇率 MUTEX 钩子/价格链 helper）、common 抽象族归属格基类逐方法走查（`AbstractProcessor.illegal*`/`AbstractErpCrudBizModel`/Immutable 状态锁基类/共享 config 基建）；共性焦点：②跨实体 I*Biz ⑧状态机 ⑨审批流/作业 ⑮断言抽样——每单元 ≥1 owner doc × ≥2 关键断言（状态名/字段名/角色名/迁移路径/ErrorCode），任一单元 ≥2 处漂移扩大至该单元全部 owner doc）；blocker/major/minor 按 prompt 严重性指南分级
      - Skill: nop-platform-conformance-audit-prompt
      - **实核 2026-09-09**：15/15 无跳维逐单元落 verdict（六报告 §1 DIM-B 行）：mnt finding（2 新立）/aps finding（4 新立：012-r3 P2 终态复活+3 P3）/log finding（3 新立 P3）/notify finding（2 新立：009-r3 死码族+010-r3 越权纠错）/md finding（3 新立：015-r3 P2 ORM 孤儿默认值+2 P3）/common finding（5 新立：011-r3 P2 save 通道盲区+012/013/014/015-r3）；②跨域写六单元全经 I*Biz（log 跨域 daoFor 零命中/aps 9 文件豁免注释全齐）；⑧逐值 writers 核对（mnt visit/request/status-log 全活+DISPOSAL successor 裁决、aps 8 值全活零死值、log 6 值全活、notify PENDING/FAILED 预留死值 README 登记在案、md partner-type EMPLOYEE/CUSTOMS_BROKER 死值观察登记）；⑨FNPT 家族 mnt/aps/log/md 4 新站点 + aps/log job yaml 键对齐合规样本；基类逐方法走查落 common 报告 §1（illegal* 契约/状态锁列探测/save 通道盲区/共享基建 12 组件）
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样（六单元合计 ≥6 doc × ≥12 断言）独立落注记，抽样对象与逐条结论在案
      - Skill: code-quality-audit-prompt
      - **实核 2026-09-09**：合计 11 doc 面 × 34 断言（≥6 doc × ≥12 达标）：mnt state-machine+equipment-integration 6 断言（5 一致 1 漂移=021-r3）、aps state-machine+scheduling 6 断言（4 一致 2 漂移归并 aps-003/004/005）、log state-machine+carrier-integration+delivery-window 7 断言（4 一致 3 漂移=2 归并+013-r3 新立+1 命名微漂移注记）、notify inbox-patterns+README 8 断言（4 一致 4 漂移=2 归并+011-r3 新立+1 软漂移）、md exchange-rate-management+sku-multi-unit+unified-party-identity 7 断言（3 一致 4 漂移=1 归并 md-005+017/018-r3 新立+1 软漂移）、common 无 owner doc=基类契约 6 检查点替代面（1 违反=013-r3）；扩样条款逐单元履行（记录落六报告 §2.5）
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（逐单元查 r1 对应报告 §Finding 追踪 + r2 目录 + §Mission 基线快照）：同型 fixed=复用（复核 HEAD 仍有效）/同型 open=归并原 ID/确属新发=新立 `P{n}-CK-{mnt|aps|log|notify|md|common}-{NNN}-r3`；裁决证据落勾选注记（报告落 Phase 7）
      - Skill: code-quality-audit-prompt
      - **实核 2026-09-09**：r1 全 73 条逐一三态裁决（mnt 19/aps 11/log 11/notify 8/md 14/common 10）= 复用 6（mnt-001/002/003 已修在位+索引 open=回填缺口注记 fin4-013 先例、mnt-006/007 F1.3/F1.2、log-001 F1.1 引擎侧在位消费侧收口）+ 归并 67（全量 file:line 现症复核落六报告 §2.2；aps-008 补充裁定守卫惰性、aps-011A 现症收窄 orm mandatory、notify-002 现症扩大 8→34、common-008 追加 mnt 新站点）+ 新立 27（mnt 020..023/aps 012..016/log 012..015/notify 009..011/md 015..020/common 011..015，编号承接各域 r1 最大号顺延，历史 ID 零覆写）；r2 目录核对无同型新独立登记；双 handoff（fin3-017/fin4-022/ast-028/qa-027→common-014-r3、prj-023→common-015-r3）落槽裁决

Exit Criteria:

- [x] 六单元 DIM-B 格 verdict 全落盘（pass/finding/n-a 带理由），15 维度无跳维
- [x] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成（复用/归并/新立逐条在案）

> 实核 2026-09-09：15/15 无跳维六报告 §1 B 行落盘；checker 19 规则零漂移 + 反模式族全零数字在案；73 条 r1 三态裁决闭合（复用 6/归并 67/新立 27）。

## Phase 3 — DIM-F 前端页面与 E2E 走查（六单元逐单元）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2 所列 architecture/design docs + e2e-runbook）
> Targets: 六单元 web 面 `_vfs`（mnt 设备台账/维保看板页面、aps 排产甘特图（复杂手写页清单成员）、log 发运追踪页（复杂手写页清单成员）、notify 通知收件箱（user TOPM 注册，维度⑭特例核对：app.action-auth.xml user TOPM grep）、md 主数据选择器页（picker 范式）/SKU 多单位 UI）；U20 common 层无独立页面 = `n-a` 记录理由；E2E 涉六单元 spec 时按 §1.2 ③
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（step [1/3] 导出 0 error / 999 页；整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移，successor 在案，非本切片 finding）+ flux-only grep（`component="AMIS"` 保留层 expect 0 / ORM `ext:web-renderer="flux"` 缺失 expect 空）+ notify 维度⑭特例（user TOPM 注册 grep）；325 ERR 族按六单元命中条数分别对账（既有外部漂移不立项）
      - Skill: none
      - **实核 2026-09-09**：step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；整体 855 validated / errors=325 / warnings=18491（全部 ERR 行含 variant 属性，非 variant ERR = 0 = 既有外部漂移 successor 在案不立项）；六单元 ERR 对账：mnt 26/aps 4/log 8/notify 2/md 22/common 0（无页面）合计 62 条全 variant 同族；`component="AMIS"` module-*/erp-*-web 全局 0；`grep -L 'ext:web-renderer="flux"' module-*/model/app-erp-*.orm.xml` = 空；notify 维度⑭特例 pass：聚合器 :23 x:extends 引入 + erp-notify.action-auth.xml:6-14 user TOPM（`resourceType="TOPM" roles="user"`）+ SUBM 双重标注非 admin-only
- [x] <Proof> 六单元页面逐页走查（codegen+delta 面对照 view-and-page-strategy：REST `/r/` 数据访问 / codegen vs 手写边界查 M0.4 矩阵 / 定制走 `x:extends` / i18n-en 承载约定；md 页对照 picker-patterns）逐页落 verdict；涉六单元 E2E spec 时核对 PageObject 模式 + `E2E_ENGINE` 缺省 flux + 禁 GraphQL 断言；U20 格 `n-a` 理由落注记
      - Skill: none
      - **实核 2026-09-09**：六单元页面面全量清点落六报告 §1 DIM-F 行——mnt 15 实体（15/15 x:extends、6 手写 yaml 全 /r/ REST、E2E 8 spec 合规；visual selector 家族 cs-027-r3 归并 2 站点）；aps 7 实体（甘特双载体零遮蔽缺陷、i18nEn 齐全、归并态注记 aps-004 消费点）；log 8 实体（8/8 x:extends、追踪页 log-012-r3 新立 P2）；notify 3 视图族（inbox 316 行 i18nEn 双语、admin bounded-merge 合规、notify-011-r3 新立）；md 25 实体（picker 25/25 全覆盖、10 处 @query REST、view.xml 25/25 i18n-en、md-016-r3 死样式分支新立）；common n-a（无 erp-*-web 模块无页面资产，判定面归 U21）；E2E_ENGINE 缺省 flux 实证（engine.ts `return 'flux'`）+ 六单元 16 business-actions spec PageObject 合规 + 页面级 GraphQL 断言 0

Exit Criteria:

- [x] 六单元 DIM-F 格 verdict 全落盘（U20 为 n-a 带理由）；全局面门禁数字（导出 error 数 / flux-only 命中数 / 六单元 ERR 命中对账）在案
- [x] 六单元范围页面逐页走查完成，无跳页

> 实核 2026-09-09：五格 verdict（mnt/log/notify/md finding、aps pass 归并态、U20 n-a）+ FLUX_PAGE_ERROR_COUNT 0/999/855 + AMIS 0/ORM 缺失 0 + ERR 对账 26/4/8/2/22/0 在案；六单元页面全量清点无跳页（六报告 §1 F 行）。

## Phase 4 — DIM-S seed 数据走查（六单元逐单元）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ 六单元相关 seed（mnt equipment/schedule/request/downtime/visit 族（0930-2 批）+ M1.2b 批全量扩展 / aps 7 表（M1.4b 批，`docs/design/aps/seed-data.md`）/ logistics 8 表（M1.4b 批）/ notify `erp_sys_notification_template.csv`（27 行，0825 聚合先例）+ deploy 同步登记处 / md 主数据 21 表基座 CSV（org/currency/partner/material/subject…全域 FK 上游））；U20 common 层无独立 seed = `n-a` 记录理由
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数）
      - Skill: none
      - **实核 2026-09-09**：`TestErpSeedDataIntegrity` **4/0/0/0 全绿 BUILD SUCCESS**（exit 0）
- [x] <Proof> 六单元 seed 面核对：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` expect 空（只读审计零 seed 变更）+ 资产清点对账（对照 seed-data.md 对账表最新登记行 372 CSV + 1 SQL）+ deploy `_seed_*.sql` 同步义务逐命中查登记处表（notify 三方言命中 + cs 三方言命中，M1.10 先例口径）+ 六单元 seed 变更检测/双面快照重录义务抽查 + md 基座表变更影响面说明（全域 FK 上游）；U20 格 `n-a` 理由落注记
      - Skill: none
      - **实核 2026-09-09**：`_init-data` porcelain 空（零 seed 变更，双面快照重录义务零触发）；资产清点 **372 CSV + 1 SQL**（zz-sequence-advance.sql）= 冻结登记值；六单元 CSV 对账：erp_mnt_* 15 / erp_aps_* 7（M1.4b 批精确）/ erp_log_* 8（精确）/ erp_sys_notification_template 27 数据行实数（28 行含表头）/ erp_md_* 24（21 表基座批+后续批）/ common 无 seed；deploy `_seed_*.sql` 全仓命中 = notify 三方言 + cs 三方言共 6 处，seed-data.md §同步义务登记处 :107/:108 均「✅ 已聚合（2026-08-25）三方言 diff 零差异」逐字在案；md 基座变更影响面：全域 18 域 FK 上游（TestErpSeedDataIntegrity 4/4 门禁佐证健康度），本切片零变更；U20 格 n-a 理由：common 两模块无 deploy/sql 种子无 _init-data CSV 贡献（六报告 §1 S 行落注记）

Exit Criteria:

- [x] 六单元 DIM-S 格 verdict 全落盘（U20 为 n-a 带理由）；`TestErpSeedDataIntegrity` 全绿在案
- [x] seed 零变更 + 同步义务核对 + 六单元 seed 抽查结果在案

> 实核 2026-09-09：五格 pass + U20 n-a；4/0/0/0 全绿；porcelain 空 + 372 CSV + 1 SQL + deploy 三方言登记处在案 + 六单元 CSV 对账（15/7/8/27 行/24/n-a）。

## Phase 5 — DIM-T 单元测试走查（六单元逐单元）

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-maintenance/erp-mnt-service`、`module-aps/erp-aps-service`、`module-logistics/erp-log-service`、`module-notify/erp-notify-service`、`module-master-data/erp-md-service`、`module-common-service`（U20 回归面）+ `module-common-test`（U20 基座走查面；`<SVC>` 逐单元，同冻结清单 §1.4 记法）
> Prereqs: Phase 1 完成

- [x] <Proof> 切片本地回归：`mvn test -pl` 六单元逐模块全绿零失败（mnt/maintenance、aps/aps、log/logistics、notify/notify、md/master-data、common-service；数字落注记，对照锚点 mnt 157 / aps 82 / log 66 / notify 23 / md 160 / common 23 + 同批姊妹计划增量披露）
      - Skill: none
      - **实核 2026-09-09**：第一轮（Phase 5）exit 0：mnt **157**/aps **82**/log **66**/notify **23**/md **160**/common **23** 全绿零失败 = 锚点逐值一致零增量（同批姊妹 0547-1/0547-2 已收官无批内测试面增量）；第二轮（Phase 7 复跑）exit 0 BUILD SUCCESS 逐值同上（两次全绿）
- [x] <Proof> 覆盖缺口对账：`grep -rc "@BizQuery\|@BizMutation"` 公开方法清单 × `ls <SVC>/_cases/...` 测试目录清单逐项对账（六单元逐单元）；关键业务流清单核对——mnt 维护链 / aps P2 OperationOrder 排产 / log P2 Shipment / notify 通知派发集成测试（C16 族快照纪律）/ md 主数据夹具 `app-erp-test-data` 边界（测试资产 vs 部署资产）/ common-test 基座（BaseTestCase/初始化模式）对全域测试的一致性支撑（testing-strategy §关键业务流清单对应行）；缺口按业务关键度定级
      - Skill: none
      - **实核 2026-09-09**：六单元注解动作 × _cases 逐项对账落六报告 §1 T 行——mnt 27 测试类+22 用例根（维护链 TestErpMntDowntimeAndE2E 全链在位；缺口=maintenanceHistoryData/downtimeSummaryData 零引用→mnt-020-r3）；aps 17 类+74 用例（P2 排产链 Engine/Toc/AutoDispatch/StateGuards 13 边逐边在位；缺口=4 动作零引用→aps-016-r3）；log 14 类（P2 Shipment 链 7 测试类全在位，10/10 动作有引用零缺口）；notify 6 类（C16 族 TestErpSysNotificationCrossDomain 三用例在位；findRead 零引用归并 011-r3 注记）；md 31 类+14 用例根 626 文件（状态机 4 变体+DateRanges 42 在位；缺口=4 动作零引用→md-019-r3）；common-test 基座一致性支撑核验 pass（ThreadLocalFrozenClock/AbstractFrozenClockExtension 15 域复用/FaultInjectionStubs/PerfTiming/CoreMetricsSysCalendar，全域 61 文件消费 ServiceContextImpl 测试上下文）；md app-erp-test-data 边界=骨架占位 README 明示，实际夹具走 app-erp-all/_cases 用例自包含——测试资产 vs 部署资产分离成立
- [x] <Proof> 快照纪律：`grep -rn "SnapshotTest.RECORDING" <SVC>/src/test/java` 六单元 expect 0（提交态零残留）+ `delVersion`/decimal `*` 通配屏蔽合规抽查
      - Skill: none
      - **实核 2026-09-09**：六单元 src/test `SnapshotTest.RECORDING` 全部 **0 命中**（mnt/aps/log/notify/md/common-service/common-test 逐单元 grep 实跑）；快照用例抽查 _cases 目录 input/output/tables 成对齐备（notify 6 族 22 用例/aps 74 用例目录/md 626 文件）零残留录制态

Exit Criteria:

- [x] 六单元 DIM-T 格 verdict 全落盘；六单元本地回归全绿数字在案
- [x] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案

> 实核 2026-09-09：mnt/aps/md finding（覆盖缺口新立 4 条）+ log/notify/common pass；两轮 157/82/66/23/160/23 全绿；覆盖对账+关键业务流行核对+RECORDING 全零在案。

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + 六单元白名单条目核对
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，vs 冻结 SNAPSHOT 单向收紧）+ `--self-test` PASS；脚本红 = MI 回归，升级裁决报 MI 通道，不立 DIM-I finding
      - Skill: none
      - **实核 2026-09-09**：`--strict` **PASS exit 0**（0 新增违规 vs 冻结快照 170 baseline files；170 文件全部 removed-from-tree 改善项，totals CAT1..4=0/0/209/1318 快照口径）；`--self-test` **PASS exit 0**（含 CAT-4 scalar carrier/strict compare 注入用例全绿）
- [x] <Proof> 白名单合规核对：`docs/audits/cjk-baseline.md` §WHITELIST 六单元条目（mnt 1 + aps 2 + md 1 + common 2 = 6 条抽 ≥3 条核对四要素：文件路径/理由/owner doc 指针/裁决来源；log/notify 记录条目数 0）+ `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空（`_` 前缀生成 i18n yaml 禁手改）；四要素缺失 = 白名单登记缺陷 finding（非同类 CJK finding）
      - Skill: none
      - **实核 2026-09-09**：六单元白名单 **6 条全数核对四要素齐备**（mnt 1: ErpMntDashboardBizModel E3；aps 2: IErpApsOperationOrderBiz + ErpApsOperationOrderBizModel E3；md 1: ErpMdDashboardBizModel E3；common 2: ErpOrgIsolationQueryTransformer E3 + MaskHelper C2 角色名契约）——文件路径在位 + 理由实核（5 文件 @Description 各 1 行 + MaskHelper ROLE_ 常量族 8 命中）+ owner doc 指针（i18n-compliance.md 判定准绳表 #5/CAT-3 行）+ 裁决来源（plan 2026-09-07-1715-1 Phase 2/4）逐条在案，**6/6 全抽超 ≥3 义务**；log/notify 条目数 0 记录在案（六报告 §1 I 行）；`grep -L "@Locale"` 全量 *Errors.java = 空；meta/聚合 i18n porcelain 空（零手改）

Exit Criteria:

- [x] 六单元 DIM-I 格 verdict 全落盘；`--strict`/`--self-test` PASS 在案
- [x] 白名单四要素核对 + `@Locale` + meta 禁手改核对结果在案

> 实核 2026-09-09：六单元 DIM-I 全 pass（MI 先行口径零回归）；双 PASS exit 0；白名单 6/6 条四要素齐备（log/notify 0 条记录）；@Locale 空 + meta porcelain 空。

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/` 下六份报告（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（30 格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：逐条确认三态（复用/归并/新立）与级别（P0~P3）一致性与 ID 规范（`P{n}-CK-{mnt|aps|log|notify|md|common}-{NNN}-r3`，历史 ID 永不覆写；升级级别按 code-history-deferred-triangulation-audit-prompt §3.1）
      - Skill: code-quality-audit-prompt
      - **实核 2026-09-09**：27 条新立复裁决一致（mnt 020..023/aps 012..016/log 012..015/notify 009..011/md 015..020/common 011..015——各域编号承接 r1 最大号顺延零冲突零覆写；级别：6 P2（aps-012/log-012/notify-009/notify-010/md-015/common-011）+ 21 P3，P0/P1 = 0；三态计数 复用 6 + 归并 67 + 新立 27 = r1 全 73 条对账闭合）；ID 规范逐条核对 `-r3` 后缀 + 域短码；六报告 §3 统计表逐格对账（六报告五格 verdict：B 均 finding、mnt/log/notify/md F=finding 与 aps F=pass、U20 F/S=n-a、S/T/I 主 pass 详见各报告 §3）
- [x] <Add> 落盘六份报告 `ck-maintenance-r3.md`/`ck-aps-r3.md`/`ck-logistics-r3.md`/`ck-notify-r3.md`/`ck-master-data-r3.md`/`ck-common-r3.md`：各含五维覆盖矩阵（5 格 verdict 全落）+ finding 列表（含归属标注与归并指针）+ 统计（P0~P3 计数）+ 剩余风险声明（横切关注点 13 完整四件套）
      - Skill: none
      - **实核 2026-09-09**：六报告落盘执行目录（各含 §1 五维矩阵 5/5 + §2 三态裁决复用/归并/新立/归属标注/⑮记录 + §3 统计 + §4 剩余风险四件套已查/未深查/残留风险/successor）；30 格 verdict 完整（U20 DIM-F/S 显式 n-a 带理由归 U21）
- [x] <Add> 双索引同步：本轮 `ai-check-r3-index.md` 产物清单追加 6 行 + 跨轮 `docs/audits/check/ai-check-index.md` §报告清单追加 M1.15 六单元行 + §Finding 追踪追加新立 `-r3` ID 行（归并的追加证据至原 ID）
      - Skill: none
      - **实核 2026-09-09**：`ai-check-r3-index.md` +6 行（ck-maintenance/aps/logistics/notify/master-data/common-r3）；跨轮索引 §报告清单 +6 行 + §Finding 追踪 +27 行（6 P2 + 21 P3 全部 open 带修复方向）+ M1.15 六单元合并复核注记 1 条（复用 6/归并 67/新立 27/历史 73 ID 零覆写/双 handoff 落槽记录）
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空（roadmap 规则 6；触碰面 = 执行目录报告 + 索引 + 本计划勾选注记）
      - Skill: none
      - **实核 2026-09-09**：`git status --porcelain | grep -E "^.. (module-|app-erp-all/)"` = 空（生产路径零触碰）；全树脏面 = 2 M（双索引文档）+ 6 ??（六报告）+ 1 ??（本计划自身）——触碰面与计划声明一致
- [x] <Proof> 收尾回归：六单元 `mvn test -pl` 复跑全绿（审计只读不变式复证；全仓验证归收官机制）
      - Skill: none
      - **实核 2026-09-09**：第二轮 exit 0 BUILD SUCCESS：mnt 157/aps 82/log 66/notify 23/md 160/common 23 全绿零失败（与 Phase 5 第一轮逐值一致 = 审计期间生产面零漂移复证）

Exit Criteria:

- [x] 六份 `ck-*-r3.md` 落盘且各五维矩阵 5 格 verdict 完整（6 单元 × 5 格 = 30 格，缺一格不算完）
- [x] 双索引行追加在案；零生产代码改动核证通过；六单元回归全绿

> 实核 2026-09-09：六报告 30/30 格（U20 F/S 显式 n-a）；双索引 +6/+6/+27 行 + M1.15 复核注记；porcelain 过滤生产路径空；六单元两轮全绿 157/82/66/23/160/23。

## Draft Review Record

- dispatch review #review-2026-09-08-193051-mission-driver-2026-09-09-0547-3-m115-mnt-aps-log-notify-md-common-five-dim-audit-1-7c2e41b9 to 2026-09-08-193051-mission-driver
- 2026-09-09：iteration 1，共识 accept #review-2026-09-08-193051-mission-driver-2026-09-09-0547-3-m115-mnt-aps-log-notify-md-common-five-dim-audit-1-7c2e41b9（修正基线先例误述——M1.14/M1.11 为本批 active 同侪计划非已执行先例，执行先例锚定 M1.12/M1.13 + 15 份在盘报告；Phase 2 Targets `module-common_service` 路径笔误改 `module-common-service`；Phase 5 Targets 补 `module-common-service` U20 回归面（原仅列 `module-common-test`，与 Phase 5 自身 `mvn test -pl` common-service 项矛盾，冻结清单 §4 U20 = 双模块）；Closure Gates 按 M1.14 同批先例为本只读审计计划定制（ledger 格式门控以非复选框条目记录，30 格/双索引/零改动核证/独立结束审计门）；Verification/Closure 保持闭包时回执槽位与批内姊妹一致；基线断言逐一实仓复验在盘——HEAD `2c1c1ef25` 工作树零脏面、MI 终态行 4006/0/0/1 + CAT 0/0/0/0 + `--strict`/`--self-test` PASS（known-good-baselines 2026-09-08 行）、M0.3 checker 全表 11 值逐值一致、白名单 27 文件中六单元 6 条（mnt 1/aps 2/md 1/common 2，log/notify 0）实核 `cjk-baseline.md` §WHITELIST、六单元 service 回归锚点 157/82/66/23/160/23（cjk-baseline §批注账 MI.6 批 2 行）、seed-data.md 最新行 372 CSV+1 SQL、`TestErpSeedDataIntegrity` 实仓在盘、六单元 owner doc 全数在盘、r1 六报告统计与 r2 目录/跨轮索引 §Mission 基线快照在盘、冻结清单 §4 行 15 = U10+U15+U16+U11+U01+U20 ×五维 与 §1.5 MI 先行口径/§3.2 共享归属（common 抽象族→U20、notify 本体→U11）/§3.3 六单元焦点、§6 勘误 E1 路径 `app.action-auth.xml` 实仓在盘、`app-erp-test-data` 模块在盘、validate:flux 999/855/325 与姊妹切片行一致、roadmap M1.15 行范围/Skill「同上」链解析（= nop-platform-conformance-audit-prompt + code-quality-audit-prompt，承 M1.1 行）/横切关注点、执行目录 15 报告 = 12 切片实数核对；M0.6/MI.9 前置执行态属执行时依赖非计划缺陷，不阻塞本切片；frontmatter `status: draft` → `active`）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行；roadmap 收官机制承载全仓回归与 done 翻转）。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果。

- 范围内行为完成（Phase 1~7 全部执行项与退出标准 `[x]`；30 格 verdict 完整——六份 `ck-*-r3.md` 覆盖矩阵各 5/5，6 单元 × 5 格 = 30 格缺一格不算完；U20 DIM-F/S 为显式 n-a 带理由）
- 相关文档对齐（双索引同步在案——本轮 `ai-check-r3-index.md` 产物清单 6 行 + 跨轮 `ai-check-index.md` §报告清单 M1.15 六单元行与 §Finding 追踪新 `-r3` ID 行；历史 ID 零覆写；零生产代码改动 = owner doc 零对齐义务）
- 已运行验证：Phase 1 双 checker 红线（compliance checker 对照 M0.3 快照行 + CJK report mode 对照 0/0/0/0 终态）+ Phase 4 `TestErpSeedDataIntegrity` 全绿 + Phase 5/7 六单元 service `mvn test -pl` 两次全绿（对照锚点 mnt 157 / aps 82 / log 66 / notify 23 / md 160 / common 23）+ Phase 3 `npm run validate:flux` step [1/3] 导出 0 error + Phase 6 `--strict` PASS + `--self-test` PASS + Phase 7 零改动核证（`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空）——各命令数字落 Phase 勾选注记；任一红线漂移 = 登记裁决后再闭包，不就地吞掉
- 无范围内项目降级为 deferred/follow-up（finding 的「归属 fin-1/U20/U11/U21」归并标注与修复归 M2.x 是审计产出分流，非范围降级）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1）
- 文本一致性已验证：状态、阶段、门控和日志都一致（ledger 格式：frontmatter `status: active` 保持，完成态由全部勾选 + `## Verification` pass 线 + `## Closure` 回执派生；当日日志条目 `docs/logs/2026/09-09.md` 落盘）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Phase 勾选注记 + 六份 `ck-*-r3.md` + 双索引行 + `## Verification` pass 线 + `## Closure` 审计回执）

## Verification

> 执行时点：2026-09-09；HEAD `e331c55b2fc42196cdd008ca2bb45232c9046843`（脏面 = 本计划 + 本轮审计产物，tracked 零修改）。验证命令组 = 本只读审计计划的结果表面本身（Closure Gates 对照面）。

- PASS `bash docs/audits/nop-compliance-checker.sh` → exit 0；19 规则全表 = M0.3 快照行逐值一致零漂移（R1d 14 / R2a 34 / R2b 242 / R2c 1542 / R2d 38 / R3 5 / R6 2 / R10 14 / R12a 71 / R12b 66 / R12c 42，余 0）
- PASS `node tools/check-hardcoded-cjk.mjs`（report mode）→ exit 0；CAT-1..4 = 0/0/0/0（3430 java + 886 yaml 扫描）= MI 终态行一致零漂移
- PASS `node tools/check-hardcoded-cjk.mjs --strict` → exit 0（0 新增违规 vs 冻结快照 170 baseline files）
- PASS `node tools/check-hardcoded-cjk.mjs --self-test` → exit 0（PASS）
- PASS `npm run validate:flux` → step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；整体 855 validated / errors=325（全 variant 族既有外部漂移 successor 在案，非本切片 finding；非 variant ERR = 0；六单元命中 26/4/8/2/22/0 对账在案）
- PASS `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` → exit 0；Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
- PASS `mvn test -pl module-maintenance/erp-mnt-service,module-aps/erp-aps-service,module-logistics/erp-log-service,module-notify/erp-notify-service,module-master-data/erp-md-service,module-common-service`（第一轮 Phase 5）→ exit 0；mnt 157 / aps 82 / log 66 / notify 23 / md 160 / common 23 全绿零失败 = 锚点零增量
- PASS 同上（第二轮 Phase 7 复跑）→ exit 0 BUILD SUCCESS；逐值同上（两次全绿）
- PASS 零生产代码改动核证 → `git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 = 空；触碰面 = 执行目录六报告 + 双索引 + 本计划勾选注记
- PASS 覆盖完整性 → 六报告覆盖矩阵 30/30 格 verdict（U20 DIM-F/S 显式 n-a 带理由归 U21）；双索引同步（r3 索引 +6 行、跨轮索引 §报告清单 +6 行 + §Finding 追踪 +27 行 + M1.15 复核注记）；历史 73 ID 零覆写
- PASS 反模式机械族六单元全零（extends RuntimeException / @Inject private / System.currentTimeMillis / @Transactional∩@BizMutation / LocalDate.now）+ E1 勘误路径聚合器 x:extends 六单元注册在案 + `__XGEN_FORCE_OVERRIDE__` 全为 meta dict 校验点 + 各模块 porcelain 空
- pass test 2026-09-09-224252 exit=0

## Closure

- dispatch audit #audit-2026-09-09-224252-2026-09-09-0547-3-m115-mnt-aps-log-notify-md-common-five-dim-audit-1-3911864a to 2026-09-09-210030-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-2026-09-09-224252-2026-09-09-0547-3-m115-mnt-aps-log-notify-md-common-five-dim-audit-1-3911864a：独立闭包审计 ACCEPT——七阶段 35 项全勾，六报告 30 格 verdict/双索引同步/零生产改动核证实仓在盘；闭包 visit 实跑 `mvn clean install -DskipTests` BUILD SUCCESS（01:39 min）+ 全 reactor `mvn test` BUILD SUCCESS exit 0 零失败（13:19 min），`plan-check.mjs --strict` 绿，语义审计（Phase 勾选一致性/退出标准对实仓/反 hollow/deferred 诚实/文档同步）无残留
