package app.erp.mfg.service;

import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.service.costing.ProductionVarianceCalculator;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 行为测试（plan 2026-07-06-0642-1）：生产差异阈值告警消费者。
 *
 * <p>覆盖：{@link ProductionVarianceCalculator#calculateVariances} 计算结果落定后按阈值判定，
 * 超阈值调 {@code IErpSysNotificationBiz.notify("mfg.production-variance", ctx)}；断言：
 * <ul>
 *   <li>超阈值场景：notify 被调 + ErpSysNotification 行落入（recipient 匹配 USER_LIST 模板接收人）</li>
 *   <li>未超阈值：不派发通知</li>
 *   <li>config 关闭（erp-mfg.variance-alert-enabled=false）：静默跳过</li>
 * </ul>
 *
 * <p>权威：{@code docs/plans/2026-07-05-1838-2}（差异引擎）+ 本计划 §Phase 3 Decision（旁路告警，与过账解耦）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgVarianceAlert extends JunitAutoTestCase {

    static final String ORG_ID = "1401";
    static final String UOM_ID = "5401";
    static final String CURRENCY_ID = "6401";
    static final String WC1 = "6201";
    static final String P = "1201";
    static final String NOTIFY_EVENT = ErpMfgConstants.NOTIFY_EVENT_PRODUCTION_VARIANCE;
    static final String RECIPIENT = "mfg-variance-recipient";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ProductionVarianceCalculator productionVarianceCalculator;

    @Test
    public void testVarianceOverThresholdTriggersNotify() {
        // 阈值设小（=10），任何非零差异都会触发
        setThreshold(new BigDecimal("10"));
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        String bomId = seedBom("9211", P);
        seedBomOperation("4211", bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedNotifyTemplate("7106", RECIPIENT);
        int before = countNotifications(NOTIFY_EVENT);

        // 实际材料 25 远超标准 20 → 材料用量差异 +5（绝对值 5 < 10 不触发）
        // 但人工效率差异 +30（绝对值 30 > 10 触发）
        ErpMfgWorkOrder wo = seedCompletedWorkOrder("8211", "WO-VAR-ALERT-001", bomId, P,
                bd("2"), bd("2"), bd("25"), bd("35"), bd("8"));
        seedTimeLog("5611", "8211", bd("150"));

        productionVarianceCalculator.calculateVariances("8211");

        int after = countNotifications(NOTIFY_EVENT);
        assertTrue(after > before, "差异超阈值应派发 mfg.production-variance 通知");
        ErpSysNotification n = findNotification(NOTIFY_EVENT);
        assertEquals(RECIPIENT, n.getRecipientUserId(), "接收人应匹配模板 USER_LIST");
    }

    @Test
    public void testVarianceUnderThresholdSkipsNotify() {
        // 阈值设很大（=10000），所有差异都未超阈值
        setThreshold(new BigDecimal("10000"));
        try {
            seedProduct(P);
            seedWorkcenter(WC1, bd("20"));
            String bomId = seedBom("9212", P);
            seedBomOperation("4212", bomId, WC1, bd("60"));
            seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
            seedNotifyTemplate("7116", RECIPIENT);
            seedCompletedWorkOrder("8212", "WO-VAR-ALERT-002", bomId, P,
                    bd("2"), bd("2"), bd("25"), bd("35"), bd("8"));
            seedTimeLog("5612", "8212", bd("150"));
            int before = countNotifications(NOTIFY_EVENT);

            productionVarianceCalculator.calculateVariances("8212");

            int after = countNotifications(NOTIFY_EVENT);
            assertEquals(before, after, "差异未超阈值应不派发通知");
        } finally {
            setThreshold(ErpMfgConstants.DEFAULT_VARIANCE_ALERT_THRESHOLD);
        }
    }

    @Test
    public void testAlertDisabledSkipsNotify() {
        setAlertEnabled(false);
        try {
            seedProduct(P);
            seedWorkcenter(WC1, bd("20"));
            String bomId = seedBom("9213", P);
            seedBomOperation("4213", bomId, WC1, bd("60"));
            seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
            seedNotifyTemplate("7126", RECIPIENT);
            seedCompletedWorkOrder("8213", "WO-VAR-ALERT-003", bomId, P,
                    bd("2"), bd("2"), bd("25"), bd("35"), bd("8"));
            seedTimeLog("5613", "8213", bd("150"));
            int before = countNotifications(NOTIFY_EVENT);

            productionVarianceCalculator.calculateVariances("8213");

            int after = countNotifications(NOTIFY_EVENT);
            assertEquals(before, after, "config 关闭时应静默跳过 notify 派发");
        } finally {
            setAlertEnabled(true);
        }
    }

    // ---------- helpers ----------

    private void setThreshold(BigDecimal value) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMfgConstants.CONFIG_VARIANCE_ALERT_THRESHOLD, value.toPlainString());
    }

    private void setAlertEnabled(boolean enabled) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMfgConstants.CONFIG_VARIANCE_ALERT_ENABLED, String.valueOf(enabled));
    }

    private int countNotifications(String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q).size();
    }

    private ErpSysNotification findNotification(String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        q.addOrderField("createTime", true);
        q.setLimit(1);
        List<ErpSysNotification> list = daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void seedNotifyTemplate(String id, String recipientUserId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(NOTIFY_EVENT);
            t.setName("生产差异超阈值告警");
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("生产差异超阈值告警: ${workOrderCode}");
            t.setBodyTpl("工单 ${workOrderCode}（产品 ${productCode}）${varianceType} 差异金额 ${varianceAmount} 已超阈值 ${threshold}，请核查");
            t.setRecipientResolver("USER_LIST");
            t.setRecipientConfig("{\"userIds\":[\"" + recipientUserId + "\"]}");
            t.setMergeWindowSeconds(60);
            t.setMergeStrategy("MERGE_BY_USER_TYPE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
        });
    }

    private void seedProduct(String id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Product " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            dao.saveEntity(m);
        });
    }

    private void seedWorkcenter(String id, BigDecimal hourlyRate) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenter> dao = daoProvider.daoFor(ErpMfgWorkcenter.class);
            ErpMfgWorkcenter wc = new ErpMfgWorkcenter();
            wc.orm_propValueByName("id", id);
            wc.setCode("WC-" + id);
            wc.setName("Workcenter " + id);
            wc.setHourlyRate(hourlyRate);
            dao.saveEntity(wc);
        });
    }

    private String seedBom(String id, String productId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", id);
            bom.setCode("BOM-" + id);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(Boolean.TRUE);
            bom.setIsActive(Boolean.TRUE);
            bom.setQty(bd("1"));
            dao.saveEntity(bom);
        });
        return id;
    }

    private void seedBomOperation(String id, String bomId, String workcenterId, BigDecimal standardTime) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBomOperation> dao = daoProvider.daoFor(ErpMfgBomOperation.class);
            ErpMfgBomOperation op = new ErpMfgBomOperation();
            op.orm_propValueByName("id", id);
            op.setBomId(bomId);
            op.setLineNo(10);
            op.setOperationId("9000");
            op.setWorkcenterId(workcenterId);
            op.setStandardTime(standardTime);
            dao.saveEntity(op);
        });
    }

    private void seedFirmedRollup(String productId, BigDecimal materialCost, BigDecimal laborCost,
                                  BigDecimal overheadCost, BigDecimal unitCost) {
        ormTemplate.runInSession(() -> {
            String headerId = String.valueOf(Long.parseLong(productId) * 10000 + 1);
            IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
            ErpMfgCostRollup header = new ErpMfgCostRollup();
            header.orm_propValueByName("id", headerId);
            header.setCode("ROLLUP-" + productId);
            header.setOrgId(ORG_ID);
            header.setBusinessDate(LocalDate.of(2026, 6, 1));
            header.orm_propValueByName("status", ErpMfgConstants.COST_ROLLUP_STATUS_FIRMED);
            headerDao.saveEntity(header);

            IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
            ErpMfgCostRollupLine line = new ErpMfgCostRollupLine();
            line.orm_propValueByName("id", String.valueOf(Long.parseLong(productId) * 10000 + 2));
            line.setCostRollupId(headerId);
            line.setLineNo(10);
            line.setMaterialId(productId);
            line.setUoMId(UOM_ID);
            line.setMaterialCost(materialCost);
            line.setLaborCost(laborCost);
            line.setOverheadCost(overheadCost);
            line.setUnitCost(unitCost);
            line.setTotalCost(unitCost);
            line.setCurrencyId(CURRENCY_ID);
            lineDao.saveEntity(line);
        });
    }

    private ErpMfgWorkOrder seedCompletedWorkOrder(String id, String code, String bomId, String productId,
                                                   BigDecimal planned, BigDecimal completed,
                                                   BigDecimal materialCost, BigDecimal laborCost,
                                                   BigDecimal overheadCost) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(productId);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(planned);
            wo.setCompletedQuantity(completed);
            wo.setMaterialCost(materialCost);
            wo.setLaborCost(laborCost);
            wo.setOverheadCost(overheadCost);
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED);
            dao.saveEntity(wo);
        });
        return daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(id);
    }

    private void seedTimeLog(String id, String workOrderId, BigDecimal durationMins) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgJobCardTimeLog> dao = daoProvider.daoFor(ErpMfgJobCardTimeLog.class);
            ErpMfgJobCardTimeLog log = new ErpMfgJobCardTimeLog();
            log.orm_propValueByName("id", id);
            log.setJobCardId("9001");
            log.setWorkOrderId(workOrderId);
            log.setOperatorId("OP-001");
            log.setWorkDate(LocalDate.of(2026, 7, 1));
            log.setDurationMins(durationMins);
            dao.saveEntity(log);
        });
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
