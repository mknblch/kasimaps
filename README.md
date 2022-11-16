# JavaFX based map for [p1999-Everquest](https://www.project1999.com/)

### Installation

- install [Java](https://openjdk.org/) 
- download the [windows](https://github.com/mknblch/kasimaps/tree/develop/dist/snapshot/Kasimaps.jar) or [linux](https://github.com/mknblch/kasimaps/tree/develop/dist/snapshot/Kasimaps_lnx.jar) version
- run using `java -jar Kasimaps.jar` or `start.bat` 
- **_config.json_ from previous versions must be removed or the app won't start**

### Features

- on first start you will be asked for the Everquest root directory (not _/Logs_ !)
- the program monitors Everquests `/log` directory and reacts to `/location` commands or zone-change
- this behaviour can be activated using in-game command `/log on` or set as default in the ini-file
- the current map can be switched using the menu
- on zone-change or login the map should change automatically
- zone-data is taken from [nparse](https://github.com/nomns/nparse) and [wiki](https://wiki.project1999.com) (*99)


#### Map explorer

- map can be dragged and zoomed using `mouse & mouse-wheel`
- `App -> Lock Window` brings the window to the top and disables window-dragging and resizing
- `App -> Reset` resets the EQ directory and restarts the log parser
- `Options -> Color -> Z-Color` enables X Axis based coloring
- `Options -> Filter Z-Axis` hides map data far above or below your current position 
- `CTRL + mouse-wheel` manually changes the Z-Position
- `right click` copies the current position to clipboard
- the position can be pasted directly into in-game chat (/say, /tell, /group, /ooc) 
- when the parser reads the position in chat it creates a single waypoint marker

#### p99 Zone-Data

- Map data taken from P1999 Wiki 10/2022
- POIs have been grouped and merged by distance
- `Click on POI` to cycle through the names
- Since locations on wiki mostly miss the Z part the p99 POI data will be visible regardless of your current position

#### Multiplayer Map Synchronization

- `App -> Synchronization` activates Sync over IRC
- once connected the client will try to set the channel password
- people using the same encryption password share their positions among each other
- only location messages on the active map are processed and shown 

![](doc/Animation4.gif)
![](doc/Animation.gif)
![](doc/Animation2.gif)
![](doc/Animation5.gif)

<a href="https://www.flaticon.com/free-icons/dragon" title="dragon icons">Dragon icons created by Icongeek26 - Flaticon</a>