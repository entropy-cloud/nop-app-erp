import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'aps',
  entityRoute: 'ErpApsOperationOrder',
  addFormField: 'code',
});
