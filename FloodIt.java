import java.awt.Color;
import java.util.*;
import javalib.impworld.*;
import javalib.worldimages.AboveImage;
import javalib.worldimages.BesideImage;
import javalib.worldimages.EmptyImage;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.OverlayImage;
import javalib.worldimages.Posn;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.TextImage;
import javalib.worldimages.WorldImage;
import tester.Tester;

// Utils class that initializes cellSize
class Utils {
  int cellSize = 40;
}

//interface ICell that can be implemented by EmptyCell or Cell
interface ICell {
  // returns an image representing this cell
  public WorldImage drawCell();

  // links this cell to all surrounding cells
  public void linkBoard(ArrayList<ArrayList<Cell>> board, int col, int row);

  // determines if the position clicked hits this cell
  public boolean cellClicked(Posn p);

  // draws this cell and all the cells below it
  public WorldImage drawColumn();

  // draws the grid of cells starting at this cell as the origin
  public WorldImage drawGrid();

  // determines if the color of this cell equals the given color
  public boolean colorEquals(Color color);

  // floods the cells with a new color starting at this cell as the origin
  public ArrayList<Cell> flood(Color originalColor);

  // flood helper - checks cells to the top, bottom, left, and right, and possibly floods
  public void floodHelp(Color originalColor, ArrayList<Cell> list);

  // returns the color of this cell
  public Color getColor();
}

//EmptyCell represents a cell that is empty at the end of rows and columns
class EmptyCell implements ICell {
  Utils utils = new Utils();

  public WorldImage drawCell() {
    throw new IllegalArgumentException("Cannot draw empty cell");
  }

  public void linkBoard(ArrayList<ArrayList<Cell>> board, int col, int row) {
    throw new IllegalArgumentException("Cannot link empty cell");
  }

  public boolean cellClicked(Posn p) {
    return false;
  }

  public WorldImage drawGrid() {
    return new RectangleImage(0, utils.cellSize, OutlineMode.SOLID, Color.white);
  }

  public WorldImage drawColumn() {
    return new RectangleImage(utils.cellSize, 0, OutlineMode.SOLID, Color.white);
  }

  public Color getColor() {
    throw new IllegalArgumentException("Cannot get color of empty cell");
  }

  public boolean colorEquals(Color color) {
    return false;
  }

  public void floodHelp(Color originalColor, ArrayList<Cell> list) {
    throw new IllegalArgumentException("Empty cell cannot be part of flooded area.");
  }

  public ArrayList<Cell> flood(Color originalColor) {
    throw new IllegalArgumentException("Empty cell cannot be part of flooded area.");
  }
}

// Represents a single square of the game area
class Cell implements ICell {
  Utils utils = new Utils();
  int cellSize;
  // In logical coordinates, with the origin at the top-left corner of the screen
  int x;
  int y;
  Color color;
  // the four adjacent cells to this one
  ICell left;
  ICell top;
  ICell right;
  ICell bottom;

  ArrayList<Cell> filled = new ArrayList<Cell>();

  Cell(int x, int y, Color color) {
    this.cellSize = 40;
    this.x = x;
    this.y = y;
    this.color = color;

    this.left = new EmptyCell();
    this.top = new EmptyCell();
    this.right = new EmptyCell();
    this.bottom = new EmptyCell();
  }

  public WorldImage drawCell() {
    return new RectangleImage(this.cellSize, this.cellSize, OutlineMode.SOLID, this.color);
  }

  public void linkBoard(ArrayList<ArrayList<Cell>> board, int col, int row) {
    int boardSize = board.size();
    if (col != 0) {
      this.left = board.get(col - 1).get(row);
    }
    else {
      this.left = new EmptyCell();
    }
    if (col != boardSize - 1) {
      this.right = board.get(col + 1).get(row);
    }
    else {
      this.right = new EmptyCell();
    }
    if (row != 0) {
      this.top = board.get(col).get(row - 1);
    }
    else {
      this.top = new EmptyCell();
    }
    if (row != boardSize - 1) {
      this.bottom = board.get(col).get(row + 1);
    }
    else {
      this.bottom = new EmptyCell();
    }
  }

