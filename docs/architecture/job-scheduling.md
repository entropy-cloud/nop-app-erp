# 计划作业调度

## 目的

定义 nop-app-erp 的定时任务调度机制，使用 nop-job 组件实现 DAG 依赖的任务编排。

## 调度架构

```
nop-job（调度引擎）
    ├─ JobDefinition（作业定义）
    │      ├─ jobId
    │      ├─ jobName
    │      ├─ cronExpression
    │      ├─ jobType（JAVA/SCRIPT）
    │      └─ dependencies（前置作业列表）
    │
    └─ JobExecution（执行记录）
           ├─ executionId
           ├─ jobId
           ├─ startTime/endTime
           ├─ status（SUCCESS/FAILED/RUNNING）
           └─ errorMessage
```

## 标准作业

| 作业 | 调度 | 功能 |
|------|------|------|
| erp-fin-posting-scan | 每分钟 | 扫描 posted=false 单据触发过账 |
| erp-fin-period-close | 每月最后一天 22:00 | 触发期末结账流程 |
| erp-ast-depreciation | 每月 1 日 02:00 | 批量计提折旧 |
| erp-inv-stock-check | 每日 03:00 | 库存余额对账 |
| erp-md-data-sync | 每小时 | 主数据缓存刷新 |

## DAG 依赖

作业间支持依赖编排：

```
erp-fin-period-close
    ├─ erp-fin-posting-scan（必须先完成）
    ├─ erp-ast-depreciation（必须先完成）
    └─ erp-inv-stock-check（必须先完成）
```

## 告警

| 失败类型 | 告警方式 |
|----------|----------|
| 作业执行失败 | 站内消息 + 邮件 |
| 作业超时 | 站内消息 |
| 依赖作业失败 | 跳过后续作业 + 告警 |
