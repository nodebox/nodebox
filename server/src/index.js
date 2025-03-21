import "dotenv/config";
import express from "express";
import jwt from "jsonwebtoken";
import path from "path";
import fs from "fs";
import { readFile } from "fs/promises";
import { fileURLToPath } from "url";
import compression from "compression";
import helmet from "helmet";
import bcrypt from "bcryptjs";
import cors from "cors";
import { marked, use } from "marked";

import { sendForgotPasswordEmail } from "./email.js";
import * as fileStore from "./file-store.js";
import { cleanFilename } from "./file-utils.js";
import * as s3Store from "./s3-store.js";
import { validateEmail, validatePassword, validateUsername } from "./validate.js";
import { verify } from "crypto";
import { callbackPromise } from "nodemailer/lib/shared/index.js";

const USERS_PUBLIC_SCOPE = ["example", "core"];
const ADMIN_USERS = ["fdb"];

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Check that JWT_SECRET is set
if (!process.env.JWT_SECRET) {
  console.error("ERROR: JWT_SECRET is not set. Please set it in .env and restart.");
  process.exit(-1);
}
if (!process.env.DATA_STORE) {
  console.error('ERROR: DATA_STORE is not set. Please set it to "file" or "s3" in .env and restart.');
  process.exit(-1);
}
const store = process.env.DATA_STORE === "s3" ? s3Store : fileStore;

const app = express();

const getMembership = async (userId) => {
  try {
    const membership = await store.getMembership(userId);
    return membership;
  } catch (e) {
    return null;
  }
};

// Middleware for parsing JSON bodies, except for the /upload route
app.use((req, res, next) => {
  if (req.path.startsWith("/api/upload")) {
    // Skip JSON parsing for uploads
    next();
  } else if (req.path.startsWith("/api/save-svg")) {
    express.json({ limit: "4096kb" })(req, res, next);
  } else {
    express.json({ limit: "1024kb" })(req, res, next);
  }
});
app.use(compression()); // Middleware for gzip compression
app.use(
  // Middleware for setting various HTTP headers
  helmet({
    contentSecurityPolicy: false,
  }),
);
app.use(express.static(path.join(__dirname, "../../web/dist")));
// Support CORS for GET requests to the API (for embedding the NodeBoxPlayer)
app.use("/api", cors({ origin: "*", methods: ["GET"] }));

// Write out the object as JSON.
function success(res, data, statusCode = 200) {
  res.writeHead(statusCode, { "Content-Type": "application/json" });
  res.end(JSON.stringify({ status: "ok", ...data }));
}

// Write out an error to the response.
function error(res, message, statusCode = 400) {
  res.writeHead(statusCode, { "Content-Type": "application/json" });
  res.end(JSON.stringify({ status: "error", message }));
}

function _checkOwnership(req, res, userId, next) {
  if (USERS_PUBLIC_SCOPE.includes(userId)) next();
  else {
    const authHeader = req.headers["authorization"];
    if (!authHeader) {
      return error(res, "Missing authorization header", 401);
    }
    const token = authHeader.split(" ")[1];

    if (!token) {
      error(res, "Missing authorization header", 401);
      return;
    }
    let decoded;
    try {
      decoded = jwt.verify(token, process.env.JWT_SECRET);
    } catch (err) {
      return error(res, "Invalid authorization token", 401);
    }
    if (decoded.userId !== userId) {
      return error(res, "Invalid user ID", 401);
    }
    req._membership = decoded.membership;
    req._authUserId = decoded.userId;
    next();
  }
}

function checkAdmin(req) {
  const authHeader = req.headers["authorization"];
  if (!authHeader) {
    return false;
  }
  const token = authHeader.split(" ")[1];
  if (!token) {
    return false;
  }
  let decoded;
  try {
    decoded = jwt.verify(token, process.env.JWT_SECRET);
  } catch (err) {
    return false;
  }
  if (!ADMIN_USERS.includes(decoded.userId)) {
    return false;
  }
  return true;
}

