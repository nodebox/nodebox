#!/usr/bin/env python

# Convert SVG artwork to PNG images.
# The SVG images are created as black artwork, so we also invert them.
# This script needs png2svg and ImageMagick to work.
# On Mac, using Homebrew, you can install these dependencies like this:
#
#     brew install png2svg imagemagick

from glob import glob
from os import system
import os.path

SOURCE_DIRECTORY = 'artwork'
TARGET_DIRECTORY = 'libraries'

LIBRARIES = ['corevector', 'coreimage', 'data', 'list', 'math']

def png_file(svg_file, target_directory):
    base, ext = os.path.splitext(os.path.basename(svg_file))
    png_file = '%s.png' % base
    return os.path.join(target_directory, png_file)

def convert_svg_file(svg_file, target_directory, overwrite=False):
    target_file = png_file(svg_file, target_directory)
    if not os.path.exists(target_file) or overwrite:
        print target_file
        os.system('svg2png %s %s' % (svg_file, target_file))
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
