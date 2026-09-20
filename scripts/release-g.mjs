#!/usr/bin/env node

import { execSync } from "child_process";
import { readFileSync, writeFileSync } from "fs";
import { join } from "path";

const G_PACKAGE_PATH = "packages/g/package.json";

function checkWorkingDirectory() {
  try {
    const status = execSync("git status --porcelain").toString();
    if (status.length > 0) {
      console.error("Working directory is not clean. Please commit or stash your changes first.");
      process.exit(1);
    }
  } catch (error) {
    console.error("Failed to check git status:", error.message);
    process.exit(1);
  }
}

function bumpVersion() {
  const packageJson = JSON.parse(readFileSync(G_PACKAGE_PATH, "utf8"));
  const currentVersion = packageJson.version;
  const [major, minor, patch] = currentVersion.split(".").map(Number);
  const newVersion = `${major}.${minor}.${patch + 1}`;
  packageJson.version = newVersion;
  writeFileSync(G_PACKAGE_PATH, JSON.stringify(packageJson, null, 2) + "\n");
  return newVersion;
}

function updateDependencies() {
  try {
    execSync("npm install", { stdio: "inherit" });
  } catch (error) {
    console.error("Failed to update dependencies:", error.message);
    process.exit(1);
  }
}

function commitChanges(version) {
  try {
    execSync("git add package-lock.json packages/g/package.json");
    execSync(`git commit -m "g version ${version}"`);
    execSync(`git tag g@${version}`);
  } catch (error) {
    console.error("Failed to commit changes:", error.message);
    process.exit(1);
  }
}

function publishPackage() {
  try {
    execSync("npm publish", {
      cwd: "packages/g",
      stdio: "inherit",
    });
  } catch (error) {
    console.error("Failed to publish package:", error.message);
    process.exit(1);
  }
}

function pushChanges() {
  try {
    execSync("git push");
    execSync("git push --tags");
  } catch (error) {
    console.error("Failed to push changes:", error.message);
    process.exit(1);
  }
}

// Main release process
console.log("Starting @ndbx/g release process...");

console.log("Checking working directory...");
checkWorkingDirectory();

console.log("Bumping version...");
const newVersion = bumpVersion();
console.log(`New version: ${newVersion}`);

console.log("Updating dependencies...");
updateDependencies();

console.log("Committing changes...");
commitChanges(newVersion);

console.log("Publishing package...");
publishPackage();

console.log("Pushing changes...");
pushChanges();

console.log("Release completed successfully!");
