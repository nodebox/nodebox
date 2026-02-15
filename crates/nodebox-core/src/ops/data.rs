//! Data operations for NodeBox.
//!
//! Functions for importing, creating, querying, and filtering tabular data.
//! Data is represented as rows of key-value pairs where values are either
//! strings or floating-point numbers.

use std::collections::HashMap;

/// A value within a data row.
///
/// Matches Java's DataFunctions behavior where CSV values are either
/// String or Double (auto-detected per column).
#[derive(Clone, Debug, PartialEq)]
pub enum DataValue {
    String(String),
    Float(f64),
}

impl DataValue {
    /// Returns the value as a string representation.
    pub fn as_string(&self) -> String {
        match self {
            DataValue::String(s) => s.clone(),
            DataValue::Float(f) => format!("{}", f),
        }
    }

    /// Tries to extract a float value, parsing from string if needed.
    pub fn as_float(&self) -> Option<f64> {
        match self {
            DataValue::Float(f) => Some(*f),
            DataValue::String(s) => s.parse::<f64>().ok(),
        }
    }
}

/// Import CSV content into a list of data rows.
///
/// - First row is treated as headers.
/// - Empty headers become "Column N".
/// - Per-column numeric detection: if every non-empty value parses as f64,
///   the column is stored as `DataValue::Float`; otherwise `DataValue::String`.
/// - `number_separator`: if `"comma"`, commas in values are replaced with periods
///   before parsing as numbers.
pub fn import_csv(
    content: &str,
    delimiter: u8,
    quote_char: u8,
    number_separator: &str,
) -> Vec<HashMap<String, DataValue>> {
    let mut reader = csv::ReaderBuilder::new()
        .delimiter(delimiter)
        .quote(quote_char)
        .has_headers(true)
        .flexible(true)
        .from_reader(content.as_bytes());

    // Read headers
    let headers: Vec<String> = match reader.headers() {
        Ok(record) => record
            .iter()
            .enumerate()
            .map(|(i, h)| {
                let trimmed = h.trim().to_string();
                if trimmed.is_empty() {
                    format!("Column {}", i + 1)
                } else {
                    trimmed
                }
            })
            .collect(),
        Err(_) => return Vec::new(),
    };

    if headers.is_empty() {
        return Vec::new();
    }

    // Read all string records first
    let mut raw_rows: Vec<Vec<String>> = Vec::new();
    for result in reader.records() {
        match result {
            Ok(record) => {
                let row: Vec<String> = record.iter().map(|s| s.trim().to_string()).collect();
                raw_rows.push(row);
            }
            Err(_) => continue,
        }
    }

    // Detect numeric columns: a column is numeric if every non-empty value parses as f64
    let num_cols = headers.len();
    let mut is_numeric = vec![true; num_cols];

    for row in &raw_rows {
        for (col, value) in row.iter().enumerate() {
            if col >= num_cols {
                break;
            }
            if value.is_empty() {
                continue;
            }
            let parse_value = if number_separator == "comma" {
                value.replace(',', ".")
            } else {
                value.clone()
            };
            if parse_value.parse::<f64>().is_err() {
                is_numeric[col] = false;
            }
        }
    }

    // Build data rows
    let mut result = Vec::with_capacity(raw_rows.len());
    for row in &raw_rows {
        let mut data_row = HashMap::new();
        for (col, header) in headers.iter().enumerate() {
            let raw = row.get(col).map(|s| s.as_str()).unwrap_or("");
            let value = if is_numeric[col] && !raw.is_empty() {
                let parse_value = if number_separator == "comma" {
                    raw.replace(',', ".")
                } else {
                    raw.to_string()
                };
                match parse_value.parse::<f64>() {
                    Ok(f) => DataValue::Float(f),
                    Err(_) => DataValue::String(raw.to_string()),
                }
            } else {
                DataValue::String(raw.to_string())
            };
            data_row.insert(header.clone(), value);
        }
        result.push(data_row);
    }

    result
}

/// Build a data table from header names and input lists.
///
/// - `headers`: column names (split by the caller, e.g. on `;` or `,`)
/// - `lists`: one list per column; only the first `headers.len()` lists are used
/// - Row count = max list length across non-empty lists
/// - Shorter lists pad with `DataValue::String("")`
pub fn make_table(
    headers: &[String],
    lists: &[Vec<DataValue>],
) -> Vec<HashMap<String, DataValue>> {
    if headers.is_empty() {
        return Vec::new();
    }

    // Only use as many lists as there are headers
    let num_cols = headers.len().min(lists.len());
    if num_cols == 0 {
        return Vec::new();
    }

    // Find max row count
    let max_rows = lists[..num_cols]
        .iter()
        .map(|l| l.len())
        .max()
        .unwrap_or(0);

    if max_rows == 0 {
        return Vec::new();
    }

    let mut result = Vec::with_capacity(max_rows);
    for row_idx in 0..max_rows {
        let mut row = HashMap::new();
        for col_idx in 0..num_cols {
            let value = lists[col_idx]
                .get(row_idx)
                .cloned()
                .unwrap_or_else(|| DataValue::String(String::new()));
            row.insert(headers[col_idx].clone(), value);
        }
        result.push(row);
    }

    result
}

