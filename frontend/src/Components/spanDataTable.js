import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { Grid, Card, CardContent, Box } from '@material-ui/core';
import CustomizedTables from './Datatable';

const useStyles = makeStyles({
  idaCard: {
    marginTop: 15,
    marginBottom: 15
  }
});

export default function SpanningTable(props) {
  const classes = useStyles();
  const tableData = props.data || [];
  const keysName = [{
    'label': 'Column Index',
    'colName': 'colIndex',
    'key': 'colIndex'
  }, {
    'label': 'Column Name',
    'colName': 'colName',
    'key': 'colName'
  }, {
    'label': 'Column Description',
    'colName': 'colDesc',
    'key': 'colDesc'
  }, {
    'label': 'Column Data Type',
    'colName': 'colType',
    'key': 'colType'
  }];
  tableData.forEach(table => {
    table.metaData = Object.keys(table).filter(k => k !== 'fileColMd' && k !== 'metaData').map(key => ({
      'key': key,
      'value': table[key] + ''
    }));
  });
  return (
    <>
      {
        tableData.map(
          (table, i) => (
            <Card className={classes.idaCard} key={i}>
              <CardContent>
                {table.metaData.map(
                  (tableMd, j) => (
                    <Grid container spacing={3} key={j}>
                      <Grid container item xs={12} sm={6} md={4} lg={2}>
                        {tableMd.key}:
                      </Grid>
                      <Grid container item xs={12} sm={6} md={8} lg={10}>
                        {tableMd.value}
                      </Grid>
                    </Grid>
                  )
                )}
                <Box mt={2} mb={2}>
                  <CustomizedTables data={table.fileColMd} columns={keysName} noPagination={true} />
                </Box>
              </CardContent>
            </Card>
          )
        )
      }
    </>
  );
}
