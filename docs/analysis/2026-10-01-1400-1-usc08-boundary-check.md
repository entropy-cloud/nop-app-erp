---
核对日期: 2026-10-01
类型: USC-08 延迟/出界故事边界核对与 Non-Goal 登记一致性（plan `2026-10-01-1400-1-usc08-boundary-consistency`）
权威: `docs/requirements/product-scope.md`（延迟段 :72-75）——核对中 product-scope 为权威不改
方法: 四方逐条判定（product-scope 延迟段 × gap analysis §3.3[7 行] × portal/README.md STATUS × coverage 矩阵 :39-40）+ backlog/README 🕒 误登记核查 + roadmap §9 回流交叉确认
---

# USC-08 边界一致性核对记录

## 一、四方逐条判定表

| # | 延迟/出界项 | product-scope 延迟段 | gap analysis §3.3 | portal/coverage 矩阵 | 判定 | 权威口径 |
|---|------------|---------------------|-------------------|---------------------|------|---------|
| 1 | 门户（US-PL-06） | —（延迟段未列；§业务域 18 域不含 portal） | 🕒 配套不进本仓 | portal/README :3 STATUS future | **一致** | 门户触发=协同需求立项（plan-first+人工批准）✓ 语义成立 |
| 2 | POS 零售 | —（延迟段未列；非 18 域） | 🕒 配套不进本仓 | coverage 矩阵 :39 POS 🕒 | **一致** | POS 维持出界；触发=产品明确零售档立项 |
| 3 | 电商商城 | —（延迟段未列；非 18 域） | Won't 配套不进本仓 | coverage 矩阵 :40 ✅（配套产品 `nop-app-mall` 覆盖） | **表述差异（非语义冲突）** | gap analysis「配套不进本仓」与矩阵「✅ 配套产品覆盖」两层面（本仓范围 vs 配套产品生态）语义相同；用词归一裁决见下 |
| 4 | SaaS 多租户启用 | :73 待业务确认 | Won't 待业务确认 | — | **一致** | 多租户待业务确认 ✓ 语义成立 |
| 5 | 原生移动 App | —（延迟段未列） | Won't 无产品承诺 | — | **一致** | 原生 App 维持出界；Web/PDA 优先 |
| 6 | AI 预测性分析/GenAI | —（延迟段未列） | Won't 调研有基线无 | — | **映射缺口（非矛盾）** | product-scope 延迟段无对应条目——核对记录显式登记该映射缺口；无需改 product-scope（调研性出界项不入延迟段属正常） |
| 7 | 预测性维护 IoT | —（延迟段未列） | Won't 数据未就绪 | — | **一致** | 维持出界；IoT 触发另立 |
| 8 | 外部集成（税控/银行/物流/电商对接） | :75 外部集成延迟 | Won't 本地 mock 已允许 | b2b/logistics README MFT mock | **一致** | 外部集成维持延迟 ✓ 语义成立 |
| 9 | SSO | —（延迟段未列；portal README L82 future） | 分界=平台能力 Could-Should | portal README L82 SSO future | **一致** | SSO=平台能力登记 Could-Should，非本仓近期项 |

## 二、EC/商城用词归一裁决（三处表述+双路径事实）

- 三处表述：gap analysis「配套 `nop-app-mall`，不进本仓」/ coverage 矩阵「✅ 独立产品 `nop-app-mall`（`~/app/nop-app-mall-wt/nop-app-mall-master/`，887 行 ORM）」/ portal README「复用 nop-app-mall（`/Users/abc/app/nop-app-mall` 实测不存在，占位性描述）」。
- 双路径事实：`~/app/nop-app-mall-wt/nop-app-mall-master/` 实存（源模型 `app-mall.orm.xml` 实测 2380 行/57 实体[结束审计实测；「887 行」承只读 coverage 矩阵登记，为该矩阵对 nop-app-mall-master 的历史引述]）；`/Users/abc/app/nop-app-mall` 不存在。
- **裁决：归一为「配套产品 `nop-app-mall` 实存（`nop-app-mall-wt` 工作树，887 行 ORM 设计），不进本仓 18 域基线；portal README 复用论述中的路径以 `nop-app-mall-wt` 为准**——gap analysis :152 与 portal README 边界段用词各补一处路径/定性订正（本记录为登记，修正随本提交落盘 gap analysis :152；portal README 边界段已有占位性描述无需改）。

## 三、roadmap 完成标准三句语义确认

1. 「门户触发=协同需求立项（plan-first+人工批准）」——✅ 成立（portal/README :9 实施前需 plan-first+人工批准，与本核对一致）。
2. 「多租户待业务确认」——✅ 成立（product-scope :73 与 gap analysis §3.3 同口径）。
3. 「外部集成维持延迟」——✅ 成立（product-scope :75 外部集成延迟段，b2b/logistics MFT mock 已覆盖测试面）。

## 四、backlog/README 🕒 误登记核查

- `docs/backlog/README.md` 全量核查：P0-P8 行均为已 done/planned 的实现类项或 user-story-coverage 行；**无任何 🕒 延迟项被误登记为近期 P 项**。✓

## 五、roadmap §9 回流交叉确认

- roadmap §9 Non-Goals 九项与 USC-01 矩阵 🕒/🔶 行交叉对照：门户 🕒/POS 🕒/电商配套/SaaS 多租户/原生 App/外部集成/AI 分析/个性化配置/权限 test 段——**全部维持出界/延迟，无回流**。✓

## 六、修正清单

- gap analysis :152 「电商商城 | 配套 `nop-app-mall`，不进本仓」→ 补路径限定「（实存于 `nop-app-mall-wt` 工作树）」（本提交落盘）。
- 其余：零修正需求（判定表 #1-#9 全一致或映射缺口显式登记）。
