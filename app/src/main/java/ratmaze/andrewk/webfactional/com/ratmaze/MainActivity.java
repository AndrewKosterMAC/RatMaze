package ratmaze.andrewk.webfactional.com.ratmaze;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity
{
    // Constants

    static final boolean displayCurrentSquareDebugInfo = false;

    static final int blocksPerCell = 5;

    static Map<String, String> _pieceShapesByType = null;

    static Map<String, String> getPieceShapesByType()
    {
        if (null == _pieceShapesByType)
        {
            _pieceShapesByType = new HashMap<>();

            _pieceShapesByType.put("protagonist",
                "  0\r\n" +
                "0 00\r\n" +
                "00000\r\n" +
                "0 0\r\n" +
                "0 0");
            _pieceShapesByType.put("wall",
                "00000\r\n" +
                "00000\r\n" +
                "00000\r\n" +
                "00000\r\n" +
                "00000");
            _pieceShapesByType.put("cheese",
                "0\r\n" +
                " 0\r\n" +
                "000\r\n" +
                "00 0\r\n" +
                "00000");
            _pieceShapesByType.put("enemy",
                "0   0\r\n" +
                "00 00\r\n" +
                "00000\r\n" +
                "0 0 0\r\n" +
                "00000");
        }

        return _pieceShapesByType;
    }

    static Map<String, String> _pieceColorsByType = null;

    static Map<String, String> getPieceColorsByType()
    {
        if (null == _pieceColorsByType)
        {
            _pieceColorsByType = new HashMap<>();

            _pieceColorsByType.put("protagonist", "#646464");
            _pieceColorsByType.put("wall", "#00C800");
            _pieceColorsByType.put("cheese", "#FFFF00");
            _pieceColorsByType.put("enemy", "#FF0000");
        }

        return _pieceColorsByType;
    }

    static final String[] possibleAiCommands = new String[]
    {
        "Up",
        "Down",
        "Left",
        "Right",
        "Stop",
    };


    // Properties

    List<PieceOnBoard> protagonistsOnBoard = new ArrayList<>();

    List<PieceOnBoard> wallsOnBoard = new ArrayList<>();

    List<PieceOnBoard> cheesesOnBoard = new ArrayList<>();

    List<PieceOnBoard> enemiesOnBoard = new ArrayList<>();

    List<List<BoardPosition>> boardPositions = null;

    List<String> userCommandsQueue = new ArrayList<>();

    List<String> aiCommandsQueue = new ArrayList<>();

    boolean isGameWon = false;

    boolean isGameLost = false;

    boolean isEndGameMessageDisplayed = false;

    boolean wasGameStateResetRequested = false;

    private Canvas canvas;

    private Bitmap bitmap;

    private Paint paint;

    private LinearLayout gameDisplay;

    private TextView gameStatusDisplay;

    private TextView currentSquareInfo;


    // Methods

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameDisplay = (LinearLayout)findViewById(R.id.gameDisplay);

        gameStatusDisplay = (TextView)findViewById(R.id.gameStatusDisplay);

        currentSquareInfo = (TextView)findViewById(R.id.currentSquareInfo);

        paint = new Paint();
        bitmap = Bitmap.createBitmap(280, 280, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);

        startDrawing();
    }

    private int getCoordinateFromDrawingSize(int drawingSize)
    {
        return drawingSize / 5;
    }

    private int getDrawingSizeFromCoordinate(int coordinate)
    {
        return coordinate * 5;
    }

    private void drawPiece(PieceOnBoard pieceOnBoard)
    {
        paint.setColor(Color.parseColor(getPieceColorsByType().get(pieceOnBoard.type)));

        for (int i = 0; i < pieceOnBoard.shapeCellRows.length; i++) {

            for (int j = 0; j < pieceOnBoard.shapeCellRows[i].length(); j++) {

                if ('0' == pieceOnBoard.shapeCellRows[i].charAt(j))
                {
                    int left = getDrawingSizeFromCoordinate(pieceOnBoard.locationX + j);
                    int top = getDrawingSizeFromCoordinate(pieceOnBoard.locationY + i);

                    canvas.drawRect(
                        left,
                        top,
                        left + 5,
                        top + 5,
                        paint);
                    gameDisplay.setBackgroundDrawable(new BitmapDrawable(bitmap));
                }
            }
        }
    }

    private List<List<BoardPosition>> generateEmptyBoardPositions(int boardWidth, int boardHeight)
    {
        List<List<BoardPosition>> result = new ArrayList<>();

        for (int i = 0; i < boardHeight; i++) {

            List<BoardPosition> row = new ArrayList<>();

            for (int j = 0; j < boardWidth; j++) {

                BoardPosition position = new BoardPosition();
                position.contents = new ArrayList();
                position.locationX = j;
                position.locationY = i;

                row.add(position);
            }

            result.add(row);
        }

        return result;
    }

    private List findOccupiedGridPositionsForPiece(PieceOnBoard piece)
    {
        List result = new ArrayList();

        for (int i = piece.locationX; i < (piece.locationX + piece.shapeWidth); i++) {

            for (int j = piece.locationY; j < (piece.locationY + piece.shapeHeight); j++) {

                if ('0' == piece.shapeCellRows[j - piece.locationY].charAt(i - piece.locationX)) {

                    Coordinates coordinates = new Coordinates();
                    coordinates.x = i;
                    coordinates.y = j;
                    result.add(coordinates);
                }
            }
        }

        return result;
    }

    private int randomInt(int min, int max)
    {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private PieceOnBoard spawnNewPiece(String newPieceType)
    {
        String newPieceShape = getPieceShapesByType().get(newPieceType);

        String[] shapeRows = newPieceShape.split("\r\n");

        int shapeHeight = shapeRows.length;

        int shapeWidth = shapeRows[0].length();

        Integer locationX = null;
        Integer locationY = null;
        BoardPosition locationPosition = null;

        while (null == locationX || null == locationY
            || doesPositionContainWall(locationPosition)
            || doesPositionContainCheese(locationPosition)
            || doesPositionContainEnemy(locationPosition)
            || doesPositionContainProtagonist(locationPosition))
        {
            locationX = randomInt(0, (getCoordinateFromDrawingSize(canvas.getWidth()) - blocksPerCell) / blocksPerCell) * blocksPerCell;

            locationY = randomInt(0, (getCoordinateFromDrawingSize(canvas.getHeight()) - blocksPerCell) / blocksPerCell) * blocksPerCell;

            locationPosition = boardPositions.get(locationY).get(locationX);
        }

        PieceOnBoard result = new PieceOnBoard();
        result.type = newPieceType;
        result.shapeHeight = shapeHeight;
        result.shapeWidth = shapeWidth;
        result.shapeCellRows = shapeRows;
        result.locationX = locationX;
        result.locationY = locationY;
        result.color = getPieceColorsByType().get(newPieceType);

        boardPositions.get(result.locationY).get(result.locationX).contents.add(result);

        return result;
    }

    private PieceOnBoard spawnNewProtagonist()
    {
        return spawnNewPiece("protagonist");
    }

    private PieceOnBoard spawnNewWall()
    {
        return spawnNewPiece("wall");
    }

    private PieceOnBoard spawnNewCheese()
    {
        return spawnNewPiece("cheese");
    }

    private PieceOnBoard spawnNewEnemy()
    {
        return spawnNewPiece("enemy");
    }

    private PieceOnBoard clonePiece(PieceOnBoard originalPiece)
    {
        PieceOnBoard result = new PieceOnBoard();
        result.type = originalPiece.type;
        result.shapeHeight = originalPiece.shapeHeight;
        result.shapeWidth = originalPiece.shapeWidth;
        result.shapeCellRows = originalPiece.shapeCellRows;
        result.locationX = originalPiece.locationX;
        result.locationY = originalPiece.locationY;
        result.color = originalPiece.color;

        return result;
    }

    private boolean doesPositionContainCheese(BoardPosition position)
    {
        for (int i = 0; i < position.contents.size(); i++) {

            PieceOnBoard content = position.contents.get(i);

            if (Objects.equals("cheese", content.type)) {

                return true;
            }
        }

        return false;
    }

    private boolean doesPositionContainEnemy(BoardPosition position)
    {
        for (int i = 0; i < position.contents.size(); i++) {

            PieceOnBoard content = position.contents.get(i);

            if (Objects.equals("enemy", content.type)) {

                return true;
            }
        }

        return false;
    }

    private boolean doesPositionContainProtagonist(BoardPosition position)
    {
        for (int i = 0; i < position.contents.size(); i++) {

            PieceOnBoard content = position.contents.get(i);

            if (Objects.equals("protagonist", content.type)) {

                return true;
            }
        }

        return false;
    }

    private boolean doesPositionContainWall(BoardPosition position)
    {
        for (int i = 0; i < position.contents.size(); i++) {

            PieceOnBoard content = position.contents.get(i);

            if (Objects.equals("wall", content.type)) {

                return true;
            }
        }

        return false;
    }

    private void displayEndGameMessage()
    {
        gameStatusDisplay.setText(isGameWon ? "Victory!" : "Defeat!");

        isEndGameMessageDisplayed = true;
    }

    private void hideEndGameMessage()
    {
        gameStatusDisplay.setText("");

        isEndGameMessageDisplayed = false;
    }

    private void moveProtagonist(PieceOnBoard pieceOnBoard, int boardHeight, int boardWidth)
    {
        boolean wasPieceMoved = false;

        BoardPosition originalPosition = boardPositions.get(pieceOnBoard.locationY).get(pieceOnBoard.locationX);

        if (Objects.equals("Left", userCommandsQueue.get(0))) {

            if (pieceOnBoard.locationX >= blocksPerCell) {

                int newLocationX = pieceOnBoard.locationX - blocksPerCell;

                BoardPosition newPosition = boardPositions.get(pieceOnBoard.locationY).get(newLocationX);

                if (doesPositionContainEnemy(newPosition)) {

                    isGameLost = true;
                }

                if (doesPositionContainCheese(newPosition)) {

                    isGameWon = true;
                }

                if (newPosition.contents.size() < 1 || doesPositionContainEnemy(newPosition) || doesPositionContainCheese(newPosition)) {

                    pieceOnBoard.locationX = newLocationX;
                    wasPieceMoved = true;
                }
            }
        }
        else if (Objects.equals("Right", userCommandsQueue.get(0))) {

            if (pieceOnBoard.locationX <= boardWidth - (blocksPerCell * 2)) {

                int newLocationX = pieceOnBoard.locationX + blocksPerCell;

                BoardPosition newPosition = boardPositions.get(pieceOnBoard.locationY).get(newLocationX);

                if (doesPositionContainEnemy(newPosition)) {

                    isGameLost = true;
                }

                if (doesPositionContainCheese(newPosition)) {

                    isGameWon = true;
                }

                if (newPosition.contents.size() < 1 || doesPositionContainEnemy(newPosition) || doesPositionContainCheese(newPosition)) {

                    pieceOnBoard.locationX = newLocationX;
                    wasPieceMoved = true;
                }
            }
        }
        else if (Objects.equals("Down", userCommandsQueue.get(0))) {

            if (pieceOnBoard.locationY <= boardHeight - (blocksPerCell * 2)) {

                int newLocationY = pieceOnBoard.locationY + blocksPerCell;

                BoardPosition newPosition = boardPositions.get(newLocationY).get(pieceOnBoard.locationX);

                if (doesPositionContainEnemy(newPosition)) {

                    isGameLost = true;
                }

                if (doesPositionContainCheese(newPosition)) {

                    isGameWon = true;
                }

                if (newPosition.contents.size() < 1 || doesPositionContainEnemy(newPosition) || doesPositionContainCheese(newPosition)) {

                    pieceOnBoard.locationY = newLocationY;
                    wasPieceMoved = true;
                }
            }
        }
        else if (Objects.equals("Up", userCommandsQueue.get(0))) {

            if (pieceOnBoard.locationY  >= blocksPerCell) {

                int newLocationY = pieceOnBoard.locationY - blocksPerCell;

                BoardPosition newPosition = boardPositions.get(newLocationY).get(pieceOnBoard.locationX);

                if (doesPositionContainEnemy(newPosition)) {

                    isGameLost = true;
                }

                if (doesPositionContainCheese(newPosition)) {

                    isGameWon = true;
                }

                if (newPosition.contents.size() < 1 || doesPositionContainEnemy(newPosition) || doesPositionContainCheese(newPosition)) {

                    pieceOnBoard.locationY = newLocationY;
                    wasPieceMoved = true;
                }
            }
        }

        if (wasPieceMoved) {

            userCommandsQueue.remove(0);

            List<PieceOnBoard> contents = new ArrayList<>();

            for (int i = 0; i < originalPosition.contents.size(); i += 0) {

                if (Objects.equals("protagonist", originalPosition.contents.get(i).type)) {

                    PieceOnBoard content = originalPosition.contents.remove(i);

                    contents.add(content);
                }
                else {

                    i++;
                }
            }

            BoardPosition newPosition = boardPositions.get(pieceOnBoard.locationY).get(pieceOnBoard.locationX);

            for (int i = 0; i < contents.size(); i++) {

                PieceOnBoard content = contents.get(i);

                newPosition.contents.add(0, content);
            }

            if (displayCurrentSquareDebugInfo) {

                currentSquareInfo.setText(new Gson().toJson(newPosition.contents));
            }
        }
        else {

            userCommandsQueue.remove(0);
        }
    }

    private void applyUserCommands(int boardHeight, int boardWidth)
    {
        for (int i = 0; i < protagonistsOnBoard.size(); i++) {

            PieceOnBoard pieceOnBoard = protagonistsOnBoard.get(i);

            if (userCommandsQueue.size() > 0) {

                moveProtagonist(pieceOnBoard, boardHeight, boardWidth);
            }
        }
    }

    private void moveEnemy(PieceOnBoard pieceOnBoard, int boardHeight, int boardWidth)
    {
        boolean wasPieceMoved = false;

        BoardPosition originalPosition = boardPositions.get(pieceOnBoard.locationY).get(pieceOnBoard.locationX);

        if (Objects.equals("Stop", aiCommandsQueue.get(0))) {

        }
        else if (Objects.equals("Left", aiCommandsQueue.get(0))) {

            if (pieceOnBoard.locationX >= blocksPerCell) {

                int newLocationX = pieceOnBoard.locationX - blocksPerCell;

                BoardPosition newPosition = boardPositions.get(pieceOnBoard.locationY).get(newLocationX);

                if (doesPositionContainProtagonist(newPosition)) {

                    isGameLost = true;
                }

                if (newPosition.contents.size() < 1
                    || doesPositionContainEnemy(newPosition)
                    || doesPositionContainCheese(newPosition)
                    || doesPositionContainProtagonist(newPosition)) {

                    pieceOnBoard.locationX = newLocationX;
                    wasPieceMoved = true;
                }
            }
        }
        else if (Objects.equals("Right", aiCommandsQueue.get(0))) {

            if (pieceOnBoard.locationX <= boardWidth - (blocksPerCell * 2)) {

                int newLocationX = pieceOnBoard.locationX + blocksPerCell;

                BoardPosition newPosition = boardPositions.get(pieceOnBoard.locationY).get(newLocationX);

                if (doesPositionContainProtagonist(newPosition)) {

                    isGameLost = true;
                }

                if (newPosition.contents.size() < 1
                    || doesPositionContainEnemy(newPosition)
                    || doesPositionContainCheese(newPosition)
                    || doesPositionContainProtagonist(newPosition)) {

                    pieceOnBoard.locationX = newLocationX;
                    wasPieceMoved = true;
                }
            }
        }
        else if (Objects.equals("Down", aiCommandsQueue.get(0))) {

            if (pieceOnBoard.locationY <= boardHeight - (blocksPerCell * 2)) {

                int newLocationY = pieceOnBoard.locationY + blocksPerCell;

                BoardPosition newPosition = boardPositions.get(newLocationY).get(pieceOnBoard.locationX);

                if (doesPositionContainProtagonist(newPosition)) {

                    isGameLost = true;
                }

                if (newPosition.contents.size() < 1
                    || doesPositionContainEnemy(newPosition)
                    || doesPositionContainCheese(newPosition)
                    || doesPositionContainProtagonist(newPosition)) {

                    pieceOnBoard.locationY = newLocationY;
                    wasPieceMoved = true;
                }
            }
        }
        else if (Objects.equals("Up", aiCommandsQueue.get(0))) {

            if (pieceOnBoard.locationY >= blocksPerCell) {

                int newLocationY = pieceOnBoard.locationY - blocksPerCell;

                BoardPosition newPosition = boardPositions.get(newLocationY).get(pieceOnBoard.locationX);

                if (doesPositionContainProtagonist(newPosition)) {

                    isGameLost = true;
                }

                if (newPosition.contents.size() < 1
                    || doesPositionContainEnemy(newPosition)
                    || doesPositionContainCheese(newPosition)
                    || doesPositionContainProtagonist(newPosition)) {

                    pieceOnBoard.locationY = newLocationY;
                    wasPieceMoved = true;
                }
            }
        }

        aiCommandsQueue.remove(0);

        if (wasPieceMoved) {

            PieceOnBoard content = originalPosition.contents.remove(0);

            BoardPosition newPosition = boardPositions.get(pieceOnBoard.locationY).get(pieceOnBoard.locationX);

            newPosition.contents.add(0, content);

            if (doesPositionContainProtagonist(newPosition) && doesPositionContainEnemy(newPosition)) {

                isGameLost = true;
            }

            if (doesPositionContainProtagonist(newPosition) && displayCurrentSquareDebugInfo) {

                currentSquareInfo.setText(new Gson().toJson(newPosition.contents));
            }
        }
    }

    private void applyAiCommands(int boardHeight, int boardWidth)
    {
        for (int i = 0; i < enemiesOnBoard.size(); i++) {

            PieceOnBoard pieceOnBoard = enemiesOnBoard.get(i);

            String aiCommand = generateAiCommand(pieceOnBoard, boardHeight, boardWidth);

            aiCommandsQueue.add(aiCommand);

            if (aiCommandsQueue.size() > 0) {

                moveEnemy(pieceOnBoard, boardHeight, boardWidth);
            }
        }
    }

    private String generateAiCommand(PieceOnBoard pieceOnBoard, int boardHeight, int boardWidth)
    {
        BoardPosition position = boardPositions.get(pieceOnBoard.locationY).get(pieceOnBoard.locationX);

        if (doesPositionContainProtagonist(position)) {

            return "Stop";
        }

        if (pieceOnBoard.locationY >= blocksPerCell) {

            int newLocationUpY = pieceOnBoard.locationY - blocksPerCell;

            BoardPosition newPositionUp = boardPositions.get(newLocationUpY).get(pieceOnBoard.locationX);

            if (doesPositionContainProtagonist(newPositionUp)) {

                return "Up";
            }
        }

        if (pieceOnBoard.locationY <= boardHeight - (blocksPerCell * 2)) {

            int newLocationDownY = pieceOnBoard.locationY + blocksPerCell;

            BoardPosition newPositionDown = boardPositions.get(newLocationDownY).get(pieceOnBoard.locationX);

            if (doesPositionContainProtagonist(newPositionDown)) {

                return "Down";
            }
        }

        if (pieceOnBoard.locationX >= blocksPerCell) {

            int newLocationLeftX = pieceOnBoard.locationX - blocksPerCell;

            BoardPosition newPositionLeft = boardPositions.get(pieceOnBoard.locationY).get(newLocationLeftX);

            if (doesPositionContainProtagonist(newPositionLeft)) {

                return "Left";
            }
        }

        if (pieceOnBoard.locationX <= boardWidth - (blocksPerCell * 2)) {

            int newLocationRightX = pieceOnBoard.locationX + blocksPerCell;

            BoardPosition newPositionRight = boardPositions.get(pieceOnBoard.locationY).get(newLocationRightX);

            if (doesPositionContainProtagonist(newPositionRight)) {

                return "Right";
            }
        }

        return possibleAiCommands[randomInt(0, possibleAiCommands.length - 1)];
    }

    private String calculateBackgroundColor()
    {
        if (isGameWon) {

            return "#aaffaa";
        }

        if (isGameLost) {

            return "#ffaaaa";
        }

        return "#e5e5e5";
    }

    private void drawFrame()
    {
        int coordinateHeight = getCoordinateFromDrawingSize(canvas.getHeight());
        int coordinateWidth = getCoordinateFromDrawingSize(canvas.getWidth());

        if (!isGameWon && !isGameLost) {

            applyAiCommands(coordinateHeight, coordinateWidth);
        }

        if ((isGameLost || isGameWon) && !isEndGameMessageDisplayed) {

            displayEndGameMessage();
        }

        if (!isGameWon && !isGameLost) {

            applyUserCommands(coordinateHeight, coordinateWidth);
        }

        if ((isGameLost || isGameWon) && !isEndGameMessageDisplayed) {

            displayEndGameMessage();
        }

        if ((isGameLost || isGameWon) && !wasGameStateResetRequested) {

            wasGameStateResetRequested = true;

            new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        resetGameState();
                    }
                },
                3000);
        }

        if (protagonistsOnBoard.size() < 1) {

            PieceOnBoard newPiece = spawnNewProtagonist();

            protagonistsOnBoard.add(newPiece);
        }

        while (wallsOnBoard.size() < 25) {

            PieceOnBoard newPiece = spawnNewWall();

            wallsOnBoard.add(newPiece);
        }

        while (cheesesOnBoard.size() < 1) {

            PieceOnBoard newPiece = spawnNewCheese();

            cheesesOnBoard.add(newPiece);
        }

        while (enemiesOnBoard.size() < 3) {

            PieceOnBoard newPiece = spawnNewEnemy();

            enemiesOnBoard.add(newPiece);
        }

        paint.setColor(Color.parseColor(calculateBackgroundColor()));

        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        gameDisplay.setBackgroundDrawable(new BitmapDrawable(bitmap));

        for (int i = 0; i < wallsOnBoard.size(); i++) {

            PieceOnBoard pieceOnBoard = wallsOnBoard.get(i);

            drawPiece(pieceOnBoard);
        }

        for (int i = 0; i < cheesesOnBoard.size(); i++) {

            PieceOnBoard pieceOnBoard = cheesesOnBoard.get(i);

            drawPiece(pieceOnBoard);
        }

        for (int i = 0; i < enemiesOnBoard.size(); i++) {

            PieceOnBoard pieceOnBoard = enemiesOnBoard.get(i);

            drawPiece(pieceOnBoard);
        }

        for (int i = 0; i < protagonistsOnBoard.size(); i++) {

            PieceOnBoard pieceOnBoard = protagonistsOnBoard.get(i);

            drawPiece(pieceOnBoard);
        }

        setupNextFrame();
    }

    private void setupNextFrame()
    {
        new android.os.Handler().postDelayed(
            new Runnable() {
                public void run() {
                    drawFrame();
                }
            },
            200);
    }

    private void resetGameState()
    {
        protagonistsOnBoard = new ArrayList();

        wallsOnBoard = new ArrayList();

        cheesesOnBoard = new ArrayList();

        enemiesOnBoard = new ArrayList();

        boardPositions
            = generateEmptyBoardPositions(
                getCoordinateFromDrawingSize(canvas.getWidth()),
                getCoordinateFromDrawingSize(canvas.getHeight()));

        userCommandsQueue = new ArrayList();

        aiCommandsQueue = new ArrayList();

        isGameWon = false;

        isGameLost = false;

        hideEndGameMessage();

        wasGameStateResetRequested = false;
    }

    private void startDrawing()
    {
        resetGameState();

        setupNextFrame();
    }

    public void upButtonOnClick(View view)
    {
        userCommandsQueue.add("Up");
    }

    public void leftButtonOnClick(View view)
    {
        userCommandsQueue.add("Left");
    }

    public void rightButtonOnClick(View view)
    {
        userCommandsQueue.add("Right");
    }

    public void downButtonOnClick(View view)
    {
        userCommandsQueue.add("Down");
    }
}