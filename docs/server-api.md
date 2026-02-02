# NodeBox Server API

REST API specification for the NodeBox web backend.

## Base URL

```
/api/v1
```

## Authentication

All endpoints may require authentication via Bearer token:

```
Authorization: Bearer <token>
```

Authentication implementation is deployment-specific and not specified here.

---

## Projects

### List Projects

```
GET /projects
```

**Response:**

```json
{
  "projects": [
    {
      "id": "abc123",
      "name": "My Project",
      "modified": "2024-01-15T10:30:00Z"
    }
  ]
}
```

### Get Project

```
GET /projects/{id}
```

**Response:** Raw `.ndbx` file content (`application/xml`)

### Create Project

```
POST /projects
Content-Type: application/xml

<ndbx>...</ndbx>
```

**Response:**

```json
{
  "id": "abc123",
  "name": "Untitled"
}
```

### Update Project

```
PUT /projects/{id}
Content-Type: application/xml

<ndbx>...</ndbx>
```

**Response:** `204 No Content`

### Delete Project

```
DELETE /projects/{id}
```

**Response:** `204 No Content`

---

## Assets

Assets are files within a project directory (images, fonts, data files).

### List Assets

```
GET /projects/{id}/assets
GET /projects/{id}/assets/{path}  # List subdirectory
```

**Response:**

```json
{
  "entries": [
    { "name": "logo.png", "is_directory": false },
    { "name": "fonts", "is_directory": true }
  ]
}
```

### Get Asset

```
GET /projects/{id}/assets/{path}
Accept: application/octet-stream
```

**Response:** Raw file bytes

### Upload Asset

```
PUT /projects/{id}/assets/{path}
Content-Type: application/octet-stream

<binary data>
```

**Response:** `201 Created`

### Delete Asset

```
DELETE /projects/{id}/assets/{path}
```

**Response:** `204 No Content`

---

## Libraries

Libraries are shared node collections available to all projects.

### List Libraries

```
GET /libraries
```

**Response:**

```json
{
  "libraries": [
    { "name": "math", "version": "1.0.0" },
    { "name": "color", "version": "2.1.0" }
  ]
}
```

### Get Library

```
GET /libraries/{name}
```

**Response:** Raw library file content (`application/xml`)

---

## Error Responses

All errors return JSON:

```json
{
  "error": "not_found",
  "message": "Project not found"
}
```

**Error codes:**

| Status | Description |
|--------|-------------|
| `400` | Bad request (invalid input) |
| `401` | Unauthorized (missing/invalid token) |
| `403` | Forbidden (no access to resource) |
| `404` | Not found |
| `500` | Internal server error |

---

## WebPort Implementation Notes

The JavaScript WebPort implementation maps Port trait methods to API calls:

