import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;


public class Evaluation {
    /*Idea:
    - Punish pinned pieces
    - Reward pawns for pushing up
    - Need a way to emphasize center control so moves like rook b1 does not happen
    - Need some endgame mating help (like pushing the enemy king to corners might help to find mate)
     */
    PieceSquareTable piece_table = new PieceSquareTable();

    // The game phase is determined by the number of pieces on the board
    private int[] gamePhase(Board board){
        int [] game_phase = new int[2];
        long bitboard = board.getBitboard();
        int midgame = 0;
        int endgame;
        for (int i = 0; i < 64; i++){
            if ((bitboard & (1L << i)) != 0) {
                midgame ++;
            }
        }
        endgame = 32 - midgame;
        //Mid-game phase
        game_phase[0] = midgame;
        //End game phase
        game_phase[1] = endgame;
        return game_phase;
    }

    public int pieceWorthMg(PieceType pieceType){
        return switch (pieceType) {
            case PAWN -> 82;
            case KNIGHT -> 337;
            case BISHOP -> 365;
            case ROOK -> 477;
            case QUEEN -> 1025;
            case KING -> 0;
            case null, default -> 0;
        };
    }

    public int pieceWorthEg(PieceType pieceType){
        return switch (pieceType) {
            case PAWN -> 94;
            case KNIGHT -> 281;
            case BISHOP -> 297;
            case ROOK -> 512;
            case QUEEN -> 936;
            case KING -> 0;
            case null, default -> 0;
        };
    }

    private int total_pieces_value_mg(Board board){
        int white_value = 0;
        int black_value = 0;

        for (Piece piece : Piece.allPieces){
            PieceType pieceType = piece.getPieceType();
            int reward = pieceWorthMg(pieceType);

            long piece_bitboard = board.getBitboard(piece);

            // the piece is not on the board
            if (piece_bitboard == 0){
                continue;
            }

            for (int i = 0; i < 64; i++){
                if ((piece_bitboard & (1L << i)) != 0) {
                    if (piece.getPieceSide() == Side.WHITE){
                        white_value += reward;
                    }
                    else{
                        black_value += reward;
                    }
                }
            }
        }
        return white_value - black_value;
    }

    private int total_pieces_value_eg(Board board){
        int white_value = 0;
        int black_value = 0;

        for (Piece piece : Piece.allPieces){
            PieceType pieceType = piece.getPieceType();
            int reward = pieceWorthEg(pieceType);

            long piece_bitboard = board.getBitboard(piece);

            // the piece is not on the board
            if (piece_bitboard == 0){
                continue;
            }

            for (int i = 0; i < 64; i++){
                if ((piece_bitboard & (1L << i)) != 0) {
                    if (piece.getPieceSide() == Side.WHITE){
                        white_value += reward;
                    }
                    else{
                        black_value += reward;
                    }
                }
            }
        }
        return white_value - black_value;
    }

    public int positional_value(Board board){
        int position_value_white = 0;
        int position_value_black = 0;
        for (Piece piece : Piece.allPieces){
            // The piece does not exist on the board then skip over it
            if (board.getBitboard(piece) == 0){
                continue;
            }
            // Weighting the positional values according to game phase (number of pieces on the board)
            if (piece.getPieceSide() == Side.WHITE) {
                position_value_white += (piece_table.piece_positional_value_md(board, piece) * gamePhase(board)[0] +
                        piece_table.piece_positional_value_eg(board, piece) * gamePhase(board)[1]) / 32;
            }
            else{
                position_value_black += (piece_table.piece_positional_value_md(board, piece) * gamePhase(board)[0] +
                piece_table.piece_positional_value_eg(board, piece) * gamePhase(board)[1]) / 32;
            }
        }
        return position_value_white - position_value_black;
    }

    private int mobilityScore(Board board){
        int white_mobility = 0;
        int black_mobility = 0;
        if (board.getSideToMove() == Side.WHITE){
            white_mobility = board.legalMoves().size();
        }
        else{
            black_mobility = board.legalMoves().size();
        }
        return white_mobility - black_mobility;
    }


    public int eval(Board board){
        int total_pieces_value = (total_pieces_value_mg(board) * gamePhase(board)[0] +
                total_pieces_value_eg(board) * gamePhase(board)[1]) / 32;
        return total_pieces_value + positional_value(board) + mobilityScore(board);
    }
}

