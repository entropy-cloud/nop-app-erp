package app.erp.fin.service.intercompany;

import app.erp.fin.biz.IErpFinIntercompanyTransferBiz;
import app.erp.fin.dao.api.IErpFinTransferPriceResolver;
import app.erp.fin.dao.dto.TransferPriceResult;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<Long> onTransferConfirmed(Long transferOrderId, Long fromWarehouseId, Long toWarehouseId,
                                         LocalDate businessDate, IServiceContext context) {
        if (!isIntercompanyPostingEnabled()) {
            return Collections.emptyList();
        }
        if (fromWarehouseId == null || toWarehouseId == null || fromWarehouseId.equals(toWarehouseId)) {
            return Collections.emptyList();
        }

        Long fromOrgId = resolveWarehouseOrgId(fromWarehouseId);
        Long toOrgId = resolveWarehouseOrgId(toWarehouseId);
        if (fromOrgId == null || toOrgId == null) {
            return Collections.emptyList();
        }

        Long fromLegalId = resolveLegalEntityRoot(fromOrgId);
        Long toLegalId = resolveLegalEntityRoot(toOrgId);
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
        Long fromAcctSchemaId = resolveOrgAcctSchemaId(fromLegalId);
        Long toAcctSchemaId = resolveOrgAcctSchemaId(toLegalId);
        Long periodId = resolvePeriodId(businessDate);
        Long currencyId = 1L;
        java.math.BigDecimal amount = pricing.getUnitPrice();

        return intercompanyVoucherGenerator.generatePairedVouchers(transferCode, fromLegalId, toLegalId,
                fromAcctSchemaId, toAcctSchemaId, periodId, currencyId, amount);
    }

    // ---------- 内部辅助 ----------

    private boolean isIntercompanyPostingEnabled() {
        return Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_INTERCOMPANY_POSTING_ENABLED, Boolean.FALSE));
    }

    private Long resolveWarehouseOrgId(Long warehouseId) {
        ErpMdWarehouse wh = daoProvider.daoFor(ErpMdWarehouse.class).getEntityById(warehouseId);
        return wh == null ? null : wh.getOrgId();
    }

    /**
     * 沿 parentId 链向上找首个 orgType=COMPANY 的法人根。
     * 带环检测（visited set）防止脏数据导致死循环。
     */
    Long resolveLegalEntityRoot(Long orgId) {
        Map<Long, Boolean> visited = new HashMap<>();
        Long current = orgId;
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

    private String resolveTransferCode(Long transferOrderId) {
        // 调拨单 code 业财回链（无法直接读 ErpInvTransferOrder 实体因跨模块，用 ID 兜底）
        return "TRANSFER-" + transferOrderId;
    }

    private Long resolveOrgAcctSchemaId(Long orgId) {
        // 默认账套 = 1（多账套精确解析归 successor）
        return 1L;
    }

    private Long resolvePeriodId(LocalDate businessDate) {
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
