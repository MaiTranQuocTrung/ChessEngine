package ChessEngine;
import com.github.bhlangonijr.chesslib.*;

public class SimpleEval {
    private static final int MATE_SCORE = 5000;

    public int flip(int index){
        return index ^ 56;
    }

    public int [] gamePhase(Board board){
        int [] gamePhase = new int[2];
        for (Piece piece : Piece.allPieces) {
            //Skip if the piece does not exist
            if (board.getBitboard(piece) == 0){continue;}
            // else determine game phase
            int pieceGamePhase = switch (piece.getPieceType()){
                case KNIGHT, BISHOP -> Long.bitCount(board.getBitboard(piece));
                case ROOK -> 2 * Long.bitCount(board.getBitboard(piece));
                case QUEEN -> 4 * Long.bitCount(board.getBitboard(piece));
                case null, default -> 0;
            };
            gamePhase[0] += pieceGamePhase;
        }
        //Mid game is gamePhase[0], endgame is gamePhase[1]
        if (gamePhase[0] >= 24){gamePhase[0] = 24;}
        gamePhase[1] = 24 - gamePhase[0];
        return gamePhase;
    }

    public int pieceWorthMg(PieceType pieceType){
        return switch (pieceType) {
            case PAWN -> 82;
            case KNIGHT -> 337;
            case BISHOP -> 365;
            case ROOK -> 477;
            case QUEEN -> 1025;
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
            case null, default -> 0;
        };
    }

    public final int [] pawn_table_md = {
            0,   0,   0,   0,   0,   0,  0,   0,
            98, 134,  61,  95,  68, 126, 34, -11,
            -6,   7,  26,  31,  65,  56, 25, -20,
            -14,  13,   6,  21,  23,  12, 17, -23,
            -27,  -2,  -5,  12,  17,   6, 10, -25,
            -26,  -4,  -4, -10,   3,   3, 33, -12,
            -35,  -1, -20, -23, -15,  24, 38, -22,
            0,   0,   0,   0,   0,   0,  0,   0,};

    public final int [] bishop_table_md = {
            -29,   4, -82, -37, -25, -42,   7,  -8,
            -26,  16, -18, -13,  30,  59,  18, -47,
            -16,  37,  43,  40,  35,  50,  37,  -2,
            -4,   5,  19,  50,  37,  37,   7,  -2,
            -6,  13,  13,  26,  34,  12,  10,   4,
            0,  15,  15,  15,  14,  27,  18,  10,
            4,  15,  16,   0,   7,  21,  33,   1,
            -33,  -3, -14, -21, -13, -12, -39, -21,};

    public final int [] knight_table_md = {
            -167, -89, -34, -49,  61, -97, -15, -107,
            -73, -41,  72,  36,  23,  62,   7,  -17,
            -47,  60,  37,  65,  84, 129,  73,   44,
            -9,  17,  19,  53,  37,  69,  18,   22,
            -13,   4,  16,  13,  28,  19,  21,   -8,
            -23,  -9,  12,  10,  19,  17,  25,  -16,
            -29, -53, -12,  -3,  -1,  18, -14,  -19,
            -105, -21, -58, -33, -17, -28, -19,  -23,};

    public final int [] rook_table_md = {
            32,  42,  32,  51, 63,  9,  31,  43,
            27,  32,  58,  62, 80, 67,  26,  44,
            -5,  19,  26,  36, 17, 45,  61,  16,
            -24, -11,   7,  26, 24, 35,  -8, -20,
            -36, -26, -12,  -1,  9, -7,   6, -23,
            -45, -25, -16, -17,  3,  0,  -5, -33,
            -44, -16, -20,  -9, -1, 11,  -6, -71,
            -19, -13,   1,  17, 16,  7, -37, -26,};

    public final int [] queen_table_md = {
            -28,   0,  29,  12,  59,  44,  43,  45,
            -24, -39,  -5,   1, -16,  57,  28,  54,
            -13, -17,   7,   8,  29,  56,  47,  57,
            -27, -27, -16, -16,  -1,  17,  -2,   1,
            -9, -26,  -9, -10,  -2,  -4,   3,  -3,
            -14,   2, -11,  -2,  -5,   2,  14,   5,
            -35,  -8,  11,   2,   8,  15,  -3,   1,
            -1, -18,  -9,  10, -15, -25, -31, -50,};