  public boolean cellClicked(Posn p) {
    int halfSize = this.cellSize / 2;
    return p.x > this.x - halfSize 
        && p.x < this.x + halfSize 
        && p.y > this.y - halfSize 
        && p.y < this.y + halfSize;
  }

  public WorldImage drawGrid() {
    return new BesideImage(this.drawColumn(), this.right.drawGrid());
  }

  public WorldImage drawColumn() {
    return new AboveImage(this.drawCell(), this.bottom.drawColumn());
  }

  public Color getColor() {
    return this.color;
  }

  public boolean colorEquals(Color other) {
    return this.color.getBlue() == other.getBlue()
        && this.color.getRed() == other.getRed()
        && this.color.getGreen() == other.getGreen();
  }

  public ArrayList<Cell> flood(Color originalColor) {
    this.floodHelp(originalColor, filled);
    return filled;
  }

  public void floodHelp(Color originalColor, ArrayList<Cell> list) {
    list.add(this);
    if (this.right.colorEquals(originalColor) && !list.contains(right)) {
      this.right.floodHelp(originalColor, list);
    }
    if (this.bottom.colorEquals(originalColor) && !list.contains(bottom)) {
      this.bottom.floodHelp(originalColor, list);
    }
    if (this.top.colorEquals(originalColor) && !list.contains(top)) {
      this.top.floodHelp(originalColor, list);
    }
    if (this.left.colorEquals(originalColor) && !list.contains(left)) {
      this.left.floodHelp(originalColor, list);
    }
  }
}

// Represents the game Flood
class FloodItWorld extends World {
  // the amount of times board gets updated per second
  int fps;
  // number of turns the player has left
  int turnsLeft;
  // The current state of the game
  String gameState;
  // All the cells of the game
  ArrayList<ArrayList<Cell>> board;
  // Colors used on the board
  ArrayList<Color> colors;
  // The resolution of the board
  int dimension;
  // Number of colors in the game
  int numOfColors;
  // The width & height of each cell
  int cellSize;
  // The number of cells in each row or column
  int boardSize;
  // current color of the flood
  Color floodColor;
  // the top left cell that is the starting point of the flood
  Cell origin;
  // the x and y value for the center of the canvas
  int middleCanvas;
  // keeps track of the time
  double timer;
  // the scene
  WorldScene scene;
  // random for testing
  Random rand;
  // utils class 
  Utils utils = new Utils();
  // cells that are flooded
  ArrayList<Cell> flooded;
  // if the board has been generated
  boolean boardMade;
  // keeps track of time elapsed
  double stopwatch;


  // Constructor that sets the resolution & number of colors of the game
  FloodItWorld(int boardSize, int numOfColors, int fps) {
    this.rand = new Random();
    this.fps = fps;
    this.gameState = "stagnant";
    this.boardSize = boardSize;
    this.numOfColors = numOfColors;
    this.cellSize = utils.cellSize;
    this.dimension = boardSize * cellSize;
    this.middleCanvas = (this.dimension / 2);
    this.flooded = new ArrayList<Cell>();
    this.boardMade = false;
    this.turnsLeft = boardSize * numOfColors / 2;
  }

  //Constructor that sets the resolution & number of colors of the game, for testing
  FloodItWorld(Random rand, int boardSize, int numOfColors, int fps) {
    this.rand = rand;
    this.fps = fps;
    this.gameState = "stagnant";
    this.boardSize = boardSize;
    this.numOfColors = numOfColors;
    this.cellSize = utils.cellSize;
    this.dimension = boardSize * cellSize;
    this.middleCanvas = (this.dimension / 2);
    this.colors = new ArrayList<Color>(numOfColors);
    this.flooded = new ArrayList<Cell>();
    this.board = new ArrayList<ArrayList<Cell>>(boardSize);
    this.turnsLeft = boardSize * numOfColors / 2;
  }

