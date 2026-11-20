

public Column setHeaderCaption(String caption) throws IllegalStateException {
    checkColumnIsAttached();

    String headerCaption = caption == null ? "" : caption;
    state.headerCaption = headerCaption;

    HeaderRow defaultRow = grid.getHeader().getDefaultRow();
    if (defaultRow != null) {
        HeaderCell cell = defaultRow.getCell(grid.getPropertyIdByColumnId(state.id));
        if (cell != null) {
            cell.setText(headerCaption);
        }
    }
    return this;
}
