package org.dice.ida.action.def;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.protobuf.ListValue.Builder;
import org.apache.http.ParseException;
import org.apache.http.client.utils.DateUtils;
import org.dice.ida.constant.IDAConst;
import org.dice.ida.exception.IDAException;
import org.dice.ida.model.ChatMessageResponse;
import org.dice.ida.model.ChatUserMessage;
import org.dice.ida.model.LableComparator;
import org.dice.ida.model.bargraph.BarGraphData;
import org.dice.ida.model.bargraph.BarGraphItem;
import org.dice.ida.model.bubblechart.BubbleChartData;
import org.dice.ida.model.bubblechart.BubbleChartItem;
import org.dice.ida.model.groupedbargraph.GroupedBarGraphData;
import org.dice.ida.model.groupedbubblechart.GroupedBubbleChartData;
import org.dice.ida.model.linechart.LineChartData;
import org.dice.ida.model.linechart.LineChartItem;
import org.dice.ida.model.scatterplot.ScatterPlotData;
import org.dice.ida.model.scatterplot.ScatterPlotItem;
import org.dice.ida.model.scatterplotmatrix.ScatterPlotMatrixData;

import java.util.Map;
import java.util.List;
import java.util.Comparator;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.Objects;
import java.util.Calendar;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.dice.ida.util.DataUtil;
import org.dice.ida.util.DialogFlowUtil;
import org.dice.ida.util.RDFUtil;
import org.dice.ida.util.ValidatorUtil;
import org.dice.ida.util.TextUtil;
import org.dice.ida.util.FileUtil;


/**
 * Class to handle the any visualization
 *
 * @author Nandeesh Patel, Sourabh Poddar
 */
@Component
public class VisualizeAction implements Action {

	@Autowired
	private DialogFlowUtil dialogFlowUtil;

	@Autowired
	private DataUtil dataUtil;

	@Autowired
	private RDFUtil rdfUtil;

	private List<Map<String, String>> tableData;
	private Map<Integer, String> attributeList;
	private Map<String, Boolean> attributeOptionalMap;
	private Map<String, Map<String, Map<String, String>>> instanceMap;
	private Map<String, String> columnMap;
	private Map<String, String> columnUniquenessMap;
	private Map<String, String> parameterMap;
	private Map<String, String> parameterTypeMap;
	private Map<String, Object> payload;
	private Map<String, Double> graphItems;
	private Map<String, Map<String, Double>> groupedGraphItems;
	private Comparator<String> comparator;
	private StringBuilder textMsg;
	private boolean labelNeeded;
	private String labelColumn;
	private List<String> lineChartXAxisLabels = new ArrayList<>();
	private ArrayList<String> columnList = new ArrayList<>();
	private String refColumn = null;
	private boolean allParamsProcessed;
	private Map<String, String> paramDisplayNameMap = null;
	private Map<String, String> paramDisplayMessageMap = null;
	private Map<String, String> paramOptionalMessageMap = null;


	/**
	 * @param paramMap            - parameters from dialogflow
	 * @param chatMessageResponse - API response object
	 */
	@Override
	public void performAction(Map<String, Object> paramMap, ChatMessageResponse chatMessageResponse, ChatUserMessage message) throws IOException, IDAException, InvalidKeySpecException, NoSuchAlgorithmException {
		textMsg = new StringBuilder(paramMap.get(IDAConst.PARAM_TEXT_MSG).toString());
		List<String> columnNameList = new ArrayList<>();
		Set<String> options = new HashSet<>();
		boolean columnListVizProcessed = false;
		if (ValidatorUtil.preActionValidation(chatMessageResponse)) {
			String vizType = paramMap.get(IDAConst.INTENT_NAME).toString();
			payload = chatMessageResponse.getPayload();
			instanceMap = rdfUtil.getInstances(vizType);
			String datasetName = payload.get("activeDS").toString();
			String tableName = payload.get("activeTable").toString();
			boolean onTemporaryData = message.isTemporaryData();
			String filterString = paramMap.get(IDAConst.PARAM_FILTER_STRING).toString();
			String intent = paramMap.get(IDAConst.INTENT_NAME).toString();
			attributeList = rdfUtil.getAttributeList(intent);
			attributeOptionalMap = rdfUtil.getAttributeOptionalMap(intent);
			paramDisplayNameMap = rdfUtil.getParamDisplayNames();
			paramDisplayMessageMap = rdfUtil.getParamDisplayMessages();
			paramOptionalMessageMap = rdfUtil.getParamOptionalMessages();
			if (attributeList.containsValue(IDAConst.HAS_LIST_COLUMN)) {
				columnListVizProcessed = processListAttribute(paramMap, vizType, datasetName, tableName, onTemporaryData, filterString, message);
			} else {
				columnNameList = getColumnNames(attributeList, paramMap);
				List<Map<String, String>> columnDetail = ValidatorUtil.areParametersValid(datasetName, tableName, columnNameList, onTemporaryData);
				columnMap = columnDetail.get(0);
				columnUniquenessMap = columnDetail.get(1);
				allParamsProcessed = false;
				options = processParameters(paramMap);
			}
			if ((options.size() == 1 && allParamsProcessed) || columnListVizProcessed) {
				if (options.size() == 1) {
					getParameters(paramMap);
					if (onTemporaryData) {
						tableData = message.getActiveTableData();
						tableData = dataUtil.filterData(tableData, columnNameList, columnMap);
					} else {
						tableData = dataUtil.getData(datasetName, tableName, columnNameList, columnMap);
					}
					comparator = LableComparator.getForKey(IDAConst.COMPARATOR_TYPE_UNKNOWN);
				}

				switch (vizType) {
					case IDAConst.VIZ_TYPE_BAR_CHART:
						createGraphData(IDAConst.X_AXIS_PARAM, IDAConst.Y_AXIS_PARAM, paramMap);
						if (Boolean.parseBoolean(paramMap.getOrDefault("Group_Column_choice", "").toString())) {
							createGroupedBarGraphResponse();
							chatMessageResponse.setUiAction(IDAConst.UIA_GROUPED_BARGRAPH);
						} else {
							createBarGraphResponse();
							chatMessageResponse.setUiAction(IDAConst.UIA_BARGRAPH);
						}
						chatMessageResponse.setMessage(IDAConst.BAR_GRAPH_LOADED);

						break;
					case IDAConst.VIZ_TYPE_BUBBLE_CHART:
						createGraphData(IDAConst.BUBBLE_LABEL_PARAM, IDAConst.BUBBLE_SIZE_PARAM, paramMap);
						if (Boolean.parseBoolean(paramMap.getOrDefault("Group_Column_choice", "").toString())) {
							createGroupedBubbleChartResponse();
							chatMessageResponse.setUiAction(IDAConst.UIA_GROUPED_BUBBLECHART);
						} else {
							createBubbleChartResponse(datasetName, tableName);
							chatMessageResponse.setUiAction(IDAConst.UIA_BUBBLECHART);
						}
						chatMessageResponse.setMessage(IDAConst.BC_LOADED);
						break;
					case IDAConst.VIZ_TYPE_SCATTER_PLOT:
						createScatterPlotData();
						chatMessageResponse.setUiAction(IDAConst.UIA_SCATTERPLOT);
						chatMessageResponse.setMessage(IDAConst.SCATTER_PLOT_LOADED);
						break;
					case IDAConst.VIZ_TYPE_LINE_CHART:
						createLineChartResponse(paramMap);
						chatMessageResponse.setUiAction(IDAConst.UIA_LINECHART);
						chatMessageResponse.setMessage(IDAConst.LINE_CHART_LOADED);
						break;
					case IDAConst.VIZ_TYPE_SCATTER_PLOT_MATRIX:
						createScatterPlotMatrixData(tableData, columnList, labelColumn, refColumn);
						chatMessageResponse.setUiAction(IDAConst.UIA_SCATTERPLOT_MATRIX);
						chatMessageResponse.setMessage(IDAConst.SCATTER_PLOT_MATRIX_LOADED);
						break;
					default:
						chatMessageResponse.setMessage(IDAConst.BOT_SOMETHING_WRONG);
						chatMessageResponse.setUiAction(IDAConst.UAC_NRMLMSG);
						break;
				}
				dialogFlowUtil.resetContext();
			} else {
				chatMessageResponse.setMessage(textMsg.toString());
				chatMessageResponse.setUiAction(IDAConst.UAC_NRMLMSG);
			}
		}
	}