  // returns the scene
  public WorldScene makeScene() {
    if (!boardMade) {
      this.resetBoard();
      boardMade = true;
    }
    scene = new WorldScene(dimension, dimension);
    // winning screen
    if (gameState.equals("won")) {
      scene.placeImageXY(new OverlayImage(
          new TextImage("You won! Press 'R'!", boardSize * 3, Color.black),
          new RectangleImage(dimension, dimension, OutlineMode.SOLID, Color.green)), 
          middleCanvas, middleCanvas);  
    }
    // losing screen
    else if (gameState.equals("lost")) {
      scene.placeImageXY(new OverlayImage(
          new TextImage("You Lost. Press 'R'!",  boardSize * 3, Color.white),
          new RectangleImage(dimension, dimension, OutlineMode.SOLID, Color.red)), 
          middleCanvas, middleCanvas);    
    }
    else {
      // gameplay screen
      scene.placeImageXY(new AboveImage(this.origin.drawGrid()), middleCanvas, middleCanvas);
      scene.placeImageXY(new AboveImage(
          new TextImage("Turns Left: " + turnsLeft, boardSize * 2, floodColor),
          new TextImage(this.generateTime(), boardSize * 2, floodColor)), 
          dimension / 2, dimension + cellSize * 2);
    }
    return scene;
  }

  // generates a string to represent the time elapsed
  public String generateTime() {
    if (Math.round(stopwatch) % 60 < 10) {
      return "Time elapsed: " + (Math.round(stopwatch) / 60) + ":0" + (Math.round(stopwatch) % 60);
    }
    return "Time elapsed: " + (Math.round(stopwatch) / 60) + ":" + (Math.round(stopwatch) % 60);
  }

  // creates all the cells and their linked relationships
  public void generateBoard() {  
    // creates a grid of all the cells
    for (int col = 0; col < boardSize; col++) {
      board.add(new ArrayList<Cell>());
      for (int row = 0; row < boardSize; row++) {
        Color randCol = colors.get(rand.nextInt(colors.size()));
        if (col == 0 && row == 0) {
          this.floodColor = randCol;
        }
        board.get(col).add(new Cell(cellSize * col + (cellSize / 2), 
            cellSize * row + (cellSize / 2), 
            randCol));
      }
    }
  }

  // links all the cells in a board together
  public void linkBoard() {
    // sets the adjacent cells for each cell
    for (int col = 0; col < boardSize; col++) {
      for (int row = 0; row < boardSize; row++) {
        this.board.get(col).get(row).linkBoard(this.board, col, row);
      }
    }
    // sets the origin
    this.origin = this.board.get(0).get(0);
  }

  // generates a list of random colors of the given length
  public void generateColors() {
    for (int i = 0; i < numOfColors; i++) {
      int red = rand.nextInt(255);
      int green = rand.nextInt(255);
      int blue = rand.nextInt(255);
      colors.add(new Color(red, green, blue));
    }
  }

  // responds if the game is in the state of flooding
  public void onTick() {
    // updates stopwatch
    this.stopwatch += 1.0 / this.fps;
    //ticks timer
    this.timer += .1;

    if (this.turnsLeft == 0) {
      this.gameState = "lost";
    }

    else if (gameState.equals("flooding")) {
      if (timer >= .2) {
        if (flooded.isEmpty()) {
          if (isGameWon()) {
            gameState = "won";
          }
          else {
            gameState = "stagnant";
            timer = 0;
          }
        }
        else {
          flooded.remove(0).color = floodColor;
          timer = 0;
        }
      }
    }
  }

  // determines if the player won the game
  public boolean isGameWon() {
    for (ArrayList<Cell> col : board) {
      for (Cell c : col) {
        if (!c.colorEquals(floodColor)) {
          return false;
        }
      }
    }
    return true;
  }

