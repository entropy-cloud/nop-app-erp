package app.erp.log.biz;

import app.erp.log.dao.entity.ErpLogDeliveryBooking;
import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;

/**
 * 配送时段预约业务接口（RC-R1.84 / P1-RC-086，UC-LOG-07）。容量预约引擎入口：
 * book（容量守卫 + 计数 +1 + 幂等）/ releaseForShipment（释放 + 计数 -1 下限 0）/
 * markArrived / markMissed（爽约费 + priorityScore 提升）。
 */
public interface IErpLogDeliveryBookingBiz extends ICrudBiz<ErpLogDeliveryBooking> {

    /**
     * 预约配送时段：窗口有效期内 + 容量未满（currentBooked &lt; maxCapacity）时创建 BOOKED 预约并
     * {@code currentBooked += 1}；同一发运单重复预约幂等拒绝；星期不匹配/窗口过期拒绝。
     */
    @BizMutation
    ErpLogDeliveryBooking book(@Name("shipmentId") Long shipmentId,
                               @Name("windowId") Long windowId,
                               @Name("bookedDate") LocalDate bookedDate,
                               IServiceContext context);

    /**
     * 释放发运单预约（发运单 CANCELLED/DELIVERED 联动或手工释放）：预约置 CANCELLED +
     * {@code currentBooked -= 1}（下限 0）。无有效预约时幂等 no-op。
     */
    @BizMutation
    void releaseForShipment(@Name("shipmentId") Long shipmentId, IServiceContext context);

    /** 标记预约已到达（BOOKED/CONFIRMED → ARRIVED）。 */
    @BizMutation
    ErpLogDeliveryBooking markArrived(@Name("shipmentId") Long shipmentId, IServiceContext context);

    /** 标记爽约（BOOKED/CONFIRMED → MISSED）：记爽约费（erp-log.booking-missed-fee 配置）+ priorityScore 提升。 */
    @BizMutation
    ErpLogDeliveryBooking markMissed(@Name("shipmentId") Long shipmentId, IServiceContext context);

    /** 按发运单查找当前有效（非 CANCELLED）预约，无则返回 null。 */
    @BizAction
    ErpLogDeliveryBooking findActiveByShipment(@Name("shipmentId") Long shipmentId, IServiceContext context);
}
