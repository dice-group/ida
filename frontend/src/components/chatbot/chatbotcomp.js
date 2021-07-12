import React from "react";
import axios from "axios";
import "./chatbotcomp.css";
import { IDA_CONSTANTS } from "../constants";
import idaChatbotActionHandler from "../action-handler";
import IDALinearProgress from "../progress/progress";
import CloseIcon from "@material-ui/icons/Close";
import Draggable from "react-draggable";

export default class ChatApp extends React.Component {

	suggestionParams = null;
	sessionTimeOut = null;
	chatbotMessage = "";

	constructor(props) {
		super(props);
		this.state = {
			title: "IDA chatbot",
			iterator: -1,
			hideProgress: true,
			hideBot: false,
			messages: [{
				sender: "them",
				type: "text",
				key: 1,
				message: "Hello, Welcome to IDA. How may I help you?",
				timestamp: Date.now()

			}],
			changeCSS: {},
			selectClick: [],
			pyld: [],
			action: "1004",
			msg: [],
			textAreaDisable: false,
			timeOut: null,
			beingDragged: false
		};
	}


	showMessage(text, time) {
		this.setState({
			messages: [...this.state.messages, {
				sender: "them",
				type: "text",
				key: Math.random(),
				message: text,
				timestamp: time
			}]
		});
		this.chatbotMessage = text;
	}

	componentDidUpdate(_prevProps, _prevState) {
		const msgs = document.getElementById("chat-area-msgs");
		msgs.scrollTop = msgs.scrollHeight;
		if (this.props.isChatbotOpen) {
			document.getElementById("chat-input").focus();
		}
	}

	onSendClick = (e) => {
		this.suggestionParams = JSON.parse(e.currentTarget.getAttribute("data-params"));
		this.messageSend({
			keyCode: 13,
			target: document.getElementById("chat-input")
		});
	}

	messageSend = (e) => {
		let msgs = [...this.state.messages];
		const userMsgs = this.state.messages.filter((v) => v.sender === "user");

		/**
		 * Section to manage new message from the user
		 */
		if (e.keyCode === 13 && e.target.value.trim() !== "") {
			let msg = {
				sender: "user",
				message: e.target.value,
				key: Math.random(),
				timestamp: Date.now(),
				senderName: "user",
				activeDS: this.props.activeDS,
				activeTable: this.props.activeTable,
				activeTableData: this.props.activeTableData,
				temporaryData: !!this.props.activeTableData,
				suggestionParams: this.suggestionParams,
				renderSuggestion: !!this.suggestionParams,
				chatbotMessage: this.chatbotMessage
			};
			msgs = [...msgs, msg];
			this.setState({
				messages: msgs,
				hideProgress: false,
				iterator: msgs.filter((v) => v.sender === "user").length - 1
			});
			e.target.value = "";
			this.processMessage(msg);
		}
		this.state.iterator !== -1 && this.msgIterator(e, userMsgs);
		this.suggestionParams = null;

	}

	processMessage = (msg) => {
		if (this.sessionTimeOut) {
			clearTimeout(this.sessionTimeOut);
		}
		this.sessionTimeOut = setTimeout(() => {
			this.setState({
				timeOut: null,
				textAreaDisable: true,
				messages: [...this.state.messages, {
					sender: "them",
					timestamp: Date.now(),
					message: "You have been inactive with IDA for 30 minutes! Your session has been expired. Kindly reload the page."
				}]
			});
		}, 1000 * 60 * 30);
		axios.post(IDA_CONSTANTS.API_BASE + "/chatmessage", msg, { withCredentials: true, },)
			.then((response) => {
				this.showMessage(response.data.message, response.data.timestamp);
				const actionCode = response.data.uiAction;
				const payload = response.data.payload;
				this.props.setContexts(response.data.activeContexts || []);
				idaChatbotActionHandler(this.props, actionCode, payload);
			})
			.catch((err) => {
				if (err.response && err.response.status && err.response.status === IDA_CONSTANTS.GATEWAY_TIMEOUT_STATUS) {
					this.showMessage(IDA_CONSTANTS.TIMEOUT_MESSAGE, Date.now());
				} else {
					this.showMessage(IDA_CONSTANTS.ERROR_MESSAGE, Date.now());
				}
			}).finally(() => {
				this.setState({
					hideProgress: true
				});
			});
	}

