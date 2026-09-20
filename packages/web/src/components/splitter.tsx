import React, { useState, useEffect, useCallback, useRef } from "react";
import clsx from "clsx";

interface SplitterProps {
  direction?: "horizontal" | "vertical";
  defaultSplit?: number;
  className?: string;
  firstChildClassName?: string;
  secondChildClassName?: string;
  children: [React.ReactNode, React.ReactNode];
}

const Splitter: React.FC<SplitterProps> = ({
  direction = "horizontal",
  defaultSplit = 30,
  className = "",
  firstChildClassName = "",
  secondChildClassName = "",
  children,
}) => {
  const splitterId = React.useId();
  const [fraction, setFraction] = useState<number>(() => {
    const savedFraction = localStorage.getItem(`splitter-${splitterId}`);
    return savedFraction ? parseFloat(savedFraction) : defaultSplit / 100;
  });
  const [, setVersion] = useState<number>(0);
  const mouseStartRef = useRef<number>(0);
  const fractionStartRef = useRef<number>(0);
  const currentFractionRef = useRef<number>(fraction);
  const containerRef = useRef<HTMLDivElement>(null);

  const handleMouseDown = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      e.preventDefault();
      mouseStartRef.current = direction === "horizontal" ? e.clientX : e.clientY;
      fractionStartRef.current = fraction;
      currentFractionRef.current = fraction;
      window.addEventListener("mousemove", handleMouseDrag);
      window.addEventListener("mouseup", handleMouseUp);
    },
    [fraction, direction],
  );

  const handleMouseDrag = useCallback(
    (e: MouseEvent) => {
      e.preventDefault();
      if (!containerRef.current) return;

      const containerRect = containerRef.current.getBoundingClientRect();
      const containerSize = direction === "horizontal" ? containerRect.width : containerRect.height;
      const mousePos = direction === "horizontal" ? e.clientX : e.clientY;
      const deltaPos = mousePos - mouseStartRef.current;

      let newFraction = fractionStartRef.current + deltaPos / containerSize;
      newFraction = Math.max(0.1, Math.min(0.9, newFraction));
      currentFractionRef.current = newFraction;
      setFraction(newFraction);
    },
    [direction],
  );

  const handleMouseUp = useCallback(
    (e: MouseEvent) => {
      e.preventDefault();
      window.removeEventListener("mousemove", handleMouseDrag);
      window.removeEventListener("mouseup", handleMouseUp);
      localStorage.setItem(`splitter-${splitterId}`, currentFractionRef.current.toFixed(4));
    },
    [splitterId],
  );

  useEffect(() => {
    const forceReload = () => setVersion((v) => v + 1);
    window.addEventListener("resize", forceReload);
    return () => window.removeEventListener("resize", forceReload);
  }, []);

  const containerClass = clsx(className, "flex", direction === "horizontal" ? "flex-row" : "flex-col");

  const dividerClass = clsx("flex-shrink-0 relative bg-zinc-700", direction === "horizontal" ? "w-px" : "h-px");

  const hitAreaClass = clsx(
    " absolute hover:bg-zinc-600 transition-colors duration-200 z-10",
    direction === "horizontal" ? "w-2 h-full -left-1 cursor-col-resize" : "h-2 w-full -top-1 cursor-row-resize",
  );
  return (
    <div ref={containerRef} className={containerClass}>
      <div style={{ flexBasis: `${fraction * 100}%`, overflow: "hidden" }} className={firstChildClassName}>
        {children[0]}
      </div>
      <div className={dividerClass} onMouseDown={handleMouseDown}>
        <div className={hitAreaClass} />
      </div>
      <div style={{ flex: 1, overflow: "hidden" }} className={secondChildClassName}>
        {children[1]}
      </div>
    </div>
  );
};

export default Splitter;
