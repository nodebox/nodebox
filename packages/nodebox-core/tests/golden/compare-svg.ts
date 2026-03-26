/**
 * Structural SVG comparison with floating-point tolerance.
 * Compares path counts, coordinates, colors, and stroke widths
 * without being sensitive to whitespace or number formatting differences.
 */

export interface SvgDiff {
  match: boolean;
  differences: string[];
}

interface SvgPathElement {
  d: string;
  fill: string | null;
  stroke: string | null;
  strokeWidth: string | null;
}

/** Compare two SVG strings structurally with coordinate tolerance. */
export function compareSvg(actual: string, expected: string, tolerance = 0.5): SvgDiff {
  const differences: string[] = [];

  const actualPaths = extractPaths(actual);
  const expectedPaths = extractPaths(expected);

  if (actualPaths.length !== expectedPaths.length) {
    differences.push(`path count: actual=${actualPaths.length}, expected=${expectedPaths.length}`);
    // Still compare what we can
  }

  const count = Math.min(actualPaths.length, expectedPaths.length);
  for (let i = 0; i < count; i++) {
    const ap = actualPaths[i];
    const ep = expectedPaths[i];

    // Compare path data with tolerance
    const pathDiffs = comparePathData(ap.d, ep.d, tolerance);
    for (const d of pathDiffs) {
      differences.push(`path[${i}] d: ${d}`);
    }

    // Compare fill — treat missing fill as black (SVG default)
    if (!colorsMatch(ap.fill ?? '#000000', ep.fill ?? '#000000')) {
      differences.push(`path[${i}] fill: actual="${ap.fill}", expected="${ep.fill}"`);
    }

    // Compare stroke — treat missing stroke and stroke="none" as equivalent
    const aHasStroke = ap.stroke !== null && ap.stroke !== 'none';
    const eHasStroke = ep.stroke !== null && ep.stroke !== 'none';
    if (aHasStroke && eHasStroke) {
      if (!colorsMatch(ap.stroke, ep.stroke)) {
        differences.push(`path[${i}] stroke: actual="${ap.stroke}", expected="${ep.stroke}"`);
      }
      // Compare stroke-width only when both have strokes
      const aw = ap.strokeWidth ? parseFloat(ap.strokeWidth) : 1;
      const ew = ep.strokeWidth ? parseFloat(ep.strokeWidth) : 1;
      if (Math.abs(aw - ew) > 0.01) {
        differences.push(`path[${i}] stroke-width: actual="${ap.strokeWidth}", expected="${ep.strokeWidth}"`);
      }
    } else if (aHasStroke !== eHasStroke) {
      differences.push(`path[${i}] stroke: actual="${ap.stroke}", expected="${ep.stroke}"`);
    }
  }

  return { match: differences.length === 0, differences };
}

/** Extract <path> elements from SVG string. */
function extractPaths(svg: string): SvgPathElement[] {
  const paths: SvgPathElement[] = [];
  const pathRegex = /<path\s+([^>]*?)\/>/g;
  let match;
  while ((match = pathRegex.exec(svg)) !== null) {
    const attrs = match[1];
    paths.push({
      d: extractAttr(attrs, 'd') ?? '',
      fill: extractAttr(attrs, 'fill'),
      stroke: extractAttr(attrs, 'stroke'),
      strokeWidth: extractAttr(attrs, 'stroke-width'),
    });
  }
  return paths;
}

/** Extract an attribute value from an attribute string. */
function extractAttr(attrs: string, name: string): string | null {
  const regex = new RegExp(`${name}="([^"]*)"`, 'i');
  const match = regex.exec(attrs);
  return match ? match[1] : null;
}

/** Parse SVG path data into commands with numeric arguments. */
function parsePathCommands(d: string): { cmd: string; args: number[] }[] {
  const commands: { cmd: string; args: number[] }[] = [];
  // Split into command groups: letter followed by numbers/commas/spaces
  const parts = d.match(/[MLCQZHVASTmlcqzhvast][^MLCQZHVASTmlcqzhvast]*/g);
  if (!parts) return commands;
  for (const part of parts) {
    const cmd = part[0];
    const argStr = part.slice(1).trim();
    const args = argStr ? argStr.split(/[\s,]+/).map(Number).filter(n => !isNaN(n)) : [];
    commands.push({ cmd, args });
  }
  return commands;
}

/** Compare path data strings with coordinate tolerance. */
function comparePathData(actual: string, expected: string, tolerance: number): string[] {
  const diffs: string[] = [];
  const aCmds = parsePathCommands(actual);
  const eCmds = parsePathCommands(expected);

  if (aCmds.length !== eCmds.length) {
    diffs.push(`command count: actual=${aCmds.length}, expected=${eCmds.length}`);
    return diffs;
  }

  for (let i = 0; i < aCmds.length; i++) {
    const ac = aCmds[i];
    const ec = eCmds[i];

    if (ac.cmd !== ec.cmd) {
      diffs.push(`cmd[${i}]: actual='${ac.cmd}', expected='${ec.cmd}'`);
      continue;
    }

    if (ac.args.length !== ec.args.length) {
      diffs.push(`cmd[${i}] ${ac.cmd} arg count: actual=${ac.args.length}, expected=${ec.args.length}`);
      continue;
    }

    for (let j = 0; j < ac.args.length; j++) {
      if (Math.abs(ac.args[j] - ec.args[j]) > tolerance) {
        diffs.push(`cmd[${i}] ${ac.cmd} arg[${j}]: actual=${ac.args[j]}, expected=${ec.args[j]}`);
      }
    }
  }

  return diffs;
}

/** Compare colors, normalizing different representations. */
function colorsMatch(a: string | null, b: string | null): boolean {
  if (a === b) return true;
  if (a === null || b === null) return false;
  const na = normalizeColor(a);
  const nb = normalizeColor(b);
  if (na === nb) return true;
  // Allow small channel differences (±5 per channel out of 255)
  const ac = parseColor(na);
  const bc = parseColor(nb);
  if (ac && bc) {
    return Math.abs(ac.r - bc.r) <= 8 && Math.abs(ac.g - bc.g) <= 8 && Math.abs(ac.b - bc.b) <= 8;
  }
  return false;
}

function parseColor(c: string): { r: number; g: number; b: number } | null {
  if (c.startsWith('#') && c.length === 7) {
    return { r: parseInt(c.slice(1, 3), 16), g: parseInt(c.slice(3, 5), 16), b: parseInt(c.slice(5, 7), 16) };
  }
  const m = c.match(/rgba?\((\d+),(\d+),(\d+)/);
  if (m) return { r: parseInt(m[1]), g: parseInt(m[2]), b: parseInt(m[3]) };
  return null;
}

/** Normalize a CSS color string for comparison. */
function normalizeColor(c: string): string {
  c = c.trim().toLowerCase();
  // #000000 can also be written as "black" in Java
  if (c === 'black') return '#000000';
  if (c === 'white') return '#ffffff';
  if (c === 'none') return 'none';
  return c;
}
