import { useAuth } from "../auth-context";
import NodeBoxLogo from "./nodebox-logo";
import { Link } from "wouter";
import Icon from "./icon";
import UserMenu from "./user-menu.tsx";

export default function LoggedInHeader() {
  const { userId } = useAuth()!;
  return (
    <header className="flex items-center justify-between bg-zinc-800 text-white border-b border-b-zinc-700 h-12">
      <a className="h-full w-12 hover:bg-zinc-900 flex justify-center items-center" href={`/${userId ?? ""}`}>
        <NodeBoxLogo size={20} />
      </a>
      <nav className="flex gap-3 mr-4">
        {userId && (
          <Link
            href="/create"
            className="rounded bg-blue-500 hover:bg-blue-600 px-2 text-xs text-zinc-100 flex items-center gap-1"
          >
            <Icon name="plus" />
            Project
          </Link>
        )}
        <UserMenu />
      </nav>
    </header>
  );
}
