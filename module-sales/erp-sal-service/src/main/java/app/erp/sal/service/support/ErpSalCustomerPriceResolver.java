package app.erp.sal.service.support;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.dto.ResolvedPrice;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.spi.IErpMdCustomerPriceResolver;
import app.erp.sal.dao.entity.ErpSalPriceList;
import app.erp.sal.dao.entity.ErpSalPriceListLine;

import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpSalPriceList、ErpSalPriceListLine）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * 客户价格清单解析器（UC-SAL-11 定价引擎取价层）。
 *
 * <p>实现 {@link IErpMdCustomerPriceResolver} SPI，由 master-data 域 {@code ErpMdMaterialSkuBizModel}
 * 经 {@code @Inject @Nullable} 注入。匹配逻辑：
 * <ol>
 *   <li>按 partnerId 精确匹配价格清单头，否则按 {@code partner.customerGroup} 匹配清单头 {@code customerGroupCode}。</li>
 *   <li>清单须 isActive=true 且期间有效（validFrom/validTo，空端开放）。</li>
 *   <li>行匹配：skuId 或 materialId + uoMId（可选）+ 数量阶梯（minQuantity/maxQuantity）+ 币种。</li>
 *   <li>按清单 priority（小优先）选最优命中。</li>
 * </ol>
 *
 * <p>使用 QueryBean 构造查询（避免 @SqlLibMapper 引入复杂映射），取价为高频读路径但价格清单行数据量可控。
 */
public class ErpSalCustomerPriceResolver implements IErpMdCustomerPriceResolver {

    private static final String SOURCE_PRICE_LIST = "PRICE_LIST";

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Override
    public ResolvedPrice resolveCustomerPrice(ErpMdMaterialSku sku, String partnerId,
                                              BigDecimal quantity, String currencyId,
                                              IServiceContext context) {
        if (sku == null) {
            return null;
        }
        BigDecimal qty = quantity != null ? quantity : BigDecimal.ONE;
        LocalDate today = CoreMetrics.currentDate();

        List<ErpSalPriceList> candidates = findCandidatePriceLists(partnerId, currencyId, today, context);
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator.comparingInt(
                p -> p.getPriority() == null ? Integer.MAX_VALUE : p.getPriority()));

