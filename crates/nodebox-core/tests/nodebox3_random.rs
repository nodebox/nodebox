//! Seeded nodes must give the same output as NodeBox 3.
//!
//! `nodebox3_random.txt` holds the output of `nodebox3_random.py`, which runs
//! NodeBox 3's scatter and wiggle code on Jython 2.7.2 with Java's
//! `GeneralPath` for point-in-shape tests.

use nodebox_core::geometry::{Contour, Path, PathPoint, Point};
use nodebox_core::ops::{scatter, wiggle, WiggleScope};

fn contour(points: &[(f64, f64, char)]) -> Contour {
    let points = points
        .iter()
        .map(|&(x, y, t)| match t {
            'D' => PathPoint::curve_data(x, y),
            'C' => PathPoint::curve_to(x, y),
            _ => PathPoint::line_to(x, y),
        })
        .collect();
    Contour::from_points(points, true)
}

fn star() -> Contour {
    contour(&[
        (0.0, -50.0, 'L'), (11.8, -16.2, 'L'), (47.6, -15.5, 'L'), (19.1, 6.2, 'L'), (29.4, 40.5, 'L'),
        (0.0, 20.0, 'L'), (-29.4, 40.5, 'L'), (-19.1, 6.2, 'L'), (-47.6, -15.5, 'L'), (-11.8, -16.2, 'L'),
    ])
}

fn blob() -> Contour {
    contour(&[
        (0.0, 0.0, 'L'), (40.0, -30.0, 'D'), (90.0, 10.0, 'D'), (80.0, 50.0, 'C'),
        (70.0, 90.0, 'D'), (10.0, 80.0, 'D'), (-10.0, 40.0, 'C'),
    ])
}

fn hole() -> Contour {
    contour(&[(20.0, 20.0, 'L'), (50.0, 20.0, 'L'), (50.0, 40.0, 'L'), (20.0, 40.0, 'L')])
}

fn hex(v: f64) -> String {
    format!("{:016x}", v.to_bits())
}

fn line(name: &str, points: &[Point]) -> String {
    let pts: Vec<String> = points.iter().map(|p| format!("{},{}", hex(p.x), hex(p.y))).collect();
    format!("{} {} {}", name, points.len(), pts.join(" ")).trim_end().to_string()
}

fn all_points(path: &Path) -> Vec<Point> {
    path.contours.iter().flat_map(|c| c.points.iter().map(|p| p.point)).collect()
}

#[test]
fn scatter_and_wiggle_match_nodebox_3() {
    let star_path = Path::from_contours(vec![star()]);
    let blob_path = Path::from_contours(vec![blob()]);
    let blob_hole = Path::from_contours(vec![blob(), hole()]);
    let ellipse = Path::ellipse(5.0, -3.0, 80.0, 50.0);

    let b = blob_path.curve_bounds().unwrap();
    let mut got = vec![format!("bounds blob {} {} {} {}", hex(b.x), hex(b.y), hex(b.width), hex(b.height))];
    for seed in [0, 42, -3] {
        got.push(line(&format!("scatter star {seed}"), &scatter(&star_path, 25, seed)));
        got.push(line(&format!("scatter blob+hole {seed}"), &scatter(&blob_hole, 25, seed)));
        got.push(line(&format!("scatter ellipse {seed}"), &scatter(&ellipse, 25, seed)));
        for (name, scope) in [("points", WiggleScope::Points), ("contours", WiggleScope::Contours), ("paths", WiggleScope::Paths)] {
            let wiggled = wiggle(&blob_hole, scope, Point::new(10.0, 5.0), seed);
            got.push(line(&format!("wiggle {name} {seed}"), &all_points(&wiggled)));
        }
    }

    let expected: Vec<&str> = include_str!("nodebox3_random.txt").lines().map(str::trim_end).collect();
    assert_eq!(got.len(), expected.len());
    for (g, e) in got.iter().zip(expected) {
        assert_eq!(g, e);
    }
}

#[test]
fn java_seeded_nodes_match_nodebox_3() {
    // Expected values from NodeBox 3's Java code (MathFunctions.randomNumbers,
    // StringFunctions.randomCharacter, ListFunctions.pick) on OpenJDK 21.
    let numbers: Vec<String> = nodebox_core::ops::math::random_numbers(4, -10.0, 90.0, 5)
        .into_iter()
        .map(hex)
        .collect();
    assert_eq!(numbers, ["4050877cfaa40638", "4053b96042da0d6e", "4047b6c340e189d7", "403186c44b46b859"]);

    let chars = nodebox_core::ops::string::random_character("abcdefghijklmnopqrstuvwxyz", 12, 7);
    assert_eq!(chars.concat(), "oaygtcwerniy");

    let items: Vec<i64> = (0..20).collect();
    assert_eq!(nodebox_core::ops::list::pick(&items, 5, -2), [0, 2, 16, 8, 12]);
}
