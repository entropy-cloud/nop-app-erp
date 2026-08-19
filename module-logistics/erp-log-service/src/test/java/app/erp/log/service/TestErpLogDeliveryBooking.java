package app.erp.log.service;

import app.erp.log.biz.IErpLogDeliveryBookingBiz;
import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogDeliveryBooking;
import app.erp.log.dao.entity.ErpLogDeliveryWindow;
import app.erp.log.dao.entity.ErpLogShipment;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配送窗口容量预约引擎测试（RC-R1.84，P1-RC-086，UC-LOG-07 九验收标准之后端落地）。
 *
 * <p>覆盖：容量满拒绝 / 预约成功计数+1 / 重复预约幂等拒绝 / 释放计数-1（下限 0 守卫）/
 * 爽约费（系统参数）+priorityScore 提升 / markArrived 入口 / 窗口过期不可预约 /
 * 发运单 CANCELLED 联动释放（容量 -1）/ 并发计数守卫（容量守卫按最新值复核 + 每预约恰 +1）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogDeliveryBooking extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpLogDeliveryBookingBiz bookingBiz;
    @Inject
    IErpLogShipmentBiz shipmentBiz;

    @AfterEach
    public void resetMissedFeeConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConstants.CONFIG_BOOKING_MISSED_FEE, BigDecimal.ZERO);
    }

    /** 组 1：预约成功 → BOOKED + currentBooked +1。 */
    @Test
    public void testBookIncrementsCount() {
        Long windowId = seedWindow(10);
        Long shipmentId = seedShipment("SHP-BK-1");

        LocalDate date = nextWednesday();
        ErpLogDeliveryBooking booking = ormTemplate.runInSession(s ->
                bookingBiz.book(shipmentId, windowId, date, CTX));

        assertNotNull(booking.getId(), "预约应创建成功");
        assertEquals(ErpLogConstants.BOOKING_STATUS_BOOKED, booking.getStatus());
        assertEquals(0, booking.getPriorityScore());
        assertEquals(Integer.valueOf(1), window(windowId).getCurrentBooked(), "currentBooked 应 +1");
        assertEquals(date, booking.getBookedDate());
    }

    /** 组 2：容量满拒绝（currentBooked >= maxCapacity）。 */
    @Test
    public void testCapacityFullRejected() {
        Long windowId = seedWindow(1);
        Long shipment1 = seedShipment("SHP-BK-CAP-1");
        Long shipment2 = seedShipment("SHP-BK-CAP-2");

        LocalDate date = nextWednesday();
        ormTemplate.runInSession(s -> bookingBiz.book(shipment1, windowId, date, CTX));

        NopExceptionLike ex = catchBooking(() ->
                ormTemplate.runInSession(s -> bookingBiz.book(shipment2, windowId, date, CTX)));
        assertEquals(ErpLogErrors.ERR_LOG_BOOKING_CAPACITY_FULL.getErrorCode(), ex.code,
                "容量不足应显式拒绝");
        assertEquals(Integer.valueOf(1), window(windowId).getCurrentBooked(), "拒绝路径计数不变");
    }

    /** 组 3：同一发运单重复预约幂等拒绝。 */
    @Test
    public void testDuplicateBookingRejected() {
        Long windowId = seedWindow(10);
        Long shipmentId = seedShipment("SHP-BK-DUP-1");
        LocalDate date = nextWednesday();

        ormTemplate.runInSession(s -> bookingBiz.book(shipmentId, windowId, date, CTX));
        NopExceptionLike ex = catchBooking(() ->
                ormTemplate.runInSession(s -> bookingBiz.book(shipmentId, windowId, date.plusDays(7), CTX)));
        assertEquals(ErpLogErrors.ERR_LOG_BOOKING_DUPLICATE.getErrorCode(), ex.code,
                "同一发运单重复预约应幂等拒绝（换窗口同样拒绝）");
        assertEquals(Integer.valueOf(1), window(windowId).getCurrentBooked(), "拒绝路径计数不变");
    }

    /** 组 4：释放 → 预约 CANCELLED + currentBooked -1；重复释放幂等 no-op 且计数不被击穿至负。 */
    @Test
    public void testReleaseDecrementsWithFloorZero() {
        Long windowId = seedWindow(2);
        Long shipmentId = seedShipment("SHP-BK-REL-1");
        LocalDate date = nextWednesday();

        ormTemplate.runInSession(s -> bookingBiz.book(shipmentId, windowId, date, CTX));
        ormTemplate.runInSession(s -> {
            bookingBiz.releaseForShipment(shipmentId, CTX);
            return null;
        });
        assertEquals(Integer.valueOf(0), window(windowId).getCurrentBooked(), "释放后 currentBooked -1");
        assertNull(activeBooking(shipmentId), "释放后无有效预约");

        // 幂等重复释放 + 下限 0 守卫：无有效预约时 no-op，计数不为负
        ormTemplate.runInSession(s -> {
            bookingBiz.releaseForShipment(shipmentId, CTX);
            return null;
        });
        assertEquals(Integer.valueOf(0), window(windowId).getCurrentBooked(), "重复释放不击穿下限 0");
        assertNull(activeBooking(shipmentId), "释放后无有效预约");

        // CANCELLED 预约不阻断新预约（释放后可重新预约）
        Long rebook = seedShipment("SHP-BK-REL-2");
        ormTemplate.runInSession(s -> bookingBiz.book(rebook, windowId, date, CTX));
        assertEquals(Integer.valueOf(1), window(windowId).getCurrentBooked(), "释放槽位可复用");
    }

    /** 组 5：爽约 markMissed → MISSED + 爽约费（系统参数）+ priorityScore 提升；markArrived 入口可达。 */
    @Test
    public void testMarkMissedFeeAndPriorityScore() {
        AppConfig.getConfigProvider().assignConfigValue(ErpLogConstants.CONFIG_BOOKING_MISSED_FEE,
                new BigDecimal("88.50"));
        Long windowId = seedWindow(10);
        Long missedShipment = seedShipment("SHP-BK-MISS-1");
        Long arrivedShipment = seedShipment("SHP-BK-ARR-1");
        LocalDate date = nextWednesday();

        ormTemplate.runInSession(s -> bookingBiz.book(missedShipment, windowId, date, CTX));
        ormTemplate.runInSession(s -> bookingBiz.book(arrivedShipment, windowId, date, CTX));

        ErpLogDeliveryBooking missed = ormTemplate.runInSession(s ->
                bookingBiz.markMissed(missedShipment, CTX));
        assertEquals(ErpLogConstants.BOOKING_STATUS_MISSED, missed.getStatus());
        assertEquals(0, new BigDecimal("88.50").compareTo(missed.getMissedFee()), "爽约费应从系统参数读取");
        assertEquals(Integer.valueOf(ErpLogConstants.BOOKING_MISSED_PRIORITY_SCORE_STEP), missed.getPriorityScore(),
                "爽约后 priorityScore 提升获得优先重新预约权");

        ErpLogDeliveryBooking arrived = ormTemplate.runInSession(s ->
                bookingBiz.markArrived(arrivedShipment, CTX));
        assertEquals(ErpLogConstants.BOOKING_STATUS_ARRIVED, arrived.getStatus(), "markArrived 入口可达");

        // MISSED 终态不可再 markArrived
        NopExceptionLike ex = catchBooking(() ->
                ormTemplate.runInSession(s -> bookingBiz.markArrived(missedShipment, CTX)));
        assertEquals(ErpLogErrors.ERR_LOG_BOOKING_ILLEGAL_STATUS.getErrorCode(), ex.code);
    }

    /** 组 6：窗口过期（effectiveTo 已过）不可预约；星期不匹配拒绝。 */
    @Test
    public void testExpiredWindowAndWeekdayMismatchRejected() {
        Long expiredWindowId = seedWindowWithEffectivity(10,
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(1));
        Long shipment1 = seedShipment("SHP-BK-EXP-1");
        NopExceptionLike ex = catchBooking(() ->
                ormTemplate.runInSession(s -> bookingBiz.book(shipment1, expiredWindowId, nextWednesday(), CTX)));
        assertEquals(ErpLogErrors.ERR_LOG_BOOKING_WINDOW_NOT_BOOKABLE.getErrorCode(), ex.code,
                "窗口过期（effectiveTo 已过）不可预约");

        Long windowId = seedWindow(10);
        Long shipment2 = seedShipment("SHP-BK-WD-1");
        LocalDate notWednesday = nextWednesday().plusDays(1);
        assertTrue(notWednesday.getDayOfWeek() != DayOfWeek.WEDNESDAY, "测试前提：对照日期非周三");
        NopExceptionLike ex2 = catchBooking(() ->
                ormTemplate.runInSession(s -> bookingBiz.book(shipment2, windowId, notWednesday, CTX)));
        assertEquals(ErpLogErrors.ERR_LOG_BOOKING_WEEKDAY_MISMATCH.getErrorCode(), ex2.code,
                "预约日期星期与窗口不匹配应拒绝");
    }

    /** 组 7：发运单 CANCELLED 迁移点联动释放（容量 -1），主迁移不受影响。 */
    @Test
    public void testShipmentCancelReleasesBooking() {
        Long windowId = seedWindow(5);
        Long shipmentId = seedShipment("SHP-BK-CXL-1");
        LocalDate date = nextWednesday();

        ormTemplate.runInSession(s -> bookingBiz.book(shipmentId, windowId, date, CTX));
        assertEquals(Integer.valueOf(1), window(windowId).getCurrentBooked());

        ErpLogShipment cancelled = ormTemplate.runInSession(s ->
                shipmentBiz.cancelShipment(shipmentId, CTX));
        assertEquals(ErpLogConstants.SHIPMENT_STATUS_CANCELLED, cancelled.getStatus(), "主状态迁移成功");
        assertEquals(Integer.valueOf(0), window(windowId).getCurrentBooked(), "CANCELLED 联动释放容量 -1");
        assertNull(activeBooking(shipmentId), "预约已随发运单取消释放");
    }

    /** 组 8：发运单 DELIVERED 迁移点联动释放（webhook 路径，主迁移 + 运费过账不受预约释放影响）。 */
    @Test
    public void testShipmentDeliveredReleasesBooking() {
        Long windowId = seedWindow(5);
        String carrierCode = "MOCK-BK-DLV-CAR";
        Long shipmentId = ormTemplate.runInSession(s -> {
            seedCarrier(carrierCode);
            ErpLogShipment sh = new ErpLogShipment();
            sh.setCode("SHP-BK-DLV-1");
            sh.setOrgId(1L);
            sh.setCarrierId(carrierIdCache);
            sh.setStatus(ErpLogConstants.SHIPMENT_STATUS_DISPATCHED);
            sh.setTrackingNo("TRK-BK-DLV-1");
            sh.setBusinessDate(LocalDate.now());
            daoProvider.daoFor(ErpLogShipment.class).saveEntity(sh);
            return sh.getId();
        });

        ormTemplate.runInSession(s -> bookingBiz.book(shipmentId, windowId, nextWednesday(), CTX));
        assertEquals(Integer.valueOf(1), window(windowId).getCurrentBooked());

        String payload = "{\"trackingNo\":\"TRK-BK-DLV-1\",\"eventType\":\"DELIVERED\"}";
        String sig = hmacSha256(payload, carrierCode);
        ErpLogShipment delivered = ormTemplate.runInSession(s ->
                shipmentBiz.handleTrackingWebhook(carrierCode, sig, payload, CTX));

        assertEquals(ErpLogConstants.SHIPMENT_STATUS_DELIVERED, delivered.getStatus(), "主迁移成功");
        assertEquals(Integer.valueOf(0), window(windowId).getCurrentBooked(), "DELIVERED 联动释放容量 -1");
        assertNull(activeBooking(shipmentId), "预约已随发运单签收释放");
    }

    /** 组 9（并发计数守卫）：容量守卫按 DB 最新值复核——直接改 currentBooked 至满额后，后续 book 被拒；
     每次成功预约恰 +1（多次预约不跳变）。 */
    @Test
    public void testConcurrentCountGuardRevalidatesLatestValue() {
        Long windowId = seedWindow(3);
        // 模拟并发竞态：绕过引擎直接将计数推至满额（另一并发预约已 +1 的等价态）
        forceWindowCount(windowId, 3);

        Long shipmentId = seedShipment("SHP-BK-RACE-1");
        NopExceptionLike ex = catchBooking(() ->
                ormTemplate.runInSession(s -> bookingBiz.book(shipmentId, windowId, nextWednesday(), CTX)));
        assertEquals(ErpLogErrors.ERR_LOG_BOOKING_CAPACITY_FULL.getErrorCode(), ex.code,
                "容量守卫按最新计数复核，竞态后满额拒绝（防并发超卖）");

        // 恢复容量后多次预约：计数恰按次数 +1，无跳变
        forceWindowCount(windowId, 1);
        Long s2 = seedShipment("SHP-BK-RACE-2");
        ormTemplate.runInSession(s -> bookingBiz.book(s2, windowId, nextWednesday(), CTX));
        assertEquals(Integer.valueOf(2), window(windowId).getCurrentBooked(), "每预约恰 +1");
    }

    /** 直接覆写窗口计数（session 内重读后写，模拟并发方提交后的最新 DB 值）。 */
    private void forceWindowCount(Long windowId, int count) {
        ormTemplate.runInSession(s -> {
            ErpLogDeliveryWindow w = daoProvider.daoFor(ErpLogDeliveryWindow.class).getEntityById(windowId);
            w.setCurrentBooked(count);
            daoProvider.daoFor(ErpLogDeliveryWindow.class).saveOrUpdateEntity(w);
            return null;
        });
    }

    // ---------- helpers ----------

    private static LocalDate nextWednesday() {
        LocalDate d = LocalDate.now();
        int delta = (DayOfWeek.WEDNESDAY.getValue() - d.getDayOfWeek().getValue() + 7) % 7;
        return d.plusDays(delta == 0 ? 7 : delta);
    }

    private ErpLogDeliveryWindow window(Long windowId) {
        return ormTemplate.runInSession(s -> daoProvider.daoFor(ErpLogDeliveryWindow.class).getEntityById(windowId));
    }

    private ErpLogDeliveryBooking activeBooking(Long shipmentId) {
        return ormTemplate.runInSession(s -> bookingBiz.findActiveByShipment(shipmentId, CTX));
    }

    /** seed 配送窗口：周三 09:00-12:00，容量 maxCapacity。 */
    private Long seedWindow(int maxCapacity) {
        return seedWindowWithEffectivity(maxCapacity, LocalDate.now().minusDays(1), LocalDate.now().plusDays(90));
    }

    private Long seedWindowWithEffectivity(int maxCapacity, LocalDate from, LocalDate to) {
        ErpLogDeliveryWindow w = new ErpLogDeliveryWindow();
        w.setPartnerId(9901L);
        w.setOrgId(1L);
        w.setWeekday(3);
        w.setStartTime("09:00");
        w.setEndTime("12:00");
        w.setMaxCapacity(maxCapacity);
        w.setCurrentBooked(0);
        w.setIsActive(Boolean.TRUE);
        w.setEffectiveFrom(from);
        w.setEffectiveTo(to);
        ormTemplate.runInSession(s -> {
            daoProvider.daoFor(ErpLogDeliveryWindow.class).saveEntity(w);
            return null;
        });
        return w.getId();
    }

    private Long seedShipment(String code) {
        ErpLogShipment s = new ErpLogShipment();
        s.setCode(code);
        s.setOrgId(1L);
        s.setCarrierId(seedCarrierId());
        s.setStatus(ErpLogConstants.SHIPMENT_STATUS_DRAFT);
        s.setBusinessDate(LocalDate.now());
        ormTemplate.runInSession(session -> {
            daoProvider.daoFor(ErpLogShipment.class).saveEntity(s);
            return null;
        });
        return s.getId();
    }

    private Long carrierIdCache;

    private Long seedCarrierId() {
        if (carrierIdCache != null) {
            return carrierIdCache;
        }
        seedCarrier("MOCK-BK-CAR");
        return carrierIdCache;
    }

    private void seedCarrier(String code) {
        app.erp.log.dao.entity.ErpLogCarrier c = new app.erp.log.dao.entity.ErpLogCarrier();
        c.setCode(code);
        c.setCarrierName("预约测试承运商");
        c.setCarrierType("EXPRESS");
        c.setGatewayId(ErpLogConstants.GATEWAY_ID_MOCK);
        c.setPartnerId(9901L);
        c.setIsActive(1);
        daoProvider.daoFor(app.erp.log.dao.entity.ErpLogCarrier.class).saveEntity(c);
        carrierIdCache = c.getId();
    }

    private static String hmacSha256(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class NopExceptionLike {
        final String code;

        NopExceptionLike(String code) {
            this.code = code;
        }
    }

    private NopExceptionLike catchBooking(Runnable action) {
        try {
            action.run();
        } catch (io.nop.api.core.exceptions.NopException e) {
            return new NopExceptionLike(e.getErrorCode());
        }
        throw new AssertionError("预期抛出 NopException 但未抛出");
    }
}
