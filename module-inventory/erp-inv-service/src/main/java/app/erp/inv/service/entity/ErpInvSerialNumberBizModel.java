
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import app.erp.common.service.AbstractErpCrudBizModel;
import app.erp.inv.biz.IErpInvSerialNumberBiz;
import app.erp.inv.dao.ErpInvDaoConstants;
import app.erp.inv.dao.entity.ErpInvSerialNumber;
import app.erp.inv.service.ErpInvErrors;
import io.nop.commons.util.StringHelper;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

@BizModel("ErpInvSerialNumber")
public class ErpInvSerialNumberBizModel extends AbstractErpCrudBizModel<ErpInvSerialNumber> implements IErpInvSerialNumberBiz {
    public ErpInvSerialNumberBizModel(){
        setEntityName(ErpInvSerialNumber.class.getName());
    }

    /**
     * P2-CK-inv-012-r3：出库翻转 writer（state-machine.md §异常路径「已售序列号拒绝再次出库」）。
     * 出库移动单 complete 编排在记账同事务调用（经 I*Biz 注入，ErpInvStockMoveProcessor）；
     * 同事务可见性由调用方 @BizMutation 事务边界保证。
     */
    @Override
    @BizMutation
    public ErpInvSerialNumber markOutbound(@Name("serialNo") String serialNo,
                                           @Name("materialId") String materialId,
                                           @Name("outBillType") String outBillType,
                                           @Name("outBillCode") String outBillCode,
                                           IServiceContext context) {
        if (StringHelper.isBlank(serialNo)) {
            return null;
        }
        ErpInvSerialNumber sn = findSerial(serialNo, materialId);
        if (sn == null) {
            // 台账无记录（未登记序列号）：no-op，出库守卫仅校验已登记行（与确认期状态守卫同边界）
            return null;
        }
        if (!ErpInvDaoConstants.SERIAL_STATUS_IN_STOCK.equals(sn.getStatus())) {
            throw new NopException(ErpInvErrors.ERR_SERIAL_NOT_IN_STOCK)
                    .param(ErpInvErrors.ARG_MATERIAL_ID, materialId)
                    .param(ErpInvErrors.ARG_SERIAL_NO, serialNo)
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, sn.getStatus());
        }
        sn.setStatus(ErpInvDaoConstants.SERIAL_STATUS_OUT);
        sn.setOutBillType(outBillType);
        sn.setOutBillCode(outBillCode);
        dao().updateEntity(sn);
        return sn;
    }

    private ErpInvSerialNumber findSerial(String serialNo, String materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("serialNo", serialNo));
        q.addFilter(eq("materialId", materialId));
        q.addOrderField("id", true);
        List<ErpInvSerialNumber> list = dao().findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
