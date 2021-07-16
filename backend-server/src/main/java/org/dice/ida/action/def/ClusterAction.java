package org.dice.ida.action.def;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.Value;
import org.dice.ida.constant.IDAConst;
import org.dice.ida.exception.IDAException;
import org.dice.ida.model.ChatMessageResponse;
import org.dice.ida.model.ChatUserMessage;
import org.dice.ida.model.clustering.FarthestFirstAttribute;
import org.dice.ida.model.clustering.KmeansAttribute;
import org.dice.ida.util.DialogFlowUtil;
import org.dice.ida.util.FileUtil;
import org.dice.ida.util.SessionUtil;
import org.dice.ida.util.ValidatorUtil;
import org.dice.ida.util.SuggestionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import weka.clusterers.EM;
import weka.clusterers.FarthestFirst;
import weka.clusterers.RandomizableClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Class to handle the Clustering implementation
 *
 * @author Sourabh Poddar
 */
@Component
public class ClusterAction implements Action {

	private Map<String, Object> sessionMap;
	private Map<String, Object> payload;
	private FileUtil fileUtil;
	@Autowired
	private DialogFlowUtil dialogFlowUtil;
	private StringBuilder textMsg;
	private Map<String, Object> paramMap;
	private Instances data;
	private String datasetName;
	private String tableName;
	private String clusterMethod;
	private String paramtertoChange;
	private String paramValue = "";
	private int numCluster;
	private Map<String, String> multiParmaValue;
	@Autowired
	private SessionUtil sessionUtil;
	private List<String> columnNames;
	@Autowired
	private SuggestionUtil suggestionUtil;

	/**
	 * @param paramMap            - parameters from dialogflow
	 * @param chatMessageResponse - API response object
	 */
	@Override
	public void performAction(Map<String, Object> paramMap, ChatMessageResponse chatMessageResponse, ChatUserMessage message) throws Exception {
		fileUtil = new FileUtil();
		textMsg = new StringBuilder(paramMap.get(IDAConst.PARAM_TEXT_MSG).toString());
		if (ValidatorUtil.preActionValidation(chatMessageResponse)) {
			payload = chatMessageResponse.getPayload();
			datasetName = payload.get("activeDS").toString();
			tableName = payload.get("activeTable").toString();
			multiParmaValue = null;
			sessionMap = sessionUtil.getSessionMap();
			this.paramMap = paramMap;
			String fullIntentName = paramMap.get(IDAConst.FULL_INTENT_NAME).toString();
			clusterMethod = getClusterMethod(fullIntentName);
			String parameterChangeChoice = getParameterChangeChoice(fullIntentName);
			paramtertoChange = getParameterToChange(fullIntentName);
			if (!clusterMethod.isEmpty() && parameterChangeChoice.isEmpty()) {
				if (clusterMethod.equals("getColumnList")) {
					if (verifynApplyFilter(paramMap.get("column_List"))) {
						textMsg.append("Okay! Here is the list algorithms you can use,<br/><ul>");
						textMsg.append("<li>Kmean</li><li>Farthest First</li></ul>");
						textMsg.append("<br/>Which algorithm would you like to use?");
						if (checkforNominalAttribute())
							textMsg.append("<br/><b>Warning</b> : If too many nominal attributes are selected, clustering might take longer than expected. If its taking longer than <b>" + (IDAConst.TIMEOUT_LIMIT / 60000) + " minutes</b>, the process will be terminated.");
					}
				} else {
					textMsg = new StringBuilder("Okay!! Here is the list of default parameter and our suggested parameters<br/>");
					showParamList();
					textMsg.append("<br/>Would you like to change any parameter?");
					if (checkforNominalAttribute())
						textMsg.append("<br/><b>Warning</b> : If too many nominal attributes are selected, clustering might take longer than expected. If its taking longer than <b>" + (IDAConst.TIMEOUT_LIMIT / 60000) + " minutes</b>, the process will be terminated.");
				}
			} else if (!paramtertoChange.isEmpty()) {
				getnsetNewParamValue();
			}
			if (parameterChangeChoice.equals("no")) {
				loadClusteredData();
				loadScatterPlotMatrixParams();
				chatMessageResponse.setPayload(payload);
				textMsg = new StringBuilder("Your clustered data is loaded. <br/><br/> <ida-btn msg='Draw Scatterplot matrix' vizParams='scatterPlotMatrixParams' value='Click me' style='default'> to render the scatter plot matrix for the clustered data.");
				chatMessageResponse.setUiAction(IDAConst.UIA_CLUSTER);
				dialogFlowUtil.resetContext();
			} else {
				chatMessageResponse.setUiAction(IDAConst.UAC_NRMLMSG);
			}
			chatMessageResponse.setMessage(textMsg.toString());
		}
	}

