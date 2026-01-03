import {Cell} from "./Cell";

type RowProps = {
    row: (number | null)[]
    play: (columnIndex: number) => void
    disabled?: boolean
}

export const Row = ({row, play, disabled = false}: RowProps) => {
    const rowCells = row.map((cell, i) => (
        <Cell 
            key={i} 
            value={cell} 
            columnIndex={i} 
            play={play}
            disabled={disabled}
        />
    ))

    return (
        <tr>{rowCells}</tr>
    );

}
