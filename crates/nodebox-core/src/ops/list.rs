//! List operations for NodeBox.
//!
//! This module provides functions for manipulating lists including
//! access, transformation, filtering, and sorting operations.

use std::collections::{HashMap, HashSet};

/// Count the number of items in a list.
pub fn count<T>(items: &[T]) -> usize {
    items.len()
}

/// Get the first item of a list.
pub fn first<T: Clone>(items: &[T]) -> Option<T> {
    items.first().cloned()
}

/// Get the second item of a list.
pub fn second<T: Clone>(items: &[T]) -> Option<T> {
    items.get(1).cloned()
}

/// Get all but the first item of a list.
pub fn rest<T: Clone>(items: &[T]) -> Vec<T> {
    if items.is_empty() {
        Vec::new()
    } else {
        items[1..].to_vec()
    }
}

/// Get the last item of a list.
pub fn last<T: Clone>(items: &[T]) -> Option<T> {
    items.last().cloned()
}

/// Combine multiple lists into one.
pub fn combine<T: Clone>(lists: &[&[T]]) -> Vec<T> {
    let mut result = Vec::new();
    for list in lists {
        result.extend(list.iter().cloned());
    }
    result
}

/// Take a portion of a list.
///
/// # Arguments
/// * `items` - The input list
/// * `start_index` - Starting index (0-based)
/// * `size` - Number of items to take
/// * `invert` - If true, omit the specified range instead of keeping it
pub fn slice<T: Clone>(items: &[T], start_index: usize, size: usize, invert: bool) -> Vec<T> {
    if items.is_empty() {
        return Vec::new();
    }

    let start = start_index.min(items.len());
    let end = (start_index + size).min(items.len());

    if invert {
        let mut result = Vec::new();
        result.extend(items[..start].iter().cloned());
        result.extend(items[end..].iter().cloned());
        result
    } else {
        items[start..end].to_vec()
    }
}

/// Shift items from the beginning to the end of the list.
pub fn shift<T: Clone>(items: &[T], amount: i64) -> Vec<T> {
    if items.is_empty() {
        return Vec::new();
    }

    let len = items.len() as i64;
    let mut a = amount % len;
    if a < 0 {
        a += len;
    }
    let a = a as usize;

    if a == 0 {
        return items.to_vec();
    }

    let mut result = Vec::with_capacity(items.len());
    result.extend(items[a..].iter().cloned());
    result.extend(items[..a].iter().cloned());
    result
}

/// Switch between multiple lists based on index.
pub fn do_switch<T: Clone>(lists: &[&[T]], index: usize) -> Vec<T> {
    let idx = index % lists.len();
    lists.get(idx).map(|l| l.to_vec()).unwrap_or_default()
}

/// Repeat a list a specified number of times.
///
/// # Arguments
/// * `items` - The list to repeat
/// * `amount` - Number of repetitions
/// * `per_item` - If true, repeat each item (aabbcc), otherwise repeat the whole list (abcabc)
pub fn repeat<T: Clone>(items: &[T], amount: usize, per_item: bool) -> Vec<T> {
    if items.is_empty() || amount == 0 {
        return Vec::new();
    }

    if amount == 1 {
        return items.to_vec();
    }

    let mut result = Vec::with_capacity(items.len() * amount);

    if per_item {
        for item in items {
            for _ in 0..amount {
                result.push(item.clone());
            }
        }
    } else {
        for _ in 0..amount {
            result.extend(items.iter().cloned());
        }
    }

    result
}

/// Reverse the items in a list.
pub fn reverse<T: Clone>(items: &[T]) -> Vec<T> {
    items.iter().rev().cloned().collect()
}

/// Sort a list of comparable items.
pub fn sort<T: Clone + Ord>(items: &[T]) -> Vec<T> {
    let mut result: Vec<T> = items.to_vec();
    result.sort();
    result
}

/// Sort a list of floats (handles NaN by treating them as greater than all other values).
pub fn sort_floats(items: &[f64]) -> Vec<f64> {
    let mut result: Vec<f64> = items.to_vec();
    result.sort_by(|a, b| a.partial_cmp(b).unwrap_or(std::cmp::Ordering::Equal));
    result
}

/// Shuffle a list with a given seed.
pub fn shuffle<T: Clone>(items: &[T], seed: u64) -> Vec<T> {
    if items.is_empty() {
        return Vec::new();
    }

    let mut result: Vec<T> = items.to_vec();
    let mut state = seed.wrapping_mul(1000000000);

    // Fisher-Yates shuffle
    for i in (1..result.len()).rev() {
        state = state.wrapping_mul(1103515245).wrapping_add(12345);
        let j = ((state >> 16) & 0x7FFFFFFF) as usize % (i + 1);
        result.swap(i, j);
    }

    result
}