  // resets the data on the board
  public void resetBoard() {
    this.timer = 0;
    this.stopwatch = 0;
    this.gameState = "stagnant";
    this.colors = new ArrayList<Color>(numOfColors);
    this.board = new ArrayList<ArrayList<Cell>>(boardSize);
    this.turnsLeft = boardSize * numOfColors / 2;
    this.generateColors();
    this.generateBoard();
    this.linkBoard();
  }

  // checks if the cell clicked is a valid color & sets the state to flooding
  public void onMouseClicked(Posn p) {  
    if (gameState.equals("stagnant")) {
      for (ArrayList<Cell> col : board) {
        for (Cell c : col) {
          if (c.cellClicked(p) && !c.colorEquals(this.origin.color)) {
            this.turnsLeft--;
            floodColor = c.color;
            this.flooded = origin.flood(this.origin.getColor());
            gameState = "flooding";
          }
        }
      }
    }
  }

  // handles if a key gets pressed
  public void onKeyEvent(String e) {
    if (e.equals("r") 
        && (gameState.equals("won") || gameState.equals("lost"))) {
      this.resetBoard();
    }
  }
}

class ExamplesFloodItWorld {

  FloodItWorld f1Cell1Color;
  FloodItWorld f4Cell2Color;

  ICell cell1;
  ICell mtCell;

  WorldScene placedScene; 

  // initializes some example Worlds, ICells, and WorldScenes.
  void initData() {
    // A FloodItWorld with 1 cell (boardSize = 1) and 1 color, with randomSeed 10.
    f1Cell1Color = new FloodItWorld(new Random(10), 1, 1, 60);
    // A FloodItWorld with 4 cells (boardSize = 2) and 2 colors, with randomSeed 10
    f4Cell2Color = new FloodItWorld(new Random(10), 2, 2, 60);

    // An example Cell
    cell1 = new Cell(10, 10, new Color(1, 2, 3));
    // An example EmptyCell
    mtCell = new EmptyCell();

    // An example WorldScene with dimensions 40x40
    placedScene = new WorldScene(40, 40);  
    placedScene.placeImageXY(new AboveImage(
        new RectangleImage(40, 40, OutlineMode.SOLID, new Color(3, 75, 108)), 
        new EmptyImage()), 20, 20);
  }

  // generates the colors and boards for the example FloodItWorlds
  void initBoard() {
    // generates colors for f1Cell1Color
    f1Cell1Color.generateColors();

    // generates the board for f1Cell1Color
    f1Cell1Color.generateBoard();

    // links the board for f1Cell1Color
    f1Cell1Color.linkBoard();

    // generates colors for f4Cell2Color
    f4Cell2Color.generateColors();

    // generates the board for f4Cell2Color
    f4Cell2Color.generateBoard(); 

    // links the board for f1Cell1Color
    f4Cell2Color.linkBoard();
  }

  // testing drawCell
  void testDrawCell(Tester t) {
    initData();

    t.checkExpect(this.cell1.drawCell(), 
        new RectangleImage(40, 40, OutlineMode.SOLID, new Color(1, 2, 3)));
    t.checkException(
        new IllegalArgumentException("Cannot draw empty cell"), this.mtCell, "drawCell");
  }

  // testing cellClicked
  void testCellClicked(Tester t) {
    initData();
    initBoard();

    t.checkExpect(mtCell.cellClicked(new Posn(3, 3)), false);
    t.checkExpect(cell1.cellClicked(new Posn(20, 20)), true);
    t.checkExpect(cell1.cellClicked(new Posn(30, 20)), false);

    t.checkExpect(f1Cell1Color.board.get(0).get(0).cellClicked(new Posn(30, 30)), true);
    t.checkExpect(f4Cell2Color.board.get(0).get(1).cellClicked(new Posn(30, 70)), true);
  }

  // testing getColor
  void testGetColor(Tester t) {
    initData();
    initBoard();

    t.checkException(new IllegalArgumentException("Cannot get color of empty cell"), 
        this.mtCell, "getColor");
    t.checkExpect(cell1.getColor(), new Color(1, 2, 3));
    t.checkExpect(f1Cell1Color.board.get(0).get(0).getColor(), new Color(3, 75, 108));
  }

