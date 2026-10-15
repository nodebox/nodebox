//! Benchmark: the Copy node, and cloning its output, at 10,000 and 50,000 copies.
//!
//! Copies share their shape and differ only in placement, so neither should
//! cost a copy of the points.

use std::hint::black_box;
use std::time::{Duration, Instant};

use nodebox_core::geometry::{Path, Point};
use nodebox_core::ops::{copy, resample, CopyOrder};

fn time<T>(label: &str, iterations: u32, mut f: impl FnMut() -> T) {
    // Warm up.
    black_box(f());
    let start = Instant::now();
    for _ in 0..iterations {
        black_box(f());
    }
    let per: Duration = start.elapsed() / iterations;
    println!("{label:<44} {:>10.3} ms", per.as_secs_f64() * 1000.0);
}

fn main() {
    // An ellipse resampled to 64 points, a typical shape to copy.
    let shape = resample(&Path::ellipse(0.0, 0.0, 20.0, 20.0), 64);

    for n in [10_000u32, 50_000] {
        let copies = || {
            copy(&shape, n, CopyOrder::TSR, Point::new(0.1, 0.0), 1.0, Point::new(100.0, 100.0))
        };
        time(&format!("copy {n}"), 20, copies);

        let result = copies();
        time(&format!("clone the output of copy {n}"), 20, || result.clone());
        time(&format!("bounds of every path, copy {n}"), 20, || {
            result.iter().filter_map(|p| p.bounds()).count()
        });
    }
}
