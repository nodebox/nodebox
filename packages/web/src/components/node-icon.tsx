import { useMemo } from "react";
import { createAvatar } from "@dicebear/core";
import { shapes } from "@dicebear/collection";

export default function NodeIcon({ name, alt, size = 30 }: { name: string; alt: string; size?: number }) {
  const avatar = useMemo(() => {
    return createAvatar(shapes, {
      size,
      seed: name,
    }).toDataUri();
  }, [name, size]);

  return <img src={avatar} alt={alt} />;
}
