all:
	javac Auction.java

run:
	java -cp .:./postgresql-42.6.0.jar Auction s23311459 changethis
