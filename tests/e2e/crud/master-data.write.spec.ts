import { runCrudWriteCycle } from './_helper';

runCrudWriteCycle({
  entityName: 'ErpMdPartner',
  route: '/ErpMdPartner-main',
  fields: { name: 'E2E Write Cycle Partner', partnerType: 'CUSTOMER', status: 'ACTIVE' },
  editField: { name: 'name', value: 'E2E Write Cycle UPDATED' },
});
