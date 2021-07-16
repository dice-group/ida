package org.dice.ida.action;

import org.dice.ida.controller.MessageController;
import org.dice.ida.model.ChatMessageResponse;
import org.dice.ida.model.ChatUserMessage;
import org.dice.ida.model.scatterplot.ScatterPlotData;
import org.dice.ida.model.scatterplot.ScatterPlotItem;
import org.dice.ida.util.SessionUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ScatterPlotActionTest {
	@Autowired
	private MessageController messageController;
	@Autowired
	private SessionUtil sessionUtil;

	@Test
	void testScatterPlotFlow() throws Exception {
		ChatUserMessage chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw scatter plot");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("countries-of-the-world.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("density");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("gdp");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("country");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("no");
		ChatMessageResponse chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		ScatterPlotData scatterPlotData = (ScatterPlotData) chatMessageResponse.getPayload().get("scatterPlotData");
		List<ScatterPlotItem> scatterPlotItemList = new ArrayList<>() {{
			add(new ScatterPlotItem(48.0, 700.0, null, "Afghanistan "));
			add(new ScatterPlotItem(124.6, 4500.0, null, "Albania "));
			add(new ScatterPlotItem(13.8, 6000.0, null, "Algeria "));
			add(new ScatterPlotItem(290.4, 8000.0, null, "American Samoa "));
			add(new ScatterPlotItem(152.1, 19000.0, null, "Andorra "));
		}};
		assertNotNull(scatterPlotData);
		assertTrue(scatterPlotData.getItems().containsAll(scatterPlotItemList));
		sessionUtil.resetSessionId();
	}

	@Test
	void testScatterPlotReference() throws Exception {
		ChatUserMessage chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw scatter plot");
		chatUserMessage.setActiveDS("countries");
		chatUserMessage.setActiveTable("countries-of-the-world.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("density");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("gdp");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("area");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("yes");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("region");
		ChatMessageResponse chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		ScatterPlotData scatterPlotData = (ScatterPlotData) chatMessageResponse.getPayload().get("scatterPlotData");
		List<ScatterPlotItem> scatterPlotItemList = new ArrayList<>() {{
			add(new ScatterPlotItem(48.0, 700.0, "ASIA (EX. NEAR EAST)         ", "647500"));
			add(new ScatterPlotItem(124.6, 4500.0, "EASTERN EUROPE                     ", "28748"));
			add(new ScatterPlotItem(13.8, 6000.0, "NORTHERN AFRICA                    ", "2381740"));
			add(new ScatterPlotItem(290.4, 8000.0, "OCEANIA                            ", "199"));
			add(new ScatterPlotItem(152.1, 19000.0, "WESTERN EUROPE                     ", "468"));
		}};
		assertNotNull(scatterPlotData);
		assertTrue(scatterPlotData.getItems().containsAll(scatterPlotItemList));
		sessionUtil.resetSessionId();
	}
}
