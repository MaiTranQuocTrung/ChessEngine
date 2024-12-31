import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.Side;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.time.StopWatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
Search:
- Alpha beta pruning
- Transposition table
- Q search
- Shortest mating sequence
- MVV-LVA sorted moves
Evaluation:
- Tampered eval
- Total material (weighted by number of pieces)
- Piece square table (weighted by number of pieces)
- Simple mobility

To do:
- Iterative deepening!
- More pruning techniques
 */

public class Engine {
    Evaluation evaluation = new Evaluation();
    Helper boardHelper = new Helper();
    private static final int MATE_SCORE = 1000000;
    int total_prunes;

    public static class MinimaxInfo{
        public int state_value;
        public Move move;
        public List<Move> main_line;

        public MinimaxInfo(int state_value, Move move) {
            this.move = move;
            this.state_value = state_value;
            this.main_line = new ArrayList<>();
        }

        // Used to store the line the engine found
        public MinimaxInfo(int state_value, Move move, List<Move>main_line) {
            this.move = move;
            this.state_value = state_value;
            this.main_line = new ArrayList<>(main_line);
        }
    }

    private List<Move> actions(Board board){
        // Sorting by MVV-LVA
        return boardHelper.sortMoves(board,board.legalMoves());
    }

    private int utility(Board board){
        if (board.isMated()){
            // If it's the player's turn but its mate then the other side wins
            if(board.getSideToMove() == Side.WHITE){
                return -MATE_SCORE;
            }
            else{
                return MATE_SCORE;
            }
        }
        return 0;
    }

    // Search through capture moves to give an accurate eval of quiet positions
    private int QSearch(Board board, int alpha, int beta){
        int stand_pat = evaluation.eval(board);
        int bestMove = stand_pat;
        int score;

        if (board.getSideToMove() == Side.WHITE){
            if (stand_pat >= beta){
                total_prunes++;
                return stand_pat;
            }
            if (alpha < stand_pat){
                alpha = stand_pat;
            }

            for(Move action : actions(board)){
                if (boardHelper.isCapture(board,action)){
                    board.doMove(action);
                    score = QSearch(board,alpha,beta);
                    board.undoMove();

                    if (score >= beta){
                        total_prunes++;
                        return score;
                    }
                    if (score > bestMove){
                        bestMove = score;
                    }
                    if (score > alpha){
                        alpha = score;
                    }
                }
            }
            return bestMove;
        }
        else{
            if (stand_pat <= alpha){
                return stand_pat;
            }
            if (beta > stand_pat){
                beta = stand_pat;
            }

            for (Move action : actions(board)){
                if (boardHelper.isCapture(board,action)){
                    board.doMove(action);
                    score = QSearch(board,alpha,beta);
                    board.undoMove();

                    if (score <= alpha){
                        total_prunes++;
                        return score;
                    }
                    if (score < bestMove){
                        bestMove = score;
                    }
                    if (score < beta){
                        beta = score;
                    }
                }
            }
            return bestMove;
        }
    }

    public MinimaxInfo Search(Board board, HashMap<Long,MinimaxInfo> transposition_table, int alpha, int beta, int max_depth, int depth){
        
        if(board.isMated()){
            // Punish longer mating sequences
            int utils;
            // Black wins
            if (board.getSideToMove() == Side.WHITE){
                utils = utility(board) + depth;
            }
            // White wins
            else{
                utils = utility(board)  - depth;
            }
            return new MinimaxInfo(utils,null);
        }

        if (board.isDraw() || board.isStaleMate() || board.isInsufficientMaterial() || board.isRepetition()){
            return new MinimaxInfo(0,null);
        }
        //Only return the position if it has been evaluated until cutoff
        if(transposition_table.containsKey(board.getZobristKey())
                && transposition_table.get(board.getZobristKey()).main_line.size() == max_depth){

            return transposition_table.get(board.getZobristKey());
        }

        else if(depth == max_depth){
            int heuristic = QSearch(board,alpha,beta);
            // Remember, we won't ever need to use this minimax object
            // we only care about getting the Q Search running and getting the evals back to the nodes
            return new MinimaxInfo(heuristic,null);
        }

        else if(board.getSideToMove() == Side.WHITE){
            int value = Integer.MIN_VALUE;
            Move best_move = null;
            List<Move> best_line = new ArrayList<>();
            for(Move action : actions(board)){
                board.doMove(action);
                MinimaxInfo child_info = Search(board,transposition_table,alpha,beta,max_depth,depth+1);
                // A bit counter-intuitive (bcs recursion...) but the Q Search eval is used here and propagated up the tree
                int value2 = child_info.state_value;
                board.undoMove();
                if(value2 > value){
                    value = value2;
                    best_move = action;

                    best_line.clear();
                    best_line.add(action);
                    best_line.addAll(child_info.main_line);

                    alpha = Math.max(alpha, value);
                }
                if(value >= beta){
                    total_prunes++;
                    return new MinimaxInfo(value,best_move,best_line);
                }
            }
            MinimaxInfo info = new MinimaxInfo(value,best_move,best_line);
            transposition_table.put(board.getZobristKey(), info);
            return info;
        }
        else{
            int value = Integer.MAX_VALUE;
            Move best_move = null;
            List<Move> best_line = new ArrayList<>();
            for(Move action : actions(board)){
                board.doMove(action);
                MinimaxInfo child_info = Search(board,transposition_table,alpha,beta,max_depth,depth+1);
                int value2 = child_info.state_value;
                board.undoMove();
                if(value2 < value){
                    value = value2;
                    best_move = action;

                    best_line.clear();
                    best_line.add(action);
                    best_line.addAll(child_info.main_line);

                    beta = Math.min(beta,value);
                }
                if(value <= alpha){
                    total_prunes++;
                    return new MinimaxInfo(value,best_move,best_line);
                }
            }
            MinimaxInfo info = new MinimaxInfo(value,best_move,best_line);
            transposition_table.put(board.getZobristKey(), info);
            return info;
        }
    }
}