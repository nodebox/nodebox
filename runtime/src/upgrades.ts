// rename updateFormatVersion -> upgrades

// Import the 'Project' type from the types module.
// import { stringify } from "json5";
// import { Network, FunctionItem, Project } from "./types";
import { Project, Node } from "./types";

// Define a constant for the latest supported format version.
export const LATEST_FORMAT_VERSION = 4;

interface QueryItem {
  [key: string]: any;
}

function queryProjectItems(project: Project, query: QueryItem, subQuery: QueryItem): QueryItem[] {
  const items = project.items;
  const childOrConnection = Object.keys(subQuery)[0];
  const domain = subQuery[childOrConnection];
  const ret: any[] = [];
  const resultArray = items.filter((item) =>
    Object.keys(query).every((key) => (item as QueryItem)[key] === query[key]),
  );

  resultArray.forEach((el: QueryItem) => {
    ret.push(
      ...el[childOrConnection].filter((item: QueryItem) =>
        Object.keys(domain).every((key: string) => item[key] === domain[key]),
      ),
    );
  });
  return ret;
}

/**
 * Sets the current format version on the project to the latest supported version,
 * ensuring compatibility and integrity of the project's format.
 *
 * @param project The project object to be updated.
 * @returns The updated project object with the correct format version.
 * @throws Error if the project's version is newer than supported.
 */
export function updateFormatVersion(project: Project, formatVersion: number = LATEST_FORMAT_VERSION) {
  // Check if the project format version is undefined and initialize it to 1.
  if (project.formatVersion === undefined) {
    project.formatVersion = 1;
  }
  // If the project's version is already the latest, return the project as-is.
  if (project.formatVersion === formatVersion) {
    return project;
  }

  // If the project's version is greater than the latest supported version,
  // throw an error indicating that this version is actually not possible in the online version.
  if (project.formatVersion > formatVersion) {
    throw new Error("Invalid project format version. You might want to retry later on.");
  }

  // Upgrade the project format version incrementally until it reaches the latest version.
  while (project.formatVersion < formatVersion) {
    if (project.formatVersion === 1) {
      // Version 2: Update load-csv to import-data
      queryProjectItems(project, { type: "NETWORK" }, { children: { fn: "core/g/load-csv" } }).forEach((el) => {
        el.fn = "core/g/import-data";
        // Use replace to keep sequence numbers.
        el.name = el.name.replace(/load-csv/g, "import-data");
      });
    } else if (project.formatVersion === 2) {
      // Version 3: Add "outputPorts": [] to project > items
      project.items.forEach((el) => {
        if (!el["outputPorts"]) el["outputPorts"] = [];
      });
    } else if (project.formatVersion === 3) {
      // Version 4: Rename all core nodes from "names-with-dashes" to "Names With Spaces"
      // So for example "core/g/filter-data" becomes "core/g/Filter Data".
      // Also rename the node's name if it starts with the old name.
      for (const item of project.items) {
        if (item.type === "NETWORK") {
          for (const child of item.children) {
            if (child.type === "NODE") {
              const node = child as Node;
              const functionName = node.fn;
              const parts = functionName.split("/");
              const userProjectPart = parts.slice(0, parts.length - 1).join("/");
              if (userProjectPart !== "core/g") continue;
              const namePart = parts[parts.length - 1];
              const namePartWords = namePart.split("-");
              const newName = namePartWords.map((w) => w.charAt(0).toUpperCase() + w.slice(1)).join(" ");
              node.fn = `${userProjectPart}/${newName}`;
              if (node.name.startsWith(namePart)) {
                node.name = node.name.replace(namePart, newName);
              }
            }
          }
        }
      }
    }

    // if (project.formatVersion === 3) {
    //   // Version 4: Add dependency
    //   if (!project.dependencies["core/plot"]) {
    //     project.dependencies["core/plot"] = "dev";
    //   }
    // }

    project.formatVersion++;
  }

  // Return the project after potentially upgrading it.
  return project;
}
