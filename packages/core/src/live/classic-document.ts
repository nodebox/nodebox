// Opening a classic NodeBox Live project together with the projects it depends on: the reader
// turns each project into a library, the runtime evaluates their function sources, and the two
// come back as one repository pair ready for a NodeContext.

import { NodeRepository } from "../model/library";
import { FunctionRepository } from "../runtime/function-repository";
import { Library } from "../model/types";
import { parseClassicProject } from "./classic-reader";
import { ClassicNamespace, ClassicRuntime, ClassicRuntimeOptions } from "./classic-runtime";
import { classicCoreG } from "./classic-g";
import { ClassicProject } from "./classic-types";

export interface ClassicDocumentInput {
  /** "userId/projectId", the key a project's dependencies are listed under. */
  key: string;
  project: ClassicProject;
}

/** The classic library every classic project builds on; its code comes from the g.js package. */
export const CLASSIC_CORE_G_KEY = "core/g";

export interface OpenClassicOptions extends ClassicRuntimeOptions {
  /**
   * The projects this one depends on, directly or through them. "core/g" is supplied from the
   * library built into this package unless it is listed here.
   */
  dependencies?: ClassicDocumentInput[];
  /** Assets by file name, as the sources expect them: text as a string, binary as an ArrayBuffer. */
  assets?: Record<string, unknown>;
}

export interface ClassicDocument {
  /** The library of the project itself. */
  library: Library;
  libraries: Library[];
  repository: NodeRepository;
  functions: FunctionRepository;
  runtime: ClassicRuntime;
  warnings: string[];
}

/** The namespace of a project, which is the second half of its "userId/projectId" key. */
export function classicNamespaceOf(key: string): string {
  const slash = key.indexOf("/");
  return slash < 0 ? key : key.slice(slash + 1);
}

/**
 * The classic project for a dependency key that @ndbx/core ships itself. Only "core/g" does: its
 * 172 declarations sit over the g.js package, and the server's own copy was rewritten in a later
 * format that the classic nodes no longer match.
 */
export function classicLibraryProject(key: string): ClassicProject | undefined {
  return key === CLASSIC_CORE_G_KEY ? classicCoreG : undefined;
}

export function openClassicProject(
  key: string,
  project: ClassicProject,
  options: OpenClassicOptions = {},
): ClassicDocument {
  const warnings: string[] = [];
  const runtime = new ClassicRuntime(options);
  const repository = new NodeRepository();
  const libraries: Library[] = [];

  // Dependencies first: a project's networks instantiate their prototypes.
  const given = options.dependencies ?? [];
  const main = { key, project };
  const needsCoreG =
    [main, ...given].some((input) => Object.keys(input.project.dependencies ?? {}).includes(CLASSIC_CORE_G_KEY)) &&
    !given.some((d) => d.key === CLASSIC_CORE_G_KEY);
  const inputs = [
    ...inDependencyOrder([...(needsCoreG ? [{ key: CLASSIC_CORE_G_KEY, project: classicCoreG }] : []), ...given]),
    main,
  ];
  for (const input of inputs) {
    const namespace = classicNamespaceOf(input.key);
    warnings.push(...runtime.addProject(input.project, namespace));
    const result = parseClassicProject(input.project, { namespace, repository });
    warnings.push(...result.warnings);
    repository.add(result.library);
    libraries.push(result.library);
  }
  Object.assign(runtime.ndbx.assets, options.assets ?? {});
  return {
    library: libraries[libraries.length - 1],
    libraries,
    repository,
    functions: runtime.repository(),
    runtime,
    warnings,
  };
}

/**
 * A project is read after the projects it depends on, because a network instantiates the
 * prototypes it uses. A cycle falls back to the order the caller gave.
 */
function inDependencyOrder(inputs: ClassicDocumentInput[]): ClassicDocumentInput[] {
  const byKey = new Map(inputs.map((input) => [input.key, input]));
  const ordered: ClassicDocumentInput[] = [];
  const seen = new Set<string>();
  const visit = (input: ClassicDocumentInput): void => {
    if (seen.has(input.key)) return;
    seen.add(input.key);
    for (const key of Object.keys(input.project.dependencies ?? {})) {
      const dependency = byKey.get(key);
      if (dependency) visit(dependency);
    }
    ordered.push(input);
  };
  for (const input of inputs) visit(input);
  return ordered;
}

/** The namespace object a host passes for a library whose functions live in a package. */
export type ClassicLibraryNamespace = ClassicNamespace;
