
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBankStatementLineBiz;
import app.erp.fin.dao.dto.BankStatementMatchResult;
import app.erp.fin.dao.entity.ErpFinBankStatementLine;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.bankrecon.BankStatementMatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

@BizModel("ErpFinBankStatementLine")
public class ErpFinBankStatementLineBizModel extends CrudBizModel<ErpFinBankStatementLine>
        implements IErpFinBankStatementLineBiz {
    public ErpFinBankStatementLineBizModel() {
        setEntityName(ErpFinBankStatementLine.class.getName());
    }

    @Inject
    BankStatementMatcher bankStatementMatcher;

    @Override
    @BizMutation
    @SingleSession
    public BankStatementMatchResult autoMatch(@Name("statementId") Long statementId, IServiceContext context) {
        return bankStatementMatcher.autoMatch(statementId);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinBankStatementLine manualMatch(@Name("lineId") Long lineId,
                                                @Name("voucherLineId") Long voucherLineId,
                                                IServiceContext context) {
        IDaoProvider daoProvider = daoProvider();
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
