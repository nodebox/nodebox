// A port of nodebox.function.StringFunctions (namespace "string").

import { JavaScriptLibrary } from "../runtime/function-repository";
import { JavaRandom } from "./java-random";

function str(s: unknown): string {
  return s === null || s === undefined ? "" : String(s);
}

export function string(s: string): string {
  return s;
}

export function makeStrings(s: string, separator: string): string[] {
  if (s === null || s === undefined) return [];
  if (!separator) return Array.from(s);
  return s.split(separator);
}

export function length(s: string): number {
  return str(s).length;
}

export function wordCount(s: string): number {
  const m = str(s).match(/\w+/g);
  return m ? m.length : 0;
}

export function concatenate(...parts: unknown[]): string {
  return parts.map(str).join("");
}

export function toTitleCase(value: string): string {
  let result = "";
  let nextTitle = true;
  for (const c of value) {
    if (/\s/.test(c)) {
      nextTitle = true;
      result += c;
    } else if (nextTitle) {
      result += c.toUpperCase();
      nextTitle = false;
    } else {
      result += c;
    }
  }
  return result;
}

export function changeCase(value: string, caseMethod: string): string {
  value = str(value);
  switch (str(caseMethod).toLowerCase()) {
    case "lowercase":
      return value.toLowerCase();
    case "uppercase":
      return value.toUpperCase();
    case "titlecase":
      return toTitleCase(value);
    default:
      return value;
  }
}

