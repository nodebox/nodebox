// Derive a function item's parameters and ports from its source without running it, the way
// NodeBox Live does at load time (packages/runtime/src/lexer.ts and loaders.ts). The statements
// `node.numberIn({...})` etc. are found by a small tokenizer that skips strings and comments.

import { LiveChoice, LiveParameter, LiveParameterType, LivePort, LivePortType, LiveSection, LiveWidgetType } from "./types";

enum TokenType {
  String,
  Comment,
  Identifier,
  Punctuation,
  Whitespace,
  Number,
  Other,
}

interface Token {
  type: TokenType;
  value: string;
}

function tokenize(source: string): Token[] {
  const tokens: Token[] = [];
  let i = 0;
  const n = source.length;
  while (i < n) {
    const c = source[i];
    if (c === '"' || c === "'" || c === "`") {
      let value = c;
      i++;
      while (i < n && source[i] !== c) {
        if (source[i] === "\\" && i + 1 < n) {
          value += source[i] + source[i + 1];
          i += 2;
        } else {
          value += source[i++];
        }
      }
      if (i < n) value += source[i++];
      tokens.push({ type: TokenType.String, value });
    } else if (c === "/" && source[i + 1] === "/") {
      let value = "";
      while (i < n && source[i] !== "\n") value += source[i++];
      tokens.push({ type: TokenType.Comment, value });
    } else if (c === "/" && source[i + 1] === "*") {
      let value = "";
      while (i < n && !(source[i] === "*" && source[i + 1] === "/")) value += source[i++];
      i += 2;
      tokens.push({ type: TokenType.Comment, value });
    } else if (/[A-Za-z_$]/.test(c)) {
      let value = "";
      while (i < n && /[A-Za-z0-9_$]/.test(source[i])) value += source[i++];
      tokens.push({ type: TokenType.Identifier, value });
    } else if (/[0-9]/.test(c)) {
      let value = "";
      while (i < n && /[0-9.eExXa-fA-F]/.test(source[i])) value += source[i++];
      tokens.push({ type: TokenType.Number, value });
    } else if (/\s/.test(c)) {
      let value = "";
      while (i < n && /\s/.test(source[i])) value += source[i++];
      tokens.push({ type: TokenType.Whitespace, value });
    } else if (/[{}()[\].,;:=+\-*/%<>!&|?~^]/.test(c)) {
      tokens.push({ type: TokenType.Punctuation, value: c });
      i++;
    } else {
      tokens.push({ type: TokenType.Other, value: c });
      i++;
    }
  }
  return tokens;
}

/** The texts of all `node.<method>(...)` calls in the source, excluding `node.onX = ...` assignments. */
export function findNodeStatements(source: string): string[] {
  const tokens = tokenize(source).filter((t) => t.type !== TokenType.Comment && t.type !== TokenType.Whitespace);
  const statements: string[] = [];
  for (let i = 0; i + 3 < tokens.length; i++) {
    if (tokens[i].type !== TokenType.Identifier || tokens[i].value !== "node") continue;
    if (tokens[i + 1].value !== ".") continue;
    const method = tokens[i + 2];
    if (method.type !== TokenType.Identifier || method.value.startsWith("on")) continue;
    if (tokens[i + 3].value !== "(") continue;
    let depth = 0;
    let text = "node." + method.value;
    for (let j = i + 3; j < tokens.length; j++) {
      const t = tokens[j];
      text += t.type === TokenType.Punctuation ? t.value : (j > i + 3 && tokens[j - 1].type === TokenType.Identifier && t.type === TokenType.Identifier ? " " : "") + t.value;
      if (t.value === "(" || t.value === "{" || t.value === "[") depth++;
      else if (t.value === ")" || t.value === "}" || t.value === "]") {
        depth--;
        if (depth === 0) {
          i = j;
          break;
        }
      }
    }
    statements.push(text);
  }
  return statements;
}

export interface FunctionSignature {
  parameters: LiveParameter[];
  sections: LiveSection[];
  inputPorts: LivePort[];
  outputPorts: LivePort[];
  description: string;
  category: string;
}

const PARAMETER_TYPES: Record<string, LiveParameterType> = {
  numberIn: "NUMBER",
  stringIn: "STRING",
  booleanIn: "BOOLEAN",
  pointIn: "POINT",
  colorIn: "COLOR",
  fileIn: "FILE",
  choiceIn: "CHOICE",
};
const INPUT_TYPES: Record<string, LivePortType> = { tableIn: "TABLE", shapeIn: "SHAPE", specIn: "SPEC" };
const OUTPUT_TYPES: Record<string, LivePortType> = { tableOut: "TABLE", shapeOut: "SHAPE", specOut: "SPEC" };

