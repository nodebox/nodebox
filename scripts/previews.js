#!/usr/bin/env node

"use strict";

var _ = require("lodash");
var AWS = require("aws-sdk");

var sourceBucket = "nodeboxlive";
var s3 = new AWS.S3({ params: { Bucket: sourceBucket } });

var sourcePrefix = "";

function listObjects(marker, projects, callback) {
  var params = { Prefix: sourcePrefix };
  if (marker) {
    params.Marker = marker;
    console.log("LIST", marker);
  } else {
    console.log("LIST (start)");
  }
  s3.listObjects(params, function (err, data) {
    if (data.IsTruncated) {
      listObjects(data.Contents[data.Contents.length - 1].Key, projects, callback);
    }
    var key;
    var projectDir;
    for (var i = 0; i < data.Contents.length; i += 1) {
      key = data.Contents[i].Key;
      if (key.indexOf("project.json") > -1) {
        projectDir = key.split("/").slice(0, 2).join("/");
        if (!projects[projectDir]) {
          projects[projectDir] = {};
        }
        projects[projectDir].projectLastModified = data.Contents[i].LastModified;
      }
      if (key.indexOf("screenshot.svg") > -1) {
        projectDir = key.split("/").slice(0, 2).join("/");
        if (!projects[projectDir]) {
          projects[projectDir] = {};
        }
        projects[projectDir].screenshotLastModified = data.Contents[i].LastModified;
      }
    }
    if (!data.IsTruncated) {
      callback(projects);
    }
  });
}

listObjects(null, {}, function (projects) {
  var renderProjects = [];
  _.each(projects, function (project, key) {
    if (!project.screenshotLastModified) {
      renderProjects.push(key);
    } else if (project.projectLastModified) {
      if (new Date(project.projectLastModified) > new Date(project.screenshotLastModified)) {
        renderProjects.push(key);
      }
    }
  });
  console.log("Projects to render:");
  console.log(renderProjects);
  // todo: render the projects to SVG.
});
