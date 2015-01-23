NodeBox 3
=========
NodeBox is a new software application for creating generative art using procedural graphics and a new way to approach graphic design.

Highlights:

- Node based -- the software uses a non-destructive workflow where every operation is represented by a visual block of code.
- Open to extend -- look and change the source of every code block.
- Python or Clojure -- Nodes can be written in popular dynamic programming languages.

For downloads, documentation and the forum, visit the website:

<http://nodebox.net/>

[![Build Status](https://secure.travis-ci.org/nodebox/nodebox.png)](http://travis-ci.org/nodebox/nodebox)

## Building on Mac

If you're on Mac OS X Lion or higher, Git is already installed. For older versions, install Git first (<http://git-scm.com/>).

Then from the Terminal, run:

    git clone git://github.com/nodebox/nodebox.git
    cd nodebox
    ant run

    # To create a full app (the build will be in dist/mac):
    ant dist-mac

## Building on Windows

- Install [Git](<http://git-scm.com/>).
- Install the [Java SDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
- Install [Unicode NSIS](http://www.scratchpaper.com/).
- Install [Ant](http://ant.apache.org/).

From the command prompt, run:

    # Setup the correct environment variables
    set PATH=%PATH%;c:\java\apache-ant-1.8.2\bin
    set JAVA_HOME=c:\java\jdk6

    git clone git://github.com/nodebox/nodebox.git
    # Modify build.properties to point to the correct installation paths of the JRE and NSIS.

    cd nodebox
    ant run

    # To create a full app (the EXE will be in dist):
    ant dist-win

## Building on Linux

### Ubuntu Linux

Or other distributions based on APT package system:

    sudo apt-get install git-core openjdk-6-jdk ant
    git clone git://github.com/nodebox/nodebox.git
    cd nodebox
    ant run

### Fedora Linux

Or other distributions based on YUM package system:

    sudo yum install git java-1.8.0-openjdk ant
    git clone git://github.com/nodebox/nodebox.git
    cd nodebox
    ant run

## Building on FreeBSD/PC-BSD

Just use pkg:

    sudo pkg install git openjdk-7 apache-ant
    git clone git://github.com/nodebox/nodebox.git
    cd nodebox
    ant run
