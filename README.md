NodeBox 3
=========
NodeBox is a new software application for creating generative art using procedural graphics and a new way to approach graphic design.

Highlights:

- Node based -- the software uses a non-destructive workflow where every operation is represented by a visual block of code.
- Open to extend -- look and change the source of every code block.
- Python or Clojure -- Nodes can be written in popular dynamic programming languages.

For downloads, documentation and the forum, visit the website:

<https://nodebox.net/>

[![Build Status](https://secure.travis-ci.org/nodebox/nodebox.png)](http://travis-ci.org/nodebox/nodebox)

## Building on Mac

NodeBox requires the [Java JDK](http://jdk.java.net/15/), and [Homebrew](http://brew.sh/) (for Ant and Maven)

NodeBox uses Ant and Maven to build a running version. Install these first:

```shell
brew install ant maven
```

Then from the Terminal, run:

```shell
git clone https://github.com/nodebox/nodebox.git
cd nodebox
ant run

# To create a full app (the build will be in dist/mac):
ant dist-mac
```

## Building on Windows

- Install [Git](http://git-scm.com/).
- Install a recent [Java SDK](http://openjdk.java.net/).
- Install [Ant](http://ant.apache.org/).
- Install [Wix Toolset](https://wixtoolset.org/).

From the command prompt, run:

```shell
# Setup the correct environment variables
# (Modify these paths to your installation directories.)
set JAVA_HOME=c:\java\jdk
set ANT_HOME=c:\java\ant
set WIX_HOME=c:\java\wix
set PATH=%PATH%;%ANT_HOME%\bin;%WIX_HOME%\bin

git clone https://github.com/nodebox/nodebox.git

cd nodebox
ant run

# To create a full app (the MSI will be in dist/windows):
# Set the correct version in src/main/resources/version.properties
ant dist-win
```

## Building on Linux

### Ubuntu Linux

Or other distributions based on APT package system:

```shell
sudo apt install git openjdk-11-jdk ant
git clone https://github.com/nodebox/nodebox.git
cd nodebox
ant run
```

### Fedora Linux

Or other distributions based on YUM package system:

```shell
sudo yum install git java-11-openjdk ant
git clone https://github.com/nodebox/nodebox.git
cd nodebox
ant run
```

### Arch Linux
Nodebox has an aur package for distributions based on Arch linux : nodebox-git

```shell
yaourt -S nodebox-git
```

or

```shell
git clone https://aur.archlinux.org/nodebox-git.git
cd nodebox-git
makepkg
sudo pacman -U nodebox-git-[version-number]-any.pkg.tar.xz
```

You can then launch nodebox as any desktop application, or by running the ```nodebox``` command on terminal.

## Building on FreeBSD/PC-BSD

Just use pkg:

```shell
sudo pkg install git openjdk-11 apache-ant
git clone https://github.com/nodebox/nodebox.git
cd nodebox
ant run
```