import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

public class uciLichess {
    public static void main(String[] args) throws Exception {
        //Declare new board
        Board board = new Board();
        //Declare my engine class
        Engine engine = new Engine();
        //Params
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        HashMap<Long, Engine.MinimaxInfo> transposition_table = new HashMap<>();

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
                    //Get the time to search
                    long timeSearch = timeManagement(board,input);
                    // Running my engine
                    Engine.MinimaxInfo engine_choice = engine.Think(board, transposition_table, alpha, beta,timeSearch);
                    //Getting move
                    Move bestMove = engine_choice.move;
                    int engine_state_value = engine_choice.state_value;
                    List<Move> engine_move_line = engine_choice.main_line;
                    //Printing debug
                    System.out.println("Best move:" + bestMove + " Engine line:" + engine_move_line + " State value:" + (float)engine_state_value/100 + " Transpo size:" + transposition_table.size());
                    //Write to console
                    writer.write("bestmove " + bestMove + "\n");
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
        long timeLong = Long.parseLong(time)/50;
        // I don't want the bot to think pass 20 seconds
        if (timeLong >= 20000){
            return 20000;
        }
        return timeLong;
    }
}
