import React, { FormEvent, useState, useRef, useEffect } from "react";
import { useAuth } from "../auth-context";
import Icon from "../components/icon";
import {
  project,
  projectModalVisible,
  publishProject,
  setProjectDescription,
  setProjectScope,
  exportAllExampleNetworks,
  scopePrivateNoPlan,
} from "./signals";
import FullscreenModal from "../components/fullscreen-modal";
import { SubmitField, TextAreaField } from "../components/fields";
import InlineMessage from "../components/inline-message";
import { sleep } from "../util";
import { useParams } from "wouter";

enum PublishState {
  Publish = "Publish",
  RePublish = "Re-Publish",
  Publishing = "Publishing...",
  Published = "Published",
  Error = "Error",
}

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
  const readOnly = currentUserId !== userId || scopePrivateNoPlan.value || version !== "dev";

  const [error, setError] = React.useState<string | null>(null);
  const [publishState, setPublishState] = React.useState<PublishState>(
    project.value?.isPublished === true ? PublishState.RePublish : PublishState.Publish,
  );
  const [description, setDescription] = useState(project.value!.description || "");
  const [scope, setScope] = useState(project.value!.scope || "public");

  const initialDescriptionRef = useRef(description);
  const initialScopeRef = useRef(scope);

  useEffect(() => {
    initialDescriptionRef.current = project.value!.description || "";
    initialScopeRef.current = project.value!.scope || "public";
  }, [project.value]);

  const hasValueDescriptionChanged = description !== initialDescriptionRef.current;
  const hasValueScopeChanges = scope !== initialScopeRef.current;

  const handleClose = () => {
    projectModalVisible.value = false;
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    try {
      setProjectDescription(description);
      handleClose();
    } catch (err) {
      console.error(err);
      setError((err as Error)?.message);
    }
  };

  const handleSubmitScope = (e: FormEvent) => {
    e.preventDefault();
    try {
      setProjectScope(scope);
      handleClose();
    } catch (err) {
      console.error(err);
      setError((err as Error)?.message);
    }
  };

  const handlePublish = async () => {
    if (publishState !== PublishState.Publish && publishState !== PublishState.RePublish) return;
    setPublishState(PublishState.Publishing);
    try {
      await publishProject();
      if (userId === "example") {
        await exportAllExampleNetworks();
      }
      setPublishState(PublishState.Published);
      await sleep(2000);
      setPublishState(PublishState.RePublish);
    } catch (err) {
      setPublishState(PublishState.Error);
      console.error(err);
      setError((err as Error)?.message);
      await sleep(3000);
      setPublishState(PublishState.Publish);
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
          <div className="flex flex-col gap-2 mb-2 p-4 border rounded-sm border-zinc-600">
            {userId === "example" ||
            (membership.membership_type === "plus" && userId === currentUserId && (version || "dev") === "dev") ? (
              <>
                <div className="flex justify-start px-2">
                  {publishState === PublishState.RePublish && (
                    <div className="flex justify-between">
                      The published version can be found&nbsp;
                      <b>
                        <u>
                          <a href={`/${userId}/${project.value!.id}/published`}>here</a>
                        </u>
                      </b>
                      .
                    </div>
                  )}
                </div>
                <div className="flex justify-end px-2">
                  <button
                    className="text-xs bg-green-500 text-white px-2 py-1 rounded"
                    onClick={handlePublish}
                    disabled={publishState !== PublishState.Publish && publishState !== PublishState.RePublish}
                  >
                    {publishState as string}
                  </button>
                </div>
              </>
            ) : (
              <>
                {userId !== currentUserId ? (
                  <div className="text-zinc-600">You can only publish your own projects.</div>
                ) : (
                  <div>
                    Publishing projects is a premium feature. Click{" "}
                    <u>
                      <b>
                        <a href="/membership" target="_blank" rel="noopener noreferrer">
                          here
                        </a>
                      </b>
                    </u>{" "}
                    for more details (opens in new window).
                  </div>
                )}
              </>
            )}
          </div>

          <div className="flex flex-col gap-2 mb-2 p-4 border rounded-sm border-zinc-600">
            <form onSubmit={handleSubmit}>
              <TextAreaField
                disabled={readOnly}
                name="description"
                label="Description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className={readOnly ? "disabled-textarea" : ""}
              />
              <div className="flex justify-center mb-2">
                <SubmitField name="save" label="Save" disabled={readOnly || !hasValueDescriptionChanged} />
              </div>
            </form>
          </div>
          {
            <div className="flex flex-col gap-2 mb-4 p-4 border rounded-sm border-zinc-600">
              <div>
                <form>
                  <button
                    id="projectScope"
                    type="button"
                    disabled={membership.membership_type !== "plus"}
                    onClick={() => {
                      const toggle = scope === "private" ? "public" : "private";
                      setScope(toggle);
                    }}
                    className="flex items-center gap-2 px-3 py-1 text-sm rounded hover:bg-zinc-700 transition-colors"
                  >
                    <Icon name={scope === "private" ? "lock" : "unlock"} size={24} />
                    <span>{scope.charAt(0).toUpperCase() + scope.slice(1)}</span>
                  </button>
                </form>
              </div>
              <div className="flex justify-center mb-2">
                <SubmitField
                  name="confirm"
                  label="Confirm"
                  onClick={handleSubmitScope}
                  disabled={!hasValueScopeChanges || membership.membership_type === "plus" || scopePrivateNoPlan.value}
                />
              </div>
            </div>
          }
        </div>
      </main>
    </FullscreenModal>
  );
}
