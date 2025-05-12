import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util. *;

public class Auction {
	private static final Scanner scanner = new Scanner(System.in);
	private static String username;
	private static Connection conn;

	enum Category {
		ELECTRONICS, 
		BOOKS,
		HOME,
		CLOTHING,
		SPORTINGGOODS,
		OTHERS
	}
	enum Condition {
		NEW,
		LIKE_NEW,
		GOOD,
		ACCEPTABLE
	}

	private static void handleAuctionClosure() {
		/* 입찰 시간이 종료(즉, 경매 마감)되었지만, 경매 상태가 변경되지 않은 경매를 처리  */
		// 1. 경매가 마감되었지만, 경매 상태가 'LISTED'거나 'BIDDING'인 경매 선택
		// 2. 경매 상태를 각각 'LISTED'인 경우 'EXPIRED'로, 'BIDDING'인 경우 'SOLD'로 변경
		// 3. 경매 상태를 'SOLD'로 변경한 경우, 최고가 입찰자의 상태('ACTIVE')를 'WON'으로 변경
		// 4.                               청구서 작성
		String status, statement, seller_id, bidder_id;
		long auction_id, item_id, bid_id;
		int bid_price;
		try {
			conn.setAutoCommit(false);

			try (PreparedStatement p = conn.prepareStatement(
				"SELECT auction_id, item_id, bid_end_time, status " +
				"FROM auctions " +
				"WHERE bid_end_time < CURRENT_TIMESTAMP AND (status = 'LISTED' OR status = 'BIDDING')"
			)) {
				try (ResultSet auction_rset = p.executeQuery()) {
					while (auction_rset.next()) {
						auction_id = auction_rset.getLong("auction_id");
						item_id = auction_rset.getLong("item_id");
						status = auction_rset.getString("status");
						if (status.equals("LISTED"))
							statement = "UPDATE auctions SET status = 'EXPIRED' WHERE auction_id = ?";
						else // 'BIDDING'
							statement = "UPDATE auctions SET status = 'SOLD' WHERE auction_id = ?";

						try (PreparedStatement pStmt = conn.prepareStatement(statement)) {
							pStmt.setLong(1, auction_id);
							if (pStmt.executeUpdate() == 0) throw new SQLException();
						}

						if (status.equals("LISTED")) continue;

						// 최고가 입찰자의 입찰서
						try (PreparedStatement pStmt = conn.prepareStatement(
							"SELECT bid_id, bidder_id, bid_price " +
							"FROM bids " +
							"WHERE auction_id = ? " +
							"ORDER BY bid_price DESC, bid_time ASC " +
							"LIMIT 1"
						)) {
							pStmt.setLong(1, auction_id);

							try (ResultSet bid_rset = pStmt.executeQuery()) {
								if (!bid_rset.next()) throw new SQLException();

								bid_id = bid_rset.getLong("bid_id");
								bidder_id = bid_rset.getString("bidder_id");
								bid_price = bid_rset.getInt("bid_price");
							}
						}

						try (PreparedStatement pStmt = conn.prepareStatement(
							"UPDATE bids SET bid_status = 'WON' WHERE bid_id = ?"
						)) {
							pStmt.setLong(1, bid_id);
							if (pStmt.executeUpdate() == 0) throw new SQLException();
						}

						try (PreparedStatement pStmt = conn.prepareStatement(
							"SELECT seller_id FROM items WHERE item_id = ?"
						)) {
							pStmt.setLong(1, item_id);

							try (ResultSet item_ret = pStmt.executeQuery()) {
								item_ret.next();
								seller_id = item_ret.getString("seller_id");
							}
						}

						// 청구서 작성
						try (PreparedStatement pStmt = conn.prepareStatement(
							"INSERT INTO billing (item_id, buyer_id, seller_id, final_price, transaction_time) " +
							"VALUES (?, ?, ?, ?, ?)"
						)) {
							pStmt.setLong(1, item_id);
							pStmt.setString(2, bidder_id);
							pStmt.setString(3, seller_id);
							pStmt.setInt(4, bid_price);
							pStmt.setTimestamp(5, auction_rset.getTimestamp("bid_end_time"));

							if (pStmt.executeUpdate() == 0) throw new SQLException();
						}
						conn.commit();
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("SQLException : " + e);
			try {
				conn.rollback();
			} catch (SQLException rollbackEx) {
				System.out.println(rollbackEx.getMessage());
			}
		} finally {
			try {
				conn.setAutoCommit(true);
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
	}

	private static void LoginMenu() {
		String userpass;
		System.out.print(
			"----< User Login > \n" +
			" ** To go back, enter 'back' in user ID. \n" +
			"     user ID: "
			);

		username = scanner.next();
		scanner.nextLine();
		if(username.equalsIgnoreCase("back"))
			return;

		System.out.print("     password: ");
		userpass = scanner.next();
		scanner.nextLine();

		/* TODO: Your code should come here to check ID and password */
		try (PreparedStatement pStmt = conn.prepareStatement(
			"SELECT 1 " +
			"FROM users " +
			"WHERE user_id = ? AND password = ?"
		)) {
			pStmt.setString(1, username);
			pStmt.setString(2, userpass);

			try (ResultSet rset = pStmt.executeQuery()) {
				if (!rset.next()) throw new SQLException();
			}
		} catch (SQLException e) {
			System.out.println("Error: Incorrect user name or password\n");
			username = null;
			return;
		}

		System.out.println("You are successfully logged in.\n");
	}

	private static void SignupMenu() {
		boolean is_admin;
		String new_username, userpass, isAdmin;
		System.out.print(
			"----< Sign Up >\n" +
			" ** To go back, enter 'back' in user ID.\n" +
			"---- user name: "
			);

		try {
			new_username = scanner.next();
			scanner.nextLine();
			if (new_username.equalsIgnoreCase("back"))
				return;

			System.out.print("---- password: ");
			userpass = scanner.next();
			scanner.nextLine();

			System.out.print("---- In this user an administrator? (Y/N): ");
			isAdmin = scanner.next();
			scanner.nextLine();
			if (isAdmin.equalsIgnoreCase("Y") || isAdmin.equalsIgnoreCase("YES"))
				is_admin = true;
			else if (isAdmin.equalsIgnoreCase("N") || isAdmin.equalsIgnoreCase("NO"))
				is_admin = false;
			else throw new InputMismatchException();
		} catch (java.util.InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Please select again.\n");
			return;
		}

		/* TODO: Your code should come here to create a user account in your database */
        try (PreparedStatement pStmt = conn.prepareStatement(
			"INSERT INTO users VALUES(?, ?, ?)"
		)) {
			pStmt.setString(1, new_username);
			pStmt.setString(2, userpass);
			pStmt.setBoolean(3, is_admin);

			if (pStmt.executeUpdate() == 0) throw new SQLException();
        } catch (SQLException e) {
			System.out.println("Error: Sign up failed. Please select again.\n");
			return;
        }
        System.out.println("Your account has been successfully created.\n");
	}

	private static boolean SellMenu() {
		Category category;
		Condition condition;
		char choice;
		int price;
	private static void SellMenu() {
		Category category = null;
		Condition condition = null;
		LocalDateTime dateTime;
		String description;
		int start_price, BIN_price;

		char choice;
		boolean flag_catg = true, flag_cond = true;
		do {
			System.out.println(
				"----< Sell Item >\n" +
				"---- Choose a category.\n" +
				"    1. Electronics\n" +
				"    2. Books\n" +
				"    3. Home\n" +
				"    4. Clothing\n" +
				"    5. Sporting Goods\n" +
				"    6. Other Categories\n" +
				"    P. Go Back to Previous Menu"
			);

			try {
				choice = scanner.next().charAt(0);
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.\n");
				continue;
			}

			flag_catg = true;
			switch ((int) choice) {
				case '1':
					category = Category.ELECTRONICS;
					break;
				case '2':
					category = Category.BOOKS;
					break;
				case '3':
					category = Category.HOME;
					break;
				case '4':
					category = Category.CLOTHING;
					break;
				case '5':
					category = Category.SPORTINGGOODS;
					break;
				case '6':
					category = Category.OTHERS;
					break;
				case 'p':
				case 'P':
					return;
				default:
					System.out.println("Error: Invalid input is entered. Try again.\n");
					flag_catg = false;
			}
		} while (!flag_catg);
		System.out.println();

		do {
			System.out.println(
				"---- Select the condition of the item to sell.\n" +
				"   1. New\n" +
				"   2. Like-new\n" +
				"   3. Used (Good)\n" +
				"   4. Used (Acceptable)\n" +
				"   P. Go Back to Previous Menu"
			);

			try {
				choice = scanner.next().charAt(0);
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.\n");
				continue;
			}

			flag_cond = true;
			switch (choice) {
				case '1':
					condition = Condition.NEW;
					break;
				case '2':
					condition = Condition.LIKE_NEW;
					break;
				case '3':
					condition = Condition.GOOD;
					break;
				case '4':
					condition = Condition.ACCEPTABLE;
					break;
				case 'p':
				case 'P':
					return;
				default:
					System.out.println("Error: Invalid input is entered. Try again.\n");
					flag_cond = false;
			}
		} while (!flag_cond);
		System.out.println();

		try {
			System.out.println("---- Description of the item (one line): ");
			description = scanner.nextLine();

			System.out.println("---- Starting price: ");
			while (!scanner.hasNextInt()) {
				scanner.next();
				System.out.println("Invalid input is entered. Please enter Starting price: ");
			}
			start_price = scanner.nextInt();
			scanner.nextLine();

			System.out.println("---- Buy-It-Now price: ");
			while (!scanner.hasNextInt()) {
				scanner.next();
				System.out.println("Invalid input is entered. Please enter Buy-It-Now price: ");
			}
			BIN_price = scanner.nextInt();
			scanner.nextLine();

			System.out.print("---- Bid closing date and time (YYYY-MM-DD HH:MM): ");
			// you may assume users always enter valid date/time
			String date = scanner.nextLine();  // "2023-03-04 11:30"
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
			dateTime = LocalDateTime.parse(date, formatter);
		} catch (Exception e) {
			System.out.println("Error: Invalid input is entered. Going back to the previous menu.\n");
			return;
		}

		/* TODO: Your code should come here to store the user inputs in your database */
		long item_id, auction_id;
		try {
			conn.setAutoCommit(false);

			try (PreparedStatement pStmt = conn.prepareStatement(
				"INSERT INTO items (category, description, condition, seller_id, auction_id) VALUES (?, ?, ?, ?, NULL)",
				Statement.RETURN_GENERATED_KEYS  // for auto-generated item_id
			)) {
				pStmt.setString(1, category.name());
				pStmt.setString(2, description);
				pStmt.setString(3, condition.name());
				pStmt.setString(4, username);
				pStmt.executeUpdate();

				try (ResultSet rset = pStmt.getGeneratedKeys()) {
					if (rset.next()) item_id = rset.getLong(1);
					else throw new SQLException();
				}
			}

			try (PreparedStatement pStmt = conn.prepareStatement(
				"INSERT INTO auctions (item_id, starting_price, current_price, buy_it_now_price, bid_end_time) VALUES (?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS // for auto-generated item_id
			)) {
				pStmt.setLong(1, item_id);
				pStmt.setInt(2, start_price);
				pStmt.setInt(3, start_price);
				pStmt.setInt(4, BIN_price);
				pStmt.setTimestamp(5, Timestamp.valueOf(dateTime));
				pStmt.executeUpdate();

				try (ResultSet rset = pStmt.getGeneratedKeys()) {
					if (rset.next()) auction_id = rset.getLong(1);
					else throw new SQLException();
				}
			}

			try (PreparedStatement pStmt = conn.prepareStatement(
				"UPDATE items SET auction_id = ? WHERE item_id = ?"
			)) {
				pStmt.setLong(1, auction_id);
				pStmt.setLong(2, item_id);
				if (pStmt.executeUpdate() == 0) throw new SQLException();
			}

			conn.commit();
		} catch (SQLException e) {
			try {
				conn.rollback();
			} catch (SQLException rollbackEx) {
				System.out.println(rollbackEx.getMessage());
			}
			System.out.println("Error: Sell item failed. Please select again.\n");
			return;
		} finally {
			try {
				conn.setAutoCommit(true);
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}

		System.out.println("Your item has been successfully listed.\n");
	}

	public static void CheckSellStatus(){
		/* TODO: Check the status of the item the current user is selling */

		System.out.println("item listed in Auction | bidder (buyer ID) | bidding price | bidding date/time \n");
		System.out.println("-------------------------------------------------------------------------------\n");
		/*
		   while(rset.next(){
		   System.out.println();
		   }
		 */
	}

	public static boolean BuyItem(){
		Category category;
		Condition condition;
		char choice;
		int price;
		String keyword, seller, datePosted;
		boolean flag_catg = true, flag_cond = true;
		
		do {

			System.out.println( "----< Select category > : \n" +
					"    1. Electronics\n"+
					"    2. Books\n" + 
					"    3. Home\n" + 
					"    4. Clothing\n" + 
					"    5. Sporting Goods\n" +
					"    6. Other categories\n" +
					"    7. Any category\n" +
					"    P. Go Back to Previous Menu"
					);

			try {
				choice = scanner.next().charAt(0);;
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				return false;
			}

			flag_catg = true;

			switch (choice) {
				case '1':
					category = Category.ELECTRONICS;
					break;
				case '2':
					category = Category.BOOKS;
					break;
				case '3':
					category = Category.HOME;
					break;
				case '4':
					category = Category.CLOTHING;
					break;
				case '5':
					category = Category.SPORTINGGOODS;
					break;
				case '6':
					category = Category.OTHERS;
					break;
				case '7':
					break;
				case 'p':
				case 'P':
					return false;
				default:
					System.out.println("Error: Invalid input is entered. Try again.");
					flag_catg = false;
					continue;
			}
		} while(!flag_catg);

		do {

			System.out.println(
					"----< Select the condition > \n" +
					"   1. New\n" +
					"   2. Like-new\n" +
					"   3. Used (Good)\n" +
					"   4. Used (Acceptable)\n" +
					"   P. Go Back to Previous Menu"
					);
			try {
				choice = scanner.next().charAt(0);;
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				return false;
			}

			flag_cond = true;

			switch (choice) {
				case '1':
					condition = Condition.NEW;
					break;
				case '2':
					condition = Condition.LIKE_NEW;
					break;
				case '3':
					condition = Condition.GOOD;
					break;
				case '4':
					condition = Condition.ACCEPTABLE;
					break;
				case 'p':
				case 'P':
					return false;
				default:
					System.out.println("Error: Invalid input is entered. Try again.");
					flag_cond = false;
					continue;
				}
		} while(!flag_cond);

		try {
			System.out.println("---- Enter keyword to search the description : ");
			keyword = scanner.next();
			scanner.nextLine();

			System.out.println("---- Enter Seller ID to search : ");
			System.out.println(" ** Enter 'any' if you want to see items from any seller. ");
			seller = scanner.next();
			scanner.nextLine();

			System.out.println("---- Enter date posted (YYYY-MM-DD): ");
			System.out.println(" ** This will search items that have been posted after the designated date.");
			datePosted = scanner.next();
			scanner.nextLine();
		} catch (java.util.InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Try again.");
			return false;
		}

		/* TODO: Query condition: item category */
		/* TODO: Query condition: item condition */
		/* TODO: Query condition: items whose description match the keyword (use LIKE operator) */
		/* TODO: Query condition: items from a particular seller */
		/* TODO: Query condition: posted date of item */

		/* TODO: List all items that match the query condition */
		System.out.println("Item ID | Item description | Condition | Seller | Buy-It-Now | Current Bid | highest bidder | Time left | bid close");
		System.out.println("-------------------------------------------------------------------------------------------------------");
		/* 
		   while(rset.next()){ 
		   }
		 */

		System.out.println("---- Select Item ID to buy or bid: ");

		try {
			choice = scanner.next().charAt(0);;
			scanner.nextLine();
			System.out.println("     Price: ");
			price = scanner.nextInt();
			scanner.nextLine();
		} catch (java.util.InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Try again.");
			return false;
		}

		/* TODO: Buy-it-now or bid: If the entered price is higher or equal to Buy-It-Now price, the bid ends and the following needs to be printed. */
		/* Even if the bid price is higher than the Buy-It-Now price, the buyer pays the B-I-N price. */
		System.out.println("Thank you for the purchase.\n"); 

		/* If the entered price is lower than the current highest price, print out the following. */
		System.out.println("You must bid higher than the current price. \n"); 

                /* Otherwise, print the following */
		System.out.println("Congratulations, you are the highest bidder.\n"); 
		return true;
	}

	public static void CheckBuyStatus(){
		/* TODO: Check the status of the item the current buyer is bidding on */
		/* Even if you are outbidded or the bid closing date has passed, all the items this user has bidded on must be displayed */

		System.out.println("item ID   | item description   | highest bidder | highest bidding price | your bidding price | bid closing date/time");
		System.out.println("--------------------------------------------------------------------------------------------------------------------");
		/*
		   while(rset.next(){
		   System.out.println();
		   }
		 */
	}

	public static void CheckAccount(){
		/* TODO: Check the balance of the current user.  */
		System.out.println("[Sold Items] \n");
		System.out.println("item category  | item ID   | sold date | sold price  | buyer ID | commission  ");
		System.out.println("------------------------------------------------------------------------------");
		/*
		   while(rset.next(){
		   System.out.println();
		   }
		 */
		System.out.println("[Purchased Items] \n");
		System.out.println("item category  | item ID   | purchased date | puchased price  | seller ID ");
		System.out.println("--------------------------------------------------------------------------");
		/*
		   while(rset.next(){
		   System.out.println();
		   }
		 */
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java Auction postgres_id password");
			System.exit(1);
		}

		try {
			conn = DriverManager.getConnection("jdbc:postgresql://localhost/" + args[0], args[0], args[1]);
		} catch (SQLException e) {
			System.out.println("SQLException : " + e); // 연결 실패
			System.exit(1);
		}

		char choice;
		do {
			username = null;
			System.out.println(
				"----< Login menu > \n" +
				"----(1) Login \n" +
				"----(2) Sign up \n" +
				"----(3) Login as Administrator \n" +
				"----(Q) Quit"
			);

			try {
				choice = scanner.next().charAt(0);
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.\n");
				continue;
			}
			System.out.println();

			try {
				switch ((int) choice) {
					case '1':
						LoginMenu();
						break;
					case '2':
						SignupMenu();
						continue;
					case '3':
						AdminMenu();
						continue;
					case 'q':
					case 'Q':
						System.out.println("Good Bye");
						/* TODO: close the connection and clean up everything here */
						conn.close();
						System.exit(1);
					default:
						System.out.println("Error: Invalid input is entered. Try again.\n");
				}
			} catch (SQLException e) {
				System.out.println("SQLException : " + e);	
			}
		} while (username == null || username.equalsIgnoreCase("back"));

		// logged in as a normal user 
		do {
			System.out.println(
				"---< Main menu > : \n" +
				"----(1) Sell Item \n" +
				"----(2) Check Status of Your Listed Item \n" +
				"----(3) Buy Item \n" +
				"----(4) Check Status of your Bid \n" +
				"----(5) Check your Account \n" +
				"----(Q) Quit"
			);

			try {
				choice = scanner.next().charAt(0);;
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.\n");
				continue;
			}
			System.out.println();

			try {
				switch (choice) {
					case '1':
						SellMenu();
						continue;
					case '2':
						CheckSellStatus();
						continue;
					case '3':
						BuyItem();
						continue;
					case '4':
						CheckBuyStatus();
						continue;
					case '5':
						CheckAccount();
						continue;
					case 'q':
					case 'Q':
						System.out.println("Good Bye");
						/* TODO: close the connection and clean up everything here */
						conn.close();
						System.exit(1);
					default:
						System.out.println("Error: Invalid input is entered. Try again.\n");
				}
			} catch (SQLException e) {
				System.out.println("SQLException : " + e);
				System.exit(1);
			}
		} while (true);
	} // End of main
} // End of class


