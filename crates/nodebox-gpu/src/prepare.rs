//! The CPU side: curves, bands and instances for a list of paths.

use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;

use kurbo::{BezPath, CubicBez, PathEl, Point};
use nodebox_core::geometry::{Color, Contour, Path, PointType, Shape, Transform};

/// How far, in document units, curves may deviate when cubics become
/// quadratics and strokes become outlines.
const TOLERANCE: f64 = 0.01;

/// The most bands a shape is split into, in each direction.
const MAX_BANDS: usize = 64;

type Quad = [[f32; 2]; 3];

#[repr(C)]
#[derive(Clone, Copy, Debug, bytemuck::Pod, bytemuck::Zeroable)]
pub(crate) struct Entry {
    pub bbox: [f32; 4],
    /// Offset of the band table, horizontal bands, vertical bands.
    pub bands: [u32; 4],
}

/// One drawn outline: an atlas entry, placed by `m` and `t`
/// (x' = m0 x + m2 y + t0, y' = m1 x + m3 y + t1), in one color.
#[repr(C)]
#[derive(Clone, Copy, Debug, bytemuck::Pod, bytemuck::Zeroable)]
pub(crate) struct Instance {
    pub entry: u32,
    pub m: [f32; 4],
    pub t: [f32; 2],
    /// Premultiplied.
    pub color: [f32; 4],
}

/// Curves, bands and instances, ready to upload.
#[derive(Default)]
pub struct Prepared {
    pub(crate) id: u64,
    /// Each curve is two vec4s: (p0, p1) and (p2, unused).
    pub(crate) curves: Vec<[f32; 4]>,
    pub(crate) entries: Vec<Entry>,
    pub(crate) bands: Vec<u32>,
    pub(crate) instances: Vec<Instance>,
}

impl Prepared {
    /// The number of outlines drawn.
    pub fn instance_count(&self) -> usize {
        self.instances.len()
    }

    /// The number of distinct outlines in the atlas.
    pub fn outline_count(&self) -> usize {
        self.entries.len()
    }

    fn add(&mut self, quads: &[Quad]) -> Option<u32> {
        if quads.is_empty() {
            return None;
        }
        let index = self.entries.len() as u32;
        let mut lo = [f32::MAX; 2];
        let mut hi = [f32::MIN; 2];
        for p in quads.iter().flatten() {
            lo = [lo[0].min(p[0]), lo[1].min(p[1])];
            hi = [hi[0].max(p[0]), hi[1].max(p[1])];
        }
        let first = self.curves.len() as u32 / 2;
        for q in quads {
            self.curves.push([q[0][0], q[0][1], q[1][0], q[1][1]]);
            self.curves.push([q[2][0], q[2][1], 0.0, 0.0]);
        }
        let n = quads.len().div_ceil(3).clamp(1, MAX_BANDS);
        let base = self.bands.len();
        self.bands.resize(base + n * 4, 0);
        let mut list: Vec<(f32, u32)> = Vec::new();
        for axis in 0..2 {
            let (along, across) = if axis == 0 { (0, 1) } else { (1, 0) };
            let span = (hi[across] - lo[across]).max(1e-6);
            let eps = span / 4096.0;
            for b in 0..n {
                let b0 = lo[across] + span * b as f32 / n as f32 - eps;
                let b1 = lo[across] + span * (b + 1) as f32 / n as f32 + eps;
                list.clear();
                list.extend(
                    quads
                        .iter()
                        .enumerate()
                        .filter(|(_, q)| {
                            let mn = q[0][across].min(q[1][across]).min(q[2][across]);
                            let mx = q[0][across].max(q[1][across]).max(q[2][across]);
                            mx >= b0 && mn <= b1 && mx - mn > 1e-7
                        })
                        .map(|(i, q)| (q[0][along].max(q[1][along]).max(q[2][along]), first + i as u32)),
                );
                // Sorted by their far end, so the shader can stop early.
                list.sort_unstable_by(|a, b| b.0.total_cmp(&a.0));
                let slot = base + (axis * n + b) * 2;
                self.bands[slot] = self.bands.len() as u32;
                self.bands[slot + 1] = list.len() as u32;
                self.bands.extend(list.iter().map(|l| l.1));
            }
        }
        self.entries.push(Entry {
            bbox: [lo[0], lo[1], hi[0], hi[1]],
            bands: [base as u32, n as u32, n as u32, 0],
        });
        Some(index)
    }
}

