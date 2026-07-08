import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'b2b',
  entityRoute: 'ErpB2bAsn',
  addFormField: 'code',
});
