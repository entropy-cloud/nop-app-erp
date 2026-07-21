package app.erp.md.service.party;

import app.erp.md.biz.IErpPartyBiz;
import app.erp.md.dao.dto.ErpPartyType;
import app.erp.md.dao.dto.PartyRef;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.spi.IErpMdEmployeeReferenceChecker;
import app.erp.md.spi.IErpMdOrganizationReferenceChecker;
import app.erp.md.spi.IErpMdPartnerReferenceChecker;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.TreeBean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.like;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * 统一 Party 跨实体查询实现（{@code docs/design/master-data/unified-party-identity.md §3-§4}）。
 *
 * <p>非实体 BizModel（不继承 {@code CrudBizModel<?>}），镜像 {@code ErpMdDashboardBizModel} 范式：
 * {@code @Inject IDaoProvider}/{@code IOrmTemplate} + 内存聚合。
 *
 * <p>查询策略（owner doc §3.2 Decision）：{@link ErpPartyType} 各实体独立 {@code findAllByQuery} 后
 * Java 内 merge + 字段投影到 {@link PartyRef}，截断到 {@code limit}。
 */
@BizModel("ErpParty")
public class ErpPartyBizModel implements IErpPartyBiz {

    /** keyword < 此阈值返回空 List（避免全表 LIKE 扫描，{@code owner doc §2.1}）。 */
    private static final int MIN_KEYWORD_LENGTH = 2;
    /** findParties 默认行数上限（{@code erp-md.party-search.max-results=50}）。 */
    private static final int DEFAULT_LIMIT = 50;
    /** findParties 硬上限（防止恶意/误操作大 limit）。 */
    private static final int MAX_LIMIT = 200;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    /** 跨域引用计数 SPI（Path A 严格同构，单实例 nullable）。 */
    @Inject
    @Nullable
    protected IErpMdPartnerReferenceChecker partnerReferenceChecker;
    @Inject
    @Nullable
    protected IErpMdEmployeeReferenceChecker employeeReferenceChecker;
    @Inject
    @Nullable
    protected IErpMdOrganizationReferenceChecker organizationReferenceChecker;

    @Override
    @BizQuery
    public List<PartyRef> findParties(@Name("keyword") @Nullable String keyword,
                                      @Name("partyTypes") @Optional Set<ErpPartyType> partyTypes,
                                      @Name("limit") @Optional Integer limit,
                                      IServiceContext context) {
        int effectiveLimit = resolveLimit(limit);
        if (!isValidKeyword(keyword)) {
            return new ArrayList<>();
        }
        String kw = keyword.trim();
        Set<ErpPartyType> types = (partyTypes == null || partyTypes.isEmpty())
                ? EnumSet.allOf(ErpPartyType.class)
                : EnumSet.copyOf(partyTypes);

        List<PartyRef> merged = new ArrayList<>();
        for (ErpPartyType type : types) {
            // 各实体单独查询，截断到 effectiveLimit（防止单实体超大结果集压垮 merge）
            List<PartyRef> refs = findByType(type, kw, effectiveLimit);
            merged.addAll(refs);
            if (merged.size() >= effectiveLimit) {
                break;
            }
        }
        if (merged.size() > effectiveLimit) {
            merged = new ArrayList<>(merged.subList(0, effectiveLimit));
        }
        return merged;
    }

    @Override
    @BizQuery
    public PartyRef getParty(@Name("partyType") @Nullable ErpPartyType partyType,
                             @Name("partyId") @Nullable Long partyId,
                             IServiceContext context) {
        if (partyType == null || partyId == null) {
            return null;
        }
        return loadPartyRef(partyType, partyId);
    }

    @Override
    @BizQuery
    public Map<String, Long> findReferences(@Name("partyType") @Nullable ErpPartyType partyType,
                                            @Name("partyId") @Nullable Long partyId,
                                            IServiceContext context) {
        if (partyType == null || partyId == null) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = null;
        switch (partyType) {
            case PARTNER:
                if (partnerReferenceChecker != null) {
                    result = partnerReferenceChecker.countReferences(partyId);
                }
                break;
            case EMPLOYEE:
                if (employeeReferenceChecker != null) {
                    result = employeeReferenceChecker.countReferences(partyId);
                }
                break;
            case ORGANIZATION:
                if (organizationReferenceChecker != null) {
                    result = organizationReferenceChecker.countReferences(partyId);
                }
                break;
            default:
                return Collections.emptyMap();
        }
        return result == null ? Collections.emptyMap() : result;
    }

    // ===================== helpers =====================

    private static boolean isValidKeyword(String keyword) {
        if (keyword == null) {
            return false;
        }
        String trimmed = keyword.trim();
        return trimmed.length() >= MIN_KEYWORD_LENGTH;
    }

