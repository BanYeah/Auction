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

	private static void HandleAuctionClosure() {
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
							"INSERT INTO billings (item_id, buyer_id, seller_id, final_price, transaction_time) " +
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
		if(username.equalsIgnoreCase("back")) {
			System.out.println();
			return;
		}

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

	private static void AdminMenu() {
		String adminname, adminpass;
		System.out.print(
			"----< Login as Administrator >\n" +
			" ** To go back, enter 'back' in user ID.\n" +
			"---- admin ID: "
		);

		adminname = scanner.next();
		scanner.nextLine();
		if (adminname.equalsIgnoreCase("back")) {
			System.out.println();
			return;
		}

		System.out.print("---- password: ");
		adminpass = scanner.next();
		scanner.nextLine();

		/* TODO: check the admin's account and password. */
		try (PreparedStatement pStmt = conn.prepareStatement(
			"SELECT 1 " +
			"FROM users " +
			"WHERE user_id = ? AND password = ? AND is_admin = TRUE"
		)) {
			pStmt.setString(1, adminname);
			pStmt.setString(2, adminpass);

			try (ResultSet rset = pStmt.executeQuery()) {
				if (!rset.next()) throw new SQLException();
			}
		} catch (SQLException e) {
			System.out.println();
			return; // login failed. go back to the previous menu.
		}
		System.out.println();

		String category, seller;
		char choice;
		do {
			System.out.println(
				"----< Admin menu > \n" +
				"    1. Print Sold Items per Category \n" +
				"    2. Print Account Balance for Seller \n" +
				"    3. Print Seller Ranking \n" +
				"    4. Print Buyer Ranking \n" +
				"    P. Go Back to Previous Menu"
				);

			try {
				choice = scanner.next().charAt(0);
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.\n");
				continue;
			}
			System.out.println();

			LocalDateTime dateTime;
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			if (choice == '1') {
				/* TODO: Print Sold Items per Category */
				HandleAuctionClosure(); // 경매 마감 확인

				System.out.print("----Enter Category to search : ");
				category = scanner.next();
				scanner.nextLine();

				System.out.println("sold item       | sold date       | seller ID   | buyer ID   | price");
				System.out.println("--------------------------------------------------------------------");

				String description, buyer_id, seller_id;
				long item_id;
				int final_price;
				try (PreparedStatement p = conn.prepareStatement(
					"SELECT item_id, description " +
					"FROM items NATURAL JOIN auctions " +
					"WHERE status = 'SOLD' AND category = ?"
				)) {
					p.setString(1, category);

					try (ResultSet item_rset = p.executeQuery()) {
						while (item_rset.next()) {
							item_id = item_rset.getLong("item_id");
							description = item_rset.getString("description");

							try (PreparedStatement pStmt = conn.prepareStatement(
								"SELECT buyer_id, seller_id, final_price, transaction_time " +
								"FROM billings " +
								"WHERE item_id = ?"
							)) {
								pStmt.setLong(1, item_id);

								try (ResultSet rset = pStmt.executeQuery()) {
									if (!rset.next()) throw new SQLException();

									buyer_id = rset.getString("buyer_id");
									seller_id = rset.getString("seller_id");
									final_price = rset.getInt("final_price");
									dateTime = rset.getTimestamp("transaction_time").toLocalDateTime();
								}
							}

							System.out.println(
								description + " | " +
								dateTime.format(formatter) + " | " +
								seller_id + " | " +
								buyer_id + " | " +
								final_price
							);
						}
					}
				} catch (SQLException e) {
					System.out.println("SQLException : " + e);
					return;
				}
				System.out.println();
			} else if (choice == '2') {
				/* TODO: Print Account Balance for Seller */
				HandleAuctionClosure(); // 경매 마감 확인

				System.out.print("---- Enter Seller ID to search : ");
				seller = scanner.next();
				scanner.nextLine();
				System.out.println();

				System.out.println("sold item       | sold date       | buyer ID   | price");
				System.out.println("------------------------------------------------------");

				String description, buyer_id;
				int final_price;
				try (PreparedStatement pStmt = conn.prepareStatement(
					"SELECT description, buyer_id, final_price, transaction_time " +
					"FROM items NATURAL JOIN billings " +
					"WHERE seller_id = ?"
				)) {
					pStmt.setString(1, seller);

					try (ResultSet rset = pStmt.executeQuery()) {
						while (rset.next()) {
							description = rset.getString("description");
							buyer_id = rset.getString("buyer_id");
							final_price = rset.getInt("final_price");
							dateTime = rset.getTimestamp("transaction_time").toLocalDateTime();

							System.out.println(
								description + " | " +
								dateTime.format(formatter) + " | " +
								buyer_id + " | " +
								final_price
							);
						}
					}
				} catch (SQLException e) {
					System.out.println("SQLException : " + e);
					return;
				}
				System.out.println();
			} else if (choice == '3') {
				/* TODO: Print Seller Ranking */
				HandleAuctionClosure(); // 경매 마감 확인

				System.out.println("seller ID   | # of items sold | Total Profit");
				System.out.println("--------------------------------------------");

				String seller_id;
				long item_num, total_profit;
				try (PreparedStatement pStmt = conn.prepareStatement(
					"SELECT seller_id, COUNT(*) AS item_num, SUM(final_price) AS total_profit " +
					"FROM billings " +
					"GROUP BY seller_id " +
					"ORDER BY total_profit DESC, item_num DESC"
				)) {
					try (ResultSet rset = pStmt.executeQuery()) {
						while (rset.next()) {
							seller_id = rset.getString("seller_id");
							item_num = rset.getLong("item_num");
							total_profit = rset.getLong("total_profit");

							System.out.println(
								seller_id + " | " +
								item_num + " | " +
								total_profit
							);
						}
					}
				} catch (SQLException e) {
					System.out.println("SQLException : " + e);
					return;
				}
				System.out.println();
			} else if (choice == '4') {
				/* TODO: Print Buyer Ranking */
				HandleAuctionClosure(); // 경매 마감 확인

				System.out.println("buyer ID   | # of items purchased | Total Money Spent");
				System.out.println("-----------------------------------------------------");

				String buyer_id;
				long item_num, total_spent;
				try (PreparedStatement pStmt = conn.prepareStatement(
					"SELECT buyer_id, COUNT(*) AS item_num, SUM(final_price) AS total_spent " +
					"FROM billings " +
					"GROUP BY buyer_id " +
					"ORDER BY total_spent DESC, item_num DESC"
				)) {
					try (ResultSet rset = pStmt.executeQuery()) {
						while (rset.next()) {
							buyer_id = rset.getString("buyer_id");
							item_num = rset.getLong("item_num");
							total_spent = rset.getLong("total_spent");

							System.out.println(
								buyer_id + " | " +
								item_num + " | " +
								total_spent + " | "
							);
						}
					}
				} catch (SQLException e) {
					System.out.println("SQLException : " + e);
					return;
				}
				System.out.println();
			} else if (choice == 'P' || choice == 'p') {
				System.out.println();
				return;
			} else
				System.out.println("Error: Invalid input is entered. Try again.\n");
		} while (true);
	}

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
			System.out.print("---- Description of the item (one line): ");
			description = scanner.nextLine();

			System.out.print("---- Starting price: ");
			while (!scanner.hasNextInt()) {
				scanner.next();
				System.out.println("Invalid input is entered. Please enter Starting price: ");
			}
			start_price = scanner.nextInt();
			scanner.nextLine();

			System.out.print("---- Buy-It-Now price: ");
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
				"INSERT INTO items (category, description, condition, seller_id, auction_id) " +
					"VALUES (?, ?, ?, ?, NULL)",
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
				"INSERT INTO auctions (item_id, starting_price, current_price, buy_it_now_price, bid_end_time) " +
					"VALUES (?, ?, ?, ?, ?)",
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

	public static void CheckSellStatus() {
		/* TODO: Check the status of the item the current user is selling */
		HandleAuctionClosure(); // 경매 마감 확인

		System.out.println("item listed in Auction | status | bidder (buyer ID) | bidding price | bidding date/time");
		System.out.println("---------------------------------------------------------------------------------------");

		Timestamp bid_time;
		String description, status, bidder_id;
		long auction_id;
		int bid_price;
		try (PreparedStatement p = conn.prepareStatement(
			"SELECT description, auction_id, status " +
			"FROM items NATURAL JOIN auctions " +
			"WHERE seller_id = ?"
		)) {
			p.setString(1, username);

			try (ResultSet item_rset = p.executeQuery()) {
				while (item_rset.next()) {
					description = item_rset.getString("description");
					auction_id = item_rset.getLong("auction_id");
					status = item_rset.getString("status");

					System.out.print(description + " | " + status);
					if (status.equals("LISTED") || status.equals("EXPIRED")) {
						System.out.println();
						continue;
					}

					try (PreparedStatement pStmt = conn.prepareStatement(
						"SELECT bidder_id, bid_price, bid_time " +
						"FROM bids " +
						"WHERE auction_id = ? " +
						"ORDER BY bid_price DESC, bid_time ASC " +
						"LIMIT 1"
					)) {
						pStmt.setLong(1, auction_id);

						try (ResultSet rset = pStmt.executeQuery()) {
							rset.next();
							bidder_id = rset.getString("bidder_id");
							bid_price = rset.getInt("bid_price");
							bid_time = rset.getTimestamp("bid_time");
						}
					}

					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
					LocalDateTime dateTime = bid_time.toLocalDateTime();
					System.out.println(" | " + bidder_id + " | " + bid_price + " | " + dateTime.format(formatter));
				}
				System.out.println();
			}
		} catch (SQLException e) {
			System.out.println("SQLException : " + e);
		}
	}

	public static void BuyItem() {
		Category category = null;
		Condition condition = null;
		LocalDateTime date;
		String keyword, seller, datePosted;

		char choice;
		boolean flag_catg = true, flag_cond = true;
		do {
			System.out.println(
				"----< Select category > : \n" +
				"    1. Electronics\n" +
				"    2. Books\n" +
				"    3. Home\n" +
				"    4. Clothing\n" +
				"    5. Sporting Goods\n" +
				"    6. Other categories\n" +
				"    7. Any category\n" +
				"    P. Go Back to Previous Menu"
				);

			try {
				choice = scanner.next().charAt(0);
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.\n");
				return;
			}
			System.out.println();

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
				case '7': // any category
					break;
				case 'p':
				case 'P':
					return;
				default:
					System.out.println("Error: Invalid input is entered. Try again.\n");
					flag_catg = false;
			}
		} while (!flag_catg);

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
				choice = scanner.next().charAt(0);
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.\n");
				return;
			}
			System.out.println();

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

		System.out.print("---- Enter keyword to search the description : ");
		keyword = scanner.nextLine();
		System.out.println();

		System.out.println(" ** Enter 'any' if you want to see items from any seller. ");
		System.out.print("---- Enter Seller ID to search : ");
		seller = scanner.next();
		scanner.nextLine();
		System.out.println();

		System.out.println(" ** This will search items that have been posted after the designated date.");
		System.out.print("---- Enter date posted (YYYY-MM-DD): ");
		datePosted = scanner.next();
		scanner.nextLine();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		date = LocalDate.parse(datePosted, formatter).atStartOfDay();
		System.out.println();


		/* TODO: Query condition: item category */
		/* TODO: Query condition: item condition */
		/* TODO: Query condition: items whose description match the keyword (use LIKE operator) */
		/* TODO: Query condition: items from a particular seller */
		/* TODO: Query condition: posted date of item */

		/* TODO: List all items that match the query condition */
		HandleAuctionClosure(); // 경매 마감 확인

		System.out.println("Item ID | Item description | Condition | Seller | Buy-It-Now | Current Bid | highest bidder | Time left | bid close");
		System.out.println("-------------------------------------------------------------------------------------------------------------------");

		boolean any_category = false, any_seller = false;
		if (category == null)
			any_category = true;
		if (seller.equalsIgnoreCase("any"))
			any_seller = true;

		Timestamp bid_end_time;
		String description, seller_id, highest_bidder_id;
		long item_id, auction_id;
		int BIN_price, current_price;
		try (PreparedStatement p = conn.prepareStatement(
			"SELECT item_id, auction_id, description, seller_id, current_price, buy_it_now_price, bid_end_time " +
			"FROM items NATURAL JOIN auctions " +
			"WHERE condition = ? AND description LIKE ? AND bid_start_time > ? AND " +
				"(status = 'LISTED' OR status = 'BIDDING') " +
				(any_category ? "" : "AND category = ? ") +
				(any_seller ? "" : "AND seller_id = ?")
		)) {
			p.setString(1, condition.name());
			p.setString(2, "%" + keyword + "%");
			p.setTimestamp(3, Timestamp.valueOf(date));
			if (!any_category) p.setString(4, category.name());
			if (any_category && !any_seller) p.setString(4, seller);
			else if (!any_seller) p.setString(5, seller);

			try (ResultSet item_rset = p.executeQuery()) {
				while (item_rset.next()) {
					item_id = item_rset.getLong("item_id");
					auction_id = item_rset.getLong("auction_id");
					description = item_rset.getString("description");
					seller_id = item_rset.getString("seller_id");
					current_price = item_rset.getInt("current_price");
					BIN_price = item_rset.getInt("buy_it_now_price");
					bid_end_time = item_rset.getTimestamp("bid_end_time");

					System.out.print(
						item_id + " | " +
						description + " | " +
						condition.name() + " | " +
						seller_id + " | " +
						BIN_price + " | " +
						current_price + " | "
					);

					try (PreparedStatement pStmt = conn.prepareStatement(
						"SELECT bidder_id " +
						"FROM bids " +
						"WHERE auction_id = ? " +
						"ORDER BY bid_price DESC, bid_time ASC " +
						"LIMIT 1"
					)) {
						pStmt.setLong(1, auction_id);

						try (ResultSet rset = pStmt.executeQuery()) {
							if (!rset.next())
								System.out.print(" | ");
							else {
								highest_bidder_id = rset.getString("bidder_id");
								System.out.print(highest_bidder_id + " | ");
							}
						}
					}

					formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
					LocalDateTime dateTime = bid_end_time.toLocalDateTime();
					LocalDateTime now = LocalDateTime.now();

					Duration duration = Duration.between(now, dateTime);
					long totalMinutes = duration.toMinutes();
					long days = totalMinutes / (24 * 60);
					long hours = (totalMinutes % (24 * 60)) / 60;
					long minutes = totalMinutes % 60;

					StringBuilder timeLeft = new StringBuilder();
					if (days > 0) timeLeft.append(days).append("d ");
					if (hours > 0 || days > 0) timeLeft.append(hours).append("h ");
					timeLeft.append(minutes).append("m");

					System.out.println(
						timeLeft.toString().trim() + " | " +
						dateTime.format(formatter)
					);
				}
			}
		} catch (SQLException e) {
			System.out.println("SQLException : " + e);
		}
		System.out.println();

		System.out.print("---- Select Item ID to buy or bid: ");

		int price;
		try {
			item_id = scanner.nextLong();
			scanner.nextLine();

			System.out.print("     Price: ");
			price = scanner.nextInt();
			scanner.nextLine();
		} catch (java.util.InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Try again.\n");
			return;
		}

		/* TODO: Buy-it-now or bid: If the entered price is higher or equal to Buy-It-Now price, the bid ends and the following needs to be printed. */
		HandleAuctionClosure(); // 경매 마감 확인

		String status;
		try (PreparedStatement pStmt = conn.prepareStatement(
			"SELECT seller_id, auction_id, current_price, buy_it_now_price, status " +
			"FROM items NATURAL JOIN auctions " +
			"WHERE item_id = ?"
		)) {
			pStmt.setLong(1, item_id);

			try (ResultSet rset = pStmt.executeQuery()) {
				if (!rset.next()) {
					System.out.println("Error: Incorrect Item ID\n");
					return;
				}

				seller_id = rset.getString("seller_id");
				auction_id = rset.getLong("auction_id");
				current_price = rset.getInt("current_price");
				BIN_price = rset.getInt("buy_it_now_price");
				status = rset.getString("status");
			}
		} catch (SQLException e) {
			System.out.println("SQLException : " + e);
			return;
		}

		/* If the bid is ended, print out the following. */
		if (status.equals("SOLD") || status.equals("EXPIRED")) {
			System.out.println("Bid ended.\n");
			return;
		}

		/* If the entered price is lower than the current highest price, print out the following. */
		else if (price <= current_price) {
			System.out.println("You must bid higher than the current price.\n");
			return;
		}

		// 해당 경매의 모든 입찰서의 상태를 'OUTBID'로 변경
		try (PreparedStatement pStmt = conn.prepareStatement(
			"UPDATE bids SET bid_status = 'OUTBID' WHERE auction_id = ?"
		)) {
			pStmt.setLong(1, auction_id);
			pStmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println("SQLException : " + e);
			return;
		}

		// 기존 입찰서 제거
		try (PreparedStatement pStmt = conn.prepareStatement(
			"DELETE FROM bids WHERE auction_id = ? AND bidder_id = ?"
		)) {
			pStmt.setLong(1, auction_id);
			pStmt.setString(2, username);
			pStmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println("SQLException : " + e);
			return;
		}

		/* If the bid price is higher than the Buy-It-Now price, the buyer pays the B-I-N price. */
		if (price >= BIN_price) {
			// 새 입찰서 작성
			try (PreparedStatement pStmt = conn.prepareStatement(
				"INSERT INTO bids(bidder_id, auction_id, bid_price, bid_status) VALUES (?, ?, ?, 'WON')"
			)) {
				pStmt.setString(1, username);
				pStmt.setLong(2, auction_id);
				pStmt.setInt(3, price);
				if (pStmt.executeUpdate() == 0) throw new SQLException();
			} catch (SQLException e) {
				System.out.println("SQLException : " + e);
				return;
			}

			// 경매에서 현재 가격과 경매 상태 변경
			try (PreparedStatement pStmt = conn.prepareStatement(
				"UPDATE auctions SET current_price = ?, status = 'SOLD', bid_end_time = CURRENT_TIMESTAMP " +
				"WHERE auction_id = ?"
			)) {
				pStmt.setInt(1, price);
				pStmt.setLong(2, auction_id);
				if (pStmt.executeUpdate() == 0) throw new SQLException();
			} catch (SQLException e) {
				System.out.println("SQLException : " + e);
				return;
			}

			// 청구서 작성
			try (PreparedStatement pStmt = conn.prepareStatement(
				"INSERT INTO billings(item_id, buyer_id, seller_id, final_price) VALUES (?, ?, ?, ?)"
			)) {
				pStmt.setLong(1, item_id);
				pStmt.setString(2, username);
				pStmt.setString(3, seller_id);
				pStmt.setInt(4, price);
				if (pStmt.executeUpdate() == 0) throw new SQLException();
			} catch (SQLException e) {
				System.out.println("SQLException : " + e);
				return;
			}

			System.out.println("Thank you for the purchase.\n");
		}

		/* Otherwise, print the following */
		else {
			// 새 입찰서 작성
			try (PreparedStatement pStmt = conn.prepareStatement(
				"INSERT INTO bids(bidder_id, auction_id, bid_price) VALUES (?, ?, ?)"
			)) {
				pStmt.setString(1, username);
				pStmt.setLong(2, auction_id);
				pStmt.setInt(3, price);
				if (pStmt.executeUpdate() == 0) throw new SQLException();
			} catch (SQLException e) {
				System.out.println("SQLException : " + e);
				return;
			}

			// 경매에서 현재 가격과 경매 상태 변경
			try (PreparedStatement pStmt = conn.prepareStatement(
				"UPDATE auctions SET current_price = ?, status = 'BIDDING' WHERE auction_id = ?"
			)) {
				pStmt.setInt(1, price);
				pStmt.setLong(2, auction_id);
				if (pStmt.executeUpdate() == 0) throw new SQLException();
			} catch (SQLException e) {
				System.out.println("SQLException : " + e);
				return;
			}

			System.out.println("Congratulations, you are the highest bidder.\n");
		}
	}

	public static void CheckBuyStatus() {
		/* TODO: Check the status of the item the current buyer is bidding on */
		/* Even if you are outbidded or the bid closing date has passed, all the items this user has bidded on must be displayed */
		HandleAuctionClosure(); // 경매 마감 확인

		System.out.println("item ID   | item description   | highest bidder | highest bidding price | your bidding price | bid closing date/time");
		System.out.println("--------------------------------------------------------------------------------------------------------------------");

		Timestamp bid_end_time;
		String description, highest_bidder_id;
		int highest_bid_price, bid_price;
		long auction_id, item_id;
		try (PreparedStatement p = conn.prepareStatement(
			"SELECT auction_id, bid_price FROM bids WHERE bidder_id = ?"
		)) {
			p.setString(1, username);

			try (ResultSet auction_rset = p.executeQuery()) {
				while (auction_rset.next()) {
					auction_id = auction_rset.getLong("auction_id");
					bid_price = auction_rset.getInt("bid_price");

					try (PreparedStatement pStmt = conn.prepareStatement(
						"SELECT item_id, description, bid_end_time " +
						"FROM items NATURAL JOIN auctions " +
						"WHERE auction_id = ?"
					)) {
						pStmt.setLong(1, auction_id);

						try (ResultSet rset = pStmt.executeQuery()) {
							if (!rset.next()) throw new SQLException();

							item_id = rset.getLong("item_id");
							description = rset.getString("description");
							bid_end_time = rset.getTimestamp("bid_end_time");
						}
					}

					try (PreparedStatement pStmt = conn.prepareStatement(
						"SELECT bidder_id, bid_price " +
						"FROM bids " +
						"WHERE auction_id = ? " +
						"ORDER BY bid_price DESC, bid_time ASC " +
						"LIMIT 1"
					)) {
						pStmt.setLong(1, auction_id);

						try (ResultSet bid_rset = pStmt.executeQuery()) {
							if (!bid_rset.next()) throw new SQLException();

							highest_bidder_id = bid_rset.getString("bidder_id");
							highest_bid_price = bid_rset.getInt("bid_price");
						}
					}

					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
					LocalDateTime dateTime = bid_end_time.toLocalDateTime();
					System.out.println(
						item_id + " | " +
						description + " | " +
						highest_bidder_id + " | " +
						highest_bid_price + " | " +
						bid_price + " | " +
						dateTime.format(formatter)
					);
				}
			}
		} catch (SQLException e) {
			System.out.println("SQLException : " + e);
		}
		System.out.println();
	}

	public static void CheckAccount() {
		/* TODO: Check the balance of the current user. */
		HandleAuctionClosure(); // 경매 마감 확인

		System.out.println("[Sold Items] \n");
		System.out.println("item category  | item ID   | sold date | sold price  | buyer ID");
		System.out.println("---------------------------------------------------------------");

		LocalDateTime dateTime;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		try (PreparedStatement pStmt = conn.prepareStatement(
			"SELECT item_id, category, buyer_id, final_price, transaction_time " +
			"FROM items NATURAL JOIN billings " +
			"WHERE seller_id = ?"
		)) {
			pStmt.setString(1, username);

			try (ResultSet rset = pStmt.executeQuery()) {
				while (rset.next()) {
					dateTime = rset.getTimestamp("transaction_time").toLocalDateTime();
					System.out.println(
						rset.getString("category") + " | " +
						rset.getLong("item_id") +  " | " +
						dateTime.format(formatter) + " | " +
						rset.getInt("final_price") + " | " +
						rset.getString("buyer_id")
					);
				}
			}
		} catch (SQLException e) {
			System.out.println("SQLException : " + e);
		}
		System.out.println();

		System.out.println("[Purchased Items] \n");
		System.out.println("item category  | item ID   | purchased date | puchased price  | seller ID");
		System.out.println("-------------------------------------------------------------------------");

		try (PreparedStatement pStmt = conn.prepareStatement(
				"SELECT item_id, category, seller_id, final_price, transaction_time " +
						"FROM items NATURAL JOIN billings " +
						"WHERE buyer_id = ?"
		)) {
			pStmt.setString(1, username);

			try (ResultSet rset = pStmt.executeQuery()) {
				while (rset.next()) {
					dateTime = rset.getTimestamp("transaction_time").toLocalDateTime();
					System.out.println(
						rset.getString("category") + " | " +
						rset.getLong("item_id") +  " | " +
						dateTime.format(formatter) + " | " +
						rset.getInt("final_price") + " | " +
						rset.getString("seller_id")
					);
				}
			}
		} catch (SQLException e) {
			System.out.println("SQLException : " + e);
		}
		System.out.println();
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


