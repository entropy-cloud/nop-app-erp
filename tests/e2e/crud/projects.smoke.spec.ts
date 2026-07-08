import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'projects',
  entityRoute: 'ErpPrjProject',
  addFormField: 'code',
});
