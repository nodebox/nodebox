export function AddressBar() {
  return (
    <div
      className="flex items-center titlebar-drag bg-zinc-800 text-zinc-200 text-[11px]"
      style={{ height: 28, paddingLeft: 80 }}
    >
      <span className="titlebar-no-drag text-zinc-300">
        root
      </span>
    </div>
  );
}
