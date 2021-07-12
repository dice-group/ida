import React from "react";
import PropTypes from "prop-types";
import AppBar from "@material-ui/core/AppBar";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Box from "@material-ui/core/Box";
import SpanningTable from "../datatable/spanDataTable";
import IDABarChart from "./../visualizations/barchart/barchart";
import IDABubbleGraph from "./../visualizations/bubblechart/bubblechart";
import CloseIcon from "@material-ui/icons/Close";
import "./tabs.css";
import { Grid, IconButton } from "@material-ui/core";
import IDALineChart from "../visualizations/linechart/linechart";
import IDAScatterPLot from "../visualizations/scatterplot/scatterplot";
import IDAGroupedBarGraph from "../visualizations/groupedBarChart/groupedBarChart";
import IDAGroupedBubbleChart from "../visualizations/groupedBubbleChart/groupedBubbleChart";
import IDAScatterPlotMatrix from "../visualizations/scatterplotMatrix/scatterplotMatrix";
import IDAVisualizationSuggestion from "../vizSuggestion/vizSuggestion";
import IDAEnhancedTable from "../datatable/datatable";

function TabPanel(props) {
  const { children, value, index, ...other } = props;
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`scrollable-auto-tabpanel-${index}`}
      aria-labelledby={`scrollable-auto-tab-${index}`}
      {...other}
    >
      <Box p={3}>
        {children}
      </Box>
    </div>
  );
}

TabPanel.propTypes = {
  children: PropTypes.node,
  index: PropTypes.any.isRequired,
  value: PropTypes.any.isRequired,
};

function TabHeader(props) {
  const { removeTab, ...newProps } = props;
  return (
    <div>
      <Grid container alignItems="flex-start" wrap="nowrap">
        <Tab {...newProps} title={props.label} />
        <IconButton onClick={() => removeTab(props.value)}>
          <CloseIcon />
        </IconButton>
      </Grid>
    </div>
  );
}

export default function ScrollableTabsButtonAuto(props) {
  const data = props.detail.find((ds) => ds.id === props.selectedNodeId || ds.children.findIndex((child) => child.id === props.selectedNodeId) >= 0) || {};
  const tabs = props.tabs;
  const value = props.selectedNodeId || data.id;
  const handleChange = (event, newValue) => {
    props.setSelectedNodeId(newValue);
    const selectedTab = tabs.find((tab) => tab.id === newValue && (tab.type === "table" || tab.type === "clustering" || tab.type === "suggestion")) || {};
    props.setActiveTable(selectedTab.fileName || "");
    if (selectedTab.type === "clustering") {
      props.setActiveTableData(selectedTab.data);
    } else {
      props.setActiveTableData(null);
    }
  };
  const renderData = (tab) => {
    switch (tab.type) {
      case "table":
        return <IDAEnhancedTable data={tab.data} columns={tab.columns} nodeId={tab.id} />;
      case "metadata":
        return <SpanningTable data={tab.data} />;
      case "barchart":
        return <IDABarChart data={tab.data} nodeId={tab.id} />;
      case "bubblechart":
        return <IDABubbleGraph data={tab.data} nodeId={tab.id} />;
      case "linechart":
        return <IDALineChart data={tab.data} nodeId={tab.id} />;
      case "clustering":
        return <IDAEnhancedTable data={tab.data} columns={tab.columns} nodeId={tab.id} />;
      case "scatterplot":
        return <IDAScatterPLot data={tab.data} nodeId={tab.id} />;
      case "scatterplotmatrix":
        return <IDAScatterPlotMatrix data={tab.data} nodeId={tab.id} />;
      case "groupedBarchart":
        return <IDAGroupedBarGraph data={tab.data} nodeId={tab.id} />;
      case "groupedBubblechart":
        return <IDAGroupedBubbleChart data={tab.data} nodeId={tab.id} />;
      case "suggestion":
        return <IDAVisualizationSuggestion data={tab.data} nodeId={tab.id} tableName={tab.fileName} isChatbotOpen={props.isChatbotOpen} setIsChatbotOpen={props.setIsChatbotOpen} />;
      default:
        return null;
    }
  };
  const removeTab = (tabId) => {
    const newTabs = tabs.filter((t) => t.id !== tabId);
    if (tabId === value) {
      handleChange(null, newTabs[0] ? newTabs[0].id : "");
    }
    props.setTabs(newTabs);
  };

  return (
    <div className={"root"}>
      <ul className={"tab-list"}>
        <AppBar position="static" color="default"  >
          <Tabs
            onChange={handleChange}
            indicatorColor="primary"
            textColor="primary"
            variant="scrollable"
            scrollButtons="auto"
            aria-label="scrollable auto tabs example"
            value={value}
            selectionFollowsFocus={true}
          >
            {tabs.map(
              (tab) => (
                <TabHeader value={tab.id} label={tab.name} key={tab.id} removeTab={removeTab}>
                </TabHeader>
              ))}
          </Tabs>
        </AppBar>
        {tabs.map(
          (tab) => (
            <TabPanel value={value} index={tab.id} key={tab.id}>
              {renderData(tab)}
            </TabPanel>
          )
        )}
      </ul>
    </div>
  );
}
