# ChessEngine (King Bob IV)

A personal deep dive into chess programming. King Bob IV is written to be readable for future more complex implementations.

## Features

### Search
- Alpha-beta pruning
- Transposition table (reuse values from ID and move ordering)
- Quiescence search (both checks and captures)
- MVV-LVA sorted moves
- Iterative deepening

### Evaluation
- **Tampered evaluation**: Game phase determined by the number of pieces remaining on the board.
- **Total material**: Weights positions based on the remaining pieces.
- **Piece-square table**: Evaluates piece positions using weighted tables, influenced by the number of pieces.
- **Simple mobility**: Considers the number of legal moves available for each player at their given turn.