function _getOwnership(req, res, userId, next) {
  if (USERS_PUBLIC_SCOPE.includes(userId)) next();
  else {
    const authHeader = req.headers["authorization"];
    if (!authHeader) {
      next();
      return;
    }
    const token = authHeader.split(" ")[1];

    if (!token) {
      next();
      return;
    }
    let decoded;
    try {
      decoded = jwt.verify(token, process.env.JWT_SECRET);
    } catch (err) {
      next();
      return;
    }

    if (userId === decoded?.userId) {
      req._membership = decoded.membership;
      req._authUserId = decoded.userId;
    }
    next();
  }
}

function checkOwnershipFromParam(req, res, next) {
  const userId = req.params.userId;
  _checkOwnership(req, res, userId, next);
}

function checkOwnershipFromBody(req, res, next) {
  const { userId } = req.body;
  _checkOwnership(req, res, userId, next);
}

function getOwnershipFromParam(req, res, next) {
  const userId = req.params.userId;
  _getOwnership(req, res, userId, next);
}

function getOwnershipFromBody(req, res, next) {
  const { userId } = req.body;
  _getOwnership(req, res, userId, next);
}

app.get("/guide/", async (req, res) => {
  res.redirect(301, "/guide/welcome");
});

app.get("/guide/:slug", async (req, res) => {
  const { slug } = req.params;
  let text;
  let statusCode = 200;
  try {
    text = await readFile(path.join(__dirname, `../../../doc/guide/${slug}.md`), "utf8");
  } catch (error) {
    statusCode = 404;
    text = `# Page not found\n\nThe page "${slug}" does not exist.\n\n[Back to the guide](/guide/welcome)`;
  }
  // Get out the first H1 title
  text = text.trim();
  const titleMatch = text.match(/^#\s+(.*)/m);
  text = text.substring(titleMatch ? titleMatch.index + titleMatch[0].length : 0);
  const title = titleMatch ? titleMatch[1] : "NodeBox Guide";

  const content = marked.parse(text);
  const data = {
    title,
    content,
  };
  const template = await readFile(path.join(__dirname, `../template/guide.html`), "utf8");
  const html = template.replace(/{{\s*(\w+)\s*}}/g, (match, key) => data[key] || "");

  res.writeHead(statusCode, { "Content-Type": "text/html" });
  res.end(html);
});

app.get("/guide/media/:slug(*)", async (req, res) => {
  const { slug } = req.params;
  const filePath = path.join(__dirname, `../../../doc/guide/media/${slug}`);
  try {
    const file = await readFile(filePath);
    const extension = path.extname(filePath).toLowerCase();
    let contentType = "application/octet-stream";
    if (extension === ".png") {
      contentType = "image/png";
    } else if (extension === ".jpg" || extension === ".jpeg") {
      contentType = "image/jpeg";
    } else if (extension === ".svg") {
      contentType = "image/svg+xml";
    }
    res.writeHead(200, { "Content-Type": contentType });
    res.end(file);
  } catch (error) {
    res.status(404).send("File not found");
  }
});

// Login
app.post("/api/auth/login", async (req, res) => {
  let { userId, password } = req.body;
  if (!userId || !password) {
    return error(res, "Missing user ID or password", 400);
  }
  userId = userId?.toLowerCase();

  const userExists = await store.userExists(userId);
  if (!userExists) {
    return error(res, "Invalid user ID or password", 403);
  }
  try {
    const valid = await store.checkUserIdAndPassword(userId, password);
    if (valid) {
      let membership = await getMembership(userId);
      const token = jwt.sign({ userId, membership }, process.env.JWT_SECRET);

      // call an external api and check for membership type
      const userProfile = await store.getUserProfile(userId);
      userProfile.membership_message = "";
      if (membership === null) {
        // if the api call fails
        membership = {
          membership_type: userProfile.membership_type || "free",
          membership_until: userProfile.membership_until || "",
        };
      } else {
        if (membership && typeof membership === "object") {
          // convert date 2024-10-17 and compare with today
          const today = new Date();
          const membershipUntil = new Date(membership.membership_until);
          if (today > membershipUntil) {
            membership.membership_type = "free";
            userProfile.membership_message = "Your premium subscription has ended on " + membershipUntil.toDateString();
          } else {
            if (userProfile.membership_type !== membership.membership_type) {
              switch (membership.membership_type) {
                case "free":
                  userProfile.membership_message = "Your premium subscription has ended.";
                  break;
                case "plus":
                  userProfile.membership_message = "Your premium subscription has been activated.";
                  break;
              }
            }
          }
          userProfile.membership_type = membership.membership_type || "free";
          userProfile.membership_until = membership.membership_until || "";
        }
      }
      await store.saveUserProfile(userId, userProfile);

      return success(res, { token, membership, message: userProfile.membership_message });
    } else {
      return error(res, "Invalid user ID or password", 403);
    }
  } catch (err) {
    return error(res, "Invalid user ID or password", 403);
  }
});

// Signup
app.post("/api/auth/signup", async (req, res) => {
  let { userId, email, password } = req.body;
  userId = userId?.toLowerCase();
  const alreadyExists = await store.userExists(userId);
  if (alreadyExists) {
    return error(res, "User ID already exists", 400);
  }
  try {
    validateUsername(userId);
    validateEmail(email);
    validatePassword(password);
  } catch (err) {
    return error(res, err.message, 400);
  }
  const passwordHash = await bcrypt.hash(password, 10);
  try {
    await store.createUser(userId, email, passwordHash);
    const membership = await getMembership(userId);
    const token = jwt.sign({ userId, membership }, process.env.JWT_SECRET);
    return success(res, { token });
  } catch (err) {
    return error(res, err.message, 400);
  }
});

// Check authorization
app.post("/api/auth/check", async (req, res) => {
  const authHeader = req.headers["authorization"];
  if (!authHeader) {
    return error(res, "Missing authorization header", 401);
  }
  const token = authHeader.split(" ")[1];
  if (!token) {
    error(res, "Missing authorization header", 401);
    return;
  }
  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    return success(res, { userId: decoded.userId });
  } catch (err) {
    return error(res, err.message, 401);
  }
});

