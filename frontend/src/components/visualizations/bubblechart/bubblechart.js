import React, { Component } from 'react';
import * as d3 from "d3";

export default class IDABubbleGraph extends Component {
  height = 700;
  width = 1000;
  graphData = {};
  containerId = "";

  constructor(props) {
    super(props);
    this.containerId = props.nodeId;
    this.graphData = props.data;
  }

  componentDidMount() {
    this.graphData && this.graphData.items && this.drawGraph();
  }

  drawGraph() {
    const data = this.graphData.items;
    if (data && data.length) {
      const pack = data => d3.pack()
        .size([this.width - 2, this.height - 2])
        .padding(3)
        (d3.hierarchy({ children: data })
          .sum(d => d.size))
      const root = pack(data);
      const svg = d3.select("#" + this.containerId)
        .append("svg")
        .attr("height", this.height)
        .attr("width", this.width);

      const entry = svg.selectAll("g")
        .data(root.leaves())
        .join("g")
        .attr("transform", d => `translate(${d.x + 1},${d.y + 1})`);

      entry.append("circle")
        .attr("id", (d, i) => "clip" + i)
        .attr("r", d => d.r)
        .attr("fill", d => "#4f8bff");

      entry.append("text")
        .attr("dy", ".2em")
        .style("text-anchor", "middle")
        .text((d) => d.data.label)
        .attr("font-family", "sans-serif")
        .attr("font-size", (d) => d.r / 5)
        .attr("fill", "white");

      entry.append("title")
        .text(d => (d.data.description + ':  ' + d.value));
    }
  }
  render() {
    return <div className="bubblechart-container" id={this.containerId} ></div >;
  }

}