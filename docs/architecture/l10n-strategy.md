# 本地化策略设计

## 目的

说明 ERP 系统本地化的架构策略，特别是中国本地化的设计。本地化模块独立可拔，不污染核心 ERP 实体，支持多地区本地化并行。

本文件是 `odoo-l10n-china.md` 调研结论的落地设计。

## 设计背景

### 调研发现

从 OCA/l10n-china 的调研中发现：

| 发现 | 说明 |
|------|------|
| 本地化应独立可拔 | 金税/发票/银行对账各自独立 addon，不内建在核心 ERP 实体里 |
| 模块隔离演进 | 本地化可单独演进，不污染通用进销存模型 |
| 版本分支隔离 | 按主版本分支隔离，保证兼容性 |

### 核心原则

1. **本地化模块独立**：不修改核心 ERP 实体，通过扩展/覆盖实现
2. **可装卸**：本地化模块可独立启用/停用
3. **多地区并行**：支持多地区本地化同时存在
4. **版本隔离**：本地化模块版本与核心版本解耦

## 本地化架构

### 模块结构

```
本地化模块结构
        │
        ├─► 核心 ERP（app-erp-*）
        │      ├─ 通用业务实体（不带本地化字段）
        │      ├─ 通用业务逻辑
        │      └─ 通用凭证模板
        │
        ├─► 本地化基座（app-erp-l10n-base）
        │      ├─ 本地化接口定义
        │      ├─ 本地化扩展点
        │      └─ 本地化注册机制
        │
        ├─► 中国本地化（app-erp-l10n-cn）
        │      ├─ 金税接口（l10n-cn-gtax）
        │      ├─ 增值税发票（l10n-cn-vat-invoice）
        │      ├─ 银行对账（l10n-cn-bank-reconciliation）
        │      ├─ 中国报表（l10n-cn-reports）
        │      └─ 中国科目表模板（l10n-cn-coa-template）
        │
        └─► 其他地区本地化（可选）
               ├─ app-erp-l10n-us（美国）
               ├─ app-erp-l10n-eu（欧盟）
               └─ app-erp-l10n-jp（日本）
```

### 本地化扩展点

本地化通过 nop 的 delta 机制实现扩展：

| 扩展方式 | 用途 | 示例 |
|----------|------|------|
| ORM 片段 | 扩展实体字段 | 中国发票增加"发票代码/发票号码"字段 |
| XMeta 扩展 | 扩展页面展示 | 中国发票页面增加金税按钮 |
| BizModel 覆盖 | 扩展业务逻辑 | 中国发票审核触发金税接口 |
| 凭证模板扩展 | 本地化凭证规则 | 中国增值税专用发票的进项税抵扣凭证 |

## 中国本地化模块

### 模块清单

| 模块 | 职责 | 依赖 |
|------|------|------|
| `l10n-cn-gtax` | 金税接口（开票/查询/作废） | l10n-cn-base |
| `l10n-cn-vat-invoice` | 增值税发票管理 | l10n-cn-gtax |
| `l10n-cn-bank-reconciliation` | 银行对账 | l10n-cn-base |
| `l10n-cn-reports` | 中国财务报表 | l10n-cn-base |
| `l10n-cn-coa-template` | 中国科目表模板 | l10n-cn-base |

### 金税接口（l10n-cn-gtax）

#### 功能范围

| 功能 | 说明 |
|------|------|
| 发票开具 | 向金税系统开具发票 |
| 发票查询 | 查询已开具发票状态 |
| 发票作废 | 作废已开具发票 |
| 红字发票 | 开具红字发票（退货/折让） |
| 发票下载 | 从金税系统下载发票信息 |

#### 接口设计

```java
public interface IGTaxService {
    /**
     * 开具发票
     */
    GTaxInvoiceResult issueInvoice(GTaxInvoiceRequest request);
    
    /**
     * 查询发票
     */
    GTaxInvoiceResult queryInvoice(String invoiceCode, String invoiceNumber);
    
    /**
     * 作废发票
     */
    GTaxInvoiceResult voidInvoice(String invoiceCode, String invoiceNumber);
    
    /**
     * 开具红字发票
     */
    GTaxInvoiceResult issueRedInvoice(GTaxRedInvoiceRequest request);
    
    /**
     * 下载发票
     */
    List<GTaxInvoiceInfo> downloadInvoices(GTaxDownloadRequest request);
}
```

#### 发票开具流程

