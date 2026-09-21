// A small XML document model with a parser and a serializer. NodeBox documents only use elements,
// attributes, text, CDATA and comments, and the core has to run where DOMParser is absent (Node.js,
// Cloudflare Workers), so a purpose-built implementation beats a dependency.

export class XmlElement {
  tagName: string;
  attributes: Map<string, string>;
  children: XmlNode[] = [];
  parent: XmlElement | null = null;

  constructor(tagName: string, attributes: Iterable<[string, string]> = []) {
    this.tagName = tagName;
    this.attributes = new Map(attributes);
  }

  getAttribute(name: string): string | undefined {
    return this.attributes.get(name);
  }

  hasAttribute(name: string): boolean {
    return this.attributes.has(name);
  }

  setAttribute(name: string, value: string): void {
    this.attributes.set(name, value);
  }

  removeAttribute(name: string): void {
    this.attributes.delete(name);
  }

  get childElements(): XmlElement[] {
    return this.children.filter((c): c is XmlElement => c instanceof XmlElement);
  }

  childElementsWithName(name: string): XmlElement[] {
    return this.childElements.filter((c) => c.tagName === name);
  }

  firstChildElement(name: string): XmlElement | undefined {
    return this.childElements.find((c) => c.tagName === name);
  }

  appendChild(child: XmlNode): XmlNode {
    if (child instanceof XmlElement) child.parent = this;
    this.children.push(child);
    return child;
  }

  insertBefore(child: XmlNode, reference: XmlNode | null): void {
    if (child instanceof XmlElement) child.parent = this;
    const i = reference ? this.children.indexOf(reference) : -1;
    if (i < 0) this.children.push(child);
    else this.children.splice(i, 0, child);
  }

  removeChild(child: XmlNode): void {
    const i = this.children.indexOf(child);
    if (i >= 0) this.children.splice(i, 1);
    if (child instanceof XmlElement) child.parent = null;
  }

  replaceChild(newChild: XmlNode, oldChild: XmlNode): void {
    const i = this.children.indexOf(oldChild);
    if (i < 0) return;
    this.children[i] = newChild;
    if (newChild instanceof XmlElement) newChild.parent = this;
    if (oldChild instanceof XmlElement) oldChild.parent = null;
  }

  /** The concatenated text content of the direct text and CDATA children. */
  get textContent(): string {
    return this.children
      .filter((c): c is XmlText => c instanceof XmlText)
      .map((c) => c.text)
      .join("");
  }

  /** Depth-first walk over this element and all descendant elements. */
  *descendants(): Generator<XmlElement> {
    yield this;
    for (const child of this.children) if (child instanceof XmlElement) yield* child.descendants();
  }
}

export class XmlText {
  text: string;
  isCData: boolean;
  constructor(text: string, isCData = false) {
    this.text = text;
    this.isCData = isCData;
  }
}

export class XmlComment {
  text: string;
  constructor(text: string) {
    this.text = text;
  }
}

export type XmlNode = XmlElement | XmlText | XmlComment;

export interface XmlDocument {
  root: XmlElement;
  /** The XML declaration, e.g. `<?xml version="1.0" encoding="UTF-8"?>`; kept for round trips. */
  declaration?: string;
}

export class XmlParseError extends Error {
  constructor(message: string, public position: number) {
    super(`${message} (at offset ${position})`);
  }
}

const ENTITIES: Record<string, string> = { lt: "<", gt: ">", amp: "&", quot: '"', apos: "'" };

export function decodeEntities(s: string): string {
  if (s.indexOf("&") < 0) return s;
  return s.replace(/&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);/g, (match, body: string) => {
    if (body[0] === "#") {
      const code = body[1] === "x" || body[1] === "X" ? parseInt(body.slice(2), 16) : parseInt(body.slice(1), 10);
      return Number.isFinite(code) ? String.fromCodePoint(code) : match;
    }
    return ENTITIES[body] ?? match;
  });
}

export function escapeText(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

export function escapeAttribute(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/\n/g, "&#10;")
    .replace(/\r/g, "&#13;")
    .replace(/\t/g, "&#9;");
}

