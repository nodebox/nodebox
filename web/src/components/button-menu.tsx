import React, { useState } from "react";
import { Menu, MenuItem } from "./menu";
import Icon from "./icon";

export interface MenuOption {
  label: string;
  icon?: string;
  action: () => void;
}

interface ButtonMenuProps {
  label: string;
  icon?: string;
  options: MenuOption[];
}

export function ButtonMenu({ label, icon, options }: ButtonMenuProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [anchorPosition, setAnchorPosition] = useState({ x: 0, y: 0 });

  const handleButtonClick = (event: React.MouseEvent) => {
    const rect = event.currentTarget.getBoundingClientRect();
    setAnchorPosition({ x: rect.left, y: rect.bottom + 2 });
    setMenuOpen(true);
  };

  const handleCloseMenu = () => {
    setMenuOpen(false);
  };

  return (
    <div className="relative">
      <button
        className="flex items-center border border-zinc-700 text-white py-1 px-2 rounded"
        onClick={handleButtonClick}
      >
        {icon && <Icon name={icon} className="mr-2" />}
        {label}
        <Icon name="caret-down" className="ml-2" />
      </button>
      <Menu open={menuOpen} onClose={handleCloseMenu} anchorPosition={anchorPosition}>
        {options.map((option, index) => (
          <MenuItem
            key={index}
            onClick={() => {
              option.action();
              handleCloseMenu();
            }}
          >
            <div className="flex items-center">
              {option.icon && <Icon name={option.icon} className="mr-2" />}
              {option.label}
            </div>
          </MenuItem>
        ))}
      </Menu>
    </div>
  );
}