  // testing colorEquals
  void testColorEquals(Tester t) {
    initData();
    initBoard();

    t.checkExpect(mtCell.colorEquals(new Color(1, 2, 3)), false);
    t.checkExpect(cell1.colorEquals(new Color(1, 2, 3)), true);
    t.checkExpect(f1Cell1Color.board.get(0).get(0).colorEquals(new Color(3, 75, 108)), true);
  }

  // testing flood
  void testFlood(Tester t) {
    initData();
    initBoard();

    // test flooding an empty cell (exception):
    t.checkException(new IllegalArgumentException("Empty cell cannot be part of flooded area."), 
        this.mtCell, "flood", new Color(1, 2, 3));

    // testing flood on f1Cell1Color (fills top left):
    // origin of f1Cell1Color (for convenience)
    Cell origin1x1 = f1Cell1Color.origin;
    t.checkExpect(origin1x1.flood(origin1x1.color), 
        Arrays.asList(origin1x1));

    // testing flood on f4Cell2Color (fills bottom left, bottom right, top left):
    // origin of f4Cell2Color (for convenience)
    Cell origin2x2 = f4Cell2Color.origin;
    t.checkExpect(origin2x2.flood(origin2x2.color), 
        Arrays.asList(origin2x2, origin2x2.bottom, f4Cell2Color.board.get(1).get(1)));
  }

  void testFloodHelp(Tester t) {
    initData();
    initBoard();

    // test floodHelp on empty cell (exception):
    t.checkException(new IllegalArgumentException("Empty cell cannot be part of flooded area."), 
        this.mtCell, "floodHelp", new Color(1, 2, 3), new ArrayList<Cell>());

    // origin of f1Cell1Color (for convenience)
    Cell origin1x1 = f1Cell1Color.origin;
    // flooded cells before mutation
    t.checkExpect(f1Cell1Color.flooded, new ArrayList<Cell>());
    //mutation 
    origin1x1.floodHelp(origin1x1.color, f1Cell1Color.flooded);
    // flooded cells after mutation
    t.checkExpect(f1Cell1Color.flooded, 
        Arrays.asList(origin1x1));

    // origin of f4Cell2Color (for convenience)
    Cell origin2x2 = f4Cell2Color.origin;
    // flooded cells before mutation
    t.checkExpect(f4Cell2Color.flooded, new ArrayList<Cell>());
    //mutation 
    origin2x2.floodHelp(origin2x2.color, f4Cell2Color.flooded);
    // flooded cells after mutation (top left, bottom left, bottom right)
    t.checkExpect(f4Cell2Color.flooded, 
        Arrays.asList(origin2x2, origin2x2.bottom, f4Cell2Color.board.get(1).get(1)));
  }

  // testing drawColumn
  void testDrawColumn(Tester t) {
    initData();
    initBoard();

    t.checkExpect(f1Cell1Color.board.get(0).get(0).drawColumn(), 
        new AboveImage(new RectangleImage(40, 40, OutlineMode.SOLID, new Color(3, 75, 108)), 
            new RectangleImage(40, 0, OutlineMode.SOLID, Color.white)));
    t.checkExpect(f4Cell2Color.board.get(0).get(0).drawColumn(),
        new AboveImage(new RectangleImage(40, 40, OutlineMode.SOLID, new Color(3, 75, 108)), 
            new AboveImage(new RectangleImage(40, 40, OutlineMode.SOLID, new Color(3, 75, 108)), 
                new RectangleImage(40, 0, OutlineMode.SOLID, Color.white))));
    t.checkExpect(f4Cell2Color.board.get(1).get(0).drawColumn(),
        new AboveImage(new RectangleImage(40, 40, OutlineMode.SOLID, new Color(60, 151, 241)), 
            new AboveImage(new RectangleImage(40, 40, OutlineMode.SOLID, new Color(3, 75, 108)), 
                new RectangleImage(40, 0, OutlineMode.SOLID, Color.white))));
  }

