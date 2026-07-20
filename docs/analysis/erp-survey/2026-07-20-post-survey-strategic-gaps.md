# Post-Survey Strategic Gap Analysis

> **Date**: 2026-07-20
> **Context**: Systematic comparison of nop-app-erp against OFBiz Framework, Wimoor, ERP5 (Nexedi), and NocoBase.
> **Purpose**: Identify design document gaps and roadmap supplements needed for nop-app-erp to achieve "the most advanced and powerful open-source ERP" positioning.

---

## 1. Executive Summary

nop-app-erp has achieved an exceptionally broad and deep feature set across 18+1 business domains. The 3-layer posting engine, multi-account schema native support, bidirectional red-ink reversal, bad-debt Allowance lifecycle, 300+ E2E tests across all domains, and full CRM/CS/HR/APS/Contract/DRP/Logistics/B2B state machines collectively provide a foundation that already surpasses most open-source ERP offerings in **domain breadth** and **financial depth**.

However, several categories of gaps prevent the project from credibly claiming "most advanced and powerful" status. **Note**: The Nop Platform itself provides many capabilities that the compared projects implement at the application layer — including multi-tenancy (`tenant-model.md`), runtime dictionary management, `NopSysEvent` outbox event queue (topic-based routing, partition serial, lease mechanism, delayed scheduling), `IMessageService` for event publish/consume, ORM driver layer for heterogeneous data sources, JsonOrmComponent for ext fields, XWF workflow engine, and BizModel + xbiz + Processor + I*Biz for cross-domain orchestration. After accounting for platform-provided capabilities, the remaining application-level gaps are:

1. **GL mapping rule tables** — no database-backed GL account determination rules. Each deployment currently requires Java SPI changes for account mapping setup (OFBiz multi-layer GL mapping gap).
2. **Reference external API integration pattern** — while Nop's GraphQL, xpl, and IoC provide the plumbing, there's no standardized reference pattern for third-party credential management, rate limiting, or API client factory (Wimoor ApiBuildService comparison).
3. **Domain-specific gaps** — budget depth (multi-year, carry-forward), MRP/DRP simulation engine, cross-border trade fields, multi-company consolidation operational layer, unified party identity query, date-ranged validity, cost calculation strategy pattern.

---

## 2. Current State Assessment

### Already Delivered (Verified Against Source)

| Capability | Evidence | Status |
|-----------|----------|--------|
| 18 business domains + 1 cross-domain notify subsystem | `domain-module-split-analysis.md:46-69` | ✅ Live |
| Per-domain ORM models (18 `*.orm.xml` files, ~7000 lines total) | Each `module-<domain>/model/` | ✅ Live |
| 3-layer posting engine (SYNC business+inventory → configurable voucher timing → immutable saga) | `posting.md:11-42` | ✅ Live |
| 40+ businessType enum with documented debit/credit semantics | `app-erp-finance.orm.xml:60-127` | ✅ Live |
| Pluggable IErpFinAcctDocProvider SPI (register via @Inject Map) | `posting.md:196-209` | ✅ Live |
| IErpFinFactsValidator extension point | `posting.md:222-262` | ✅ Live |
| Native multi-book accounting with acctSchemaId | `domain-design-guidelines.md:503` | ✅ Live |
| Multi-currency four-piece standard (currencyId + exchangeRate + amountSource + amountFunctional) | `domain-design-guidelines.md:488-493` | ✅ Live |
| Bidirectional red-ink reversal (VoucherReversedEvent → domain listener) | `posting.md:337-418` | ✅ Live |
| Bad-debt Allowance lifecycle (provisioning → write-off → recovery → release) | `bad-debt.md` | ✅ Live |
| 5-state period state machine (NOT_OPENED/OPEN/CLOSING/CLOSED/CLOSED_FINAL) | `period-close.md:150-158` | ✅ Live |
| FX revaluation workflow | `domain-design-guidelines.md:388-405` | ✅ Live |
| Year-end closing (P&L → retained earnings) | `period-close.md:241-271` | ✅ Live |
| AR/AP reconciliation + auto-reconciliation (3 strategies FIFO/BY_AMOUNT/BY_RATIO) | `ar-ap-reconciliation.md` | ✅ Live |
| Bank reconciliation with auto/manual matching | `bank-reconciliation.md` | ✅ Live |
| Treasury management (notes receivable/payable, credit facility, cash forecast) | `treasury.md` | ✅ Live |
| Expense claim + employee advance lifecycle | `expense-claim.md` | ✅ Live |
| 3-layer inventory model (Move → Ledger → Balance) | `inventory/README.md:26-35` | ✅ Live |
| LIFO/FIFO/Moving Average cost layers + landed cost | `costing-methods.md` | ✅ Live |
| Manufacturing: BOM, WorkOrder 10-state, JobCard, MRP engine, CRP load, subcontracting, genealogy | All manufacturing plans | ✅ Live |
| Quality: Inspection 4-state, NCR 5-state + CAPA, SPC control charts, batch recall | All quality plans | ✅ Live |
| Maintenance: Visit 5-state, Request 6-state, spare-part consumption, downtime | All maintenance plans | ✅ Live |
| Assets: Depreciation, CIP, maintenance, inventory, split/merge, value adjustment | All assets plans | ✅ Live |
| Projects: Cost collection, DAG validation, settlement, P&L closing | All projects plans | ✅ Live |
| Cross-domain notification subsystem (module-notify) | `notification-strategy.md` | ✅ Live |
| CRM: Lead scoring, sales forecasting, event timeline | All CRM plans | ✅ Live |
| Customer Service: Ticket 6-state, SLA, CSAT survey | All CS plans | ✅ Live |
| HR: Payroll engine, income tax, shift scheduling, simulation, recruitment, leave | All HR plans | ✅ Live |
| APS: Scheduling engine, forward/backward, rush order | All APS plans | ✅ Live |
| Contract: Lifecycle, version management, rebate, invoice plan trigger | All contract plans | ✅ Live |
| DRP: Net requirements, safety stock, release to TransferOrder/PurOrder | All DRP plans | ✅ Live |
| Logistics: Shipment state, carrier gateway, freight posting | All logistics plans | ✅ Live |
| B2B: EDI envelope, ASN match, MFT | All B2B plans | ✅ Live |
| Sales pricing engine (price list + rule engine) | `core-business-roadmap.md:28` | ✅ Live |
| Credit hold check (delivery + invoice gates) | `core-business-roadmap.md:29` | ✅ Live |
| 300+ E2E tests (business actions, orchestration, dashboard visual, report download, screenshot snapshot) | Full test suite | ✅ Live |
| Dashboard KPI 10 domains + 24 reports with AMIS rendering regression | `test-results/` | ✅ Live |
| Inter-company consolidation design | `intercompany-consolidation.md` | ✅ Design |
| Cost center allocation design | `cost-center.md` | ✅ Design |