        for (ErpSalPriceList priceList : candidates) {
            ErpSalPriceListLine line = matchLine(priceList.getId(), sku, qty, today, context);
            if (line != null && line.getUnitPrice() != null) {
                return new ResolvedPrice(line.getUnitPrice(),
                        SOURCE_PRICE_LIST,
                        priceList.getId(), priceList.getName());
            }
        }
        return null;
    }

    /**
     * 查找候选价格清单：按 partnerId 精确匹配 + 按客户组匹配，均须 isActive + 期间有效 + 币种匹配。
     */
    protected List<ErpSalPriceList> findCandidatePriceLists(String partnerId, String currencyId,
                                                            LocalDate today, IServiceContext context) {
        String customerGroup = resolveCustomerGroup(partnerId, context);

        IEntityDao<ErpSalPriceList> dao = daoProvider.daoFor(ErpSalPriceList.class);
        QueryBean query = new QueryBean();
        query.addFilter(eq("isActive", Boolean.TRUE));
        // 期间匹配在内存完成（validFrom/validTo 空端开放），避免对 nullable 列加 IS NULL OR 条件复杂化 QueryBean
        List<ErpSalPriceList> all = dao.findAllByQuery(query);
        List<ErpSalPriceList> result = new java.util.ArrayList<>();
        for (ErpSalPriceList pl : all) {
            if (!matchesPeriod(pl, today)) {
                continue;
            }
            if (currencyId != null && !Objects.equals(pl.getCurrencyId(), currencyId)) {
                continue;
            }
            boolean matched = false;
            if (partnerId != null && Objects.equals(pl.getPartnerId(), partnerId)) {
                matched = true;
            } else if (customerGroup != null && !customerGroup.isEmpty()
                    && Objects.equals(pl.getCustomerGroupCode(), customerGroup)
                    && pl.getPartnerId() == null) {
                matched = true;
            }
            if (matched) {
                result.add(pl);
            }
        }
        return result;
    }

    /**
     * 从 partner.customerGroup 解析客户组标签。
     */
    protected String resolveCustomerGroup(String partnerId, IServiceContext context) {
        if (partnerId == null) {
            return null;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(partnerId, context);
        return partner == null ? null : partner.getCustomerGroup();
    }

    /**
     * 在指定清单下匹配最优行：skuId 优先 > materialId + 数量阶梯 + 行级期间。
     * P2-CK-sal-008：多命中时按确定性 comparator 取优——skuId 专属命中 > materialId 通用命中 >
     * 更窄数量阶梯（minQuantity 更大）> validFrom 更晚 > lineNo 兜底；修复原「迭代序第一条」的
     * DB 返回顺序不确定性（sku 专属价可能被 material 通用价遮蔽）。
     */
    protected ErpSalPriceListLine matchLine(String priceListId, ErpMdMaterialSku sku,
                                            BigDecimal qty, LocalDate today,
                                            IServiceContext context) {
        IEntityDao<ErpSalPriceListLine> dao = daoProvider.daoFor(ErpSalPriceListLine.class);
        QueryBean query = new QueryBean();
        query.addFilter(eq("priceListId", priceListId));
        List<ErpSalPriceListLine> lines = dao.findAllByQuery(query);

        ErpSalPriceListLine best = null;
        for (ErpSalPriceListLine line : lines) {
            if (!lineMatchesSkuOrMaterial(line, sku)) {
                continue;
            }
            if (!lineMatchesQuantity(line, qty)) {
                continue;
            }
            if (!lineMatchesPeriod(line, today)) {
                continue;
            }
            if (best == null || comparePriceLineCandidates(line, best) < 0) {
                best = line;
            }
        }
        return best;
    }

    /** P2-CK-sal-008：候选行确定性比较（返回负值表示 a 优于 b）。 */
    protected int comparePriceLineCandidates(ErpSalPriceListLine a, ErpSalPriceListLine b) {
        // skuId 专属命中优先于 materialId 通用命中
        boolean aSku = a.getSkuId() != null;
        boolean bSku = b.getSkuId() != null;
        if (aSku != bSku) {
            return aSku ? -1 : 1;
        }
        // 更窄数量阶梯（minQuantity 更大）优先
        BigDecimal aMin = a.getMinQuantity() == null ? BigDecimal.ZERO : a.getMinQuantity();
        BigDecimal bMin = b.getMinQuantity() == null ? BigDecimal.ZERO : b.getMinQuantity();
        int byMin = bMin.compareTo(aMin);
        if (byMin != 0) {
            return byMin;
        }
        // validFrom 更晚优先（更近生效的定价）
        if (a.getValidFrom() != null && b.getValidFrom() != null) {
            int byValidFrom = b.getValidFrom().compareTo(a.getValidFrom());
            if (byValidFrom != 0) {
                return byValidFrom;
            }
        }
        // id 兜底（实体无 lineNo 列；id 字符串序保证稳定确定性）
        String aId = a.getId() == null ? "" : a.getId();
        String bId = b.getId() == null ? "" : b.getId();
        return aId.compareTo(bId);
    }

    protected boolean lineMatchesSkuOrMaterial(ErpSalPriceListLine line, ErpMdMaterialSku sku) {
        if (line.getSkuId() != null) {
            return Objects.equals(line.getSkuId(), sku.getId());
        }
        return line.getMaterialId() != null && Objects.equals(line.getMaterialId(), sku.getMaterialId());
    }

    protected boolean lineMatchesQuantity(ErpSalPriceListLine line, BigDecimal qty) {
        BigDecimal min = line.getMinQuantity();
        BigDecimal max = line.getMaxQuantity();
        if (min != null && qty.compareTo(min) < 0) {
            return false;
        }
        return max == null || qty.compareTo(max) <= 0;
    }

    protected boolean lineMatchesPeriod(ErpSalPriceListLine line, LocalDate today) {
        LocalDate from = line.getValidFrom() == null ? null : line.getValidFrom();
        LocalDate to = line.getValidTo() == null ? null : line.getValidTo();
        if (from != null && today.isBefore(from)) {
            return false;
        }
        return to == null || !today.isAfter(to);
    }

    protected boolean matchesPeriod(ErpSalPriceList pl, LocalDate today) {
        LocalDate from = pl.getValidFrom();
        LocalDate to = pl.getValidTo();
        if (from != null && today.isBefore(from)) {
            return false;
        }
        return to == null || !today.isAfter(to);
    }
}
