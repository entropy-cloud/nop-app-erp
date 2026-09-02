# Flux Validation Warnings Analysis

## Overview

After fixing `visibleOn→visible` mapping, there are 13866 warnings across 855 pages. This document categorizes each warning type and recommends the proper approach.

## Warning Categories

### Category 1: Grid Column Properties (2206 warnings)

These properties are set by `GenGridCol` in `flux-web.xlib` and are valid Flux `TableColumnSchema` properties, but are **missing from the `columns` propContract `fieldRules`**.

| Property | Count | Flux Schema | Recommendation |
|----------|-------|-------------|----------------|
| `toggled` | 957 | `TableColumnSchema.toggled` | Add to propContracts |
| `sortable` | 957 | `TableColumnSchema.sortable` | Add to propContracts |
| `align` | 184 | `TableColumnSchema.align` | Add to propContracts |
| `fixed` | 88 | `TableColumnSchema.fixed` | Add to propContracts |

**Action**: Add these to `data-renderer-definitions.ts` and `crud-renderer-definition.ts` column `fieldRules`.

### Category 2: Action Properties (1120 warnings)

These properties are set by `NormalizeAction` or are AMIS action properties that need conversion.

| Property | Count | Flux Equivalent | Recommendation |
|----------|-------|-----------------|----------------|
| `batch` | 425 | N/A | Add to delete list (deprecated AMIS concept) |
| `source` | 387 | `loadAction` | Convert `source` → `loadAction` in conversion layer |
| `actionType` | 104 | `action` | Already partially handled; remaining need conversion |
| `target` | 47 | `targetId` | Convert `target` → `targetId` in conversion layer |
| `onEvent` | 102 | `on*` handlers | Add to delete list (Flux uses reaction system) |
| `api` | 56 | `action: 'ajax'` | Add to delete list (API calls use action system) |

**Action**: Update `NormalizeAction` in `flux-web.xlib` to convert or delete these properties.

### Category 3: Picker Properties (2110 warnings)

| Property | Count | Flux Equivalent | Recommendation |
|----------|-------|-----------------|----------------|
| `size` | 375 | `pickerDialog.size` | Convert `size` → `pickerDialog.size` |
| `pickerSchema` | 368 | N/A | Add to delete list (AMIS-specific) |
| `modalSize` | 368 | `pickerDialog.size` | Convert `modalSize` → `pickerDialog.size` |
| `hiddenFieldPolicy` | 999 | Form-level | Already in form propContracts; not needed on picker |

**Action**: Update picker conversion in `flux-web.xlib` to convert or delete these properties.

### Category 4: Field Properties (873 warnings)

| Property | Count | Flux Equivalent | Recommendation |
|----------|-------|-----------------|----------------|
| `validations` | 343 | `rules` | Already partially handled; remaining need conversion |
| `validationErrors` | 138 | N/A | Add to delete list (runtime errors, not schema) |
| `submitOnChange` | 204 | Form-level | Already in form propContracts; not needed on field |
| `readOnly` | 47 | `readOnly` | Already in `formFieldContracts`; conversion needed |

**Action**: Update field conversion in `flux-web.xlib` to convert or delete these properties.

### Category 5: Input Properties (211 warnings)

| Property | Count | Flux Equivalent | Recommendation |
|----------|-------|-----------------|----------------|
| `step` | 165 | N/A | Add to delete list (AMIS-specific) |
| `placeholder` | 46 | `placeholder` | Already in some renderers; add to conversion |

**Action**: Add `placeholder` support where missing; delete `step`.

### Category 6: VisibleOn in Nested Pages (329 warnings)

| Property | Count | Flux Equivalent | Recommendation |
|----------|-------|-----------------|----------------|
| `visibleOn` | 329 | `visible` | Conversion exists but not applied to nested pages |

**Action**: Ensure conversion is applied to all page content, including dialogs/drawers.

## Implementation Priority

### Phase 1: Grid Column Properties (2206 warnings)
1. Add `toggled`, `sortable`, `align`, `fixed` to column `fieldRules` in `data-renderer-definitions.ts`
2. Add same to `crud-renderer-definition.ts`
3. Rebuild `flux-renderers-data` and run validation

### Phase 2: Action Properties (1120 warnings)
1. Add `batch`, `onEvent`, `api` to delete list in `NormalizeAction`
2. Add `target` → `targetId` conversion in `NormalizeAction`
3. Add `source` → `loadAction` conversion for picker actions
4. Rebuild `nop-web` and run validation

### Phase 3: Picker Properties (2110 warnings)
1. Add `size`/`modalSize` → `pickerDialog.size` conversion
2. Add `pickerSchema` to delete list
3. Add `hiddenFieldPolicy` to delete list for picker
4. Rebuild `nop-web` and run validation

### Phase 4: Field Properties (873 warnings)
1. Complete `validations` → `rules` conversion
2. Add `validationErrors`, `submitOnChange` to delete list
3. Add `readOnly` conversion
4. Rebuild `nop-web` and run validation

### Phase 5: Input Properties (211 warnings)
1. Add `placeholder` support where missing
2. Add `step` to delete list
3. Rebuild `nop-web` and run validation

### Phase 6: Nested Page Conversion (329 warnings)
1. Ensure `visibleOn` → `visible` conversion is applied to all page content
2. This may require changes to `LoadPage` or `GenPage` functions
3. Rebuild `nop-web` and run validation

## Expected Result

If all phases complete successfully, warnings should reduce from 13866 to approximately 0.

## Questions for Review

1. **Grid Column Properties**: Should we add these to propContracts, or is there a reason they're intentionally excluded?
2. **Action Properties**: Should `batch` be converted to something, or is it truly deprecated?
3. **Picker Properties**: Is `pickerSchema` used anywhere, or can it be safely deleted?
4. **Nested Page Conversion**: Is there a better approach than modifying `LoadPage`/`GenPage`?
