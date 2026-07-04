package app.erp.fin.service.entity;

import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import io.nop.sys.dao.entity.NopSysChangeLog;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 凭证模板变更审计测试（计划 {@code 2026-07-04-1452-1} Phase 3）。
 *
 * <p>验证复用平台 {@link NopSysChangeLog}（{@link io.nop.sys.dao.log.OrmEntityChangeLogInterceptor}）
 * 记录 {@link ErpFinVoucherTemplate}/{@link ErpFinVoucherTemplateLine} 增删改的字段级 old→new。
 * 实体声明 {@code tagSet="audit,audit-save"} 后，平台在 ORM 层兜底所有写路径，业务代码零侵入
 * （见 {@code posting-log.md §裁决1}）。
 *
 * <p>断言：save 记初始全量（operationName=save，每非 version 列一行）、update 记已变更字段 old/new、
 * delete 记删除标志。{@code bizKey} 取自实体 {@code orm:bizKeyProp="code"}（模板编码）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinVoucherTemplateAuditLog extends JunitAutoTestCase {

    static final String BIZ_OBJ_TEMPLATE = "ErpFinVoucherTemplate";
    static final String BIZ_OBJ_TEMPLATE_LINE = "ErpFinVoucherTemplateLine";
    static final String BUSINESS_TYPE_AP_INVOICE = "AP_INVOICE";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testTemplateSaveUpdateDeleteAudited() {
        final Long[] templateIdHolder = new Long[1];
        final String originalName = "应付发票模板-原";
        final String updatedName = "应付发票模板-改";

        // save：经 audit-save 记初始全量
        ormTemplate.runInSession(() -> {
            ErpFinVoucherTemplate tpl = new ErpFinVoucherTemplate();
            tpl.setCode("TPL-AUDIT-001");
            tpl.setName(originalName);
            tpl.setBusinessType(BUSINESS_TYPE_AP_INVOICE);
            tpl.setVoucherType("TRANSFER");
            tpl.setIsActive(true);
            daoProvider.daoFor(ErpFinVoucherTemplate.class).saveEntity(tpl);
            templateIdHolder[0] = tpl.getId();
            ormTemplate.flushSession();
        });
        Long templateId = templateIdHolder[0];

        List<NopSysChangeLog> saveLogs = findLogs(BIZ_OBJ_TEMPLATE, templateId.toString(), "save");
        assertTrue(saveLogs.size() >= 4,
                "audit-save 应记模板初始全量（code/name/businessType/voucherType/isActive 等多列），实际：" + saveLogs.size());
        assertTrue(saveLogs.stream().anyMatch(l -> "code".equals(l.getPropName())
                        && "TPL-AUDIT-001".equals(l.getNewValue())),
                "save 日志应含 code 列的 newValue");
        assertTrue(saveLogs.stream().allMatch(l -> "TPL-AUDIT-001".equals(l.getBizKey())),
                "变更日志 bizKey 应取自 orm:bizKeyProp=code");

        // update：只记已变更字段 old→new
        ormTemplate.runInSession(() -> {
            ErpFinVoucherTemplate tpl = daoProvider.daoFor(ErpFinVoucherTemplate.class).getEntityById(templateId);
            tpl.setName(updatedName);
            daoProvider.daoFor(ErpFinVoucherTemplate.class).updateEntity(tpl);
            ormTemplate.flushSession();
        });

        List<NopSysChangeLog> updateLogs = findLogs(BIZ_OBJ_TEMPLATE, templateId.toString(), "update");
        assertTrue(updateLogs.size() <= 3,
                "update 应只记已变更字段（name + 框架 auto 字段），实际：" + updateLogs.size());
        NopSysChangeLog nameLog = updateLogs.stream()
                .filter(l -> "name".equals(l.getPropName()))
                .findFirst()
                .orElse(null);
        assertTrue(nameLog != null, "update 日志应含 name 列的 old→new 记录");
        assertEquals(originalName, nameLog.getOldValue(), "name oldValue");
        assertEquals(updatedName, nameLog.getNewValue(), "name newValue");

        // delete：实体 useLogicalDelete=true，deleteEntity 经逻辑删除（置 delVersion）→ 平台记为 update
        // 审计记录（propName=delVersion 的 old→new）。断言该逻辑删除轨迹存在。
        ormTemplate.runInSession(() -> {
            ErpFinVoucherTemplate tpl = daoProvider.daoFor(ErpFinVoucherTemplate.class).getEntityById(templateId);
            daoProvider.daoFor(ErpFinVoucherTemplate.class).deleteEntity(tpl);
            ormTemplate.flushSession();
        });

        List<NopSysChangeLog> allTemplateLogs = findAllLogs(BIZ_OBJ_TEMPLATE, templateId.toString());
        assertTrue(allTemplateLogs.stream().anyMatch(l -> "delVersion".equals(l.getPropName())
                        && l.getNewValue() != null && !"0".equals(l.getNewValue())),
                "逻辑删除应在审计日志留 delVersion 非 0 的变更记录（actual：" + describeProps(allTemplateLogs) + "）");
    }

    @Test
    public void testTemplateLineSaveAudited() {
        final Long[] lineIdHolder = new Long[1];

        ormTemplate.runInSession(() -> {
            ErpFinVoucherTemplate tpl = new ErpFinVoucherTemplate();
            tpl.setCode("TPL-AUDIT-LINE-001");
            tpl.setName("行审计模板");
            tpl.setBusinessType(BUSINESS_TYPE_AP_INVOICE);
            tpl.setVoucherType("TRANSFER");
            tpl.setIsActive(true);
            daoProvider.daoFor(ErpFinVoucherTemplate.class).saveEntity(tpl);

            ErpFinVoucherTemplateLine line = new ErpFinVoucherTemplateLine();
            line.setTemplateId(tpl.getId());
            line.setLineNo(1);
            line.setSubjectCode("2202");
            line.setDcDirection("CREDIT");
            line.setAmountKey("TOTAL");
            daoProvider.daoFor(ErpFinVoucherTemplateLine.class).saveEntity(line);
            lineIdHolder[0] = line.getId();
            ormTemplate.flushSession();
        });

        List<NopSysChangeLog> lineSaveLogs = findLogs(BIZ_OBJ_TEMPLATE_LINE, lineIdHolder[0].toString(), "save");
        assertTrue(lineSaveLogs.size() >= 3,
                "audit-save 应记模板行初始全量（subjectCode/dcDirection/amountKey 等），实际：" + lineSaveLogs.size());
        assertTrue(lineSaveLogs.stream().anyMatch(l -> "subjectCode".equals(l.getPropName())
                        && "2202".equals(l.getNewValue())),
                "行 save 日志应含 subjectCode 列的 newValue");
    }

    private List<NopSysChangeLog> findLogs(String bizObjName, String objId, String operationName) {
        IEntityDao<NopSysChangeLog> dao = daoProvider.daoFor(NopSysChangeLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("bizObjName", bizObjName));
        q.addFilter(eq("objId", objId));
        q.addFilter(eq("operationName", operationName));
        return dao.findAllByQuery(q);
    }

    private List<NopSysChangeLog> findAllLogs(String bizObjName, String objId) {
        IEntityDao<NopSysChangeLog> dao = daoProvider.daoFor(NopSysChangeLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("bizObjName", bizObjName));
        q.addFilter(eq("objId", objId));
        return dao.findAllByQuery(q);
    }

    private static String describeProps(List<NopSysChangeLog> logs) {
        StringBuilder sb = new StringBuilder();
        for (NopSysChangeLog l : logs) {
            sb.append('[').append(l.getOperationName()).append('/').append(l.getPropName())
                    .append(':').append(l.getOldValue()).append("->").append(l.getNewValue()).append("] ");
        }
        return sb.toString();
    }
}
