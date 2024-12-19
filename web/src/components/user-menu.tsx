import React from "react";
import { autoUpdate, useFloating, useDismiss, useInteractions, Placement } from "@floating-ui/react";
import { useAuth } from "../auth-context";

export default function UserMenu() {
  const [menuVisible, setMenuVisible] = React.useState<boolean>(false);
  const { logout, userId } = useAuth()!;

  const { refs, floatingStyles, context } = useFloating<HTMLDivElement>({
    whileElementsMounted: autoUpdate,
    open: menuVisible,
    onOpenChange: setMenuVisible,
    placement: "bottom-end" as Placement,
  });

  const dismiss = useDismiss(context);
  const { getReferenceProps, getFloatingProps } = useInteractions([dismiss]);

  const handleLogout = (e: React.MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    logout();
    window.location.href = "/";
  };

  return (
    <>
      <div className="relative rounded-full">
        <div
          className="relative shrink-0 rounded-full overflow-hidden transition-all duration-300 w-8 h-8"
          onClick={() => setMenuVisible(true)}
          ref={refs.setReference}
          {...getReferenceProps()}
        >
          <div
            className="w-full h-full bg-cover bg-center"
            style={{ backgroundImage: `url("https://api.dicebear.com/7.x/initials/svg?seed=${userId ?? ""}")` }}
          ></div>
        </div>
        {menuVisible && (
          <div ref={refs.setFloating} style={{ ...floatingStyles, zIndex: 10 }} {...getFloatingProps()}>
            <div className="bg-zinc-700 rounded flex flex-col w-48 overflow-hidden shadow mt-1">
              {!userId && (
                <>
                  <p className="text-zinc-400 text-xs px-4 py-2 border-b border-b-zinc-600 text-center">
                    You are currently not logged in.
                  </p>
                  <a href="/auth/login" className="hover:bg-zinc-600 text-zinc-100 px-4 py-2 text-sm">
                    Log In
                  </a>
                  <a href="/auth/signup" className="hover:bg-zinc-600 text-zinc-100 px-4 py-2 text-sm">
                    Sign Up
                  </a>
                </>
              )}
              {userId && (
                <>
                  <p className="text-zinc-100 px-4 py-2 text-sm font-bold border-b border-b-zinc-600">{userId}</p>
                  <a href={`/${userId}`} className="hover:bg-zinc-600 text-zinc-100 px-4 py-2 text-sm">
                    Your Projects
                  </a>
                  <div className="separator h-px border-t border-zinc-600"></div>
                  <a
                    href="/guide/welcome"
                    target="_blank"
                    className="hover:bg-zinc-600 text-zinc-100 px-4 py-2 text-sm"
                  >
                    Guide
                  </a>
                  <a href="/gallery" target="_blank" className="hover:bg-zinc-600 text-zinc-100 px-4 py-2 text-sm">
                    Gallery
                  </a>
                  <div className="separator h-px border-t border-zinc-600"></div>

                  <a
                    href="/auth/logout"
                    className="hover:bg-zinc-600 text-zinc-100 px-4 py-2 text-sm"
                    onClick={handleLogout}
                  >
                    Log Out
                  </a>
                </>
              )}
            </div>
          </div>
        )}
      </div>
    </>
  );
}
