/**
 * Change data based on a condition.
 *
 * @category Data
 */

export default function (node) {
  const dataIn = node.tableIn({ name: "dataIn", label: "Data" });
  const changeIn = node.tableIn({ name: "change data" });

  node.pushSection({ name: "General" });
  const changeAttrIn = node.stringIn({ name: "changeAttr", label: "Change attribute name" });
  const changeValueIn = node.stringIn({ name: "changeValue", label: "Change value" });
  node.popSection();
  node.pushSection({ name: "Condition" });
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

  // Function to change data
  function changeData(data, changeData, fleft, fright, fattr, fval) {
    let newData = new Array();
    let cond, arr, regex;
    data.forEach((d, i) => {
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

      // If the condition is met, update or create the attribute
      if (cond) {
        const attrName = fattr(d);
        const j = i < changeData.length ? i : 0;
        if (!(attrName in d)) {
          d[attrName] = undefined; // Initialize the new attribute if it doesn't exist
        }
        d[attrName] = fval(changeData[j]);
      }

      newData.push(d);
    });
    return newData;
  }

  node.onRender = () => {
    let data = structuredClone(dataIn.value ? dataIn.value : []);
    const changeDataInput = structuredClone(changeIn.value ? changeIn.value : dataIn.value);
    const [fleft, fright, fattr, fval] = [leftIn.fn, rightIn.fn, changeAttrIn.fn, changeValueIn.fn];

    const newData = changeData(data, changeDataInput, fleft, fright, fattr, fval);

    dataOut.set(newData);
  };
}
