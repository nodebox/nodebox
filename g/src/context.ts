import { ClipPath } from "./clip-path";
import { Paint, LinearGradientPaint, RadialGradientPaint } from "./paint";

export class Context {
  clipPaths: ClipPath[] = [];
  gradients: (LinearGradientPaint | RadialGradientPaint)[] = [];

  constructor() {}

  addLinearGradient(gradient: LinearGradientPaint) {
    this.gradients.push(gradient);
  }

  addRadialGradient(gradient: RadialGradientPaint) {
    this.gradients.push(gradient);
  }
}
