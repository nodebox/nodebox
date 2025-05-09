import {
  S3Client,
  PutObjectCommand,
  ListObjectsV2Command,
  CopyObjectCommand,
  GetObjectCommand,
  HeadObjectCommand,
  DeleteObjectCommand,
} from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import bcrypt from "bcryptjs";

// Configure AWS SDK
// For DigitalOcean Spaces: https://docs.digitalocean.com/products/spaces/how-to/use-aws-sdks/
const DATA_STORE = process.env.DATA_STORE;
const AWS_ACCESS_KEY_ID = process.env.AWS_ACCESS_KEY_ID;
const AWS_SECRET_ACCESS_KEY = process.env.AWS_SECRET_ACCESS_KEY;
const AWS_REGION = "us-east-1"; // This has to be set like this even for DigitalOcean Spaces
const AWS_S3_BUCKET = process.env.AWS_S3_BUCKET;
const AWS_S3_ENDPOINT = process.env.AWS_S3_ENDPOINT;
const AWS_S3_ASSETS_URL = process.env.AWS_S3_ASSETS_URL;

if (
  DATA_STORE === "s3" &&
  !(AWS_ACCESS_KEY_ID && AWS_SECRET_ACCESS_KEY && AWS_S3_BUCKET && AWS_S3_ENDPOINT && AWS_S3_ASSETS_URL)
) {
  throw new Error("Missing AWS configuration. Check your .env settings");
}

let s3Client;
if (AWS_ACCESS_KEY_ID && AWS_SECRET_ACCESS_KEY && AWS_S3_BUCKET && AWS_S3_ENDPOINT) {
  s3Client = new S3Client({
    forcePathStyle: false, // Configures to use subdomain/virtual calling format.
    endpoint: AWS_S3_ENDPOINT,
    region: AWS_REGION,
    credentials: {
      accessKeyId: AWS_ACCESS_KEY_ID,
      secretAccessKey: AWS_SECRET_ACCESS_KEY,
    },
  });
}

async function deleteDirectory(prefix) {
  let continuationToken;
  do {
    const { Contents, NextContinuationToken } = await s3Client.send(
      new ListObjectsV2Command({
        Bucket: AWS_S3_BUCKET,
        Prefix: prefix,
        ContinuationToken: continuationToken,
      }),
    );
    continuationToken = NextContinuationToken;

    if (Contents.length === 0) break;

    for (const object of Contents) {
      await s3Client.send(
        new DeleteObjectCommand({
          Bucket: AWS_S3_BUCKET,
          Key: object.Key,
        }),
      );
    }
  } while (continuationToken);
}

async function copyDir(src, dst) {
  let continuationToken;
  do {
    const { Contents, NextContinuationToken } = await s3Client.send(
      new ListObjectsV2Command({ Bucket: AWS_S3_BUCKET, Prefix: src, ContinuationToken: continuationToken }),
    );
    continuationToken = NextContinuationToken;
    if (Contents)
      for (const object of Contents) {
        const key = object.Key;
        // Don't copy directories.
        if (object.Size === 0 && key.endsWith("/")) continue;
        const dstKey = dst + key.slice(src.length);
        await s3Client.send(
          new CopyObjectCommand({ Bucket: AWS_S3_BUCKET, CopySource: `${AWS_S3_BUCKET}/${key}`, Key: dstKey }),
        );
      }
  } while (continuationToken);
}

export async function listRootDir() {
  let keys = [];
  let continuationToken;
  do {
    const res = await s3Client.send(
      new ListObjectsV2Command({
        Bucket: AWS_S3_BUCKET,
        Prefix: "users/",
        Delimiter: "/",
        ContinuationToken: continuationToken,
      }),
    );
    continuationToken = res.NextContinuationToken;
    let prefixes = res.CommonPrefixes?.map((prefix) => prefix.Prefix) ?? [];
    // Remove both the trailing '/' and the 'users/' prefix
    prefixes = prefixes.map((prefix) => prefix.slice("users/".length, -1));
    keys.push(...prefixes);
  } while (continuationToken);
  return keys;
}

// Helper functions to get the S3 keys for different objects.
// 'path' refers in this context to keys (buckets), to facilitate comparison with functions in 'file-store.js'
function userPath(userId) {
  return `users/${userId}`;
}

function userProfilePath(userId) {
  return `users/${userId}/profile.json`;
}

function galleryPath() {
  return `users/example/gallery.json`;
}

function userProjectFolder(userId, projectId, version = "dev") {
  if (version === "dev") {
    return `users/${userId}/${projectId}`;
  } else {
    return `users/${userId}/${projectId}/versions`;
  }
}
function userProjectPath(userId, projectId, version = "dev") {
  const fileName = version === "dev" ? "project.json" : `${version}.json`;
  return `${userProjectFolder(userId, projectId, version)}/${fileName}`;
}

