import { useEffect, useState } from "react";
import { NodeBoxPlayer, loadMainProject, renderItemToSvgString, renderItem, renderItemToPngBlob } from "@ndbx/runtime";

const DEFAULT_PUBLISHED_URL_TEMPLATE =
  "https://nodeboxlive.ams3.digitaloceanspaces.com/users/{{ userId }}/{{ projectId }}/versions/published.json";
const DEFAULT_ASSETS_URL_TEMPLATE =
  "https://nodeboxlive.ams3.cdn.digitaloceanspaces.com/users/{{ userId }}/{{ projectId }}/blobs/{{ hash }}";
const DEFAULT_LIB_URL_TEMPLATE =
  "https://nodeboxlive.ams3.cdn.digitaloceanspaces.com/users/{{ userId }}/{{ projectId }}/lib/{{ file }}.js";

function parseNodeBoxUrl(url) {
  const match = url.match(/https?:\/\/[^\/]+\/([^\/]+)\/([^\/]+)/);
  if (match) {
    return {
      userId: match[1],
      projectId: match[2],
    };
  }
  return null;
}

function App() {
  const [userId, setUserId] = useState("example");
  const [projectId, setProjectId] = useState("scatter");
  const [publishedUrlTemplate, setPublishedUrlTemplate] = useState(DEFAULT_PUBLISHED_URL_TEMPLATE);
  const [assetsUrlTemplate, setAssetsUrlTemplate] = useState(DEFAULT_ASSETS_URL_TEMPLATE);
  const [libUrlTemplate, setLibUrlTemplate] = useState(DEFAULT_LIB_URL_TEMPLATE);
  const [nodeboxUrl, setNodeboxUrl] = useState("");
  const [showAdvanced, setShowAdvanced] = useState(false);

  const handleUrlParse = () => {
    const parsed = parseNodeBoxUrl(nodeboxUrl);
    if (parsed) {
      setUserId(parsed.userId);
      setProjectId(parsed.projectId);
    } else {
      alert("Invalid NodeBox URL format");
    }
  };

  const handleExportSVG = async () => {
    const cx = await loadMainProject(userId, projectId, "dev");
    const item = cx.project.items[0];
    const resultValue = await renderItem(cx, item);
    const svg = renderItemToSvgString(item, resultValue);
    console.log(svg);
    const blob = new Blob([svg], { type: "image/svg+xml" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    const filename = `${userId}-${projectId}-${item.id}.svg`;
    link.download = filename;
    document.body.appendChild(link); // Required for Firefox
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleExportPNG = async () => {
    const cx = await loadMainProject(userId, projectId, "dev");
    const item = cx.project.items[0];
    const resultValue = await renderItem(cx, item);
    const pngBlob = await renderItemToPngBlob(item, resultValue, { scale: 2 });
    if (!pngBlob) {
      alert("PNG export failed: no drawable result");
      return;
    }
    const url = URL.createObjectURL(pngBlob);
    const link = document.createElement("a");
    link.href = url;
    const filename = `${userId}-${projectId}-${item.id}.png`;
    link.download = filename;
    document.body.appendChild(link); // Required for Firefox
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const values = {
    width: 200,
  };

  return (
    <main className="app__main">
      <div className="config-section">
        <h1 className="app__title">NodeBox Player Embed Demo</h1>

        <div className="input-group">
          <label>NodeBox URL:</label>
          <input
            type="text"
            value={nodeboxUrl}
            onChange={(e) => setNodeboxUrl(e.target.value)}
            placeholder="https://new.nodebox.live/fienstappaerts/920fb0d8-55af-457e-8d4b-a3cb3118deb4"
          />
          <button onClick={handleUrlParse}>Parse URL</button>
        </div>

        <div className="input-group">
          <label>User ID:</label>
          <input type="text" value={userId} onChange={(e) => setUserId(e.target.value)} />
        </div>

        <div className="input-group">
          <label>Project ID:</label>
          <input type="text" value={projectId} onChange={(e) => setProjectId(e.target.value)} />
        </div>

        <div className="advanced-toggle" onClick={() => setShowAdvanced(!showAdvanced)}>
          <span className={`triangle ${showAdvanced ? "expanded" : ""}`}>▶</span>
          <span>Advanced URL Templates</span>
        </div>

        {showAdvanced && (
          <div className="advanced-section">
            <div className="input-group">
              <label>Published URL Template:</label>
              <input
                type="text"
                value={publishedUrlTemplate}
                onChange={(e) => setPublishedUrlTemplate(e.target.value)}
              />
            </div>

            <div className="input-group">
              <label>Assets URL Template:</label>
              <input type="text" value={assetsUrlTemplate} onChange={(e) => setAssetsUrlTemplate(e.target.value)} />
            </div>

            <div className="input-group">
              <label>Library URL Template:</label>
              <input type="text" value={libUrlTemplate} onChange={(e) => setLibUrlTemplate(e.target.value)} />
            </div>
          </div>
        )}
        <div className="actions-group">
          <button onClick={handleExportSVG}>Export SVG</button>
          <button onClick={handleExportPNG}>Export PNG</button>
        </div>
      </div>

      <NodeBoxPlayer
        userId={userId}
        projectId={projectId}
        item=""
        values={values}
        publishedUrlTemplate={publishedUrlTemplate}
        assetsUrlTemplate={assetsUrlTemplate}
        libUrlTemplate={libUrlTemplate}
      />
    </main>
  );
}

export default App;
