import { useStore } from '../state/store';
import {
  DIALOG_BACKGROUND,
  DIALOG_BORDER,
  TEXT_DEFAULT,
  TEXT_SUBDUED,
  FONT_SIZE_BASE,
  FONT_SIZE_HEADING,
  VIOLET_400,
} from '../theme/tokens';

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
        className="flex flex-col items-center p-8 gap-4"
        style={{
          width: 320,
          background: DIALOG_BACKGROUND,
          border: `1px solid ${DIALOG_BORDER}`,
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <h1 style={{ color: TEXT_DEFAULT, fontSize: FONT_SIZE_HEADING }}>
          NodeBox
        </h1>
        <p style={{ color: TEXT_SUBDUED, fontSize: FONT_SIZE_BASE }}>
          Visual programming environment for generative design
        </p>
        <p style={{ color: TEXT_SUBDUED, fontSize: FONT_SIZE_BASE }}>
          Version 0.1.0
        </p>
        <a
          href="#"
          style={{ color: VIOLET_400, fontSize: FONT_SIZE_BASE }}
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
