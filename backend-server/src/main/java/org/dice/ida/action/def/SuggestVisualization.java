package org.dice.ida.action.def;

import org.dice.ida.constant.IDAConst;
import org.dice.ida.model.ChatMessageResponse;
import org.dice.ida.model.ChatUserMessage;
import org.dice.ida.model.suggestion.SuggestionData;
import org.dice.ida.model.suggestion.SuggestionParam;
import org.dice.ida.model.suggestion.VisualizationInfo;
import org.dice.ida.util.DataUtil;
import org.dice.ida.util.RDFUtil;
import org.dice.ida.util.SuggestionUtil;
import org.dice.ida.util.ValidatorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


@Component
public class SuggestVisualization implements Action {

	@Autowired
	private DataUtil dataUtil;

	@Autowired
	private SuggestionUtil suggestionUtil;

	@Autowired
	private RDFUtil rdfUtil;
	private Map<String, Map<String, Map<String, String>>> instanceMap;
	private Map<String, Object> payload;
	private Map<String, String> columnUniquenessMap;
	private Map<String, String> columnMap;

	@Override
	public void performAction(Map<String, Object> paramMap, ChatMessageResponse chatMessageResponse, ChatUserMessage message) throws Exception {
		payload = chatMessageResponse.getPayload();
		if (ValidatorUtil.preActionValidation(chatMessageResponse)) {
			String datasetName = payload.get("activeDS").toString();
			String tableName = payload.get("activeTable").toString();
			Map<String, Map<String, Double>> statProps = suggestionUtil.getStatProps(datasetName, tableName);
			Map<String, Map<String, List<String>>> suggestionParam = rdfUtil.getSuggestionParamters();
			Map<String, VisualizationInfo> vizInfoMap = rdfUtil.getVisualizationInfo();
			List<SuggestionData> suggestionResponse = new ArrayList<>();
			Map<String, String> paramDisplayNameMap = rdfUtil.getParamDisplayNames();
			for (String viz : suggestionParam.keySet()) {
				String vizIntent = rdfUtil.getVizIntent(viz);
				Map<Integer, String> attributeListViz = rdfUtil.getSuggestionAttributeList(vizIntent);
				List<String> columnNameList = new ArrayList<>();
				Map<String, Set<String>> vizParamTypeList = new HashMap<>();
				instanceMap = rdfUtil.getInstances(vizIntent);
				Map<String, List<String>> paramList = suggestionParam.get(viz);
				List<SuggestionParam> suggestionParamList = new ArrayList<>();
				Map<String, String> vizParams = new HashMap<>(){{
					put(IDAConst.PARAM_INTENT_DETECTION_CONFIDENCE, "1.0");
					put(IDAConst.INTENT_NAME, vizIntent);
					put(IDAConst.PARAM_FILTER_STRING, "all");
					put(IDAConst.PARAM_TEXT_MSG, "Suggested visualization rendered");
					put("isGrouped", "false");
					put("Reference_Values_choice", "false");
				}};
				for (String param : paramList.keySet()) {
					Map<String, Double> attributeList = statProps.get(paramList.get(param).get(0));
					String attributeProperty = paramList.get(param).get(1);
					String key;
					if (attributeList.isEmpty()) {
						suggestionParamList = new ArrayList<>();
						break;
					}
					if (attributeProperty.equals("min"))
						key = Collections.min(attributeList.entrySet(), Map.Entry.comparingByValue()).getKey();
					else
						key = Collections.max(attributeList.entrySet(), Map.Entry.comparingByValue()).getKey();
					if (key.contains("|")) {
						key = vizParams.containsValue(key.substring(0, key.indexOf("|"))) ? key.substring(key.indexOf("|") + 1) : key.substring(0, key.indexOf("|"));
					}
					vizParams.put(param, key);
					paramMap.put(param, key);
					columnNameList.add(key);
					String paramLabel = paramDisplayNameMap.getOrDefault(param, param);
					suggestionParamList.add(new SuggestionParam(paramLabel, key, param));
				}
				paramMap.put(IDAConst.PARAM_FILTER_STRING, IDAConst.BG_FILTER_ALL);
				if (columnNameList.size() == paramList.size()) {
					for (Integer attrpos : attributeListViz.keySet()) {
						List<Map<String, String>> columnDetail = ValidatorUtil.areParametersValid(datasetName, tableName, columnNameList, false);
						columnMap = columnDetail.get(0);
						columnUniquenessMap = columnDetail.get(1);
						String paramType = columnMap.get(paramMap.get(attributeListViz.get(attrpos)).toString());
						Set<String> options = getFilteredInstances(attributeListViz.get(attrpos), paramType.toLowerCase(), paramMap.get(attributeListViz.get(attrpos)).toString(), false, paramMap);
						if(options.contains(IDAConst.COLUMN_TYPE_BINS))
							options.remove(IDAConst.COLUMN_TYPE_BINS);
						vizParamTypeList.put(attributeListViz.get(attrpos),options);
					}
				}
				if (!suggestionParamList.isEmpty()) {
					SuggestionData suggestionData = new SuggestionData();
					suggestionData.setVizName(viz);
					suggestionData.setVisualizationInfo(vizInfoMap.get(viz));
					suggestionData.setSuggestionParamList(suggestionParamList);
					suggestionData.setVisualizationParams(vizParams);
					suggestionData.setVisualizationParamTypeList(vizParamTypeList);
					suggestionResponse.add(suggestionData);
				}
			}
			payload.put("suggestionData", suggestionResponse);
			chatMessageResponse.setUiAction(IDAConst.UIA_LOAD_SUGGESTION);
			chatMessageResponse.setMessage(IDAConst.SUGGESTION_LOADED);
		}
	}

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

}