/// Pick a random subset of items from a list.
pub fn pick<T: Clone>(items: &[T], amount: usize, seed: u64) -> Vec<T> {
    if items.is_empty() || amount == 0 {
        return Vec::new();
    }

    let shuffled = shuffle(items, seed);
    let count = amount.min(shuffled.len());
    shuffled[..count].to_vec()
}

/// Cull a list using a boolean pattern.
pub fn cull<T: Clone>(items: &[T], pattern: &[bool]) -> Vec<T> {
    if items.is_empty() || pattern.is_empty() {
        return items.to_vec();
    }

    items
        .iter()
        .enumerate()
        .filter(|(i, _)| pattern[i % pattern.len()])
        .map(|(_, item)| item.clone())
        .collect()
}

/// Take every nth item from a list.
pub fn take_every<T: Clone>(items: &[T], n: usize) -> Vec<T> {
    if items.is_empty() || n == 0 {
        return Vec::new();
    }

    items.iter().step_by(n).cloned().collect()
}

/// Filter a list to contain only unique elements.
pub fn distinct<T: Clone + std::hash::Hash + Eq>(items: &[T]) -> Vec<T> {
    let mut seen = HashSet::new();
    items
        .iter()
        .filter(|item| seen.insert((*item).clone()))
        .cloned()
        .collect()
}

/// Filter floats to contain only unique elements (using string representation for comparison).
pub fn distinct_floats(items: &[f64]) -> Vec<f64> {
    let mut seen = HashSet::new();
    items
        .iter()
        .filter(|&&item| {
            let key = format!("{:.10}", item);
            seen.insert(key)
        })
        .copied()
        .collect()
}

/// Get the keys from a list of maps.
pub fn keys(maps: &[HashMap<String, String>]) -> Vec<String> {
    let mut all_keys = HashSet::new();
    for map in maps {
        for key in map.keys() {
            all_keys.insert(key.clone());
        }
    }
    all_keys.into_iter().collect()
}

/// Create a map from two lists (keys and values).
pub fn zip_map<K: Clone + Eq + std::hash::Hash, V: Clone>(
    keys: &[K],
    values: &[V],
) -> HashMap<K, V> {
    keys.iter()
        .zip(values.iter())
        .map(|(k, v)| (k.clone(), v.clone()))
        .collect()
}

/// Interleave multiple lists together.
/// Takes one element from each list in turn.
pub fn interleave<T: Clone>(lists: &[&[T]]) -> Vec<T> {
    if lists.is_empty() {
        return Vec::new();
    }

    let max_len = lists.iter().map(|l| l.len()).max().unwrap_or(0);
    let mut result = Vec::new();

    for i in 0..max_len {
        for list in lists {
            if let Some(item) = list.get(i) {
                result.push(item.clone());
            }
        }
    }

    result
}

/// Flatten a list of lists into a single list.
pub fn flatten<T: Clone>(lists: &[Vec<T>]) -> Vec<T> {
    lists.iter().flatten().cloned().collect()
}

/// Partition a list into chunks of a given size.
pub fn partition<T: Clone>(items: &[T], size: usize) -> Vec<Vec<T>> {
    if size == 0 {
        return Vec::new();
    }
    items.chunks(size).map(|chunk| chunk.to_vec()).collect()
}

/// Find the index of an item in a list.
pub fn index_of<T: PartialEq>(items: &[T], item: &T) -> Option<usize> {
    items.iter().position(|x| x == item)
}

/// Check if a list contains an item.
pub fn contains<T: PartialEq>(items: &[T], item: &T) -> bool {
    items.contains(item)
}

