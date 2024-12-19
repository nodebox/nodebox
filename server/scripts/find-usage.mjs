// Check for the usage of a specific node for all users.
// Returns the projects and network item that use the node.

import "dotenv/config";
import * as s3Store from "../src/s3-store.js";
import process from "process";

const args = process.argv.slice(2);
const nodeName = args[0];
if (!nodeName) {
  console.error("Usage: node find-usage.mjs <node-name>");
  console.error("Example: node find-usage.mjs core/g/Rect");
  process.exit(1);
}
const keys = nodeName.split("/");
if (keys.length !== 3) {
  console.error("Invalid node name. Should look like core/g/Rect");
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
      console.error(`Error loading project ${userId}/${projectId}: ${err.message}`);
      continue;
    }
    if (!Array.isArray(project.items)) continue;
    // console.log(project);
    for (const item of project.items) {
      if (item.type !== "NETWORK") continue;
      for (const node of item.children) {
        if (node.type !== "NODE") continue;
        if (node.fn.toLowerCase() === nodeName.toLowerCase()) {
          console.log(`User: ${userId}, Project: ${projectId}, Network: ${item.name}`);
          break;
        }
      }
    }
    // console.log(data);
  }
}
