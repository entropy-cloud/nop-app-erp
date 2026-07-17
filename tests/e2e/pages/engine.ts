import { AmisAdapter } from './AmisAdapter';
import { FluxAdapter } from './FluxAdapter';
import type { EngineAdapter } from './types';

export type EngineType = 'amis' | 'flux';

let cached: EngineAdapter | null = null;

export function getEngineType(): EngineType {
  const v = (process.env.E2E_ENGINE || 'amis').toLowerCase();
  return v === 'flux' ? 'flux' : 'amis';
}

export function createEngine(type?: EngineType): EngineAdapter {
  const t = type || getEngineType();
  switch (t) {
    case 'flux':
      return new FluxAdapter();
    case 'amis':
    default:
      return new AmisAdapter();
  }
}

export function getEngine(): EngineAdapter {
  if (!cached) {
    cached = createEngine();
  }
  return cached;
}
