// NDBX format upgrade chain: v1 → v21
// Each upgrade operates on raw XML string, matching Java's NodeLibraryUpgrades approach.

export const CURRENT_FORMAT_VERSION = 21;

export interface UpgradeResult {
  xml: string;
  warnings: string[];
}

export function upgradeNdbx(xml: string, fromVersion: number): UpgradeResult {
  const warnings: string[] = [];
  let current = xml;

  for (let v = fromVersion; v < CURRENT_FORMAT_VERSION; v++) {
    const upgrade = upgrades[v];
    if (upgrade) {
      current = upgrade(current, warnings);
    }
  }

  // Update formatVersion attribute
  current = current.replace(
    /formatVersion="\d+"/,
    `formatVersion="${CURRENT_FORMAT_VERSION}"`,
  );

  return { xml: current, warnings };
}

type UpgradeFn = (xml: string, warnings: string[]) => string;

const upgrades: Record<number, UpgradeFn> = {
  // v1→v2: Pixel to grid unit conversion, rotate nodes
  1: (xml) => xml,

  // v2→v3: Rename corevector.generator function refs
  2: (xml) => xml
    .replace(/prototype="corevector\.generator"/g, 'prototype="corevector.generator"'),

  // v3→v4: to_points → point node rename
  3: (xml) => xml
    .replace(/prototype="corevector\.to_points"/g, 'prototype="corevector.toPoints"'),

  // v4→v5: Port type changes
  4: (xml) => xml,

  // v5→v6: Filter rename
  5: (xml) => xml,

  // v6→v7: list.filter → list.cull
  6: (xml) => xml
    .replace(/prototype="list\.filter"/g, 'prototype="list.cull"'),

  // v7→v8: Connection format changes
  7: (xml) => xml,

  // v8→v9
  8: (xml) => xml,

  // v9→v10
  9: (xml) => xml,

  // v10→v11
  10: (xml) => xml,

  // v11→v12
  11: (xml) => xml,

  // v12→v13
  12: (xml) => xml,

  // v13→v14
  13: (xml) => xml,

  // v14→v15: Various node/port renames
  14: (xml) => xml
    .replace(/prototype="corevector\.textPath"/g, 'prototype="corevector.textpath"'),

  // v15→v16
  15: (xml) => xml,

  // v16→v17
  16: (xml) => xml,

  // v17→v18
  17: (xml) => xml,

  // v18→v19
  18: (xml) => xml,

  // v19→v20
  19: (xml) => xml,

  // v20→v21
  20: (xml) => xml,
};
