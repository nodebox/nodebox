//! Parallel operations using Rayon.
//!
//! This module provides parallel versions of operations that can benefit from
//! multi-threaded execution when processing large datasets.
//!
//! # Usage
//!
//! ```rust
//! use nodebox_core::geometry::{Path, Point};
//! use nodebox_ops::parallel;
//!
//! let paths: Vec<Path> = (0..100)
//!     .map(|i| Path::rect(i as f64 * 10.0, 0.0, 10.0, 10.0))
//!     .collect();
//!
//! let translated = parallel::translate_all(&paths, Point::new(100.0, 100.0));
//! ```

use nodebox_core::geometry::{Color, Path, Point, Transform};
use rayon::prelude::*;

/// Translate multiple paths in parallel.
pub fn translate_all(paths: &[Path], offset: Point) -> Vec<Path> {
    let transform = Transform::translate(offset.x, offset.y);
    paths.par_iter().map(|p| p.transform(&transform)).collect()
}

/// Rotate multiple paths in parallel.
pub fn rotate_all(paths: &[Path], angle: f64, origin: Point) -> Vec<Path> {
    let transform = Transform::translate(origin.x, origin.y)
        .then(&Transform::rotate(angle))
        .then(&Transform::translate(-origin.x, -origin.y));
    paths.par_iter().map(|p| p.transform(&transform)).collect()
}

/// Scale multiple paths in parallel.
pub fn scale_all(paths: &[Path], scale_pct: Point, origin: Point) -> Vec<Path> {
    let sx = scale_pct.x / 100.0;
    let sy = scale_pct.y / 100.0;
    let transform = Transform::translate(origin.x, origin.y)
        .then(&Transform::scale_xy(sx, sy))
        .then(&Transform::translate(-origin.x, -origin.y));
    paths.par_iter().map(|p| p.transform(&transform)).collect()
}

/// Apply an arbitrary transform to multiple paths in parallel.
pub fn transform_all(paths: &[Path], transform: &Transform) -> Vec<Path> {
    paths.par_iter().map(|p| p.transform(transform)).collect()
}

/// Colorize multiple paths in parallel.
pub fn colorize_all(paths: &[Path], fill: Color, stroke: Color, stroke_width: f64) -> Vec<Path> {
    paths
        .par_iter()
        .map(|p| {
            let mut result = p.clone();
            result.fill = if fill.a > 0.0 { Some(fill) } else { None };
            if stroke_width > 0.0 {
                result.stroke = Some(stroke);
                result.stroke_width = stroke_width;
            } else {
                result.stroke = None;
                result.stroke_width = 0.0;
            }
            result
        })
        .collect()
}

/// Resample multiple paths in parallel.
pub fn resample_all(paths: &[Path], amount: usize) -> Vec<Path> {
    paths
        .par_iter()
        .map(|p| p.resample_by_amount(amount))
        .collect()
}

/// Get points from multiple paths in parallel.
pub fn make_points_all(paths: &[Path], amount: usize) -> Vec<Vec<Point>> {
    paths.par_iter().map(|p| p.make_points(amount)).collect()
}

/// Get path lengths in parallel.
pub fn path_lengths(paths: &[Path]) -> Vec<f64> {
    paths.par_iter().map(|p| p.length()).collect()
}

/// Get centroids of multiple paths in parallel.
pub fn centroids(paths: &[Path]) -> Vec<Point> {
    paths
        .par_iter()
        .map(|p| match p.bounds() {
            Some(b) => Point::new(b.x + b.width / 2.0, b.y + b.height / 2.0),
            None => Point::ZERO,
        })
        .collect()
}

/// Apply a function to each path in parallel.
pub fn map_paths<F>(paths: &[Path], f: F) -> Vec<Path>
where
    F: Fn(&Path) -> Path + Sync + Send,
{
    paths.par_iter().map(f).collect()
}

/// Apply a function to each path with its index in parallel.
pub fn map_paths_indexed<F>(paths: &[Path], f: F) -> Vec<Path>
where
    F: Fn(usize, &Path) -> Path + Sync + Send,
{
    paths.par_iter().enumerate().map(|(i, p)| f(i, p)).collect()
}

/// Filter paths in parallel based on a predicate.
pub fn filter_paths<F>(paths: &[Path], predicate: F) -> Vec<Path>
where
    F: Fn(&Path) -> bool + Sync + Send,
{
    paths
        .par_iter()
        .filter(|p| predicate(p))
        .cloned()
        .collect()
}

/// Parallel numeric operations on lists.
pub mod numeric {
    use rayon::prelude::*;

    /// Sum of numbers (parallel).
    pub fn sum(numbers: &[f64]) -> f64 {
        numbers.par_iter().sum()
    }

    /// Average of numbers (parallel).
    pub fn average(numbers: &[f64]) -> f64 {
        if numbers.is_empty() {
            return 0.0;
        }
        sum(numbers) / numbers.len() as f64
    }

    /// Map a function over numbers in parallel.
    pub fn map<F>(numbers: &[f64], f: F) -> Vec<f64>
    where
        F: Fn(f64) -> f64 + Sync + Send,
    {
        numbers.par_iter().map(|&n| f(n)).collect()
    }

