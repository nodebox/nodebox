// A port of nodebox.function.DeviceFunctions (namespace "device"). Devices deliver their data
// through the render data: "mouse.position", "<device>.messages" for OSC, "<device>.audio" for audio.
// Hosts without those devices get empty results rather than errors, so documents still render.

import { Point } from "../graphics/point";
import type { NodeContext } from "../runtime/context";
import { JavaScriptLibrary } from "../runtime/function-repository";
import { toArray } from "../runtime/values";

export function mousePosition(context: NodeContext): Point {
  const p = context.data["mouse.position"];
  return p instanceof Point ? p : Point.ZERO;
}

export interface OscMessage {
  address: string;
  arguments: unknown[];
}

export function receiveOSC(deviceName: string, oscAddressPrefix: string, argumentNames: string, context: NodeContext): Record<string, unknown>[] {
  const messages = toArray(context.data[`${deviceName}.messages`]) as OscMessage[];
  const names = String(argumentNames ?? "")
    .split(/[,;]/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
  const result: Record<string, unknown>[] = [];
  for (const message of messages) {
    if (!message || typeof message.address !== "string") continue;
    if (!message.address.startsWith(oscAddressPrefix ?? "")) continue;
    const row: Record<string, unknown> = { address: message.address };
    const args = toArray(message.arguments);
    args.forEach((arg, i) => {
      row[i < names.length ? names[i] : `Column ${i + 1}`] = arg;
    });
    result.push(row);
  }
  return result;
}

export type OscSender = (ipAddress: string, port: number, oscAddress: string, args: number[]) => void;

let oscSender: OscSender | null = null;

/** Install how send_osc delivers messages (the desktop app provides a UDP sender). */
export function setOscSender(sender: OscSender | null): void {
  oscSender = sender;
}

export function sendOSC(ipAddress: string, port: number, oscAddress: string, oscArguments: unknown): void {
  if (!oscSender) return;
  oscSender(ipAddress, Math.trunc(port), oscAddress, toArray(oscArguments).map(Number));
}

function audioData(context: NodeContext, deviceName: string): Record<string, unknown> {
  const d = context.data[`${deviceName}.audio`];
  return d && typeof d === "object" ? (d as Record<string, unknown>) : {};
}

export function audioAnalysis(deviceName: string, channel: string, averages: number, context: NodeContext): number[] {
  const data = audioData(context, deviceName);
  const spectrum = toArray(data[`spectrum.${channel}`] ?? data.spectrum).map(Number);
  averages = Math.max(1, Math.trunc(averages));
  if (spectrum.length === 0) return new Array(averages).fill(0);
  const result: number[] = [];
  const bucket = spectrum.length / averages;
  for (let i = 0; i < averages; i++) {
    const slice = spectrum.slice(Math.floor(i * bucket), Math.max(Math.floor(i * bucket) + 1, Math.floor((i + 1) * bucket)));
    result.push(slice.reduce((a, b) => a + b, 0) / slice.length);
  }
  return result;
}

export function audioLogAvg(deviceName: string, channel: string, baseFreq: number, bandsPerOctave: number, context: NodeContext): number[] {
  const data = audioData(context, deviceName);
  const bands = toArray(data[`logAverages.${channel}`] ?? data.logAverages).map(Number);
  if (bands.length > 0) return bands;
  const octaves = Math.max(1, Math.ceil(Math.log2(22050 / Math.max(1, baseFreq))));
  return new Array(octaves * Math.max(1, Math.trunc(bandsPerOctave))).fill(0);
}

export function audioWave(deviceName: string, context: NodeContext): Record<string, number>[] {
  const data = audioData(context, deviceName);
  const left = toArray(data["wave.left"] ?? data.wave).map(Number);
  const right = toArray(data["wave.right"] ?? data.wave).map(Number);
  const n = Math.max(left.length, right.length);
  const result: Record<string, number>[] = [];
  for (let i = 0; i < n; i++) result.push({ left: left[i] ?? 0, right: right[i] ?? 0 });
  return result;
}

export function beatDetect(deviceName: string, context: NodeContext): Record<string, boolean> {
  const data = audioData(context, deviceName);
  return { kick: Boolean(data.kick), snare: Boolean(data.snare), hat: Boolean(data.hat) };
}

export const deviceLibrary = new JavaScriptLibrary(
  "device",
  { mousePosition, receiveOSC, sendOSC, audioAnalysis, audioLogAvg, audioWave, beatDetect },
  { impure: ["mousePosition", "receiveOSC", "sendOSC", "audioAnalysis", "audioLogAvg", "audioWave", "beatDetect"] },
);
