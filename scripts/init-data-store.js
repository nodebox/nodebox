#!/usr/bin/env node

// Initialize the data store.
// This scripts imports all users and projects in the "data" folder into the data store.
// Set the data store using the DATA_STORE environment variable first, e.g.
//
//     DATA_STORE=s3 npm run init-data-store

"use strict";

var _ = require("lodash");
var async = require("async");
var fs = require("fs");
var _path = require("path");

var fsUtils = require("../src/server/fsUtils");
var s3Utils = require("../src/server/s3Utils");

var DEFAULT_USERS = ["core", "demo", "skel", "template", "tutorial"];
var DATA_DIR = "./data";

var DATA_STORE = (process.env.DATA_STORE || "file").toLowerCase();

if (DATA_STORE === "file") {
  console.log('WARN: Using the "file" data store, which does not need to be initialized.');
  process.exit(0);
} else if (DATA_STORE === "s3") {
} else {
  console.log('Unsupported data store "%s".', DATA_STORE);
  process.exit(0);
}

// Recursively copy the given path to S3.
function copyToS3(path, callback) {
  var fileName = _path.join(DATA_DIR, path);
  var s3Key = path;
  fsUtils.isDirectory(fileName, function (isDir) {
    if (isDir) {
      fs.readdir(fileName, function (err, files) {
        if (err) {
          callback("Could not list directory " + fileName + ": " + err);
        } else {
          var tasks = [];
          for (var i = 0; i < files.length; i += 1) {
            var f = files[i];
            if (f[0] === ".") continue; // Skip .DS_Store etc.
            tasks.push(_.partial(copyToS3, _path.join(path, f)));
          }
          async.parallel(tasks, callback);
        }
      });
    } else {
      fs.readFile(fileName, function (err, data) {
        if (err) {
          callback(new Error("Could not read " + fileName + ": " + err));
        } else {
          s3Utils.put(s3Key, data, callback);
        }
      });
    }
  });
}

async.each(DEFAULT_USERS, copyToS3, function (err) {
  if (err) {
    console.log(err);
  } else {
    console.log("Copied project files from file system to " + DATA_STORE + ".");
  }
});
