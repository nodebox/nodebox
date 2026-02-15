//! String operations for NodeBox.
//!
//! This module provides functions for string manipulation including
//! creation, properties, manipulation, testing, and encoding.

/// Identity function for strings.
pub fn string(s: &str) -> String {
    s.to_string()
}

/// Split a string into a list of strings.
///
/// # Arguments
/// * `s` - The input string
/// * `separator` - The separator to split on. If empty, splits into individual characters.
pub fn make_strings(s: &str, separator: &str) -> Vec<String> {
    if s.is_empty() {
        return Vec::new();
    }
    if separator.is_empty() {
        s.chars().map(|c| c.to_string()).collect()
    } else {
        s.split(separator).map(|part| part.to_string()).collect()
    }
}

/// Get the length of a string.
pub fn length(s: &str) -> usize {
    s.len()
}

/// Count the number of words in a string.
pub fn word_count(s: &str) -> usize {
    s.split_whitespace().count()
}

/// Concatenate multiple strings.
pub fn concatenate(strings: &[&str]) -> String {
    strings.concat()
}

/// Change case method.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CaseMethod {
    Lowercase,
    Uppercase,
    Titlecase,
}

impl CaseMethod {
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "uppercase" => Self::Uppercase,
            "titlecase" => Self::Titlecase,
            _ => Self::Lowercase,
        }
    }
}

/// Change the case of a string.
pub fn change_case(value: &str, case_method: CaseMethod) -> String {
    match case_method {
        CaseMethod::Lowercase => value.to_lowercase(),
        CaseMethod::Uppercase => value.to_uppercase(),
        CaseMethod::Titlecase => to_title_case(value),
    }
}

/// Convert a string to title case.
fn to_title_case(s: &str) -> String {
    let mut result = String::with_capacity(s.len());
    let mut capitalize_next = true;

    for c in s.chars() {
        if c.is_whitespace() {
            capitalize_next = true;
            result.push(c);
        } else if capitalize_next {
            result.extend(c.to_uppercase());
            capitalize_next = false;
        } else {
            result.extend(c.to_lowercase());
        }
    }

    result
}

/// Format a number using a format string.
pub fn format_number(value: f64, format: &str) -> String {
    // Handle common format patterns
    if format.contains('%') {
        // Try to parse as printf-style format
        if format.contains('f') || format.contains('e') || format.contains('g') {
            // Floating point format
            let precision = extract_precision(format).unwrap_or(6);
            return format!("{:.prec$}", value, prec = precision);
        } else if format.contains('d') || format.contains('i') {
            // Integer format
            return format!("{}", value as i64);
        }
    }
    // Default: just convert to string
    format!("{}", value)
}

/// Extract precision from a format string like "%.2f".
fn extract_precision(format: &str) -> Option<usize> {
    let start = format.find('.')?;
    let after_dot = &format[start + 1..];
    let num_str: String = after_dot.chars().take_while(|c| c.is_ascii_digit()).collect();
    num_str.parse().ok()
}

/// Split a string into individual characters.
pub fn characters(s: &str) -> Vec<String> {
    s.chars().map(|c| c.to_string()).collect()
}

/// Generate random characters from a character set.
pub fn random_character(character_set: &str, amount: usize, seed: u64) -> Vec<String> {
    if character_set.is_empty() {
        return Vec::new();
    }

    let chars: Vec<char> = character_set.chars().collect();
    let mut state = seed.wrapping_mul(1000000000);
    let mut result = Vec::with_capacity(amount);

    for _ in 0..amount {
        state = state.wrapping_mul(1103515245).wrapping_add(12345);
        let index = ((state >> 16) & 0x7FFFFFFF) as usize % chars.len();
        result.push(chars[index].to_string());
    }

    result
}

/// Convert a string to a binary representation.
pub fn as_binary_string(s: &str, digit_sep: &str, byte_sep: &str) -> String {
    let bytes = s.as_bytes();
    let mut result = String::new();

    for byte in bytes {
        let mut val = *byte;
        for i in 0..8 {
            result.push(if val & 128 != 0 { '1' } else { '0' });
            if i < 7 {
                result.push_str(digit_sep);
            }
            val <<= 1;
        }
        result.push_str(byte_sep);
    }

    result
}

/// Convert a string to a list of binary digits.
pub fn as_binary_list(s: &str) -> Vec<String> {
    let bytes = s.as_bytes();
    let mut result = Vec::with_capacity(bytes.len() * 8);

    for byte in bytes {
        let mut val = *byte;
        for _ in 0..8 {
            result.push(if val & 128 != 0 { "1" } else { "0" }.to_string());
            val <<= 1;
        }
    }

    result
}

