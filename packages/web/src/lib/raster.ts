// Rasters in the viewer.
//
// A raster is a rectangle of 32-bit floats, which an SVG cannot show; it becomes a data URL
// through a canvas. One channel draws as black on white, because that is how a coverage or a tone
// reads; three channels draw as they are. The URL is cached on the raster, so panning and zooming
// do not re-encode it.

import { Raster } from "@ndbx/core";

const CACHE = new WeakMap<Raster, string>();

export function isRaster(value: unknown): value is Raster {
  return value instanceof Raster;
}

/** The raster as a PNG data URL, or an empty string where there is no canvas to draw on. */
export function rasterToDataUrl(raster: Raster): string {
  const cached = CACHE.get(raster);
  if (cached !== undefined) return cached;
  let url = "";
  try {
    const canvas = document.createElement("canvas");
    canvas.width = raster.width;
    canvas.height = raster.height;
    const context = canvas.getContext("2d");
    if (context) {
      const pixels = context.createImageData(raster.width, raster.height);
      for (let p = 0, i = 0; p < raster.pixelCount; p += 1, i += 4) {
        if (raster.channels === 1) {
          // A tone of 1 is full ink, so it draws black.
          const v = Math.round(255 * (1 - clamp(raster.data[p])));
          pixels.data[i] = v;
          pixels.data[i + 1] = v;
          pixels.data[i + 2] = v;
        } else {
          pixels.data[i] = Math.round(255 * clamp(raster.data[p * 3]));
          pixels.data[i + 1] = Math.round(255 * clamp(raster.data[p * 3 + 1]));
          pixels.data[i + 2] = Math.round(255 * clamp(raster.data[p * 3 + 2]));
        }
        pixels.data[i + 3] = 255;
      }
      context.putImageData(pixels, 0, 0);
      url = canvas.toDataURL("image/png");
    }
  } catch (e) {
    console.error("Could not draw the raster.", e);
  }
  CACHE.set(raster, url);
  return url;
}

const clamp = (v: number) => (v < 0 ? 0 : v > 1 ? 1 : v);