// Function to check if a user exists
export async function userExists(userId) {
  const filePath = userProfilePath(userId);
  try {
    await s3Client.send(
      new HeadObjectCommand({
        Bucket: AWS_S3_BUCKET,
        Key: filePath,
      }),
    );
    return true;
  } catch (err) {
    if (err.name === "NotFound") {
      return false;
    } else {
      throw err;
    }
  }
}

export async function getUserProfile(userId) {
  const filePath = userProfilePath(userId);
  try {
    const { Body } = await s3Client.send(new GetObjectCommand({ Bucket: AWS_S3_BUCKET, Key: userProfilePath(userId) }));
    const profile = JSON.parse(await Body.transformToString());
    return profile;
  } catch (e) {
    throw new Error(`Unable to read user profile for userId: ${userId}`);
  }
}

export async function saveUserProfile(userId, profile) {
  const filePath = userProfilePath(userId);
  try {
    await s3Client.send(
      new PutObjectCommand({
        Bucket: AWS_S3_BUCKET,
        Key: filePath,
        Body: JSON.stringify(profile, null, 2),
      }),
    );
  } catch (e) {
    throw new Error(`Unable to save user profile for userId: ${userId}`);
  }
}

export async function getUserProject(userId, projectId, version = "dev") {
  if (await projectExists(userId, projectId, version)) {
    const filePath = userProjectPath(userId, projectId, version);
    try {
      const { Body } = await s3Client.send(new GetObjectCommand({ Bucket: AWS_S3_BUCKET, Key: filePath }));
      const project = JSON.parse(await Body.transformToString());
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

  // Copy the skel profile
  await copyDir(userPath("skel"), userPath(userId));

  // Update the profile
  const profile = await getUserProfile(userId);
  profile.login = userId;
  profile.email = email;
  profile.password = password;

  // Write the new profile
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
    await s3Client.send(new HeadObjectCommand({ Bucket: AWS_S3_BUCKET, Key: projectPath }));
    return true;
  } catch (err) {
    if (err.name === "NotFound") {
      return false;
    } else {
      throw err;
    }
  }
}

export const loadProject = getUserProject;

export async function saveProject(userId, projectId, project, version = "dev") {
  const projectPath = userProjectPath(userId, projectId, version);
  try {
    await s3Client.send(
      new PutObjectCommand({
        Bucket: AWS_S3_BUCKET,
        Key: projectPath, //userProjectPath(userId, projectId, (version = "dev")),
        Body: JSON.stringify(project, null, 2),
      }),
    );
  } catch (e) {
    throw new Error("Unable to save project");
  }
}

export async function updateProjectScopeInProfile(userId, projectId, scope) {
  const filePath = userProfilePath(userId);
  try {
    const { Body } = await s3Client.send(new GetObjectCommand({ Bucket: AWS_S3_BUCKET, Key: filePath }));
    const profileBody = await Body.transformToString();
    const profile = JSON.parse(profileBody);
    const project = profile.projects.find((p) => p.id === projectId);
    if (project) {
      project.scope = scope;
      await s3Client.send(
        new PutObjectCommand({
          Bucket: AWS_S3_BUCKET,
          Key: filePath,
          Body: JSON.stringify(profile, null, 2),
        }),
      );
    } else {
      throw new Error(`Project with ID ${projectId} not found`);
    }
  } catch (e) {
    throw new Error("Unable to update project title in profile");
  }
}
export async function updateProjectTitleInProfile(userId, projectId, projectTitle) {
  const filePath = userProfilePath(userId);
  try {
    const { Body } = await s3Client.send(new GetObjectCommand({ Bucket: AWS_S3_BUCKET, Key: filePath }));
    const profileBody = await Body.transformToString();
    const profile = JSON.parse(profileBody);
    const project = profile.projects.find((p) => p.id === projectId);
    if (project) {
      project.title = projectTitle;
      await s3Client.send(
        new PutObjectCommand({
          Bucket: AWS_S3_BUCKET,
          Key: filePath,
          Body: JSON.stringify(profile, null, 2),
        }),
      );
    } else {
      throw new Error(`Project with ID ${projectId} not found`);
    }
  } catch (e) {
    throw new Error("Unable to update project title in profile");
  }
}

export async function updateProjectColor(userId, projectId, color) {
  const project = await getUserProject(userId, projectId);
  project.color = color;
  await saveProject(userId, projectId, project);

  const filePath = userProfilePath(userId);
  try {
    const { Body } = await s3Client.send(
      new GetObjectCommand({
        Bucket: AWS_S3_BUCKET,
        Key: filePath,
      }),
    );
    const profileBody = await Body.transformToString();
    const profile = JSON.parse(profileBody);

    const projectInProfile = profile.projects.find((p) => p.id === projectId);
    if (projectInProfile) {
      projectInProfile.color = color;
      await s3Client.send(
        new PutObjectCommand({
          Bucket: AWS_S3_BUCKET,
          Key: filePath,
          Body: JSON.stringify(profile, null, 2),
        }),
      );
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
  await copyDir(userProjectPath("template", "graphics"), userProjectPath(userId, projectId));
  // Update the project to include the project id
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
    // Optional: Handle removal of gallery-related data if applicable.
  }

  // Delete the project's directory in S3
  await deleteDirectory(file);

  // Update the user's profile in S3
  const profile = await getUserProfile(userId);
  profile.projects = profile.projects.filter((project) => project.id !== projectId);
  await saveUserProfile(userId, profile);
}

export async function publishProject(userId, projectId) {
  const project = await loadProject(userId, projectId);
  await saveProject(userId, projectId, project);
  await saveProject(userId, projectId, project, "published");
}

// export async function unPublishProject(userId, projectId) {
//   const project = await loadProject(userId, projectId);
//   project.publishDate = "";
//   await saveProject(userId, projectId, project);
//   await removeProject(userId, projectId, "published");
// }

export async function loadGallery() {
  try {
    const { Body } = await s3Client.send(new GetObjectCommand({ Bucket: AWS_S3_BUCKET, Key: galleryPath() }));
    const data = await Body.transformToString();
    return JSON.parse(data);
  } catch (e) {
    if (e.Code === "NoSuchKey") {
      return undefined;
    } else throw new Error("Unable to read gallery");
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
    await s3Client.send(
      new PutObjectCommand({
        Bucket: AWS_S3_BUCKET,
        Key: galleryPath(),
        Body: JSON.stringify(gallery, null, 2),
      }),
    );
  } catch (e) {
    throw new Error("Unable to save gallery");
  }
}

// function assetPath(userId, projectId, filename) {
//   return `users/${userId}/${projectId}/blobs/${filename}`;
// }

export function svgPath(userId, projectId, item, version = "dev") {
  let svgFilePath = "";
  if (version === "dev") svgFilePath = `users/${userId}/${projectId}/${item.replace(/:/g, "_")}.svg`;
  else svgFilePath = `users/${userId}/${projectId}/versions/${item.replace(/:/g, "_")}.svg`;

  return svgFilePath;
}

export function assetsPath(userId, projectId, assetId) {
  return `users/${userId}/${projectId}/blobs/${assetId}`;
}

export function assetsUrlTemplate() {
  return `${AWS_S3_ASSETS_URL}/{{ userId }}/{{ projectId }}/blobs/{{ hash }}`;
}

export async function loadAsset(userId, projectId, assetId) {
  const filePath = assetsPath(userId, projectId, assetId);
  try {
    const { Body } = await s3Client.send(new GetObjectCommand({ Bucket: AWS_S3_BUCKET, Key: filePath }));
    return Body;
  } catch (err) {
    console.error(err);
    throw new Error("Unable to read asset", err);
  }
}

export async function deleteAsset(userId, projectId, assetId) {
  const filePath = assetsPath(userId, projectId, assetId);
  try {
    await s3Client.send(
      new DeleteObjectCommand({
        Bucket: AWS_S3_BUCKET,
        Key: filePath,
      }),
    );
  } catch (e) {
    throw new Error("Unable to delete asset");
  }
}

export async function presignURL(userId, projectId, filename, type) {
  const filePath = assetsPath(userId, projectId, filename);
  const params = {
    Bucket: AWS_S3_BUCKET,
    Key: filePath,
    ContentType: type,
    ACL: "public-read",
  };
  const command = new PutObjectCommand(params);
  const signedUrl = await getSignedUrl(s3Client, command, { expiresIn: 3600 });
  return signedUrl;
}

export async function saveGallerySvg(userId, projectId, item, version, svgContent) {
  const svgFilePath = svgPath(userId, projectId, item, version);
  try {
    await s3Client.send(
      new PutObjectCommand({
        Bucket: AWS_S3_BUCKET,
        Key: svgFilePath,
        Body: svgContent,
        ContentType: "image/svg+xml",
      }),
    );
  } catch (e) {
    throw new Error("Unable to save SVG file to the gallery.");
  }
}

export async function getGallerySvg(userId, projectId, item, version) {
  const svgFilePath = svgPath(userId, projectId, item, version);
  try {
    const { Body } = await s3Client.send(
      new GetObjectCommand({
        Bucket: AWS_S3_BUCKET,
        Key: svgFilePath,
      }),
    );
    const svgContent = await Body.transformToString();
    return svgContent;
  } catch (e) {
    return '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"></svg>';
    // throw new Error("Unable to retrieve SVG file from the gallery.");
  }
}
