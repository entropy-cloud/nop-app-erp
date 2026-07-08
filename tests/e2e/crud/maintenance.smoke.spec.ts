import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'maintenance',
  entityRoute: 'ErpMntVisit',
  addFormField: 'code',
});
