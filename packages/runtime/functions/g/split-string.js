/**
 * Create a string value.
 *
 * @category String
 */

export default function (node) {
  const dataIn = node.tableIn({ name: "data", label: "Data" });

  const stringIn = node.stringIn({ name: "string", label: "String" });
  const sepIn = node.stringIn({ name: "separator", label: "Separator" });
  const maxSplitIn = node.numberIn({ name: "maxSplit", label: "Max parts", value: -1 });
  const targetIn = node.stringIn({
    name: "target",
    label: "Target",
    value: "Attributes",
    choices: ["Attributes", "Records"],
  });

  const dataOut = node.tableOut({ name: "data0ut", label: "Data" });

  node.onRender = () => {
    // Set properties data

    let string, strings;
    let data = dataIn.value ? structuredClone(dataIn.value) : [{ value: null }];
    let newData = [];
    const fstr = stringIn.fn;
    console.log(data);
    console.log(stringIn.value);
    data.forEach((d) => {
      string = fstr(d);
      strings = string.split(sepIn.value, maxSplitIn.value);
      if (targetIn.value === "Records") {
        strings.forEach((s) => {
          const newRec = structuredClone(d);
          newRec["_part"] = s;
          newData.push(newRec);
        });
      } else {
        strings.forEach((s, i) => {
          d["_part" + i] = s;
        });
        newData.push(d);
      }
    });

    dataOut.set(newData);
  };
}
