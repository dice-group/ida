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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
		chatUserMessage.setMessage("Date Announced");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		LineChartData lineChartData = (LineChartData) chatMessageResponse.getPayload().get("lineChartData");
		List<LineChartItem> lineChartItemList = new ArrayList<>();
		lineChartItemList.add(new LineChartItem("Delhi", Arrays.asList(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 3.0, 1.0, 3.0, 6.0, 7.0, 1.0, 2.0, 0.0, 5.0, 4.0, 1.0, 9.0, 23.0, 25.0, 23.0, 32.0, 141.0, 93.0, 59.0, 58.0, 22.0, 51.0, 93.0, 51.0, 183.0, 166.0, 85.0, 356.0, 51.0, 17.0, 62.0, 67.0, 186.0, 110.0)));
		assertNotNull(lineChartItemList);
		assertTrue(lineChartData.getLines().containsAll(lineChartItemList));
		sessionUtil.resetSessionId();
	}

	@Test
	void testLineChartWrongColumnName() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("can you draw a line chart?");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Tested as of");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals("Tested As Of: " + IDAConst.BC_INVALID_COL, chatMessageResponse.getMessage());
		sessionUtil.resetSessionId();
	}

	@Test
	void testLineChartFlowAverageValues() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("can you draw a line chart?");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
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
		lineChartItemList.add(new LineChartItem("Delhi", Arrays.asList(0.0, 0.0, 0.0, 45.0, 0.0, 0.0, 27.0, 0.0, 0.0, 0.0, 25.0, 0.0, 46.0, 69.0, 0.0, 0.0, 0.0, 0.0, 0.0, 38.0, 25.333333333333332, 15.0, 7.142857142857143, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)));
		assertNotNull(lineChartItemList);
		assertTrue(lineChartData.getLines().containsAll(lineChartItemList));
		sessionUtil.resetSessionId();
	}

	@Test
	void testLineChartFlowSumOfValues() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("can you draw a line chart?");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
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
		lineChartItemList.add(new LineChartItem("Delhi", Arrays.asList(0.0, 0.0, 0.0, 45.0, 0.0, 0.0, 27.0, 0.0, 0.0, 0.0, 25.0, 0.0, 46.0, 69.0, 0.0, 0.0, 0.0, 0.0, 0.0, 38.0, 76.0, 90.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)));
		assertNotNull(lineChartItemList);
		assertTrue(lineChartData.getLines().containsAll(lineChartItemList));
		sessionUtil.resetSessionId();
	}
}
