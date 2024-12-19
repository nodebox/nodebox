import React, { useState } from "react";
import Icon from "../components/icon";

const TABLEVIEWER_ROWS_LIMIT = 25;
const TABLEVIEWER_ROWS_TRESHOLD = 500;
const TABLEVIEWER_ROWS_INCREMENT = 100;

const TABLEVIEWER_COLUMNS_LIMIT = 20;
const TABLEVIEWER_COLUMNS_INCREMENT = 10;

interface ValueSpanProps {
  value: null | string | number | object;
  onClick?: () => void;
}

function ValueSpan({ value, onClick }: ValueSpanProps) {
  if (value === null || typeof value === "string") {
    return <span>{value}</span>;
  } else if (typeof value === "number") {
    return <span>{Math.floor(value) === value ? value : value.toFixed(5)}</span>;
  } else if (typeof value === "object") {
    return (
      <em className="text-blue-500 cursor-pointer" onClick={onClick}>
        {"<object>"}
      </em>
    );
  }
  return <em className="text-zinc-600">{"<missing>"}</em>;
}

interface TableViewerProps {
  data: any;
  style?: React.CSSProperties;
  className?: string;
  path?: string; // Added to manage data path for nested objects if needed
  depth?: number; // Added to manage recursion depth if needed
}

function TableCell({
  content,
}: {
  content: React.ReactNode;
  isExpanded?: boolean;
  isExpandable?: boolean;
  onCollapse?: () => void;
  onExpand?: () => void;
}) {
  return (
    <div className="flex items-center">
      <div className="flex-1">{content}</div>
    </div>
  );
}

function removeAllBackgrounds(element: HTMLElement) {
  element.classList.forEach((cls) => {
    if (cls.startsWith("bg-zinc-800")) {
      element.classList.remove(cls);
    }
  });
  Array.from(element.children).forEach((child) => {
    if (child instanceof HTMLElement) {
      removeAllBackgrounds(child);
    }
  });
}

