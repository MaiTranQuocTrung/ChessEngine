import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.Side;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/*
Search:
- Alpha beta pruning
- Transposition table (move ordering + reuse positions)
- Q search (check + captures)
- MVV-LVA sorted moves
- Iterative deepening (time constraint + reusing TT positions, alpha and beta bounds)
Evaluation:
- Tampered eval (Game phase decided by number of pieces on the board)
- Total material (weighted by number of pieces)
- Piece square table (weighted by number of pieces)
- Simple mobility
 */

public class Engine {
    private final Evaluation evaluation = new Evaluation();
    private final Helper boardHelper = new Helper();
    private static final int MATE_SCORE = 1000000;
    int TOTAL_PRUNES;

    public static class MinimaxInfo{
        public int state_value;
        public Move move;
        public List<Move> main_line;
        public int depth;
        public FLAG flag;

        public MinimaxInfo(int state_value, Move move) {
            this.move = move;
            this.state_value = state_value;
            this.main_line = new ArrayList<>();
        }

        // Used to store the line the engine found
        public MinimaxInfo(int state_value, Move move, List<Move>main_line,FLAG flag,int depth) {
            this.move = move;
            this.state_value = state_value;
            this.main_line = main_line;
            this.depth = depth;
            this.flag = flag;
        }
    }

    public MinimaxInfo Think(Board board, HashMap<Long,MinimaxInfo> transpositionTable, int alpha, int beta,int maxDepth){
        int depth = 1;
        MinimaxInfo bestChoice = null;
        SearchManager timeManager = new SearchManager(10000);

        while (depth <= maxDepth) {
            if (timeManager.shouldCancel()) {
                break;
            }
            //Starting clock
            Instant starts = Instant.now();
            //Running search
            MinimaxInfo currChoice = Search(board, transpositionTable, alpha, beta, depth, 0,timeManager);
            //Stop clock
            Instant end = Instant.now();
            //print duration of search
            long timeElapsed = Duration.between(starts,end).toMillis();
            System.out.println("Depth:"+ depth+" TIME:"+ timeElapsed + " Eval:" + (float)currChoice.state_value/100 + " Line:" + currChoice.main_line);
            //Avoid taking none completed search
            if (currChoice.move != null){
                bestChoice = currChoice;
            }
            depth++;

        }
        return bestChoice;
    }

