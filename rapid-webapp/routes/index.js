var express = require('express');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'RAPID - Relation extrAction using Pattern generation and semantIc embeDding' });
});

module.exports = router;
