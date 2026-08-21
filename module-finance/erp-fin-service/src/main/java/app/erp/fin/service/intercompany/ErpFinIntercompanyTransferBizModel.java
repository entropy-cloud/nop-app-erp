package app.erp.fin.service.intercompany;

import app.erp.fin.biz.IErpFinIntercompanyTransferBiz;
import app.erp.fin.dao.api.IErpFinTransferPriceResolver;
import app.erp.fin.dao.dto.TransferPriceResult;
import app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice;
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

        TransferPriceResult pricing = transferPriceResolver.resolvePrice(fromLegalId, toLegalId, null, businessDate);
        if (pricing == null || pricing.getUnitPrice() == null) {
            throw new NopException(ERR_TRANSFER_PRICE_NOT_FOUND)
                    .param(ARG_FROM_ORG_ID, fromLegalId)
                    .param(ARG_TO_ORG_ID, toLegalId)
                    .param(ARG_MATERIAL_ID, (Object) null);
        }

        String transferCode = resolveTransferCode(transferOrderId);
        String fromAcctSchemaId = resolveOrgAcctSchemaId(fromLegalId);
        String toAcctSchemaId = resolveOrgAcctSchemaId(toLegalId);
        String periodId = resolvePeriodId(businessDate);
        String currencyId = "1";
        java.math.BigDecimal amount = pricing.getUnitPrice();

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
        String currencyId = "1";

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
        // 默认账套 = 1（多账套精确解析归 successor）
        return "1";
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
            LOG.debug("period lookup 失败 businessDate={}: {}", businessDate, e.getMessage());
            return null;
        }
    }
}