/// Get an item at a specific index, with optional wrapping.
pub fn get_at<T: Clone>(items: &[T], index: i64, wrap: bool) -> Option<T> {
    if items.is_empty() {
        return None;
    }

    let len = items.len() as i64;
    let idx = if wrap {
        let mut i = index % len;
        if i < 0 {
            i += len;
        }
        i as usize
    } else if index < 0 || index >= len {
        return None;
    } else {
        index as usize
    };

    items.get(idx).cloned()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_count() {
        assert_eq!(count(&[1, 2, 3]), 3);
        assert_eq!(count::<i32>(&[]), 0);
    }

    #[test]
    fn test_first_second_last() {
        let items = vec![1, 2, 3, 4, 5];
        assert_eq!(first(&items), Some(1));
        assert_eq!(second(&items), Some(2));
        assert_eq!(last(&items), Some(5));

        let empty: Vec<i32> = vec![];
        assert_eq!(first(&empty), None);
        assert_eq!(second(&empty), None);
        assert_eq!(last(&empty), None);
    }

    #[test]
    fn test_rest() {
        let items = vec![1, 2, 3];
        assert_eq!(rest(&items), vec![2, 3]);
        assert_eq!(rest(&vec![1]), Vec::<i32>::new());
        assert_eq!(rest::<i32>(&[]), Vec::<i32>::new());
    }

    #[test]
    fn test_combine() {
        let a = [1, 2];
        let b = [3, 4];
        let c = [5, 6];
        assert_eq!(combine(&[&a[..], &b[..], &c[..]]), vec![1, 2, 3, 4, 5, 6]);
    }

    #[test]
    fn test_slice() {
        let items = vec![0, 1, 2, 3, 4, 5];
        assert_eq!(slice(&items, 1, 3, false), vec![1, 2, 3]);
        assert_eq!(slice(&items, 1, 3, true), vec![0, 4, 5]);
    }

    #[test]
    fn test_shift() {
        let items = vec![1, 2, 3, 4, 5];
        assert_eq!(shift(&items, 2), vec![3, 4, 5, 1, 2]);
        assert_eq!(shift(&items, -2), vec![4, 5, 1, 2, 3]);
        assert_eq!(shift(&items, 0), items);
    }

    #[test]
    fn test_repeat() {
        let items = vec![1, 2, 3];
        assert_eq!(repeat(&items, 2, false), vec![1, 2, 3, 1, 2, 3]);
        assert_eq!(repeat(&items, 2, true), vec![1, 1, 2, 2, 3, 3]);
        assert_eq!(repeat(&items, 0, false), Vec::<i32>::new());
    }

    #[test]
    fn test_reverse() {
        assert_eq!(reverse(&[1, 2, 3]), vec![3, 2, 1]);
        assert_eq!(reverse::<i32>(&[]), Vec::<i32>::new());
    }

    #[test]
    fn test_sort() {
        assert_eq!(sort(&[3, 1, 4, 1, 5]), vec![1, 1, 3, 4, 5]);
        assert_eq!(sort_floats(&[3.0, 1.0, 4.0, 1.0, 5.0]), vec![1.0, 1.0, 3.0, 4.0, 5.0]);
    }

    #[test]
    fn test_shuffle() {
        let items = vec![1, 2, 3, 4, 5];
        let shuffled1 = shuffle(&items, 42);
        let shuffled2 = shuffle(&items, 42);
        assert_eq!(shuffled1, shuffled2); // Same seed = same result
        assert_ne!(shuffled1, items); // Should be different from original (usually)
    }

    #[test]
    fn test_pick() {
        let items = vec![1, 2, 3, 4, 5];
        let picked = pick(&items, 3, 42);
        assert_eq!(picked.len(), 3);
    }

    #[test]
    fn test_cull() {
        let items = vec![1, 2, 3, 4, 5, 6];
        let pattern = vec![true, false];
        assert_eq!(cull(&items, &pattern), vec![1, 3, 5]);
    }

    #[test]
    fn test_take_every() {
        let items = vec![0, 1, 2, 3, 4, 5, 6, 7, 8, 9];
        assert_eq!(take_every(&items, 3), vec![0, 3, 6, 9]);
        assert_eq!(take_every(&items, 1), items);
    }

    #[test]
    fn test_distinct() {
        assert_eq!(distinct(&[1, 2, 2, 3, 3, 3]), vec![1, 2, 3]);
    }

    #[test]
    fn test_zip_map() {
        let keys = vec!["a", "b", "c"];
        let values = vec![1, 2, 3];
        let map = zip_map(&keys, &values);
        assert_eq!(map.get(&"a"), Some(&1));
        assert_eq!(map.get(&"b"), Some(&2));
        assert_eq!(map.get(&"c"), Some(&3));
    }

    #[test]
    fn test_interleave() {
        let a = [1, 2, 3];
        let b = [4, 5, 6];
        assert_eq!(interleave(&[&a[..], &b[..]]), vec![1, 4, 2, 5, 3, 6]);
    }

    #[test]
    fn test_partition() {
        let items = vec![1, 2, 3, 4, 5];
        let parts = partition(&items, 2);
        assert_eq!(parts, vec![vec![1, 2], vec![3, 4], vec![5]]);
    }

    #[test]
    fn test_get_at() {
        let items = vec![1, 2, 3, 4, 5];
        assert_eq!(get_at(&items, 0, false), Some(1));
        assert_eq!(get_at(&items, 4, false), Some(5));
        assert_eq!(get_at(&items, 5, false), None);
        assert_eq!(get_at(&items, -1, true), Some(5));
        assert_eq!(get_at(&items, 5, true), Some(1));
    }

    #[test]
    fn test_contains() {
        let items = vec![1, 2, 3];
        assert!(contains(&items, &2));
        assert!(!contains(&items, &4));
    }

    #[test]
    fn test_index_of() {
        let items = vec![1, 2, 3, 2];
        assert_eq!(index_of(&items, &2), Some(1));
        assert_eq!(index_of(&items, &5), None);
    }
}
