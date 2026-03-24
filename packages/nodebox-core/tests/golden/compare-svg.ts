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

    // Compare fill
    if (!colorsMatch(ap.fill, ep.fill)) {
      differences.push(`path[${i}] fill: actual="${ap.fill}", expected="${ep.fill}"`);
    }

    // Compare stroke
    if (!colorsMatch(ap.stroke, ep.stroke)) {
      differences.push(`path[${i}] stroke: actual="${ap.stroke}", expected="${ep.stroke}"`);
    }

    // Compare stroke-width
    if (ap.strokeWidth !== ep.strokeWidth) {
      // Try numeric comparison
      const aw = ap.strokeWidth ? parseFloat(ap.strokeWidth) : 0;
      const ew = ep.strokeWidth ? parseFloat(ep.strokeWidth) : 0;
      if (Math.abs(aw - ew) > 0.01) {
        differences.push(`path[${i}] stroke-width: actual="${ap.strokeWidth}", expected="${ep.strokeWidth}"`);
      }
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
  // Normalize: both to lowercase
  const na = normalizeColor(a);
  const nb = normalizeColor(b);
  return na === nb;
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
