import { FunctionRepository } from "../runtime/function-repository";
import { colorLibrary } from "./color";
import { coreLibrary } from "./core";
import { corevectorLibrary, pyvectorLibrary } from "./corevector";
import { dataLibrary } from "./data";
import { deviceLibrary } from "./device";
import { listLibrary } from "./list";
import { mathLibrary } from "./math";
import { networkLibrary } from "./network";
import { stringLibrary } from "./string";

export * from "./java-random";
export * as math from "./math";
export * as list from "./list";
export * as string from "./string";
export * as color from "./color";
export * as data from "./data";
export * as corevector from "./corevector";
export * as network from "./network";
export * as device from "./device";
export * as core from "./core";
export { setTextFileReader, getTextFileReader, parseCsvTable, parseCsv } from "./data";
export { setOscSender } from "./device";
export { setDomParser, getDomParser } from "./corevector";
export {
  coreLibrary,
  mathLibrary,
  listLibrary,
  stringLibrary,
  colorLibrary,
  dataLibrary,
  corevectorLibrary,
  pyvectorLibrary,
  networkLibrary,
  deviceLibrary,
};

/** The function repository with every built-in library. */
export function builtinFunctionRepository(): FunctionRepository {
  return FunctionRepository.of(
    coreLibrary,
    mathLibrary,
    listLibrary,
    stringLibrary,
    colorLibrary,
    dataLibrary,
    corevectorLibrary,
    pyvectorLibrary,
    networkLibrary,
    deviceLibrary,
  );
}
