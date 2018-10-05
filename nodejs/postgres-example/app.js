
/**
 * The Review App - showcasing Cloud Foundry Backing Services on SAP Cloud Platform
 */

// Set Express app parameters
var http  = require('http');
var express = require('express'); 
var bodyParser = require('body-parser');
var app = express();
app.listen(8080, () => console.log('Example app listening on port 8080!'))
app.get('/index.html', (req, res) => res.send('Hello World!'))
app.use(bodyParser.json()); // support json encoded bodies
app.use(bodyParser.urlencoded({ extended: true })); // support encoded bodies

//ui rendering
var statics = require('serve-static');
var path = require('path');
app.set('views', __dirname + '/views');
app.set('view engine', 'jade');
app.use(statics(path.join(__dirname, 'public')));

/*POSTGRESQL*/
var pg = require("pg"); //Module for PostgreSQL connection
var connString = "postgres://postgres:Abcd_1234@localhost:5432/postgres";
var client = new pg.Client(connString);
client.connect(function(err) {
	if(err) {
		return console.error('could not connect to postgres', err);
	}
});

/* ---------------------Data Loading----------------------------- */
//Create Product List Table
/*
function init_product_details_table() {
		client.query("DROP TABLE IF EXISTS product_reviews");
		client.query("DROP TABLE IF EXISTS product_details");
		client.query("CREATE TABLE IF NOT EXISTS product_details(productid varchar(50) primary key, productdesc varchar(200), productname varchar(200))");
		client.query("INSERT INTO product_details(productname, productdesc, productid) values($1, $2, $3)", ['Power_Table', 'Power Table 20', 'FR-321111']);
		client.query("INSERT INTO product_details(productname, productdesc, productid) values($1, $2, $3)", ['Shiny_Chair', 'Shiny_Chair 2500', 'FR-789122']);
		client.query("INSERT INTO product_details(productname, productdesc, productid) values($1, $2, $3)", ['Fast_Light', 'Fast_Light 7', 'FR-567134']);
	}
init_product_details_table();
*/

//Create Review Table
function init_product_reviews_table() {
		client.query("DROP TABLE IF EXISTS product_reviews");
		client.query("CREATE TABLE IF NOT EXISTS product_reviews(name varchar(100),email varchar(50) ,pid varchar(50) REFERENCES product_details(productid), review varchar(200), rating real ,CONSTRAINT PK_Review PRIMARY KEY (email,pid))");
		//client.query("INSERT INTO product_reviews(name, email, pid, review, rating) values($1, $2, $3, $4,$5)", ['rahul','rahul@sap.com', 'FR-321111', 'good', 4]);
		//client.query("INSERT INTO product_reviews(name, email, pid, review, rating) values($1, $2, $3, $4,$5)", ['rahul','rahul@sap.com', 'FR-567134', 'bad', 1]);
		//client.query("INSERT INTO product_reviews(name, email, pid, review, rating) values($1, $2, $3, $4,$5)", ['gaurav','gaurv@abc.com', 'FR-321111', 'bad', 2]);		
}
init_product_reviews_table();

//=====================================================
// AMQP 
//=====================================================

var amqp = require('amqplib/callback_api'); //Module for RabbitMQ connection
var rabbitUrl = "amqp://BWv7xnFxmp5DksG2:VgE7R_oyPSdPbj1V@10.11.241.39:47472"; //Retrieve the URL for RabbitMQ
//var tweet = require( './services/tweet-review'); //Import the service to send out tweets

function postTweet(message) {  
    console.log("Entering function posttweet");
    console.log("This is rabbiturl  %s", rabbitUrl);
	amqp.connect(rabbitUrl, function(err, connect) {
		if(err){
			console.log("failed to connect "+err)	
		}
 		if (connect) {     
			console.log("connected")	
			connect.createChannel(function(err, channel) {
				var queue = 'reviews';
				channel.assertQueue(queue, {durable: false});
				channel.sendToQueue(queue, new Buffer(message));
				console.log(" [x] Sent %s", message);
				setTimeout(function() {connect.close();}, 1000); // Wait
			});
        }
     });
}


