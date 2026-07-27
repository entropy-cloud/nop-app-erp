# 2026-07-24-0930-2-shared-dict-status-enum-unification F2 字典与状态枚举真相统一

> Plan Status: completed
> Last Reviewed: 2026-07-24
> Source: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F2（HIGH）+ §闭包前必须项 #3/#9（P0/P2）
> Related: `docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（Decision D1 共享常量接口先例，已落地 purchase/sales 2 域）、`docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md`（checker R3 基线门控，本计划命中数下降后更新基线）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-24）：

**F2(a) 字典文件跨域字节级重复**：
- `approve-status.dict.yaml` 在 **9 域**（ast/cs/fin/inv/mnt/mfg/pur/qa/sal）各有一份 `src/main/resources/_vfs/dict/erp-<域>/approve-status.dict.yaml`，9 份**字节级完全相同**（md5 唯一=1），值均为 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED
- **关键发现**：ORM 列实际引用的是 `ext:dict="wf/approve-status"`（平台标准审核状态字典）——45 处；inventory 5 处用 `erp-inv/approve-status`，cs 1 处用 `erp-cs/time-entry-approve-status`。9 份 per-domain `approve-status.dict.yaml` 中**仅 inventory 1 份被 ORM 引用**（5 处），其余 8 份（ast/cs/fin/mnt/mfg/pur/qa/sal）**未被任何 ORM 列引用**——运行时作为 dict key `erp-<域>/approve-status` 加载但无 ORM 消费者；多个 Java 文件（含 javadoc + 内联注释，含测试文件）提及 per-domain approve-status dict key，Phase 0 扫描将落盘权威清单，随移除一并清理避免 stale doc
- `doc-status.dict.yaml` 在 **7 域**（ast/cs/log/mnt/pur/qa/sal）各有一份；ORM 引用分散（pur 8/sal 7/ast 6/qa 4/mnt 2/cs 1/crm-lead 1）；**实测 7 域值集合完全相同**（均为 ACTIVE/CANCELLED/DRAFT 三值）——合并候选，但合并需统一 ORM `ext:dict` 引用（ORM 保护区域，见 Non-Goals 裁决）

**F2(b) Java 常量类重复**：38 个 `Erp*Constants.java` 重复声明同一组状态字面量（"UNSUBMITTED"/"APPROVED"/"DRAFT" 等）

**F2(c) D1 共享接口模式已落地 5/9 域**：
- **已有 `Erp*DocStatus` 接口**（dao 层，`Erp*Constants extends Erp*DocStatus` 保持向后兼容）：assets/finance/inventory/purchase/sales = **5 域**
- **缺失 `Erp*DocStatus`** 的 approve-status 域：cs/maintenance/manufacturing/quality = **4 域**

**F2(d) 跨域同字面量常量重复声明**：`"CANCELLED"` 在 drp/mfg/crm 以不同常量名重复（`ErpDrpConstants.SAL_DOC_STATUS_CANCELLED`/`ErpMfgConstants.SAL_DOC_STATUS_CANCELLED`/`ErpCrmConstants.DOC_STATUS_CANCELLED`）

**F2(e) drp 在 erp-inv 命名空间下放置 3 个 dict 文件**（`drp-service-level`/`drp-ss-method`/`drp-xdock-status`，文件名带 `drp-` 前缀未冒充 inv，但目录归属语义不明）

## Goals

1. **approve-status 字典去重**：移除 8 份未被 ORM 引用的冗余 per-domain `approve-status.dict.yaml`（消费者已指向 `wf/approve-status`；inventory 1 份保留因有 5 处 ORM 引用），消除字节级重复真相
2. **D1 共享接口全域推广**：为缺失的 4 域（cs/mnt/mfg/qa）创建 `Erp*DocStatus` dao 层接口，`Erp*Constants extends Erp*DocStatus`，消除状态字面量重复声明
3. **doc-status 策略裁决**：实测 7 域 doc-status 值集合相同（ACTIVE/CANCELLED/DRAFT），但合并需统一 ORM `ext:dict` 引用（ask-first 保护区域）；本计划仅**裁决 + 文档化合并候选**，不执行 ORM 变更（归 successor）
4. **drp 命名空间归属裁决**（F2e，P2）：裁决 `erp-inv/drp-*` 3 个 dict 文件归属（迁移到 `erp-drp/` 或登记例外）

## Non-Goals

- 全量消除 126 处 `"DRAFT"` / 73 处 `"APPROVED"` 硬编码字面量——本计划消除常量声明层重复（D1 接口），硬编码字面量替换为常量引用归后续重构计划（数量大且逐文件低风险）
- 修改 ORM 列的 `ext:dict` 引用（如将 `erp-inv/approve-status` 改为 `wf/approve-status`）——ORM 保护区域，需 ask-first；本计划仅移除无消费者的冗余 dict 文件 + 新增接口
- `app-erp-common-api` 共享内核抽取（F4）——归独立 successor

## Task Route

- Type: `architecture change`（dict 真相源变更 = 跨域共享层引入；非 ORM 模型变更，不触 ask-first 硬停止，但触及 dict 层需谨慎 + 全仓库验证）
- Owner Docs: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F2、`docs/architecture/system-baseline.md`（字段与类型约定）、`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（Decision D1 先例）
- Skill Selection Basis: `nop-backend-dev`（dao 层常量接口 + Constants extends 模式）；`nop-frontend-dev` 不适用（不改 view.xml）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. dict 文件变更后 `mvn clean install -DskipTests` 触发 meta 模块重新打包即可。

