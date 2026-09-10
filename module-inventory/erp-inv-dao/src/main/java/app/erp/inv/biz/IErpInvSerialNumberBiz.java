
package app.erp.inv.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.inv.dao.entity.ErpInvSerialNumber;

/**
 * 序列号台账业务接口。除标准 CRUD 外，定义出库状态翻转写路径契约（P2-CK-inv-012-r3，
 * {@code docs/design/inventory/state-machine.md} §异常路径「序列号已售」+ README §关键业务规则 6）：
 *
 * <ul>
 *   <li>{@link #markOutbound(String, String, String, String, IServiceContext)}：出库记账同事务将
 *       IN_STOCK 序列号翻转为 OUT 并回链出库单号（出库移动单 complete 编排调用）；非 IN_STOCK
 *       拒绝（{@code erp.err.inv.serial-not-in-stock}），台账无记录 no-op。</li>
 * </ul>
 */
public interface IErpInvSerialNumberBiz extends ICrudBiz<ErpInvSerialNumber>{

    /**
     * 出库翻转 writer（IN_STOCK→OUT，出库记账同事务）。按 (serialNo, materialId) 定位台账行：
     * 状态 IN_STOCK → 翻转 OUT 并回写 outBillType/outBillCode；非 IN_STOCK 抛
     * {@code erp.err.inv.serial-not-in-stock}；台账无记录返回 null（no-op，未登记序列号不入守卫）。
     *
     * @return 翻转后的台账行；查无记录返回 null
     */
    @BizMutation
    ErpInvSerialNumber markOutbound(@Name("serialNo") String serialNo,
                                    @Name("materialId") String materialId,
                                    @Name("outBillType") String outBillType,
                                    @Name("outBillCode") String outBillCode,
                                    IServiceContext context);
}