export default function TableViewer({ data, style = {}, className = "", path = "", depth = 0 }: TableViewerProps) {
  let partialDataShown = false;
  const [expandedCells, setExpandedCells] = useState<{ [key: string]: any }>({});
  const [currentRowLimit, setCurrentRowLimit] = useState(TABLEVIEWER_ROWS_LIMIT);
  const [currentColumnLimit, setCurrentColumnLimit] = useState(TABLEVIEWER_COLUMNS_LIMIT);
  const showRowNumber = Array.isArray(data);
  let dataArray = Array.isArray(data) ? data : [data];
  const directValue = dataArray.length > 0 && (dataArray[0] === null || typeof dataArray[0] !== "object");

  const keySet = dataArray.reduce((acc, curr) => {
    if (curr && typeof curr === "object") {
      Object.keys(curr).forEach((key) => acc.add(path !== "" ? path + ">" + key : key));
    }
    return acc;
  }, new Set<string>());

  let allKeys: string[] = directValue
    ? ["value"]
    : data === null || dataArray.length === 0
      ? ["value"]
      : keySet.size > 0
        ? [...keySet]
        : ["value"];

  if (allKeys.length > currentColumnLimit + 1) {
    const hiddenColumns = allKeys.length - currentColumnLimit;
    allKeys = allKeys.slice(0, currentColumnLimit);
    if (hiddenColumns > 0) {
      allKeys.push(`...+${hiddenColumns}`);
    }
  }

  const handleCellClick = (rowIndex: number, colKey: string, cellValue: any) => {
    const cellKey = `${rowIndex}-${colKey}`;
    setExpandedCells((prev) => ({
      ...prev,
      [cellKey]: !prev[cellKey] ? cellValue : null,
    }));
  };

  const handleCollapse = (cellKey: string) => {
    setExpandedCells((prev) => ({
      ...prev,
      [cellKey]: null,
    }));
  };

  function showMoreRows() {
    const newLimit = currentRowLimit + TABLEVIEWER_ROWS_INCREMENT;
    setCurrentRowLimit(newLimit);
  }

  function showLessRows() {
    const newLimit = Math.max(currentRowLimit - TABLEVIEWER_ROWS_INCREMENT, TABLEVIEWER_ROWS_LIMIT);
    setCurrentRowLimit(newLimit);
  }

  function showMoreColumns() {
    const newLimit = currentColumnLimit + TABLEVIEWER_COLUMNS_INCREMENT;
    setCurrentColumnLimit(newLimit);
  }

  function showLessColumns() {
    const newLimit = Math.max(currentColumnLimit - TABLEVIEWER_COLUMNS_INCREMENT, TABLEVIEWER_COLUMNS_LIMIT);
    setCurrentColumnLimit(newLimit);
  }

  if (dataArray.length > TABLEVIEWER_ROWS_TRESHOLD) {
    dataArray = dataArray.slice(0, currentRowLimit);
    partialDataShown = true;
  }
  return (
    <div className={className} style={{ ...style, marginLeft: depth * 0 }}>
      <table
        style={{
          verticalAlign: "top",
          width: "100%",
          borderCollapse: "collapse",
        }}
        className="align-top" // Add table-level alignment
      >
        <thead className="p-1 font-mono text-xs text-zinc-500">
          <tr className="align-top">
            {showRowNumber && (
              <th key="row-number-header" className="pr-2 text-left align-top">
                &nbsp;
              </th>
            )}
            {allKeys.map((colKey) => (
              <th key={colKey} className="font-normal pr-2 text-left align-top whitespace-nowrap" title={colKey}>
                {colKey.split(">").pop()}
              </th>
            ))}
            {allKeys.length < keySet.size && (
              <th className="font-normal pr-2 text-left align-top">
                <div className="flex gap-2 text-sm">
                  {currentColumnLimit > TABLEVIEWER_COLUMNS_LIMIT && (
                    <button onClick={showLessColumns} className="hover:text-zinc-300">
                      <Icon
                        title={`-${TABLEVIEWER_COLUMNS_INCREMENT} columns`}
                        name="chevron-right"
                        size={16}
                        fill="currentColor"
                        className="rotate-180"
                      />
                    </button>
                  )}
                  {currentColumnLimit && (
                    <button onClick={showMoreColumns} className="hover:text-zinc-300">
                      <Icon
                        title={`+${TABLEVIEWER_COLUMNS_INCREMENT} columns`}
                        name="chevron-right"
                        size={16}
                        fill="currentColor"
                      />
                    </button>
                  )}
                </div>
              </th>
            )}
          </tr>
        </thead>

        <tbody>
          {dataArray.map((row, rowIndex) => (
            <tr key={rowIndex} className="p-1 font-mono text-xs white align-top">
              {showRowNumber && <td className="text-zinc-600 pr-2 align-top">{rowIndex + 1}</td>}
              {allKeys.map((colKeyPath) => {
                const colKey = colKeyPath.split(">").pop() || "";
                const cellValue = directValue ? row : row[colKey];
                const cellKey = `${rowIndex}-${colKey}`;
                const expandedValue = expandedCells[cellKey];

                return (
                  <td key={colKey} className="pr-2 align-top">
                    <div className="flex items-start">
                      {typeof cellValue === "object" && cellValue !== null && (
                        <button
                          onClick={() => handleCellClick(rowIndex, colKey, cellValue)}
                          onMouseEnter={(e) => {
                            removeAllBackgrounds(e.currentTarget.closest("div") as HTMLElement);
                            e.currentTarget.closest("div")?.classList.add("bg-zinc-800");
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.closest("div")?.classList.remove("bg-zinc-800");
                          }}
                          className=""
                          aria-label={expandedValue ? "Collapse" : "Expand"}
                        >
                          <Icon
                            name="chevron-down"
                            size={16}
                            fill="grey"
                            className={`transition-transform duration-200 ${expandedValue ? "rotate-180" : ""}`}
                          />
                        </button>
                      )}
                      {expandedValue ? (
                        <TableCell
                          content={
                            <TableViewer data={expandedValue} path={colKeyPath} className="m-0" depth={depth + 1} />
                          }
                          isExpanded={true}
                          isExpandable={true}
                          onCollapse={() => handleCollapse(cellKey)}
                        />
                      ) : (
                        <ValueSpan
                          value={cellValue}
                          onClick={() => {
                            if (typeof cellValue === "object" && cellValue !== null) {
                              handleCellClick(rowIndex, colKey, cellValue);
                            }
                          }}
                        />
                      )}
                    </div>
                  </td>
                );
              })}
            </tr>
          ))}
          {partialDataShown && (
            <tr>
              <td className="text-zinc-600 text-left"></td>
              <td colSpan={allKeys.length - 1} className="text-zinc-500 text-left">
                <div className="flex gap-2 text-sm">
                  {currentRowLimit > TABLEVIEWER_ROWS_LIMIT && (
                    <button onClick={showLessRows} className="hover:text-zinc-300">
                      <Icon
                        title={`${TABLEVIEWER_ROWS_INCREMENT}`}
                        name="chevron-down"
                        className="rotate-180"
                        size={24}
                        fill="currentColor"
                      />
                    </button>
                  )}
                  {currentRowLimit < TABLEVIEWER_ROWS_TRESHOLD && (
                    <div className="flex items-center">
                      <button onClick={showMoreRows} className="hover:text-zinc-300">
                        <Icon
                          title={`+${TABLEVIEWER_ROWS_INCREMENT} ${data.length - currentRowLimit} remaining`}
                          name="chevron-down"
                          size={24}
                          fill="currentColor"
                        />
                      </button>
                      <span className="text-xs pl-5">{` (${data.length - currentRowLimit} remaining)`}</span>
                    </div>
                  )}
                </div>
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
