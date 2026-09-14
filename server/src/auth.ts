import type { Context, MiddlewareHandler } from "hono";
import { sign, verify } from "hono/jwt";
import type { Store } from "./store";
import type { AuthPayload, Bindings, Membership } from "./types";

export const USERS_PUBLIC_SCOPE = ["example", "core"];
export const ADMIN_USERS = ["fdb"];

export interface AuthVariables {
  store: Store;
  membership?: Membership | null;
  authUserId?: string;
}

export type AppEnv = { Bindings: Bindings; Variables: AuthVariables };

// Tokens are HS256 without expiry, the same shape the previous server issued,
// so tokens already stored in browsers keep working.
export function signToken(payload: AuthPayload, secret: string) {
  return sign(payload, secret, "HS256");
}

export async function verifyToken(token: string, secret: string): Promise<AuthPayload | undefined> {
  try {
    return (await verify(token, secret, "HS256")) as AuthPayload;
  } catch {
    return undefined;
  }
}

export function bearerToken(c: Context<AppEnv>): string | undefined {
  const authHeader = c.req.header("authorization");
  if (!authHeader) return undefined;
  return authHeader.split(" ")[1] || undefined;
}

export function error(c: Context, message: string, statusCode = 400) {
  return c.json({ status: "error", message }, statusCode as 400);
}

async function checkOwnership(c: Context<AppEnv>, userId: string, next: () => Promise<void>) {
  if (USERS_PUBLIC_SCOPE.includes(userId)) return next();
  const token = bearerToken(c);
  if (!token) return error(c, "Missing authorization header", 401);
  const decoded = await verifyToken(token, c.env.JWT_SECRET);
  if (!decoded) return error(c, "Invalid authorization token", 401);
  if (decoded.userId !== userId) return error(c, "Invalid user ID", 401);
  c.set("membership", decoded.membership);
  c.set("authUserId", decoded.userId);
  return next();
}

async function getOwnership(c: Context<AppEnv>, userId: string, next: () => Promise<void>) {
  if (USERS_PUBLIC_SCOPE.includes(userId)) return next();
  const token = bearerToken(c);
  if (!token) return next();
  const decoded = await verifyToken(token, c.env.JWT_SECRET);
  if (decoded && userId === decoded.userId) {
    c.set("membership", decoded.membership);
    c.set("authUserId", decoded.userId);
  }
  return next();
}

export async function checkAdmin(c: Context<AppEnv>) {
  const token = bearerToken(c);
  if (!token) return false;
  const decoded = await verifyToken(token, c.env.JWT_SECRET);
  if (!decoded) return false;
  return ADMIN_USERS.includes(decoded.userId);
}

export const checkOwnershipFromParam: MiddlewareHandler<AppEnv> = (c, next) =>
  checkOwnership(c, c.req.param("userId") ?? "", next);

export const getOwnershipFromParam: MiddlewareHandler<AppEnv> = (c, next) =>
  getOwnership(c, c.req.param("userId") ?? "", next);

// The body is parsed once here and cached on the request, so the handler reads the same object.
export const checkOwnershipFromBody: MiddlewareHandler<AppEnv> = async (c, next) => {
  const body = await c.req.json<{ userId?: string }>();
  return checkOwnership(c, body.userId ?? "", next);
};
