Contribution guidelines
=======================

Workflow
--------

To efficiently contribute to this project, you need an LRZ GitLab account.
Please contact us and we will send you an invitation.
The workflow then is the following.

 1. **Fork this Git repository**
 2. Clone your own fork to your computer
 3. Checkout a new branch and work on your new feature or bugfix
 4. Push your branch and **send us a merge request**

These steps are explained in more detail at the
[GitHub help pages](https://help.github.com/articles/fork-a-repo/).
Merge/pull requests are described [on GitLab](https://about.gitlab.com/2014/09/29/gitlab-flow/#mergepull-requests-with-gitlab-flow).

Coding style guide
------------------

Basic rules:

 - No warnings
 - No unused imports
 - No unecessary `this.` qualifiers
 - Use the formatting tool!
    - Eclipse: select text (optional) and press <kbd>ctrl</kbd> + <kbd>shift</kbd> + <kbd>f</kbd>
    - IntelliJ: select text (optional) and <kbd>ctrl</kbd> + <kbd>alt</kbd> + <kbd>l</kbd>

For source code formatting, we use an adapted version of
[Google's Coding style guide](https://google.github.io/styleguide/javaguide.html).
Please check the [README in this repository](/vadere/styleguide)
for the style guide and for how to import the style settings into your IDE.

Commit style guide
------------------

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

Miscellaneous
-------------

### Author tag in JavaDoc

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

### Tests required

Especially if you implement new functionality, please also provide JUnit tests.
The test classes should be located in the `tests/` folder but in the same
package as the class under test.
