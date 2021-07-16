import { IDA_CONSTANTS } from "./constants";

function updateActiveTab(props, expandedNodes, parentSuffix, nodeId, activeDSName) {
    expandedNodes.indexOf(activeDSName + parentSuffix) < 0 && expandedNodes.push(activeDSName + parentSuffix);
    props.setExpandedNodeId(expandedNodes);
    props.setSelectedNodeId(nodeId);
    if (window.matchMedia("(max-width: 991px)").matches) {
        props.setIsChatbotOpen(false);
    }
}

function addVisualizationEntry(props, vizData, label, name, activeDSName) {
    const treeData = props.detail;
    const activeDS = treeData.find((node) => node.id === activeDSName);
    const vizChildren = activeDS.children.find((c) => c.id === activeDSName + "_visualizations");
    const viz = vizChildren || {
        id: activeDSName + "_visualizations",
        name: "Visualizations",
        type: "parent",
        children: []
    };
    if (!vizChildren) {
        activeDS.children.push(viz);
    }
    const vizCount = viz.children.filter((c) => c.type === name).length;
    const vizNode = {
        id: activeDSName + "_" + name + "_" + (vizCount + 1),
        name: label + " " + (vizCount + 1),
        type: name,
        data: vizData,
        fileName: label + " " + (vizCount + 1)
    };
    viz.children.push(vizNode);
    const tabs = props.tabs;
    tabs.push(vizNode);
    props.setTabs(tabs);
    props.setDetails(treeData);
    props.setActiveTableData(null);
    updateActiveTab(props, props.expandedNodeId, "_visualizations", vizNode.id, activeDSName);
}

function addAnalysisEntry(props, analysisData, label, name, activeDSName, tableName, columns, paramData) {
    const treeData = props.detail;
    const activeDS = treeData.find((node) => node.id === activeDSName);
    const analysisChildren = activeDS.children.find((c) => c.id === activeDSName + "_analyses");
    const analysis = analysisChildren || {
        id: activeDSName + "_analyses",
        name: "Analyses",
        type: "parent",
        children: []
    };
    if (!analysisChildren) {
        activeDS.children.push(analysis);
    }
    const analysisCount = analysis.children.filter((c) => c.type === name).length;
    const analysisNode = {
        id: activeDSName + "_" + name + "_" + (analysisCount + 1),
        name: label + " " + (analysisCount + 1),
        type: name,
        data: analysisData,
        linkData: paramData,
        fileName: tableName,
        columns: columns.map((k) => ({ colAttr: k, colName: k })),
        dsName: activeDSName
    };
    analysis.children.push(analysisNode);
    const tabs = props.tabs;
    tabs.push(analysisNode);
    props.setTabs(tabs);
    props.setDetails(treeData);
    props.setActiveTable(tableName);
    props.setActiveDS(activeDSName);
    name === "clustering" && props.setActiveTableData(analysisData);
    updateActiveTab(props, props.expandedNodeId, "_analyses", analysisNode.id, activeDSName);
}

