// Sets ACL to public-read for all published.json files across all users
// This script ensures that published projects are publicly accessible

import "dotenv/config";
import * as s3Store from "../src/s3-store.js";
import { S3Client, PutObjectCommand, CopyObjectCommand, PutObjectAclCommand } from "@aws-sdk/client-s3";
import process from "process";

const args = process.argv.slice(2);
const isDryRun = args.includes("--dry-run") || args.includes("-n");

// Configure S3 client (same as in s3-store.js)
const AWS_ACCESS_KEY_ID = process.env.AWS_ACCESS_KEY_ID;
const AWS_SECRET_ACCESS_KEY = process.env.AWS_SECRET_ACCESS_KEY;
const AWS_S3_BUCKET = process.env.AWS_S3_BUCKET;
const AWS_S3_ENDPOINT = process.env.AWS_S3_ENDPOINT;
const AWS_REGION = "us-east-1";

const s3Client = new S3Client({
  forcePathStyle: false,
  endpoint: AWS_S3_ENDPOINT,
  region: AWS_REGION,
  credentials: {
    accessKeyId: AWS_ACCESS_KEY_ID,
    secretAccessKey: AWS_SECRET_ACCESS_KEY,
  },
});

async function setObjectAcl(key) {
  if (isDryRun) {
    console.log(`[DRY RUN] Would set ACL to public-read for: ${key}`);
    return;
  }

  try {
    // Set ACL to public-read using PutObjectAclCommand
    await s3Client.send(
      new PutObjectAclCommand({
        Bucket: AWS_S3_BUCKET,
        Key: key,
        ACL: "public-read",
      })
    );
    console.log(`Set ACL to public-read for: ${key}`);
  } catch (err) {
    console.error(`Error setting ACL for ${key}: ${err.message}`);
  }
}

console.log(isDryRun ? "Running in DRY RUN mode - no changes will be made" : "Setting ACL to public-read for all published.json files");
console.log();

const userIds = await s3Store.listRootDir();
let totalFound = 0;
let totalProcessed = 0;

for (const userId of userIds) {
  const projectDetails = await s3Store.listProjects(userId);
  for (const projectDetail of projectDetails) {
    const projectId = projectDetail.id;
    
    try {
      // Check if published version exists
      const publishedExists = await s3Store.projectExists(userId, projectId, "published");
      if (publishedExists) {
        const publishedPath = `users/${userId}/${projectId}/versions/published.json`;
        totalFound++;
        console.log(`Found published project: ${userId}/${projectId}`);
        await setObjectAcl(publishedPath);
        totalProcessed++;
      }
    } catch (err) {
      console.error(`Error processing project ${userId}/${projectId}: ${err.message}`);
      continue;
    }
  }
}

console.log();
console.log(`Summary:`);
console.log(`- Found ${totalFound} published.json files`);
console.log(`- ${isDryRun ? "Would process" : "Processed"} ${totalProcessed} files`);

if (isDryRun) {
  console.log();
  console.log("To apply changes, run without --dry-run flag:");
  console.log("node set-published-acl.mjs");
}