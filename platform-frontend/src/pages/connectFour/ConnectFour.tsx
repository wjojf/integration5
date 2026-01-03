import {useState} from 'react'
import {Row} from "../../components/connectFour/Row";
import {validateAllMoves} from '../../lib/connectFour/validateBoard'
import {Button} from "../../components/app/input/Button";

export const ConnectFour = () => {
    const [player1] = useState(1)
    const [player2] = useState(2)
    const [currentPlayer, setCurrentPlayer] = useState(1)
    const [board, setBoard] = useState<(number | null)[][]>(Array(6).fill(null).map(() => Array(7).fill(null)))
    const [gameOver, setGameOver] = useState(false)
    const [message, setMessage] = useState("")


    const changePlayer = () => {
        return setCurrentPlayer(currentPlayer === player1 ? player2 : player1)
    }

    const play = (column: number) => {
        if (gameOver) {
            setMessage("Game Over! Please reset to play again.")
            return;
        }

        const newBoard = board.map(row => [...row]);
        let moveMade = false;

        for (let row = 5; row >= 0; row--) {
            const currentRow = newBoard[row]
            if (currentRow && !currentRow[column]) {
                currentRow[column] = currentPlayer
                moveMade = true;
                break;
            }
        }

        if (!moveMade) {
            return;
        }

        const result = validateAllMoves(newBoard);
        setBoard(newBoard)

        if (result === player1 || result === player2) {
            setGameOver(true)
            setMessage(`Player ${result} wins!!!`)
        } else if (result === "draw") {
            setGameOver(true)
            setMessage("Draw game.")
        } else {
            changePlayer()
        }
    }

    const resetGame = () => {
        setBoard(Array(6).fill(null).map(() => Array(7).fill(null)))
        setGameOver(false)
        setMessage("")
        setCurrentPlayer(1)
    }

    return (
        <div className="space-y-6">
            <div>
                <h1 className="mb-2">Connect 4</h1>
                <p className="text-muted-foreground">Classic Connect Four game - Get 4 in a row to win!</p>
            </div>

            <div className="connect4-container">
                <div className="connect4-game">
                    <div className="connect4-status">
                        {!gameOver && (
                            <p className="text-lg font-semibold">
                                Current Player: <span className={currentPlayer === 1 ? "text-red-500" : "text-yellow-500"}>
                                    Player {currentPlayer}
                                </span>
                            </p>
                        )}
                        {message && (
                            <p className={`message text-lg font-bold ${gameOver ? 'text-primary' : ''}`}>
                                {message}
                            </p>
                        )}
                    </div>

                    <div className="connect4-board-wrapper">
                        <table className="connect4-board">
                            <tbody>
                            {board.map((row, i) => <Row key={i} row={row} play={play}/>)}
                            </tbody>
                        </table>
                    </div>

                    {gameOver && (
                        <Button
                            onClick={resetGame}
                            className="mt-4"
                            size="lg"
                        >
                            Play Again
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
}
