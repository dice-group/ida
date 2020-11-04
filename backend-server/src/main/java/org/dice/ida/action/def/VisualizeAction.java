package org.dice.ida.action.def;

import org.dice.ida.constant.IDAConst;
import org.dice.ida.exception.IDAException;
import org.dice.ida.model.ChatMessageResponse;
import org.dice.ida.util.RDFUtil;
import org.dice.ida.util.ValidatorUtil;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to handle the line chart implementation
 *
 * @author Nandeesh Patel, Sourabh Poddar
 */
@Component
public class VisualizeAction implements Action {

	private List<Map<String, String>> tableData;
	private Map<Integer,String> attributeList;
	private LinkedHashMap<String,String> attributeMap;
	Map<String, Map<String, Map<String, String>>> instanceMap;
	private List<String> instances;
	private String Intent;


	/**
	 *
	 * @param paramMap - parameters from dialogflow
	 * @param chatMessageResponse - API response object
	 */
	@Override
	public void performAction(Map<String, Object> paramMap, ChatMessageResponse chatMessageResponse) {
		if (ValidatorUtil.preActionValidation(chatMessageResponse)) {
			Map<String, Object> payload = chatMessageResponse.getPayload();
			instanceMap = new RDFUtil().getInstances("bar_chart");
			String datasetName = payload.get("activeDS").toString();
			String tableName = payload.get("activeTable").toString();
			attributeList = new RDFUtil().getAttributeList(paramMap.get(IDAConst.INTENT_NAME).toString());
			attributeMap = getOrderedAttributeMap(attributeList,paramMap);
			instanceMap = getFilteredInstances(attributeMap);

		}



	}

	private Map<String, Map<String, Map<String, String>>> getFilteredInstances(LinkedHashMap<String, String> attributeMap) {
		return null;
	}



	private LinkedHashMap<String, String> getOrderedAttributeMap(Map<Integer, String> attributeList, Map<String, Object> paramMap) {
		LinkedHashMap<String,String> attributeMap = new LinkedHashMap<>();
		for(int i =1;i<=attributeList.size();i++)
		{
			String attributeName = attributeList.get(i);
			String attributeType = attributeName+"_type";
			attributeMap.put(attributeName,paramMap.getOrDefault(attributeName,"").toString());
			attributeMap.put(attributeType,paramMap.getOrDefault(attributeType,"").toString());

		}
		return attributeMap;
	}


