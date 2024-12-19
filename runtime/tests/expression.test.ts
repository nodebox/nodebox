import { expect, it } from "vitest";
import { TokenType, Token, tokenize, parseExpression, evaluateExpression } from "../src";

it("can tokenize an expression", () => {
  const tokens = tokenize("1 + 2");
  expect(tokens).toEqual([
    new Token(TokenType.Number, 1),
    new Token(TokenType.Plus),
    new Token(TokenType.Number, 2),
    new Token(TokenType.Eof),
  ]);
});

it("can tokenize an expression with identifier", () => {
  const tokens = tokenize("width * 2");
  expect(tokens).toEqual([
    new Token(TokenType.Identifier, "width"),
    new Token(TokenType.Mul),
    new Token(TokenType.Number, 2),
    new Token(TokenType.Eof),
  ]);
});

it("can tokenize just an identifier", () => {
  const tokens = tokenize("x");
  expect(tokens).toEqual([new Token(TokenType.Identifier, "x"), new Token(TokenType.Eof)]);
});

it("can parse a simple expression", () => {
  const expr = parseExpression("1 + 2");
  expect(expr).toEqual({
    type: "BINARY",
    left: { type: "NUMBER", value: 1 },
    operator: { type: TokenType.Plus },
    right: { type: "NUMBER", value: 2 },
  });
});

it("can parse an expression with identifier", () => {
  const expr = parseExpression("width * 2");
  //   console.log(JSON.stringify(expr, null, 2));
  expect(expr).toEqual({
    type: "BINARY",
    left: { type: "IDENTIFIER", name: "width" },
    operator: { type: TokenType.Mul },
    right: { type: "NUMBER", value: 2 },
  });
});

it("can parse a unary minus", () => {
  const expr = parseExpression("-x - y");
  expect(expr).toEqual({
    type: "BINARY",
    left: {
      type: "UNARY",
      operator: {
        type: TokenType.Minus,
      },
      operand: {
        type: "IDENTIFIER",
        name: "x",
      },
    },
    operator: {
      type: TokenType.Minus,
    },
    right: {
      type: "IDENTIFIER",
      name: "y",
    },
  });
});

it("can evaluate a simple expression", () => {
  const result = evaluateExpression("3 + 5", {});
  expect(result).toEqual(8);
});

it("can evaluate an expression with context", () => {
  const result = evaluateExpression("width * 2", { width: 100 });
  expect(result).toEqual(200);
});

it("can evaluate a string expression", () => {
  const result = evaluateExpression(`"a" + "b"`);
  expect(result).toEqual("ab");
});

it("can evaluate a dotted expression", () => {
  const result = evaluateExpression(`network.width`, { network: { width: 1234 } });
  expect(result).toEqual(1234);
});
