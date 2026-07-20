# Apache OFBiz vs nop-app-erp: Deep Comparative Analysis

> **Context**: This report compares Apache OFBiz (full framework, including accounting/order/manufacturing/party/workeffort applications) against nop-app-erp (Nop Platform-based ERP reference application). Both are Java-based open-source ERP frameworks, but they differ fundamentally in architecture philosophy, entity modeling approach, and cross-module integration strategy.
>
> **OFBiz version**: Framework HEAD (git shallow clone at `~/sources/erp/ofbiz-framework/`)
> **nop-app-erp**: Current working tree (`~/app/nop-app-erp/`)
> **Date**: 2026-07-20

---

## 1. Accounting Engine: AcctgTrans Hub vs 3-Layer Posting Engine

### OFBiz: AcctgTrans Central Hub

OFBiz accounting revolves around the `AcctgTrans` entity as a **universal journal entry hub** that references all source business documents via direct FK columns:

- `acctg-entitymodel.xml:1764-1864` — `AcctgTrans` contains `invoiceId`, `paymentId`, `fixedAssetId`, `inventoryItemId`, `shipmentId`, `receiptId`, `workEffortId`, `finAccountTransId`, `physicalInventoryId` — a flat set of 9 source-document FKs.
- `acctg-entitymodel.xml:1869-1940` — `AcctgTransEntry` holds `glAccountId`, `organizationPartyId`, `amount`, `currencyUomId`, `origAmount/origCurrencyUomId` (dual-currency), `debitCreditFlag`, `reconcileStatusId`.

Posting is triggered by **Service ECA** (Event-Condition-Action):
- `secas_invoice.xml:24-27` — When `cancelInvoice` is invoked → `revertAcctgTransOnCancelInvoice` sync.
- `secas_invoice.xml:44-48` — When `setInvoiceStatus` returns with `INVOICE_READY` → `createMatchingPaymentApplication`.
- `secas_payment.xml` mirrors same pattern for payments.

OFBiz uses **minilang** (simple XML-based scripting language) for posting logic:
- `services_ledger.xml:71-83` — `quickCreateAcctgTransAndEntries` service (minilang, creates one debit + one credit entry).
- `services_ledger.xml:94-98` — `postGlJournal` (minilang, posts journal + updates GlAccountHistory).
- `services_ledger.xml:256-268` — `createAcctgTransAndEntries` accepts `acctgTransEntries` list.
- `services_ledger.xml:278-287` — `postAcctgTrans` with `transaction-timeout="600"`, checks period closed + balance equality.

**GL Mapping** is entity-driven:
- `InvoiceItemTypeGlAccount` (`acctg-entitymodel.xml:1462`) — maps invoice item type + org → GL account.
- `ProductCategoryGlAccount` (`product-entitymodel.xml:321`) — maps product category → GL account.
- `FixedAssetTypeGlAccount` (`acctg-entitymodel.xml:896`) — maps asset type → 5 accounts (asset/accum-depr/depreciation-expense/gain/loss).
- `VarianceReasonGlAccount` (`acctg-entitymodel.xml:2590`) — maps variance reason → GL account.
- `GlAccountTypeDefault` (`acctg-entitymodel.xml:2320`) — default per org per type.

**Key weakness**: Each new source-doc type requires adding a new FK column to `AcctgTrans`. The flat FK approach creates tight coupling — any new business type (e.g., expense claims, contract milestones) requires schema change to the central hub. Posting logic is scattered across individual minilang/groovy services (`createAcctgTransForPurchaseInvoice`, `createAcctgTransForSalesInvoice`, `createAcctgTransForWorkEffortIssuance`, etc. — see `services_ledger.xml:348-503` for 15+ individual `createAcctgTransFor*` services).

### nop-app-erp: 3-Layer Posting Engine

nop-app-erp's posting engine (`docs/design/finance/posting.md:11-42`) is explicitly layered:

```
Layer 1 (SYNC, immutable): Business doc + inventory write in same @BizMutation transaction → posted=false.
Layer 2 (configurable SYNC/ASYNC): Voucher generation, routed by businessType via IErpFinAcctDocProvider.
Layer 3 (immutable saga): posted flag idempotency, back-scan, physical lock, red-ink reversal, audit trail.
```

**Pluggable Provider mechanism** (`posting.md:196-209`):
- `IErpFinAcctDocProvider` SPI with `getSupportedBusinessTypes()` + `createFacts(billData, acctSchema) → List<VoucherLine>`.
- `ErpFinAcctDocRegistry` collects all providers via `@Inject List<IErpFinAcctDocProvider>` at startup.
- Cross-domain: purchase supplies `PurAcctDocProvider` for `AP_INVOICE/PAYMENT/PURCHASE_INPUT`; sales supplies `SalAcctDocProvider` for `AR_INVOICE/RECEIPT/SALES_OUTPUT`.
- **New business type = new Provider bean, zero changes to finance core**.

**Business type enumeration** (`module-finance/model/app-erp-finance.orm.xml:60-127`):
- 40+ business types (from `PURCHASE_INPUT` through `SUBCONTRACT_FEE`), each with documented debit/credit semantics (`docs/design/finance/posting.md:79-138`).

**Validator extension point** (`posting.md:222-262`):
- `IErpFinFactsValidator` SPI for pre-write validation/rewriting of voucher lines (GL distribution, compliance, per-tenant customization).

**Red-ink reversal is bidirectional** (`posting.md:337-418`):
- Direction 1: Business doc cancellation → `IErpFinPostingBiz.reverse()` creates red-ink voucher.
- Direction 2: Finance-side red-ink → `VoucherReversedEvent` → domain listeners revert business doc status (domain autonomy, engine does not hold source entity ORM reference — see "Reverse Write Contract" at `posting.md:423-456`).

### Comparison

