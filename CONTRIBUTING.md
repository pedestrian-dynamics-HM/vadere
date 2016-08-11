Contribution guidelines
=======================

Coding style guide
------------------

Basic rules:

 - No warnings
 - No unused imports
 - Use the formatting tool!
    - Eclipse: select text (optional) and press <kbd>ctrl</kbd> + <kbd>shift</kbd> + <kbd>f</kbd>
    - IntelliJ: select text (optional) and <kbd>ctrl</kbd> + <kbd>alt</kbd> + <kbd>l</kbd>

For source code formatting, we use an adapted version of
[Google's Coding style guide](https://google.github.io/styleguide/javaguide.html).
Please check the [README in this repository](https://gitlab.lrz.de/vadere/styleguide)
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
