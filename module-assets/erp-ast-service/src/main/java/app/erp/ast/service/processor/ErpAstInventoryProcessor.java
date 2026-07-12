package app.erp.ast.service.processor;

import app.erp.ast.biz.IErpAstAssetCapitalizationBiz;
import app.erp.ast.biz.IErpAstDisposalBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.dao.entity.ErpAstInventoryLine;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.AssetInventoryPostingDispatcher;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 资产盘点编排 Processor（Facade + Processor 两层结构，protected step 方法供下游覆盖）。
 *
 * <p>状态机：DRAFT→COUNTING→RECONCILING→POSTED（+ CANCELLED）。差异处置在 RECONCILING 态进行：
 * 盘盈建新卡（直接建卡避免与 CAPITALIZATION 凭证双重过账）、盘亏置资产 SCRAPPED（避免与 DISPOSAL 凭证双重过账）。
 * 详见 owner doc {@code docs/design/assets/inventory.md}。
 */
public class ErpAstInventoryProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    AssetInventoryPostingDispatcher postingDispatcher;

    /**
     * 资本化链复用入口（owner doc inventory.md §四）。默认实现走直接建卡避免双重过账；
     * 下游覆盖 {@link #handleSurplusCreateCard} 时可调用此入口生成独立资本化单。
     */
    @Inject
    IErpAstAssetCapitalizationBiz capitalizationBiz;

    /**
     * 处置链复用入口（owner doc inventory.md §四）。默认实现走直接置 SCRAPPED 避免双重过账；
     * 下游覆盖 {@link #handleShortageTriggerDisposal} 时可调用此入口生成独立处置单。
     */
    @Inject
    IErpAstDisposalBiz disposalBiz;

    // ---------- public actions ----------

    public ErpAstInventory createInventory(Long id, IServiceContext context) {
        ErpAstInventory inv = requireInventory(id, context);
        validateTransition(inv, ErpAstConstants.INVENTORY_STATUS_DRAFT, "createInventory");
        expandAssetsToLines(inv, context);
        inv.setStatus(ErpAstConstants.INVENTORY_STATUS_DRAFT);
        inventoryDao().updateEntity(inv);
        return inv;
    }

    public ErpAstInventory submitForCount(Long id, IServiceContext context) {
        ErpAstInventory inv = requireInventory(id, context);
        validateTransition(inv, ErpAstConstants.INVENTORY_STATUS_DRAFT, "submitForCount");
        if (findLines(inv.getId()).isEmpty()) {
            throw new NopException(ErpAstErrors.ERR_AST_INVENTORY_RANGE_EMPTY)
                    .param(ErpAstErrors.ARG_INVENTORY_CODE, inv.getCode());
        }
        inv.setStatus(ErpAstConstants.INVENTORY_STATUS_COUNTING);
        inventoryDao().updateEntity(inv);
        return inv;
    }

    public ErpAstInventory reconcile(Long id, IServiceContext context) {
        ErpAstInventory inv = requireInventory(id, context);
        validateTransition(inv, ErpAstConstants.INVENTORY_STATUS_COUNTING, "reconcile");
        calculateVariance(inv, context);
        inv.setStatus(ErpAstConstants.INVENTORY_STATUS_RECONCILING);
        inventoryDao().updateEntity(inv);
        return inv;
    }

    public ErpAstInventory processVariance(Long id, IServiceContext context) {
        ErpAstInventory inv = requireInventory(id, context);
        validateReconciling(inv);
        List<ErpAstInventoryLine> lines = findLines(inv.getId());
        for (ErpAstInventoryLine line : lines) {
            handleLineVariance(inv, line, context);
            lineDao().saveOrUpdateEntity(line);
        }
        return inv;
    }

    public ErpAstInventory approve(Long id, IServiceContext context) {
        ErpAstInventory inv = requireInventory(id, context);
        validateReconciling(inv);
        inv.setApprovedBy(currentUserId());
        inv.setApprovedAt(CoreMetrics.currentDateTime());
        inventoryDao().updateEntity(inv);
        return inv;
    }

    public ErpAstInventory post(Long id, IServiceContext context) {
        ErpAstInventory inv = requireInventory(id, context);
        validateReconciling(inv);
        if (isApprovalRequired() && inv.getApprovedAt() == null) {
            throw new NopException(ErpAstErrors.ERR_AST_INVENTORY_NOT_RECONCILED)
                    .param(ErpAstErrors.ARG_INVENTORY_CODE, inv.getCode())
                    .param(ErpAstErrors.ARG_CURRENT_STATUS, inv.getStatus());
        }
        validateAllVarianceProcessed(inv);
        validateShortageBlocks(inv);

        Long voucherId = postingDispatcher.tryPost(inv);
        inv = reload(id);
        if (voucherId != null) {
            LocalDateTime now = CoreMetrics.currentDateTime();
            inv.setPosted(true);
            inv.setPostedAt(now);
            inv.setPostedBy(currentUserId());
            inv.setStatus(ErpAstConstants.INVENTORY_STATUS_POSTED);
        }
        inventoryDao().updateEntity(inv);
        return inv;
    }

    public ErpAstInventory cancel(Long id, IServiceContext context) {
        ErpAstInventory inv = requireInventory(id, context);
        String status = inv.getStatus();
        if (Objects.equals(status, ErpAstConstants.INVENTORY_STATUS_POSTED)
                || Objects.equals(status, ErpAstConstants.INVENTORY_STATUS_CANCELLED)) {
            throw illegalTransition(inv, status, "非 DRAFT/COUNTING");
        }
        inv.setStatus(ErpAstConstants.INVENTORY_STATUS_CANCELLED);
        inventoryDao().updateEntity(inv);
        return inv;
    }

    public ErpAstInventory reverse(Long id, IServiceContext context) {
        ErpAstInventory inv = requireInventory(id, context);
        if (!Objects.equals(inv.getStatus(), ErpAstConstants.INVENTORY_STATUS_POSTED)
                || !Boolean.TRUE.equals(inv.getPosted())) {
            throw illegalTransition(inv, inv.getStatus(), "POSTED + posted=true");
        }
        postingDispatcher.reverse(inv);
        inv = reload(id);
        inv.setPosted(false);
        inv.setPostedAt(null);
        inv.setPostedBy(null);
        inv.setStatus(ErpAstConstants.INVENTORY_STATUS_RECONCILING);
        inventoryDao().updateEntity(inv);
        return inv;
    }

    // ---------- step：范围展开（protected，下游可覆盖） ----------

    protected void expandAssetsToLines(ErpAstInventory inv, IServiceContext context) {
        List<ErpAstInventoryLine> existing = findLines(inv.getId());
        Set<Long> existingAssetIds = new HashSet<>();
        for (ErpAstInventoryLine line : existing) {
            if (line.getAssetId() != null) {
                existingAssetIds.add(line.getAssetId());
            }
        }
        QueryBean q = new QueryBean();
        if (inv.getOrgId() != null) {
            q.addFilter(eq("orgId", inv.getOrgId()));
        }
        if (inv.getRangeDepartmentId() != null) {
            q.addFilter(eq("departmentId", inv.getRangeDepartmentId()));
        }
        if (inv.getRangeCategoryId() != null) {
            q.addFilter(eq("categoryId", inv.getRangeCategoryId()));
        }
        if (inv.getRangeLocationId() != null) {
            q.addFilter(eq("locationId", inv.getRangeLocationId()));
        }
        List<String> liveStatuses = List.of(
                ErpAstConstants.ASSET_STATUS_IN_SERVICE,
                ErpAstConstants.ASSET_STATUS_IDLE);
        q.addFilter(in("status", liveStatuses));
        List<ErpAstAsset> assets = daoProvider.daoFor(ErpAstAsset.class).findAllByQuery(q);
        if (assets.isEmpty()) {
            throw new NopException(ErpAstErrors.ERR_AST_INVENTORY_RANGE_EMPTY)
                    .param(ErpAstErrors.ARG_INVENTORY_CODE, inv.getCode());
        }
        int lineNo = existing.size();
        for (ErpAstAsset asset : assets) {
            if (existingAssetIds.contains(asset.getId())) {
                throw new NopException(ErpAstErrors.ERR_AST_INVENTORY_LINE_ASSET_DUPLICATE)
                        .param(ErpAstErrors.ARG_INVENTORY_CODE, inv.getCode())
                        .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
            }
            ErpAstInventoryLine line = lineDao().newEntity();
            line.setInventoryId(inv.getId());
            line.setOrgId(inv.getOrgId());
            line.setLineNo(++lineNo);
            line.setAssetId(asset.getId());
            line.setAssetCodeSnapshot(asset.getCode());
            line.setAssetNameSnapshot(asset.getName());
            line.setCategoryId(asset.getCategoryId());
            line.setBookQuantity(1);
            line.setActualQuantity(null);
            line.setVarianceQuantity(0);
            line.setVarianceType(null);
            line.setBookValue(nz(asset.getNetBookValue()));
            line.setAssessedValue(BigDecimal.ZERO);
            line.setVarianceAmount(BigDecimal.ZERO);
            line.setDisposition(ErpAstConstants.INVENTORY_DISPOSITION_NONE);
            lineDao().saveEntity(line);
        }
    }

    // ---------- step：差异计算（protected，下游可覆盖） ----------

    protected void calculateVariance(ErpAstInventory inv, IServiceContext context) {
        List<ErpAstInventoryLine> lines = findLines(inv.getId());
        int surplusCount = 0, shortageCount = 0, matchedCount = 0;
        BigDecimal surplusAmount = BigDecimal.ZERO, shortageAmount = BigDecimal.ZERO;
        for (ErpAstInventoryLine line : lines) {
            int book = nzInt(line.getBookQuantity());
            int actual = nzInt(line.getActualQuantity());
            int variance = actual - book;
            line.setVarianceQuantity(variance);
            String type;
            if (variance > 0) {
                type = ErpAstConstants.VARIANCE_TYPE_SURPLUS;
                line.setVarianceAmount(line.getAssessedValue() != null ? line.getAssessedValue() : BigDecimal.ZERO);
                surplusAmount = surplusAmount.add(nz(line.getVarianceAmount()));
                surplusCount++;
            } else if (variance < 0) {
                type = ErpAstConstants.VARIANCE_TYPE_SHORTAGE;
                line.setVarianceAmount(nz(line.getBookValue()));
                shortageAmount = shortageAmount.add(nz(line.getVarianceAmount()));
                shortageCount++;
            } else {
                type = ErpAstConstants.VARIANCE_TYPE_MATCHED;
                line.setVarianceAmount(BigDecimal.ZERO);
                matchedCount++;
            }
            line.setVarianceType(type);
            lineDao().saveOrUpdateEntity(line);
        }
        inv.setSurplusCount(surplusCount);
        inv.setShortageCount(shortageCount);
        inv.setMatchedCount(matchedCount);
        inv.setSurplusAmount(surplusAmount);
        inv.setShortageAmount(shortageAmount);
    }

    // ---------- step：差异处置（protected，下游可覆盖） ----------

    protected void handleLineVariance(ErpAstInventory inv, ErpAstInventoryLine line, IServiceContext context) {
        String type = line.getVarianceType();
        if (Objects.equals(type, ErpAstConstants.VARIANCE_TYPE_SURPLUS)) {
            handleSurplusCreateCard(inv, line, context);
        } else if (Objects.equals(type, ErpAstConstants.VARIANCE_TYPE_SHORTAGE)) {
            handleShortageTriggerDisposal(inv, line, context);
        } else {
            line.setDisposition(ErpAstConstants.INVENTORY_DISPOSITION_NONE);
        }
    }

    protected void handleSurplusCreateCard(ErpAstInventory inv, ErpAstInventoryLine line, IServiceContext context) {
        String disposition = line.getDisposition();
        if (disposition == null || Objects.equals(disposition, ErpAstConstants.INVENTORY_DISPOSITION_NONE)) {
            disposition = ErpAstConstants.INVENTORY_DISPOSITION_NEW_CARD;
            line.setDisposition(disposition);
        }
        if (Objects.equals(disposition, ErpAstConstants.INVENTORY_DISPOSITION_INVESTIGATE)) {
            return;
        }
        if (!Objects.equals(disposition, ErpAstConstants.INVENTORY_DISPOSITION_NEW_CARD)) {
            return;
        }
        if (line.getNewAssetId() != null) {
            return;
        }
        Long categoryId = line.getCategoryId() != null ? line.getCategoryId() : inv.getRangeCategoryId();
        BigDecimal assessed = nz(line.getAssessedValue());
        if (assessed.signum() <= 0) {
            assessed = nz(line.getBookValue());
        }
        ErpAstAsset asset = assetDao().newEntity();
        asset.setCode(buildSurplusAssetCode(inv, line));
        asset.setName(resolveSurplusAssetName(line));
        asset.setOrgId(inv.getOrgId());
        asset.setCategoryId(categoryId);
        asset.setAcquisitionDate(inv.getBusinessDate() != null ? inv.getBusinessDate()
                : CoreMetrics.today());
        asset.setCurrencyId(inv.getCurrencyId());
        asset.setOriginalValue(assessed);
        asset.setCurrentValue(assessed);
        asset.setResidualValue(BigDecimal.ZERO);
        if (categoryId != null) {
            ErpAstAssetCategory category = daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(categoryId);
            if (category != null) {
                asset.setDepreciationMethod(category.getDepreciationMethod());
                asset.setUsefulLifeMonths(category.getUsefulLifeMonths());
            }
        }
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setNetBookValue(assessed);
        asset.setStatus(ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        assetDao().saveEntity(asset);
        line.setNewAssetId(asset.getId());
    }

    protected void handleShortageTriggerDisposal(ErpAstInventory inv, ErpAstInventoryLine line, IServiceContext context) {
        String disposition = line.getDisposition();
        if (disposition == null || Objects.equals(disposition, ErpAstConstants.INVENTORY_DISPOSITION_NONE)) {
            disposition = ErpAstConstants.INVENTORY_DISPOSITION_DISPOSAL;
            line.setDisposition(disposition);
        }
        if (Objects.equals(disposition, ErpAstConstants.INVENTORY_DISPOSITION_INVESTIGATE)) {
            return;
        }
        if (!Objects.equals(disposition, ErpAstConstants.INVENTORY_DISPOSITION_DISPOSAL)) {
            return;
        }
        if (line.getAssetId() == null) {
            return;
        }
        if (line.getDisposalId() != null) {
            return;
        }
        ErpAstAsset asset = assetDao().getEntityById(line.getAssetId());
        if (asset == null) {
            return;
        }
        String assetStatus = asset.getStatus();
        if (Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_SCRAPPED)
                || Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_SOLD)
                || Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_DISPOSED)) {
            return;
        }
        asset.setStatus(ErpAstConstants.ASSET_STATUS_SCRAPPED);
        assetDao().saveOrUpdateEntity(asset);
    }

    // ---------- step：迁移校验（protected，下游可覆盖） ----------

    protected void validateTransition(ErpAstInventory inv, String expected, String action) {
        String status = inv.getStatus();
        if (status != null && !Objects.equals(status, expected)) {
            throw illegalTransition(inv, status, expected + "（操作 " + action + "）");
        }
    }

    protected void validateReconciling(ErpAstInventory inv) {
        String status = inv.getStatus();
        if (status == null || !Objects.equals(status, ErpAstConstants.INVENTORY_STATUS_RECONCILING)) {
            throw new NopException(ErpAstErrors.ERR_AST_INVENTORY_NOT_RECONCILED)
                    .param(ErpAstErrors.ARG_INVENTORY_CODE, inv.getCode())
                    .param(ErpAstErrors.ARG_CURRENT_STATUS, status);
        }
    }

    protected void validateAllVarianceProcessed(ErpAstInventory inv) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("inventoryId", inv.getId()));
        q.addFilter(in("varianceType", List.of(
                ErpAstConstants.VARIANCE_TYPE_SURPLUS,
                ErpAstConstants.VARIANCE_TYPE_SHORTAGE)));
        q.addFilter(eq("disposition", ErpAstConstants.INVENTORY_DISPOSITION_NONE));
        List<ErpAstInventoryLine> unprocessed = lineDao().findAllByQuery(q);
        if (!unprocessed.isEmpty()) {
            throw new NopException(ErpAstErrors.ERR_AST_INVENTORY_VARIANCE_NOT_PROCESSED)
                    .param(ErpAstErrors.ARG_INVENTORY_CODE, inv.getCode());
        }
    }

    protected void validateShortageBlocks(ErpAstInventory inv) {
        if (!AppConfig.var(ErpAstConstants.CONFIG_INVENTORY_NEGATIVE_SHORTAGE_BLOCKS, false)) {
            return;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("inventoryId", inv.getId()));
        q.addFilter(eq("varianceType", ErpAstConstants.VARIANCE_TYPE_SHORTAGE));
        q.addFilter(ne("disposition", ErpAstConstants.INVENTORY_DISPOSITION_INVESTIGATE));
        List<ErpAstInventoryLine> pendingShortages = lineDao().findAllByQuery(q);
        if (!pendingShortages.isEmpty()) {
            throw new NopException(ErpAstErrors.ERR_AST_INVENTORY_SHORTAGE_BLOCKS)
                    .param(ErpAstErrors.ARG_INVENTORY_CODE, inv.getCode());
        }
    }

    // ---------- 配置 ----------

    public boolean isApprovalRequired() {
        return AppConfig.var(ErpAstConstants.CONFIG_INVENTORY_REQUIRE_APPROVAL, true);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpAstInventory requireInventory(Long id, IServiceContext context) {
        ErpAstInventory inv = inventoryDao().getEntityById(id);
        if (inv == null) {
            throw new NopException(ErpAstErrors.ERR_AST_INVENTORY_NOT_FOUND)
                    .param(ErpAstErrors.ARG_INVENTORY_ID, id);
        }
        return inv;
    }

    protected ErpAstInventory reload(Long id) {
        return inventoryDao().getEntityById(id);
    }

    protected List<ErpAstInventoryLine> findLines(Long inventoryId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("inventoryId", inventoryId));
        q.addOrderField("lineNo", false);
        return lineDao().findAllByQuery(q);
    }

    protected String buildSurplusAssetCode(ErpAstInventory inv, ErpAstInventoryLine line) {
        return "AST-INV-" + inv.getCode() + "-" + line.getLineNo();
    }

    protected String resolveSurplusAssetName(ErpAstInventoryLine line) {
        if (line.getAssetNameSnapshot() != null && !line.getAssetNameSnapshot().trim().isEmpty()) {
            return line.getAssetNameSnapshot();
        }
        return "盘盈资产-" + line.getLineNo();
    }

    protected IEntityDao<ErpAstInventory> inventoryDao() {
        return daoProvider.daoFor(ErpAstInventory.class);
    }

    protected IEntityDao<ErpAstInventoryLine> lineDao() {
        return daoProvider.daoFor(ErpAstInventoryLine.class);
    }

    protected IEntityDao<ErpAstAsset> assetDao() {
        return daoProvider.daoFor(ErpAstAsset.class);
    }

    /** 资本化链复用入口（供下游覆盖 handleSurplusCreateCard 时调用）。 */
    protected IErpAstAssetCapitalizationBiz capitalizationBiz() {
        return capitalizationBiz;
    }

    /** 处置链复用入口（供下游覆盖 handleShortageTriggerDisposal 时调用）。 */
    protected IErpAstDisposalBiz disposalBiz() {
        return disposalBiz;
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) inventoryDao()).getOrmTemplate();
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected NopException illegalTransition(ErpAstInventory inv, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_AST_INVENTORY_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_INVENTORY_CODE, inv.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    protected static int nzInt(Integer v) {
        return v != null ? v : 0;
    }
}
