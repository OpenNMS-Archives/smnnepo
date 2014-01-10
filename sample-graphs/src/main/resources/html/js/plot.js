

function queryParms(start, finish) {
    var parms;

    if (finish) {
        parms = "end=" + finish;
    }
    else {
        parms = "end=" + parseInt(new Date().getTime() / 1000);
    }

    if (start) {
        parms = parms + "&start=" + start;
    }

    return parms;
}

function plotSamples(agent, type, name, metrics, from_time, to_time) {
    var jsonObj, jsonUrl, parms, tstamp;

    if (agent || type || name || metrics) {
        if (!(agent && type && name && metrics)) {
            alert("you must supply all of agent, resource type/name, and metrics!");
            return;
        }
    }
    else {
        return;
    }

    parms = queryParms(from_time, to_time);

    jsonUrl = "/opennms/cxf/samples/"
        + agent + "/" + type + "/" + name + "/" + metrics + "?" + parms;

    document.querySelector("#chart_container").innerHTML = '<div id="y_axis"></div><div id="chart"></div><div id="legend"></div><div id="slider"></div>';

    jsonObj = new XMLHttpRequest();

    console.log("Querying: " + jsonUrl);

    jsonObj.open("GET", jsonUrl, true);
    jsonObj.onreadystatechange = function() {
        if (jsonObj.readyState === 4) {
            var elapsed = new Date().getTime() - tstamp.getTime();
            var data = JSON.parse(jsonObj.responseText);
            console.log("Complete: "+data.length+" samples; "+elapsed+" msecs");
            drawGraph(toSeries(data));
        }
    };

    tstamp = new Date();
    jsonObj.send(null);
}

function toSeries(data) {
    var mapped = new Array(), result = new Array();
    var palette = new Rickshaw.Color.Palette();
    
    if (!(data instanceof Array)) {
        console.log("unable coherce JSONP data to series");
        return undefined;
    }

    for (var i=0, len=data.length; i < len; i++) {
        var src, ts, val;

        if (!(data[i] instanceof Array)) {
            console.log("unrecognized sample format: '" + data[i] + ";");
            continue;
        }
 
        src = data[i][0], ts = data[i][1] / 1000, val = data[i][2];

        if (!(mapped.hasOwnProperty(src))) {
            mapped[src] = new Array();
        }
        mapped[src].push({ x: ts, y: val });
    }

    for (var key in mapped) {
        if (mapped.hasOwnProperty(key)) {
            result.push({ name: key, data: mapped[key], color: palette.color()});
        }
    }

    return result;
}

function drawGraph(series) {
    var graph = new Rickshaw.Graph({
        element: document.querySelector("#chart"),
        renderer: 'area',
        stroke: 'true',
        height: 240,
        width: 540,
        series: series
    });

    graph.render();

    var x_axis = new Rickshaw.Graph.Axis.Time({ graph: graph });

    x_axis.render();

    var y_axis = new Rickshaw.Graph.Axis.Y({
        graph: graph,
        orientation: 'left',
        tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
        element: document.getElementById('y_axis'),
    });

    y_axis.render();

    var slider = new Rickshaw.Graph.RangeSlider({
        graph: graph,
        element: [ document.querySelector('#slider') ]
    });
    
    var legend = new Rickshaw.Graph.Legend({
        element: document.querySelector('#legend'),
        graph: graph
    });

    var shelving = new Rickshaw.Graph.Behavior.Series.Toggle({
        graph: graph,
        legend: legend
    });

    var highlighter = new Rickshaw.Graph.Behavior.Series.Highlight({
        graph: graph,
        legend: legend
    });
}

plotSamples();