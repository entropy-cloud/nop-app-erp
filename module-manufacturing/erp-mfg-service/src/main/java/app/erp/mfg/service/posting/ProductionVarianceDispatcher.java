package app.erp.mfg.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.posting.ErpFinPostingErrors;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.costing.ProductionVarianceCalculator;
import app.erp.md.dao.AcctSchemaResolver;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.nop.api.core.beans.FilterBeans.eq;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpMfgCostVariance、ErpMfgWorkOrder）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
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
    public void dispatchIfApplicable(String workOrderId) {
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
            String voucherId = executor.postEvent(event);
            if (voucherId != null) {
                markPosted(lines);
            }
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("Production variance posting failed, work order {} remains posted=false: {}", wo.getCode(), e.getMessage());
            } else {
                LOG.error("Production variance posting error, work order {} remains posted=false", wo.getCode(), e);
            }
        }
    }

    /**
     * 红冲指定工单的既有 PRODUCTION_VARIANCE 凭证（若存在）。用于重算场景：在 {@code deleteByWorkOrder} 删数据行前
     * 调用，闭合「重算→红冲→新凭证」链路，避免数据行新金额 + GL 旧凭证金额数据分叉。
     *
     * <p>billHeadCode 派生对齐正向 {@link #buildEvent}（{@code wo.code + "-PV"}）；红冲经
     * {@link MfgPostingExecutor#reverse} → {@code IErpFinVoucherBiz.reverse}。
     *
     * <p>门控（P2-CK-mfg3-009，plan 2026-09-17-0800-1）：按「该工单存在差异行」判定（不再要求 posted=true）——
     * 红冲失败后重算重建的差异行均为 posted=false，旧门控会跳过红冲重试，与 fin 侧 post 幂等命中复合成
     * 「一次红冲失败后差异永久悬挂」。凭证存在性由 fin 侧 reverse 权威判定：无已过账凭证抛
     * {@code ERR_REVERSE_SOURCE_NOT_FOUND}（差异行为空 ⇒ 不可能有本类型凭证，凭证仅经
     * {@link #dispatchIfApplicable} 自差异行派生，故行存在性门控是其安全超集），经本地 catch 判定为良性放行。
     *
     * <p>异常处理分级（plan 2026-07-18-2251-1 范式 + P2-CK-mfg3-009 C2 裁决）：
     * <ul>
     *   <li>{@code ERR_REVERSE_SOURCE_NOT_FOUND}（无可红冲凭证）——良性，log info，返回 {@code true}；</li>
     *   <li>其他异常（期间锁定等真实红冲失败）——log warn 告警，返回 {@code false}。调用方须跳过
     *       {@link #dispatchIfApplicable}：旧凭证仍以旧金额未冲销占位时，fin 侧 post 幂等命中会返回旧凭证 ID，
     *       若照旧 markPosted 会把新金额差异行误标 posted=true（GL 与差异行金额分叉）。</li>
     * </ul>
     *
     * @return {@code true} = 重算链可继续派发新凭证（无可红冲凭证 / 红冲成功）；
     *         {@code false} = 真实红冲失败，派发段必须中止（新差异行保持 posted=false，下次重算自动重试红冲）
     */
    public boolean reverseIfExists(String workOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        q.setLimit(1);
        IEntityDao<ErpMfgCostVariance> dao = daoProvider.daoFor(ErpMfgCostVariance.class);
        if (dao.findAllByQuery(q).isEmpty()) {
            return true;
        }
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(workOrderId);
        if (wo == null) {
            return true;
        }
        String billHeadCode = wo.getCode() + "-PV";
        try {
            executor.reverse(billHeadCode, ErpFinBusinessType.PRODUCTION_VARIANCE);
            return true;
        } catch (Exception e) {
            if (isFinReverseSourceNotFound(e)) {
                LOG.info("Production variance reversal skipped (no posted voucher to reverse), work order {} billHeadCode={}",
                        wo.getCode(), billHeadCode);
                return true;
            }
            LOG.warn("Production variance reversal failed, work order {} billHeadCode={} — variance dispatch aborted, "
                    + "new variance lines remain posted=false until next recalculation retries the reversal: {}",
                    wo.getCode(), billHeadCode, e.getMessage());
            return false;
        }
    }

    /**
     * 红冲「无可冲销凭证」良性判定：fin 侧 reverse 对「全部凭证已红冲（isReversed 过滤后空集）或从未生成」
     * 抛 {@code ERR_REVERSE_SOURCE_NOT_FOUND}。遍历 cause 链兼容事务管理器/IoC 包装异常。
     * 供 mfg 域各红冲链共用（ProductionVariance / Subcontract F2.5 重入幂等）。
     */
    public static boolean isFinReverseSourceNotFound(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof NopException
                    && ErpFinPostingErrors.ERR_REVERSE_SOURCE_NOT_FOUND.getErrorCode().equals(((NopException) cur).getErrorCode())) {
                return true;
            }
        }
        return false;
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
    private String resolveAcctSchemaId(String orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
