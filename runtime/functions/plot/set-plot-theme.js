/**
 * Set a theme to change the style of the plot.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec } from "project:Utilities";

const theme = (themeName) => {
  const GGPLOT2 = "ggplot2";
  const DARK = "dark";
  const QUARTZ = "quartz";
  const VOX = "vox";
  const FIVE38 = "fiveThirtyEight";
  let config, markColor, axisColor;

  switch (themeName) {
    case GGPLOT2:
      markColor = "#000";
      config = {
        group: {
          fill: "#e5e5e5",
        },

        arc: { fill: markColor },
        area: { fill: markColor },
        line: { stroke: markColor },
        path: { stroke: markColor },
        rect: { fill: markColor },
        shape: { stroke: markColor },
        symbol: { fill: markColor, size: 40 },

        axis: {
          domain: false,
          grid: true,
          gridColor: "#FFFFFF",
          gridOpacity: 1,
          labelColor: "#7F7F7F",
          labelPadding: 4,
          tickColor: "#7F7F7F",
          tickSize: 5.67,
          titleFontSize: 16,
          titleFontWeight: "normal",
        },

        legend: {
          labelBaseline: "middle",
          labelFontSize: 11,
          symbolSize: 40,
        },

        range: {
          category: [
            "#000000",
            "#7F7F7F",
            "#1A1A1A",
            "#999999",
            "#333333",
            "#B0B0B0",
            "#4D4D4D",
            "#C9C9C9",
            "#666666",
            "#DCDCDC",
          ],
        },
      };
      break;

    case DARK:
      const lightColor = "#fff";
      const medColor = "#888";
      config = {
        background: "#333",

        view: {
          stroke: medColor,
        },

        title: {
          color: lightColor,
          subtitleColor: lightColor,
        },

        style: {
          "guide-label": {
            fill: lightColor,
          },
          "guide-title": {
            fill: lightColor,
          },
        },

        axis: {
          domainColor: lightColor,
          gridColor: medColor,
          tickColor: lightColor,
        },
      };
      break;

    case QUARTZ:
      markColor = "#ab5787";
      axisColor = "#979797";
      config = {
        background: "#f9f9f9",

        arc: { fill: markColor },
        area: { fill: markColor },
        line: { stroke: markColor },
        path: { stroke: markColor },
        rect: { fill: markColor },
        shape: { stroke: markColor },
        symbol: { fill: markColor, size: 30 },

        axis: {
          domainColor: axisColor,
          domainWidth: 0.5,
          gridWidth: 0.2,
          labelColor: axisColor,
          tickColor: axisColor,
          tickWidth: 0.2,
          titleColor: axisColor,
        },

        axisBand: {
          grid: false,
        },

        axisX: {
          grid: true,
          tickSize: 10,
        },

        axisY: {
          domain: false,
          grid: true,
          tickSize: 0,
        },

        legend: {
          labelFontSize: 11,
          padding: 1,
          symbolSize: 30,
          symbolType: "square",
        },

        range: {
          category: [
            "#ab5787",
            "#51b2e5",
            "#703c5c",
            "#168dd9",
            "#d190b6",
            "#00609f",
            "#d365ba",
            "#154866",
            "#666666",
            "#c4c4c4",
          ],
        },
      };
      break;

    case VOX:
      markColor = "#3e5c69";
      config = {
        background: "#fff",

        arc: { fill: markColor },
        area: { fill: markColor },
        line: { stroke: markColor },
        path: { stroke: markColor },
        rect: { fill: markColor },
        shape: { stroke: markColor },
        symbol: { fill: markColor },

        axis: {
          domainWidth: 0.5,
          grid: true,
          labelPadding: 2,
          tickSize: 5,
          tickWidth: 0.5,
          titleFontWeight: "normal",
        },

        axisBand: {
          grid: false,
        },

        axisX: {
          gridWidth: 0.2,
        },

        axisY: {
          gridDash: [3],
          gridWidth: 0.4,
        },

        legend: {
          labelFontSize: 11,
          padding: 1,
          symbolType: "square",
        },

        range: {
          category: ["#3e5c69", "#6793a6", "#182429", "#0570b0", "#3690c0", "#74a9cf", "#a6bddb", "#e2ddf2"],
        },
      };
      break;

    case FIVE38:
      markColor = "#30a2da";
      axisColor = "#cbcbcb";
      const guideLabelColor = "#999";
      const guideTitleColor = "#333";
      const backgroundColor = "#f0f0f0";
      const blackTitle = "#333";
      config = {
        arc: { fill: markColor },
        area: { fill: markColor },

        axis: {
          domainColor: axisColor,
          grid: true,
          gridColor: axisColor,
          gridWidth: 1,
          labelColor: guideLabelColor,
          labelFontSize: 10,
          titleColor: guideTitleColor,
          tickColor: axisColor,
          tickSize: 10,
          titleFontSize: 14,
          titlePadding: 10,
          labelPadding: 4,
        },

        axisBand: {
          grid: false,
        },

        background: backgroundColor,

        group: {
          fill: backgroundColor,
        },

        legend: {
          labelColor: blackTitle,
          labelFontSize: 11,
          padding: 1,
          symbolSize: 30,
          symbolType: "square",
          titleColor: blackTitle,
          titleFontSize: 14,
          titlePadding: 10,
        },

        line: {
          stroke: markColor,
          strokeWidth: 2,
        },

        path: { stroke: markColor, strokeWidth: 0.5 },
        rect: { fill: markColor },

        range: {
          category: [
            "#30a2da",
            "#fc4f30",
            "#e5ae38",
            "#6d904f",
            "#8b8b8b",
            "#b96db8",
            "#ff9e27",
            "#56cc60",
            "#52d2ca",
            "#52689e",
            "#545454",
            "#9fe4f8",
          ],

          diverging: ["#cc0020", "#e77866", "#f6e7e1", "#d6e8ed", "#91bfd9", "#1d78b5"],
          heatmap: ["#d6e8ed", "#cee0e5", "#91bfd9", "#549cc6", "#1d78b5"],
        },

        point: {
          filled: true,
          shape: "circle",
        },

        shape: { stroke: markColor },

        bar: {
          binSpacing: 2,
          fill: markColor,
          stroke: null,
        },

        title: {
          anchor: "start",
          fontSize: 24,
          fontWeight: 600,
          offset: 20,
        },
      };
      break;
    default:
      config = {
        background: "#fff",
      };
      break;
  }
  return config;
};

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });
  const themeIn = node.stringIn({
    name: "theme",
    label: "Theme",
    value: "dark",
    choices: ["<default>", "ggplot2", "dark", "quartz", "vox", "fiveThirtyEight"],
  });
  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });

  node.onRender = () => {
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);
    if (themeIn.value != "<default>") specOut.config = theme(themeIn.value);

    plotSpecOut.set(specOut);
  };
}
