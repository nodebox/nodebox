import { useEffect, useState } from "react";
import { NodeBoxPlayer } from "@ndbx/runtime";

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
        publishedUrlTemplate="https://nodeboxtest.s3.amazonaws.com/{{ userId }}/{{ projectId }}/versions/published.json"
        assetsUrlTemplate="https://nodeboxtest.s3.amazonaws.com/{{ userId }}/{{ projectId }}/blobs/{{ hash }}"
      />
    </main>
  );
}

export default App;
