/**
 * Filter data based on a condition.
 *
 * @category Data
 */

import { detectDataFormat, getNestedProperty, extractCoreData, replaceCoreData } from "project:Utilities";

export default function (node) {
  const dataIn = node.tableIn({ name: "dataIn", label: "Data" });
  node.pushSection({ name: "General" });
  const leftIn = node.stringIn({ name: "leftValue", label: "Left value" });
  const operatorIn = node.stringIn({
    name: "operator",
    label: "Operator",
    value: "equal",
    choices: [
      "equal",
      "not equal",
      "greater than",
      "smaller than",
      "greater than or equal",
      "smaller than or equal",
      "between",
      "between or equal",
      "in",
      "contains any",
      "contains all",
      "begins with",
      "ends with",
      "like (regex)",
    ],
  });
  const rightIn = node.stringIn({ name: "rightValue", label: "Right value" });
  const notIn = node.booleanIn({ name: "not", label: "Negate", value: false });
  node.popSection();
  node.pushSection({ name: "Data format", collapsed: true });
  const formatIn = node.stringIn({
    name: "format",
    label: "Format",
    value: "json",
    choices: [
      ["json", "<default>"],
      ["geojson", "GeoJSON"],
      ["topojson", "TopoJSON"],
    ],
  });
  const featureIn = node.stringIn({
    name: "feature",
    label: "Feature",
  });
  const propertiesIn = node.booleanIn({
    name: "properties",
    label: "Use properties",
    value: true,
  });
  node.popSection();

  const dataOut = node.tableOut({ name: "dataOut", label: "Data" });

  const [EQUAL, NOTEQUAL, GREATER, SMALLER, GREATEREQUAL, SMALLEREQUAL] = [
    "equal",
    "not equal",
    "greater than",
    "smaller than",
    "greater than or equal",
    "smaller than or equal",
  ];
  const [BETWEEN, BETWEENEQUAL, IN, CONTAINS, CONTAINSANY, CONTAINSALL] = [
    "between",
    "between or equal",
    "in",
    "contains",
    "contains any",
    "contains all",
  ];
  const [BEGINS, ENDS, LIKE] = ["begins with", "ends with", "like (regex)"];

  function param2numb(val) {
    const result = Number(val) || val;
    return result;
  }

  function param2array(val) {
    let result;
    if (typeof val === "string" && val.includes(",")) {
      const regex = new RegExp('^".*"$');
      result = val.split(",").map((d) => (!isNaN(Number(d)) ? Number(d) : d.match(regex) ? JSON.parse(d) : d)); // Convert comma-separated value to array
    } else {
      result = [val];
    }
    return result;
  }

  node.onRender = () => {
    let data = structuredClone(dataIn.value ? dataIn.value : []);
    const feature = featureIn.value === "" ? undefined : featureIn.value;
    const format = formatIn.value === "" ? undefined : formatIn.value;
    const filterData = extractCoreData(data, format, feature, propertiesIn.value);
    const [fleft, fright] = [leftIn.fn, rightIn.fn];
    let filteredData = new Array();
    let cond;
    let regex;
    filterData.forEach((d) => {
      switch (operatorIn.value) {
        case EQUAL:
          cond = param2numb(fleft(d)) === param2numb(fright(d));
          break;
        case NOTEQUAL:
          cond = param2numb(fleft(d)) !== param2numb(fright(d));
          break;
        case GREATER:
          cond = param2numb(fleft(d)) > param2numb(fright(d));
          break;
        case SMALLER:
          cond = param2numb(fleft(d)) < param2numb(fright(d));
          break;
        case GREATEREQUAL:
          cond = param2numb(fleft(d)) >= param2numb(fright(d));
          break;
        case SMALLEREQUAL:
          cond = param2numb(fleft(d)) <= param2numb(fright(d));
          break;
        case BETWEEN:
          arr = param2array(param2numb(fright(d)));
          cond = arr[0] < fleft(d) < arr[1];
          break;
        case BETWEENEQUAL:
          arr = param2array(fright(d));
          cond = arr[0] <= fleft(d) <= arr[1];
          break;
        case IN:
          cond = param2array(fright(d)).includes(fleft(d));
          break;
        case CONTAINS:
          cond = param2array(fleft(d)).includes(fright(d));
          break;
        case CONTAINSANY:
          cond = param2array(fleft(d)).some((v) => param2array(fright(d)).includes(v));
          break;
        case CONTAINSALL:
          cond = param2array(fleft(d)).every((v) => param2array(fright(d)).includes(v));
          break;
        case BEGINS:
          regex = new RegExp("^" + fright(d));
          cond = fleft(d).match(regex);
          break;
        case ENDS:
          regex = new RegExp(fright(d) + "$");
          cond = fleft(d).match(regex);
          break;
        case LIKE:
          regex = new RegExp(fright(d));
          cond = fleft(d).match(regex);
          break;
      }

      if (notIn.value) cond = !cond;
      if (cond) filteredData.push(d);
    });

    const newData = replaceCoreData(data, format, feature, filteredData, propertiesIn.value);
    dataOut.set(newData);
  };
}
