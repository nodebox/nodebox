// The server is written against the Cloudflare Workers types; inside the desktop app the bindings
// are provided by the local server, so only the names need to exist here.
type R2Bucket = import("./file-bucket").FileBucket;
type Fetcher = { fetch(request: Request | string | URL): Promise<Response> };
type SendEmail = { send(message: unknown): Promise<{ messageId?: string }> };

declare module "*.html" {
  const content: string;
  export default content;
}
declare module "@ndbx/server/src/file-utils.js" {
  export function cleanFilename(name: string): string;
}
declare module "@ndbx/server/src/validate.js" {
  export function validateEmail(email: string): boolean;
  export function validatePassword(password: string): boolean;
  export function validateUsername(name: string): boolean;
}
