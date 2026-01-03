type Board = (number | null)[][];

export const validateAllMoves = (board: Board): number | "draw" | null => {
    return (
        checkVerticalMoves(board) ||
        checkRightDiagonalMoves(board) ||
        checkLeftDiagonalMoves(board) ||
        checkHorizontalMoves(board) ||
        checkDraw(board)
    );
};

const checkHorizontalMoves = (board: Board): number | null => {
    for (let r = 0; r < 6; r++) {
        for (let c = 0; c < 4; c++) {
            const coveredCells = [board[r]?.[c + 1], board[r]?.[c + 2], board[r]?.[c + 3]];

            if (board[r]?.[c] && coveredCells.every(cell => cell === board[r]?.[c])) {
                return board[r]?.[c] as number;
            }
        }
    }
    return null;
};

const checkVerticalMoves = (board: Board): number | null => {
    for (let r = 3; r < 6; r++) {
        for (let c = 0; c < 7; c++) {
            const coveredCells = [board[r - 1]?.[c], board[r - 2]?.[c], board[r - 3]?.[c]];

            if (board[r]?.[c] && coveredCells.every(cell => cell === board[r]?.[c])) {
                return board[r]?.[c] as number;
            }
        }
    }
    return null;
};

const checkRightDiagonalMoves = (board: Board): number | null => {
    for (let r = 3; r < 6; r++) {
        for (let c = 0; c < 4; c++) {
            const coveredCells = [board[r - 1]?.[c + 1], board[r - 2]?.[c + 2], board[r - 3]?.[c + 3]];

            if (board[r]?.[c] && coveredCells.every(cell => cell === board[r]?.[c])) {
                return board[r]?.[c] as number;
            }
        }
    }
    return null;
};

const checkLeftDiagonalMoves = (board: Board): number | null => {
    for (let r = 3; r < 6; r++) {
        for (let c = 3; c < 7; c++) {
            const coveredCells = [board[r - 1]?.[c - 1], board[r - 2]?.[c - 2], board[r - 3]?.[c - 3]];

            if (board[r]?.[c] && coveredCells.every(cell => cell === board[r]?.[c])) {
                return board[r]?.[c] as number;
            }
        }
    }
    return null;
};

const checkDraw = (board: Board): "draw" | null => {
    for (let r = 0; r < 6; r++) {
        for (let c = 0; c < 7; c++) {
            if (board[r]?.[c] === null) {
                return null;
            }
        }
    }
    return "draw";
};
