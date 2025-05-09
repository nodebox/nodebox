import path from "path";
import { createReadStream } from "fs";
import { S3Client, PutObjectCommand } from "@aws-sdk/client-s3";
import { walkSync } from "@nodelib/fs.walk";
import dotenv from "dotenv";
dotenv.config();

const DATA_DIR = "data";
const USERS = ["core", "skel", "template", "example", "test"];

const s3 = new S3Client({
  credentials: {
    accessKeyId: process.env.AWS_ACCESS_KEY_ID,
    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
  },
  endpoint: process.env.AWS_S3_ENDPOINT,
  region: "us-east-1",
});

async function copyToS3(filePath) {
  console.log(`Uploading ${filePath} to S3`);
  const localFile = path.resolve(path.join(DATA_DIR, filePath));
  const fileStream = createReadStream(localFile);
  const params = {
    Bucket: process.env.AWS_S3_BUCKET,
    Key: `users/${filePath}`,
    Body: fileStream,
    // Make files in published directories publicly readable
    ACL: filePath.includes("published") ? "public-read" : undefined,
  };

  try {
    const data = await s3.send(new PutObjectCommand(params));
    return data; // Returns the response from the S3 API
  } catch (err) {
    console.error("Error uploading to S3:", err);
    throw err; // Rethrow to handle the error further up if needed
  }
}

const entries = walkSync(DATA_DIR, { stats: true });
for (const entry of entries) {
  // Only copy files
  if (!entry.stats.isFile()) continue;
  // if (entry.dirent.type !== 1) continue;
  // Remove the data directory from the path
  entry.path = entry.path.replace(`${DATA_DIR}/`, "");
  if (entry.name.startsWith(".")) continue;
  if (entry.path.startsWith("tutorial")) continue;
  const userId = entry.path.split("/")[0];
  if (!USERS.includes(userId)) continue;
  // console.log(entry.path);
  await copyToS3(entry.path);
}