//=====================================================
// APIs 
//=====================================================
app.get('/api/products', function(req, res){
	client.query('SELECT productid,productname,productdesc,ROUND(AVG(rating)::numeric,2) FROM product_details LEFT JOIN product_reviews ON pid = productid GROUP BY productid', function (err, docs) {
    res.setHeader('Content-Type', 'application/json');
	res.setHeader('Cache-Control','no-cache');
	res.send(JSON.stringify(docs.rows));
  });
});

app.get('/api/reviews', function(req, res){
  console.log(req)	
  var reviewProductId = req.query.id;
  if(reviewProductId){
	  var query = 'SELECT * FROM product_reviews, product_details WHERE pid = productid AND product_details.productid = \''+reviewProductId+'\'';
	  client.query(query, function(err, docs) { 
			res.setHeader('Content-Type', 'application/json');
			res.setHeader('Cache-Control','no-cache');
			res.send(JSON.stringify(docs.rows));
			});
  }else{
	  client.query('SELECT * FROM product_reviews, product_details WHERE pid = productid', function(err, docs) { 
			res.setHeader('Content-Type', 'application/json');
			res.setHeader('Cache-Control','no-cache');
			res.send(JSON.stringify(docs.rows));
		  });
  }
});

app.post("/api/reviews", function (req, res) {
  console.log("review called");	
  console.log(req.body)
  var productname=req.body.name;
  var email=req.body.email;
  var name=req.body.username;
  var rating=req.body.ratinginput;
  var review=req.body.feedback;
  var pid = req.body.name;
  
  console.log(pid)
  console.log(email)
  console.log(review)
  console.log(rating)
  
  if (pid && email && review && rating){
			//post a Tweet that a user commented on a product
     		var message = email +' just reviewed product '+ pid;
			console.log(message)
			
			// Update the new rating and new review number into PostgreSQL
			client.query('INSERT INTO product_reviews(name,email, pid, review, rating) values($1, $2, $3, $4,$5)',[name,email, pid, review,rating], function(err, result) {
						if (err) {
							console.log('review failed')
							res.status = 500;
							errorMessage = "duplicate key value violates unique constraint";
							errorMessageMissingProduct = "violates foreign key constraint";
							if(err.stack.indexOf(errorMessage) > -1){
								res.send('Review rejected.'+name+" already reviewed "+productname + '</br>'+'<A HREF=\"/">Home</A>')
							}else if(err.stack.indexOf(errorMessageMissingProduct) > -1){
								res.send('Review rejected.'+productname+'with id '+pid+'not found.</br><A HREF=\"/">Home</A>')
							}
							else{
								res.send('{ error:'+ err +'}')
							}
							return;
						}
						if(result){
						console.log('review submitted')
						res.status = 200;
						res.send('ok '+ '</br>'+'<A HREF=\"/">Home</A>')
						return;
						}
			});
	  }
  else {
	 res.redirect('/'); 
  }
});

//=====================================================
// UI 
//=====================================================
app.get('/', function (req, res) {
	client.query('SELECT productid,productname,productdesc,ROUND(AVG(rating)::numeric,2) FROM product_details LEFT JOIN product_reviews ON pid = productid GROUP BY productid', function (err, docs) {
		res.render('products', {
			products: docs,
			title: 'The Product App'
		});
	});
	postTweet('data');
});


// Navigation to add a new review
app.get("/ui/doreview", function (req, res) {
	
	res.render("doreview", {
		title: 'The Products List App',
		productname: req.query.id,
		productdesc: req.query.desc
	});
});

app.get('/ui/reviews', function(req, res){
  var reviewProductId = req.query.id;
  console.log("getting reviews for "+reviewProductId);
  if(!reviewProductId){
		console.log('review failed')
		res.status = 500;
		res.send('error:no review id passed')
	}else{
	  var query = 'SELECT * FROM product_reviews, product_details WHERE pid = productid AND product_details.productid = \''+reviewProductId+'\'';
	  client.query(query, function(err, docs) { 
			res.render("reviews", {
			title: 'The Products List App',
			reviews: docs.rows
			});
	  });

	}
});
