package VersionTesterTools;

import com.github.bhlangonijr.chesslib.Board;

import java.io.*;

public class PositionLoader {
    public static void main(String [] args) throws IOException {
        //Writer
        String outputFile = "src/positions";
        FileWriter fw = new FileWriter(outputFile);
        BufferedWriter bw = new BufferedWriter(fw);
        //Reader
        FileReader fr = new FileReader("src/whiteProcessedBook.txt");
        BufferedReader br = new BufferedReader(fr);
        String line;
        //Board stuff
        Board board = new Board();
        int i = 0;
        //Main loop
        while((line = br.readLine()) != null && i <= 1500){
            String[] lineContent = line.split(" ");
            for(String move : lineContent){
                board.doMove(move);
            }
            System.out.println("Board pos:"+i);
            System.out.println(board.getFen());
            //Write down the pos
            bw.write(board.getFen());
            bw.newLine();
            System.out.println(board);
            board = new Board();
            i++;
        }
    }
}
