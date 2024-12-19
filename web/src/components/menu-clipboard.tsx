import { useState, useEffect } from "react";
import { MenuItem } from "./menu";

interface MenuItemProps {
  onClick: () => void;
  shortcutKey?: string;
}

export function MenuItemClipboardAvailable({ onClick, shortcutKey }: MenuItemProps) {
  const [isDataAvailable, setIsDataAvailable] = useState(false);

  useEffect(() => {
    (async () => {
      // In order to check if clipboard data is available, we're first going to do a permissions check:
      try {
        const result = await navigator.permissions.query({ name: "clipboard-read" as any });
        if (result.state === "granted") {
          // If we're allowed to paste from the clipboard, we will paste and parse the data to check if it's valid.
          try {
            const pasteData = await navigator.clipboard.readText();
            const json = JSON.parse(pasteData);
            if (Array.isArray(json.items) && Array.isArray(json.internalConnections) && json.items.length > 0) {
              setIsDataAvailable(true);
            } else {
              // We've parsed the paste data but the data from the clipboard is not valid.
              setIsDataAvailable(false);
            }
          } catch (err) {
            // The data is not JSON
            setIsDataAvailable(false);
          }
        } else {
          // The paste permission was not granted yet, so we'll assume that we can paste.
          // Once we select "Paste", the clipboard permission thing will pop up.
          setIsDataAvailable(true);
        }
      } catch (err) {
        // If we can't do the permissions check, we're probably using Firefox or Safari, which don't implement this permission.
        // In that case, we're going to assume that paste data is available.
        setIsDataAvailable(true);
        return;
      }
    })();
  }, []);

  return (
    <>
      {
        <MenuItem disabled={!isDataAvailable} onClick={onClick} shortcutKey={shortcutKey}>
          Paste
        </MenuItem>
      }
    </>
  );
}
