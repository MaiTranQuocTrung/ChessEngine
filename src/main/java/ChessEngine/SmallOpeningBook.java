package ChessEngine;

import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.HashMap;

public class SmallOpeningBook {
    public HashMap<Long, Move> openingBook = new HashMap<>();
    public SmallOpeningBook() {
        initBook();
    }

    private void initBook(){
        //Ruy lopez
        openingBook.put(388037023464589640L,new Move(Square.E7,Square.E5));
        openingBook.put(-8307949332621147213L,new Move(Square.E2,Square.E4));
        openingBook.put(4604945406196848133L,new Move(Square.G1,Square.F3));
        openingBook.put(-5870718696893862557L,new Move(Square.B8,Square.C6));
    }
}