| Aspect | OFBiz | nop-app-erp |
|--------|-------|-------------|
| **Hub design** | Single flat `AcctgTrans` with 9 source-doc FK columns | `IErpFinAcctDocProvider` SPI + `businessType` routing (no FK columns in voucher) |
| **Posting trigger** | Service ECA (declare in `secas_*.xml`) | `@BizMutation` + `IErpFinAcctDocProvider.createFacts()` (Java SPI) |
| **Extensibility** | New FK + new minilang service `createAcctgTransFor*` | New `IErpFinAcctDocProvider` bean, zero schema change |
| **Voucher model** | Debit-credit entries with `debitCreditFlag` | Chinese 3-element voucher type (收/付/转) with single-side `drAmount`/`crAmount` |
| **Period control** | `CustomTimePeriod.isClosed` flag | `ErpFinAccountingPeriod` with 5-state machine (NEVER_OPENED/OPEN/CLOSING/CLOSED/CLOSED_FINAL) |
| **Multi-schema** | Per-org GL mapping tables | `acctSchemaId` dimension on voucher line (native multi-book) |
| **Platform language** | Minilang/Groovy XML services | Java `@BizMutation` + xbiz XPL templates |

**Verdict**: OFBiz's AcctgTrans hub is battle-tested (20+ years) but its flat FK + per-source-type minilang service pattern creates tight cross-module coupling. nop-app-erp's SPI-based provider pattern is more modular — adding a new posting source requires zero schema changes and zero changes to the finance core — but is untested in production scale. The posted flag + back-scan saga pattern (`domain-design-guidelines.md:14.2`) mirrors OFBiz's `isPosted` flag but formalizes the saga contract.

---

## 2. Entity Modeling: OFBiz Type+Attr+Status vs nop Per-Domain ORM

### OFBiz: Universal Type+Attr+Status Pattern

OFBiz uses a repetitive **three-part pattern** across every domain:

1. **{Entity}Type** with `parentTypeId` self-reference (e.g., `BudgetItemType` at `acctg-entitymodel.xml:122-133`).
2. **{Entity}TypeAttr** — allowed attributes per type (e.g., `BudgetItemTypeAttr` at `acctg-entitymodel.xml:134-151`).
3. **{Entity}Attribute** — name/value pair custom fields (e.g., `BudgetItemAttribute` at `acctg-entitymodel.xml:103-121`).

This generates 3 extra entity definitions per business entity. Evidence across all entity models:
- `order-entitymodel.xml:48-51` — `OrderAdjustment` with `orderAdjustmentTypeId`, plus `OrderAdjustmentType` (:176), `OrderAdjustmentTypeAttr`, `OrderAdjustmentAttribute`.
- `party-entitymodel.xml:117-131` — `AgreementAttribute`, `AgreementTypeAttr` (same pattern).
- `workeffort-entitymodel.xml:184-200` — `WorkEffort` has `workEffortTypeId`.

**Status machine pattern**:
- Every entity has `statusId` referencing `StatusItem`.
- `StatusValidChange` enforces allowed transitions (not shown in entity snippets but standard OFBiz pattern).
- `{Entity}Status` table tracks history.

**Result**: OFBiz entity models are extremely large. `accounting-entitymodel.xml` = 4041 lines for ~50 business entities (because of the Type/TypeAttr/Attribute triples). `order-entitymodel.xml` = 3113 lines. `party-entitymodel.xml` = 3120 lines.

### nop-app-erp: Per-Domain Compact ORM

nop-app-erp uses one `orm.xml` per domain, with **no automatic Type+Attr+Status triples**:

- `module-finance/model/app-erp-finance.orm.xml` — 1936 lines for ~30 entities + 25 dictionary definitions. Dictionary `erp-fin/business-type` (`:60-127`) replaces the EntityType pattern with a compact option list.
- `module-purchase/model/app-erp-purchase.orm.xml` — 1289 lines for ~20 entities + dictionaries.
- `module-master-data/model/app-erp-master-data.orm.xml` — 1162 lines for ~30 entities.
- `module-manufacturing/model/app-erp-manufacturing.orm.xml` — 1645 lines for ~25 entities.

**Dictionary-as-type**: Instead of `OrderType → OrderTypeAttr → OrderAttribute`, nop uses `ext:dict` on a column (e.g., `ErpPurOrder.docStatus` with `ext:dict="erp-pur/doc-status"` at `module-purchase/model/app-erp-purchase.orm.xml:118`). The dictionary options define the full closed value set inline.

**Cross-domain naming conventions** (`domain-module-split-analysis.md:46-67`):
- Table prefix: `erp_<short>_` (e.g., `erp_fin_voucher`, `erp_pur_order`).
- Class prefix: `Erp<Short>` (e.g., `ErpFinVoucher`, `ErpPurOrder`).
- All 18 domains follow identical conventions.

### Comparison

| Aspect | OFBiz | nop-app-erp |
|--------|-------|-------------|
| **Model style** | Single shared `datamodel/entitydef/*.xml` per domain group | Per-domain `module-<domain>/model/*.orm.xml` |
| **Extensibility pattern** | `{Entity}Type` + `{Entity}TypeAttr` + `{Entity}Attribute` (EAV triples per entity) | `ext:dict` inline options + per-domain dictionary namespace |
| **Entity count blowup** | 3×-4× (Type, TypeAttr, Attribute triples) | 1× (no automatic EAV triples) |
| **Status machine** | `statusId` + `StatusValidChange` table (external) | `ext:dict` + domain-specific state machine doc per domain |
| **Single file size** | 3000-4000 lines (e.g., `accounting-entitymodel.xml` 4041 lines) | 1000-2000 lines per domain |
| **Total lines** | ~20,000 across major entity files | ~7,000 across 18 domains |

**Verdict**: OFBiz's Type+Attr+Status pattern provides maximal runtime flexibility (add attributes without schema change) but at enormous complexity cost. Each new entity requires maintaining 3-4 extra XML definitions. nop-app-erp's dictionary-driven approach is far more compact, but changing a dictionary requires a redeploy (no runtime EAV for truly dynamic attributes). For most ERP use cases, the dictionary approach is sufficient; OFBiz's EAV pattern is overkill for stable domains but valuable for highly customized deployments.