app.post("/api/auth/find-by-email", async (req, res) => {
  const { email } = req.body;
  const userIds = await store.findUserIdsByEmail(email);
  success(res, { userIds });
});

app.post("/api/auth/forgot-password", async (req, res) => {
  const ONE_HOUR = 3600 * 1000; // Token validity period
  const { userIdOrEmail } = req.body;
  if (!userIdOrEmail) {
    return error(res, "Missing user ID or email");
  }

  if (userIdOrEmail.match(/^[a-zA-Z0-9]{3,20}$/)) {
    // User ID logic
    try {
      const userId = userIdOrEmail;
      const userExists = await store.userExists(userId);
      if (!userExists) {
        return success(res);
      }
      const profile = await store.getUserProfile(userId);
      const email = profile.email;
      const membership = await getMembership(userId);
      const token = jwt.sign({ userId, membership, bestBefore: Date.now() + ONE_HOUR }, process.env.JWT_SECRET);
      sendForgotPasswordEmail(email, userId, token);
      return success(res);
    } catch (e) {
      console.error(e);
      return error(res, `Something went wrong: ${e.message}`, 500);
    }
  } else if (userIdOrEmail.match(/^(.*)@(.*)$/)) {
    // Email logic
    try {
      const email = userIdOrEmail;
      const userIds = await store.findUserIdsByEmail(email);
      for (const userId of userIds) {
        const membership = await getMembership(userId);
        const token = jwt.sign({ userId, membership, bestBefore: Date.now() + ONE_HOUR }, process.env.JWT_SECRET);
        sendForgotPasswordEmail(email, userId, token);
      }
      return success(res);
    } catch (e) {
      console.error(e);
      return error(res, e.message, 500);
    }
  } else {
    return error(res, "Invalid user ID or email");
  }
});

app.post("/api/auth/reset-password", async (req, res) => {
  const { userId, password, token } = req.body;
  const valid = jwt.verify(token, process.env.JWT_SECRET);
  if (!valid) {
    return error(c, "Invalid token", 400);
  }
  if (valid.userId !== userId) {
    return error(c, "Invalid user ID", 400);
  }
  if (valid.bestBefore < Date.now()) {
    return error(c, "Token expired", 400);
  }
  if (password.length < 8) {
    return error(c, "Password must be at least 8 characters long", 400);
  }
  try {
    await store.resetPassword(userId, password);
    return success(c);
  } catch (e) {
    return error(c, e.message);
  }
});
// Load svg file
app.get("/api/svg/:userId/:projectId/:item", async (req, res) => {
  const { userId, projectId, item } = req.params;
  const svgContent = await store.getGallerySvg(userId, projectId, item, "published");

  if (!svgContent) {
    return res.status(204).send();
  }

  res.setHeader("Content-Type", "image/svg+xml");
  res.send(svgContent);
});

