import React, { useCallback, useState } from "react";
import clsx from "clsx";
import { useDropzone } from "react-dropzone";

import Icon from "../components/icon";
import {
  assetsModalVisible,
  userId,
  projectId,
  project,
  addAsset,
  deleteAsset,
  assetParameterPath,
  setParameterValue,
} from "./signals";
import FullscreenModal from "../components/fullscreen-modal";
import InlineMessage from "../components/inline-message";
import { Network, findItemById, findNodeById } from "@ndbx/runtime";

async function apiRequest(url: string, body: Record<string, string>) {
  const token = localStorage.getItem("token");
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
  });
  return response;
}

export default function AssetsModal() {
  const [error, setError] = useState<string | null>(null);
  const onDrop = useCallback(async (acceptedFiles: File[]) => {
    setError(null);
    const file = acceptedFiles[0];

    try {
      // Get a presigned URL for the upload
      const response = await apiRequest(`/api/assets/presign/${userId.value}/${projectId.value}`, {
        filename: file.name,
        type: file.type,
      });
      const data = await response.json();
      if (data.status !== "ok") {
        throw new Error(data.message);
      }
      const { url, method, filename } = data;

      // Perform the upload
      const extraHeaders: Record<string, string> = {};
      if (url.startsWith("/api")) {
        extraHeaders["Authorization"] = `Bearer ${localStorage.getItem("token")}`;
        extraHeaders.filename = filename;
      } else {
        extraHeaders["x-amz-acl"] = "public-read";
      }
      const uploadResponse = await fetch(url, {
        method,
        body: file,
        headers: {
          "Content-Type": file.type,
          ...extraHeaders,
        },
      });

      if (!uploadResponse.ok) {
        throw new Error(`Failed to upload file: ${uploadResponse.statusText}`);
      }

      // Add the asset to the project
      addAsset(filename);
    } catch (error) {
      console.error("Error uploading file:", error);
      setError(`Failed to upload file: ${error}`);
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({ onDrop });

  const handleClose = () => {
    assetsModalVisible.value = false;
  };

  const handleSelectFile = (filename: string) => {
    console.log("Selected file:", filename);
    const [networkId, nodeId, parameterName] = assetParameterPath.value.split("/");
    const network = findItemById(project.value!, networkId) as Network;
    if (!network) {
      setError(`Network not found: ${networkId}`);
      return;
    }
    const node = findNodeById(network, nodeId);
    if (!node) {
      console.error("Node not found:", nodeId);
      return;
    }
    setParameterValue(network, node, parameterName, { type: "VALUE", value: filename });
    handleClose();
  };

  const assetFilenames = Object.keys(project.value!.assets || {});

  return (
    <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={handleClose}>
      <main className="flex flex-row h-full w-full relative">
        <div className="absolute top-2 right-2 cursor-pointer">
          <Icon name="x" onClick={handleClose} size={24} />
        </div>
        <div className="modal-content flex-1 bg-zinc-900 px-8">
          <h1 className="mt-2 mb-6 font-bold text-sm">Assets</h1>
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}
          <div
            {...getRootProps()}
            className={clsx(
              "bg-zinc-800 shadow-inner text-zinc-500 px-4 py-2 text-xs border-2 cursor-pointer rounded",
              {
                "border-blue-500": isDragActive,
                "border-zinc-800": !isDragActive,
              },
            )}
          >
            <input {...getInputProps()} />
            Drag and drop to upload or <span className="text-zinc-400 underline">choose file</span>
          </div>
          <div className="h-64 overflow-x-hidden overflow-y-auto">
            <div className="flex flex-wrap gap-2 p-2 justify-start items-start">
              {assetFilenames.map((filename) => (
                <Asset
                  key={filename}
                  filename={filename}
                  setError={setError}
                  onClick={() => handleSelectFile(filename)}
                />
              ))}
            </div>
          </div>
        </div>
      </main>
    </FullscreenModal>
  );
}

function Asset({
  filename,
  onClick,
  setError,
}: {
  filename: string;
  onClick: () => void;
  setError: (error: string) => void;
}) {
  // const deleteAsset = useEditorStore((state) => state.deleteAsset);
  const [isOver, setIsOver] = React.useState(false);
  const trashStyles = clsx("text-zinc-500 hover:text-zinc-300", {
    "opacity-0": !isOver,
    "opacity-100": isOver,
  });

  async function handleDelete() {
    if (confirm("Are you sure you want to delete this asset?")) {
      try {
        await deleteAsset(filename);
      } catch (error) {
        setError((error as Error).message);
      }
    }
  }

  return (
    <div
      className="flex-shrink-0 w-52 flex flex-row items-center gap-2 h-10 p-2 hover:bg-zinc-600 rounded cursor-pointer overflow-hidden"
      onMouseEnter={() => setIsOver(true)}
      onMouseLeave={() => setIsOver(false)}
    >
      <Icon name="file" className="text-zinc-300" onClick={onClick} />
      <span className="text-sm text-zinc-300 hover:text-zinc-200 flex-1 truncate" onClick={onClick}>
        {filename}
      </span>
      <Icon name="trash" className={trashStyles} onClick={handleDelete} />
    </div>
  );
}
