/**
 * Generate random values based on a specified distribution type.
 *
 * This node generates random values based on the selected distribution type (uniform or normal).
 * The node takes parameters like count, type, min, max, mu, sigma, and seed.
 * It outputs an array of generated random values.
 *
 * @category Data Generation
 */

import { randomUniform, randomNormal, randomLcg } from "https://esm.sh/d3-random@3.0.1";

export default function (node) {
  const amountIn = node.numberIn({ name: "amount", value: 10 });
  const typeIn = node.stringIn({ name: "type", value: "uniform", choices: ["uniform", "normal"] });
  const minIn = node.numberIn({ name: "min", value: 0 });
  const maxIn = node.numberIn({ name: "max", value: 1 });
  const muIn = node.numberIn({ name: "mu", value: 0 });
  const sigmaIn = node.numberIn({ name: "sigma", value: 1 });
  const seedIn = node.numberIn({ name: "seed", value: 1234 });
  const attributeIn = node.stringIn({ name: "attribute", value: "value" });
  const valuesOut = node.tableOut({ name: "out" });

  node.onRender = () => {
    const attr = attributeIn.value;
    const randomSource = randomLcg(seedIn.value);
    let randomFunc;
    if (typeIn.value === "uniform") {
      randomFunc = randomUniform.source(randomSource)(minIn.value, maxIn.value);
    } else if (typeIn.value === "normal") {
      randomFunc = randomNormal.source(randomSource)(muIn.value, sigmaIn.value);
    } else {
      throw new Error(`Unsupported distribution type: ${typeIn.value}`);
    }

    const values = Array.from({ length: amountIn.value }, () => ({ [attr]: randomFunc() }));
    valuesOut.set(values);
  };
}
