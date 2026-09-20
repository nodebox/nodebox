/**
 * Sample data using various sampling strategies.
 *
 * This node provides multiple methods for sampling data:
 * - Size-based: Random count, percentage, first/last n rows, every n rows
 * - Statistical: Stratified, Quartil
 *
 * @category Data Transformation
 */

import { quantile } from "https://esm.sh/d3-array@3.2.4";

export default function (node) {
  const dataIn = node.tableIn({ name: "data", label: "Input Data" });

  // Sampling Strategy Selection
  node.pushSection({ name: "Sampling Strategy" });
  const strategyIn = node.stringIn({
    name: "strategy",
    label: "Strategy",
    value: "random_count",
    choices: [
      ["random_count", "Random (Count)"],
      ["random_percent", "Random (Percentage)"],
      ["first_n", "First N Rows"],
      ["last_n", "Last N Rows"],
      ["systematic", "Every Nth Row"],
      ["stratified", "Stratified"],
      ["quartile", "Quartile-based"],
    ],
  });
  node.popSection();

  // Size-based Parameters
  node.pushSection({ name: "Size Parameters" });
  const countIn = node.numberIn({
    name: "count",
    label: "Count",
    value: 100,
  });
  const percentageIn = node.numberIn({
    name: "percentage",
    label: "Percentage",
    value: 10,
    min: 0,
    max: 100,
  });
  const stepIn = node.numberIn({
    name: "step",
    label: "Step Size",
    value: 1,
  });
  node.popSection();

  // Statistical Parameters
  node.pushSection({ name: "Statistical Parameters" });
  const groupByIn = node.stringIn({
    name: "groupBy",
    label: "Group Attribute",
  });
  const quartileIn = node.stringIn({
    name: "quartile",
    label: "Quartile",
    value: "1",
    choices: [
      ["0", "Q1 (0-25%)"],
      ["1", "Q2 (25-50%)"],
      ["2", "Q3 (50-75%)"],
      ["3", "Q4 (75-100%)"],
    ],
  });
  node.popSection();

  // Seed for reproducibility
  node.pushSection({ name: "Random" });
  const seedIn = node.numberIn({
    name: "seed",
    label: "Random Seed",
    value: 1234,
  });
  node.popSection();

  const dataOut = node.tableOut({ name: "output", label: "Sampled Data" });

  // Random number generator with seed
  function seededRandom(seed) {
    return function () {
      seed = (seed * 16807) % 2147483647;
      return (seed - 1) / 2147483646;
    };
  }

  // Sampling functions
  function randomSample(data, count, random) {
    if (count >= data.length) return data;
    const sampled = new Set();
    while (sampled.size < count) {
      sampled.add(Math.floor(random() * data.length));
    }
    return Array.from(sampled).map((i) => data[i]);
  }

  function stratifiedSample(data, count, stratifyBy, random) {
    // Group data by stratification column
    const groups = new Map();
    for (const row of data) {
      const key = row[stratifyBy];
      if (!groups.has(key)) groups.set(key, []);
      groups.get(key).push(row);
    }

    // Calculate proportional sample sizes for each group
    const total = data.length;
    const result = [];
    for (const [_, group] of groups) {
      const groupCount = Math.round((group.length / total) * count);
      if (groupCount > 0) {
        result.push(...randomSample(group, groupCount, random));
      }
    }

    return result;
  }

  function quartileSample(data, quartile, column) {
    const values = data.map((row) => row[column]);
    const q = quartile * 0.25;
    const qValue = quantile(values, q);
    const nextQValue = quantile(values, q + 0.25);
    return data.filter((row) => row[column] >= qValue && row[column] < nextQValue);
  }

  node.onRender = () => {
    const data = dataIn.value;
    if (!data || data.length === 0) {
      dataOut.set([]);
      return;
    }

    const random = seededRandom(seedIn.value);
    let result = [];

    switch (strategyIn.value) {
      case "random_count":
        result = randomSample(data, countIn.value, random);
        break;

      case "random_percent":
        const count = Math.round((data.length * percentageIn.value) / 100);
        result = randomSample(data, count, random);
        break;

      case "first_n":
        result = data.slice(0, countIn.value);
        break;

      case "last_n":
        result = data.slice(-countIn.value);
        break;

      case "systematic":
        result = data.filter((_, i) => i % stepIn.value === 0);
        break;

      case "stratified":
        if (!groupByIn.value) {
          result = data;
          break;
        }
        result = stratifiedSample(data, countIn.value, groupByIn.value, random);
        break;

      case "quartile":
        if (!groupByIn.value) {
          result = data;
          break;
        }
        result = quartileSample(data, parseInt(quartileIn.value), groupByIn.value);
        break;

      default:
        result = data;
    }

    dataOut.set(result);
  };
}
