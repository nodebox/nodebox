import Icon from "./icon";

interface ToggleProps {
  label: string;
  value: boolean;
  onChange: (value: boolean) => void;
}

export default function Toggle({ label, value, onChange }: ToggleProps) {
  return (
    <span className="flex items-center cursor-pointer select-none" onClick={() => onChange(!value)}>
      <Icon name={value ? "checkbox-checked" : "checkbox"} size={24} />
      <span className="text-xs text-zinc-200">{label}</span>
    </span>
  );
}