export default function idaChatbotActionHandler(props, actionCode, payload) {
    switch (actionCode) {
        case IDA_CONSTANTS.UI_ACTION_CODES.UIA_LOADDS: {
            const metaData = payload.dsMd || {};
            const data = payload.dsData || [];
            const children = [{
                id: metaData.dsName + "_metadata",
                name: metaData.dsName + " Metadata",
                type: "metadata",
                data: metaData.filesMd,
                fileName: "dsmd.json",
				dsName: metaData.dsName
            }];
            data.forEach((table, idx) =>
                children.push({
                    id: metaData.dsName + "_" + table.name,
                    name: metaData.filesMd[parseInt(idx, 10)].displayName,
                    type: "table",
                    data: table.data,
                    fileName: table.name,
                    columns: metaData.filesMd.filter((fl) => fl.fileName.toLowerCase() === table.name.toLowerCase())[0].fileColMd,
                    dsName: metaData.dsName
                })
            );
            const main = {
                id: metaData.dsName,
                name: metaData.dsName,
                type: "parent",
                children: [{
                    id: metaData.dsName + "_dataset",
                    name: "Datasets",
                    type: "parent",
                    children
                }]
            };
            const dataSets = props.detail || [];
            if (dataSets.findIndex((ds) => ds.id === main.id) < 0) {
                dataSets.push(main);
                props.setDetails(dataSets);
            }
            const tabs = props.tabs;
            if (tabs.findIndex((t) => t.id === children[0].id) < 0) {
                tabs.push(children[0]);
                props.setTabs(tabs);
            }
            const expandedNodes = props.expandedNodeId;
            expandedNodes.push(main.id);
            expandedNodes.push(metaData.dsName + "_dataset");
            props.setExpandedNodeId(expandedNodes);
            props.setNavBarClass("");
            props.setActiveTable("");
            props.setActiveDS(metaData.dsName);
            props.setSelectedNodeId(children[0].id);
            props.setLoaded(true);
            props.setActiveTableData(null);
            if (window.matchMedia("(max-width: 991px)").matches) {
                props.setIsChatbotOpen(false);
            }
            break;
        }
        case IDA_CONSTANTS.UI_ACTION_CODES.UAC_BARGRAPH: {
            addVisualizationEntry(props, payload.barGraphData, "Bar Graph", "barchart", payload.activeDS);
            break;
        }
        case IDA_CONSTANTS.UI_ACTION_CODES.UAC_BUBBLECHART: {
            addVisualizationEntry(props, payload.bubbleChartData, "Bubble Chart", "bubblechart", payload.activeDS);
            break;
        }
        case IDA_CONSTANTS.UI_ACTION_CODES.UAC_LINECHART: {
            addVisualizationEntry(props, payload.lineChartData, "Line Chart", "linechart", payload.activeDS);
            break;
        }
		case IDA_CONSTANTS.UI_ACTION_CODES.UAC_SCATTERPLOT: {
			addVisualizationEntry(props, payload.scatterPlotData, "Scatter Plot", "scatterplot", payload.activeDS);
			break;
		}
		case IDA_CONSTANTS.UI_ACTION_CODES.UAC_SCATTERPLOTMATRIX: {
			addVisualizationEntry(props, payload.scatterPlotMatrixData, "Scatter Plot Matrix", "scatterplotmatrix", payload.activeDS);
			break;
		}
		case IDA_CONSTANTS.UI_ACTION_CODES.UAC_CLUSTERING: {
            const clusteredData = payload.clusteredData;
            clusteredData.sort((a, b) => parseInt(a.Cluster, 10) > parseInt(b.Cluster, 10) ? 1 : parseInt(a.Cluster, 10) < parseInt(b.Cluster, 10) ? -1 : 0);
            addAnalysisEntry(props, clusteredData, "Clustering", "clustering", payload.activeDS, payload.activeTable, payload.columns, payload.scatterPlotMatrixParams);
            break;
        }
        case IDA_CONSTANTS.UI_ACTION_CODES.UAC_GROUPED_BARCHART: {
            addVisualizationEntry(props, payload.barGraphData, "Grouped Bar Chart", "groupedBarchart", payload.activeDS);
            break;
        }
        case IDA_CONSTANTS.UI_ACTION_CODES.UAC_GROUPED_BUBBLECHART: {
            addVisualizationEntry(props, payload.bubbleChartData, "Grouped Bubble Chart", "groupedBubblechart", payload.activeDS);
            break;
        }
        case IDA_CONSTANTS.UI_ACTION_CODES.UAC_VIZ_SUGGESTION: {
            addAnalysisEntry(props, payload.suggestionData, "Visualization Suggestion", "suggestion", payload.activeDS, payload.activeTable, []);
            break;
        }
		case IDA_CONSTANTS.UI_ACTION_CODES.UAC_UPLDDTMSG: {
			props.setdsUploadWizardOpen(true);
		}
        default:
    }
}