    /// Filter numbers in parallel.
    pub fn filter<F>(numbers: &[f64], predicate: F) -> Vec<f64>
    where
        F: Fn(f64) -> bool + Sync + Send,
    {
        numbers
            .par_iter()
            .filter(|&&n| predicate(n))
            .copied()
            .collect()
    }
}

/// Parallel string operations.
pub mod strings {
    use rayon::prelude::*;

    /// Apply a transformation to multiple strings in parallel.
    pub fn map_strings<F>(strings: &[String], f: F) -> Vec<String>
    where
        F: Fn(&str) -> String + Sync + Send,
    {
        strings.par_iter().map(|s| f(s)).collect()
    }

    /// Convert all strings to uppercase in parallel.
    pub fn uppercase_all(strings: &[String]) -> Vec<String> {
        strings.par_iter().map(|s| s.to_uppercase()).collect()
    }

    /// Convert all strings to lowercase in parallel.
    pub fn lowercase_all(strings: &[String]) -> Vec<String> {
        strings.par_iter().map(|s| s.to_lowercase()).collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn create_test_paths(count: usize) -> Vec<Path> {
        (0..count)
            .map(|i| Path::rect(i as f64 * 10.0, 0.0, 10.0, 10.0))
            .collect()
    }

    #[test]
    fn test_translate_all() {
        let paths = create_test_paths(10);
        let translated = translate_all(&paths, Point::new(100.0, 50.0));

        assert_eq!(translated.len(), 10);

        // First path should have moved from (0,0) to (100, 50)
        let bounds = translated[0].bounds().unwrap();
        assert!((bounds.x - 100.0).abs() < 0.1);
        assert!((bounds.y - 50.0).abs() < 0.1);
    }

    #[test]
    fn test_rotate_all() {
        let paths = create_test_paths(10);
        let rotated = rotate_all(&paths, 90.0, Point::ZERO);

        assert_eq!(rotated.len(), 10);
    }

    #[test]
    fn test_scale_all() {
        let paths = create_test_paths(10);
        let scaled = scale_all(&paths, Point::new(200.0, 200.0), Point::ZERO);

        assert_eq!(scaled.len(), 10);

        // First path should be 2x size
        let bounds = scaled[0].bounds().unwrap();
        assert!((bounds.width - 20.0).abs() < 0.1);
        assert!((bounds.height - 20.0).abs() < 0.1);
    }

    #[test]
    fn test_resample_all() {
        let paths = vec![
            Path::ellipse(0.0, 0.0, 100.0, 100.0),
            Path::ellipse(100.0, 0.0, 100.0, 100.0),
        ];
        let resampled = resample_all(&paths, 20);

        assert_eq!(resampled.len(), 2);
        assert_eq!(resampled[0].contours[0].points.len(), 20);
        assert_eq!(resampled[1].contours[0].points.len(), 20);
    }

    #[test]
    fn test_path_lengths() {
        let paths = vec![
            Path::line(0.0, 0.0, 100.0, 0.0),
            Path::line(0.0, 0.0, 0.0, 50.0),
        ];
        let lengths = path_lengths(&paths);

        assert_eq!(lengths.len(), 2);
        assert!((lengths[0] - 100.0).abs() < 0.1);
        assert!((lengths[1] - 50.0).abs() < 0.1);
    }

    #[test]
    fn test_centroids() {
        let paths = vec![
            Path::rect(0.0, 0.0, 100.0, 100.0),
            Path::rect(100.0, 100.0, 50.0, 50.0),
        ];
        let centers = centroids(&paths);

        assert_eq!(centers.len(), 2);
        assert!((centers[0].x - 50.0).abs() < 0.1);
        assert!((centers[0].y - 50.0).abs() < 0.1);
        assert!((centers[1].x - 125.0).abs() < 0.1);
        assert!((centers[1].y - 125.0).abs() < 0.1);
    }

    #[test]
    fn test_map_paths() {
        let paths = create_test_paths(5);
        let result = map_paths(&paths, |p| {
            let t = Transform::translate(10.0, 10.0);
            p.transform(&t)
        });

        assert_eq!(result.len(), 5);
    }

    #[test]
    fn test_filter_paths() {
        let paths = create_test_paths(10);
        let filtered = filter_paths(&paths, |p| {
            let bounds = p.bounds().unwrap();
            bounds.x < 50.0
        });

        assert_eq!(filtered.len(), 5); // Paths at x = 0, 10, 20, 30, 40
    }

    #[test]
    fn test_numeric_sum() {
        let numbers: Vec<f64> = (1..=100).map(|i| i as f64).collect();
        let sum = numeric::sum(&numbers);
        assert_eq!(sum, 5050.0);
    }

    #[test]
    fn test_numeric_map() {
        let numbers = vec![1.0, 2.0, 3.0, 4.0, 5.0];
        let squared = numeric::map(&numbers, |n| n * n);
        assert_eq!(squared, vec![1.0, 4.0, 9.0, 16.0, 25.0]);
    }

    #[test]
    fn test_strings_case() {
        let strings: Vec<String> = vec!["Hello".to_string(), "World".to_string()];
        let upper = strings::uppercase_all(&strings);
        let lower = strings::lowercase_all(&strings);

        assert_eq!(upper, vec!["HELLO", "WORLD"]);
        assert_eq!(lower, vec!["hello", "world"]);
    }
}
