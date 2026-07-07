package app.erp.cs.service.entity;

import app.erp.cs.dao.entity.ErpCsEntitlement;
import app.erp.cs.service.ErpCsConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link EntitlementMatcher} 纯单元测试（entitlement.md §2.1/§三）。覆盖：
 * <ul>
 *   <li>各 serviceType（WARRANTY/SUPPORT_CONTRACT/PAY_PER_TICKET）匹配。</li>
 *   <li>期间失效过滤（startDate/endDate 越界）。</li>
 *   <li>余量耗尽过滤（usedTickets≥maxTickets）。</li>
 *   <li>endDate 最近者优先（取 min endDate）。</li>
 *   <li>SLA 覆盖（maxResolutionTime/maxResponseTime 不为空时返回）。</li>
 *   <li>无匹配返回 null（候选为空 / partnerId 不匹配 / isActive=false）。</li>
 * </ul>
 *
 * <p>使用 {@link JunitAutoTestCase} 仅为获取 IoC 容器；无 DB 读写，加载函数以 lambda 注入。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestEntitlementMatcher extends JunitAutoTestCase {

    private static final LocalDate NOW = LocalDate.of(2026, 7, 7);
    private static final Long PARTNER_ID = 9001L;

    @Test
    public void testWarrantyMatch() {
        ErpCsEntitlement warranty = entitlement(101L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_WARRANTY,
                NOW.minusDays(10), NOW.plusDays(20), null, null, true);
        ErpCsEntitlement matched = EntitlementMatcher.match(PARTNER_ID, NOW,
                pid -> Collections.singletonList(warranty));
        assertEquals(101L, matched.getId());
    }

    @Test
    public void testSupportContractMatch() {
        ErpCsEntitlement contract = entitlement(102L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_SUPPORT_CONTRACT,
                NOW.minusDays(100), NOW.plusDays(200), null, null, true);
        ErpCsEntitlement matched = EntitlementMatcher.match(PARTNER_ID, NOW,
                pid -> Collections.singletonList(contract));
        assertEquals(102L, matched.getId());
    }

    @Test
    public void testPayPerTicketWithQuotaMatch() {
        ErpCsEntitlement ppt = entitlement(103L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET,
                NOW.minusDays(5), NOW.plusDays(30), 5, 2, true);
        ErpCsEntitlement matched = EntitlementMatcher.match(PARTNER_ID, NOW,
                pid -> Collections.singletonList(ppt));
        assertEquals(103L, matched.getId());
    }

    @Test
    public void testPeriodExpiredFiltered() {
        ErpCsEntitlement expired = entitlement(104L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_WARRANTY,
                NOW.minusDays(30), NOW.minusDays(1), null, null, true);
        ErpCsEntitlement matched = EntitlementMatcher.match(PARTNER_ID, NOW,
                pid -> Collections.singletonList(expired));
        assertNull(matched, "已过期（endDate<now）应被过滤");
    }

    @Test
    public void testPeriodNotStartedFiltered() {
        ErpCsEntitlement future = entitlement(105L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_WARRANTY,
                NOW.plusDays(1), NOW.plusDays(30), null, null, true);
        ErpCsEntitlement matched = EntitlementMatcher.match(PARTNER_ID, NOW,
                pid -> Collections.singletonList(future));
        assertNull(matched, "未生效（startDate>now）应被过滤");
    }

    @Test
    public void testQuotaExhaustedFiltered() {
        ErpCsEntitlement exhausted = entitlement(106L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET,
                NOW.minusDays(5), NOW.plusDays(30), 5, 5, true);
        ErpCsEntitlement matched = EntitlementMatcher.match(PARTNER_ID, NOW,
                pid -> Collections.singletonList(exhausted));
        assertNull(matched, "余量耗尽（usedTickets≥maxTickets）应被过滤");
    }

    @Test
    public void testInactiveFiltered() {
        ErpCsEntitlement inactive = entitlement(107L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_WARRANTY,
                NOW.minusDays(5), NOW.plusDays(30), null, null, false);
        ErpCsEntitlement matched = EntitlementMatcher.match(PARTNER_ID, NOW,
                pid -> Collections.singletonList(inactive));
        assertNull(matched, "isActive=false 应被过滤");
    }

    @Test
    public void testPartnerMismatchFiltered() {
        ErpCsEntitlement other = entitlement(108L, 8888L,
                ErpCsConstants.SERVICE_TYPE_WARRANTY,
                NOW.minusDays(5), NOW.plusDays(30), null, null, true);
        ErpCsEntitlement matched = EntitlementMatcher.match(PARTNER_ID, NOW,
                pid -> Collections.singletonList(other));
        assertNull(matched, "partnerId 不匹配应被过滤");
    }

    @Test
    public void testNearestEndDatePreferred() {
        // 候选：远期 + 近期 + 中期，应取 endDate 最近者（即将到期优先消费）
        ErpCsEntitlement far = entitlement(201L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_WARRANTY, NOW.minusDays(1), NOW.plusDays(90), null, null, true);
        ErpCsEntitlement near = entitlement(202L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_WARRANTY, NOW.minusDays(1), NOW.plusDays(5), null, null, true);
        ErpCsEntitlement mid = entitlement(203L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_WARRANTY, NOW.minusDays(1), NOW.plusDays(30), null, null, true);
        List<ErpCsEntitlement> candidates = Arrays.asList(far, near, mid);
        ErpCsEntitlement matched = EntitlementMatcher.match(PARTNER_ID, NOW, pid -> candidates);
        assertEquals(202L, matched.getId(), "应取 endDate 最近者（near）");
    }

    @Test
    public void testNullMaxTicketsUnlimitedQuota() {
        // maxTickets=null（无限余量），usedTickets 不参与过滤
        ErpCsEntitlement unlimited = entitlement(301L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_SUPPORT_CONTRACT,
                NOW.minusDays(5), NOW.plusDays(30), null, 9999, true);
        ErpCsEntitlement matched = EntitlementMatcher.match(PARTNER_ID, NOW,
                pid -> Collections.singletonList(unlimited));
        assertEquals(301L, matched.getId(), "maxTickets=null 时余量无限，应匹配");
    }

    @Test
    public void testEmptyCandidatesReturnsNull() {
        ErpCsEntitlement matched = EntitlementMatcher.match(PARTNER_ID, NOW, pid -> Collections.emptyList());
        assertNull(matched);
    }

    @Test
    public void testNullCustomerIdReturnsNull() {
        ErpCsEntitlement matched = EntitlementMatcher.match(null, NOW, pid -> Collections.emptyList());
        assertNull(matched);
    }

    @Test
    public void testResolveSlaOverrideMinutes() {
        ErpCsEntitlement withOverride = entitlement(401L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_SUPPORT_CONTRACT,
                NOW.minusDays(1), NOW.plusDays(30), null, null, true);
        withOverride.setMaxResolutionTime(480);
        assertEquals(480, EntitlementMatcher.resolveSlaOverrideMinutes(withOverride));

        ErpCsEntitlement noOverride = entitlement(402L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_WARRANTY,
                NOW.minusDays(1), NOW.plusDays(30), null, null, true);
        assertNull(EntitlementMatcher.resolveSlaOverrideMinutes(noOverride), "maxResolutionTime 为 null 时返回 null");

        assertNull(EntitlementMatcher.resolveSlaOverrideMinutes(null), "权益为 null 时返回 null");
    }

    @Test
    public void testResolveResponseOverrideMinutes() {
        ErpCsEntitlement withResp = entitlement(501L, PARTNER_ID,
                ErpCsConstants.SERVICE_TYPE_SUPPORT_CONTRACT,
                NOW.minusDays(1), NOW.plusDays(30), null, null, true);
        withResp.setMaxResponseTime(120);
        assertEquals(120, EntitlementMatcher.resolveResponseOverrideMinutes(withResp));
    }

    private ErpCsEntitlement entitlement(Long id, Long partnerId, String serviceType,
                                          LocalDate start, LocalDate end,
                                          Integer maxTickets, Integer usedTickets, boolean active) {
        ErpCsEntitlement e = new ErpCsEntitlement();
        e.setId(id);
        e.setPartnerId(partnerId);
        e.setServiceType(serviceType);
        e.setStartDate(start);
        e.setEndDate(end);
        e.setMaxTickets(maxTickets);
        e.setUsedTickets(usedTickets);
        e.setIsActive(active);
        return e;
    }
}
