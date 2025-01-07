import com.github.bhlangonijr.chesslib.*;


public class Evaluation {
    /*Idea:
    - Punish pinned pieces
    - Reward pawns for pushing up
    - Need a way to emphasize center control so moves like rook b1 does not happen
    - Need some endgame mating help (like pushing the enemy king to corners might help to find mate)
     */
    PieceSquareTable piece_table = new PieceSquareTable();
    private static final int MATE_SCORE = 5000;
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

    private int totalPiecesValueMg(Board board){
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

    private int totalPiecesValueEg(Board board){
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

    public int positionalValue(Board board){
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
        Side originalSide = board.getSideToMove();

        // Calculate White mobility
        board.setSideToMove(Side.WHITE);
        int white_mobility = board.legalMoves().size();

        // Calculate Black mobility
        board.setSideToMove(Side.BLACK);
        int black_mobility = board.legalMoves().size();

        // Restore original side to move
        board.setSideToMove(originalSide);

        // Dividing by 6 since I don't want the mobility score to be influential
        return (white_mobility - black_mobility) / 6;
    }
    //Since we are evaluating unstable positions I think we should check for mates in case
    private int checkMate(Board board){
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

    private int doubledPawns(Board board){
        long blackPawnBitboard = board.getBitboard(Piece.BLACK_PAWN);
        long whitePawnBitboard = board.getBitboard(Piece.WHITE_PAWN);
        // This value seems good for this engine
        double penalizeScore = 50;
        int doubledPawnsWhiteCount = 0;
        int doubledPawnsBlackCount = 0;
        // Check each file (a through h)
        for (int file = 0; file < 8; file++) {
            // Create a mask for the current file
            long fileMask = 0x0101010101010101L << file;

            // Count pawns on this file for each color
            int whitePawnsInFile = Long.bitCount(whitePawnBitboard & fileMask);
            int blackPawnsInFile = Long.bitCount(blackPawnBitboard & fileMask);

            // If there are 2 or more pawns in a file, add the extras to doubled count
            if (whitePawnsInFile > 1) {
                doubledPawnsWhiteCount += (whitePawnsInFile - 1);
            }
            if (blackPawnsInFile > 1) {
                doubledPawnsBlackCount += (blackPawnsInFile - 1);
            }
        }
        // Penalize black and white by the penality value respectively
        return (int)((doubledPawnsBlackCount - doubledPawnsWhiteCount) * penalizeScore);
    }

    public int eval(Board board){
        int totalPiecesValue = (totalPiecesValueMg(board) * gamePhase(board)[0] +
                totalPiecesValueEg(board) * gamePhase(board)[1]) / 32;
        int mobilityScore = (mobilityScore(board) * gamePhase(board)[1]) / 32;
        return totalPiecesValue + positionalValue(board) + mobilityScore + checkMate(board) + doubledPawns(board);
    }
}

