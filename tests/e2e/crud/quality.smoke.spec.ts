import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'quality',
  entityRoute: 'ErpQaInspection',
  addFormField: 'code',
});
