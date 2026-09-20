// Expressions are JavaScript evaluated against a scope object, exactly as NodeBox Live does it.
// The scope holds the current data row (for per-row parameters), the render data (frame, time)
// and the enclosing network's published values under `network`.

export type ExpressionFunction = (scope: Record<string, unknown>) => unknown;

const cache = new Map<string, ExpressionFunction>();

export function compileExpression(source: string): ExpressionFunction {
  let fn = cache.get(source);
  if (fn) return fn;
  // `with` keeps the expression language identical to NodeBox Live's.
  fn = new Function("scope", `with (scope) { return (${source}); }`) as ExpressionFunction;
  cache.set(source, fn);
  return fn;
}

export function evaluateExpression(source: string, scope: Record<string, unknown> = {}): unknown {
  return compileExpression(source)(scope);
}

/** Whether an expression depends on time, so the node must re-render every frame. */
export function isTimeDependent(source: string): boolean {
  return /(\$FRAME|\$TIME|\$NOW|\bframe\b|\btime\b|osc)/.test(source);
}
