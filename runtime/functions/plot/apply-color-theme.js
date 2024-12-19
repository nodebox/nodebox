/**
 * Apply a set of colors to change the style of the plot.
 *
 * The background color is used for the background only.
 * The primary color is used for the titles, axis lines, tick marks, labels, and legend text.
 * The secundary color is used for the grid lines.
 * The accent color is used for the marks (e.g. bars, lines, points) if no color scale is set.
 * The color scheme is used as the range for color scales.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec, applyColorTheme } from "project:Utilities";
import { parse as parseVega } from "https://esm.sh/vega@5";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });
  const bgColorIn = node.colorIn({ name: "bgColor", label: "Background color", value: "#f2f2f2" });
  const primColorIn = node.colorIn({ name: "primColor", label: "Primary color", value: "#5a5a5A" });
  const secColorIn = node.colorIn({ name: "secColor", label: "Secundary color", value: "#8c8c8c" });
  const accentColorIn = node.colorIn({ name: "accentColor", label: "Accent color", value: "#2ba197" });
  const colorSchemeIn = node.stringIn({
    name: "colorScheme",
    label: "Color scheme",
    value:
      '["#286498", "#2ba197", "#f36923", "#891c1a", "#b1c8eb", "#0b5313", "#753fc2", "#e5196a", "#65a10e", "#ffa8ff"]',
  });
  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });

  node.onRender = () => {
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);
    const bgColor = bgColorIn.value.toHex() || undefined;
    const primColor = primColorIn.value.toHex() || undefined;
    const secColor = secColorIn.value.toHex() || undefined;
    const accentColor = accentColorIn.value.toHex() || undefined;
    const colorScheme = JSON.parse(colorSchemeIn.value) || undefined;

    applyColorTheme(specOut, bgColor, primColor, secColor, accentColor, colorScheme);

    plotSpecOut.set(specOut);
  };
}
