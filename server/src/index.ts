import { Hono, type Context } from "hono";
import { bodyLimit } from "hono/body-limit";
import { cors } from "hono/cors";
import { secureHeaders } from "hono/secure-headers";
import bcrypt from "bcryptjs";
import { marked } from "marked";
import { zipSync, strToU8 } from "fflate";

import {
  adminUserId,
  checkOwnershipFromBody,
  checkOwnershipFromParam,
  error,
  getOwnershipFromParam,
  signToken,
  verifyToken,
  bearerToken,
  type AppEnv,
} from "./auth";
import { sendForgotPasswordEmail } from "./email";
import { cleanFilename } from "./file-utils.js";
import { createStore } from "./store";
import { validateEmail, validatePassword, validateUsername } from "./validate.js";
import type { Membership, Project } from "./types";

import guideTemplate from "../template/guide.html";
import embedTemplate from "../template/embed.html";
import adminTemplate from "../template/admin.html";

const KB = 1024;

const app = new Hono<AppEnv>();

app.use("*", async (c, next) => {
  c.set("store", createStore(c.env.BUCKET, c.env.ASSETS_URL || undefined));
  await next();
});

// Uploads stream straight to the bucket; every other route gets the same JSON limits as before.
app.use("/api/save-svg", bodyLimit({ maxSize: 4096 * KB }));
app.use("/api/*", async (c, next) => {
  if (c.req.path.startsWith("/api/upload") || c.req.path.startsWith("/api/save-svg")) return next();
  return bodyLimit({ maxSize: 1024 * KB })(c, next);
});

// Embeds must be frameable from anywhere, so they skip the default frame headers.
app.use("*", async (c, next) => {
  if (c.req.path.startsWith("/embed/")) return next();
  return secureHeaders({ contentSecurityPolicy: undefined })(c, next);
});

// Support CORS for GET requests to the API (for embedding the NodeBoxPlayer)
app.use("/api/*", cors({ origin: "*", allowMethods: ["GET"] }));

function success(c: Context, data: object = {}, statusCode = 200) {
  return c.json({ status: "ok", ...data }, statusCode as 200);
}

function renderTemplate(template: string, data: Record<string, string | undefined>) {
  return template.replace(/{{\s*(\w+)\s*}}/g, (_, key) => data[key] || "");
}

function membershipHasExpired(membership: Membership | null | undefined) {
  if (!membership?.membership_until) return false;
  return new Date() > new Date(membership.membership_until);
}

app.get("/guide/", (c) => c.redirect("/guide/welcome", 301));

