# Contributing: Vadere

This guide explains the repository structure, how to set up the development environment and introduces coding guidelines.

## Repository Structure

The repository contains following `files` and `folders`:

- The Vadere source code: divided into the sofware modules `VadereGui`, `VadereMeshing`, `VaderSimulator`, `VadereState`, `VadereUtils`
- `Scenarios`: pre-shipped tests for different purposes and different locomotion models (e.g., gradient navigation model, optimal steps model and the social force model)
- `Tools`: scripts which are executing during the continuous integration phase.
- `.gitlab`: templates for creating issues in the Vadere [issue tracker](https://gitlab.lrz.de/vadere/vadere/issues) (this files are implicitly used by GitLab).
- `.gitlab-ci.yml`: instructions which are executed during the [continuous integration pipeline](https://docs.gitlab.com/ee/ci/quick_start/).

## Development Setup

1. Follow the **installation instructions** in the [README.md](README.md) to install all required software and to get the source code.
2. Open a shell and `cd` into the project directory.
3. Run `mvn clean install`.

The project can now be imported *As Maven Project* into your IDE.

### Eclipse

1. *File* > *Import* > *Maven* > *Existing Maven Projects*
2. Choose `pom.xml` as *Root Directory* and click *Finish*
3. Open *Vaderegui (gui)* > *src* > *org.vadere.gui.projectview* > `Vadereapplication`

### IntelliJ IDEA

1. On the welcome-screen select *Import Project*
2. Select `pom.xml` > *Next* > *Next* > *Next* > *Finish*
3. Open *VadereGui (gui)* > *src* > *org.vadere.gui.projectview* > `VadereApplication`
4. lick the *run*-icon next to the `main` method
5. Edit the run configuration for `VadereApplication` to build the project using Maven instead of IntelliJ's internal builder to avoid compilation errors:
6. Click *Edit Configurations* (in dropdown menu next to the play/debug button)
7. Under *Before launch*, **remove all existing** build instructions and add *Run Maven Goal* and use the Maven goal `compile`

Alternatively, run `mvn eclipse:eclipse` using the [Maven Eclipse Plugin](http://maven.apache.org/plugins/maven-eclipse-plugin/usage.html) or `mvn idea:idea` using the [Maven IntelliJ Plugin](http://maven.apache.org/plugins/maven-idea-plugin/).

## Workflow

To efficiently contribute to this project, you need an LRZ GitLab account.
Please contact us and we will send you an invitation.

### Use the Issue Tracker

Please, use the [issue tracker](https://gitlab.lrz.de/vadere/vadere/issues?sort=label_priority) for both

- to request a feature or to report a bug (see [how to write new issues](https://gitlab.lrz.de/vadere/vadere/issues/179))
- to work on a feature (see [how to work on an issue](https://gitlab.lrz.de/vadere/vadere/issues/184))

**Tip:** Sort the issues in the [issue tracker](https://gitlab.lrz.de/vadere/vadere/issues?sort=label_priority) by `Label priority`.

### Steps for External Contributors

The workflow is the following:

1. **Fork this Git repository**
2. Clone your own fork to your computer
3. Checkout a new branch and work on your new feature or bugfix
4. Push your branch and **send us a merge request**

These steps are explained in more detail at the
[GitHub help pages](https://help.github.com/articles/fork-a-repo/).
Merge/pull requests are described [on GitLab](https://about.gitlab.com/2014/09/29/gitlab-flow/#mergepull-requests-with-gitlab-flow).

## Style Guides

### For Coding

Basic rules:

- No warnings
- No unused imports
- No unecessary `this.` qualifiers
- Use the formatting tool!
  - Eclipse: select text (optional) and press <kbd>ctrl</kbd> + <kbd>shift</kbd> + <kbd>f</kbd>
  - IntelliJ: select text (optional) and <kbd>ctrl</kbd> + <kbd>alt</kbd> + <kbd>l</kbd>

For source code formatting, we use an adapted version of
[Google's Coding style guide](https://google.github.io/styleguide/javaguide.html).
Please check the [README in this repository](https://gitlab.lrz.de/vadere/styleguide)
for the style guide and for how to import the style settings into your IDE.

### For Commit Messages

These are examples for a good commit messages:

```
Fix typo in introduction to user guide
```

```
Refactor model initialization

Extracting a new class ModelBuilder because this functionality might be
used in multiple places.
```

Rules:

1. Separate subject from body with a blank line
2. Limit the subject line to 50 characters
3. Capitalize the subject line
4. Do not end the subject line with a period
5. Use the imperative mood in the subject line
6. Wrap the body at 72 characters
7. Use the body to explain what and why vs. how

Source: http://chris.beams.io/posts/git-commit/

This article lists good reasons for each rule!
Reasons include:

 - Clean and consistent git-log that also fits in the GitLab user interface
 - Stick to Git standards, e.g. "Merge branch ..." and "Revert ..."

Rules 1, 3, and 4 never hurt and should always be applied.

### Miscellaneous

#### Author Tag in JavaDoc

If you make important contributions to a Java class, and especially if you feel
responsible for that class, please add yourself as an author to the class-level
JavaDoc.

Example:

```
/**
 * @author First Last
 * @author Given Sur
 */
public class Foo {
    ...
```

#### Tests Required

Especially if you implement new functionality, please also provide JUnit tests.
The test classes should be located in the `tests/` folder but in the same
package as the class under test.

## Contributors

People who have contributed code to the project at the Munich University of Applied Sciences (in alphabetical order):

Florian Albrecht, Benjamin Degenhart, Felix Dietrich, Marion Gödel, Lukas Gradl, Aleksander Invanov, Benedikt Kleinmeier, Daniel Lehmberg, Christina Mayr, Simon Rahn, Jakob Schöttl, Stefan Schuhbäck, Michael Seitz, Swen Stemmer, Isabella von Sivers, Mario Teixeira Parente, Peter Zarnitz, Benedikt Zönnchen,

External Contributors:
Mina Abadeer (Uni Münster, abadeer@uni-muenster.de)
