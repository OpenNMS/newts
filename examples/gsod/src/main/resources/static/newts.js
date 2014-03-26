
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
                column.timestamp * 1000,
                column.value
            ]);
        }
    }
    
    return transformed;
};