	/**
	 * Method to load clustered data into the payload
	 */
	private void loadClusteredData() throws Exception {
		RandomizableClusterer model = getClusterModel();
		ObjectMapper mapper = new ObjectMapper();
		List<Map<String, String>> clusteredData = new ArrayList<>();
		model.buildClusterer(data);
		ArrayList<Map> dsData = fileUtil.getDatasetContent(datasetName);
		for (Map file : dsData) {
			if (file.get("name").toString().equalsIgnoreCase(tableName)) {
				ArrayList fileData = (ArrayList) file.get("data");
				for (int i = 0; i < fileData.size(); i++) {
					HashMap<String, String> row = (HashMap<String, String>) fileData.get(i);
					row.put("Cluster", String.valueOf(model.clusterInstance(data.instance(i))));
					clusteredData.add(row);
				}
			}
		}
		payload.put("clusteredData", clusteredData);
		columnNames.add("Cluster");
		payload.put("columns", columnNames);
	}

	/**
	 * Method to populate the parameter values for plotting a scatter-plot matrix for clustered data
	 *
	 * @throws Exception if the dataset name or table name is invalid
	 */
	private void loadScatterPlotMatrixParams() throws Exception {
		Map<String, Map<String, Double>> statProps = suggestionUtil.getStatProps(datasetName, tableName);
		payload.put("scatterPlotMatrixParams", new HashMap<>() {{
			put(IDAConst.PARAM_INTENT_DETECTION_CONFIDENCE, "1.0");
			put(IDAConst.INTENT_NAME, "scatter_plot_matrix");
			put(IDAConst.PARAM_FILTER_STRING, "all");
			put(IDAConst.PARAM_TEXT_MSG, "Scatter plot matrix for the clustered data has been rendered");
			put("isGrouped", "false");
			put("Reference_Values_choice", "true");
			put("Column_List", ((Value) paramMap.get("column_List")).getListValue().getValuesList().stream().map(Value::getStringValue));
			put("Reference_Column", "Cluster");
			put("ScatterPlotMatrix_Label", Collections.min(statProps.get(IDAConst.COLUMN_SD_ALL).entrySet(), Map.Entry.comparingByValue()).getKey());    // Choose the column with least standard deviation for label values
			put("Reference_Column_Choice", "true");
			put("from_suggestion", "true");
			put("All_select", "");
		}});
	}

