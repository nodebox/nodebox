# Preparing Your Data for Visualization

Clean, well-organized data is essential for creating effective visualizations. That's why we separate the process in the tasks you need to do _before_ importing the data in NodeBox (for example, using Excel) and the tasks you can do _after_ importing the data in NodeBox.

## Before Importing your Data into NodeBox

Using a tool like Excel, Google Sheets, or a database, you can:

### Clean Your Data

- Remove duplicate entries to avoid skewing your visualization.
- Handle missing values by removing them or filling them with appropriate values. For example, you could fill missing dates with the average date.
- Fix formatting issues in text, dates, and numbers:
  - Make sure all casing is consistent, e.g. "USA" vs "usa".
  - Avoid extra whitespace or special characters, e.g. "New York" vs "New York` `".
  - Convert dates to a consistent format, e.g. "2021-01-01". We prefer [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) format, since it's unambiguous and can be sorted alphabetically.
  - Avoid decimal separators in numbers (e.g. `1,000,000`), since they will be treated as text. Use a period (`.`) for the fractional part.
- Check for and handle invalid data:
  - Values that are too high or too low, e.g. ages above 150 or below 0.
  - Values of the wrong type, e.g. the value "zero" in a numeric field.

### Ensure Consistent Categories

- Use consistent naming (e.g., "USA" vs "United States").
- Group similar categories if you have too many (e.g. anything above 10-12 distinct categories is too much). For example, instead of showing data for all countries, group them by continent.
- Fix any typos in category names.

### Match Units

- Convert all measurements to the same unit (e.g., meters or feet).
- Scale large numbers appropriately (K for thousands, M for millions).
- Document which units you're using.

### Manage Data Volume

Large datasets can slow down your visualization. To handle this:

- Take a (random) sample from your data if you have millions of rows. NodeBox works well with data sets up to 100,000 rows.
- Group data points (e.g., by day instead of by minute).
- Keep only the data you need for your visualization.

### Pre-aggregate Data

Aggregate your data before visualization:

- Calculate totals or averages for groups.
- Count occurrences for categories.
- Convert raw numbers to percentages.

### Prepare Headers

Headers should be:

- Clear and descriptive.
- Without spaces (use `first_name` instead of `First Name`).
- Consistently named across all columns.

## NodeBox as a Data Preparation Tool

If you want to use NodeBox for data preparation, we recommend creating two separate projects for better performance:

1. **Data Preparation Project**

   - Create a new project for data preparation
   - Clean and prepare your data
   - Export to CSV using the `Export CSV` functionality in the "table" tab.
   - Download the CSV file

2. **Visualization Project**

   - Create another project for visualization
   - Import your prepared CSV file
   - Create your visualization with the more compact data

This separation keeps your visualization project fast and easy to maintain.

## After Importing your Data into NodeBox

Once you've imported your data into NodeBox, you can further fine-tune it using the built-in nodes. You would do this if your visualisation is interactive and you need to filter, sort, or aggregate data on the fly:

1. Use the `Filter Data` node to select specific data, e.g. by year or category.
2. Use the `Sample Data` node to take out specific data, e.g. 100 random items, or the first quartile.
3. Use the `Transform Data` node to convert units or format text.
4. Use the `Aggregate Data` node to group and summarize your data.
5. Use the `Join Data` node to merge multiple datasets together.

Remember that well-prepared data is the foundation of good visualization. Taking the time to clean and organize your data will save you time and effort when creating your visualizations.
