package org.dice.ida.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dice.ida.constant.IDAConst;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to expose util methods for Data based operation in IDA
 *
 * @author Sourabh, Nandeesh
 */
@Component
@Scope("singleton")
public class DataUtil {
	/**
	 * Method to read a CSV file and return its contents (only required columns) as a list of maps
	 *
	 * @param datasetName - name of the dataset
	 * @param tableName   - name of the table ( file name)
	 * @param columns     - list of columns whose data is required
	 * @return list of maps where each map represents a row of the table
	 * @throws IOException - Exception when the dataset or table does not exist
	 */
	public List<Map<String, String>> getData(String datasetName, String tableName, List<String> columns, Map<String, String> columnMap) throws IOException {
		List<Map<String, String>> extractedData = new ArrayList<>();
		String path = new FileUtil().fetchSysFilePath(datasetName + "/" + tableName);
		List<Map<String, String>> fileData = new FileUtil().convertToMap(new File(path));
		return filterData(fileData, columns, columnMap);
	}

	/**
	 * Method to filter the file date based on a filter string
	 *
	 * @param fileData   - File data extracted into a map.
	 * @param columns    - list of columns whose data is required
	 * @return list of maps where each map represents a row of the table
	 */
	public List<Map<String, String>> filterData(List<Map<String, String>> fileData, List<String> columns, Map<String, String> columnMap) {
		List<Map<String, String>> extractedData = new ArrayList<>();
		int rangeStart = 0;
		int rangeEnd = fileData.size();

		for (int i = rangeStart; i < rangeEnd; i++) {
			Map<String, String> dataRow = new HashMap<>();
			for (String column : columns) {
				// Getting data only from required columns
				String columnValue = fileData.get(i).get(column);
				if (columnMap.get(column).equalsIgnoreCase("Numeric"))
					dataRow.put(column, DbUtils.manageNullValues(columnValue.replaceAll(",", ".")));
				else
					dataRow.put(column, DbUtils.manageNullValues(columnValue));
			}
			extractedData.add(dataRow);
		}
		return extractedData;
	}

	public List<Map<String, String>> getDataSet(String datasetName, String tableName) throws IOException {
		Map<String, String> columnTypeMap = new HashMap<>();
		List<String> columns = new ArrayList<>();
		List<Map<String, String>> extractedData = new ArrayList<>();
		String path = new FileUtil().fetchSysFilePath(datasetName + "/" + tableName);
		List<Map<String, String>> fileData = new FileUtil().convertToMap(new File(path));
		ObjectNode metaData = new FileUtil().getDatasetMetaData(datasetName);
		JsonNode fileDetails = metaData.get(IDAConst.FILE_DETAILS_ATTR);
		for (int i = 0; i < fileDetails.size(); i++) {
			if (tableName.equals(fileDetails.get(i).get(IDAConst.FILE_NAME_ATTR).asText())) {
				JsonNode columnDetails = fileDetails.get(i).get(IDAConst.COLUMN_DETAILS_ATTR);
				String columnName;
				for (int j = 0; j < columnDetails.size(); j++) {
					columnName = columnDetails.get(j).get(IDAConst.COLUMN_NAME_ATTR).asText().strip();
					columns.add(columnName);
					columnTypeMap.put(columnName, columnDetails.get(j).get(IDAConst.COLUMN_TYPE_ATTR).asText());
				}
			}
		}


		for (int i = 0; i < fileData.size(); i++) {
			Map<String, String> dataRow = new HashMap<>();
			for (String column : columns) {
				// Getting data only from required columns
				String columnValue = fileData.get(i).get(column).strip().replaceAll("^[\'\"](.*)[\'\"]$", "$1");
				if (columnTypeMap.get(column).equalsIgnoreCase("Numeric"))
					dataRow.put(column, (columnValue.replaceAll(",", ".")));
				else
					dataRow.put(column, (columnValue));
			}
			extractedData.add(dataRow);
		}
		return extractedData;

	}
}
