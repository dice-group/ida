package org.dice.ida.action;

import org.dice.ida.controller.MessageController;
import org.dice.ida.model.ChatMessageResponse;
import org.dice.ida.model.ChatUserMessage;
import org.dice.ida.model.bubblechart.BubbleChartData;
import org.dice.ida.model.bubblechart.BubbleChartItem;
import org.dice.ida.model.groupedbubblechart.GroupedBubbleChartData;
import org.dice.ida.util.SessionUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class BubbleChartActionTest {
	private ChatUserMessage chatUserMessage;
	private ChatMessageResponse chatMessageResponse;
	@Autowired
	private MessageController messageController;
	@Autowired
	private SessionUtil sessionUtil;

	@Test
	void testBubbleChartFlow() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bubble chart");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		BubbleChartData bubbleChartData = (BubbleChartData) chatMessageResponse.getPayload().get("bubbleChartData");
		List<BubbleChartItem> bubbleChartItemList = new ArrayList<>();
		bubbleChartItemList.add(new BubbleChartItem("Andaman and Nicobar Islands", "Andaman and Nicobar Islands", 15.0));
		bubbleChartItemList.add(new BubbleChartItem("Andhra Pradesh", "Andhra Pradesh", 649.0));
		bubbleChartItemList.add(new BubbleChartItem("Arunachal Pradesh", "Arunachal Pradesh", 1.0));
		bubbleChartItemList.add(new BubbleChartItem("Assam", "Assam", 35.0));
		assertNotNull(bubbleChartData);
		assertTrue(bubbleChartData.getItems().containsAll(bubbleChartItemList));
		sessionUtil.resetSessionId();
	}

	@Test
	void testBubbleChartNumBins() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bubble chart");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Age Bracket");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("bins");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("10");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Age Bracket");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("count of");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		BubbleChartData bubbleChartData = (BubbleChartData) chatMessageResponse.getPayload().get("bubbleChartData");
		List<BubbleChartItem> bubbleChartItemList = new ArrayList<>();
		bubbleChartItemList.add(new BubbleChartItem("0.0 - 9.0", "0.0 - 9.0", 50.0));
		bubbleChartItemList.add(new BubbleChartItem("10.0 - 19.0", "10.0 - 19.0", 115.0));
		bubbleChartItemList.add(new BubbleChartItem("20.0 - 29.0", "20.0 - 29.0", 339.0));
		bubbleChartItemList.add(new BubbleChartItem("30.0 - 39.0", "30.0 - 39.0", 380.0));
		bubbleChartItemList.add(new BubbleChartItem("40.0 - 49.0", "40.0 - 49.0", 286.0));
		bubbleChartItemList.add(new BubbleChartItem("50.0 - 59.0", "50.0 - 59.0", 242.0));
		bubbleChartItemList.add(new BubbleChartItem("60.0 - 69.0", "60.0 - 69.0", 179.0));
		bubbleChartItemList.add(new BubbleChartItem("70.0 - 79.0", "70.0 - 79.0", 59.0));
		bubbleChartItemList.add(new BubbleChartItem("80.0 - 89.0", "80.0 - 89.0", 13.0));
		bubbleChartItemList.add(new BubbleChartItem("90.0 - 99.0", "90.0 - 99.0", 3.0));
		bubbleChartItemList.add(new BubbleChartItem("UNKNOWN", "UNKNOWN", 15694.0));
		assertNotNull(bubbleChartData);
		assertEquals(bubbleChartData.getItems(), bubbleChartItemList);
		sessionUtil.resetSessionId();
	}

	@Test
	void testBubbleChartDateBins() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bubble chart");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date announced");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("bins");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals("What should be the duration of each bin?<br/>Eg: 1 week, 2 weeks, 3 months", chatMessageResponse.getMessage());
		chatUserMessage.setMessage("1 month");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date Announced");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		BubbleChartData bubbleChartData = (BubbleChartData) chatMessageResponse.getPayload().get("bubbleChartData");
		List<BubbleChartItem> bubbleChartItemList = new ArrayList<>();
		bubbleChartItemList.add(new BubbleChartItem("January-2020", "January-2020", 1.0));
		bubbleChartItemList.add(new BubbleChartItem("February-2020", "February-2020", 2.0));
		bubbleChartItemList.add(new BubbleChartItem("March-2020", "March-2020", 1632.0));
		bubbleChartItemList.add(new BubbleChartItem("April-2020", "April-2020", 15729.0));
		assertNotNull(bubbleChartData);
		assertEquals(bubbleChartData.getItems(), bubbleChartItemList);
		sessionUtil.resetSessionId();
	}

	@Test
	void testBubbleChartFlowUniqueLabels() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bubble chart");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Case_Time_Series.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("unique");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Daily confirmed");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		BubbleChartData bubbleChartData = (BubbleChartData) chatMessageResponse.getPayload().get("bubbleChartData");
		List<BubbleChartItem> bubbleChartItemList = new ArrayList<>();
		bubbleChartItemList.add(new BubbleChartItem("02 February ", "02 February ", 1.0));
		bubbleChartItemList.add(new BubbleChartItem("31 January ", "31 January ", 0.0));
		bubbleChartItemList.add(new BubbleChartItem("01 February ", "01 February ", 0.0));
		bubbleChartItemList.add(new BubbleChartItem("03 February ", "03 February ", 1.0));
		bubbleChartItemList.add(new BubbleChartItem("30 January ", "30 January ", 1.0));
		assertNotNull(bubbleChartData);
		assertTrue(bubbleChartData.getItems().containsAll(bubbleChartItemList));
		sessionUtil.resetSessionId();
	}

	@Test
	void testGroupedBubbleNumLabels() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bubble chart");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Age Bracket");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("average");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("yes");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Current Status");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		GroupedBubbleChartData groupedBubbleChartData = (GroupedBubbleChartData) chatMessageResponse.getPayload().get("bubbleChartData");
		Map<String, List<BubbleChartItem>> groupedBubbleChartItems = new HashMap<>();
		groupedBubbleChartItems.put("Delhi", new ArrayList<>(){{
			add(new BubbleChartItem("Recovered", "Recovered", 22.5));
			add(new BubbleChartItem("Migrated", "Migrated", 0.0));
			add(new BubbleChartItem("Hospitalized", "Hospitalized", 0.176088044022011));
			add(new BubbleChartItem("Deceased", "Deceased", 69.0));
		}});
		groupedBubbleChartItems.put("Telangana", new ArrayList<>(){{
			add(new BubbleChartItem("Recovered", "Recovered", 24.0));
			add(new BubbleChartItem("Migrated", "Migrated", 0.0));
			add(new BubbleChartItem("Hospitalized", "Hospitalized", 1.3259345794392523));
			add(new BubbleChartItem("Deceased", "Deceased", 74.0));
		}});
		groupedBubbleChartItems.put("Kerala", new ArrayList<>(){{
			add(new BubbleChartItem("Recovered", "Recovered", 14.017241379310345));
			add(new BubbleChartItem("Migrated", "Migrated", 0.0));
			add(new BubbleChartItem("Hospitalized", "Hospitalized", 12.366568914956012));
			add(new BubbleChartItem("Deceased", "Deceased", 69.33333333333333));
		}});
		assertNotNull(groupedBubbleChartData);
		assertTrue(groupedBubbleChartData.getGroupedBubbleChartData().keySet().containsAll(groupedBubbleChartItems.keySet()));
		for(String key: groupedBubbleChartItems.keySet()){
			assertTrue(groupedBubbleChartData.getGroupedBubbleChartData().get(key).containsAll(groupedBubbleChartItems.get(key)));
		}
		sessionUtil.resetSessionId();
	}
}
