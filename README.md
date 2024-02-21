# Cycode IntelliJ Platform Plugin

<!-- Plugin description -->

The Cycode IntelliJ Platform Plugin is a plugin for JetBrains IDEs (e.g., IntelliJ IDEA, PyCharm, WebStorm) that helps
users to adopt a shift-left strategy, by enabling code scanning early in the development lifecycle, which could
significantly help businesses avoid costly repairs and potential complications down the line.

## Features

Cycode IntelliJ Platform Plugin scans your code for exposed secrets, passwords, tokens, keys, and other credentials, as
well as open-source package`s vulnerabilities.
The extension provides functionalities such as:

* Scanning your code for exposed secrets, passwords, tokens, keys, and other credentials.
* Scanning your code for open-source package`s vulnerabilities.
* Running a new scan from your IDE even before committing the code.
* Triggering a scan automatically whenever a file is saved.
* Highlighting vulnerable code in the editor - syntax highlighting for Cycode-specific code and configuration files,
  making it easy for users to identify and work with these files in their projects.
* Removing a detected secret or ignoring it by secret value, rule (type) or by path.
* Upgrading a detected vulnerable package or ignoring it by rule (type) or by path.

Coming soon: Code Security (SAST), and Infrastructure as Code (IaC).

## Installation

To install the Cycode IntelliJ Platform Plugin, follow these steps:

1. Open the editor.
2. Navigate to the Plugins Section:
    1. In the left-hand sidebar of the Settings/Preferences dialog, select "Plugins".
3. Search for "Cycode" in the search bar:
    1. In the "Plugins" section, you'll see a "Marketplace" tab.
       Click on this tab, and then use the search bar at the top to search for the plugin you wish to install.
4. Click on the "Install" button next to the Cycode plugin.
5. Wait for the installation to complete.
6. Restart the JetBrains editor.

Alternatively, you can install the plugin from the plugin page: https://plugins.jetbrains.com/plugin/23079-cycode

## Authentication

To authenticate the Cycode IntelliJ Platform Plugin, follow these steps:

1. Open the editor.
2. Click on the Cycode icon in the sidebar.
3. Click on the "Authenticate" button.

## Configuring the Plugin

To configure the plugin, go to the plugin settings to change the default settings:

1. In the Additional Parameters field, you can submit additional CLI parameters, such as `--verbose` mode for debugging
   purposes.
2. Use the API URL field to change the base URL (on-premises Cycode customers see this explanation).
3. Use the APP URL if the web URL needs to be changed.
4. Use CLI PATH to set the path to the Cycode CLI executable. In cases where the CLI can't be downloaded due to your
   network configuration (for example, due to firewall rules), use this option.
5. Clear the Scan on Save option to prevent Cycode from scanning your code every time you save your work. Instead, use
   the Scan on-Demand option.

Note: If the "Scan on Save" option is enabled in the extension settings, Cycode will scan the file in focus
(including manifest files, such as package.json and dockerfile) for hardcoded secrets.

## Usage

To use the Cycode IntelliJ Platform Plugin, follow these steps:

1. Open the editor.
2. Open a project that uses the Cycode platform.
3. Open a file to scan.
    1. Press Ctrl+S or Cmd+S on Mac to save a file → A scan will automatically be triggered.
    2. If the "Scan on Save File" option is enabled in the plugin settings, Cycode will scan the file in focus
       (including manifest files, such as package.json and dockerfile) for hardcoded secrets.
4. Also applies for auto-save.
5. Wait for the scan to complete and to display a success message.

## Viewing Scan Results

### Handling Detected Secrets

1. The scan displays a list of hardcoded secrets found in the application code.
2. Once the scan completes (either on save or on-demand), you’ll then see the violation(s) highlighted in your main
   window.
3. Hover over the violation to see the violation summary.
4. To view the details of the violation, select it in the list.
5. Next, choose how to address the detected violation(s) by selecting the Quick Fix button.
6. If the violation is a secret, you can choose to ignore it — either by secret value,
   secret rule (i.e., secret type) or the specific file.
   Note that Ignore occurs locally on the developer’s machine.
7. Go back to viewing the problem in the main window by clicking View problem.
8. You can also view a summary of all the problems by selecting the Problems tab.

## Support

If you encounter any issues or have any questions about the Cycode IntelliJ Platform Plugin, please reach out to the
Cycode support team at support@cycode.com.

## License

The Cycode IntelliJ Platform Plugin is released under the MIT license.
See the [LICENSE](https://github.com/cycodehq/intellij-platform-plugin/blob/main/LICENSE) file for more details.

<!-- Plugin description end -->
