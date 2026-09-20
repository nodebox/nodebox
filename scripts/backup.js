#!/usr/bin/env node

"use strict";

var AWS = require("aws-sdk");
var async = require("async");

var days = ["sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"];
var weekday = days[new Date().getDay()];

var sourceBucket = "nodeboxlive";
var targetBucket = "nodeboxlive-backups";
var sourcePrefix = "";
var targetPrefix = "daily/" + weekday + "/";

var s3 = new AWS.S3({ params: { Bucket: sourceBucket } });
//var targetS3 = new AWS.S3({params: {Bucket: targetBucket}});

function copyFile(file, callback) {
  var params = {
    CopySource: sourceBucket + "/" + file.Key,
    Bucket: targetBucket,
    Key: targetPrefix + file.Key,
  };
  s3.copyObject(params, function (err) {
    if (err) {
      console.log("ERR copying " + file.Key + ": " + err);
    }
    callback();
  });
}

function makeBackup(marker) {
  var params = { Prefix: sourcePrefix };
  if (marker) {
    params.Marker = marker;
    console.log("LIST", marker);
  } else {
    console.log("LIST (start)");
  }
  s3.listObjects(params, function (err, data) {
    if (data.IsTruncated) {
      makeBackup(data.Contents[data.Contents.length - 1].Key);
    }
    async.each(data.Contents, copyFile, function () {
      console.log("DONE", marker === undefined ? "(start)" : marker);
    });
  });
}

makeBackup();
