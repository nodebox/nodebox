import fs from "fs/promises";
import { createReadStream } from "fs";
import path from "path";
import { copy } from "fs-extra";
import bcrypt from "bcryptjs";

const DATA_ROOT = path.resolve("data");

async function deleteDirectory(directory) {
  try {
    await fs.rm(directory, { recursive: true, force: true });
  } catch (e) {
    throw new Error(`Unable to delete directory ${directory}`);
  }
}

async function ensureDirectory(path) {
  try {
    await fs.mkdir(path, { recursive: true });
  } catch (e) {
    if (e.code !== "EEXIST") {
      throw e;
    }
  }
}

// Helper functions to get the paths for different objects on local file server.

function userPath(userId) {
  return path.join(DATA_ROOT, userId);
}
function userProfilePath(userId) {
  return path.join(DATA_ROOT, userId, "profile.json");
}

function galleryPath() {
  return path.join(DATA_ROOT, "example", "gallery.json");
}

function userProjectFolder(userId, projectId, version = "dev") {
  if (version === "dev") {
    return path.join(DATA_ROOT, userId, projectId);
  } else {
    return path.join(DATA_ROOT, userId, projectId, "versions");
  }
}
function userProjectPath(userId, projectId, version = "dev") {
  const fileName = version === "dev" ? "project.json" : `${version}.json`;
  return path.join(userProjectFolder(userId, projectId, version), fileName);
}

export async function userExists(userId) {
  const filePath = userProfilePath(userId);
  try {
    await fs.access(filePath);
    return true;
  } catch (e) {
    return false;
  }
}
export async function getUserProfile(userId) {
  const filePath = userProfilePath(userId);
  try {
    const profileData = await fs.readFile(filePath, "utf8");
    const profile = JSON.parse(profileData);
    return profile;
  } catch (e) {
    throw new Error(`Unable to read user profile for userId: ${userId}`);
  }
}

export async function saveUserProfile(userId, profile) {
  const filePath = userProfilePath(userId);
  try {
    await fs.writeFile(filePath, JSON.stringify(profile, null, 2), "utf8");
  } catch (e) {
    throw new Error(`Unable to save user profile for userId: ${userId}`);
  }
}

export async function getUserProject(userId, projectId, version = "dev") {
  if (await projectExists(userId, projectId, version)) {
    const filePath = userProjectPath(userId, projectId, version);
    try {
      const data = await fs.readFile(filePath, "utf8");
      let project = JSON.parse(data);
      if (userId === "example") {
        project.__gallery = project.__gallery || {};
      }
      return project;
    } catch (e) {
      throw new Error(`Unable to read project for userId: ${userId}, projectId: ${projectId}`);
    }
  } else {
    // If this was the "published" version, do a check to see if the dev version exists.
    // Then we can show a better error.
    if (version === "published" && (await projectExists(userId, projectId, "dev"))) {
      throw new Error(`Project "${projectId}" exists, but has no published version.`);
    } else {
      throw new Error(`Project "${projectId}" does not exist.`);
    }
  }
}

export async function checkUserIdAndPassword(userId, password) {
  const profile = await getUserProfile(userId);
  const passwordHash = profile.password;
  const success = await bcrypt.compare(password, passwordHash);
  return success;
}

export async function createUser(userId, email, password) {
  if (await userExists(userId)) {
    throw new Error(`User "${userId}" already exists.`);
  }
  // Copy the skel profile.
  const skel = userPath("skel");
  const userRoot = userPath(userId);
  await copy(skel, userRoot);
  // Update the profile.
  const profile = await getUserProfile(userId);
  profile.login = userId;
  profile.email = email;
  profile.password = password;
  // Write the new profile.
  await saveUserProfile(userId, profile);

  if (userId === "example") {
    const project = await getUserProject(userId, "welcome");
    project.__gallery = {};
    await saveProject(userId, "welcome", project);
  }
}

export async function findUserIdsByEmail(email) {
  const userIds = [];
  try {
    const keys = await fs.readdir(DATA_ROOT, { withFileTypes: true });
    const dirs = keys.filter((dirent) => dirent.isDirectory()).map((dirent) => dirent.name);
    for (const userId of dirs) {
      try {
        const profile = await getUserProfile(userId);
        if (profile.email === email) {
          userIds.push(profile.login);
        }
      } catch (err) {
        console.error(`Error retrieving profile for userId: ${userId}`, err);
        continue;
      }
    }
  } catch (err) {
    console.error("Error listing user directories", err);
  }
  return userIds;
}

