//! Quick demo of NodeBox Rust core functionality.

use nodebox_core::geometry::{Path, Point, Color, font};

fn main() {
    println!("=== NodeBox Rust Core Demo ===\n");

    // 1. Create an ellipse and get points along it
    println!("1. Ellipse with bezier operations:");
    let ellipse = Path::ellipse(0.0, 0.0, 100.0, 80.0);
    println!("   Ellipse circumference: {:.2}", ellipse.length());

    let points = ellipse.make_points(8);
    println!("   8 points around ellipse:");
    for (i, p) in points.iter().enumerate() {
        println!("      [{i}] ({:.1}, {:.1})", p.x, p.y);
    }

    // 2. Create a line and sample it
    println!("\n2. Line sampling:");
    let line = Path::line(0.0, 0.0, 100.0, 50.0);
    println!("   Line from (0,0) to (100,50)");
    println!("   Length: {:.2}", line.length());
    println!("   Point at t=0.0: {:?}", line.point_at(0.0));
    println!("   Point at t=0.5: {:?}", line.point_at(0.5));
    println!("   Point at t=1.0: {:?}", line.point_at(1.0));

    // 3. Resample a rectangle
    println!("\n3. Resampling:");
    let rect = Path::rect(0.0, 0.0, 100.0, 100.0);
    println!("   Rectangle perimeter: {:.2}", rect.length());
    let resampled = rect.resample_by_amount(16);
    println!("   Resampled to 16 points: {} contours, {} points",
        resampled.contours.len(),
        resampled.contours.iter().map(|c| c.len()).sum::<usize>()
    );

    // 4. Text to path (if fonts available)
    println!("\n4. Text to path:");
    match font::text_to_path("Hello", "sans-serif", 72.0, Point::new(0.0, 100.0)) {
        Ok(path) => {
            let bounds = path.bounds().unwrap();
            println!("   'Hello' at 72pt:");
            println!("   Bounds: ({:.1}, {:.1}) - size: {:.1} x {:.1}",
                bounds.x, bounds.y, bounds.width, bounds.height);
            println!("   Total contours: {}", path.contours.len());
            println!("   Path length: {:.2}", path.length());
        }
        Err(e) => println!("   Font error: {}", e),
    }

    // 5. List available fonts
    println!("\n5. Available font families (first 10):");
    let families = font::list_font_families();
    for family in families.iter().take(10) {
        println!("   - {}", family);
    }
    if families.len() > 10 {
        println!("   ... and {} more", families.len() - 10);
    }

    println!("\n=== Demo complete! ===");
}