## Execution Plan

### Phase 0 — Explore：approve-status 无消费者确认 + doc-status 合并候选确认 + drp 归属裁决

Status: completed
Targets: 8 份冗余 `approve-status.dict.yaml` 消费者扫描 + 7 域 `doc-status.dict.yaml` 值矩阵 + drp 3 dict 文件
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无

- [x] Proof: 全仓扫描 `erp-*/approve-status` dict key 的所有消费者（ORM `ext:dict` + xmeta + view.xml + 代码字符串/注释引用）——确认 8 份（ast/cs/fin/mnt/mfg/pur/qa/sal）per-domain 文件确无 ORM 消费者（仅 wf/approve-status + erp-inv/approve-status 被引用），可安全移除；**落盘权威 stale 引用清单**（javadoc + 内联注释，含测试文件，精确到 file:line）供 Phase 1 逐条清理
      - Skill: `nop-backend-dev`
      - **消费者证据**：ORM `ext:dict` 引用扫描结果——`wf/approve-status` 88 处；`erp-inv/approve-status` 5 处（inv）；`erp-cs/time-entry-approve-status` 1 处（cs）；xmeta/view.xml/page.yaml 引用 **0 处**。8 份 per-domain approve-status.dict.yaml（ast/cs/fin/mnt/mfg/pur/qa/sal）均无 ORM 列消费者。
      - **权威 stale 引用清单（code，精确 file:line）**——Phase 1 逐条清理：
        1. `module-manufacturing/erp-mfg-service/.../ErpMfgConstants.java:36` — 内联注释 `erp-mfg/approve-status`
        2. `module-maintenance/erp-mnt-service/.../ErpMntConstants.java:9` — javadoc `erp-mnt/approve-status`
        3. `module-purchase/erp-pur-dao/.../constants/ErpPurDocStatus.java:7` — javadoc `erp-pur/approve-status`
        4. `module-quality/erp-qa-service/.../ErpQaConstants.java:36` — 内联注释 `erp-qa/approve-status`
        5. `module-quality/erp-qa-service/.../ErpQaConstants.java:121` — 内联注释 `erp-sal/approve-status`
        6. `module-quality/erp-qa-service/src/test/.../TestErpQaRecallLocateNotifyReturn.java:213` — 内联注释 `erp-sal/approve-status`
        7. `module-purchase/erp-pur-service/.../ErpPurConstants.java:7` — javadoc `erp-pur/approve-status`
        8. `module-finance/erp-fin-dao/.../constants/ErpFinDocStatus.java:7` — javadoc `erp-fin/approve-status`
        9. `module-cs/erp-cs-service/.../ErpCsConstants.java:41` — 内联注释 `erp-cs/approve-status`
        10. `module-assets/erp-ast-service/.../ErpAstConstants.java:9` — javadoc `erp-ast/approve-status`
        11. `module-assets/erp-ast-dao/.../constants/ErpAstDocStatus.java:7` — javadoc `erp-ast/approve-status`
        12. `module-sales/erp-sal-service/.../ErpSalConstants.java:7` — javadoc `erp-sal/approve-status`
        13. `module-sales/erp-sal-dao/.../constants/ErpSalDocStatus.java:7` — javadoc `erp-sal/approve-status`
