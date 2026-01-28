//! Math operations for NodeBox.
//!
//! This module provides mathematical functions including basic arithmetic,
//! trigonometry, aggregation, and number generation.

use nodebox_core::geometry::Point;
use std::f64::consts::{E, PI};

/// Identity function for floating point numbers.
pub fn number(n: f64) -> f64 {
    n
}

/// Identity function for integers.
pub fn integer(n: i64) -> i64 {
    n
}

/// Identity function for booleans.
pub fn make_boolean(b: bool) -> bool {
    b
}

/// Add two numbers.
pub fn add(n1: f64, n2: f64) -> f64 {
    n1 + n2
}

/// Subtract two numbers.
pub fn subtract(n1: f64, n2: f64) -> f64 {
    n1 - n2
}

/// Multiply two numbers.
pub fn multiply(n1: f64, n2: f64) -> f64 {
    n1 * n2
}

/// Divide two numbers.
///
/// # Panics
/// Panics if n2 is zero.
pub fn divide(n1: f64, n2: f64) -> f64 {
    assert!(n2 != 0.0, "Divider cannot be zero.");
    n1 / n2
}

/// Modulo operation.
///
/// # Panics
/// Panics if n2 is zero.
pub fn modulo(n1: f64, n2: f64) -> f64 {
    assert!(n2 != 0.0, "Divider cannot be zero.");
    n1 % n2
}

/// Square root.
pub fn sqrt(n: f64) -> f64 {
    n.sqrt()
}

/// Power function.
pub fn pow(base: f64, exponent: f64) -> f64 {
    base.powf(exponent)
}

/// Natural logarithm.
///
/// # Panics
/// Panics if n is zero.
pub fn log(n: f64) -> f64 {
    assert!(n != 0.0, "Value cannot be zero.");
    n.ln()
}

/// Check if a number is even.
pub fn even(n: f64) -> bool {
    n % 2.0 == 0.0
}

/// Check if a number is odd.
pub fn odd(n: f64) -> bool {
    n % 2.0 != 0.0
}

/// Negate a number.
pub fn negate(n: f64) -> f64 {
    -n
}

/// Absolute value.
pub fn abs(n: f64) -> f64 {
    n.abs()
}

/// Sum of numbers.
pub fn sum(numbers: &[f64]) -> f64 {
    numbers.iter().sum()
}

/// Average of numbers.
pub fn average(numbers: &[f64]) -> f64 {
    if numbers.is_empty() {
        return 0.0;
    }
    sum(numbers) / numbers.len() as f64
}

/// Maximum value in a list.
pub fn max(numbers: &[f64]) -> f64 {
    numbers.iter().copied().fold(f64::NEG_INFINITY, f64::max)
}

/// Minimum value in a list.
pub fn min(numbers: &[f64]) -> f64 {
    numbers.iter().copied().fold(f64::INFINITY, f64::min)
}

/// Ceiling function.
pub fn ceil(n: f64) -> f64 {
    n.ceil()
}

/// Floor function.
pub fn floor(n: f64) -> f64 {
    n.floor()
}

/// Round to nearest integer.
pub fn round(n: f64) -> i64 {
    n.round() as i64
}

/// Compare two values.
pub fn compare(o1: f64, o2: f64, comparator: &str) -> bool {
    match comparator {
        "<" => o1 < o2,
        ">" => o1 > o2,
        "<=" => o1 <= o2,
        ">=" => o1 >= o2,
        "==" => (o1 - o2).abs() < f64::EPSILON,
        "!=" => (o1 - o2).abs() >= f64::EPSILON,
        _ => panic!("unknown comparison operation: {}", comparator),
    }
}

/// Logic operation on two booleans.
pub fn logic_operator(b1: bool, b2: bool, operator: &str) -> bool {
    match operator {
        "or" => b1 || b2,
        "and" => b1 && b2,
        "xor" => b1 ^ b2,
        _ => panic!("unknown logical operation: {}", operator),
    }
}