	/**
	 * Method return weka clusterer object based on user selection
	 *
	 * @return Weka Clusterer object
	 */
	private RandomizableClusterer getClusterModel() throws Exception {
		RandomizableClusterer clusterer;
		switch (clusterMethod) {
			case IDAConst.K_MEAN_CLUSTERING:
				SimpleKMeans simpleKMeans = new SimpleKMeans();
				KmeansAttribute kmeansAttribute = (KmeansAttribute) sessionMap.get(IDAConst.K_MEAN_CLUSTERING);
				simpleKMeans.setNumClusters(kmeansAttribute.getNumberOfCluster());
				simpleKMeans.setNumExecutionSlots(kmeansAttribute.getNumOfExecutionSlots());
				simpleKMeans.setSeed(kmeansAttribute.getRandomNumberSeed());
				simpleKMeans.setMaxIterations(kmeansAttribute.getMaxIterations());
				simpleKMeans.setDontReplaceMissingValues(kmeansAttribute.getReplaceMissingValues());
				simpleKMeans.setInitializationMethod(kmeansAttribute.getInitializeMethod());
				clusterer = simpleKMeans;
				break;
			case IDAConst.FARTHEST_FIRST:
				FarthestFirst farthestFirst = new FarthestFirst();
				FarthestFirstAttribute farthestFirstAttribute = (FarthestFirstAttribute) sessionMap.get(IDAConst.FARTHEST_FIRST);
				farthestFirst.setNumClusters(farthestFirstAttribute.getNumberOfCluster());
				farthestFirst.setSeed(farthestFirstAttribute.getRandomNumberSeed());
				clusterer = farthestFirst;
				break;
			default:
				clusterer = new SimpleKMeans();
				break;
		}
		return clusterer;
	}

	/**
	 * Apply filter to the data instance based on inputs provided by User
	 *
	 * @return true if selected columns are present in the table
	 */
	private boolean verifynApplyFilter(Object column_list) throws Exception {
		ArrayList<String> columnList = new ArrayList<>();
		boolean columnExist = true;
		Value paramVal = (Value) column_list;
		paramVal.getListValue().getValuesList().forEach(str -> columnList.add(str.getStringValue()));
		String path = new FileUtil().fetchSysFilePath(datasetName + "/" + tableName);
		CSVLoader loader = new CSVLoader();
		loader.setSource(getDataReadyForClustering(path));
		data = loader.getDataSet();
		ObjectNode metaData = new FileUtil().getDatasetMetaData(datasetName);
		JsonNode fileDetails = metaData.get(IDAConst.FILE_DETAILS_ATTR);
		if (!columnList.get(0).equalsIgnoreCase("All")) {
			for (int i = 0; i < fileDetails.size(); i++) {
				if (tableName.equals(fileDetails.get(i).get(IDAConst.FILE_NAME_ATTR).asText())) {
					JsonNode columnDetails = fileDetails.get(i).get(IDAConst.COLUMN_DETAILS_ATTR);
					ArrayList<String> columns = new ArrayList<>();
					columnNames = new ArrayList<>();
					for (int j = 0; j < columnDetails.size(); j++) {
						columns.add(columnDetails.get(j).get(IDAConst.COLUMN_NAME_ATTR).asText().toLowerCase());
						columnNames.add(columnDetails.get(j).get(IDAConst.COLUMN_NAME_ATTR).asText());
					}
					for (String column : columnList) {
						if (!columns.contains(column.toLowerCase())) {
							textMsg = new StringBuilder("Sorry, But column \"" + column + "\" doesn't exist in table \"" + tableName + "\"." +
									"Please provide the columns names carefully!! ");
							columnExist = false;
						}
					}
				}
			}
			if (columnExist) {
				int numAttribute = data.numAttributes();
				for (int i = 0; i < numAttribute; i++) {
					String name = data.attribute(i).name();
					if (!(columnList.contains(name) || columnList.contains(name.substring(1)))) {
						data.deleteAttributeAt(i);
						i--;
						numAttribute--;
					}
				}
			}
		} else {
			for (int i = 0; i < fileDetails.size(); i++) {
				if (tableName.equals(fileDetails.get(i).get(IDAConst.FILE_NAME_ATTR).asText())) {
					JsonNode columnDetails = fileDetails.get(i).get(IDAConst.COLUMN_DETAILS_ATTR);
					columnNames = new ArrayList<>();
					for (int j = 0; j < columnDetails.size(); j++) {
						columnNames.add(columnDetails.get(j).get(IDAConst.COLUMN_NAME_ATTR).asText());
					}
				}
			}
		}
		return columnExist;
	}