/// Build the curves and instances for `paths`, in paint order: each path's
/// fill, then its stroke.
///
/// A path with neither fill nor stroke gets a 1-unit black stroke, as in
/// nodebox-vello. Strokes are never scaled by a path's placement: a shape's
/// stroke outline is shared only between paths whose placement is a rotation,
/// mirror and uniform scale; other paths stroke their placed points.
pub fn prepare(paths: &[Path]) -> Prepared {
    static NEXT_ID: AtomicU64 = AtomicU64::new(1);
    let mut out = Prepared { id: NEXT_ID.fetch_add(1, Ordering::Relaxed), ..Default::default() };
    // Keyed by address; `paths` keeps every shape alive while this runs.
    let mut fills: HashMap<*const Shape, Option<u32>> = HashMap::new();
    let mut strokes: HashMap<(*const Shape, i64), Option<u32>> = HashMap::new();
    let mut beziers: HashMap<*const Shape, Arc<BezPath>> = HashMap::new();
    let mut quads = Vec::new();

    for path in paths {
        let shape = path.shape();
        let key = Arc::as_ptr(shape);
        let placement = path.placement();
        let bezier = |beziers: &mut HashMap<_, Arc<BezPath>>| {
            beziers.entry(key).or_insert_with(|| Arc::new(shape_to_bezpath(shape))).clone()
        };

        if let Some(color) = path.fill {
            let entry = *fills.entry(key).or_insert_with(|| {
                quads.clear();
                quads_of(&bezier(&mut beziers), &mut quads);
                out.add(&quads)
            });
            if let Some(entry) = entry {
                out.instances.push(instance(entry, &placement, color));
            }
        }

        let stroke = match (path.fill, path.stroke) {
            (_, Some(color)) => Some((color, path.stroke_width)),
            (None, None) => Some((Color::BLACK, 1.0)),
            (Some(_), None) => None,
        };
        let Some((color, width)) = stroke else { continue };
        if width <= 0.0 {
            continue;
        }
        match similarity_scale(&placement) {
            Some(scale) => {
                let shape_width = width / scale;
                // Rotations leave the scale a hair off 1, so widths that round to
                // the same millionth share an outline.
                let width_key = (shape_width * 1e6).round() as i64;
                let entry = *strokes.entry((key, width_key)).or_insert_with(|| {
                    let outline = stroke_outline(&bezier(&mut beziers), shape_width, TOLERANCE / scale);
                    quads.clear();
                    quads_of(&outline, &mut quads);
                    out.add(&quads)
                });
                if let Some(entry) = entry {
                    out.instances.push(instance(entry, &placement, color));
                }
            }
            None => {
                let mut placed = (*bezier(&mut beziers)).clone();
                placed.apply_affine(affine(&placement));
                let outline = stroke_outline(&placed, width, TOLERANCE);
                quads.clear();
                quads_of(&outline, &mut quads);
                if let Some(entry) = out.add(&quads) {
                    out.instances.push(instance(entry, &Transform::IDENTITY, color));
                }
            }
        }
    }
    out
}

fn instance(entry: u32, t: &Transform, color: Color) -> Instance {
    let [a, b, c, d, e, f] = t.as_array();
    // Quantised to 8 bits as nodebox-vello does, so both draw the same color.
    let q = |v: f64| f32::from((v.clamp(0.0, 1.0) * 255.0) as u8) / 255.0;
    let alpha = q(color.a);
    Instance {
        entry,
        m: [a as f32, b as f32, c as f32, d as f32],
        t: [e as f32, f as f32],
        color: [q(color.r) * alpha, q(color.g) * alpha, q(color.b) * alpha, alpha],
    }
}

fn affine(t: &Transform) -> kurbo::Affine {
    kurbo::Affine::new(t.as_array())
}

/// The scale of a placement that is a rotation, mirror and uniform scale, so
/// a stroke outline can be shared; None for any other placement.
fn similarity_scale(t: &Transform) -> Option<f64> {
    let [a, b, c, d, _, _] = t.as_array();
    let scale = (a * d - b * c).abs().sqrt();
    let tolerance = 1e-9 * scale.max(1.0);
    let similar = ((a - d).abs() < tolerance && (b + c).abs() < tolerance)
        || ((a + d).abs() < tolerance && (b - c).abs() < tolerance);
    (similar && scale > 1e-12).then_some(scale)
}

