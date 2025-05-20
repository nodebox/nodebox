import { useEffect, useState } from "react";
import { NodeBoxPlayer } from "@ndbx/runtime";

const PUBLISHED_URL_TEMPLATE =
  "https://nodeboxtest.s3.amazonaws.com/{{ userId }}/{{ projectId }}/versions/published.json";
const ASSETS_URL_TEMPLATE = "https://nodeboxtest.s3.amazonaws.com/{{ userId }}/{{ projectId }}/blobs/{{ hash }}";
const LIB_URL_TEMPLATE = "https://nodeboxtest.s3.amazonaws.com/{{ userId }}/{{ projectId }}/lib/{{ file }}.js";

// const PUBLISHED_URL_TEMPLATE = "http://localhost:5173/api/published/{{ userId }}/{{ projectId }}";
// const ASSETS_URL_TEMPLATE =
//   "http://localhost:5173/api/projects/{{ userId }}/{{ projectId }}/{{ version }}/assets/{{ hash }}";
// const LIB_URL_TEMPLATE = "http://localhost:5173/api/fn/{{ userId }}/{{ projectId }}/{{ file }}.js";

function App() {
  const values = {
    width: 200,
  };

  return (
    <main className="app__main">
      <h1 className="app__title">NodeBox Player Embed Demo</h1>
      <NodeBoxPlayer
        userId="example"
        projectId="scatter"
        item=""
        values={values}
        publishedUrlTemplate={PUBLISHED_URL_TEMPLATE}
        assetsUrlTemplate={ASSETS_URL_TEMPLATE}
        libUrlTemplate={LIB_URL_TEMPLATE}
      />
    </main>
  );
}

export default App;
