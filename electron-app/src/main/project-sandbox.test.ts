// @vitest-environment node
import { mkdtemp, mkdir, realpath, rm, symlink, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { isInside, resolveAssetPath, toProjectRelative } from './project-sandbox';

let project: string;
let outside: string;

beforeEach(async () => {
  project = await realpath(await mkdtemp(join(tmpdir(), 'nodebox-project-')));
  outside = await realpath(await mkdtemp(join(tmpdir(), 'nodebox-outside-')));
  await mkdir(join(project, 'data'));
  await writeFile(join(project, 'data', 'points.csv'), 'x,y');
  await writeFile(join(outside, 'secret.txt'), 'secret');
});

afterEach(async () => {
  await rm(project, { recursive: true, force: true });
  await rm(outside, { recursive: true, force: true });
});

describe('isInside', () => {
  it('accepts the root and its children', () => {
    expect(isInside('/p', '/p')).toBe(true);
    expect(isInside('/p', '/p/a/b')).toBe(true);
    expect(isInside('/p', '/p/..data')).toBe(true);
  });

  it('rejects parents and siblings', () => {
    expect(isInside('/p', '/')).toBe(false);
    expect(isInside('/p', '/p2/a')).toBe(false);
    expect(isInside('/p', '/other')).toBe(false);
  });
});

describe('resolveAssetPath', () => {
  it('resolves a file in the project', async () => {
    expect(await resolveAssetPath(project, 'data/points.csv')).toEqual({ path: join(project, 'data', 'points.csv') });
  });

  it('rejects absolute paths and parent segments', async () => {
    expect(await resolveAssetPath(project, join(outside, 'secret.txt'))).toEqual({ error: 'sandbox' });
    expect(await resolveAssetPath(project, '../secret.txt')).toEqual({ error: 'sandbox' });
  });

  it('rejects a symlink that points out of the project', async () => {
    await symlink(outside, join(project, 'link'));
    expect(await resolveAssetPath(project, 'link/secret.txt')).toEqual({ error: 'sandbox' });
  });

  it('follows a symlink that stays in the project', async () => {
    await symlink(join(project, 'data'), join(project, 'alias'));
    expect(await resolveAssetPath(project, 'alias/points.csv')).toEqual({ path: join(project, 'data', 'points.csv') });
  });

  it('reports missing files', async () => {
    expect(await resolveAssetPath(project, 'nope.csv')).toEqual({ error: 'not_found' });
  });
});

describe('toProjectRelative', () => {
  it('returns a forward-slash path for a file in the project', async () => {
    expect(await toProjectRelative(project, join(project, 'data', 'points.csv'))).toBe('data/points.csv');
  });

  it('rejects files outside the project, also through a symlink', async () => {
    expect(await toProjectRelative(project, join(outside, 'secret.txt'))).toBeNull();
    await symlink(outside, join(project, 'link'));
    expect(await toProjectRelative(project, join(project, 'link', 'secret.txt'))).toBeNull();
  });
});