  // testing drawGrid
  void testDrawGrid(Tester t) {
    initData();
    initBoard();

    t.checkExpect(f1Cell1Color.board.get(0).get(0).drawGrid(), 
        new BesideImage(new AboveImage(
            new RectangleImage(40, 40, OutlineMode.SOLID, new Color(3, 75, 108)), 
            new RectangleImage(40, 0, OutlineMode.SOLID, Color.white)), 
            new RectangleImage(0, 40, OutlineMode.SOLID, Color.white)));

    t.checkExpect(f4Cell2Color.board.get(0).get(0).drawGrid(), 
        new BesideImage(new AboveImage(
            new RectangleImage(40, 40, OutlineMode.SOLID, new Color(3, 75, 108)),
            new AboveImage(new RectangleImage(40, 40, OutlineMode.SOLID, new Color(3, 75, 108)),
                new RectangleImage(40, 0, OutlineMode.SOLID, Color.white))),
            new BesideImage(new AboveImage(
                new RectangleImage(40, 40, OutlineMode.SOLID, new Color(60, 151, 241)),
                new AboveImage(new RectangleImage(40, 40, OutlineMode.SOLID, new Color(3, 75, 108)),
                    new RectangleImage(40, 0, OutlineMode.SOLID, Color.white))),
                new RectangleImage(0, 40, OutlineMode.SOLID, Color.white))));
  }

  // testing makeScene
  void testMakeScene(Tester t) {
    initData();

    t.checkExpect(
        f1Cell1Color.makeScene(), placedScene);
  }

  // testing generateColors
  void testGenerateColors(Tester t) {
    initData();

    // testing initial conditions
    t.checkExpect(f1Cell1Color.colors, new ArrayList<Color>());
    t.checkExpect(f4Cell2Color.colors, new ArrayList<Color>());

    // mutation
    f1Cell1Color.generateColors();
    f4Cell2Color.generateColors();

    // testing side effects after mutation
    t.checkExpect(f1Cell1Color.colors, new ArrayList<Color>(Arrays.asList(new Color(3, 75, 108))));
    t.checkExpect(f4Cell2Color.colors, new ArrayList<Color>(Arrays.asList(
        new Color(3, 75, 108), 
        new Color(60, 151, 241))));
  }

  // testing generateBoard
  void testGenerateBoard(Tester t) {
    initData();
    f1Cell1Color.generateColors();
    f4Cell2Color.generateColors();


    // testing initial conditions
    t.checkExpect(f1Cell1Color.board, new ArrayList<ArrayList<Cell>>());
    t.checkExpect(f4Cell2Color.board, new ArrayList<ArrayList<Cell>>());

    // mutation: generates boards
    f1Cell1Color.generateBoard();
    f4Cell2Color.generateBoard();     

    // testing side effects after mutation
    t.checkExpect(f1Cell1Color.board, 
        new ArrayList<ArrayList<Cell>>(
            Arrays.asList(
                new ArrayList<Cell>(Arrays.asList(new Cell(20, 20, new Color(3, 75, 108)))))));

    t.checkExpect(f4Cell2Color.board,
        new ArrayList<ArrayList<Cell>>(
            Arrays.asList(new ArrayList<Cell>(Arrays.asList(new Cell(20, 20, new Color(3, 75, 108)),
                new Cell(20, 60, new Color(3, 75, 108)))),
                new ArrayList<Cell>(Arrays.asList(new Cell(60, 20, new Color(60, 151, 241)),
                    new Cell(60, 60, new Color(3, 75, 108)))))));
  }


