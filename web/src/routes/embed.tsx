import { useParams } from "wouter";
import { config, NodeBoxPlayer } from "@ndbx/runtime";

if (import.meta.env.DEV) {
  config.apiRoot = document.location.origin;
  config.bareImportReplacer = (name: string) => import.meta.resolve(name);
}

export default function Embed() {
  let { userId, projectId, item } = useParams();
  item = item || "Main";

  function handleProjectError(message: string) {
    console.error(message);
  }

  return <NodeBoxPlayer userId={userId!} projectId={projectId!} item={item} onProjectError={handleProjectError} />;
}
