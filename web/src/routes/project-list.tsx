import { useAuth } from "../auth-context";
import { useParams } from "wouter";
import useSWR from "swr";
import apiFetcher from "../lib/api-fetcher";
import InlineMessage from "../components/inline-message";
import LoggedInHeader from "../components/logged-in-header";
import Icon from "../components/icon";
import { apiRoot } from "../config";
import { useState, useEffect, useRef, useCallback } from "react";
import { Menu, MenuItem } from "../components/menu";
import FullscreenModal from "../components/fullscreen-modal";
import Subscription from "../components/subscription";
import Chrome from "@uiw/react-color-chrome";
import { HsvaColor, rgbaToHsva, hsvaToRgba } from "@uiw/color-convert";
import { Paint } from "@ndbx/g";
import { debounce } from "../util";
import { showToast, showToastOnce } from "../util";
interface Project {
  id: string;
  title: string;
  color: string;
  scope: string;
  isPublished?: boolean;
}

interface MousePosition {
  x: number;
  y: number;
}

interface ColorValue {
  r: number;
  g: number;
  b: number;
  a: number;
}

export default function ProjectBrowser() {
  const { userId } = useParams();
  const { userId: authUserId } = useAuth()!;
  const { data, error, isLoading } = useSWR(`${apiRoot}/api/projects/${userId}?userId=${authUserId}`, apiFetcher);
  const [projectList, setProjectList] = useState<Project[]>([]);
  const [contextMenu, setContextMenu] = useState<MousePosition | null>(null);
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [enteredProjectName, setEnteredProjectName] = useState("");
  const [selectedColor, setSelectedColor] = useState<HsvaColor>({ h: 0, s: 0, v: 100, a: 1 });
  const pickerRef = useRef<HTMLDivElement | null>(null);
  const [pickerVisible, setPickerVisible] = useState(false);
  const [pickerPosition, setPickerPosition] = useState<MousePosition>({ x: 0, y: 0 });
  const [isUpdatingColor, setIsUpdatingColor] = useState(false);

  const handleDeleteProject = async (projectId: string) => {
    const res = await fetch(`${apiRoot}/api/projects/${userId}/${projectId}`, {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    });

    if (res.ok) {
      setProjectList(projectList.filter((project) => project.id !== projectId));
    } else {
      console.error("Failed to delete project:", await res.json());
    }
  };

  useEffect(() => {
    if (data) {
      if (data.projects) {
        if (data.userId === null) {
          showToast("This user does not exist.");
        } else {
          setProjectList(data.projects);
        }
      }
      if (data.membership_message) {
        showToastOnce(data.membership_message);
      }
    }
  }, [data]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (pickerRef.current && !pickerRef.current.contains(event.target as Node)) {
        setPickerVisible(false);
      }
    }

    if (pickerVisible) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [pickerVisible]);

  useEffect(() => {
    if (selectedProjectId) {
      const selectedProject = projectList.find((project) => project.id === selectedProjectId);
      if (selectedProject) {
        const element = document.getElementById(`project-${selectedProjectId}`);
        if (element) {
          const { r, g, b, a } = hsvaToRgba(selectedColor);
          element.style.backgroundColor = `rgba(${r}, ${g}, ${b}, ${a})`;
        }
      }
    }
  }, [selectedColor, selectedProjectId, projectList]);

  useEffect(() => {
    if (pickerVisible && pickerRef.current) {
      const divs = pickerRef.current.querySelectorAll("div[style*='border-style: solid; position: absolute;']");
      divs.forEach((div) => {
        div.remove();
      });
    }
  }, [pickerVisible]);

  if (error) {
    return (
      <main id="project-browser" className="px-2 md:px-8 pt-4">
        <h1 className="text-xl">Projects</h1>
        <InlineMessage key={Date.now()}>
          The user could not be loaded. Please check your connection and try again.
        </InlineMessage>
      </main>
    );
  }

  const handleContextMenu = (e: React.MouseEvent, projectId: string) => {
    e.preventDefault();
    setSelectedProjectId(projectId);
    setEnteredProjectName("");
    setContextMenu({ x: e.clientX + 2, y: e.clientY - 6 });

    const selectedProject = projectList.find((project) => project.id === projectId);
    if (selectedProject) {
      const { r, g, b, a } = Paint.parse(selectedProject.color) as unknown as ColorValue;
      const rgbColor = { r: r * 255, g: g * 255, b: b * 255, a };
      setSelectedColor(rgbaToHsva(rgbColor));
    }
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setEnteredProjectName("");
  };

  const handleDeleteClick = () => {
    setContextMenu(null);
    setIsModalOpen(true);
  };

  function handleSetColorClick() {
    setContextMenu(null);

    const PICKER_WIDTH = 225;
    const PICKER_HEIGHT = 240;
    const viewportMargin = 10;

    const clickedElement = document.getElementById(`project-${selectedProjectId}`);
    if (!clickedElement) return;

    const rect = clickedElement.getBoundingClientRect();

    let x = rect.left;
    let y = rect.bottom + 2;

    if (x + PICKER_WIDTH > window.innerWidth - viewportMargin) {
      x = Math.max(viewportMargin, window.innerWidth - PICKER_WIDTH - viewportMargin);
    }

    if (y + PICKER_HEIGHT > window.innerHeight - viewportMargin) {
      y = rect.top - PICKER_HEIGHT - 2;
    }

    setPickerPosition({ x, y });
    setPickerVisible(true);
  }

  const confirmDeleteProject = () => {
    setSelectedProjectId(null);
    if (selectedProjectId) {
      handleDeleteProject(selectedProjectId).catch((error) => {
        console.error("Error deleting project:", error);
      });
    }
    handleCloseMenu();
    setIsModalOpen(false);
  };

  const handleCloseMenu = () => {
    setContextMenu(null);
  };

  async function handleOpenProject(project: Project) {
    document.location = `/${userId}/${project.id}`;
  }

  async function handleOpenPublishedProject(project: Project) {
    document.location = `/${userId}/${project.id}/published`;
  }

  const updateProjectColor = async (colorString: string) => {
    if (!selectedProjectId || isUpdatingColor) return;

    setIsUpdatingColor(true);
    try {
      const res = await fetch(`${apiRoot}/api/set-project-color/${userId}/${selectedProjectId}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
        body: JSON.stringify({ color: colorString }),
      });

      if (!res.ok) {
        throw new Error("Failed to update project color");
      }

      setProjectList((projects) =>
        projects.map((p) => (p.id === selectedProjectId ? { ...p, color: colorString } : p)),
      );
    } catch (error) {
      console.error("Error updating project color:", error);
      showToast("Failed to update project color", "error");
    } finally {
      setIsUpdatingColor(false);
    }
  };

  const debouncedUpdateColor = useCallback(
    debounce((colorString: string) => {
      updateProjectColor(colorString);
    }, 500),
    [selectedProjectId, userId],
  );

  function handleColorChange(color: { hsva: HsvaColor }) {
    setSelectedColor(color.hsva);
    const { r, g, b, a } = hsvaToRgba(color.hsva);
    const colorString = `rgba(${r}, ${g}, ${b}, ${a})`;
    debouncedUpdateColor(colorString);
  }

  const selectedProject = projectList.find((project) => project.id === selectedProjectId);

  function getContrastTextColor(backgroundColor: string) {
    let r: number, g: number, b: number;
    if (backgroundColor.startsWith("#")) {
      const hex = backgroundColor.replace("#", "");
      r = parseInt(hex.substr(0, 2), 16);
      g = parseInt(hex.substr(2, 2), 16);
      b = parseInt(hex.substr(4, 2), 16);
    } else if (backgroundColor.startsWith("rgba")) {
      const rgba = backgroundColor.match(/[\d.]+/g);
      if (!rgba || rgba.length < 3) return "text-zinc-900";
      r = parseInt(rgba[0]);
      g = parseInt(rgba[1]);
      b = parseInt(rgba[2]);
    } else {
      try {
        const color = Paint.parse(backgroundColor) as unknown as ColorValue;
        r = color.r * 255;
        g = color.g * 255;
        b = color.b * 255;
      } catch (error) {
        return "text-gray-900";
      }
    }
    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    return luminance > 0.5 ? "text-zinc-900" : "text-zinc-100";
  }

  return (
    <div id="project-browser" className="flex flex-col h-screen overflow-hidden">
      <LoggedInHeader />
      <main className="flex-1 flex flex-row h-full overflow-hidden">
        <div className="sidebar w-48 bg-zinc-700 flex flex-col h-full justify-end">
          <div className="py-2 text-xs px-4 font-bold">{userId}</div>
          <div className="py-2 text-xs bg-zinc-600 px-4">Projects</div>
          <div className="flex-1"></div>
          <Subscription />
          <a className="py-2 text-xs px-4 flex items-center gap-2" target="_blank" rel="noreferrer" href="/gallery">
            <Icon name="book" />
            Gallery
          </a>
        </div>
        <section className="projects flex-1 p-4 h-full overflow-auto">
          {error && <div className="border border-slate-200 rounded p-2 text-red-500">{error}</div>}
          {isLoading && <div className="border border-slate-200 rounded p-2 text-red-500">Loading...</div>}
          <div className="projects grid grid-cols-2 md:grid-cols-4 gap-3 pb-10">
            {projectList.map((project: Project) =>
              userId === "example" ? (
                <div
                  className="relative inline-block rounded-lg overflow-hidden"
                  key={project.id}
                  onContextMenu={(e) => handleContextMenu(e, project.id)}
                >
                  <a href={`/${userId}/${project.id}`}>
                    <span className="flex w-full text-white bg-zinc-600 px-2 text-sm">{project.title}</span>
                    <span
                      className="block p-2 h-28 text-xs bg-contain bg-no-repeat bg-bottom border-2 rounded-b-lg border-zinc-600"
                      style={{
                        backgroundImage: `url(/api/svg/${userId}/${project.id}/0_1)`,
                      }}
                    ></span>
                  </a>
                </div>
              ) : (
                <div
                  className={`relative inline-block rounded-lg overflow-hidden`}
                  key={project.id}
                  id={`project-${project.id}`}
                  onContextMenu={(e) => handleContextMenu(e, project.id)}
                  style={{ backgroundColor: project.color }}
                >
                  <a
                    onClick={(e) => {
                      // For published projects, handle differently when clicking the check icon
                      if (project.isPublished && (e.target as Element).closest(".check-icon")) {
                        handleOpenPublishedProject(project);
                      } else {
                        handleOpenProject(project);
                      }
                    }}
                  >
                    <span className={`${getContrastTextColor(project.color)} flex w-full px-2 py-1 text-sm`}>
                      {project.title}
                      <span className="ml-auto flex">
                        {project.scope === "private" && <Icon name="lock" className="ml-2" />}
                        {project.isPublished === true && (
                          <Icon name="check-circle" className="ml-2 check-icon cursor-pointer" />
                        )}
                      </span>
                    </span>
                    <span className="block p-2 h-28 text-xs bg-contain bg-no-repeat bg-bottom rounded-b-lg"></span>
                  </a>
                </div>
              ),
            )}
          </div>
        </section>
      </main>

      <Menu open={!!contextMenu} onClose={handleCloseMenu} anchorPosition={contextMenu || { x: 0, y: 0 }}>
        <MenuItem onClick={handleDeleteClick} disabled={authUserId != userId}>
          Delete Project
        </MenuItem>
        <MenuItem onClick={handleSetColorClick} disabled={authUserId != userId}>
          Set Color
        </MenuItem>
      </Menu>

      {isModalOpen && selectedProject && (
        <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={handleModalClose}>
          <main className="flex flex-row h-full w-full relative">
            <div className="absolute top-2 right-2 cursor-pointer">
              <Icon name="x" onClick={handleModalClose} size={24} />
            </div>
            <div className="modal-content flex-1 bg-zinc-900 px-8">
              <h1 className="mt-2 mb-6 font-bold text-sm">Delete Project "{selectedProject.title}"</h1>
              <div className="p-4">
                <p>Type the name of the project to confirm deletion:</p>
                <input
                  className="w-full border border-zinc-600 p-2 rounded bg-zinc-800 text-white mt-4"
                  type="text"
                  value={enteredProjectName}
                  onChange={(e) => setEnteredProjectName(e.target.value)}
                  placeholder={selectedProject.title}
                />
                <div className="mt-4 flex justify-end space-x-2">
                  <button
                    onClick={confirmDeleteProject}
                    className={`border bg-blue-700 hover:bg-zinc-900 border-zinc-700 rounded p-4 text-zinc-100 w-full cursor-pointer ${
                      enteredProjectName === selectedProject.title ? "" : "opacity-50 cursor-not-allowed"
                    }`}
                    disabled={enteredProjectName !== selectedProject.title}
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          </main>
        </FullscreenModal>
      )}

      {pickerVisible && (
        <div className="fixed inset-0 z-40" onClick={(e) => e.stopPropagation()}>
          <div
            ref={pickerRef}
            className="fixed z-50"
            style={{
              left: pickerPosition.x,
              top: pickerPosition.y,
            }}
          >
            <Chrome color={selectedColor} onChange={handleColorChange} className="!bg-zinc-800 !border-zinc-600" />
          </div>
        </div>
      )}
    </div>
  );
}
