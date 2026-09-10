package app.erp.fin.service.intercompany;

import app.erp.fin.biz.IErpFinIntercompanyTransferBiz;
import app.erp.fin.dao.api.IErpFinTransferPriceResolver;
import app.erp.fin.dao.dto.TransferPriceResult;
import app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

import static app.erp.fin.service.ErpFinErrors.ERR_INTERCOMPANY_SAME_LEGAL_ENTITY;
import static app.erp.fin.service.ErpFinErrors.ERR_TRANSFER_PRICE_NOT_FOUND;
import static app.erp.fin.service.ErpFinErrors.ARG_FROM_ORG_ID;
import static app.erp.fin.service.ErpFinErrors.ARG_TO_ORG_ID;
import static app.erp.fin.service.ErpFinErrors.ARG_MATERIAL_ID;

/**
 * 跨法人内部交易凭证生成 SPI 实现（plan 2026-07-22-1000-1 A3，multi-company.md §跨公司交易生命周期状态机）。
 *
 * <p>跨法人判定信号：fromWarehouse.orgId / toWarehouse.orgId 沿 {@code ErpMdOrganization.parentId} 链向上走，
 * 首个 orgType=COMPANY 的节点即法人根。法人根不同 → 跨法人交易。
 *
 * <p>config-gated：{@code erp-fin.intercompany-posting-enabled}（默认 false，保护既有 inventory 测试零回归）。
 *
 * <p>权威：{@code docs/architecture/multi-company.md §组织模型 §跨公司交易生命周期状态机}。
 */
