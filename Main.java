import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args){
        Board board = new Board();
        //board.loadFromFen("3q1r2/1pk1b1Qp/3p4/2p1P3/8/4B3/PPP2PPP/4K2R w K - 1 20");
        Engine myEngine = new Engine();

        //Parameters
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int cutoff = 4;
        HashMap<Long, Engine.MinimaxInfo> transposition_table = new HashMap<>();

        Engine.MinimaxInfo engine_choice;
        int move_counter = 0;
        ArrayList<Move> move_sequence = new ArrayList<>();
        while (!board.isMated() && !board.isDraw() && !board.isStaleMate()){
            System.out.println(board);
            engine_choice = myEngine.Search(board, transposition_table, alpha, beta, cutoff, 0,false);
            Move engineMove = engine_choice.move;
            int engine_state_value = engine_choice.state_value;
            List<Move> engine_move_line = engine_choice.main_line;
            board.doMove(engineMove);
            System.out.println("Engine move:" + engineMove+ " State value:" +engine_state_value + " Line calculated:" + engine_move_line + " Transpo size:" + transposition_table.size()
            + " Total prunes:" + myEngine.total_prunes);
            move_counter++;
            move_sequence.add(engineMove);
        }
        System.out.println("Draw?:"+board.isDraw());
        System.out.println("Number of moves:"+move_counter);
        System.out.println(board);
        System.out.println(board.getFen());
        System.out.println(move_sequence);
    }
}