/// Look up a value by key in a data row.
pub fn lookup(row: &HashMap<String, DataValue>, key: &str) -> Option<DataValue> {
    row.get(key).cloned()
}

/// Filter data rows by comparing a key's value against a target.
///
/// - Tries numeric comparison first (if both the row value and target parse as f64)
/// - Falls back to string comparison for `=` and `!=`
/// - Non-string comparisons (`>`, `<`, etc.) return false for non-numeric values
/// - Rows missing the key are excluded (except for `!=` which includes them)
pub fn filter_data(
    rows: &[HashMap<String, DataValue>],
    key: &str,
    op: &str,
    value: &str,
) -> Vec<HashMap<String, DataValue>> {
    let target_float = value.parse::<f64>().ok();

    rows.iter()
        .filter(|row| {
            match row.get(key) {
                None => op == "!=", // Missing key: only != matches
                Some(data_val) => {
                    // Try numeric comparison
                    if let Some(target_f) = target_float {
                        if let Some(row_f) = data_val.as_float() {
                            return match op {
                                "=" => (row_f - target_f).abs() < f64::EPSILON,
                                "!=" => (row_f - target_f).abs() >= f64::EPSILON,
                                ">" => row_f > target_f,
                                ">=" => row_f >= target_f,
                                "<" => row_f < target_f,
                                "<=" => row_f <= target_f,
                                _ => false,
                            };
                        }
                    }

                    // String comparison
                    let row_str = data_val.as_string();
                    match op {
                        "=" => row_str == value,
                        "!=" => row_str != value,
                        // String ordering comparisons
                        ">" => row_str.as_str() > value,
                        ">=" => row_str.as_str() >= value,
                        "<" => (row_str.as_str()) < value,
                        "<=" => row_str.as_str() <= value,
                        _ => false,
                    }
                }
            }
        })
        .cloned()
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_import_csv_basic() {
        let csv = "name,age,city\nAlice,30,NYC\nBob,25,LA\n";
        let rows = import_csv(csv, b',', b'"', "period");
        assert_eq!(rows.len(), 2);
        assert_eq!(rows[0].get("name"), Some(&DataValue::String("Alice".to_string())));
        assert_eq!(rows[0].get("age"), Some(&DataValue::Float(30.0)));
        assert_eq!(rows[0].get("city"), Some(&DataValue::String("NYC".to_string())));
        assert_eq!(rows[1].get("name"), Some(&DataValue::String("Bob".to_string())));
        assert_eq!(rows[1].get("age"), Some(&DataValue::Float(25.0)));
    }

    #[test]
    fn test_import_csv_semicolon_delimiter() {
        let csv = "name;score\nAlice;9.5\nBob;8.0\n";
        let rows = import_csv(csv, b';', b'"', "period");
        assert_eq!(rows.len(), 2);
        assert_eq!(rows[0].get("score"), Some(&DataValue::Float(9.5)));
    }

    #[test]
    fn test_import_csv_comma_number_separator() {
        let csv = "name;price\nWidget;1,50\nGadget;2,75\n";
        let rows = import_csv(csv, b';', b'"', "comma");
        assert_eq!(rows.len(), 2);
        assert_eq!(rows[0].get("price"), Some(&DataValue::Float(1.5)));
        assert_eq!(rows[1].get("price"), Some(&DataValue::Float(2.75)));
    }

    #[test]
    fn test_import_csv_empty_headers() {
        let csv = "name,,city\nAlice,30,NYC\n";
        let rows = import_csv(csv, b',', b'"', "period");
        assert_eq!(rows.len(), 1);
        assert!(rows[0].contains_key("Column 2"));
    }

    #[test]
    fn test_import_csv_quoted_fields() {
        let csv = "name,description\nAlice,\"Hello, World\"\nBob,\"Line1\"\n";
        let rows = import_csv(csv, b',', b'"', "period");
        assert_eq!(rows.len(), 2);
        assert_eq!(
            rows[0].get("description"),
            Some(&DataValue::String("Hello, World".to_string()))
        );
    }

    #[test]
    fn test_import_csv_empty_content() {
        let rows = import_csv("", b',', b'"', "period");
        assert!(rows.is_empty());
    }

    #[test]
    fn test_import_csv_mixed_numeric_column() {
        let csv = "name,value\nAlice,10\nBob,N/A\n";
        let rows = import_csv(csv, b',', b'"', "period");
        // "value" column has a non-numeric entry, so all values should be strings
        assert_eq!(rows[0].get("value"), Some(&DataValue::String("10".to_string())));
        assert_eq!(rows[1].get("value"), Some(&DataValue::String("N/A".to_string())));
    }

    #[test]
    fn test_make_table_basic() {
        let headers = vec!["x".to_string(), "y".to_string()];
        let list1 = vec![DataValue::Float(1.0), DataValue::Float(2.0)];
        let list2 = vec![DataValue::Float(10.0), DataValue::Float(20.0)];
        let rows = make_table(&headers, &[list1, list2]);
        assert_eq!(rows.len(), 2);
        assert_eq!(rows[0].get("x"), Some(&DataValue::Float(1.0)));
        assert_eq!(rows[0].get("y"), Some(&DataValue::Float(10.0)));
        assert_eq!(rows[1].get("x"), Some(&DataValue::Float(2.0)));
        assert_eq!(rows[1].get("y"), Some(&DataValue::Float(20.0)));
    }

    #[test]
    fn test_make_table_uneven_lists() {
        let headers = vec!["a".to_string(), "b".to_string()];
        let list1 = vec![DataValue::Float(1.0), DataValue::Float(2.0), DataValue::Float(3.0)];
        let list2 = vec![DataValue::String("x".to_string())];
        let rows = make_table(&headers, &[list1, list2]);
        assert_eq!(rows.len(), 3);
        assert_eq!(rows[2].get("b"), Some(&DataValue::String(String::new())));
    }

    #[test]
    fn test_make_table_empty_headers() {
        let rows = make_table(&[], &[vec![DataValue::Float(1.0)]]);
        assert!(rows.is_empty());
    }

    #[test]
    fn test_make_table_more_lists_than_headers() {
        let headers = vec!["x".to_string()];
        let list1 = vec![DataValue::Float(1.0)];
        let list2 = vec![DataValue::Float(2.0)]; // Should be ignored
        let rows = make_table(&headers, &[list1, list2]);
        assert_eq!(rows.len(), 1);
        assert_eq!(rows[0].len(), 1); // Only "x" column
    }

    #[test]
    fn test_lookup_existing_key() {
        let mut row = HashMap::new();
        row.insert("name".to_string(), DataValue::String("Alice".to_string()));
        row.insert("age".to_string(), DataValue::Float(30.0));
        assert_eq!(lookup(&row, "name"), Some(DataValue::String("Alice".to_string())));
        assert_eq!(lookup(&row, "age"), Some(DataValue::Float(30.0)));
    }

    #[test]
    fn test_lookup_missing_key() {
        let row = HashMap::new();
        assert_eq!(lookup(&row, "missing"), None);
    }

    #[test]
    fn test_filter_data_equals() {
        let rows = vec![
            {
                let mut r = HashMap::new();
                r.insert("name".to_string(), DataValue::String("Alice".to_string()));
                r.insert("age".to_string(), DataValue::Float(30.0));
                r
            },
            {
                let mut r = HashMap::new();
                r.insert("name".to_string(), DataValue::String("Bob".to_string()));
                r.insert("age".to_string(), DataValue::Float(25.0));
                r
            },
        ];

        let result = filter_data(&rows, "name", "=", "Alice");
        assert_eq!(result.len(), 1);
        assert_eq!(result[0].get("name"), Some(&DataValue::String("Alice".to_string())));
    }

    #[test]
    fn test_filter_data_numeric_greater() {
        let rows = vec![
            {
                let mut r = HashMap::new();
                r.insert("score".to_string(), DataValue::Float(90.0));
                r
            },
            {
                let mut r = HashMap::new();
                r.insert("score".to_string(), DataValue::Float(70.0));
                r
            },
            {
                let mut r = HashMap::new();
                r.insert("score".to_string(), DataValue::Float(85.0));
                r
            },
        ];

        let result = filter_data(&rows, "score", ">", "80");
        assert_eq!(result.len(), 2);
    }

    #[test]
    fn test_filter_data_not_equals() {
        let rows = vec![
            {
                let mut r = HashMap::new();
                r.insert("status".to_string(), DataValue::String("active".to_string()));
                r
            },
            {
                let mut r = HashMap::new();
                r.insert("status".to_string(), DataValue::String("inactive".to_string()));
                r
            },
        ];

        let result = filter_data(&rows, "status", "!=", "active");
        assert_eq!(result.len(), 1);
        assert_eq!(result[0].get("status"), Some(&DataValue::String("inactive".to_string())));
    }

    #[test]
    fn test_filter_data_missing_key() {
        let rows = vec![
            {
                let mut r = HashMap::new();
                r.insert("name".to_string(), DataValue::String("Alice".to_string()));
                r
            },
        ];

        // Missing key with != should include the row
        let result = filter_data(&rows, "missing", "!=", "anything");
        assert_eq!(result.len(), 1);

        // Missing key with = should exclude the row
        let result = filter_data(&rows, "missing", "=", "anything");
        assert_eq!(result.len(), 0);
    }

    #[test]
    fn test_data_value_as_string() {
        assert_eq!(DataValue::String("hello".into()).as_string(), "hello");
        assert_eq!(DataValue::Float(3.14).as_string(), "3.14");
    }

    #[test]
    fn test_data_value_as_float() {
        assert_eq!(DataValue::Float(3.14).as_float(), Some(3.14));
        assert_eq!(DataValue::String("2.5".into()).as_float(), Some(2.5));
        assert_eq!(DataValue::String("hello".into()).as_float(), None);
    }
}
