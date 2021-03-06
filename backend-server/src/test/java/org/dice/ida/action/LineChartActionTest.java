package org.dice.ida.action;

import org.dice.ida.constant.IDAConst;
import org.dice.ida.controller.MessageController;
import org.dice.ida.model.ChatMessageResponse;
import org.dice.ida.model.ChatUserMessage;
import org.dice.ida.model.linechart.LineChartData;
import org.dice.ida.model.linechart.LineChartItem;
import org.dice.ida.util.SessionUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.*;

@SpringBootTest
public class LineChartActionTest {
	private ChatUserMessage chatUserMessage;
	private ChatMessageResponse chatMessageResponse;
	@Autowired
	private MessageController messageController;
	@Autowired
	private SessionUtil sessionUtil;

	@Test
	void testLineChartFlow() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("can you draw a line chart?");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("first 10");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date Announced");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		LineChartData lineChartData = (LineChartData) chatMessageResponse.getPayload().get("lineChartData");
		List<LineChartItem> lineChartItemList = new ArrayList<>();
		lineChartItemList.add(new LineChartItem("Delhi", Arrays.asList(0.0, 0.0, 0.0, 1.0, 0.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Haryana", Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0, 4.0)));
		lineChartItemList.add(new LineChartItem("Telangana", Arrays.asList(0.0, 0.0, 0.0, 1.0, 0.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Rajasthan", Arrays.asList(0.0, 0.0, 0.0, 0.0, 1.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Kerala", Arrays.asList(1.0, 1.0, 1.0, 0.0, 0.0, 0.0)));
		assertNotNull(lineChartItemList);
		assertEquals(lineChartItemList, lineChartData.getLines());
		sessionUtil.resetSessionId();
	}

	@Test
	void testLineChartWrongFilterString() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("can you draw a line chart?");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("null");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals(IDAConst.INVALID_FILTER, chatMessageResponse.getMessage());
		sessionUtil.resetSessionId();
	}

	@Test
	void testLineChartWrongColumnName() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("can you draw a line chart?");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("all");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Tested as of");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals("Tested As Of: " + IDAConst.BC_INVALID_COL, chatMessageResponse.getMessage());
		sessionUtil.resetSessionId();
	}

	@Test
	void testLineChartFlowUniqueLabels() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("can you draw a line chart?");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("first 7");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date Announced");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Age Bracket");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		LineChartData lineChartData = (LineChartData) chatMessageResponse.getPayload().get("lineChartData");
		List<LineChartItem> lineChartItemList = new ArrayList<>();
		lineChartItemList.add(new LineChartItem("Delhi", Arrays.asList(0.0, 0.0, 0.0, 45.0, 0.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Haryana", Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0, 55.0)));
		lineChartItemList.add(new LineChartItem("Telangana", Arrays.asList(0.0, 0.0, 0.0, 24.0, 0.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Rajasthan", Arrays.asList(0.0, 0.0, 0.0, 0.0, 69.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Kerala", Arrays.asList(20.0, 0.0, 0.0, 0.0, 0.0, 0.0)));
		assertNotNull(lineChartItemList);
		assertEquals(lineChartItemList, lineChartData.getLines());
		sessionUtil.resetSessionId();
	}

	@Test
	void testLineChartFlowAverageValues() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("can you draw a line chart?");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("first 10");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date Announced");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Age Bracket");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Average");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		LineChartData lineChartData = (LineChartData) chatMessageResponse.getPayload().get("lineChartData");
		List<LineChartItem> lineChartItemList = new ArrayList<>();
		lineChartItemList.add(new LineChartItem("Delhi", Arrays.asList(0.0, 0.0, 0.0, 45.0, 0.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Haryana", Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0, 55.0)));
		lineChartItemList.add(new LineChartItem("Telangana", Arrays.asList(0.0, 0.0, 0.0, 24.0, 0.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Rajasthan", Arrays.asList(0.0, 0.0, 0.0, 0.0, 69.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Kerala", Arrays.asList(20.0, 0.0, 0.0, 0.0, 0.0, 0.0)));
		assertNotNull(lineChartItemList);
		assertEquals(lineChartItemList, lineChartData.getLines());
		sessionUtil.resetSessionId();
	}

	@Test
	void testLineChartFlowSumOfValues() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("can you draw a line chart?");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("first 10");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date Announced");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Age Bracket");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("sum of");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		LineChartData lineChartData = (LineChartData) chatMessageResponse.getPayload().get("lineChartData");
		List<LineChartItem> lineChartItemList = new ArrayList<>();
		lineChartItemList.add(new LineChartItem("Delhi", Arrays.asList(0.0, 0.0, 0.0, 45.0, 0.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Haryana", Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0, 220.0)));
		lineChartItemList.add(new LineChartItem("Telangana", Arrays.asList(0.0, 0.0, 0.0, 24.0, 0.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Rajasthan", Arrays.asList(0.0, 0.0, 0.0, 0.0, 69.0, 0.0)));
		lineChartItemList.add(new LineChartItem("Kerala", Arrays.asList(20.0, 0.0, 0.0, 0.0, 0.0, 0.0)));
		assertNotNull(lineChartItemList);
		assertEquals(lineChartItemList, lineChartData.getLines());
		sessionUtil.resetSessionId();
	}
}