    public final int [] king_table_md = {
            -65,  23,  16, -15, -56, -34,   2,  13,
            29,  -1, -20,  -7,  -8,  -4, -38, -29,
            -9,  24,   2, -16, -20,   6,  22, -22,
            -17, -20, -12, -27, -30, -25, -14, -36,
            -49,  -1, -27, -39, -46, -44, -33, -51,
            -14, -14, -22, -46, -44, -30, -15, -27,
            1,   7,  -8, -64, -43, -16,   9,   8,
            -15,  36,  12, -54,   8, -28,  24,  14,};

    public final int [] pawn_table_eg = {
            0,   0,   0,   0,   0,   0,   0,   0,
            178, 173, 158, 134, 147, 132, 165, 187,
            94, 100,  85,  67,  56,  53,  82,  84,
            32,  24,  13,   5,  -2,   4,  17,  17,
            13,   9,  -3,  -7,  -7,  -8,   3,  -1,
            4,   7,  -6,   1,   0,  -5,  -1,  -8,
            13,   8,   8,  10,  13,   0,   2,  -7,
            0,   0,   0,   0,   0,   0,   0,   0,};

    public final int [] knight_table_eg = {
            -58, -38, -13, -28, -31, -27, -63, -99,
            -25,  -8, -25,  -2,  -9, -25, -24, -52,
            -24, -20,  10,   9,  -1,  -9, -19, -41,
            -17,   3,  22,  22,  22,  11,   8, -18,
            -18,  -6,  16,  25,  16,  17,   4, -18,
            -23,  -3,  -1,  15,  10,  -3, -20, -22,
            -42, -20, -10,  -5,  -2, -20, -23, -44,
            -29, -51, -23, -15, -22, -18, -50, -64,};

    public final int [] bishop_table_eg = {
            -14, -21, -11,  -8, -7,  -9, -17, -24,
            -8,  -4,   7, -12, -3, -13,  -4, -14,
            2,  -8,   0,  -1, -2,   6,   0,   4,
            -3,   9,  12,   9, 14,  10,   3,   2,
            -6,   3,  13,  19,  7,  10,  -3,  -9,
            -12,  -3,   8,  10, 13,   3,  -7, -15,
            -14, -18,  -7,  -1,  4,  -9, -15, -27,
            -23,  -9, -23,  -5, -9, -16,  -5, -17,};

    public final int [] rook_table_eg = {
            13, 10, 18, 15, 12,  12,   8,   5,
            11, 13, 13, 11, -3,   3,   8,   3,
            7,  7,  7,  5,  4,  -3,  -5,  -3,
            4,  3, 13,  1,  2,   1,  -1,   2,
            3,  5,  8,  4, -5,  -6,  -8, -11,
            -4,  0, -5, -1, -7, -12,  -8, -16,
            -6, -6,  0,  2, -9,  -9, -11,  -3,
            -9,  2,  3, -1, -5, -13,   4, -20,};

    public final int [] queen_table_eg = {
            -9,  22,  22,  27,  27,  19,  10,  20,
            -17,  20,  32,  41,  58,  25,  30,   0,
            -20,   6,   9,  49,  47,  35,  19,   9,
            3,  22,  24,  45,  57,  40,  57,  36,
            -18,  28,  19,  47,  31,  34,  39,  23,
            -16, -27,  15,   6,   9,  17,  10,   5,
            -22, -23, -30, -16, -16, -23, -36, -32,
            -33, -28, -22, -43,  -5, -32, -20, -41,};

    public final int [] king_table_eg = {
            -74, -35, -18, -18, -11,  15,   4, -17,
            -12,  17,  14,  17,  17,  38,  23,  11,
            10,  17,  23,  15,  20,  45,  44,  13,
            -8,  22,  24,  27,  26,  33,  26,   3,
            -18,  -4,  21,  24,  27,  23,   9, -11,
            -19,  -3,  11,  21,  23,  16,   7,  -9,
            -27, -11,   4,  13,  14,   4,  -5, -17,
            -53, -34, -21, -11, -28, -14, -24, -43};

