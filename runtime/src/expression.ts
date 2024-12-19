export enum TokenType {
  Eof = "EOF",
  Plus = "PLUS",
  Minus = "MINUS",
  Mul = "MUL",
  Div = "DIV",
  Lparen = "LPAREN",
  Rparen = "RPAREN",
  Dot = "DOT",
  Number = "NUMBER",
  String = "STRING",
  Identifier = "IDENTIFIER",
}

export class Token {
  type: TokenType;
  value?: number | string;

  constructor(type: TokenType, value?: number | string) {
    this.type = type;
    this.value = value;
  }
}

export function tokenize(input: string): Token[] {
  const tokens = [];
  let i = 0;

  while (i < input.length) {
    const char = input[i];

    if (/\s/.test(char)) {
      i++;
    } else if (/\d/.test(char)) {
      let num = "";
      // Integer part
      while (/\d/.test(input[i])) {
        num += input[i];
        i++;
      }
      // Decimal part, if exists
      if (input[i] === ".") {
        num += ".";
        i++;
        // It's crucial to check for digits after the decimal point to avoid incorrect tokens
        if (!/\d/.test(input[i])) {
          throw new Error("Invalid float format after decimal point");
        }
        while (/\d/.test(input[i])) {
          num += input[i];
          i++;
        }
      }
      tokens.push(new Token(TokenType.Number, parseFloat(num)));
    } else if (/[a-zA-Z_]/.test(char)) {
      let id = "";
      while (i < input.length && /[a-zA-Z0-9_]/.test(input[i])) {
        id += input[i++];
      }
      tokens.push(new Token(TokenType.Identifier, id));
    } else if (char === "+") {
      tokens.push(new Token(TokenType.Plus));
      i++;
    } else if (char === "-") {
      tokens.push(new Token(TokenType.Minus));
      i++;
    } else if (char === "*") {
      tokens.push(new Token(TokenType.Mul));
      i++;
    } else if (char === "/") {
      tokens.push(new Token(TokenType.Div));
      i++;
    } else if (char === "(") {
      tokens.push(new Token(TokenType.Lparen));
      i++;
    } else if (char === ")") {
      tokens.push(new Token(TokenType.Rparen));
      i++;
    } else if (char === ".") {
      tokens.push(new Token(TokenType.Dot));
      i++;
    } else if (char === '"') {
      i++;
      let strValue = "";
      while (i < input.length && input[i] !== '"') {
        strValue += input[i];
        i++;
      }
      if (input[i] === '"') {
        i++; // Skip the closing quote
        tokens.push(new Token(TokenType.String, strValue));
      } else {
        throw new Error("Unterminated string literal");
      }
    } else {
      throw new Error("Invalid character: " + char);
    }
  }
  tokens.push(new Token(TokenType.Eof));
  return tokens;
}

// AST node types
abstract class Expression {
  type: string;

  constructor(type: string) {
    this.type = type;
  }
}

class BinaryExpression extends Expression {
  constructor(
    public left: Expression,
    public operator: Token,
    public right: Expression,
  ) {
    super("BINARY");
  }
}

class UnaryExpression extends Expression {
  constructor(
    public operator: Token,
    public operand: Expression,
  ) {
    super("UNARY");
  }
}

class NumberLiteral extends Expression {
  constructor(public value: number) {
    super("NUMBER");
  }
}

class StringLiteral extends Expression {
  constructor(public value: string) {
    super("STRING");
  }
}

class Identifier extends Expression {
  constructor(public name: string) {
    super("IDENTIFIER");
  }
}

class MemberExpression extends Expression {
  constructor(
    public object: Expression,
    public property: Expression,
  ) {
    super("MEMBER");
  }
}

export class Parser {
  private tokens: Token[];
  private position: number;

  constructor(tokens: Token[]) {
    this.tokens = tokens;
    this.position = 0;
  }

  private get currentToken(): Token {
    return this.tokens[this.position];
  }

  private match(type: TokenType): Token | null {
    if (this.currentToken.type === type) {
      return this.tokens[this.position++];
    }
    return null;
  }

