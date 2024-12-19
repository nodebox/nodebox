import { useEffect, useState } from "react";
import { useParams } from "wouter";
import { v4 as uuidv4 } from "uuid";

import Header from "./header";
import Outliner from "./outliner";
import Properties from "./properties";
import InlineMessage from "../components/inline-message";
import {
  cx,
  error,
  loadProject,
  activeItem,
  createItemModalVisible,
  createNodeModalVisible,
  addDependencyModalVisible,
  assetsModalVisible,
  projectModalVisible,
  createOutletModalVisible,
  parameterMetaPanelVisible,
  shareModalVisible,
} from "./signals";
import CodeEditor from "./code-editor";
import NetworkEditor from "./network-editor";
import Splitter from "../components/splitter";
import Viewer from "./viewer";
import AssetsModal from "./assets-modal";
import AddDependencyModal from "./add-dependency-modal";
import CreateItemModal from "./create-item-modal";
import CreateNodeModal from "./create-node-modal";
import ProjectModal from "./project-modal";
import CreateOutletModal from "./create-outlet-modal";
import ShareModal from "./share-modal";
import ParameterMetaPanel from "./parameter-meta-panel";

import FullscreenModal from "../components/fullscreen-modal";
import Icon from "../components/icon";

export default function Editor() {
  let { userId, projectId, version } = useParams(); // Use params to get userId and projectId
  const [isProjectOpenElsewhere, setIsProjectOpenElsewhere] = useState(false);
  const [projectChecked, setProjectChecked] = useState(false);
  const [confirmedProjectId, setConfirmedProjectId] = useState<string | null>(null);
  const [showConfirmationModal, setShowConfirmationPopup] = useState(false);
  const [windowId] = useState(uuidv4()); // Generate a unique windowId for this window/tab
  const isLoading = !(cx.value || error.value);
  version = version ? version : "dev";
  useEffect(() => {
    const broadcast = new BroadcastChannel("project_editor_channel");

    // Check if the project is open in another tab, using both userId and projectId
    const checkIfProjectOpen = () => {
      if (userId && projectId) {
        broadcast.postMessage({ type: "check", userId, projectId, version });
      }
    };

    // Listen for messages from other tabs
    broadcast.onmessage = (event) => {
      if (event.data.type === "check" && event.data.userId === userId && event.data.projectId === projectId) {
        // Respond to check by notifying this tab has opened the project
        broadcast.postMessage({ type: "response", userId, projectId, version });
      }
      if (event.data.type === "response" && event.data.userId === userId && event.data.projectId === projectId) {
        setIsProjectOpenElsewhere(true);
      }
      if (
        event.data.type === "redirect" &&
        event.data.userId === userId &&
        event.data.projectId === projectId &&
        event.data.windowId !== windowId
      ) {
        window.location.href = `/${userId}`;
      }
    };

    // Send the initial check message
    checkIfProjectOpen();

    // Set the project as checked after a short delay to allow responses
    const timeout = setTimeout(() => {
      setProjectChecked(true);
    }, 500); // Wait 500ms for other tabs to respond

    return () => {
      broadcast.close();
      clearTimeout(timeout);
    };
  }, [userId, projectId, version, windowId]);

  // Only load the project if it's not open elsewhere and the check is complete
  useEffect(() => {
    if (projectChecked && userId && projectId) {
      // Ensure userId and projectId are available
      if (isProjectOpenElsewhere) {
        setShowConfirmationPopup(true); // Show confirmation only if open elsewhere
      } else {
        // If not open elsewhere, load the project immediately
        (async () => {
          await loadProject(userId, projectId, version);
          setConfirmedProjectId(projectId);
        })();
      }
    }
  }, [projectChecked, isProjectOpenElsewhere, userId, projectId, version]);

  // Handle loading the project after confirmation
  const handleConfirmLoadProject = async () => {
    const broadcast = new BroadcastChannel("project_editor_channel");
    setShowConfirmationPopup(false);

    if (userId && projectId) {
      setConfirmedProjectId(projectId);

      // Send a redirect message to all other tabs with this project open, but include this window's id
      broadcast.postMessage({ type: "redirect", userId, projectId, version, windowId });

      await loadProject(userId, projectId, version);
    }

    broadcast.close();
  };

  const handleCancelLoadProject = () => {
    setShowConfirmationPopup(false);
    window.history.back(); // Go back to the previous page or stay where they are
  };

  useEffect(() => {
    if (confirmedProjectId && userId) {
      (async () => {
        await loadProject(userId, confirmedProjectId, version);
      })();
    }
  }, [confirmedProjectId, userId]);

  return (
    <div className="w-screen h-screen overflow-hidden">
      {showConfirmationModal && (
        <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={handleCancelLoadProject}>
          <main className="">
            <div className="absolute top-2 right-2 cursor-pointer">
              <Icon name="x" onClick={handleCancelLoadProject} size={24} />
            </div>
            <div className="modal-content flex-1 bg-zinc-900 px-10 py-10">
              <h1 className="text-xl text-center mb-2">This project is already open in another window</h1>
              <h2 className="text-xs text-center mb-7">
                Opening this project in multiple windows can cause conflicts and data loss.
              </h2>
              <div className="flex justify-around gap-2">
                <button
                  onClick={handleConfirmLoadProject}
                  className="border bg-blue-700 hover:bg-zinc-900 border-zinc-700 rounded p-4 text-zinc-100 w-full cursor-pointer"
                >
                  Load Project Here
                  <br />
                  <span className="text-xs">This closes the other window</span>
                </button>
                <button
                  onClick={handleCancelLoadProject}
                  className="border bg-zinc-700 hover:bg-zinc-900 border-zinc-700 rounded p-4 text-zinc-100 w-full cursor-pointer"
                >
                  Cancel and Go Back
                </button>
              </div>
            </div>
          </main>
        </FullscreenModal>
      )}

      <Header userId={userId!} projectId={projectId!} version={version!} showReadOnlyWarning={false} />
      {isLoading && (
        <div className="w-full h-full flex justify-center items-center">
          <p className="text-xl text-zinc-500">Loading project...</p>
        </div>
      )}
      {error.value && (
        <div className="z-30 m-auto w-screen fixed flex justify-center pointer-events-none">
          <InlineMessage key={Date.now()}>Project error: {error.value}</InlineMessage>
        </div>
      )}
      {cx.value && (
        <>
          <div className="w-full flex flex-col" style={{ height: `calc(100vh - 48px)` }}>
            <Splitter direction="horizontal" defaultSplit={20} className="flex-grow overflow-hidden">
              <Splitter
                direction="vertical"
                defaultSplit={50}
                className="h-full overflow-hidden"
                firstChildClassName="flex flex-col"
                secondChildClassName="flex flex-col"
              >
                <Outliner />
                <Properties />
              </Splitter>
              <Splitter direction="horizontal" defaultSplit={50} className="h-full overflow-hidden">
                <>
                  {activeItem.value?.type === "FUNCTION" && <CodeEditor />}
                  {activeItem.value?.type === "NETWORK" && <NetworkEditor />}
                </>
                <Viewer />
              </Splitter>
            </Splitter>
          </div>
          {assetsModalVisible.value && <AssetsModal />}
          {addDependencyModalVisible.value && <AddDependencyModal />}
          {createItemModalVisible.value && <CreateItemModal />}
          {createNodeModalVisible.value && <CreateNodeModal />}
          {projectModalVisible.value && <ProjectModal />}
          {createOutletModalVisible.value && <CreateOutletModal />}
          {parameterMetaPanelVisible.value && <ParameterMetaPanel />}
          {shareModalVisible.value && <ShareModal />}
        </>
      )}
    </div>
  );
}
