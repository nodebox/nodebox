/**
 * Add a data transform to the Vega plot specification.
 *
 * @category Plot
 */

import { setPlotDataTransform, validateVegaSpec } from "project:Utilities";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot Spec In" });

  // General parameters
  node.pushSection({ name: "General" });
  const dataNameIn = node.stringIn({ name: "dataName", label: "Data Name", value: "table" });
  const transformTypeIn = node.stringIn({
    name: "transformType",
    label: "Transform Type",
    value: "aggregate",
    choices: ["aggregate", "filter", "sort", "bin", "collect", "lookup", "project", "formula", "graticule"],
  });
  const asIn = node.stringIn({ name: "as", label: "As" });
  const indexIn = node.numberIn({ name: "index", label: "Transform Index", value: -1 });
  node.popSection();

  // Formula transform
  node.pushSection({ name: "Formula", collapsed: true });
  const formulaExprIn = node.stringIn({ name: "formulaExpr", label: "Expression", widget: "TEXT" });
  node.popSection();

  // Filter transform
  node.pushSection({ name: "Filter", collapsed: true });
  const exprIn = node.stringIn({ name: "expr", label: "Expression", widget: "TEXT" });
  node.popSection();

  // Sort transform
  node.pushSection({ name: "Sort", collapsed: true });
  const orderIn = node.stringIn({
    name: "order",
    label: "Order",
    value: "ascending",
    choices: ["ascending", "descending"],
  });
  node.popSection();

  // Aggregate transform
  node.pushSection({ name: "Aggregate", collapsed: true });
  const aggrFieldsIn = node.stringIn({ name: "aggrFields", label: "Fields" });
  const operationsIn = node.stringIn({
    name: "operations",
    label: "Operations",
    value: "sum",
    choices: ["sum", "mean", "median", "min", "max", "count", "distinct", "variance", "stdev", "q1", "q3"],
  });
  const groupByIn = node.stringIn({ name: "groupBy", label: "Group By", value: "" });
  node.popSection();

  // Collect transform
  node.pushSection({ name: "Collect", collapsed: true });
  const collectSortIn = node.stringIn({
    name: "collectSort",
    label: "Sort Order",
    value: "ascending",
    choices: ["ascending", "descending"],
  });
  node.popSection();

  // Bin transform
  node.pushSection({ name: "Bin", collapsed: true });
  const binFieldIn = node.stringIn({ name: "binField", label: "Field" });
  const maxBinsIn = node.numberIn({ name: "maxBins", label: "Max Bins", value: 10 });
  const baseIn = node.numberIn({ name: "base", label: "Base", value: 10 });
  const stepIn = node.numberIn({ name: "step", label: "Step" });
  const stepsIn = node.stringIn({ name: "steps", label: "Steps" });
  const extentIn = node.stringIn({ name: "extent", label: "Extent" });
  node.popSection();

  // Lookup transform
  node.pushSection({ name: "Lookup", collapsed: true });
  const fromIn = node.stringIn({ name: "from", label: "From Data Set (right)" });
  const keyIn = node.stringIn({ name: "key", label: "Lookup key (right)" });
  const lookupFieldsIn = node.stringIn({ name: "lookupFields", label: "In fields (left)" });
  const lookupValuesIn = node.stringIn({ name: "lookupValues", label: "Join attributes (right)" });
  node.popSection();

  // Project transform
  node.pushSection({ name: "Project", collapsed: true });
  const projectFieldsIn = node.stringIn({ name: "projectFields", label: "Fields to Project" });
  node.popSection();

  // Graticule transform
  node.pushSection({ name: "Graticule", collapsed: true });
  const graticuleStepIn = node.stringIn({ name: "graticuleStep", label: "Step" });
  const graticuleExtentIn = node.stringIn({ name: "graticuleExtent", label: "Extent" });
  const graticulePrecisionIn = node.numberIn({ name: "graticulePrecision", label: "Precision", value: 2.5 });
  node.popSection();

  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot Spec Out" });

  node.onRender = () => {
    const specOut = structuredClone(plotSpecIn.value || {});
    validateVegaSpec(specOut);

    const transformType = transformTypeIn.value;
    const dataName = dataNameIn.value;
    const fields =
      transformType === "aggregate"
        ? aggrFieldsIn.value
        : transformType === "lookup"
          ? lookupFieldsIn.value
          : transformType === "project"
            ? projectFieldsIn.value
            : undefined;
    const field = transformType === "bin" ? binFieldIn.value : undefined;
    const values = transformType === "lookup" ? lookupValuesIn.value : undefined;
    const params = {
      as: asIn.value,
      operations: operationsIn.value,
      groupby: groupByIn.value,
      expr: exprIn.value,
      order: orderIn.value,
      maxBins: maxBinsIn.value,
      base: baseIn.value,
      step: stepIn.value,
      steps: stepsIn.value,
      extent: extentIn.value,
      collectSort: collectSortIn.value,
      from: fromIn.value,
      key: keyIn.value,
      field: field,
      fields: fields,
      values: values,
      formulaExpr: formulaExprIn.value,
      graticuleStep: graticuleStepIn.value,
      graticuleExtent: graticuleExtentIn.value,
      graticulePrecision: graticulePrecisionIn.value,
    };

    setPlotDataTransform({
      spec: specOut,
      dataName,
      transformType,
      params,
      index: indexIn.value,
    });

    plotSpecOut.set(specOut);
  };
}