- [x] Proof: 提取 7 域 `doc-status.dict.yaml` 值集合，确认跨域值矩阵（实测均为 ACTIVE/CANCELLED/DRAFT 三值相同）→ 合并候选确认，但合并需统一 ORM ext:dict（保护区域，本计划不执行）
      - Skill: `none`
      - **值矩阵**：pur/ast/qa/sal/log/cs/mnt 7 域 doc-status.dict.yaml 值集合完全相同 = {DRAFT, ACTIVE, CANCELLED}。
- [x] Decision: doc-status 合并策略裁决。(a) 记录 7 域值集合相同为合并候选；(b) 合并执行需统一 ORM `ext:dict` 引用到共享 dict key（属 ask-first ORM 保护区域）→ 裁决为**文档化合并候选 + Deferred successor**（触发：ORM ext:dict 统一授权）。本计划不执行 ORM 变更
      - Skill: `none`
      - **裁决：Deferred successor**（已记录于 §Deferred But Adjudicated）。7 域值集合相同（DRAFT/ACTIVE/CANCELLED），合并候选确认；合并需统一 ORM ext:dict 引用到共享 dict key，属 ask-first ORM 保护区域，本计划不执行。
- [x] Decision: drp 3 dict 文件（`erp-inv/drp-*`）归属裁决。(a) 迁移到 `erp-drp/` 目录（按拥有者归属）；(b) 保留在 `erp-inv/` 并在 drp owner doc 登记命名例外（按描述对象邻接归属）。记录裁决理由
      - Skill: `none`
      - **裁决：选择 (b) 登记命名例外**。理由：3 个 dict（`drp-service-level`/`drp-ss-method`/`drp-xdock-status`）的 ORM 定义（`module-drp/model/app-erp-drp.orm.xml`）+ 物理文件（`module-drp/erp-drp-meta/.../erp-inv/`）+ ORM 消费者（3 列）**全部归属 module-drp**，仅在 dict key 命名空间上挂 `erp-inv/`。迁移到 `erp-drp/` 需改 ORM `name=` + `ext:dict=`（3 dict 定义 + 3 列），触 Non-Goal "ORM ext:dict 引用变更保护区域"。物理归属已正确（文件在 module-drp 内），仅命名空间为历史遗留，登记例外即可消除歧义且零 ORM 风险。

Exit Criteria:

> Phase 0 产出可执行的去重/迁移裁决（含消费者证据 + 值矩阵 + doc-status Deferred 裁决），解除后续阶段阻塞。

- [x] approve-status 8 份无消费者证据已记录 + stale 引用权威清单（file:line）已落盘
- [x] doc-status 跨域值矩阵已确认（7 域相同）+ 合并候选 Deferred 裁决已记录
- [x] drp 归属裁决已记录

### Phase 1 — approve-status 字典去重 + javadoc 清理

Status: completed
Targets: 8 份冗余 `erp-*-meta/src/main/resources/_vfs/dict/erp-<域>/approve-status.dict.yaml` + Phase 0 落盘的 stale 引用清单（javdoc/内联注释 file:line）
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 0 确认 8 份文件确无 ORM 消费者

