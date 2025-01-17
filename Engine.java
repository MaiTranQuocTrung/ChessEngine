import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
- Doubled pawns punishment
- Reward passed pawns
 */

public class Engine {
    private final Evaluation evaluation = new Evaluation();
    private final Helper boardHelper = new Helper();
    private final TranspositionTable TT = new TranspositionTable(1024);
    private static final int MATE_SCORE = 1000000;
    int TOTAL_PRUNES;
    int TOTAL_NODES;
    int currAge = 0;

    public static class MinimaxInfo{
        public int state_value;
        public Move move;
        public List<Move> main_line;
        public int depth;
        public int age;
        public FLAG flag;

        public MinimaxInfo(int state_value, Move move) {
            this.move = move;
            this.state_value = state_value;
            this.main_line = new ArrayList<>();
        }

        // Used to store the line the engine found
        public MinimaxInfo(int state_value, Move move, List<Move>main_line, FLAG flag, int depth, int age) {
            this.move = move;
            this.state_value = state_value;
            this.main_line = main_line;
            this.depth = depth;
            this.flag = flag;
            this.age = age;
        }
    }

    public MinimaxInfo Think(Board board, int alpha, int beta, long searchTime){
        // Parameters
        int depth = 1;
        MinimaxInfo bestChoice = null;
        int bestChoiceDepth = 1;
        // Time manager class
        SearchManager timeManager = new SearchManager(searchTime);
        // We have to reset TT if it overflows
        if (TT.getCapacity() >= 100){
            TT.clear();
        }
        // Age increments
        TT.incrementAge();
        currAge++;
        while (depth <= 64) {
            if (timeManager.shouldCancel()) {
                break;
            }
            TOTAL_NODES = 0;
            //Starting clock
            Instant starts = Instant.now();
            //Running search
            MinimaxInfo currChoice = Search(board, alpha, beta, depth, 0,timeManager);
            //Stop clock
            Instant end = Instant.now();
            //print duration of search
            long timeElapsed = Duration.between(starts,end).toMillis();
            long nps = (long)TOTAL_NODES/(timeElapsed/1000 + 1);
            System.out.printf("VERSION 1.5 | Depth: %-2d | Time: %-5d | NPS: %-7d | Eval: %6.2f | Age: %d/%d | Result Depth: %-2d | Line: %s%n",
                    depth,
                    timeElapsed,
                    nps,
                    (float)currChoice.state_value/100,
                    currChoice.age,
                    currAge,
                    currChoice.depth,
                    currChoice.main_line
            );
            //Avoid taking none completed search
            //If at depth 1 we have a cached depth of 7 and then at depth 2 we re-search. If we run out of time use the cached result at depth 1
            if (currChoice.move != null && bestChoiceDepth <= currChoice.depth ){
                bestChoice = currChoice;
                bestChoiceDepth = bestChoice.depth;
            }
            depth++;

        }
        return bestChoice;
    }

    // Sorting by MVV-LVA and TT + checks and promotions
    private List<Move> actions(Board board, int maxDepth){
        return boardHelper.sortMoves(board,board.pseudoLegalMoves(), TT);
    }

