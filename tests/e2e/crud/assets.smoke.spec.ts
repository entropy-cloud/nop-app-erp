import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'assets',
  entityRoute: 'ErpAstAsset',
  addFormField: 'code',
});
