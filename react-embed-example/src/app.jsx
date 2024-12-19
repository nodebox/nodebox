import { useEffect, useState } from "react";
import { NodeBoxPlayer } from "@ndbx/runtime";

function App() {
  const values = {
    width: 200,
  };

  return (
    <main className="app__main">
      <h1 className="app__title">NodeBox Player Embed Demo</h1>
      <NodeBoxPlayer userId="example" projectId="scatter" item="" values={values} />
    </main>
  );
}

export default App;
