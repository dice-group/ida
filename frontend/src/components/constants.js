export const IDA_CONSTANTS = {
	API_BASE: "/ida-ws",        // change it to http://localhost:8080 if not using docker
	PYDSMX_BASE: "/pydsmx", // change it to http://localhost:5000 if not using docker
	UI_ACTION_CODES: {
		UAC_NRMLMSG: 1001,
		UAC_UPLDDTMSG: 1002,
		UIA_LOADDS: 1004,
		UAC_BARGRAPH: 1005,
		UAC_BUBBLECHART: 1006,
		UAC_LINECHART: 1007,
		UAC_CLUSTERING: 1008,
		UAC_GROUPED_BARCHART: 1009,
		UAC_GROUPED_BUBBLECHART: 1010,
		UAC_SCATTERPLOT: 1011,
		UAC_SCATTERPLOTMATRIX: 1012,
		UAC_VIZ_SUGGESTION: 1013
	},
	BUTTON_ACTIONS: {
		PDAC_KNWMR: 2001,
		PDAC_UPLDDT: 2002,
		PDAC_VWDTS: 2003
	},
	UNKNOWN_LABEL: "UNKNOWN",
	ERROR_MESSAGE: "Something went wrong. Please try again!",
	TIMEOUT_MESSAGE: "Sorry, it looks like that request has crossed the allowed time limit of 3 minutes. Please optimize your task.",
	GATEWAY_TIMEOUT_STATUS: 504,
	SORT_MODE_ASC_Y: "asc_y",
	SORT_MODE_DESC_Y: "desc_y",
	SORT_MODE_ASC_X: "asc_x",
	SORT_MODE_DESC_X: "desc_x",
	DATE_FORMATS: ["DD/MM/yyyy", "DD MMM", "MMM YYYY", "DD/MM/yyyy HH:mm:ss", "DD-MMM-yyyy", "MMMM-yyyy", "YYYY"]
};
