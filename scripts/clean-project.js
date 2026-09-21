// Update order in project files.

"use strict";

var fs = require("fs");

var fileName = process.argv[2];
console.assert(fileName);
var project = JSON.parse(fs.readFileSync(fileName));
console.log(project.id);

function assertProperties(obj, requiredProps, optionalProps) {
  var keys = Object.keys(obj);
  var i;
  var prop;
  var propIndex;
  for (i = 0; i < requiredProps.length; i++) {
    prop = requiredProps[i];
    propIndex = keys.indexOf(prop);
    console.assert(propIndex !== -1, 'Object does not have a "' + prop + '" property.');
    keys.splice(propIndex, 1);
  }
  if (optionalProps) {
    for (i = 0; i < optionalProps.length; i++) {
      prop = optionalProps[i];
      propIndex = keys.indexOf(prop);
      if (propIndex !== -1) {
        keys.splice(propIndex, 1);
      }
    }
  }
  console.assert(keys.length === 0, "Additional properties found: " + keys);
}

var newProject = {};

assertProperties(project, ["id", "title", "dependencies", "functions"]);

newProject.id = project.id;
newProject.title = project.title;
newProject.dependencies = project.dependencies;

var newFunctions = [];
for (var i = 0; i < project.functions.length; i++) {
  var fn = project.functions[i];
  console.log(fn.name);
  assertProperties(fn, ["name", "type", "outputType", "parameters", "ref", "category"], ["returnsList", "source"]);
  var newFn = {};
  newFn.name = fn.name;
  newFn.type = fn.type;
  newFn.outputType = fn.outputType;
  if (fn.returnsList) {
    newFn.returnsList = fn.returnsList;
  }
  newFn.parameters = fn.parameters;
  if (fn.source) {
    newFn.source = fn.source;
  }
  newFn.ref = fn.ref;
  newFn.category = fn.category;
  newFunctions.push(newFn);
}
newProject.functions = newFunctions;

fs.writeFileSync(fileName, JSON.stringify(newProject, null, 4));
console.log("Project " + fileName + " cleaned.");
