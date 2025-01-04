import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args){
        Board board = new Board();
        //board.loadFromFen("r5rk/2p1Nppp/3p3P/pp2p1P1/4P3/2qnPQK1/8/R6R w - - 1 0");
        Engine myEngine = new Engine();

        //Parameters
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int cutoff = 5;
        HashMap<Long, Engine.MinimaxInfo> transposition_table = new HashMap<>();

        Engine.MinimaxInfo engine_choice;
        int move_counter = 0;
        while (!board.isMated() && !board.isDraw() && !board.isStaleMate()){
            System.out.println(board);
            engine_choice = myEngine.Think(board,transposition_table,alpha,beta,cutoff);
            //engine_choice = myEngine.Search(board, transposition_table, alpha, beta, cutoff, 0);
            Move engineMove = engine_choice.move;
            int engine_state_value = engine_choice.state_value;
            List<Move> engine_move_line = engine_choice.main_line;
            board.doMove(engineMove);
            System.out.println("Engine move:" + engineMove+ " State value:" + (float)engine_state_value/100 + " Line calculated:" + engine_move_line + " Transpo size:" + transposition_table.size()
            + " Total prunes:" + myEngine.TOTAL_PRUNES);
            move_counter++;
        }
        System.out.println("Draw?:" + board.isDraw());
        System.out.println("Number of moves:"+move_counter);
        System.out.println(board);
        System.out.println(board.getFen());
    }
}