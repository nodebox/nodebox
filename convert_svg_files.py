#!/usr/bin/env python

# Convert SVG artwork to PNG images.
# The SVG images are created as black artwork, so we also invert them.
# This script needs png2svg (or Inkscape if you're not using Mac) and ImageMagick to work.
#
# To run this on Mac OS X 10.8, install XQuartz first:
#
#     http://xquartz.macosforge.org/
#
# Install the Homebrew package manager:
#
#     http://mxcl.github.com/homebrew/
#
# Then install the dependencies like this:
#
#     brew install svg2png imagemagick libtool

from glob import glob
from os import system
import os.path
import sys

SOURCE_DIRECTORY = 'artwork'
TARGET_DIRECTORY = 'libraries'

LIBRARIES = ['color', 'core', 'corevector', 'data', 'device', 'list', 'math', 'string']

if sys.platform == 'darwin':
    SVG2PNG_COMMAND = 'svg2png -w 52 -h 52 %s %s'
else:
    SVG2PNG_COMMAND = "inkscape -w 52 -h 52 -f %s -e %s"

def png_file(svg_file, target_directory):
    base, ext = os.path.splitext(os.path.basename(svg_file))
    png_file = '%s.png' % base
    return os.path.join(target_directory, png_file)

def convert_svg_file(svg_file, target_directory, overwrite=False):
    target_file = png_file(svg_file, target_directory)
    if not os.path.exists(target_file) or overwrite:
        print target_file
        os.system(SVG2PNG_COMMAND % (svg_file, target_file))
        os.system('mogrify -negate %s' % target_file)
    
def convert_directory(source_directory, target_directory):
    svg_files = glob(os.path.join(source_directory, '*.svg'))
    for svg_file in svg_files:
        convert_svg_file(svg_file, target_directory)

def convert_library(library):
    source_dir = os.path.join(SOURCE_DIRECTORY, library)
    target_dir = os.path.join(TARGET_DIRECTORY, library)
    convert_directory(source_dir, target_dir)
    
if __name__=='__main__':
    for library in LIBRARIES:
        convert_library(library)
