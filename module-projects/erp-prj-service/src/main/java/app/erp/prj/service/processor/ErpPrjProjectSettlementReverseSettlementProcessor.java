package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpPrjProjectSettlement reverseSettlement per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含红冲凭证 + 回退卡片状态的冲销编排；共享 protected helper 单一真相源在
 * {@link ErpPrjProjectSettlementProcessor}（slim-to-S-delegation facade）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>RC-R1.63 / P1-RC-052（D2 选项 A，Explore ⑥）：主结算凭证含留存腿（billHeadCode=结算单号，红冲自动覆盖），
 * 但返还凭证（billHeadCode=结算单号#RETURN）独立存在——已返还后红冲会悬挂返还凭证 → 红冲前须守卫「未返还」。
 */
public class ErpPrjProjectSettlementReverseSettlementProcessor {

    @Inject
    ErpPrjProjectSettlementProcessor facade;

    public ErpPrjProjectSettlement reverseSettlement(Long settlementId, IServiceContext context) {
        ErpPrjProjectSettlement settlement = facade.requireSettlement(settlementId);
        if (!Objects.equals(Boolean.TRUE, settlement.getPosted())) {
            throw new NopException(ErpPrjErrors.ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpPrjErrors.ARG_SETTLEMENT_CODE, settlement.getCode())
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, "posted=false")
                    .param(ErpPrjErrors.ARG_EXPECTED_STATUS, "posted=true");
        }
        if (facade.isRetentionReturned(settlement)) {
            throw new NopException(ErpPrjErrors.ERR_RETENTION_RETURN_NOT_ALLOWED)
                    .param(ErpPrjErrors.ARG_SETTLEMENT_CODE, settlement.getCode())
                    .param(ErpPrjErrors.ARG_REASON, "质保金已到期返还（返还凭证独立存在），红冲主结算会悬挂返还凭证，不允许");
        }
        facade.postingDispatcher.reverse(settlement);
        facade.rollbackAssetIfNeeded(settlement);
        settlement = facade.requireSettlement(settlementId);
        settlement.setPosted(false);
        settlement.setPostedAt(null);
        settlement.setPostedBy(null);
        facade.save(settlement);
        return settlement;
    }
}
