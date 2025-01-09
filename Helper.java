import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import java.util.ArrayList;
import java.util.HashMap;
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
    public List<Move> sortMoves(Board board, List<Move> legalMoves, HashMap<Long, Engine.MinimaxInfo> transpositionTable, int maxDepth){
        List<MoveInfo> move_scores = new ArrayList<>();
        List<Move> sortedMoves = new ArrayList<>();

        for (Move move : legalMoves){
            MoveInfo moveInfo = new MoveInfo(move,calculateMoveValue(board,move, transpositionTable, maxDepth));
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
    private int calculateMoveValue(Board board, Move move, HashMap<Long, Engine.MinimaxInfo> transpositionTable, int currDepth){
        //Transposition value
        if (transpositionTable.containsKey(board.getZobristKey())){
            Engine.MinimaxInfo node = transpositionTable.get(board.getZobristKey());
            Move TT_move = node.move;
            if (move.equals(TT_move) && node.depth >= currDepth){
                // So the move ordering will always evaluate the move from transposition first
                return 800;
            }
            // If the move is from a lesser depth then still look at it, though its less important
            else if(move.equals(TT_move)){
                return 500 - node.depth;
            }
        }
        // If the move is a promotion, it is likely to be very good
        else if (move.getPromotion() != Piece.NONE){
            return 700;
        }
        // If the move is a check it is also likely to be decent
        else if (isCheck(board,move)){
            return 200;
        }

        //MVV-LVA here
        // Origin square and destination square
        Square origin = move.getFrom();
        Square destination = move.getTo();
        // Getting the pieces at origin and destination
        Piece originPiece = board.getPiece(origin);
        Piece destinationPiece = board.getPiece(destination);
        // If its not a capture then we dont have an opinion on it
        if (!isCapture(board,move)){
            return 0;
        }

        // Getting the piece type of victim and attacker
        PieceType origin_piece_type = originPiece.getPieceType();
        PieceType destination_piece_type = destinationPiece.getPieceType();

        // Getting the values of attacker and victim
        int origin_piece_value = evaluation.pieceWorthMg(origin_piece_type);
        int destination_piece_value = evaluation.pieceWorthMg(destination_piece_type);

        return destination_piece_value - origin_piece_value;
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
