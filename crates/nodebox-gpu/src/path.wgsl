// NodeBox outlines drawn straight from their curves with Eric Lengyel's Slug algorithm, as in
// bybike's text.wgsl: an outline is quadratic curves in horizontal and vertical bands; a pixel
// casts a ray right through its horizontal band and one up through its vertical band, and the
// crossings, each weighted by how much of the pixel lies past it, give the coverage (nonzero).

struct Entry {
    bbox: vec4f,
    // offset of the band table, horizontal bands, vertical bands
    bands: vec4u,
};

struct View {
    viewport: vec2f,
    scale: f32,
    pad: f32,
    t: vec2f,
    pad2: vec2f,
};

@group(0) @binding(0) var<uniform> view: View;
@group(0) @binding(1) var<storage, read> curves: array<vec4f>;
@group(0) @binding(2) var<storage, read> entries: array<Entry>;
@group(0) @binding(3) var<storage, read> bands: array<u32>;

struct In {
    @location(0) entry: u32,
    // the placement: x' = a x + c y + e, y' = b x + d y + f
    @location(1) m: vec4f,
    @location(2) t: vec2f,
    @location(3) color: vec4f,
};

struct Out {
    @builtin(position) pos: vec4f,
    @location(0) p: vec2f,
    @location(1) @interpolate(flat) entry: u32,
    @location(2) @interpolate(flat) color: vec4f,
};

@vertex
fn vs(@builtin(vertex_index) vi: u32, i: In) -> Out {
    var o: Out;
    let e = entries[i.entry];
    let corner = array<vec2f, 6>(vec2f(0.0, 0.0), vec2f(1.0, 0.0), vec2f(1.0, 1.0), vec2f(0.0, 0.0), vec2f(1.0, 1.0), vec2f(0.0, 1.0))[vi];
    // a pixel and a half of margin for the antialiased edge, in shape units
    let k = view.scale * sqrt(max(abs(i.m.x * i.m.w - i.m.y * i.m.z), 1e-12));
    let margin = 1.5 / k;
    let p = mix(e.bbox.xy - vec2f(margin), e.bbox.zw + vec2f(margin), corner);
    let w = vec2f(i.m.x * p.x + i.m.z * p.y, i.m.y * p.x + i.m.w * p.y) + i.t;
    let px = w * view.scale + view.t;
    o.pos = vec4f(px.x / view.viewport.x * 2.0 - 1.0, 1.0 - px.y / view.viewport.y * 2.0, 0.0, 1.0);
    o.p = p;
    o.entry = i.entry;
    o.color = i.color;
    return o;
}

fn root_code(y1: f32, y2: f32, y3: f32) -> u32 {
    let i1 = bitcast<u32>(y1) >> 31u;
    let i2 = bitcast<u32>(y2) >> 30u;
    let i3 = bitcast<u32>(y3) >> 29u;
    var shift = (i2 & 2u) | (i1 & ~2u);
    shift = (i3 & 4u) | (shift & ~4u);
    return (0x2E74u >> shift) & 0x0101u;
}

// The two roots of the curve's y (solve_horiz) or x (solve_vert) relative to the pixel, and
// the other coordinate at each. The roots come from the numerically stable form of the
// quadratic formula: lines are stored as quadratics with their control point in the middle,
// and in f32 those are almost but not quite straight, which the textbook form turns into
// garbage on long lines. A curve that is straight to within 1/10,000 is solved as a line.
fn roots(y1: f32, a: f32, b: f32) -> vec2f {
    if abs(a) <= abs(b) * 1e-4 {
        let t = y1 * 0.5 / b;
        return vec2f(t, t);
    }
    let d = sqrt(max(b * b - a * y1, 0.0));
    if b >= 0.0 {
        let q = b + d;
        return vec2f(select(y1 / q, 0.0, q == 0.0), q / a);
    }
    let q = b - d;
    return vec2f(q / a, y1 / q);
}

fn solve_horiz(p12: vec4f, p3: vec2f) -> vec2f {
    let a = p12.xy - p12.zw * 2.0 + p3;
    let b = p12.xy - p12.zw;
    let t = roots(p12.y, a.y, b.y);
    return vec2f((a.x * t.x - b.x * 2.0) * t.x + p12.x, (a.x * t.y - b.x * 2.0) * t.y + p12.x);
}

