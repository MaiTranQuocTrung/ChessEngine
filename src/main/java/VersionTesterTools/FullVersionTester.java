package VersionTesterTools;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import ChessEngine.Engine;
import ChessEngine.OldEngine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FullVersionTester {
    public static void main (String [] args) throws IOException {
        //Reader stuff
        FileReader fr = new FileReader("src/positions");
        BufferedReader br = new BufferedReader(fr);
        String position;

        //Board stuff
        Board board = new Board();

        //Engine stuff
        OldEngine oldEngine = new OldEngine();
        Engine engine = new Engine();
        Engine.MinimaxInfo engine_choice;
        OldEngine.MinimaxInfo oldEngineChoice;

        //Win-Lose-Draw tracker
        int whiteWin = 0;
        int draw = 0;
        int blackWin = 0;

        //Position number tracker
        int i = 0;

        //Tester loop
        while((position = br.readLine()) != null){
            board.loadFromFen(position);
            //Play
            while (!board.isMated() && !board.isDraw() && !board.isStaleMate()) {
                if (board.getSideToMove() == Side.WHITE) {
                    System.out.printf("\u001B[31mPosition number %d/%d\u001B[0m\n",i,1000);
                    System.out.println(board);
                    engine_choice = engine.Think(board, 500);
                    Move engineMove = engine_choice.move;
                    int engine_state_value = engine_choice.state_value;
                    board.doMove(engineMove);
                    System.out.println("Engine move:" + engineMove + " State value:" + (float) engine_state_value / 100  + " Depth:" + engine_choice.depth);
                } else {
                    System.out.printf("\u001B[31mPosition number %d/%d\u001B[0m\n",i,1000);
                    System.out.println(board);
                    oldEngineChoice = oldEngine.Think(board, 500);
                    Move engineMove = oldEngineChoice.move;
                    int oldEngineValue = oldEngineChoice.state_value;
                    board.doMove(engineMove);
                    System.out.println("Engine move:" + engineMove + " State value:" + (float) oldEngineValue / 100 + " Total prunes:" + " Depth:" + oldEngineChoice.depth);
                }
            }
            if(board.isDraw()){draw++;}
            else if(board.getSideToMove() == Side.WHITE){blackWin++;}
            else{whiteWin++;}
            System.out.printf("\u001B[31mWHITE WIN:%d|BLACK WIN:%d|DRAW:%d\u001B[0m\n",whiteWin,blackWin,draw);
            i++;
        }
        System.out.println("WHITE WIN:"+ whiteWin);
        System.out.println("BLACK WIN:"+ blackWin);
        System.out.println("DRAW:"+draw);
    }
}
