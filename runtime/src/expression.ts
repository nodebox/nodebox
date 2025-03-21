export type Expression = {
  fn: (context: Record<string, any>) => unknown;
};

export function parseExpression(s: string): Expression {
  const fn = new Function("context", `with (context) { return ${s}; }`) as (context: Record<string, any>) => unknown;
  return { fn };
}

export function evaluateExpression(expression: Expression | string, context?: Record<string, any>): unknown {
  let expr: Expression;
  if (typeof expression === "string") {
    expr = parseExpression(expression);
  } else {
    expr = expression;
  }
  return expr.fn(context || {});
}