fn solve_vert(p12: vec4f, p3: vec2f) -> vec2f {
    let a = p12.xy - p12.zw * 2.0 + p3;
    let b = p12.xy - p12.zw;
    let t = roots(p12.x, a.x, b.x);
    return vec2f((a.y * t.x - b.y * 2.0) * t.x + p12.y, (a.y * t.y - b.y * 2.0) * t.y + p12.y);
}

// Each ray returns the crossings, weighted by how much of the pixel lies past them, and how
// close to the pixel centre the nearest crossing is (1 at the centre, 0 half a pixel away).
fn ray_right(e: Entry, p: vec2f, ppu: f32) -> vec2f {
    let size = e.bbox.zw - e.bbox.xy;
    let nh = e.bands.y;
    var sum = 0.0;
    var weight = 0.0;
    let hb = u32(clamp(floor((p.y - e.bbox.y) / size.y * f32(nh)), 0.0, f32(nh - 1u)));
    let start = bands[e.bands.x + hb * 2u];
    let count = bands[e.bands.x + hb * 2u + 1u];
    for (var k = 0u; k < count; k++) {
        let ci = bands[start + k];
        let p12 = curves[ci * 2u] - vec4f(p, p);
        let p3 = curves[ci * 2u + 1u].xy - p;
        // sorted by their right end: past this one, all curves are left of the pixel
        if max(max(p12.x, p12.z), p3.x) * ppu < -0.5 {
            break;
        }
        let code = root_code(p12.y, p12.w, p3.y);
        if code != 0u {
            let r = solve_horiz(p12, p3) * ppu;
            if (code & 1u) != 0u {
                sum += clamp(r.x + 0.5, 0.0, 1.0);
                weight = max(weight, clamp(1.0 - abs(r.x) * 2.0, 0.0, 1.0));
            }
            if code > 1u {
                sum -= clamp(r.y + 0.5, 0.0, 1.0);
                weight = max(weight, clamp(1.0 - abs(r.y) * 2.0, 0.0, 1.0));
            }
        }
    }
    return vec2f(sum, weight);
}

fn ray_up(e: Entry, p: vec2f, ppu: f32) -> vec2f {
    let size = e.bbox.zw - e.bbox.xy;
    let nh = e.bands.y;
    let nv = e.bands.z;
    var sum = 0.0;
    var weight = 0.0;
    let vb = u32(clamp(floor((p.x - e.bbox.x) / size.x * f32(nv)), 0.0, f32(nv - 1u)));
    let start = bands[e.bands.x + nh * 2u + vb * 2u];
    let count = bands[e.bands.x + nh * 2u + vb * 2u + 1u];
    for (var k = 0u; k < count; k++) {
        let ci = bands[start + k];
        let p12 = curves[ci * 2u] - vec4f(p, p);
        let p3 = curves[ci * 2u + 1u].xy - p;
        if max(max(p12.y, p12.w), p3.y) * ppu < -0.5 {
            break;
        }
        let code = root_code(p12.x, p12.z, p3.x);
        if code != 0u {
            let r = solve_vert(p12, p3) * ppu;
            if (code & 1u) != 0u {
                sum -= clamp(r.x + 0.5, 0.0, 1.0);
                weight = max(weight, clamp(1.0 - abs(r.x) * 2.0, 0.0, 1.0));
            }
            if code > 1u {
                sum += clamp(r.y + 0.5, 0.0, 1.0);
                weight = max(weight, clamp(1.0 - abs(r.y) * 2.0, 0.0, 1.0));
            }
        }
    }
    return vec2f(sum, weight);
}

@fragment
fn fs(i: Out) -> @location(0) vec4f {
    let e = entries[i.entry];
    let ppu = 1.0 / max(fwidth(i.p), vec2f(1.0 / 65536.0));
    let h = ray_right(e, i.p, ppu.x);
    let v = ray_up(e, i.p, ppu.y);
    // Trust each ray in proportion to how close its nearest crossing is: a ray running
    // along an edge crosses it far away and says little about this pixel. Away from edges
    // both rays agree, and the smaller count keeps overlaps from going darker (nonzero).
    let weighted = abs(h.x * h.y + v.x * v.y) / max(h.y + v.y, 1.0 / 65536.0);
    let a = clamp(max(weighted, min(abs(h.x), abs(v.x))), 0.0, 1.0);
    if a <= 0.0 {
        discard;
    }
    // premultiplied
    return i.color * a;
}
