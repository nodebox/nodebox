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
  const needsCoreG =
    Object.keys(project.dependencies ?? {}).includes(CLASSIC_CORE_G_KEY) &&
    !given.some((d) => d.key === CLASSIC_CORE_G_KEY);
  const inputs = [
    ...(needsCoreG ? [{ key: CLASSIC_CORE_G_KEY, project: classicCoreG }] : []),
    ...given,
    { key, project },
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

/** The namespace object a host passes for a library whose functions live in a package. */
export type ClassicLibraryNamespace = ClassicNamespace;
