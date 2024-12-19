import { useState, useRef, useEffect } from "react";
import clsx from "clsx";

interface InlineEditProps {
  value: string;
  onChange: (value: string) => void;
  className?: string;
  disabled?: boolean;
}

export default function InlineEdit({ value, onChange, className, disabled = false }: InlineEditProps) {
  const [inputValue, setInputValue] = useState(value);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [isEditing, setIsEditing] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setInputValue(value);
  }, [value]);

  useEffect(() => {
    if (isEditing) {
      inputRef.current?.select();
    }
  }, [isEditing]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setInputValue(value);
  };

  const handleBlur = () => {
    onChange(inputValue);
    setIsEditing(false);
    setSuggestions([]); // Hide suggestions on blur
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.currentTarget.blur();
    } else if (e.key === "Escape") {
      setInputValue(value);
      setIsEditing(false);
      setSuggestions([]);
    }
  };

  const handleSuggestionClick = (suggestion: string) => {
    setInputValue(suggestion);
    setSuggestions([]);
    onChange(suggestion);
    setIsEditing(false);
  };

  if (isEditing) {
    return (
      <div className="relative flex-1 overflow-hidden">
        <input
          ref={inputRef}
          type="text"
          value={inputValue}
          onChange={handleInputChange}
          onBlur={handleBlur}
          onKeyDown={handleKeyDown}
          className={clsx("rounded-sm px-2 py-2 outline-none text-xs w-full overflow-hidden bg-transparent", className)}
          disabled={disabled}
        />
        {suggestions.length > 0 && (
          <ul className="absolute bg-white border rounded shadow-md mt-1 w-full z-10">
            {suggestions.map((suggestion) => (
              <li
                key={suggestion}
                onMouseDown={() => handleSuggestionClick(suggestion)} // onMouseDown prevents blur
                className="px-2 py-1 cursor-pointer hover:bg-gray-200"
              >
                {suggestion}
              </li>
            ))}
          </ul>
        )}
      </div>
    );
  } else {
    return (
      <span
        onClick={() => setIsEditing(true)}
        className={clsx("text-xs px-2 h-8 overflow-hidden text-ellipsis whitespace-nowrap", className)}
        style={{ lineHeight: "32px" }}
      >
        {value}
      </span>
    );
  }
}
