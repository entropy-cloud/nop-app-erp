package app.erp.mfg.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.costing.ProductionVarianceCalculator;
import app.erp.md.dao.AcctSchemaResolver;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生产差异过账派发器（manufacturing 域侧独立 dispatcher，plan 2026-07-05-1838-2 §Phase 3 Decision）。
 *
 * <p>语义：在 {@link ProductionVarianceCalculator#calculateVariances} 计算完成后，按成本要素汇总净差异金额，
 * 组装 {@link PostingEvent} 经 {@link MfgPostingExecutor} 调用财务过账引擎，成功后回写 {@code ErpMfgCostVariance.posted=true}。
 *
 * <p>承接 PPV {@code InvPostingDispatcher.dispatchPurchasePriceVariance} 范式：差异计算 → 装配 PostingEvent →
 * 调 Facade post() → 成功置 posted=true。过账失败以 try/catch 吞异常告警，保持差异行 posted=false（不阻断差异计算结果）。
 *
 * <p>本类不复用 inventory 域 {@code InvPostingDispatcher}——生产差异属 manufacturing 结果面，跨域写 inventory
 * dispatcher 违反 DAG 内聚（inventory 不持有 manufacturing 差异语义）。
 *
 * <p>触发：本类不自带自动触发；由调用方（完工触发 / 手动入口）在差异计算后显式调用
 * {@link #dispatchIfApplicable}。
 */
public class ProductionVarianceDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ProductionVarianceDispatcher.class);

    @Inject
    MfgPostingExecutor executor;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    ProductionVarianceCalculator varianceCalculator;

    public void setExecutor(MfgPostingExecutor executor) {
        this.executor = executor;
    }

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setVarianceCalculator(ProductionVarianceCalculator varianceCalculator) {
        this.varianceCalculator = varianceCalculator;
    }

    /**
     * 派发指定工单的生产差异过账：按成本要素汇总净差异 → 调 {@link MfgPostingExecutor#postEvent} → 成功回写 posted=true。
     *
     * <p>过账失败不阻塞差异计算结果：以 try/catch 吞异常告警，保持 posted=false（对齐 PPV 范式）。
     */
    public void dispatchIfApplicable(Long workOrderId) {
        List<ErpMfgCostVariance> lines = varianceCalculator.findByWorkOrder(workOrderId);
        if (lines.isEmpty()) {
            return;
        }

        // 跳过已过账行（幂等：重算后旧行删除、新行 posted=false）
        boolean anyUnposted = lines.stream().anyMatch(l -> !Boolean.TRUE.equals(l.getPosted()));
        if (!anyUnposted) {
            return;
        }

        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(workOrderId);
        if (wo == null) {
            return;
        }

        // 按成本要素汇总净差异（同要素多类型行的 varianceAmount 求和）
        Map<String, BigDecimal> elementVariance = new LinkedHashMap<>();
        for (ErpMfgCostVariance line : lines) {
            String element = line.getCostElement();
            BigDecimal variance = nullToZero(line.getVarianceAmount());
            elementVariance.merge(element, variance, BigDecimal::add);
        }

        BigDecimal materialNet = elementVariance.getOrDefault(ErpMfgConstants.COST_ELEMENT_MATERIAL, BigDecimal.ZERO);
        BigDecimal laborNet = elementVariance.getOrDefault(ErpMfgConstants.COST_ELEMENT_LABOR, BigDecimal.ZERO);
        BigDecimal overheadNet = elementVariance.getOrDefault(ErpMfgConstants.COST_ELEMENT_OVERHEAD, BigDecimal.ZERO);
        BigDecimal subcontractNet = elementVariance.getOrDefault(ErpMfgConstants.COST_ELEMENT_SUBCONTRACT, BigDecimal.ZERO);

        // 全部为零则跳过过账（差异为零无需入账）
        if (materialNet.signum() == 0 && laborNet.signum() == 0
                && overheadNet.signum() == 0 && subcontractNet.signum() == 0) {
            return;
        }

        PostingEvent event = buildEvent(wo, materialNet, laborNet, overheadNet, subcontractNet);
        try {
            Long voucherId = executor.postEvent(event);
            if (voucherId != null) {
                markPosted(lines);
            }
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("生产差异过账失败，工单 {} 保持 posted=false：{}", wo.getCode(), e.getMessage());
            } else {
                LOG.error("生产差异过账异常，工单 {} 保持 posted=false", wo.getCode(), e);
            }
        }
    }

    private PostingEvent buildEvent(ErpMfgWorkOrder wo, BigDecimal materialNet, BigDecimal laborNet,
                                    BigDecimal overheadNet, BigDecimal subcontractNet) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.PRODUCTION_VARIANCE);
        event.setBillHeadCode(wo.getCode() + "-PV");
        event.setOrgId(wo.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(wo.getOrgId()));
        event.setCurrencyId(wo.getCurrencyId());
        event.setExchangeRate(BigDecimal.ONE);
        LocalDate voucherDate = wo.getBusinessDate() != null ? wo.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ProductionVarianceAcctDocProvider.KEY_MATERIAL_VARIANCE, materialNet.abs());
        billData.put(ProductionVarianceAcctDocProvider.KEY_LABOR_VARIANCE, laborNet.abs());
        billData.put(ProductionVarianceAcctDocProvider.KEY_OVERHEAD_VARIANCE, overheadNet.abs());
        billData.put(ProductionVarianceAcctDocProvider.KEY_SUBCONTRACT_VARIANCE, subcontractNet.abs());
        billData.put(ProductionVarianceAcctDocProvider.KEY_MATERIAL_DIRECTION,
                directionOf(materialNet));
        billData.put(ProductionVarianceAcctDocProvider.KEY_LABOR_DIRECTION,
                directionOf(laborNet));
        billData.put(ProductionVarianceAcctDocProvider.KEY_OVERHEAD_DIRECTION,
                directionOf(overheadNet));
        billData.put(ProductionVarianceAcctDocProvider.KEY_SUBCONTRACT_DIRECTION,
                directionOf(subcontractNet));
        billData.put(ProductionVarianceAcctDocProvider.KEY_WORKORDER_CODE, wo.getCode());
        event.setBillData(billData);
        return event;
    }

    /**
     * 净差异方向：>0（实际>标准，unfavorable）→ DEBIT；<0（实际<标准，favorable）→ CREDIT。
     */
    private String directionOf(BigDecimal netVariance) {
        return netVariance.signum() > 0
                ? ProductionVarianceAcctDocProvider.DIRECTION_DEBIT
                : ProductionVarianceAcctDocProvider.DIRECTION_CREDIT;
    }

    private void markPosted(List<ErpMfgCostVariance> lines) {
        IEntityDao<ErpMfgCostVariance> dao = daoProvider.daoFor(ErpMfgCostVariance.class);
        for (ErpMfgCostVariance line : lines) {
            line.setPosted(true);
            dao.updateEntity(line);
        }
    }

    /**
     * 解析工单所属组织的会计账套 ID。工单不持有 acctSchemaId（非财务实体），经 ErpMdAcctSchema 按组织取第一条 ACTIVE 账套。
     * 对齐 PPV 范式中 ledger.acctSchemaId 的来源——库存账套来自库存域移动单，生产差异无库存通道故经组织解析。
     */
    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
