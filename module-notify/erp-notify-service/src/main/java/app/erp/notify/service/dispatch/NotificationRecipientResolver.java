package app.erp.notify.service.dispatch;

import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import app.erp.notify.service.ErpNotifyErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import io.nop.auth.dao.entity.NopAuthRole;
import io.nop.auth.dao.entity.NopAuthUser;
import io.nop.auth.dao.entity.NopAuthUserRole;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 通知接收人解析器。按模板 {@code recipientResolver} 类型解析为目标 userId 集合。
 *
 * <p>解析策略（Decision Phase 1）：
 * <ul>
 *   <li>{@link ErpNotifyConstants#RESOLVER_ROLE ROLE}：复用平台 nop-auth，角色名 → NopAuthUserRole → userId 集合</li>
 *   <li>{@link ErpNotifyConstants#RESOLVER_USER_LIST USER_LIST}：模板配置 userId 列表（静态）</li>
 *   <li>{@link ErpNotifyConstants#RESOLVER_ORG ORG}：deptId 下用户</li>
 *   <li>{@link ErpNotifyConstants#RESOLVER_PARTNER PARTNER}：partnerId 关联用户（bootstrap 暂无映射，WARN 返回空）</li>
 * </ul>
 *
 * <p>注：此处对平台 nop-auth 实体（NopAuthUser/Role/UserRole）为只读查询，使用 IDaoProvider/IOrmTemplate
 * 而非 I*Biz——平台实体无应用层 IBiz 包装，且为纯读访问。
 */
public class NotificationRecipientResolver {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationRecipientResolver.class);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 解析接收人 userId 集合。
     *
     * @param template   模板（持有 recipientResolver 与 recipientConfig）
     * @return userId 集合（String，对齐 stdDomain userId）；config-gated 无匹配时返回空集
     */
    public Set<String> resolve(ErpSysNotificationTemplate template) {
        String resolver = template.getRecipientResolver();
        Map<String, Object> cfg = parseConfig(template.getRecipientConfig());

        switch (StringHelper.toString(resolver, ErpNotifyConstants.RESOLVER_ROLE)) {
            case ErpNotifyConstants.RESOLVER_USER_LIST:
                return resolveUserList(cfg);
            case ErpNotifyConstants.RESOLVER_ORG:
                return resolveOrg(cfg);
            case ErpNotifyConstants.RESOLVER_PARTNER:
                return resolvePartner(template, cfg);
            case ErpNotifyConstants.RESOLVER_ROLE:
            default:
                return resolveRole(cfg);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> resolveUserList(Map<String, Object> cfg) {
        Object userIds = cfg.get("userIds");
        if (userIds instanceof List) {
            Set<String> ret = new LinkedHashSet<>();
            for (Object o : (List<Object>) userIds) {
                if (o != null && !StringHelper.isBlank(o.toString())) {
                    ret.add(o.toString());
                }
            }
            return ret;
        }
        return Collections.emptySet();
    }

    private Set<String> resolveRole(Map<String, Object> cfg) {
        Object rolesObj = cfg.get("roles");
        if (!(rolesObj instanceof List) || ((List<?>) rolesObj).isEmpty()) {
            return Collections.emptySet();
        }
        List<String> roleNames = new ArrayList<>();
        for (Object o : (List<?>) rolesObj) {
            if (o != null) roleNames.add(o.toString());
        }
        // 角色名 → roleId
        QueryBean roleQ = new QueryBean();
        roleQ.addFilter(in("roleName", roleNames));
        IEntityDao<NopAuthRole> roleDao = daoProvider.daoFor(NopAuthRole.class);
        List<NopAuthRole> roles = roleDao.findAllByQuery(roleQ);
        if (roles.isEmpty()) {
            LOG.warn("notify.resolve-role: 角色名{}无匹配 NopAuthRole，config-gated 返回空", roleNames);
            return Collections.emptySet();
        }
        List<String> roleIds = new ArrayList<>(roles.size());
        Set<String> roleNameSet = new HashSet<>(roleNames);
        for (NopAuthRole r : roles) {
            if (roleNameSet.contains(r.getRoleName())) {
                roleIds.add(r.getRoleId());
            }
        }
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        // roleId → userId
        QueryBean urQ = new QueryBean();
        urQ.addFilter(in("roleId", roleIds));
        List<NopAuthUserRole> urs = daoProvider.daoFor(NopAuthUserRole.class).findAllByQuery(urQ);
        Set<String> ret = new LinkedHashSet<>();
        for (NopAuthUserRole ur : urs) {
            if (!StringHelper.isBlank(ur.getUserId())) {
                ret.add(ur.getUserId());
            }
        }
        return ret;
    }

    private Set<String> resolveOrg(Map<String, Object> cfg) {
        Object deptId = cfg.get("deptId");
        if (deptId == null || StringHelper.isBlank(deptId.toString())) {
            return Collections.emptySet();
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("deptId", deptId.toString()));
        List<NopAuthUser> users = daoProvider.daoFor(NopAuthUser.class).findAllByQuery(q);
        Set<String> ret = new LinkedHashSet<>();
        for (NopAuthUser u : users) {
            if (!StringHelper.isBlank(u.getUserId())) {
                ret.add(u.getUserId());
            }
        }
        return ret;
    }

    private Set<String> resolvePartner(ErpSysNotificationTemplate template, Map<String, Object> cfg) {
        // bootstrap 阶段业务伙伴→用户的映射尚未建立，站内消息按 partner 不可达；config-gated 返回空。
        Object partnerId = cfg.get("partnerId");
        LOG.warn("notify.resolve-partner: 模板[{}] partnerId={} 暂无 partner→user 映射，config-gated 返回空",
                template.getId(), partnerId);
        return Collections.emptySet();
    }

    private Map<String, Object> parseConfig(String recipientConfig) {
        if (StringHelper.isBlank(recipientConfig)) {
            return Collections.emptyMap();
        }
        try {
            Object parsed = io.nop.core.lang.json.JsonTool.parseNonStrict(recipientConfig);
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) parsed;
                return m;
            }
        } catch (Exception e) {
            throw new NopException(ErpNotifyErrors.ERR_NOTIFY_RECIPIENT_RESOLVE_FAILED, e)
                    .param(ErpNotifyErrors.ARG_NOTIFICATION_TYPE, "")
                    .param(ErpNotifyErrors.ARG_RESOLVER, "parseConfig")
                    .param(ErpNotifyErrors.ARG_REASON, "recipientConfig 非合法 JSON: " + e.getMessage());
        }
        return Collections.emptyMap();
    }

    // 兼容：标记当前时间，供 merge 判定引用
    public long nowMillis() {
        return CoreMetrics.currentTimeMillis();
    }
}
