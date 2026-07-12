package app.erp.prj.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 项目结算过账派发器。结算单 approve 后（CLOSE 转固建卡之后）组装 {@link PostingEvent}(PROJECT_SETTLEMENT)
 * 经 {@link ProjectPostingExecutor}（独立新事务由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code REQUIRES_NEW}
 * 承接）调用财务过账引擎。
 *
 * <p>对齐 assets/sales 失败语义：过账失败吞异常记日志、保持 APPROVED+{@code posted=false}，
 * 不阻塞终态。本类为 Facade 编排层，不持久化源单据——源单据 {@code posted} 标志由调用方 Processor
 * 在主事务内统一持久化。
 */
public class ProjectSettlementPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectSettlementPostingDispatcher.class);

    @Inject
    ProjectPostingExecutor executor;
    @Inject
    IDaoProvider daoProvider;

    /**
     * 结算审核通过后调用。成功返回 true（调用方据此置 posted=true）；失败吞异常返回 false（保持 posted=false）。
     */
    public boolean tryPost(ErpPrjProjectSettlement settlement) {
        PostingEvent event = buildEvent(settlement);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("项目结算过账失败，结算单 {} 保持 APPROVED、posted=false：{}", settlement.getCode(), e.getMessage());
            } else {
                LOG.error("项目结算过账异常，结算单 {} 保持 APPROVED、posted=false", settlement.getCode(), e);
            }
            return false;
        }
    }

    /**
     * 反审批/取消前红字冲销已过账凭证（对齐 posting.md §冲销）。冲销是硬前置，失败向上抛出阻断状态迁移。
     */
    public void reverse(ErpPrjProjectSettlement settlement) {
        try {
            executor.reverse(settlement.getCode(), ErpFinBusinessType.PROJECT_SETTLEMENT);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("项目结算红字冲销失败，结算单 {}：{}", settlement.getCode(), e.getMessage());
            } else {
                LOG.error("项目结算红字冲销异常，结算单 {}", settlement.getCode(), e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpPrjProjectSettlement settlement) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.PROJECT_SETTLEMENT);
        event.setBillHeadCode(settlement.getCode());
        event.setOrgId(settlement.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(settlement.getOrgId()));
        event.setCurrencyId(settlement.getCurrencyId());
        event.setExchangeRate(settlement.getExchangeRate() != null ? settlement.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = settlement.getBusinessDate() != null ? settlement.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpPrjConstants.BILL_DATA_PROJECT_ID, settlement.getProjectId());
        billData.put(ErpPrjConstants.BILL_DATA_FINAL_REVENUE, nz(settlement.getFinalRevenue()));
        billData.put(ErpPrjConstants.BILL_DATA_FINAL_COST, nz(settlement.getFinalCost()));
        billData.put(ErpPrjConstants.BILL_DATA_FINAL_PROFIT, nz(settlement.getFinalProfit()));
        billData.put(ErpPrjConstants.BILL_DATA_SETTLEMENT_TYPE, settlement.getSettlementType());
        billData.put(ErpPrjConstants.BILL_DATA_TRANSFER_TO_ASSET,
                Boolean.TRUE.equals(settlement.getTransferToAsset()));
        if (settlement.getAssetCardId() != null) {
            billData.put(ErpPrjConstants.BILL_DATA_ASSET_CARD_CODE, settlement.getAssetCardId());
        }
        event.setBillData(billData);
        return event;
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