- [x] Fix: 移除 8 份未被 ORM 引用的冗余 `approve-status.dict.yaml`（ast/cs/fin/mnt/mfg/pur/qa/sal；消费者已指向 `wf/approve-status`）；保留 inventory 的 `erp-inv/approve-status`（5 处 ORM 引用）+ cs 的 `time-entry-approve-status`（1 处引用）
      - Skill: `nop-backend-dev`
      - **执行**：8 份文件已删除（ast/cs/fin/mnt/mfg/pur/qa/sal 各 1）；实测仅余 `module-inventory/erp-inv-meta/.../erp-inv/approve-status.dict.yaml`（5 处 ORM 引用保留）+ `module-cs/erp-cs-meta/.../erp-cs/time-entry-approve-status.dict.yaml`（1 处引用保留）。8 域 ORM 模型均无 `<dict name="erp-*/approve-status">` 定义（仅 inv/cs 有专属 approve-status dict 定义），删除的 per-domain dict 文件为非 ORM 生成产物（手动放置的重复真相），不会被 codegen 重新生成。
- [x] Fix: 按 Phase 0 落盘的 stale 引用清单，逐条清理指向已移除 per-domain approve-status dict key 的 javadoc/内联注释（更新为 `wf/approve-status` 引用，避免文档漂移）
      - Skill: `nop-backend-dev`
      - **执行**：Phase 0 落盘的 13 条权威 stale 引用清单（file:line）已逐条清理为 `wf/approve-status`——ErpMfgConstants/ErpMntConstants/ErpPurDocStatus/ErpQaConstants(×2)/TestErpQaRecallLocateNotifyReturn/ErpPurConstants/ErpFinDocStatus/ErpCsConstants/ErpAstConstants/ErpAstDocStatus/ErpSalConstants/ErpSalDocStatus。清单全清零。
- [x] Proof: 移除后全仓库 `mvn clean install -DskipTests` BUILD SUCCESS（154 模块，meta 重新打包）；确认无 dict 引用断裂（运行期 dict 注册无 missing）
      - Skill: `none`
      - **验证**：`mvn clean install -DskipTests` 全 154 模块 reactor BUILD SUCCESS；8 份被删 dict 均 0 ORM 消费者（Phase 0 证据），运行期 dict 注册无 missing（`wf/approve-status` 平台字典 + `erp-inv/approve-status` + `erp-cs/time-entry-approve-status` 均在位）。dict 引用无断裂。

Exit Criteria:

> Phase 1 消除 approve-status 8 份字节级重复真相 + stale 引用清单逐条清理，且无运行时断裂。

- [x] 8 份冗余 dict 文件已移除（inventory 的 `erp-inv/approve-status` 保留 + cs 的 `time-entry-approve-status` 保留）
- [x] Phase 0 stale 引用清单逐条清理完毕（清单全清零）
- [x] `mvn clean install -DskipTests` BUILD SUCCESS

### Phase 2 — D1 共享接口全域推广（4 缺失域）

Status: completed
Targets: cs/mnt/mfg/qa 各 `erp-*-dao/src/main/java/.../constants/Erp*DocStatus.java`（NEW）+ 对应 `Erp*Constants.java`（extends）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Item Types Note: Phase 2 is Add-heavy (interface creation + Constants extends)
- Prereqs: Phase 1 完成