---

## 3. Application-Level Design Gaps

*Note: This section identifies gaps at the nop-app-erp application level. Nop Platform capabilities (multi-tenancy, runtime dict management, IMessageService, ORM driver layer, XWF workflow, JsonOrmComponent) are acknowledged as already provided and not listed as gaps.*

### P0/P1 — Must Address for "Most Advanced" Claim

#### 3.1 GL Mapping Rule Tables

**Missing**: nop-app-erp's posting engine delegates GL account resolution to the `IErpFinAcctDocProvider.createFacts()` SPI implementation, but there is no prescribed database-backed GL mapping table structure. Current implementations likely hardcode or use minimal config. Compare: OFBiz has 5+ dedicated GL mapping entities (InvoiceItemTypeGlAccount, ProductCategoryGlAccount, FixedAssetTypeGlAccount, VarianceReasonGlAccount, GlAccountTypeDefault).

| Evidence | Source |
|----------|--------|
| "nop's current design delegates GL mapping to SPI implementation, which doesn't prescribe a mapping table structure" | `ofbiz-compare.md:437` |
| "Implement GL mapping tables similar to OFBiz's pattern as the default implementation of the Provider SPI" | `ofbiz-compare.md:437` |
| Multi-dimensional subject resolution design exists but has no table implementation | `posting.md:267-282` |

**Why P0**: Without database-backed GL mapping tables, each new customer deployment requires Java changes to set up account determination rules. Most advanced ERPs (SAP, Oracle, iDempiere) make GL mapping a runtime configuration artifact. This is the single most impactful usability improvement for the finance domain.

**Recommendation**: Design document defining:
- `ErpFinGlMappingRule` entity (businessType, dimensionKey, dimensionValue, subjectCode, priority, orgId, acctSchemaId)
- Dimension system: materialCategory, partnerGroup, warehouseType, department
- Resolution algorithm: exact → wildcard → default (specific → generic priority per `posting.md:279`)
- Migration: existing Provider implementations can remain as Java SPI fallback; new deployments use rule tables

#### 3.2 External API Integration Reference Pattern

**Context**: Nop Platform already provides the foundational capabilities for external API integration:
- **For external consumers** (serving APIs to external systems): Nop's `IGraphQLEngine` automatically exposes all `@BizQuery`/`@BizMutation` as GraphQL APIs, with REST and RPC protocol support. No additional interface definitions needed.
- **For external integration** (connecting to third-party APIs): Nop ORM's driver layer supports GraphQL driver for accessing third-party APIs as data sources; `xpl` tag libraries can encapsulate HTTP client logic; Nop IoC provides `@Inject` by bean name (no SPI needed for known implementations).
- **For credential management**: Platform security layer handles auth tokens.
- **Endpoint registry**: `ErpSysExternalSystem` or similar config storage can be defined at application level.

**Reference value from Wimoor** — while Nop Platform provides the plumbing, Wimoor's `ApiBuildService` factory pattern (`wimoor-compare.md:123-145`) serves as a concrete reference architecture for a **standardized external integration layer** that combines auth management, rate limiting, region mapping, and client lifecycle in one place. This is useful as a reference for nop-app-erp if e-commerce or logistics API integration becomes a requirement.

| Evidence | Source |
|----------|--------|
| Wimoor `ApiBuildService.java:35-507` — factory for 20+ API clients with LWA auth + Region mapping + Rate limiting | `wimoor-compare.md:123-145` |
| Nop GraphQL auto-exposure of BizModel methods for external consumers | `api-and-graphql.md` |
| Nop ORM driver layer + GraphQL driver for external data access | Platform reference |

