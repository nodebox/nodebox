import type { Value } from '../node/value.js';

export function importCsv(
  content: string, delimiter: string, quoteChar: string, _numberSeparator: string,
): Value[] {
  if (!content) return [];
  const lines = parseCSV(content, delimiter, quoteChar);
  if (lines.length === 0) return [];

  const headers = lines[0];
  const result: Value[] = [];
  for (let i = 1; i < lines.length; i++) {
    const row: Record<string, unknown> = {};
    for (let j = 0; j < headers.length; j++) {
      const val = j < lines[i].length ? lines[i][j] : '';
      const num = parseFloat(val);
      row[headers[j]] = isNaN(num) ? val : num;
    }
    result.push({ type: 'data', value: row });
  }
  return result;
}

function parseCSV(text: string, delimiter: string, quoteChar: string): string[][] {
  const rows: string[][] = [];
  const lines = text.split('\n');
  for (const line of lines) {
    if (line.trim() === '') continue;
    const fields: string[] = [];
    let current = '';
    let inQuote = false;
    for (let i = 0; i < line.length; i++) {
      const ch = line[i];
      if (inQuote) {
        if (ch === quoteChar) {
          if (i + 1 < line.length && line[i + 1] === quoteChar) {
            current += quoteChar;
            i++;
          } else {
            inQuote = false;
          }
        } else {
          current += ch;
        }
      } else if (ch === quoteChar) {
        inQuote = true;
      } else if (ch === delimiter) {
        fields.push(current);
        current = '';
      } else {
        current += ch;
      }
    }
    fields.push(current);
    rows.push(fields);
  }
  return rows;
}

export function importText(content: string): string[] {
  if (!content) return [];
  return content.split('\n');
}

export function lookup(list: Value[], key: string): Value[] {
  const result: Value[] = [];
  for (const item of list) {
    if (item.type === 'data') {
      const val = (item.value as Record<string, unknown>)[key];
      if (val !== undefined) {
        if (typeof val === 'number') result.push({ type: 'float', value: val });
        else if (typeof val === 'string') result.push({ type: 'string', value: val });
        else if (typeof val === 'boolean') result.push({ type: 'boolean', value: val });
        else result.push({ type: 'string', value: String(val) });
      }
    }
  }
  return result;
}

export function filterData(data: Value[], key: string, op: string, value: string): Value[] {
  return data.filter(item => {
    if (item.type !== 'data') return false;
    const v = (item.value as Record<string, unknown>)[key];
    const sv = String(v);
    if (op === 'equal' || op === '==') return sv === value;
    if (op === 'not-equal' || op === '!=') return sv !== value;
    if (op === 'contains') return sv.includes(value);
    if (op === 'starts-with') return sv.startsWith(value);
    if (op === 'greater-than' || op === '>') return Number(v) > Number(value);
    if (op === 'less-than' || op === '<') return Number(v) < Number(value);
    return true;
  });
}

export function makeTable(
  headers: string[],
  list1: Value[], list2: Value[], list3: Value[],
  list4: Value[], list5: Value[], list6: Value[],
): Value[] {
  const lists = [list1, list2, list3, list4, list5, list6].filter(l => l.length > 0);
  const maxLen = Math.max(...lists.map(l => l.length), 0);
  const result: Value[] = [];
  for (let i = 0; i < maxLen; i++) {
    const row: Record<string, unknown> = {};
    for (let j = 0; j < headers.length && j < lists.length; j++) {
      const val = i < lists[j].length ? lists[j][i] : null;
      row[headers[j]] = val ? (val as any).value : null;
    }
    result.push({ type: 'data', value: row });
  }
  return result;
}
