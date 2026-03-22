import { describe, it, expect } from 'vitest';
import { encodeUrl, queryJson } from '../../src/ops/network.js';

describe('Network Operations', () => {
  it('encodes URL', () => {
    expect(encodeUrl('hello world')).toBe('hello%20world');
    expect(encodeUrl('a&b=c')).toBe('a%26b%3Dc');
  });

  it('queries JSON with dot notation', () => {
    const json = '{"results": [{"name": "Alice"}, {"name": "Bob"}]}';
    expect(queryJson(json, 'results.0.name')).toBe('Alice');
    expect(queryJson(json, 'results.1.name')).toBe('Bob');
  });

  it('returns null for invalid query', () => {
    expect(queryJson('{"a": 1}', 'b.c')).toBeNull();
  });

  it('handles invalid JSON', () => {
    expect(queryJson('not json', 'a')).toBeNull();
  });
});
