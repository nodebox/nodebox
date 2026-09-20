#!/usr/bin/env node
// Compute a hash for the given object.

"use strict";

var fsUtils = require("../src/server/fsUtils");

var fileName = process.argv[2];
fsUtils.hashFile(fileName, function (err, hash) {
  if (err) {
    console.log("ERROR: " + err + err.stack);
  } else {
    console.log(hash);
  }
});
