
package app.erp.log.service.entity;

import app.erp.log.biz.IErpLogDeliveryBookingBiz;
import app.erp.log.biz.IErpLogDeliveryWindowBiz;
import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogDeliveryBooking;
import app.erp.log.dao.entity.ErpLogDeliveryWindow;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.ErpLogConstants;
import app.erp.log.service.ErpLogErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 配送时段预约容量引擎（RC-R1.84 / P1-RC-086，UC-LOG-07，delivery-window.md 契约物化）。
 *
 * <p>{@link #book}：窗口有效期内（isActive + effectiveFrom/effectiveTo）+ 星期匹配 +
 * {@code currentBooked < maxCapacity} 容量守卫 → 创建 BOOKED 预约 + {@code currentBooked += 1}；
 * 同一发运单重复预约幂等拒绝（应用层守卫 + DB {@code UK_LOG_DELIVERY_BOOKING_SHIPMENT(shipmentId,delVersion)} 并发兜底）。
 *
 * <p>{@link #releaseForShipment}：发运单 CANCELLED/DELIVERED 迁移点后置释放（{@code GatewayDispatcher} 联动，
 * 失败隔离不阻断主状态迁移）+ {@code currentBooked -= 1} 下限 0 守卫；无有效预约幂等 no-op。
 *
 * <p>容器计数更新经 {@code ErpLogDeliveryWindow.version} 乐观锁防并发超卖（updateEntity 冲突时事务失败回滚，
 * 双读同值只有一笔提交成功）。{@link #markMissed} 爽约费读 {@code erp-log.booking-missed-fee} 系统参数
 * （L1「爽约费金额从系统参数配置读取」，默认 0）+ priorityScore 提升（优先重新预约权）。
 */
@BizModel("ErpLogDeliveryBooking")
public class ErpLogDeliveryBookingBizModel extends CrudBizModel<ErpLogDeliveryBooking> implements IErpLogDeliveryBookingBiz {

    @Inject
    IErpLogShipmentBiz shipmentBiz;
    @Inject
    IErpLogDeliveryWindowBiz deliveryWindowBiz;

    public ErpLogDeliveryBookingBizModel() {
        setEntityName(ErpLogDeliveryBooking.class.getName());
    }

    @Override
    @BizMutation
    public ErpLogDeliveryBooking book(@Name("shipmentId") Long shipmentId,
                                      @Name("windowId") Long windowId,
                                      @Name("bookedDate") LocalDate bookedDate,
                                      IServiceContext context) {
        ErpLogShipment shipment = shipmentBiz.requireEntity(String.valueOf(shipmentId), null, context);
        if (ErpLogConstants.SHIPMENT_STATUS_CANCELLED.equals(shipment.getStatus())
                || ErpLogConstants.SHIPMENT_STATUS_DELIVERED.equals(shipment.getStatus())) {
            throw new NopException(ErpLogErrors.ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION)
                    .param(ErpLogErrors.ARG_SHIPMENT_CODE, shipment.getCode())
                    .param(ErpLogErrors.ARG_CURRENT_STATUS, shipment.getStatus())
                    .param(ErpLogErrors.ARG_EXPECTED_STATUS,
                            ErpLogConstants.SHIPMENT_STATUS_DRAFT + "/" + ErpLogConstants.SHIPMENT_STATUS_ADVISED);
        }
        if (findActiveByShipment(shipmentId, context) != null) {
            throw new NopException(ErpLogErrors.ERR_LOG_BOOKING_DUPLICATE)
                    .param(ErpLogErrors.ARG_SHIPMENT_ID, shipmentId);
        }

        ErpLogDeliveryWindow window = deliveryWindowBiz.get(String.valueOf(windowId), false, context);
        if (window == null) {
            throw new NopException(ErpLogErrors.ERR_LOG_BOOKING_WINDOW_NOT_BOOKABLE)
                    .param(ErpLogErrors.ARG_WINDOW_ID, windowId)
                    .param(ErpLogErrors.ARG_BOOKED_DATE, bookedDate);
        }
        validateWindowBookable(window, bookedDate);

        Integer current = window.getCurrentBooked() == null ? 0 : window.getCurrentBooked();
        Integer max = window.getMaxCapacity() == null ? 0 : window.getMaxCapacity();
        if (current >= max) {
            throw new NopException(ErpLogErrors.ERR_LOG_BOOKING_CAPACITY_FULL)
                    .param(ErpLogErrors.ARG_WINDOW_ID, windowId)
                    .param(ErpLogErrors.ARG_CURRENT_BOOKED, current)
                    .param(ErpLogErrors.ARG_MAX_CAPACITY, max);
        }

        ErpLogDeliveryBooking booking = newEntity();
        booking.setShipmentId(shipmentId);
        booking.setWindowId(windowId);
        booking.setOrgId(window.getOrgId());
        booking.setBookedDate(bookedDate);
        booking.setBookedTime(window.getStartTime());
        booking.setStatus(ErpLogConstants.BOOKING_STATUS_BOOKED);
        booking.setPriorityScore(0);
        saveEntity(booking, null, context);

        window.setCurrentBooked(current + 1);
        deliveryWindowBiz.updateEntity(window, null, context);
        return booking;
    }

    @Override
    @BizMutation
    public void releaseForShipment(@Name("shipmentId") Long shipmentId, IServiceContext context) {
        ErpLogDeliveryBooking booking = findActiveByShipment(shipmentId, context);
        if (booking == null) {
            return;
        }
        booking.setStatus(ErpLogConstants.BOOKING_STATUS_CANCELLED);
        updateEntity(booking, null, context);

        ErpLogDeliveryWindow window = deliveryWindowBiz.get(String.valueOf(booking.getWindowId()), false, context);
        if (window != null && window.getCurrentBooked() != null && window.getCurrentBooked() > 0) {
            window.setCurrentBooked(window.getCurrentBooked() - 1);
            deliveryWindowBiz.updateEntity(window, null, context);
        }
    }

    @Override
    @BizMutation
    public ErpLogDeliveryBooking markArrived(@Name("shipmentId") Long shipmentId, IServiceContext context) {
        ErpLogDeliveryBooking booking = requireActiveBooking(shipmentId, context);
        if (!ErpLogConstants.BOOKING_STATUS_BOOKED.equals(booking.getStatus())
                && !ErpLogConstants.BOOKING_STATUS_CONFIRMED.equals(booking.getStatus())) {
            throw new NopException(ErpLogErrors.ERR_LOG_BOOKING_ILLEGAL_STATUS)
                    .param(ErpLogErrors.ARG_CURRENT_STATUS, booking.getStatus())
                    .param(ErpLogErrors.ARG_EXPECTED_STATUS,
                            ErpLogConstants.BOOKING_STATUS_BOOKED + "/" + ErpLogConstants.BOOKING_STATUS_CONFIRMED);
        }
        booking.setStatus(ErpLogConstants.BOOKING_STATUS_ARRIVED);
        updateEntity(booking, null, context);
        return booking;
    }

    @Override
    @BizMutation
    public ErpLogDeliveryBooking markMissed(@Name("shipmentId") Long shipmentId, IServiceContext context) {
        ErpLogDeliveryBooking booking = requireActiveBooking(shipmentId, context);
        if (!ErpLogConstants.BOOKING_STATUS_BOOKED.equals(booking.getStatus())
                && !ErpLogConstants.BOOKING_STATUS_CONFIRMED.equals(booking.getStatus())) {
            throw new NopException(ErpLogErrors.ERR_LOG_BOOKING_ILLEGAL_STATUS)
                    .param(ErpLogErrors.ARG_CURRENT_STATUS, booking.getStatus())
                    .param(ErpLogErrors.ARG_EXPECTED_STATUS,
                            ErpLogConstants.BOOKING_STATUS_BOOKED + "/" + ErpLogConstants.BOOKING_STATUS_CONFIRMED);
        }
        booking.setStatus(ErpLogConstants.BOOKING_STATUS_MISSED);
        // L1 UC-LOG-07：爽约费金额从系统参数配置读取（默认 0）
        BigDecimal missedFee = AppConfig.var(ErpLogConstants.CONFIG_BOOKING_MISSED_FEE, BigDecimal.ZERO);
        booking.setMissedFee(missedFee);
        // 爽约后 priorityScore 提升获得优先重新预约权
        int score = booking.getPriorityScore() == null ? 0 : booking.getPriorityScore();
        booking.setPriorityScore(score + ErpLogConstants.BOOKING_MISSED_PRIORITY_SCORE_STEP);
        updateEntity(booking, null, context);
        return booking;
    }

    @Override
    @BizAction
    public ErpLogDeliveryBooking findActiveByShipment(@Name("shipmentId") Long shipmentId, IServiceContext context) {
        if (shipmentId == null) {
            return null;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("shipmentId", shipmentId));
        // status 的 xmeta 过滤面不支持 ne，eq 检索后内存剔除 CANCELLED（参重复发运守卫范式）
        List<ErpLogDeliveryBooking> bookings = findList(q, null, context);
        ErpLogDeliveryBooking found = null;
        for (ErpLogDeliveryBooking booking : bookings) {
            if (ErpLogConstants.BOOKING_STATUS_CANCELLED.equals(booking.getStatus())) {
                continue;
            }
            if (found == null || (booking.getId() != null && booking.getId() > found.getId())) {
                found = booking;
            }
        }
        return found;
    }

    private ErpLogDeliveryBooking requireActiveBooking(Long shipmentId, IServiceContext context) {
        ErpLogDeliveryBooking booking = findActiveByShipment(shipmentId, context);
        if (booking == null) {
            throw new NopException(ErpLogErrors.ERR_LOG_BOOKING_NOT_FOUND)
                    .param(ErpLogErrors.ARG_SHIPMENT_ID, shipmentId);
        }
        return booking;
    }

    /** 窗口可预约性：isActive + 预约日期在 [effectiveFrom, effectiveTo] 生效期内 + 星期匹配（L1 按星期过滤）。 */
    private void validateWindowBookable(ErpLogDeliveryWindow window, LocalDate bookedDate) {
        LocalDate today = CoreMetrics.today();
        boolean active = !Boolean.FALSE.equals(window.getIsActive());
        LocalDate from = window.getEffectiveFrom();
        LocalDate to = window.getEffectiveTo();
        boolean inEffect = (from == null || !today.isBefore(from)) && (to == null || !today.isAfter(to));
        if (!active || !inEffect) {
            throw new NopException(ErpLogErrors.ERR_LOG_BOOKING_WINDOW_NOT_BOOKABLE)
                    .param(ErpLogErrors.ARG_WINDOW_ID, window.getId())
                    .param(ErpLogErrors.ARG_BOOKED_DATE, bookedDate);
        }
        if (bookedDate != null && window.getWeekday() != null
                && window.getWeekday() != bookedDate.getDayOfWeek().getValue()) {
            throw new NopException(ErpLogErrors.ERR_LOG_BOOKING_WEEKDAY_MISMATCH)
                    .param(ErpLogErrors.ARG_BOOKED_DATE, bookedDate)
                    .param(ErpLogErrors.ARG_WINDOW_ID, window.getId())
                    .param(ErpLogErrors.ARG_WEEKDAY, window.getWeekday());
        }
    }
}