**Why P1**: While Nop Platform provides the technical foundations, nop-app-erp lacks a **reference integration pattern document** that would guide developers in implementing standardized external API connections. This is P1 rather than P0 because real third-party integration requirements are domain-specific and each may need custom handling.

**Recommendation**: Reference design document (not standard SPI) covering:
- Standard auth patterns: OAuth2, API Key, LWA — mapped to `@Inject Map<String, IExternalApiAuthProvider>`
- Rate limiting: token bucket per-tenant/per-key, using either platform `Cache` or Redis
- Endpoint registry: `ErpSysExternalSystem` entity (systemName, baseUrl, authType, retryPolicy, rateLimit)
- Reference implementation for 1-2 common scenarios (e.g., logistics tracking, e-commerce sync)

#### 3.3 GL Mapping Rule Tables

**Missing**: nop-app-erp's posting engine delegates GL account resolution to the `IErpFinAcctDocProvider.createFacts()` SPI implementation, but there is no prescribed database-backed GL mapping table structure. Current implementations likely hardcode or use minimal config. Compare: OFBiz has 5+ dedicated GL mapping entities (InvoiceItemTypeGlAccount, ProductCategoryGlAccount, FixedAssetTypeGlAccount, VarianceReasonGlAccount, GlAccountTypeDefault).

| Evidence | Source |
|----------|--------|
| "nop's current design delegates GL mapping to SPI implementation, which doesn't prescribe a mapping table structure" | `ofbiz-compare.md:437` |
| "Implement GL mapping tables similar to OFBiz's pattern as the default implementation of the Provider SPI" | `ofbiz-compare.md:437` |
| Multi-dimensional subject resolution design exists but has no table implementation | `posting.md:267-282` |

**Why P0**: Without database-backed GL mapping tables, each new customer deployment requires Java changes to set up account determination rules. Most advanced ERPs (SAP, Oracle, iDempiere) make GL mapping a runtime configuration artifact. This is the single most impactful usability improvement for the finance domain.

**Recommendation**: Design document defining:
- `ErpFinGlMappingRule` entity (businessType, dimensionKey, dimensionValue, subjectCode, priority, orgId, acctSchemaId)
- Dimension system: materialCategory, partnerGroup, warehouseType, department
- Resolution algorithm: exact → wildcard → default (specific → generic priority per `posting.md:279`)
- Migration: existing Provider implementations can remain as Java SPI fallback; new deployments use rule tables

---

### P1 — High Strategic Value (continued)

#### 3.4 Tree Classification Reference (Informational)

**Context**: Nop Platform already provides runtime-configurable dictionary management through its built-in dict system. Dictionaries defined in ORM XML are the authoritative compile-time source, but they can be supplemented at runtime. The existing tree entity pattern (`tree-entity-patterns.md`) already supports tree-structured classification via `parentId` self-FK (used for `ErpMdMaterialCategory`, `ErpMdSubject`, `ErpHrDepartment`, `ErpCsServiceCatalogItem`).

**Reference value from ERP5**: ERP5's Category system is uniquely powerful — it uses a single classification to replace ALL foreign keys, with runtime acquisition (property inheritance along relationship graph). This degree of unification is fundamentally tied to ERP5's ZODB object model and not suitable for nop's relational model. However, it demonstrates that a **unified classification table** (beyond per-entity trees) can be useful for cross-cutting dimensions like auxiliary accounting (cost centers, profit centers).

| Evidence | Source |
|----------|--------|
| ERP5 Category: unified tree classification replacing FKs — referenced for concept only | `erp5-compare.md:100,124-130` |
| nop already has runtime dict management + tree entity pattern | Platform + `tree-entity-patterns.md` |
| Existing tree entities: MaterialCategory, Subject, Department, ServiceCatalogItem | `tree-entity-patterns.md` |

**Why P1 (informational)**: This is not a gap — nop already provides adequate dictionary and tree classification for current needs. The ERP5 Category system is architecturally interesting but inappropriate for direct adoption. Noted as "inspiration" for potential future auxiliary accounting dimension design.

#### 3.5 Unified Party Identity Query

**Missing**: nop-app-erp splits business entities into `ErpMdPartner`, `ErpMdEmployee`, `ErpMdOrganization` by role. There is no unified view or query mechanism to answer "find all transactions involving entity X" (as partner, employee, or organization).

| Evidence | Source |
|----------|--------|
| OFBiz unified `Party` + `PartyRole` pattern — "single Party table — query all transactions for party" | `ofbiz-compare.md:431,461` |
| nop's split approach "makes cross-entity queries harder — finding 'all transactions for given partner' requires multiple queries or a union" | `ofbiz-compare.md:373` |
| "Consider ErpParty view-entity or lightweight unified identity column for audit/query purposes" | `ofbiz-compare.md:431` |

**Why P1**: While the split-domain approach is correct for DDD bounded contexts, the absence of any unified identity query is a practical pain point for reporting, audit, and compliance. Most advanced ERPs provide both: domain-specific entities for modeling + unified identity for cross-cutting queries.

**Recommendation**: Design document for:
- `ErpParty` SQL view (UNION of Partner + Employee + Organization, or a lightweight view entity in EQL)
- Option B: Add `unifiedPartyId` column to all three entities with a UUID cross-reference table

#### 3.6 Cost Calculation Strategy Pattern