export function analyzeFunctionSource(source: string): FunctionSignature {
  const parameters: LiveParameter[] = [];
  const sections: LiveSection[] = [];
  const inputPorts: LivePort[] = [];
  const outputPorts: LivePort[] = [];
  let currentSection: LiveSection | undefined;
  for (const statement of findNodeStatements(source)) {
    const m = /^node\.(\w+)\(/.exec(statement);
    if (!m) continue;
    const method = m[1];
    const argsText = statement.slice(statement.indexOf("(") + 1, statement.lastIndexOf(")"));
    const args = parseArguments(argsText);
    if (PARAMETER_TYPES[method]) parameters.push(createParameter(PARAMETER_TYPES[method], args, currentSection?.name));
    else if (INPUT_TYPES[method]) inputPorts.push({ name: String(args.name ?? ""), type: INPUT_TYPES[method], label: args.label as string | undefined });
    else if (OUTPUT_TYPES[method]) outputPorts.push({ name: String(args.name ?? ""), type: OUTPUT_TYPES[method], label: args.label as string | undefined });
    else if (method === "pushSection") {
      currentSection = { name: String(args.name ?? ""), collapsed: Boolean(args.collapsed) };
      sections.push(currentSection);
    } else if (method === "popSection") currentSection = undefined;
  }
  const doc = extractJsDoc(source);
  return { parameters, sections, inputPorts, outputPorts, ...doc };
}

/** Argument objects are JavaScript object literals; evaluate them as such (JSON5 superset). */
function parseArguments(text: string): Record<string, unknown> {
  if (text.trim() === "") return {};
  try {
    const value = new Function(`"use strict"; return (${text});`)();
    return value && typeof value === "object" ? (value as Record<string, unknown>) : {};
  } catch {
    // Arguments that reference variables cannot be evaluated statically; keep the name if possible.
    const name = /name\s*:\s*["'`]([^"'`]*)["'`]/.exec(text);
    return name ? { name: name[1] } : {};
  }
}

export function parseChoices(choices: unknown): LiveChoice[] | undefined {
  if (!Array.isArray(choices) || choices.length === 0) return undefined;
  if (typeof choices[0] === "string") return (choices as string[]).map((name) => ({ name, label: startCase(name) }));
  return (choices as string[][]).map(([name, label]) => ({ name, label: label ?? name }));
}

export function startCase(s: string): string {
  return s
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/[_-]+/g, " ")
    .split(" ")
    .filter((w) => w.length > 0)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

export function convertToLabel(name: string): string {
  const spaced = name.replace(/([a-z])([A-Z])/g, "$1 $2");
  return spaced.charAt(0).toUpperCase() + spaced.slice(1).toLowerCase();
}

export function defaultLiveValue(type: LiveParameterType): LiveParameter["defaultValue"] {
  switch (type) {
    case "NUMBER":
      return 0;
    case "BOOLEAN":
      return false;
    case "POINT":
      return { x: 0, y: 0 };
    case "COLOR":
      return { r: 0, g: 0, b: 0, a: 1 };
    default:
      return "";
  }
}

function createParameter(type: LiveParameterType, args: Record<string, unknown>, section?: string): LiveParameter {
  return {
    name: String(args.name ?? ""),
    type,
    widget: (args.widget as LiveWidgetType) ?? (type as LiveWidgetType),
    label: (args.label as string) ?? convertToLabel(String(args.name ?? "")),
    section,
    defaultValue: (args.value as LiveParameter["defaultValue"]) ?? defaultLiveValue(type),
    choices: parseChoices(args.choices),
    min: typeof args.min === "number" ? args.min : -Infinity,
    max: typeof args.max === "number" ? args.max : Infinity,
    step: typeof args.step === "number" && args.step ? args.step : 1,
  };
}

export function extractJsDoc(source: string): { description: string; category: string } {
  const m = /\/\*\*([\s\S]*?)\*\//.exec(source);
  if (!m) return { description: "", category: "" };
  const lines = m[1].split("\n").map((l) => l.trim().replace(/^\*\s?/, ""));
  const firstTag = lines.findIndex((l) => l.startsWith("@"));
  const descriptionLines = firstTag < 0 ? lines : lines.slice(0, firstTag);
  const tags = new Map<string, string>();
  for (const line of firstTag < 0 ? [] : lines.slice(firstTag)) {
    if (!line.startsWith("@")) continue;
    const [tag, ...rest] = line.split(" ");
    tags.set(tag.slice(1), rest.join(" "));
  }
  return { description: descriptionLines.join("\n").trim(), category: tags.get("category") ?? "" };
}
