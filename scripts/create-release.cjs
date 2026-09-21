#!/usr/bin/env node
// Create a release for the given userId / projectId / version.

"use strict";

var projects = require("../src/server/projects.cjs");

function usage() {
  console.log("Usage: npm run create-release USER_ID PROJECT_ID VERSION");
  console.log();
  console.log("Creates a versioned release of the given project.");
  console.log('Versions should be in x.y.z format, e.g. "1.0.5".');
  console.log();
}

var userId = process.argv[2];
var projectId = process.argv[3];
var version = process.argv[4];
if (userId && projectId && version) {
  projects.createRelease(userId, projectId, version, {}, function (err) {
    if (err) {
      console.log("ERROR:", err);
    } else {
      console.log(userId + "/" + projectId + ": Release " + version + " created.");
    }
  });
} else {
  usage();
}
