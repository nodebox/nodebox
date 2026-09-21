const g = require("g.js");
const preact = require("preact");
const renderToString = require("preact-render-to-string").render;
const DOMParser = require("xmldom").DOMParser;
const ndbx = require("../src/client/ndbx");

//ndbx.origin = 'https://nodebox.live';
//ndbx.assetsOrigin = 'https://nodeboxlive.s3.amazonaws.com/';
ndbx.origin = "http://localhost:3000";
ndbx.assetsOrigin = "http://localhost:3000/api/assets/";

ndbx.globalNamespace.g = g;
ndbx.globalNamespace.preact = preact;
ndbx.globalNamespace.DOMParser = DOMParser;
ndbx.globalNamespace.ndbx = ndbx;

async function render(userId, projectId, functionId) {
  try {
    const plan = await ndbx.loadProject(userId, projectId, "dev");
    const results = await ndbx.evalFunction(plan, `${projectId}.${functionId}`);
    const div = preact.h("div", {}, results);
    const html = renderToString(div);
    console.log(html);
  } catch (e) {
    console.error(e);
  }
}

function usage() {
  console.log("Usage: node scripts/render.js <userId> <projectId> [<functionId>]");
}

if (process.argv.length < 3) {
  usage();
} else {
  const userId = process.argv[2];
  const projectId = process.argv[3];
  const functionId = process.argv.length >= 5 ? process.argv[4] : "main";
  render(userId, projectId, functionId);
}
