# Lesson 05: Nop 失败诊断——日志优先、从后向前定位

> **来源**：2026-07-16 plan 2246-1 Phase 3 根因调查。跨 ≥10 份计划长期携带的「17 CRUD smoke `.cxd-Crud` 15s 渲染超时 = 预存环境问题」被证伪，真因为 `nop.err.xui.grid.col-not-prop` 生产缺陷阻断页面渲染。
> **适用场景**：Nop 应用任何 E2E/浏览器层失败、页面空白/不渲染、Playwright `locator.waitFor` 超时、GraphQL 请求失败、`mvn test` 后端绿但前端红。
> **失败模式**：把服务端页面构建错误误诊为「前端渲染慢/环境时序问题」，长期携带不根因。

## 核心论点

**Playwright 的超时报错只告诉你"等了什么"，不告诉你"为什么"。** 为什么几乎总在服务端。最快的定位路径是**查服务端日志，不是调前端等待参数**。

## 问题（本案）

`tests/e2e/crud/*.smoke.spec.ts` 中 13+ 域稳定失败，报错统一为：

```
TimeoutError: locator.waitFor: Timeout 15000ms exceeded.
> await table.waitFor({ state: 'visible', timeout: DEFAULT_LIST_TIMEOUT });
```

被登记为「`.cxd-Crud`/table DOM 15s 渲染超时 = 预存环境问题」，fresh-DB+fresh-server 隔离仍复现，跨 ≥10 份计划无人根因。

## 根因（经干净 server 实证）

`CrudListPage.waitForList()` 等待 `.cxd-Crud, .cxd-Table, table` 可见。失败域的页面因 **view.xml grid 列引用了不存在的实体属性**，服务端在 `PageProvider__getPage` → `loadComponentModel` 阶段抛 `nop.err.xui.grid.col-not-prop`，页面根本没构建出 grid，AMIS 的 `.cxd-Crud` 永不出现 → 15s 超时。**不是时序问题，是生产缺陷。**

完整因果链（干净 server 复现 cs 域）：

```
cs smoke → goto /ErpCsTicket-main
  → 加载 /erp/cs/pages/ErpCsTicket/main.page.yaml
  → 传递加载 ErpCsTicketType.view.xml
  → grid[list] 列 slaPolicyId 非法 → nop.err.xui.grid.col-not-prop
  → loadComponentModel 失败 → NopEvalException(invoke-method-fail)
  → PageProvider__getPage GraphQL 失败
  → AMIS 不渲染 grid → .cxd-Crud 缺失 → waitForList() 15s 超时
```

## 正确做法：日志优先六步法

### 1. 不信表面症状
`locator.waitFor Timeout` / "渲染超时" / 页面空白——这些都是**果**，不是**因**。先假设因在服务端。

### 2. 起一个自己控制的干净 server，日志重定向到文件
**不要复用遗留/共享 server**——它可能已降级或已死（本案一个遗留 server 跑到一半进程消失，导致 18 个用例全挂在 `goto('/')`，制造了误导性二次症状）。自己起、确认健康再测：

```bash
rm -f db/erp.mv.db db/erp.trace.db
nohup java <E2E flags> -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar \
  > /tmp/erp-fresh.log 2>&1 &
# 轮询健康度，HTTP 200 才继续
for i in $(seq 1 30); do
  c=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8011/ 2>/dev/null)
  [ "$c" = "200" ] && { echo "ready"; break; }
  sleep 2
done
```

### 3. 跑**最小**复现——单用例，或 pass+fail 对照对
最小信号最干净。对照对（如 master-data 通过 vs cs 失败）能立刻隔离差异：

```bash
BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 \
  npx playwright test tests/e2e/crud/cs.smoke.spec.ts tests/e2e/crud/master-data.smoke.spec.ts
```

### 4. 从后向前查服务端日志（关键）
你刚触发的错误就是日志里**最近**的。`tail` 倒着看，找 `errorCode=` / `NopException` / `ERROR` / `Caused by:`：

```bash
rg "col-not-prop|errorCode=|NopException|ERROR" /tmp/erp-fresh.log | tail -30
```

### 5. 提取因果链——读 Nop 结构化错误
Nop 日志是结构化的，一条 NopException 行含定位到**源文件+行列**的 `@_loc`：

```
... NopException[seq=3,errorCode=nop.err.xui.grid.col-not-prop,
   params={gridId=list, colId=slaPolicyId},
   desc=表格[list]的列[slaPolicyId]不是已定义的实体属性]
   @_loc=[11:18:0:0]/erp/cs/pages/ErpCsTicketType/ErpCsTicketType.view.xml
```

- `errorCode=` → 错误类别（查 `nop-entropy/docs-for-ai/04-reference/debugging-checklist.md` 速查表）
- `@_loc=[行:列...]/path` → **精确源文件 + 行号**，直接打开修
- `params={...}` → 上下文（如 colId=slaPolicyId 指明是哪一列）
- `Caused by:` → 顺链追到根因（外层常是 `NopEvalException`/`invoke-method-fail`，内层才是真因）

### 6. 用工具自动化 4-5 步
人工 grep 多次重复易漏。本项目 `tools/parse-nop-errors.mjs` 解析日志、去重、按频次/时间输出错误表：

```bash
node tools/parse-nop-errors.mjs /tmp/erp-fresh.log              # 按频次（最常坏的最先看）
node tools/parse-nop-errors.mjs /tmp/erp-fresh.log --recent     # 按时间倒序（刚发生的）
node tools/parse-nop-errors.mjs /tmp/erp-fresh.log --grep col-not-prop
tail -n 5000 /tmp/erp-fresh.log | node tools/parse-nop-errors.mjs   # 也可走 stdin
```

## 反模式

- ❌ 看到"渲染超时"就加 `waitForTimeout` / 调大 timeout / 改等待条件——治果不治因
- ❌ 信遗留/共享 server 的结果——它可能已死或降级，制造假症状
- ❌ 把跨多份计划携带的失败默认成"环境问题"而不查日志——**长期携带的失败几乎都是未诊断的真实缺陷**
- ❌ 只看 GraphQL `end-graphql-request` 行的 `errorCode`——它常是外层 `invoke-method-fail`，要看 `Caused by:` 内层的 `@_loc`
- ❌ 后端 `mvn test` 全绿就假设前端必绿——view.xml/xmeta 层错误后端单测覆盖不到（本案 1497 后端测试全绿，但 13 页面渲染崩溃）

## 检查清单

- [ ] 复现是否在**自己起的干净 server**上（非遗留），且已 curl 确认 HTTP 200
- [ ] 是否跑了最小复现（单用例 / pass+fail 对照对），而非整批
- [ ] 是否查了服务端日志的 `errorCode=` / `NopException` / `Caused by:`，而非只看 Playwright 报错
- [ ] 是否提取了 `@_loc=[行:列]/path` 并打开该源文件行
- [ ] 是否对"长期携带的环境问题"失败保持怀疑、追到真实 errorCode

## 关联

- 工具：`tools/parse-nop-errors.mjs`（`tools/package.json` 的 `parse:nop-errors` 脚本）
- 平台速查：`nop-entropy/docs-for-ai/04-reference/debugging-checklist.md`
- 技能：`nop-debugging`（Iron Law：先根因后修复；condition-based-waiting 替任意 timeout）
- 本案计划：`docs/plans/2026-07-15-2246-1-fk-display-name-e2e-regression.md` Phase 3
