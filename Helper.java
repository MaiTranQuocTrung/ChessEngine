import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import java.util.ArrayList;
import java.util.List;


public class Helper {

    Evaluation evaluation = new Evaluation();

    public boolean isCapture(Board board, Move move){
        // What is the square the piece is moving to
        Square destination = move.getTo();
        // get piece at destination square
        Piece destinationPiece = board.getPiece(destination);
        // If there is nothing at that destination square
        if (destinationPiece == null || destinationPiece == Piece.NONE) {
            return false;
        }
        else{
            return true;
        }
    }

    // Sorting by MVV-LVA and also promotion/check
    public List<Move> sortMoves(Board board, List<Move> legalMoves){
        List<MoveInfo> move_scores = new ArrayList<>();
        List<Move> sortedMoves = new ArrayList<>();

        for (Move move : legalMoves){
            MoveInfo moveInfo = new MoveInfo(move,calculateMoveValue(board,move));
            move_scores.add(moveInfo);
        }

        // sort by biggest to smallest
        move_scores.sort((a,b) -> b.value - a.value);

        for (MoveInfo move_info : move_scores) {
            sortedMoves.add(move_info.move);
        }

        return sortedMoves;
    }

    // Calculating the value of each moves according to MVV-LVA
    private int calculateMoveValue(Board board, Move move){
        // Origin square and destination square
        Square origin = move.getFrom();
        Square destination = move.getTo();
        // Getting the pieces at origin and destination
        Piece originPiece = board.getPiece(origin);
        Piece destinationPiece = board.getPiece(destination);

        if (destinationPiece == null || destinationPiece == Piece.NONE || originPiece == null || originPiece == Piece.NONE){
            return 0;
        }

        // Getting the piece type of victim and attacker
        PieceType origin_piece_type = originPiece.getPieceType();
        PieceType destination_piece_type = destinationPiece.getPieceType();

        // Getting the values of attacker and victim
        int origin_piece_value = evaluation.pieceWorthMg(origin_piece_type);
        int destination_piece_value = evaluation.pieceWorthMg(destination_piece_type);

        if (move.getPromotion() != Piece.NONE){
            return 700;
        }
        else if (isCheck(board,move)){
            return 200;
        }

        return destination_piece_value - origin_piece_value;
    }

    // check if a move is a check
    private boolean isCheck(Board board, Move move) {
        board.doMove(move);
        boolean isCheck = board.isKingAttacked();
        board.undoMove();
        return isCheck;
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
