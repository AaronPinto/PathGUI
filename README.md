# PathGUI

The field is whatever field the current FRC season's field is.

The program takes the mouse position on the field drawn in the GUI and then based off of that, when the mouse button is
clicked, it adds that point to a series of points, called a PathSegment, and generates a path accordingly. This is called
click mode. The path generation will either be based off of the generator that we currently use or I will redo it using cubic
spline interpolation. Either way, the filename is MPGen2D. Drag mode is toggleable off/on (Ctrl + D) and by default it is on,
which means you can use it in tandem with click mode, and it automatically switches depending on whether or not you hold the
left button on your mouse down or not. Drag mode essentially allows you to draw a path yourself without having to generate one
each time. It allows for more freedom of design regarding the path that the robot follows. The program also has various
keyboard shortcuts to make it more intuitive and easy to use. Ctrl + C to copy points on the path in proper Java syntax
for a 2D array, Ctrl + N to keep the old paths and start a new one if, for example, you want the robot to follow
multiple paths in auto, Ctrl + Z removes each point on the current path and Ctrl + Y adds that point back plus you can
hold down the keys and it will rapidly get rid of each point, and Ctrl + O to open and parse a file containing waypoints
in proper Java 2D array format, and then display the paths from that file.

For more information on the (FRC 2018) field, see: https://www.youtube.com/watch?v=HZbdwYiCY74 and
https://firstfrc.blob.core.windows.net/frc2018/Drawings/LayoutandMarkingDiagram.pdf
