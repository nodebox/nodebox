export function cleanFilename(filename) {
  const allowedExtensions = [
    "png",
    "jpg",
    "jpeg",
    "gif",
    "csv",
    "json",
    "geojson",
    "topojson",
    "txt",
    "otf",
    "ttf",
    "svg",
  ];

  let extension = filename.split(".").pop().toLowerCase();
  let name = filename.replace(/\.[^/.]+$/, ""); // Remove extension from name

  // Remove special characters and replace spaces
  name = name.replace(/[^a-zA-Z0-9\s-_\.]/g, "").replace(/\s+/g, "-");

  // Check if the extension is in the allowlist
  if (!allowedExtensions.includes(extension)) {
    // Handle the case where the extension is not allowed
    // For example, reject the file or assign a default extension
    throw new Error("File type not allowed");
  }

  // Truncate to 255 characters (common maximum filename length)
  const maxLength = 255 - extension.length;
  name = name.substring(0, maxLength);

  // Append the extension back
  return `${name}.${extension}`;
}
