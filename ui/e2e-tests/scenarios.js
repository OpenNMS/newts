'use strict';

/* https://github.com/angular/protractor/blob/master/docs/toc.md */

describe('newts', function() {
    
    browser.get('index.html');

    it('should have a (hidden) graph-builder', function() {
        var builders, outer;

        builders = element.all(by.tagName('graph-builder'));
        
        // There should be exactly one <graph-builder />
        expect(builders.count()).toBe(1);

        // First (outer) <div /> of <graph-builder />
        outer = builders.first().all(by.tagName('div')).first();

        // Ensure that the outer div is hidden
        outer.getAttribute('class').then(assertHidden);

        function assertHidden(classValue) {
            expect(classValue).toContain('ng-hide');
        }
    });
    
    it('should search resources', function() {
        doSearch(function() {
            expect(element.all(by.repeater('resource in vm.resources')).count()).toBeGreaterThan(0);
        });
    });

    it('should graph', function() {
        selectMetrics(function() {
            var inputs, buttons;

            // The graph-builder should no longer by hidden.
            expect(element(by.css('graph-builder')).all(by.css('.ng-hide')).count()).toBe(0);

            // ...but the graph div should still be empty
            expect(element(by.css(':scope #graph')).all(by.tagName('canvas')).count()).toBe(0);

            inputs = element(by.css(':scope #graphConfigListGroup')).all(by.tagName('input'));

            inputs.get(0).sendKeys('Tue Jan 12 2015 10:00:00 GMT-0600 (CST)');
            inputs.get(1).sendKeys('Tue Jan 13 2015 10:00:00 GMT-0600 (CST)');
            inputs.get(2).sendKeys('15m');
            inputs.get(3).sendKeys('5m');
            inputs.get(4).sendKeys('label0');
            inputs.get(6).sendKeys('10m');
            inputs.get(7).sendKeys('label1');
            inputs.get(9).sendKeys('10m');
            inputs.get(10).sendKeys('label2');
            inputs.get(12).sendKeys('10m');

            buttons = element(by.css(':scope #graphConfigListGroup')).all(by.tagName('button'));

            buttons.get(3).click().then(function() {
                // The graph div should now contain a canvas
                expect(element(by.css(':scope #graph')).all(by.tagName('canvas')).count()).toBeGreaterThan(0);
            });
        });
    });
    
    it('should restore previously created graph', function() {
        browser.refresh();
        doSearch(function() {
            selectMetrics(function() {
                expect(element(by.css(':scope #graph')).all(by.tagName('canvas')).count()).toBeGreaterThan(0);
            });
        });
    });
    
    function doSearch(func) {
        element(by.linkText('Search')).click().then(function() {
            // This probably isn't as bad as it seems; Clicking the search link brings up a modal
            // search window using a fade effect.  The sleep is there to give the effect time to
            // render the form input visible.
            browser.sleep(500);

            element(by.model('vm.query')).sendKeys('latency').submit().then(func);
        });
    }
    
    function selectMetrics(func) {
        var search, row, center, right;
        
        search = element(by.tagName('resource-search'));
        if (!search) console.log('err: unable to locate a <resource-search />');

        row = search.all(by.repeater('resource in vm.resources')).get(0);
        if (!row) console.log('err: unable to locate any resources (did you perform a search? is there data?)');

        // Get the center column; Multiselect-containing column
        center = row.all(by.css('td')).get(1);
        center.all(by.css('button')).first().click();

        center.all(by.repeater('option in options')).then(function(options) {
            options.forEach(function(option) {
                option.all(by.css('a')).first().click();
            });
        });

        // Get the right-most column; The submit button-containing column
        right = row.all(by.css('td')).get(2);
        right.all(by.css('button')).first().click().then(func);
    }

});
