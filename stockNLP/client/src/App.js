import React, { Component } from 'react';
import {Row, Col} from 'react-flexbox-grid';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      date: "",
      n:"",
      stockName:"",
      dates:"",
      queryString:"",
      winSize:"",
      err:"",
      // results
      toDisplay: 0,
      data: [],
      // state regulation
    }
    this.handleInput = this.handleInput.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleDisplay= this.handleDisplay.bind(this);
  }

  handleInput(e) {
    this.setState({[e.target.name]: e.target.value});
  }

  handleSubmit(e, endPoint, field1, value1, field2, value2) {
    e.preventDefault();
    fetch("http://localhost:9090/"+endPoint+"?"+field1+"="+value1+"?"+field2+"="+value2, {
      method: "GET",
      headers: {
        "Accept": "application/json"
      }
    })
    .then(res => res.json())
    .then(res => {
      this.setState({[field1]:"",[field2]:""})
      this.handleDisplay(endPoint, res);
    }).catch(err => {
      console.log(err)
      this.setState({err: err.message})
    })
  }

  handleDisplay(endPoint, data) {
    switch (endPoint) {
      case "popWords":
        this.setState({toDisplay: 0, data: data})
        break;
      case "queryStock":
        this.setState({toDisplay: 1, data: JSON.parse(data.docs)})
        break;
      case "query":
        this.setState({toDisplay: 1, data: data})
        break;
      default:
        break;
    }

  }

  render() {
    return (
      <div className="App">
        <header className="App-header">
          <h1 className="App-title">StockNLP User Interface</h1>
        </header>
        <p style={{color:"red"}}>{this.state.err}</p>
        <Row>
          <Col md={2} style={{"paddingLeft":"20px"}}>
            <form>
              Search for popular words: 
              <input style={{"display":"block", "margin": "5px 0 0 0"}} type="text" name="date" value={this.state.date} placeholder="Date: eg 07-30" onChange={this.handleInput}/>
              <input style={{"display":"block", "margin": "5px 0 0 0"}} type="text" name="n" value={this.state.n} placeholder="N: eg 10" onChange={this.handleInput}/>
              <button style={{"display":"block", "margin": "5px 0 0 0"}} onClick={e => this.handleSubmit(e, "popWords", "date", this.state.date, "n", this.state.n)}>search</button>
            </form>
            <form style={{"margin": "20px 0 0 0"}}>
              Query Stock: 
              <input style={{"display":"block", "margin": "5px 0 0 0"}} type="text" name="stockName" value={this.state.stockName} placeholder="Stock Name:" onChange={this.handleInput}/>
              <input style={{"display":"block", "margin": "5px 0 0 0"}} type="text" name="dates" value={this.state.dates} placeholder="Dates: eg 07-30,07-29" onChange={this.handleInput}/>
              <button style={{"display":"block", "margin": "5px 0 0 0"}} onClick={e => this.handleSubmit(e, "queryStock", "stockName", this.state.stockName, "dates", this.state.dates)}>query</button>
            </form>
            <form style={{"margin": "20px 0 0 0"}}>
              General query: 
              <input style={{"display":"block", "margin": "5px 0 0 0"}} type="text" name="queryString" value={this.state.queryString} placeholder="Query String:" onChange={this.handleInput}/>
              <input style={{"display":"block", "margin": "5px 0 0 0"}} type="text" name="winSize" value={this.state.winSize} placeholder="Window Length: eg 3" onChange={this.handleInput}/>
              <button style={{"display":"block", "margin": "5px 0 0 0"}} onClick={e => this.handleSubmit(e, "query", "queryString", this.state.queryString, "winSize", this.state.winSize)}>query</button>
            </form>
          </Col>
          <Col md={10}>
            {this.state.toDisplay === 0 ? <DisplayTerms collections={this.state.data}/> : <DisplayDocs collections={this.state.data}/>}
          </Col>
        </Row>
      </div>
    );
  }
}

function DisplayDocs(props) {
  // props.docs should be a json string that represents an array of docs (url, title, snippet)
  // props.pos/neg are optional
  if (props.collections === null) {
    return null;
  }
  var rows = [];
  for (var i in props.collections) {
    var doc = props.collections[i]
    if (doc != null) {
      rows.push(<DisplayDoc url={doc.url} title={doc.title} snippet={doc.snippet} score={doc.score}/>)
    }
  }
  return (
    <div style={{listStyleType: 'none', height: '700px', 'overflow-y': "scroll"}}>
      {rows}
    </div>
  )
}

function DisplayDoc(props) {
  // props should be a json with fields of url, title, snippet
  return (
    <div>
      <a href={props.url} style={{"color":"blue"}}>{props.title}</a> {props.score}
      <p>{props.snippet}</p>
    </div>
  )
}

function DisplayTerms(props) {
  // props.collections should be a list of term-df json pairs
  var rows = [];
  for (var i in props.collections) {
    var pair = props.collections[i]
    rows.push(<li>{pair.term} : {pair.df}</li>);
  }

  return (
    <div style={{listStyleType: 'none', height: '700px', 'overflow-y': "scroll"}}>
      {rows}
    </div>
  )
}

export default App;
