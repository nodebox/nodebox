import {
  ADDRESS_BAR_BACKGROUND,
  ADDRESS_BAR_HEIGHT,
  PANE_HEADER_FOREGROUND_COLOR,
  TEXT_SUBDUED,
  FONT_SIZE_SMALL,
} from '../theme/tokens';

export function AddressBar() {
  return (
    <div
      className="flex items-center titlebar-drag"
      style={{
        height: ADDRESS_BAR_HEIGHT,
        background: ADDRESS_BAR_BACKGROUND,
        color: PANE_HEADER_FOREGROUND_COLOR,
        fontSize: FONT_SIZE_SMALL,
        paddingLeft: 80,
      }}
    >
      <span className="titlebar-no-drag" style={{ color: TEXT_SUBDUED }}>
        root
      </span>
    </div>
  );
}
