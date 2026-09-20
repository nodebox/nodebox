import CodeMirror from "@uiw/react-codemirror";
import { javascript } from "@codemirror/lang-javascript";
import { EditorState } from "@codemirror/state";
import { indentUnit } from "@codemirror/language";

import { activeItem, setFunctionSource } from "./signals";
import { FunctionItem } from "@ndbx/runtime";

export default function CodeEditor() {
  if (!activeItem.value || activeItem.value.type !== "FUNCTION") return null;

  const fn = activeItem.value as FunctionItem;

  function handleChange(source: string) {
    setFunctionSource(fn.id, source);
  }

  const tabIndent = EditorState.tabSize.of(2);
  const useTabsForIndentation = indentUnit.of("\t");

  return (
    <div className="h-full overflow-hidden">
      <CodeMirror
        value={fn.source}
        theme="dark"
        extensions={[javascript(), tabIndent, useTabsForIndentation]}
        indentWithTab={true}
        onChange={handleChange}
      />
    </div>
  );
}