- [x] Add: 为 cs/mnt/mfg/qa 4 域创建 `Erp*DocStatus` dao 层接口（对齐 `ErpPurDocStatus` 范式：approve-status 4 值 + 该域 doc-status 值集合 ACTIVE/CANCELLED/DRAFT），对应 `Erp*Constants extends Erp*DocStatus` 保持向后兼容
      - Skill: `nop-backend-dev`
      - **执行**：4 域 dao 层接口已创建——`ErpCsDocStatus`（approve-status 4 + doc-status 3）、`ErpMntDocStatus`（approve-status 4 + doc-status 3；mnt 有 erp-mnt/doc-status 字典 DRAFT/ACTIVE/CANCELLED）、`ErpMfgDocStatus`（仅 approve-status 4；**事实修正**：mfg 的 docStatus 列绑定域专属状态字典 work-order-status/issue-status/subcontract-status，无统一 doc-status 字典且无 ACTIVE 值，故仅承载审核轴避免引入死常量）、`ErpQaDocStatus`（approve-status 4 + doc-status 3）。4 域 `Erp*Constants extends Erp*DocStatus`，移除各自重复的 APPROVE_STATUS_* / DOC_STATUS_* 声明（cs/qa 移除 approve+doc 双轴；mnt 移除 approve 轴，doc 轴经继承新增；mfg 移除 approve 轴），向后兼容（常量经继承仍可经 `Erp*Constants.XXX` 访问）。
- [x] Proof: 4 域 dao 模块 `mvn compile` 通过（接口新增）+ `Erp*Constants extends Erp*DocStatus` 编译通过；抽样验证既有 5 域（assets/finance/inventory/purchase/sales）`Erp*DocStatus.` 常量引用已在 BizModel 在位（确认 D1 模式生效）；新 4 域的 BizModel 硬编码字面量→常量引用替换归 Deferred（Non-Goal）
      - Skill: `nop-backend-dev`
      - **验证**：4 域 service（含 dao -am）`mvn compile` BUILD SUCCESS；全 154 模块 `mvn clean install -DskipTests` BUILD SUCCESS；4 域 `mvn test` BUILD SUCCESS（cs/mnt/mfg/qa 共 95 tests, 0 failures）；既有 5 域 `Erp*DocStatus.` 常量引用在 23 个实体文件在位（D1 模式生效确认）。

Exit Criteria:

> Phase 2 消除 4 域状态字面量重复声明（D1 接口）；doc-status 合并因 ORM 保护区域 Deferred（Phase 0 已裁决）。

- [x] 4 域 `Erp*DocStatus` 接口存在 + `Erp*Constants extends` 编译通过
- [x] 既有 5 域 `Erp*DocStatus.` 常量引用抽样在位确认
- [x] `mvn clean install -DskipTests` BUILD SUCCESS（154 模块）

### Phase 3 — drp 命名空间归属执行 + 文档对齐 + 基线同步

Status: completed
Targets: drp 3 dict 文件（按 Phase 0 裁决迁移或登记）+ `docs/audits/2026-07-23-0000-architecture-governance-review.md`（F2 闭包项标注）+ `docs/audits/compliance-baseline.md`（R3 基线同步）
Skill: `none`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成；`docs/audits/compliance-baseline.md` 由 `2026-07-24-0930-1` 创建（若该计划未先落地，本项改为"创建或更新"基线文件并补 R3 计数）

- [x] Add: 按 Phase 0 drp 归属裁决执行（迁移到 `erp-drp/` 或在 `docs/design/drp/README.md` 登记命名例外）
      - Skill: `none`
      - **执行**：按 Phase 0 裁决（方案 b 登记命名例外），在 `docs/design/drp/README.md` 新增「命名例外登记（plan 2026-07-24-0930-2 §F2e 裁决）」小节，登记 3 个 dict（`drp-service-level`/`drp-ss-method`/`drp-xdock-status`）保留 `erp-inv/` 命名空间的裁决理由（ORM 定义+物理文件+消费者全归属 module-drp；迁移触 ORM ext:dict 保护区域）。实测 ORM 定义在 `module-drp/model/app-erp-drp.orm.xml`（L29/34/40）+ 3 列消费者（L228/229/282），登记准确。
- [x] Add: 治理审查 F2 闭包项 #3/#9 标注达成 + `docs/audits/compliance-baseline.md` R3 计数同步（若 new Erp*() 构造数因接口引入变化；文件由 0930-1 创建，本计划追加更新）
      - Skill: `none`
      - **执行**：闭包项 #9 标注 ✅ 裁决完成（登记例外）；闭包项 #3 标注 🔶 部分 Done（D1 全域 9 域 + approve-status 8 冗余去重 done；共享 dict 统一/inventory ORM ext:dict Deferred successor）。`compliance-baseline.md` 追加「R3 同步注记」——checker 实测 R3=19 不变（接口 extends 无 new Erp*() 构造），全 16 规则 ≤ 基线无回归。

