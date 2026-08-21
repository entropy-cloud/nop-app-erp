package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.ct.service.ErpCtConfigs;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * ErpCtInvoicePlan triggerDuePlans per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含到期计划批量触发生成发票草稿编排（config-gated + 逐 plan 委托 triggerInvoice）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtInvoicePlanTriggerDuePlansProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpCtInvoicePlanTriggerInvoiceProcessor triggerInvoiceProcessor;

    public int triggerDuePlans(String contractId, LocalDate asOfDate, IServiceContext context) {
        // config-gated：erp-ct.invoiceplan-auto-trigger 默认 true
        if (!AppConfig.var(ErpCtConfigs.CFG_INVOICEPLAN_AUTO_TRIGGER, true)) {
            return 0;
        }
        QueryBean query = new QueryBean();
        query.addFilter(le("planDate", asOfDate));
        query.addFilter(eq("isInvoiced", false));
        // 经 dao() 直查绕过 XMeta 查询算子白名单（planDate 仅允许 [eq,in,dateBetween,dateTimeBetween]，
        // 不支持 le；findList 会经 meta 安全层校验报错）。对齐同模块 loadAccruedBillCodes /
        // findPeriodInvoices 经 daoProvider 直查的范式——内部批量逻辑不经外部 GraphQL 查询算子约束。
        List<ErpCtInvoicePlan> due = daoProvider.daoFor(ErpCtInvoicePlan.class).findAllByQuery(query);
        int triggered = 0;
        for (ErpCtInvoicePlan plan : due) {
            // 里程碑/完工条款需人工/上游事件确认；triggerInvoice 单点入口校验合同 ACTIVE
            ErpCtContractLine line = plan.getContractLine();
            if (line == null || line.getContractId() == null
                    || !Objects.equals(line.getContractId(), contractId)) {
                continue;
            }
            triggerInvoiceProcessor.triggerInvoice(plan.getId(), context);
            triggered++;
        }
        return triggered;
    }

    protected IEntityDao<ErpCtInvoicePlan> dao() {
        return daoProvider.daoFor(ErpCtInvoicePlan.class);
    }
}