/// Parse a string into a list of numbers.
pub fn make_numbers(s: &str, separator: &str) -> Vec<f64> {
    if s.is_empty() {
        return Vec::new();
    }
    if separator.is_empty() {
        s.chars()
            .filter_map(|c| c.to_string().parse().ok())
            .collect()
    } else {
        s.split(separator)
            .filter_map(|part| part.trim().parse().ok())
            .collect()
    }
}

/// Generate random numbers.
pub fn random_numbers(amount: usize, start: f64, end: f64, seed: u64) -> Vec<f64> {
    // Simple LCG random number generator for reproducibility
    let mut state = seed.wrapping_mul(1000000000);
    let mut result = Vec::with_capacity(amount);
    for _ in 0..amount {
        state = state.wrapping_mul(1103515245).wrapping_add(12345);
        let normalized = ((state >> 16) & 0x7FFF) as f64 / 32767.0;
        result.push(start + normalized * (end - start));
    }
    result
}

/// Generate evenly-spaced samples between start and end.
pub fn sample(amount: usize, start: f64, end: f64) -> Vec<f64> {
    if amount == 0 {
        return Vec::new();
    }
    if amount == 1 {
        return vec![start + (end - start) / 2.0];
    }
    let step = (end - start) / (amount - 1) as f64;
    (0..amount).map(|i| start + step * i as f64).collect()
}

/// Generate a range of numbers from start to end with a given step.
pub fn range(start: f64, end: f64, step: f64) -> Vec<f64> {
    if step == 0.0 || start == end {
        return Vec::new();
    }
    if (start < end && step < 0.0) || (start > end && step > 0.0) {
        return Vec::new();
    }

    let mut result = Vec::new();
    let mut current = start;
    if step > 0.0 {
        while current < end {
            result.push(current);
            current += step;
        }
    } else {
        while current > end {
            result.push(current);
            current += step;
        }
    }
    result
}

/// Running total (cumulative sum).
pub fn running_total(numbers: &[f64]) -> Vec<f64> {
    if numbers.is_empty() {
        return vec![0.0];
    }
    let mut total = 0.0;
    let mut result = Vec::with_capacity(numbers.len());
    for &n in numbers {
        result.push(total);
        total += n;
    }
    result
}

/// Convert degrees to radians.
pub fn radians(degrees: f64) -> f64 {
    degrees * PI / 180.0
}

/// Convert radians to degrees.
pub fn degrees(radians: f64) -> f64 {
    radians * 180.0 / PI
}

/// Calculate the angle between two points in degrees.
pub fn angle(p1: Point, p2: Point) -> f64 {
    degrees((p2.y - p1.y).atan2(p2.x - p1.x))
}

/// Calculate the distance between two points.
pub fn distance(p1: Point, p2: Point) -> f64 {
    p1.distance_to(p2)
}

/// Calculate coordinates from a point given angle (in degrees) and distance.
pub fn coordinates(p: Point, angle_deg: f64, dist: f64) -> Point {
    let angle_rad = radians(angle_deg);
    Point::new(p.x + angle_rad.cos() * dist, p.y + angle_rad.sin() * dist)
}

/// Reflect a point through another point.
pub fn reflect(p1: Point, p2: Point, angle_offset: f64, distance_factor: f64) -> Point {
    let dist = distance(p1, p2) * distance_factor;
    let ang = angle(p1, p2) + angle_offset;
    coordinates(p1, ang, dist)
}

/// Sine function.
pub fn sin(n: f64) -> f64 {
    n.sin()
}

/// Cosine function.
pub fn cos(n: f64) -> f64 {
    n.cos()
}

/// The constant Pi.
pub fn pi() -> f64 {
    PI
}

/// The constant E (Euler's number).
pub fn e() -> f64 {
    E
}

/// Overflow handling method for convertRange.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum OverflowMethod {
    Wrap,
    Mirror,
    Clamp,
    Ignore,
}

impl OverflowMethod {
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "wrap" => Self::Wrap,
            "mirror" => Self::Mirror,
            "clamp" => Self::Clamp,
            _ => Self::Ignore,
        }
    }
}

/// Clamp a value between min and max.
pub fn clamp(value: f64, min: f64, max: f64) -> f64 {
    value.max(min).min(max)
}