Exit Criteria:

> Phase 3 收尾 drp 归属 + 文档对齐 + 基线同步。

- [x] drp 3 dict 文件归属已裁决并执行
- [x] 治理审查 F2 闭包项标注 + checker 基线同步

## Draft Review Record

- Independent draft review iteration 1: needs-revision (ses_07042546e) because BLOCKER "9 份未被 ORM 引用" 实为 8 份（inventory 5 处 ORM 引用）致 Phase 1 "移除 9 份" 与 "保留 inventory" 自相矛盾 + compliance-baseline.md 未存在未列 prereq + Phase 2 Proof 验证新域 Erp*DocStatus. 引用与 Non-Goal 矛盾 + doc-status 合并与 ORM Non-Goal 张力未声明 + javadoc 引用数 3 低估（实 12）+ doc-status 值臆测 IN_PROCESS
- Independent draft review iteration 2: needs-revision (ses_0703bac35) after 修正 8 份 + javadoc 清理 + doc-status Deferred + D1 Proof 改既有域验证——但发现 javadoc/stale 注释硬编码 "11" 实为 12（第 12 个文件 TestErpQaRecallLocateNotifyReturn.java:213）致 Exit Criteria 可误关
- Independent draft review iteration 3: accept (ses_07036b86b) after 移除全部硬编码计数改为 Phase 0 落盘权威 stale 引用清单（file:line）→ Phase 1 按清单逐条清理 → Exit "清单全清零" 消除计数漂移风险；MINORS 非阻塞（javadoc typo）

## Closure Gates

> 本计划触及 dict 层（运行时注册）+ dao 层接口新增，无 ORM 模型/ext:dict 引用变更（除非 Phase 2 裁决许可）。完整仓库验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ dict 注册无断裂 + 4 域 dao 编译。

- [x] 范围内行为完成（approve-status 8 份去重 + javadoc 清理 + D1 接口 4 域 + doc-status 合并候选 Deferred 裁决 + drp 归属）
- [x] 相关文档对齐（治理审查 F2 闭包项 #3/#9 verification checkpoint 达成 + compliance-baseline.md R3 同步）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + dict 注册无断裂验证
- [x] 无范围内项目降级为 deferred/follow-up（doc-status 合并/ORM ext:dict 统一/硬编码字面量替换均为 Goal/Non-Goal 明示的 adjudicated successor，非范围内工作被静默降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 硬编码状态字面量全量替换（126 处 "DRAFT" / 73 处 "APPROVED" 等）

- Classification: `optimization candidate`
- **Status: RELEASED（successor `docs/plans/2026-07-24-0605-2-hardcoded-status-literal-constant-convergence.md` 已完成；服务层全 9 域 doc/approve 轴字面量→`Erp*DocStatus` 常量引用，R3/R11 checker 基线无回归）**
- Why Not Blocking Closure: 本计划消除常量声明层重复（D1 接口）+ dict 文件层重复；硬编码字面量替换为常量引用是逐文件低风险重构，数量大，归后续计划
- Successor Required: `yes`（触发条件：D1 接口全域落地后，按域推进硬编码字面量→常量引用重构时）→ **已满足并执行完毕**

