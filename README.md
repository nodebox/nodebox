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

NodeBox requires [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html), [Xcode](https://developer.apple.com/xcode/downloads/) and [Homebrew](http://brew.sh/) (for Ant and Maven)

NodeBox uses Ant and Maven to build a running version. Install these first:

    brew update
    brew install ant maven

Then from the Terminal, run:

    git clone https://github.com/nodebox/nodebox.git
    cd nodebox
    ant run

    # To create a full app (the build will be in dist/mac):
    ant dist-mac

## Building on Windows

- Install [Git](http://git-scm.com/).
- Install the [Java 8 SDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
- Install [Unicode NSIS](http://www.scratchpaper.com/).
- Install [Ant](http://ant.apache.org/).
- Install [Inno Setup (Unicode)](http://www.jrsoftware.org/isdl.php)

From the command prompt, run:

    # Setup the correct environment variables
    # (Modify these paths to your installation directories.)
    set JAVA_HOME=c:\java\jdk8
    set ANT_HOME=c:\java\ant
    set PATH=%PATH%;%ANT_HOME%\bin

    git clone git://github.com/nodebox/nodebox.git
    # Modify build.properties to point to the correct installation paths of the JRE and NSIS.

    cd nodebox
    ant run

    # To create a full app (the MSI will be in dist/windows/bundles):
    ant dist-win

## Building on Linux

### Ubuntu Linux

Or other distributions based on APT package system:

    sudo apt install git openjdk-8-jdk ant
    git clone https://github.com/nodebox/nodebox.git
    cd nodebox
    ant run

### Fedora Linux

Or other distributions based on YUM package system:

    sudo yum install git java-1.8.0-openjdk ant
    git clone https://github.com/nodebox/nodebox.git
    cd nodebox
    ant run

### Arch Linux
Nodebox has an aur package for distributions based on Arch linux : nodebox-git

    yaourt -S nodebox-git

or

    git clone https://aur.archlinux.org/nodebox-git.git
    cd nodebox-git
    makepkg
    sudo pacman -U nodebox-git-[version-number]-any.pkg.tar.xz

you can then launch nodebox as any desktop application, or by running the ```nodebox``` command on terminal

## Building on FreeBSD/PC-BSD

Just use pkg:

    sudo pkg install git openjdk-8 apache-ant
    git clone https://github.com/nodebox/nodebox.git
    cd nodebox
    ant run