/// Convert a string to a list of numbers in a given radix.
pub fn as_number_list(s: &str, radix: u32, padding: bool) -> Vec<String> {
    if radix < 2 || radix > 36 {
        return Vec::new();
    }

    let bytes = s.as_bytes();
    let mut result = Vec::with_capacity(bytes.len());

    // Determine padding width based on radix
    let pad_width = if padding {
        match radix {
            2 => 8,
            3 => 6,
            4..=6 => 4,
            7..=14 => 3,
            _ => 2,
        }
    } else {
        0
    };

    for &byte in bytes {
        let num_str = format_radix(byte as u32, radix);
        if padding && num_str.len() < pad_width {
            let padded = format!("{:0>width$}", num_str, width = pad_width);
            result.push(padded);
        } else {
            result.push(num_str);
        }
    }

    result
}

/// Format a number in a given radix.
fn format_radix(mut n: u32, radix: u32) -> String {
    if n == 0 {
        return "0".to_string();
    }

    let mut result = String::new();
    while n > 0 {
        let digit = n % radix;
        let c = if digit < 10 {
            (b'0' + digit as u8) as char
        } else {
            (b'a' + (digit - 10) as u8) as char
        };
        result.insert(0, c);
        n /= radix;
    }
    result
}

/// Get the character at a specific index.
pub fn character_at(s: &str, index: i64) -> String {
    if s.is_empty() {
        return String::new();
    }

    let chars: Vec<char> = s.chars().collect();
    let len = chars.len() as i64;

    let idx = if index < 0 { len + index } else { index };

    if idx < 0 || idx >= len {
        return String::new();
    }

    chars[idx as usize].to_string()
}

/// Check if a string contains a substring.
pub fn contains(s: &str, value: &str) -> bool {
    s.contains(value)
}

/// Check if a string ends with a suffix.
pub fn ends_with(s: &str, value: &str) -> bool {
    s.ends_with(value)
}

/// Check if two strings are equal.
pub fn equal(s: &str, value: &str, case_sensitive: bool) -> bool {
    if case_sensitive {
        s == value
    } else {
        s.eq_ignore_ascii_case(value)
    }
}

/// Replace occurrences of a substring.
pub fn replace(s: &str, old_val: &str, new_val: &str) -> String {
    s.replace(old_val, new_val)
}

/// Check if a string starts with a prefix.
pub fn starts_with(s: &str, value: &str) -> bool {
    s.starts_with(value)
}

/// Get a substring.
///
/// # Arguments
/// * `s` - The input string
/// * `start` - Start index
/// * `end` - End index
/// * `end_offset` - If true, include the character at the end position
pub fn sub_string(s: &str, start: i64, end: i64, end_offset: bool) -> String {
    if s.is_empty() {
        return String::new();
    }

    let chars: Vec<char> = s.chars().collect();
    let len = chars.len() as i64;

    let (start, end) = if start < 0 && end < 0 {
        (len + start, len + end)
    } else {
        (start, end)
    };

    if end < start {
        return String::new();
    }

    let actual_end = if end_offset { end + 1 } else { end };

    let start = start.max(0) as usize;
    let end = (actual_end.min(len) as usize).min(chars.len());

    chars[start..end].iter().collect()
}

/// Remove whitespace from the start and end of a string.
pub fn trim(s: &str) -> String {
    s.trim().to_string()
}

/// Trim whitespace from the left of a string.
pub fn trim_left(s: &str) -> String {
    s.trim_start().to_string()
}

/// Trim whitespace from the right of a string.
pub fn trim_right(s: &str) -> String {
    s.trim_end().to_string()
}

/// Pad a string to a given length from the left.
pub fn pad_left(s: &str, length: usize, pad_char: char) -> String {
    if s.len() >= length {
        s.to_string()
    } else {
        format!("{:>width$}", s, width = length).replace(' ', &pad_char.to_string())
    }
}

/// Pad a string to a given length from the right.
pub fn pad_right(s: &str, length: usize, pad_char: char) -> String {
    if s.len() >= length {
        s.to_string()
    } else {
        format!("{:<width$}", s, width = length).replace(' ', &pad_char.to_string())
    }
}

/// Split a string into lines.
pub fn lines(s: &str) -> Vec<String> {
    s.lines().map(|l| l.to_string()).collect()
}

/// Join a list of strings with a separator.
pub fn join(strings: &[&str], separator: &str) -> String {
    strings.join(separator)
}

/// Repeat a string a given number of times.
pub fn repeat_string(s: &str, count: usize) -> String {
    s.repeat(count)
}

