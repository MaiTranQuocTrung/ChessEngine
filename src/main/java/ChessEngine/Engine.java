package ChessEngine;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/*
Search:
- Alpha beta pruning
- Transposition table (move ordering + reuse positions)
- Q search
- Check search extension
- MVV-LVA sorted moves
- 2 Killer moves
- Iterative deepening (time constraint + move ordering)
- Null move pruning
- Aspiration Window
- Reverse Futility Pruning
- Delta pruning
- PVS
Evaluation:
- Tampered eval (Game phase decided by number of pieces on the board)
- Total material (weighted by number of pieces)
- Piece square table (weighted by number of pieces)
- Reward passed pawns
 */

public class Engine{
    // Classes for eval, move ordering, TT
    private final Helper boardHelper = new Helper();
    private final SimpleEval simpleEval = new SimpleEval();
    // Approx 1GB TT
    private final TranspositionTable TT = new TranspositionTable(1024);
    private static final int MATE_SCORE = 1000000;
    // Search debug info
    int TOTAL_PRUNES;
    int TOTAL_NODES;
    // Null move pruning params
    int R = 2;

    public static class MinimaxInfo{
        public int state_value;
        public Move move;
        public List<Move> main_line;
        public int depth;

        public MinimaxInfo(int state_value, Move move) {
            this(state_value, move, null, 0);
        }

        // Used to store the line the engine found
        public MinimaxInfo(int state_value, Move move, List<Move>main_line, int depth) {
            this.state_value = state_value;
            this.move = move;
            this.main_line = main_line != null ? main_line : new ArrayList<>();
            this.depth = depth;
        }
    }

    public MinimaxInfo Think(Board board, long searchTime) {
        int depth = 1;
        MinimaxInfo bestChoice = null;
        SearchManager timeManager = new SearchManager(searchTime);

        if (TT.getCapacity() >= 100) {
            TT.clear();
        }

        while (depth <= 64) {
            TOTAL_NODES = 0;
            Instant starts = Instant.now();
            int aspirationWindow = 40;
            MinimaxInfo currChoice;

            while (true) {
                int alpha = bestChoice != null ? bestChoice.state_value - aspirationWindow : -MATE_SCORE;
                int beta = bestChoice != null ? bestChoice.state_value + aspirationWindow : MATE_SCORE;

                currChoice = Search(board, alpha, beta, depth, 0, timeManager, 0);

                // If score is inside the window (aka. alpha < score < beta) then we can proceed to next depth
                int score = currChoice.state_value;
                if(alpha < score && score < beta){
                    break;
                }
                //Widen the window
                aspirationWindow *= 2;

            }

            Instant end = Instant.now();
            long timeElapsed = Duration.between(starts, end).toMillis();
            long nps = (long)(TOTAL_NODES / Math.max(timeElapsed / 1000.0, 0.001));

            System.out.printf("Depth: %-2d | Time: %-5d | NPS: %-7d | Eval: %6.2f | Result Depth: %-2d | Line: %s%n",
                    depth,
                    timeElapsed,
                    nps,
                    (float)currChoice.state_value/100,
                    currChoice.depth,
                    currChoice.main_line
            );

            if (currChoice.move != null) {
                bestChoice = currChoice;
                boardHelper.iterativeDeepeningMove(bestChoice.move);
            }

            if (timeManager.shouldCancel()) {
                break;
            }
            depth++;
        }
        return bestChoice;
    }


    // Sorting by MVV-LVA and TT + checks and promotions
    private List<Move> moveGenerator(Board board, boolean isCapture, int ply){
        if (isCapture){
            return boardHelper.sortMoves(board, board.pseudoLegalCaptures(), TT, ply,true);
        }
        return boardHelper.sortMoves(board,board.pseudoLegalMoves(), TT, ply,false);
    }


