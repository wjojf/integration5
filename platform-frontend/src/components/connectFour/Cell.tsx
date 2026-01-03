type CellProps = {
    value: number | null
    columnIndex: number
    play: (columnIndex: number) => void
    disabled?: boolean
}

export const Cell = ({ value, columnIndex, play, disabled = false }: CellProps) => {
    const color = value === 1 ? "red" : value === 2 ? "yellow" : "white";

    const handleClick = () => {
        if (!disabled) {
            play(columnIndex);
        }
    };

    return (
        <td>
            <div 
                className={`cell ${disabled ? 'cell-disabled' : ''}`} 
                onClick={handleClick}
                style={{ cursor: disabled ? 'not-allowed' : 'pointer' }}
            >
                <div className={color}></div>
            </div>
        </td>
    );
}
