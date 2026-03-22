import { describe, it, expect } from 'vitest';
import * as str from '../../src/ops/string.js';

describe('String Operations', () => {
  it('string passthrough', () => expect(str.string('hello')).toBe('hello'));
  it('makeStrings', () => expect(str.makeStrings('a,b,c', ',')).toEqual(['a', 'b', 'c']));
  it('length', () => expect(str.length('hello')).toBe(5));
  it('wordCount', () => expect(str.wordCount('hello world foo')).toBe(3));
  it('wordCount empty', () => expect(str.wordCount('')).toBe(0));

  it('concatenate', () => {
    expect(str.concatenate('a', 'b', 'c', '', '', '', '', '-')).toBe('a-b-c');
  });

  it('formatNumber', () => {
    expect(str.formatNumber(3.14159, '%.2f')).toBe('3.14');
    expect(str.formatNumber(42, '%d')).toBe('42');
  });

  it('characters', () => expect(str.characters('abc')).toEqual(['a', 'b', 'c']));
  it('characterAt', () => expect(str.characterAt('hello', 1)).toBe('e'));
  it('characterAt out of range', () => expect(str.characterAt('hi', 5)).toBe(''));

  it('contains', () => expect(str.contains('hello world', 'world')).toBe(true));
  it('startsWith', () => expect(str.startsWith('hello', 'hel')).toBe(true));
  it('endsWith', () => expect(str.endsWith('hello', 'llo')).toBe(true));
  it('equals', () => expect(str.equals('abc', 'abc')).toBe(true));

  it('replace', () => expect(str.replace('hello world', 'world', 'earth')).toBe('hello earth'));
  it('subString', () => expect(str.subString('hello', 1, 3)).toBe('el'));
  it('trim', () => expect(str.trim('  hello  ')).toBe('hello'));

  it('changeCase uppercase', () => expect(str.changeCase('hello', 'uppercase')).toBe('HELLO'));
  it('changeCase lowercase', () => expect(str.changeCase('HELLO', 'lowercase')).toBe('hello'));
  it('changeCase titlecase', () => expect(str.changeCase('hello world', 'titlecase')).toBe('Hello World'));

  it('randomCharacter deterministic', () => {
    const a = str.randomCharacter('abc', 10, 42);
    const b = str.randomCharacter('abc', 10, 42);
    expect(a).toBe(b);
    expect(a.length).toBe(10);
  });

  it('asBinaryString', () => expect(str.asBinaryString('A')).toBe('01000001'));
  it('asBinaryList', () => expect(str.asBinaryList('AB')).toEqual([65, 66]));
  it('asNumberList', () => expect(str.asNumberList('1,2,3', ',')).toEqual([1, 2, 3]));
});
