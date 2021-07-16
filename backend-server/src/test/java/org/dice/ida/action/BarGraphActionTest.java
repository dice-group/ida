package org.dice.ida.action;

import org.dice.ida.constant.IDAConst;
import org.dice.ida.controller.MessageController;
import org.dice.ida.model.ChatMessageResponse;
import org.dice.ida.model.ChatUserMessage;
import org.dice.ida.model.bargraph.BarGraphData;
import org.dice.ida.model.bargraph.BarGraphItem;
import org.dice.ida.model.groupedbargraph.GroupedBarGraphData;
import org.dice.ida.util.SessionUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
public class BarGraphActionTest {
	@Autowired
	private MessageController messageController;
	private ChatUserMessage chatUserMessage;
	private ChatMessageResponse chatMessageResponse;
	@Autowired
	private SessionUtil sessionUtil;

	@Test
	void testBarGraphFlow() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		BarGraphData barGraphData = (BarGraphData) chatMessageResponse.getPayload().get("barGraphData");
		List<BarGraphItem> barGraphItemList = new ArrayList<>();
		barGraphItemList.add(new BarGraphItem("Andaman and Nicobar Islands", 15.0));
		barGraphItemList.add(new BarGraphItem("Andhra Pradesh", 649.0));
		barGraphItemList.add(new BarGraphItem("Arunachal Pradesh", 1.0));
		barGraphItemList.add(new BarGraphItem("Assam", 35.0));
		assertNotNull(barGraphData);
		assertTrue(barGraphData.getItems().containsAll(barGraphItemList));
		sessionUtil.resetSessionId();
	}

	@Test
	void testBarGraphSteps() throws Exception{
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Age Bracket");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("unique");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals("Age Bracket cannot be used as Unique. Please provide correct type.", chatMessageResponse.getMessage());
		chatUserMessage.setMessage("bins");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals("What should be the size of each bin?<br/>Eg: 10, 25, 15, twenty, twelve", chatMessageResponse.getMessage());
		chatUserMessage.setMessage("10");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals("Which column values should be used for Y-Axis?", chatMessageResponse.getMessage());
		sessionUtil.resetSessionId();
	}

	@Test
	void testBarGraphNonSuitableColumn() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Case_Time_Series.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("unique");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals("Date cannot be used as Y-Axis. Please provide a different column?", chatMessageResponse.getMessage());
		sessionUtil.resetSessionId();
	}

	@Test
	void testBarGraphNumBins() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
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
		BarGraphData barGraphData = (BarGraphData) chatMessageResponse.getPayload().get("barGraphData");
		List<BarGraphItem> barGraphItemList = new ArrayList<>();
		barGraphItemList.add(new BarGraphItem("0.0 - 9.0", 50.0));
		barGraphItemList.add(new BarGraphItem("10.0 - 19.0", 115.0));
		barGraphItemList.add(new BarGraphItem("20.0 - 29.0", 339.0));
		barGraphItemList.add(new BarGraphItem("30.0 - 39.0", 380.0));
		barGraphItemList.add(new BarGraphItem("40.0 - 49.0", 286.0));
		barGraphItemList.add(new BarGraphItem("50.0 - 59.0", 242.0));
		barGraphItemList.add(new BarGraphItem("60.0 - 69.0", 179.0));
		barGraphItemList.add(new BarGraphItem("70.0 - 79.0", 59.0));
		barGraphItemList.add(new BarGraphItem("80.0 - 89.0", 13.0));
		barGraphItemList.add(new BarGraphItem("90.0 - 99.0", 3.0));
		barGraphItemList.add(new BarGraphItem("UNKNOWN", 15694.0));
		assertNotNull(barGraphData);
		assertEquals(barGraphData.getItems(), barGraphItemList);
		sessionUtil.resetSessionId();
	}

	@Test
	void testBarGraphDateBins() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date Announced");
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
		BarGraphData barGraphData = (BarGraphData) chatMessageResponse.getPayload().get("barGraphData");
		List<BarGraphItem> barGraphItemList = new ArrayList<>();
		barGraphItemList.add(new BarGraphItem("January-2020", 1.0));
		barGraphItemList.add(new BarGraphItem("February-2020", 2.0));
		barGraphItemList.add(new BarGraphItem("March-2020", 1632.0));
		barGraphItemList.add(new BarGraphItem("April-2020", 15729.0));
		assertNotNull(barGraphData);
		assertEquals(barGraphData.getItems(), barGraphItemList);
		sessionUtil.resetSessionId();
	}

	@Test
	void testBarGraphDateWeekBins() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("ICMR_Tests_Datewise.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Tested as of");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("bins");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("2 weeks");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Total positive cases");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("sum of");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		BarGraphData barGraphData = (BarGraphData) chatMessageResponse.getPayload().get("barGraphData");
		List<BarGraphItem> barGraphItemList = new ArrayList<>();
		barGraphItemList.add(new BarGraphItem("03-05-20 to 16-05-20", 0.0));
		barGraphItemList.add(new BarGraphItem("09-08-20 to 22-08-20", 0.0));
		barGraphItemList.add(new BarGraphItem("12-07-20 to 25-07-20", 0.0));
		barGraphItemList.add(new BarGraphItem("31-05-20 to 13-06-20", 0.0));
		barGraphItemList.add(new BarGraphItem("05-04-20 to 18-04-20", 107431.581));
		barGraphItemList.add(new BarGraphItem("08-03-20 to 21-03-20", 1606.0));
		barGraphItemList.add(new BarGraphItem("26-07-20 to 08-08-20", 0.0));
		barGraphItemList.add(new BarGraphItem("19-04-20 to 02-05-20", 62914.0));
		barGraphItemList.add(new BarGraphItem("22-03-20 to 04-04-20", 16094.0));
		barGraphItemList.add(new BarGraphItem("28-06-20 to 11-07-20", 0.0));
		barGraphItemList.add(new BarGraphItem("17-05-20 to 30-05-20", 0.0));
		barGraphItemList.add(new BarGraphItem("14-06-20 to 27-06-20", 0.0));
		barGraphItemList.add(new BarGraphItem("UNKNOWN", 0.0));
		assertNotNull(barGraphData);
		assertEquals(barGraphData.getItems(), barGraphItemList);
		sessionUtil.resetSessionId();
	}

	@Test
	void testBarGraphDateDayBins() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("ICMR_Tests_Datewise.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Tested as of");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("bins");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("14 days");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Total positive cases");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("sum of");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		BarGraphData barGraphData = (BarGraphData) chatMessageResponse.getPayload().get("barGraphData");
		List<BarGraphItem> barGraphItemList = new ArrayList<>();
		barGraphItemList.add(new BarGraphItem("03-05-20 to 16-05-20", 0.0));
		barGraphItemList.add(new BarGraphItem("09-08-20 to 22-08-20", 0.0));
		barGraphItemList.add(new BarGraphItem("12-07-20 to 25-07-20", 0.0));
		barGraphItemList.add(new BarGraphItem("31-05-20 to 13-06-20", 0.0));
		barGraphItemList.add(new BarGraphItem("05-04-20 to 18-04-20", 107431.581));
		barGraphItemList.add(new BarGraphItem("08-03-20 to 21-03-20", 1606.0));
		barGraphItemList.add(new BarGraphItem("26-07-20 to 08-08-20", 0.0));
		barGraphItemList.add(new BarGraphItem("19-04-20 to 02-05-20", 62914.0));
		barGraphItemList.add(new BarGraphItem("22-03-20 to 04-04-20", 16094.0));
		barGraphItemList.add(new BarGraphItem("28-06-20 to 11-07-20", 0.0));
		barGraphItemList.add(new BarGraphItem("17-05-20 to 30-05-20", 0.0));
		barGraphItemList.add(new BarGraphItem("14-06-20 to 27-06-20", 0.0));
		barGraphItemList.add(new BarGraphItem("UNKNOWN", 0.0));
		assertNotNull(barGraphData);
		assertEquals(barGraphData.getItems(), barGraphItemList);
		sessionUtil.resetSessionId();
	}

	@Test
	void testBarGraphDateYearBins() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("ICMR_Tests_Datewise.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Tested as of");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("bins");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("1 year");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Total positive cases");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("average");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		BarGraphData barGraphData = (BarGraphData) chatMessageResponse.getPayload().get("barGraphData");
		List<BarGraphItem> barGraphItemList = new ArrayList<>();
		barGraphItemList.add(new BarGraphItem("2020", 1205.420391025641));
		barGraphItemList.add(new BarGraphItem("UNKNOWN", 0.0));
		assertNotNull(barGraphData);
		assertEquals(barGraphData.getItems(), barGraphItemList);
		sessionUtil.resetSessionId();
	}

	@Test
	void testBarGraphFlowUniqueLabels() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Case_Time_Series.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("unique");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Daily confirmed");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		BarGraphData barGraphData = (BarGraphData) chatMessageResponse.getPayload().get("barGraphData");
		List<BarGraphItem> barGraphItemList = new ArrayList<>();
		barGraphItemList.add(new BarGraphItem("02 February ", 1.0));
		barGraphItemList.add(new BarGraphItem("31 January ", 0.0));
		barGraphItemList.add(new BarGraphItem("01 February ", 0.0));
		barGraphItemList.add(new BarGraphItem("03 February ", 1.0));
		barGraphItemList.add(new BarGraphItem("30 January ", 1.0));
		assertNotNull(barGraphData);
		assertTrue(barGraphData.getItems().containsAll(barGraphItemList));
		sessionUtil.resetSessionId();
	}

	@Test
	void testBarGraphWrongColumnName() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Population");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("unique");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Daily confirmed");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals("Population: " + IDAConst.BC_INVALID_COL, chatMessageResponse.getMessage());
		sessionUtil.resetSessionId();
	}

	@Test
	void testGroupedBarGraph() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("current status");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("current status");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("yes");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		GroupedBarGraphData groupedBarGraphData = (GroupedBarGraphData) chatMessageResponse.getPayload().get("barGraphData");
		Map<String, List<BarGraphItem>> groupedBarChartItems = new HashMap<>();
		groupedBarChartItems.put("Recovered", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 2.0));
			add(new BarGraphItem("Haryana", 12.0));
			add(new BarGraphItem("Kerala", 58.0));
			add(new BarGraphItem("Rajasthan", 3.0));
			add(new BarGraphItem("Telangana", 1.0));
		}});
		groupedBarChartItems.put("Deceased", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 1.0));
			add(new BarGraphItem("Haryana", 0.0));
			add(new BarGraphItem("Kerala", 3.0));
			add(new BarGraphItem("Rajasthan", 1.0));
			add(new BarGraphItem("Telangana", 1.0));
		}});
		groupedBarChartItems.put("Hospitalized", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 1999.0));
			add(new BarGraphItem("Haryana", 238.0));
			add(new BarGraphItem("Kerala", 341.0));
			add(new BarGraphItem("Rajasthan", 1474.0));
			add(new BarGraphItem("Telangana", 856.0));
		}});
		groupedBarChartItems.put("Migrated", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 1.0));
			add(new BarGraphItem("Haryana", 0.0));
			add(new BarGraphItem("Kerala", 0.0));
			add(new BarGraphItem("Rajasthan", 0.0));
			add(new BarGraphItem("Telangana", 0.0));
		}});
		assertNotNull(groupedBarGraphData);
		assertEquals(groupedBarGraphData.getGroupedBarChartData().keySet(), groupedBarChartItems.keySet());
		for(String key: groupedBarGraphData.getGroupedBarChartData().keySet()){
			assertTrue(groupedBarGraphData.getGroupedBarChartData().get(key).containsAll(groupedBarChartItems.get(key)));
		}
		sessionUtil.resetSessionId();
	}

	@Test
	void testGroupedBarGraphNumLabels() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Age Bracket");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("bin");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("20");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Age Bracket");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("count of");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("yes");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		GroupedBarGraphData groupedBarGraphData = (GroupedBarGraphData) chatMessageResponse.getPayload().get("barGraphData");
		Map<String, List<BarGraphItem>> groupedBarChartItems = new HashMap<>();
		groupedBarChartItems.put("40.0 - 59.0", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 3.0));
			add(new BarGraphItem("Kerala", 41.0));
			add(new BarGraphItem("Telangana", 10.0));
		}});
		groupedBarChartItems.put("20.0 - 39.0", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 9.0));
			add(new BarGraphItem("Kerala", 64.0));
			add(new BarGraphItem("Telangana", 17.0));
		}});
		groupedBarChartItems.put("0.0 - 19.0", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 0.0));
			add(new BarGraphItem("Kerala", 16.0));
			add(new BarGraphItem("Telangana", 2.0));
		}});
		groupedBarChartItems.put("60.0 - 79.0", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 1.0));
			add(new BarGraphItem("Kerala", 13.0));
			add(new BarGraphItem("Telangana", 4.0));
		}});
		groupedBarChartItems.put("80.0 - 99.0", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 0.0));
			add(new BarGraphItem("Kerala", 3.0));
			add(new BarGraphItem("Telangana", 0.0));
		}});
		groupedBarChartItems.put("UNKNOWN", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 1990.0));
			add(new BarGraphItem("Kerala", 265.0));
			add(new BarGraphItem("Telangana", 825.0));
		}});
		assertNotNull(groupedBarGraphData);
		assertEquals(groupedBarGraphData.getGroupedBarChartData().keySet(), groupedBarChartItems.keySet());
		for(String key: groupedBarChartItems.keySet()){
			assertTrue(groupedBarGraphData.getGroupedBarChartData().get(key).containsAll(groupedBarChartItems.get(key)));
		}
		sessionUtil.resetSessionId();
	}

	@Test
	void testGroupedBarGraphDateLabels() throws Exception {
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date Announced");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("bin");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("4 weeks");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Date Announced");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("yes");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Detected State");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		GroupedBarGraphData groupedBarGraphData = (GroupedBarGraphData) chatMessageResponse.getPayload().get("barGraphData");
		Map<String, List<BarGraphItem>> groupedBarChartItems = new HashMap<>();
		groupedBarChartItems.put("22-03-20 to 18-04-20", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 1866.0));
			add(new BarGraphItem("Haryana", 211.0));
			add(new BarGraphItem("Kerala", 348.0));
			add(new BarGraphItem("Rajasthan", 1327.0));
			add(new BarGraphItem("Telangana", 788.0));
			add(new BarGraphItem("Uttar Pradesh", 951.0));
		}});
		groupedBarChartItems.put("26-01-20 to 22-02-20", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 0.0));
			add(new BarGraphItem("Haryana", 0.0));
			add(new BarGraphItem("Kerala", 3.0));
			add(new BarGraphItem("Rajasthan", 0.0));
			add(new BarGraphItem("Telangana", 0.0));
			add(new BarGraphItem("Uttar Pradesh", 0.0));
		}});
		groupedBarChartItems.put("23-02-20 to 21-03-20", new ArrayList<>(){{
			add(new BarGraphItem("Delhi", 27.0));
			add(new BarGraphItem("Haryana", 21.0));
			add(new BarGraphItem("Kerala", 49.0));
			add(new BarGraphItem("Rajasthan", 24.0));
			add(new BarGraphItem("Telangana", 21.0));
			add(new BarGraphItem("Uttar Pradesh", 28.0));
		}});
		assertNotNull(groupedBarGraphData);
		assertEquals(groupedBarChartItems.keySet(), groupedBarGraphData.getGroupedBarChartData().keySet());
		for(String key: groupedBarChartItems.keySet()){
			assertTrue(groupedBarGraphData.getGroupedBarChartData().get(key).containsAll(groupedBarChartItems.get(key)));
		}
		sessionUtil.resetSessionId();
	}
}
