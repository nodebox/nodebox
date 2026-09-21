// A port of nodebox.function.DataFunctions (namespace "data"). File access goes through a host
// supplied reader so the same code runs on the desktop (fs), in the browser (fetch) and in tests.

import { JavaScriptLibrary } from "../runtime/function-repository";
import { lookup } from "../runtime/lookup";
import { toArray } from "../runtime/values";

export type TextFileReader = (fileName: string) => string | Promise<string>;

let textFileReader: TextFileReader | null = null;

/** Install how import_text and import_csv read files (a path or URL, already resolved). */
export function setTextFileReader(reader: TextFileReader | null): void {
  textFileReader = reader;
}

export function getTextFileReader(): TextFileReader | null {
  return textFileReader;
}

async function readText(fileName: string): Promise<string> {
  if (!textFileReader) throw new Error(`Could not read file ${fileName}: no file reader is installed.`);
  try {
    return await textFileReader(fileName);
  } catch (e) {
    throw new Error(`Could not read file ${fileName}: ${e instanceof Error ? e.message : String(e)}`);
  }
}

export { lookup };

export async function importText(fileName: string): Promise<string[]> {
  if (!fileName || fileName.trim() === "") return [];
  const text = await readText(fileName);
  const lines = text.split(/\r\n|\r|\n/);
  // A trailing newline does not produce an extra empty line, like BufferedReader.readLine.
  if (lines.length > 0 && lines[lines.length - 1] === "" && /[\r\n]$/.test(text)) lines.pop();
  return lines;
}

const SEPARATORS: Record<string, string> = {
  comma: ",",
  semicolon: ";",
  colon: ":",
  tab: "\t",
  space: " ",
  double: '"',
  single: "'",
};

/** Parse CSV text into rows of fields, honoring quotes and escaped quotes. */
export function parseCsv(text: string, separator = ",", quote = '"'): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let field = "";
  let inQuotes = false;
  let i = 0;
  const n = text.length;
  while (i < n) {
    const c = text[i];
    if (inQuotes) {
      if (c === quote) {
        if (text[i + 1] === quote) {
          field += quote;
          i += 2;
          continue;
        }
        inQuotes = false;
        i++;
        continue;
      }
      field += c;
      i++;
      continue;
    }
    if (c === quote) {
      inQuotes = true;
      i++;
    } else if (c === separator) {
      row.push(field);
      field = "";
      i++;
    } else if (c === "\r" || c === "\n") {
      row.push(field);
      field = "";
      rows.push(row);
      row = [];
      if (c === "\r" && text[i + 1] === "\n") i++;
      i++;
    } else {
      field += c;
      i++;
    }
  }
  if (field !== "" || row.length > 0) {
    row.push(field);
    rows.push(row);
  }
  return rows;
}

function parseLocaleNumber(v: string, decimalComma: boolean): number | undefined {
  let s = v.trim();
  if (s === "") return undefined;
  if (decimalComma) s = s.replace(/\./g, "").replace(",", ".");
  else s = s.replace(/,/g, "");
  if (!/^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?$/.test(s)) return undefined;
  const d = Number(s);
  return Number.isFinite(d) ? d : undefined;
}

export async function importCSV(
  fileName: string,
  delimiter: string,
  quotationCharacter: string,
  numberSeparator: string,
): Promise<Record<string, unknown>[]> {
  if (!fileName || fileName.trim() === "") return [];
  const text = await readText(fileName);
  return parseCsvTable(text, delimiter, quotationCharacter, numberSeparator);
}