  private factor(): Expression {
    const token = this.currentToken;

    if (this.match(TokenType.Number)) {
      return new NumberLiteral(token.value as number);
    } else if (this.match(TokenType.Identifier)) {
      let expr: Identifier | MemberExpression = new Identifier(token.value as string);
      //   return this.parseMemberExpression();

      while (this.match(TokenType.Dot)) {
        const property = this.currentToken;
        if (!this.match(TokenType.Identifier)) {
          throw new Error("Expected identifier after dot");
        }
        expr = new MemberExpression(expr, new Identifier(property.value as string));
      }
      return expr;
    } else if (this.match(TokenType.Lparen)) {
      const expr = this.expression();
      this.match(TokenType.Rparen);
      return expr;
    } else if (this.match(TokenType.Minus)) {
      return new UnaryExpression(token, this.factor());
    } else if (this.match(TokenType.String)) {
      return new StringLiteral(token.value as string);
    }

    throw new Error(`Unexpected token: ${token.type}`);
  }

  private term(): Expression {
    let node = this.factor();

    while (this.currentToken.type === TokenType.Mul || this.currentToken.type === TokenType.Div) {
      const token = this.currentToken;
      if (this.match(TokenType.Mul)) {
        node = new BinaryExpression(node, token, this.factor());
      } else if (this.match(TokenType.Div)) {
        node = new BinaryExpression(node, token, this.factor());
      }
    }

    return node;
  }

  private expression(): Expression {
    let node = this.term();

    while (this.currentToken.type === TokenType.Plus || this.currentToken.type === TokenType.Minus) {
      const token = this.currentToken;
      if (this.match(TokenType.Plus)) {
        node = new BinaryExpression(node, token, this.term());
      } else if (this.match(TokenType.Minus)) {
        node = new BinaryExpression(node, token, this.term());
      }
    }

    return node;
  }

  public parse(): Expression {
    return this.expression();
  }
}

export function parseExpression(s: string): Expression {
  const tokens = tokenize(s);
  const parser = new Parser(tokens);
  return parser.parse();
}

export function evaluateExpression(expression: Expression | string, context?: Record<string, any>): unknown {
  if (typeof expression === "string") {
    expression = parseExpression(expression);
  }

  if (!context) {
    context = {};
  }

  switch (expression.type) {
    case "NUMBER":
      return (expression as NumberLiteral).value;

    case "STRING":
      return (expression as StringLiteral).value;

    case "IDENTIFIER":
      const identifier = expression as Identifier;
      const value = context[identifier.name];
      if (value !== undefined) {
        return value;
      }
      throw new Error(`Undefined identifier: ${identifier.name}`);

    case "UNARY":
      const unary = expression as UnaryExpression;
      const operand = evaluateExpression(unary.operand, context) as number;
      if (unary.operator.type === TokenType.Minus) {
        return -operand;
      }
      throw new Error(`Unsupported unary operator: ${unary.operator.type}`);

    case "BINARY":
      const binary = expression as BinaryExpression;
      const left = evaluateExpression(binary.left, context) as number;
      const right = evaluateExpression(binary.right, context) as number;
      switch (binary.operator.type) {
        case TokenType.Plus:
          return left + right;
        case TokenType.Minus:
          return left - right;
        case TokenType.Mul:
          return left * right;
        case TokenType.Div:
          if (right === 0) {
            throw new Error("Division by zero");
          }
          return left / right;
        default:
          throw new Error(`Unsupported binary operator: ${binary.operator.type}`);
      }

    case "MEMBER":
      const member = expression as MemberExpression;
      const object: any = evaluateExpression(member.object, context);
      if (typeof object === "object" && object !== null) {
        const property = (member.property as Identifier).name;
        const value = object[property];
        if (value !== undefined) {
          return object[property];
        }
      }
      throw new Error(
        `Invalid member access: ${(member.object as Identifier).name}.${(member.property as Identifier).name}`,
      );

    default:
      throw new Error(`Invalid expression type: ${expression.type}`);
  }
}
