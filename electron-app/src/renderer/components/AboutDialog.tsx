import { useStore } from '../state/store';

export function AboutDialog() {
  const visible = useStore((s) => s.aboutDialogVisible);
  const setVisible = useStore((s) => s.setAboutDialogVisible);

  if (!visible) return null;

  return (
    <div
      className="fixed inset-0 flex items-center justify-center"
      style={{ zIndex: 200, background: 'rgba(0,0,0,0.5)' }}
      onClick={() => setVisible(false)}
    >
      <div
        className="flex flex-col items-center p-8 gap-4 bg-zinc-700 border border-zinc-500"
        style={{ width: 320 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h1 className="text-zinc-100 text-[16px]">
          NodeBox
        </h1>
        <p className="text-zinc-300 text-[13px]">
          Visual programming environment for generative design
        </p>
        <p className="text-zinc-300 text-[13px]">
          Version 0.1.0
        </p>
        <a
          href="#"
          className="text-violet-400 text-[13px]"
          onClick={(e) => {
            e.preventDefault();
            setVisible(false);
          }}
        >
          Close
        </a>
      </div>
    </div>
  );
}
