package ChessEngine;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OldEngine {
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
        // Parameters
        int depth = 1;
        int bestChoiceDepth = 1;
        int alpha = -MATE_SCORE;
        int beta = MATE_SCORE;
        MinimaxInfo bestChoice = null;
        // Time manager class
        SearchManager timeManager = new SearchManager(searchTime);
        // We have to reset TT if it overflows
        if (TT.getCapacity() >= 100) {
            TT.clear();
        }

        while (depth <= 64) {
            TOTAL_NODES = 0;
            //Starting clock
            Instant starts = Instant.now();
            //Running search
            MinimaxInfo currChoice = Search(board, alpha, beta, depth, 0, timeManager, 0);
            //Stop clock
            Instant end = Instant.now();
            long timeElapsed = Duration.between(starts, end).toMillis();
            long nps = (long)(TOTAL_NODES / Math.max(timeElapsed / 1000.0, 0.001));

            // Print duration of search
            System.out.printf("Depth: %-2d | Time: %-5d | NPS: %-7d | Eval: %6.2f | Result Depth: %-2d | Line: %s%n",
                    depth,
                    timeElapsed,
                    nps,
                    (float)currChoice.state_value/100,
                    currChoice.depth,
                    currChoice.main_line
            );

            // Accept any valid move, even from partial searches
            if (currChoice.move != null && currChoice.depth >= bestChoiceDepth) {
                bestChoice = currChoice;
                bestChoiceDepth = currChoice.depth;
                // Update ID move so we will look at it first in move ordering
                boardHelper.iterativeDeepeningMove(bestChoice.move);
            }

            // Check if we should stop searching
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

        if (board.isRepetition()) {
            return 0;
        }

        int stand_pat = simpleEval.positionalEvaluation(board);
        int bestValue = stand_pat;

        if (stand_pat >= beta) {
            TOTAL_PRUNES++;
            return stand_pat;
        }

        if (alpha < stand_pat) {
            alpha = stand_pat;
        }

        for (Move move : moveGenerator(board, true, ply)) {
            if (!board.doMove(move)){
                continue;
            }
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

        // Quiescence search at leaf nodes
        if (depth <= 0 || ply >= 100) {
            int score = QSearch(board, alpha, beta, ply);
            return new MinimaxInfo(score, null);
        }
        // Time management check
        if (timeManager.shouldCancel()) {
            return new MinimaxInfo(0, null);
        }

        // Draw detection
        if (board.isDraw() || board.isStaleMate() || board.isRepetition()) {
            return new MinimaxInfo(0, null);
        }

        // Checkmate detection
        if (board.isMated()) {
            return new MinimaxInfo(-MATE_SCORE + ply, null);
        }

        // Transposition table lookup
        TranspositionTable.Entry entry = TT.getEntry(board.getZobristKey());
        if (entry != null && entry.depth >= depth && ply > 0) {
            int entryValue = entry.value;
            Move entryMove = entry.move;
            List<Move> entryLine = entry.mainLine;
            int entryDepth = entry.depth;

            // Adjust the bestValue based on the flag
            if (entry.flag == FLAG.EXACT) {
                return new MinimaxInfo(entryValue, entryMove, entryLine, entryDepth);
            } else if (entry.flag == FLAG.LOWER && entryValue > beta) {
                return new MinimaxInfo(entryValue, entryMove, entryLine, entryDepth);
            } else if (entry.flag == FLAG.UPPER && entryValue <= alpha) {
                return new MinimaxInfo(entryValue, entryMove, entryLine, entryDepth);
            }
        }

        int bestValue = -Integer.MAX_VALUE;
        Move bestMove = null;
        List<Move> bestLine = new ArrayList<>();

        //Null move pruning
        boolean isKingAttacked = board.isKingAttacked();
        if (depth > 3 && simpleEval.positionalEvaluation(board) >= beta
                && !isKingAttacked && boardHelper.nullMovePruning(board) && numExtension == 0){
            board.doNullMove();
            MinimaxInfo childInfo = Search(board, -beta, -beta + 1, depth - R, ply + 1, timeManager, numExtension);
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
            // Check extension
            int extension = numExtension < 16 && board.isKingAttacked() ? 1 : 0;
            MinimaxInfo childInfo = Search(board, -beta, -alpha, depth - 1 + extension, ply + 1, timeManager, numExtension + extension);
            int score = -childInfo.state_value;
            board.undoMove();

            // Time management check
            if (timeManager.shouldCancel()) {
                return new MinimaxInfo(0, null);
            }

            if (score > bestValue) {
                bestValue = score;
                bestMove = move;

                bestLine.clear();
                bestLine.add(move);
                bestLine.addAll(childInfo.main_line);

                if (score > alpha) {
                    alpha = score;
                }
            }

            if (score >= beta) {
                TOTAL_PRUNES++;
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
        OldEngine myEngine = new OldEngine();
        OldEngine.MinimaxInfo engine_choice;
        while (!board.isMated() && !board.isDraw() && !board.isStaleMate()){
            System.out.println(board);
            engine_choice = myEngine.Think(board,10000);
            Move engineMove = engine_choice.move;
            int engine_state_value = engine_choice.state_value;
            board.doMove(engineMove);
            System.out.println("Engine move:" + engineMove+ " State value:" + (float)engine_state_value/100 + " Total prunes:" + myEngine.TOTAL_PRUNES + " Depth:" + engine_choice.depth);
        }
        System.out.println("Draw?:" + board.isDraw());
        System.out.println(board);
        System.out.println(board.getFen());
    }
}
