# tinyrs
A minimalistic game client for Old School RuneScape.

## Features
- Super portable. One ~50 kB file is all you need! Ideal for people who use
  multiple computers.
- Cross-platform support. Should run on any platform that supports Java with
  Swing.
- Gamepack caching and automatic updating. This ensures quick loading times,
  as the game client is only downloaded when a new version comes out.
- Built-in screenshot ability.

![Screenshot](./screenshot.png)

## Usage
- The easiest way to use this application is to download and run the executable
  **tinyrs.jar** in this repository. Alternatively, the more wary users can
  compile the project for themselves and run `tinyrs.Application`.
- Through the use of the command-line, three additional program arguments can be
  specified:
  - `defaultWorld`, which specifies the initial game world to connect to, and
  - `storageDirectory`, which overrides the default storage directory (which
    lies in the home directory), and
  - `pluginArchive`, which specifies the path to a plugin JAR file; see **Plugins**.
    It can be specified multiple times.
  
#### Plugins
Plugins are (small) programs that are loaded along with the client that provide
a new feature or enhance an existing one.

Each plugin needs to be packed in its own archive (`.jar` file). The archive needs
to contain two things to be considered a valid plugin:
 1. A concrete class that implements `tinyrs.plugin.Plugin` with a nullary constructor.
 2. A manifest file with the attribute `Plugin-Class` which specifies the binary name
    of the class that implements `Plugin`.
    

Any plugins that have been specified by command-line are loaded when the client first
starts up. The plugins are *started* when the game client is loaded. If any plugins
have been (successfully) started, a "Plugins" menu will appear in the menu bar which
will contain items corresponding to each plugin.

### Dependencies
- Java 6
