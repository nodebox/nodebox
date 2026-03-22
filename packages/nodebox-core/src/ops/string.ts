// ─── String Operations ─────────────────────────────────
export function string(v: string): string { return v; }

export function makeStrings(str: string, separator: string): string[] {
  if (!str) return [];
  return str.split(separator);
}

export function length(str: string): number { return str.length; }

export function wordCount(str: string): number {
  return str.trim().split(/\s+/).filter(w => w.length > 0).length;
}

export function concatenate(
  s1: string, s2: string, s3: string, s4: string,
  s5: string, s6: string, s7: string, separator: string,
): string {
  return [s1, s2, s3, s4, s5, s6, s7].filter(s => s !== '').join(separator);
}

export function formatNumber(value: number, format: string): string {
  // Simple format: support %d, %f, %.Nf
  if (!format) return String(value);
  try {
    if (format.includes('%d')) return format.replace('%d', String(Math.round(value)));
    const match = format.match(/%\.(\d+)f/);
    if (match) {
      const decimals = parseInt(match[1], 10);
      return format.replace(match[0], value.toFixed(decimals));
    }
    if (format.includes('%f')) return format.replace('%f', String(value));
    return format;
  } catch {
    return String(value);
  }
}

export function characters(str: string): string[] {
  return [...str];
}

export function characterAt(str: string, index: number): string {
  const i = Math.round(index);
  if (i < 0 || i >= str.length) return '';
  return str[i];
}

export function contains(str: string, sub: string): boolean {
  return str.includes(sub);
}

export function startsWith(str: string, prefix: string): boolean {
  return str.startsWith(prefix);
}

export function endsWith(str: string, suffix: string): boolean {
  return str.endsWith(suffix);
}

export function equals(str1: string, str2: string): boolean {
  return str1 === str2;
}

export function replace(str: string, old: string, replacement: string): string {
  return str.split(old).join(replacement);
}

export function subString(str: string, start: number, end: number): string {
  return str.substring(Math.round(start), Math.round(end));
}

export function trim(str: string): string {
  return str.trim();
}

export function changeCase(str: string, method: string): string {
  if (method === 'uppercase') return str.toUpperCase();
  if (method === 'lowercase') return str.toLowerCase();
  if (method === 'titlecase') {
    return str.replace(/\b\w/g, c => c.toUpperCase());
  }
  return str;
}

export function randomCharacter(chars: string, amount: number, seed: number): string {
  let state = seed;
  function rand(): number {
    state = (state * 1664525 + 1013904223) & 0xffffffff;
    return (state >>> 0) / 0xffffffff;
  }
  const n = Math.max(0, Math.round(amount));
  let result = '';
  for (let i = 0; i < n; i++) {
    result += chars[Math.floor(rand() * chars.length)];
  }
  return result;
}

export function asBinaryString(str: string): string {
  return [...str].map(c => c.charCodeAt(0).toString(2).padStart(8, '0')).join(' ');
}

export function asBinaryList(str: string): number[] {
  return [...str].map(c => c.charCodeAt(0));
}

export function asNumberList(str: string, separator: string): number[] {
  return str.split(separator).map(s => parseFloat(s.trim())).filter(n => !isNaN(n));
}