/// Convert a value from one range to another.
pub fn convert_range(
    mut value: f64,
    src_min: f64,
    src_max: f64,
    target_min: f64,
    target_max: f64,
    overflow_method: OverflowMethod,
) -> f64 {
    let src_range = src_max - src_min;

    match overflow_method {
        OverflowMethod::Wrap => {
            value = src_min + value % src_range;
        }
        OverflowMethod::Mirror => {
            let rest = value % src_range;
            if ((value / src_range) as i32) % 2 == 1 {
                value = src_max - rest;
            } else {
                value = src_min + rest;
            }
        }
        OverflowMethod::Clamp => {
            value = clamp(value, src_min, src_max);
        }
        OverflowMethod::Ignore => {}
    }

    // Convert to 0.0-1.0 range
    if src_range.abs() < f64::EPSILON {
        value = src_min;
    } else {
        value = (value - src_min) / src_range;
    }

    // Convert to target range
    target_min + value * (target_max - target_min)
}

/// Wave type for wave function.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum WaveType {
    Sine,
    Square,
    Triangle,
    Sawtooth,
}

impl WaveType {
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "square" => Self::Square,
            "triangle" => Self::Triangle,
            "sawtooth" => Self::Sawtooth,
            _ => Self::Sine,
        }
    }
}

