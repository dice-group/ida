package org.dice.ida.action;

import org.dice.ida.controller.MessageController;
import org.dice.ida.model.ChatMessageResponse;
import org.dice.ida.model.ChatUserMessage;
import org.dice.ida.model.scatterplotmatrix.ScatterPlotMatrixData;
import org.dice.ida.util.SessionUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class ScatterPlotMatrixActionTest {
	@Autowired
	private MessageController messageController;
	@Autowired
	private SessionUtil sessionUtil;
	private ChatMessageResponse chatMessageResponse;

	@Test
	void testScatterPlotMatrixFlow() throws Exception {
		ChatUserMessage chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw scatter plot matrix");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("countries-of-the-world.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("country, density, gdp, literacy");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("region");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		ScatterPlotMatrixData actualData = (ScatterPlotMatrixData) chatMessageResponse.getPayload().get("scatterPlotMatrixData");
		ScatterPlotMatrixData expectedData = new ScatterPlotMatrixData();
		expectedData.setColumns(new ArrayList<>() {{
			add("Pop. Density (per sq. mi.)");
			add("GDP ($ per capita)");
			add("Literacy (%)");
		}});
		expectedData.setLabelColumn("Region");
		expectedData.setItems(new ArrayList<>() {{
			add(new HashMap<>() {{
				put("Literacy (%)", "36.0");
				put("GDP ($ per capita)", "700");
				put("Region", "ASIA (EX. NEAR EAST)         ");
				put("Pop. Density (per sq. mi.)", "48.0");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "4500");
				put("Literacy (%)", "86.5");
				put("Pop. Density (per sq. mi.)", "124.6");
				put("Region", "EASTERN EUROPE                     ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "6000");
				put("Literacy (%)", "70.0");
				put("Pop. Density (per sq. mi.)", "13.8");
				put("Region", "NORTHERN AFRICA                    ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "8000");
				put("Literacy (%)", "97.0");
				put("Pop. Density (per sq. mi.)", "290.4");
				put("Region", "OCEANIA                            ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "19000");
				put("Literacy (%)", "100.0");
				put("Pop. Density (per sq. mi.)", "152.1");
				put("Region", "WESTERN EUROPE                     ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "1900");
				put("Literacy (%)", "42.0");
				put("Pop. Density (per sq. mi.)", "9.7");
				put("Region", "SUB-SAHARAN AFRICA                 ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "8600");
				put("Literacy (%)", "95.0");
				put("Pop. Density (per sq. mi.)", "132.1");
				put("Region", "LATIN AMER. & CARIB    ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "11000");
				put("Literacy (%)", "89.0");
				put("Pop. Density (per sq. mi.)", "156.0");
				put("Region", "LATIN AMER. & CARIB    ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "11200");
				put("Literacy (%)", "97.1");
				put("Pop. Density (per sq. mi.)", "14.4");
				put("Region", "LATIN AMER. & CARIB    ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "3500");
				put("Literacy (%)", "98.6");
				put("Pop. Density (per sq. mi.)", "99.9");
				put("Region", "C.W. OF IND. STATES ");
			}});
		}});
		assertNotNull(actualData);
		assertEquals(expectedData.getLabelColumn(), actualData.getLabelColumn());
		assertEquals(expectedData.getColumns(), actualData.getColumns());
		assertTrue(actualData.getItems().containsAll(expectedData.getItems()));
		sessionUtil.resetSessionId();
	}

	@Test
	void testLabelledScatterPlotMatrix() throws Exception {
		ChatUserMessage chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw scatter plot matrix");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("countries-of-the-world.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("country, density, gdp, literacy");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("country");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("yes");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("region");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		ScatterPlotMatrixData actualData = (ScatterPlotMatrixData) chatMessageResponse.getPayload().get("scatterPlotMatrixData");
		ScatterPlotMatrixData expectedData = new ScatterPlotMatrixData();
		expectedData.setColumns(new ArrayList<>() {{
			add("Pop. Density (per sq. mi.)");
			add("GDP ($ per capita)");
			add("Literacy (%)");
		}});
		expectedData.setReferenceColumn("Region");
		expectedData.setLabelColumn("Country");
		expectedData.setItems(new ArrayList<>() {{
			add(new HashMap<>() {{
				put("Literacy (%)", "36.0");
				put("GDP ($ per capita)", "700");
				put("Region", "ASIA (EX. NEAR EAST)         ");
				put("Pop. Density (per sq. mi.)", "48.0");
				put("Country", "Afghanistan ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "4500");
				put("Literacy (%)", "86.5");
				put("Pop. Density (per sq. mi.)", "124.6");
				put("Region", "EASTERN EUROPE                     ");
				put("Country", "Albania ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "6000");
				put("Literacy (%)", "70.0");
				put("Pop. Density (per sq. mi.)", "13.8");
				put("Region", "NORTHERN AFRICA                    ");
				put("Country", "Algeria ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "8000");
				put("Literacy (%)", "97.0");
				put("Pop. Density (per sq. mi.)", "290.4");
				put("Region", "OCEANIA                            ");
				put("Country", "American Samoa ");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "19000");
				put("Literacy (%)", "100.0");
				put("Pop. Density (per sq. mi.)", "152.1");
				put("Region", "WESTERN EUROPE                     ");
				put("Country", "Andorra ");
			}});
		}});
		assertNotNull(actualData);
		assertEquals(expectedData.getLabelColumn(), actualData.getLabelColumn());
		assertEquals(expectedData.getReferenceColumn(), actualData.getReferenceColumn());
		assertEquals(expectedData.getColumns(), actualData.getColumns());
		assertTrue(actualData.getItems().containsAll(expectedData.getItems()));
		sessionUtil.resetSessionId();
	}

	@Test
	void testScatterPlotMatrixAllColumns() throws Exception {
		ChatUserMessage chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw scatter plot matrix");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("all");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("detected state");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		ScatterPlotMatrixData actualData = (ScatterPlotMatrixData) chatMessageResponse.getPayload().get("scatterPlotMatrixData");
		ScatterPlotMatrixData expectedData = new ScatterPlotMatrixData();
		expectedData.setColumns(new ArrayList<>() {{
			add("Patient Number");
			add("Age Bracket");
		}});
		expectedData.setLabelColumn("Detected State");
		expectedData.setItems(new ArrayList<>() {{
			add(new HashMap<>() {{
				put("Age Bracket", "20");
				put("Detected State", "Kerala");
				put("Patient Number", "1");
			}});
			add(new HashMap<>() {{
				put("Age Bracket", "UNKNOWN");
				put("Detected State", "Kerala");
				put("Patient Number", "2");
			}});
			add(new HashMap<>() {{
				put("Age Bracket", "UNKNOWN");
				put("Detected State", "Kerala");
				put("Patient Number", "3");
			}});
			add(new HashMap<>() {{
				put("Age Bracket", "45");
				put("Detected State", "Delhi");
				put("Patient Number", "4");
			}});
			add(new HashMap<>() {{
				put("Age Bracket", "24");
				put("Detected State", "Telangana");
				put("Patient Number", "5");
			}});
		}});
		assertNotNull(actualData);
		assertEquals(expectedData.getLabelColumn(), actualData.getLabelColumn());
		assertEquals(expectedData.getColumns(), actualData.getColumns());
		assertTrue(actualData.getItems().containsAll(expectedData.getItems()));
		sessionUtil.resetSessionId();
	}

	@Test
	void testScatterPlotMatrixNonNumericColumn() throws Exception {
		ChatUserMessage chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw scatter plot matrix");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("countries-of-the-world.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("region, density");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals("Please provide more than one Numeric columns", chatMessageResponse.getMessage());
		sessionUtil.resetSessionId();
	}

	@Test
	void testClusterScatterPlotMatrix() throws Exception {
		ChatUserMessage chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("cluster");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("countries-of-the-world.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("population, population density");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("kmeans");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setActiveTableData((List<Map<String, String>>) chatMessageResponse.getPayload().get("clusteredData"));
		chatUserMessage.setTemporaryData(true);
		chatUserMessage.setMessage("draw scatter plot matrix");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("density, gdp, literacy");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("cluster");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		ScatterPlotMatrixData actualData = (ScatterPlotMatrixData) chatMessageResponse.getPayload().get("scatterPlotMatrixData");
		ScatterPlotMatrixData expectedData = new ScatterPlotMatrixData();
		expectedData.setColumns(new ArrayList<>() {{
			add("Pop. Density (per sq. mi.)");
			add("GDP ($ per capita)");
			add("Literacy (%)");
		}});
		expectedData.setLabelColumn("Cluster");
		expectedData.setItems(new ArrayList<>() {{
			add(new HashMap<>() {{
				put("Literacy (%)", "36.0");
				put("GDP ($ per capita)", "700");
				put("Cluster", "3");
				put("Pop. Density (per sq. mi.)", "48.0");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "4500");
				put("Literacy (%)", "86.5");
				put("Pop. Density (per sq. mi.)", "124.6");
				put("Cluster", "0");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "6000");
				put("Literacy (%)", "70.0");
				put("Pop. Density (per sq. mi.)", "13.8");
				put("Cluster", "3");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "8000");
				put("Literacy (%)", "97.0");
				put("Pop. Density (per sq. mi.)", "290.4");
				put("Cluster", "5");
			}});
			add(new HashMap<>() {{
				put("GDP ($ per capita)", "19000");
				put("Literacy (%)", "100.0");
				put("Pop. Density (per sq. mi.)", "152.1");
				put("Cluster", "0");
			}});

		}});
		assertNotNull(actualData);
		assertEquals(expectedData.getLabelColumn(), actualData.getLabelColumn());
		assertEquals(expectedData.getColumns(), actualData.getColumns());
		assertTrue(actualData.getItems().containsAll(expectedData.getItems()));
		sessionUtil.resetSessionId();
	}
}
