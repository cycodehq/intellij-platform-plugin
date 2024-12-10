<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Cycode plugin for IntelliJ Platform

## [Unreleased]

## [2.2.0] - 2024-12-10

- Add AI remediations for IaC and SAST
- Fix "Path to executable" field applying in the settings

## [2.1.0] - 2024-10-07

- Add sync flow for Secrets and IaC

## [2.0.1] - 2024-09-25

- Fix empty IaC scan results on Windows
- Fix missed markdown blocks on violation cards

## [2.0.0] - 2024-08-20

- Add support for IDEs 2024.2
- Drop support for IDEs 2021 and 2022

## [1.9.5] - 2024-07-30

- Fix UI tree component loading in new IDE versions

## [1.9.4] - 2024-07-25

- Disable Sentry for on-premise installations
- Fix deserialization errors
- Fix auth check

## [1.9.3] - 2024-07-15

- Disable unhandled exceptions logging

## [1.9.2] - 2024-07-15

- Integrate Sentry

## [1.9.1] - 2024-06-25

- Remove forgotten "coming soon" from SAST

## [1.9.0] - 2024-06-24

- Add SAST support

## [1.8.0] - 2024-05-23

- Add "Ignore this violation" for violation card of secrets
- Make CWE and CVE clickable on violation cards
- Leave "Open violation card" as only one quick fix action

## [1.7.0] - 2024-05-15

- Add experimental SAST support
- Add intention actions to open violation card
- Add policy display name as the title of SAST detections
- Improve UX of violation cards by clarifying fields
- Improve UX of tree view by using relative paths

## [1.6.0] - 2024-04-16

- Add Infrastructure as Code (IaC) support
- Add Secrets Violation Card
- Add IaC Violation Card
- Add icons for file nodes in the tree view
- Add a full path of file nodes in the tree view
- Fix filter by info severity

## [1.5.0] - 2024-03-13

- Add SCA Violation Card
- Add action toolbar to the Cycode tab
- Add the on-demand scan of an entire project for Secrets

## [1.4.0] - 2024-02-28

- Add Tree View
- Improve loading tab

## [1.3.1] - 2024-02-13

- Fix CLI upgrading when the cache of remote checksums is not expired

## [1.3.0] - 2024-02-13

- Add new SCA flow which decreases execution time

## [1.2.0] - 2024-02-01

- Add Open-source Threats (SCA) support

## [1.1.5] - 2024-02-01

- Fix external annotator for registered languages
- Fix performing of intention action on hover

## [1.1.4] - 2024-01-31

- Fix scan results sharing across projects

## [1.1.3] - 2024-01-30

- Fix work with many opened projects

## [1.1.2] - 2024-01-18

- Fix performance on macOS
- Fix performance of quick fix actions

## [1.1.1] - 2023-12-21

- Fix working on Windows

## [1.1.0] - 2023-12-13

- Add Company Guidelines
- Fix the severity of detected secrets

## [1.0.1] - 2023-12-12

- Fix the creation of many scan jobs on many on-save events for a short period of time

## [1.0.0] - 2023-12-05

The first public release of the plugin.

[2.2.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v2.2.0

[2.1.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v2.1.0

[2.0.1]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v2.0.1 

[2.0.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v2.0.0

[1.9.5]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.9.5

[1.9.4]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.9.4

[1.9.3]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.9.3

[1.9.2]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.9.2

[1.9.1]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.9.1

[1.9.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.9.0

[1.8.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.8.0

[1.7.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.7.0

[1.6.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.6.0

[1.5.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.5.0

[1.4.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.4.0

[1.3.1]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.3.1

[1.3.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.3.0

[1.2.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.2.0

[1.1.5]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.1.5

[1.1.4]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.1.4

[1.1.3]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.1.3

[1.1.2]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.1.2

[1.1.1]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.1.1

[1.1.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.1.0

[1.0.1]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.0.1

[1.0.0]: https://github.com/cycodehq/intellij-platform-plugin/releases/tag/v1.0.0

[Unreleased]: https://github.com/cycodehq/intellij-platform-plugin/compare/v2.2.0...HEAD