    // Search through capture and check moves to give an accurate eval of quiet positions
    private int QSearch(Board board, int alpha, int beta, int ply) {
        TOTAL_NODES++;

        if (board.isDraw()) {
            return 0;
        }

        int stand_pat = simpleEval.positionalEvaluation(board);
        int bestValue = stand_pat;

        if (stand_pat >= beta) {
            TOTAL_PRUNES++;
            return stand_pat;
        }

        // Delta pruning. Even if you capture a queen along with the move it still doesn't improve the position
        int BIG_DELTA = 1025;
        // Deactivate delta pruning for endgame transition
        if(stand_pat < alpha - BIG_DELTA && simpleEval.gamePhase(board)[1] < 10){
            TOTAL_PRUNES++;
            return alpha;
        }

        if (alpha < stand_pat) {
            alpha = stand_pat;
        }

        for (Move move : moveGenerator(board, true, ply)) {
                board.doMove(move);
                int score = -QSearch(board, -beta, -alpha, ply + 1);
                board.undoMove();

                if (score >= beta) {
                    TOTAL_PRUNES++;
                    return score;
                }
                if (score > bestValue){
                    bestValue = score;
                }
                if (score > alpha) {
                    alpha = score;
                }
        }
        return bestValue;
    }

    private MinimaxInfo Search(Board board, int alpha, int beta, int depth, int ply, SearchManager timeManager, int numExtension) {
        TOTAL_NODES++;

        if (depth <= 0) {
            int score = QSearch(board, alpha, beta, ply);
            return new MinimaxInfo(score, null);
        }

        // Time management check
        if (timeManager.shouldCancel()) {
            return new MinimaxInfo(0, null);
        }

        // Draw detection
        if (board.isDraw() || board.isStaleMate() || board.isRepetition(1)) {
            return new MinimaxInfo(0, null);
        }
        // Checkmate detection
        if (board.isMated()) {
            return new MinimaxInfo(-MATE_SCORE + ply, null);
        }

        // Transposition table lookup
        TranspositionTable.Entry entry = TT.getEntry(board.getZobristKey());
        if (entry != null && entry.move != null && entry.depth >= depth && ply > 0) {
            int entryValue = entry.value;
            Move entryMove = entry.move;
            List<Move> entryLine = entry.mainLine;
            int entryDepth = entry.depth;

            // Adjust the bestValue based on the flag. Alpha, beta pruning based on flag type.
            if (entry.flag == FLAG.EXACT) {
                return new MinimaxInfo(entryValue, entryMove, entryLine, entryDepth);
            } else if (entry.flag == FLAG.LOWER && entryValue > beta) {
                return new MinimaxInfo(entryValue, entryMove, entryLine, entryDepth);
            } else if (entry.flag == FLAG.UPPER && entryValue <= alpha) {
                return new MinimaxInfo(entryValue, entryMove, entryLine, entryDepth);
            }
        }

        // PVS search params
        int moveCounter = 0;
        int bestValue = -Integer.MAX_VALUE;
        Move bestMove = null;

        //Move lists
        List<Move> bestLine = new ArrayList<>();

        //Pruning params
        boolean isKingAttacked = board.isKingAttacked();
        boolean nullWindow = alpha == beta - 1;

        //Reverse Futility Pruning
        int futilityMargin = 150 * depth;
        //If your score is so good you can take a big hit and still get the beta cutoff, go for it
        //Only apply when we are at a relatively low depth, at deeper depths there are more tactical nuances
        if (depth < 5 && simpleEval.positionalEvaluation(board) > beta + futilityMargin
                && !isKingAttacked && numExtension == 0 && nullWindow){
            return new MinimaxInfo(simpleEval.positionalEvaluation(board),null);
        }

        //Null move pruning
        // If you skip a turn and still manage to obtain a beta cutoff then go for it
        if (depth > 3 && simpleEval.positionalEvaluation(board) >= beta
                && !isKingAttacked && boardHelper.nullMovePruning(board) && numExtension == 0 && nullWindow){
            board.doNullMove();
            MinimaxInfo childInfo = Search(board, -beta, beta + 1, depth - R, ply + 1, timeManager, numExtension);
            int score = -childInfo.state_value;
            Move move = childInfo.move;
            board.undoMove();
            if (score >= beta){
                TT.store(board.getZobristKey(), depth, score, FLAG.LOWER, move, null);
                return new MinimaxInfo(score,null);
            }
        }

        // Main search loop
        for (Move move : moveGenerator(board, false, ply)) {
            board.doMove(move);
            int extension = numExtension < 16 && board.isKingAttacked() ? 1 : 0;
            MinimaxInfo childInfo;
            int score;
            moveCounter++;
            /*
            Principle variation search:
            The idea is that our move ordering is so good is that most likely the best move will always be first.
            So search the first move at full window but narrow the window down for the other moves,
            but if we do find a better move (a move that raises the current alpha value) then we must research it at full window.
             */
            if(moveCounter == 1){
                childInfo = Search(board, -beta, -alpha, depth - 1 + extension, ply + 1, timeManager, numExtension + extension);
                score = -childInfo.state_value;
            }
            //Else do a narrow search
            else{
                childInfo = Search(board, - alpha - 1, -alpha, depth - 1 + extension, ply + 1, timeManager, numExtension + extension);
                score = -childInfo.state_value;
                //If it turns out we found a better move then do a full search
                if (score > alpha){
                    childInfo = Search(board, -beta, -alpha, depth - 1 + extension, ply + 1, timeManager, numExtension + extension);
                    score = -childInfo.state_value;
                }
            }
            board.undoMove();

            // If time is up return the best move found so far.
            // Since we always place iterative deepening move first there is no risk of taking partial searches
            if (timeManager.shouldCancel()) {
                //If this is wrong revert to taking null and 0
                return new MinimaxInfo(bestValue, bestMove, bestLine, depth);
            }

            if (score > bestValue) {
                bestValue = score;
                bestMove = move;

                bestLine.clear();
                bestLine.add(move);
                bestLine.addAll(childInfo.main_line);
                // We found a new highest valued move in the position. Record it for future pruning
                if (score > alpha) {
                    alpha = score;
                }
            }
            // Beta cutoff, the tree is pruned, no further nodes will be explored
            if (score >= beta) {
                TOTAL_PRUNES++;
                // Killer Move is a quiet move which caused a beta-cutoff
                if (bestMove != null && !boardHelper.isCapture(board, bestMove)){
                    boardHelper.killerMoves[1][ply] = boardHelper.killerMoves[0][ply];
                    boardHelper.killerMoves[0][ply] = bestMove;
                }
                break;
            }
        }
        // Store position in transposition table
        FLAG flag;
        if (bestValue >= beta) {
            flag = FLAG.LOWER;
        } else if (bestValue > alpha) {
            flag = FLAG.EXACT;
        } else {
            flag = FLAG.UPPER;
        }
        TT.store(board.getZobristKey(),  depth, bestValue, flag, bestMove, bestLine);
        return new MinimaxInfo(bestValue, bestMove, bestLine, depth);
    }

    public static void main(String[] args){
        Board board = new Board();
        //board.loadFromFen("3rr1k1/pp3ppp/3b4/2p5/2Q5/6qP/PPP1B1P1/R1B2K1R b - - 0 1");
        Engine myEngine = new Engine();

        MinimaxInfo engine_choice;
        while (!board.isMated() && !board.isDraw() && !board.isStaleMate()){
            System.out.println(board);
            engine_choice = myEngine.Think(board, 10000);
            Move engineMove = engine_choice.move;
            int engine_state_value = engine_choice.state_value;
            board.doMove(engineMove);
            System.out.println("Engine move:" + engineMove + " State value:" + (float) engine_state_value / 100 + " Total prunes:" + myEngine.TOTAL_PRUNES + " Depth:" + engine_choice.depth);
        }
        System.out.println("Draw?:" + board.isDraw());
        System.out.println(board);
        System.out.println(board.getFen());
    }
}