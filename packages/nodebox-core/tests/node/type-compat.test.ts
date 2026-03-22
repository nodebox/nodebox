import { describe, it, expect } from 'vitest';
import { isCompatible } from '../../src/node/type-compat.js';

describe('Type Compatibility', () => {
  it('same type is compatible', () => {
    expect(isCompatible('int', 'int')).toBe(true);
    expect(isCompatible('float', 'float')).toBe(true);
    expect(isCompatible('geometry', 'geometry')).toBe(true);
  });

  it('anything connects to string', () => {
    expect(isCompatible('int', 'string')).toBe(true);
    expect(isCompatible('float', 'string')).toBe(true);
    expect(isCompatible('boolean', 'string')).toBe(true);
    expect(isCompatible('point', 'string')).toBe(true);
    expect(isCompatible('color', 'string')).toBe(true);
    expect(isCompatible('geometry', 'string')).toBe(true);
  });

  it('int <-> float', () => {
    expect(isCompatible('int', 'float')).toBe(true);
    expect(isCompatible('float', 'int')).toBe(true);
  });

  it('int/float -> point', () => {
    expect(isCompatible('int', 'point')).toBe(true);
    expect(isCompatible('float', 'point')).toBe(true);
  });

  it('list accepts anything', () => {
    expect(isCompatible('int', 'list')).toBe(true);
    expect(isCompatible('geometry', 'list')).toBe(true);
  });

  it('point -> geometry', () => {
    expect(isCompatible('point', 'geometry')).toBe(true);
  });

  it('incompatible types', () => {
    expect(isCompatible('boolean', 'int')).toBe(false);
    expect(isCompatible('geometry', 'int')).toBe(false);
    expect(isCompatible('color', 'point')).toBe(false);
    expect(isCompatible('string', 'geometry')).toBe(false);
  });
});
