import bcrypt from "bcryptjs";
import type { Gallery, Membership, Profile, Project } from "./types";

const PROJECT_COLORS = [
  // https://colorbrewer2.org/#type=diverging&scheme=Spectral&n=11
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
];

const EMPTY_SVG = '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"></svg>';

// Keys in the bucket. The layout is identical to the S3 bucket this replaces,
// so objects can be copied over without renaming.
function userPath(userId: string) {
  return `users/${userId}`;
}

function userProfilePath(userId: string) {
  return `users/${userId}/profile.json`;
}

function galleryPath() {
  return `users/example/gallery.json`;
}

function userProjectFolder(userId: string, projectId: string, version = "dev") {
  if (version === "dev") {
    return `users/${userId}/${projectId}`;
  } else {
    return `users/${userId}/${projectId}/versions`;
  }
}

function userProjectPath(userId: string, projectId: string, version = "dev") {
  const fileName = version === "dev" ? "project.json" : `${version}.json`;
  return `${userProjectFolder(userId, projectId, version)}/${fileName}`;
}

export function svgPath(userId: string, projectId: string, item: string, version = "dev") {
  const fileName = `${item.replace(/:/g, "_")}.svg`;
  if (version === "dev") return `users/${userId}/${projectId}/${fileName}`;
  return `users/${userId}/${projectId}/versions/${fileName}`;
}

export function assetsPath(userId: string, projectId: string, assetId: string) {
  return `users/${userId}/${projectId}/blobs/${assetId}`;
}

export type Store = ReturnType<typeof createStore>;

