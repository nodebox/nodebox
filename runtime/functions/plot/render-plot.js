/**
 * Render Vega spec to graphics.
 *
 * @category Plot
 */

import { emptyPlot } from "project:Utilities";
import { parse as parseVega, View } from "https://esm.sh/vega@5";
import { parseSVG } from "@ndbx/g";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });
  const specIn = node.stringIn({ name: "spec", label: "Plot specification", widget: "TEXT" });
  const shapeOut = node.shapeOut({ name: "Out" });

  specIn.defaultValue = JSON.stringify(emptyPlot);

  node.onRender = async () => {
    const specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : JSON.parse(specIn.value));
    const flow = parseVega(specOut);
    const view = new View(flow);
    view.finalize();
    const svg = await view.toSVG(1.0);
    const shape = parseSVG(svg);
    shapeOut.set(shape);
  };
}