    private List<Move> actions(Board board, HashMap<Long, MinimaxInfo> transpositionTable){
        // Sorting by MVV-LVA and TT + checks and promotions
        return boardHelper.sortMoves(board,board.legalMoves(), transpositionTable);
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

    // Search through capture and check moves to give an accurate eval of quiet positions
    private int QSearch(Board board, int alpha, int beta, HashMap<Long, MinimaxInfo> transpositionTable){
        int stand_pat = evaluation.eval(board);
        int bestScore = stand_pat;
        int score;

        if (board.getSideToMove() == Side.WHITE){

            if (stand_pat >= beta){
                TOTAL_PRUNES++;
                return stand_pat;
            }

            alpha = Math.max(alpha,stand_pat);

            for(Move action : actions(board, transpositionTable)){
                if (boardHelper.isCapture(board,action) || boardHelper.isCheck(board,action)){
                    board.doMove(action);
                    score = QSearch(board,alpha,beta, transpositionTable);
                    board.undoMove();

                    if (score > bestScore){
                        bestScore = score;
                        alpha = Math.max(alpha,bestScore);
                    }

                    if (score >= beta){
                        TOTAL_PRUNES++;
                        return score;
                    }
                }
            }
            return bestScore;
        }
        else{
            if (stand_pat <= alpha){
                return stand_pat;
            }

            beta = Math.min(beta,stand_pat);

            for (Move action : actions(board, transpositionTable)){
                if (boardHelper.isCapture(board,action) || boardHelper.isCheck(board,action)){
                    board.doMove(action);
                    score = QSearch(board,alpha,beta, transpositionTable);
                    board.undoMove();

                    if (score < bestScore){
                        bestScore = score;
                        beta = Math.min(beta, bestScore);
                    }

                    if (score <= alpha){
                        TOTAL_PRUNES++;
                        return score;
                    }
                }
            }
            return bestScore;
        }
    }

    private MinimaxInfo Search(Board board, HashMap<Long,MinimaxInfo> transpositionTable, int alpha, int beta, int maxDepth, int depth,SearchManager timeManager){
        //Out of time
        if (timeManager.shouldCancel()){
            return new MinimaxInfo(0,null);
        }

        if(board.isMated()){
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

        //Change transposition table to return exact but also alpha beta pruning
        else if(transpositionTable.containsKey(board.getZobristKey()) && transpositionTable.get(board.getZobristKey()).depth >= maxDepth - depth){
            MinimaxInfo info = transpositionTable.get(board.getZobristKey());
            if (info.flag == FLAG.EXACT){
                return info;
            }
            else if (info.flag == FLAG.LOWER){
                alpha = Math.max(alpha, info.state_value);
            }
            else if (info.flag == FLAG.UPPER){
                beta = Math.min(beta, info.state_value);
            }
            if (alpha >= beta){
                return info;
            }
        }

        if(depth == maxDepth){
            int heuristic = QSearch(board,alpha,beta,transpositionTable);
            return new MinimaxInfo(heuristic,null);
        }

        else if(board.getSideToMove() == Side.WHITE){
            int value = Integer.MIN_VALUE;
            Move bestMove = null;
            List<Move> bestLine = new ArrayList<>();
            for(Move action : actions(board, transpositionTable)){
                board.doMove(action);
                MinimaxInfo child_info = Search(board, transpositionTable,alpha,beta, maxDepth,depth+1,timeManager);
                int value2 = child_info.state_value;
                board.undoMove();

                //Out of time, instantly exit the search
                if (timeManager.shouldCancel()){
                    return new MinimaxInfo(0,null);
                }

                if(value2 > value){
                    value = value2;
                    bestMove = action;

                    bestLine.clear();
                    bestLine.add(action);
                    bestLine.addAll(child_info.main_line);

                    alpha = Math.max(alpha, value);
                }
                if(value >= beta){
                    TOTAL_PRUNES++;
                    transpositionTable.put(board.getZobristKey(), new MinimaxInfo(value,bestMove,bestLine,FLAG.LOWER,maxDepth - depth));
                    return new MinimaxInfo(value,bestMove,bestLine,FLAG.LOWER,maxDepth - depth);
                }
            }
            MinimaxInfo info = new MinimaxInfo(value,bestMove,bestLine,FLAG.EXACT,maxDepth - depth);
            transpositionTable.put(board.getZobristKey(), info);
            return info;
        }
        else{
            int value = Integer.MAX_VALUE;
            Move bestMove = null;
            List<Move> bestLine = new ArrayList<>();
            for(Move action : actions(board, transpositionTable)){
                board.doMove(action);
                MinimaxInfo child_info = Search(board, transpositionTable,alpha,beta, maxDepth,depth+1,timeManager);
                int value2 = child_info.state_value;
                board.undoMove();

                //Out of time, instantly exit the search
                if (timeManager.shouldCancel()){
                    return new MinimaxInfo(0,null);
                }

                if(value2 < value){
                    value = value2;
                    bestMove = action;

                    bestLine.clear();
                    bestLine.add(action);
                    bestLine.addAll(child_info.main_line);

                    beta = Math.min(beta,value);
                }
                if(value <= alpha){
                    TOTAL_PRUNES++;
                    transpositionTable.put(board.getZobristKey(), new MinimaxInfo(value,bestMove,bestLine,FLAG.UPPER,maxDepth - depth));
                    return new MinimaxInfo(value,bestMove,bestLine,FLAG.UPPER,maxDepth - depth);
                }
            }
            MinimaxInfo info = new MinimaxInfo(value,bestMove,bestLine,FLAG.EXACT,maxDepth - depth);
            transpositionTable.put(board.getZobristKey(), info);
            return info;
        }
    }
}