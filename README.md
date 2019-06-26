#  RAPID (Relation extrAction using Pattern generation and semantIc embeDdings)

RAPID aims to determine dbo relations from a given natural language text, and tries to disambiguate between multiple relations by means pattern generation and semantic embeddings.

## Resources Required
### External Libraries

 - JWI 2.4.0 WordNet ([https://projects.csail.mit.edu/jwi/](https://projects.csail.mit.edu/jwi/))
 - Stanford CoreNLP Models ([https://stanfordnlp.github.io/CoreNLP/download.html](https://stanfordnlp.github.io/CoreNLP/download.html)):
-- stanford-corenlp-3.9.1-models
-- stanford-english-corenlp-2018-02-27-models
-- stanford-english-kbp-corenlp-2018-02-27-models

### External Data
- Word2Vec Model (https://drive.google.com/file/d/0B7XkCwpI5KDYNlNUTTlSS21pQmM/)
- Glove Model (http://nlp.stanford.edu/data/glove.840B.300d.zip)
- fastText Model (https://dl.fbaipublicfiles.com/fasttext/vectors-english/wiki-news-300d-1M.vec.zip)
- WordNet 3.0 ([https://wordnet.princeton.edu/download](https://wordnet.princeton.edu/download))

### Remaining Data

 - All other data can be found at "[data](https://github.com/danishahmed92/RAPID/tree/master/data "data")"

### Installation Guide

 1. Import "[evaluation_all_embed_threshold.sql](https://github.com/danishahmed92/RAPID/blob/master/data/db/evaluation_all_embed_threshold.sql "evaluation_all_embed_threshold.sql")" MySQL database.
 2. Set the required database configuration, and data paths in "[dbConfig.ini](https://github.com/danishahmed92/RAPID/blob/master/rapid/src/main/resources/dbConfig.ini "dbConfig.ini")" and "[systemConfig.ini](https://github.com/danishahmed92/RAPID/blob/master/rapid/src/main/resources/systemConfig.ini "systemConfig.ini")" respectively.
 3. Execute "[Application.java](https://github.com/danishahmed92/RAPID/blob/master/rapid/src/main/java/rapid/service/Application.java "Application.java")" to start REST API service.
 4. To run Web-App demo, run command "*node app.js*" from directory "[rapid-webapp](https://github.com/danishahmed92/RAPID/tree/master/rapid-webapp "rapid-webapp")"
	 4.1 Make sure the REST API is already executing. 

## System Requirements

 1. Tomcat 8
 2. MySQL 5.1.39
 3. NodeJS 8.10.0
 4. At least 18 GB of ram (Since all models of Word2Vec, Glove, and fastText are loaded simultaneously along with their respective Property Embedding Model - We used 32 GB of ram)

## Useful Readings

 1. [Theoretical Document](https://github.com/danishahmed92/RAPID/blob/master/document/thesis.pdf "thesis.pdf")
 2. [Brief Overview](https://github.com/danishahmed92/RAPID/blob/master/Thesis_Defense_Final.pdf "Thesis_Defense_Final.pdf")
 3. [RAPID In Action](https://github.com/danishahmed92/RAPID/tree/master/demo "demo")
 4. [Contact Person](mailto:danish.ahmed92@live.com "Danish Ahmed")

## Credits

 - Supervised by [Dr. Ricardo Usbeck](https://github.com/RicardoUsbeck)
 - Submitted to [Prof. Dr. Axel-Cyrille Ngonga Ngomo](https://dice.cs.uni-paderborn.de/team/profiles/ngonga/)
