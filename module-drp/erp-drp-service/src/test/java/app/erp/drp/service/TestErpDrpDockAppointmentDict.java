package app.erp.drp.service;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.DictBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.dict.DictProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * P3-CK-drp-023-r3 月台预约状态字典真相源对齐回归：ErpInvDrpDockAppointment.status 绑定
 * {@code erp-inv/drp-xdock-dock-status}（owner doc cross-dock.md §月台预约 5 值，value==code 单轨；
 * Non-Goal 实体真相源缺陷消解，dock 关系 Non-Goal 裁决注记落 cross-dock.md）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpDockAppointmentDict extends JunitAutoTestCase {

    @Test
    public void testDockAppointmentStatusDictExistsWithFiveValues() {
        DictBean dict = DictProvider.instance().getDict(null, "erp-inv/drp-xdock-dock-status", null, null);
        assertNotNull(dict, "erp-inv/drp-xdock-dock-status 字典应存在（ORM 真相源对齐 cross-dock.md）");
        for (String value : new String[]{"AVAILABLE", "BOOKED", "ARRIVED", "COMPLETED", "CANCELLED"}) {
            assertNotNull(dict.getLabelByValue(value), "字典应可按 value 解析 label: " + value);
        }
        assertEquals("BOOKED-已预约", dict.getLabelByValue("BOOKED"));
    }
}
