var color = {};

color.color = function (c) {
    return c;
};

color.gray = function (gray, alpha, range) {
    range = Math.max(range, 1);
    return g.color(gray / range, gray / range, gray / range, alpha / range);
};

color.rgb = function (red, green, blue, alpha, range) {
    range = Math.max(range, 1);
    return g.color(red / range, green / range, blue / range, alpha / range);
};

color.hsb = function (hue, saturation, brightness, alpha, range) {
    range = Math.max(range, 1);
  return g.color(hue / range, saturation / range, brightness / range, alpha / range, { colorspace: g.HSB });
};
