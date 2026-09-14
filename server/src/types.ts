export interface Bindings {
  BUCKET: R2Bucket;
  ASSETS: Fetcher;
  JWT_SECRET: string;
  EMAIL?: SendEmail;
  ASSETS_URL?: string;
}

export interface Membership {
  membership_type?: string;
  membership_until?: string;
}

export interface AuthPayload {
  userId: string;
  membership: Membership | null;
  bestBefore?: number;
  [key: string]: unknown;
}

export interface ProjectInfo {
  id: string;
  title: string;
  color?: string;
  scope?: string;
}

export interface Profile {
  login: string;
  email: string;
  password: string;
  projects: ProjectInfo[];
  membership_type?: string;
  membership_until?: string;
  membership_message?: string;
}

// Project files are free-form JSON documents owned by the editor; the server only touches a few fields.
export type Project = Record<string, any>;

export interface GalleryItem {
  projectId: string;
  itemId: string;
  name: string;
  description: string;
  category: string;
  keyword?: string;
  subKeyword?: string;
}

export interface Gallery {
  items: GalleryItem[];
}
