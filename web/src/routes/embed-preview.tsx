import React from "react";
import { useParams } from "wouter";
import InlineMessage from "../components/inline-message";
import LoggedInHeader from "../components/logged-in-header";
import { config, Color, Context, LiteralValue, NodeBoxPlayer } from "@ndbx/runtime";
import { colorToCss } from "../lib/color-utils";

if (import.meta.env.DEV) {
  config.apiRoot = document.location.origin;
  config.bareImportReplacer = (name: string) => import.meta.resolve(name);
}

export default function RawEditor() {
  let { userId, projectId, item } = useParams();
  item = item || "Main";
  const [cx, setCx] = React.useState<Context | undefined>();
  const [error, setError] = React.useState<string | null>(null);
  const [values, setValues] = React.useState<Record<string, LiteralValue>>({});
  const [activeItem, setActiveItem] = React.useState<string>(item);

  function handleProjectLoaded(cx: Context) {
    setCx(cx);
  }

  function handleProjectError(message: string) {
    setError(message);
  }

  function handleFieldChange(name: string, value: LiteralValue) {
    setValues((values) => ({ ...values, [name]: value }));
  }

  function handleChangeItem(e: React.ChangeEvent<HTMLSelectElement>) {
    setActiveItem(e.target.value);
  }

  if (error) {
    return (
      <main id="embed-preview" className="px-2 md:px-8 pt-4">
        <h1 className="text-xl">
          Project {userId}/{projectId}
        </h1>
        <InlineMessage key={Date.now()}>{error}</InlineMessage>
      </main>
    );
  }

  const fields: JSX.Element[] = [];
  if (cx) {
    const mainItem = cx?.project.items.find((i) => i.name === item);
    mainItem?.parameters?.forEach((param) => {
      if (param.type === "NUMBER") {
        fields.push(
          <div key={param.name} className="flex flex-row gap-2 items-baseline">
            <label className="w-32  text-right text-zinc-300">{param.name}</label>
            <input
              className="border border-zinc-700 rounded p-1 bg-transparent text-zinc-300"
              type="number"
              value={((values[param.name] as number) || param.defaultValue).toString()}
              onChange={(e) => handleFieldChange(param.name, e.target.value)}
            />
          </div>,
        );
      } else if (param.type === "STRING") {
        fields.push(
          <div key={param.name} className="flex flex-row gap-2 items-baseline">
            <label className="w-32  text-right text-zinc-300">{param.name}</label>
            <input
              className="border border-zinc-700 rounded p-1 bg-transparent text-zinc-300"
              type="text"
              value={(values[param.name] as string) || (param.defaultValue as string)}
              onChange={(e) => handleFieldChange(param.name, e.target.value)}
            />
          </div>,
        );
      } else if (param.type === "COLOR") {
        fields.push(
          <div key={param.name} className="flex flex-row gap-2 items-baseline">
            <label className="w-32  text-right text-zinc-300">{param.name}</label>
            <input
              className="border border-zinc-700 rounded p-1 bg-transparent text-zinc-300"
              type="color"
              value={(values[param.name] as string) || colorToCss(param.defaultValue as Color)}
              onChange={(e) => handleFieldChange(param.name, e.target.value)}
            />
          </div>,
        );
      }
    });
  }

  return (
    <div id="embed-preview" className="flex flex-col h-screen overflow-hidden">
      <LoggedInHeader />
      <main className="flex-1 flex flex-col h-full overflow-hidden gap-2 items-center">
        <div className="flex gap-1 items-center py-2">
          <a href={`/${userId}/${projectId}`} className="text-xs font-bold underline">
            {projectId}
          </a>
          <select className="bg-zinc-700 text-xs rounded-md px-2 py-1" onChange={handleChangeItem}>
            {cx?.project.items.map((item) => (
              <option key={item.name}>{item.name}</option>
            ))}
          </select>
        </div>
        <div>
          <NodeBoxPlayer
            userId={userId!}
            projectId={projectId!}
            item={activeItem}
            values={values}
            onProjectLoaded={handleProjectLoaded}
            onProjectError={handleProjectError}
          />
          <div className="flex flex-col gap-2 pt-2">{fields}</div>
        </div>
      </main>
    </div>
  );
}