---

## 3. Cross-Module Integration: OFBiz Direct FK + ECA vs nop I\*Biz + Weak Pointer

### OFBiz: Direct FK + View-Entity + Service ECA

OFBiz uses three integration mechanisms:

1. **Direct FK references** across entity models (all entities in shared `datamodel/` namespace):
   - `OrderItem.productId → Product` (`order-entitymodel.xml`)
   - `AcctgTrans.invoiceId → Invoice` (`acctg-entitymodel.xml`)
   - `WorkEffortGoodStandard.workEffortId → WorkEffort` (`workeffort-entitymodel.xml`)
   - `TimeEntry.workEffortId → WorkEffort` (`workeffort-entitymodel.xml:50`)

2. **View-entities** for aggregate queries:
   - `MrpEventView` at `manufacturing-entitymodel.xml:194-200` — SQL JOIN between `MrpEvent` and `Product`.
   - `AcctgTransEntrySums` — GROUP BY aggregate view entity.

3. **Service ECA** for cross-module orchestration:
   - `secas_invoice.xml:39-43` — Invoice status change → auto-create payment application.
   - Service ECA ties together the `accounting` and `order` modules declaratively.

4. **Association entities** for N:M relationships:
   - `OrderItemBilling` at `order-entitymodel.xml:719` — links OrderItem ↔ InvoiceItem (order-to-cash key joint).
   - `OrderHeaderWorkEffort` — links Order ↔ WorkEffort (order-to-manufacturing).

### nop-app-erp: I\*Biz + Weak Pointer + DAG Constraints

nop-app-erp enforces strict **DAG (Directed Acyclic Graph)** dependency rules (`domain-module-split-analysis.md:163-236`):

1. **All cross-domain references must follow DAG direction**: `master-data → inventory → purchase/sales → finance`. Upstream → downstream references are **forbidden**.

2. **Approved ORM cross-references only from master-data** (`domain-module-split-analysis.md:217-231`): All modules can reference master-data entities via ORM `<to-one>` (e.g., `ErpPurRequisitionLine.material` → `ErpMdMaterial` at `module-purchase/model/app-erp-purchase.orm.xml:181`). Beyond that, modules use **weak pointers** with `source_bill_type`/`source_bill_code` string pairs.

3. **I\*Biz interface pattern** for all cross-domain writes (`domain-design-guidelines.md:34-39`):
   - `IErpInvStockMoveBiz.generateIncomingMove()` invoked by purchase domain.
   - `IErpFinAcctDocProvider` SPI for cross-domain voucher generation.
   - `IErpMdMaterialBiz`, `IErpMdPartnerBiz` for master-data queries.

4. **Business type routing** instead of direct FK:
   - `businessType` (`module-finance/model/app-erp-finance.orm.xml:60`) routes to `IErpFinAcctDocProvider`.
   - `billType` + `billHeadCode` + `lineCode` triple identifies source documents (`posting.md:66-69`).

### Comparison

| Aspect | OFBiz | nop-app-erp |
|--------|-------|-------------|
| **Module coupling** | Flat namespace — all entities can FK to anything | Strict DAG — only master-data FKs allowed cross-module |
| **Integration mechanism** | Direct FK + view-entity + Service ECA | I\*Biz interface + weak pointer (type+code triple) |
| **Decoupling level** | Moderate (ECA helps but FK graph is dense) | Strong (ORM-level isolation, integration only via interfaces) |
| **Query flexibility** | Cross-entity SQL JOIN via view-entities | EQL with I\*Biz cross-domain queries (not SQL JOIN) |
| **Schema change cost** | Low (add FK + view-entity) | Low (add I\*Biz method + Provider implementation) |
| **Dependency cycles** | Possible (no framework-level guard) | Impossible (explicit DAG + prohibited reverse references) |

**Verdict**: OFBiz's approach is pragmatic and fast to develop — you just add a FK and a view-entity. But this creates a dense dependency graph that makes modular extraction difficult (evidenced by OFBiz's monolith retention). nop-app-erp's DAG + I\*Biz approach is architecturally cleaner and enables true domain autonomy, at the cost of more interface definitions per cross-domain operation. The "weak pointer" pattern (type+code strings) is a proven ERP pattern (iDempiere uses `AD_Table_ID + Record_ID`) and avoids ORM-level coupling entirely.

---

## 4. Multi-Company / Multi-Currency

### OFBiz: Organization-Party + GlAccountOrganization

OFBiz handles multi-company via the `organizationPartyId` dimension:

- `GlAccountOrganization` at `acctg-entitymodel.xml:2150` — maps GL accounts to organizations. Every `AcctgTransEntry` has `organizationPartyId`.
- `GlAccountHistory` at `acctg-entitymodel.xml:2167` — period balance per account per organization (`openingBalance`, `postedDebits`, `postedCredits`, `endingBalance`).
- `PartyAcctgPreference` — per-org accounting preferences.
- `CustomTimePeriod` — periods can be per-organization (no dedicated entity for per-org period close).

**Multi-currency**: `AcctgTransEntry` has dual-currency fields:
- `amount` + `currencyUomId` (transaction currency).
- `origAmount` + `origCurrencyUomId` (original currency for dual-currency transactions at `acctg-entitymodel.xml:1869-1940`).

**Completeness**: OFBiz has period-end closing (`services_ledger.xml:289-293` — `closeFinancialTimePeriod`), trial balance (`calculateGlAccountTrialBalance`), income statement (`prepareIncomeStatement`), and `GlAccountGroup`/`GlAccountGroupMember` for financial statements.

### nop-app-erp: Native Multi-Book with acctSchemaId

nop-app-erp separates organizational from accounting dimensions:

- **Multi-book native**: `acctSchemaId` on voucher lines (`domain-design-guidelines.md:503`). The `ErpMdAcctSchema` entity (with `erp-md/acct-schema-nature` dict — FINANCIAL/MANAGEMENT/TAX/CONSOLIDATION/BUDGET at `module-master-data/model/app-erp-master-data.orm.xml:116-122`) supports multiple parallel accounting books.
- **Per-org period**: `orgId` on all business documents (`domain-design-guidelines.md:435`). Period state machine (NOT_OPENED/OPEN/CLOSING/CLOSED) is per-org (`domain-design-guidelines.md:347-363`).
- **Multi-currency four-piece**: `currencyId` + `exchangeRate` + `amountSource` + `amountFunctional` on all financial entities (`domain-design-guidelines.md:490-493`).
- **Period-end closing** includes: FX revaluation (`domain-design-guidelines.md:388-405`), cost closing, bad-debt provisioning, P&L closing.

### Comparison

| Aspect | OFBiz | nop-app-erp |
|--------|-------|-------------|
| **Multi-company** | `organizationPartyId` on every entry | `orgId` dimension + separate `acctSchemaId` for multi-book |
| **Multi-book** | Not native (per-org GL mapping) | Native (`acctSchemaId` on every voucher line, 5 schema types) |
| **Period control** | `CustomTimePeriod.isClosed` | 5-state period state machine with module-level close gate |
| **FX revaluation** | Not built-in (manual adjustment entries) | Formal revaluation workflow + FX gain/loss business type |
| **Bad-debt provisioning** | Not built-in (ad-hoc adjustment) | Full Allowance-method lifecycle (`bad-debt.md`) |
| **Financial reports** | `GlAccountGroup` + `prepareIncomeStatement` service | nop-report integration + seed reports per domain |

**Verdict**: OFBiz's multi-company support is adequate for basic scenarios but lacks native multi-book parallel accounting. nop-app-erp's `acctSchemaId` dimension provides cleaner multi-book separation (statutory, tax, management, consolidation books with different COAs). However, nop-app-erp's approach is currently untested at production scale, while OFBiz's `GlAccountOrganization` is proved in many deployments.

---

## 5. Manufacturing: WorkEffort-as-Production-Run vs ErpMfgWorkOrder + Processor

### OFBiz: Production Run = WorkEffort Subtype

OFBiz treats **production runs as a `WorkEffort` subtype** (`workEffortTypeId="PROD_RUN"`):

- `workeffort-entitymodel.xml:184-200` — `WorkEffort` is a universal entity for tasks, projects, production runs, maintenance. It has ~40 fields including `workEffortTypeId`, `currentStatusId`, `workEffortParentId`, `priority`, `percentComplete`, `estimatedStartDate`, `estimatedCompletionDate`, `actualStartDate`, etc.
- `manufacturing-entitymodel.xml:43-74` — `ProductManufacturingRule` is the BOM (productId → productIdIn + quantity). Note: BOM is defined per-rule, not as a versioned head-line structure.
- `manufacturing-entitymodel.xml:80-149` — `TechDataCalendar` / `TechDataCalendarWeek` / `TechDataCalendarExcDay` for production calendar (day-level capacity definition with individual startTime+capacity per weekday).

**WorkEffort sub-type associations**:
- `WorkEffortGoodStandard` — BOM requirements (raw materials → finished goods).
- `WorkEffortFixedAssetAssign` — equipment/workcenter assignment.
- `WorkEffortPartyAssignment` — personnel assignment.
- `WorkEffortInventoryProduced` — produced inventory tracking.

**Service files**: `services_production_run.xml` — `createProductionRun` (:76), `createProductionRunsForOrder` (:123) that creates production runs from sales orders.

### nop-app-erp: ErpMfgWorkOrder + Processor Pattern

nop-app-erp has a **dedicated WorkOrder entity** with its own 10-state machine:

- `module-manufacturing/model/app-erp-manufacturing.orm.xml:35-46` — `erp-mfg/work-order-status` dict with 10 states: DRAFT, SUBMITTED, NOT_STARTED, IN_PROCESS, STOCK_RESERVED, STOCK_PARTIAL, COMPLETED, STOPPED, CLOSED, CANCELLED.
- `ErpMfgWorkOrder` has dedicated BOM (`ErpMfgBom` at `:192`), Routing (`ErpMfgRouting`), Workcenter (`ErpMfgWorkcenter`), and Production Version entities — not shared with projects or tasks.
- `ErpMfgWorkOrderLine` at `:668-720` — multi-purpose line (`lineType` = OUTPUT/INPUT/BYPRODUCT, with `materialId`, `plannedQuantity`, `actualQuantity`, `scrappedQuantity`).

**Processor pattern** (`ErpMfgWorkOrderBizModel.java:32-106`):
- `ErpMfgWorkOrderBizModel` is a thin Facade (extends `CrudBizModel<ErpMfgWorkOrder>`), delegating all operations to `ErpMfgWorkOrderProcessor`.
- Methods: `checkAvailability()` (`:45`), `start()` (`:51`), `stop()` (`:57`), `resume()` (`:63`), `close()` (`:69`), `cancel()` (`:75`), `reportCompletion()` (`:81`), `generateJobCardsFromSchedule()` (`:89`).
- **Two-layer structure** (Facade BizModel + Processor): The Processor pattern (`processor-extension-pattern.md`) allows downstream subclasses to override individual step methods, enabling vertical industry customization without touching the core BizModel.

### Comparison