    private List<Move> captureMoves(Board board, int maxDepth){
        return boardHelper.sortMoves(board,board.pseudoLegalCaptures(),TT);
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
    private int QSearch(Board board, int alpha, int beta, int maxDepth){
        TOTAL_NODES++;
        int stand_pat = evaluation.eval(board);
        int bestScore = stand_pat;

        if (board.isRepetition()){
            return 0;
        }

        if (board.getSideToMove() == Side.WHITE){

            if (stand_pat >= beta){
                TOTAL_PRUNES++;
                return stand_pat;
            }

            alpha = Math.max(alpha,stand_pat);

            for(Move action : captureMoves(board,maxDepth)){
                    board.doMove(action);
                    int score = QSearch(board, alpha, beta, maxDepth);
                    board.undoMove();

                    if (score > bestScore) {
                        bestScore = score;
                        alpha = Math.max(alpha, bestScore);
                    }

                    if (score >= beta) {
                        TOTAL_PRUNES++;
                        return score;
                    }
            }
            return bestScore;
        }
        else{
            if (stand_pat <= alpha){
                return stand_pat;
            }

            beta = Math.min(beta,stand_pat);

            for (Move action : captureMoves(board,maxDepth)){
                    board.doMove(action);
                    int score = QSearch(board, alpha, beta, maxDepth);
                    board.undoMove();

                    if (score < bestScore) {
                        bestScore = score;
                        beta = Math.min(beta, bestScore);
                    }

                    if (score <= alpha) {
                        TOTAL_PRUNES++;
                        return score;
                    }
            }
            return bestScore;
        }
    }

    private MinimaxInfo Search(Board board, int alpha, int beta, int maxDepth, int depth,SearchManager timeManager){
        //Out of time
        TOTAL_NODES++;
        if (timeManager.shouldCancel()){
            return new MinimaxInfo(0,null);
        }

        else if(board.isMated()){
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

        else if (board.isDraw()){
            return new MinimaxInfo(0,null);
        }

        else if (TT.containsKey(board.getZobristKey())) {
            TranspositionTable.Entry entry = TT.getEntry(board.getZobristKey());
            int entryAge = entry.age;

            if (entry.depth >= maxDepth - depth && Math.abs(TT.getCurrentAge() - entryAge) <= 1) {
                Side currSide = board.getSideToMove();

                // Extract values from the TT entry
                FLAG entryFlag = entry.flag;
                int entryValue = entry.value;
                Move entryMove = entry.move;
                List<Move> entryLine = entry.mainLine;
                int entryDepth = entry.depth;

                // Construct the MinimaxInfo object
                MinimaxInfo info = new MinimaxInfo(entryValue, entryMove, entryLine, entryFlag, entryDepth, entryAge);

                // Evaluate based on the flag
                if (entryFlag == FLAG.EXACT) {
                    return info;
                } else if (entryFlag == FLAG.LOWER) {
                    alpha = Math.max(alpha, entryValue);
                } else if (entryFlag == FLAG.UPPER) {
                    beta = Math.min(beta, entryValue);
                }

                // Prune based on the alpha-beta bounds
                if ((currSide == Side.WHITE && entryValue >= beta) ||
                        (currSide == Side.BLACK && entryValue <= alpha)) {
                    return info;
                }
            }
        }

        if(depth == maxDepth){
            int heuristic = QSearch(board,alpha,beta,maxDepth);
            return new MinimaxInfo(heuristic,null);
        }

        else if(board.getSideToMove() == Side.WHITE){
            int value = Integer.MIN_VALUE;
            Move bestMove = null;
            List<Move> bestLine = new ArrayList<>();
            for(Move action : actions(board, maxDepth)){
                board.doMove(action);
                MinimaxInfo child_info = Search(board, alpha,beta, maxDepth,depth + 1 ,timeManager);
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
                    TT.store(board.getZobristKey(),maxDepth - depth,value,FLAG.LOWER,bestMove,bestLine,TT.getCurrentAge());
                    return new MinimaxInfo(value,bestMove,bestLine,FLAG.LOWER,maxDepth - depth, TT.getCurrentAge());
                }
            }
            MinimaxInfo info = new MinimaxInfo(value,bestMove,bestLine,FLAG.EXACT,maxDepth - depth, TT.getCurrentAge());
            TT.store(board.getZobristKey(),maxDepth - depth,value,FLAG.EXACT,bestMove,bestLine,TT.getCurrentAge());
            return info;
        }
        else{
            int value = Integer.MAX_VALUE;
            Move bestMove = null;
            List<Move> bestLine = new ArrayList<>();
            for(Move action : actions(board, maxDepth)){
                board.doMove(action);
                MinimaxInfo child_info = Search(board,alpha,beta, maxDepth,depth + 1 ,timeManager);
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
                    TT.store(board.getZobristKey(),maxDepth - depth,value,FLAG.UPPER,bestMove,bestLine,TT.getCurrentAge());
                    return new MinimaxInfo(value,bestMove,bestLine,FLAG.UPPER,maxDepth - depth, TT.getCurrentAge());
                }
            }
            MinimaxInfo info = new MinimaxInfo(value,bestMove,bestLine,FLAG.EXACT,maxDepth - depth, TT.getCurrentAge());
            TT.store(board.getZobristKey(),maxDepth - depth,value,FLAG.EXACT,bestMove,bestLine,TT.getCurrentAge());
            return info;
        }
    }
}