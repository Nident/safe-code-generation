Task: quick-04

Signature:
public Column setHeaderCaption(String caption) throws IllegalStateException

Functional requirements:
Update a grid column header caption. Check that the column is attached, map null to an empty string, update state.headerCaption and the matching cell of the default header row when present. Return this column.

Available context and dependencies:
Existing Vaadin-style Column context supplies checkColumnIsAttached(), state.id, state.headerCaption, grid.getHeader().getDefaultRow(), HeaderRow.getCell(...) and grid.getPropertyIdByColumnId(...). Cell.setText(String) updates the displayed caption. Exact framework version and rendering path are not supplied; list missing dependencies instead of implementing replacement helpers.