export function createStore(bucket: R2Bucket, assetsUrl?: string) {
  async function readJson<T>(key: string): Promise<T | undefined> {
    const object = await bucket.get(key);
    if (!object) return undefined;
    return (await object.json()) as T;
  }

  async function writeJson(key: string, data: unknown) {
    await bucket.put(key, JSON.stringify(data, null, 2), {
      httpMetadata: { contentType: "application/json" },
    });
  }

  async function exists(key: string) {
    const head = await bucket.head(key);
    return head !== null;
  }

  async function* listKeys(prefix: string) {
    let cursor: string | undefined;
    do {
      const result = await bucket.list({ prefix, cursor });
      for (const object of result.objects) yield object;
      cursor = result.truncated ? result.cursor : undefined;
    } while (cursor);
  }

  async function deleteDirectory(prefix: string) {
    for await (const object of listKeys(prefix)) {
      await bucket.delete(object.key);
    }
  }

  // R2 has no server-side copy, so each object streams through the worker.
  async function copyDir(src: string, dst: string) {
    for await (const object of listKeys(src)) {
      const key = object.key;
      if (object.size === 0 && key.endsWith("/")) continue;
      const source = await bucket.get(key);
      if (!source) continue;
      await bucket.put(dst + key.slice(src.length), source.body, { httpMetadata: source.httpMetadata });
    }
  }

  async function listRootDir() {
    const keys: string[] = [];
    let cursor: string | undefined;
    do {
      const result = await bucket.list({ prefix: "users/", delimiter: "/", cursor });
      // Remove both the trailing '/' and the 'users/' prefix
      keys.push(...result.delimitedPrefixes.map((prefix) => prefix.slice("users/".length, -1)));
      cursor = result.truncated ? result.cursor : undefined;
    } while (cursor);
    return keys;
  }

  async function userExists(userId: string) {
    return exists(userProfilePath(userId));
  }

  async function getUserProfile(userId: string): Promise<Profile> {
    const profile = await readJson<Profile>(userProfilePath(userId));
    if (!profile) throw new Error(`Unable to read user profile for userId: ${userId}`);
    return profile;
  }

  async function saveUserProfile(userId: string, profile: Profile) {
    try {
      await writeJson(userProfilePath(userId), profile);
    } catch (e) {
      throw new Error(`Unable to save user profile for userId: ${userId}`);
    }
  }

  // The current membership lives in the profile; there is no external membership service.
  async function getMembership(_userId: string): Promise<Membership | null> {
    return null;
  }

  async function getUserProject(userId: string, projectId: string, version = "dev"): Promise<Project> {
    if (await projectExists(userId, projectId, version)) {
      const project = await readJson<Project>(userProjectPath(userId, projectId, version));
      if (!project) throw new Error(`Unable to read project for userId: ${userId}, projectId: ${projectId}`);
      if (userId === "example") {
        project.__gallery = project.__gallery || {};
      }
      return project;
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

  async function checkUserIdAndPassword(userId: string, password: string) {
    const profile = await getUserProfile(userId);
    return bcrypt.compare(password, profile.password);
  }

  async function createUser(userId: string, email: string, passwordHash: string) {
    if (await userExists(userId)) {
      throw new Error(`User "${userId}" already exists.`);
    }
    // Copy the skel profile
    await copyDir(userPath("skel") + "/", userPath(userId) + "/");
    // Update the profile
    const profile = await getUserProfile(userId);
    profile.login = userId;
    profile.email = email;
    profile.password = passwordHash;
    await saveUserProfile(userId, profile);
    if (userId === "example") {
      const project = await getUserProject(userId, "welcome");
      project.__gallery = {};
      await saveProject(userId, "welcome", project);
    }
  }

  async function findUserIdsByEmail(email: string) {
    const userIds: string[] = [];
    try {
      const keys = await listRootDir();
      for (const userId of keys) {
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

  async function resetPassword(userId: string, newPassword: string) {
    try {
      const profile = await getUserProfile(userId);
      profile.password = await bcrypt.hash(newPassword, 10);
      await saveUserProfile(userId, profile);
    } catch (err) {
      console.error(`Failed to reset password for userId: ${userId}`, err);
      throw new Error("Unable to reset password. Please try again.");
    }
  }

  async function listProjects(userId: string) {
    const profile = await getUserProfile(userId);
    return profile.projects || [];
  }

  async function projectExists(userId: string, projectId: string, version = "dev") {
    return exists(userProjectPath(userId, projectId, version));
  }

  async function saveProject(userId: string, projectId: string, project: Project, version = "dev") {
    try {
      await writeJson(userProjectPath(userId, projectId, version), project);
    } catch (e) {
      throw new Error("Unable to save project");
    }
  }

  async function updateProjectScopeInProfile(userId: string, projectId: string, scope: string) {
    try {
      const profile = await getUserProfile(userId);
      const project = profile.projects.find((p) => p.id === projectId);
      if (project) {
        project.scope = scope;
        await writeJson(userProfilePath(userId), profile);
      } else {
        throw new Error(`Project with ID ${projectId} not found`);
      }
    } catch (e) {
      throw new Error("Unable to update project scope in profile");
    }
  }

  async function updateProjectTitleInProfile(userId: string, projectId: string, projectTitle: string) {
    try {
      const profile = await getUserProfile(userId);
      const project = profile.projects.find((p) => p.id === projectId);
      if (project) {
        project.title = projectTitle;
        await writeJson(userProfilePath(userId), profile);
      } else {
        throw new Error(`Project with ID ${projectId} not found`);
      }
    } catch (e) {
      throw new Error("Unable to update project title in profile");
    }
  }

  async function updateProjectColor(userId: string, projectId: string, color: string) {
    const project = await getUserProject(userId, projectId);
    project.color = color;
    await saveProject(userId, projectId, project);
    try {
      const profile = await getUserProfile(userId);
      const projectInProfile = profile.projects.find((p) => p.id === projectId);
      if (projectInProfile) {
        projectInProfile.color = color;
        await writeJson(userProfilePath(userId), profile);
      } else {
        throw new Error(`Project with ID ${projectId} not found`);
      }
    } catch (e) {
      throw new Error("Unable to update color of project in profile");
    }
  }

  async function createProject(userId: string, projectId: string, projectTitle: string, scope: string) {
    if (await projectExists(userId, projectId)) {
      throw new Error(`Project "${projectId}" already exists.`);
    }
    const profile = await getUserProfile(userId);
    // Copy the template project
    await copyDir(userProjectPath("template", "graphics"), userProjectPath(userId, projectId));
    // Update the project to include the project id
    const project = await getUserProject(userId, projectId);
    project.id = projectId;
    project.title = projectTitle;
    project.scope = scope;
    project.color = PROJECT_COLORS[profile.projects.length % PROJECT_COLORS.length];
    if (userId === "example") {
      project.__gallery = {};
    }
    await saveProject(userId, projectId, project);
    // Update the profile file
    profile.projects.push({ id: project.id, title: project.title, color: project.color, scope: project.scope });
    await saveUserProfile(userId, profile);
  }

  async function removeProject(userId: string, projectId: string, version = "dev") {
    if (!(await projectExists(userId, projectId, version))) {
      throw new Error("Project does not exist");
    }
    await deleteDirectory(userProjectFolder(userId, projectId, version) + "/");
    const profile = await getUserProfile(userId);
    profile.projects = profile.projects.filter((project) => project.id !== projectId);
    await saveUserProfile(userId, profile);
  }

  async function publishProject(userId: string, projectId: string) {
    const project = await getUserProject(userId, projectId);
    await saveProject(userId, projectId, project);
    await saveProject(userId, projectId, project, "published");
  }

  async function loadGallery(): Promise<Gallery | undefined> {
    try {
      return await readJson<Gallery>(galleryPath());
    } catch (e) {
      throw new Error("Unable to read gallery");
    }
  }

  async function updateGallery() {
    const projectInfos = await listProjects("example");
    const galleryItems = [];
    for (const projectInfo of projectInfos) {
      const { id: projectId } = projectInfo;
      if (!(await projectExists("example", projectId, "published"))) continue;
      const project = await getUserProject("example", projectId, "published");
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
    try {
      await writeJson(galleryPath(), { items: galleryItems });
    } catch (e) {
      throw new Error("Unable to save gallery");
    }
  }

  function assetsUrlTemplate() {
    if (assetsUrl) return `${assetsUrl}/users/{{ userId }}/{{ projectId }}/blobs/{{ hash }}`;
    return "/api/assets/{{ userId }}/{{ projectId }}/blobs/{{ hash }}";
  }

  async function loadAsset(userId: string, projectId: string, assetId: string) {
    return bucket.get(assetsPath(userId, projectId, assetId));
  }

  async function saveAsset(
    userId: string,
    projectId: string,
    filename: string,
    body: ReadableStream | null,
    type: string,
  ) {
    await bucket.put(assetsPath(userId, projectId, filename), body, { httpMetadata: { contentType: type } });
  }

  async function deleteAsset(userId: string, projectId: string, assetId: string) {
    try {
      await bucket.delete(assetsPath(userId, projectId, assetId));
    } catch (e) {
      throw new Error("Unable to delete asset");
    }
  }

  async function saveGallerySvg(userId: string, projectId: string, item: string, version: string, svgContent: string) {
    try {
      await bucket.put(svgPath(userId, projectId, item, version), svgContent, {
        httpMetadata: { contentType: "image/svg+xml" },
      });
    } catch (e) {
      throw new Error("Unable to save SVG file to the gallery.");
    }
  }

  async function getGallerySvg(userId: string, projectId: string, item: string, version: string) {
    const object = await bucket.get(svgPath(userId, projectId, item, version));
    if (!object) return EMPTY_SVG;
    return object.text();
  }

  return {
    listRootDir,
    userExists,
    getUserProfile,
    saveUserProfile,
    getMembership,
    getUserProject,
    loadProject: getUserProject,
    checkUserIdAndPassword,
    createUser,
    findUserIdsByEmail,
    resetPassword,
    listProjects,
    projectExists,
    saveProject,
    updateProjectScopeInProfile,
    updateProjectTitleInProfile,
    updateProjectColor,
    createProject,
    removeProject,
    publishProject,
    loadGallery,
    updateGallery,
    assetsUrlTemplate,
    loadAsset,
    saveAsset,
    deleteAsset,
    saveGallerySvg,
    getGallerySvg,
  };
}