  // testing linkBoard
  void testLinkBoard(Tester t) {
    initData();
    f1Cell1Color.generateColors();
    f1Cell1Color.generateBoard();
    f4Cell2Color.generateColors();
    f4Cell2Color.generateBoard(); 

    // testing initial conditions
    t.checkExpect(f1Cell1Color.board.get(0).get(0).left, new EmptyCell());
    t.checkExpect(f1Cell1Color.board.get(0).get(0).top, new EmptyCell());
    t.checkExpect(f1Cell1Color.board.get(0).get(0).right, new EmptyCell());
    t.checkExpect(f1Cell1Color.board.get(0).get(0).bottom, new EmptyCell());

    t.checkExpect(f4Cell2Color.board.get(0).get(0).left, new EmptyCell());
    t.checkExpect(f4Cell2Color.board.get(0).get(0).top, new EmptyCell());
    t.checkExpect(f4Cell2Color.board.get(0).get(0).right, new EmptyCell());
    t.checkExpect(f4Cell2Color.board.get(0).get(0).bottom, new EmptyCell());

    // mutation
    f1Cell1Color.linkBoard();
    f4Cell2Color.linkBoard();

    // testing side effects after mutation
    t.checkExpect(f1Cell1Color.board.get(0).get(0).left, new EmptyCell());
    t.checkExpect(f1Cell1Color.board.get(0).get(0).top, new EmptyCell());
    t.checkExpect(f1Cell1Color.board.get(0).get(0).right, new EmptyCell());
    t.checkExpect(f1Cell1Color.board.get(0).get(0).bottom, new EmptyCell());
    t.checkExpect(f4Cell2Color.board.get(0).get(0).left, new EmptyCell());
    t.checkExpect(f4Cell2Color.board.get(0).get(0).top, new EmptyCell());
    t.checkExpect(f4Cell2Color.board.get(0).get(0).right, f4Cell2Color.board.get(1).get(0));
    t.checkExpect(f4Cell2Color.board.get(0).get(0).bottom, f4Cell2Color.board.get(0).get(1));
  }

  // testing generateTime
  void testGenerateTime(Tester t) {
    initData();
    initBoard();

    t.checkExpect(f1Cell1Color.generateTime(), "Time elapsed: 0:00");

    f1Cell1Color.stopwatch = 35.0;

    t.checkExpect(f1Cell1Color.generateTime(), "Time elapsed: 0:35");

    f1Cell1Color.stopwatch = 185.0;

    t.checkExpect(f1Cell1Color.generateTime(), "Time elapsed: 3:05");
  }

  // testing onTick()
  void testOnTick(Tester t) {
    initData();
    initBoard();

    // testing the timing
    t.checkExpect(f4Cell2Color.stopwatch, 0.0);
    t.checkExpect(f4Cell2Color.timer, 0.0);

    f4Cell2Color.onTick();

    t.checkExpect(f4Cell2Color.stopwatch, 1.0 / 60.0);
    t.checkExpect(f4Cell2Color.timer, 0.1);

    // testing the lost gameState  
    t.checkExpect(f4Cell2Color.gameState, "stagnant");

    f4Cell2Color.turnsLeft = 0;
    f4Cell2Color.onTick();

    t.checkExpect(f4Cell2Color.gameState, "lost");

    // testing the flooding function
    initData();
    initBoard();
    t.checkExpect(f4Cell2Color.gameState, "stagnant");

    f4Cell2Color.onMouseClicked(new Posn(65, 20));
    t.checkExpect(f4Cell2Color.gameState, "flooding");

    // wait for flood to complete
    while (!f4Cell2Color.isGameWon()) {
      f4Cell2Color.onTick();
    }
    // set preconditions to win
    f4Cell2Color.timer = 0.2;
    f4Cell2Color.onTick();
    f4Cell2Color.flooded = new ArrayList<Cell>();

    // game now won
    t.checkExpect(f4Cell2Color.gameState, "won");
  }

  // testing isGameWon
  void testIsGameWon(Tester t) {
    initData();
    initBoard();

    // will always be true
    t.checkExpect(f1Cell1Color.isGameWon(), true);

    // before mutation
    t.checkExpect(f4Cell2Color.isGameWon(), false);

    // after mutation
    f4Cell2Color.board.get(1).get(0).color = f4Cell2Color.origin.color;

    // after mutation (all colors on board the same color)
    t.checkExpect(f4Cell2Color.isGameWon(), true);
  }