app.get("/guide/:slug", async (c) => {
  const { slug } = c.req.param();
  let text: string;
  let statusCode = 200;
  // A missing markdown file comes back as the SPA index page, so check the type as well as the status.
  const res = await c.env.ASSETS.fetch(new URL(`/_guide/${slug}.md`, c.req.url));
  if (res.ok && !(res.headers.get("content-type") ?? "").includes("text/html")) {
    text = await res.text();
  } else {
    statusCode = 404;
    text = `# Page not found\n\nThe page "${slug}" does not exist.\n\n[Back to the guide](/guide/welcome)`;
  }
  // Get out the first H1 title
  text = text.trim();
  const titleMatch = text.match(/^#\s+(.*)/m);
  text = text.substring(titleMatch ? titleMatch.index! + titleMatch[0].length : 0);
  const title = titleMatch ? titleMatch[1] : "NodeBox Guide";
  const content = await marked.parse(text);
  return c.html(renderTemplate(guideTemplate, { title, content }), statusCode as 200);
});

// Login
app.post("/api/auth/login", async (c) => {
  const store = c.var.store;
  let { userId, password } = await c.req.json<{ userId?: string; password?: string }>();
  if (!userId || !password) {
    return error(c, "Missing user ID or password", 400);
  }
  userId = userId.toLowerCase();

  const userExists = await store.userExists(userId);
  if (!userExists) {
    return error(c, "Invalid user ID or password", 403);
  }
  try {
    const valid = await store.checkUserIdAndPassword(userId, password);
    if (!valid) return error(c, "Invalid user ID or password", 403);

    let membership = await store.getMembership(userId);
    const token = await signToken({ userId, membership }, c.env.JWT_SECRET);

    const userProfile = await store.getUserProfile(userId);
    userProfile.membership_message = "";
    if (membership === null) {
      membership = {
        membership_type: userProfile.membership_type || "free",
        membership_until: userProfile.membership_until || "",
      };
    } else if (membership && typeof membership === "object") {
      const membershipUntil = new Date(membership.membership_until ?? "");
      if (new Date() > membershipUntil) {
        membership.membership_type = "free";
        userProfile.membership_message = "Your premium subscription has ended on " + membershipUntil.toDateString();
      } else if (userProfile.membership_type !== membership.membership_type) {
        switch (membership.membership_type) {
          case "free":
            userProfile.membership_message = "Your premium subscription has ended.";
            break;
          case "plus":
            userProfile.membership_message = "Your premium subscription has been activated.";
            break;
        }
      }
      userProfile.membership_type = membership.membership_type || "free";
      userProfile.membership_until = membership.membership_until || "";
    }
    await store.saveUserProfile(userId, userProfile);
    return success(c, { token, membership, message: userProfile.membership_message });
  } catch (err) {
    return error(c, "Invalid user ID or password", 403);
  }
});

// Signup
app.post("/api/auth/signup", async (c) => {
  const store = c.var.store;
  let { userId, email, password } = await c.req.json<{ userId?: string; email: string; password: string }>();
  userId = userId?.toLowerCase() ?? "";
  if (await store.userExists(userId)) {
    return error(c, "User ID already exists", 400);
  }
  try {
    validateUsername(userId);
    validateEmail(email);
    validatePassword(password);
  } catch (err) {
    return error(c, (err as Error).message, 400);
  }
  const passwordHash = await bcrypt.hash(password, 10);
  try {
    await store.createUser(userId, email, passwordHash);
    const membership = await store.getMembership(userId);
    const token = await signToken({ userId, membership }, c.env.JWT_SECRET);
    return success(c, { token });
  } catch (err) {
    return error(c, (err as Error).message, 400);
  }
});

// Check authorization
app.post("/api/auth/check", async (c) => {
  const token = bearerToken(c);
  if (!token) return error(c, "Missing authorization header", 401);
  const decoded = await verifyToken(token, c.env.JWT_SECRET);
  if (!decoded) return error(c, "Invalid authorization token", 401);
  return success(c, { userId: decoded.userId });
});

app.post("/api/auth/find-by-email", async (c) => {
  const { email } = await c.req.json<{ email: string }>();
  const userIds = await c.var.store.findUserIdsByEmail(email);
  return success(c, { userIds });
});

app.post("/api/auth/forgot-password", async (c) => {
  const ONE_HOUR = 3600 * 1000; // Token validity period
  const store = c.var.store;
  const { userIdOrEmail } = await c.req.json<{ userIdOrEmail?: string }>();
  if (!userIdOrEmail) {
    return error(c, "Missing user ID or email");
  }

  if (userIdOrEmail.match(/^[a-zA-Z0-9]{3,20}$/)) {
    // User ID logic
    try {
      const userId = userIdOrEmail;
      if (!(await store.userExists(userId))) return success(c);
      const profile = await store.getUserProfile(userId);
      const membership = await store.getMembership(userId);
      const token = await signToken({ userId, membership, bestBefore: Date.now() + ONE_HOUR }, c.env.JWT_SECRET);
      c.executionCtx.waitUntil(sendForgotPasswordEmail(c.env.EMAIL, profile.email, userId, token));
      return success(c);
    } catch (e) {
      console.error(e);
      return error(c, `Something went wrong: ${(e as Error).message}`, 500);
    }
  } else if (userIdOrEmail.match(/^(.*)@(.*)$/)) {
    // Email logic
    try {
      const email = userIdOrEmail;
      const userIds = await store.findUserIdsByEmail(email);
      for (const userId of userIds) {
        const membership = await store.getMembership(userId);
        const token = await signToken({ userId, membership, bestBefore: Date.now() + ONE_HOUR }, c.env.JWT_SECRET);
        c.executionCtx.waitUntil(sendForgotPasswordEmail(c.env.EMAIL, email, userId, token));
      }
      return success(c);
    } catch (e) {
      console.error(e);
      return error(c, (e as Error).message, 500);
    }
  } else {
    return error(c, "Invalid user ID or email");
  }
});

app.post("/api/auth/reset-password", async (c) => {
  const { userId, password, token } = await c.req.json<{ userId: string; password: string; token: string }>();
  const valid = await verifyToken(token, c.env.JWT_SECRET);
  if (!valid) return error(c, "Invalid token", 400);
  if (valid.userId !== userId) return error(c, "Invalid user ID", 400);
  if (!valid.bestBefore || valid.bestBefore < Date.now()) return error(c, "Token expired", 400);
  if (password.length < 8) return error(c, "Password must be at least 8 characters long", 400);
  try {
    await c.var.store.resetPassword(userId, password);
    return success(c);
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// Load svg file
app.get("/api/svg/:userId/:projectId/:item", async (c) => {
  const { userId, projectId, item } = c.req.param();
  const svgContent = await c.var.store.getGallerySvg(userId, projectId, item, "published");
  if (!svgContent) return c.body(null, 204);
  return c.body(svgContent, 200, { "Content-Type": "image/svg+xml" });
});

app.post("/api/save-svg", checkOwnershipFromBody, async (c) => {
  const { userId, projectId, item, version, svgContent } = await c.req.json();
  if (userId !== "example") return c.json({ error: "Invalid user." }, 400);
  if (!projectId || !svgContent) {
    return c.json({ error: "Filename and SVG content are required" }, 400);
  }
  try {
    await c.var.store.saveGallerySvg(userId, projectId, item, version, svgContent);
    return success(c, { status: "saved" });
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// List projects for user
app.get("/api/projects/:userId", async (c) => {
  const store = c.var.store;
  const userId = c.req.param("userId");
  const authUserId = c.req.query("userId");
  try {
    if (!(await store.userExists(userId))) {
      return success(c, { projects: [], userId: null });
    }
    const profile = await store.getUserProfile(userId);
    if (authUserId !== userId) {
      return success(c, { projects: profile.projects.filter((project) => project?.scope !== "private") });
    } else {
      return success(c, { projects: profile.projects, membership_message: profile.membership_message });
    }
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// Get membership details for user from profile
app.get("/api/membership/:userId", async (c) => {
  try {
    const profile = await c.var.store.getUserProfile(c.req.param("userId"));
    return success(c, { membership_type: profile.membership_type, membership_until: profile.membership_until });
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// Create new project
app.post("/api/projects", checkOwnershipFromBody, async (c) => {
  const { userId, projectId, projectTitle, public: scope } = await c.req.json();
  try {
    await c.var.store.createProject(userId, projectId, projectTitle, scope);
    return success(c, { userId, projectId });
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// Check if project exists (the client sends HEAD; Hono answers HEAD through the GET handler)
app.get("/api/projects/:userId/:projectId", async (c) => {
  const { userId, projectId } = c.req.param();
  if (await c.var.store.projectExists(userId, projectId)) return success(c);
  return error(c, "Project not found", 404);
});

// Update scope in profile overview
app.post("/api/set-scope/:userId/:projectId", checkOwnershipFromParam, async (c) => {
  const { userId, projectId } = c.req.param();
  const body = await c.req.json<{ scope: string }>();
  // Overrule scope if membership has expired through token.
  if (membershipHasExpired(c.var.membership)) body.scope = "public";
  try {
    await c.var.store.updateProjectScopeInProfile(userId, projectId, body.scope);
    return success(c);
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// Update project title in overview
app.post("/api/set-title/:userId/:projectId", checkOwnershipFromParam, async (c) => {
  const { userId, projectId } = c.req.param();
  const body = await c.req.json<{ title: string }>();
  try {
    await c.var.store.updateProjectTitleInProfile(userId, projectId, body.title);
    return success(c);
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// Update project color in overview
app.post("/api/set-project-color/:userId/:projectId", checkOwnershipFromParam, async (c) => {
  const { userId, projectId } = c.req.param();
  const body = await c.req.json<{ color: string }>();
  try {
    await c.var.store.updateProjectColor(userId, projectId, body.color);
    return success(c);
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// Load project
app.get("/api/projects/:userId/:projectId/:version", getOwnershipFromParam, async (c) => {
  const store = c.var.store;
  const { userId, projectId, version } = c.req.param();
  try {
    const project = await store.loadProject(userId, projectId, version);
    if (project && project.scope == "private" && userId !== c.var.authUserId) {
      return error(c, "Project not accesible");
    }
    return success(c, { assetsUrlTemplate: store.assetsUrlTemplate(new URL(c.req.url).origin), project });
  } catch (e) {
    return error(c, (e as Error).message, 404);
  }
});

// Load published project directly
app.get("/api/published/:userId/:projectId", async (c) => {
  const { userId, projectId } = c.req.param();
  try {
    const project = await c.var.store.loadProject(userId, projectId, "published");
    return c.json(project);
  } catch (e) {
    return error(c, (e as Error).message, 404);
  }
});

// Save project
app.post("/api/projects/:userId/:projectId", checkOwnershipFromParam, async (c) => {
  const { userId, projectId } = c.req.param();
  const project = await c.req.json<Project>();
  try {
    await c.var.store.saveProject(userId, projectId, project);
    return success(c);
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// Delete project
app.delete("/api/projects/:userId/:projectId", checkOwnershipFromParam, async (c) => {
  const store = c.var.store;
  const { userId, projectId } = c.req.param();
  if (await store.projectExists(userId, projectId)) {
    try {
      await store.removeProject(userId, projectId);
      return success(c, { userId, projectId });
    } catch (e) {
      return error(c, (e as Error).message);
    }
  } else {
    try {
      const profile = await store.getUserProfile(userId);
      profile.projects = profile.projects.filter((project) => project.id !== projectId);
      await store.saveUserProfile(userId, profile);
      return success(c, { message: "Project deleted successfully" });
    } catch (e) {
      return error(c, "Failed to delete project from profile.", 500);
    }
  }
});

// Publish project
app.post("/api/projects/:userId/:projectId/publish", checkOwnershipFromParam, async (c) => {
  const store = c.var.store;
  const { userId, projectId } = c.req.param();
  try {
    await store.publishProject(userId, projectId);
    if (userId === "example") c.executionCtx.waitUntil(store.updateGallery());
    return success(c);
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// Uploads go through the worker into the bucket, so the client gets a worker URL instead of a presigned one.
app.post("/api/assets/presign/:userId/:projectId", checkOwnershipFromParam, async (c) => {
  const { userId, projectId } = c.req.param();
  const { filename: dirtyFilename } = await c.req.json<{ filename: string; type: string }>();
  let filename;
  try {
    filename = cleanFilename(dirtyFilename);
  } catch (e) {
    return error(c, (e as Error).message, 400);
  }
  const url = `/api/upload/${userId}/${projectId}`;
  return success(c, { method: "POST", url, filename });
});

// Load asset
app.get("/api/assets/:userId/:projectId/blobs/:assetId", async (c) => {
  const { userId, projectId, assetId } = c.req.param();
  try {
    const object = await c.var.store.loadAsset(userId, projectId, assetId);
    if (!object) return error(c, `Asset not found: ${assetId}`, 404);
    return c.body(object.body, 200, { "Content-Type": "application/octet-stream" });
  } catch (e) {
    return error(c, (e as Error).message, 500);
  }
});

// Upload asset
app.post("/api/upload/:userId/:projectId", checkOwnershipFromParam, async (c) => {
  const { userId, projectId } = c.req.param();
  const contentType = c.req.header("content-type");
  const filename = c.req.header("filename");
  if (!contentType || !filename) {
    return error(c, "Missing content-type or filename header", 400);
  }
  const targetFilename = cleanFilename(filename);
  try {
    await c.var.store.saveAsset(userId, projectId, targetFilename, c.req.raw.body, contentType);
    return success(c, { filename: targetFilename });
  } catch (err) {
    console.error(err);
    return error(c, (err as Error).message, 500);
  }
});

app.delete("/api/assets/:userId/:projectId/:assetId", checkOwnershipFromParam, async (c) => {
  const { userId, projectId, assetId } = c.req.param();
  try {
    await c.var.store.deleteAsset(userId, projectId, assetId);
    return success(c);
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// Load a function as a JavaScript module. This is used by the web app for dynamic imports.
async function functionModule(c: any, userId: string, projectId: string, functionName: string) {
  try {
    const project = await c.var.store.loadProject(userId, projectId);
    const item = project.items?.find((item: any) => item.name === functionName);
    if (!item || item.type !== "FUNCTION") {
      const items = project.items?.map((item: any) => item.name).join(", ");
      return c.text(
        `// The function "${functionName}" does not exist in the project.\n// Valid functions are: ${items}\n// Use like this:\n//\n//   import { myHelperFunction } from "project:MyFunction";\n`,
        404,
        { "Content-Type": "application/javascript" },
      );
    }
    return c.text(item.source, 200, { "Content-Type": "application/javascript" });
  } catch (e) {
    return error(c, (e as Error).message);
  }
}

app.get("/api/fn/:userId/:projectId/:filename{.+\\.js}", (c) => {
  const { userId, projectId, filename } = c.req.param();
  const functionName = filename
    .replace(/\.js$/, "")
    .replace(/-/g, " ")
    .replace(/\b\w/g, (char) => char.toUpperCase());
  return functionModule(c, userId, projectId, functionName);
});

app.get("/api/fn/:userId/:projectId/:functionName", (c) => {
  const { userId, projectId, functionName } = c.req.param();
  return functionModule(c, userId, projectId, functionName);
});

// Download a ZIP file containing all files to self-host a project.
// This includes the "core" user's published and lib directories, as well as the "version" directory of the given userId/projectId.
app.get("/api/download-published/:userId/:projectId", async (c) => {
  const store = c.var.store;
  const { userId, projectId } = c.req.param();
  const files: Record<string, Uint8Array> = {};

  try {
    // 1. Load the main project
    const mainProject = await store.loadProject(userId, projectId, "published");
    files[`${userId}/${projectId}/versions/published.json`] = strToU8(JSON.stringify(mainProject, null, 2));

    // 2. Process assets for the main project
    if (mainProject.assets) {
      for (const [filename, assetId] of Object.entries(mainProject.assets as Record<string, string>)) {
        try {
          const object = await store.loadAsset(userId, projectId, assetId);
          if (!object) throw new Error("Asset not found");
          files[`${userId}/${projectId}/blobs/${filename}`] = new Uint8Array(await object.arrayBuffer());
        } catch (e) {
          console.warn(`Failed to load asset ${assetId} for ${userId}/${projectId}: ${(e as Error).message}`);
          files[`${userId}/${projectId}/blobs/${filename}.error.txt`] = strToU8(
            `Error loading asset: ${(e as Error).message}`,
          );
        }
      }
    }

    // 3. Process dependencies
    const processedDependencies = new Set<string>();
    if (mainProject.dependencies) {
      for (const depKey of Object.keys(mainProject.dependencies)) {
        if (processedDependencies.has(depKey)) continue;
        processedDependencies.add(depKey);

        const [depUserId, depProjectId] = depKey.split("/");
        if (!depUserId || !depProjectId) {
          console.warn(`Invalid dependency key: ${depKey}`);
          continue;
        }

        try {
          const depProject = await store.loadProject(depUserId, depProjectId, "published");
          files[`${depUserId}/${depProjectId}/versions/published.json`] = strToU8(JSON.stringify(depProject, null, 2));

          // 4. Handle Utilities for core/g and core/plot
          if (depUserId === "core" && (depProjectId === "g" || depProjectId === "plot")) {
            const utilitiesItem = depProject.items?.find((item: any) => item.name === "Utilities");
            if (utilitiesItem && utilitiesItem.source) {
              files[`${depUserId}/${depProjectId}/lib/utilities.js`] = strToU8(utilitiesItem.source);
            } else {
              console.warn(`Utilities not found or no source for ${depKey}`);
              files[`${depUserId}/${depProjectId}/lib/utilities.notfound.txt`] = strToU8(
                `Utilities not found for ${depKey}`,
              );
            }
          }
        } catch (e) {
          console.warn(`Failed to load dependency project ${depKey}: ${(e as Error).message}`);
          files[`${depUserId}/${depProjectId}/versions/published.error.txt`] = strToU8(
            `Error loading dependency ${depKey}: ${(e as Error).message}`,
          );
        }
      }
    }

    const zip = zipSync(files, { level: 9 });
    return c.body(zip, 200, {
      "Content-Type": "application/zip",
      "Content-Disposition": `attachment; filename="${userId}-${projectId}-project.zip"`,
    });
  } catch (e) {
    console.error(`Error creating ZIP for ${userId}/${projectId}:`, e);
    return error(c, `Failed to generate project ZIP: ${(e as Error).message}`);
  }
});

app.get("/api/gallery/:keyword", async (c) => {
  const store = c.var.store;
  const keyword = c.req.param("keyword");
  const gallery = await store.loadGallery();
  if (gallery === undefined) {
    return success(c, { items: [], description: "Sorry, no examples available." });
  }
  try {
    const project = await store.loadProject("example", keyword, "published");
    const items = gallery.items.filter((i) => i.keyword?.toLowerCase() === keyword.toLowerCase());
    return success(c, { items, description: project?.description });
  } catch (e) {
    return success(c, { items: [], description: "Sorry, no examples available." });
  }
});

app.get("/embed/:userId/:projectId/:item?", async (c) => {
  const { userId, projectId, item } = c.req.param();
  const data = { userId, projectId, item: item || "__undefined__", version: "published" };
  if (!(await c.var.store.projectExists(data.userId, data.projectId, data.version))) {
    return c.text("Project not found or not published.", 404);
  }
  return c.html(renderTemplate(embedTemplate, data), 200, { "Content-Security-Policy": "frame-ancestors *" });
});

app.get("/admin", (c) => c.html(renderTemplate(adminTemplate, {})));

app.get("/api/admin/current-user", async (c) => {
  const userId = await adminUserId(c);
  if (!userId) return error(c, "Not logged in", 401);
  return success(c, { userId });
});

app.post("/admin/reset-password", async (c) => {
  const store = c.var.store;
  if (!(await adminUserId(c))) return error(c, "Not logged in", 401);
  const { userIdOrEmail, newPassword, confirmPassword } = await c.req.json<{
    userIdOrEmail: string;
    newPassword: string;
    confirmPassword: string;
  }>();
  let userId: string;
  if (userIdOrEmail.match(/^[a-zA-Z0-9]{3,20}$/)) {
    userId = userIdOrEmail;
  } else if (userIdOrEmail.match(/^(.*)@(.*)$/)) {
    const userIds = await store.findUserIdsByEmail(userIdOrEmail);
    if (userIds.length === 0) return error(c, "User not found", 404);
    userId = userIds[0];
  } else {
    return error(c, "Invalid user ID or email");
  }
  if (newPassword.length < 6) return error(c, "Password must be at least 8 characters long", 400);
  if (newPassword !== confirmPassword) return error(c, "Passwords do not match");
  try {
    await store.resetPassword(userId, newPassword);
    return success(c, { message: `Password for user ${userId} reset successfully` });
  } catch (e) {
    return error(c, (e as Error).message);
  }
});

// Everything else is the web app: static files, with index.html as the SPA fallback.
app.all("*", (c) => c.env.ASSETS.fetch(c.req.raw));

export default app;