app.post("/api/save-svg", checkOwnershipFromBody, async (req, res) => {
  const { userId, projectId, item, version, svgContent } = req.body;
  if (userId !== "example") return res.status(400).send({ error: "Invalid user." });
  if (!projectId || !svgContent) {
    return res.status(400).send({ error: "Filename and SVG content are required" });
  }
  try {
    await store.saveGallerySvg(userId, projectId, item, version, svgContent);
    success(res, { status: "saved" });
  } catch (e) {
    error(res, e.message);
  }
});

// List projects for user
app.get("/api/projects/:userId", async (req, res) => {
  const userId = req.params.userId;
  const authUserId = req.query.userId;
  try {
    const exists = await store.userExists(userId);
    if (!exists) {
      success(res, { projects: [], userId: null });
      return;
    }
    const profile = await store.getUserProfile(userId);
    if (authUserId === null || authUserId !== userId) {
      success(res, { projects: profile.projects.filter((project) => project?.scope !== "private") });
    } else {
      success(res, { projects: profile.projects, membership_message: profile.membership_message });
    }
  } catch (e) {
    error(res, e.message);
  }
});

// Get membership details for user from profile
app.get("/api/membership/:userId", async (req, res) => {
  const userId = req.params.userId;
  try {
    const profile = await store.getUserProfile(userId);
    success(res, {
      membership_type: profile.membership_type,
      membership_until: profile.membership_until,
    });
  } catch (e) {
    error(res, e.message);
  }
});

// Create new project
app.post("/api/projects", checkOwnershipFromBody, async (req, res) => {
  const userId = req.body.userId;
  const projectId = req.body.projectId;
  const projectTitle = req.body.projectTitle;
  const scope = req.body.public;
  try {
    await store.createProject(userId, projectId, projectTitle, scope);
    success(res, { userId, projectId });
  } catch (e) {
    error(res, e.message);
  }
});

// Check if project exists
app.head("/api/projects/:userId/:projectId", async (req, res) => {
  const userId = req.params.userId;
  const projectId = req.params.projectId;
  const exists = await store.projectExists(userId, projectId);
  if (exists) {
    success(res);
  } else {
    error(res, "Project not found", 404);
  }
});

// Update scope in profile overview
app.post("/api/set-scope/:userId/:projectId", checkOwnershipFromParam, async (req, res) => {
  const userId = req.params.userId;
  const projectId = req.params.projectId;
  const body = req.body;
  // Overrule scope if membership has expired through token.
  if (req._membership?.membership_until) {
    const today = new Date();
    const membershipUntil = new Date(req._membership.membership_until);
    if (today > membershipUntil) {
      body.scope = "public";
    }
  }
  try {
    await store.updateProjectScopeInProfile(userId, projectId, body.scope);
    success(res);
  } catch (e) {
    error(res, e.message);
  }
});
// Update project title in overview
app.post("/api/set-title/:userId/:projectId", checkOwnershipFromParam, async (req, res) => {
  const userId = req.params.userId;
  const projectId = req.params.projectId;
  const body = req.body;
  try {
    await store.updateProjectTitleInProfile(userId, projectId, body.title);
    success(res);
  } catch (e) {
    error(res, e.message);
  }
});
// Update project color in overview
app.post("/api/set-project-color/:userId/:projectId", checkOwnershipFromParam, async (req, res) => {
  const userId = req.params.userId;
  const projectId = req.params.projectId;
  const body = req.body;
  try {
    await store.updateProjectColor(userId, projectId, body.color);
    success(res);
  } catch (e) {
    error(res, e.message);
  }
});
// Load project
app.get("/api/projects/:userId/:projectId/:version", getOwnershipFromParam, async (req, res) => {
  const userId = req.params.userId;
  const _authUserId = req._authUserId;
  const projectId = req.params.projectId;
  const version = req.params.version;
  try {
    // const exists = await store.userExists(userId);
    // if (!exists) {
    //   success(res, { projects: [], userId: null });
    //   return;
    // }
    const project = await store.loadProject(userId, projectId, version);
    if (project && project.scope == "private" && userId !== _authUserId) {
      error(res, "Project not accesible");
      return;
    }
    success(res, { assetsRoot: store.assetsRoot(), project });
  } catch (e) {
    error(res, e.message, 404);
  }
});

