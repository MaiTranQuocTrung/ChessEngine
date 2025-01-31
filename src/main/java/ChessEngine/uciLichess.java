package ChessEngine;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class uciLichess {
    public static void main(String[] args) throws Exception {
        //Declare new board
        Board board = new Board();
        //Declare my engine class
        Engine engine = new Engine();
        //Declare smol opening book
        SmallOpeningBook smallOpeningBook = new SmallOpeningBook();
        //Other readers
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Writer writer = new OutputStreamWriter(System.out);
        String input;

        while ((input = reader.readLine()) != null) {
            // Extract the prefix of the input to match the switch case
            String command = input.split(" ")[0]; // Extracts the command (e.g., "uci", "isready", etc.)
            String fen = "";
            switch (command) {
                case "uci":
                    writer.write("uciok\n");
                    writer.flush();
                    break;

                case "isready":
                    writer.write("readyok\n");
                    writer.flush();
                    break;

                case "position":
                    fen = toFen(input);
                    board.loadFromFen(fen);
                    writer.flush();
                    break;

                case "go":
                    if(smallOpeningBook.openingBook.containsKey(board.getZobristKey())){
                        System.out.println("Play opening move");
                        Move openingMove = smallOpeningBook.openingBook.get(board.getZobristKey());
                        writer.write("bestmove " + openingMove + "\n");
                    }
                    else {
                        //Get the time to search
                        long timeSearch = timeManagement(board, input);
                        // Running my engine
                        Engine.MinimaxInfo engine_choice = engine.Think(board, timeSearch);
                        //Getting move
                        Move bestMove = engine_choice.move;
                        int engine_state_value = engine_choice.state_value;
                        //Printing debug
                        System.out.println("Best move:" + bestMove + " State value:" + (float) engine_state_value / 100);
                        //Write to console
                        writer.write("bestmove " + bestMove + "\n");
                    }
                    writer.flush();
                    break;

                case "quit":
                    writer.close();
                    return;

                default:
                    writer.write("Unknown command\n");
                    writer.flush();
                    break;
            }
        }
    }
    public static String toFen(String input){
        Board board = new Board();
        for (String i : input.split(" ")){
            if (!i.equals("position") && !i.equals("startpos") && !i.equals("moves")){
                board.doMove(i);
            }
        }
        return board.getFen();
    }

    public static long timeManagement(Board board,String input){
        String side;
        String time = "10000";
        if (board.getSideToMove() == Side.WHITE){
            side = "wtime";
        }
        else{
            side = "btime";
        }
        String[] inputParts = input.split(" ");

        for (int i = 0; i < inputParts.length; i++){
            if (inputParts[i].equals(side) && i + 1 < inputParts.length){
                time = inputParts[i + 1];
            }
        }

        // Division factor
        long remainingTime = Long.parseLong(time);
        int divisionFactor;

        if (remainingTime > 300000) {     // More than 5 minutes
            divisionFactor = 50;         // Very conservative
        } else if (remainingTime > 120000) { // 2-5 minutes
            divisionFactor = 40;
        } else if (remainingTime > 60000) {  // 1-2 minutes
            divisionFactor = 30;
        } else if (remainingTime > 30000) {  // 30-60 seconds
            divisionFactor = 25;
        } else {                           // Less than 30 seconds
            divisionFactor = 20;           // Use time aggressively
        }

        long timeLong = Long.parseLong(time)/divisionFactor;
        // I don't want the bot to think pass 20 seconds
        if (timeLong >= 20000){
            return 20000;
        }
        return timeLong;
    }
}
