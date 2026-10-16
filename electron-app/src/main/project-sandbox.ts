// Keeps asset access inside the project folder.
//
// Paths are compared after resolving symlinks, so a link inside the project that points
// elsewhere is treated as being elsewhere.

import { realpath } from 'node:fs/promises';
import { isAbsolute, relative, resolve, sep } from 'node:path';

/** Whether `child` is `root` or somewhere below it. Both must already be resolved. */
export function isInside(root: string, child: string): boolean {
  const rel = relative(root, child);
  return rel === '' || (rel !== '..' && !rel.startsWith('..' + sep) && !isAbsolute(rel));
}

export type AssetPath = { path: string } | { error: 'sandbox' | 'not_found' };

/** Resolve a project-relative path to the real path of an existing file in the project. */
export async function resolveAssetPath(projectDir: string, relativePath: string): Promise<AssetPath> {
  if (isAbsolute(relativePath)) return { error: 'sandbox' };
  let root: string;
  let real: string;
  try {
    root = await realpath(projectDir);
    const target = resolve(root, relativePath);
    if (!isInside(root, target)) return { error: 'sandbox' };
    real = await realpath(target);
  } catch {
    return { error: 'not_found' };
  }
  return isInside(root, real) ? { path: real } : { error: 'sandbox' };
}

/**
 * Turn a file the user picked into a project-relative path with forward slashes, so the
 * stored value works on every platform. Returns null when the file is outside the project.
 */
export async function toProjectRelative(projectDir: string, absolutePath: string): Promise<string | null> {
  try {
    const root = await realpath(projectDir);
    const real = await realpath(absolutePath);
    if (!isInside(root, real) || real === root) return null;
    return relative(root, real).split(sep).join('/');
  } catch {
    return null;
  }
}
