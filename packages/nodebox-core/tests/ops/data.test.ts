import { describe, it, expect } from 'vitest';
import { importCsv, importText, lookup, filterData, makeTable } from '../../src/ops/data.js';
import type { Value } from '../../src/node/value.js';

describe('Data Operations', () => {
  describe('importCsv', () => {
    it('parses simple CSV', () => {
      const csv = 'name,age\nAlice,30\nBob,25';
      const result = importCsv(csv, ',', '"', '.');
      expect(result.length).toBe(2);
      expect((result[0] as any).value.name).toBe('Alice');
      expect((result[0] as any).value.age).toBe(30);
      expect((result[1] as any).value.name).toBe('Bob');
    });

    it('handles quoted fields', () => {
      const csv = 'name,desc\nAlice,"hello, world"\nBob,"say ""hi"""';
      const result = importCsv(csv, ',', '"', '.');
      expect((result[0] as any).value.desc).toBe('hello, world');
      expect((result[1] as any).value.desc).toBe('say "hi"');
    });

    it('handles tab delimiter', () => {
      const csv = 'a\tb\n1\t2';
      const result = importCsv(csv, '\t', '"', '.');
      expect((result[0] as any).value.a).toBe(1);
    });

    it('returns empty for empty input', () => {
      expect(importCsv('', ',', '"', '.')).toEqual([]);
    });
  });

  describe('importText', () => {
    it('splits text into lines', () => {
      expect(importText('hello\nworld')).toEqual(['hello', 'world']);
    });
    it('returns empty for empty input', () => {
      expect(importText('')).toEqual([]);
    });
  });

  describe('lookup', () => {
    it('extracts values by key', () => {
      const data: Value[] = [
        { type: 'data', value: { name: 'Alice', age: 30 } },
        { type: 'data', value: { name: 'Bob', age: 25 } },
      ];
      const result = lookup(data, 'name');
      expect(result).toEqual([
        { type: 'string', value: 'Alice' },
        { type: 'string', value: 'Bob' },
      ]);
    });

    it('extracts numeric values', () => {
      const data: Value[] = [
        { type: 'data', value: { x: 10 } },
      ];
      expect(lookup(data, 'x')).toEqual([{ type: 'float', value: 10 }]);
    });
  });

  describe('filterData', () => {
    const data: Value[] = [
      { type: 'data', value: { name: 'Alice', age: 30 } },
      { type: 'data', value: { name: 'Bob', age: 25 } },
      { type: 'data', value: { name: 'Charlie', age: 35 } },
    ];

    it('filters equal', () => {
      const result = filterData(data, 'name', 'equal', 'Alice');
      expect(result.length).toBe(1);
    });

    it('filters greater than', () => {
      const result = filterData(data, 'age', 'greater-than', '28');
      expect(result.length).toBe(2);
    });

    it('filters contains', () => {
      const result = filterData(data, 'name', 'contains', 'li');
      expect(result.length).toBe(2); // Alice, Charlie
    });
  });

  describe('makeTable', () => {
    it('creates table from columns', () => {
      const headers = ['x', 'y'];
      const col1: Value[] = [{ type: 'float', value: 1 }, { type: 'float', value: 2 }];
      const col2: Value[] = [{ type: 'float', value: 10 }, { type: 'float', value: 20 }];
      const result = makeTable(headers, col1, col2, [], [], [], []);
      expect(result.length).toBe(2);
      expect((result[0] as any).value.x).toBe(1);
      expect((result[0] as any).value.y).toBe(10);
    });
  });
});
