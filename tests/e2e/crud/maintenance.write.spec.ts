import { runCrudWriteCycle } from './_helper';

runCrudWriteCycle({
  entityName: 'ErpMntEquipmentCategory',
  route: '/ErpMntEquipmentCategory-main',
  fields: { name: 'E2E Write Cycle Equipment Category' },
  editField: { name: 'name', value: 'E2E Write Cycle Category UPDATED' },
});
