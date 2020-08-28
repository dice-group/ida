package org.dice.ida.action.def;

import java.io.IOException;
import java.util.Map;

import org.dice.ida.constant.IDAConst;
import org.dice.ida.util.FileUtil;
import org.dice.ida.model.ChatMessageResponse;

public class LoadDataSetAction implements Action {


	@Override
	public void performAction(Map<String, Object> paramMap, ChatMessageResponse resp) {
		try {
			FileUtil fileUtil = new FileUtil();
			// Check if datasetName is provided
			String datasetName = paramMap.get(IDAConst.PARAM_DATASET_NAME).toString();
			if(datasetName != null && !datasetName.isEmpty()) {
				if (fileUtil.datasetExists(datasetName)) {
					Map<String, Object> dataMap = resp.getPayload();
					dataMap.put("label", datasetName);
					dataMap.put("dsName", datasetName);
					dataMap.put("activeTable", ""); // its required for visualization suggestions
					dataMap.put("dsMd", fileUtil.getDatasetMetaData(datasetName));
					dataMap.put("dsData", fileUtil.getDatasetContent(datasetName));
					resp.setPayload(dataMap);
					resp.setUiAction(IDAConst.UIA_LOADDS);
				}
				setLoadDatasetResponse(paramMap, resp);
			} else {
				// Forward the message from the chatbot to the user
				SimpleTextAction.setSimpleTextResponse(paramMap, resp);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setLoadDatasetResponse(Map<String, Object> paramMap, ChatMessageResponse resp) {
		String textMsg = paramMap.get(IDAConst.PARAM_TEXT_MSG).toString();
		resp.setMessage(textMsg);
		resp.setUiAction(IDAConst.UIA_LOADDS);
	}

}