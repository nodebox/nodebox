import { useMemo } from "react";
import { createAvatar } from "@dicebear/core";
import { initials } from "@dicebear/collection";

export default function UserAvatar({ userId, size = 30 }: { userId: string; size?: number }) {
  const avatar = useMemo(() => {
    return createAvatar(initials, {
      size,
      seed: userId,
    }).toDataUri();
  }, [userId, size]);

  return <div className="w-full h-full bg-cover bg-center" style={{ backgroundImage: `url("${avatar}")` }}></div>;
}
