# ChessEngine (King Bob IV)

A personal deep dive into chess programming. King Bob IV is written to be readable for future more complex implementations.

## Features

### Search
- **Alpha-beta pruning**: Efficient pruning of the search tree to focus on promising moves.
- **Transposition table**: Implements move ordering and reuses previously evaluated positions.
- **Quiescence search**: Evaluates positions involving checks and captures to avoid the horizon effect.
- **MVV-LVA sorted moves**: Most Valuable Victim - Least Valuable Attacker sorting for better move selection.
- **Iterative deepening**: Gradually increases search depth, refining results.

### Evaluation
- **Tampered evaluation**: Game phase determined by the number of pieces remaining on the board.
- **Total material**: Weights positions based on the remaining pieces.
- **Piece-square table**: Evaluates piece positions using weighted tables, influenced by the number of pieces.
- **Simple mobility**: Considers the number of legal moves available for each piece.


