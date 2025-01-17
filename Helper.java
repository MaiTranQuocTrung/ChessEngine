import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.ArrayList;
import java.util.List;



public class Helper {
    Evaluation evaluation = new Evaluation();

    public boolean isCapture(Board board, Move move){
        Square origin = move.getFrom();
        // What is the square the piece is moving to
        Square destination = move.getTo();
        // get piece at destination square
        Piece destinationPiece = board.getPiece(destination);
        // get piece at origin
        Piece originPiece = board.getPiece(origin);
        // If there is nothing at that destination square
        return destinationPiece != Piece.NONE && destinationPiece.getPieceSide() != originPiece.getPieceSide();
    }

    // check if a move is a check
    public boolean isCheck(Board board, Move move) {
        board.doMove(move);
        boolean isCheck = board.isKingAttacked();
        board.undoMove();
        return isCheck;
    }

    // Sorting by MVV-LVA and also promotion/check
    public List<Move> sortMoves(Board board, List<Move> legalMoves, TranspositionTable transpositionTable){
        List<MoveInfo> move_scores = new ArrayList<>();
        List<Move> sortedMoves = new ArrayList<>();

        for (Move move : legalMoves){
            if(!board.doMove(move)){continue;}
            board.undoMove();
            MoveInfo moveInfo = new MoveInfo(move,calculateMoveValue(board,move, transpositionTable));
            move_scores.add(moveInfo);
        }

        // sort by biggest to smallest
        move_scores.sort((a,b) -> b.value - a.value);
        for (MoveInfo move_info : move_scores) {
            sortedMoves.add(move_info.move);
        }
        return sortedMoves;
    }

    // Calculating the value of each moves according to MVV-LVA but checking TT moves first + valuing promotions and checks
    private int calculateMoveValue(Board board, Move move, TranspositionTable transpositionTable){
        // Transposition Table (PV) Handling
        if (transpositionTable.containsKey(board.getZobristKey())) {
            TranspositionTable.Entry entry = transpositionTable.getEntry(board.getZobristKey());
            List<Move> pvMoves = entry.mainLine;
            for (int i = 0; i < pvMoves.size(); i++) {
                Move pvMove = pvMoves.get(i);
                if (move.equals(pvMove)) {
                    return 1000 - i + 10 * entry.depth; // Priority for PV moves
                }
            }
        }

        // Promotion Handling
        if (move.getPromotion() != null) {
            return 900; // Promotions are highly prioritized
        }

        // Check Handling
        if (isCheck(board, move)) {
            return 300; // Checks are prioritized after PV and promotions
        }

        // MVV-LVA Scoring
        if (isCapture(board, move)) {
            Square origin = move.getFrom();
            Square destination = move.getTo();
            Piece attacker = board.getPiece(origin);
            Piece victim = board.getPiece(destination);
            int attackerValue = evaluation.pieceWorthMg(attacker.getPieceType());
            int victimValue = evaluation.pieceWorthMg(victim.getPieceType());
            return victimValue - attackerValue + 50; // Tiebreaker for captures
        }

        // Default for non-captures
        return 10; // Minimal value for non-captures to keep them in consideration
    }

    private static class MoveInfo{
        Move move;
        int value;

        public MoveInfo(Move move, int value){
            this.move = move;
            this.value = value;
        }
    }
}