	/**
	 * Method to check for too many nominal attribute selection.
	 *
	 * @return true if too many nominal attributes are selected
	 */
	private boolean checkforNominalAttribute() {
		boolean check = false;
		int num_nominal = 0;
		for (int i = 0; i < data.numAttributes(); i++) {
			if (data.attribute(i).isString() || data.attribute(i).isNominal()) {
				num_nominal++;
			}
		}
		if (((double) num_nominal / (double) data.numAttributes() > .7))
			check = true;
		return check;
	}

	/**
	 * Method to filter and convert string attributes to nominal.
	 */
	private void filterStringAttribyte() throws Exception {
		StringToNominal filter = new StringToNominal();
		StringBuilder columnsRange = new StringBuilder();
		for (int i = 0; i < data.numAttributes(); i++) {
			if (data.attribute(i).isString()) {
				columnsRange.append(i + 1).append(",");
			}
		}
		if (!columnsRange.toString().isEmpty()) {
			if (columnsRange.lastIndexOf(",") == columnsRange.length() - 1)
				columnsRange.deleteCharAt(columnsRange.length() - 1);
			filter.setAttributeRange(columnsRange.toString());
			filter.setInputFormat(data);
			data = Filter.useFilter(data, filter);
		}
	}

	/**
	 * Method to change parameters set by user for clustering alogorithm
	 */
	private void getnsetNewParamValue() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		switch (clusterMethod) {
			case IDAConst.K_MEAN_CLUSTERING:
				getnSetKmeanParam();
				break;
			case IDAConst.FARTHEST_FIRST:
				getnSetFarthestFirstParam();
				break;
			default:
				break;
		}
	}

	/**
	 * Method to change parameters for Farthest First clustering algorithm
	 */
	private void getnSetFarthestFirstParam() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		FarthestFirstAttribute farthestFirstAttribute = (FarthestFirstAttribute) sessionMap.get(IDAConst.FARTHEST_FIRST);
		switch (paramtertoChange) {
			case IDAConst.GET_NUM_CLUSTER:
				paramValue = paramMap.get(IDAConst.NUMBER_OF_CLUSTER).toString();
				if (!paramValue.isEmpty()) {
					farthestFirstAttribute.setNumberOfCluster(getNumericValue(paramValue));
				}
				break;
			case IDAConst.GET_RANDOM_SEED:
				paramValue = paramMap.get(IDAConst.RANDOM_SEED).toString();
				if (!paramValue.isEmpty()) {
					farthestFirstAttribute.setRandomNumberSeed(getNumericValue(paramValue));
				}
				break;
			case IDAConst.GET_MULTI_PARAM:
				multiParmaValue = getFarthestFirstMultiParam();
				if (!multiParmaValue.get(IDAConst.NUMBER_OF_CLUSTER).isEmpty())
					farthestFirstAttribute.setNumberOfCluster(getNumericValue(multiParmaValue.get(IDAConst.NUMBER_OF_CLUSTER)));
				if (!multiParmaValue.get(IDAConst.RANDOM_SEED).isEmpty())
					farthestFirstAttribute.setRandomNumberSeed(getNumericValue(multiParmaValue.get(IDAConst.RANDOM_SEED)));
				break;
			default:
				break;
		}
		if (!paramValue.isEmpty() || multiParmaValue != null) {
			sessionMap.put(IDAConst.FARTHEST_FIRST, farthestFirstAttribute);
			sessionUtil.setSessionMap(sessionMap);
			textMsg = new StringBuilder("Value Changed!! Would you like to change another parameter");
			dialogFlowUtil.setContext("clustering-FarthestFirst-followup", 5);
		}
	}

	/**
	 * Method to change multiple parameters for Farthest First clustering algorithm
	 *
	 * @return Map of attributes for Farthest First algorithm
	 */
	private Map<String, String> getFarthestFirstMultiParam() {
		Map<String, String> farthestFirstMultiParam = new HashMap<>();
		farthestFirstMultiParam.put(IDAConst.NUMBER_OF_CLUSTER, paramMap.get(IDAConst.NUMBER_OF_CLUSTER).toString());
		farthestFirstMultiParam.put(IDAConst.RANDOM_SEED, paramMap.get(IDAConst.RANDOM_SEED).toString());
		return farthestFirstMultiParam;
	}

	/**
	 * Method to change parameters for K-mean clustering algorithm
	 */
	private void getnSetKmeanParam() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		KmeansAttribute kmeansAttribute = (KmeansAttribute) sessionMap.get(IDAConst.K_MEAN_CLUSTERING);
		switch (paramtertoChange) {
			case IDAConst.GET_NUM_CLUSTER:
				paramValue = paramMap.get(IDAConst.NUMBER_OF_CLUSTER).toString();
				if (!paramValue.isEmpty()) {
					kmeansAttribute.setNumberOfCluster(getNumericValue(paramValue));
				}
				break;
			case IDAConst.GET_INIT_METHOD:
				paramValue = paramMap.get(IDAConst.INIT_METHOD).toString();
				if (!paramValue.isEmpty()) {
					kmeansAttribute.setInitializeMethod(getNumericValue(paramValue));
				}
				break;
			case IDAConst.GET_MAX_ITERATION:
				paramValue = paramMap.get(IDAConst.MAX_ITERATION).toString();
				if (!paramValue.isEmpty()) {
					kmeansAttribute.setMaxIterations(getNumericValue(paramValue));
				}
				break;
			case IDAConst.GET_REPLACE_MISSING_VALUES:
				paramValue = paramMap.get(IDAConst.IS_REPLACE_MISSING_VALUE).toString();
				if (!paramValue.isEmpty()) {
					kmeansAttribute.setReplaceMissingValues(Boolean.parseBoolean(paramValue));
				}
				break;
			case IDAConst.GET_NUM_EXECUTION_SLOT:
				paramValue = paramMap.get(IDAConst.NUM_OF_SLOT).toString();
				if (!paramValue.isEmpty()) {
					kmeansAttribute.setNumOfExecutionSlots(getNumericValue(paramValue));
				}
				break;
			case IDAConst.GET_RANDOM_SEED:
				paramValue = paramMap.get(IDAConst.RANDOM_SEED).toString();
				if (!paramValue.isEmpty()) {
					kmeansAttribute.setRandomNumberSeed(getNumericValue(paramValue));
				}
				break;
			case IDAConst.GET_MULTI_PARAM:
				multiParmaValue = getKmeanMultiParam();
				if (!multiParmaValue.get(IDAConst.NUMBER_OF_CLUSTER).isEmpty())
					kmeansAttribute.setNumberOfCluster(getNumericValue(multiParmaValue.get(IDAConst.NUMBER_OF_CLUSTER)));
				if (!multiParmaValue.get(IDAConst.MAX_ITERATION).isEmpty())
					kmeansAttribute.setMaxIterations(getNumericValue(multiParmaValue.get(IDAConst.MAX_ITERATION)));
				if (!multiParmaValue.get(IDAConst.INIT_METHOD).isEmpty())
					kmeansAttribute.setInitializeMethod(getNumericValue(multiParmaValue.get(IDAConst.INIT_METHOD)));
				if (!multiParmaValue.get(IDAConst.NUM_OF_SLOT).isEmpty())
					kmeansAttribute.setNumOfExecutionSlots(getNumericValue(multiParmaValue.get(IDAConst.NUM_OF_SLOT)));
				if (!multiParmaValue.get(IDAConst.RANDOM_SEED).isEmpty())
					kmeansAttribute.setRandomNumberSeed(getNumericValue(multiParmaValue.get(IDAConst.RANDOM_SEED)));
				if (!multiParmaValue.get(IDAConst.IS_REPLACE_MISSING_VALUE).isEmpty())
					kmeansAttribute.setReplaceMissingValues(Boolean.parseBoolean(multiParmaValue.get(IDAConst.IS_REPLACE_MISSING_VALUE)));
				break;
			default:
				break;
		}
		setKmeanParam(kmeansAttribute);
	}

	/**
	 * Method to add parameters of K-mean clusterer to Session Map
	 */
	private void setKmeanParam(KmeansAttribute kmeansAttribute) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		if (!paramValue.isEmpty() || multiParmaValue != null) {
			sessionMap.put(IDAConst.K_MEAN_CLUSTERING, kmeansAttribute);
			sessionUtil.setSessionMap(sessionMap);
			textMsg = new StringBuilder("Value Changed!! Would you like to change another parameter");
			dialogFlowUtil.setContext("clustering-Kmeans-followup", 5);
		}
	}

	/**
	 * Method to change multiple parameters for K-mean clustering algorithm
	 *
	 * @return Map of attributes for K-mean clustering algorithm
	 */
	private Map<String, String> getKmeanMultiParam() {
		Map<String, String> kmeanMultiParam = new HashMap<>();
		kmeanMultiParam.put(IDAConst.NUMBER_OF_CLUSTER, paramMap.get(IDAConst.NUMBER_OF_CLUSTER).toString());
		kmeanMultiParam.put(IDAConst.MAX_ITERATION, paramMap.get(IDAConst.MAX_ITERATION).toString());
		kmeanMultiParam.put(IDAConst.INIT_METHOD, paramMap.get(IDAConst.INIT_METHOD).toString());
		kmeanMultiParam.put(IDAConst.NUM_OF_SLOT, paramMap.get(IDAConst.NUM_OF_SLOT).toString());
		kmeanMultiParam.put(IDAConst.RANDOM_SEED, paramMap.get(IDAConst.RANDOM_SEED).toString());
		kmeanMultiParam.put(IDAConst.IS_REPLACE_MISSING_VALUE, paramMap.get(IDAConst.IS_REPLACE_MISSING_VALUE).toString());
		return kmeanMultiParam;

	}

	/**
	 * Extract parameter name from full intent name
	 *
	 * @param fullIntentName - Full intent name
	 * @return - parameter name
	 */
	private String getParameterToChange(String fullIntentName) {
		return fullIntentName.contains(" - ") && fullIntentName.split(" - ").length > 3 ? fullIntentName.split(" - ")[3] : "";
	}

	/**
	 * Extract parameter change choice from full intent name
	 *
	 * @param fullIntentName - Full intent name
	 * @return - parameter change choice
	 */
	private String getParameterChangeChoice(String fullIntentName) {
		return fullIntentName.contains(" - ") && fullIntentName.split(" - ").length > 2 ? fullIntentName.split(" - ")[2] : "";
	}

	/**
	 * Extract clustering algorithm name from full intent name
	 *
	 * @param fullIntentName - Full intent name
	 * @return - clustering algorithm name
	 */
	private String getClusterMethod(String fullIntentName) {
		return fullIntentName.contains(" - ") ? fullIntentName.split(" - ")[1] : "";
	}

	/**
	 * Method to show default and suggested parameter list
	 */
	private void showParamList() throws Exception {
		EM em = new EM();
		filterStringAttribyte();
		em.buildClusterer(data);
		numCluster = em.numberOfClusters();
		switch (clusterMethod) {
			case IDAConst.K_MEAN_CLUSTERING:
				sessionMap.put(IDAConst.K_MEAN_CLUSTERING, new KmeansAttribute(new SimpleKMeans(), numCluster));
				sessionUtil.setSessionMap(sessionMap);
				showKmeansParamList();
				break;
			case IDAConst.FARTHEST_FIRST:
				sessionMap.put(IDAConst.FARTHEST_FIRST, new FarthestFirstAttribute(new FarthestFirst(), numCluster));
				sessionUtil.setSessionMap(sessionMap);
				showFarthestFirstParamList();
				break;
			default:
				break;
		}
	}

	/**
	 * Method to show parameter list for K-mean clustering algorithm
	 */
	private void showKmeansParamList() {
		KmeansAttribute kmeansAttribute = (KmeansAttribute) sessionMap.get(IDAConst.K_MEAN_CLUSTERING);
		textMsg.append("<ul><li>Number of Clusters(N) = ").append(kmeansAttribute.getNumberOfCluster());
		textMsg.append("</li><li>Intitialize Method(P) = ").append(kmeansAttribute.getInitializeMethod().getSelectedTag().getReadable());
		textMsg.append("</li><li>Max No. of iterations(I) = ").append(kmeansAttribute.getMaxIterations());
		textMsg.append("</li><li>Replace missing values(M) = ").append(kmeansAttribute.getReplaceMissingValues());
		textMsg.append("</li><li>No. of execution slots(E) = ").append(kmeansAttribute.getNumOfExecutionSlots());
		textMsg.append("</li><li>Random number seed(S) = ").append(kmeansAttribute.getRandomNumberSeed()).append("</li></ul>");
	}

	/**
	 * Method to show parameter list for Farthest First clustering algorithm
	 */
	private void showFarthestFirstParamList() {
		FarthestFirstAttribute farthestFirstAttribute = (FarthestFirstAttribute) sessionMap.get(IDAConst.FARTHEST_FIRST);
		textMsg.append("<ul><li>Number of Clusters(N) = ").append(farthestFirstAttribute.getNumberOfCluster());
		textMsg.append("</li><li>Random number seed(S) = ").append(farthestFirstAttribute.getRandomNumberSeed()).append("</li></ul>");
	}

	/**
	 * Convert numeric param values from String to integer
	 *
	 * @param paramValue - parameter value in string
	 * @return - integer value of paramter
	 */
	private int getNumericValue(String paramValue) {

		return Integer.parseInt(paramValue.split(":")[1].split("\\.")[0].trim());

	}

	private File getDataReadyForClustering(String path) throws IDAException, IOException {
		List<Map<String, String>> dataMap = new FileUtil().convertToMap(new File(path));
		Set<String> keys = dataMap.get(0).keySet();
		Set<String> numericKeys = dataMap.get(0).keySet();
		List<Map<String, String>> columnDetail = ValidatorUtil.areParametersValid(datasetName, tableName, new ArrayList<>(keys), false);
		Map<String, String> columnMap = columnDetail.get(0);
		Map<String, Map<String, String>> indicesMap = new HashMap<>();
		keys = keys.stream().filter(i -> !columnMap.get(i).equals(IDAConst.COLUMN_TYPE_NUMERIC)).collect(Collectors.toSet());
		numericKeys = numericKeys.stream().filter(i -> columnMap.get(i).equals(IDAConst.COLUMN_TYPE_NUMERIC)).collect(Collectors.toSet());
		Map<String, String> wordToIndexMap;
		List<String> entriesLst;
		for (String key : keys) {
			entriesLst = dataMap.stream().map(r -> r.get(key)).distinct().collect(Collectors.toList());
			wordToIndexMap = IntStream.range(0, entriesLst.size())
					.boxed()
					.collect(Collectors.toMap(entriesLst::get, Object::toString));
			indicesMap.put(key, wordToIndexMap);
		}
		File tempFile = File.createTempFile("temp", "csv");
		BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
		StringBuilder csvString = new StringBuilder();
		List<String> rowString = new ArrayList<>(dataMap.get(0).keySet());
		csvString.append(String.join(",", rowString)).append("\n");
		for (Map<String, String> row : dataMap) {
			rowString = new ArrayList<>();
			for (String key : row.keySet()) {
				if (numericKeys.contains(key)) {
					try {
						rowString.add(String.valueOf(Double.parseDouble(row.get(key).replaceAll(",", "."))));
					} catch (Exception ex) {
						rowString.add("");
					}
				} else {
					rowString.add("\"" + row.get(key) + "\"");
				}
			}
			csvString.append(String.join(",", rowString)).append("\n");
		}
		writer.write(csvString.toString());
		writer.close();
		tempFile.deleteOnExit();
		return tempFile;
	}
}