	/**
	 * Method to validate the datatype of the columns provided for the line chart
	 *
	 * @param paramMap - map containing the name and datatype of all the columns in selected table
	 * @throws IDAException - An exception when the datatype of the given columns are not suitable for line chart.
	 */
/*	private void validateParamTypes(Map<String, String> paramMap) throws IDAException {
		if (!IDAConst.COLUMN_TYPE_DATE.equals(paramMap.get(dateColumn))) {
			throw new IDAException(dateColumn + IDAConst.INVALID_DATE_COLUMN_MSG);
		} else if (!IDAConst.COLUMN_TYPE_NUMERIC.equals(paramMap.get(valueColumn)) && !labelColumn.equals(valueColumn)) {
			throw new IDAException(valueColumn + IDAConst.INVALID_NUMERIC_COLUMN_MSG);
		}
	}

	*//**
	 * Method to create the X-Axis labels and intervals based on the size of data
	 * Eg: Days when data is small, Months when data is spread across many months, Years when data is spread across years
	 *
	 *//*
	private void setBinTypeAndLabels() {
		Calendar calendar = Calendar.getInstance();
		List<String> dayLabels = new ArrayList<>();
		List<String> monthLabels = new ArrayList<>();
		List<String> yearLabels = new ArrayList<>();
		createMonthIndexMap();
		createComparators();

		for (Map<String, String> object : tableData) {
			String currentDate = object.get(dateColumn).trim();
			try {
				calendar.setTime(DateUtils.parseDateStrictly(currentDate, IDAConst.DATE_PATTERNS));
			} catch (ParseException ex) {
				ex.printStackTrace();
				continue; // Ignore the row and continue with the next
			}

			if (!dayLabels.contains(currentDate)) {
				dayLabels.add(currentDate);
			}
			if (!monthLabels.contains(getMonth(calendar.get(Calendar.MONTH)) + ", " + calendar.get(Calendar.YEAR))) {
				monthLabels.add(getMonth(calendar.get(Calendar.MONTH)) + ", " + calendar.get(Calendar.YEAR));
			}
			if (!yearLabels.contains(String.valueOf(calendar.get(Calendar.YEAR)))) {
				yearLabels.add(String.valueOf(calendar.get(Calendar.YEAR)));
			}
		}
		dayLabels.sort(dateComparator);
		monthLabels.sort(monthComparator);
		yearLabels.sort(yearComparator);
		int d = Math.abs(dayLabels.size() - 10);
		int m = Math.abs(monthLabels.size() - 10);
		int y = Math.abs(yearLabels.size() - 10);
		if (d <= m && d <= y) {
			xAxisLabels = dayLabels;
			binType = IDAConst.LABEL_TYPE_DATE;
		} else if (m <= y) {
			xAxisLabels = monthLabels;
			binType = IDAConst.LABEL_TYPE_MONTH;
		} else {
			xAxisLabels = yearLabels;
			binType = IDAConst.LABEL_TYPE_YEAR;
		}
	}

	*//**
	 * Method to create the data for each lines based on the bin type.
	 *
	 *//*
	private void createChartData() {
		Map<String, Double> labelData;
		String date;
		String label;
		Double value;
		String dateKey;
		for (Map<String, String> row : tableData) {
			date = row.get(dateColumn);
			label = row.get(labelColumn);
			value = 0.0;
			try {
				value = labelColumn.equals(valueColumn) ? 1.0 : Double.parseDouble(row.get(valueColumn));
			} catch (NumberFormatException ex) {
				System.out.println(ex.getMessage());
			}
			if (binType.equals(IDAConst.LABEL_TYPE_MONTH)) {
				labelData = new TreeMap<>(monthComparator);
			} else if (binType.equals(IDAConst.LABEL_TYPE_YEAR)) {
				labelData = new TreeMap<>(yearComparator);
			} else {
				labelData = new TreeMap<>(dateComparator);
			}
			if (chartData.containsKey(label)) {
				labelData = chartData.get(label);
			} else {
				for (String l : xAxisLabels) {
					labelData.put(l, 0.0);
				}
			}
			try {
				dateKey = extractDateKey(binType, date);
			} catch (ParseException ex) {
				ex.printStackTrace();
				continue; // Ignore the row and continue with the next
			}
			if (labelData.containsKey(dateKey))
				labelData.put(dateKey, labelData.get(dateKey) + value);
			else
				labelData.put(dateKey, value);
			chartData.put(label, labelData);
		}
	}

	*//**
	 * Method to create a LineChartData object with all the values that would be sent in the response
	 *
	 * @return - An object of LineChartData class with all the values
	 *//*
	private LineChartData createLineChartData() {
		LineChartData lineChartData = new LineChartData();
		lineChartData.setxAxisLabel(dateColumn);
		String yAxisLabel = valueColumn;
		if (labelColumn.equals(valueColumn)) {
			yAxisLabel = IDAConst.COUNT_OF_PREFIX + valueColumn;
		}
		lineChartData.setyAxisLabel(yAxisLabel);
		lineChartData.setChartDesc(IDAConst.LINE_CHART_DESC_PREFIX + yAxisLabel + " across " + dateColumn);
		lineChartData.setxAxisLabels(xAxisLabels);
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
		return lineChartData;
	}

	*//**
	 * Method to convert the month index to its string equivalent
	 *
	 * @param month - index of the month as integer
	 * @return - name of the month based on the integer
	 *//*
	private String getMonth(int month) {
		return dateFormatSymbols.getMonths()[month];
	}

	*//**
	 * Method to create the label for the graph based on the bin type for a given date string
	 *
	 * @param type - type of the binning (date, month, or year)
	 * @param dateString - date in string format
	 * @return - Label to be used on the graph
	 * @throws ParseException - Exception when a date is not in correct format
	 *//*
	private String extractDateKey(String type, String dateString) throws ParseException {
		Calendar calendar = Calendar.getInstance();
		Date date = DateUtils.parseDateStrictly(dateString, IDAConst.DATE_PATTERNS);
		calendar.setTime(date);
		switch (type) {
			case IDAConst.LABEL_TYPE_MONTH:
				return getMonth(calendar.get(Calendar.MONTH)) + ", " + calendar.get(Calendar.YEAR);
			case IDAConst.LABEL_TYPE_YEAR:
				return String.valueOf(calendar.get(Calendar.YEAR));
			default:
				return dateString;
		}
	}

	*//**
	 * Method to create a map of months and their index which is used for sorting the month labels
	 *//*
	private void createMonthIndexMap() {
		monthIndexMap = new HashMap<>();
		for (int i = 0; i < dateFormatSymbols.getMonths().length; i++) {
			monthIndexMap.put(dateFormatSymbols.getMonths()[i], i);
		}
	}

	*//**
	 * Method to create comparators for different label types (date, month, and year) which is used for sorting the labels
	 *//*
	private void createComparators() {
		dateComparator = (String date1, String date2) -> {
			try {
				return DateUtils.parseDateStrictly(date1, IDAConst.DATE_PATTERNS).compareTo(DateUtils.parseDateStrictly(date2, IDAConst.DATE_PATTERNS));
			} catch (ParseException e) {
				return -1;
			}
		};
		monthComparator = Comparator.comparing((String month) -> monthIndexMap.get(month.split(",")[0]));
		yearComparator = Comparator.comparing(Integer::parseInt);
	}*/

}
