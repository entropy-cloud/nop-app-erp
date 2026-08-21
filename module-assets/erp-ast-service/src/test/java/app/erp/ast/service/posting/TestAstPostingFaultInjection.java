package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.common.test.FaultInjectionStubs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * assets G4 故障注入测试（A4-alert，设计文档 §5.2）。
 *
 * <p>Proxy 桩 {@code IErpFinVoucherBiz.post} 抛 + Proxy 桩 {@code IErpSysNotificationBiz} 录制 →
 * {@link DepreciationPostingDispatcher} catch-swallow 返回 null + 派发 {@code ast.depreciation-posting-failure} 告警。
 *
 * <p>断言契约（设计文档 §4.2 G4）：
 * <ul>
 *   <li>A1（posted 一致性）：{@code tryPost} 返回 null（过账失败 → posted=false 悬挂）</li>
 *   <li>A2+A4（告警闭环）：captured event type = {@code ast.depreciation-posting-failure}</li>
 * </ul>
 *
 * <p>纯单元测试：{@code orgId=null} 跳过 {@code AcctSchemaResolver}；{@code category=null} 使科目解析回退默认值（不触及 daoProvider）。
 * 恢复路径：告警 + 期末前置检查兜底（{@code ErpAstDepreciationSchedule} posted=false 扫描阻断结账）。
 */
public class TestAstPostingFaultInjection {

    @Test
    public void testDepreciationPostingFailureReturnsNullAndAlerts() {
        DepreciationPostingDispatcher dispatcher = new DepreciationPostingDispatcher();
        AssetPostingExecutor executor = new AssetPostingExecutor();
        executor.voucherBiz = FaultInjectionStubs.throwingVoucherBiz();
        dispatcher.executor = executor;
        String[] captured = new String[1];
        dispatcher.notificationBiz = FaultInjectionStubs.recordingNotificationBiz(captured);

        ErpAstDepreciationSchedule schedule = new ErpAstDepreciationSchedule();
        schedule.setPeriod("2026-08");
        schedule.setOrgId(null);
        ErpAstAsset asset = new ErpAstAsset();
        asset.setCode("AST-FAIL-001");
        asset.setOrgId(null);

        String voucherId = dispatcher.tryPost(schedule, asset, null);

        assertNull(voucherId,
                "折旧过账失败应吞异常返回 null（保持 posted=false 悬挂，A1）");
        assertEquals(DepreciationPostingDispatcher.NOTIFY_EVENT_DEPRECIATION_FAILURE, captured[0],
                "折旧过账失败应派发 ast.depreciation-posting-failure 告警（A2+A4）");
    }

    @Test
    public void testNullNotificationBizSkipsGracefully() {
        DepreciationPostingDispatcher dispatcher = new DepreciationPostingDispatcher();
        ErpAstAsset asset = new ErpAstAsset();
        asset.setCode("AST-NULL-001");
        ErpAstDepreciationSchedule schedule = new ErpAstDepreciationSchedule();
        schedule.setPeriod("2026-08");
        dispatcher.dispatchFailureAlert(asset, schedule, new RuntimeException("down"));
        assertTrue(true, "null notificationBiz 应静默跳过不抛异常");
    }
}
