import React, { FormEvent, useState, useRef, useEffect } from "react";
import { useAuth } from "../auth-context";
import Icon from "../components/icon";
import { project, projectModalVisible, setProjectTitle, setProjectScope, scopePrivateNoPlan } from "./signals";
import FullscreenModal from "../components/fullscreen-modal";
import { SubmitField, TextAreaField } from "../components/fields";
import InlineMessage from "../components/inline-message";
import { useParams } from "wouter";

const drawAttention = (element: HTMLElement) => {
  element.animate(
    [
      { transform: "scale(1)", background: "#27272A" },
      { transform: "scale(1.2)" },
      { transform: "scale(1/1.2)" },
      { transform: "scale(1.2)", background: "none" },
      { transform: "scale(1/1.2)" },
      { transform: "scale(1.2)" },
      { transform: "scale(1)", background: "#27272A" },
    ],
    {
      duration: 500,
      easing: "ease",
      iterations: 2,
    },
  );
};

export const drawAttentionProjectScope = () => {
  const projectScope = document.getElementById("projectScope");
  if (projectScope) {
    drawAttention(projectScope);
  }
};

export default function ProjectModal() {
  const { userId, version } = useParams();
  const { userId: currentUserId, membership } = useAuth()!;
  const isOwner = currentUserId === userId;
  const readOnly = !isOwner || scopePrivateNoPlan.value || version !== "dev";
  const scope = project.value!.scope || "public";

  const [error, setError] = React.useState<string | null>(null);
  const [title, setTitle] = useState(project.value!.title || "");
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const [isChangingScope, setChangingScope] = useState(false);

  const initialTitleRef = useRef(title);
  const initialScopeRef = useRef(scope);

  useEffect(() => {
    initialTitleRef.current = project.value!.title || "";
    initialScopeRef.current = project.value!.scope || "public";
  }, [project.value]);

  const hasValueTitleChanged = title !== initialTitleRef.current;

  const handleClose = () => {
    projectModalVisible.value = false;
  };

  const handleSubmitTitle = (e: FormEvent) => {
    e.preventDefault();
    try {
      setProjectTitle(title);
      handleClose();
    } catch (err) {
      console.error(err);
      setError((err as Error)?.message);
    }
  };

  const handleChangeProjectScope = async (e?: FormEvent) => {
    if (e) e.preventDefault();
    setChangingScope(true);
    const newScope = scope === "public" ? "private" : "public";
    try {
      await setProjectScope(newScope);
    } catch (err) {
      console.error(err);
      setError((err as Error)?.message);
    } finally {
      setChangingScope(false);
    }
  };

  return (
    <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={handleClose}>
      <main className="flex flex-row h-full w-full relative">
        <div className="absolute top-2 right-2 cursor-pointer">
          <Icon name="x" onClick={handleClose} size={24} />
        </div>
        <div className="modal-content flex-1 bg-zinc-900 px-8">
          <h1 className="mt-2 mb-6 font-bold text-sm">Project Settings</h1>
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}

          <div className="flex flex-col gap-2 mb-4">
            <form onSubmit={handleSubmitTitle}>
              <TextAreaField
                disabled={!isOwner}
                name="title"
                label="Project Title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className=""
              />
              <div className="flex justify-center mb-2">
                <SubmitField name="saveTitle" label="Save" disabled={!isOwner || !hasValueTitleChanged} />
              </div>
            </form>
          </div>
          <div className="flex flex-col gap-2 mb-4">
            <h2 className="text-sm font-bold mb-1">Project Visibility</h2>
            <div className="flex items-center bg-zinc-800 rounded-md p-3 mb-2">
              <div className="flex items-center flex-1">
                <Icon name={scope === "private" ? "lock" : "unlock"} size={24} className="mr-2" />
                <div>
                  <p className="font-medium">{scope === "private" ? "Private" : "Public"}</p>
                  <p className="text-xs text-zinc-400">
                    {scope === "private" ? "Only you can see this project" : "Anyone can see this project"}
                  </p>
                </div>
              </div>
              {membership.membership_type === "plus" ? (
                <button
                  id="projectScope"
                  type="button"
                  onClick={() => setShowConfirmDialog(true)}
                  disabled={!isOwner}
                  className={`px-3 py-1 text-sm rounded transition-colors ${
                    isOwner ? "bg-zinc-700 hover:bg-zinc-600" : "bg-zinc-800 text-zinc-500 cursor-not-allowed"
                  }`}
                >
                  Change to {scope === "private" ? "public" : "private"}
                </button>
              ) : (
                <span className="text-xs text-zinc-400">
                  <a href="/membership" className="text-blue-400 hover:underline">
                    Upgrade to Plus
                  </a>
                </span>
              )}
            </div>

            {membership.membership_type !== "plus" && (
              <p className="text-xs text-zinc-400 mb-2">
                Plus members can switch between private and public projects.
                <a href="/membership" className="text-blue-400 ml-1 hover:underline">
                  Upgrade to Plus
                </a>
              </p>
            )}

            {showConfirmDialog && (
              <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                <div className="bg-zinc-800 p-4 rounded-md max-w-md">
                  <h3 className="font-bold mb-2">Change visibility to {scope === "private" ? "public" : "private"}?</h3>
                  <p className="mb-4 text-sm">
                    {scope === "private"
                      ? "This will make your project visible to anyone with the link. Are you sure?"
                      : "This will make your project private and only visible to you. Are you sure?"}
                  </p>
                  <div className="flex justify-end gap-2">
                    <button
                      onClick={() => setShowConfirmDialog(false)}
                      className="px-3 py-1 rounded bg-zinc-700 hover:bg-zinc-600"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={() => {
                        handleChangeProjectScope();
                        setShowConfirmDialog(false);
                      }}
                      disabled={!isOwner}
                      className={`px-3 py-1 rounded ${
                        isOwner ? "bg-blue-600 hover:bg-blue-700" : "bg-blue-800 text-blue-300 cursor-not-allowed"
                      }`}
                    >
                      {isChangingScope
                        ? "Changing scope..."
                        : `Change scope to ${scope === "private" ? "public" : "private"}`}
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </main>
    </FullscreenModal>
  );
}
