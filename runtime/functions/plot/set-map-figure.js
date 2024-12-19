/**
 * Define the properties of the map figure.
 *
 * @category Geo
 */

import { emptyMap, validateVegaSpec, setGeoMap, setGraticule, setThemeProperties } from "project:Utilities";

export default function (node) {
  // General properties (from Set Plot Figure)
  node.pushSection({ name: "General" });
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });
  const widthIn = node.numberIn({ name: "width", label: "Width", value: 400 });
  const heightIn = node.numberIn({ name: "height", label: "Height", value: 400 });
  const paddingIn = node.numberIn({ name: "padding", label: "Padding", value: 10 });
  const padTopIn = node.numberIn({ name: "padTop", label: "Top padding" });
  const padBottomIn = node.numberIn({ name: "padBottom", label: "Bottom padding" });
  const padLeftIn = node.numberIn({ name: "padLeft", label: "Left padding" });
  const padRightIn = node.numberIn({ name: "padRight", label: "Right padding" });
  const autosizeIn = node.stringIn({
    name: "autosize",
    label: "Autosize",
    value: "none",
    choices: ["pad", "fit", "fit-x", "fit-y", "none"],
  });
  const bgColorIn = node.colorIn({ name: "bgColor", label: "Background color", value: "#f2f2f2" });
  node.popSection();

  // Map properties
  node.pushSection({ name: "Map Properties", collapsed: false });
  const targetProjectIn = node.stringIn({
    name: "projection",
    label: "Projection",
    value: "mercator",
    choices: [
      ["mercator", "Mercator"],
      ["albers", "Albers"],
      ["albersUsa", "AlbersUSA"],
      ["orthographic", "Orthographic"],
      ["equirectangular", "Equirectangular"],
      ["gnomonic", "Gnomonic"],
      ["stereographic", "Stereographic"],
      ["transverseMercator", "Transverse Mercator"],
      ["conicConformal", "Conic Conformal"],
      ["azimuthalEqualArea", "Azimuthal Equal Area"],
    ],
  });
  const centerLongIn = node.numberIn({
    name: "centerLong",
    label: "Center longitude",
    value: 4.3, // Default center longitude
  });
  const centerLatIn = node.numberIn({
    name: "centerLat",
    label: "Center latitude",
    value: 51.2, // Default center latitude
  });
  const scaleIn = node.numberIn({
    name: "scale",
    label: "Scale",
    value: 1000, // Default scale
  });
  const rotateLambdaIn = node.numberIn({
    name: "rotateLambda",
    label: "Rotate lambda",
    value: 0, // Default rotation lambda
  });
  const rotatePhiIn = node.numberIn({
    name: "rotatePhi",
    label: "Rotate phi",
    value: 0, // Default rotation phi
  });
  const rotateGammaIn = node.numberIn({
    name: "rotateGamma",
    label: "Rotate gamma",
    value: 0, // Default rotation gamma
  });
  const translateXIn = node.numberIn({
    name: "translateX",
    label: "Translate X",
    value: 0, // Default is null, allowing auto-centering
  });
  const translateYIn = node.numberIn({
    name: "translateY",
    label: "Translate Y",
    value: 0, // Default is null, allowing auto-centering
  });
  const graticuleIn = node.booleanIn({ name: "graticule", label: "Show grid (Graticule)", value: true });
  node.popSection();

  // Output
  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });

  node.onRender = () => {
    // Initialize spec
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyMap);
    validateVegaSpec(specOut);

    // Remove axes
    specOut.axes = [];

    // Apply general properties
    specOut.width = widthIn.value;
    specOut.height = heightIn.value;
    specOut.autosize = autosizeIn.value;
    if (padTopIn.value || padBottomIn.value || padLeftIn.value || padRightIn.value) {
      specOut.padding = {
        top: padTopIn.value || paddingIn.value || null,
        bottom: padBottomIn.value || paddingIn.value || null,
        left: padLeftIn.value || paddingIn.value || null,
        right: padRightIn.value || paddingIn.value || null,
      };
    } else {
      specOut.padding = paddingIn.value;
    }

    // set background color
    if (bgColorIn.value) {
      setThemeProperties(specOut, "background", bgColorIn.value.toString());
    }

    // Extract map parameters
    const center = [centerLongIn.value, centerLatIn.value];
    const scale = scaleIn.value;
    const rotate = [rotateLambdaIn.value, rotatePhiIn.value, rotateGammaIn.value];
    const translate = [translateXIn.value, translateYIn.value];

    // Apply map properties using setGeoMap
    setGeoMap({
      spec: specOut,
      projectionType: targetProjectIn.value,
      center,
      scale,
      rotate,
      translate,
    });
    /*
    // Optionally add/remove graticule
    if (graticuleIn.value) {
      if (!specOut.marks.find((mark) => mark.name === "graticule")) {
        specOut.marks.push({
          name: "graticule",
          type: "shape",
          from: { data: "graticule" },
          encode: {
            update: {
              stroke: { value: "lightgray" },
              strokeWidth: { value: 1 },
            },
          },
        });
        specOut.data.push({ name: "graticule", transform: [{ type: "graticule" }] });
      }
    } else {
      specOut.marks = specOut.marks.filter((mark) => mark.name !== "graticule");
      specOut.data = specOut.data.filter((data) => data.name !== "graticule");
    }
*/
    setGraticule({ spec: specOut, removeGraticule: !graticuleIn.value });

    // Output updated spec
    plotSpecOut.set(specOut);
  };
}
