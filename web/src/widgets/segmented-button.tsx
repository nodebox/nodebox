import React from "react";
import clsx from "clsx";

interface Segment {
  text: string;
  fontSize: number;
}

interface SegmentedButtonProps {
  segments: Segment[];
  selected: number;
  onClick: (index: number) => void;
}

const SegmentedButton: React.FC<SegmentedButtonProps> = ({ segments, selected, onClick }) => {
  return (
    <div className="flex text-sm border border-zinc-700 rounded-lg overflow-hidden">
      {segments.map((segment, index) => (
        <button
          key={index}
          className={clsx("flex-1 px-3 py-1 cursor-pointer transition-colors duration-200 hover:bg-zinc-700", {
            "bg-zinc-700": selected === index,
            "text-zinc-100": selected === index,
            "text-zinc-500": selected !== index,
            "border-r border-zinc-700": index !== segments.length - 1,
          })}
          onClick={() => onClick(index)}
        >
          {segment.text}
        </button>
      ))}
    </div>
  );
};

export default SegmentedButton;