	/**
	 * Method to process visualization with List type attribute
	 *
	 * @param paramMap        - param map from Dialogflow
	 * @param vizType         - Name of the Visualization
	 * @param datasetName     - Dataset Name
	 * @param tableName       - Table Name
	 * @param onTemporaryData - boolean to check if temporary data loaded
	 * @param filterString    - filter String
	 * @return - true if the list attributes are processed
	 */
	private boolean processListAttribute(Map<String, Object> paramMap, String vizType, String datasetName, String tableName, boolean onTemporaryData, String filterString, ChatUserMessage message) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, IDAException {
		List<Map<String, String>> columnDetail;
		ArrayList<String> columnListParameter = new ArrayList<>();
		String selectAll = (String) paramMap.get("All_select");
		boolean columnListVizProcessed = false;
		instanceMap = rdfUtil.getInstances(vizType);
		List<String> attributeList = new ArrayList<>();
		Map<String, String> attributeTypeMap = new HashMap<>();
		instanceMap.keySet().forEach(instance -> attributeList.addAll(instanceMap.get(instance).keySet()));
		for (String attribute : attributeList) {
			instanceMap.keySet().forEach(instance -> attributeTypeMap.put(attribute, instanceMap.get(instance).get(attribute).get("type")));
		}
		Value paramVal;
		if ("true".equals(paramMap.get("from_suggestion"))) {
			Builder listBuilder = ListValue.newBuilder();
			((List<String>) paramMap.get(attributeList.get(0))).forEach(c -> listBuilder.addValues(Value.newBuilder().setStringValue(c).build()));
			paramVal = Value.newBuilder().setListValue(listBuilder.build()).build();
		} else {
			paramVal = (Value) paramMap.get(attributeList.get(0));
		}
		if (!(selectAll == null && paramVal == null)) {
			paramVal.getListValue().getValuesList().forEach(str -> columnListParameter.add(str.getStringValue()));
			if (!selectAll.isEmpty()) {
				columnList = getColumnList(datasetName, tableName);
			} else {
				columnList = columnListParameter;
			}
			columnDetail = ValidatorUtil.areParametersValid(datasetName, tableName, columnList, onTemporaryData);
			columnMap = columnDetail.get(0);
			columnList = filterColumns(columnList, attributeTypeMap.get("Column_List"));
			if (columnList.size() < 2) {
				textMsg = new StringBuilder("Please provide more than one Numeric columns");
				dialogFlowUtil.deleteContext("get_labelColumn");
			} else {
				dialogFlowUtil.deleteContext("get_column");
				refColumn = (String) paramMap.get(attributeList.get(2));
				if (refColumn != null) {
					if (columnMap.containsKey(refColumn)) {
						columnList.add(refColumn);
						labelNeeded = false;
						dialogFlowUtil.deleteContext("get_labelColumn");
						if (handleLabelLogic(paramMap, attributeList)) {
							if (labelNeeded) {
								columnList.add(labelColumn);
							}
							if (onTemporaryData) {
								tableData = message.getActiveTableData();
								tableData = dataUtil.filterData(tableData, columnList, columnMap);
							} else
								tableData = dataUtil.getData(datasetName, tableName, columnList, columnMap);
							columnListVizProcessed = true;
						}
					} else {
						textMsg = new StringBuilder("Column <b>" + refColumn + "</b> doesn't exist in the table " + tableName);
					}
				} else {
					textMsg = new StringBuilder(paramDisplayMessageMap.get(IDAConst.SCATTERPLOT_MATRIX_LABEL_PARAM));
				}
			}
		}
		return columnListVizProcessed;
	}

	/**
	 * Method to populate the Scatter plot matrix data object in the response.
	 */
	private void createScatterPlotMatrixData(List<Map<String, String>> tableData, ArrayList<String> columnList, String ref_column, String labelColumn) {
		ScatterPlotMatrixData scatterPlotMatrixData = new ScatterPlotMatrixData();
		scatterPlotMatrixData.setReferenceColumn(ref_column);
		scatterPlotMatrixData.setLabelColumn(labelColumn);
		ArrayList<String> numericColumns;
		numericColumns = (ArrayList<String>) columnList.clone();
		numericColumns.remove(ref_column);
		numericColumns.remove(labelColumn);
		scatterPlotMatrixData.setColumns(numericColumns);
		scatterPlotMatrixData.setItems(tableData);
		payload.put("scatterPlotMatrixData", scatterPlotMatrixData);
	}

	/**
	 * Method to filter column list bases on required data type.
	 */
	private ArrayList<String> filterColumns(ArrayList<String> columnList, String type) {

		ArrayList<String> columnListTemp;
		columnListTemp = (ArrayList<String>) columnList.clone();
		for (String column : columnList) {
			if (!columnMap.get(column).equalsIgnoreCase(type))
				columnListTemp.remove(column);
		}
		return columnListTemp;
	}

