# Releasing your Project

Once you've created a project that you want to share with the world, you can _release_ it. Releasing creates a version
of a project that other people can use as a _dependency_.

Click the <button class="btn"><i class="fa fa-floppy-o"></i></button> _Releases_ button in the header
to bring up the _Release Manager_. Here you can see a list of released versions, as well as a form to create a new
version:

![Screenshot of the Release Manager](releases-screenshot.png)

## Creating a new release

Fill out the title, description and version number. All this information will be displayed in the
[Dependency Manager](guide:dependencies). Make it informative so other people can find it.

We use [semantic versioning](http://semver.org/) to indicate version numbers. In short, this means versions consists of
three numbers, separated by digits, like "1.0.5":

- The first number indicates the **major** version, used for making incompatible changes.
- The second number indicates the **minor** version, used for adding functionality in a backwards-compatible manner.
- The third number indicates the **patch** version, used for making backwards-compatible bug fixes.

When you're still in development, use a _major_ version of 0 to indicate things can still change, e.g. "0.1.0". It's
okay to have numbers higher than 9. In other words, "0.35.2" is a perfectly valid version number.

Press <button class="btn">New Release</button> to create a new version. Once a version is created it can no longer
be changed. This allows users to depend on the functionality of a specific version, with the guarantee that their
projects will keep working in the future.
