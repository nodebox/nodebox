import React, { FormEvent, useRef } from "react";

import Icon from "../components/icon";
import {
  addDependencyModalVisible,
  userId as currentUserId,
  projectId as currentProjectId,
  project,
  addDependency,
} from "./signals";
import FullscreenModal from "../components/fullscreen-modal";
import { SubmitField, TextField } from "../components/fields";
import InlineMessage from "../components/inline-message";

enum ProjectCheckState {
  Idle = "IDLE",
  Loading = "LOADING",
  Exists = "EXISTS",
  NotFound = "NOT_FOUND",
  Double = "DOUBLE",
  Own = "OWN",
  Error = "ERROR",
}

const isIdentifierValid = (identifier: string): boolean => {
  return !!identifier.match(/^[a-zA-Z0-9_\-\s]{2,40}\/[a-zA-Z0-9_\-\s]{1,40}$/);
};

export default function CreateItemModal() {
  const [identifier, setIdentifier] = React.useState("");
  const [error, setError] = React.useState<string | null>(null);
  const [projectCheckState, setProjectCheckState] = React.useState<ProjectCheckState>(ProjectCheckState.Idle);
  const typeTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const handleClose = () => {
    addDependencyModalVisible.value = false;
  };

  const handleInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setIdentifier(value);
    if (isIdentifierValid(value)) {
      setProjectCheckState(ProjectCheckState.Loading);
    } else {
      setProjectCheckState(ProjectCheckState.Idle);
    }

    if (typeTimeoutRef.current) {
      clearTimeout(typeTimeoutRef.current);
    }
    if (value.trim().length > 0) {
      typeTimeoutRef.current = setTimeout(() => {
        lookupProject(value);
      }, 500);
    }
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    try {
      const [userId, projectId] = identifier.split("/");
      addDependency(userId, projectId);
      handleClose();
    } catch (err) {
      console.error(err);
      setError((err as Error)?.message);
    }
  };

  const lookupProject = async (identifier: string) => {
    if (!isIdentifierValid(identifier)) return;
    setProjectCheckState(ProjectCheckState.Loading);
    const [userId, projectId] = identifier.split("/");
    if (userId === currentUserId.value && projectId === currentProjectId.value) {
      setProjectCheckState(ProjectCheckState.Own);
      return;
    }
    if (identifier in project.value!.dependencies) {
      setProjectCheckState(ProjectCheckState.Double);
      return;
    }
    const url = `/api/projects/${userId}/${projectId}`;
    const res = await fetch(url, { method: "HEAD" });
    if (res.ok) {
      setProjectCheckState(ProjectCheckState.Exists);
    } else {
      if (res.status === 404) {
        setProjectCheckState(ProjectCheckState.NotFound);
      } else {
        setProjectCheckState(ProjectCheckState.Error);
      }
    }
  };

  return (
    <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={handleClose}>
      <main className="flex flex-row h-full w-full relative">
        <div className="absolute top-2 right-2 cursor-pointer">
          <Icon name="x" onClick={handleClose} size={24} />
        </div>
        <div className="modal-content flex-1 bg-zinc-900 px-8">
          <h1 className="mt-2 mb-6 font-bold text-sm">Add Dependency</h1>
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}
          <p className="mb-4">Add another project as a dependency.</p>
          <form onSubmit={handleSubmit}>
            <TextField
              name="id"
              label=""
              value={identifier}
              onChange={handleInput}
              autoFocus={true}
              autoComplete="off"
              placeholder="core/g"
            />
            {projectCheckState === ProjectCheckState.Idle && (
              <p className="text-xs text-zinc-400 mb-4">
                Enter an identifier in the format{" "}
                <span className="font-mono text-blue-400 font-bold">userId/projectId</span>.
              </p>
            )}
            {projectCheckState === ProjectCheckState.Loading && (
              <p className="text-xs text-zinc-400 mb-4">Checking if this project exists...</p>
            )}
            {projectCheckState === ProjectCheckState.Exists && (
              <p className="text-xs text-zinc-400 mb-4">This project exists.</p>
            )}
            {projectCheckState === ProjectCheckState.NotFound && (
              <p className="text-xs text-red-400 mb-4">This project does not exist.</p>
            )}
            {projectCheckState === ProjectCheckState.Double && (
              <p className="text-xs text-red-400 mb-4">The project already has this dependency.</p>
            )}
            {projectCheckState === ProjectCheckState.Own && (
              <p className="text-xs text-red-400 mb-4">You can't add your own project.</p>
            )}
            {projectCheckState === ProjectCheckState.Error && (
              <p className="text-xs text-red-400 mb-4">There was an error in lookup up the project.</p>
            )}

            <div className="flex justify-center mb-6">
              <SubmitField
                name="add"
                label="Add Dependency"
                disabled={projectCheckState !== ProjectCheckState.Exists}
              />
            </div>
          </form>
        </div>
      </main>
    </FullscreenModal>
  );
}
