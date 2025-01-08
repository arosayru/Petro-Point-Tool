
package petro.point.tool;
import java.sql.*;
import java.text.SimpleDateFormat;
import javax.swing.JOptionPane;

public class PetroPointInterface extends javax.swing.JFrame {

  
    private int[] petrolStock; // Array for petrol stock
    private int[] dieselStock; // Array for diesel stock (added for fuel type selection)
    
    ResultSet rst;
  
    public PetroPointInterface() {
        
           petrolStock = new int[1]; // Fixed size for petrol queue
           dieselStock = new int[1]; // Fixed size for diesel queue
        initComponents();
        loadFuelStocksFromDatabase(); // Load both petrol and diesel stocks from the database
        updateStockDisplay("Petrol");
    }
    
    private void loadFuelStocksFromDatabase() {
        try {
            Statement st = DBConnection.getdbconnection().createStatement();

            // Get the total petrol stock
            rst = st.executeQuery("SELECT SUM(amount) AS TotalAmount FROM petrolstock");
            if (rst.next()) {
                petrolStock[0] = rst.getInt("TotalAmount");
            } else {
                petrolStock[0] = 0; // Default stock if no rows found
            }

            // Get the total diesel stock
            rst = st.executeQuery("SELECT SUM(amount) AS TotalAmount FROM dieselstock");
            if (rst.next()) {
                dieselStock[0] = rst.getInt("TotalAmount");
            } else {
                dieselStock[0] = 0; // Default stock if no rows found
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading fuel stocks from database!");
        }
    }
    
   // Update available stock and free stock
    private void updateStockDisplay(String fuelType) {
        try {
            int availableStock = 0;
            String tableName = fuelType.equals("Petrol") ? "petrolstock" : "dieselstock";

            Statement st = DBConnection.getdbconnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT SUM(amount) AS TotalAmount FROM " + tableName);

            if (rs.next()) {
                availableStock = rs.getInt("TotalAmount");
            }

            int totalCapacity = 10000; // Maximum capacity
            int freeStock = totalCapacity - availableStock;

            // Update text fields
            availablestock_txt.setText(String.valueOf(availableStock+"L"));
            spacestock_txt.setText(String.valueOf(freeStock+"L"));

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating stock display!");
        }
    }

    
    // Refill fuel
    public void RefillFuel(String fuelType, int fuelAmount) {
        int currentStock = fuelType.equals("Petrol") ? petrolStock[0] : dieselStock[0];

        if (currentStock + fuelAmount > 10000) {
            int canAdd = 10000 - currentStock;
            JOptionPane.showMessageDialog(this, "Cannot refill more than 10,000 for " + fuelType + "! Can add only: " + canAdd);
        } else {
            if (fuelType.equals("Petrol")) {
                petrolStock[0] += fuelAmount;
                addNewStockRowToDatabase("Petrol", fuelAmount);
            } else if (fuelType.equals("Diesel")) {
                dieselStock[0] += fuelAmount;
                addNewStockRowToDatabase("Diesel", fuelAmount);
            }

            updateStockDisplay(fuelType); // Refresh stock display
        }
    }


  private void addNewStockRowToDatabase(String fuelType, int fuelAmount) {
      
      String tableName = fuelType.equals("Petrol") ? "petrolstock" : "dieselstock";
    String tableNameWithValue = fuelType.equals("Petrol") ? "petrolstocktable" : "dieselstocktable";
    String stockPriceText = stock_price.getText(); // Get the value from the stock_price text field

    // Check if the stock price is valid (numeric check)
    double stockPrice = 0;
    try {
        stockPrice = Double.parseDouble(stockPriceText);
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Please enter a valid stock price.");
        return; // Exit if stock price is invalid
    }

    try (Connection con = DBConnection.getdbconnection()) {

        // Step 1: Insert into the dieselstock or petrolstock table (without value)
        String insertQuery = "INSERT INTO " + tableName + " (amount, datetime) VALUES (?, ?)";
        try (PreparedStatement insertStmt = con.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            insertStmt.setInt(1, fuelAmount);
            insertStmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            int rowsInserted = insertStmt.executeUpdate();

            // Step 2: Get the latest inserted ID (dsid or psid)
            ResultSet rs = insertStmt.getGeneratedKeys();
            int latestId = -1;
            if (rs.next()) {
                latestId = rs.getInt(1);  // dsid or psid
            }

            if (latestId == -1) {
                JOptionPane.showMessageDialog(this, "Error getting the latest ID.");
                return;
            }

            // Step 3: Insert into the dieselstocktable or petrolstocktable with stock price (value)
            String insertQueryWithValue = "INSERT INTO " + tableNameWithValue + " (amount, datetime, value) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmtWithValue = con.prepareStatement(insertQueryWithValue)) {
                insertStmtWithValue.setInt(1, fuelAmount);
                insertStmtWithValue.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                insertStmtWithValue.setDouble(3, stockPrice);
                int rowsInsertedValue = insertStmtWithValue.executeUpdate();

                if (rowsInsertedValue > 0) {
                    JOptionPane.showMessageDialog(this, "New stock row added for " + fuelType + ": " + fuelAmount);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to add value for " + fuelType);
                }
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error adding new stock row for " + fuelType + " in database: " + e.getMessage());
    }
    }
  
  private void recordPetrolStockTransaction(int fuelAmount,double stockPrice) {
    try (Connection con = DBConnection.getdbconnection();
         PreparedStatement stmt = con.prepareStatement(
                 "INSERT INTO petrolstocktable (amount, datetime, value) VALUES (?, ?, ?)")) {

        stmt.setInt(1, fuelAmount);
        stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        stmt.setDouble(3, stockPrice);  // Set the stock price for the 'value' column

        stmt.executeUpdate();
        System.out.println("Transaction recorded in petrolstocktable: " + fuelAmount + ", Price: " + stockPrice);

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error recording petrol transaction: " + e.getMessage());
    }
}
  
 private void recordDieselStockTransaction(int fuelAmount,double stockPrice) {
    try (Connection con = DBConnection.getdbconnection();
         PreparedStatement stmt = con.prepareStatement(
                 "INSERT INTO dieselstocktable (amount, datetime, value) VALUES (?, ?, ?)")) {

        stmt.setInt(1, fuelAmount);
        stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        stmt.setDouble(3, stockPrice);  // Set the stock price for the 'value' column

        stmt.executeUpdate();
        System.out.println("Transaction recorded in dieselstocktable: " + fuelAmount + ", Price: " + stockPrice);

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error recording diesel transaction: " + e.getMessage());
    }
}
  
  // Method to reduce fuel stock after pumping
    public void reduceFuelStock(String fuelType, int fuelAmount) {
        if (fuelType.equals("Petrol")) {
            if (petrolStock[0] >= fuelAmount) {
                petrolStock[0] -= fuelAmount;
                System.out.println("Pumped Petrol. Updated Petrol stock: " + petrolStock[0]);
            } else {
                System.out.println("Not enough Petrol in stock!");
            }
        } else if (fuelType.equals("Diesel")) {
            if (dieselStock[0] >= fuelAmount) {
                dieselStock[0] -= fuelAmount;
                System.out.println("Pumped Diesel. Updated Diesel stock: " + dieselStock[0]);
            } else {
                System.out.println("Not enough Diesel in stock!");
            }
        }
    }         
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        FuelType_Combo_for_stock_by_date = new javax.swing.JComboBox<>();
        Refill_text = new javax.swing.JTextField();
        btn_refill = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        btn_back = new javax.swing.JButton();
        btn_clr = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        availablestock_txt = new javax.swing.JTextField();
        spacestock_txt = new javax.swing.JTextField();
        jSeparator2 = new javax.swing.JSeparator();
        jPanel4 = new javax.swing.JPanel();
        added_stock_text_field = new javax.swing.JTextField();
        btn_search__stock_added_by_date = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jDateChooser_to_stock = new com.toedter.calendar.JDateChooser();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        price_sum = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jDateChooser_from_stock = new com.toedter.calendar.JDateChooser();
        jSeparator3 = new javax.swing.JSeparator();
        stock_price = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        jPanel1.setForeground(new java.awt.Color(255, 255, 255));
        jPanel1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        FuelType_Combo_for_stock_by_date.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        FuelType_Combo_for_stock_by_date.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Petrol", "Diesel" }));
        FuelType_Combo_for_stock_by_date.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FuelType_Combo_for_stock_by_dateActionPerformed(evt);
            }
        });
        jPanel1.add(FuelType_Combo_for_stock_by_date, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 110, 250, 40));

        Refill_text.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jPanel1.add(Refill_text, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 340, 140, 40));

        btn_refill.setBackground(new java.awt.Color(178, 0, 0));
        btn_refill.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btn_refill.setForeground(new java.awt.Color(255, 255, 255));
        btn_refill.setText("ReFill");
        btn_refill.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_refillActionPerformed(evt);
            }
        });
        jPanel1.add(btn_refill, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 430, 170, 40));

        jPanel2.setBackground(new java.awt.Color(178, 0, 0));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("PETRO POINT");
        jPanel2.add(jLabel1);

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 670, 40));

        jPanel3.setBackground(new java.awt.Color(178, 0, 0));
        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 490, 670, 30));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setText("Enter Price");
        jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 240, 140, -1));

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("Select Fuel Type");
        jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 80, 130, -1));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 51, 51));
        jLabel2.setText("Fuel Tank Capacity   :  10 000L");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 60, -1, -1));

        btn_back.setBackground(new java.awt.Color(178, 0, 0));
        btn_back.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btn_back.setForeground(new java.awt.Color(255, 255, 255));
        btn_back.setText("Back");
        btn_back.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_backActionPerformed(evt);
            }
        });
        jPanel1.add(btn_back, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 430, 170, 40));

        btn_clr.setBackground(new java.awt.Color(178, 0, 0));
        btn_clr.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btn_clr.setForeground(new java.awt.Color(255, 255, 255));
        btn_clr.setText("Clear");
        btn_clr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_clrActionPerformed(evt);
            }
        });
        jPanel1.add(btn_clr, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 430, 170, 40));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setText("Available Stock");
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 110, -1, -1));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("Free Stock");
        jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 160, -1, -1));
        jPanel1.add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 200, 220, 10));

        availablestock_txt.setEditable(false);
        availablestock_txt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                availablestock_txtActionPerformed(evt);
            }
        });
        jPanel1.add(availablestock_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 100, 110, 30));

        spacestock_txt.setEditable(false);
        jPanel1.add(spacestock_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 150, 110, 30));
        jPanel1.add(jSeparator2, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 80, 220, 10));

        jPanel4.setBackground(new java.awt.Color(234, 234, 234));
        jPanel4.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        added_stock_text_field.setEditable(false);
        added_stock_text_field.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jPanel4.add(added_stock_text_field, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 70, 140, -1));

        btn_search__stock_added_by_date.setBackground(new java.awt.Color(178, 0, 0));
        btn_search__stock_added_by_date.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btn_search__stock_added_by_date.setForeground(new java.awt.Color(255, 255, 255));
        btn_search__stock_added_by_date.setText("Search");
        btn_search__stock_added_by_date.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_search__stock_added_by_dateActionPerformed(evt);
            }
        });
        jPanel4.add(btn_search__stock_added_by_date, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 140, -1, 30));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setText("Stocked Value");
        jPanel4.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 100, 130, 30));
        jPanel4.add(jDateChooser_to_stock, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 40, 160, -1));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setText("From Date");
        jPanel4.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 10, 80, 30));

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel8.setText("To Date");
        jPanel4.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 40, 80, 30));

        price_sum.setEditable(false);
        jPanel4.add(price_sum, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 100, 140, -1));

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel10.setText("Stocked Amount");
        jPanel4.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 70, 130, 30));
        jPanel4.add(jDateChooser_from_stock, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 10, 160, -1));

        jPanel1.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 200, 360, 180));
        jPanel1.add(jSeparator3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 410, 620, 10));
        jPanel1.add(stock_price, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 260, 140, 40));

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel11.setText("Enter Refill Amount");
        jPanel1.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 320, 140, -1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 5, Short.MAX_VALUE))
        );

        setSize(new java.awt.Dimension(684, 532));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btn_refillActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_refillActionPerformed
         try {
            int fuelAmount = Integer.parseInt(Refill_text.getText());
            String selectedFuelType = FuelType_Combo_for_stock_by_date.getSelectedItem().toString();
            RefillFuel(selectedFuelType, fuelAmount);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
        }

    }//GEN-LAST:event_btn_refillActionPerformed

    private void FuelType_Combo_for_stock_by_dateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FuelType_Combo_for_stock_by_dateActionPerformed
         String selectedFuelType = FuelType_Combo_for_stock_by_date.getSelectedItem().toString();
        updateStockDisplay(selectedFuelType);

    }//GEN-LAST:event_FuelType_Combo_for_stock_by_dateActionPerformed

    private void btn_backActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_backActionPerformed
        new HomePage().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btn_backActionPerformed

    private void btn_clrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_clrActionPerformed

    Refill_text.setText("");
    stock_price.setText("");
    
    jDateChooser_to_stock.setDate(null);
    }//GEN-LAST:event_btn_clrActionPerformed

    private void availablestock_txtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_availablestock_txtActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_availablestock_txtActionPerformed

    private void btn_search__stock_added_by_dateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_search__stock_added_by_dateActionPerformed
        
   try {
        // Get the dates from the date pickers
        SimpleDateFormat dformat = new SimpleDateFormat("yyyy-MM-dd");

        if (jDateChooser_from_stock.getDate() == null || jDateChooser_to_stock.getDate() == null) {
            JOptionPane.showMessageDialog(null, "Please select both From and To dates.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return; // Exit if no dates are selected
        }

        String fromDate = dformat.format(jDateChooser_from_stock.getDate());
        String toDate = dformat.format(jDateChooser_to_stock.getDate());

        // Get the selected fuel type
        String fuelType = FuelType_Combo_for_stock_by_date.getSelectedItem() != null 
                          ? FuelType_Combo_for_stock_by_date.getSelectedItem().toString() 
                          : "";

        if (fuelType.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please select a fuel type.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return; // Exit if no fuel type is selected
        }

        String pumpTable = fuelType.equals("Petrol") ? "petrolstocktable" : "dieselstocktable";

        // SQL query to fetch pump values for the selected date range
        String query = "SELECT SUM(amount) AS totalAmount, SUM(amount * value) AS totalPrice " +
                       "FROM " + pumpTable + " WHERE DATE(datetime) BETWEEN ? AND ?";

        try (Connection con = DBConnection.getdbconnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            // Set the from and to dates in the query
            stmt.setString(1, fromDate);
            stmt.setString(2, toDate);

            try (ResultSet rs = stmt.executeQuery()) {
                double totalAmount = 0;
                double totalPrice = 0;

                // Calculate the total amount pumped and total price for the selected date range
                if (rs.next()) {
                    totalAmount = rs.getDouble("totalAmount");
                    totalPrice = rs.getDouble("totalPrice");
                }

                // Calculate price per liter
                double pricePerLiter = totalAmount != 0 ? totalPrice / totalAmount : 0;

                // Display the total amount of fuel pumped in the added_stock_text_field
                added_stock_text_field.setText(totalAmount + "L");

                // Display the total price of the fuel pumped in the price_sum text field
                price_sum.setText("LKR:" + totalPrice);

                // Optionally, log or use the pricePerLiter for further calculations
                System.out.println("Average price per liter: " + pricePerLiter);

                // If no records were found for the selected date range, show a message
                if (totalAmount == 0) {
                    JOptionPane.showMessageDialog(null, "No records found for the selected date range.", "Information", JOptionPane.INFORMATION_MESSAGE);
                }

            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error fetching pump data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }

    } catch (Exception e) {
        // Catch unexpected exceptions to prevent application crash
        JOptionPane.showMessageDialog(null, "An unexpected error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }


    }//GEN-LAST:event_btn_search__stock_added_by_dateActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(PetroPointInterface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PetroPointInterface().setVisible(true);
            }
        }); 
      
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> FuelType_Combo_for_stock_by_date;
    private javax.swing.JTextField Refill_text;
    private javax.swing.JTextField added_stock_text_field;
    private javax.swing.JTextField availablestock_txt;
    private javax.swing.JButton btn_back;
    private javax.swing.JButton btn_clr;
    private javax.swing.JButton btn_refill;
    private javax.swing.JButton btn_search__stock_added_by_date;
    private com.toedter.calendar.JDateChooser jDateChooser_from_stock;
    private com.toedter.calendar.JDateChooser jDateChooser_to_stock;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTextField price_sum;
    private javax.swing.JTextField spacestock_txt;
    private javax.swing.JTextField stock_price;
    // End of variables declaration//GEN-END:variables
}