// Save project
app.post("/api/projects/:userId/:projectId", checkOwnershipFromParam, async (req, res) => {
  const userId = req.params.userId;
  const projectId = req.params.projectId;
  const project = req.body;
  try {
    await store.saveProject(userId, projectId, project);
    success(res);
  } catch (e) {
    error(res, e.message);
  }
});

//delete project remove project
app.delete("/api/projects/:userId/:projectId", checkOwnershipFromParam, async (req, res) => {
  const { userId, projectId } = req.params;
  const exists = await store.projectExists(userId, projectId);
  if (exists)
    try {
      await store.removeProject(userId, projectId);
      success(res, { userId, projectId });
    } catch (e) {
      error(res, e.message);
    }
  else {
    try {
      const profile = await store.getUserProfile(userId);
      profile.projects = profile.projects.filter((project) => project.id !== projectId);
      await store.saveUserProfile(userId, profile);
      success(res, { message: "Project deleted successfully" });
    } catch (e) {
      error(res, "Failed to delete project from profile.", 500);
    }
  }
});

// Publish project
app.post("/api/projects/:userId/:projectId/publish", checkOwnershipFromParam, async (req, res) => {
  const userId = req.params.userId;
  const projectId = req.params.projectId;

  try {
    await store.publishProject(userId, projectId);
    if (userId === "example") {
      store.updateGallery(userId, projectId);
    }
    success(res);
  } catch (e) {
    error(res, e.message);
  }
});

// Presign S3 asset URL
app.post("/api/assets/presign/:userId/:projectId", checkOwnershipFromParam, async (req, res) => {
  const userId = req.params.userId;
  const projectId = req.params.projectId;
  const { filename: dirtyFilename, type } = req.body;
  let filename;
  try {
    filename = cleanFilename(dirtyFilename);
  } catch (e) {
    return error(res, e.message, 400);
  }
  if (process.env.DATA_STORE === "s3") {
    const url = await s3Store.presignURL(userId, projectId, filename, type);
    success(res, { method: "PUT", url, filename, headers: { "content-type": type } });
  } else {
    const url = `/api/upload/${userId}/${projectId}`;
    success(res, { method: "POST", url, filename });
  }
});

// Load asset
app.get("/api/assets/:userId/:projectId/blobs/:assetId", async (req, res) => {
  const userId = req.params.userId;
  const projectId = req.params.projectId;
  const assetId = req.params.assetId;

  try {
    const body = await store.loadAsset(userId, projectId, assetId);

    body.on("error", (err) => {
      if (err.code === "ENOENT") {
        error(res, `Asset not found: ${assetId}`, 404);
      } else {
        error(res, err.message, 500);
      }
    });

    res.setHeader("Content-Type", "application/octet-stream");
    body.pipe(res);
  } catch (e) {
    error(res, e.message, 500);
  }
});

// Upload asset
app.post("/api/upload/:userId/:projectId", checkOwnershipFromParam, async (req, res) => {
  const userId = req.params.userId;
  const projectId = req.params.projectId;
  const contentType = req.headers["content-type"];
  const filename = req.headers["filename"];

  if (!contentType || !filename) {
    return error(res, "Missing content-type or filename header", 400);
  }
  const targetFilename = cleanFilename(filename);
  const targetPath = fileStore.assetsPath(userId, projectId, targetFilename);
  const targetDir = path.dirname(targetPath);
  if (!fs.existsSync(targetDir)) {
    fs.mkdirSync(targetDir, { recursive: true });
  }

  const fileStream = fs.createWriteStream(targetPath);
  req.pipe(fileStream);

  fileStream.on("finish", () => {
    success(res, { filename: targetFilename });
  });

  fileStream.on("error", (err) => {
    console.error(err);
    error(res, err.message, 500);
  });

  req.on("error", (err) => {
    console.error(err);
    fileStream.close();
    error(res, err.message, 500);
  });
});

