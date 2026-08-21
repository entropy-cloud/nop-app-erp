package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.ct.biz.ErpCtInvoicePlanGenerateItem;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCtInvoicePlan generateInvoicePlansByTerm per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含按生成项批量生成 InvoicePlan 编排（RC-R1.33 P1-RC-074，UC-CT-03 A）：
 * 合同 ACTIVE 守卫 → 行归属校验 → 幂等查重（contractLineId+invoiceTerm+planDate）→ 批量落库。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>守卫语义对齐 {@link ErpCtInvoicePlanTriggerInvoiceProcessor}（SUSPENDED 专属
 * {@code ERR_CT_CONTRACT_SUSPENDED} + 通用非 ACTIVE {@code ERR_CT_CONTRACT_NOT_ACTIVE}）。
 * 行归属校验经 IDaoProvider 直查（同域实体行归属判定，对齐 triggerInvoice Processor 直查范式）。
 */
public class ErpCtInvoicePlanGenerateByTermProcessor {

    @Inject
    IDaoProvider daoProvider;

    public List<ErpCtInvoicePlan> generateInvoicePlansByTerm(String contractId,
                                                             List<ErpCtInvoicePlanGenerateItem> items,
                                                             IServiceContext context) {
        ErpCtContract contract = requireContract(contractId);
        assertContractActive(contract);

        List<ErpCtInvoicePlan> created = new ArrayList<>();
        for (ErpCtInvoicePlanGenerateItem item : items) {
            assertLineBelongsTo(contractId, item.getContractLineId());
            assertNotDuplicate(item);
            created.add(createPlan(item));
        }
        return created;
    }

    // ---------- steps（protected，下游可经 Delta 覆盖） ----------

    protected ErpCtContract requireContract(String contractId) {
        ErpCtContract contract = dao().daoFor(ErpCtContract.class).getEntityById(contractId);
        if (contract == null) {
            throw new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCtErrors.ARG_CONTRACT_ID, contractId);
        }
        return contract;
    }

    protected void assertContractActive(ErpCtContract contract) {
        String status = contract.getStatus();
        if (Objects.equals(status, ErpCtConstants.CONTRACT_STATUS_SUSPENDED)) {
            throw new NopException(ErpCtErrors.ERR_CT_CONTRACT_SUSPENDED)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode());
        }
        if (!Objects.equals(status, ErpCtConstants.CONTRACT_STATUS_ACTIVE)) {
            throw new NopException(ErpCtErrors.ERR_CT_CONTRACT_NOT_ACTIVE)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, status);
        }
    }

    protected void assertLineBelongsTo(String contractId, String contractLineId) {
        ErpCtContractLine line = contractLineDao().getEntityById(contractLineId);
        if (line == null || !Objects.equals(line.getContractId(), contractId)) {
            throw new NopException(ErpCtErrors.ERR_CT_INVOICE_PLAN_LINE_NOT_IN_CONTRACT)
                    .param(ErpCtErrors.ARG_CONTRACT_LINE_ID, contractLineId)
                    .param(ErpCtErrors.ARG_CONTRACT_ID, contractId);
        }
    }

    protected void assertNotDuplicate(ErpCtInvoicePlanGenerateItem item) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("contractLineId", item.getContractLineId()));
        query.addFilter(eq("invoiceTerm", item.getInvoiceTerm()));
        query.addFilter(eq("planDate", item.getPlanDate()));
        if (!planDao().findAllByQuery(query).isEmpty()) {
            throw new NopException(ErpCtErrors.ERR_CT_INVOICE_PLAN_DUPLICATE)
                    .param(ErpCtErrors.ARG_CONTRACT_LINE_ID, item.getContractLineId())
                    .param(ErpCtErrors.ARG_INVOICE_TERM, item.getInvoiceTerm())
                    .param(ErpCtErrors.ARG_PLAN_DATE, item.getPlanDate());
        }
    }

    protected ErpCtInvoicePlan createPlan(ErpCtInvoicePlanGenerateItem item) {
        ErpCtInvoicePlan plan = planDao().newEntity();
        plan.setContractLineId(item.getContractLineId());
        plan.setInvoiceTerm(item.getInvoiceTerm());
        plan.setPlanDate(item.getPlanDate());
        plan.setAmount(item.getAmount());
        plan.setIsInvoiced(false);
        planDao().saveEntity(plan);
        return plan;
    }

    // ---------- helpers ----------

    protected IEntityDao<ErpCtInvoicePlan> planDao() {
        return daoProvider.daoFor(ErpCtInvoicePlan.class);
    }

    protected IEntityDao<ErpCtContractLine> contractLineDao() {
        return daoProvider.daoFor(ErpCtContractLine.class);
    }

    protected IDaoProvider dao() {
        return daoProvider;
    }
}
