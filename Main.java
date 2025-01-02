import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args){
        Board board = new Board();
        //board.loadFromFen("8/8/8/2P3R1/5B2/2rP1p2/p1P1PP2/RnQ1K2k w Q - 5 3");
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
            //engine_choice = myEngine.Think(board,transposition_table,alpha,beta);
            engine_choice = myEngine.Search(board, transposition_table, alpha, beta, cutoff, 0);
            Move engineMove = engine_choice.move;
            int engine_state_value = engine_choice.state_value;
            List<Move> engine_move_line = engine_choice.main_line;
            board.doMove(engineMove);
            System.out.println("Engine move:" + engineMove+ " State value:" + (float)engine_state_value/100 + " Line calculated:" + engine_move_line + " Transpo size:" + transposition_table.size()
            + " Total prunes:" + myEngine.total_prunes);
            move_counter++;
        }
        System.out.println("Draw?:" + board.isDraw());
        System.out.println("Number of moves:"+move_counter);
        System.out.println(board);
        System.out.println(board.getFen());
    }
}