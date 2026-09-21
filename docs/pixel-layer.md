# The pixel layer

NodeBox drew vectors and stopped there: a graph ended in geometry, and the viewer turned that into
SVG. The `image` library adds the layer underneath a printed picture. A raster is a value like a
shape is a value; it flows between nodes, and `image.print` turns three coverages into a sheet of
paper with three inks on it.

The design follows [riso-drawing](https://github.com/fdb/riso-drawing), which is where the look
comes from and which is worth reading first. Its rule is kept whole: **one kernel per node, and a
mode that picks a different algorithm is a different node**. `add`, `multiply`, `maximum` and
`minimum` are four nodes, not one with an operator menu. Anything above a kernel is a graph, not
code, which is why `image.halftone` is 23 image nodes that you can open and change.

## Values

A **raster** is a rectangle of 32-bit floats with one or three channels. One channel is coverage or
tone, where 0 is nothing and 1 is full; three are linear RGB. Kernels mix them: a one-channel
raster broadcasts to all three, so a tone can drive a colour with no conversion node in between.
Values are not clamped in transit, because a chain of adds and multiplies is easier to follow when
the clamp happens where it is meant to.

A raster port is a value-range port, so a node that receives a list of rasters runs once per
raster, as any other node would. The two rasterizers are the exception: `image.fill` and
`image.stroke` take the whole list of shapes and expose one plate in one pass.

## Nodes

| | |
| --- | --- |
| Sources | `fill`, `stroke`, `constant`, `screen`, `noise`, `grain`, `paper` |
| Operators | `add`, `multiply`, `maximum`, `minimum`, `compare`, `remap`, `shift`, `ink`, `expression` |
| Graphs | `halftone` (one ink printed), `print` (paper and three of them) |

`compare` is the one worth knowing: a soft step of a against b. Compared against `screen`, a tone
becomes halftone dots whose area follows it; compared against a `constant`, it is a gate.

`expression` is the escape hatch, one per value type, as in riso: `a` and `b` are the inputs, `x`
and `y` run from 0 to 1, and `Math` is in scope.

## The print, as data

`image.halftone` is the chain riso calls `risoInk`, node for node:

```
coverage → shift (registration) → multiply (slow mottle) → add (edge grain)
         → compare (screen) → maximum (solid where the ink nearly covers)
         → multiply (gate, density, mottle, press grain) → multiply (speckle) → ink
```

The amounts that are not published ports are literals on the nodes inside; open the network to
reach them. `image.print` is `paper` multiplied with one `halftone` per ink, at 15, 45 and 0
degrees, each with its own registration slip.

## Where it runs

Kernels run on WebGPU when there is a device and on the CPU when there is not. The rasterizer and
the paper fibres always run on the CPU and upload their result, because they are geometry, not
pixels. A raster made on the device stays there until something asks for pixels; the editor reads
it back to show it.

Both paths are meant to give the same picture, and that is checked rather than asserted:

```bash
cd packages/core
node --import tsx scripts/gpu-parity.mts 256          # the whole print, both ways
KERNELS=1 node --import tsx scripts/gpu-parity.mts 64 # each kernel on its own
```

The script bundles the core, serves it over `127.0.0.1` (WebGPU needs a secure context) and drives
Chromium. On the SwiftShader device in CI the worst channel of a 256 by 256 print differs by
1.2e-4 and no channel differs by half a printed step. The CPU kernels compute in doubles and store
floats while the GPU computes in floats throughout, so the last bits never match; what matters is
that nothing survives to the print. SwiftShader is a software device, so those runs say nothing
about how fast this is on real hardware.

The generator and the noise grid are riso's, bit for bit (`mulberry32`, a 32 by 32 value grid), so
the same seed gives the same grain there and here, and the WGSL twin of `grain` can use the closed
form: the state of mulberry32 after k calls is `seed + k*0x6D2B79F5`, so the k-th value is a pure
function of k.

## A document to start from

`packages/core/examples/riso-print.ndbx` is a document that ends in `image.print`: a grid of rings
on the blue plate, a spiral of dots on the pink one, a disc on the yellow one. It lives in the
package rather than in `examples/`, because the Java application cannot open a document that uses
these nodes.

Open it in the editor the way any other `.ndbx` document is opened, or render it headlessly
through `NodeContext`. `test/image.test.ts` renders it and checks that the paper shows through and
that the three inks land where they should.

## What is not there yet

- **Static rasters are recomputed every frame.** riso measures dynamism (reads of `t`, `u`, `f`
  and `iris` are recorded through getters while a node's parameters evaluate) and keeps everything
  static across frames, which is most of its speed. The core evaluator has no such notion.
- **Rasterization is on the CPU.** riso paints marks into its stencils with render passes, 16
  samples per pixel, and keeps a snapshot of the static prefix. Here a shape is scanline-filled on
  the CPU and uploaded.
- **Paper fibres cost 300 ms at 700 px** and are drawn on every render for the same reason.
- **Mixed sizes fall back to the CPU**, because the scaling rule lives in one place.