	/**
	 * Method to get list of columns from a table.
	 *
	 * @param datasetName - Data set name
	 * @param tableName   - Table Name
	 * @return List of colums
	 */
	private ArrayList<String> getColumnList(String datasetName, String tableName) {

		ObjectNode metaData = null;
		ArrayList<String> columns = new ArrayList<>();
		try {
			metaData = new FileUtil().getDatasetMetaData(datasetName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		JsonNode fileDetails = metaData.get(IDAConst.FILE_DETAILS_ATTR);
		for (int i = 0; i < fileDetails.size(); i++) {
			if (tableName.equals(fileDetails.get(i).get(IDAConst.FILE_NAME_ATTR).asText())) {
				JsonNode columnDetails = fileDetails.get(i).get(IDAConst.COLUMN_DETAILS_ATTR);
				for (int j = 0; j < columnDetails.size(); j++)
					columns.add(columnDetails.get(j).get(IDAConst.COLUMN_NAME_ATTR).asText());
			}
		}
		return columns;
	}

	/**
	 * Method to handle the chatbot flow for label in scatter plot matrix visualizations
	 *
	 * @param paramMap - parameter map from dialogflow
	 * @return - true if label flow is complete and false otherwise
	 * @throws NoSuchAlgorithmException - dialogflow auth encryption algorithm is invalid
	 * @throws IOException              - dialogflow credentials file does not exist
	 * @throws InvalidKeySpecException  - dialogflow auth key invalid
	 */
	private boolean handleLabelLogic(Map<String, Object> paramMap, List<String> attributeList) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		String isLabelNeeded = paramMap.getOrDefault("Reference_Column_Choice", "").toString();
		labelColumn = "";
		if (isLabelNeeded.isEmpty()) {
			textMsg = new StringBuilder(paramOptionalMessageMap.get(IDAConst.SCATTERPLOT_MATRIX_REFERENCE_PARAM));
		} else if (!Boolean.parseBoolean(isLabelNeeded)) {
			labelNeeded = false;
			return true;
		} else {
			labelNeeded = true;
			labelColumn = (String) paramMap.getOrDefault(attributeList.get(1), "");
			if (labelColumn.isEmpty()) {
				textMsg = new StringBuilder(paramDisplayMessageMap.get(IDAConst.SCATTERPLOT_MATRIX_REFERENCE_PARAM));
			} else {
				if (!columnMap.containsKey(labelColumn)) {
					textMsg = new StringBuilder("Column " + labelColumn + " doesn't exist in the loaded table.");
				} else
					return true;
			}
		}
		return false;
	}

	/**
	 * Method to process the user input for the visualization parameters and get all available options
	 *
	 * @param paramMap - param map from Dialogflow
	 * @return - set of options possible for rendering the visualization based on user inputs
	 */
	private Set<String> processParameters(Map<String, Object> paramMap) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		String attributeType;
		String attributeName;
		String paramType;
		Set<String> options = new HashSet<>();
		int processedParameterCount = 0;
		for (int i = 1; i <= attributeList.size(); i++) {
			Set<String> remainingAttributes = new HashSet<>();
			instanceMap.keySet().forEach(k -> remainingAttributes.addAll(instanceMap.get(k).keySet()));
			attributeName = attributeList.get(i);
			attributeType = paramMap.getOrDefault(attributeName + IDAConst.ATTRIBUTE_TYPE_SUFFIX, "").toString();
			if (!remainingAttributes.contains(attributeName)) {
				processedParameterCount++;
				continue;
			}
			if (attributeOptionalMap.get(attributeName) && paramMap.getOrDefault(attributeName, "").toString().isEmpty()) {
				if (paramMap.getOrDefault(attributeName + IDAConst.ATTRIBUTE_CHOICE_SUFFIX, "").toString().isEmpty()) {
					dialogFlowUtil.setContext("get_" + attributeName + IDAConst.ATTRIBUTE_CHOICE_SUFFIX, 5);
					String question = "Do you want to use " + paramDisplayNameMap.getOrDefault(attributeList.get(i), attributeList.get(i)) + "?";
					if (paramOptionalMessageMap.containsKey(attributeList.get(i))) {
						question = paramOptionalMessageMap.get(attributeList.get(i));
					}
					textMsg = new StringBuilder(question);
					break;
				}
				boolean attributeChoice = Boolean.parseBoolean(paramMap.getOrDefault(attributeName + IDAConst.ATTRIBUTE_CHOICE_SUFFIX, "").toString());
				if (attributeChoice) {
					dialogFlowUtil.deleteContext("get_" + attributeName + IDAConst.ATTRIBUTE_CHOICE_SUFFIX);
					dialogFlowUtil.setContext("get_" + attributeList.get(i), 5);
					String question = "Which column values should be mapped to " + paramDisplayNameMap.getOrDefault(attributeList.get(i), attributeList.get(i)) + "?";
					if (paramDisplayMessageMap.containsKey(attributeList.get(i))) {
						question = paramDisplayMessageMap.get(attributeList.get(i));
					}
					textMsg = new StringBuilder(question);
					break;
				} else {
					attributeList.remove(i);
					break;
				}
			} else {
				if (paramMap.getOrDefault(attributeName, "").toString().isEmpty()) {
					if (i > 1) {
						dialogFlowUtil.deleteContext("get_" + attributeList.get(i - 1) + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
					}
					dialogFlowUtil.setContext("get_" + attributeList.get(i), 5);
					String question = "Which column values should be mapped to " + paramDisplayNameMap.getOrDefault(attributeList.get(i), attributeList.get(i)) + "?";
					if (paramDisplayMessageMap.containsKey(attributeList.get(i))) {
						question = paramDisplayMessageMap.get(attributeList.get(i));
					}
					textMsg = new StringBuilder(question);
					break;
				}
			}
			paramType = attributeType.isEmpty() ?
					columnMap.get(paramMap.get(attributeName).toString()) :
					attributeType;
			options = getFilteredInstances(attributeName, paramType.toLowerCase(), paramMap.get(attributeName).toString(), !attributeType.isEmpty(), paramMap);
			if (createResponseForUser(options, i, attributeName, attributeType, paramMap, paramType)) {
				break;
			}
			processedParameterCount++;
		}
		if (processedParameterCount == attributeList.size()) {
			allParamsProcessed = true;
		}
		return options;
	}

	/**
	 * Validate the value for attribute and its type. Return true if response is ready
	 *
	 * @param options       - options available after filtering
	 * @param i             - priority of the attribute
	 * @param attributeName - name of the attribute
	 * @param attributeType - type of the attribute
	 * @param paramMap      - parameter map from dialogflow
	 * @return - true if response is ready and false otherwise
	 */
	private boolean createResponseForUser(Set<String> options, int i, String attributeName, String attributeType, Map<String, Object> paramMap, String paramType) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		String columnName = paramMap.get(attributeName).toString();
		if (options.size() == 0 && attributeType.isEmpty()) {
			dialogFlowUtil.deleteContext("get_" + attributeList.get(i) + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
			dialogFlowUtil.setContext("get_" + attributeList.get(i), 5);
			textMsg = new StringBuilder(columnName + " cannot be used as " + paramDisplayNameMap.getOrDefault(attributeList.get(i), attributeList.get(i)) + ". Please provide a different column?");
			return true;
		} else if (options.size() == 0) {
			dialogFlowUtil.deleteContext("get_" + attributeList.get(i + 1));
			dialogFlowUtil.setContext("get_" + attributeList.get(i) + IDAConst.ATTRIBUTE_TYPE_SUFFIX, 5);
			textMsg = new StringBuilder(columnName + " cannot be used as " + attributeType + ". Please provide correct type.");
			return true;
		} else if (options.size() > 1 && attributeType.isEmpty()) {
			dialogFlowUtil.deleteContext("get_" + attributeList.get(i + 1));
			dialogFlowUtil.setContext("get_" + attributeList.get(i) + IDAConst.ATTRIBUTE_TYPE_SUFFIX, 5);
			textMsg = new StringBuilder(columnName + " can be used as:<br>");
			textMsg.append("<ul>");
			for (String t : options) {
				textMsg.append("<li><b>").append(t).append("</b> - ");
				if (IDAConst.INSTANCE_PARAM_TYPE_BINS.equalsIgnoreCase(t)) {
					textMsg.append(IDAConst.PARAM_TYPE_EG_MAP.get(paramType));
				} else if (IDAConst.TRANSFORMATION_TYPES.contains(t)) {
					textMsg.append(IDAConst.TRANSFORMATION_EG_MAP.get(t));
				} else {
					textMsg.append(IDAConst.PARAM_TYPE_NON_BIN);
				}
				textMsg.append("</li>");
			}
			textMsg.append("</ul><br/>");
			textMsg.append("\n Which option do you need (");
			textMsg.append(String.join(" / ", options));
			textMsg.append(")?");
			return true;
		}
		if (i == 1 && !attributeType.isEmpty() && IDAConst.INSTANCE_PARAM_TYPE_BINS.equals(attributeType.toLowerCase())) {
			Value paramVal = (Value) paramMap.get(IDAConst.PARAMETER_TYPE_BIN_SIZE);
			if ("date".equals(columnMap.get(paramMap.get(attributeName).toString()))) {
				if (paramVal == null || !paramVal.hasStructValue()) {
					dialogFlowUtil.deleteContext("get_" + attributeList.get(i + 1));
					dialogFlowUtil.setContext(IDAConst.CONTEXT_GET_BIN_DURATION, 5);
					textMsg = new StringBuilder("What should be the duration of each bin?<br/>Eg: 1 week, 2 weeks, 3 months");
					return true;
				}
			} else {
				if (paramVal == null) {
					dialogFlowUtil.deleteContext("get_" + attributeList.get(i + 1));
					dialogFlowUtil.setContext(IDAConst.CONTEXT_GET_BIN_SIZE, 5);
					textMsg = new StringBuilder("What should be the size of each bin?<br/>Eg: 10, 25, 15, twenty, twelve");
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Method to filter the possible instances for the visualization based on the parameter and its type
	 *
	 * @param attribute      - parameter label
	 * @param attributeType  - parameter type ( bin, numeric, date etc.,)
	 * @param columnName     - name of the column to be mapped to the parameter
	 * @param isTypeFromUser - Was the type selected by user or fetched from metadata
	 * @return - list of options for the user to choose from
	 */
	private Set<String> getFilteredInstances(String attribute, String attributeType, String columnName, boolean isTypeFromUser, Map<String, Object> paramMap) throws IOException {
		Map<String, Map<String, Map<String, String>>> filteredInstances = new HashMap<>();
		String instanceParamType;
		String instanceParamTransType;
		Set<String> options = new HashSet<>();
		boolean areValuesUnique;
		for (String instance : instanceMap.keySet()) {
			for (String param : instanceMap.get(instance).keySet()) {
				instanceParamType = instanceMap.get(instance).get(param).get(IDAConst.INSTANCE_PARAM_TYPE_KEY).toLowerCase();
				instanceParamTransType = instanceMap.get(instance).get(param).get(IDAConst.INSTANCE_PARAM_TRANS_TYPE_KEY).toLowerCase();
				if (attribute.equals(param) &&
						(
								attributeType.equals(instanceParamType) ||
										attributeType.equals(instanceParamTransType) ||
										(!isTypeFromUser && IDAConst.INSTANCE_PARAM_TYPE_NOT_REQUIRED.equals(instanceParamType)) ||
										(IDAConst.PARAM_TYPE_TREE.get(attributeType) != null && IDAConst.PARAM_TYPE_TREE.get(attributeType).contains(instanceParamType))
						)) {
					if (!instanceMap.get(instance).get(param).get(IDAConst.INSTANCE_PARAM_DEPENDENT_KEY).isEmpty()) {
						areValuesUnique = areCompositeColumnsUnique(columnName, instanceMap.get(instance).get(param).get(IDAConst.INSTANCE_PARAM_DEPENDENT_KEY), paramMap);
					} else {
						areValuesUnique = Boolean.parseBoolean(columnUniquenessMap.get(columnName));
					}
					if ((IDAConst.INSTANCE_PARAM_TYPE_UNIQUE.equals(instanceParamType) && !areValuesUnique) ||
							(IDAConst.INSTANCE_PARAM_TYPE_NON_UNIQUE.equals(instanceParamType) && areValuesUnique)) {
						break;
					}
					filteredInstances.put(instance, instanceMap.get(instance));
				}
			}
		}
		instanceMap = filteredInstances;
		for (String instance : instanceMap.keySet()) {
			for (String attr : instanceMap.get(instance).keySet()) {
				if (attr.equals(attribute)) {
					options.add(instanceMap.get(instance).get(attribute).get(IDAConst.INSTANCE_PARAM_TRANS_TYPE_KEY));
				}
			}
		}
		return options;
	}

	/**
	 * Method to get the list of column names required for rendering the visualization from the values received from Dialogflow
	 *
	 * @param attributeList - list of attributes from the RDF model
	 * @param paramMap      - map of parameter and its value from dialogflow
	 * @return - list of column names
	 */
	private List<String> getColumnNames(Map<Integer, String> attributeList, Map<String, Object> paramMap) {
		List<String> columnList = new ArrayList<>();
		for (String param : attributeList.values()) {
			if (!paramMap.getOrDefault(param, "").toString().isEmpty()) {
				columnList.add(paramMap.get(param).toString());
			}
		}
		return columnList;
	}

	/**
	 * Method to initialize the columns for all parameters and values for the parameter types
	 *
	 * @param paramMap - map of parameter and its value from dialogflow
	 */
	private void getParameters(Map<String, Object> paramMap) {
		parameterMap = new HashMap<>();
		parameterTypeMap = new HashMap<>();
		String attributeName;
		Map<String, Map<String, String>> instance = new HashMap<>();
		for (String instanceKey : instanceMap.keySet()) {
			instance = instanceMap.get(instanceKey);
		}
		for (int i = 1; i <= attributeList.size(); i++) {
			attributeName = attributeList.get(i);
			parameterMap.put(attributeName, paramMap.getOrDefault(attributeName, "").toString());
			for (String attr : instance.keySet()) {
				if (attributeName.equals(attr)) {
					parameterTypeMap.put(attributeName + IDAConst.ATTRIBUTE_TYPE_SUFFIX, instance.get(attr).get(IDAConst.INSTANCE_PARAM_TRANS_TYPE_KEY).toLowerCase());
				}
			}
		}
	}

	/**
	 * Method to create data for the visualization based on parameters (for now, this works only for bar graph and bubble chart)
	 *
	 * @param param1   - first param for the visualization
	 * @param param2   - second param for the visualization
	 * @param paramMap - map of parameter and its value from dialogflow
	 */
	private void createGraphData(String param1, String param2, Map<String, Object> paramMap) {
		String xAxisColumn = parameterMap.get(param1);
		String yAxisColumn = parameterMap.get(param2);
		String xAxisColumnType = parameterTypeMap.get(param1 + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
		String yAxisColumnType = parameterTypeMap.get(param2 + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
		graphItems = new HashMap<>();
		groupedGraphItems = new HashMap<>();
		boolean groupingNeeded = Boolean.parseBoolean(paramMap.getOrDefault("Group_Column_choice", "").toString());
		String xValue;
		double yValue;
		Map<String, Integer> labelCounts = new HashMap<>();
		if (IDAConst.INSTANCE_PARAM_TYPE_UNIQUE.equals(xAxisColumnType)) {
			for (Map<String, String> entry : tableData) {
				xValue = entry.get(xAxisColumn);
				try {
					yValue = Double.parseDouble(entry.get(yAxisColumn));
				} catch (Exception ex) {
					yValue = 0.0;
				}
				graphItems.put(xValue, yValue);
			}
		} else if (IDAConst.INSTANCE_PARAM_TYPE_NON_UNIQUE.equals(xAxisColumnType)) {
			if (groupingNeeded) {
				String groupColumn = paramMap.get("Group_Column").toString();
				List<String> groups = tableData.stream().map(e -> e.get(groupColumn)).distinct().collect(Collectors.toList());
				List<String> labels = tableData.stream().map(e -> e.get(xAxisColumn)).distinct().collect(Collectors.toList());
				Map<String, Double> groupEntries = new HashMap<>();
				Map<String, Map<String, Integer>> groupedLabelCounts = new HashMap<>();
				for (String group : groups) {
					labelCounts.put(group, 0);
					groupEntries.put(group, 0.0);
				}
				for (String label : labels) {
					groupedGraphItems.put(label, new HashMap<>() {{
						putAll(groupEntries);
					}});
					groupedLabelCounts.put(label, new HashMap<>() {{
						putAll(labelCounts);
					}});
				}
				for (Map<String, String> entry : tableData) {
					xValue = entry.get(groupColumn);
					updateGraphItemList(xValue, entry.get(yAxisColumn), yAxisColumnType, groupedLabelCounts.get(entry.get(xAxisColumn)), groupedGraphItems.get(entry.get(xAxisColumn)));
				}
				if (IDAConst.TRANSFORMATION_TYPE_AVG.equals(yAxisColumnType)) {
					for (String group : groupedGraphItems.keySet()) {
						Map<String, Double> entries = groupedGraphItems.get(group);
						Map<String, Integer> lbls = groupedLabelCounts.get(group);
						entries.replaceAll((l, v) -> Double.isNaN(entries.get(l) / lbls.get(l)) ? 0.0 : entries.get(l) / lbls.get(l));
						groupedGraphItems.put(group, entries);
					}
					graphItems.replaceAll((l, v) -> graphItems.get(l) / labelCounts.get(l));
				}
			}
			for (Map<String, String> entry : tableData) {
				xValue = entry.get(xAxisColumn);
				updateGraphItemList(xValue, entry.get(yAxisColumn), yAxisColumnType, labelCounts, graphItems);
			}
			if (IDAConst.TRANSFORMATION_TYPE_AVG.equals(yAxisColumnType)) {
				graphItems.replaceAll((l, v) -> graphItems.get(l) / labelCounts.get(l));
			}
		} else {
			Value paramVal = (Value) paramMap.get(IDAConst.PARAMETER_TYPE_BIN_SIZE);
			int binSize;
			String binType;
			if (IDAConst.COLUMN_TYPE_NUMERIC.equals(columnMap.get(xAxisColumn))) {
				if (groupingNeeded) {
					processGroupedBinsForNumericLabels((int) Math.abs(paramVal.getNumberValue()), xAxisColumn, yAxisColumn, yAxisColumnType, paramMap.get("Group_Column").toString());
				}
				processBinsForNumericLabels((int) Math.abs(paramVal.getNumberValue()), xAxisColumn, yAxisColumn, yAxisColumnType, labelCounts);
			} else if (IDAConst.COLUMN_TYPE_DATE.equals(columnMap.get(xAxisColumn))) {
				binSize = (int) Math.abs(paramVal.getStructValue().getFieldsMap().get(IDAConst.PARAMETER_TYPE_DURATION_SIZE).getNumberValue());
				binType = paramVal.getStructValue().getFieldsMap().get(IDAConst.PARAMETER_TYPE_DURATION_UNIT).getStringValue();
				if (groupingNeeded) {
					processGroupedBinsForDateLabels(binSize, binType, xAxisColumn, yAxisColumn, yAxisColumnType, paramMap.get("Group_Column").toString());
				}
				processBinsForDateLabels(binSize, binType, xAxisColumn, yAxisColumn, yAxisColumnType, labelCounts);
			}
		}
		if (IDAConst.COLUMN_TYPE_NUMERIC.equals(columnMap.get(xAxisColumn))) {
			comparator = IDAConst.INSTANCE_PARAM_TYPE_BINS.equals(xAxisColumnType) ? LableComparator.getForKey(IDAConst.COMPARATOR_TYPE_DOUBLE_BIN) : LableComparator.getForKey(IDAConst.COMPARATOR_TYPE_DOUBLE);
		} else if (IDAConst.COLUMN_TYPE_DATE.equals(columnMap.get(xAxisColumn))) {
			comparator = IDAConst.INSTANCE_PARAM_TYPE_BINS.equals(xAxisColumnType) ? LableComparator.getForKey(IDAConst.COMPARATOR_TYPE_DATE_BIN) : LableComparator.getForKey(IDAConst.COMPARATOR_TYPE_DATE);
		}
	}

	/**
	 * Method to process the bins for numeric labels
	 *
	 * @param binSize         - size of the bins
	 * @param xAxisColumn     - column for primary parameter
	 * @param yAxisColumn     - column for secondary parameter
	 * @param yAxisColumnType - type of secondary parameter
	 * @param labelCounts     - map of labels to its counts
	 */
	private void processBinsForNumericLabels(int binSize, String xAxisColumn, String yAxisColumn, String yAxisColumnType, Map<String, Integer> labelCounts) {
		String xValue;
		List<Double> values = tableData.stream().map(e -> {
			try {
				return Double.parseDouble(e.get(xAxisColumn));
			} catch (NumberFormatException ex) {
				return 0.0;
			}
		}).sorted().collect(Collectors.toList());
		double min = values.get(0);
		double max = values.get(values.size() - 1);
		double binVal;
		double intervalBegin;
		for (double i = min; i < max; i += binSize) {
			graphItems.put(i + " - " + (i + binSize - 1), 0.0);
			labelCounts.put(i + " - " + (i + binSize - 1), 1);
		}
		for (Map<String, String> entry : tableData) {
			String valueString = entry.get(xAxisColumn);
			if (TextUtil.isDoubleString(valueString)) {
				binVal = Double.parseDouble(valueString);
				intervalBegin = binVal - (binVal % binSize);
				xValue = intervalBegin + " - " + (intervalBegin + binSize - 1);
				updateGraphItemList(xValue, entry.get(yAxisColumn), yAxisColumnType, labelCounts, graphItems);
			} else if (valueString.equalsIgnoreCase(IDAConst.NULL_VALUE_IDENTIFIER)) {
				updateGraphItemList(valueString, entry.get(yAxisColumn), yAxisColumnType, labelCounts, graphItems);
			}
		}
		if (IDAConst.TRANSFORMATION_TYPE_AVG.equals(yAxisColumnType)) {
			graphItems.replaceAll((l, v) -> graphItems.get(l) / labelCounts.get(l));
		}
	}

	/**
	 * Method to process the bins for numeric labels along with grouping
	 *
	 * @param binSize         - size of the bins
	 * @param xAxisColumn     - column for primary parameter
	 * @param yAxisColumn     - column for secondary parameter
	 * @param yAxisColumnType - type of secondary parameter
	 * @param groupColumn     - column for grouping the labels
	 */
	private void processGroupedBinsForNumericLabels(int binSize, String xAxisColumn, String yAxisColumn, String yAxisColumnType, String groupColumn) {
		String xValue;
		List<Double> values = tableData.stream().map(e -> {
			try {
				return Double.parseDouble(e.get(xAxisColumn));
			} catch (NumberFormatException ex) {
				return 0.0;
			}
		}).sorted().collect(Collectors.toList());
		List<String> groups = tableData.stream().map(e -> e.get(groupColumn)).distinct().collect(Collectors.toList());
		double min = values.get(0);
		double max = values.get(values.size() - 1);
		double binVal;
		double intervalBegin;
		Map<String, Double> groupEntries = new HashMap<>();
		Map<String, Map<String, Integer>> groupedLabelCounts = new HashMap<>();
		Map<String, Integer> labelCounts = new HashMap<>();
		for (String group : groups) {
			groupEntries.put(group, 0.0);
			labelCounts.put(group, 1);
		}
		for (double i = min; i < max; i += binSize) {
			groupedGraphItems.put(i + " - " + (i + binSize - 1), new HashMap<>() {{
				putAll(groupEntries);
			}});
			groupedLabelCounts.put(i + " - " + (i + binSize - 1), new HashMap<>() {{
				putAll(labelCounts);
			}});
		}
		if ((int) tableData.stream().filter(e -> IDAConst.NULL_VALUE_IDENTIFIER.equalsIgnoreCase(e.get(xAxisColumn))).count() > 0) {
			groupedGraphItems.put(IDAConst.NULL_VALUE_IDENTIFIER, groupEntries);
			groupedLabelCounts.put(IDAConst.NULL_VALUE_IDENTIFIER, labelCounts);
		}
		for (Map<String, String> entry : tableData) {
			String valueString = entry.get(xAxisColumn);
			if (TextUtil.isDoubleString(valueString)) {
				binVal = Double.parseDouble(valueString);
				intervalBegin = binVal - (binVal % binSize);
				xValue = intervalBegin + " - " + (intervalBegin + binSize - 1);
				updateGraphItemList(entry.get(groupColumn), entry.get(yAxisColumn), yAxisColumnType, groupedLabelCounts.get(xValue), groupedGraphItems.get(xValue));
			} else if (valueString.equalsIgnoreCase(IDAConst.NULL_VALUE_IDENTIFIER)) {
				updateGraphItemList(entry.get(groupColumn), entry.get(yAxisColumn), yAxisColumnType, groupedLabelCounts.get(valueString), groupedGraphItems.get(valueString));
			}
		}
		if (IDAConst.TRANSFORMATION_TYPE_AVG.equals(yAxisColumnType)) {
			for (String group : groupedGraphItems.keySet()) {
				Map<String, Double> entries = groupedGraphItems.get(group);
				Map<String, Integer> labels = groupedLabelCounts.get(group);
				entries.replaceAll((l, v) -> entries.get(l) / labels.get(l));
				groupedGraphItems.put(group, entries);
			}
		}
	}

	/**
	 * Method to process the bins for date labels
	 *
	 * @param binSize         - size of the bins
	 * @param binType         - type of duration (days, weeks, months or years)
	 * @param xAxisColumn     - column for labels
	 * @param yAxisColumn     - column for values
	 * @param yAxisColumnType - type of value column
	 * @param labelCounts     - map of labels to its counts
	 */
	private void processBinsForDateLabels(int binSize, String binType, String xAxisColumn, String yAxisColumn, String yAxisColumnType, Map<String, Integer> labelCounts) {
		String xValue;
		Calendar calendar = Calendar.getInstance();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(IDAConst.LABEL_PATTERN_DATE);
		if (IDAConst.DURATION_TYPE_MONTH.equals(binType)) {
			formatter = DateTimeFormatter.ofPattern(IDAConst.LABEL_PATTERN_MONTH);
		} else if (IDAConst.DURATION_TYPE_YEAR.equals(binType)) {
			formatter = DateTimeFormatter.ofPattern(IDAConst.LABEL_PATTERN_YEAR);
		}

		labelCounts.putAll(initializeGraphItemsForDateBins(binSize, binType, xAxisColumn, calendar));
		Date min = calendar.getTime();
		LocalDate localMin = min.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		for (Map<String, String> entry : tableData) {
			String valueString = entry.get(xAxisColumn);
			try {
				calendar.setTime(DateUtils.parseDate(valueString, IDAConst.DATE_PATTERNS));
				xValue = getBinLabelFromDate(binType, calendar, binSize, localMin, formatter);
				updateGraphItemList(xValue, entry.get(yAxisColumn), yAxisColumnType, labelCounts, graphItems);
			} catch (ParseException | NullPointerException ex) {
				if (valueString.equalsIgnoreCase(IDAConst.NULL_VALUE_IDENTIFIER))
					updateGraphItemList(valueString, entry.get(yAxisColumn), yAxisColumnType, labelCounts, graphItems);
			}
		}
		if (IDAConst.TRANSFORMATION_TYPE_AVG.equals(yAxisColumnType)) {
			graphItems.replaceAll((l, v) -> graphItems.get(l) / labelCounts.get(l));
		}
	}

	/**
	 * Method to process the bins for date labels along with grouping
	 *
	 * @param binSize         - size of the bins
	 * @param binType         - type of duration (days, weeks, months or years)
	 * @param xAxisColumn     - column for labels
	 * @param yAxisColumn     - column for values
	 * @param yAxisColumnType - type of value column
	 * @param groupColumn     - column for grouping the labels
	 */
	private void processGroupedBinsForDateLabels(int binSize, String binType, String xAxisColumn, String yAxisColumn, String yAxisColumnType, String groupColumn) {
		String xValue;
		Calendar calendar = Calendar.getInstance();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(IDAConst.LABEL_PATTERN_DATE);
		if (IDAConst.DURATION_TYPE_MONTH.equals(binType)) {
			formatter = DateTimeFormatter.ofPattern(IDAConst.LABEL_PATTERN_MONTH);
		} else if (IDAConst.DURATION_TYPE_YEAR.equals(binType)) {
			formatter = DateTimeFormatter.ofPattern(IDAConst.LABEL_PATTERN_YEAR);
		}
		List<String> groups = tableData.stream().map(e -> e.get(groupColumn)).distinct().collect(Collectors.toList());
		Map<String, Map<String, Integer>> groupedLabelCounts = new HashMap<>();
		Map<String, Integer> labelCounts = new HashMap<>(initializeGraphItemsForDateBins(binSize, binType, xAxisColumn, calendar));
		Map<String, Double> groupEntries = new HashMap<>();
		Map<String, Integer> lblCounts = new HashMap<>();
		for (String group : groups) {
			groupEntries.put(group, 0.0);
			lblCounts.put(group, 0);
		}
		for (String label : labelCounts.keySet()) {
			groupedGraphItems.put(label, new HashMap<>() {{
				putAll(groupEntries);
			}});
			groupedLabelCounts.put(label, new HashMap<>() {{
				putAll(lblCounts);
			}});
		}
		Date min = calendar.getTime();
		LocalDate localMin = min.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		for (Map<String, String> entry : tableData) {
			String valueString = entry.get(xAxisColumn);
			try {
				calendar.setTime(DateUtils.parseDate(valueString, IDAConst.DATE_PATTERNS));
				xValue = getBinLabelFromDate(binType, calendar, binSize, localMin, formatter);
				updateGraphItemList(entry.get(groupColumn), entry.get(yAxisColumn), yAxisColumnType, groupedLabelCounts.get(xValue), groupedGraphItems.get(xValue));
			} catch (ParseException | NullPointerException ex) {
				if (valueString.equalsIgnoreCase(IDAConst.NULL_VALUE_IDENTIFIER))
					updateGraphItemList(entry.get(groupColumn), entry.get(yAxisColumn), yAxisColumnType, groupedLabelCounts.get(valueString), groupedGraphItems.get(valueString));
			}
		}
		if (IDAConst.TRANSFORMATION_TYPE_AVG.equals(yAxisColumnType)) {
			for (String group : groupedGraphItems.keySet()) {
				Map<String, Double> entries = groupedGraphItems.get(group);
				Map<String, Integer> labels = groupedLabelCounts.get(group);
				entries.replaceAll((l, v) -> entries.get(l) / labels.get(l));
				groupedGraphItems.put(group, entries);
			}
		}
	}

	/**
	 * Method to initialize the graph items for all bins
	 *
	 * @param binSize     - size of each bin
	 * @param binType     - type of bin (Eg: days, weeks, months)
	 * @param xAxisColumn - column mapped to labels
	 * @param calendar    - calender instance passed as reference for future use from the callee
	 * @return - map of bin labels and their counts
	 */
	private Map<String, Integer> initializeGraphItemsForDateBins(int binSize, String binType, String xAxisColumn, Calendar calendar) {
		List<Date> values = tableData.stream().map(e -> e.get(xAxisColumn))
				.sorted(LableComparator.getForKey(IDAConst.COMPARATOR_TYPE_DATE_BIN))
				.map(e -> {
					try {
						return DateUtils.parseDate(e, IDAConst.DATE_PATTERNS);
					} catch (ParseException ex) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		Date min = values.get(0);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(IDAConst.LABEL_PATTERN_DATE);
		calendar.setTime(min);
		if (IDAConst.DURATION_TYPE_MONTH.equals(binType)) {
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			formatter = DateTimeFormatter.ofPattern(IDAConst.LABEL_PATTERN_MONTH);
		} else if (IDAConst.DURATION_TYPE_YEAR.equals(binType)) {
			calendar.set(Calendar.DAY_OF_YEAR, 1);
			formatter = DateTimeFormatter.ofPattern(IDAConst.LABEL_PATTERN_YEAR);
		} else {
			calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
		}
		min = calendar.getTime();
		LocalDate localMin = min.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate max = values.get(values.size() - 1).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();


		Map<String, Integer> labelCounts = new HashMap<>();
		String label;
		LocalDate nextDate = localMin;
		while (nextDate.isBefore(max)) {
			switch (binType) {
				case IDAConst.DURATION_TYPE_WEEK:
					label = nextDate.format(formatter) + " to " + nextDate.plusWeeks(binSize).minusDays(1).format(formatter);
					nextDate = nextDate.plusWeeks(binSize);
					break;
				case IDAConst.DURATION_TYPE_MONTH:
					label = binSize == 1 ? nextDate.format(formatter) : nextDate.format(formatter) + " to " + nextDate.plusMonths(binSize).minusDays(1).format(formatter);
					nextDate = nextDate.plusMonths(binSize);
					break;
				case IDAConst.DURATION_TYPE_YEAR:
					label = binSize == 1 ? nextDate.format(formatter) : nextDate.format(formatter) + " to " + nextDate.plusYears(binSize).minusDays(1).format(formatter);
					nextDate = nextDate.plusYears(binSize);
					break;
				default:
					label = nextDate.format(formatter) + " to " + nextDate.plusDays(binSize - 1).format(formatter);
					nextDate = nextDate.plusDays(binSize);
					break;
			}
			graphItems.put(label, 0.0);
			labelCounts.put(label, 1);
		}
		return labelCounts;
	}

	/**
	 * Method to get bin label based on the date string, bin type and bin size
	 *
	 * @param binType   - type of the bin
	 * @param calendar  - calendar set from date string
	 * @param binSize   - size of the bin
	 * @param localMin  - min date from the table data
	 * @param formatter - date string formatter
	 * @return - bin label for the given date
	 */
	private String getBinLabelFromDate(String binType, Calendar calendar, int binSize, LocalDate localMin, DateTimeFormatter formatter) {
		String xValue;
		LocalDate binVal;
		LocalDate intervalBegin;
		long diff;
		switch (binType) {
			case IDAConst.DURATION_TYPE_WEEK:
				calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
				binVal = calendar.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				diff = ChronoUnit.WEEKS.between(localMin, binVal);
				intervalBegin = localMin.plusWeeks((diff / binSize) * binSize);
				xValue = intervalBegin.format(formatter) + " to " + intervalBegin.plusWeeks(binSize).minusDays(1).format(formatter);
				break;
			case IDAConst.DURATION_TYPE_MONTH:
				calendar.set(Calendar.DAY_OF_MONTH, 1);
				binVal = calendar.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				diff = ChronoUnit.MONTHS.between(localMin, binVal);
				intervalBegin = localMin.plusMonths((diff / binSize) * binSize);
				xValue = binSize == 1 ? intervalBegin.format(formatter) : intervalBegin.format(formatter) + " to " + intervalBegin.plusMonths(binSize).minusDays(1).format(formatter);
				break;
			case IDAConst.DURATION_TYPE_YEAR:
				calendar.set(Calendar.DAY_OF_YEAR, 1);
				binVal = calendar.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				diff = ChronoUnit.YEARS.between(localMin, binVal);
				intervalBegin = localMin.plusYears((diff / binSize) * binSize);
				xValue = binSize == 1 ? intervalBegin.format(formatter) : intervalBegin.format(formatter) + " to " + intervalBegin.plusYears(binSize).minusDays(1).format(formatter);
				break;
			default:
				binVal = calendar.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				diff = ChronoUnit.DAYS.between(localMin, binVal);
				intervalBegin = localMin.plusDays((diff / binSize) * binSize);
				xValue = intervalBegin.format(formatter) + " to " + intervalBegin.plusDays(binSize - 1).format(formatter);
				break;
		}
		return xValue;
	}

	/**
	 * Method to add a new entry to list of graph items
	 *
	 * @param xValue          - label value of an entry
	 * @param yValueString    - second parameter value in string
	 * @param yAxisColumnType - second parameter type
	 * @param labelCounts     - count of each label (useful for calculating the average)
	 */
	private void updateGraphItemList(String xValue, String yValueString, String yAxisColumnType, Map<String, Integer> labelCounts, Map<String, Double> graphItems) {
		Double yValue;
		try {
			yValue = Double.parseDouble(yValueString);
		} catch (Exception ex) {
			yValue = 0.0;
		}
		if (graphItems.get(xValue) == null) {
			if (IDAConst.TRANSFORMATION_TYPE_COUNT.equals(yAxisColumnType)) {
				graphItems.put(xValue, 1.0);
			} else {
				graphItems.put(xValue, yValue);
			}
			labelCounts.put(xValue, 1);
		} else {
			if (IDAConst.TRANSFORMATION_TYPE_COUNT.equals(yAxisColumnType)) {
				graphItems.put(xValue, graphItems.get(xValue) + 1.0);
			} else {

				graphItems.put(xValue, graphItems.get(xValue) + yValue);
			}
			labelCounts.put(xValue, labelCounts.get(xValue) + 1);
		}
	}

	/**
	 * Method to create a response object based on graph items for bar graph
	 */
	private void createBarGraphResponse() {
		String xAxisColumn = parameterMap.get(IDAConst.X_AXIS_PARAM);
		String yAxisColumn = parameterMap.get(IDAConst.Y_AXIS_PARAM);
		String xAxisColumnType = parameterTypeMap.get(IDAConst.X_AXIS_PARAM + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
		String yAxisColumnType = parameterTypeMap.get(IDAConst.Y_AXIS_PARAM + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
		String yAxisLabel = IDAConst.INSTANCE_PARAM_TYPE_UNIQUE.equals(xAxisColumnType) ? yAxisColumn : yAxisColumnType + " " + yAxisColumn;
		String graphLabel = "Bar graph for " + xAxisColumn + " and " + yAxisLabel;
		List<BarGraphItem> barGraphItemList = new ArrayList<>();
		for (String label : graphItems.keySet().stream().sorted(comparator).collect(Collectors.toList())) {
			barGraphItemList.add(new BarGraphItem(label, graphItems.get(label)));
		}
		payload.put("barGraphData", new BarGraphData(graphLabel, barGraphItemList, xAxisColumn, yAxisLabel));
	}

	/**
	 * Method to create a response object based on graph items for grouped bar graph
	 */
	private void createGroupedBarGraphResponse() {
		String xAxisColumn = parameterMap.get("Group_Column");
		String yAxisColumn = parameterMap.get(IDAConst.Y_AXIS_PARAM);
		String xAxisColumnType = parameterTypeMap.get(IDAConst.X_AXIS_PARAM + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
		String yAxisColumnType = parameterTypeMap.get(IDAConst.Y_AXIS_PARAM + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
		String yAxisLabel = IDAConst.INSTANCE_PARAM_TYPE_UNIQUE.equals(xAxisColumnType) ? yAxisColumn : yAxisColumnType + " " + yAxisColumn;
		String graphLabel = "Bar graph for " + xAxisColumn + " and " + yAxisLabel;
		List<BarGraphItem> barGraphItemList;
		Map<String, List<BarGraphItem>> groupedBarChartData = new TreeMap<>();
		for (String groupLabel : groupedGraphItems.keySet().stream().sorted(comparator).collect(Collectors.toList())) {
			barGraphItemList = new ArrayList<>();
			for (String label : groupedGraphItems.get(groupLabel).keySet()) {
				barGraphItemList.add(new BarGraphItem(label, groupedGraphItems.get(groupLabel).get(label)));
			}
			groupedBarChartData.put(groupLabel, barGraphItemList);
		}
		List<String> xAxisLabels = groupedGraphItems.get(groupedGraphItems.keySet().iterator().next()).keySet().stream().sorted(comparator).collect(Collectors.toList());
		payload.put("barGraphData", new GroupedBarGraphData(graphLabel, xAxisColumn, yAxisLabel, xAxisLabels, groupedBarChartData));
	}

	/**
	 * Method to create a response object based on graph items for bubble graph
	 *
	 * @param dsName    - name of the dataset
	 * @param tableName - name of the table
	 */
	private void createBubbleChartResponse(String dsName, String tableName) {
		String labelColumn = parameterMap.get(IDAConst.BUBBLE_LABEL_PARAM);
		String sizeColumn = parameterMap.get(IDAConst.BUBBLE_SIZE_PARAM);
		String labelColumnType = parameterTypeMap.get(IDAConst.BUBBLE_LABEL_PARAM + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
		String sizeColumnType = parameterTypeMap.get(IDAConst.BUBBLE_SIZE_PARAM + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
		String sizeLabel = IDAConst.INSTANCE_PARAM_TYPE_UNIQUE.equals(labelColumnType) ? sizeColumn : sizeColumnType + " " + sizeColumn;
		String graphLabel = "Bubble chart for " + labelColumn + " and " + sizeLabel;
		List<BubbleChartItem> bubbleChartItemList = new ArrayList<>();
		for (String label : graphItems.keySet().stream().sorted(comparator).collect(Collectors.toList())) {
			bubbleChartItemList.add(new BubbleChartItem(label, label, graphItems.get(label)));
		}
		payload.put("bubbleChartData", new BubbleChartData(graphLabel, bubbleChartItemList, dsName, tableName));
	}

	/**
	 * Method to create a response object based on graph items for grouped bubble chart
	 */
	private void createGroupedBubbleChartResponse() {
		String labelColumn = parameterMap.get("Group_Column");
		String sizeColumn = parameterMap.get(IDAConst.BUBBLE_SIZE_PARAM);
		String labelColumnType = parameterTypeMap.get(IDAConst.BUBBLE_LABEL_PARAM + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
		String sizeColumnType = parameterTypeMap.get(IDAConst.BUBBLE_SIZE_PARAM + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
		String sizeLabel = IDAConst.INSTANCE_PARAM_TYPE_UNIQUE.equals(labelColumnType) ? sizeColumn : sizeColumnType + " " + sizeColumn;
		String graphLabel = "Bubble chart for " + labelColumn + " and " + sizeLabel;
		List<BubbleChartItem> bubbleChartItemList;
		Map<String, List<BubbleChartItem>> groupedBubbleChartData = new TreeMap<>();
		for (String groupLabel : groupedGraphItems.keySet().stream().sorted(comparator).collect(Collectors.toList())) {
			bubbleChartItemList = new ArrayList<>();
			for (String label : groupedGraphItems.get(groupLabel).keySet()) {
				bubbleChartItemList.add(new BubbleChartItem(label, label, groupedGraphItems.get(groupLabel).get(label)));
			}
			groupedBubbleChartData.put(groupLabel, bubbleChartItemList);
		}
		List<String> bubbleLabels = groupedGraphItems.get(groupedGraphItems.keySet().iterator().next()).keySet().stream().sorted(comparator).collect(Collectors.toList());
		payload.put("bubbleChartData", new GroupedBubbleChartData(graphLabel, bubbleLabels, groupedBubbleChartData));
	}

	/**
	 * Method to check the uniqueness of values of multiple columns in a table combined.
	 *
	 * @param primaryCol    - main parameter
	 * @param dependentCols - comma separated value od secondary parameters
	 * @param paramMap      - parameter map from the dialogflow
	 * @return boolean value representing the uniqueness
	 * @throws IOException - If the dataset or table name is invalid
	 */
	private boolean areCompositeColumnsUnique(String primaryCol, String dependentCols, Map<String, Object> paramMap) throws IOException {
		boolean areAllUnique = true;
		List<String> columnsLst = new ArrayList<>();
		HashSet<String> combinedValue = new HashSet<>();
		columnsLst.add(primaryCol);
		for (String param : dependentCols.split(",")) {
			if (paramMap.get(param) != null && !paramMap.get(param).toString().isEmpty()) {
				columnsLst.add(paramMap.get(param).toString());
			}
		}
		for (String col : columnsLst) {
			if (!Boolean.parseBoolean(columnUniquenessMap.get(col))) {
				areAllUnique = false;
			}
		}
		if (areAllUnique) {
			return areAllUnique;
		}
		List<Map<String, String>> data = dataUtil.getData(payload.get("activeDS").toString(), payload.get("activeTable").toString(), columnsLst, columnMap);
		List<String> rowVal;
		for (Map<String, String> row : data) {
			rowVal = new ArrayList<>();
			for (String col : columnsLst) {
				rowVal.add(row.get(col));
			}
			if (combinedValue.contains(String.join(" | ", rowVal))) {
				return false;
			} else {
				combinedValue.add(String.join(" | ", rowVal));
			}
		}
		return true;
	}

	/**
	 * Method to create line chart data based on user options.
	 *
	 * @param dateColumn  - column to be mapped to X-Axis
	 * @param labelColumn - column to be mapped to line labels
	 * @param valueColumn - Column to be mapped to Y-Axis
	 * @param valueType   - Type of transformation if the Date & Line labels are repeating
	 * @return - line chart data
	 */
	private Map<String, Map<String, Double>> createLineChartData(String dateColumn, String labelColumn, String valueColumn, String valueType) {
		Map<String, Double> labelData = new TreeMap<>();
		String date;
		String label;
		double value;
		Map<String, Map<String, Double>> chartData = new HashMap<>();
		Calendar calendar = Calendar.getInstance();
		comparator = LableComparator.getForKey(IDAConst.COMPARATOR_TYPE_DATE);
		Map<String, Map<String, Integer>> labelCountsMap = new HashMap<>();
		Map<String, Integer> labelCounts;

		createlineChartXAxisLabels(calendar, dateColumn);
		for (Map<String, String> row : tableData) {
			date = row.get(dateColumn).trim();
			label = row.get(labelColumn);
			value = 0.0;
			try {
				value = labelColumn.equals(valueColumn) ? 1.0 : Double.parseDouble(row.get(valueColumn));
			} catch (NumberFormatException ex) {
				System.out.println(ex.getMessage());
			}
			labelData = new TreeMap<>(comparator);
			labelCounts = new HashMap<>();
			if (chartData.containsKey(label)) {
				labelData = chartData.get(label);
				labelCounts = labelCountsMap.get(label);
			} else {
				for (String l : lineChartXAxisLabels) {
					labelData.put(l, 0.0);
					labelCounts.put(l, 0);
				}
			}
			labelCounts.put(date, labelCounts.get(date) + 1);
			labelCountsMap.put(label, labelCounts);
			updateLineChartData(chartData, labelData, label, date, valueType, value);
		}
		if (IDAConst.TRANSFORMATION_TYPE_AVG.equals(valueType)) {
			updateLinesWithAverage(chartData, labelCountsMap);
		}
		return chartData;
	}

	/**
	 * Method to create the X-Axis labels for the line chart
	 *
	 * @param calendar   - Calendar instance
	 * @param dateColumn - Temporal column
	 */
	private void createlineChartXAxisLabels(Calendar calendar, String dateColumn) {
		lineChartXAxisLabels = new ArrayList<>();
		for (Map<String, String> object : tableData) {
			String currentDate = object.get(dateColumn).trim();
			try {
				calendar.setTime(org.apache.commons.lang3.time.DateUtils.parseDateStrictly(currentDate, IDAConst.DATE_PATTERNS));
			} catch (java.text.ParseException ex) {
				ex.printStackTrace();
				continue; // Ignore the row and continue with the next
			}

			if (!lineChartXAxisLabels.contains(currentDate)) {
				lineChartXAxisLabels.add(currentDate);
			}
		}
		lineChartXAxisLabels.sort(comparator);
	}

	/**
	 * Method to update the line values of the line chart data after processing a row
	 *
	 * @param chartData - line chart data instance
	 * @param labelData - line values for a label (Empty map for newly seen label or existing data for already seen label)
	 * @param label     - label of the line
	 * @param date      - date string for the row
	 * @param valueType - Y-axis value type
	 * @param value     - Y-Axis value of the row
	 */
	private void updateLineChartData(Map<String, Map<String, Double>> chartData, Map<String, Double> labelData, String label, String date, String valueType, double value) {
		double oldValue = labelData.getOrDefault(date, 0.0);
		double newValue;
		switch (valueType) {
			case IDAConst.TRANSFORMATION_TYPE_COUNT:
				newValue = oldValue + 1.0;
				break;
			case IDAConst.TRANSFORMATION_TYPE_AVG:
			case IDAConst.TRANSFORMATION_TYPE_SUM:
				newValue = oldValue + value;
				break;
			default:
				newValue = value;
				break;
		}
		labelData.put(date, newValue);
		chartData.put(label, labelData);
	}

	/**
	 * Method to update the line values with the average.
	 *
	 * @param chartData      - line chart data
	 * @param labelCountsMap - count of values for each labels to calculate the average
	 */
	private void updateLinesWithAverage(Map<String, Map<String, Double>> chartData, Map<String, Map<String, Integer>> labelCountsMap) {
		Map<String, Integer> labelCounts;
		for (String lineLabel : chartData.keySet()) {
			labelCounts = labelCountsMap.get(lineLabel);
			Map<String, Double> labelData = chartData.get(lineLabel);
			for (String dateLabel : labelData.keySet()) {
				labelData.put(dateLabel, labelData.get(dateLabel) / (labelCounts.get(dateLabel) > 0.0 ? labelCounts.get(dateLabel) : 1.0));
			}
			chartData.put(lineLabel, labelData);
		}
	}

	/**
	 * Method to populate the line chart data object in the response.
	 */
	private void createLineChartResponse(Map<String, Object> paramMap) {
		LineChartData lineChartData = new LineChartData();
		getParameters(paramMap);
		String dateColumn = parameterMap.get(IDAConst.LINE_CHART_TEMPORAL_PARAM);
		String labelColumn = parameterMap.get(IDAConst.LINE_CHART_LABLE_PARAM);
		String valueColumn = parameterMap.get(IDAConst.LINE_CHART_VALUE_PARAM);
		String valueType = parameterTypeMap.get(IDAConst.LINE_CHART_VALUE_PARAM + IDAConst.ATTRIBUTE_TYPE_SUFFIX);
		Map<String, Map<String, Double>> chartData = createLineChartData(dateColumn, labelColumn, valueColumn, valueType.toLowerCase());
		lineChartData.setxAxisLabel(dateColumn);
		String yAxisLabel = valueColumn;
		if (labelColumn.equals(valueColumn)) {
			yAxisLabel = IDAConst.COUNT_OF_PREFIX + valueColumn;
		}
		lineChartData.setyAxisLabel(yAxisLabel);
		lineChartData.setChartDesc(IDAConst.LINE_CHART_DESC_PREFIX + yAxisLabel + " across " + dateColumn);
		List<Date> dateLabels = lineChartXAxisLabels.stream().map(l -> {
			try {
				return org.apache.commons.lang3.time.DateUtils.parseDate(l, IDAConst.DATE_PATTERNS);
			} catch (java.text.ParseException e) {
				System.out.println("Date parse exception:" + l);
			}
			return new Date();
		}).collect(Collectors.toList());
		lineChartData.setxAxisLabels(dateLabels);
		List<LineChartItem> lines = new ArrayList<>();
		for (String label : chartData.keySet()) {
			Map<String, Double> labelData = chartData.get(label);
			LineChartItem lineChartItem = new LineChartItem();
			lineChartItem.setLabel(label);
			List<Double> values = new ArrayList<>();
			for (String key : labelData.keySet()) {
				values.add(labelData.get(key));
			}
			lineChartItem.setLineValues(values);
			lines.add(lineChartItem);
		}
		lineChartData.setLines(lines);
		payload.put("lineChartData", lineChartData);
	}

	/**
	 * Method to create scatterplot data from given parameter values.
	 */
	private void createScatterPlotData() {
		String xAxisColumn = parameterMap.get(IDAConst.X_AXIS_PARAM);
		String yAxisColumn = parameterMap.get(IDAConst.Y_AXIS_PARAM);
		String referenceColumn = parameterMap.get(IDAConst.REFERENCE_VALUES_PARAM);
		String labelColumn = parameterMap.get(IDAConst.SCATTER_PLOT_LABEL_PARAM);
		String graphLabel = "Scatter plot for " + xAxisColumn + " and " + yAxisColumn;
		List<ScatterPlotItem> scatterPlotItemList = new ArrayList<>();
		double xValue;
		double yValue;
		String refValue;
		String labelValue;
		for (Map<String, String> entry : tableData) {
			labelValue = entry.get(labelColumn);
			refValue = entry.get(referenceColumn);
			try {
				yValue = Double.parseDouble(entry.get(yAxisColumn));
			} catch (Exception ex) {
				yValue = 0.0;
			}
			try {
				xValue = Double.parseDouble(entry.get(xAxisColumn));
			} catch (Exception ex) {
				xValue = 0.0;
			}
			scatterPlotItemList.add(new ScatterPlotItem(xValue, yValue, refValue, labelValue));
		}
		payload.put("scatterPlotData", new ScatterPlotData(graphLabel, scatterPlotItemList, xAxisColumn, yAxisColumn));
	}

}
