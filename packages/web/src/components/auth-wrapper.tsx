import NodeBoxLogo from "./nodebox-logo.tsx";

export default function AuthWrapper({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center pt-5 gap-5">
      <a href="/" target="_self">
        <NodeBoxLogo />
      </a>

      <main className="wrapper w-full md:w-1/2 m-auto my-10 bg-zinc-700 rounded-lg shadow-xl">{children}</main>
    </div>
  );
}
