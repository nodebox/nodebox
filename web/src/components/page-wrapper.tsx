import React from "react";
import LoggedInHeader from "./logged-in-header";

export default function PageWrapper({ children }: { children: React.ReactNode }) {
  return (
    <>
      <LoggedInHeader />
      <main className="wrapper w-full md:w-1/2 m-auto my-10 bg-zinc-700 rounded-lg shadow-xl">{children}</main>
    </>
  );
}
