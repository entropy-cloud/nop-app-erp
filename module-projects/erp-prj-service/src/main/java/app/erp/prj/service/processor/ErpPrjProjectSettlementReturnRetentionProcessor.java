package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ErpPrjProjectSettlement returnRetention per-mutation Processor（RC-R1.63 / P1-RC-052，UC-PRJ-07 ⑤ 到期返还，
 * R6.6 {@code processor-extension-pattern.md} 每 mutation 一 Processor）。共享 protected helper 单一真相源在
 * {@link ErpPrjProjectSettlementProcessor}（slim-to-S-delegation facade）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>守卫链（D3 选项 A）：docStatus=APPROVED + approveStatus=APPROVED + posted=true + retentionAmount&gt;0
 * + retentionDueDate&lt;=today + 未返还。守卫失败抛 {@link ErpPrjErrors#ERR_RETENTION_RETURN_NOT_ALLOWED}（中文 + reason 参数）；
 * 已返还重复调用=幂等 no-op 零副作用（返还标记=ErpFinVoucherBillR billCode=结算单号#RETURN 存在性反查，
 * 引擎 post() 按 (billHeadCode, businessType) 幂等为第二层防线）。
 */
public class ErpPrjProjectSettlementReturnRetentionProcessor {

    @Inject
    ErpPrjProjectSettlementProcessor facade;

    public ErpPrjProjectSettlement returnRetention(Long settlementId, IServiceContext context) {
        ErpPrjProjectSettlement settlement = facade.requireSettlement(settlementId);

        // 幂等：已返还 → no-op 零副作用（D3 选项 A——已返还重复调用非错误，重复调返回单不重复过账）
        if (facade.isRetentionReturned(settlement)) {
            return settlement;
        }

        // 守卫链（硬守卫失败抛 ERR_RETENTION_RETURN_NOT_ALLOWED）
        if (!ErpPrjConstants.DOC_STATUS_APPROVED.equals(settlement.getDocStatus())
                || !ErpPrjConstants.APPROVE_STATUS_APPROVED.equals(settlement.getApproveStatus())) {
            throw notAllowed(settlement, "结算单非已审批状态（docStatus/approveStatus 须 APPROVED）");
        }
        if (!Boolean.TRUE.equals(settlement.getPosted())) {
            throw notAllowed(settlement, "结算单未过账（posted=false），留存凭证未生成不可返还");
        }
        BigDecimal retention = settlement.getRetentionAmount();
        if (retention == null || retention.signum() <= 0) {
            throw notAllowed(settlement, "无质保金留存（retentionAmount 为空或<=0）");
        }
        LocalDate due = settlement.getRetentionDueDate();
        if (due == null || due.isAfter(CoreMetrics.today())) {
            throw notAllowed(settlement, "质保金未到期（retentionDueDate=" + due + " 晚于今天）");
        }

        // 返还是用户显式操作：过账失败显式抛 ERR_RETENTION_RETURN_POSTING_FAILED（与主结算过账失败隔离语义区分）
        facade.postingDispatcher.postRetentionReturn(settlement);
        return settlement;
    }

    private NopException notAllowed(ErpPrjProjectSettlement settlement, String reason) {
        return new NopException(ErpPrjErrors.ERR_RETENTION_RETURN_NOT_ALLOWED)
                .param(ErpPrjErrors.ARG_SETTLEMENT_CODE, settlement.getCode())
                .param(ErpPrjErrors.ARG_REASON, reason);
    }
}