app.delete("/api/assets/:userId/:projectId/:assetId", checkOwnershipFromParam, async (req, res) => {
  const userId = req.params.userId;
  const projectId = req.params.projectId;
  const assetId = req.params.assetId;
  try {
    await store.deleteAsset(userId, projectId, assetId);
    success(res);
  } catch (e) {
    error(res, e.message);
  }
});

// Load a function as a JavaScript module
// This is used by the web app for dynamic imports
app.get("/api/fn/:userId/:projectId/:functionName", async (req, res) => {
  const { userId, projectId, functionName } = req.params;
  try {
    const project = await store.loadProject(userId, projectId);
    const item = project.items?.find((item) => item.name === functionName);
    if (!item || item.type !== "FUNCTION") {
      const items = project.items?.map((item) => item.name).join(", ");
      res.header("Content-Type", "application/javascript");
      return res
        .status(404)
        .send(
          `// The function "${functionName}" does not exist in the project.\n// Valid functions are: ${items}\n// Use like this:\n//\n//   import { myHelperFunction } from "project:MyFunction";\n`,
        );
    }
    res.header("Content-Type", "application/javascript");
    return res.end(item.source);
  } catch (e) {
    error(res, e.message);
  }
});

app.get("/api/gallery/:keyword", async (req, res) => {
  const gallery = await store.loadGallery();
  if (gallery === undefined) {
    success(res, { items: [], description: "Sorry, no examples available." });
  }
  try {
    const project = await store.loadProject("example", req.params.keyword, "published");
    const items = gallery.items.filter((i) => i.keyword?.toLowerCase() === req.params.keyword.toLowerCase());
    success(res, { items, description: project?.description });
  } catch (error) {
    success(res, { items: [], description: "Sorry, no examples available." });
  }
});

app.get("/embed/:userId/:projectId/:item?", async (req, res) => {
  const data = structuredClone(req.params);
  data.item = data.item || "__undefined__";
  data.version = "published";
  if (!(await store.projectExists(data.userId, data.projectId, data.version))) {
    return res.status(404).send("Project not found or not published.");
  }
  const template = await readFile(path.join(__dirname, `../template/embed.html`), "utf8");
  const html = template.replace(/{{\s*(\w+)\s*}}/g, (_, key) => data[key] || "");

  res.removeHeader("X-Frame-Options");
  res.setHeader("Content-Security-Policy", "frame-ancestors *");
  res.writeHead(200, { "Content-Type": "text/html" });
  res.end(html);
});

app.get("/admin", async (req, res) => {
  const data = {};
  const template = await readFile(path.join(__dirname, `../template/admin.html`), "utf8");
  const html = template.replace(/{{\s*(\w+)\s*}}/g, (_, key) => data[key] || "");
  res.writeHead(200, { "Content-Type": "text/html" });
  res.end(html);
});

app.get("/api/admin/current-user", async (req, res) => {
  const result = checkAdmin(req);
  if (!result) {
    return error(res, "Not logged in", 401);
  }
  success(res, { userId: "fdb" });
});

app.post("/admin/reset-password", async (req, res) => {
  const isAdmin = checkAdmin(req);
  if (!isAdmin) {
    return error(res, "Not logged in", 401);
  }
  const { userIdOrEmail, newPassword, confirmPassword } = req.body;
  let userId;
  if (userIdOrEmail.match(/^[a-zA-Z0-9]{3,20}$/)) {
    userId = userIdOrEmail;
  } else if (userIdOrEmail.match(/^(.*)@(.*)$/)) {
    const userIds = await store.findUserIdsByEmail(userIdOrEmail);
    if (userIds.length === 0) {
      return error(res, "User not found", 404);
    }
    userId = userIds[0];
  } else {
    error(res, "Invalid user ID or email");
  }
  if (newPassword.length < 6) {
    return error(res, "Password must be at least 8 characters long", 400);
  }
  if (newPassword !== confirmPassword) {
    return error(res, "Passwords do not match");
  }
  try {
    await store.resetPassword(userId, newPassword);
    success(res, { message: `Password for user ${userId} reset successfully` });
  } catch (e) {
    error(res, e.message);
  }
});

// Catch-all SPA route for web app
app.get("*", (_, res) => {
  res.sendFile(path.resolve(__dirname, "../../web/dist", "index.html"));
});

const port = process.env.PORT || 3000;
app.listen(port, () => {
  console.log(`Server running on http://localhost:${port}`);
});
