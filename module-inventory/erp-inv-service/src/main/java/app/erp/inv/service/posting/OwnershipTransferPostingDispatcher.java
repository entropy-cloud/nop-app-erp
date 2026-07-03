package app.erp.inv.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.inv.dao.entity.ErpInvOwnershipTransfer;
import app.erp.inv.dao.entity.ErpInvOwnershipTransferLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 所有权转移过账派发器。转移单 DONE 后（同库位调账同事务确立之后）按 {@code transferType} 派生业务类型，
 * 构造 {@link PostingEvent}(OWNERSHIP_TRANSFER) 经 {@link InvPostingExecutor}（独立新事务由 Facade
 * {@code IErpFinVoucherBiz.post()} 的 {@code REQUIRES_NEW} 承接）调用财务过账引擎。
 *
 * <p>config 门控（consignment.md §配置点）：{@code erp-inv.vmi-auto-generate-ap=true}（默认）时 VMI_CONSUME
 * 转移 DONE 自动生成应付；非 VMI_CONSUME 类型（CONSIGNMENT_RETURN/OWNERSHIP_TO_CUSTOMER）物权内部转移或转客户，
 * 无供应商结算，不派发过账事件。
 *
 * <p>过账失败不阻塞转移单终态（对齐 {@link InvPostingDispatcher} 语义）：以 try/catch 包裹，成功置
 * {@code posted=true}，失败吞异常记日志、保持 {@code posted=false}（由 Deferred 兜底扫描重试）。
 */
public class OwnershipTransferPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(OwnershipTransferPostingDispatcher.class);

    @Inject
    InvPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    /**
     * DONE 后调用。VMI_CONSUME 且 {@code vmi-auto-generate-ap=true} 时派发 OWNERSHIP_TRANSFER 过账；
     * 非 VMI_CONSUME 类型跳过（无供应商结算）。成功置转移单 {@code posted=true}；失败吞异常保持 {@code posted=false}。
     */
    public void dispatchIfApplicable(ErpInvOwnershipTransfer transfer,
                                     List<ErpInvOwnershipTransferLine> lines) {
        if (!shouldPost(transfer)) {
            return;
        }
        PostingEvent event = buildEvent(transfer, lines);
        try {
            Long voucherId = executor.postEvent(event);
            if (voucherId != null) {
                transfer.setPosted(true);
                transfer.setPostedAt(CoreMetrics.currentDateTime());
                daoProvider.daoFor(ErpInvOwnershipTransfer.class).saveOrUpdateEntity(transfer);
            }
        } catch (Exception e) {
            // 过账失败不阻塞转移单终态：保持 DONE + posted=false，由兜底扫描重试。
            if (e instanceof NopException) {
                LOG.warn("所有权转移过账失败，转移单 {} 保持 DONE、posted=false：{}",
                        transfer.getCode(), e.getMessage());
            } else {
                LOG.error("所有权转移过账异常，转移单 {} 保持 DONE、posted=false", transfer.getCode(), e);
            }
        }
    }

    /**
     * 仅 VMI_CONSUME 且 config 开启时派发。非 VMI_CONSUME（CONSIGNMENT_RETURN/OWNERSHIP_TO_CUSTOMER）
     * 物权内部转移或转客户，无供应商结算，不生成应付。
     */
    boolean shouldPost(ErpInvOwnershipTransfer transfer) {
        if (!ErpInvConstants.TRANSFER_TYPE_VMI_CONSUME.equals(transfer.getTransferType())) {
            return false;
        }
        Boolean autoAp = AppConfig.var(ErpInvConstants.CONFIG_VMI_AUTO_GENERATE_AP, Boolean.TRUE);
        return !Boolean.FALSE.equals(autoAp);
    }

    private PostingEvent buildEvent(ErpInvOwnershipTransfer transfer, List<ErpInvOwnershipTransferLine> lines) {
        BigDecimal totalCost = BigDecimal.ZERO;
        Long materialId = null;
        for (ErpInvOwnershipTransferLine line : lines) {
            BigDecimal lineCost = line.getTotalCost() != null ? line.getTotalCost() : BigDecimal.ZERO;
            totalCost = totalCost.add(lineCost.abs());
            if (materialId == null) {
                materialId = line.getMaterialId();
            }
        }

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.OWNERSHIP_TRANSFER);
        event.setBillHeadCode(transfer.getCode());
        event.setOrgId(transfer.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(transfer.getOrgId()));
        event.setCurrencyId(transfer.getCurrencyId());
        event.setExchangeRate(BigDecimal.ONE);
        LocalDate voucherDate = transfer.getBusinessDate() != null
                ? transfer.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put("TOTAL_COST", totalCost);
        billData.put("SUPPLIER_ID", transfer.getPartnerId());
        billData.put("partnerId", transfer.getPartnerId());
        billData.put("MATERIAL_ID", materialId);
        billData.put("WAREHOUSE_ID", transfer.getWarehouseId());
        billData.put("businessDate", voucherDate != null ? voucherDate.toString() : null);
        event.setBillData(billData);
        return event;
    }

    private Long resolveAcctSchemaId(Long orgId) {
        if (orgId == null) {
            return null;
        }
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        q.setLimit(1);
        List<ErpMdAcctSchema> schemas = dao.findAllByQuery(q);
        return schemas.isEmpty() ? null : schemas.get(0).getId();
    }
}
