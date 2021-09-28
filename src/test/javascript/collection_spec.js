let collectionScript = require('../../main/webapp/js/collection.js');

describe("Collection Javascript functions", function() {
    it("Expect string to not be an object", function() {
        expect(collectionScript.isObject("string")).toBe(false);
    });
});
