import React from "react";
import { useParams } from "wouter";
import clsx from "clsx";
import InlineMessage from "../components/inline-message";
import LoggedInHeader from "../components/logged-in-header";
import { apiRoot } from "../config";

export default function RawEditor() {
  const { userId, projectId } = useParams();
  const [initialSource, setInitialSource] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);
  const [isSaving, setIsSaving] = React.useState(false);
  const [validationError, setValidationError] = React.useState<string | null>(null);
  const [lines, setLines] = React.useState("");
  const textAreaRef = React.useRef<HTMLTextAreaElement>(null);
  const lineNumbersRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    async function load() {
      setError(null);
      const res = await fetch(`${apiRoot}/api/projects/${userId}/${projectId}/dev`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      });
      if (res.status !== 200) {
        setError("Failed to load project.");
        return;
      }
      const json = await res.json();
      const source = JSON.stringify(json.project, null, 2);
      setIsLoading(false);
      setInitialSource(source);
      updateLineNumbers(source);
    }
    load();
  }, [userId, projectId]);

  if (isLoading) {
    return (
      <main id="project-browser" className="px-2 md:px-8 pt-4">
        <h1 className="text-xl">
          Loading {userId}/{projectId}...
        </h1>
      </main>
    );
  }

  if (error) {
    return (
      <main id="project-browser" className="px-2 md:px-8 pt-4">
        <h1 className="text-xl">
          Project {userId}/{projectId}
        </h1>
        <InlineMessage key={Date.now()}>
          The project could not be loaded. Please check your connection and try again.
        </InlineMessage>
      </main>
    );
  }

  function handleChange(e: React.ChangeEvent<HTMLTextAreaElement>) {
    try {
      JSON.parse(e.target.value);
      setValidationError(null);
    } catch (error) {
      setValidationError((error as Error).message);
    }
    updateLineNumbers(e.target.value);
  }

  function updateLineNumbers(source: string) {
    const lineCount = source.split("\n").length;
    const lineNumbers = Array.from({ length: lineCount }, (_, i) => i + 1).join("\n");
    setLines(lineNumbers);
  }

  function handleScroll() {
    const y = textAreaRef.current!.scrollTop;
    lineNumbersRef.current!.style.transform = `translateY(-${y}px)`;
  }

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setIsSaving(true);
    const source = textAreaRef.current!.value;
    const res = await fetch(`${apiRoot}/api/projects/${userId}/${projectId}`, {
      method: "post",
      headers: {
        Authorization: `Bearer ${localStorage.getItem("token")}`,
        "Content-Type": "application/json",
      },
      body: source,
    });
    if (res.status !== 200) {
      setError(`Failed to save project: ${res.statusText}`);
    }
    setIsSaving(false);
    return;
  }

  return (
    <div id="project-browser" className="flex flex-col h-screen overflow-hidden">
      <LoggedInHeader />
      <main className="flex-1 flex flex-row h-full overflow-hidden">
        <div className="sidebar w-48 bg-zinc-700 flex flex-col h-full p-2">
          <span className="text-xs text-gray-500">User</span>
          <a href={`/${userId}`} className="text-xs font-bold underline">
            {userId}
          </a>
          <span className="text-xs text-gray-500 mt-2">Project</span>
          <a href={`/${userId}/${projectId}`} className="text-xs font-bold underline">
            {projectId}
          </a>
        </div>
        <section className="project flex-1 p-4 overflow-hidden">
          {error && <div className="border border-slate-200 rounded p-2 text-red-500">{error}</div>}
          {isLoading && <div className="border border-slate-200 rounded p-2 text-red-500">Loading...</div>}
          <form className="flex-1 flex flex-col h-full" onSubmit={handleSubmit}>
            <div className="flex-1 flex overflow-hidden rounded bg-zinc-800">
              <div
                ref={lineNumbersRef}
                className="text-sm text-right select-none py-2 px-2 text-zinc-500 whitespace-pre font-mono"
              >
                {lines}
              </div>
              <textarea
                ref={textAreaRef}
                className={clsx("w-full h-full font-mono bg-zinc-700 text-sm p-2 outline-none border", {
                  "border-transparent": !validationError,
                  "border-red-500": validationError,
                })}
                name="source"
                onChange={handleChange}
                onScroll={handleScroll}
                defaultValue={initialSource!}
              />
            </div>
            <div className="button-row flex flex-row justify-between gap-2 mt-2">
              <span className="text-xs font-mono text-red-500">{validationError}</span>
              <input
                type="submit"
                value={isSaving ? "..." : "Save"}
                disabled={isSaving}
                className="bg-blue-500 rounded-sm text-xs px-4 py-2 disabled:opacity-50"
              />
            </div>
          </form>
        </section>
      </main>
    </div>
  );
}
