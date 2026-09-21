enum TokenType {
  STRING = "STRING",
  COMMENT = "COMMENT",
  IDENTIFIER = "IDENTIFIER",
  PUNCTUATION = "PUNCTUATION",
  WHITESPACE = "WHITESPACE",
  OTHER = "OTHER",
  NUMBER = "NUMBER",
}

interface Token {
  type: TokenType;
  value: string;
}

function lexer(source: string): Token[] {
  const tokens: Token[] = [];
  let i = 0;

  while (i < source.length) {
    let char = source[i];

    if (char === '"' || char === "'") {
      let value = char;
      const quote = char;
      i++;
      while (i < source.length && source[i] !== quote) {
        if (source[i] === "\\" && i + 1 < source.length) {
          value += source[i] + source[i + 1];
          i += 2;
        } else {
          value += source[i];
          i++;
        }
      }
      if (i < source.length) {
        value += source[i];
        i++;
      }
      tokens.push({ type: TokenType.STRING, value });
    } else if (char === "/" && source[i + 1] === "/") {
      let value = "//";
      i += 2;
      while (i < source.length && source[i] !== "\n") {
        value += source[i];
        i++;
      }
      tokens.push({ type: TokenType.COMMENT, value });
    } else if (char === "/" && source[i + 1] === "*") {
      let value = "/*";
      i += 2;
      while (i < source.length && !(source[i] === "*" && source[i + 1] === "/")) {
        value += source[i];
        i++;
      }
      if (i < source.length) {
        value += "*/";
        i += 2;
      }
      tokens.push({ type: TokenType.COMMENT, value });
    } else if (/[a-zA-Z_$]/.test(char)) {
      let value = "";
      while (i < source.length && /[a-zA-Z0-9_$]/.test(source[i])) {
        value += source[i];
        i++;
      }
      tokens.push({ type: TokenType.IDENTIFIER, value });
    } else if (/[0-9]/.test(char) || (char === "." && /[0-9]/.test(source[i + 1]))) {
      let value = "";
      while (i < source.length && /[0-9.]/.test(source[i])) {
        value += source[i];
        i++;
      }
      tokens.push({ type: TokenType.NUMBER, value });
    } else if (/[(){};,.:=+\-*/%<>!&|^~]/.test(char)) {
      tokens.push({ type: TokenType.PUNCTUATION, value: char });
      i++;
    } else if (/\s/.test(char)) {
      let value = "";
      while (i < source.length && /\s/.test(source[i])) {
        value += source[i];
        i++;
      }
      tokens.push({ type: TokenType.WHITESPACE, value });
    } else {
      tokens.push({ type: TokenType.OTHER, value: char });
      i++;
    }
  }

  return tokens;
}

function findNodeStatements(source: string): string[] {
  const tokens = lexer(source);
  const statements: string[] = [];
  let currentStatement = "";
  let bracketCount = 0;
  let inNodeStatement = false;

  for (let i = 0; i < tokens.length; i++) {
    const token = tokens[i];
    const nextToken = tokens[i + 1];
    const nextNextToken = tokens[i + 2];

    if (
      !inNodeStatement &&
      token.type === TokenType.IDENTIFIER &&
      token.value === "node" &&
      nextToken?.type === TokenType.PUNCTUATION &&
      nextToken.value === "." &&
      nextNextToken?.type === TokenType.IDENTIFIER &&
      !nextNextToken.value.startsWith("on")
    ) {
      inNodeStatement = true;
      currentStatement = "";
    }

    if (inNodeStatement) {
      currentStatement += token.value;

      if (token.type === TokenType.PUNCTUATION) {
        if (token.value === "(" || token.value === "{" || token.value === "[") {
          bracketCount++;
        } else if (token.value === ")" || token.value === "}" || token.value === "]") {
          bracketCount--;
          if (bracketCount === 0) {
            statements.push(currentStatement.trim());
            inNodeStatement = false;
            currentStatement = "";
          }
        }
      }
    }
  }

  // Handle case where the last statement is incomplete
  if (inNodeStatement && currentStatement.trim() !== "") {
    statements.push(currentStatement.trim());
  }

  return statements;
}
export { findNodeStatements };