**Missing**: nop-app-erp's cost layer supports MOVING_AVERAGE and FIFO (`costing-methods.md`), but cost calculation is not structured as a pluggable strategy pattern with injectable sub-calculators. Compare: Wimoor's `IProfitService` + sub-calculator injection (ReferralFeeService, VariableClosingFeeService, etc.).

| Evidence | Source |
|----------|--------|
| Wimoor profit strategy: 10 site implementations + sub-calculator injection pattern | `wimoor-compare.md:174-193` |
| "nop's cost calculation could benefit from pluggable sub-calculators like Wimoor" | `wimoor-compare.md:663` |
| Current cost layer supports methods but no strategy pattern documentation exists | `costing-methods.md` |

**Why P1**: The strategy pattern would make cost calculation extensible for:
- Industry-specific cost elements (e.g., pharmaceutical: R&D cost absorption)
- Multi-element cost rollup (material + labor + overhead + subcontract + tooling)
- Custom allocation bases per customer deployment

**Recommendation**: Design document for `IErpInvCostCalculator` SPI with `@Inject Map<String, IErpInvCostCalculator>`, with implementations for MOVING_AVERAGE, FIFO, BATCH_SPECIFIC, and STANDARD_COST.

#### 3.7 MRP/DRP Simulation Engine

**Missing**: nop-app-erp has MRP calculation (`ErpMfgMrpBizModel`) and DRP demand aggregation, but there is no discrete simulation engine that can model "what-if" scenarios (e.g., what happens if supplier X delays 2 weeks, or if we increase safety stock by 20%). Compare: ERP5 SimulationMovement with plan → ordered → confirmed → stopped → delivered lifecycle.

| Evidence | Source |
|----------|--------|
| ERP5 SimulationMovement "plan→ordered→confirmed→stopped→delivered" 5-state model | `erp5-compare.md:323` |
| "nop's MRP/DRP prediction engine could benefit from SimulationMovement state design" | `erp5-compare.md:325,382` |
| Current MRP is single-pass calculation, not multi-scenario | Implementation reality |

**Why P1**: An MRP that can only calculate one scenario is insufficient for advanced manufacturing. "Most advanced" ERP should support what-if analysis, simulation versioning, and scenario comparison.

**Recommendation**: Design document defining:
- `ErpMfgSimulationScenario` entity (scenario version, assumptions, baseline)
- Simulation state machine: DRAFT → CALCULATING → COMPLETED → COMPARED
- Delta-based output: simulation result rows are MRP plan data with scenarioId dimension

---

### P2 — Strategic Differentiation

#### 3.8 Budget Management Design Enhancement

**Missing**: Budget management exists in design (`budget.md`, 107 lines) with BUDGET shadow posting via `businessType=BUDGET`, but the design lacks the comprehensive entity suite found in OFBiz (Budget, BudgetItem, BudgetRevision, BudgetReview, BudgetItemType, BudgetItemTypeAttr, BudgetItemAttribute). Implementation via `2026-07-10-1100-4` is marked ✅ done (budget scenario + detail lines + control log 3 entities + BUDGET shadow posting + control SPI), but the 7+ entity OFBiz suite (BudgetRevision, BudgetReview, etc.) provides deeper maturity.

| Evidence | Source |
|----------|--------|
| OFBiz Budget entity suite: Budget, BudgetItem, BudgetRevision, BudgetReview + Type/TypeAttr/Attribute | `ofbiz-compare.md:466` |
| nop design exists (`budget.md:107` lines) but "zero implementation" | `backlog/README.md:73` |
| Budget comparison dimension missing formal entity model | Design gap |

**Why P2**: Budget control is a core ERP requirement that most advanced ERPs implement deeply. The design exists but lacks the entity depth and operational maturity (multi-year budget, budget carry-forward, soft/hard freeze).

**Recommendation**: Enhance `budget.md` with:
- Full entity model matching OFBiz depth (Budget + BudgetScenario + BudgetItem + BudgetRevision)
- Multi-year budget vs annual budget semantics
- Budget carry-forward / rollover rules
- Budget transfer workflow (request → approve → execute)
- Configurable freeze levels (SOFT_FREEZE / HARD_FREEZE per period)

#### 3.9 Cross-Border Trade Extensions

**Missing**: `ErpMdMaterial` lacks cross-border trade fields (vatRate, drawbackRate, customsHS, countryOfOrigin). No MaterialCustoms entity. Compare: Wimoor has full cross-border fields integrated into its core material entity.

| Evidence | Source |
|----------|--------|
| Wimoor Material: vatrate/drawbackRate/deliveryCycle/assemblyTime fields | `wimoor-compare.md:539-546` |
| "nop Cross-border trade field supplement (vatRate/drawbackRate) — P2 priority" | `wimoor-compare.md:664` |
| nop ErpMdMaterial has leadTimeDays but no VAT/drawback fields | `master-data.orm.xml:171-200` |

**Why P2**: Cross-border trade (import/export) is a core ERP requirement for any system targeting manufacturing or trading companies. "Most advanced" ERP must handle VAT, duty drawback, customs declarations, and trading partner compliance.

**Recommendation**: Design document for:
- Extension fields on `ErpMdMaterial`: vatRate, drawbackRate, customsHS, countryOfOrigin, preferenceCode
- New entity `ErpMdMaterialCustoms` (customs declaration data per transaction)
- Delta extension path (not modifying core orm.xml) via `ext:baseClass`

