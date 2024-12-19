/**
 * Draw a map with countries.
 *
 * @category Geo
 */

import { Path, Paint, Group } from "@ndbx/g";
import { geoOrthographic, geoMercator, geoEquirectangular, geoPath, geoGraticule } from "https://esm.sh/d3-geo";
import * as topojson from "https://esm.sh/topojson-client@3.1.0";

function geoToShape(geo, pathGen, { fill = undefined, stroke = Paint.white(), strokeWidth = 2 } = {}) {
  const d = pathGen(geo);
  if (d === null) {
    return new Group();
  }
  const shape = Path.fromPathData(d);
  shape.fill = fill;
  shape.stroke = stroke;
  shape.strokeWidth = strokeWidth;
  return shape;
}

export default function (node) {
  const mapTypeIn = node.stringIn({ name: "Map Type", value: "land", choices: ["land", "countries"] });
  const projectionIn = node.stringIn({
    name: "Projection",
    value: "orthographic",
    choices: ["orthographic", "equirectangular", "mercator"],
  });
  const widthIn = node.numberIn({ name: "width", min: 1, value: 800 });
  const heightIn = node.numberIn({ name: "height", min: 1, value: 800 });
  const marginIn = node.numberIn({ name: "margin", min: 0, value: 20 });
  const lambdaIn = node.numberIn({ name: "lambda", min: -360, max: 360, step: 0.1, value: 0 });
  const phiIn = node.numberIn({ name: "phi", min: -360, max: 360, step: 0.1, value: -40 });
  const shapeOut = node.shapeOut({ name: "out" });
  const projectionOut = node.shapeOut({ name: "projection" });
  let countriesData, landData;
  const margin = marginIn.value;

  node.onRender = async () => {
    if (mapTypeIn.value === "land" && !landData) {
      const res = await fetch("https://data.nodebox.live/land-110m.json");
      const data = await res.json();
      landData = topojson.feature(data, data.objects.land);
    }
    if (mapTypeIn.value === "countries" && !countriesData) {
      const res = await fetch("https://data.nodebox.live/countries-110m.json");
      const data = await res.json();
      countriesData = topojson.feature(data, data.objects.countries);
    }
    const width = widthIn.value;
    const height = heightIn.value;
    const projectionType = projectionIn.value;
    let projection;
    if (projectionType === "orthographic") {
      projection = geoOrthographic()
        .scale((Math.min(width, height) - margin * 2) / 2)
        .translate([width / 2, height / 2])
        .rotate([lambdaIn.value, phiIn.value])
        .clipAngle(90)
        .precision(0.1);
    } else if (projectionType === "equirectangular") {
      projection = geoEquirectangular()
        // .rotate([lambdaIn.value, phiIn.value])
        // .center([centerLngIn.value, centerLatIn.value])
        .fitExtent(
          [
            [margin, margin],
            [width - margin * 2, height - margin * 2],
          ],
          landData || countriesData,
        )
        // .scale((Math.min(width, height) - margin * 2) / 2)
        // .translate([width / 2, height / 2])
        .precision(0.1);
    } else if (projectionType === "mercator") {
      projection = geoMercator()
        // .scale((Math.min(width, height) - margin * 2) / 2)
        // .center([centerLngIn.value, centerLatIn.value])
        .fitExtent(
          [
            [margin, margin],
            [width - margin * 2, height - margin * 2],
          ],
          landData || countriesData,
        )
        .precision(0.1);
    }
    const pathGen = geoPath(projection);

    const g = new Group();
    const spherePath = geoToShape({ type: "Sphere" }, pathGen, { stroke: Paint.solid(0.6, 0.6, 0.6), strokeWidth: 4 });
    g.add(spherePath);
    const graticulePath = geoToShape(geoGraticule()(), pathGen, {
      stroke: Paint.solid(0.3, 0.3, 0.3),
      strokeWidth: 0.5,
    });
    g.add(graticulePath);
    if (mapTypeIn.value === "land") {
      const landPath = geoToShape(landData, pathGen, { fill: Paint.solid(0.8, 0.8, 0.8), stroke: null });
      g.add(landPath);
    }
    if (mapTypeIn.value === "countries") {
      const countriesPaths = countriesData.features.map((f) => geoToShape(f, pathGen, { strokeWidth: 0.3 }));
      g.children.push(...countriesPaths);
    }
    shapeOut.set(g);
    projectionOut.set(projection);
  };
}
