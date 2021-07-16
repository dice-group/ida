package org.dice.ida.action;

import org.dice.ida.controller.MessageController;
import org.dice.ida.model.ChatMessageResponse;
import org.dice.ida.model.ChatUserMessage;
import org.dice.ida.util.RDFUtil;
import org.dice.ida.util.SessionUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class UserHelpActionTest {
	@Autowired
	private MessageController messageController;
	private ChatUserMessage chatUserMessage;
	private ChatMessageResponse chatMessageResponse;
	@Autowired
	private SessionUtil sessionUtil;
	@Autowired
	private RDFUtil rdfUtil;
	private Map<String, String> userHelpMessageMap;

	@Test
	void testUserHelpMessage() throws Exception {
		userHelpMessageMap = rdfUtil.getUserHelpMessages();
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw bar graph");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("What is X-Axis?");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals(userHelpMessageMap.get("X-Axis"), chatMessageResponse.getMessage());
		sessionUtil.resetSessionId();
	}

	@Test
	void testUserHelpTopic() throws Exception {
		userHelpMessageMap = rdfUtil.getUserHelpMessages();
		chatUserMessage = new ChatUserMessage();
		chatUserMessage.setMessage("draw line chart");
		chatUserMessage.setActiveDS("test_dataset");
		chatUserMessage.setActiveTable("Patient_Data_Before_20-04-2020.csv");
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		chatUserMessage.setMessage("Give a hint");
		chatUserMessage.setChatbotMessage(chatMessageResponse.getMessage());
		chatMessageResponse = messageController.handleMessage(chatUserMessage).call();
		assertEquals(userHelpMessageMap.get("Temporal_Column"), chatMessageResponse.getMessage());
		sessionUtil.resetSessionId();
	}
}