#### 3.10 Date-Ranged Validity Pattern

**Missing**: nop-app-erp uses `businessDate` for transaction timestamping but lacks `fromDate`/`thruDate` pattern for validity-ranged master data (prices, agreements, relationships, qualifications). Compare: OFBiz uses this on `PartyRelationship`, `ProductCategoryMember`, `AgreementTerm`.

| Evidence | Source |
|----------|--------|
| OFBiz fromDate/thruDate on PartyRelationship, ProductCategoryMember, AgreementTerm | `ofbiz-compare.md:439` |
| "Add effectiveDate/expiryDate to master data entities (prices, agreements) where temporal validity is needed" | `ofbiz-compare.md:439` |

**Why P2**: Without date-ranged validity, master data changes are immediate and irreversible in time — you cannot ask "what was the price on January 1st?" if the price was updated on February 1st. This is essential for auditing, period-end closing, and regulatory compliance.

**Recommendation**: Design document defining:
- `effectiveDate`/`expiryDate` pattern for master data entities
- Temporal query helper methods in BizModel layer
- Affected entities: prices (ErpMdPriceListLine), agreements (ErpCtContract terms), supplier qualifications (AVL)
- Option A: Add columns to existing entities; Option B: Temporal version table pattern

#### 3.11 Multi-Company Architecture Depth

**Missing**: nop-app-erp has `orgId` for multi-org and `acctSchemaId` for multi-book, but lacks the explicit multi-company constructs found in OFBiz (GlAccountOrganization, PartyAcctgPreference, per-org CustomTimePeriod). Parent-subsidiary consolidation, intercompany elimination, and transfer pricing are high-level design only.

| Evidence | Source |
|----------|--------|
| OFBiz GlAccountOrganization + PartyAcctgPreference + per-org CustomTimePeriod | `ofbiz-compare.md:197-206` |
| "nop-app-erp's multi-org + multi-book is more correct but more complex" | `ofbiz-compare.md:497` |
| Inter-company consolidation design exists as `intercompany-consolidation.md` | `docs/design/finance/intercompany-consolidation.md` ✅ Design exists |

**Why P2**: Multi-company architecture is a hard requirement for any ERP serving enterprise groups. While nop's foundation is architecturally sound (orgId + acctSchemaId), the operational layer (intercompany elimination at period close, cross-org AR/AP netting, transfer pricing) is not fully designed.

**Recommendation**: Design document/update to `intercompany-consolidation.md` covering:
- Per-org accounting period state machine (close subsidiaries before parent)
- Intercompany elimination rules (investment elimination, AR/AP netting, revenue/cost elimination)
- Transfer pricing: standard cost vs arm's-length vs CUP methods
- Cross-org document flows (sales from company A, fulfillment from company B)

---

### P2/P3 — Enhancement Opportunities

#### 3.12 Business Module Metadata (BT5-style Dependency Declaration)

**Missing**: No business-level version/dependency declaration between modules. Current dependency is purely Maven-level (POM). Compare: ERP5 BT5 `dependency_list` declares `erp5_core >= 5.4.3` for `erp5_trade`, enabling versioned module composition.

| Evidence | Source |
|----------|--------|
| ERP5 BT5 `dependency_list`: explicit business-level version constraints between modules | `erp5-compare.md:313-315` |
| "nop could learn: add version/dependency metadata to module-meta.json, declare businessDependencies" | `erp5-compare.md:317,381` |
| nop has `module-meta.json` generation pipeline but no business dependency declaration | Design gap |

**Why P2**: As nop's module count grows, explicit business dependency metadata becomes essential for upgrade safety, multi-tenant version management, and SaaS deployment scenarios. Lightweight addition to existing `module-meta.json` pipeline.

**Missing**: No runtime plugin lifecycle. All modules are compile-time Maven dependencies. Compare: NocoBase supports plugin enable/disable without restart, with formal lifecycle hooks (afterAdd, beforeLoad, load, install, upgrade, beforeEnable/Disable, beforeRemove).

| Evidence | Source |
|----------|--------|
| NocoBase Plugin lifecycle: 11 hooks + topology-sorted loading + runtime enable/disable | `nocobase-compare.md:59-71` |
| "NocoBase plugins can be enabled/disabled at runtime — Nop modules fixed at compile time" | `nocobase-compare.md:95,411` |
| "Learn from NocoBase: plugin lifecycle management — introduce enable/disable mechanism to reduce restart needs" | `nocobase-compare.md:506` |

**Why P3**: Runtime plugin management is a major UX differentiator for platform-level extensibility. However, implementing it in nop would require fundamental changes to the Maven-based module system. Consider this post-stable-release.

---

*Note: The following capabilities were evaluated but determined to be already provided by the Nop Platform:*
- **Runtime field extension** — Nop Platform provides `JsonOrmComponent` / ext field pattern for adding extension fields without ORM changes.
- **Visual workflow designer** — Nop Platform provides the XWF workflow engine with XML configuration. A graphical designer would be a platform-level UX enhancement, not an application-level gap.
- **SaaS multi-tenancy** — Nop Platform has built-in multi-tenancy (`tenant-model.md` with `useTenant="true"`, automatic tenant filtering, tenant-scoped session cache). nop-app-erp's `orgId` provides additional organizational dimension.
- **Multi-data source abstraction** — Nop ORM driver layer + GraphQL driver support accessing third-party APIs as ORM data sources. This is a platform-level capability.
- **Runtime dictionary management** — Nop Platform provides runtime dictionary override/supplement through `nop-dict` API, in addition to compile-time ORM dictionaries.

