// The built-in node libraries (color, core, corevector, data, device, list, math, network, string),
// parsed from the .ndbx sources embedded at build time, and the repository that resolves
// "library.node" prototypes against them.

import { NodeRepository } from "../model/library";
import { Library } from "../model/types";
import { parseNdbx, setDefaultRepository } from "../ndbx/reader";
import { builtinLibrarySources } from "./builtin-ndbx";

let repository: NodeRepository | undefined;
const libraries = new Map<string, Library>();

/** Load order matters: core first (node, network, frame), then the libraries that extend it. */
const LOAD_ORDER = ["core", "math", "list", "string", "color", "corevector", "data", "network", "device"];

export function builtinLibraryNames(): string[] {
  return Object.keys(builtinLibrarySources);
}

/** Parse (once) and return the built-in node repository. */
export function builtinNodeRepository(): NodeRepository {
  if (repository) return repository;
  repository = new NodeRepository();
  const names = [...LOAD_ORDER, ...builtinLibraryNames().filter((n) => !LOAD_ORDER.includes(n))];
  for (const name of names) {
    const xml = builtinLibrarySources[name];
    if (!xml) continue;
    const { library } = parseNdbx(xml, { repository, name, upgrade: false });
    libraries.set(name, library);
    repository.add(library);
  }
  setDefaultRepository(repository);
  return repository;
}

export function builtinLibrary(name: string): Library | undefined {
  builtinNodeRepository();
  return libraries.get(name);
}

export { builtinLibrarySources };
