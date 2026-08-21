package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtConsumptionLine;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.ct.biz.ErpCtConsumptionPeriodSummarizeResult;
import app.erp.ct.biz.ErpCtInvoicePlanGenerateItem;
import app.erp.notify.biz.IErpSysNotificationBiz;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * ErpCtConsumptionLine periodSummarize per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含消耗计费周期汇总编排（RC-R1.33 P1-RC-075，UC-CT-04 B/C/D）：
 * 期间内 Σ ConsumptionLine.quantity 对比合同行预估总量（line.quantity）→ 超量生成额外 InvoicePlan
 * （复用 {@link ErpCtInvoicePlanGenerateByTermProcessor} 内部能力，D6 超量金额推导）→
 * 复用 {@link ErpCtInvoicePlanTriggerInvoiceProcessor} 生成 AP/AR 发票草稿 →
 * Σ &gt; 预估 × 120% 经 {@link IErpSysNotificationBiz#notify} 派发超量审批通知（D5 契约，无 ACTIVE 模板静默跳过）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p><b>跨实体访问方式偏离说明</b>：期间汇总经 IDaoProvider 直查（对齐 triggerDuePlans Processor
 * 绕过 XMeta 查询算子白名单的既有范式——消费日期区间 le/ge 查询非 GraphQL 查询算子白名单面）。
 * 超量 InvoicePlan 生成与发票草稿触发分别委托同模块既有 Processor（生成面 + 触发面复用，零新过账逻辑，
 * O-4 豁免语义不变）。
 */
public class ErpCtConsumptionPeriodSummarizeProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    ErpCtInvoicePlanGenerateByTermProcessor generateByTermProcessor;

    @Inject
    ErpCtInvoicePlanTriggerInvoiceProcessor triggerInvoiceProcessor;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    public ErpCtConsumptionPeriodSummarizeResult periodSummarize(String contractLineId,
                                                                  LocalDate fromDate,
                                                                  LocalDate toDate,
                                                                  String invoiceTerm,
                                                                  LocalDate planDate,
                                                                  IServiceContext context) {
        assertPeriodValid(fromDate, toDate);

        ErpCtContractLine line = requireLine(contractLineId);
        ErpCtContract contract = line.getContract();
        assertContractActive(contract);

        BigDecimal total = sumConsumedQuantity(contractLineId, fromDate, toDate);
        BigDecimal estimated = nz(line.getQuantity());
        BigDecimal overQuantity = total.subtract(estimated);
        boolean overage = overQuantity.signum() > 0;

        ErpCtConsumptionPeriodSummarizeResult result = new ErpCtConsumptionPeriodSummarizeResult();
        result.setContractLineId(contractLineId);
        result.setEstimatedQuantity(estimated);
        result.setTotalConsumedQuantity(total);
        result.setOverQuantity(overage ? overQuantity : BigDecimal.ZERO);

        if (!overage) {
            return result;
        }

        // 超量金额推导（D6 选项 A）：overQuantity × line.unitPrice，scale 4 HALF_UP
        BigDecimal overAmount = overQuantity.multiply(nz(line.getUnitPrice()))
                .setScale(4, RoundingMode.HALF_UP);
        result.setOverAmount(overAmount);

        // 超量 InvoicePlan 生成（复用生成面内部能力：守卫/行归属/幂等查重一体复用）
        ErpCtInvoicePlanGenerateItem item = new ErpCtInvoicePlanGenerateItem();
        item.setContractLineId(contractLineId);
        item.setInvoiceTerm(invoiceTerm);
        item.setPlanDate(planDate);
        item.setAmount(overAmount);
        List<ErpCtInvoicePlan> created = generateByTermProcessor
                .generateInvoicePlansByTerm(contract.getId(), List.of(item), context);
        ErpCtInvoicePlan plan = created.isEmpty() ? null : created.get(0);
        if (plan != null) {
            result.setOveragePlanId(plan.getId());
            // 同事务内新实体为 NEW 态，triggerInvoice 的 dao().updateEntity 要求 DB 加载态
            // （update-entity-not-managed）——flush + evict 使后续 getEntityById 重新 DB 加载。
            ormTemplate.flushSession();
            ormTemplate.evict(plan);
            // 发票草稿触发（复用既有 triggerInvoice 路径：INBOUND→AP / OUTBOUND→AR + 回写 isInvoiced）
            ErpCtInvoicePlan triggered = triggerInvoiceProcessor.triggerInvoice(plan.getId(), context);
            result.setInvoiceBillCode(triggered.getInvoiceBillCode());
        }

        // 120% 超量通知（D5 契约）：Σ > 预估 × 1.2 → notify；无 ACTIVE 模板静默跳过（best-effort 不阻断）
        boolean overLimit = overRatioExceeds(estimated, total);
        result.setOverRatio(ratioOf(estimated, total));
        if (overLimit) {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("contractId", contract.getId());
            ctx.put("contractCode", contract.getCode());
            ctx.put("contractLineId", contractLineId);
            ctx.put("lineDescription", line.getDescription());
            ctx.put("estimatedQuantity", estimated);
            ctx.put("totalConsumedQuantity", total);
            ctx.put("overRatio", ratioOf(estimated, total));
            result.setNotificationSent(!notificationBiz.notify(
                    ErpCtConstants.NOTIFY_EVENT_CONSUMPTION_OVER_120, ctx, context).isEmpty());
        }
        return result;
    }

    // ---------- steps（protected，下游可经 Delta 覆盖） ----------

    protected void assertPeriodValid(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new NopException(ErpCtErrors.ERR_CT_CONSUMPTION_DATE_RANGE_INVALID)
                    .param(ErpCtErrors.ARG_FROM_DATE, fromDate)
                    .param(ErpCtErrors.ARG_TO_DATE, toDate);
        }
    }

    protected ErpCtContractLine requireLine(String contractLineId) {
        ErpCtContractLine line = contractLineDao().getEntityById(contractLineId);
        if (line == null) {
            throw new NopException(ErpCtErrors.ERR_CT_CONSUMPTION_LINE_NOT_FOUND)
                    .param(ErpCtErrors.ARG_CONTRACT_LINE_ID, contractLineId);
        }
        return line;
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

    protected BigDecimal sumConsumedQuantity(String contractLineId, LocalDate fromDate, LocalDate toDate) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("contractLineId", contractLineId));
        if (fromDate != null) {
            query.addFilter(ge("consumptionDate", fromDate));
        }
        if (toDate != null) {
            query.addFilter(le("consumptionDate", toDate));
        }
        // 经 dao() 直查绕过 XMeta 查询算子白名单（消费日期区间 le/ge 非 GraphQL 算子白名单面，
        // 对齐 triggerDuePlans Processor 直查范式——内部批量逻辑不经外部查询算子约束）。
        List<ErpCtConsumptionLine> lines = consumptionLineDao().findAllByQuery(query);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpCtConsumptionLine line : lines) {
            sum = sum.add(nz(line.getQuantity()));
        }
        return sum;
    }

    protected boolean overRatioExceeds(BigDecimal estimated, BigDecimal total) {
        if (estimated.signum() <= 0) {
            // 预估为 0 时任何消耗即视为超 120%（overRatio 语义不可除零）
            return total.signum() > 0;
        }
        return total.compareTo(estimated.multiply(new BigDecimal("1.2"))) > 0;
    }

    protected BigDecimal ratioOf(BigDecimal estimated, BigDecimal total) {
        if (estimated.signum() <= 0) {
            return null;
        }
        return total.divide(estimated, 4, RoundingMode.HALF_UP);
    }

    protected BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    protected IEntityDao<ErpCtConsumptionLine> consumptionLineDao() {
        return daoProvider.daoFor(ErpCtConsumptionLine.class);
    }

    protected IEntityDao<ErpCtContractLine> contractLineDao() {
        return daoProvider.daoFor(ErpCtContractLine.class);
    }
}