/** Parse an XML string. Namespaces, DTDs and processing instructions are tolerated but not interpreted. */
export function parseXml(xml: string): XmlDocument {
  let pos = 0;
  const n = xml.length;
  let declaration: string | undefined;
  let root: XmlElement | null = null;
  let current: XmlElement | null = null;

  function fail(message: string): never {
    throw new XmlParseError(message, pos);
  }

  function parseName(): string {
    const start = pos;
    while (pos < n) {
      const c = xml.charCodeAt(pos);
      // Letters, digits, '_', '-', '.', ':' and anything non-ASCII.
      if (
        (c >= 65 && c <= 90) ||
        (c >= 97 && c <= 122) ||
        (c >= 48 && c <= 57) ||
        c === 95 ||
        c === 45 ||
        c === 46 ||
        c === 58 ||
        c > 127
      ) {
        pos++;
      } else break;
    }
    if (pos === start) fail("Expected a name");
    return xml.slice(start, pos);
  }

  function skipWhitespace(): void {
    while (pos < n) {
      const c = xml.charCodeAt(pos);
      if (c === 32 || c === 9 || c === 10 || c === 13) pos++;
      else break;
    }
  }

  function addText(text: string, isCData: boolean): void {
    if (current === null) return;
    if (!isCData && text.trim().length === 0) return;
    current.appendChild(new XmlText(isCData ? text : decodeEntities(text), isCData));
  }

  while (pos < n) {
    const lt = xml.indexOf("<", pos);
    if (lt < 0) {
      addText(xml.slice(pos), false);
      break;
    }
    if (lt > pos) addText(xml.slice(pos, lt), false);
    pos = lt;
    if (xml.startsWith("<?", pos)) {
      const end = xml.indexOf("?>", pos);
      if (end < 0) fail("Unterminated processing instruction");
      const pi = xml.slice(pos, end + 2);
      if (pi.startsWith("<?xml") && declaration === undefined) declaration = pi;
      pos = end + 2;
    } else if (xml.startsWith("<!--", pos)) {
      const end = xml.indexOf("-->", pos);
      if (end < 0) fail("Unterminated comment");
      if (current) current.appendChild(new XmlComment(xml.slice(pos + 4, end)));
      pos = end + 3;
    } else if (xml.startsWith("<![CDATA[", pos)) {
      const end = xml.indexOf("]]>", pos);
      if (end < 0) fail("Unterminated CDATA section");
      addText(xml.slice(pos + 9, end), true);
      pos = end + 3;
    } else if (xml.startsWith("<!", pos)) {
      // DOCTYPE and friends: skip to the matching '>' (internal subsets are not supported).
      const end = xml.indexOf(">", pos);
      if (end < 0) fail("Unterminated declaration");
      pos = end + 1;
    } else if (xml.startsWith("</", pos)) {
      pos += 2;
      const name = parseName();
      skipWhitespace();
      if (xml.charAt(pos) !== ">") fail("Expected '>'");
      pos++;
      if (current === null || current.tagName !== name) fail(`Unexpected closing tag </${name}>`);
      current = current.parent;
    } else {
      pos++;
      const name = parseName();
      const element = new XmlElement(name);
      for (;;) {
        skipWhitespace();
        const c = xml.charAt(pos);
        if (c === ">") {
          pos++;
          break;
        }
        if (c === "/") {
          if (xml.charAt(pos + 1) !== ">") fail("Expected '/>'");
          pos += 2;
          element.parent = current;
          if (current) current.children.push(element);
          else if (root === null) root = element;
          else fail("Multiple root elements");
          element.parent = current;
          // Self-closing: do not descend.
          current = current;
          break;
        }
        if (pos >= n) fail("Unterminated start tag");
        const attrName = parseName();
        skipWhitespace();
        if (xml.charAt(pos) !== "=") fail(`Expected '=' after attribute ${attrName}`);
        pos++;
        skipWhitespace();
        const quote = xml.charAt(pos);
        if (quote !== '"' && quote !== "'") fail("Expected a quoted attribute value");
        const end = xml.indexOf(quote, pos + 1);
        if (end < 0) fail("Unterminated attribute value");
        element.attributes.set(attrName, decodeEntities(xml.slice(pos + 1, end)));
        pos = end + 1;
      }
      // A start tag (not self-closing) makes the element current.
      if (xml.charAt(pos - 1) === ">" && xml.charAt(pos - 2) !== "/") {
        if (current) current.appendChild(element);
        else if (root === null) root = element;
        else fail("Multiple root elements");
        current = element;
      }
    }
  }
  if (current !== null) fail(`Unclosed element <${current.tagName}>`);
  if (root === null) throw new XmlParseError("No root element", 0);
  return { root, declaration };
}

export interface SerializeOptions {
  indent?: string;
  /** Include the XML declaration (defaults to the parsed one or the standard UTF-8 declaration). */
  declaration?: string | false;
}

/** Serialize a document with one element per line and nested indentation, as NodeBox 3 writes it. */
export function serializeXml(doc: XmlDocument, options: SerializeOptions = {}): string {
  const indent = options.indent ?? "    ";
  const lines: string[] = [];
  const declaration =
    options.declaration === false
      ? undefined
      : options.declaration ?? doc.declaration ?? '<?xml version="1.0" encoding="UTF-8" standalone="no"?>';
  if (declaration) lines.push(declaration);
  serializeElement(doc.root, "", indent, lines);
  return lines.join("\n") + "\n";
}

function serializeElement(element: XmlElement, prefix: string, indent: string, lines: string[]): void {
  let open = `${prefix}<${element.tagName}`;
  for (const [name, value] of element.attributes) open += ` ${name}="${escapeAttribute(value)}"`;
  if (element.children.length === 0) {
    lines.push(`${open}/>`);
    return;
  }
  const onlyText = element.children.every((c) => c instanceof XmlText);
  if (onlyText) {
    const text = element.children.map((c) => serializeText(c as XmlText)).join("");
    lines.push(`${open}>${text}</${element.tagName}>`);
    return;
  }
  lines.push(`${open}>`);
  for (const child of element.children) {
    if (child instanceof XmlElement) serializeElement(child, prefix + indent, indent, lines);
    else if (child instanceof XmlText) lines.push(prefix + indent + serializeText(child));
    else lines.push(`${prefix}${indent}<!--${child.text}-->`);
  }
  lines.push(`${prefix}</${element.tagName}>`);
}

function serializeText(text: XmlText): string {
  return text.isCData ? `<![CDATA[${text.text}]]>` : escapeText(text.text);
}