```
金税开票流程
        │
        ├─► 步骤1：销售发票审核通过
        │      └─ 触发金税开票钩子
        │
        ├─► 步骤2：组装开票请求
        │      ├─ 购方信息（名称、税号、地址、电话、银行、账号）
        │      ├─ 商品明细（名称、规格、单位、数量、单价、金额、税率、税额）
        │      ├─ 发票类型（专用/普通）
        │      └─ 开票日期
        │
        ├─► 步骤3：调用金税接口
        │      ├─ 发送开票请求
        │      ├─ 接收开票结果
        │      └─ 记录发票代码/发票号码
        │
        ├─► 步骤4：更新销售发票
        │      ├─ 写入发票代码/发票号码
        │      ├─ 更新 gtaxStatus = ISSUED
        │      └─ 关联金税返回信息
        │
        └─► 步骤5：异常处理
               ├─ 开票失败：记录错误，允许重试
               └─ 金税系统异常：队列排队，定时重试
```

### 增值税发票（l10n-cn-vat-invoice）

#### 发票类型

| 类型 | 说明 | 税率 |
|------|------|------|
| 增值税专用发票 | 可抵扣进项税 | 13%/9%/6% |
| 增值税普通发票 | 不可抵扣 | 13%/9%/6% |
| 增值税电子发票 | 电子形式 | 13%/9%/6% |
| 机动车销售统一发票 | 机动车销售 | 13% |

#### 发票字段扩展

通过 ORM 片段扩展销售发票：

```xml
<!-- l10n-cn-vat-invoice.orm.xml -->
<orm>
    <entity name="ErpSalInvoice" extends="true">
        <column name="invoiceType" dict="l10n-cn/invoice-type"/>
        <column name="invoiceCode" type="String" length="12"/>
        <column name="invoiceNumber" type="String" length="8"/>
        <column name="gtaxStatus" dict="l10n-cn/gtax-status"/>
        <column name="gtaxMessage" type="String" length="500"/>
        <column name="buyerName" type="String" length="100"/>
        <column name="buyerTaxNo" type="String" length="20"/>
        <column name="buyerAddress" type="String" length="200"/>
        <column name="buyerPhone" type="String" length="50"/>
        <column name="buyerBank" type="String" length="100"/>
        <column name="buyerAccount" type="String" length="50"/>
    </entity>
</orm>
```

#### 进项税抵扣

采购发票的进项税抵扣机制：

```
进项税抵扣流程
        │
        ├─► 步骤1：采购发票录入
        │      ├─ 发票类型（专用发票）
        │      ├─ 税额字段
        │      └─ 发票代码/发票号码
        │
        ├─► 步骤2：发票认证
        │      ├─ 登录税务平台认证
        │      ├─ 认证通过后可抵扣
        │      └─ 认证状态记录
        │
        ├─► 步骤3：凭证生成
        │      ├─ 借：采购成本（不含税金额）
        │      ├─ 借：应交税费-应交增值税-进项税额（税额）
        │      └─ 贷：应付账款（含税金额）
        │
        └─► 步骤4：抵扣统计
               ├─ 进项税额汇总
               ├─ 销项税额汇总
               └─ 应交增值税 = 销项 - 进项
```

### 银行对账（l10n-cn-bank-reconciliation）

#### 功能范围

| 功能 | 说明 |
|------|------|
| 银行流水导入 | 导入银行对账单 |
| 自动匹配 | 按金额/日期自动匹配 ERP 收付款 |
| 手工匹配 | 手工确认匹配关系 |
| 未达账项 | 记录未匹配项 |
| 对账报告 | 生成银行存款余额调节表 |

#### 对账流程

```
银行对账流程
        │
        ├─► 步骤1：导入银行对账单
        │      ├─ 支持多种格式（Excel/CSV/XML）
        │      ├─ 解析银行流水
        │      └─ 写入 BankStatement 表
        │
        ├─► 步骤2：自动匹配
        │      ├─ 按金额匹配
        │      ├─ 按日期范围匹配
        │      ├─ 按往来单位匹配
        │      └─ 匹配规则可配置
        │
        ├─► 步骤3：手工匹配
        │      ├─ 显示未匹配银行流水
        │      ├─ 显示未核销收付款
        │      └─ 手工确认匹配关系
        │
        ├─► 步骤4：生成调节表
        │      ├─ 银行余额
        │      ├─ ERP 账面余额
        │      ├─ 未达账项
        │      └─ 调节后余额
        │
        └─► 步骤5：对账确认
               ├─ 对账日期锁定
               └─ 生成对账报告
```

### 中国报表（l10n-cn-reports）

#### 报表清单

| 报表 | 说明 | 周期 |
|------|------|------|
| 资产负债表 | 财务状况表 | 月/季/年 |
| 利润表 | 经营成果表 | 月/季/年 |
| 现金流量表 | 现金变动表 | 月/季/年 |
| 所有者权益变动表 | 权益变动表 | 年 |
| 增值税申报表 | 增值税纳税申报 | 月 |
| 企业所得税申报表 | 所得税纳税申报 | 季/年 |

