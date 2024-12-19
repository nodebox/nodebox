/**
 * Add an attribute to the data.
 *
 * @category Data
 */

export default function (node) {
  const dataIn = node.tableIn({ name: "data", label: "Data" });
  const attrIn = node.stringIn({ name: "attributeName", label: "Attribute name" });
  const valuePortIn = node.tableIn({ name: "new data", label: "New data" });
  const valueIn = node.stringIn({ name: "value", label: "Value" });
  const dataOut = node.tableOut({ name: "dataOut", label: "Data" });

  function param2numb(val) {
    let result = Number(val) || val;
    const objRegex = new RegExp("^\\{.*\\}$");
    const arrRegex = new RegExp("^\\[.*\\]$");
    //const paramType = (nodeParamIn.node.values[nodeParamIn.name])?nodeParamIn.node.values[nodeParamIn.name].type : undefined
    if (val === undefined) {
      // undefined
      result = undefined;
    } else if (typeof val === "string") {
      const regex = new RegExp('^".*"$');
      result = val.split(",").map((d) => (!isNaN(Number(d)) ? Number(d) : d.match(regex) ? JSON.parse(d) : d)); // Convert comma-separated value to array
      if (!Array.isArray(result) || result.length === 1) result = result[0];
    }
    return result;
  }

  node.onRender = () => {
    let data = structuredClone(dataIn.value ? dataIn.value : []);
    let changeData = structuredClone(valuePortIn.value ? valuePortIn.value : dataIn.value);
    const [fattr, fval] = [attrIn.fn, valueIn.fn];
    let newData = new Array();
    data.forEach((d, i) => {
      const j = i < changeData.length ? i : 0;
      const newValue = param2numb(fval(changeData[j]));
      d[fattr(d)] = newValue;
      newData.push(d);
    });

    dataOut.set(newData);
  };
}