    public int positionalEvaluation(Board board){
        // Determine game phase
        int midGame = gamePhase(board)[0];
        int endGame = gamePhase(board)[1];
        //Total value
        int valueWhite = 0;
        int valueBlack = 0;
        for (Piece piece : Piece.allPieces){
            long pieceBitboard = board.getBitboard(piece);
            PieceType pieceType = piece.getPieceType();
            Side pieceSide = piece.getPieceSide();
            if (pieceBitboard == 0){continue;}
            for (int i = 0; i < 64; i++){
                if ((pieceBitboard & (1L << i)) != 0) {
                    if (pieceSide == Side.BLACK){
                        int value = switch (pieceType) {
                            case PAWN -> ((pawn_table_md[i] + pieceWorthMg(PieceType.PAWN)) * midGame +
                                    (pawn_table_eg[i] + pieceWorthEg(PieceType.PAWN)) * endGame);
                            case BISHOP -> ((bishop_table_md[i] + pieceWorthMg(PieceType.BISHOP)) * midGame +
                                    (bishop_table_eg[i] + pieceWorthEg(PieceType.BISHOP)) * endGame);
                            case KNIGHT -> ((knight_table_md[i] + pieceWorthMg(PieceType.KNIGHT)) * midGame +
                                    (knight_table_eg[i] + pieceWorthEg(PieceType.KNIGHT)) * endGame);
                            case QUEEN -> ((queen_table_md[i] + pieceWorthMg(PieceType.QUEEN)) * midGame +
                                    (queen_table_eg[i] + pieceWorthEg(PieceType.QUEEN)) * endGame);
                            case ROOK -> ((rook_table_md[i] + pieceWorthMg(PieceType.ROOK)) * midGame +
                                    (rook_table_eg[i] + pieceWorthEg(PieceType.ROOK)) * endGame);
                            case KING -> ((king_table_md[i] + pieceWorthMg(PieceType.KING)) * midGame +
                                    (king_table_eg[i] + pieceWorthEg(PieceType.KING)) * endGame);
                            default -> 0;
                        };
                        valueBlack += value;
                    }
                    else{
                        int value = switch (pieceType) {
                            case PAWN -> ((pawn_table_md[flip(i)] + pieceWorthMg(PieceType.PAWN)) * midGame +
                                    (pawn_table_eg[flip(i)] + pieceWorthEg(PieceType.PAWN)) * endGame);
                            case BISHOP -> ((bishop_table_md[flip(i)] + pieceWorthMg(PieceType.BISHOP)) * midGame +
                                    (bishop_table_eg[flip(i)] + pieceWorthEg(PieceType.BISHOP)) * endGame);
                            case KNIGHT -> ((knight_table_md[flip(i)] + pieceWorthMg(PieceType.KNIGHT)) * midGame +
                                    (knight_table_eg[flip(i)] + pieceWorthEg(PieceType.KNIGHT)) * endGame);
                            case QUEEN -> ((queen_table_md[flip(i)] + pieceWorthMg(PieceType.QUEEN)) * midGame +
                                    (queen_table_eg[flip(i)] + pieceWorthEg(PieceType.QUEEN)) * endGame);
                            case ROOK -> ((rook_table_md[flip(i)] + pieceWorthMg(PieceType.ROOK)) * midGame +
                                    (rook_table_eg[flip(i)] + pieceWorthEg(PieceType.ROOK)) * endGame);
                            case KING -> ((king_table_md[flip(i)] + pieceWorthMg(PieceType.KING)) * midGame +
                                    (king_table_eg[flip(i)] + pieceWorthEg(PieceType.KING)) * endGame);
                            default -> 0;
                        };
                        valueWhite += value;
                    }
                }
            }
        }
        int finalEval = (valueWhite - valueBlack) / 24;
        //Adding some other eval characteristics
        finalEval += checkMate(board);
        finalEval += passedPawns(board);
        int sideToMove = board.getSideToMove() == Side.WHITE ? 1 : -1;
        return finalEval * sideToMove;
    }


    private int checkMate(Board board) {
        if (board.isMated()) {
            // Assign mate score relative to the player to move
            return -MATE_SCORE;
        }
        return 0; // No checkmate
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
        return (whitePassedPawnValue - blackPassedPawnValue);
    }

    public static void main (String[] args){
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        SimpleEval simpleEval = new SimpleEval();
    }
}
