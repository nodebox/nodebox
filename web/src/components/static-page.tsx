import { Link } from "wouter";
import NodeBoxLogo from "./nodebox-logo";
import { useAuth } from "../auth-context";

interface StaticPageProps {
  title: string;
  children: React.ReactNode;
}

export default function StaticPage({ title, children }: StaticPageProps) {
  const { userId } = useAuth()!;
  return (
    <>
      <header className="flex items-center justify-between h-12 px-4 fixed top-0 w-full bg-black z-30">
        <a href="/" className="logo flex gap-2 items-center" target="_self">
          <NodeBoxLogo /> <span className="text-xs font-bold">NodeBox Live</span>
        </a>
        <nav className="text-xs flex gap-3">
          {!userId && <Link href="/auth/signup">Sign Up</Link>}
          {!userId && <Link href="/auth/login">Login</Link>}
          {userId && <Link href={`/${userId}`}>Projects</Link>}
          <a href="/guide/welcome">Guide</a>
          <a href="/gallery">Gallery</a>
        </nav>
      </header>
      <h1 className="text-4xl">{title}</h1>
      <div className="pt-12">{children}</div>
      <footer className="bg-black">
        <div className="max-w-lg m-auto flex flex-col items-center gap-2  py-8">
          <p className="text-xs">&copy; 2024 NodeBox Live</p>
          <p className="text-xs flex flex-wrap gap-2">
            <a href="/">Home</a>
            <a href="/guide/welcome">Guide</a>
            <a href="/gallery">Gallery</a>
          </p>
          <p className="text-xs flex flex-wrap gap-2">
            <a href="/terms">Terms</a>
            <a href="/privacy">Privacy</a>
          </p>
        </div>
      </footer>
    </>
  );
}
