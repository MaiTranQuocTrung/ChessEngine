import com.github.bhlangonijr.chesslib.*;

public class Evaluation {
    PieceSquareTable piece_table = new PieceSquareTable();
    private static final int MATE_SCORE = 5000;

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
    // The game phase is determined by the number of pieces on the board
    public int[] gamePhase(Board board){
        int [] game_phase = new int[2];
        long bitboard = board.getBitboard();
        int midGame = Long.bitCount(bitboard);
        int endgame = 32 - midGame;
        //Mid-game phase
        game_phase[0] = midGame;
        //End game phase
        game_phase[1] = endgame;
        return game_phase;
    }

    private int[] totalPiecesValue(Board board){
        int [] piecesValues = new int[2];
        int whiteValueMg = 0;
        int blackValueMg = 0;
        int whiteValueEg = 0;
        int blackValueEg = 0;

        for (Piece piece : Piece.allPieces){
            PieceType pieceType = piece.getPieceType();
            int rewardMg = pieceWorthMg(pieceType);
            int rewardEg = pieceWorthEg(pieceType);

            long pieceBitboard = board.getBitboard(piece);

            // the piece is not on the board
            if (pieceBitboard == 0){
                continue;
            }
            if (piece.getPieceSide() == Side.WHITE){
                whiteValueMg += rewardMg * Long.bitCount(pieceBitboard);
                whiteValueEg += rewardEg * Long.bitCount(pieceBitboard);
            }
            else{
                blackValueMg += rewardMg * Long.bitCount(pieceBitboard);
                blackValueEg += rewardEg * Long.bitCount(pieceBitboard);
            }
        }
        int valueMg = whiteValueMg - blackValueMg;
        int valueEg = whiteValueEg - blackValueEg;
        //Mid game
        piecesValues[0] = valueMg;
        //end game
        piecesValues[1] = valueEg;
        return  piecesValues;
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

    private int mobilityScore(Board board) {
        Side originalSide = board.getSideToMove();
        int currentMobility = board.legalMoves().size();

        int opponentMobility;
        if (originalSide == Side.WHITE) {
            board.setSideToMove(Side.BLACK);
            opponentMobility = board.legalMoves().size();
        } else {
            board.setSideToMove(Side.WHITE);
            opponentMobility = board.legalMoves().size();
        }

        board.setSideToMove(originalSide);
        return (currentMobility - opponentMobility) / 6;
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
        // Penalize black and white by the penalty value respectively
        return (int)((doubledPawnsBlackCount - doubledPawnsWhiteCount) * penalizeScore);
    }

    private int passedPawns(Board board){
        long blackPawnBitboard = board.getBitboard(Piece.BLACK_PAWN);
        long whitePawnBitboard = board.getBitboard(Piece.WHITE_PAWN);
        int baseReward = 10;
        int whitePassedPawnValue = 0;
        int blackPassedPawnValue = 0;

        for (int file = 0; file < 8; file++) {
            long fileMask = 0x0101010101010101L << file;
            long fileMaskRight = file < 7 ? fileMask << 1 : 0;
            long fileMaskLeft = file > 0 ? fileMask >> 1 : 0;

            // Check for passed pawns
            if ((whitePawnBitboard & fileMask) != 0 &&
                    (blackPawnBitboard & (fileMask | fileMaskRight | fileMaskLeft)) == 0) {
                int rank = Long.numberOfTrailingZeros(whitePawnBitboard & fileMask) / 8 + 1;
                whitePassedPawnValue += rank * baseReward;
            }

            if ((blackPawnBitboard & fileMask) != 0 &&
                    (whitePawnBitboard & (fileMask | fileMaskRight | fileMaskLeft)) == 0) {
                int rank = 8 - Long.numberOfTrailingZeros(Long.highestOneBit(blackPawnBitboard & fileMask)) / 8;
                blackPassedPawnValue += rank * baseReward;
            }
        }
        return whitePassedPawnValue - blackPassedPawnValue;
    }

    public int eval(Board board){
        //Get the game phases
        int midGame = gamePhase(board)[0];
        int endGame = gamePhase(board)[1];
        //Tampered eval
        int totalPiecesValue = (totalPiecesValue(board)[0] * midGame +
                totalPiecesValue(board)[1] * endGame) / 32;
        int mobilityScore = (mobilityScore(board) * endGame) / 32;
        return totalPiecesValue +
                positionalValue(board) +
                mobilityScore + checkMate(board) +
                doubledPawns(board) +
                (passedPawns(board) * endGame / 32);
    }
}

