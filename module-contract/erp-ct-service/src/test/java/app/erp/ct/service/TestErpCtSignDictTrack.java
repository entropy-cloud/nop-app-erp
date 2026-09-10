package app.erp.ct.service;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.DictBean;
import io.nop.core.dict.DictProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * P1-CK-ct-025-r3 双轨失效面回归（plan 2026-09-10-1141-2 Phase 1）。
 *
 * <p>缺陷面：sign-status/sign-provider 字典 value(10..60/99)/code(PENDING_SIGNATURE/MOCK) 双轨——
 * writer 全链存 code 形态（ErpCtConstants + InitProcessor:44 + seed/_cases），而列 ext:dict 绑定按
 * value 翻译，双轨并存时实存数据 label 解析失败、死数值轨反可解析。
 *
 * <p>修复裁决 = dict 收敛 value==code（D1 单轨语义编码，docs/skills/README.md §命名约定）；
 * 存量数据全为 code 形态故零迁移。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtSignDictTrack extends JunitAutoTestCase {

    @Test
    public void testSignStatusDictResolvesWriterCodeForm() {
        DictBean dict = DictProvider.instance().getDict(null, "erp-ct/sign-status", null, null);
        assertNotNull(dict, "erp-ct/sign-status 字典应存在");
        assertNotNull(dict.getLabelByValue(ErpCtConstants.SIGNATURE_STATUS_PENDING),
                "writer 实存形态 PENDING_SIGNATURE 应可按 value 解析 label（双轨并存时解析失败）");
        assertNull(dict.getLabelByValue("10"), "死数值轨 10 不应存在（value==code 单轨收敛）");
    }

    @Test
    public void testSignProviderDictResolvesWriterCodeForm() {
        DictBean dict = DictProvider.instance().getDict(null, "erp-ct/sign-provider", null, null);
        assertNotNull(dict, "erp-ct/sign-provider 字典应存在");
        assertNotNull(dict.getLabelByValue(ErpCtConstants.SIGNATURE_PROVIDER_MOCK),
                "writer 实存形态 MOCK 应可按 value 解析 label（双轨并存时解析失败）");
        assertNull(dict.getLabelByValue("99"), "死数值轨 99 不应存在（value==code 单轨收敛）");
    }
}