	idaElementParser(msg) {
		var textArr = msg.trim().split(/(?=<ida.*?>)/);
		let processed = textArr;
		if (textArr.length > 1) {
			processed = processed.reduce((acc, cur, i) => {
				if (i === 1) {
					acc = acc.split(/(?<=<ida.*?>)/);
				}
				return acc.concat(cur.split(/(?<=<ida.*?>)/));
			});
		}

		processed = processed.map((token) => {
			if (token.trim().startsWith("<ida")) {
				const regex = /(\S+)=["']?((?:.(?!["']?\s+(?:\S+)=|\s*\/?[>"']))+.)["']?/g;
				const attrsExtract = token.match(regex);
				const eleExtract = token.match(/<([^\s>]+)(\s|>)+/)[1];

				let idaBtn = { name: eleExtract };

				attrsExtract.forEach((e) => {
					var attrs = e.split("=");
					idaBtn[attrs[0]] = attrs[1].replaceAll(/\'|\"/g, "");
				});

				return idaBtn;
			} else {
				return token;
			}
		});
		return processed instanceof Array ? processed : [processed];
	}

	idaElementRenderer(el) {
		const idaEles = {
			"ida-btn": "button"
		};

		return React.createElement(idaEles[el.name], {
			onClick: () => {
				this.messageSend({ keyCode: 13, target: { value: el.msg } });
			}, // mimicking message sent from input field
			className: el.style
		}, el.value);
	}

	msgIterator = (e, userMsgs) => {
		let target = e.target;
		/***
		 * Section to manage mesaages iteration
		 */
		// only update and iterate values if iterator has been updated i.e. user has send atleast one message
		if (e.keyCode === 38) {
			// up arrow key
			this.setState({
				iterator: this.state.iterator > 0 ? this.state.iterator - 1 : this.state.iterator,
			});
			target.value = userMsgs[this.state.iterator].message;
		} else if (e.keyCode === 40) {
			// down arrow key
			const iter = userMsgs.length - 1 > this.state.iterator ? this.state.iterator + 1 : this.state.iterator;
			this.setState({
				iterator: iter
			}, () => {
				target.value = userMsgs[this.state.iterator].message;
			});
		}
	}

	handlebutton = () => {
		this.props.setIsChatbotOpen(!this.props.isChatbotOpen);
	}

	dragStart = () => {
		this.setState({ beingDragged: true });
	}

	dragEnd = () => {
		this.setState({ beingDragged: false });
	}

	render() {
		return (
			<div
				className={`chatbox-container ${this.props.detail.length ? "with-data" : "no-data"} ${this.props.isChatbotOpen ? "" : "hidden"}`}>
				<Draggable handle=".draggable" bounds={"#root"} onStart={this.dragStart} onStop={this.dragEnd}>
					<div className={`chatbox ${this.state.beingDragged ? "drag" : ""}`} id="chatbox">
						<div className={`chatbox-title ${this.props.detail.length ? "draggable" : ""}`}>
							<div className="chat-window-title">
								{this.state.title}
							</div>
							<div>
								<CloseIcon onClick={this.handlebutton} className="chatbot-close" />
							</div>
						</div>
						<div className="chatbox-chat-area">
							<div className="chat-area-msgs" id="chat-area-msgs">
								{
									this.state.messages.map((val, i) => {
										if (val.sender === "user") {
											return (
												<div className="clearfix">
													<div className="user" key={i}>
														<div className="msg" key={Math.random()}> {val.message}</div>
														<div
															className="time">{new Date(val.timestamp).toLocaleTimeString()}</div>
													</div>
												</div>
											);
										} else {
											return (
												<div className="clearfix">
													<div className="agent" key={Math.random()}>
														<div>
															<div className="msg" key={Math.random()}>{
																this.idaElementParser(val.message).map((token) => {
																	if (token instanceof Object) {
																		return this.idaElementRenderer(token);
																	} else {
																		return <span
																			dangerouslySetInnerHTML={{ __html: token }} />;
																	}
																})
															}</div>
															<div
																className="time">{new Date(val.timestamp).toLocaleTimeString()}</div>
														</div>
														<div className="agent-pic" key={Math.random()} />
													</div>
												</div>
											);
										}
									})
								}
							</div>
						</div>
						<div className="chat-area-input clearfix">
							<IDALinearProgress hide={this.state.hideProgress} />
							<textarea id="chat-input" placeholder="Enter your message .." onKeyUp={this.messageSend} disabled={this.state.textAreaDisable} />
							<button hidden id="send-btn" onClick={this.onSendClick}></button>
						</div>
					</div>
				</Draggable>
			</div>

		);
	}
}

