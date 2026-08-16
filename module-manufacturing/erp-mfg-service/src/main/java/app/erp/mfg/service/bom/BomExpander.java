package app.erp.mfg.service.bom;

import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderBomLineSnapshot;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderBomSnapshot;
import app.erp.mfg.biz.BomExplosionNode;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * BOM 展开（单级 / 多级 / phantom / 环检测 / 深度上限）。服务于 {@code IErpMfgBomBiz.explode}。
 *
 * <p>展开规则（{@code bom-and-routing.md §多级 BOM 展开}）：
 * <ul>
 *   <li>有效用量 = {@code line.quantity × requestedQty / BOM.qty}，逐层乘积。</li>
 *   <li>phantom（{@code bomType=20}）：展开其子件并入父级层级，不产生独立节点。</li>
 *   <li>多级：制造子件（{@code bomType=10}，有默认 BOM）递归展开其子件 BOM。</li>
 *   <li>环检测：DFS 访问集合（路径回溯），成环抛 {@code ERR_BOM_CYCLE}；深度超 {@code erp-mfg.bom-max-depth}
 *       抛 {@code ERR_BOM_MAX_DEPTH_EXCEEDED}（环兜底）。</li>
 * </ul>
 *
 * <p>快照展开（plan 2026-08-16-0904-1 RC-R1.49，UC-MFG-10 断言④⑤⑥）：{@link #explodeFromSnapshot}
 * 以工单 BOM 快照头（{@link ErpMfgWorkOrderBomSnapshot}）为数据源展开首级子件，复用本类 DFS/phantom/环检测
 * 算法；子级制造件仍按实时默认 BOM 递归（快照仅锁定工单自身 BOM 内容，非全局 BOM 版本管理）。
 *
 * <p>本类为非 BizModel 服务助手（对齐 inventory {@code TraceChainQuery} / costing 策略范式），直接用
 * {@link IDaoProvider} 查询 BOM/行（只读展开，无权限管道语义）。
 */
public class BomExpander {

    static final BigDecimal SIXTY = new BigDecimal("60");
    static final int SCALE = 12;

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 取物料的默认且有效的 BOM（{@code isDefault=true && isActive=true}）。无则返回 null。
     */
    public ErpMfgBom findDefaultBomOrNull(Long productId) {
        if (productId == null) {
            return null;
        }
        IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("productId", productId));
        q.addFilter(eq("isDefault", Boolean.TRUE));
        q.addFilter(eq("isActive", Boolean.TRUE));
        q.addOrderField("id", false);
        q.setLimit(1);
        List<ErpMfgBom> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 展开指定 BOM。
     *
     * @param bomId         被展开的 BOM（不必为默认 BOM）
     * @param requestedQty  期望产出量（null → {@code BOM.qty}，即一个标准批量）
     * @param useMultiLevel true=制造子件递归；false=仅直接子件
     */
    public List<BomExplosionNode> explode(Long bomId, BigDecimal requestedQty, boolean useMultiLevel) {
        ErpMfgBom bom = requireBom(bomId);
        BigDecimal qty = requestedQty != null ? requestedQty : nz(bom.getQty());
        List<BomExplosionNode> nodes = new ArrayList<>();
        expandLines(bom, qty, 1, new LinkedHashSet<>(), useMultiLevel, nodes);
        return nodes;
    }

    /**
     * 按工单 BOM 快照展开（plan 2026-08-16-0904-1 RC-R1.49，UC-MFG-10 断言④⑤）。
     * 首级子件来自快照行（提交时点锁定内容）；子级制造件经 {@link #findDefaultBomOrNull} 实时递归
     * （快照仅锁定工单自身 BOM，Non-Goal「BOM 自身版本历史管理」）。
     */
    public List<BomExplosionNode> explodeFromSnapshot(ErpMfgWorkOrderBomSnapshot snapshot,
                                                      BigDecimal requestedQty, boolean useMultiLevel) {
        if (snapshot == null) {
            return new ArrayList<>();
        }
        BigDecimal qty = requestedQty != null ? requestedQty : nz(snapshot.getQty());
        List<BomExplosionNode> nodes = new ArrayList<>();
        expandSnapshotLines(snapshot, qty, 1, new LinkedHashSet<>(), useMultiLevel, nodes);
        return nodes;
    }

    private void expandLines(ErpMfgBom bom, BigDecimal requestedQty, int level,
                             Set<Long> path, boolean recurseMfg, List<BomExplosionNode> nodes) {
        Long product = bom.getProductId();
        if (path.contains(product)) {
            throw new NopException(ErpMfgErrors.ERR_BOM_CYCLE)
                    .param(ErpMfgErrors.ARG_MATERIAL_ID, product)
                    .param(ErpMfgErrors.ARG_PATH, path.toString());
        }
        int maxDepth = AppConfig.var(ErpMfgConstants.CONFIG_BOM_MAX_DEPTH, ErpMfgConstants.DEFAULT_BOM_MAX_DEPTH);
        if (level > maxDepth) {
            throw new NopException(ErpMfgErrors.ERR_BOM_MAX_DEPTH_EXCEEDED)
                    .param(ErpMfgErrors.ARG_DEPTH, maxDepth);
        }

        path.add(product);
        try {
            BigDecimal scale = divide(qty(requestedQty), nz(bom.getQty()));
            for (ErpMfgBomLine line : loadLines(bom.getId())) {
                BigDecimal effQty = nz(line.getQuantity()).multiply(scale);
                ErpMfgBom childBom = findDefaultBomOrNull(line.getMaterialId());
                if (childBom != null && childBom.getBomType() != null
                        && Objects.equals(childBom.getBomType(), ErpMfgConstants.BOM_TYPE_PHANTOM)) {
                    // phantom：展开其子件并入当前层级，不产生独立节点
                    expandLines(childBom, effQty, level, path, recurseMfg, nodes);
                } else if (childBom != null && recurseMfg) {
                    nodes.add(node(line.getMaterialId(), line.getOperationId(), effQty, bom.getId(), level, true));
                    expandLines(childBom, effQty, level + 1, path, recurseMfg, nodes);
                } else {
                    nodes.add(node(line.getMaterialId(), line.getOperationId(), effQty, bom.getId(), level, childBom != null));
                }
            }
        } finally {
            path.remove(product);
        }
    }

    private void expandSnapshotLines(ErpMfgWorkOrderBomSnapshot snapshot, BigDecimal requestedQty, int level,
                                     Set<Long> path, boolean recurseMfg, List<BomExplosionNode> nodes) {
        Long product = snapshot.getProductId();
        if (product != null && path.contains(product)) {
            throw new NopException(ErpMfgErrors.ERR_BOM_CYCLE)
                    .param(ErpMfgErrors.ARG_MATERIAL_ID, product)
                    .param(ErpMfgErrors.ARG_PATH, path.toString());
        }
        int maxDepth = AppConfig.var(ErpMfgConstants.CONFIG_BOM_MAX_DEPTH, ErpMfgConstants.DEFAULT_BOM_MAX_DEPTH);
        if (level > maxDepth) {
            throw new NopException(ErpMfgErrors.ERR_BOM_MAX_DEPTH_EXCEEDED)
                    .param(ErpMfgErrors.ARG_DEPTH, maxDepth);
        }

        if (product != null) {
            path.add(product);
        }
        try {
            BigDecimal scale = divide(qty(requestedQty), nz(snapshot.getQty()));
            for (ErpMfgWorkOrderBomLineSnapshot line : snapshot.getLines()) {
                BigDecimal effQty = nz(line.getQuantity()).multiply(scale);
                ErpMfgBom childBom = findDefaultBomOrNull(line.getMaterialId());
                if (childBom != null && childBom.getBomType() != null
                        && Objects.equals(childBom.getBomType(), ErpMfgConstants.BOM_TYPE_PHANTOM)) {
                    // phantom：展开其子件并入当前层级，不产生独立节点（子件 BOM 实时——快照仅锁定工单自身 BOM）
                    expandLines(childBom, effQty, level, path, recurseMfg, nodes);
                } else if (childBom != null && recurseMfg) {
                    nodes.add(node(line.getMaterialId(), line.getOperationId(), effQty, snapshot.getBomId(), level, true));
                    expandLines(childBom, effQty, level + 1, path, recurseMfg, nodes);
                } else {
                    nodes.add(node(line.getMaterialId(), line.getOperationId(), effQty, snapshot.getBomId(), level, childBom != null));
                }
            }
        } finally {
            if (product != null) {
                path.remove(product);
            }
        }
    }

    private BomExplosionNode node(Long materialId, Long operationId, BigDecimal effQty, Long sourceBomId, int level,
                                  boolean manufactured) {
        BomExplosionNode n = new BomExplosionNode();
        n.setMaterialId(materialId);
        n.setQuantity(effQty);
        n.setOperationId(operationId);
        n.setSourceBomId(sourceBomId);
        n.setLevel(level);
        n.setManufactured(manufactured);
        return n;
    }

    private ErpMfgBom requireBom(Long bomId) {
        if (bomId == null) {
            throw new NopException(ErpMfgErrors.ERR_BOM_NOT_FOUND).param(ErpMfgErrors.ARG_BOM_ID, bomId);
        }
        ErpMfgBom bom = daoProvider.daoFor(ErpMfgBom.class).getEntityById(bomId);
        if (bom == null) {
            throw new NopException(ErpMfgErrors.ERR_BOM_NOT_FOUND).param(ErpMfgErrors.ARG_BOM_ID, bomId);
        }
        return bom;
    }

    /** 查询 BOM 子件行（按行号升序）。public 供快照复制（submit）复用。 */
    public List<ErpMfgBomLine> loadLines(Long bomId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("bomId", bomId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpMfgBomLine.class).findAllByQuery(q);
    }

    /** 查询 BOM 工艺行（按行号升序）。public 供快照复制（submit）复用。 */
    public List<ErpMfgBomOperation> loadOperations(Long bomId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("bomId", bomId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpMfgBomOperation.class).findAllByQuery(q);
    }

    private static BigDecimal qty(BigDecimal v) {
        return nz(v);
    }

    static BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (b.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return a.divide(b, SCALE, RoundingMode.HALF_UP);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