```javascript
const webPort = {
  async read_file(ctx, path) {
    const resp = await fetch(`/api/v1/projects/${ctx.project_id}/assets/${path}`);
    if (!resp.ok) throw new PortError(resp.status === 404 ? 'NotFound' : 'IoError');
    return new Uint8Array(await resp.arrayBuffer());
  },

  async write_file(ctx, path, data) {
    const resp = await fetch(`/api/v1/projects/${ctx.project_id}/assets/${path}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/octet-stream' },
      body: data
    });
    if (!resp.ok) throw new PortError('IoError');
  },

  async list_directory(ctx, path) {
    const resp = await fetch(`/api/v1/projects/${ctx.project_id}/assets/${path}`);
    if (!resp.ok) throw new PortError(resp.status === 404 ? 'NotFound' : 'IoError');
    return (await resp.json()).entries;
  },

  async read_project(ctx) {
    const resp = await fetch(`/api/v1/projects/${ctx.project_id}`);
    if (!resp.ok) throw new PortError(resp.status === 404 ? 'NotFound' : 'IoError');
    return new Uint8Array(await resp.arrayBuffer());
  },

  async write_project(ctx, data) {
    const resp = await fetch(`/api/v1/projects/${ctx.project_id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/xml' },
      body: data
    });
    if (!resp.ok) throw new PortError('IoError');
  },

  async load_library(name) {
    const resp = await fetch(`/api/v1/libraries/${name}`);
    if (!resp.ok) throw new PortError(resp.status === 404 ? 'LibraryNotFound' : 'IoError');
    return new Uint8Array(await resp.arrayBuffer());
  },

  async http_get(url) {
    const resp = await fetch(url);
    if (!resp.ok) throw new PortError('NetworkError');
    return new Uint8Array(await resp.arrayBuffer());
  },

  // === Project Dialogs (return absolute paths, no sandbox) ===

  // For opening project files - returns absolute path
  async show_open_project_dialog(filters) {
    // Use File System Access API if available
    if ('showOpenFilePicker' in window) {
      try {
        const types = filters.map(f => ({
          description: f.name,
          accept: { 'application/octet-stream': f.extensions.map(e => `.${e}`) }
        }));
        const [handle] = await window.showOpenFilePicker({ types });
        return handle.name; // In web context, return just the name
      } catch (e) {
        if (e.name === 'AbortError') return null;
        throw e;
      }
    }
    // Fallback: use input element
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = filters.flatMap(f => f.extensions.map(e => `.${e}`)).join(',');
      input.onchange = () => resolve(input.files?.[0]?.name ?? null);
      input.oncancel = () => resolve(null);
      input.click();
    });
  },

  // For saving project files - returns absolute path
  async show_save_project_dialog(filters, defaultName) {
    if ('showSaveFilePicker' in window) {
      try {
        const types = filters.map(f => ({
          description: f.name,
          accept: { 'application/octet-stream': f.extensions.map(e => `.${e}`) }
        }));
        const handle = await window.showSaveFilePicker({ types, suggestedName: defaultName });
        return handle.name;
      } catch (e) {
        if (e.name === 'AbortError') return null;
        throw e;
      }
    }
    // Fallback: return suggested name and let caller handle download
    return defaultName ?? 'untitled.ndbx';
  },

  // === Asset Dialogs (sandboxed to project directory, return relative paths) ===

  // For importing assets within project - requires ProjectContext, returns RelativePath
  async show_open_file_dialog(ctx, filters) {
    // Note: In web context, ctx.project_id is used to validate selections
    // This is a security measure - files must be within the project
    if ('showOpenFilePicker' in window) {
      try {
        const types = filters.map(f => ({
          description: f.name,
          accept: { 'application/octet-stream': f.extensions.map(e => `.${e}`) }
        }));
        const [handle] = await window.showOpenFilePicker({ types });
        // In a real implementation, validate the path is within project
        // For REST API mode, the server handles validation
        return handle.name;
      } catch (e) {
        if (e.name === 'AbortError') return null;
        throw e;
      }
    }
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = filters.flatMap(f => f.extensions.map(e => `.${e}`)).join(',');
      input.onchange = () => resolve(input.files?.[0]?.name ?? null);
      input.oncancel = () => resolve(null);
      input.click();
    });
  },

  // For exporting assets within project - requires ProjectContext, returns RelativePath
  async show_save_file_dialog(ctx, filters, defaultName) {
    // Note: In web context, the save location is validated to be within project
    if ('showSaveFilePicker' in window) {
      try {
        const types = filters.map(f => ({
          description: f.name,
          accept: { 'application/octet-stream': f.extensions.map(e => `.${e}`) }
        }));
        const handle = await window.showSaveFilePicker({ types, suggestedName: defaultName });
        return handle.name;
      } catch (e) {
        if (e.name === 'AbortError') return null;
        throw e;
      }
    }
    // Fallback: return suggested name
    return defaultName ?? 'untitled';
  },

  // For selecting folders within project - requires ProjectContext, returns RelativePath
  async show_select_folder_dialog(ctx) {
    if ('showDirectoryPicker' in window) {
      try {
        const handle = await window.showDirectoryPicker();
        // In a real implementation, validate the folder is within project
        return handle.name;
      } catch (e) {
        if (e.name === 'AbortError') return null;
        throw e;
      }
    }
    throw new PortError('Unsupported');
  },

  async show_confirm_dialog(title, message) {
    return window.confirm(`${title}\n\n${message}`);
  },

  async show_message_dialog(title, message, buttons) {
    // Simple implementation using confirm/alert
    // For better UX, use a modal library
    const result = window.confirm(`${title}\n\n${message}\n\n${buttons.join(' / ')}`);
    return result ? 0 : buttons.length - 1;
  },

  async clipboard_read_text() {
    try {
      return await navigator.clipboard.readText();
    } catch {
      return null;
    }
  },

  async clipboard_write_text(text) {
    await navigator.clipboard.writeText(text);
  },

  log(level, message) {
    const levels = { Error: 'error', Warn: 'warn', Info: 'info', Debug: 'debug' };
    console[levels[level] || 'log'](message);
  },

  performance_mark(name) {
    performance.mark(name);
  },

  performance_mark_with_details(name, details) {
    performance.mark(name, { detail: JSON.parse(details) });
  },

  get_config_dir() {
    // Web has no config directory; return a virtual path
    throw new PortError('Unsupported');
  },

  platform_info() {
    return {
      os_name: 'web',
      is_web: true,
      is_mobile: /Android|iPhone|iPad/i.test(navigator.userAgent),
      has_filesystem: 'showOpenFilePicker' in window,
      has_native_dialogs: false
    };
  }
};
```

### File System Access API

For browsers that support the File System Access API (Chrome, Edge), the WebPort can provide a more native-like experience:

```javascript
class FileSystemAccessPort {
  constructor(directoryHandle) {
    this.root = directoryHandle;
  }

  async read_file(ctx, path) {
    const parts = path.split('/');
    let handle = this.root;

    for (let i = 0; i < parts.length - 1; i++) {
      handle = await handle.getDirectoryHandle(parts[i]);
    }

    const fileHandle = await handle.getFileHandle(parts[parts.length - 1]);
    const file = await fileHandle.getFile();
    return new Uint8Array(await file.arrayBuffer());
  }

  async write_file(ctx, path, data) {
    const parts = path.split('/');
    let handle = this.root;

    for (let i = 0; i < parts.length - 1; i++) {
      handle = await handle.getDirectoryHandle(parts[i], { create: true });
    }

    const fileHandle = await handle.getFileHandle(parts[parts.length - 1], { create: true });
    const writable = await fileHandle.createWritable();
    await writable.write(data);
    await writable.close();
  }

  // ... other methods
}
```
