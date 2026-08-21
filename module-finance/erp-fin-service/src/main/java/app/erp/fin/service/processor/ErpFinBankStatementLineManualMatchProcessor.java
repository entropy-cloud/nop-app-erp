package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinBankStatementLine;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinBankStatementLine manualMatch per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含人工勾对编排（加载行 + 状态守卫 + 凭证行存在校验 + 标记 MANUAL_MATCHED）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinBankStatementLineManualMatchProcessor {

    @Inject
    IDaoProvider daoProvider;

    public ErpFinBankStatementLine manualMatch(String lineId, String voucherLineId, IServiceContext context) {
        IEntityDao<ErpFinBankStatementLine> lineDao = daoProvider.daoFor(ErpFinBankStatementLine.class);
        ErpFinBankStatementLine line = lineDao.getEntityById(lineId);
        if (line == null) {
            throw new NopException(ErpFinErrors.ERR_BANK_STMT_LINE_NOT_FOUND)
                    .param(ErpFinErrors.ARG_LINE_ID, lineId);
        }
        if (!ErpFinConstants.BANK_MATCH_UNMATCHED.equals(line.getMatchStatus())
                && !ErpFinConstants.BANK_MATCH_SUSPENSE.equals(line.getMatchStatus())) {
            throw new NopException(ErpFinErrors.ERR_BANK_STMT_LINE_ALREADY_MATCHED)
                    .param(ErpFinErrors.ARG_LINE_ID, lineId);
        }
        IEntityDao<ErpFinVoucherLine> voucherLineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        if (voucherLineDao.getEntityById(voucherLineId) == null) {
            throw new NopException(ErpFinErrors.ERR_VOUCHER_LINE_NOT_FOUND)
                    .param(ErpFinErrors.ARG_VOUCHER_LINE_ID, voucherLineId);
        }
        line.setMatchStatus(ErpFinConstants.BANK_MATCH_MANUAL_MATCHED);
        line.setMatchedLineId(voucherLineId);
        return line;
    }
}
