import React, { useState, useEffect, useRef } from "react";

interface DropdownWidgetProps {
  initialItems: string[];
  label: string;
  defaultValue?: string;
}

const DropdownWidget: React.FC<DropdownWidgetProps> = ({ initialItems, label, defaultValue }) => {
  const [items, setItems] = useState<string[]>(initialItems);
  const [inputValue, setInputValue] = useState<string>(defaultValue || "");
  const [filteredItems, setFilteredItems] = useState<string[]>(initialItems);
  const [dropdownValue, setDropdownValue] = useState<string>(defaultValue || "");
  const [showInput, setShowInput] = useState<boolean>(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const lowerCaseInputValue = inputValue.toLowerCase();
    const matchingItems = items.filter((item) => item.toLowerCase().includes(lowerCaseInputValue));
    setFilteredItems(matchingItems);
  }, [inputValue, items]);

  useEffect(() => {
    if (showInput && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [showInput]);

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    setInputValue(value);

    if (value === "") {
      setFilteredItems(items); // Show all items when input is cleared
    } else {
      const lowerCaseInputValue = value.toLowerCase();
      const matchingItems = items.filter((item) => item.toLowerCase().includes(lowerCaseInputValue));
      setFilteredItems(matchingItems);
    }
  };

  const handleAddItem = (item: string) => {
    if (!items.includes(item)) {
      setItems([...items, item]);
    }
    setDropdownValue(item);
    setInputValue("");
    setFilteredItems([]);
    setShowInput(false);
  };

  const handleSelectItem = (item: string) => {
    setDropdownValue(item);
    setInputValue(item);
    setFilteredItems([]);
    setShowInput(false);
  };

  const handleDropdownClick = () => {
    setShowInput(true);
  };

  const handleInputBlur = () => {
    setShowInput(false);
    setInputValue(dropdownValue);
    setFilteredItems([]);
  };

  return (
    <div className="flex items-stretch hover:bg-zinc-700 px-1 h-8 group relative">
      <label className="w-24 px-1 text-xs leading-8 text-zinc-500 text-right select-none">{label}</label>
      <div className="flex items-stretch hover:bg-zinc-700 px-1 h-8 group relative">
        <select
          value={dropdownValue}
          onChange={(e) => handleSelectItem(e.target.value)}
          onClick={handleDropdownClick}
          className="menu-widget flex-1 text-xs bg-transparent px-2 overflow-hidden border border-transparent outline-none rounded-sm"
        >
          <option value="" disabled>
            Select an option
          </option>
          {filteredItems.map((item, index) => (
            <option key={index} value={item} className="bg-zinc-800 text-white">
              {item}
            </option>
          ))}
          {inputValue && !filteredItems.length && (
            <option value={inputValue} className="bg-zinc-800 text-white">
              add value: {inputValue}
            </option>
          )}
        </select>
        {showInput && (
          <input
            ref={inputRef}
            type="text"
            value={inputValue}
            onChange={handleInputChange}
            onBlur={handleInputBlur}
            placeholder="Search or add..."
            className="absolute top-2 left-3 text-xs bg-zinc-800 overflow-hidden border border-transparent outline-none rounded-sm"
            style={{ width: "calc(100% - 38px)", zIndex: 10 }}
          />
        )}
        {inputValue && showInput && (
          <div
            id="xxx"
            className="absolute top-8 bg-zinc-800 text-white border border-gray-600 w-full max-h-60 overflow-y-auto z-10"
          >
            {filteredItems.length > 0 ? (
              filteredItems.map((item, index) => (
                <div
                  key={index}
                  onClick={() => handleSelectItem(item)}
                  className="flex items-stretch hover:bg-zinc-700 px-1 h-8 cursor-pointer"
                >
                  <div className="flex-1 text-xs px-2 text-zinc-200 leading-8">{item}</div>
                </div>
              ))
            ) : (
              <div
                onClick={() => handleAddItem(inputValue)}
                className="flex items-stretch hover:bg-zinc-700 px-1 h-8 cursor-pointer"
              >
                <div className="flex-1 text-xs px-2 text-zinc-200 leading-8">(create {inputValue})</div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default DropdownWidget;