## 4. Roadmap Supplements Needed

### Current Backlog Assessment

The current backlog (`docs/backlog/README.md` and sub-roadmaps) is structured as:

- **crud-roadmap.md**: 18 domains CRUD — ✅ done
- **core-business-roadmap.md**: M1 (core business) + M4 (finance end-to-end) + M5 (operational maturity) — ✅ done
- **extended-roadmap.md**: M2 (5 extended domains) + M3 (8 new domains) — ✅ done
- **frontend-ui-roadmap.md**: UI completeness — `planned` (pending)

The backlog is exceptionally operations-focused — it covers what the ERP does, but not how the platform is hardened for production, cloud, or customer-facing extensibility.

### 4.1 Platform Architecture Hardening Track

**Missing from backlog**: No explicit work items for cross-domain event orchestration, ECA-like declarative routing, or event-driven architecture formalization.

**Items to add**:

| Item | Priority | Dependencies | Rationale |
|------|----------|-------------|-----------|
| P0: Declarative cross-domain event routing design | P0 | None (pure design) | Addresses ERP5 §8 + OFBiz §9 gaps |
| P0: Event route runtime table + UI | P0-P1 | Design | Make cross-domain flows inspectable/configurable |
| P1: Migration of existing `@BizMutation` orchestration to event-driven | P1 | Design | Eliminates hardcoded step sequencing |
| P2: Event monitoring dashboard (event throughput, latency, failure rate) | P2 | Infrastructure | Operational visibility for event-driven arch |

### 4.2 Finance Deepening Track

**Missing from backlog**: Multi-company consolidation depth, intercompany elimination, transfer pricing, GL mapping rule tables.

**Items to add**:

| Item | Priority | Dependencies | Rationale |
|------|----------|-------------|-----------|
| P0: GL mapping rule table design + implementation | P0 | None | Addresses OFBiz §9.4 gap |
| P2: Intercompany elimination engine | P2 | Multi-company base | Required for enterprise group deployments |
| P2: Transfer pricing rules framework | P2 | Intercompany base | Cross-org transaction pricing |
| P2: Consolidated financial statements | P2 | Intercompany + multi-book | IFRS/GAAP consolidation |

### 4.3 Manufacturing Intelligence Track

**Missing from backlog**: Simulation engine, what-if analysis, advanced scheduling beyond existing APS.

**Items to add**:

| Item | Priority | Dependencies | Rationale |
|------|----------|-------------|-----------|
| P1: MRP/DRP simulation engine design | P1 | Existing MRP | Addresses ERP5 §8.4 gap |
| P2: What-if scenario comparison UI | P2 | Simulation engine | User-facing simulation analysis |
| P2: Multi-scenario MRP result diff | P2 | Simulation engine | Compare plan A vs plan B |

*Note: The Nop Platform already provides multi-tenancy (`tenant-model.md`), runtime field extension (`JsonOrmComponent`), and XWF workflow engine. These are not application-level gaps.*

### 4.4 Globalization Track

**Missing from backlog**: Cross-border trade fields, multi-country localization, VAT/GST compliance.

**Items to add**:

| Item | Priority | Dependencies | Rationale |
|------|----------|-------------|-----------|
| P2: Cross-border trade material fields | P2 | master-data delta | Addresses Wimoor §9 gap |
| P2: Customs declaration document flow | P2 | Cross-border base | Import/export compliance |
| P2: Multi-country VAT/GST engine | P2 | Tax infrastructure | Global tax compliance |
| P3: Localization packs (l10n) per country | P3 | Platform i18n | Country-specific accounting |

---

## 5. Competitive Positioning

### Coverage Matrix (10 Key Dimensions)

