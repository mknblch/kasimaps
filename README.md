# JavaFX based map for [p1999-Everquest](https://www.project1999.com/)

## Installation

- install [Java](https://openjdk.org/) 
- download current [version](https://github.com/mknblch/kasimaps/tree/develop/dist/1.0beta)
- adapt **eq_directory** in `application.properties` (unix style path's using slashes instead of backslashes!)
- run using `java -jar Kasimaps.jar` or `start.bat` on windows 

## Usage

- the program monitors Everquests `/log` directory and reacts to `/location` commands or zone-change
- this behaviour can be activated using in-game command `/log on` or set as default in the ini-file
- on startup the current map can be switched using the menu
- on zone-change or login the map should change automatically
- zone-data is taken from [nparse](https://github.com/nomns/nparse) and [wiki](https://wiki.project1999.com) (*99)
- map can be dragged and zoomed using `mouse & mouse-wheel`
- use `Options -> Filter Z-Axis` in zones with multiple layers
- `ctrl + mouse-wheel` manually changes the Z-Position 
- `right-click` manually sets cursor position like `/loc` does
- `Options -> Color -> Z-Color` enables X Axis based coloring
- Background and False-Colors for map elements can be changed too
- `App -> Lock Window` brings the window to the top and disables dragging and resizing
- `App -> Transparent Window` changes window opacity
- ...

### Map explorer

![](doc/561314df.png)

### Z-Axis coloring

![](doc/668e040b.png)

### Z-Axis filtering

![](doc/ed173785.png)

### Configurable map and background color & transparency

![](doc/afb1b7d5.png)
![](doc/59371472.png)
![](doc/1b918a61.png)

![](doc/6a4e3af6.png)