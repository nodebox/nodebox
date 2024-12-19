import { useState, useEffect, useRef } from "react";
import InlineEdit from "../components/inline-edit";
import FullscreenModal from "../components/fullscreen-modal";
import Icon from "../components/icon";
import {
  WidgetProps,
  WidgetMetaButton,
  WidgetRemoveButton,
  WidgetExpressionButton,
  createParameterValueFromLiteral,
} from "./widget-utils";

interface StringWidgetProps extends WidgetProps {
  isMultiline?: boolean;
}
export default function StringWidget({
  label,
  value,
  onChange,
  onToggleExpression,
  onPublishParameter,
  onRemove,
  onMeta,
  isMultiline = false,
}: StringWidgetProps) {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [tempValue, setTempValue] = useState(value as string);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  useEffect(() => {
    if (isModalOpen && textareaRef.current) {
      textareaRef.current.focus();
    }
  }, [isModalOpen]);

  const handleClick = () => {
    if (isMultiline) {
      setTempValue(value as string);
      setIsModalOpen(true);
    }
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
  };

  const handleSave = () => {
    onChange(createParameterValueFromLiteral(tempValue));
    setIsModalOpen(false);
  };
  return (
    <>
      <div className="flex items-stretch px-1 h-8 hover:bg-zinc-700 group relative" onClick={handleClick}>
        <WidgetMetaButton onMeta={onMeta} />
        <div className="w-28 px-1 text-xs leading-8 text-zinc-400 text-right">{label}</div>
        <InlineEdit
          value={value as string}
          onChange={(s) => onChange(createParameterValueFromLiteral(s))}
          className="flex-1"
        />
        <WidgetExpressionButton onToggleExpression={onToggleExpression} onPublishParameter={onPublishParameter} />
        <WidgetRemoveButton onRemove={onRemove} />
      </div>
      {isModalOpen && (
        <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={handleModalClose}>
          <main className="flex flex-row h-full w-full relative">
            <div className="absolute top-2 right-2 cursor-pointer">
              <Icon name="x" onClick={handleModalClose} size={24} />
            </div>
            <div className="modal-content flex-1 bg-zinc-900 px-8">
              <h1 className="mt-2 mb-6 font-bold text-sm">{label}</h1>
              <div className="p-4">
                <textarea
                  ref={textareaRef}
                  value={tempValue}
                  onChange={(e) => setTempValue(e.target.value)}
                  className="text-lg border bg-zinc-800 border-zinc-600 rounded p-2 outline-none focus:border-blue-500 text-zinc-200 w-full "
                />
                <div className="mt-4 flex justify-end space-x-2">
                  <button
                    onClick={handleSave}
                    className="border bg-blue-700 hover:bg-zinc-900 border-zinc-700 rounded p-4 text-zinc-100 w-full cursor-pointer "
                  >
                    Save
                  </button>
                </div>
              </div>
            </div>
          </main>
        </FullscreenModal>
      )}
    </>
  );
}