  // testing resetBoard
  void testResetBoard(Tester t) {
    initData();
    initBoard();

    // changes the world to not be the default world
    f4Cell2Color.onTick();
    f4Cell2Color.onMouseClicked(new Posn(65, 20));

    // checks the conditions (not default)
    t.checkExpect(f4Cell2Color.timer, 0.1);
    t.checkExpect(f4Cell2Color.stopwatch, 1.0 / 60.0);
    t.checkExpect(f4Cell2Color.gameState, "flooding");
    t.checkExpect(f4Cell2Color.colors, 
        new ArrayList<Color>(Arrays.asList(new Color(3, 75, 108), 
            new Color(60, 151, 241))));
    t.checkExpect(f4Cell2Color.turnsLeft, 1);

    //mutation
    f4Cell2Color.resetBoard();

    // checks for default conditions
    t.checkExpect(f4Cell2Color.timer, 0.0);
    t.checkExpect(f4Cell2Color.stopwatch, 0.0);
    t.checkExpect(f4Cell2Color.gameState, "stagnant");
    t.checkExpect(f4Cell2Color.colors, 
        new ArrayList<Color>(Arrays.asList(new Color(58, 164, 141), 
            new Color(108, 165, 240))));
    t.checkExpect(f4Cell2Color.turnsLeft, 2);

  }

  // testing onMouseClicked
  void testOnMouseClicked(Tester t) {
    initData();
    initBoard();

    // default conditions
    t.checkExpect(f4Cell2Color.turnsLeft, 2);
    t.checkExpect(f4Cell2Color.floodColor, new Color(3, 75, 108));
    t.checkExpect(f4Cell2Color.gameState, "stagnant");

    // mutation - a cell is clicked
    f4Cell2Color.onMouseClicked(new Posn(65, 20));

    // conditions after mutation
    t.checkExpect(f4Cell2Color.turnsLeft, 1);
    t.checkExpect(f4Cell2Color.floodColor, new Color(60, 151, 241));
    t.checkExpect(f4Cell2Color.gameState, "flooding");
  }

  // testing onKeyEvent
  void testOnKeyEvent(Tester t) {
    initData();
    initBoard();

    // changes the world to not be the default world
    f4Cell2Color.onTick();
    f4Cell2Color.onMouseClicked(new Posn(65, 20));

    // checks the conditions (not default)
    t.checkExpect(f4Cell2Color.timer, 0.1);
    t.checkExpect(f4Cell2Color.stopwatch, 1.0 / 60.0);
    t.checkExpect(f4Cell2Color.gameState, "flooding");
    t.checkExpect(f4Cell2Color.colors, new ArrayList<Color>(Arrays.asList(
        new Color(3, 75, 108), new Color(60, 151, 241))));
    t.checkExpect(f4Cell2Color.turnsLeft, 1);
    f4Cell2Color.gameState = "won";

    // mutation - presses "r"
    f4Cell2Color.onKeyEvent("r");

    // checks for default conditions
    t.checkExpect(f4Cell2Color.timer, 0.0);
    t.checkExpect(f4Cell2Color.stopwatch, 0.0);
    t.checkExpect(f4Cell2Color.gameState, "stagnant");
    t.checkExpect(f4Cell2Color.colors, new ArrayList<Color>(Arrays.asList(
        new Color(58, 164, 141), new Color(108, 165, 240))));
    t.checkExpect(f4Cell2Color.turnsLeft, 2);
  }

  // tests class FloodItWorld
  void testFloodItWorld(Tester t) {
    // REFERENCE: bigBang(width, height, 1 / fps)
    int fps = 60;
    FloodItWorld f = new FloodItWorld(6, 4, fps);
    f.bigBang(600, 600, 1.0 / fps);
  }
}