```
Dimension                  nop-app-erp   OFBiz    Wimoor    ERP5    Odoo*   ERPNext*
                          ------------   -----    ------    ----    ----    --------
Financial Accounting         ████████    ██████   ██░░░░   ██████  ██████  ██████
  - Posting engine           ★LEAD        ●        ○        ●       ●       ●
  - Multi-book native        ★UNIQUE      ○        ○        ○       ○       ○
  - Bad-debt Allowance       ★UNIQUE      ○        ○        ○       ○       ○
  - GL mapping tables        ○            ●        ○        ○       ●       ●
  - Consolidated fin stmts   ○            ○        ○        ○       ●       ●

Manufacturing                ████████    ██████   ░░░░░░   ██░░░░  ██████  ██████
  - BOM/Routing/WorkOrder    ★LEAD        ●        ○        ○       ●       ●
  - MRP/DRP engine           ●            ●        ○        ●       ●       ●
  - Simulation/what-if       ○            ○        ○        ●       ○       ○
  - CRP/APS                  ★LEAD        ○        ○        ○       ●       ○

Inventory                    ████████    ██████   ██░░░░   ████░░  ██████  ██████
  - 3-layer model            ★UNIQUE      ○        ○        ○       ○       ○
  - Batch/Serial/Consignment ★LEAD        ●        ○        ●       ●       ●
  - Landed cost              ●            ○        ○        ○       ●       ●

Sales/Purchase               ████████    ███████  ██░░░░   ██████  ███████ ███████
  - Pricing engine           ●            ●        ○        ○       ★       ●
  - Three-way matching       ●            ●        ○        ○       ●       ●
  - E-commerce integration   ○            ●        ★        ○       ●       ●

HR Management                ██████      ████░░   ░░░░░░   ████░░  ███████ ███████
  - Payroll + tax            ●            ○        ○        ○       ●       ●
  - Shift/Scheduling         ●            ○        ○        ○       ●       ●
  - Recruitment/Leave        ●            ○        ○        ○       ●       ●

Platform                     ████████    ███████  ██████   ██████  ██████  ██████
  - Codegen                  ★UNIQUE      ○        ○        ○       ○       ○
  - Delta customization      ★UNIQUE      ○        ○        ○       ○       ○
  - Runtime extensibility    ○            ●        ○        ★       ●       ●
  - Plugin lifecycle         ○            ○        ○        ●       ●       ●
  - Visual workflow designer ○            ○        ○        ○       ●       ○
  - SaaS multi-tenancy       ●※           ○        ●        ○       ●       ○
  - External API integration ○            ●        ★        ○       ●       ●

  ※ Nop Platform provides built-in multi-tenancy (tenant-model.md); nop-app-erp leverages it via orgId dimension

Cross-Domain                 ████████    ███████  ████░░   ██████  ██████  ██████
  - Event orchestration      ●※           ●        ○        ★       ●       ●
  - Unified party query      ○            ●        ○        ●       ●       ●
  - Notification subsystem   ★LEAD        ●        ○        ●       ●       ●

Quality & Maintenance        ████████    ██░░░░   ░░░░░░   ██░░░░  ████░░  ████░░
  - SPC / Control charts     ★LEAD        ○        ○        ○       ○       ○
  - NCR/CAPA                 ★LEAD        ○        ○        ●       ○       ○
  - Maintenance state machine★LEAD        ●        ○        ○       ●       ●

Testing & Quality            ████████    ██░░░░   ██░░░░   ██░░░░  ████░░  ████░░
  - E2E browser tests        ★UNIQUE      ○        ○        ○       ○       ○
  - Snapshot testing          ★UNIQUE      ○        ○        ○       ○       ○
  - 300+ automated tests     ★UNIQUE      ○        ○        ○       ●       ●

Legend:
  ★LEAD    = clear leader in open-source
  ★UNIQUE  = no comparable open-source implementation found
  ●        = feature present
  ○        = feature absent or minimal
  ░        = partial coverage in matrix bar

* Odoo/ERPNext assessments based on public knowledge — NOT from the 4 comparison reports.
```

### Strategic Assessment

**Where nop-app-erp already leads (no comparable open-source ERP matches):**

1. **Financial depth**: Multi-book native (`acctSchemaId`), bad-debt Allowance lifecycle, 40+ businessType dictionary, 3-layer posting engine with immutable saga, FX revaluation workflow, year-end closing
2. **Inventory model**: 3-layer (Move → Ledger → Balance) with cost layers, batch/serial, consignment, VMI, landed cost
3. **Manufacturing state machine**: 10-state WorkOrder + JobCard + CRP + APS integration + config-gated inspection
4. **Quality**: SPC control charts (Western Electric rules, Cp/Cpk), NCR/CAPA lifecycle, batch recall
5. **Notification**: Cross-domain event-driven notify subsystem with templates, frequency control, inbox
6. **Test coverage**: 300+ E2E tests across all domains, snapshot testing, visual regression, download binary regression
7. **Platform**: Model-first codegen + Delta customization — both unique in open-source

**Where gaps remain against best-in-class (application-level, net of Nop Platform capabilities):**

1. **GL mapping tables** → **NEW** `docs/design/finance/gl-mapping-rules.md` (existing `posting.md` has concept only, no schema)
2. **External API integration pattern** → **NEW** `docs/architecture/external-api-integration-pattern.md` (existing `integration-pattern.md` covers webhooks only)
3. **MRP/DRP Simulation engine** → **NEW** `docs/design/manufacturing/simulation-engine.md` (existing `mrp.md` is single-deterministic-run)
4. **Unified party identity query** → **NEW** `docs/design/master-data/unified-party-identity.md`
5. **Cross-border trade fields** → **NEW** `docs/design/master-data/cross-border-trade.md`
6. **Date-ranged validity pattern** → **NEW** `docs/design/date-ranged-validity-pattern.md` (convention used in ~6 entities but undocumented)
7. **Business module metadata** → **NEW** `docs/architecture/business-module-metadata.md`
8. **Cost calculation sub-calculator injection** → **EXPAND** `docs/design/finance/costing-methods.md` (well-covered for method dispatch, needs sub-calculator doc)
9. **Budget multi-year/carry-forward** → **EXPAND** `docs/design/finance/budget.md` (basic budget done, depth deferred)
10. **Multi-company operational depth** → **EXPAND** `docs/architecture/multi-company.md` (52L skeleton, needs operational detail)
11. **Plugin hot management** → **NEW** research doc (P3 feasibility study)

