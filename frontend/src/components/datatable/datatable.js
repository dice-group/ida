import React, { Component } from "react";
import PropTypes from "prop-types";
import { withStyles, useTheme } from "@material-ui/core/styles";
import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableContainer from "@material-ui/core/TableContainer";
import TableHead from "@material-ui/core/TableHead";
import TablePagination from "@material-ui/core/TablePagination";
import TableRow from "@material-ui/core/TableRow";
import TableSortLabel from "@material-ui/core/TableSortLabel";
import Paper from "@material-ui/core/Paper";
import IconButton from "@material-ui/core/IconButton";
import FirstPageIcon from "@material-ui/icons/FirstPage";
import KeyboardArrowLeft from "@material-ui/icons/KeyboardArrowLeft";
import KeyboardArrowRight from "@material-ui/icons/KeyboardArrowRight";
import LastPageIcon from "@material-ui/icons/LastPage";
import moment from "moment";

import { IDA_CONSTANTS } from "../constants";

function descendingComparator(a, b, orderBy) {
  let value1 = a[`${orderBy.id}`];
  let value2 = b[`${orderBy.id}`];
  if (orderBy.type === "numeric") {
    value1 = parseFloat(value1);
    value2 = parseFloat(value2);
    value1 = isNaN(value1) ? 0.0 : value1;
    value2 = isNaN(value2) ? 0.0 : value2;
  } else if (orderBy.type === "date") {
    value1 = moment(value1, IDA_CONSTANTS.DATE_FORMATS).valueOf();
    value2 = moment(value2, IDA_CONSTANTS.DATE_FORMATS).valueOf();
    value1 = isNaN(value1) ? "" : value1;
    value2 = isNaN(value2) ? "" : value2;
  }
  return value2 < value1 ? -1 : value2 > value1 ? 1 : 0;
}

function getComparator(order, orderBy) {
  return order === "desc"
    ? (a, b) => descendingComparator(a, b, orderBy)
    : order === "asc"
      ? (a, b) => -descendingComparator(a, b, orderBy)
      : (a, b) => "";
}

function stableSort(array, comparator) {
  const stabilizedThis = array.map((el, index) => [el, index]);
  stabilizedThis.sort((a, b) => {
    const order = comparator(a[0], b[0]);
    if (order !== 0) {
      return order;
    }
    return a[1] - b[1];
  });
  return stabilizedThis.map((el) => el[0]);
}

function EnhancedTableHead(props) {
  const { classes, order, orderBy, onRequestSort, headCells } = props;
  const createSortHandler = (property) => (event) => {
    onRequestSort(event, property);
  };

  return (
    <TableHead>
      <TableRow>
        {headCells.map((headCell) => (
          <TableCell
            key={headCell.id}
            align="left"
            sortDirection={orderBy.id === headCell.id ? order : false}
            className={orderBy.id === headCell.id && order ? "sort-active" : ""}
          >
            <TableSortLabel
              active={orderBy.id === headCell.id && order ? true : false}
              direction={orderBy.id === headCell.id && order ? order : "asc"}
              onClick={createSortHandler(headCell)}
            >
              {headCell.label}
            </TableSortLabel>
          </TableCell>
        ))}
      </TableRow>
    </TableHead>
  );
}

function TablePaginationActions(props) {
  const theme = useTheme();
  const { count, page, rowsPerPage, onChangePage } = props;

  const handleFirstPageButtonClick = (event) => {
    onChangePage(event, 0);
  };

  const handleBackButtonClick = (event) => {
    onChangePage(event, page - 1);
  };

  const handleNextButtonClick = (event) => {
    onChangePage(event, page + 1);
  };

  const handleLastPageButtonClick = (event) => {
    onChangePage(event, Math.max(0, Math.ceil(count / rowsPerPage) - 1));
  };

  return (
    <div className={"pagination-root"}>
      <IconButton
        onClick={handleFirstPageButtonClick}
        disabled={page === 0}
        aria-label="first page"
      >
        {theme.direction === "rtl" ? <LastPageIcon /> : <FirstPageIcon />}
      </IconButton>
      <IconButton onClick={handleBackButtonClick} disabled={page === 0} aria-label="previous page">
        {theme.direction === "rtl" ? <KeyboardArrowRight /> : <KeyboardArrowLeft />}
      </IconButton>
      <IconButton
        onClick={handleNextButtonClick}
        disabled={page >= Math.ceil(count / rowsPerPage) - 1}
        aria-label="next page"
      >
        {theme.direction === "rtl" ? <KeyboardArrowLeft /> : <KeyboardArrowRight />}
      </IconButton>
      <IconButton
        onClick={handleLastPageButtonClick}
        disabled={page >= Math.ceil(count / rowsPerPage) - 1}
        aria-label="last page"
      >
        {theme.direction === "rtl" ? <FirstPageIcon /> : <LastPageIcon />}
      </IconButton>
    </div>
  );
}

