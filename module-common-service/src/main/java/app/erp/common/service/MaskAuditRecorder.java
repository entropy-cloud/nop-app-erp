package app.erp.common.service;

import io.nop.api.core.audit.AuditRequest;
import io.nop.api.core.audit.IAuditService;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.json.JSON;
import io.nop.api.core.time.CoreMetrics;
import io.nop.orm.IOrmEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * E4.2 保密字段读访问审计记录器（plan 2026-08-11-1030-1）。
 *
 * <p>由 {@link MaskHelper} 在 authorized-clear-text 分支委托（单一 chokepoint，覆盖全 5 域 15 BizModel
 * masking 面）。授权用户读取保密字段明文 = 敏感数据披露事件 → 经本记录器写审计记录。
 *
 * <p><b>存储机制</b>（Phase 1 Decision (b)）：复用平台 {@link IAuditService#saveAudit(AuditRequest)}
 * → 写 {@code NopAuthOpLog}（平台批处理异步写入，零阻塞主请求）。**无 ORM 新实体，无平台代码改动**
 * （保护区域 ask-first 未触发）。
 *
 * <p><b>粒度策略</b>（Phase 1 Decision (c)）：按实体去重窗口——同一 {@code userId × entityName × objId
 * × fieldName} 在同一请求线程内仅记一次（ThreadLocal {@link LinkedHashSet}）。上限 {@link #MAX_DEDUP_ENTRIES}
 * 防泄漏；典型 list 请求（100 行 × 13 字段）去重后 = 13 条审计。
 *
 * <p><b>config-gate</b>（Phase 1 Decision (d)）：{@link #CONFIG_FIELD_READ_AUDIT_ENABLED} 默认 false；
 * %test=ON / %dev=%prod=OFF。fail-safe：OFF 时 {@link #recordDisclosure} 首行 return，零开销。
 *
 * <p><b>fail-closed 语义保持</b>：无 {@link IUserContext}（masking fail-closed = 不披露）→
 * {@link #recordDisclosure} 直接 return（无披露事件 → 无审计）。
 *
 * <p>本组件为 IoC bean（注册于 {@code app-service.beans.xml}），承载 {@link IAuditService} 注入；
 * {@link MaskHelper} 静态方法经 {@link #instance()} 静态引用委托本组件。
 */
public class MaskAuditRecorder {

    public static final String CONFIG_FIELD_READ_AUDIT_ENABLED = "erp.audit.field-read.enabled";

    public static final String OPERATION_FIELD_READ_DISCLOSURE = "FIELD_READ_DISCLOSURE";

    static final int MAX_DEDUP_ENTRIES = 500;

    private static volatile MaskAuditRecorder instance;

    private final ThreadLocal<LinkedHashSet<DedupKey>> tlDedup = ThreadLocal.withInitial(LinkedHashSet::new);

    private IAuditService auditService;

    public static MaskAuditRecorder instance() {
        return instance;
    }

    static void setInstance(MaskAuditRecorder inst) {
        instance = inst;
    }

    @Inject
    public void setAuditService(IAuditService auditService) {
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        setInstance(this);
    }

    @PreDestroy
    public void destroy() {
        if (instance == this) {
            instance = null;
        }
    }

    public static boolean isEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(CONFIG_FIELD_READ_AUDIT_ENABLED, Boolean.FALSE));
    }

    /**
     * 由 {@link MaskHelper} 调用的静态入口：经 IoC 注入的 {@link #instance()} 委托。
     *
     * <p>无 IoC（纯单元测试场景）= instance null = 直接 return（fail-safe）。
     */
    static void recordDisclosureIfEnabled(Object entity, String fieldName, String authorizedRole) {
        MaskAuditRecorder inst = instance;
        if (inst == null) {
            return;
        }
        inst.recordDisclosure(entity, fieldName, authorizedRole);
    }

    /**
     * 记录一次保密字段明文披露事件（authorized-clear-text 路径）。
     *
     * <p>调用契约：仅当 {@link MaskHelper#isAuthorized(Set)} 返回 true（即授权角色拿到明文）时调用。
     *
     * @param entity        被读取的实体（用于提取 entityName/objId）
     * @param fieldName     被读取的字段名
     * @param authorizedRole 命中的授权角色（roleId 字面，与 {@code nop_auth_role.csv} 一致）
     */
    public void recordDisclosure(Object entity, String fieldName, String authorizedRole) {
        if (!isEnabled()) {
            return;
        }
        IAuditService svc = auditService;
        if (svc == null) {
            return;
        }
        IUserContext ctx = IUserContext.get();
        if (ctx == null) {
            return;
        }

        String entityName = resolveEntityName(entity);
        String objId = resolveObjId(entity);

        DedupKey key = new DedupKey(ctx.getUserId(), entityName, objId, fieldName);
        LinkedHashSet<DedupKey> seen = tlDedup.get();
        if (seen.size() >= MAX_DEDUP_ENTRIES) {
            seen.clear();
        }
        if (!seen.add(key)) {
            return;
        }

        AuditRequest req = new AuditRequest();
        req.setOperation(OPERATION_FIELD_READ_DISCLOSURE);
        req.setEntityId(buildEntityId(entityName, objId));
        req.setDescription("Authorized disclosure of confidential field " + fieldName
                + " on " + entityName + " to role " + authorizedRole);
        req.setUserId(ctx.getUserId());
        req.setUserName(ctx.getUserName());
        req.setSessionId(ctx.getSessionId());
        req.setTenantId(ctx.getTenantId());
        Timestamp now = CoreMetrics.currentTimestamp();
        req.setActionTime(now);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("entity", entityName);
        details.put("field", fieldName);
        details.put("objId", objId);
        details.put("authorizedRole", authorizedRole);
        details.put("disclosedAt", now.toString());
        req.setRequestData(JSON.stringify(details));

        try {
            svc.saveAudit(req);
        } catch (Exception e) {
            throw NopException.adapt(e);
        }
    }

    static String resolveEntityName(Object entity) {
        if (entity instanceof IOrmEntity) {
            String name = ((IOrmEntity) entity).orm_entityName();
            if (name != null && !name.isEmpty()) {
                int dot = name.lastIndexOf('.');
                if (dot >= 0 && dot < name.length() - 1) {
                    return name.substring(dot + 1);
                }
                return name;
            }
        }
        return entity == null ? "null" : entity.getClass().getSimpleName();
    }

    static String resolveObjId(Object entity) {
        if (entity instanceof IOrmEntity) {
            return ((IOrmEntity) entity).orm_idString();
        }
        return null;
    }

    private static String buildEntityId(String entityName, String objId) {
        return objId == null ? entityName : entityName + ":" + objId;
    }

    static void clearThreadDedup() {
        LinkedHashSet<DedupKey> seen = instance == null ? null : instance.tlDedup.get();
        if (seen != null) {
            seen.clear();
        }
    }

    static int threadDedupSize() {
        LinkedHashSet<DedupKey> seen = instance == null ? null : instance.tlDedup.get();
        return seen == null ? 0 : seen.size();
    }

    static final class DedupKey {
        final String userId;
        final String entityName;
        final String objId;
        final String fieldName;

        DedupKey(String userId, String entityName, String objId, String fieldName) {
            this.userId = userId;
            this.entityName = entityName;
            this.objId = objId;
            this.fieldName = fieldName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DedupKey)) return false;
            DedupKey k = (DedupKey) o;
            return java.util.Objects.equals(userId, k.userId)
                    && java.util.Objects.equals(entityName, k.entityName)
                    && java.util.Objects.equals(objId, k.objId)
                    && java.util.Objects.equals(fieldName, k.fieldName);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, entityName, objId, fieldName);
        }
    }
}
