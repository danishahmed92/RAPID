var bodyParser = require("body-parser");
var path = require('path');
var debug = require('debug')('rapid-webapp:server');

var express = require("express");
var app = express();

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');

app.use(bodyParser.urlencoded({extended: false}));
app.use(express.static(path.join(__dirname, 'public')));

var router = express.Router();
app.get('/', function (req, res, next) {
    res.render('index', {title: 'RAPID - Relation extrAction using Pattern generation and semantIc embeDding'});
});

app.post('/api/re', function (req, res) {
    var reqContext = req.body.context;
    var reqAlpha = req.body.alpha;
    var reqBeta = req.body.beta;
    var reqEmbedding = req.body.embedding;

    console.log(reqContext);

    var request = require('request');

    request({
        url: "http://danish-thesis.cs.upb.de:8181/rapid/api/re/",
        method: "POST",
        headers: {
            "content-type": "multipart/form-data"
        },
        formData: {context: reqContext, alpha: reqAlpha, beta: reqBeta, embeddingClassifier: reqEmbedding}
    }, function (error, response, body) {
        console.log(response);
        res.send(response);
    });
});

app.listen(3000, function () {
    console.log("Started on http://localhost:3000");
});