import React from "react";

interface DropdownProps {
  id: string;
  label: string;
  selectedValue: string;
  options: string[];
  onChange: (id: string, value: string) => void;
}
export const Dropdown: React.FC<DropdownProps> = ({ id, label, selectedValue, options, onChange }) => {
  const handleChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
    onChange(id, event.target.value);
  };

  return (
    <div className="flex items-stretch hover:bg-zinc-700 px-1 h-8 group relative">
      <label className="w-24 px-1 text-xs leading-8 text-zinc-500 text-right select-none">
        {label === "keyword" ? "Keywords" : ""}
      </label>
      <select
        className="menu-widget flex-1 text-xs bg-transparent px-2 overflow-hidden border border-transparent outline-none rounded-sm"
        value={selectedValue}
        onChange={handleChange}
      >
        {options.map((option, index) => (
          <option key={index} value={option}>
            {option}
          </option>
        ))}
      </select>
    </div>
  );
};

export default Dropdown;