public class ErpFinIntercompanyTransferBizModel implements IErpFinIntercompanyTransferBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinIntercompanyTransferBizModel.class);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpFinTransferPriceResolver transferPriceResolver;
    @Inject
    IntercompanyVoucherGenerator intercompanyVoucherGenerator;

    @Override
    public List<String> onTransferConfirmed(String transferOrderId, String fromWarehouseId, String toWarehouseId,
                                         LocalDate businessDate, IServiceContext context) {
        // 兼容重载：无数量 → 空聚合（调用方走带数量重载；保持既有行为）。
        return onTransferConfirmed(transferOrderId, fromWarehouseId, toWarehouseId,
                java.util.Collections.emptyMap(), businessDate, context);
    }

    @Override
    public List<String> onTransferConfirmed(String transferOrderId, String fromWarehouseId, String toWarehouseId,
                                         Map<String, BigDecimal> qtyByMaterial, LocalDate businessDate,
                                         IServiceContext context) {
        if (!isIntercompanyPostingEnabled()) {
            return Collections.emptyList();
        }
        if (fromWarehouseId == null || toWarehouseId == null || fromWarehouseId.equals(toWarehouseId)) {
            return Collections.emptyList();
        }

        String fromOrgId = resolveWarehouseOrgId(fromWarehouseId);
        String toOrgId = resolveWarehouseOrgId(toWarehouseId);
        if (fromOrgId == null || toOrgId == null) {
            return Collections.emptyList();
        }

        String fromLegalId = resolveLegalEntityRoot(fromOrgId);
        String toLegalId = resolveLegalEntityRoot(toOrgId);
        if (fromLegalId == null || toLegalId == null) {
            return Collections.emptyList();
        }
        if (fromLegalId.equals(toLegalId)) {
            // 同法人调拨 → 仅库存移动，无凭证（既有行为不变）
            LOG.debug("intercompany skip (same legal entity): transferOrderId={} fromLegal={} toLegal={}",
                    transferOrderId, fromLegalId, toLegalId);
            return Collections.emptyList();
        }

        // P1-CK-fin4-003：Σ调拨行数量（跨物料聚合）参与凭证金额（amount = 单价 × Σ数量）。
        // 物料维度取首个正数量物料参与定价解析（使物料级定价规则可命中）。
        // 近似语义（登记）：多物料调拨单按「首物料单价 × 跨物料 Σ数量」入账（非逐物料 Σ price(m)×qty(m)），
        // 且首物料选取依赖调用方 map 顺序——逐物料精确计价为 successor（dual-agent review 2026-08-31 findings）。
        BigDecimal totalQty = BigDecimal.ZERO;
        String firstMaterialId = null;
        for (Map.Entry<String, BigDecimal> e : qtyByMaterial.entrySet()) {
            BigDecimal q = e.getValue() == null ? BigDecimal.ZERO : e.getValue();
            if (q.signum() > 0) {
                totalQty = totalQty.add(q);
                if (firstMaterialId == null) {
                    firstMaterialId = e.getKey();
                }
            }
        }
        if (totalQty.signum() <= 0) {
            // 无数量 → 保持既有行为（单价入账，避免行为回归）。
            totalQty = BigDecimal.ONE;
        }

        TransferPriceResult pricing = transferPriceResolver.resolvePrice(fromLegalId, toLegalId, firstMaterialId, businessDate);
        if (pricing == null || pricing.getUnitPrice() == null) {
            throw new NopException(ERR_TRANSFER_PRICE_NOT_FOUND)
                    .param(ARG_FROM_ORG_ID, fromLegalId)
                    .param(ARG_TO_ORG_ID, toLegalId)
                    .param(ARG_MATERIAL_ID, firstMaterialId);
        }

        String transferCode = resolveTransferCode(transferOrderId);
        String fromAcctSchemaId = resolveOrgAcctSchemaId(fromLegalId);
        String toAcctSchemaId = resolveOrgAcctSchemaId(toLegalId);
        String periodId = resolvePeriodId(businessDate);
        // P3-CK-fin4-024-r3 修复：币种经 AR 侧法人根账套本位币解析（修复前硬编码 "1"）
        String currencyId = resolveOrgCurrencyId(fromLegalId);
        // P1-CK-fin4-003：凭证金额 = 转移定价单价 × Σ数量（修复前仅按单价入账，N 倍失真）。
        BigDecimal amount = pricing.getUnitPrice().multiply(totalQty);

        return intercompanyVoucherGenerator.generatePairedVouchers(transferCode, fromLegalId, toLegalId,
                fromAcctSchemaId, toAcctSchemaId, periodId, currencyId, amount);
    }

    @Override
    public List<String> onTradeDocumentApproved(String docType, String docId, String docCode, String executingOrgId,
                                              BigDecimal amount, LocalDate businessDate, IServiceContext context) {
        if (!isIntercompanyPostingEnabled()) {
            return Collections.emptyList();
        }
        if (executingOrgId == null || amount == null || amount.signum() <= 0 || docCode == null || docCode.isEmpty()) {
            return Collections.emptyList();
        }

        String executingLegal = resolveLegalEntityRoot(executingOrgId);
        if (executingLegal == null) {
            return Collections.emptyList();
        }

        String counterpartyLegal = resolveCounterpartyLegalEntity(executingLegal, docType, businessDate);
        if (counterpartyLegal == null) {
            LOG.debug("intercompany trade-document skip (no counterparty pricing rule): docType={} docCode={} executingLegal={}",
                    docType, docCode, executingLegal);
            return Collections.emptyList();
        }
        if (executingLegal.equals(counterpartyLegal)) {
            LOG.debug("intercompany trade-document skip (same legal entity): docCode={} legal={}", docCode, executingLegal);
            return Collections.emptyList();
        }

        // AR/AP 方向固定：seller(fromOrg)=AR，buyer(toOrg)=AP（Decision C）
        String sellerLegal;
        String buyerLegal;
        if (ErpFinConstants.INTERCOMPANY_DOC_TYPE_PURCHASE_ORDER.equals(docType)) {
            // PO 执行方=买方，对手=卖方
            sellerLegal = counterpartyLegal;
            buyerLegal = executingLegal;
        } else {
            // SO 执行方=卖方，对手=买方
            sellerLegal = executingLegal;
            buyerLegal = counterpartyLegal;
        }

        String sellerAcctSchemaId = resolveOrgAcctSchemaId(sellerLegal);
        String buyerAcctSchemaId = resolveOrgAcctSchemaId(buyerLegal);
        String periodId = resolvePeriodId(businessDate);
        // P3-CK-fin4-024-r3 修复：币种经卖方（AR 侧）法人根账套本位币解析（修复前硬编码 "1"）
        String currencyId = resolveOrgCurrencyId(sellerLegal);

        return intercompanyVoucherGenerator.generatePairedVouchers(docCode, sellerLegal, buyerLegal,
                sellerAcctSchemaId, buyerAcctSchemaId, periodId, currencyId, amount);
    }

    @Override
    public List<String> onTradeDocumentReversed(String docType, String docId, String docCode, IServiceContext context) {
        if (!isIntercompanyPostingEnabled()) {
            return Collections.emptyList();
        }
        if (docCode == null || docCode.isEmpty()) {
            return Collections.emptyList();
        }
        return intercompanyVoucherGenerator.reverseIntercompany(docCode);
    }

    // ---------- 内部辅助 ----------

    /**
     * 经转移定价规则表反向查找对手方法人根（Decision B）。
     * PO（执行方=买方）：查 toOrgId=executingLegal 的活跃规则 → fromOrgId 为卖方对手。
     * SO（执行方=卖方）：查 fromOrgId=executingLegal 的活跃规则 → toOrgId 为买方对手。
     *
     * <p>不按 validFrom/validTo 过滤：intercompany 交易关系是稳定的（org A 与 org B 互为对手），
     * 有效期窗口仅影响转移定价金额解析（经 IErpFinTransferPriceResolver），不影响对手方关系存在性。
     */
    private String resolveCounterpartyLegalEntity(String executingLegalId, String docType, LocalDate businessDate) {
        IEntityDao<ErpFinIntercompanyTransferPrice> dao =
                daoProvider.daoFor(ErpFinIntercompanyTransferPrice.class);
        QueryBean q = new QueryBean();
        if (ErpFinConstants.INTERCOMPANY_DOC_TYPE_PURCHASE_ORDER.equals(docType)) {
            q.addFilter(eq("toOrgId", executingLegalId));
        } else {
            q.addFilter(eq("fromOrgId", executingLegalId));
        }
        q.addFilter(eq("isActive", Boolean.TRUE));
        q.setLimit(1);
        List<ErpFinIntercompanyTransferPrice> rules = dao.findAllByQuery(q);
        if (rules.isEmpty()) {
            return null;
        }
        ErpFinIntercompanyTransferPrice rule = rules.get(0);
        return ErpFinConstants.INTERCOMPANY_DOC_TYPE_PURCHASE_ORDER.equals(docType)
                ? rule.getFromOrgId() : rule.getToOrgId();
    }

    private boolean isIntercompanyPostingEnabled() {
        return Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_INTERCOMPANY_POSTING_ENABLED, Boolean.FALSE));
    }

    private String resolveWarehouseOrgId(String warehouseId) {
        ErpMdWarehouse wh = daoProvider.daoFor(ErpMdWarehouse.class).getEntityById(warehouseId);
        return wh == null ? null : wh.getOrgId();
    }

    /**
     * 沿 parentId 链向上找首个 orgType=COMPANY 的法人根。
     * 带环检测（visited set）防止脏数据导致死循环。
     */
    String resolveLegalEntityRoot(String orgId) {
        Map<String, Boolean> visited = new HashMap<>();
        String current = orgId;
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        while (current != null && visited.putIfAbsent(current, Boolean.TRUE) == null) {
            ErpMdOrganization org = dao.getEntityById(current);
            if (org == null) {
                return null;
            }
            if (ErpFinConstants.ORG_TYPE_COMPANY.equals(org.getOrgType())) {
                return current;
            }
            // 集团顶层无 COMPANY 时，退而认顶层组织为法人根（向后兼容单公司场景）
            if (ErpFinConstants.ORG_TYPE_GROUP.equals(org.getOrgType()) && org.getParentId() == null) {
                return current;
            }
            current = org.getParentId();
        }
        return null;
    }

    private String resolveTransferCode(String transferOrderId) {
        // 调拨单 code 业财回链（无法直接读 ErpInvTransferOrder 实体因跨模块，用 ID 兜底）
        return "TRANSFER-" + transferOrderId;
    }

    private String resolveOrgAcctSchemaId(String orgId) {
        // P3-CK-fin4-024-r3 修复：orgId → 法人根主账套真实解析（镜像 fin3-005 范式，
        // 经 AcctSchemaResolver 共享解析器）；无 ACTIVE 账套时回退 "1"（恒等部署行为逐字节不变）。
        // successor 注记（多账套精确解析）就此收敛。
        ErpMdAcctSchema schema = resolveOrgAcctSchema(orgId);
        return schema != null ? schema.getId() : "1";
    }

    /**
     * P3-CK-fin4-024-r3 修复：经账套本位币解析币种（镜像 fin3-005 范式）。
     * 取 AR 侧法人根（调出方/卖方）主账套 functionalCurrencyId；无账套或账套无本位币时回退 "1"。
     */
    private String resolveOrgCurrencyId(String orgId) {
        ErpMdAcctSchema schema = resolveOrgAcctSchema(orgId);
        return schema != null && schema.getFunctionalCurrencyId() != null
                ? schema.getFunctionalCurrencyId() : "1";
    }

    private ErpMdAcctSchema resolveOrgAcctSchema(String orgId) {
        return app.erp.md.dao.AcctSchemaResolver.resolvePrimarySchema(daoProvider, orgId);
    }

    private String resolvePeriodId(LocalDate businessDate) {
        if (businessDate == null) {
            return null;
        }
        try {
            IEntityDao<app.erp.fin.dao.entity.ErpFinAccountingPeriod> dao =
                    daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinAccountingPeriod.class);
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.le("startDate", businessDate));
            q.addFilter(io.nop.api.core.beans.FilterBeans.ge("endDate", businessDate));
            q.setLimit(1);
            List<app.erp.fin.dao.entity.ErpFinAccountingPeriod> list = dao.findAllByQuery(q);
            return list.isEmpty() ? null : list.get(0).getId();
        } catch (RuntimeException e) {
            LOG.debug("period lookup failed businessDate={}: {}", businessDate, e.getMessage());
            return null;
        }
    }
}