#### 报表生成机制

```
报表生成流程
        │
        ├─► 步骤1：科目余额汇总
        │      ├─ 查询科目余额表
        │      ├─ 按科目层级汇总
        │      └─ 区分借贷方向
        │
        ├─► 步骤2：报表模板匹配
        │      ├─ 按报表类型匹配模板
        │      ├─ 科目→报表行映射
        │      └─ 计算公式执行
        │
        ├─► 步骤3：报表数据填充
        │      ├─ 本期金额
        │      ├─ 上期金额（对比）
        │      └─ 本年累计
        │
        └─► 步骤4：报表输出
               ├─ Excel 导出
               ├─ PDF 导出
               └─ 打印格式
```

### 中国科目表模板（l10n-cn-coa-template）

#### 科目表结构

中国会计准则科目表模板：

| 科目类别 | 一级科目示例 |
|----------|-------------|
| 资产类 | 1001 库存现金、1002 银行存款、1012 其他货币资金、1101 交易性金融资产、1121 应收票据、1122 应收账款... |
| 负债类 | 2001 短期借款、2201 应付票据、2202 应付账款、2203 预收账款、2211 应付职工薪酬、2221 应交税费... |
| 所有者权益类 | 4001 实收资本、4002 资本公积、4101 盈余公积、4103 本年利润、4104 利润分配... |
| 成本类 | 5001 生产成本、5101 制造费用、5201 劳务成本、5301 研发支出... |
| 损益类 | 6001 主营业务收入、6051 其他业务收入、6101 公允价值变动损益、6111 投资收益、6301 营业外收入... |

#### 科目模板导入

```xml
<!-- 科目模板导入配置 -->
<import-config>
    <entity name="ErpMdAccount">
        <source type="excel" path="l10n-cn-coa-template.xlsx"/>
        <mapping>
            <column name="accountCode" source="科目编码"/>
            <column name="accountName" source="科目名称"/>
            <column name="accountType" source="科目类别"/>
            <column name="parentAccountCode" source="上级科目"/>
            <column name="balanceDirection" source="余额方向"/>
        </mapping>
    </entity>
</import-config>
```

## 本地化注册机制

### 本地化提供者接口

```java
public interface IL10nProvider {
    /**
     * 本地化标识（如 cn/us/eu）
     */
    String getL10nCode();
    
    /**
     * 本地化名称
     */
    String getL10nName();
    
    /**
     * 提供的本地化模块
     */
    List<IL10nModule> getModules();
    
    /**
     * 科目表模板
     */
    IL10nCOATemplate getCOATemplate();
}
```

### 本地化模块接口

```java
public interface IL10nModule {
    /**
     * 模块标识
     */
    String getModuleCode();
    
    /**
     * 模块名称
     */
    String getModuleName();
    
    /**
     * ORM 片段路径
     */
    String getOrmFragmentPath();
    
    /**
     * XMeta 扩展路径
     */
    String getXMetaExtensionPath();
    
    /**
     * 凭证模板扩展
     */
    List<String> getVoucherTemplateCodes();
}
```

### 本地化注册

```java
@Component
public class L10nRegistry {
    @Inject
    private Map<String, IL10nProvider> providers; // key = l10nCode
    
    /**
     * 获取本地化提供者
     */
    public IL10nProvider getProvider(String l10nCode) {
        return providers.get(l10nCode);
    }
    
    /**
     * 获取所有本地化
     */
    public List<IL10nProvider> getAllProviders() {
        return new ArrayList<>(providers.values());
    }
}
```

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp.l10n.default` | cn | 默认本地化 |
| `erp.l10n.cn.gtax.enabled` | false | 是否启用金税接口 |
| `erp.l10n.cn.gtax.url` | — | 金税接口地址 |
| `erp.l10n.cn.gtax.timeout` | 30000 | 金税接口超时（毫秒） |
| `erp.l10n.cn.vat-invoice.auto-issue` | false | 发票审核后自动开票 |
| `erp.l10n.cn.bank.auto-match` | true | 银行对账自动匹配 |

## 开源参考

| 项目 | 参考维度 | 具体借鉴 |
|------|----------|----------|
| OCA/l10n-china | 本地化独立可拔 | 独立 addon 模式，不修改核心实体 |
| Odoo | 本地化模块隔离 | 按版本分支隔离本地化 |
| ERPNext | 本地化报表 | 区域报表模板机制 |
| 赤龙 | 中国凭证模板 | 中式复式记账凭证字规范 |