TablePaginationActions.propTypes = {
  count: PropTypes.number.isRequired,
  onChangePage: PropTypes.func.isRequired,
  page: PropTypes.number.isRequired,
  rowsPerPage: PropTypes.number.isRequired,
};

const useStyles = (theme) => ({
  root: {
    width: "100%",
  },
  paper: {
    width: "100%",
    marginBottom: theme.spacing(2)
  },
  visuallyHidden: {
    border: 0,
    clip: "rect(0 0 0 0)",
    height: 1,
    margin: -1,
    overflow: "hidden",
    padding: 0,
    position: "absolute",
    top: 20,
    width: 1,
  },
});

class IDAEnhancedTable extends Component {
  rows = [];
  emptyRows = 0;
  columns = [];
  tableId = "";
  noPagination;

  constructor(props) {
    super(props);
    this.state = {
      order: "asc",
      orderBy: "",
      page: 0,
      rowsPerPage: props.noPagination ? 0 : 5,
      rowsPerPageList: []
    };
    this.noPagination = props.noPagination;
    this.rows = props.data;
    this.tableId = props.nodeId;
    this.columns = props.columns.map((col) => ({
      "id": col.colAttr,
      "label": col.colName,
      "type": col.colType
    }));
    this.emptyRows = this.state.rowsPerPage - Math.min(this.state.rowsPerPage, this.rows.length - this.state.page * this.state.rowsPerPage);
  }

  componentDidMount() {
    setTimeout(() => {
      if (this.tableId && document.getElementById(this.tableId)) {
        const rowHeight = document.getElementById(this.tableId).getElementsByClassName("ida-table-row")[0].offsetHeight;
        if (rowHeight) {
          const defaultRowsPerPage = Math.floor((window.innerHeight * 0.65) / rowHeight);
          this.setState({
            page: 0,
            rowsPerPage: defaultRowsPerPage,
            rowsPerPageList: [defaultRowsPerPage, defaultRowsPerPage * 2, defaultRowsPerPage * 3, defaultRowsPerPage * 4, defaultRowsPerPage * 5]
          });
        }
      }
    }, 100);
  }

  handleRequestSort = (event, property) => {
    const isAsc = this.state.orderBy === property && this.state.order === "asc";
    const isDesc = this.state.orderBy === property && this.state.order === "desc";
    this.setState({
      order: isAsc ? "desc" : isDesc ? false : "asc",
      orderBy: property
    });
  };

  handleChangePage = (event, newPage) => {
    this.setState({
      page: newPage
    });
  };

  handleChangeRowsPerPage = (event) => {
    this.setState({
      rowsPerPage: parseInt(event.target.value, 10),
      page: 0
    });
  };

  render() {
    const classes = this.props.classes;
    return (
      <div className={classes.root} >
        <Paper className={classes.paper}>
          <TableContainer className="ida-table-container">
            <Table
              className="ida-table"
              aria-labelledby="tableTitle"
              aria-label="enhanced table"
              id={this.tableId}
            >
              <EnhancedTableHead
                classes={classes}
                order={this.state.order}
                orderBy={this.state.orderBy}
                onRequestSort={this.handleRequestSort}
                rowCount={this.rows.length}
                headCells={this.columns}
              />
              <TableBody>
                {stableSort(this.rows, getComparator(this.state.order, this.state.orderBy))
                  .slice(this.state.page * this.state.rowsPerPage, this.noPagination ? this.rows.length : this.state.page * this.state.rowsPerPage + this.state.rowsPerPage)
                  .map((row, index) => {
                    return (
                      <TableRow
                        hover
                        tabIndex={-1}
                        key={index}
                        className="ida-table-row"
                      >
                        {this.columns.map((colName, index) => (
                          <TableCell align="left" component="th" scope="row" key={index} >{row[colName["id"].trim()]}</TableCell>
                        ))}
                      </TableRow>
                    );
                  })}
                {this.emptyRows > 0 && (
                  <TableRow style={{ height: 53 * this.emptyRows }}>
                    <TableCell colSpan={6} />
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
          {
            this.noPagination ? null : <TablePagination
              rowsPerPageOptions={this.state.rowsPerPageList}
              component="div"
              colSpan={this.columns.length}
              count={this.rows.length}
              rowsPerPage={this.state.rowsPerPage}
              page={this.state.page}
              SelectProps={{
                inputProps: { "aria-label": "rows per page" },
                native: true,
              }}
              onChangePage={this.handleChangePage}
              onChangeRowsPerPage={this.handleChangeRowsPerPage}
              ActionsComponent={TablePaginationActions}
            />
          }
        </Paper>
      </div>
    );
  }
}

export default withStyles(useStyles)(IDAEnhancedTable);
