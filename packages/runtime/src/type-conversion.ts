import { ParameterType, Point } from "./types";

export function parseParameterValue(
  value: string,
  type?: ParameterType,
): number | boolean | Point | string | undefined {
  if (!type) {
    return undefined;
  }

  switch (type) {
    case ParameterType.Number:
      const parsedNumber = parseFloat(value);
      return isNaN(parsedNumber) ? 0 : parsedNumber;

    case ParameterType.Boolean:
      return value === "true";

    case ParameterType.Point:
      return parsePoint(value);

    default:
      return value;
  }
}

function parsePoint(value: string): Point {
  if (!value) {
    return { x: 0, y: 0 };
  }

  const parts = value.split(",");
  const x = parseFloat(parts[0]);
  const y = parseFloat(parts[1] ?? parts[0]);

  if (isNaN(x) || isNaN(y)) {
    return { x: 0, y: 0 };
  }

  return { x, y };
}

export function formatParameterValue(value: number | boolean | Point | string, type?: ParameterType): string {
  if (!type) {
    return "";
  }

  switch (type) {
    case ParameterType.Number:
      return String(value);

    case ParameterType.String:
      return value as string;

    case ParameterType.Boolean:
      return value ? "true" : "false";

    case ParameterType.Point:
      return formatPoint(value as Point);

    default:
      return String(value);
  }
}

function formatPoint(value: Point): string {
  return value ? `${value.x}, ${value.y}` : "0, 0";
}
