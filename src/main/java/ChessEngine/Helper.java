package ChessEngine;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.ArrayList;
import java.util.List;



public class Helper {
    SimpleEval simpleEval = new SimpleEval();
    private Move idMove;
    public Move [][] killerMoves = new Move[2][100];

    //fetching iterative deepening move
    public void iterativeDeepeningMove(Move move){
        this.idMove = move;
    }

    //Null move pruning
    public boolean nullMovePruning(Board board){
        // King's bitboard
        long kingBitboard = board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING));
        // Pawn's bitboard
        long pawnBitboard = board.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN));
        // All pieces bitboard
        long allPiecesBitboard = board.getBitboard(board.getSideToMove());
        // As long as we have other pieces on the board that are not king and pawns, allow null move pruning
        return (kingBitboard | pawnBitboard) != allPiecesBitboard;
    }

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

    // Sorting by MVV-LVA, TT moves, checks, promotions and previous move from ID
    public List<Move> sortMoves(Board board, List<Move> legalMoves, TranspositionTable transpositionTable, int ply, boolean qSearch){
        List<MoveInfo> move_scores = new ArrayList<>();
        List<Move> sortedMoves = new ArrayList<>();

        for (Move move : legalMoves){
            if(!board.doMove(move)){continue;}
            board.undoMove();
            MoveInfo moveInfo;
            if(qSearch){
                moveInfo = new MoveInfo(move, calculateCaptureMoveValue(board, move, transpositionTable));
            }
            else {
                moveInfo = new MoveInfo(move, calculateMoveValue(board, move, transpositionTable, ply));
            }
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
    //Ordering scheme as such ID Move > TT move > MVV-LVA > Promotion > Killer 1 > Killer 2 > PST
    private int calculateMoveValue(Board board, Move move, TranspositionTable transpositionTable, int ply){
        //ID value should always be looked at first
        if (move.equals(idMove)) {
            return 60000;
        }
        //Transposition value
        if (transpositionTable.containsKey(board.getZobristKey())){
            TranspositionTable.Entry node = transpositionTable.getEntry(board.getZobristKey());
            Move TT_move = node.move;
            if(move.equals(TT_move)){
                return 50000 + node.depth;
            }
        }
        // Promotion Handling
        if (move.getPromotion() == Piece.WHITE_QUEEN || move.getPromotion() == Piece.BLACK_QUEEN) {
            return 35000;
        }
        //Capture move ordering
        if(isCapture(board,move)){
            return 30000 + MVV_LVA(board,move);
        }
        //Quiet move ordering
        else {
            int quietScore = 0;
            //Killer moves
            if (killerMoves[0][ply] != null && move.equals(killerMoves[0][ply])) {
                quietScore = 9000;
            }

            if (killerMoves[1][ply] != null && move.equals(killerMoves[1][ply])) {
                quietScore = 7000;
            }
            quietScore += PST(board, move);
            return quietScore;
        }
    }

    //Move ordering move Q-search
    private int calculateCaptureMoveValue(Board board, Move move,TranspositionTable transpositionTable){
        // TT values are good
        int score = 0;
        if (transpositionTable.containsKey(board.getZobristKey())){
            TranspositionTable.Entry node = transpositionTable.getEntry(board.getZobristKey());
            Move TT_move = node.move;
            if(move.equals(TT_move)){
                score +=  4000 - node.depth;
            }
        }

        if (move.getPromotion() == Piece.WHITE_QUEEN || move.getPromotion() == Piece.BLACK_QUEEN) {
            score += 2000; // Promotions are highly prioritized
        }
        score += 3000 + MVV_LVA(board, move);
        return score;
    }

    private int MVV_LVA(Board board, Move move){
        // Origin square and destination square
        Square origin = move.getFrom();
        Square destination = move.getTo();
        // Getting the pieces at origin and destination
        Piece originPiece = board.getPiece(origin);
        Piece destinationPiece = board.getPiece(destination);
        // Getting the piece type of victim and attacker
        PieceType origin_piece_type = originPiece.getPieceType();
        PieceType destination_piece_type = destinationPiece.getPieceType();
        // Getting the values of attacker and victim
        int origin_piece_value = simpleEval.pieceWorthMg(origin_piece_type);
        int destination_piece_value = simpleEval.pieceWorthMg(destination_piece_type);
        return destination_piece_value - origin_piece_value;
    }

    private int PST(Board board, Move move){
        // Information about the move
        Square destination = move.getTo();
        Square origin = move.getFrom();

        // get the piece type of the move
        PieceType pieceType = board.getPiece(origin).getPieceType();
        Side pieceSide =  board.getPiece(origin).getPieceSide();

        // Get the single piece's bitboard for origin and then the move
        long pieceBitboardOrigin = origin.getBitboard();
        long pieceBitboardDestination = destination.getBitboard();

        //Loop through the bitboard (this is actually the same as the for loop bitwise shift method but super fast)
        int originIndex = Long.numberOfTrailingZeros(pieceBitboardOrigin);
        int destinationIndex = Long.numberOfTrailingZeros(pieceBitboardDestination);

        // Value we will be returning
        int value;
        if (pieceSide == Side.BLACK) {
            value = switch (pieceType) {
                case PAWN -> simpleEval.pawn_table_md[destinationIndex] - simpleEval.pawn_table_md[originIndex];
                case BISHOP -> simpleEval.bishop_table_md[destinationIndex] - simpleEval.bishop_table_md[originIndex];
                case KNIGHT -> simpleEval.knight_table_md[destinationIndex] - simpleEval.knight_table_md[originIndex];
                case QUEEN -> simpleEval.queen_table_md[destinationIndex] - simpleEval.queen_table_md[originIndex];
                case ROOK -> simpleEval.rook_table_md[destinationIndex] - simpleEval.rook_table_md[originIndex];
                case KING -> simpleEval.king_table_md[destinationIndex] - simpleEval.king_table_md[originIndex];
                case null, default -> 0;
            };
        }
        else{
            value = switch (pieceType) {
                case PAWN -> simpleEval.pawn_table_md[simpleEval.flip(destinationIndex)] - simpleEval.pawn_table_md[simpleEval.flip(originIndex)];
                case BISHOP -> simpleEval.bishop_table_md[simpleEval.flip(destinationIndex)] - simpleEval.bishop_table_md[simpleEval.flip(originIndex)];
                case KNIGHT -> simpleEval.knight_table_md[simpleEval.flip(destinationIndex)] - simpleEval.knight_table_md[simpleEval.flip(originIndex)];
                case QUEEN -> simpleEval.queen_table_md[simpleEval.flip(destinationIndex)] - simpleEval.queen_table_md[simpleEval.flip(originIndex)];
                case ROOK -> simpleEval.rook_table_md[simpleEval.flip(destinationIndex)] - simpleEval.rook_table_md[simpleEval.flip(originIndex)];
                case KING -> simpleEval.king_table_md[simpleEval.flip(destinationIndex)] - simpleEval.king_table_md[simpleEval.flip(originIndex)];
                case null, default -> 0;
            };
        }
        return value;
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