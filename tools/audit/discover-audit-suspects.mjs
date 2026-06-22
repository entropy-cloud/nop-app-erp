import { handleFatalError, runScanner } from './shared.mjs';
import { genericAuditRules } from './template-rules.mjs';

runScanner({ label: 'discover-audit-suspects', rules: genericAuditRules }).catch((error) => {
  handleFatalError('discover-audit-suspects', error);
});