/** Turn CSV text into rows keyed by header, with numeric columns converted, like NodeBox 3's import_csv. */
export function parseCsvTable(text: string, delimiter = "comma", quotationCharacter = "double", numberSeparator = "period"): Record<string, unknown>[] {
  const sep = SEPARATORS[delimiter] ?? ",";
  const quot = SEPARATORS[quotationCharacter] ?? '"';
  const rows = parseCsv(text, sep, quot);
  if (rows.length === 0) return [];
  const headers = rows[0].map((h, i) => {
    const trimmed = h.trim();
    return trimmed === "" ? `Column ${i + 1}` : trimmed;
  });
  const duplicates = new Map<string, number>();
  const seen: string[] = [];
  for (const h of headers) {
    if (seen.includes(h)) duplicates.set(h, 0);
    seen.push(h);
  }
  for (let i = 0; i < headers.length; i++) {
    const h = headers[i];
    if (duplicates.has(h)) {
      const number = duplicates.get(h)! + 1;
      headers[i] = `${h} ${number}`;
      duplicates.set(h, number);
    }
  }
  const decimalComma = numberSeparator === "comma";
  const numeric = new Map<string, boolean | null>();
  for (const h of headers) numeric.set(h, null);
  const rawRows: Record<string, string>[] = [];
  for (const row of rows.slice(1)) {
    if (row.length === 1 && row[0] === "") continue;
    const record: Record<string, string> = {};
    for (let i = 0; i < row.length; i++) {
      const header = i < headers.length ? headers[i] : `Column ${i + 1}`;
      if (!numeric.has(header)) numeric.set(header, null);
      const v = row[i].trim();
      if (parseLocaleNumber(v, decimalComma) === undefined) numeric.set(header, false);
      else if (numeric.get(header) === null) numeric.set(header, true);
      record[header] = v;
    }
    rawRows.push(record);
  }
  const numericColumns = Array.from(numeric.entries())
    .filter(([, v]) => v === true)
    .map(([k]) => k);
  if (numericColumns.length === 0) return rawRows;
  return rawRows.map((r) => {
    const out: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(r)) {
      if (numericColumns.includes(k)) {
        const d = parseLocaleNumber(v, decimalComma);
        if (d !== undefined) out[k] = d;
      } else out[k] = v;
    }
    return out;
  });
}

export function filterData(rows: unknown, key: string, op: string, value: unknown): unknown[] {
  const list = toArray(rows);
  if (value === null || value === undefined) return list;
  const floatValue = typeof value === "number" ? value : Number(String(value));
  if (!Number.isNaN(floatValue) && String(value).trim() !== "") {
    return list.filter((o) => doubleMatches(o, key, op, floatValue));
  }
  return list.filter((o) => objectMatches(o, key, op, value));
}

function objectMatches(o: unknown, key: string, op: string, value: unknown): boolean {
  const v = lookup(o, key);
  if (op === "=") return String(value) === String(v);
  if (op === "!=") return String(value) !== String(v);
  return false;
}

function doubleMatches(o: unknown, key: string, op: string, value: number): boolean {
  const v = lookup(o, key);
  if (v === null || v === undefined) return false;
  let dv: number;
  if (typeof v === "number") dv = v;
  else if (typeof v === "string") {
    dv = Number(v);
    if (Number.isNaN(dv) || v.trim() === "") dv = Number.MAX_VALUE;
  } else return false;
  switch (op) {
    case "=":
      return dv === value;
    case "!=":
      return dv !== value;
    case ">":
      return dv > value;
    case ">=":
      return dv >= value;
    case "<":
      return dv < value;
    case "<=":
      return dv <= value;
    default:
      return false;
  }
}

export function makeTable(headers: string, ...lists: unknown[]): Record<string, unknown>[] {
  const dirty = String(headers ?? "").split(/[,;]/);
  const headerList: string[] = [];
  const columns = Math.max(6, lists.length);
  for (let i = 0; i < columns; i++) {
    let key = i < dirty.length ? dirty[i].trim() : "";
    if (key === "") key = `list${i + 1}`;
    headerList.push(key);
  }
  const arrays = lists.map((l) => (l === null || l === undefined ? [] : toArray(l)));
  let colCount = 0;
  arrays.forEach((l, i) => {
    if (l.length > 0) colCount = i + 1;
  });
  const rowCount = Math.max(0, ...arrays.map((l) => l.length));
  const result: Record<string, unknown>[] = [];
  for (let rowIndex = 0; rowIndex < rowCount; rowIndex++) {
    const row: Record<string, unknown> = {};
    for (let colIndex = 0; colIndex < colCount; colIndex++) {
      const l = arrays[colIndex];
      if (l.length > 0) row[headerList[colIndex]] = rowIndex < l.length ? l[rowIndex] : "";
    }
    result.push(row);
  }
  return result;
}

export const dataLibrary = new JavaScriptLibrary("data", { lookup, importText, importCSV, filterData, makeTable }, {
  impure: ["importText", "importCSV"],
});