export async function resetPassword(userId, newPassword) {
  try {
    const profile = await getUserProfile(userId);
    const passwordHash = await bcrypt.hash(newPassword, 10);
    profile.password = passwordHash;
    await saveUserProfile(userId, profile);
  } catch (err) {
    console.error(`Failed to reset password for userId: ${userId}`, err);
    throw new Error("Unable to reset password. Please try again.");
  }
}

export async function listProjects(userId) {
  const profile = await getUserProfile(userId);
  return profile.projects || [];
}

export async function projectExists(userId, projectId, version = "dev") {
  const projectPath = userProjectPath(userId, projectId, version);
  try {
    await fs.access(projectPath);
    return true;
  } catch (e) {
    return false;
  }
}

export const loadProject = getUserProject;

export async function saveProject(userId, projectId, project, version = "dev") {
  const projectPath = userProjectPath(userId, projectId, version);
  try {
    if (version !== "dev") {
      await ensureDirectory(path.join(DATA_ROOT, userId, projectId, "versions"));
    }
    await fs.writeFile(projectPath, JSON.stringify(project, null, 2));
  } catch (e) {
    throw new Error("Unable to save project");
  }
}

export async function updateProjectScopeInProfile(userId, projectId, scope) {
  const filePath = userProfilePath(userId);
  try {
    const profileBody = await fs.readFile(filePath, "utf8");
    const profile = JSON.parse(profileBody);
    const project = profile.projects.find((p) => p.id === projectId);
    if (project) {
      project.scope = scope;
      await fs.writeFile(filePath, JSON.stringify(profile, null, 2));
    } else {
      throw new Error(`Project with ID ${projectId} not found`);
    }
  } catch (e) {
    throw new Error("Unable to update project scope in profile");
  }
}

export async function updateProjectTitleInProfile(userId, projectId, projectTitle) {
  const filePath = userProfilePath(userId);
  try {
    const profileBody = await fs.readFile(filePath, "utf8");
    const profile = JSON.parse(profileBody);
    const project = profile.projects.find((p) => p.id === projectId);
    if (project) {
      project.title = projectTitle;
      await fs.writeFile(filePath, JSON.stringify(profile, null, 2));
    } else {
      throw new Error(`Project with ID ${projectId} not found.`);
    }
  } catch (e) {
    throw new Error("Unable to update project title in profile.");
  }
}

export async function updateProjectColor(userId, projectId, color) {
  const project = await getUserProject(userId, projectId);
  project.color = color;
  await saveProject(userId, projectId, project);

  const filePath = userProfilePath(userId);
  try {
    const profileBody = await fs.readFile(filePath, "utf8");
    const profile = JSON.parse(profileBody);
    const project = profile.projects.find((p) => p.id === projectId);
    if (project) {
      project.color = color;
      await fs.writeFile(filePath, JSON.stringify(profile, null, 2));
    } else {
      throw new Error(`Project with ID ${projectId} not found`);
    }
  } catch (e) {
    throw new Error("Unable to update color of project in profile");
  }
}

export async function createProject(userId, projectId, projectTitle, scope) {
  if (await projectExists(userId, projectId)) {
    throw new Error(`Project "${projectId}" already exists.`);
  }
  const profile = await getUserProfile(userId);
  // Copy the template project
  const userRoot = userPath(userId);
  await ensureDirectory(userRoot);
  const projectRoot = path.join(userRoot, projectId);
  await copy(path.join(DATA_ROOT, "template", "graphics"), projectRoot);
  // Update the project ot include the project id
  const project = await getUserProject(userId, projectId);
  project.id = projectId;
  project.title = projectTitle;
  project.scope = scope;
  // https://colorbrewer2.org/#type=diverging&scheme=Spectral&n=11
  project.color = [
    "#9e0142",
    "#d53e4f",
    "#f46d43",
    "#fdae61",
    "#fee08b",
    "#ffffbf",
    "#e6f598",
    "#abdda4",
    "#66c2a5",
    "#3288bd",
    "#5e4fa2",
  ][profile.projects.length % 11];
  if (userId === "example") {
    project.__gallery = {};
  }

  await saveProject(userId, projectId, project);
  // Update the profile file
  profile.projects.push({ id: project.id, title: project.title, color: project.color, scope: project.scope });
  await saveUserProfile(userId, profile);
}

