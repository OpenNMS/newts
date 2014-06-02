
function Url(urlString) {
    this.url = urlString;
    this.paramCount = 0;

    this.andParam = function(kw, parameter) {
        var sep = this.paramCount > 0 ? "&" : "?";

        if (parameter) {
            this.paramCount += 1;
            this.url = this.url + sep + kw + "=" + parameter;
        }

        return this;
    };

    this.toString = function() {
        return this.url;
    };

};

measurementsUrl = function(station, start, end, resolution) {
    return new Url("http://localhost:8080/measurements/gsod/"+station)
        .andParam("start", start)
        .andParam("end", end)
        .andParam("resolution", resolution)
        .toString();
};

summerUrl = function(station) {
    return measurementsUrl(station, "1988-05-01", "1988-08-01", "1w");
};

// Transform from Newts results to Flot data format.
transform = function(data) {
    var transformed = [], i, j, column, labels = {}, len;

    for (i=0; i < data.length; i++) {
        for (j=0; j < data[i].length; j++) {
            column = data[i][j];
            
            if (!labels.hasOwnProperty(column.name)) {
                len = transformed.push({"label": column.name, "data": []});
                labels[column.name] = len - 1
            }
            
            transformed[labels[column.name]].data.push([
                column.timestamp,
                column.value
            ]);
        }
    }
    
    return transformed;
};