| Aspect | OFBiz | nop-app-erp |
|--------|-------|-------------|
| **Entity model** | `WorkEffort` universal (40+ fields, all types in one table) | Dedicated `ErpMfgWorkOrder` (10-state specific state machine) |
| **BOM** | Flat `ProductManufacturingRule` (per-rule, no head-line versioning) | `ErpMfgBom` head + lines with versioning, routing, workcenter |
| **State machine** | `WorkEffort.currentStatusId` (shared status across all work effort types) | 10-state machine specific to manufacturing (DRAFT→SUBMITTED→NOT_STARTED→...) |
| **Customization** | Override services in Groovy/minilang | BizModel Facade + Processor pattern (Java, protected step override) |
| **APS integration** | MRP events (`MrpEvent` at `manufacturing-entitymodel.xml:166-192`) | `ErpMfgScheduleToJobCardProcessor` (Java, `:89-104`) |
| **Job Card** | No dedicated job card entity | `ErpMfgJobCard` with 9-state machine (`erp-mfg/job-card-status` at `:47-56`) |

**Verdict**: OFBiz's single-entity approach (`WorkEffort` for everything) is simpler and enables cross-domain associations (work effort → project → timesheet → invoice) without any mapping. But the 40-field table has significant downsides: many fields are irrelevant for any given subtype, and adding a manufacturing-specific field affects all work effort entities. nop-app-erp's dedicated entity approach is more type-safe and maintainable, while the Processor pattern provides a cleaner extension path for vertical customization than OFBiz's service-override model.

---

## 6. Order-to-Cash Chain

### OFBiz: Order → Ship → Invoice → Payment → AcctgTrans

OFBiz Order-to-Cash chain (`order-entitymodel.xml`):

1. **Quote** → **OrderHeader/OrderItem** (`order-entitymodel.xml:415,520`) — `orderTypeId`, `statusId`, `currencyUom`, `grandTotal`.
2. **OrderItemShipGroup** (`order-entitymodel.xml:905`) — shipping group per order (address, carrier, tracking).
3. **ItemIssuance** (product domain) — inventory issue (COGS trigger).
4. **OrderItemBilling** (`order-entitymodel.xml:719`) — **key link entity**: connects `OrderItem` ↔ `InvoiceItem` + `ItemIssuance` (the 3-way joint for order→invoice→shipment). This is a highly pragmatic approach — a single association table tracks all three legs.
5. **Invoice/InvoiceItem** (accounting domain) — AR creation.
6. **PaymentApplication** — payment ↔ invoice matching.
7. **Payment** — actual cash receipt.
8. **AcctgTrans** — AR/Revenue/COGS/Tax posting.

**Key design**: `OrderItemBilling` is the central linking entity — it records the relationship between an order item, its shipment issuance, and its invoice item. This avoids complex multi-way joins.

### nop-app-erp: Purchase Receive → Invoice → Payment → Posting

nop-app-erp separates purchase (AP) and sales (AR) into separate domains with **mirror symmetry** (`docs/design/sales/README.md:109-124`):

**Procure-to-Pay** (purchase domain):
- `ErpPurOrder → ErpPurReceive → ErpPurInvoice → ErpPurPayment → posted=false → posting event`
- `docs/design/purchase/README.md:38-47` — "三单匹配 (three-way matching): PO → Receive → Invoice".

**Order-to-Cash** (sales domain):
- `ErpSalOrder → ErpSalDelivery → ErpSalInvoice → ErpSalReceipt → posted=false → posting event`
- `docs/design/sales/README.md:38-45` — Same structure as purchase but mirrored.

**Cross-document navigation** (`docs/design/cross-doc-navigation-patterns.md:23-28`):
- Purchase chain: `RFQ → Quotation → PO → Receive → Invoice → Payment → Voucher`
- Sales chain: `Quotation → SO → Delivery → Invoice → Receipt → Voucher`
- Each step: FK-based `fixedProps` drawer or `link` button for forward/backward navigation.

**Business type routing** (`docs/design/finance/posting.md:81-89`):
- `PURCHASE_INPUT` → Debit Inventory / Credit AP (purchase receipt).
- `AP_INVOICE` → Debit Expense/Purchase, Debit Input Tax / Credit AP.
- `AR_INVOICE` → Debit AR / Credit Revenue, Credit Output Tax.
- `PAYMENT` → Debit AP / Credit Bank.
- `RECEIPT` → Debit Bank / Credit AR.

### Comparison

| Aspect | OFBiz | nop-app-erp |
|--------|-------|-------------|
| **Chain structure** | Quote→Order→Ship→Invoice→Payment→AcctgTrans | Separate purchase (PO→Receive→Invoice→Payment) and sales (SO→Delivery→Invoice→Receipt) chains |
| **Key linking entity** | `OrderItemBilling` (3-way: order↔ship↔invoice) | Weak pointer `(billType, billHeadCode, billCode)` + businessType routing |
| **Mirror symmetry** | Not explicit (order handles both purchase and sales via `orderTypeId`) | Deliberate mirror symmetry between purchase and sales domains |
| **Three-way matching** | Supported via `OrderItemBilling` (linked receipt) | Supported via `three-way-match.md` (`docs/design/purchase/three-way-match.md`) |
| **Adjustments** | `OrderAdjustment` entity with 22 fields (tax, promo, shipping, override GL) | Simpler line-level discount/tax fields per business entity |
| **Returns** | `ReturnHeader/ReturnItem` (separate entity) | Purchase return `ErpPurReturn` / Sales return `ErpSalReturn` (mirror symmetry) |

**Verdict**: OFBiz's unified `Order` entity (both purchase and sales through `orderTypeId`) is simpler at schema level but conflates two different workflows. nop-app-erp's split purchase/sales with mirror symmetry is cleaner for domain modeling and aligns with the multi-module DAG architecture. OFBiz's `OrderItemBilling` is more elegant than nop's weak-pointer approach for the specific order→ship→invoice link — nop could consider a similar dedicated linking entity for high-traffic order→delivery→invoice paths.

---

## 7. Party Model: Unified Party vs Split Domain Entities

### OFBiz: Unified Party + PartyRole

OFBiz's Party model is its most influential design pattern:

- `Party` — universal entity: `partyId`, `partyTypeId` (PERSON/GROUP), `statusId`. All parties (customers, suppliers, employees, organizations) share this table.
- `Person` — personal subtype (firstName, lastName, birthDate).
- `PartyGroup` — organizational subtype (groupName, taxId, etc.).
- `PartyRole` — role: `partyId + roleTypeId`. Any party can have multiple roles (e.g., same party can be both CUSTOMER and SUPPLIER).
- `RoleType` — role type catalog (CUSTOMER, SUPPLIER, EMPLOYEE, MANAGER, etc.).
- `PartyRelationship` — relationship between parties (partyIdTo/From + roleTypeIdTo/From + fromDate/thruDate — date-ranged validity).

This pattern pervades all OFBiz modules: every entity that references a business entity uses `partyId` + `roleTypeId` (e.g., `AcctgTransEntry.partyId`, `OrderHeader.partyId`, `Invoice.partyId`).

### nop-app-erp: Split Domain Entities

nop-app-erp splits business entities into separate domain entities based on their role:

- `ErpMdPartner` (`module-master-data/model/app-erp-master-data.orm.xml`) — customers AND suppliers combined (with `partnerType` dict `erp-md/partner-type`: CUSTOMER/SUPPLIER/BOTH/EMPLOYEE at `:77-82`).
- `ErpMdEmployee` — staff/employee entity (separate from Partner, referenced by manufacturing, HR, maintenance).
- `ErpMdOrganization` — organizational structure (group, company, branch, department) with `erp-md/org-type` dict (`:108-115`).

**Domain-specific references**:
- `ErpPurRequisitionLine.suggestedSupplierId → ErpMdPartner` (`module-purchase/model/app-erp-purchase.orm.xml:169`).
- `ErpPurRequisition.requesterId → ErpMdEmployee` (`:114`).
- `ErpPurRequisition.orgId → ErpMdOrganization` (`:131`).

### Comparison

| Aspect | OFBiz | nop-app-erp |
|--------|-------|-------------|
| **Unified identity** | `Party` table for all (customers, suppliers, employees, orgs) | Split: `ErpMdPartner` (customer/supplier), `ErpMdEmployee` (staff), `ErpMdOrganization` (org units) |
| **Role system** | `PartyRole` + `RoleType` (dynamic, unlimited roles) | `partnerType` dict fixed to 4 values (CUSTOMER/SUPPLIER/BOTH/EMPLOYEE) |
| **Relationships** | `PartyRelationship` with date-ranged validity (fromDate/thruDate) | Not formalized (relationship through business documents) |
| **Federation** | `Group` → `Person` relationship via PartyRelationship | `ErpMdOrganization` tree (orgType hierarchy from GROUP→COMPANY→BRANCH→DEPARTMENT...) |
| **Contact mechanisms** | `ContactMech` entity (email, phone, address, postal) with `ContactMechPurpose` | Embedded fields in Partner/Employee entities |
| **Agreements** | `Agreement` + `AgreementItem` + `AgreementTerm` (full-featured) | Separate `contract` domain (`module-contract/`) with `ErpCtContract` |

**Verdict**: OFBiz's unified Party model is architecturally elegant and proven in cross-domain scenarios — querying "all transactions for party X" requires one simple join. However, it creates an extremely wide `Party` table and requires careful index management. nop-app-erp's split approach is more domain-idiomatic (better aligns with bounded contexts in DDD) but makes cross-entity queries harder — finding "all transactions for a given partner" requires multiple queries or a union. For an ERP that needs to eventually support complex party relationships (contact hierarchy, trading partner networks, customer→employee relationships), nop-app-erp may need to consider a more unified approach.

---

## 8. Platform Capabilities: OFBiz Framework vs Nop Platform

### OFBiz Framework

OFBiz is a **complete application framework** with:

1. **Entity Engine** — XML entity definitions → auto-generated CRUD, view-entities for SQL JOIN, `entity-auto` service engine for automatic service generation.
2. **Service Engine** — Declarative services via XML (`services.xsd`), supports multiple `engine` types: `entity-auto` (auto CRUD at `services_ledger.xml:30-39`), `simple` (minilang at `:71-83`), `groovy` (at `:196-202`), `java` (at `:667-672`), `interface` (service interface at `:184-194`).
3. **Service ECA** — Event-Condition-Action for cross-service orchestration (`secas_invoice.xml:24-27`).
4. **Widget/Webapp Engine** — Screen widgets, forms, menus, FreeMarker templates.
5. **Minilang** — Proprietary XML scripting language for inline service logic.
6. **Resource/Message** — i18n via `*EntityLabels.xml` resource bundles.
7. **Security** — Permission service checking (`service name="basicGeneralLedgerPermissionCheck"` at `services_ledger.xml:32`).

**Key design choice**: Everything is XML-declared — entity, service, UI, security. This enables non-developer configuration but creates a steep learning curve for the proprietary DSLs.

### Nop Platform

Nop Platform provides:

1. **Nop ORM** — XML-based `orm.xdef` (like `module-finance/model/app-erp-finance.orm.xml:29-32`) with strict schema validation against `.xdef` schemas.
2. **Code Generation** — `nop-cli gen` or Maven `clean install` → incremental codegen from ORM model. **Model is the single source of truth**.
3. **Delta Mechanism** — `x:extends` and `x:override` for customizing generated code without editing `_gen/` directories (`AGENTS.md` delta section).
4. **IoC Container** — `@Inject` DI (same as Spring but from Nop's own container).
5. **CrudBizModel<T>** — Auto-generates CRUD GraphQL endpoints from ORM model.
6. **xbiz** — XPL-based XML logic layer for hooks (`beforeSave`, `afterQuery`, field `autoExpr`).
7. **XMeta / XView** — Declarative meta and view layer for AMIS frontend rendering.
8. **Nop Reports** — Built-in reporting (`nop-report` integration for seed reports).
9. **GraphQL Engine** — Auto-derived GraphQL schema from BizModel annotations (`@BizQuery`, `@BizMutation`).

### Comparison

| Capability | OFBiz | Nop Platform |
|-----------|-------|-------------|
| **CRUD auto-generation** | `entity-auto` service engine + auto forms | `CrudBizModel<T>` + codegen `_gen/` files |
| **Customization mechanism** | Service override in XML (same file + `xsi:override`) | Delta overlays (`_delta/`), `x:extends` inheritance |
| **Logic language** | Minilang (proprietary XML), Groovy, Java | XPL (template language), Java `@BizMutation`, xbiz |
| **UI framework** | Widget/FreeMarker (server-side rendered) | AMIS (JSON-based, client-side rendered) |
| **Schema validation** | DTD-based (`entitymodel.xsd`) | XDEF-based (100+ schemas, compiled) |
| **Code generation** | Not primary (manual entity def → auto services) | Primary paradigm (ORM model → all layers via `nop-cli gen`) |
| **Query layer** | View-entities (SQL JOIN in entity engine) | EQL (Nop's own query language) + sql-lib.xml |
| **Test framework** | No unified test framework | `JunitAutoTestCase` with RECORDING→CHECKING snapshot testing |
| **Workflow** | WorkEffort-based manual workflow | `nop-wf` (integrated workflow engine) |
| **Reporting** | Basic (via widget screens) | `nop-report` (integrated reporting engine) |
| **Learning curve** | Steep (5+ proprietary DSLs: minilang, widget, form, screen, service-eca) | Moderate (2 DSLs: orm.xml, xbiz; AMIS is standard JSON) |

**Verdict**: OFBiz's `entity-auto` + view-entity approach is simpler to get started with (write entity → get CRUD screens), but the 5+ proprietary DSLs create a high barrier. Nop Platform's model-first codegen approach has a higher initial setup cost (must learn orm.xml + xdef schemas), but the delta mechanism provides cleaner upgrade paths than OFBiz's inline-override model. Nop's AMIS frontend is far more modern than OFBiz's Widget/FreeMarker.

---

## 9. Academic Lessons: What nop Can Borrow from OFBiz

### Proven OFBiz Patterns Worth Adopting

1. **Party unified identity** (`party-entitymodel.xml:42-200`): OFBiz's single `Party` + `PartyRole` pattern enables queries like "all transactions for entity X" with a single FK. nop-app-erp's split `ErpMdPartner`/`ErpMdEmployee`/`ErpMdOrganization` approach is cleaner for domain modeling but will eventually need a unified view layer for cross-entity identity queries. **Recommendation**: Consider a `ErpParty` view-entity or a lightweight unified identity column for audit/query purposes.

2. **OrderItemBilling 3-way linking** (`order-entitymodel.xml:719`): nop-app-erp's weak-pointer approach works but OFBiz's explicit linking entity is more performant and self-documenting for the high-traffic order→invoice→shipment link. **Recommendation**: For high-volume chains (sales delivery → invoice), consider a dedicated linking entity similar to `OrderItemBilling` rather than purely string-based weak pointers.

3. **Service ECA pattern** (`secas_invoice.xml:24-27`): OFBiz's declarative event handling is a clean way to trigger cross-module operations. nop-app-erp's `@OnChange` in xbiz achieves similar results but the ECA pattern is more declarative and runtime-configurable. **Recommendation**: Evaluate if xbiz event hooks are sufficient or if a formal ECA layer is needed for customer-configurable posting rules.

4. **GL mapping rule tables** (`acctg-entitymodel.xml:1462`): OFBiz's multiple GL mapping entities (`InvoiceItemTypeGlAccount`, `ProductCategoryGlAccount`, `VarianceReasonGlAccount`) provide fine-grained control. nop-app-erp's current design delegates GL mapping to the `IErpFinAcctDocProvider` SPI implementation, which doesn't prescribe a mapping table structure. **Recommendation**: Implement GL mapping tables similar to OFBiz's pattern as the default implementation of the Provider SPI—store mappings in the database rather than hardcoding.

5. **Date-ranged validity** (`fromDate`/`thruDate` on `PartyRelationship`, `ProductCategoryMember`, `AgreementTerm`): OFBiz uses this extensively for temporal data. nop-app-erp uses `businessDate` for transaction timestamping but lacks `fromDate/thruDate` for validity-ranged master data. **Recommendation**: Add `effectiveDate`/`expiryDate` to master data entities (prices, agreements) where temporal validity is needed.

### Where nop Already Surpasses

1. **Posting engine modularity**: nop's `IErpFinAcctDocProvider` SPI (registered via `@Inject Map<BusinessType, Provider>`) is strictly more modular than OFBiz's individual `createAcctgTransFor*` service pattern. No need to add new FK columns or new minilang services — just implement the Provider interface.

2. **Code generation pipeline**: nop's `orm.xml → codegen → dao/service/web` pipeline with incremental Maven regeneration is far more productive than OFBiz's manual entity definition + separate service/permission/widget definition cycle.

3. **Delta customization**: nop's `x:extends` + `_delta/` overlay pattern is cleaner than OFBiz's inline XML override. Delta overlays are isolated, reversible, and survive model regeneration.

4. **Module isolation via DAG**: nop's strict DAG dependency rules (no reverse references, no cycle) enable true domain autonomy. OFBiz's flat namespace creates implicit coupling that only becomes visible at transaction boundaries.

5. **Voucher model (Chinese accounting)**: nop's three-type voucher (收/付/转), single-side amount (`drAmount` vs `crAmount`), and continuous numbering per voucher type correctly models Chinese GAAP — OFBiz's AcctgTrans with `debitCreditFlag` is designed for Western accounting.

---

## 10. Gap Analysis

### OFBiz Features Not (Yet) in nop-app-erp

| Feature | OFBiz Evidence | Impact | Priority |
|---------|---------------|--------|----------|
| **Unified Party query** | Single `Party` table — query all transactions for party | Easy cross-entity identity search | Medium — future enhancement |
| **Shopping cart / e-commerce** | `order-entitymodel.xml` ShoppingCart entities | B2C sales missing | Low — not in scope |
| **Content management** | `applications/content/` component | Document attachment missing | Low — nop-file already exists |
| **Marketing campaigns** | `applications/marketing/` component | Lead scoring, campaigns | Low — CRM basic coverage |
| **Human Resources (full)** | `applications/humanres/` (positions, skills, qualifications, benefits) | nop only has basic employee entity | Medium — not yet depth |
| **Budget management** | `acctg-entitymodel.xml:48-121` Budget/BudgetItem/BudgetRevision/BudgetReview | Budget control, budget vs actual | Planned (see `erp-fin/budget-scenario-type` dict at `orm.xml:250`) |
| **Bank reconciliation** | `GlReconciliation` + `GlReconciliationEntry` at `acctg-entitymodel.xml:2377` | Bank statement matching | Deferred |
| **Service ECA declarative config** | `secas_*.xml` files — runtime-configurable event handlers | Configurable posting rules | Medium — evaluate post-MVP |
| **Email/notification templates** | OFBiz has built-in notification via workeffort + party contact | System-level notifications | nop-notify subsystem covers basic but templating less mature |
| **Multi-language i18n out of box** | `*EntityLabels.xml` per component | nop has i18n but fewer complete translations | Low (nop erp is Chinese-first) |

### nop-app-erp Features Not in OFBiz

| Feature | nop-app-erp Evidence | Advantage |
|---------|---------------------|-----------|
| **3-Layer posting engine** | `posting.md:11-42` (immutable saga + configurable timing) | Stronger consistency guarantees, better failure isolation |
| **Pluggable Provider SPI** | `posting.md:196-209` (IErpFinAcctDocProvider + Registry) | Zero core changes for new document types |
| **Multi-book native** | `acctSchemaId` dimension on voucher line (`domain-design-guidelines.md:503`) | True parallel accounting (statutory + tax + management) |
| **Chinese accounting model** | 收/付/转 voucher types (`orm.xml:36-39`), single-side amounts | GAAP-compliant for target market |
| **Bad-debt allowance lifecycle** | `bad-debt.md`: Allowance method (provisioning → write-off → recovery → release) | IAS 39 / IFRS 9 compliant |
| **Bad-debt aging buckets** | `bad-debt.md` — aging × historical loss rate → allowance | More sophisticated than OFBiz's ad-hoc adjustment |
| **FX revaluation workflow** | `domain-design-guidelines.md:388-405` | Formal period-end revaluation with FX gain/loss posting |
| **Red-ink reversal domain callback** | `posting.md:351-400` — `VoucherReversedEvent` → domain listener | Cleaner than OFBiz's `revertAcctgTransOnCancelInvoice` |
| **Model-first codegen** | `orm.xml` → all layers via `nop-cli gen` | Dramatically less boilerplate |
| **Delta customization** | x:extends + _delta/ directory | Cleaner customization without modifying generated code |
| **AMIS frontend** | JSON-based declarative UI, `view.xml` layer | Modern SPA, more extensible than FreeMarker widgets |
| **18-domain modular architecture** | `domain-module-split-analysis.md:46-67` | True microservice-ready domain isolation |

### Risk Assessment

| Risk | OFBiz | nop-app-erp | Mitigation |
|------|-------|-------------|------------|
| **Deployment complexity** | Monolith (single WAR) | 18 + 1 Maven modules | `app-erp-all` aggregates; quarantine mode if needed |
| **Production maturity** | 20+ years, hundreds of deployments | New project, no production deployment | Borrow OFBiz patterns for proven domains; incremental delivery |
| **Cross-module query performance** | Direct SQL JOIN via view-entities | I\*Biz cross-domain queries (no SQL JOIN) | EQL tuning; materialized views for high-traffic paths |
| **Multi-company complexity** | Per-org with simple flag | Per-org + multi-book + multi-currency | More correct but more complex; start simple |
| **Customization support** | EAV pattern (runtime flexible) | Dictionary-driven (schema change required) | Evaluate if EAV is needed for truly dynamic attributes |

---

## Summary

| Dimension | Winner | Rationale |
|-----------|--------|-----------|
| **Accounting engine modularity** | nop-app-erp | SPI-based Provider pattern > flat FK hub |
| **Accounting engine maturity** | OFBiz | Battle-tested 20+ years vs greenfield |
| **Entity modeling clarity** | nop-app-erp | Dictionary-driven compact models vs EAV triple blow-up |
| **Cross-module isolation** | nop-app-erp | DAG + I\*Biz > flat namespace |
| **Multi-book accounting** | nop-app-erp | Native `acctSchemaId` dimension |
| **Party model elegance** | OFBiz | Unified Party + Role is more powerful |
| **Manufacturing state machine** | nop-app-erp | Dedicated 10-state machine > generic WorkEffort |
| **Platform productivity** | nop-app-erp | Model-first codegen > manual DSL layers |
| **Production readiness** | OFBiz | Decades of deployments vs new project |
| **UI modernity** | nop-app-erp | AMIS > FreeMarker widgets |
| **Customization mechanism** | nop-app-erp | Delta overlays > inline XML override |
| **Runtime configurability** | OFBiz | EAV + Service ECA more runtime-flexible |

**Final assessment**: OFBiz is a battle-proven but architecturally dated monolith with a dense cross-module dependency graph and proprietary DSL complexity. nop-app-erp is architecturally superior in modularity (DAG isolation, SPI-based integration, multi-book native) and platform productivity (model-first codegen, delta customization, modern AMIS UI), but entirely unproven in production. The pragmatic path is to adopt OFBiz's proven patterns (Party unified identity, OrderItemBilling linking, GL mapping rule tables, Service ECA-like configurable event handling) while maintaining nop-app-erp's architectural advantages (SPI modularity, DAG constraints, codegen pipeline, Chinese accounting compliance).
