#!/usr/bin/env node
// Convert nodes in all projects to the new, value-only format.

"use strict";

var fs = require("fs");
var path = require("path");
var _ = require("lodash");

var fsUtils = require("../src/server/fsUtils.cjs");

// Location of the projects
var DATA_DIR = "./data";

function convertNode(node) {
  // The node is already converted.
  if (!node.values) {
    node.values = {};
    _.each(node.parameters, function (p) {
      node.values[p.name] = p.value;
    });
  }
  // Make sure parameters doesn't exist.
  delete node.parameters;
  // And node._fn shouldn't be there either.
  delete node._fn;
  return node;
}

function convertFunction(fn) {
  if (fn.type !== "network") return fn;
  fn.nodes = _.map(fn.nodes, convertNode);
  return fn;
}

function latestVersionForDependency(projectName, dependencyName) {
  var userId = dependencyName.split("/")[0];
  var projectId = dependencyName.split("/")[1];
  var releaseFile = path.join(DATA_DIR, userId, projectId, "release.json");
  if (fs.existsSync(releaseFile)) {
    var releaseData = JSON.parse(fs.readFileSync(releaseFile));
    return releaseData.latest;
  } else {
    console.log(
      "WARN: Project " +
        projectName +
        ": dependency " +
        dependencyName +
        " has no released version. Using dev version.",
    );
    return "dev";
  }
}

// Convert dependencies from a list-style to map-style.
// e.g. ["core/math"] --> {"core/math": "0.0.1"}
function convertDependencies(projectName, dependencies) {
  if (!_.isArray(dependencies)) {
    // The dependencies are already converted to a map.
    return dependencies;
  }
  var newDependencies = {};
  for (var i = 0; i < dependencies.length; i++) {
    var dependencyName = dependencies[i];
    var latestVersion = latestVersionForDependency(projectName, dependencyName);
    newDependencies[dependencyName] = latestVersion;
  }
  return newDependencies;
}

function ensureAttachmentsDirectoryEmpty(userId, projectId, attachmentType) {
  var attachmentsDir = path.join(DATA_DIR, userId, projectId, attachmentType);
  if (!fs.existsSync(attachmentsDir)) return;
  var files = fs.readdirSync(attachmentsDir);
  if (files.length > 0) {
    console.log(
      "WARN: Project " + userId + "/" + projectId + ": " + attachmentType + " directory is not empty.",
      files,
    );
  } else {
    fs.rmdirSync(attachmentsDir);
  }
}

// Convert attachments of `type` to blobs.
// Type is either "assets" or "scripts".
function _convertToBlobs(userId, projectId, attachmentType, attachments) {
  if (_.isArray(attachments)) {
    var attachmentsMap = {};
    var attachmentsDir = path.join(DATA_DIR, userId, projectId, attachmentType);
    var blobsDir = path.join(DATA_DIR, userId, projectId, "blobs");
    for (var i = 0; i < attachments.length; i++) {
      var attachment = attachments[i];
      var attachmentFile = path.join(attachmentsDir, attachment);
      var hash = fsUtils.hashFileSync(attachmentFile);
      var blobFile = path.join(blobsDir, hash);
      fsUtils.ensureDirectorySync(blobsDir);
      fs.renameSync(attachmentFile, blobFile);
      attachmentsMap[attachment] = hash;
    }
    ensureAttachmentsDirectoryEmpty(userId, projectId, attachmentType);
    return attachmentsMap;
  } else {
    ensureAttachmentsDirectoryEmpty(userId, projectId, attachmentType);
    return attachments;
  }
}

// Convert assets from a list-style to map-style.
// This also hashes the assets and puts them in a "blobs" folder.
function convertAssets(userId, projectId, assets) {
  return _convertToBlobs(userId, projectId, "assets", assets);
}

// Convert scripts from a list-style to map-style.
// This also hashes the scripts and puts them in a "blobs" folder.
function convertScripts(userId, projectId, scripts) {
  return _convertToBlobs(userId, projectId, "scripts", scripts);
}

function convertProject(userId, projectId, fileName) {
  console.log("  " + projectId);
  var projectName = userId + "/" + projectId;
  var project = JSON.parse(fs.readFileSync(fileName));
  project.dependencies = convertDependencies(projectName, project.dependencies);
  project.assets = convertAssets(userId, projectId, project.assets);
  project.scripts = convertScripts(userId, projectId, project.scripts);
  project.functions = _.map(project.functions, convertFunction);
  fs.writeFileSync(fileName, JSON.stringify(project, null, 4));
}

// Convert all projects for a given userId.
function convertUserProjects(userId) {
  console.log(userId);
  var userDir = path.join(DATA_DIR, userId);
  var projectIds = fs.readdirSync(userDir);
  _.each(projectIds, function (projectId) {
    var projectDir = path.join(DATA_DIR, userId, projectId);
    var stats = fs.statSync(projectDir);
    if (stats.isDirectory()) {
      var oldProjectFile = path.join(projectDir, projectId + ".json");
      var projectFile = path.join(projectDir, "project.json");
      if (fs.existsSync(oldProjectFile)) {
        fs.renameSync(oldProjectFile, projectFile);
      }
      convertProject(userId, projectId, projectFile);
    }
  });
}

// Find all users and convert their projects.
function convertAllProjects() {
  var userNames = fs.readdirSync(DATA_DIR);
  _.each(userNames, function (userName) {
    var userDir = path.join(DATA_DIR, userName);
    var stats = fs.statSync(userDir);
    if (stats.isDirectory()) {
      convertUserProjects(userName);
    }
  });
}

convertAllProjects();
