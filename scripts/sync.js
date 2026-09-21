#!/usr/bin/env node
// List the projects in the data store and update the user profile.
// This synchronizes the data stored in the projects with the data stored in the user profiles.

/* jshint strict: false */

var _ = require("lodash");
var async = require("async");

var users = require("../users");
var projects = require("../projects");

// Synchronize all projects for a given user.
// The callback takes one argument (err).
function syncUserProjects(userId, callback) {
  console.log("Syncing", userId);
  projects.getProjectIds(userId, function (err, projectIds) {
    if (err) {
      callback(err);
    } else {
      async.map(
        projectIds,
        function (projectId, callback) {
          projects.read(userId, projectId, function (err, project) {
            if (err) {
              callback(err);
            } else {
              callback(null, [projectId, project]);
            }
          });
        },
        function (err, results) {
          var projectObjects = _.map(results, function (idProject) {
            var id = idProject[0],
              project = idProject[1],
              p = {};
            p.id = id;
            p.title = project.title || project.id;
            p.color = project.color || "teal";
            return p;
          });
          users.read(userId, function (err, profile) {
            if (err) {
              callback(err);
            } else {
              profile.projects = projectObjects;
              users.update(userId, profile, callback);
            }
          });
        },
      );
    }
  });
}

users.getUserIds(function (err, userIds) {
  if (err) {
    console.log(err);
  } else {
    var tasks = _.map(userIds, function (userId) {
      return _.partial(syncUserProjects, userId);
    });
    async.parallel(tasks, function (err) {
      if (err) {
        console.log(err);
      } else {
        console.log("All profiles synced.");
      }
    });
  }
});
