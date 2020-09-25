import React, { useState } from "react";
import { makeStyles } from "@material-ui/core/styles";
import Grid from "@material-ui/core/Grid";
import ChatBot from "./chatbot/chatBot";
import IDANavbar from "./navbar/navbar";
import TabsWrappedLabel from "./tabs/tabs";
import AppBar from "@material-ui/core/AppBar";
import Toolbar from "@material-ui/core/Toolbar";
import CssBaseline from "@material-ui/core/CssBaseline";
import ChevronLeftIcon from "@material-ui/icons/ChevronLeft";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";
import ChatIcon from '@material-ui/icons/Chat';
import SpeakerNotesOffIcon from '@material-ui/icons/SpeakerNotesOff';
import { Fab, Typography } from "@material-ui/core";

import "./home.css";

const useStyles = makeStyles((theme) => ({
  menuButton: {
    marginRight: theme.spacing(2),
  },
  title: {
    flexGrow: 1,
  },
  chatbotToggleIcon: {
    color: "#4e8cff",
    backgroundColor: "#fff"
  }
}));

export default function Home(props) {
  const classes = useStyles();
  const [detail, setDetails] = useState([]);
  const [loaded, setLoaded] = useState(false);
  const [selectedNodeId, setSelectedNodeId] = useState("");
  const [expandedNodeId, setExpandedNodeId] = useState([]);
  const [activeDS, setActiveDS] = useState("");
  const [activeTable, setActiveTable] = useState("");
  const [tabs, setTabs] = useState([]);
  const [isChatbotOpen, setIsChatbotOpen] = useState(true);
  const loadTab = (loaded) => {
    if (loaded && tabs.length) {
      return <TabsWrappedLabel
        loaded={loaded}
        detail={detail}
        selectedNodeId={selectedNodeId}
        setSelectedNodeId={setSelectedNodeId}
        setActiveTable={setActiveTable}
        tabs={tabs}
        setTabs={setTabs}
      />
    }
  }
  const [navBarVisiblity, setNavBarVisiblity] = useState(true);
  const [navBarClass, setNavBarClass] = useState("no-navbar");
  const toggleNavBar = () => {
    if (!navBarVisiblity) {
      setNavBarClass("");
    } else {
      setNavBarClass("navbar-hidden");
    }
    setNavBarVisiblity(!navBarVisiblity);
  }
  const toggleChatWindow = () => {
    setIsChatbotOpen(!isChatbotOpen);
  }
  return (
    <>
      <CssBaseline />
      <AppBar>
        <Toolbar>
          <Typography variant="h6" className={classes.title} align="center">
            Intelligent Data Assistant
          </Typography>
          <Fab size="small" color="default" aria-label="toggle" className={classes.chatbotToggleIcon} onClick={toggleChatWindow}>
            {
              isChatbotOpen ? <SpeakerNotesOffIcon /> : <ChatIcon />
            }
          </Fab>
        </Toolbar>
      </AppBar>
      <Toolbar />
      <div className={navBarClass}>
        <Grid container>
          <Grid item className={"nav-bar-container"}>
            <div className={"navbar"}>
              <IDANavbar
                loaded={loaded}
                selectedNodeId={selectedNodeId}
                setSelectedNodeId={setSelectedNodeId}
                expandedNodeId={expandedNodeId}
                setExpandedNodeId={setExpandedNodeId}
                detail={detail}
                setActiveDS={setActiveDS}
                setActiveTable={setActiveTable}
                tabs={tabs}
                setTabs={setTabs}
              />
            </div>
            <Fab size="small" color="primary" aria-label="toggle" className={"navbar-toggle-icon"} onClick={toggleNavBar}>
              {
                loaded && (navBarVisiblity ? <ChevronLeftIcon /> : <ChevronRightIcon />)
              }
            </Fab>
          </Grid>
          <Grid item className={"content"}>
            {loadTab(loaded)}
          </Grid>
        </Grid>
        <ChatBot
          setDetails={setDetails}
          setSelectedNodeId={setSelectedNodeId}
          detail={detail}
          expandedNodeId={expandedNodeId}
          setExpandedNodeId={setExpandedNodeId}
          setLoaded={setLoaded}
          activeDS={activeDS}
          activeTable={activeTable}
          setActiveDS={setActiveDS}
          setActiveTable={setActiveTable}
          tabs={tabs}
          setTabs={setTabs}
          setNavBarClass={setNavBarClass}
          isChatbotOpen={isChatbotOpen}
          setIsChatbotOpen={setIsChatbotOpen}
        />
      </div>
    </>
  );
}
