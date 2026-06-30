package io.nop.app.all.auth;

import io.nop.core.initialize.ICoreInitializer;
import io.nop.core.initialize.impl.ReflectionHelperMethodInitializer;
import io.nop.core.initialize.impl.VirtualFileSystemInitializer;
import io.nop.core.lang.xml.XNode;
import io.nop.core.resource.IResource;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.xlang.initialize.RegisterModelCoreInitializer;
import io.nop.xlang.initialize.XLangCoreInitializer;
import io.nop.xlang.xdsl.DslNodeLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 app-erp-all 聚合的 app.action-auth.xml 在合并全部 18 业务域 + 系统模块后,
 * 加载无 resource id 冲突,且 18 域 TOPM 菜单全部出现在 site=main。
 *
 * 使用 DslNodeLoader 执行 x:extends 节点级合并(平台运行时合并菜单的同一机制),
 * 手动初始化核心链(Reflection → XLang → VFS → RegisterModel),跳过 IocCoreInitializer
 * 以避免聚合 18 域实体驱动的 ORM 会话工厂初始化(ErpProProject 类不在本测试范围)。
 */
public class TestAppActionAuthMerge {

    private static final String ACTION_AUTH_PATH = "/nop/main/auth/app.action-auth.xml";

    private static final String[] DOMAIN_TOPM_IDS = {
            "erp-md", "erp-pur", "erp-sal", "erp-inv", "erp-fin", "erp-ast",
            "erp-prj", "erp-mfg", "erp-qa", "erp-mnt",
            "erp-crm", "erp-cs", "erp-hr", "erp-aps", "erp-ct", "erp-drp",
            "erp-log", "erp-b2b"
    };

    private static final List<ICoreInitializer> INITIALIZERS = new ArrayList<>();

    @BeforeAll
    static void initCore() {
        INITIALIZERS.add(new ReflectionHelperMethodInitializer());
        INITIALIZERS.add(new XLangCoreInitializer());
        INITIALIZERS.add(new VirtualFileSystemInitializer());
        INITIALIZERS.add(new RegisterModelCoreInitializer());
        INITIALIZERS.forEach(ICoreInitializer::initialize);
    }

    @AfterAll
    static void destroyCore() {
        for (int i = INITIALIZERS.size() - 1; i >= 0; i--) {
            INITIALIZERS.get(i).destroy();
        }
        INITIALIZERS.clear();
    }

    @Test
    public void testMergedActionAuthLoadsAndContainsAllDomains() {
        IResource resource = VirtualFileSystem.instance().getResource(ACTION_AUTH_PATH);
        assertTrue(resource.exists(), "app.action-auth.xml not found in virtual filesystem: " + ACTION_AUTH_PATH);

        XNode mergedNode = DslNodeLoader.INSTANCE.loadFromResource(resource).getNode();
        assertNotNull(mergedNode, "merged action-auth node is null");

        XNode mainSite = mergedNode.childByTag("site");
        assertNotNull(mainSite, "<site> not found in merged action-auth");
        assertEquals("main", mainSite.attrText("id"), "site id should be 'main'");

        Set<String> allIds = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        collectResourceIds(mainSite, allIds, duplicates);

        assertTrue(duplicates.isEmpty(),
                "duplicate resource ids in merged action-auth (site=main): " + duplicates);

        for (String topmId : DOMAIN_TOPM_IDS) {
            assertTrue(allIds.contains(topmId),
                    "domain TOPM id missing in merged action-auth: " + topmId
                            + ". present ids: " + allIds);
        }

        assertEquals(18, DOMAIN_TOPM_IDS.length, "expected 18 domain TOPM ids");
        assertFalse(allIds.isEmpty(), "main site has no resources after merge");
    }

    private void collectResourceIds(XNode node, Set<String> ids, List<String> duplicates) {
        if (node == null) return;
        if ("resource".equals(node.getTagName())) {
            String id = node.attrText("id");
            if (id != null && !id.isEmpty()) {
                if (!ids.add(id)) {
                    duplicates.add(id);
                }
            }
        }
        if (node.hasChild()) {
            for (XNode child : node.getChildren()) {
                collectResourceIds(child, ids, duplicates);
            }
        }
    }
}
