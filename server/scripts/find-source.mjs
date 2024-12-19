// Check for occurrences of a string in function sources for all users.
// Returns the projects and function items that contain the search string.

import "dotenv/config";
import * as s3Store from "../src/s3-store.js";
import process from "process";

const args = process.argv.slice(2);
const searchString = args[0];
if (!searchString) {
  console.error("Usage: node find-source.mjs <search-string>");
  console.error("Example: node find-source.mjs 'node.onRender'");
  process.exit(1);
}

const userIds = await s3Store.listRootDir();
for (const userId of userIds) {
  const projectDetails = await s3Store.listProjects(userId);
  for (const projectDetail of projectDetails) {
    const projectId = projectDetail.id;
    let project;
    try {
      project = await s3Store.getUserProject(userId, projectId);
    } catch (err) {
      console.error(`Error loading project ${projectId}: ${err.message}`);
      continue;
    }
    if (!Array.isArray(project.items)) continue;

    for (const item of project.items) {
      if (item.type !== "FUNCTION") continue;

      // Check if the function source contains the search string
      if (item.source && item.source.toLowerCase().includes(searchString.toLowerCase())) {
        console.log(`https://new.nodebox.live/${userId}/${projectId}#${item.id} Function ${item.name}`);
        const lines = item.source.split("\n");
        lines.forEach((line, i) => {
          if (line.toLowerCase().includes(searchString.toLowerCase())) {
            console.log(`  ${i + 1}: ${line}`);
          }
        });
        console.log();
      }
    }
  }
}
