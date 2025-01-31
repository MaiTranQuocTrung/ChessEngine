package ChessEngine;

import com.github.bhlangonijr.chesslib.move.Move;

import java.util.Arrays;
import java.util.List;

public class TranspositionTable {
    private int size;
    private final Entry[] table;

    public TranspositionTable(int mbSize) {
        // Calculate number of entries based on memory size
        // Entry size ≈ 32 bytes (long + 4 ints + Move object + flag)
        int numEntries = (mbSize * 1024 * 1024) / 32;
        // Round to power of 2 for efficient modulo
        int tableSize = Integer.highestOneBit(numEntries);
        table = new Entry[tableSize];
    }

    public static class Entry{
        long key;
        int depth;
        int value;
        FLAG flag;
        Move move;
        List<Move> mainLine;

        public Entry(long key, int depth, int value, FLAG flag, Move move, List<Move> mainLine){
            this.key = key;
            this.depth = depth;
            this.value = value;
            this.flag = flag;
            this.move = move;
            this.mainLine = mainLine;
        }
    }
    //clearing table
    public void clear(){
        Arrays.fill(table, null);
        size = 0;
    }

    public boolean containsKey(long key){
        int index = index(key);
        Entry entry = table[index];
        return entry != null && entry.key == key;
    }

    //generate index
    private int index(long key){
        return (int) (key & (table.length - 1));
    }

    // Get entry from table
    public Entry getEntry(long key){
        int index = index(key);
        if(containsKey(key)){
            return table[index];
        }
        return null;
    }

    // Get the percentage filled
    public double getCapacity(){
        return (double) size / table.length;
    }

    //storing data
    public void store(long key, int depth, int value, FLAG flag, Move move, List<Move> mainLine){
        int index = index(key);
        Entry oldEntry = table[index];

        if (oldEntry == null || replace(oldEntry,key,depth)){
            table[index] = new Entry(key,depth,value,flag,move,mainLine);
            size++;
        }
    }

    private boolean replace(Entry currEntry, long newEntryKey, int newEntryDepth){
        if (currEntry.key != newEntryKey){return true;}
        return currEntry.depth < newEntryDepth;
    }
}
