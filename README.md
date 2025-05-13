# Auction
## Quick Start guide
1. Create user and database
```sql
CREATE USER s23311459 with encrypted password 'changethis';
CREATE DATABASE s23311459;
GRANT ALL PRIVILEGES ON DATABASE s23311459 TO s23311459;
```
2. Connect to database
```sql
psql -h localhost -U s23311459
\c s23311459
```
3. Data definition statements
```sql
\i ddl.sql
```
4. (optional) Sample data
```sql
\i data.sql
-- Before excute this, you need to insert users(apple, banana, cherry).
-- Those are commented in data.sql. (You may have to de-commented)
```
5. Compile ```Auction.java``` using ```make```
6. Run using ```make run```
<br>
<br>

## About
This project is a backend implementation of an online auction system. The system uses a menu-driven text interface in Java and interacts with a **PostgreSQL** database to handle core auction functionalities. It enables users to register, list items for sale, place bids, and manage auction transactions.

### Key Features
User Authentication:
* ```SignupMenu()``` and ```LoginMenu()``` allow users to register and log in using credentials stored in the database.

Admin Dashboard (```AdminMenu()```):
* View total sold items per category
* Check seller account balances
* View seller and buyer rankings based on total sales and purchases

Selling Functions:
* ```SellMenu()``` enables sellers to post new items for auction.
* ```CheckSellStatus()``` shows the current status of the seller’s listed items.

Auction Management:
* ```HandleAuctionClosure()``` processes expired auctions, determines winners, updates statuses, and generates billing records.

Buying Functions:
* ```BuyItem()``` allows users to search, filter, and bid on active listings.
* ```CheckBuyStatus()``` provides updates on ongoing and completed bids.

Account Management:
* ```CheckAccount()``` displays the user's transaction and billing history.

Database Design
The database includes the following key entities:
* **Users**: Stores user credentials.
* **Items**: Auctioned goods with category, condition, and description.
* **Auctions**: Details such as current price, buy-it-now price, time limits, and status.
* **Bids**: User bids linked to auctions with price and status tracking.
* **Billings**: Records completed transactions and payment status.

This system is designed for a backend-only environment without a GUI, with emphasis on SQL query correctness, schema integrity, and logic-driven flow for auction operations.
> @SKKU (Sungkyunkwan University)