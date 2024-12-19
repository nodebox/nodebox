import { useState } from "react";
import FullscreenModal from "../components/fullscreen-modal";
import Icon from "../components/icon";
import { project, publishProject, shareModalVisible, exportAllExampleNetworks } from "./signals";
import { sleep, useRelativeTime } from "../util";
import { useParams } from "wouter";
import { useAuth } from "../auth-context";
import InlineMessage from "../components/inline-message";

enum PublishState {
  Publish = "Publish",
  Publishing = "Publishing...",
  Published = "Published",
  Error = "Error",
}

export default function ShareModal() {
  const { userId, version = "dev" } = useParams();
  const { userId: currentUserId, membership } = useAuth()!;
  const notYourProject = currentUserId !== userId;
  const noPlan = !membership || membership.membership_type !== "plus";
  const readOnly = notYourProject || noPlan || version !== "dev";
  const isPrivate = project.value?.scope === "private";
  const isPublished = project.value?.isPublished;
  const publishDate = project.value?.publishDate;

  const [publishState, setPublishState] = useState<PublishState>(PublishState.Publish);
  const [error, setError] = useState<string | null>(null);

  const handlePublish = async () => {
    if (readOnly) return;
    if (publishState !== PublishState.Publish) return;
    setPublishState(PublishState.Publishing);
    try {
      await publishProject();
      project.value!.isPublished = true;
      project.value!.publishDate = new Date().toISOString();
      if (userId === "example") {
        await exportAllExampleNetworks();
      }
      setPublishState(PublishState.Published);
      await sleep(2000);
      setPublishState(PublishState.Publish);
    } catch (err) {
      setPublishState(PublishState.Error);
      console.error(err);
      setError((err as Error)?.message);
      await sleep(3000);
      setPublishState(PublishState.Publish);
    }
  };

  const handleClose = () => {
    shareModalVisible.value = false;
  };

  const handleCopyEmbedCode = () => {
    const embedInput = document.getElementById("embed") as HTMLInputElement;
    embedInput.select();
    navigator.clipboard.writeText(embedInput.value);
  };

  const handleCopyLink = () => {
    const link = document.location.href;
    navigator.clipboard.writeText(link);
  };

  const embedCode = `<iframe src="https://nodebox.live/embed/${userId}/${project.value?.id}" width="100%" height="100%" frameborder="0"></iframe>`;
  const relativePublishTime = useRelativeTime(isPublished ? publishDate! : new Date().toISOString());
  return (
    <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={handleClose}>
      <main className="bg-zinc-900">
        <div className="flex justify-between items-center px-4 py-2 border-b border-b-zinc-700 mb-2">
          <h1 className="text-xs">Share Project</h1>
          <div className="flex gap-2 items-center">
            <span
              className="text-xs text-blue-400 font-bold flex items-center gap-1 cursor-pointer"
              onClick={handleCopyLink}
            >
              <Icon name="link" />
              Copy Link
            </span>
            <Icon name="x" className="cursor-pointer" onClick={handleClose} size={24} />
          </div>
        </div>
        <div className="modal-content flex-1 bg-zinc-900 py-2 px-4 pb-4">
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}
          <h2 className="text-sm font-bold mb-2">Embed</h2>
          <div className="flex items-center gap-2">
            <input
              className="font-mono flex h-10 w-full rounded-md border border-zinc-700 bg-transparent px-2 py-2 text-xs focus:outline-none focus:border-blue-400"
              id="embed"
              readOnly={true}
              value={embedCode}
              onClick={(e) => (e.target as HTMLInputElement).select()}
            ></input>
            <div
              className="rounded-md border-zinc-700 bg-zinc-700 w-10 h-10 flex items-center justify-center cursor-pointer"
              onClick={handleCopyEmbedCode}
            >
              <Icon name="copy" className="text-zinc-300" size={24} />
            </div>
          </div>
          <h2 className="text-sm font-bold mb-2 mt-6">Publish</h2>
          <p className="text-xs text-zinc-500">
            Publishing a project makes it available for embedding using the{" "}
            <a href="https://new.nodebox.live/guide/embedding-nodebox" target="_blank" className="underline">
              NodeBox Player
            </a>
            .
          </p>
          {/* Publishing */}
          <div className="flex justify-end items-center gap-2 mt-4">
            <span className="flex flex-col items-start gap-1">
              {readOnly && (
                <span className="text-xs text-zinc-400 flex items-center gap-1">
                  <Icon name="lock" />
                  {notYourProject && <span>You can only publish your own projects.</span>}
                  {noPlan && <span>You need a NodeBox Plus subscription to publish projects.</span>}
                </span>
              )}
              {isPrivate && !isPublished && (
                <span className="text-xs text-red-400 flex items-center gap-1">
                  <Icon name="error" />
                  This is a private project. Publishing it will make it viewable.
                </span>
              )}
              {isPrivate && isPublished && (
                <span className="text-xs text-zinc-500 flex items-center gap-1">
                  <Icon name="error" />
                  This is a private project, but is viewable because it is published.
                </span>
              )}
              {isPublished && publishDate && (
                <span className="text-xs text-zinc-500 flex items-center gap-1">
                  <Icon name="check-circle" />
                  Last published {relativePublishTime}.
                </span>
              )}
            </span>
            <button
              className="text-xs bg-blue-600 hover:bg-blue-700 text-white px-2 py-2 rounded"
              onClick={handlePublish}
              disabled={readOnly || publishState !== PublishState.Publish}
            >
              {publishState as string}
            </button>
          </div>
        </div>
      </main>
    </FullscreenModal>
  );
}
