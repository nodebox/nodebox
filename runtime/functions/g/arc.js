/**
 * Draw an arc or ring segment.
 *
 * The arc can be configured with:
 * - Center point (x, y)
 * - Inner and outer radius to create ring segments
 * - Start and end angles in degrees (0-360)
 * - Fill, stroke and stroke width for styling
 *
 * When inner radius equals 0, it creates a pie-slice shape.
 * When inner radius approaches outer radius, it creates a thin arc.
 *
 * @category Graphics
 */

import { Path, Group } from "@ndbx/g";

export default function (node) {
  // Input table for handling multiple arcs
  const tableIn = node.tableIn({ name: "table" });

  // Center point parameters
  const xIn = node.numberIn({ name: "x", value: 100 });
  const yIn = node.numberIn({ name: "y", value: 100 });

  // Radius parameters
  const innerRadiusIn = node.numberIn({ name: "innerRadius", value: 0, min: 0 });
  const outerRadiusIn = node.numberIn({ name: "outerRadius", value: 75, min: 0 });

  // Angle parameters (in degrees)
  const startAngleIn = node.numberIn({ name: "startAngle", value: 0, min: 0, max: 360 });
  const endAngleIn = node.numberIn({ name: "endAngle", value: 360, min: 0, max: 360 });

  // Visual properties
  const fillIn = node.colorIn({ name: "fill", value: "black" });
  const strokeIn = node.colorIn({ name: "stroke", value: "transparent" });
  const strokeWidthIn = node.numberIn({ name: "strokeWidth", value: 1, min: 0 });

  const shapeOut = node.shapeOut({ name: "Out" });

  function degreesToRadians(degrees) {
    return degrees * (Math.PI / 180);
  }

  function createArc(x, y, innerRadius, outerRadius, startAngleDegrees, endAngleDegrees, fill, stroke, strokeWidth) {
    const startAngleRadians = degreesToRadians(startAngleDegrees);
    const endAngleRadians = degreesToRadians(endAngleDegrees);
    const path = new Path();
    path.complexArc(x, y, innerRadius, outerRadius, startAngleRadians, endAngleRadians);
    path.fill = fill;
    path.stroke = stroke;
    path.strokeWidth = strokeWidth;
    return path;
  }

  node.onRender = () => {
    if (!tableIn.value) {
      // Create a single arc
      const arc = createArc(
        xIn.value,
        yIn.value,
        innerRadiusIn.value,
        outerRadiusIn.value,
        startAngleIn.value,
        endAngleIn.value,
        fillIn.value,
        strokeIn.value,
        strokeWidthIn.value,
      );

      shapeOut.set(arc);
    } else {
      // Create one arc per row in the table
      const [fx, fy, fir, for_, fsa, fea, ff, fs, fsw] = [
        xIn.fn,
        yIn.fn,
        innerRadiusIn.fn,
        outerRadiusIn.fn,
        startAngleIn.fn,
        endAngleIn.fn,
        fillIn.fn,
        strokeIn.fn,
        strokeWidthIn.fn,
      ];

      const table = tableIn.value;
      const group = new Group();
      for (const d of table) {
        const arc = createArc(fx(d), fy(d), fir(d), for_(d), fsa(d), fea(d), ff(d), fs(d), fsw(d));
        group.add(arc);
      }
      shapeOut.set(group);
    }
  };
}