/// Generate a wave value at a given offset.
pub fn wave(min: f64, max: f64, period: f64, offset: f64, wave_type: WaveType) -> f64 {
    if period.abs() < f64::EPSILON {
        return min;
    }

    let range = max - min;
    let phase = (offset % period) / period;

    let normalized = match wave_type {
        WaveType::Sine => (phase * 2.0 * PI).sin() * 0.5 + 0.5,
        WaveType::Square => {
            if phase < 0.5 {
                1.0
            } else {
                0.0
            }
        }
        WaveType::Triangle => {
            if phase < 0.5 {
                phase * 2.0
            } else {
                2.0 - phase * 2.0
            }
        }
        WaveType::Sawtooth => phase,
    };

    min + normalized * range
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_basic_arithmetic() {
        assert_eq!(add(2.0, 3.0), 5.0);
        assert_eq!(subtract(5.0, 3.0), 2.0);
        assert_eq!(multiply(4.0, 3.0), 12.0);
        assert_eq!(divide(10.0, 2.0), 5.0);
        assert_eq!(modulo(10.0, 3.0), 1.0);
    }

    #[test]
    fn test_math_functions() {
        assert_eq!(sqrt(16.0), 4.0);
        assert_eq!(pow(2.0, 3.0), 8.0);
        assert!((log(E) - 1.0).abs() < 1e-10);
        assert_eq!(abs(-5.0), 5.0);
        assert_eq!(negate(5.0), -5.0);
    }

    #[test]
    fn test_even_odd() {
        assert!(even(4.0));
        assert!(!even(3.0));
        assert!(odd(3.0));
        assert!(!odd(4.0));
    }

    #[test]
    fn test_aggregation() {
        let numbers = vec![1.0, 2.0, 3.0, 4.0, 5.0];
        assert_eq!(sum(&numbers), 15.0);
        assert_eq!(average(&numbers), 3.0);
        assert_eq!(max(&numbers), 5.0);
        assert_eq!(min(&numbers), 1.0);
    }

    #[test]
    fn test_rounding() {
        assert_eq!(ceil(2.3), 3.0);
        assert_eq!(floor(2.7), 2.0);
        assert_eq!(round(2.5), 3);
        assert_eq!(round(2.4), 2);
    }

    #[test]
    fn test_compare() {
        assert!(compare(1.0, 2.0, "<"));
        assert!(compare(2.0, 1.0, ">"));
        assert!(compare(2.0, 2.0, "=="));
        assert!(compare(1.0, 2.0, "!="));
        assert!(compare(2.0, 2.0, "<="));
        assert!(compare(2.0, 2.0, ">="));
    }

    #[test]
    fn test_logic_operator() {
        assert!(logic_operator(true, false, "or"));
        assert!(!logic_operator(true, false, "and"));
        assert!(logic_operator(true, false, "xor"));
        assert!(!logic_operator(true, true, "xor"));
    }

    #[test]
    fn test_sample() {
        let samples = sample(3, 0.0, 100.0);
        assert_eq!(samples, vec![0.0, 50.0, 100.0]);

        let samples = sample(5, 0.0, 10.0);
        assert_eq!(samples.len(), 5);
        assert_eq!(samples[0], 0.0);
        assert_eq!(samples[4], 10.0);
    }

    #[test]
    fn test_range() {
        let r = range(0.0, 5.0, 1.0);
        assert_eq!(r, vec![0.0, 1.0, 2.0, 3.0, 4.0]);

        let r = range(5.0, 0.0, -1.0);
        assert_eq!(r, vec![5.0, 4.0, 3.0, 2.0, 1.0]);

        let r = range(0.0, 5.0, -1.0);
        assert!(r.is_empty());
    }

    #[test]
    fn test_running_total() {
        let r = running_total(&[1.0, 2.0, 3.0]);
        assert_eq!(r, vec![0.0, 1.0, 3.0]);
    }

    #[test]
    fn test_trig() {
        assert!((radians(180.0) - PI).abs() < 1e-10);
        assert!((degrees(PI) - 180.0).abs() < 1e-10);
        assert!((sin(PI / 2.0) - 1.0).abs() < 1e-10);
        assert!((cos(0.0) - 1.0).abs() < 1e-10);
    }

    #[test]
    fn test_angle_distance() {
        let p1 = Point::new(0.0, 0.0);
        let p2 = Point::new(3.0, 4.0);
        assert_eq!(distance(p1, p2), 5.0);

        let p1 = Point::new(0.0, 0.0);
        let p2 = Point::new(1.0, 0.0);
        assert!((angle(p1, p2) - 0.0).abs() < 1e-10);
    }

    #[test]
    fn test_coordinates() {
        let p = Point::new(0.0, 0.0);
        let result = coordinates(p, 0.0, 10.0);
        assert!((result.x - 10.0).abs() < 1e-10);
        assert!(result.y.abs() < 1e-10);
    }

    #[test]
    fn test_random_numbers() {
        let r1 = random_numbers(5, 0.0, 10.0, 42);
        let r2 = random_numbers(5, 0.0, 10.0, 42);
        assert_eq!(r1, r2); // Same seed should produce same results

        for &n in &r1 {
            assert!(n >= 0.0 && n <= 10.0);
        }
    }

    #[test]
    fn test_make_numbers() {
        let nums = make_numbers("1,2,3", ",");
        assert_eq!(nums, vec![1.0, 2.0, 3.0]);

        let nums = make_numbers("1;2;3", ";");
        assert_eq!(nums, vec![1.0, 2.0, 3.0]);
    }

    #[test]
    fn test_convert_range() {
        let v = convert_range(50.0, 0.0, 100.0, 0.0, 1.0, OverflowMethod::Ignore);
        assert!((v - 0.5).abs() < 1e-10);

        let v = convert_range(0.0, 0.0, 100.0, 0.0, 10.0, OverflowMethod::Ignore);
        assert!(v.abs() < 1e-10);
    }

    #[test]
    fn test_wave_sine() {
        let v = wave(0.0, 1.0, 1.0, 0.0, WaveType::Sine);
        assert!((v - 0.5).abs() < 1e-10); // Sine starts at 0.5 (middle)

        let v = wave(0.0, 1.0, 1.0, 0.25, WaveType::Sine);
        assert!((v - 1.0).abs() < 1e-10); // Peak at quarter period
    }

    #[test]
    fn test_wave_square() {
        let v = wave(0.0, 1.0, 1.0, 0.0, WaveType::Square);
        assert_eq!(v, 1.0);

        let v = wave(0.0, 1.0, 1.0, 0.6, WaveType::Square);
        assert_eq!(v, 0.0);
    }

    #[test]
    fn test_constants() {
        assert!((pi() - PI).abs() < 1e-10);
        assert!((e() - E).abs() < 1e-10);
    }
}