/** The subset of java.util.Formatter the format_number node needs: %d, %f, %e, %s, %x with flags, width and precision. */
export function javaFormat(format: string, ...args: unknown[]): string {
  let argIndex = 0;
  return format.replace(/%(\d+\$)?([-#+ 0,(]*)(\d+)?(?:\.(\d+))?([a-zA-Z%])/g, (match, index, flags: string, width, precision, conv: string) => {
    if (conv === "%") return "%";
    if (conv === "n") return "\n";
    const arg = index ? args[parseInt(index, 10) - 1] : args[argIndex++];
    let s: string;
    const value = Number(arg);
    switch (conv) {
      case "d":
        s = String(Math.trunc(value));
        if (flags.includes(",")) s = groupThousands(s);
        break;
      case "f":
        s = value.toFixed(precision !== undefined ? parseInt(precision, 10) : 6);
        if (flags.includes(",")) {
          const [i, frac] = s.split(".");
          s = groupThousands(i) + (frac !== undefined ? `.${frac}` : "");
        }
        break;
      case "e":
      case "E": {
        s = value.toExponential(precision !== undefined ? parseInt(precision, 10) : 6);
        s = s.replace(/e([+-])(\d)$/, "e$10$2");
        if (conv === "E") s = s.toUpperCase();
        break;
      }
      case "g":
      case "G":
        s = value.toPrecision(precision !== undefined ? Math.max(1, parseInt(precision, 10)) : 6);
        break;
      case "x":
        s = Math.trunc(value).toString(16);
        break;
      case "X":
        s = Math.trunc(value).toString(16).toUpperCase();
        break;
      case "o":
        s = Math.trunc(value).toString(8);
        break;
      case "s":
      case "S":
        s = str(arg);
        if (conv === "S") s = s.toUpperCase();
        break;
      case "b":
        s = String(Boolean(arg));
        break;
      case "c":
        s = typeof arg === "number" ? String.fromCodePoint(arg) : str(arg).charAt(0);
        break;
      default:
        return match;
    }
    if ("dfeEgG".includes(conv) && value >= 0) {
      if (flags.includes("+")) s = `+${s}`;
      else if (flags.includes(" ")) s = ` ${s}`;
    }
    if (width !== undefined) {
      const w = parseInt(width, 10);
      if (s.length < w) {
        if (flags.includes("-")) s = s.padEnd(w);
        else if (flags.includes("0") && "dfeEgGxXo".includes(conv)) {
          const sign = s.startsWith("-") || s.startsWith("+") ? s.charAt(0) : "";
          s = sign + s.slice(sign.length).padStart(w - sign.length, "0");
        } else s = s.padStart(w);
      }
    }
    return s;
  });
}

function groupThousands(s: string): string {
  const negative = s.startsWith("-");
  const digits = negative ? s.slice(1) : s;
  const grouped = digits.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return negative ? `-${grouped}` : grouped;
}

export function formatNumber(value: number, format: string): string {
  return javaFormat(format, value);
}

export function characters(s: string): string[] {
  return s === null || s === undefined ? [] : Array.from(s);
}

export function randomCharacter(characterSet: string, amount: number, seed: number): string[] {
  const result: string[] = [];
  const r = new JavaRandom(BigInt.asIntN(64, BigInt(Math.trunc(seed)) * 1000000000n));
  const set = str(characterSet);
  if (set.length === 0) return result;
  for (let i = 0; i < amount; i++) {
    const index = Math.trunc(r.nextDouble() * set.length);
    result.push(set.charAt(index));
  }
  return result;
}

function utf8Bytes(s: string): number[] {
  return Array.from(new TextEncoder().encode(s)).map((b) => (b > 127 ? b - 256 : b));
}

export function asBinaryString(s: string, digitSep: string, byteSep: string): string {
  if (s === null || s === undefined) return s;
  let result = "";
  for (let val of utf8Bytes(s)) {
    for (let i = 0; i < 8; i++) {
      result += (val & 128) === 0 ? "0" : "1";
      if (i < 7) result += str(digitSep);
      val <<= 1;
    }
    result += str(byteSep);
  }
  return result;
}

export function asBinaryList(s: string): string[] {
  const result: string[] = [];
  if (s === null || s === undefined) return result;
  for (let val of utf8Bytes(s)) {
    for (let i = 0; i < 8; i++) {
      result.push((val & 128) === 0 ? "0" : "1");
      val <<= 1;
    }
  }
  return result;
}

export function asNumberList(s: string, radix: number, padding: boolean): string[] {
  const result: string[] = [];
  radix = Math.trunc(radix);
  if (radix < 2 || s === null || s === undefined) return result;
  const bytes = utf8Bytes(s);
  const binary = (b: number) => {
    let val = b;
    let out = "";
    for (let i = 0; i < 8; i++) {
      out += (val & 128) === 0 ? "0" : "1";
      val <<= 1;
    }
    return out;
  };
  if (padding) {
    if (radix === 2) for (const b of bytes) result.push(binary(b));
    else if (radix === 3) for (const b of bytes) result.push(padJava(parseInt(b.toString(radix), 10), 6));
    else if (radix > 3 && radix < 7) for (const b of bytes) result.push(padJava(parseInt(b.toString(radix), 10), 4));
    else if (radix < 15) for (const b of bytes) result.push(b.toString(radix).padStart(3, "0"));
    else for (const b of bytes) result.push(b.toString(radix).padStart(2, "0"));
  } else {
    if (radix === 2) for (const b of bytes) result.push(binary(b));
    else for (const b of bytes) result.push(b.toString(radix));
  }
  return result;
}

function padJava(v: number, width: number): string {
  // String.format("%06d", v): the minus sign counts against the width.
  const s = String(Math.abs(v)).padStart(v < 0 ? width - 1 : width, "0");
  return v < 0 ? `-${s}` : s;
}

export function characterAt(s: string, index: number): string {
  if (s === null || s === undefined || s === "") return s;
  index = Math.trunc(index);
  if (index < 0) index = s.length + index;
  if (index >= s.length || index < 0) return "";
  return s.charAt(index);
}

export function countCharacters(s: string): string {
  return s;
}

export function contains(s: string, value: string): boolean {
  if (s === null || s === undefined || value === null || value === undefined) return false;
  return s.includes(value);
}

export function endsWith(s: string, value: string): boolean {
  if (s === null || s === undefined || value === null || value === undefined) return false;
  return s.endsWith(value);
}

export function equal(s: string, value: string, caseSensitive: boolean): boolean {
  if (s === null || s === undefined || value === null || value === undefined) return false;
  return caseSensitive ? s === value : s.toLowerCase() === value.toLowerCase();
}

export function replace(s: string, oldVal: string, newVal: string): string {
  if (oldVal === null || oldVal === undefined || newVal === null || newVal === undefined) return s;
  if (oldVal === "") return s;
  return str(s).split(oldVal).join(newVal);
}

export function startsWith(s: string, value: string): boolean {
  if (s === null || s === undefined || value === null || value === undefined) return false;
  return s.startsWith(value);
}

export function subString(s: string, start: number, end: number, endOffset: boolean): string {
  if (s === null || s === undefined) return s;
  start = Math.trunc(start);
  end = Math.trunc(end);
  if (end < start) return "";
  if (start < 0 && end < 0) {
    start = s.length + start;
    end = s.length + end;
  }
  if (endOffset) end++;
  return s.substring(Math.max(0, start), Math.min(s.length, end));
}

export function trim(s: string): string {
  return s === null || s === undefined ? s : s.trim();
}

export const stringLibrary = new JavaScriptLibrary("string", {
  string,
  makeStrings,
  length,
  wordCount,
  concatenate,
  changeCase,
  formatNumber,
  characters,
  randomCharacter,
  asBinaryString,
  asBinaryList,
  asNumberList,
  countCharacters,
  characterAt,
  contains,
  endsWith,
  equal,
  replace,
  startsWith,
  subString,
  trim,
});
