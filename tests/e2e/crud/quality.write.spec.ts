import { runCrudWriteCycle } from './_helper';

runCrudWriteCycle({
  entityName: 'ErpQaRiskRegister',
  route: '/ErpQaRiskRegister-main',
  fields: {
    riskDate: '2026-07-09',
    likelihood: '3',
    severity: '4',
    status: 'OPEN',
  },
  editField: { name: 'status', value: 'MITIGATED' },
});