    private static int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 单实体关键字查询。Organization 无 phone/email 列，仅匹配 code/name。
     *
     * <p>实现说明：直接经 {@code dao.findAllByQuery(query)} 查询（非实体 BizModel 无 objMeta filter 限制；
     * LIKE/OR 过滤器可能超出 XMeta 默认允许的 filterOp，绕过 meta 限制），
     * 对齐 {@code ErpMdPartnerBizModel.isCodeUnique}/{@code ErpMdDashboardBizModel.getDashboardKpi} 范式。
     */
    private List<PartyRef> findByType(ErpPartyType type, String keyword, int limit) {
        QueryBean query = new QueryBean();
        query.setLimit(limit);
        query.addFilter(buildKeywordFilter(type, keyword));
        List<PartyRef> result = new ArrayList<>();
        switch (type) {
            case PARTNER: {
                IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
                for (ErpMdPartner e : dao.findAllByQuery(query)) {
                    result.add(toPartyRef(e));
                }
                break;
            }
            case EMPLOYEE: {
                IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
                for (ErpMdEmployee e : dao.findAllByQuery(query)) {
                    result.add(toPartyRef(e));
                }
                break;
            }
            case ORGANIZATION: {
                IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
                for (ErpMdOrganization e : dao.findAllByQuery(query)) {
                    result.add(toPartyRef(e));
                }
                break;
            }
            default:
                break;
        }
        return result;
    }

    /**
     * 构造关键字 OR 过滤器。
     *
     * <p>PARTNER/EMPLOYEE: code/name/phone/email 4 字段 OR LIKE。
     * <p>ORGANIZATION: 仅 code/name 2 字段（无 phone/email 列）。
     */
    private static TreeBean buildKeywordFilter(ErpPartyType type, String keyword) {
        String pattern = "%" + keyword + "%";
        switch (type) {
            case PARTNER:
            case EMPLOYEE:
                return or(
                        like("code", pattern),
                        like("name", pattern),
                        like("phone", pattern),
                        like("email", pattern)
                );
            case ORGANIZATION:
                return or(
                        like("code", pattern),
                        like("name", pattern)
                );
            default:
                throw new IllegalArgumentException("Unsupported ErpPartyType: " + type);
        }
    }

    private PartyRef loadPartyRef(ErpPartyType type, Long partyId) {
        switch (type) {
            case PARTNER: {
                ErpMdPartner e = daoProvider.daoFor(ErpMdPartner.class).getEntityById(partyId);
                return e == null ? null : toPartyRef(e);
            }
            case EMPLOYEE: {
                ErpMdEmployee e = daoProvider.daoFor(ErpMdEmployee.class).getEntityById(partyId);
                return e == null ? null : toPartyRef(e);
            }
            case ORGANIZATION: {
                ErpMdOrganization e = daoProvider.daoFor(ErpMdOrganization.class).getEntityById(partyId);
                return e == null ? null : toPartyRef(e);
            }
            default:
                return null;
        }
    }

    // ---------- entity → PartyRef projectors ----------

    private static PartyRef toPartyRef(ErpMdPartner e) {
        PartyRef ref = new PartyRef();
        ref.setPartyType(ErpPartyType.PARTNER);
        ref.setPartyId(e.getId());
        ref.setCode(e.getCode());
        ref.setName(e.getName());
        ref.setPhone(e.getPhone());
        ref.setEmail(e.getEmail());
        ref.setStatus(e.getStatus());
        ref.setDisplayName(buildDisplayName(e.getCode(), e.getName()));
        ref.putExtension("partnerType", e.getPartnerType());
        return ref;
    }

    private static PartyRef toPartyRef(ErpMdEmployee e) {
        PartyRef ref = new PartyRef();
        ref.setPartyType(ErpPartyType.EMPLOYEE);
        ref.setPartyId(e.getId());
        ref.setCode(e.getCode());
        ref.setName(e.getName());
        ref.setPhone(e.getPhone());
        ref.setEmail(e.getEmail());
        ref.setStatus(e.getStatus());
        ref.setDisplayName(buildDisplayName(e.getCode(), e.getName()));
        ref.putExtension("position", e.getPosition());
        ref.putExtension("orgId", e.getOrgId());
        ref.putExtension("partnerId", e.getPartnerId());
        return ref;
    }

    private static PartyRef toPartyRef(ErpMdOrganization e) {
        PartyRef ref = new PartyRef();
        ref.setPartyType(ErpPartyType.ORGANIZATION);
        ref.setPartyId(e.getId());
        ref.setCode(e.getCode());
        ref.setName(e.getName());
        // Organization 无 phone/email 列（owner doc §2.1 容忍字段缺失），投影为 null
        ref.setPhone(null);
        ref.setEmail(null);
        ref.setStatus(e.getStatus());
        ref.setDisplayName(buildDisplayName(e.getCode(), e.getName()));
        ref.putExtension("orgType", e.getOrgType());
        ref.putExtension("parentId", e.getParentId());
        ref.putExtension("functionalCurrencyId", e.getFunctionalCurrencyId());
        return ref;
    }

    private static String buildDisplayName(String code, String name) {
        if (code == null || code.isEmpty()) {
            return name;
        }
        if (name == null || name.isEmpty()) {
            return code;
        }
        return code + " - " + name;
    }
}
