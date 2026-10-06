package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.dao.entity.ErpAstMergeLine;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * P2-CK-ast-013 / P2-CK-ast-014 白盒单元载体：{@link ErpAstMergeProcessor#validateSources} 的
 * 悬空源（src==null）与重复源精确面。
 *
 * <p>集成路径（{@code TestErpAstSplitMerge}）上悬空引用经 ORM to-one 关系解析为**未加载桩实体**
 * （status=null）→ 被既有 status 守卫（ERR_AST_MERGE_SOURCE_NOT_IN_SERVICE）先行拦截，字面 null 面
 * 不可达；本载体在 processor 同包直调 protected validateSources，锁定修复新增的 null 分支与去重分支
 * 本身（IoC 无需：两分支不触 dao）。
 */
public class TestErpAstMergeSourceValidation extends BaseTestCase {

    private ErpAstMergeProcessor newProcessor() {
        return new ErpAstMergeProcessor();
    }

    private ErpAstMerge newMerge() {
        ErpAstMerge merge = new ErpAstMerge();
        merge.setCode("MRG-013-T");
        return merge;
    }

    private ErpAstAsset newInServiceAsset(String id, String code) {
        ErpAstAsset asset = new ErpAstAsset();
        asset.setId(id);
        asset.setCode(code);
        asset.setStatus(ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        asset.setOriginalValue(new BigDecimal("10000"));
        asset.setCategoryId("CAT-A");
        asset.setCurrencyId("1");
        return asset;
    }

    /** ast-013：悬空源（null）→ ERR_AST_MERGE_SOURCE_NOT_FOUND（mergeCode + lineIndex=1），非 NPE 下泄。 */
    @Test
    public void testAst013NullSourceRejectedWithMergeCodeAndLineIndex() {
        ErpAstMergeProcessor processor = newProcessor();
        ErpAstMerge merge = newMerge();
        ErpAstMergeLine line = new ErpAstMergeLine();
        line.setLineNo(1);

        List<ErpAstMergeLine> lines = Collections.singletonList(line);
        List<ErpAstAsset> sources = Arrays.asList((ErpAstAsset) null);

        NopException ex = assertThrows(NopException.class,
                () -> processor.validateSources(merge, lines, sources, null));
        assertEquals(ErpAstErrors.ERR_AST_MERGE_SOURCE_NOT_FOUND.getErrorCode(), ex.getErrorCode(),
                "悬空源（null）抛 ERR_AST_MERGE_SOURCE_NOT_FOUND");
        assertEquals("MRG-013-T", ex.getParam(ErpAstErrors.ARG_MERGE_CODE), "参数带 mergeCode");
        assertEquals(1, ((Number) ex.getParam("lineIndex")).intValue(), "参数带行索引 lineIndex=1");
    }

    /** ast-014：重复源（同资产两行）→ ERR_AST_MERGE_SOURCE_DUPLICATE（mergeCode + assetCode），价值单计由拒绝前置保证。 */
    @Test
    public void testAst014DuplicateSourceRejectedWithMergeCodeAndAssetCode() {
        ErpAstMergeProcessor processor = newProcessor();
        ErpAstMerge merge = newMerge();
        ErpAstAsset dup = newInServiceAsset("1", "AST-DUP");
        ErpAstMergeLine l1 = new ErpAstMergeLine();
        l1.setLineNo(1);
        ErpAstMergeLine l2 = new ErpAstMergeLine();
        l2.setLineNo(2);

        List<ErpAstMergeLine> lines = Arrays.asList(l1, l2);
        List<ErpAstAsset> sources = Arrays.asList(dup, dup);

        NopException ex = assertThrows(NopException.class,
                () -> processor.validateSources(merge, lines, sources, null));
        assertEquals(ErpAstErrors.ERR_AST_MERGE_SOURCE_DUPLICATE.getErrorCode(), ex.getErrorCode(),
                "重复源抛 ERR_AST_MERGE_SOURCE_DUPLICATE");
        assertEquals("MRG-013-T", ex.getParam(ErpAstErrors.ARG_MERGE_CODE), "参数带 mergeCode");
        assertEquals("AST-DUP", ex.getParam(ErpAstErrors.ARG_ASSET_CODE), "参数带 assetCode");
    }

    /** 正常双源（非空、不重复、同类别同币种）不被两守卫误伤。 */
    @Test
    public void testDistinctSourcesPassValidation() {
        ErpAstMergeProcessor processor = newProcessor();
        ErpAstMerge merge = newMerge();
        ErpAstAsset a1 = newInServiceAsset("1", "AST-A1");
        ErpAstAsset a2 = newInServiceAsset("2", "AST-A2");
        ErpAstMergeLine l1 = new ErpAstMergeLine();
        l1.setLineNo(1);
        ErpAstMergeLine l2 = new ErpAstMergeLine();
        l2.setLineNo(2);

        // 不抛异常即通过（状态/类别/币种一致性由夹具保证）
        processor.validateSources(merge, Arrays.asList(l1, l2), Arrays.asList(a1, a2), null);
    }
}