/// Reverse a string.
pub fn reverse(s: &str) -> String {
    s.chars().rev().collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_string() {
        assert_eq!(string("hello"), "hello");
    }

    #[test]
    fn test_make_strings() {
        assert_eq!(make_strings("a,b,c", ","), vec!["a", "b", "c"]);
        assert_eq!(make_strings("abc", ""), vec!["a", "b", "c"]);
        assert_eq!(make_strings("", ","), Vec::<String>::new());
    }

    #[test]
    fn test_length() {
        assert_eq!(length("hello"), 5);
        assert_eq!(length(""), 0);
    }

    #[test]
    fn test_word_count() {
        assert_eq!(word_count("hello world"), 2);
        assert_eq!(word_count("one"), 1);
        assert_eq!(word_count(""), 0);
        assert_eq!(word_count("  multiple   spaces  "), 2);
    }

    #[test]
    fn test_concatenate() {
        assert_eq!(concatenate(&["hello", " ", "world"]), "hello world");
        assert_eq!(concatenate(&[]), "");
    }

    #[test]
    fn test_change_case() {
        assert_eq!(change_case("Hello World", CaseMethod::Lowercase), "hello world");
        assert_eq!(change_case("Hello World", CaseMethod::Uppercase), "HELLO WORLD");
        assert_eq!(change_case("hello world", CaseMethod::Titlecase), "Hello World");
    }

    #[test]
    fn test_format_number() {
        assert_eq!(format_number(3.14159, "%.2f"), "3.14");
        assert_eq!(format_number(42.7, "%d"), "42");
    }

    #[test]
    fn test_characters() {
        assert_eq!(characters("abc"), vec!["a", "b", "c"]);
        assert_eq!(characters(""), Vec::<String>::new());
    }

    #[test]
    fn test_random_character() {
        let chars1 = random_character("abc", 5, 42);
        let chars2 = random_character("abc", 5, 42);
        assert_eq!(chars1, chars2); // Same seed = same result
        assert_eq!(chars1.len(), 5);
    }

    #[test]
    fn test_as_binary_string() {
        let result = as_binary_string("A", "", "");
        assert_eq!(result, "01000001");
    }

    #[test]
    fn test_as_binary_list() {
        let result = as_binary_list("A");
        assert_eq!(result, vec!["0", "1", "0", "0", "0", "0", "0", "1"]);
    }

    #[test]
    fn test_as_number_list() {
        let result = as_number_list("A", 10, false);
        assert_eq!(result, vec!["65"]); // ASCII for 'A' is 65

        let result = as_number_list("A", 16, false);
        assert_eq!(result, vec!["41"]); // 65 in hex
    }

    #[test]
    fn test_character_at() {
        assert_eq!(character_at("hello", 0), "h");
        assert_eq!(character_at("hello", 4), "o");
        assert_eq!(character_at("hello", -1), "o");
        assert_eq!(character_at("hello", 10), "");
    }

    #[test]
    fn test_contains() {
        assert!(contains("hello world", "world"));
        assert!(!contains("hello world", "xyz"));
    }

    #[test]
    fn test_ends_with() {
        assert!(ends_with("hello world", "world"));
        assert!(!ends_with("hello world", "hello"));
    }

    #[test]
    fn test_equal() {
        assert!(equal("hello", "hello", true));
        assert!(!equal("hello", "HELLO", true));
        assert!(equal("hello", "HELLO", false));
    }

    #[test]
    fn test_replace() {
        assert_eq!(replace("hello world", "world", "rust"), "hello rust");
    }

    #[test]
    fn test_starts_with() {
        assert!(starts_with("hello world", "hello"));
        assert!(!starts_with("hello world", "world"));
    }

    #[test]
    fn test_sub_string() {
        assert_eq!(sub_string("hello world", 0, 5, false), "hello");
        assert_eq!(sub_string("hello world", 0, 5, true), "hello ");
        assert_eq!(sub_string("hello", -2, -1, true), "lo");
    }

    #[test]
    fn test_trim() {
        assert_eq!(trim("  hello  "), "hello");
        assert_eq!(trim_left("  hello  "), "hello  ");
        assert_eq!(trim_right("  hello  "), "  hello");
    }

    #[test]
    fn test_pad() {
        assert_eq!(pad_left("42", 5, '0'), "00042");
        assert_eq!(pad_right("42", 5, '0'), "42000");
    }

    #[test]
    fn test_lines() {
        assert_eq!(lines("a\nb\nc"), vec!["a", "b", "c"]);
    }

    #[test]
    fn test_join() {
        assert_eq!(join(&["a", "b", "c"], ", "), "a, b, c");
    }

    #[test]
    fn test_repeat_string() {
        assert_eq!(repeat_string("ab", 3), "ababab");
    }

    #[test]
    fn test_reverse() {
        assert_eq!(reverse("hello"), "olleh");
    }
}
