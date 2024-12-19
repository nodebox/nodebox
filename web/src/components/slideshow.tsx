import { useEffect, useState } from "react";
import clsx from "clsx";

interface SlideshowProps {
  children: React.ReactNode[];
  height?: string;
}

export default function Slideshow({ children, height = "600px" }: SlideshowProps) {
  const [slideIndex, setSlideIndex] = useState(0);
  useEffect(() => {
    const intervalId = setInterval(() => {
      setSlideIndex((slideIndex) => (slideIndex + 1) % children.length);
    }, 2000);
    return () => clearInterval(intervalId);
  });
  return (
    <div className="relative w-full overflow-hidden" style={{ height }}>
      {children.map((child, index) => (
        <div
          key={index}
          className={clsx("absolute w-full h-full object-contain", {
            "opacity-0": index !== slideIndex,
            "opacity-100": index === slideIndex,
          })}
          style={{ transition: "opacity 1s" }}
        >
          {child}
        </div>
      ))}
    </div>
  );
}