*Note: Cross-domain event orchestration is already provided by Nop Platform's NopSysEvent (topic-based routing, partition serial), IMessageService, I*Biz + xbiz delta, and Processor pattern. Runtime flexibility (classification, ext fields, dicts) is provided by platform dict API + JsonOrmComponent + tree entity pattern. SaaS multi-tenancy is provided by tenant-model.md.*

**Net position**: nop-app-erp has the deepest domain model breadth and financial architecture of any open-source ERP. Application-level gaps are concentrated in event routing declarativeness, GL mapping runtime configurability, simulation, and cross-border trade — all addressable without platform-level changes.

---

## 6. Recommendation: Priority-Actioned Gap Closure Plan

### Phase 1: New Design Documents (Weeks 1-4)

| Order | Document | Action | Priority | Rationale |
|-------|----------|--------|----------|-----------|
| 1 | `docs/design/finance/gl-mapping-rules.md` | **NEW** | P1 | GL account rule table schema + engine; existing `posting.md` covers concept only |
| 2 | `docs/architecture/external-api-integration-pattern.md` | **NEW** | P1 | Reference pattern for 3rd-party API auth/rate-limit/retry; existing `integration-pattern.md` (43L) covers webhooks only |
| 3 | `docs/design/manufacturing/simulation-engine.md` | **NEW** | P1 | Multi-scenario what-if MRP/DRP; existing `mrp.md` covers single deterministic run only |
| 4 | `docs/design/master-data/unified-party-identity.md` | **NEW** | P1 | Cross-entity identity query for Partner/Employee/Organization |

### Phase 2: New Design Documents (Weeks 5-8)

| Order | Document | Action | Priority | Rationale |
|-------|----------|--------|----------|-----------|
| 5 | `docs/design/master-data/cross-border-trade.md` | **NEW** | P2 | vatRate/drawbackRate/customsHS fields; no existing doc |
| 6 | `docs/design/date-ranged-validity-pattern.md` | **NEW** | P2 | fromDate/thruDate convention; pattern used in ~6 entities but undocumented |
| 7 | `docs/architecture/business-module-metadata.md` | **NEW** | P2 | BT5-style version/dependency in module-meta.json |

### Phase 3: Existing Document Expansions (Weeks 9-12)

| Order | Document | Action | Priority | Rationale |
|-------|----------|--------|----------|-----------|
| 8 | `docs/design/finance/costing-methods.md` | **EXPAND** | P1 | Add sub-calculator injection pattern documentation |
| 9 | `docs/design/finance/budget.md` | **EXPAND** | P2 | Add multi-year/carry-forward/commitment sections |
| 10 | `docs/architecture/multi-company.md` | **EXPAND** | P2 | Add operational depth (transfer pricing, cross-org AR/AP, consolidated reporting) |

### Phase 4: Research (Weeks 13-16)

| Order | Topic | Action | Priority | Rationale |
|-------|-------|--------|----------|-----------|
| 11 | **Plugin Hot Management Research** | **NEW research doc** | P3 | Feasibility study for Maven → runtime module lifecycle |

*Note: The following capabilities were evaluated but determined to be already provided by the Nop Platform: runtime field extension (JsonOrmComponent), visual workflow designer (XWF engine), SaaS multi-tenancy (tenant-model.md), multi-data source (ORM driver + GraphQL driver), runtime dictionary management (nop-dict API), event queue (NopSysEvent topic-based routing + partition serial + lease), cross-domain orchestration (IMessageService + I*Biz + xbiz delta + Processor).*

### Suggested Timeline

```
Week 1-2    Week 3-4     Week 5-8      Week 9-12      Week 13-16
┌─────────┐ ┌──────────┐ ┌───────────┐ ┌────────────┐ ┌──────────────┐
│GL Map-   │ │Party Iden│ │Cross-Bor- │ │Costing     │ │Plugin Hot    │
│ping      │ │tity Query│ │der Trade  │ │Methods ex- │ │Mgmt Research │
│Tables    │ │          │ │           │ │pansion     │ │              │
├─────────┤ ├──────────┤ ├───────────┤ ├────────────┤ └──────────────┘
│External  │ │Simulation│ │Date-Rang- │ │Budget.md   │
│API Pat-  │ │Engine    │ │ed Validi- │ │expansion   │
│tern      │ │          │ │ty Pattern │ │            │
└─────────┘ └──────────┘ ├───────────┤ ├────────────┤
                        │Module Me- │ │Multi-Comp  │
                        │tadata BT5 │ │Arch expan- │
                        └───────────┘ └────────────┘
```

### Backlog Supplement Recommendations

1. **Create `docs/backlog/deepening-roadmap.md`** — new sub-roadmap for all 11 items above
2. **Update `docs/backlog/implementation-roadmap.md`** — add reference to deepening-roadmap
3. **Update `docs/backlog/README.md`** — add P0-P2 items as structured work items

### Success Criteria for "Most Advanced Open-Source ERP" Claim

When all of the following are true:

1. **GL mapping rules** are database-configurable: new customer deployment can configure account determination without Java changes
2. **At least one simulation engine** (MRP/DRP) supports multi-scenario what-if analysis
3. **Unified party identity query** available: single query endpoint resolves partner/employee/organization identity
4. **Cross-border trade** fields and customs declaration entity available for import/export scenarios

The gap closure plan above is designed to achieve criteria 1-4 within 16 weeks of dedicated effort, positioning nop-app-erp as the undeniable leader across all competitive dimensions.