### ORM ext:dict 引用统一（approve-status/doc-status 共享 dict key）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ORM `ext:dict` 变更属 ask-first 保护区域。approve-status：inventory 5 处 + cs 1 处特化引用可保留（值相同，仅 dict key 不同）。doc-status：7 域值集合相同（ACTIVE/CANCELLED/DRAFT）为合并候选，但合并需统一 ORM ext:dict 到共享 dict key（保护区域）
- Successor Required: `yes`（触发条件：ORM 变更授权 + 强制统一 dict key 引用需求）
- **RELEASED（inventory approve-status 子项）by `2026-07-24-1600-1`**：触发条件已满足（ORM 变更人工批准 + 计划 §Approval & Execution Status）。inventory 5 列 `ext:dict="erp-inv/approve-status"` → `wf/approve-status` + 内联 `<dict name="erp-inv/approve-status">` 移除 + `erp-inv/approve-status.dict.yaml` 删除 + `ErpInvDaoConstants extends ErpInvDocStatus` 兼容；154 模块 BUILD SUCCESS + inv 114 测试 0 失败 + checker 零回归。
- **RELEASED（doc-status 子项）by `2026-07-26-0300-1`**：触发条件已满足（ORM 变更人工批准 + 计划 §Approval & Execution Status，路径 a 会话内确认）。6 域 28 列 `ext:dict="erp-<domain>/doc-status"` → `erp/doc-status` + 共享 dict 创建于 `module-common-service/src/main/resources/_vfs/dict/erp/doc-status.dict.yaml`（选址候选 (c)：6 域 service 均依赖 common-service）+ 6 处内联 `<dict>` 移除 + 6 份 per-domain YAML 删除 + logistics stale 清理（内联 dict + YAML + en i18n label）+ `ErpMntDaoConstants extends ErpMntDocStatus` 兼容（maintenance 13 处直接引用）；154 模块 BUILD SUCCESS + 6 域+logistics 7 service 测试 0 失败 + checker 零回归（R3=5 不变）。
- **cs `time-entry-approve-status`——永久裁决特化（Successor: no）**：值集合 PENDING/APPROVED/REJECTED ≠ wf/approve-status UNSUBMITTED/SUBMITTED/APPROVED/REJECTED，为合法特化非冗余，不统一

## Closure

Status Note: 全部 4 阶段执行完成（Phase 0 由前序会话完成，Phase 1/2/3 本次执行）。`mvn clean install -DskipTests` 全 154 模块 reactor BUILD SUCCESS；4 域 service `mvn test` 95 tests 0 failures；checker 全 16 规则 ≤ 基线无回归。独立结束审计（ses_06f82fc62ffez7wPSIGL5Rfpnj，新会话不复用执行者上下文）判定 **PASS**：全 4 phase deliverable 实仓核验在位，mfg 事实修正（docStatus 仅 approve-status 轴）判定 sound，无 anti-hollow / 无 silently-dropped scope。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_06f82fc62ffez7wPSIGL5Rfpnj（general，fresh context，不复用执行者上下文），2026-07-24
- Audit Scope: 五点一致性、Exit Criteria vs 实时代码、Anti-Hollow、Deferred honesty、Docs sync、mfg 事实修正判定
- Verdict: **PASS**（8 项核验全 ✓）
  - Phase 1：`find approve-status.dict.yaml` 仅返回 inventory 1 份；8 域 ORM 无 approve-status dict 定义（非生成产物）；13 stale 代码引用清零（`rg *.java` 0 匹配）
  - Phase 2：4 新 `Erp*DocStatus` 存在 + 4 `Erp*Constants extends`（无重复字段）；mfg 修正 sound（docStatus 绑 work-order/issue/subcontract-status，均无 ACTIVE）；既有 5 域 D1 引用在 23 实体文件在位
  - Phase 3：drp README 命名例外 + 治理审查 #3/#9 标注 + compliance-baseline R3 注记均到位
  - 独立复跑 `mvn compile -pl cs/mnt/mfg/qa-service -am` BUILD SUCCESS
  - Anti-Hollow：继承常量被生产代码消费（MrpReleaseService / ErpMfgSubcontractOrderProcessor 等），非空壳
  - Deferred Honesty：硬编码字面量替换 + ORM ext:dict 统一均 Successor Required: yes，doc-status 合并为 Goal 明示的 adjudicate-only，无静默降级

Follow-up:

- 硬编码状态字面量→常量引用重构（D1 接口全域落地后）
