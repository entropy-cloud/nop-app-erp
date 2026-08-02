
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBankStatementLineBiz;
import app.erp.fin.dao.dto.BankStatementMatchResult;
import app.erp.fin.dao.entity.ErpFinBankStatementLine;
import app.erp.fin.service.processor.ErpFinBankStatementLineAutoMatchProcessor;
import app.erp.fin.service.processor.ErpFinBankStatementLineManualMatchProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

@BizModel("ErpFinBankStatementLine")
public class ErpFinBankStatementLineBizModel extends CrudBizModel<ErpFinBankStatementLine>
        implements IErpFinBankStatementLineBiz {
    public ErpFinBankStatementLineBizModel() {
        setEntityName(ErpFinBankStatementLine.class.getName());
    }

    @Inject
    ErpFinBankStatementLineAutoMatchProcessor autoMatchProcessor;
    @Inject
    ErpFinBankStatementLineManualMatchProcessor manualMatchProcessor;

    @Override
    @BizMutation
    public BankStatementMatchResult autoMatch(@Name("statementId") Long statementId, IServiceContext context) {
        return autoMatchProcessor.autoMatch(statementId, context);
    }

    @Override
    @BizMutation
    public ErpFinBankStatementLine manualMatch(@Name("lineId") Long lineId,
                                                @Name("voucherLineId") Long voucherLineId,
                                                IServiceContext context) {
        return manualMatchProcessor.manualMatch(lineId, voucherLineId, context);
    }

    // matchedLineId 为内部匹配链路（自引用 voucherLine），保留原始 ID。

}
