# JavaFX based map for [p1999-Everquest](https://www.project1999.com/)

### Installation

- install [Java](https://openjdk.org/) 
- download current [version](https://github.com/mknblch/kasimaps/tree/develop/dist/snapshot)
- run using `java -jar Kasimaps.jar` or `start.bat` 
 
### Features

- the program monitors Everquests `/log` directory and reacts to `/location` commands or zone-change
- this behaviour can be activated using in-game command `/log on` or set as default in the ini-file
- the current map can be switched using the menu
- on zone-change or login the map should change automatically
- zone-data is taken from [nparse](https://github.com/nomns/nparse) and [wiki](https://wiki.project1999.com) (*99)


#### Map explorer

- map can be dragged and zoomed using `mouse & mouse-wheel`
- `App -> Lock Window` brings the window to the top and disables window-dragging and resizing
- `App -> Transparent Window` enables transparent window
- `ALT + mouse wheel` changes transparency (if enabled)
- `Options -> Color -> Z-Color` enables X Axis based coloring
- `Options -> Filter Z-Axis` hides map data far above or below your current position
- `CTRL + mouse-wheel` manually changes the Z-Position
- `right click` copies the current position to clipboard
- when the parser reads the position in chat it creates a single waypoint marker  

#### p99 Zone-Data

- Map data taken from P1999 Wiki 10/2022
- POIs have been grouped and merged by distance
- `Click on POI` to cycle through the names

![](doc/Animation4.gif)
![](doc/Animation.gif)
![](doc/Animation2.gif)