export async function removeProject(userId, projectId, version = "dev") {
  if (!(await projectExists(userId, projectId, version))) {
    throw new Error("Project does not exist");
  }
  const file = userProjectFolder(userId, projectId, version);
  if (userId === "example") {
    // remove project.__gallery = {};
  }
  await deleteDirectory(file);
  // Update the profile file
  const profile = await getUserProfile(userId);
  profile.projects = profile.projects.filter((project) => project.id !== projectId);
  await saveUserProfile(userId, profile);
}

export async function publishProject(userId, projectId) {
  const project = await loadProject(userId, projectId);
  await saveProject(userId, projectId, project);
  await saveProject(userId, projectId, project, "published");

  //update overview
  const filePath = userProfilePath(userId);
  try {
    const profileBody = await fs.readFile(filePath, "utf8");
    const profile = JSON.parse(profileBody);
    const project = profile.projects.find((p) => p.id === projectId);
    if (project) {
      await fs.writeFile(filePath, JSON.stringify(profile, null, 2));
    } else {
      throw new Error(`Project with ID ${projectId} not found.`);
    }
  } catch (e) {
    throw new Error("Unable to update published state in profile.");
  }
}

// export async function unPublishProject(userId, projectId) {
//   const project = await loadProject(userId, projectId);
//   await saveProject(userId, projectId, project);
//   await removeProject(userId, projectId, "published");
// }

export async function loadGallery() {
  try {
    const data = await fs.readFile(galleryPath(), "utf8");
    return JSON.parse(data);
  } catch (e) {
    throw new Error("Unable to read gallery");
  }
}

export async function updateGallery() {
  const projectInfos = await listProjects("example");
  const galleryItems = [];
  for (const projectInfo of projectInfos) {
    const { id: projectId } = projectInfo;
    if (!(await projectExists("example", projectId, "published"))) continue;
    const project = await loadProject("example", projectId, "published");
    for (const item of project.items) {
      if (item.description !== "") {
        galleryItems.push({
          projectId,
          itemId: item.id,
          name: item.name,
          description: item.description,
          category: item.category,
          keyword: item.__gallery?.keyword,
          subKeyword: item.__gallery?.subKeyword,
        });
      }
    }
  }

  const gallery = { items: galleryItems };
  try {
    await fs.writeFile(galleryPath(), JSON.stringify(gallery, null, 2));
  } catch (e) {
    throw new Error("Unable to save gallery");
  }
}

// function assetPath(userId, projectId, filename) {
//   return path.join("users", userId, projectId, "blobs", filename);
// }

export function svgPath(userId, projectId, item, version = "dev") {
  let svgFilePath = "";
  if (version === "dev") {
    svgFilePath = path.join(DATA_ROOT, userId, projectId, `${item.replace(/:/g, "_")}.svg`);
  } else {
    svgFilePath = path.join(DATA_ROOT, userId, projectId, "versions", `${item.replace(/:/g, "_")}.svg`);
  }
  return svgFilePath;
}
export function assetsPath(userId, projectId, assetId) {
  return path.join(DATA_ROOT, userId, projectId, "blobs", assetId);
}

export function assetsRoot() {
  return "/api/assets";
}

export function loadAsset(userId, projectId, assetId) {
  const filePath = assetsPath(userId, projectId, assetId);
  try {
    return createReadStream(filePath);
  } catch (err) {
    throw new Error("Unable to read asset", err);
  }
}

export async function deleteAsset(userId, projectId, assetId) {
  const filePath = assetsPath(userId, projectId, assetId);
  try {
    await fs.unlink(filePath);
  } catch (e) {
    throw new Error("Unable to delete asset");
  }
}
export async function saveGallerySvg(userId, projectId, item, version, svgContent) {
  const svgFilePath = svgPath(userId, projectId, item, version);
  try {
    await fs.writeFile(svgFilePath, svgContent, "utf8");
  } catch (e) {
    throw new Error("Unable to save SVG file to the gallery.");
  }
}
export async function getGallerySvg(userId, projectId, item, version) {
  const svgFilePath = svgPath(userId, projectId, item, version);
  try {
    const svgContent = await fs.readFile(svgFilePath, "utf8");
    return svgContent;
  } catch (e) {
    return '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"></svg>'; // Default SVG if not found
  }
}