/// Round joins and caps, as Vello's default stroke.
fn stroke_outline(path: &BezPath, width: f64, tolerance: f64) -> BezPath {
    let style = kurbo::Stroke::new(width);
    kurbo::stroke(path.iter(), &style, &kurbo::StrokeOpts::default(), tolerance)
}

fn p32(p: Point) -> [f32; 2] {
    [p.x as f32, p.y as f32]
}

/// Quadratics for a filled outline. Every subpath is closed, as a fill closes it.
fn quads_of(path: &BezPath, out: &mut Vec<Quad>) {
    let mut start = Point::ZERO;
    let mut at = Point::ZERO;
    let line = |a: Point, b: Point, out: &mut Vec<Quad>| {
        if a != b {
            out.push([p32(a), p32(a.midpoint(b)), p32(b)]);
        }
    };
    for el in path.elements() {
        match *el {
            PathEl::MoveTo(p) => {
                line(at, start, out);
                start = p;
                at = p;
            }
            PathEl::LineTo(p) => {
                line(at, p, out);
                at = p;
            }
            PathEl::QuadTo(c, p) => {
                out.push([p32(at), p32(c), p32(p)]);
                at = p;
            }
            PathEl::CurveTo(c1, c2, p) => {
                for (_, _, q) in CubicBez::new(at, c1, c2, p).to_quads(TOLERANCE) {
                    out.push([p32(q.p0), p32(q.p1), p32(q.p2)]);
                }
                at = p;
            }
            PathEl::ClosePath => {
                line(at, start, out);
                at = start;
            }
        }
    }
    line(at, start, out);
}

/// The shape's contours as a BezPath, in shape coordinates. The same reading
/// of NodeBox points as nodebox-vello's `contour_to_bezpath`.
fn shape_to_bezpath(shape: &Shape) -> BezPath {
    let mut bez = BezPath::new();
    for contour in shape.contours() {
        add_contour(&mut bez, contour);
    }
    bez
}

fn add_contour(bez: &mut BezPath, contour: &Contour) {
    let points = &contour.points;
    if points.is_empty() {
        return;
    }
    let kp = |i: usize| Point::new(points[i].point.x, points[i].point.y);
    bez.move_to(kp(0));
    let mut i = 1;
    while i < points.len() {
        let kind = |k: usize| points.get(k).map(|p| p.point_type);
        match points[i].point_type {
            PointType::CurveData
                if kind(i + 1) == Some(PointType::CurveData) && kind(i + 2) == Some(PointType::CurveTo) =>
            {
                bez.curve_to(kp(i), kp(i + 1), kp(i + 2));
                i += 3;
            }
            PointType::QuadData if kind(i + 1) == Some(PointType::QuadTo) => {
                bez.quad_to(kp(i), kp(i + 1));
                i += 2;
            }
            // Lines, and any point out of sequence, as nodebox-vello reads them.
            _ => {
                bez.line_to(kp(i));
                i += 1;
            }
        }
    }
    if contour.closed {
        bez.close_path();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn copies_share_their_outlines() {
        let shape = Path::ellipse(0.0, 0.0, 10.0, 10.0).with_stroke(Some(Color::BLACK));
        let paths: Vec<Path> = (0..100)
            .map(|i| shape.transform(&Transform::rotate(i as f64).then(&Transform::translate(i as f64, 0.0))))
            .collect();
        let prepared = prepare(&paths);
        assert_eq!(prepared.instance_count(), 200);
        assert_eq!(prepared.outline_count(), 2);
    }

    #[test]
    fn uneven_scale_strokes_placed_points() {
        let shape = Path::rect(0.0, 0.0, 10.0, 10.0).with_stroke(Some(Color::BLACK));
        let paths = vec![
            shape.transform(&Transform::scale_xy(2.0, 1.0)),
            shape.transform(&Transform::scale_xy(1.0, 2.0)),
        ];
        let prepared = prepare(&paths);
        // One shared fill, two stroke outlines of their own.
        assert_eq!(prepared.outline_count(), 3);
    }

    #[test]
    fn unstyled_paths_get_a_black_stroke() {
        let mut path = Path::line(0.0, 0.0, 10.0, 0.0);
        path.stroke = None;
        let prepared = prepare(&[path]);
        assert_eq!(prepared.instance_count(), 1);
        assert_eq!(prepared.instances[0].color, [0.0, 0.0, 0.0, 1.0]);
    }
}
